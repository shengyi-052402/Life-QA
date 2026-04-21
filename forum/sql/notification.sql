CREATE TABLE IF NOT EXISTS `notification` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '通知ID',
  `receiver_user_id` bigint NOT NULL COMMENT '接收者用户ID',
  `sender_user_id` bigint NOT NULL COMMENT '触发者用户ID',
  `type` varchar(50) NOT NULL COMMENT '通知类型',
  `post_id` bigint DEFAULT NULL COMMENT '相关帖子ID',
  `comment_id` bigint DEFAULT NULL COMMENT '相关评论ID',
  `content` varchar(255) DEFAULT NULL COMMENT '通知内容',
  `is_read` tinyint DEFAULT '0' COMMENT '0=未读,1=已读',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  KEY `idx_receiver_user_id` (`receiver_user_id`),
  KEY `idx_is_read` (`is_read`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='通知表';
