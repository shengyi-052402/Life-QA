package com.forum.server.service.cache;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.forum.pojo.vo.PostDetailVO;
import com.forum.pojo.entity.*;
import com.forum.server.mapper.*;
import com.forum.server.service.distribution.InteractionProjectionQueue;
import com.forum.server.service.distribution.UserInterestService;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;
import static org.mockito.ArgumentMatchers.any;

/** Runs the actual Lua scripts against a private, non-persistent Redis process on a random local port. */
@EnabledIfSystemProperty(named = "redis.test.server", matches = ".+")
@TestInstance(TestInstance.Lifecycle.PER_CLASS)
class PostDetailCacheConcurrencyTest {
    private Process server;
    private Path workingDirectory;
    private LettuceConnectionFactory connectionFactory;
    private StringRedisTemplate redis;
    private PostDetailCacheService cache;

    @BeforeAll
    void startRedis() throws Exception {
        int port;
        try (ServerSocket socket = new ServerSocket(0)) {
            port = socket.getLocalPort();
        }
        workingDirectory = Files.createTempDirectory("forum-cache-test-");
        server = new ProcessBuilder(System.getProperty("redis.test.server"),
                "--bind", "127.0.0.1", "--port", String.valueOf(port), "--save", "", "--appendonly", "no")
                .directory(workingDirectory.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                .redirectError(ProcessBuilder.Redirect.DISCARD).start();
        connectionFactory = new LettuceConnectionFactory("127.0.0.1", port);
        connectionFactory.afterPropertiesSet();
        redis = new StringRedisTemplate(connectionFactory);
        long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
        while (true) {
            assertTrue(server.isAlive(), "Isolated Redis process exited before startup");
            try {
                redis.opsForValue().set("ready", "1");
                break;
            } catch (Exception e) {
                if (System.nanoTime() >= deadline) {
                    throw e;
                }
                Thread.sleep(20);
            }
        }
        cache = new PostDetailCacheService(redis, new ObjectMapper().findAndRegisterModules());
        ReflectionTestUtils.setField(cache, "postDetailTtlSeconds", 600L);
        ReflectionTestUtils.setField(cache, "postDetailTtlJitterSeconds", 0L);
        ReflectionTestUtils.setField(cache, "postDetailStaleRetainSeconds", 300L);
        ReflectionTestUtils.setField(cache, "postDetailRebuildLockSeconds", 10L);
    }

    @AfterAll
    void stopRedis() throws Exception {
        if (redis != null && server != null && server.isAlive()) {
            // Graceful shutdown also stops the Windows Redis helper process.
            try (var connection = connectionFactory.getConnection()) {
                connection.serverCommands().shutdown();
            } catch (Exception ignored) {
                // A closed connection is expected when Redis handles SHUTDOWN.
            }
        }
        if (connectionFactory != null) {
            connectionFactory.destroy();
        }
        if (server != null) {
            server.destroy();
            if (!server.waitFor(5, TimeUnit.SECONDS)) {
                server.destroyForcibly();
                assertTrue(server.waitFor(5, TimeUnit.SECONDS));
            }
        }
        if (workingDirectory != null) {
            // Persistence is disabled; remove only the known temporary directory if empty.
            try (var files = Files.list(workingDirectory)) {
                for (Path file : files.toList()) {
                    Files.delete(file);
                }
            }
            // Windows may briefly retain process working-directory handles after shutdown.
            for (int attempt = 0; ; attempt++) {
                try {
                    Files.delete(workingDirectory);
                    break;
                } catch (java.nio.file.FileSystemException e) {
                    if (attempt == 49) {
                        throw e;
                    }
                    Thread.sleep(20);
                }
            }
        }
    }

    @Test
    void oldReadCannotRefillCacheAfterCommittedInvalidation() throws Exception {
        Long postId = 1L;
        String oldVersion = cache.getVersion(postId);
        CountDownLatch resumeOldRead = new CountDownLatch(1);
        ExecutorService worker = Executors.newSingleThreadExecutor();
        try {
            Future<?> oldRead = worker.submit(() -> {
                try {
                    assertTrue(resumeOldRead.await(5, TimeUnit.SECONDS));
                    cache.put(postId, detail(0), oldVersion);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    throw new RuntimeException(e);
                }
            });
            cache.evict(postId);
            cache.put(postId, detail(1), cache.getVersion(postId));
            resumeOldRead.countDown();
            oldRead.get(5, TimeUnit.SECONDS);
            assertEquals(1, cache.get(postId).getPostDetail().getLikeCount());
        } finally {
            resumeOldRead.countDown();
            worker.shutdownNow();
            assertTrue(worker.awaitTermination(5, TimeUnit.SECONDS));
        }
    }

    @Test
    void evictionDeletesCachedDetailAndChangesVersionWithoutExpiringFence() {
        Long postId = 2L;
        String oldVersion = cache.getVersion(postId);
        cache.put(postId, detail(0), oldVersion);
        assertTrue(cache.get(postId).hit());
        cache.evict(postId);
        assertFalse(cache.get(postId).hit());
        assertNotEquals(oldVersion, cache.getVersion(postId));
        assertEquals(-1L, redis.getExpire("forum:post:detail:version:" + postId));
        cache.put(postId, detail(0), oldVersion);
        assertFalse(cache.get(postId).hit());
        cache.put(postId, detail(1), cache.getVersion(postId));
        assertTrue(cache.get(postId).hit());
        assertTrue(redis.getExpire("forum:post:detail:" + postId) > 0);
    }

    @Test
    void invalidationDoesNotDeleteAnActiveRebuildLock() {
        assertTrue(cache.tryLockRebuild(3L));
        cache.evict(3L);
        assertFalse(cache.tryLockRebuild(3L));
        cache.unlockRebuild(3L);
        assertTrue(cache.tryLockRebuild(3L));
    }

    private PostDetailVO detail(int likeCount) {
        PostDetailVO detail = new PostDetailVO();
        detail.setLikeCount(likeCount);
        return detail;
    }

    @Test
    void profileSnapshotsAreAtomicIdempotentAndRejectOlderVersions() {
        PostMapper posts = mock(PostMapper.class);
        PostTagMapper tags = mock(PostTagMapper.class);
        PostLikeMapper likes = mock(PostLikeMapper.class);
        FavoriteMapper favorites = mock(FavoriteMapper.class);
        CommentMapper comments = mock(CommentMapper.class);
        when(likes.selectList(any())).thenReturn(List.of(PostLike.builder().userId(70L).postId(10L).build()));
        when(favorites.selectList(any())).thenReturn(List.of(Favorite.builder().userId(70L).postId(10L).build()));
        when(comments.selectList(any())).thenReturn(List.of());
        when(posts.selectList(any())).thenReturn(List.of(Post.builder().id(10L).categoryId(3L).status(1).build()));
        when(tags.selectList(any())).thenReturn(List.of(PostTag.builder().postId(10L).tagId(4L).build()));
        UserInterestService interests = new UserInterestService(redis, posts, tags, likes, favorites, comments,
                mock(InteractionProjectionQueue.class));
        interests.rebuildStrict(70L, 2);
        interests.rebuildStrict(70L, 2);
        assertEquals(6.0, redis.opsForZSet().score("forum:distribution:interest:tag:70", "4"));
        assertEquals(6.0, redis.opsForZSet().score("forum:distribution:interest:category:70", "3"));
        when(likes.selectList(any())).thenReturn(List.of());
        interests.rebuildStrict(70L, 3);
        assertEquals(4.0, redis.opsForZSet().score("forum:distribution:interest:tag:70", "4"));
        when(favorites.selectList(any())).thenReturn(List.of());
        interests.rebuildStrict(70L, 1);
        assertEquals(4.0, redis.opsForZSet().score("forum:distribution:interest:tag:70", "4"));
        interests.rebuildStrict(70L, 4);
        assertFalse(Boolean.TRUE.equals(redis.hasKey("forum:distribution:interest:tag:70")));
        assertTrue(Boolean.TRUE.equals(redis.hasKey("forum:distribution:interest:ready:70")));
        assertEquals(-1L, redis.getExpire("forum:distribution:interest:version:70"));
    }
}
