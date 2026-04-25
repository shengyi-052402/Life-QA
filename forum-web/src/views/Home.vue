<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="18">
        <div class="glass-panel content-box">
          <div class="filter-header">
            <el-tabs v-model="queryParams.sort" class="sort-tabs" @tab-change="handleFilter">
              <el-tab-pane label="最新发布" name="latest" />
              <el-tab-pane label="最热浏览" name="hot" />
              <el-tab-pane label="点赞最多" name="most_liked" />
              <el-tab-pane label="最多评论" name="most_commented" />
            </el-tabs>
          </div>

          <div v-if="activeTagName || activeCategoryName" class="active-filters">
            <el-tag v-if="activeCategoryName" closable @close="handleCategoryChange(null)">
              分类：{{ activeCategoryName }}
            </el-tag>
            <el-tag v-if="activeTagName" type="warning" closable @close="handleTagChange(null)">
              标签：#{{ activeTagName }}
            </el-tag>
          </div>

          <div v-loading="loading" class="post-list">
            <template v-if="postList.length > 0">
              <div
                v-for="post in postList"
                :key="post.id"
                class="post-item"
                @click="$router.push(`/post/${post.id}`)"
              >
                <div class="post-content-wrap">
                  <h3 class="post-title">
                    <el-tag v-if="post.isTop" type="danger" size="small" effect="dark" class="mr-2">置顶</el-tag>
                    <el-tag v-if="post.isEssence" type="warning" size="small" effect="dark" class="mr-2">精华</el-tag>
                    {{ post.title }}
                  </h3>
                  <p class="post-summary">{{ post.summary }}</p>

                  <div class="post-meta">
                    <div class="meta-left">
                      <el-avatar :size="24" :src="post.author?.avatar || defaultAvatar" />
                      <span class="author-name">{{ post.author?.nickname || post.author?.username }}</span>
                      <span class="divider">|</span>
                      <span class="post-time">{{ formatDate(post.createdAt) }}</span>
                      <span class="divider">|</span>
                      <el-tag size="small" type="info">{{ post.categoryName }}</el-tag>
                    </div>

                    <div class="meta-right">
                      <span class="stat-item"><el-icon><View /></el-icon> {{ post.viewCount }}</span>
                      <span class="stat-item"><el-icon><ChatDotRound /></el-icon> {{ post.commentCount }}</span>
                      <span class="stat-item"><el-icon><Pointer /></el-icon> {{ post.likeCount }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </template>
            <el-empty v-else description="暂无帖子数据" />
          </div>

          <div class="pagination-wrap" v-if="total > 0">
            <el-pagination
              v-model:current-page="queryParams.page"
              v-model:page-size="queryParams.size"
              layout="prev, pager, next"
              :total="total"
              @current-change="handlePageChange"
            />
          </div>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="glass-panel sidebar-box">
          <el-button type="primary" size="large" class="w-full" @click="$router.push('/post/create')">
            发布帖子
          </el-button>
        </div>

        <div class="glass-panel sidebar-box mt-20">
          <h3>分类</h3>
          <div class="category-list mt-10">
            <div
              class="category-item"
              :class="{ active: queryParams.categoryId === null }"
              @click="handleCategoryChange(null)"
            >
              全部
            </div>
            <div
              v-for="cat in categories"
              :key="cat.id"
              class="category-item"
              :class="{ active: queryParams.categoryId === cat.id }"
              @click="router.push(`/category/${cat.id}`)"
            >
              {{ cat.name }} <span class="count">{{ cat.postCount }}</span>
            </div>
          </div>
        </div>

        <div class="glass-panel sidebar-box mt-20">
          <h3>热门标签</h3>
          <div class="tags-container mt-10">
            <el-tag
              v-for="tag in hotTags"
              :key="tag.id"
              class="mr-2 mb-2 cursor-pointer"
              @click="router.push(`/tag/${tag.id}`)"
            >
              #{{ tag.name }}
            </el-tag>
          </div>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategories } from '@/api/category'
import { getPostPage } from '@/api/post'
import { getHotTags } from '@/api/tag'
import { formatDate } from '@/utils/format'
import { ChatDotRound, Pointer, View } from '@element-plus/icons-vue'

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const route = useRoute()
const router = useRouter()

const loading = ref(false)
const postList = ref([])
const total = ref(0)
const categories = ref([])
const hotTags = ref([])
const syncingRoute = ref(false)
const selectedTagName = ref('')

const queryParams = reactive({
  page: 1,
  size: 15,
  sort: 'latest',
  categoryId: null,
  tagId: null,
  keyword: ''
})

const activeCategoryName = computed(() => {
  return categories.value.find(item => item.id === queryParams.categoryId)?.name || ''
})

const activeTagName = computed(() => {
  return selectedTagName.value
})

function syncQueryToRoute() {
  syncingRoute.value = true
  router.replace({
    path: '/explore',
    query: {
      sort: queryParams.sort !== 'latest' ? queryParams.sort : undefined,
      categoryId: queryParams.categoryId ?? undefined,
      tagId: queryParams.tagId ?? undefined,
      tagName: queryParams.tagId ? selectedTagName.value || undefined : undefined,
      page: queryParams.page > 1 ? queryParams.page : undefined
    }
  })
}

function applyRouteQuery() {
  queryParams.sort = route.query.sort || 'latest'
  queryParams.categoryId = route.query.categoryId ? Number(route.query.categoryId) : null
  queryParams.tagId = route.query.tagId ? Number(route.query.tagId) : null
  queryParams.page = route.query.page ? Number(route.query.page) : 1
  selectedTagName.value = queryParams.tagId
    ? route.query.tagName || hotTags.value.find(item => item.id === queryParams.tagId)?.name || ''
    : ''
}

async function fetchPosts() {
  loading.value = true
  try {
    const res = await getPostPage(queryParams)
    postList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

async function fetchCategories() {
  const res = await getCategories()
  categories.value = res.data
}

async function fetchHotTags() {
  const res = await getHotTags(10)
  hotTags.value = res.data
  applyRouteQuery()
}

function handleFilter() {
  queryParams.page = 1
  syncQueryToRoute()
}

function handleCategoryChange(id) {
  queryParams.categoryId = id
  handleFilter()
}

function handleTagChange(id, name = '') {
  queryParams.tagId = id
  selectedTagName.value = id ? name || hotTags.value.find(item => item.id === id)?.name || '' : ''
  handleFilter()
}

function handlePageChange(page) {
  queryParams.page = page
  syncQueryToRoute()
}

onMounted(() => {
  applyRouteQuery()
  fetchCategories()
  fetchHotTags()
  fetchPosts()
})

watch(
  () => route.query,
  () => {
    applyRouteQuery()
    if (syncingRoute.value) {
      syncingRoute.value = false
    }
    fetchPosts()
  }
)
</script>

<style scoped>
.content-box {
  padding: 20px;
  border-radius: var(--radius-lg);
  min-height: 800px;
}

.sidebar-box {
  padding: 20px;
  border-radius: var(--radius-lg);
}

.filter-header {
  border-bottom: 1px solid var(--border-color);
  margin-bottom: 20px;
}

.active-filters {
  display: flex;
  gap: 10px;
  margin-bottom: 16px;
}

:deep(.el-tabs__nav-wrap::after) {
  display: none;
}

.post-item {
  padding: 16px 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition);
}

.post-item:hover {
  background-color: var(--bg-color);
  border-radius: var(--radius-md);
  padding: 16px;
  margin: 0 -16px;
}

.post-title {
  font-size: 1.1rem;
  font-weight: 600;
  margin-bottom: 8px;
  color: var(--text-primary);
}

.post-summary {
  color: var(--text-secondary);
  font-size: 0.95rem;
  margin-bottom: 12px;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.post-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.meta-left {
  display: flex;
  align-items: center;
}

.author-name {
  margin-left: 8px;
  color: var(--text-primary);
  font-weight: 500;
}

.divider {
  margin: 0 10px;
  color: var(--border-color);
}

.meta-right {
  display: flex;
  gap: 15px;
}

.stat-item {
  display: flex;
  align-items: center;
  gap: 4px;
}

.pagination-wrap {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.category-list {
  display: flex;
  flex-direction: column;
  gap: 8px;
}

.category-item {
  padding: 8px 12px;
  border-radius: var(--radius-md);
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
  transition: var(--transition);
  color: var(--text-secondary);
}

.category-item:hover,
.category-item.active {
  background-color: var(--bg-color);
  color: var(--primary-color);
}

.mt-20 {
  margin-top: 20px;
}

.mt-10 {
  margin-top: 10px;
}

.mr-2 {
  margin-right: 8px;
}

.mb-2 {
  margin-bottom: 8px;
}
</style>
