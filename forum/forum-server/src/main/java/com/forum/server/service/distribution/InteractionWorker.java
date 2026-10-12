package com.forum.server.service.distribution;

import com.forum.server.messaging.NotificationEventPublisher;
import com.forum.server.service.cache.PostDetailCacheService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.*;

/** Database work and Redis work have separate durable checkpoints and independent retries. */
@Slf4j
@Service
public class InteractionWorker {
    private final JdbcTemplate jdbc;
    private final TransactionTemplate transaction;
    private final InteractionProjectionQueue queue;
    private final NotificationEventPublisher notifications;
    private final PostDetailCacheService cache;
    private final ContentRankingService ranking;
    private final UserInterestService interests;

    @Value("${forum.interactions.batch-size:500}")
    private int batchSize = 500;
    @Value("${forum.interactions.projection-batch-size:500}")
    private int projectionBatchSize = 500;

    public InteractionWorker(JdbcTemplate jdbc, PlatformTransactionManager manager,
                             InteractionProjectionQueue queue, NotificationEventPublisher notifications,
                             PostDetailCacheService cache, ContentRankingService ranking, UserInterestService interests) {
        this.jdbc = jdbc;
        this.queue = queue;
        this.notifications = notifications;
        this.cache = cache;
        this.ranking = ranking;
        this.interests = interests;
        transaction = new TransactionTemplate(manager);
        transaction.setIsolationLevel(TransactionDefinition.ISOLATION_READ_COMMITTED);
        transaction.setPropagationBehavior(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        transaction.setTimeout(30);
    }

    /**
     * 处理点赞,收藏相关事件
     */
    @Scheduled(fixedDelayString = "${forum.interactions.scan-delay-ms:1000}")
    public void processEvents() {
        List<Event> selected = new ArrayList<>();
        long started = System.nanoTime();
        try {
            transaction.executeWithoutResult(status -> {
                lock("counts");
                selected.addAll(jdbc.query("""
                        SELECT * FROM interaction_event
                        WHERE processed_at IS NULL AND next_retry_at <= CURRENT_TIMESTAMP(6)
                        ORDER BY id LIMIT ? FOR UPDATE
                        """, (rs, n) -> event(rs), batchSize));
                Set<Long> posts = new TreeSet<>();
                Set<Long> comments = new TreeSet<>();
                Set<Long> users = new TreeSet<>();
                for (Event event : selected) {
                    if (!Set.of("post_like", "favorite", "comment_like").contains(event.kind())) {
                        throw new IllegalStateException("Unknown interaction kind: " + event.kind());
                    }
                    posts.add(event.postId());
                    users.add(event.userId());
                    if (event.kind().equals("comment_like")) comments.add(event.targetId());
                    if (!exists("post", event.postId())) {
                        removePostRelations(event.postId(), users);
                    }
                    if (event.kind().equals("comment_like") && !exists("comment", event.targetId())) {
                        users.addAll(jdbc.queryForList("SELECT user_id FROM comment_like WHERE comment_id = ?",
                                Long.class, event.targetId()));
                        jdbc.update("DELETE FROM comment_like WHERE comment_id = ?", event.targetId());
                    }
                    if (event.active() && visible(event) && event.receiverId() != null
                            && event.receiverId() != event.userId()) {
                        String type = event.kind().equals("favorite") ? "post_favorite" : event.kind();
                        notifications.publish(event.receiverId(), event.userId(), type, event.postId(),
                                event.kind().equals("comment_like") ? event.targetId() : null,
                                event.kind().equals("favorite") ? "favorited your post" :
                                        event.kind().equals("comment_like") ? "liked your comment" : "liked your post");
                    }
                }
                for (Long id : posts) {
                    long likes = count("SELECT COUNT(*) FROM post_like WHERE post_id = ?", id);
                    long favorites = count("SELECT COUNT(*) FROM favorite WHERE post_id = ?", id);
                    jdbc.update("UPDATE post SET like_count = ?, favorite_count = ? WHERE id = ?", likes, favorites, id);
                    queue.post(id, null);
                }
                for (Long id : comments) {
                    jdbc.update("UPDATE comment SET like_count = ? WHERE id = ?",
                            count("SELECT COUNT(*) FROM comment_like WHERE comment_id = ?", id), id);
                }
                users.forEach(queue::user);
                for (Event event : selected) {
                    jdbc.update("UPDATE interaction_event SET processed_at = CURRENT_TIMESTAMP(6), last_error = NULL WHERE id = ?",
                            event.id());
                }
            });
            if (!selected.isEmpty()) log.debug("Interaction batch committed: size={}, elapsedMs={}", selected.size(),
                    (System.nanoTime() - started) / 1_000_000);
        } catch (RuntimeException ex) {
            log.error("Interaction batch failed; durable events retained, size={}", selected.size(), ex);
            for (Event event : selected) {
                retryEvent(event, ex);
            }
        }
    }

    /**
     * 执行缓存,热榜和用户兴趣更新任务
     */
    @Scheduled(fixedDelayString = "${forum.interactions.projection-delay-ms:1000}")
    public void processProjections() {
        for (int i = 0; i < projectionBatchSize; i++) {
            List<Job> selected = new ArrayList<>();
            try {
                transaction.executeWithoutResult(status -> {
                    lock("projections");
                    selected.addAll(jdbc.query("""
                            SELECT * FROM interaction_projection
                            WHERE pending = TRUE AND next_retry_at <= CURRENT_TIMESTAMP(6)
                            ORDER BY created_at, kind, target_id LIMIT 1 FOR UPDATE
                            """, (rs, n) -> new Job(rs.getString("kind"), rs.getLong("target_id"),
                            rs.getLong("revision"), rs.getObject("previous_category_id", Long.class),
                            rs.getInt("attempts"))));
                    if (selected.isEmpty()) return;
                    Job job = selected.get(0);
                    // Row stays locked through Redis execution. Later enqueue increments after completion.
                    if (job.kind().equals("POST")) {
                        cache.evictStrict(job.targetId());
                        ranking.refreshStrict(job.targetId(), job.previousCategory());
                    } else if (job.kind().equals("USER")) {
                        interests.rebuildStrict(job.targetId(), job.revision());
                    } else {
                        throw new IllegalStateException("Unknown projection kind: " + job.kind());
                    }
                    jdbc.update("""
                            UPDATE interaction_projection SET completed_revision = ?, pending = FALSE,
                                previous_category_id = NULL, attempts = 0, last_error = NULL
                            WHERE kind = ? AND target_id = ? AND revision = ?
                            """, job.revision(), job.kind(), job.targetId(), job.revision());
                });
                if (selected.isEmpty()) break;
            } catch (RuntimeException ex) {
                log.error("Interaction projection failed; job retained", ex);
                for (Job job : selected) {
                    try {
                        jdbc.update("""
                                UPDATE interaction_projection SET attempts = attempts + 1, next_retry_at = ?, last_error = ?
                                WHERE kind = ? AND target_id = ? AND pending = TRUE AND revision = ?
                                """, retryAt(job.attempts()), error(ex), job.kind(), job.targetId(), job.revision());
                    } catch (RuntimeException retryFailure) {
                        log.error("Cannot record projection retry; original job remains pending", retryFailure);
                    }
                }
                if (selected.isEmpty()) break;
            }
        }
    }

    @Scheduled(fixedDelayString = "${forum.interactions.cleanup-delay-ms:60000}")
    public void cleanOrphans() {
        try {
            transaction.executeWithoutResult(status -> {
                lock("cleanup");
                Set<Long> users = new TreeSet<>();
                for (String table : List.of("post_like", "favorite", "comment_like")) {
                    boolean comment = table.equals("comment_like");
                    String column = comment ? "comment_id" : "post_id";
                    String joins = comment ? "LEFT JOIN comment c ON c.id = r.comment_id LEFT JOIN post p ON p.id = c.post_id"
                            : "LEFT JOIN post p ON p.id = r.post_id";
                    List<Map<String, Object>> rows = jdbc.queryForList("SELECT r.user_id, r." + column + " AS target_id FROM "
                            + table + " r " + joins + " WHERE p.id IS NULL LIMIT ?", batchSize);
                    for (Map<String, Object> row : rows) {
                        long user = ((Number) row.get("user_id")).longValue();
                        long target = ((Number) row.get("target_id")).longValue();
                        jdbc.update("DELETE FROM " + table + " WHERE user_id = ? AND " + column + " = ?", user, target);
                        users.add(user);
                    }
                }
                users.forEach(queue::user);
            });
        } catch (RuntimeException ex) {
            log.error("Orphan cleanup failed; will retry next minute", ex);
        }
    }

    @Scheduled(fixedDelayString = "${forum.interactions.monitor-delay-ms:30000}")
    public void monitor() {
        try {
            for (String table : List.of("interaction_event", "interaction_projection")) {
                String pending = table.equals("interaction_event") ? "processed_at IS NULL" : "pending = TRUE";
                Map<String, Object> metrics = jdbc.queryForMap("SELECT COUNT(*) AS backlog, MIN(created_at) AS oldest, "
                        + "COALESCE(SUM(attempts), 0) AS retries FROM " + table + " WHERE " + pending);
                log.info("Interaction queue {}: {}", table, metrics);
                Object oldest = metrics.get("oldest");
                if (oldest instanceof Timestamp time && time.toLocalDateTime().isBefore(LocalDateTime.now().minusSeconds(5))) {
                    log.warn("Interaction queue {} has work older than the 5-second target", table);
                }
            }
        } catch (RuntimeException ex) {
            log.error("Cannot read interaction backlog metrics", ex);
        }
    }

    private void lock(String name) {
        jdbc.queryForObject("SELECT name FROM interaction_worker_lock WHERE name = ? FOR UPDATE", String.class, name);
    }

    private boolean exists(String table, long id) {
        return count("SELECT COUNT(*) FROM " + table + " WHERE id = ?", id) > 0;
    }

    private boolean visible(Event event) {
        if (count("SELECT COUNT(*) FROM post WHERE id = ? AND status = 1", event.postId()) == 0) return false;
        return !event.kind().equals("comment_like") ||
                count("SELECT COUNT(*) FROM comment WHERE id = ? AND status = 1", event.targetId()) > 0;
    }

    private void removePostRelations(long postId, Set<Long> users) {
        for (String table : List.of("post_like", "favorite")) {
            users.addAll(jdbc.queryForList("SELECT user_id FROM " + table + " WHERE post_id = ?", Long.class, postId));
            jdbc.update("DELETE FROM " + table + " WHERE post_id = ?", postId);
        }
    }

    private long count(String sql, Object... args) {
        return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class, args));
    }

    private Event event(ResultSet rs) throws SQLException {
        return new Event(rs.getLong("id"), rs.getString("kind"), rs.getLong("user_id"), rs.getLong("target_id"),
                rs.getLong("post_id"), rs.getObject("receiver_id", Long.class), rs.getBoolean("active"), rs.getInt("attempts"));
    }

    private void retryEvent(Event event, RuntimeException ex) {
        try {
            jdbc.update("""
                    UPDATE interaction_event SET attempts = attempts + 1, next_retry_at = ?, last_error = ?
                    WHERE id = ? AND processed_at IS NULL
                    """, retryAt(event.attempts()), error(ex), event.id());
        } catch (RuntimeException retryFailure) {
            log.error("Cannot record event retry; original event remains pending", retryFailure);
        }
    }

    private Timestamp retryAt(int attempts) {
        return Timestamp.valueOf(LocalDateTime.now().plusSeconds(Math.min(60, 1L << Math.min(attempts, 6))));
    }

    private String error(RuntimeException ex) {
        String message = ex.toString();
        return message.substring(0, Math.min(message.length(), 1000));
    }

    private record Event(long id, String kind, long userId, long targetId, long postId, Long receiverId,
                         boolean active, int attempts) { }
    private record Job(String kind, long targetId, long revision, Long previousCategory, int attempts) { }
}
