# 当前状态

## 已完成
- 个人中心：已支持“我的帖子 / 我的收藏 / 我的动态”展示，收藏仅本人可见。
- 设置页：已支持头像上传、昵称修改、个人简介修改。
- 通知中心：已支持评论、回复、帖子点赞、评论点赞、帖子收藏、系统通知。
- 通知跳转：评论/回复类通知支持跳转到帖子详情中的对应评论位置。
- 统计修复：已修复删帖后首页分类计数和用户主页帖子/收藏数据回显异常。
- 管理后台：已支持数据概览、用户管理、帖子管理、评论管理、分类管理。
- 搜索：已支持独立搜索接口、搜索建议、Elasticsearch 可选接入与索引同步。
- 部署：已补生产配置、环境变量样例、Docker Compose 与 Nginx 配置。
- Token 刷新：已支持后端刷新接口与前端请求前自动续期。

## 当前可用账号
- 管理员：`admin / 123456`
- 普通用户：通过注册页自行创建测试账号

## 当前优先级
- 当前前三个开发优先级已完成
- 下一步建议：性能优化、生产环境联调、关键链路回归测试

## 约束
- 不要动 `forum-web/src/views/Landing.vue` 的 3D 核心交互。
- 所有接口出参必须封装在 `Result<T>` 中。
- 前端日期展示统一使用 `formatDate`。
- 数据库计数字段只能通过 Service 层同步维护，不能直接在 Mapper 层单独改冗余计数。

## 环境
- JDK：`17.0.12`
- Node：`v24.13.0`
- npm：`11.12.1`
- MySQL：`8.0.45`
- Maven：`3.6+`
- 图片上传目录：`D:/ShengYi/QA/uploads/`

## 数据库说明
- 基准 SQL：`forum/sql/init.sql`
- 通知表补充脚本：`forum/sql/notification.sql`
- 帖子定位补充脚本：`forum/sql/v3_post_location.sql`

## 搜索说明
- 默认使用数据库搜索，确保无 ES 时也可运行。
- 设置 `FORUM_SEARCH_ES_ENABLED=true` 后启用 Elasticsearch。
- 历史帖子可在后台首页点击“重建搜索索引”全量导入。

## 生产配置
- 后端生产配置：`forum/forum-server/src/main/resources/application-prod.yml`
- 环境变量模板：`.env.example`
- Docker Compose 示例：`deploy/docker-compose.prod.yml`
- Nginx 配置：`deploy/nginx/forum.conf`
- 部署文档：`docs/部署说明.md`

## 已知风险
- WangEditor 图片回显依赖 Vite 代理，部署环境变化时容易失效。
- `Landing.vue` 使用 `preserve-3d`，低性能机器可能掉帧，暂未做专项优化。
- 搜索分页高亮在大数据量下未做压测。
- 评论删除级联在复杂场景下建议继续回归验证。

## 下次接手建议
下次继续开发时，优先读取以下文件：

- `docs/需求文档.md`
- `docs/开发计划.md`
- `docs/current_status.md`
- `docs/项目功能完成度清单.md`
