# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Life-QA (生活问答论坛): a Q&A forum with posts, comments, likes/favorites, notifications, hot ranking, and interest-based recommendations. Split into two halves:

- `forum/` — Spring Boot 3.2.5 / Java 17 multi-module Maven backend (`forum-common`: Result wrapper, BaseContext, exceptions, utils; `forum-pojo`: entity/DTO/VO; `forum-server`: the runnable app — controllers, services, mappers, config, interceptors, messaging).
- `forum-web/` — Vue 3 + Vite SPA with Element Plus, Pinia, vue-router, axios; API clients in `src/api/`, feature views in `src/views/`.

Infrastructure: MySQL (Flyway), Redis (raw `StringRedisTemplate`, no `@Cacheable`), Kafka, optional Elasticsearch, Aliyun OSS. Backend on `:8080` (Knife4j API docs at `/doc.html`), Vite dev server on `:3000` proxying `/api` and `/uploads` to `:8080`.

`AGENTS.md` has additional guidelines; `docs/DATABASE_SCHEMA.md`, `docs/REDIS_CACHE.md`, and `forum/INTERACTION_CONSISTENCY.md` are deep-dive design docs (written in Chinese).

## Commands

All backend commands run from `forum/`. Always pass the project-local Maven repo: `"-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository"` (other paths can fail with "Access denied"). On Windows PowerShell use `mvn.cmd` / `npm.cmd`.

```powershell
# Build all backend modules (runs tests)
mvn.cmd clean package "-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository"

# Run the backend locally (dev profile). -am is REQUIRED: forum-server depends on
# the in-repo forum-common and forum-pojo modules. Do not use -pl forum-server alone.
mvn.cmd -DskipTests "-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository" -pl forum-server -am spring-boot:run

# Run all tests (H2 by default)
mvn.cmd test "-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository" -pl forum-server -am

# Run a single test class / method
mvn.cmd test "-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository" -pl forum-server -am "-Dtest=NotificationOutboxDispatcherTest" -Dsurefire.failIfNoSpecifiedTests=false
```

Frontend, from `forum-web/`:

```powershell
npm.cmd run dev      # Vite dev server on :3000
npm.cmd run build    # production build check
```

Docker (see `DOCKER_DEPLOYMENT.md`): `compose.yaml` runs MySQL/Redis/Kafka + topic init (use `--env-file .env`); `docker-compose.app.yml` adds backend + nginx frontend. Never use `down -v` routinely — it deletes MySQL/Redis/Kafka data.

## Database & Migrations

- Local dev defaults: `forum` db, `root`/`123456` at `localhost:3306` (override via env vars / root `.env`).
- `forum/sql/init.sql`, `notification.sql`, `v3`–`v6` are the legacy baseline (v1–v6), applied manually or by MySQL's `/docker-entrypoint-initdb.d` on first volume creation. Flyway has `baseline-version: 6`.
- Schema changes from V7 on are Flyway migrations in `forum-server/src/main/resources/db/migration` (`V7__add_post_address_and_place_id.sql`, `V8__async_interactions.sql`), applied automatically at startup. **Never edit an applied migration; add a new `V<number>__description.sql`.** `repair_interaction_counts.sql` is a one-off repair script.
- No `FOREIGN KEY` constraints are declared anywhere; ID references are by convention only (see `docs/DATABASE_SCHEMA.md` for the 16 tables).

## Architecture: The Big Picture

### Async interactions (V8) — likes/favorites are final-consistency

The request path (`InteractionController` → `InteractionServiceImpl`) is deliberately tiny: `PUT /api/interactions/{posts|comments}/{id}/{like|favorite}` with `{"active": bool}` writes only the personal relation row plus a persistent `interaction_event` in one short READ COMMITTED transaction (retried up to 3× on unique-key races/transient failures), then returns. It never locks post rows, never touches counters/Redis/Kafka.

Everything derived happens in background workers (`service/distribution/`):
- `InteractionWorker` scans up to 500 events/sec, groups by target, recomputes counts from the real relation tables (absolute writes), and commits counts + notification-outbox events + projection task registrations + event completion in one transaction. Idempotent; batch failure rolls back fully.
- `interaction_projection` tasks (with revisions + completed_revision) drive Redis updates for post-detail invalidation, hot ranking, and interest profiles. Multi-JVM instances coordinate via row locks on `interaction_worker_lock` (counts/projection use separate locks).
- Redis writes are atomic Lua replaces keyed by a version; stale snapshots are rejected, and the read path never writes to Redis. Failed events/projections retry with exponential backoff (max 60s).

Full semantics (content hiding/restoration, orphan cleanup, frontend button-disabling contract, migration notes): `forum/INTERACTION_CONSISTENCY.md`. Tuning lives under `forum.interactions` in `application.yml` (scan delay, batch sizes, cleanup).

### Notification delivery — transactional outbox over Kafka

Business code writes to `notification_outbox_event`; `NotificationOutboxDispatcher` scans and publishes to `forum.notification.events`; `NotificationEventConsumer` creates user-visible `notification` rows. Failures go to the DLT topic and `notification_dead_letter`, replayable via admin endpoints (`AdminNotificationDeliveryController`). The outbox, DLT, and dead-letter tables carry explicit event states — treat notification persistence and delivery as distinct stages.

### Post-detail cache — bloom filter + logical expiry

`PostServiceImpl.getPostDetail` checks the `forum:post:bloom` bitmap (avoids probing missing IDs), then reads `forum:post:detail:{postId}` (JSON with logical expiry, jittered TTL, stale-retain window). Concurrent misses are serialized by a `SET NX` rebuild lock; invalidation rotates the `forum:post:detail:version:{postId}` UUID so in-flight queries can't backfill stale data. Invalidations are broadcast via the `forum.post-cache-invalidation.events` Kafka topic, plus persistent projection tasks as a recovery path. Some legacy like/favorite Redis Sets exist but are no longer read or written by the request path. Key-by-key table: `docs/REDIS_CACHE.md`.

### Distribution — hot ranking & interest recommendation

`service/distribution/` maintains 24h hot rankings (Redis ZSets `forum:distribution:hot:24h:{all|category:{id}}`, hourly decay via a persisted queue, never wholesale rewrites) and per-user interest profiles (`forum:distribution:interest:{tag|category}:{userId}` ZSets, weights: post like 2, favorite 4, comment 3; comment likes add no weight; 90-day window). Profile rebuilds are registered as projection tasks from the async-interaction pipeline, not done synchronously on read.

### Search

`SearchService` has two implementations: `DatabaseSearchServiceImpl` (default, LIKE-based) and `EsSearchServiceImpl`, selected by `ES_SEARCH_ENABLED` (off by default for local dev). Index updates flow through `SearchIndexWriter` → `forum.search-index.events` Kafka topic, so search-index maintenance is also eventual.

### Configuration

- `application.yml` in `forum-server` imports `optional:file:../.env[.properties]` — i.e. the repo-root `.env` is picked up automatically when running from `forum/`. Active profile defaults to `dev` (`application-dev.yml` / `application-prod.yml`).
- MyBatis-Plus with mapper XML at `classpath:mapper/*.xml`; entities auto-aliased from `com.forum.pojo.entity`.
- JWT auth via `forum.auth.jwt` config + interceptor (`interceptor/` package); admin APIs live under `controller/admin/` and are checked against the `user` role.
- All config keys are env-overridable (`forum.cache.*`, `forum.kafka.topics.*`, `forum.distribution.*`, etc.) — see `.env.example`.

### Frontend conventions

`src/utils/request.js` is the axios instance (token header, unified error handling); `src/stores/` holds Pinia stores (user, notification). Feature views in `src/views/`, reusable pieces in `src/components/`. The post-location feature uses `globe.gl`/three and `src/utils/amap.js`; `vite.config.js` manually chunks vendor libs (globe-vendor, editor-vendor, element-vendor, vue-vendor). During like/favorite requests the buttons are disabled client-side and local counts only ever corrected toward the server state.

## Tests

JUnit 5 tests live only in `forum-server/src/test/java`, packaged to mirror the code under test. Tests build their Spring context programmatically (`AnnotationConfigApplicationContext`, no `@SpringBootTest`) and use H2 by default. The interaction/concurrency suite can optionally run against a **dedicated real MySQL** (it creates and drops isolated `interaction_test_*` databases) and a real Redis server — see the command at the end of `forum/INTERACTION_CONSISTENCY.md` (`-Dinteraction.test.url`, `-Dredis.test.server`, `-Dinteraction.test.benchmark=true`). H2 does not substitute for InnoDB locking behavior.

## Code Style

Follow `AGENTS.md`: Java 4-space indent, packages under `com.forum.*`, PascalCase classes, `*DTO`/`*VO`/`*Entity` suffixes; Vue 2-space indent, single quotes, PascalCase component files, lower-case API/util module names. Controllers return the `Result<T>` wrapper from `forum-common`. Never edit `node_modules/` or `**/target/`.
