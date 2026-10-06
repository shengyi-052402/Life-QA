package com.forum.server.service.cache;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

/**
 * Cleans up legacy interaction Sets. Membership and counts are now owned by MySQL;
 * no request initializes or toggles these Sets, including when Redis is unavailable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionCacheService {

    private final StringRedisTemplate stringRedisTemplate;

    public void evictPostLike(Long postId) {
        evict("forum:post:like:users:" + postId, "forum:post:like:init:" + postId);
    }

    public void evictPostFavorite(Long postId) {
        evict("forum:post:favorite:users:" + postId, "forum:post:favorite:init:" + postId);
    }

    private void evict(String setKey, String initKey) {
        try {
            stringRedisTemplate.delete(List.of(setKey, initKey));
        } catch (Exception e) {
            log.warn("Evict post interaction cache failed, setKey={}", setKey, e);
        }
    }
}