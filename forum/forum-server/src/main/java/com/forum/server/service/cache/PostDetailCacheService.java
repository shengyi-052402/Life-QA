package com.forum.server.service.cache;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.pojo.vo.PostDetailVO;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.Optional;
import java.util.concurrent.ThreadLocalRandom;

@Slf4j
@Service
@RequiredArgsConstructor
public class PostDetailCacheService {
    private static final String POST_DETAIL_KEY_PREFIX = "forum:post:detail:";

    private final StringRedisTemplate stringRedisTemplate;
    private final ObjectMapper objectMapper;

    @Value("${forum.cache.post-detail-ttl-seconds:600}")
    private long postDetailTtlSeconds;

    @Value("${forum.cache.post-detail-ttl-jitter-seconds:120}")
    private long postDetailTtlJitterSeconds;

    public Optional<PostDetailVO> get(Long postId) {
        try {
            String json = stringRedisTemplate.opsForValue().get(buildKey(postId));
            if (json == null || json.isBlank()) {
                return Optional.empty();
            }
            return Optional.of(objectMapper.readValue(json, PostDetailVO.class));
        } catch (Exception e) {
            log.warn("Read post detail cache failed, postId={}", postId, e);
            return Optional.empty();
        }
    }

    public void put(Long postId, PostDetailVO postDetail) {
        try {
            stringRedisTemplate.opsForValue().set(
                    buildKey(postId),
                    objectMapper.writeValueAsString(postDetail),
                    buildTtl()
            );
        } catch (JsonProcessingException e) {
            log.warn("Serialize post detail cache failed, postId={}", postId, e);
        } catch (Exception e) {
            log.warn("Write post detail cache failed, postId={}", postId, e);
        }
    }

    public void evict(Long postId) {
        try {
            stringRedisTemplate.delete(buildKey(postId));
        } catch (Exception e) {
            log.warn("Evict post detail cache failed, postId={}", postId, e);
        }
    }

    private Duration buildTtl() {
        long jitter = postDetailTtlJitterSeconds <= 0
                ? 0
                : ThreadLocalRandom.current().nextLong(postDetailTtlJitterSeconds + 1);
        return Duration.ofSeconds(postDetailTtlSeconds + jitter);
    }

    private String buildKey(Long postId) {
        return POST_DETAIL_KEY_PREFIX + postId;
    }
}
