# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

This is a full-stack forum website project with:
- **Backend**: Spring Boot 3 + MyBatis-Plus + Spring Security + JWT (Java 17)
- **Frontend**: Vue 3 + Vite + Element Plus + Pinia + WangEditor 5

Target: ~1000 users, responsive design for PC and mobile browsers.

## Commands

### Backend (Maven multi-module)

```bash
# Build all modules
cd forum && mvn clean install

# Run the server (from forum directory)
mvn -pl forum-server spring-boot:run

# Or run the packaged jar
java -jar forum/forum-server/target/forum-server-1.0-SNAPSHOT.jar

# Run tests
mvn test

# Build single module
mvn -pl forum-common clean install
```

Backend runs on `http://localhost:8080`

### Frontend (Vue 3 + Vite)

```bash
# Install dependencies
cd forum-web && npm install

# Development server
npm run dev

# Build for production
npm run build

# Preview production build
npm run preview
```

Frontend runs on `http://localhost:3000` with proxy to backend on `/api` and `/uploads`.

### Database

MySQL 8.0, database name: `forum`. Connection configured in `forum/forum-server/src/main/resources/application.yml`.

### API Documentation

Knife4j (Swagger) available at `http://localhost:8080/doc.html` when server is running.

## Architecture

### Backend Module Structure

Multi-module Maven project with dependency chain: `forum-server` → `forum-pojo` → `forum-common`

- **forum-common**: Shared utilities, constants, exceptions, Result wrapper, JWT utility, ThreadLocal context
- **forum-pojo**: Entity classes (DB tables), DTOs (request params), VOs (response objects)
- **forum-server**: Main Spring Boot application with controllers, services, mappers, configs

Key packages in forum-server:
- `config/`: SecurityConfig, MyBatisPlusConfig
- `controller/`: REST endpoints (AuthController, UserController, PostController, CategoryController, TagController, FileController)
- `controller/admin/`: Admin endpoints (AdminCategoryController)
- `service/` + `service/impl/`: Business logic layer
- `mapper/`: MyBatis-Plus mapper interfaces
- `handler/`: GlobalExceptionHandler for unified error responses

### Frontend Structure

- `src/api/`: Axios-based API modules (auth.js, category.js, tag.js, post.js)
- `src/stores/`: Pinia stores (user.js for auth state)
- `src/views/`: Page components (Login.vue, Register.vue, Home.vue)
- `src/utils/`: request.js (Axios interceptors), auth.js (token management)

### Authentication Flow

JWT token-based authentication:
- Login returns JWT token stored in localStorage
- Token attached via Authorization header: `Bearer <token>`
- Current user ID stored in ThreadLocal (BaseContext) via interceptor

### API Response Format

All endpoints return unified format:
```json
{
  "code": 200,
  "message": "success",
  "data": { ... }
}
```

Codes: 200=success, 400=bad request, 401=unauthorized, 403=forbidden, 404=not found, 500=server error

### Data Models (Key Entities)

- **User**: username, email, password (BCrypt), nickname, avatar, bio, role (0=user, 1=admin), status
- **Post**: title, content (HTML), summary, user_id, category_id, view/like/comment/favorite counts, is_top, is_essence, status
- **Comment**: content, user_id, post_id, parent_id (NULL for top-level), reply_to_user_id (for nested replies), like_count
- **Category**: name, description, icon, sort_order, post_count
- **Tag**: name, post_count
- **PostTag**: post_id, tag_id (many-to-many)

Comments use B站风格 two-level structure: parent_id=NULL = top-level comment; replies have parent_id pointing to the top-level comment.

## Development Notes

- File upload stored locally at `D:/ShengYi/QA/uploads/` (configurable in application.yml under `forum.upload.path`)
- Elasticsearch 8.x integration planned for full-text search (not yet implemented)
- WangEditor used for rich text post content
- MyBatis-Plus handles CRUD with auto-generated SQL; custom queries in `mapper/*.xml`