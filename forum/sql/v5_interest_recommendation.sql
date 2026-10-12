-- Phase 2: rebuild a user's recent interest profile without full table scans.
ALTER TABLE `post_like`
  ADD KEY `idx_post_like_user_created` (`user_id`, `created_at`);

ALTER TABLE `favorite`
  ADD KEY `idx_favorite_user_created` (`user_id`, `created_at`);

ALTER TABLE `comment`
  ADD KEY `idx_comment_user_status_created` (`user_id`, `status`, `created_at`);
