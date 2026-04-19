CREATE DATABASE IF NOT EXISTS forum DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;

USE forum;

-- 用户表
CREATE TABLE IF NOT EXISTS `user` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '用户ID',
  `username` varchar(50) NOT NULL COMMENT '用户名',
  `email` varchar(100) NOT NULL COMMENT '邮箱',
  `password` varchar(255) NOT NULL COMMENT '密码(BCrypt加密)',
  `nickname` varchar(50) DEFAULT NULL COMMENT '昵称',
  `avatar` varchar(255) DEFAULT NULL COMMENT '头像URL',
  `bio` varchar(500) DEFAULT NULL COMMENT '个人简介',
  `role` tinyint DEFAULT '0' COMMENT '0=普通用户, 1=管理员',
  `status` tinyint DEFAULT '1' COMMENT '0=禁用, 1=正常',
  `post_count` int DEFAULT '0' COMMENT '发帖数(冗余)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '注册时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_username` (`username`),
  UNIQUE KEY `uk_email` (`email`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户表';

-- 插入默认管理员账号 (admin / 123456)
INSERT INTO `user` (`username`, `email`, `password`, `nickname`, `role`, `status`) 
VALUES ('admin', 'admin@forum.com', '$2a$10$Rz2QOfY/O01C7aL9m241Y.YdGDBQ7x.0MGEj12bO88hG0YV8GzQJ2', '管理员', 1, 1)
ON DUPLICATE KEY UPDATE id=id;

-- 分类表
CREATE TABLE IF NOT EXISTS `category` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '分类ID',
  `name` varchar(50) NOT NULL COMMENT '分类名称',
  `description` varchar(255) DEFAULT NULL COMMENT '分类描述',
  `icon` varchar(50) DEFAULT NULL COMMENT '图标',
  `sort_order` int DEFAULT '0' COMMENT '排序序号',
  `post_count` int DEFAULT '0' COMMENT '帖子数(冗余)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='分类表';

-- 插入默认分类
INSERT INTO `category` (`name`, `description`, `sort_order`) VALUES 
('技术讨论', '各类编程技术交流与分享', 1),
('问答求助', '技术问题提问与解答', 2),
('灌水区', '日常闲聊放松', 3)
ON DUPLICATE KEY UPDATE id=id;

-- 帖子表
CREATE TABLE IF NOT EXISTS `post` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '帖子ID',
  `title` varchar(200) NOT NULL COMMENT '标题',
  `content` longtext NOT NULL COMMENT '内容(HTML)',
  `summary` varchar(500) DEFAULT NULL COMMENT '摘要(用户手填)',
  `cover_image` varchar(255) NOT NULL COMMENT '背景封面图',
  `user_id` bigint NOT NULL COMMENT '作者ID',
  `category_id` bigint NOT NULL COMMENT '分类ID',
  `view_count` int DEFAULT '0' COMMENT '浏览量',
  `like_count` int DEFAULT '0' COMMENT '点赞数(冗余)',
  `comment_count` int DEFAULT '0' COMMENT '评论数(冗余)',
  `favorite_count` int DEFAULT '0' COMMENT '收藏数(冗余)',
  `is_top` tinyint DEFAULT '0' COMMENT '是否置顶',
  `is_essence` tinyint DEFAULT '0' COMMENT '是否精华',
  `status` tinyint DEFAULT '1' COMMENT '0=待审核, 1=正常, 2=隐藏',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '发帖时间',
  `updated_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_category_id` (`category_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子表';

-- 评论表
CREATE TABLE IF NOT EXISTS `comment` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '评论ID',
  `content` text NOT NULL COMMENT '评论内容',
  `user_id` bigint NOT NULL COMMENT '评论用户ID',
  `post_id` bigint NOT NULL COMMENT '所属帖子ID',
  `parent_id` bigint DEFAULT NULL COMMENT '父评论ID(NULL=一级评论)',
  `reply_to_user_id` bigint DEFAULT NULL COMMENT '回复目标用户ID',
  `like_count` int DEFAULT '0' COMMENT '点赞数(冗余)',
  `status` tinyint DEFAULT '1' COMMENT '0=隐藏, 1=正常',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '评论时间',
  PRIMARY KEY (`id`),
  KEY `idx_post_id` (`post_id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_parent_id` (`parent_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论表';

-- 标签表
CREATE TABLE IF NOT EXISTS `tag` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '标签ID',
  `name` varchar(30) NOT NULL COMMENT '标签名',
  `post_count` int DEFAULT '0' COMMENT '使用次数(冗余)',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_name` (`name`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='标签表';

-- 帖子-标签关联表
CREATE TABLE IF NOT EXISTS `post_tag` (
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `tag_id` bigint NOT NULL COMMENT '标签ID',
  PRIMARY KEY (`post_id`,`tag_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子标签关联表';

-- 帖子点赞表
CREATE TABLE IF NOT EXISTS `post_like` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='帖子点赞表';

-- 评论点赞表
CREATE TABLE IF NOT EXISTS `comment_like` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `comment_id` bigint NOT NULL COMMENT '评论ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '点赞时间',
  PRIMARY KEY (`user_id`,`comment_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='评论点赞表';

-- 收藏表
CREATE TABLE IF NOT EXISTS `favorite` (
  `user_id` bigint NOT NULL COMMENT '用户ID',
  `post_id` bigint NOT NULL COMMENT '帖子ID',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '收藏时间',
  PRIMARY KEY (`user_id`,`post_id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='收藏表';

-- 操作日志表
CREATE TABLE IF NOT EXISTS `operation_log` (
  `id` bigint NOT NULL AUTO_INCREMENT COMMENT '日志ID',
  `user_id` bigint DEFAULT NULL COMMENT '操作人ID',
  `action` varchar(50) NOT NULL COMMENT '操作类型',
  `target_type` varchar(30) DEFAULT NULL COMMENT '目标类型',
  `target_id` bigint DEFAULT NULL COMMENT '目标ID',
  `detail` varchar(500) DEFAULT NULL COMMENT '操作详情',
  `ip` varchar(50) DEFAULT NULL COMMENT '操作IP',
  `created_at` datetime NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '操作时间',
  PRIMARY KEY (`id`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='操作日志表';
