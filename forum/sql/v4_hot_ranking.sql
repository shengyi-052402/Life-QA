-- Phase 1: query support for the 24-hour global/category hot ranking.
ALTER TABLE `post`
  ADD KEY `idx_post_hot_listing` (`status`, `is_top`, `category_id`, `created_at`);
