-- =============================================
-- v3: 帖子表新增地理位置字段
-- 执行方式: 在 MySQL 中执行此脚本（只需执行一次）
-- =============================================

ALTER TABLE `post`
  ADD COLUMN `location_name` varchar(100) DEFAULT NULL COMMENT '发帖地区名称（城市/地区）',
  ADD COLUMN `latitude`      decimal(9,6) DEFAULT NULL COMMENT '纬度 (-90 to 90)',
  ADD COLUMN `longitude`     decimal(9,6) DEFAULT NULL COMMENT '经度 (-180 to 180)';

-- 为经纬度字段添加索引，以便后续地理范围查询
ALTER TABLE `post` ADD INDEX `idx_location` (`latitude`, `longitude`);
