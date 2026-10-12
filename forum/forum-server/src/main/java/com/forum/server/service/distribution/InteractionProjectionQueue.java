package com.forum.server.service.distribution;

import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Durable coalescing queue. Retained rows prevent profile versions going backwards. */
@Service
@RequiredArgsConstructor
public class InteractionProjectionQueue {
    private final JdbcTemplate jdbc;

    @Transactional
    public void post(Long id, Long previousCategory) {
        enqueue("POST", id, previousCategory);
    }

    @Transactional
    public void user(Long id) {
        enqueue("USER", id, null);
    }

    private void enqueue(String kind, Long id, Long previousCategory) {
        if (id == null) return;
        jdbc.update("""
                INSERT INTO interaction_projection (kind, target_id, previous_category_id) VALUES (?, ?, ?)
                ON DUPLICATE KEY UPDATE revision = revision + 1,
                    created_at = CASE WHEN pending = FALSE THEN CURRENT_TIMESTAMP(6) ELSE created_at END,
                    previous_category_id = COALESCE(previous_category_id, VALUES(previous_category_id)),
                    pending = TRUE, attempts = 0, next_retry_at = CURRENT_TIMESTAMP(6), last_error = NULL
                """, kind, id, previousCategory);
    }

    /** Content edits can change recommendation inputs without another like. */
    @Transactional
    public void contentChanged(Long postId, Long previousCategory) {
        post(postId, previousCategory);
        jdbc.queryForList("""
                SELECT user_id FROM post_like WHERE post_id = ?
                UNION SELECT user_id FROM favorite WHERE post_id = ?
                UNION SELECT user_id FROM comment WHERE post_id = ?
                """, Long.class, postId, postId, postId).forEach(this::user);
    }
}
