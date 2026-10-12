package com.forum.server.service.distribution;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.forum.pojo.entity.Post;
import com.forum.server.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import lombok.extern.slf4j.Slf4j;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.LinkedHashSet;
import java.util.UUID;

/** Maintains the all-site and category 24-hour hot rankings. */
@Service
@RequiredArgsConstructor
@Slf4j
public class ContentRankingService {
    private static final String ALL_KEY = "forum:distribution:hot:24h:all";
    private static final String CATEGORY_KEY_PREFIX = "forum:distribution:hot:24h:category:";
    private static final String KEY_REGISTRY = "forum:distribution:hot:keys";
    private static final String REBUILD_LOCK = "forum:distribution:hot:rebuild:lock";
    private static final Duration KEY_TTL = Duration.ofHours(26);
    private static final Duration REBUILD_LOCK_TTL = Duration.ofMinutes(5);

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;
    private final InteractionProjectionQueue projectionQueue;

    public void refreshPost(Long postId) {
        refreshPost(postId, null);
    }

    public void refreshPost(Long postId, Long previousCategoryId) {
        projectionQueue.post(postId, previousCategoryId);
    }

    /**
     * 删除变化帖子的热榜缓存,重新刷新榜单
     * @param postId
     * @param previousCategoryId
     */
    public void refreshStrict(Long postId, Long previousCategoryId) {
        if (postId == null) return;
        Post post = postMapper.selectById(postId);
        Set<String> registered = stringRedisTemplate.opsForSet().members(KEY_REGISTRY);
        if (registered != null) {
            for (String key : registered) {
                if (key.startsWith(CATEGORY_KEY_PREFIX) &&
                        (post == null || !key.equals(categoryKey(post.getCategoryId())))) {
                    stringRedisTemplate.opsForZSet().remove(key, String.valueOf(postId));
                }
            }
        }
        if (previousCategoryId != null && (post == null || !previousCategoryId.equals(post.getCategoryId()))) {
            remove(postId, previousCategoryId);
        }
        if (post == null || post.getStatus() == null || post.getStatus() != 1 || isExpired(post)) {
            remove(postId, post == null ? previousCategoryId : post.getCategoryId());
            return;
        }
        add(post);
    }

    public List<Long> getHotPostIds(Long categoryId, int limit) {
        if (limit <= 0) return Collections.emptyList();
        try {
            String key = categoryId == null ? ALL_KEY : categoryKey(categoryId);
            Set<String> values = stringRedisTemplate.opsForZSet().reverseRange(key, 0, limit - 1L);
            if (values == null || values.isEmpty()) return Collections.emptyList();
            List<Long> ids = new ArrayList<>(values.size());
            for (String value : values) ids.add(Long.valueOf(value));
            return ids;
        } catch (RuntimeException ignored) {
            return Collections.emptyList();
        }
    }

    public List<Long> getAllHotPostIds(Long categoryId) {
        try {
            String key = categoryId == null ? ALL_KEY : categoryKey(categoryId);
            Set<String> values = stringRedisTemplate.opsForZSet().reverseRange(key, 0, -1);
            if (values == null) return Collections.emptyList();
            List<Long> ids = new ArrayList<>(values.size());
            for (String value : values) ids.add(Long.valueOf(value));
            return ids;
        } catch (RuntimeException ex) {
            log.warn("Read hot ranking failed, categoryId={}", categoryId, ex);
            return Collections.emptyList();
        }
    }

    public double hotScore(Post post) {
        double engagement = value(post.getViewCount()) * 0.2
                + value(post.getLikeCount()) * 2.0
                + value(post.getCommentCount()) * 3.0
                + value(post.getFavoriteCount()) * 4.0;
        double ageHours = Math.max(0, Duration.between(post.getCreatedAt(), LocalDateTime.now()).toMinutes() / 60.0);
        return (engagement + 1.0) * Math.exp(-ageHours / 12.0);
    }

    @Scheduled(cron = "${forum.distribution.hot-rebuild-cron:0 0 * * * *}")
    public void rebuildHourly() {
        String token = UUID.randomUUID().toString();
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(REBUILD_LOCK, token, REBUILD_LOCK_TTL);
            if (!Boolean.TRUE.equals(locked)) return;
            rebuildNow();
        } catch (RuntimeException ex) {
            log.warn("Rebuild hot ranking failed", ex);
        } finally {
            try {
                String current = stringRedisTemplate.opsForValue().get(REBUILD_LOCK);
                if (token.equals(current)) stringRedisTemplate.delete(REBUILD_LOCK);
            } catch (RuntimeException ignored) {
            }
        }
    }

    private void rebuildNow() {
        LocalDateTime since = LocalDateTime.now().minusHours(24);
        List<Post> posts = postMapper.selectList(new LambdaQueryWrapper<Post>()
                .eq(Post::getStatus, 1)
                .ge(Post::getCreatedAt, since));
        // The same durable worker handles hourly decay, expiration and event refreshes.
        // Do not clear/repopulate Redis concurrently with a newer per-post projection.
        Set<Long> ids = posts.stream().map(Post::getId).collect(java.util.stream.Collectors.toSet());
        Set<String> previous = stringRedisTemplate.opsForZSet().range(ALL_KEY, 0, -1);
        if (previous != null) previous.forEach(id -> ids.add(Long.valueOf(id)));
        ids.forEach(id -> projectionQueue.post(id, null));
    }

    /**
     * 添加帖子,刷新缓存
     * @param post
     */
    private void add(Post post) {
        String id = String.valueOf(post.getId());
        double score = hotScore(post);
        stringRedisTemplate.opsForZSet().add(ALL_KEY, id, score);
        stringRedisTemplate.expire(ALL_KEY, KEY_TTL);
        stringRedisTemplate.opsForSet().add(KEY_REGISTRY, ALL_KEY);
        stringRedisTemplate.expire(KEY_REGISTRY, KEY_TTL);
        if (post.getCategoryId() != null) {
            String key = categoryKey(post.getCategoryId());
            stringRedisTemplate.opsForZSet().add(key, id, score);
            stringRedisTemplate.expire(key, KEY_TTL);
            stringRedisTemplate.opsForSet().add(KEY_REGISTRY, key);
        }
    }

    /**
     * 移除缓存
     * @param postId
     * @param categoryId
     */
    private void remove(Long postId, Long categoryId) {
        stringRedisTemplate.opsForZSet().remove(ALL_KEY, String.valueOf(postId));
        if (categoryId != null) stringRedisTemplate.opsForZSet().remove(categoryKey(categoryId), String.valueOf(postId));
    }

    private boolean isExpired(Post post) {
        return post.getCreatedAt() == null || post.getCreatedAt().isBefore(LocalDateTime.now().minusHours(24));
    }

    private String categoryKey(Long categoryId) {
        return CATEGORY_KEY_PREFIX + categoryId;
    }

    private int value(Integer value) {
        return value == null ? 0 : value;
    }
}
