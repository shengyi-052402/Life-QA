# 全栈社区论坛：项目深度接手文档

> [!IMPORTANT]
> **致接手开发者（Claude Code）**: 这是一个处于高速开发阶段的精美社区项目。为了保持代码的一致性和系统的稳定性，请务必在开发新功能前遵循以下既定架构和流程。

## 1. 架构逻辑与设计模式 (核心契约)

### 1.1 后端响应契约 (Java)
- **统一返回格式**: 必须使用 `com.forum.common.result.Result<T>`。严禁直接返回实体类或 Map。
- **异常处理**: 业务异常请抛出 `BaseException`，由 `GlobalExceptionHandler` 统一捕获。
- **数据传输模型**:
  - `Entity`: 严禁暴露给前端。
  - `DTO`: 用于接收前端 POST/PUT 数据（需配合 `@Validated` 校验）。
  - `VO`: 用于返回前端展示，必须包含所有页面渲染所需的字段。
- **身份上下文**: 通过 `BaseContext` 获取当前登录用户 ID，它是由 `JwtTokenInterceptor` 在校验 Token 后注入的。

### 1.2 前端工程规范 (Vue 3)
- **Store 管理**: 统一使用 `Pinia`。用户信息存储在 `userStore` 中。
- **组件风格**: 逻辑复杂的组件（如 `CommentSection.vue`）采用组合式 API (Setup)。
- **交互规范**: 全局使用 `Element Plus`。点击封面图等核心交互需先检查 `userStore.token`。

---

## 2. 核心业务逻辑说明 (血脉细节)

### 2.1 沉浸式 3D 首页 (Landing.vue)
- **实现原理**: 基于 CSS 3D 透视 + 自研阻尼算法。
- **核心依赖**: 后端 `Post` 对象的 `cover_image` 和 `summary`。
- **视觉契约**: 首页是暗色系、沉浸式、极简风格。任何新加入首页的动效必须遵循“丝滑、无边框、微动效”原则。

### 2.2 评论系统 (B 站模式)
- **结构**: 数据库层是扁平的，通过 `parent_id` 关联。
- **查询逻辑**: 采用递归/层级构建，一级评论带 2 条预览回复，点击展开更多。

### 2.3 互动计数 (数据一致性)
- **注意**: 帖子和评论的点赞/收藏数采用了**冗余字段**（如 `post.like_count`）。
- **操作规则**: 增加点赞记录时，必须**同时同步更新**对应的主表计数，建议使用 MyBatis-Plus 的 `wrapper.setSql("like_count = like_count + 1")` 来保证原子性。

---

## 3. 待办事项与阶段目标 (Roadmap)

### 第一优先级：个人中心 (User Center)
- [ ] **个人资料卡片**: 背景图更换（与帖子封面逻辑一致）、头像上传。
- [ ] **我的动态**: 展示用户发布的帖子、参与的评论。
- [ ] **偏好设置**: 切换明亮/暗黑模式（当前强制暗黑用于 Landing）。

### 第二优先级：内容增强
- [ ] **搜索高亮优化**: 目前是简单的正则替换，后期考虑接入 Elasticsearch。
- [ ] **Markdown 支持**: 当前使用 WangEditor (HTML)，可考虑增加 Markdown 编辑切换。

---

## 4. 关键环境检查点
- **数据库**: `ALTER TABLE post ADD COLUMN cover_image VARCHAR(255) NOT NULL DEFAULT '' AFTER summary;` (若缺失则执行)。
- **存储**: 后端图片存储在 `forum.upload.path` 映射的目录。

---
**接手完毕信号**: 请阅读完毕后，输出一份你对该项目架构的理解简述，并询问我现在想要开始实现哪一个 TODO 任务。

