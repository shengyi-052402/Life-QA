package com.forum.server.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.pojo.vo.PostDetailVO;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class PostDetailCacheFailureTest {
    @Test
    @SuppressWarnings("unchecked")
    void versionReadFailureSkipsCacheWriteEvenIfRedisRecovers() {
        StringRedisTemplate redis = mock(StringRedisTemplate.class);
        ValueOperations<String, String> values = mock(ValueOperations.class);
        when(redis.opsForValue()).thenReturn(values);
        when(values.get("forum:post:detail:version:1")).thenThrow(new IllegalStateException("Redis unavailable"));
        PostDetailCacheService cache = new PostDetailCacheService(redis, new ObjectMapper());
        String version = cache.getVersion(1L);
        assertNull(version);
        clearInvocations(redis);
        cache.put(1L, new PostDetailVO(), version);
        verifyNoInteractions(redis);
    }
}
