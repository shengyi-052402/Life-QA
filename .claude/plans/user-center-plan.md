# 用户个人中心开发计划

## 目标
实现个人主页、个人设置、收藏列表功能

## 后端接口开发 (UserController)

### 1. GET `/api/users/{id}` - 获取用户公开信息
- 返回 UserVO (id, nickname, avatar, bio, postCount, createdAt)
- 已有 UserService.getUserProfile()，需调整路由

### 2. GET `/api/users/{id}/posts` - 获取用户帖子列表
- 分页查询，返回 PageResult<PostListVO>
- 新增 UserService.getUserPosts(Long userId, int page, int size)

### 3. GET `/api/users/me/favorites` - 获取我的收藏列表
- 仅本人可访问
- 分页查询，返回 PageResult<PostListVO>
- 新增 UserService.getMyFavorites(int page, int size)

### 4. PUT `/api/users/profile` - 修改个人资料
- 接收 UserUpdateDTO (nickname, bio)
- 新增 UserService.updateProfile(UserUpdateDTO dto)

### 5. PUT `/api/users/password` - 修改密码
- 接收 PasswordUpdateDTO (oldPassword, newPassword)
- 验证旧密码后更新
- 新增 UserService.updatePassword(PasswordUpdateDTO dto)

### 6. PUT `/api/users/avatar` - 更新头像
- 接收 avatar URL (已通过文件上传获得)
- 新增 UserService.updateAvatar(String avatarUrl)

## 前端页面开发

### 1. UserProfile.vue (`/user/:id`)
- 展示用户信息卡片
- 用户帖子列表 (带分页)
- 如果是本人，显示"我的收藏"标签页

### 2. UserSettings.vue (`/settings`)
- 修改昵称、简介表单
- 修改密码表单
- 头像上传组件

### 3. API 模块
- 新增 forum-web/src/api/user.js

### 4. 路由更新
- 添加 /user/:id 和 /settings 路由

## 实现顺序

1. 后端: UserController 新增接口
2. 后端: UserService/UserServiceImpl 新增方法
3. 前端: api/user.js
4. 前端: UserProfile.vue
5. 前端: UserSettings.vue
6. 前端: 路由配置