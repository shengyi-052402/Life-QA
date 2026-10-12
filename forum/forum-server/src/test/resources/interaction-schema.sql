CREATE TABLE post (
    id BIGINT PRIMARY KEY, title VARCHAR(255), content TEXT, summary VARCHAR(255), cover_image VARCHAR(255),
    user_id BIGINT, category_id BIGINT, location_name VARCHAR(255), address VARCHAR(255), place_id VARCHAR(255),
    latitude DECIMAL(10, 7), longitude DECIMAL(10, 7), view_count INT DEFAULT 0,
    like_count INT DEFAULT 0, comment_count INT DEFAULT 0, favorite_count INT DEFAULT 0,
    is_top INT DEFAULT 0, is_essence INT DEFAULT 0, status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE comment (
    id BIGINT PRIMARY KEY, content TEXT, user_id BIGINT, post_id BIGINT, parent_id BIGINT,
    reply_to_user_id BIGINT, like_count INT DEFAULT 0, status INT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
CREATE TABLE post_like (
    user_id BIGINT NOT NULL, post_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id)
);
CREATE TABLE post_tag (
    post_id BIGINT NOT NULL, tag_id BIGINT NOT NULL,
    PRIMARY KEY (post_id, tag_id)
);
CREATE TABLE favorite (
    user_id BIGINT NOT NULL, post_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, post_id)
);
CREATE TABLE comment_like (
    user_id BIGINT NOT NULL, comment_id BIGINT NOT NULL, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (user_id, comment_id)
);

CREATE TABLE notification_outbox_event (
    id BIGINT AUTO_INCREMENT PRIMARY KEY, event_id VARCHAR(36) NOT NULL UNIQUE,
    receiver_user_id BIGINT NOT NULL, sender_user_id BIGINT NOT NULL, type VARCHAR(32) NOT NULL,
    post_id BIGINT, comment_id BIGINT, content VARCHAR(255), status INT NOT NULL DEFAULT 0,
    retry_count INT DEFAULT 0, next_retry_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    last_error VARCHAR(1000), lock_token VARCHAR(36), locked_until TIMESTAMP,
    sent_at TIMESTAMP, created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);
