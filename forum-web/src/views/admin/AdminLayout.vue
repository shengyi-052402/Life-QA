<template>
  <div class="admin-shell">
    <div class="admin-backdrop"></div>
    <div class="admin-panel">
      <aside class="admin-sidebar glass-panel">
        <div class="brand" @click="router.push('/admin')">
          <p class="brand-eyebrow">Console</p>
          <h2>Life-Q&A</h2>
        </div>

        <el-menu
          :default-active="activeMenu"
          class="admin-menu"
          router
        >
          <el-menu-item index="/admin">
            <el-icon><DataAnalysis /></el-icon>
            <span>数据概览</span>
          </el-menu-item>
          <el-menu-item index="/admin/users">
            <el-icon><User /></el-icon>
            <span>用户管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/posts">
            <el-icon><Document /></el-icon>
            <span>帖子管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/comments">
            <el-icon><ChatDotRound /></el-icon>
            <span>评论管理</span>
          </el-menu-item>
          <el-menu-item index="/admin/categories">
            <el-icon><Collection /></el-icon>
            <span>分类管理</span>
          </el-menu-item>
        </el-menu>

        <div class="sidebar-footer">
          <el-button round @click="router.push('/explore')">返回 Life-Q&A</el-button>
        </div>
      </aside>

      <main class="admin-main">
        <header class="admin-header glass-panel">
          <div>
            <p class="page-eyebrow">Admin Center</p>
            <h1>{{ route.meta.title || '管理后台' }}</h1>
          </div>
          <div class="admin-user">
            <el-avatar :src="userStore.userInfo?.avatar" :size="36" />
            <div>
              <strong>{{ userStore.userInfo?.nickname || userStore.userInfo?.username }}</strong>
              <p>管理员</p>
            </div>
          </div>
        </header>

        <section class="admin-content">
          <router-view />
        </section>
      </main>
    </div>
  </div>
</template>

<script setup>
import { computed } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ChatDotRound, Collection, DataAnalysis, Document, User } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const activeMenu = computed(() => {
  if (route.path.startsWith('/admin/users')) return '/admin/users'
  if (route.path.startsWith('/admin/posts')) return '/admin/posts'
  if (route.path.startsWith('/admin/comments')) return '/admin/comments'
  if (route.path.startsWith('/admin/categories')) return '/admin/categories'
  return '/admin'
})
</script>

<style scoped>
.admin-shell {
  min-height: 100vh;
  background:
    radial-gradient(circle at top left, rgba(77, 216, 255, 0.2), transparent 35%),
    radial-gradient(circle at bottom right, rgba(155, 124, 255, 0.18), transparent 30%),
    linear-gradient(180deg, #02040a 0%, #050812 100%);
  padding: 24px;
  position: relative;
}

.admin-shell::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(126, 214, 255, 0.045) 1px, transparent 1px),
    linear-gradient(90deg, rgba(126, 214, 255, 0.045) 1px, transparent 1px);
  background-size: 38px 38px;
}

.admin-panel {
  position: relative;
  z-index: 1;
  max-width: 1480px;
  margin: 0 auto;
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 24px;
}

.admin-sidebar {
  padding: 24px 18px;
  border-radius: 24px;
  display: flex;
  flex-direction: column;
  min-height: calc(100vh - 48px);
}

.brand {
  padding: 8px 10px 18px;
  cursor: pointer;
}

.brand-eyebrow,
.page-eyebrow,
.admin-user p {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.brand h2,
.admin-header h1 {
  font-size: 1.6rem;
  color: var(--text-primary);
  font-family: 'Space Grotesk', 'Inter', sans-serif;
}

.admin-menu {
  border-right: none;
  background: transparent;
  flex: 1;
}

.admin-menu :deep(.el-menu-item) {
  border-radius: var(--radius-md);
  margin-bottom: 8px;
}

.admin-menu :deep(.el-menu-item.is-active) {
  background: rgba(77, 216, 255, 0.12);
  box-shadow: inset 3px 0 0 var(--primary-color), var(--glow-cyan);
}

.admin-main {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.admin-header {
  padding: 20px 24px;
  border-radius: 24px;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.admin-user {
  display: flex;
  align-items: center;
  gap: 12px;
}

.admin-content {
  min-height: calc(100vh - 140px);
}

.sidebar-footer {
  padding-top: 16px;
}

@media (max-width: 960px) {
  .admin-shell {
    padding: 12px;
  }

  .admin-panel {
    grid-template-columns: 1fr;
  }

  .admin-sidebar {
    min-height: auto;
  }

  .admin-header {
    flex-direction: column;
    align-items: flex-start;
    gap: 16px;
  }
}
</style>
