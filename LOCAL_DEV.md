# Local Development

## Backend

Prepare MySQL locally, then import the SQL scripts in order:

1. `forum/sql/init.sql`
2. `forum/sql/notification.sql`
3. `forum/sql/v3_post_location.sql`
4. `forum/sql/v4_hot_ranking.sql`
5. `forum/sql/v5_interest_recommendation.sql`
6. `forum/sql/v6_notification_delivery.sql`

Scripts 1-6 are the legacy schema baseline. Starting with V7, the backend uses
Flyway migrations from `forum-server/src/main/resources/db/migration` and applies
them automatically at startup. Do not change an already-applied Flyway migration;
add a new `V<number>__description.sql` file for every schema change.

The dev profile defaults to:

- `DB_URL=jdbc:mysql://localhost:3306/forum?useUnicode=true&characterEncoding=utf8&serverTimezone=Asia/Shanghai&useSSL=false&allowPublicKeyRetrieval=true`
- `DB_USERNAME=root`
- `DB_PASSWORD=123456`

Override them with environment variables if your local MySQL uses different credentials.

Start the backend from `forum/`:

```powershell
mvn.cmd -DskipTests "-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository" -pl forum-server -am spring-boot:run
```

Use `-am` so Maven also builds the local `forum-common` and `forum-pojo` modules required by `forum-server`.

For local development, Elasticsearch search is disabled by default. Enable it with:

```powershell
$env:ES_SEARCH_ENABLED='true'
```

## Frontend

Start the frontend from `forum-web/`:

```powershell
npm.cmd run dev
```

Build check:

```powershell
npm.cmd run build
```
