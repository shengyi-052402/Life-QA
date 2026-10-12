-- Post.java and PostCreateDTO both expose these fields. Keep the schema in sync
-- so inserts and MyBatis-Plus generated selects cannot reference missing columns.
ALTER TABLE `post`
  ADD COLUMN `address` varchar(255) DEFAULT NULL COMMENT '精确地址' AFTER `location_name`,
  ADD COLUMN `place_id` varchar(255) DEFAULT NULL COMMENT '地图地点 ID' AFTER `address`;
