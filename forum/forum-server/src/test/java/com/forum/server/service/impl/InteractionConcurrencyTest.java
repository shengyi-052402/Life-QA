package com.forum.server.service.impl;

import com.baomidou.mybatisplus.core.MybatisConfiguration;
import com.baomidou.mybatisplus.extension.spring.MybatisSqlSessionFactoryBean;
import com.forum.common.context.BaseContext;
import com.forum.common.exception.BaseException;
import com.forum.server.mapper.*;
import com.forum.server.messaging.NotificationEventPublisher;
import com.forum.server.service.InteractionService;
import com.forum.server.service.cache.PostDetailCacheService;
import com.forum.server.service.distribution.*;
import org.apache.ibatis.session.SqlSessionFactory;
import org.junit.jupiter.api.*;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.mybatis.spring.annotation.MapperScan;
import org.springframework.context.annotation.*;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.*;
import org.springframework.jdbc.datasource.init.ResourceDatabasePopulator;
import org.springframework.data.redis.connection.lettuce.LettuceConnectionFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.test.util.AopTestUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.EnableTransactionManagement;
import org.springframework.transaction.support.TransactionTemplate;

import javax.sql.DataSource;
import java.net.ServerSocket;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;
import java.util.concurrent.*;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/** Set interaction.test.url to a private MySQL server URL (without a database) for InnoDB checks. */
class InteractionConcurrencyTest {
    private AnnotationConfigApplicationContext context;
    private JdbcTemplate jdbc;
    private InteractionService first;
    private InteractionService second;
    private InteractionWorker worker;
    private ExecutorService threads;

    @BeforeEach
    void setUp() {
        context = new AnnotationConfigApplicationContext(TestConfiguration.class);
        jdbc = context.getBean(JdbcTemplate.class);
        first = context.getBean("first", InteractionService.class);
        second = context.getBean("second", InteractionService.class);
        worker = context.getBean(InteractionWorker.class);
        threads = Executors.newFixedThreadPool(12);
        jdbc.update("INSERT INTO post(id, user_id, title, view_count) VALUES (1, 999, 'original', 100)");
        jdbc.update("INSERT INTO comment(id, user_id, post_id, content) VALUES (1, 999, 1, 'original')");
    }

    @AfterEach
    void tearDown() throws Exception {
        BaseContext.removeCurrentId();
        if (threads != null) {
            threads.shutdownNow();
            assertTrue(threads.awaitTermination(15, TimeUnit.SECONDS));
        }
        if (context != null) {
            String url = ((DriverManagerDataSource) context.getBean(DataSource.class)).getUrl();
            context.close();
            if (System.getProperty("interaction.test.url") != null) {
                String base = url.substring(0, url.indexOf('?'));
                String schema = base.substring(base.lastIndexOf('/') + 1);
                new JdbcTemplate(TestConfiguration.adminSource()).execute("DROP DATABASE " + schema);
            }
        }
    }

    @Test
    void differentUsersDoNotWriteCountersUntilWorkerRuns() throws Exception {
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (long user = 1; user <= 48; user++) {
            long id = user;
            requests.add(() -> asUser(id, () -> first.setPostLike(1L, true)));
        }
        assertTrue(together(requests).stream().allMatch(Boolean::booleanValue));
        assertEquals(48, count("SELECT COUNT(*) FROM post_like"));
        assertEquals(48, count("SELECT COUNT(*) FROM interaction_event"));
        assertEquals(0, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(0, count("SELECT COUNT(*) FROM notification_outbox_event"));
        worker.processEvents();
        assertEquals(48, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(48, count("SELECT COUNT(*) FROM notification_outbox_event"));
        worker.processEvents();
        assertEquals(48, count("SELECT COUNT(*) FROM notification_outbox_event"));
        together(requests);
        assertEquals(48, count("SELECT COUNT(*) FROM interaction_event"));
        requests.clear();
        for (long user = 1; user <= 48; user++) {
            long id = user;
            requests.add(() -> asUser(id, () -> second.setPostLike(1L, false)));
        }
        assertTrue(together(requests).stream().noneMatch(Boolean::booleanValue));
        worker.processEvents();
        assertEquals(0, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(48, count("SELECT COUNT(*) FROM notification_outbox_event"));
    }

    @Test
    void duplicateDesiredStatesAreIdempotentAcrossReplicas() throws Exception {
        for (boolean active : List.of(true, false)) {
            List<Callable<Boolean>> requests = new ArrayList<>();
            for (int i = 0; i < 24; i++) {
                InteractionService replica = i % 2 == 0 ? first : second;
                requests.add(() -> asUser(7, () -> replica.setPostLike(1L, active)));
            }
            assertTrue(together(requests).stream().allMatch(result -> result == active));
        }
        assertEquals(0, count("SELECT COUNT(*) FROM post_like"));
        assertEquals(2, count("SELECT COUNT(*) FROM interaction_event"));
        worker.processEvents();
        assertEquals(0, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM notification_outbox_event"));
    }

    @Test
    void oppositeStatesConvergeToAuthoritativeRelation() throws Exception {
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            boolean active = i % 2 == 0;
            requests.add(() -> asUser(7, () -> second.setPostLike(1L, active)));
        }
        together(requests);
        worker.processEvents();
        assertEquals(count("SELECT COUNT(*) FROM post_like"), count("SELECT like_count FROM post WHERE id = 1"));
    }

    @Test
    void likesFavoritesCommentLikesAndViewsDoNotOverwriteEachOther() throws Exception {
        List<Callable<Boolean>> requests = new ArrayList<>();
        for (long user = 1; user <= 16; user++) {
            long id = user;
            requests.add(() -> asUser(id, () -> first.setPostLike(1L, true)));
            requests.add(() -> asUser(id, () -> second.setPostFavorite(1L, true)));
            requests.add(() -> asUser(id, () -> first.setCommentLike(1L, true)));
            requests.add(() -> { jdbc.update("UPDATE post SET view_count = view_count + 1 WHERE id = 1"); return true; });
        }
        together(requests);
        worker.processEvents();
        assertEquals(16, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(16, count("SELECT favorite_count FROM post WHERE id = 1"));
        assertEquals(16, count("SELECT like_count FROM comment WHERE id = 1"));
        assertEquals(116, count("SELECT view_count FROM post WHERE id = 1"));
        assertEquals("original", jdbc.queryForObject("SELECT title FROM post WHERE id = 1", String.class));
    }

    @Test
    void requestSucceedsWhileAnotherTransactionHoldsPostExclusiveLock() throws Exception {
        TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
        tx.executeWithoutResult(status -> {
            jdbc.queryForObject("SELECT id FROM post WHERE id = 1 FOR UPDATE", Long.class);
            Future<Boolean> request = threads.submit(() -> asUser(7, () -> first.setPostLike(1L, true)));
            try {
                assertTrue(request.get(3, TimeUnit.SECONDS));
            } catch (Exception ex) {
                throw new AssertionError("Request must not wait on the post row", ex);
            }
        });
    }

    @Test
    void eventFailureRollsBackPersonalRelation() {
        jdbc.execute("ALTER TABLE interaction_event ADD CONSTRAINT reject_user CHECK (user_id <> 7)");
        assertThrows(RuntimeException.class, () -> asUser(7, () -> first.setPostLike(1L, true)));
        assertEquals(0, count("SELECT COUNT(*) FROM post_like"));
        assertEquals(0, count("SELECT COUNT(*) FROM interaction_event"));
    }

    @Test
    void workerFailureRollsBackCountsNotificationsAndCheckpointThenRecovers() throws Exception {
        asUser(7, () -> first.setPostLike(1L, true));
        NotificationEventPublisher publisher = AopTestUtils.getTargetObject(context.getBean(NotificationEventPublisher.class));
        doThrow(new IllegalStateException("simulated outbox outage")).when(publisher)
                .publish(anyLong(), anyLong(), anyString(), anyLong(), isNull(), anyString());
        worker.processEvents();
        assertEquals(0, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM interaction_event WHERE processed_at IS NULL AND attempts = 1"));
        doCallRealMethod().when(publisher).publish(anyLong(), anyLong(), anyString(), anyLong(), isNull(), anyString());
        jdbc.update("UPDATE interaction_event SET next_retry_at = CURRENT_TIMESTAMP(6)");
        // New worker object models restart; recovery does not depend on in-memory selected events.
        newWorker().processEvents();
        assertEquals(1, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM notification_outbox_event"));
    }

    @Test
    void redisFailureLeavesProjectionPendingAndLaterRevisionIsNotLost() throws Exception {
        asUser(7, () -> first.setPostLike(1L, true));
        worker.processEvents();
        PostDetailCacheService cache = context.getBean(PostDetailCacheService.class);
        doThrow(new IllegalStateException("Redis offline")).when(cache).evictStrict(1L);
        worker.processProjections();
        assertEquals(1, count("SELECT COUNT(*) FROM interaction_projection WHERE kind = 'POST' AND pending = TRUE AND attempts = 1"));
        doNothing().when(cache).evictStrict(1L);
        context.getBean(InteractionProjectionQueue.class).post(1L, null);
        worker.processProjections();
        assertEquals(0, count("SELECT COUNT(*) FROM interaction_projection WHERE pending = TRUE"));
        assertEquals(2, count("SELECT completed_revision FROM interaction_projection WHERE kind = 'POST'"));
    }

    @Test
    void enqueueDuringProjectionIsPreservedAfterCompletion() throws Exception {
        context.getBean(InteractionProjectionQueue.class).post(1L, null);
        CountDownLatch entered = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);
        doAnswer(invocation -> {
            entered.countDown();
            assertTrue(release.await(5, TimeUnit.SECONDS));
            return null;
        }).when(context.getBean(PostDetailCacheService.class)).evictStrict(1L);
        Future<?> projecting = threads.submit(worker::processProjections);
        assertTrue(entered.await(5, TimeUnit.SECONDS));
        Future<?> enqueue = threads.submit(() -> context.getBean(InteractionProjectionQueue.class).post(1L, null));
        release.countDown();
        projecting.get(10, TimeUnit.SECONDS);
        enqueue.get(10, TimeUnit.SECONDS);
        worker.processProjections();
        assertEquals(2, count("SELECT completed_revision FROM interaction_projection WHERE kind = 'POST'"));
        assertEquals(0, count("SELECT COUNT(*) FROM interaction_projection WHERE pending = TRUE"));
    }

    @Test
    void twoWorkersCannotDuplicateNotificationOrCount() throws Exception {
        asUser(7, () -> first.setPostLike(1L, true));
        Future<?> a = threads.submit(worker::processEvents);
        Future<?> b = threads.submit(newWorker()::processEvents);
        a.get(10, TimeUnit.SECONDS);
        b.get(10, TimeUnit.SECONDS);
        assertEquals(1, count("SELECT like_count FROM post WHERE id = 1"));
        assertEquals(1, count("SELECT COUNT(*) FROM notification_outbox_event"));
    }

    @Test
    void deletionHidingSelfLikeAndOrphanCleanupDoNotProduceInvalidNotifications() throws Exception {
        asUser(7, () -> first.setPostLike(1L, true));
        asUser(8, () -> first.setCommentLike(1L, true));
        jdbc.update("UPDATE post SET status = 2 WHERE id = 1");
        worker.processEvents();
        assertEquals(0, count("SELECT COUNT(*) FROM notification_outbox_event"));
        assertEquals(1, count("SELECT COUNT(*) FROM post_like"));
        assertThrows(BaseException.class, () -> asUser(9, () -> first.setPostFavorite(1L, true)));
        jdbc.update("UPDATE post SET status = 1 WHERE id = 1");
        asUser(999, () -> first.setPostLike(1L, true));
        worker.processEvents();
        assertEquals(0, count("SELECT COUNT(*) FROM notification_outbox_event"));
        jdbc.update("DELETE FROM post WHERE id = 1");
        worker.cleanOrphans();
        assertEquals(0, count("SELECT COUNT(*) FROM post_like"));
        assertEquals(0, count("SELECT COUNT(*) FROM comment_like"));
        assertTrue(count("SELECT COUNT(*) FROM interaction_projection WHERE kind = 'USER' AND pending = TRUE") >= 3);
    }

    private InteractionWorker newWorker() {
        return new InteractionWorker(jdbc, context.getBean(PlatformTransactionManager.class),
                context.getBean(InteractionProjectionQueue.class), context.getBean(NotificationEventPublisher.class),
                context.getBean(PostDetailCacheService.class), context.getBean(ContentRankingService.class),
                context.getBean(UserInterestService.class));
    }

    @Test
    void databaseEventsReachRealRedisProjectionsWithinLocalTarget() throws Exception {
        Assumptions.assumeTrue(System.getProperty("interaction.test.url") != null
                && System.getProperty("redis.test.server") != null);
        int port;
        try (ServerSocket socket = new ServerSocket(0)) { port = socket.getLocalPort(); }
        Path directory = Files.createTempDirectory("forum-interaction-redis-");
        Process redisProcess = null;
        LettuceConnectionFactory factory = null;
        ScheduledExecutorService scheduling = Executors.newScheduledThreadPool(2);
        try {
            redisProcess = new ProcessBuilder(System.getProperty("redis.test.server"), "--bind", "127.0.0.1",
                    "--port", String.valueOf(port), "--save", "", "--appendonly", "no")
                    .directory(directory.toFile()).redirectOutput(ProcessBuilder.Redirect.DISCARD)
                    .redirectError(ProcessBuilder.Redirect.DISCARD).start();
            factory = new LettuceConnectionFactory("127.0.0.1", port);
            factory.afterPropertiesSet();
            StringRedisTemplate redis = new StringRedisTemplate(factory);
            long readyDeadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(10);
            while (true) {
                try { redis.opsForValue().set("ready", "1"); break; }
                catch (RuntimeException ex) {
                    if (System.nanoTime() > readyDeadline) throw ex;
                    Thread.sleep(20);
                }
            }
            jdbc.update("UPDATE post SET category_id = 3 WHERE id = 1");
            InteractionProjectionQueue queue = context.getBean(InteractionProjectionQueue.class);
            PostMapper posts = context.getBean(PostMapper.class);
            PostDetailCacheService cache = new PostDetailCacheService(redis, new ObjectMapper().findAndRegisterModules());
            ContentRankingService ranking = new ContentRankingService(redis, posts, queue);
            UserInterestService interests = new UserInterestService(redis, posts, context.getBean(PostTagMapper.class),
                    context.getBean(PostLikeMapper.class), context.getBean(FavoriteMapper.class),
                    context.getBean(CommentMapper.class), queue);
            InteractionWorker realWorker = new InteractionWorker(jdbc, context.getBean(PlatformTransactionManager.class),
                    queue, context.getBean(NotificationEventPublisher.class), cache, ranking, interests);
            scheduling.scheduleWithFixedDelay(realWorker::processEvents, 1000, 1000, TimeUnit.MILLISECONDS);
            scheduling.scheduleWithFixedDelay(realWorker::processProjections, 1000, 1000, TimeUnit.MILLISECONDS);
            long started = System.nanoTime();
            List<Callable<Boolean>> actions = new ArrayList<>();
            for (long user = 1; user <= 120; user++) {
                long id = user;
                actions.add(() -> asUser(id, () -> first.setPostLike(1L, true)));
            }
            together(actions);
            long deadline = started + TimeUnit.SECONDS.toNanos(15);
            while (count("SELECT COUNT(*) FROM interaction_event WHERE processed_at IS NULL") > 0
                    || count("SELECT COUNT(*) FROM interaction_projection WHERE pending = TRUE") > 0) {
                assertTrue(System.nanoTime() < deadline, "Durable queues did not drain");
                Thread.sleep(20);
            }
            long elapsedMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);
            assertEquals(120, count("SELECT like_count FROM post WHERE id = 1"));
            assertNotNull(redis.opsForZSet().score("forum:distribution:hot:24h:all", "1"));
            for (long user = 1; user <= 120; user++) {
                assertEquals(2.0, redis.opsForZSet().score("forum:distribution:interest:category:" + user, "3"));
            }
            assertTrue(elapsedMs <= 5000, "Local 120-user propagation target exceeded: " + elapsedMs + " ms");
            System.out.println("INTERACTION_PROPAGATION users=120 threads=12 realMysql=true realRedis=true allProjectionsMs=" + elapsedMs);
        } finally {
            scheduling.shutdownNow();
            assertTrue(scheduling.awaitTermination(10, TimeUnit.SECONDS));
            if (factory != null) {
                try (var connection = factory.getConnection()) { connection.serverCommands().shutdown(); }
                catch (RuntimeException ignored) { }
                factory.destroy();
            }
            if (redisProcess != null) {
                redisProcess.destroy();
                if (!redisProcess.waitFor(5, TimeUnit.SECONDS)) {
                    redisProcess.destroyForcibly();
                    assertTrue(redisProcess.waitFor(5, TimeUnit.SECONDS));
                }
            }
            // No recursive removal: only this test's known files and temporary directory.
            try (var files = Files.list(directory)) {
                for (Path file : files.toList()) Files.deleteIfExists(file);
            }
            for (int attempt = 0; ; attempt++) {
                try { Files.delete(directory); break; }
                catch (java.nio.file.FileSystemException ex) {
                    if (attempt == 49) throw ex;
                    Thread.sleep(20);
                }
            }
        }
    }

    @Test
    @EnabledIfSystemProperty(named = "interaction.test.benchmark", matches = "true")
    void compareSinglePostWritePaths() throws Exception {
        assertNotNull(System.getProperty("interaction.test.url"), "Run the benchmark on private MySQL");
        int requests = 300;
        long oldWaits = mysqlMetric("Innodb_row_lock_waits");
        long oldWaitTime = mysqlMetric("Innodb_row_lock_time");
        long[] baseline = measure(requests, user -> {
            TransactionTemplate tx = new TransactionTemplate(context.getBean(PlatformTransactionManager.class));
            tx.executeWithoutResult(status -> {
                jdbc.queryForObject("SELECT id FROM post WHERE id = 1 FOR UPDATE", Long.class);
                jdbc.update("INSERT INTO post_like(user_id, post_id) VALUES (?, 1)", user);
                jdbc.update("UPDATE post SET like_count = like_count + 1 WHERE id = 1");
                jdbc.update("INSERT INTO interaction_event(event_id, kind, user_id, target_id, post_id, receiver_id, active) "
                        + "VALUES (?, 'post_like', ?, 1, 1, 999, TRUE)", UUID.randomUUID().toString(), user);
            });
            return true;
        });
        long baselineWaits = mysqlMetric("Innodb_row_lock_waits") - oldWaits;
        long baselineWaitTime = mysqlMetric("Innodb_row_lock_time") - oldWaitTime;
        jdbc.update("DELETE FROM post_like");
        jdbc.update("DELETE FROM interaction_event");
        jdbc.update("UPDATE post SET like_count = 0 WHERE id = 1");
        oldWaits = mysqlMetric("Innodb_row_lock_waits");
        oldWaitTime = mysqlMetric("Innodb_row_lock_time");
        long[] redesigned = measure(requests, user -> asUser(user, () -> first.setPostLike(1L, true)));
        long newWaits = mysqlMetric("Innodb_row_lock_waits") - oldWaits;
        long newWaitTime = mysqlMetric("Innodb_row_lock_time") - oldWaitTime;
        long processingStart = System.nanoTime();
        worker.processEvents();
        worker.processProjections();
        long processingMs = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - processingStart);
        assertEquals(requests, count("SELECT like_count FROM post WHERE id = 1"));
        System.out.printf(Locale.ROOT,
                "INTERACTION_BENCHMARK users=%d threads=12 baselineTotalMs=%d baselineP95Ms=%d baselineLockWaits=%d baselineLockWaitMs=%d "
                        + "newTotalMs=%d newP95Ms=%d newLockWaits=%d newLockWaitMs=%d countAndFirstProjectionBatchMs=%d remainingProjections=%d%n",
                requests, baseline[0], baseline[1], baselineWaits, baselineWaitTime, redesigned[0], redesigned[1],
                newWaits, newWaitTime, processingMs, count("SELECT COUNT(*) FROM interaction_projection WHERE pending = TRUE"));
    }

    private long mysqlMetric(String name) {
        return Long.parseLong(jdbc.queryForObject("SHOW GLOBAL STATUS LIKE '" + name + "'", (rs, n) -> rs.getString(2)));
    }

    private long[] measure(int size, Request request) throws Exception {
        List<Long> latencies = Collections.synchronizedList(new ArrayList<>());
        List<Callable<Boolean>> actions = new ArrayList<>();
        for (long i = 1; i <= size; i++) {
            long user = i;
            actions.add(() -> {
                long start = System.nanoTime();
                try { return request.run(user); }
                finally { latencies.add(TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start)); }
            });
        }
        long start = System.nanoTime();
        together(actions);
        long total = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - start);
        latencies.sort(Long::compareTo);
        return new long[] { total, latencies.get((int) Math.ceil(size * 0.95) - 1) };
    }

    @FunctionalInterface
    private interface Request { boolean run(long user) throws Exception; }

    private long count(String sql) { return Objects.requireNonNull(jdbc.queryForObject(sql, Long.class)); }
    private boolean asUser(long id, Callable<Boolean> action) throws Exception {
        BaseContext.setCurrentId(id);
        try { return action.call(); } finally { BaseContext.removeCurrentId(); }
    }
    private List<Boolean> together(List<Callable<Boolean>> requests) throws Exception {
        CountDownLatch start = new CountDownLatch(1);
        List<Future<Boolean>> futures = new ArrayList<>();
        for (Callable<Boolean> request : requests) futures.add(threads.submit(() -> {
            assertTrue(start.await(5, TimeUnit.SECONDS));
            return request.call();
        }));
        start.countDown();
        List<Boolean> results = new ArrayList<>();
        for (Future<Boolean> future : futures) results.add(future.get(20, TimeUnit.SECONDS));
        return results;
    }

    @Configuration
    @EnableTransactionManagement
    @MapperScan(basePackageClasses = PostMapper.class)
    static class TestConfiguration {
        static DriverManagerDataSource adminSource() {
            return new DriverManagerDataSource(System.getProperty("interaction.test.url") +
                    "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
                    System.getProperty("interaction.test.user", "root"), System.getProperty("interaction.test.password", ""));
        }
        @Bean DataSource dataSource() {
            DriverManagerDataSource source;
            if (System.getProperty("interaction.test.url") != null) {
                String schema = "interaction_test_" + UUID.randomUUID().toString().replace("-", "");
                new JdbcTemplate(adminSource()).execute("CREATE DATABASE " + schema);
                source = new DriverManagerDataSource(System.getProperty("interaction.test.url") + schema +
                        "?allowPublicKeyRetrieval=true&useSSL=false&serverTimezone=Asia/Shanghai",
                        System.getProperty("interaction.test.user", "root"), System.getProperty("interaction.test.password", ""));
            } else {
                source = new DriverManagerDataSource("jdbc:h2:mem:interactions-" + UUID.randomUUID()
                        + ";MODE=MySQL;DB_CLOSE_DELAY=-1;LOCK_TIMEOUT=10000", "sa", "");
            }
            new ResourceDatabasePopulator(new ClassPathResource("interaction-schema.sql"),
                    new ClassPathResource("db/migration/V8__async_interactions.sql")).execute(source);
            return source;
        }
        @Bean JdbcTemplate jdbc(DataSource source) { return new JdbcTemplate(source); }
        @Bean SqlSessionFactory sqlSessionFactory(DataSource source) throws Exception {
            MybatisSqlSessionFactoryBean factory = new MybatisSqlSessionFactoryBean();
            factory.setDataSource(source);
            MybatisConfiguration config = new MybatisConfiguration();
            config.setMapUnderscoreToCamelCase(true);
            factory.setConfiguration(config);
            return factory.getObject();
        }
        @Bean PlatformTransactionManager transactionManager(DataSource source) { return new DataSourceTransactionManager(source); }
        @Bean NotificationEventPublisher notification(NotificationOutboxEventMapper mapper) {
            return spy(new NotificationEventPublisher(mapper));
        }
        @Bean ContentRankingService ranking() { return mock(ContentRankingService.class); }
        @Bean UserInterestService interests() { return mock(UserInterestService.class); }
        @Bean PostDetailCacheService cache() { return mock(PostDetailCacheService.class); }
        @Bean InteractionProjectionQueue queue(JdbcTemplate jdbc) { return new InteractionProjectionQueue(jdbc); }
        @Bean("first") InteractionService first(PostMapper posts, CommentMapper comments, JdbcTemplate jdbc,
                                                 PlatformTransactionManager manager) {
            return new InteractionServiceImpl(posts, comments, jdbc, manager);
        }
        @Bean("second") InteractionService second(PostMapper posts, CommentMapper comments, JdbcTemplate jdbc,
                                                   PlatformTransactionManager manager) {
            return new InteractionServiceImpl(posts, comments, jdbc, manager);
        }
        @Bean InteractionWorker worker(JdbcTemplate jdbc, PlatformTransactionManager manager,
                                        InteractionProjectionQueue queue, NotificationEventPublisher notifications,
                                        PostDetailCacheService cache, ContentRankingService ranking, UserInterestService interests) {
            return new InteractionWorker(jdbc, manager, queue, notifications, cache, ranking, interests);
        }
    }
}
