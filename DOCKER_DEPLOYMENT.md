# Life-QA Docker 部署

项目采用两层 Compose：

- `compose.yaml`：MySQL、Redis、Kafka 和 Kafka Topic 初始化。
- `docker-compose.app.yml`：Spring Boot 后端与 Vue/Nginx 前端。

## 1. 准备配置

```powershell
cd D:\ShengYi\QA-PLUS
Copy-Item .env.example .env
notepad .env
```

至少修改 `MYSQL_ROOT_PASSWORD`、`MYSQL_PASSWORD`、`DB_PASSWORD` 和 `JWT_SECRET`。`MYSQL_PASSWORD` 与 `DB_PASSWORD` 应保持一致。

## 2. 只启动中间件

```powershell
docker compose --env-file .env config --quiet
docker compose --env-file .env up -d
docker compose --env-file .env ps
```

`kafka-init` 最终显示 `Exited (0)` 是正常状态，表示 Topic 已创建完成。

## 3. 本地运行后端

`application.yml` 会自动读取仓库根目录的 `.env`：

```powershell
cd D:\ShengYi\QA-PLUS\forum
mvn.cmd clean package "-Dmaven.repo.local=D:/ShengYi/QA-PLUS/.m2/repository"
java -jar .\forum-server\target\forum-server-1.0-SNAPSHOT.jar
```

## 4. 完整容器部署

先检查合并后的配置：

```powershell
cd D:\ShengYi\QA-PLUS
docker compose --env-file .env -f compose.yaml -f docker-compose.app.yml config --quiet
```

构建并启动：

```powershell
docker compose --env-file .env -f compose.yaml -f docker-compose.app.yml up -d --build
```

查看状态和日志：

```powershell
docker compose --env-file .env -f compose.yaml -f docker-compose.app.yml ps
docker compose --env-file .env -f compose.yaml -f docker-compose.app.yml logs --tail 100
```

访问地址：

- 前端：<http://localhost:3000>
- 后端：<http://localhost:8080>
- 接口文档：<http://localhost:8080/doc.html>

## 5. 停止

保留数据卷：

```powershell
docker compose --env-file .env -f compose.yaml -f docker-compose.app.yml down
```

不要日常使用 `down -v`；它会删除 MySQL、Redis 和 Kafka 数据。

## 6. 数据库初始化边界

`forum/sql` 下的 1-6 号 SQL 是旧结构基线，通过 MySQL 官方镜像的
`/docker-entrypoint-initdb.d` 执行，只会在全新 MySQL 数据卷首次创建时运行。
V7 及后续结构升级由后端启动时的 Flyway 自动执行，迁移文件位于
`forum/forum-server/src/main/resources/db/migration`。每次结构变更必须新增迁移版本，
不要修改已经执行过的迁移，也不要通过删除数据卷重复初始化生产数据。
