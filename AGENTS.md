# Repository Guidelines

## Project Structure & Module Organization
This repository is split into a Spring Boot backend in `forum/` and a Vue 3 frontend in `forum-web/`. Backend modules are `forum-common/` for shared utilities and result wrappers, `forum-pojo/` for DTO/entity/VO classes, and `forum-server/` for controllers, services, mappers, config, and interceptors. Frontend source lives in `forum-web/src/` with feature views in `views/`, reusable UI in `components/`, API clients in `api/`, routing in `router/`, and global styles in `assets/styles/`. Supporting docs are in `docs/`, SQL bootstrap scripts in `forum/sql/`, and uploaded files in `uploads/`.

## Build, Test, and Development Commands
Use `cd forum-web && npm install` once to install frontend dependencies.

- `cd forum-web && npm run dev`: start the Vite dev server.
- `cd forum-web && npm run build`: create a production frontend build.
- `cd forum-web && npm run preview`: preview the built frontend locally.
- `cd forum && mvn clean package`: build all backend modules and run Maven tests.
- `cd forum && mvn -pl forum-server spring-boot:run`: launch the API on port `8080`.

## Coding Style & Naming Conventions
Follow the existing style in each stack. Vue files use 2-space indentation, single quotes, and PascalCase component filenames such as `PostDetail.vue`; keep API/helper modules in lower-case names like `post.js` and `request.js`. Java uses 4-space indentation, package names under `com.forum.*`, PascalCase class names, and suffix-based DTO/VO/entity naming such as `UserRegisterDTO` and `PostDetailVO`. Do not edit generated directories such as `forum-web/node_modules/` or `**/target/`.

## Testing Guidelines
There is no committed test suite yet. For backend changes, add JUnit tests under `forum/*/src/test/java` and run them with `mvn test` or `mvn clean package`. For frontend changes, at minimum verify the affected flow with `npm run build` and a manual check in `npm run dev`. Name Java tests `*Test.java` and keep test classes aligned with the package of the code under test.

## Commit & Pull Request Guidelines
Current history uses short subject lines (`初始化`, `初始化项目`); keep commits concise, imperative, and focused on one change. Pull requests should include a brief summary, affected areas (`forum-server`, `forum-web`, SQL/docs), setup or migration notes when relevant, and screenshots for UI changes. Link the related issue or requirement document when one exists.

## Security & Configuration Tips
Backend settings live in `forum/forum-server/src/main/resources/application.yml`. Replace local credentials before sharing branches, keep secrets out of Git, and validate upload-path changes carefully because files are written to `uploads/`.
