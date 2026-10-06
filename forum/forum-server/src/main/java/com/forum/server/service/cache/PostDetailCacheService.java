package com.forum.server.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.pojo.vo.PostDetailVO;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostDetailCacheService {
    private static final String POST_DETAIL_KEY_PREFIX = "forum:post:detail:";
    private static final String POST_DETAIL_LOCK_KEY_PREFIX = "forum:post:detail:lock:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${forum.cache.post-detail-ttl-seconds:600}")
    private long postDetailTtlSeconds;

    @Value("${forum.cache.post-detail-ttl-jitter-seconds:120}")
    private long postDetailTtlJitterSeconds;

    @Value("${forum.cache.post-detail-stale-retain-seconds:300}")
    private long postDetailStaleRetainSeconds;

    @Value("${forum.cache.post-detail-rebuild-lock-seconds:10}")
    private long postDetailRebuildLockSeconds;

    public CacheResult get(Long postId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(buildKey(postId));
            if (json == null || json.isBlank()) {
                return CacheResult.miss();
            }

            JsonNode root = objectMapper.readTree(json);
            if (root.has("data") && root.has("expireAtMillis")) {
                PostDetailVO postDetail = objectMapper.treeToValue(root.get("data"), PostDetailVO.class);
                long expireAtMillis = root.get("expireAtMillis").asLong();
                // 逻辑过期：Redis key 还在，但里面记录的业务过期时间已经到了。
                // 热点 key 过期时可以先返回旧数据，再让一个请求后台重建缓存。
                return CacheResult.hit(postDetail, System.currentTimeMillis() >= expireAtMillis);
            }

            // 兼容旧版本缓存：旧值只有 PostDetailVO，没有逻辑过期字段。
            return CacheResult.hit(objectMapper.readValue(json, PostDetailVO.class), false);
        } catch (Exception e) {
            log.warn("Read post detail cache failed, postId={}", postId, e);
            return CacheResult.miss();
        }
    }

    public void put(Long postId, PostDetailVO postDetail) {
        try {
            Duration logicalTtl = buildLogicalTtl();
            CachedPostDetail cached = new CachedPostDetail(
                    postDetail,
                    System.currentTimeMillis() + logicalTtl.toMillis()
            );
            stringRedisTemplate.opsForValue().set(
                    buildKey(postId),
                    objectMapper.writeValueAsString(cached),
                    buildPhysicalTtl(logicalTtl)
            );
        } catch (JsonProcessingException e) {
            log.warn("Serialize post detail cache failed, postId={}", postId, e);
        } catch (Exception e) {
            log.warn("Write post detail cache failed, postId={}", postId, e);
        }
    }
//清缓存
    public void evict(Long postId) {
        try {
            stringRedisTemplate.delete(buildKey(postId));
            stringRedisTemplate.delete(buildLockKey(postId));
        } catch (Exception e) {
            log.warn("Evict post detail cache failed, postId={}", postId, e);
        }
    }

    public boolean tryLockRebuild(Long postId) {
        try {
            Boolean locked = stringRedisTemplate.opsForValue().setIfAbsent(
                    buildLockKey(postId),
                    "1",
                    Duration.ofSeconds(postDetailRebuildLockSeconds)
            );
            return Boolean.TRUE.equals(locked);
        } catch (Exception e) {
            log.warn("Lock post detail cache rebuild failed, postId={}", postId, e);
            return false;
        }
    }

    public void unlockRebuild(Long postId) {
        try {
            stringRedisTemplate.delete(buildLockKey(postId));
        } catch (Exception e) {
            log.warn("Unlock post detail cache rebuild failed, postId={}", postId, e);
        }
    }
//设置动态抖动ttl---逻辑过期时间
    private Duration buildLogicalTtl() {
        long jitter = postDetailTtlJitterSeconds <= 0
                ? 0
                : ThreadLocalRandom.current().nextLong(postDetailTtlJitterSeconds + 1);
        return Duration.ofSeconds(postDetailTtlSeconds + jitter);
    }
//设置物理过期时间
    private Duration buildPhysicalTtl(Duration logicalTtl) {
        // 物理 TTL 比逻辑 TTL 更长，让过期后的旧值还能短暂留在 Redis 中兜底。
        long retainSeconds = Math.max(1, postDetailStaleRetainSeconds);
        return logicalTtl.plusSeconds(retainSeconds);
    }

    private String buildKey(Long postId) {
        return POST_DETAIL_KEY_PREFIX + postId;
    }

    private String buildLockKey(Long postId) {
        return POST_DETAIL_LOCK_KEY_PREFIX + postId;
    }

    @Getter
    @AllArgsConstructor
    private static class CachedPostDetail {
        private final PostDetailVO data;
        private final long expireAtMillis;
    }

    @Getter
    @AllArgsConstructor
    public static class CacheResult {
        private final PostDetailVO postDetail;
        private final boolean expired;

        public static CacheResult miss() {
            return new CacheResult(null, false);
        }

        public static CacheResult hit(PostDetailVO postDetail, boolean expired) {
            return new CacheResult(postDetail, expired);
        }

        public boolean hit() {
            return postDetail != null;
        }
    }
}
