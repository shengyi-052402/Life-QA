-- Optional one-time repair for counters corrupted by the previous Redis-first implementation.
-- Run during maintenance AFTER stopping all old-version application instances.
-- This script is deliberately not a Flyway migration and is not executed by application startup.
-- After commit, clear affected post detail caches (and refresh hot rankings) through the application.
START TRANSACTION;

-- Keep the application lock order: post -> comment -> interaction records.
SELECT id FROM post ORDER BY id FOR UPDATE;
SELECT id FROM comment ORDER BY id FOR UPDATE;

UPDATE post p
SET p.like_count = (SELECT COUNT(*) FROM post_like pl WHERE pl.post_id = p.id),
    p.favorite_count = (SELECT COUNT(*) FROM favorite f WHERE f.post_id = p.id),
    p.comment_count = (SELECT COUNT(*) FROM comment c WHERE c.post_id = p.id AND c.status = 1);

UPDATE comment c
SET c.like_count = (SELECT COUNT(*) FROM comment_like cl WHERE cl.comment_id = c.id);

COMMIT;
