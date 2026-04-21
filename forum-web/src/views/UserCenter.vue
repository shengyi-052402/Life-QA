<template>
  <div class="user-center">
    <div class="glass-panel hero-card" v-loading="profileLoading">
      <div class="hero-cover"></div>
      <div class="hero-content" v-if="profile">
        <el-avatar
          :size="88"
          :src="profile.avatar || defaultAvatar"
          class="hero-avatar"
        />
        <div class="hero-meta">
          <div class="hero-name-row">
            <h1>{{ profile.nickname || profile.username }}</h1>
            <el-tag v-if="isSelf" type="primary" round>My Space</el-tag>
          </div>
          <p class="hero-handle">@{{ profile.username }}</p>
          <p class="hero-bio">{{ profile.bio || '这个人还没有留下简介。' }}</p>
          <div class="hero-stats">
            <div class="stat-chip">
              <strong>{{ profile.postCount || 0 }}</strong>
              <span>帖子</span>
            </div>
            <div class="stat-chip">
              <strong>{{ activityTotal }}</strong>
              <span>动态</span>
            </div>
            <div class="stat-chip">
              <strong>{{ favoriteTotal }}</strong>
              <span>收藏</span>
            </div>
            <div class="stat-chip">
              <strong>{{ formatDate(profile.createdAt) }}</strong>
              <span>加入时间</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="glass-panel content-card">
      <el-tabs v-model="activeTab" class="user-tabs">
        <el-tab-pane label="我的帖子" name="posts" />
        <el-tab-pane v-if="isSelf" label="我的收藏" name="favorites" />
        <el-tab-pane label="我的动态" name="activities" />
      </el-tabs>

      <div v-loading="tabLoading" class="tab-content">
        <template v-if="activeTab === 'activities'">
          <template v-if="activities.length > 0">
            <div
              v-for="item in activities"
              :key="`${item.type}-${item.targetId}`"
              class="activity-item"
            >
              <div class="activity-dot" :class="item.type"></div>
              <div class="activity-main">
                <div class="activity-head">
                  <span class="activity-type">{{ item.type === 'post' ? '发布了帖子' : '参与了评论' }}</span>
                  <span class="activity-time">{{ formatDate(item.createdAt, true) }}</span>
                </div>
                <template v-if="item.type === 'post'">
                  <h3 class="activity-title" @click="goPost(item.postId)">{{ item.title }}</h3>
                  <p class="activity-summary">{{ item.summary || '暂无摘要' }}</p>
                </template>
                <template v-else>
                  <div class="activity-link" @click="goPost(item.postId)">{{ item.postTitle }}</div>
                  <p class="activity-summary">{{ item.content || '暂无评论内容' }}</p>
                </template>
              </div>
            </div>
          </template>
          <el-empty v-else description="还没有动态内容" />
        </template>

        <template v-else>
          <template v-if="currentList.length > 0">
            <div
              v-for="post in currentList"
              :key="post.id"
              class="post-card"
              @click="goPost(post.id)"
            >
              <div class="post-card-cover">
                <img v-if="post.coverImage" :src="post.coverImage" :alt="post.title" />
                <div v-else class="cover-fallback">NO COVER</div>
              </div>
              <div class="post-card-main">
                <div class="post-card-head">
                  <h3>{{ post.title }}</h3>
                  <span class="post-card-time">{{ formatDate(post.createdAt) }}</span>
                </div>
                <p class="post-card-summary">{{ post.summary || '暂无摘要' }}</p>
                <div class="post-card-meta">
                  <span>{{ post.categoryName || '未分类' }}</span>
                  <span>阅读 {{ post.viewCount || 0 }}</span>
                  <span>评论 {{ post.commentCount || 0 }}</span>
                  <span>点赞 {{ post.likeCount || 0 }}</span>
                </div>
              </div>
            </div>
          </template>
          <el-empty v-else :description="activeTab === 'favorites' ? '还没有收藏内容' : '还没有发布帖子'" />
        </template>
      </div>

      <div class="pagination-wrap" v-if="currentTotal > 0">
        <el-pagination
          v-model:current-page="currentQuery.page"
          v-model:page-size="currentQuery.size"
          layout="prev, pager, next"
          :total="currentTotal"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getMyFavorites, getUserActivities, getUserPosts, getUserProfile } from '@/api/user'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const activeTab = ref('posts')
const profileLoading = ref(false)
const tabLoading = ref(false)

const profile = ref(null)
const posts = ref([])
const favorites = ref([])
const activities = ref([])

const postsTotal = ref(0)
const favoriteTotal = ref(0)
const activityTotal = ref(0)

const postQuery = ref({ page: 1, size: 10 })
const favoriteQuery = ref({ page: 1, size: 10 })
const activityQuery = ref({ page: 1, size: 10 })

const userId = computed(() => Number(route.params.id))
const isSelf = computed(() => userStore.userInfo && userStore.userInfo.id === userId.value)

const currentList = computed(() => {
  if (activeTab.value === 'favorites') return favorites.value
  return posts.value
})

const currentTotal = computed(() => {
  if (activeTab.value === 'favorites') return favoriteTotal.value
  if (activeTab.value === 'activities') return activityTotal.value
  return postsTotal.value
})

const currentQuery = computed(() => {
  if (activeTab.value === 'favorites') return favoriteQuery.value
  if (activeTab.value === 'activities') return activityQuery.value
  return postQuery.value
})

async function fetchProfile() {
  profileLoading.value = true
  try {
    const res = await getUserProfile(userId.value)
    profile.value = res.data
  } finally {
    profileLoading.value = false
  }
}

async function fetchPosts() {
  tabLoading.value = true
  try {
    const res = await getUserPosts(userId.value, postQuery.value)
    posts.value = res.data.records
    postsTotal.value = res.data.total
  } finally {
    tabLoading.value = false
  }
}

async function fetchFavorites() {
  if (!isSelf.value) return
  tabLoading.value = true
  try {
    const res = await getMyFavorites(favoriteQuery.value)
    favorites.value = res.data.records
    favoriteTotal.value = res.data.total
  } finally {
    tabLoading.value = false
  }
}

async function fetchActivities() {
  tabLoading.value = true
  try {
    const res = await getUserActivities(userId.value, activityQuery.value)
    activities.value = res.data.records
    activityTotal.value = res.data.total
  } finally {
    tabLoading.value = false
  }
}

async function loadActiveTab() {
  if (activeTab.value === 'favorites') {
    await fetchFavorites()
    return
  }
  if (activeTab.value === 'activities') {
    await fetchActivities()
    return
  }
  await fetchPosts()
}

function resetQueries() {
  postQuery.value.page = 1
  favoriteQuery.value.page = 1
  activityQuery.value.page = 1
}

function handlePageChange(page) {
  currentQuery.value.page = page
  loadActiveTab()
}

function goPost(postId) {
  router.push(`/post/${postId}`)
}

watch(
  () => route.params.id,
  async () => {
    resetQueries()
    activeTab.value = 'posts'
    await fetchProfile()
    await fetchPosts()
  },
  { immediate: true }
)

watch(activeTab, () => {
  loadActiveTab()
})
</script>

<style scoped>
.user-center {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.hero-card {
  position: relative;
  overflow: hidden;
  border-radius: var(--radius-lg);
}

.hero-cover {
  height: 160px;
  background:
    radial-gradient(circle at top left, rgba(255, 179, 71, 0.35), transparent 40%),
    linear-gradient(135deg, rgba(16, 24, 40, 0.92), rgba(28, 44, 77, 0.88));
}

.hero-content {
  position: relative;
  display: flex;
  gap: 24px;
  padding: 0 28px 28px;
  margin-top: -44px;
}

.hero-avatar {
  border: 4px solid rgba(255, 255, 255, 0.92);
  box-shadow: 0 16px 40px rgba(15, 23, 42, 0.2);
}

.hero-meta {
  flex: 1;
  padding-top: 48px;
}

.hero-name-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.hero-name-row h1 {
  margin: 0;
  font-size: 1.9rem;
}

.hero-handle {
  margin: 6px 0 0;
  color: var(--text-secondary);
}

.hero-bio {
  margin: 16px 0 0;
  max-width: 720px;
  line-height: 1.7;
  color: var(--text-secondary);
}

.hero-stats {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  margin-top: 20px;
}

.stat-chip {
  min-width: 112px;
  padding: 12px 14px;
  border-radius: 16px;
  background: rgba(255, 255, 255, 0.58);
  backdrop-filter: blur(12px);
}

.stat-chip strong {
  display: block;
  font-size: 1rem;
}

.stat-chip span {
  display: block;
  margin-top: 6px;
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.content-card {
  padding: 24px;
  border-radius: var(--radius-lg);
}

.tab-content {
  min-height: 420px;
}

.post-card {
  display: grid;
  grid-template-columns: 180px minmax(0, 1fr);
  gap: 18px;
  padding: 16px 0;
  cursor: pointer;
  border-bottom: 1px solid var(--border-color);
}

.post-card:first-child {
  padding-top: 8px;
}

.post-card-cover {
  height: 116px;
  overflow: hidden;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(30, 64, 175, 0.18), rgba(249, 115, 22, 0.18));
}

.post-card-cover img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.cover-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 100%;
  color: var(--text-secondary);
  letter-spacing: 0.2em;
  font-size: 0.75rem;
}

.post-card-head {
  display: flex;
  align-items: flex-start;
  justify-content: space-between;
  gap: 12px;
}

.post-card-head h3 {
  margin: 0;
  font-size: 1.1rem;
}

.post-card-time {
  flex-shrink: 0;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.post-card-summary {
  margin: 10px 0 0;
  line-height: 1.7;
  color: var(--text-secondary);
}

.post-card-meta {
  display: flex;
  flex-wrap: wrap;
  gap: 14px;
  margin-top: 14px;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.activity-item {
  display: grid;
  grid-template-columns: 18px minmax(0, 1fr);
  gap: 16px;
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
}

.activity-dot {
  width: 10px;
  height: 10px;
  margin-top: 8px;
  border-radius: 50%;
}

.activity-dot.post {
  background: #2563eb;
}

.activity-dot.comment {
  background: #f97316;
}

.activity-head {
  display: flex;
  justify-content: space-between;
  gap: 12px;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.activity-title,
.activity-link {
  margin: 10px 0 0;
  font-size: 1.05rem;
  font-weight: 600;
  cursor: pointer;
}

.activity-link {
  color: var(--primary-color);
}

.activity-summary {
  margin: 10px 0 0;
  line-height: 1.7;
  color: var(--text-secondary);
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 28px;
}

@media (max-width: 768px) {
  .hero-content {
    flex-direction: column;
    gap: 16px;
    margin-top: -36px;
  }

  .hero-meta {
    padding-top: 0;
  }

  .post-card {
    grid-template-columns: 1fr;
  }

  .post-card-cover {
    height: 180px;
  }

  .activity-head,
  .post-card-head {
    flex-direction: column;
  }
}
</style>
