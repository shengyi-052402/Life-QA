-- Phase 3: durable Outbox delivery, database idempotency and consumer dead letters.
ALTER TABLE `notification`
  ADD COLUMN `event_id` varchar(64) DEFAULT NULL COMMENT '来源事件ID，用于消费幂等' AFTER `id`,
  ADD UNIQUE KEY `uk_notification_event_id` (`event_id`),
  ADD KEY `idx_notification_unread` (`receiver_user_id`, `is_read`, `created_at`);

CREATE TABLE IF NOT EXISTS `notification_outbox_event` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) NOT NULL,
  `receiver_user_id` bigint NOT NULL,
  `sender_user_id` bigint NOT NULL,
  `type` varchar(50) NOT NULL,
  `post_id` bigint DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=待投递,1=投递中,2=已发送,3=失败',
  `retry_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `lock_token` varchar(64) DEFAULT NULL,
  `locked_until` datetime DEFAULT NULL,
  `last_error` varchar(500) DEFAULT NULL,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `sent_at` datetime DEFAULT NULL,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_outbox_event_id` (`event_id`),
  KEY `idx_notification_outbox_dispatch` (`status`, `next_retry_at`, `locked_until`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE IF NOT EXISTS `notification_dead_letter` (
  `id` bigint NOT NULL AUTO_INCREMENT,
  `event_id` varchar(64) NOT NULL,
  `receiver_user_id` bigint NOT NULL,
  `sender_user_id` bigint NOT NULL,
  `type` varchar(50) NOT NULL,
  `post_id` bigint DEFAULT NULL,
  `comment_id` bigint DEFAULT NULL,
  `content` varchar(255) DEFAULT NULL,
  `source_topic` varchar(255) NOT NULL,
  `source_partition` int NOT NULL,
  `source_offset` bigint NOT NULL,
  `failure_reason` varchar(1000) DEFAULT NULL,
  `status` tinyint NOT NULL DEFAULT 0 COMMENT '0=待重放,1=已重发,2=已解决,3=已耗尽',
  `replay_count` int NOT NULL DEFAULT 0,
  `next_retry_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  `resolved_at` datetime DEFAULT NULL,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_notification_dead_letter_event_id` (`event_id`),
  KEY `idx_notification_dead_letter_replay` (`status`, `next_retry_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
