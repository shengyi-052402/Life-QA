CREATE TABLE interaction_event (
    id BIGINT NOT NULL AUTO_INCREMENT PRIMARY KEY,
    event_id VARCHAR(36) NOT NULL UNIQUE,
    kind VARCHAR(20) NOT NULL,
    user_id BIGINT NOT NULL,
    target_id BIGINT NOT NULL,
    post_id BIGINT NOT NULL,
    receiver_id BIGINT,
    active BOOLEAN NOT NULL,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    processed_at DATETIME(6),
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_error VARCHAR(1000),
    KEY ix_interaction_pending (processed_at, next_retry_at, id)
) ENGINE=InnoDB;

CREATE TABLE interaction_projection (
    kind VARCHAR(10) NOT NULL,
    target_id BIGINT NOT NULL,
    revision BIGINT NOT NULL DEFAULT 1,
    completed_revision BIGINT NOT NULL DEFAULT 0,
    previous_category_id BIGINT,
    pending BOOLEAN NOT NULL DEFAULT TRUE,
    created_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    attempts INT NOT NULL DEFAULT 0,
    next_retry_at DATETIME(6) NOT NULL DEFAULT CURRENT_TIMESTAMP(6),
    last_error VARCHAR(1000),
    PRIMARY KEY (kind, target_id),
    KEY ix_projection_pending (pending, next_retry_at)
) ENGINE=InnoDB;

CREATE TABLE interaction_worker_lock (name VARCHAR(20) PRIMARY KEY) ENGINE=InnoDB;
INSERT INTO interaction_worker_lock VALUES ('counts'), ('projections'), ('cleanup');
CREATE INDEX ix_post_like_target ON post_like (post_id, user_id);
CREATE INDEX ix_favorite_target ON favorite (post_id, user_id);
CREATE INDEX ix_comment_like_target ON comment_like (comment_id, user_id);

-- Run with old writers stopped. Baseline does not generate historical notifications.
UPDATE post p SET like_count = (SELECT COUNT(*) FROM post_like l WHERE l.post_id = p.id),
    favorite_count = (SELECT COUNT(*) FROM favorite f WHERE f.post_id = p.id);
UPDATE comment c SET like_count = (SELECT COUNT(*) FROM comment_like l WHERE l.comment_id = c.id);
INSERT INTO interaction_projection (kind, target_id) SELECT 'POST', id FROM post;
INSERT INTO interaction_projection (kind, target_id)
    SELECT 'USER', user_id FROM (SELECT user_id FROM post_like UNION SELECT user_id FROM favorite
                               UNION SELECT user_id FROM comment WHERE status = 1) u;
