package com.forum.server.service.cache;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostInteractionCacheService {
    private static final String LIKE_SET_KEY_PREFIX = "forum:post:like:users:";
    private static final String LIKE_INIT_KEY_PREFIX = "forum:post:like:init:";
    private static final String FAVORITE_SET_KEY_PREFIX = "forum:post:favorite:users:";
    private static final String FAVORITE_INIT_KEY_PREFIX = "forum:post:favorite:init:";

    private static final DefaultRedisScript<Long> TOGGLE_MEMBERSHIP_SCRIPT = new DefaultRedisScript<>(
            """
            if redis.call('SISMEMBER', KEYS[1], ARGV[1]) == 1 then
                redis.call('SREM', KEYS[1], ARGV[1])
                return 0
            end
            redis.call('SADD', KEYS[1], ARGV[1])
            return 1
            """,
            Long.class
    );

    private final StringRedisTemplate stringRedisTemplate;

    public void initializePostLikeUsers(Long postId, List<Long> userIds) {
        initializeSet(likeSetKey(postId), likeInitKey(postId), userIds);
    }

    public void initializePostFavoriteUsers(Long postId, List<Long> userIds) {
        initializeSet(favoriteSetKey(postId), favoriteInitKey(postId), userIds);
    }

    /**
     * 判断post_like有没有初始化
     * @param postId
     * @return
     */
    public boolean isPostLikeInitialized(Long postId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(likeInitKey(postId)));
    }

    public boolean isPostFavoriteInitialized(Long postId) {
        return Boolean.TRUE.equals(stringRedisTemplate.hasKey(favoriteInitKey(postId)));
    }
    /**
     * 调用lua脚本,原子性的切换点赞状态
     * 返回数据包含当前点赞收藏状态,及其数量count
     * @param postId
     * @param userId
     * @return
     */
    public Optional<ToggleResult> togglePostLike(Long postId, Long userId) {
        return toggleMembership(likeSetKey(postId), userId);
    }
    /**
     * 调用lua脚本,原子性的切换收藏状态
     * 返回数据包含当前点赞收藏状态,及其数量count
     * @param postId
     * @param userId
     * @return
     */
    public Optional<ToggleResult> togglePostFavorite(Long postId, Long userId) {
        return toggleMembership(favoriteSetKey(postId), userId);
    }

    public void evictPostLike(Long postId) {
        evict(likeSetKey(postId), likeInitKey(postId));
    }

    public void evictPostFavorite(Long postId) {
        evict(favoriteSetKey(postId), favoriteInitKey(postId));
    }

    private void initializeSet(String setKey, String initKey, List<Long> userIds) {
        try {
            if (Boolean.TRUE.equals(stringRedisTemplate.hasKey(initKey))) {
                return;
            }

            // 初始化时以 MySQL 为准重建 Redis Set，避免项目重启后第一次互动误判用户状态。
            stringRedisTemplate.delete(setKey);
            if (userIds != null && !userIds.isEmpty()) {
                String[] members = userIds.stream().map(String::valueOf).toArray(String[]::new);
                stringRedisTemplate.opsForSet().add(setKey, members);
            }
            stringRedisTemplate.opsForValue().set(initKey, "1");
        } catch (Exception e) {
            log.warn("Initialize post interaction cache failed, setKey={}", setKey, e);
        }
    }

    /**
     * 调用lua脚本,原子性的切换点赞/收藏状态
     * 返回数据包含当前点赞收藏状态,及其数量count
     * @param setKey
     * @param userId
     * @return
     */
    private Optional<ToggleResult> toggleMembership(String setKey, Long userId) {
        try {
            // Lua 保证“判断是否已互动”和“添加/删除用户”在 Redis 内原子完成，避免并发重复点击造成状态翻转错乱。
            Long scriptResult = stringRedisTemplate.execute(
                    TOGGLE_MEMBERSHIP_SCRIPT,
                    Collections.singletonList(setKey),
                    String.valueOf(userId)
            );
            if (scriptResult == null) {
                return Optional.empty();
            }

            Long count = stringRedisTemplate.opsForSet().size(setKey);
            return Optional.of(new ToggleResult(scriptResult == 1L, count == null ? 0L : count));
        } catch (Exception e) {
            log.warn("Toggle post interaction cache failed, setKey={}, userId={}", setKey, userId, e);
            return Optional.empty();
        }
    }

    private void evict(String setKey, String initKey) {
        try {
            stringRedisTemplate.delete(List.of(setKey, initKey));
        } catch (Exception e) {
            log.warn("Evict post interaction cache failed, setKey={}", setKey, e);
        }
    }

    private String likeSetKey(Long postId) {
        return LIKE_SET_KEY_PREFIX + postId;
    }

    private String likeInitKey(Long postId) {
        return LIKE_INIT_KEY_PREFIX + postId;
    }

    private String favoriteSetKey(Long postId) {
        return FAVORITE_SET_KEY_PREFIX + postId;
    }

    private String favoriteInitKey(Long postId) {
        return FAVORITE_INIT_KEY_PREFIX + postId;
    }

    @Getter
    @AllArgsConstructor
    public static class ToggleResult {
        //点赞收藏的状态
        private final boolean active;
        private final long count;
    }
}
