<template>
  <div class="notifications-page">
    <div class="glass-panel notifications-card">
      <div class="page-head">
        <div>
          <h1>通知中心</h1>
          <p>查看评论、回复、点赞、收藏和系统公告。</p>
        </div>
        <el-button :disabled="notificationStore.unreadCount === 0" @click="handleReadAll">
          全部已读
        </el-button>
      </div>

      <div v-if="isAdmin" class="publish-panel">
        <div class="publish-head">
          <h2>发布系统通知</h2>
          <span>管理员公告会推送给全部正常用户。</span>
        </div>
        <el-input
          v-model="publishForm.content"
          type="textarea"
          :rows="4"
          maxlength="255"
          show-word-limit
          placeholder="输入公告内容，例如版本更新、活动通知或维护提醒。"
        />
        <div class="publish-actions">
          <el-button type="primary" :loading="publishing" @click="handlePublish">
            发布公告
          </el-button>
        </div>
      </div>

      <div class="filter-tabs">
        <el-tabs v-model="query.type" @tab-change="handleTypeChange">
          <el-tab-pane
            v-for="item in notificationTypeOptions"
            :key="item.value"
            :label="item.label"
            :name="item.value"
          />
        </el-tabs>
      </div>

      <div v-loading="loading" class="notification-list">
        <template v-if="notifications.length > 0">
          <div
            v-for="item in notifications"
            :key="item.id"
            class="notification-item"
            :class="{ unread: !item.isRead }"
            @click="handleOpen(item)"
          >
            <el-avatar :size="42" :src="item.sender?.avatar || defaultAvatar" />
            <div class="notification-main">
              <div class="notification-top">
                <span class="sender-name">{{ item.sender?.nickname || item.sender?.username || '系统' }}</span>
                <span class="notification-time">{{ formatDate(item.createdAt, true) }}</span>
              </div>
              <div class="notification-text">{{ item.content }}</div>
              <el-tag size="small" :type="item.isRead ? 'info' : 'danger'" effect="plain">
                {{ notificationTypeMap[item.type] || item.type }}
              </el-tag>
            </div>
          </div>
        </template>
        <el-empty v-else description="暂无通知" />
      </div>

      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="query.page"
          v-model:page-size="query.size"
          layout="prev, pager, next"
          :total="total"
          @current-change="fetchNotifications"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCommentLocation } from '@/api/comment'
import { getNotifications, markAllNotificationsRead, markNotificationRead, publishSystemNotification } from '@/api/notification'
import { useNotificationStore } from '@/stores/notification'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const router = useRouter()
const notificationStore = useNotificationStore()
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const publishing = ref(false)
const notifications = ref([])
const total = ref(0)
const query = reactive({
  page: 1,
  size: 10,
  type: ''
})
const publishForm = reactive({
  content: ''
})

const isAdmin = computed(() => userStore.userInfo?.role === 1)

const notificationTypeMap = {
  post_comment: '帖子评论',
  comment_reply: '评论回复',
  post_like: '帖子点赞',
  comment_like: '评论点赞',
  post_favorite: '帖子收藏',
  system_notice: '系统公告'
}

const notificationTypeOptions = [
  { label: '全部', value: '' },
  { label: '帖子评论', value: 'post_comment' },
  { label: '评论回复', value: 'comment_reply' },
  { label: '帖子点赞', value: 'post_like' },
  { label: '评论点赞', value: 'comment_like' },
  { label: '帖子收藏', value: 'post_favorite' },
  { label: '系统公告', value: 'system_notice' }
]

async function fetchNotifications() {
  loading.value = true
  try {
    const res = await getNotifications(query)
    notifications.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

function handleTypeChange() {
  query.page = 1
  fetchNotifications()
}

async function handleOpen(item) {
  if (!item.isRead) {
    await markNotificationRead(item.id)
    item.isRead = true
    notificationStore.setUnreadCount(Math.max(0, notificationStore.unreadCount - 1))
  }

  if (!item.postId) {
    return
  }

  if (item.commentId) {
    const locationRes = await getCommentLocation(item.commentId, {
      topSize: 10,
      replySize: 10
    })
    const location = locationRes.data
    router.push({
      path: `/post/${item.postId}`,
      query: {
        commentId: location.commentId,
        rootCommentId: location.rootCommentId,
        commentPage: location.topPage,
        replyPage: location.replyPage
      }
    })
    return
  }

  router.push(`/post/${item.postId}`)
}

async function handleReadAll() {
  await markAllNotificationsRead()
  notifications.value = notifications.value.map(item => ({ ...item, isRead: true }))
  notificationStore.clear()
  ElMessage.success('已全部标记为已读')
}

async function handlePublish() {
  const content = publishForm.content.trim()
  if (!content) {
    ElMessage.warning('请输入公告内容')
    return
  }

  publishing.value = true
  try {
    await publishSystemNotification({ content })
    publishForm.content = ''
    query.page = 1
    await fetchNotifications()
    await notificationStore.fetchUnreadCount()
    ElMessage.success('系统通知已发布')
  } finally {
    publishing.value = false
  }
}

onMounted(async () => {
  await fetchNotifications()
  await notificationStore.fetchUnreadCount()
})
</script>

<style scoped>
.notifications-page {
  max-width: 900px;
  margin: 0 auto;
}

.notifications-card {
  padding: 30px;
  border-radius: var(--radius-lg);
}

.page-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
  gap: 20px;
  margin-bottom: 24px;
}

.page-head h1 {
  margin: 0;
  font-size: 1.7rem;
}

.page-head p {
  margin: 8px 0 0;
  color: var(--text-secondary);
}

.publish-panel {
  margin-bottom: 24px;
  padding: 20px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  background: rgba(255, 255, 255, 0.04);
}

.publish-head {
  margin-bottom: 12px;
}

.publish-head h2 {
  margin: 0;
  font-size: 1.05rem;
}

.publish-head span {
  display: block;
  margin-top: 6px;
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.publish-actions {
  display: flex;
  justify-content: flex-end;
  margin-top: 12px;
}

.filter-tabs {
  margin-bottom: 8px;
}

.filter-tabs :deep(.el-tabs__header) {
  margin-bottom: 0;
}

.notification-item {
  display: flex;
  gap: 14px;
  padding: 18px 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
}

.notification-item.unread {
  background: rgba(245, 158, 11, 0.05);
}

.notification-main {
  flex: 1;
}

.notification-top {
  display: flex;
  justify-content: space-between;
  gap: 12px;
}

.sender-name {
  font-weight: 600;
}

.notification-time {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.notification-text {
  margin: 8px 0 10px;
  color: var(--text-secondary);
  line-height: 1.6;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
