<template>
  <el-container class="layout-container">
    <el-header class="glass-panel main-header">
      <div class="header-content">
        <div class="logo" @click="$router.push('/')">
          <span class="text-gradient">开发者论坛</span>
        </div>

        <div class="header-right">
          <el-input
            v-model="searchKeyword"
            placeholder="搜索帖子..."
            class="search-input"
            :prefix-icon="Search"
            @keyup.enter="handleSearch"
            round
          />

          <template v-if="userStore.token">
            <el-badge :value="notificationStore.unreadCount" :hidden="notificationStore.unreadCount === 0" class="notification-badge">
              <div class="notification-entry" @click="router.push('/notifications')">
                <el-icon><Bell /></el-icon>
              </div>
            </el-badge>

            <el-dropdown trigger="click" @command="handleCommand">
              <div class="user-profile">
                <el-avatar :size="32" :src="userStore.userInfo?.avatar || defaultAvatar" />
                <span class="username">{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</span>
              </div>
              <template #dropdown>
                <el-dropdown-menu>
                  <el-dropdown-item command="profile">个人主页</el-dropdown-item>
                  <el-dropdown-item command="notifications">通知中心</el-dropdown-item>
                  <el-dropdown-item command="settings">账号设置</el-dropdown-item>
                  <el-dropdown-item divided command="logout">退出登录</el-dropdown-item>
                </el-dropdown-menu>
              </template>
            </el-dropdown>
          </template>

          <template v-else>
            <el-button link @click="$router.push('/login')">登录</el-button>
            <el-button type="primary" round @click="$router.push('/register')">注册</el-button>
          </template>
        </div>
      </div>
    </el-header>

    <el-main class="main-content">
      <router-view />
    </el-main>

    <el-footer class="main-footer">
      <p>&copy; 2026 开发者论坛 - By Vue 3 & Spring Boot 3</p>
    </el-footer>
  </el-container>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { Bell, Search } from '@element-plus/icons-vue'
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const notificationStore = useNotificationStore()
const searchKeyword = ref('')
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

onMounted(async () => {
  if (userStore.token && !userStore.userInfo) {
    await userStore.fetchUserInfo()
  }
  if (userStore.token) {
    await notificationStore.fetchUnreadCount()
  }
})

function handleCommand(command) {
  if (command === 'logout') {
    userStore.logout()
    router.push('/login')
  } else if (command === 'settings') {
    router.push('/settings')
  } else if (command === 'profile') {
    router.push(`/user/${userStore.userInfo?.id}`)
  } else if (command === 'notifications') {
    router.push('/notifications')
  }
}

function handleSearch() {
  if (searchKeyword.value.trim()) {
    router.push({ path: '/search', query: { q: searchKeyword.value.trim() } })
  }
}
</script>

<style scoped>
.layout-container {
  min-height: 100vh;
}

.main-header {
  position: sticky;
  top: 0;
  z-index: 100;
  border-bottom: 1px solid var(--border-color);
  padding: 0;
}

.header-content {
  max-width: 1200px;
  margin: 0 auto;
  height: 60px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 20px;
}

.logo {
  cursor: pointer;
  font-weight: 700;
  font-size: 1.25rem;
}

.header-right {
  display: flex;
  align-items: center;
  gap: 18px;
}

.search-input {
  width: 250px;
}

.notification-badge :deep(.el-badge__content) {
  top: 8px;
}

.notification-entry {
  width: 36px;
  height: 36px;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
  cursor: pointer;
  background: rgba(255, 255, 255, 0.08);
  transition: var(--transition);
}

.notification-entry:hover {
  background: var(--border-color);
}

.user-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 4px 8px;
  border-radius: var(--radius-md);
  transition: var(--transition);
}

.user-profile:hover {
  background: var(--border-color);
}

.username {
  font-size: 0.9rem;
  font-weight: 500;
}

.main-content {
  max-width: 1200px;
  margin: 0 auto;
  width: 100%;
  padding: 20px;
  min-height: calc(100vh - 120px);
}

.main-footer {
  text-align: center;
  padding: 20px;
  color: var(--text-secondary);
  font-size: 0.9rem;
  border-top: 1px solid var(--border-color);
}
</style>
