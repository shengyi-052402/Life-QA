package com.forum.server.service.cache;

import com.forum.server.mapper.PostMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostBloomFilterService {
    private static final String DEFAULT_KEY = "forum:post:bloom";
    private static final String READY_KEY_SUFFIX = ":ready";

    private final StringRedisTemplate stringRedisTemplate;
    private final PostMapper postMapper;

    @Value("${forum.cache.post-bloom-filter-key:" + DEFAULT_KEY + "}")
    private String bloomFilterKey;

    @Value("${forum.cache.post-bloom-filter-bit-size:1048576}")
    private long bitSize;

    @Value("${forum.cache.post-bloom-filter-hash-count:7}")
    private int hashCount;

    /**
     * 项目启动后把当前数据库中“正常状态”的帖子 ID 放进布隆过滤器。
     * 布隆过滤器本质上是一组 bit 位：一个 ID 会通过多个 hash 函数映射到多个 bit，
     * 查询时只要有任意一个 bit 不存在，就说明这个 ID 一定不存在。
     */
    @EventListener(ApplicationReadyEvent.class)
    public void loadExistingPosts() {
        try {
            List<Long> postIds = postMapper.selectActivePostIds();
            for (Long postId : postIds) {
                add(postId);
            }
            stringRedisTemplate.opsForValue().set(readyKey(), "1");
            log.info("Loaded post bloom filter, count={}", postIds.size());
        } catch (Exception e) {
            // Redis 不可用时不能影响应用启动；后续查询会退化为原来的数据库兜底路径。
            log.warn("Load post bloom filter failed", e);
        }
    }

    public void add(Long postId) {
        if (postId == null || bitSize <= 0 || hashCount <= 0) {
            return;
        }

        try {
            for (int i = 0; i < hashCount; i++) {
                stringRedisTemplate.opsForValue().setBit(bloomFilterKey, offset(postId, i), true);
            }
        } catch (Exception e) {
            log.warn("Add post id to bloom filter failed, postId={}", postId, e);
        }
    }

    public boolean mightContain(Long postId) {
        if (postId == null || bitSize <= 0 || hashCount <= 0) {
            return false;
        }

        try {
            // ready 标记不存在时，说明过滤器还没预热或 Redis 数据被清空。
            // 这里选择放行到数据库，避免把真实帖子误判为不存在。

            if (!Boolean.TRUE.equals(stringRedisTemplate.hasKey(readyKey()))) {
                return true;
            }
            for (int i = 0; i < hashCount; i++) {
                Boolean exists = stringRedisTemplate.opsForValue().getBit(bloomFilterKey, offset(postId, i));
                if (!Boolean.TRUE.equals(exists)) {
                    return false;
                }
            }
            return true;
        } catch (Exception e) {
            // 查询布隆过滤器失败时返回 true，表示“可能存在”，交给 MySQL 兜底，避免误杀真实帖子。
            log.warn("Check post bloom filter failed, postId={}", postId, e);
            return true;
        }
    }

    private String readyKey() {
        return bloomFilterKey + READY_KEY_SUFFIX;
    }

    private long offset(Long postId, int seed) {
        long hash = fnv1a64(postId + ":" + seed);
        return Long.remainderUnsigned(hash, bitSize);
    }

    private long fnv1a64(String value) {
        long hash = 0xcbf29ce484222325L;
        for (byte b : value.getBytes(StandardCharsets.UTF_8)) {
            hash ^= b;
            hash *= 0x100000001b3L;
        }
        return hash;
    }
}
