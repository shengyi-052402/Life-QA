<template>
  <div class="archive-container">
    <div class="glass-panel archive-header">
      <div>
        <p class="archive-kicker">Category</p>
        <h1>{{ category?.name || '分类帖子' }}</h1>
        <p class="archive-description">
          {{ category?.description || '查看该分类下的全部帖子。' }}
        </p>
      </div>
      <el-tag size="large" type="success">{{ total }} 篇帖子</el-tag>
    </div>

    <div class="glass-panel archive-content" v-loading="loading">
      <template v-if="postList.length">
        <div
          v-for="post in postList"
          :key="post.id"
          class="post-item"
          @click="router.push(`/post/${post.id}`)"
        >
          <div class="post-main">
            <h3 class="post-title">{{ post.title }}</h3>
            <p class="post-summary">{{ post.summary }}</p>
            <div class="post-meta">
              <span>@{{ post.author?.nickname || post.author?.username }}</span>
              <span>·</span>
              <span>{{ formatDate(post.createdAt) }}</span>
              <span>·</span>
              <span>{{ post.commentCount }} 评论</span>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="这个分类下还没有帖子" />

      <div v-if="total > 0" class="pagination-wrap">
        <el-pagination
          v-model:current-page="queryParams.page"
          v-model:page-size="queryParams.size"
          layout="prev, pager, next"
          :total="total"
          @current-change="fetchPosts"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getCategories } from '@/api/category'
import { getPostPage } from '@/api/post'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const postList = ref([])
const categories = ref([])

const queryParams = reactive({
  page: 1,
  size: 15,
  categoryId: Number(route.params.id),
  sort: 'latest'
})

const category = computed(() => categories.value.find(item => item.id === Number(route.params.id)) || null)

async function fetchCategories() {
  const res = await getCategories()
  categories.value = res.data
}

async function fetchPosts() {
  loading.value = true
  try {
    queryParams.categoryId = Number(route.params.id)
    const res = await getPostPage(queryParams)
    postList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchCategories()
  await fetchPosts()
})

watch(
  () => route.params.id,
  async () => {
    queryParams.page = 1
    await fetchCategories()
    await fetchPosts()
  }
)
</script>

<style scoped>
.archive-container {
  max-width: 960px;
  margin: 0 auto;
}

.archive-header,
.archive-content {
  border-radius: var(--radius-lg);
}

.archive-header {
  padding: 28px;
  display: flex;
  justify-content: space-between;
  gap: 16px;
  align-items: flex-start;
}

.archive-kicker {
  margin: 0 0 8px;
  color: var(--primary-color);
  text-transform: uppercase;
  letter-spacing: 0.14em;
  font-size: 0.78rem;
}

.archive-header h1 {
  margin: 0;
  font-size: 2rem;
}

.archive-description {
  margin: 10px 0 0;
  color: var(--text-secondary);
}

.archive-content {
  padding: 10px 24px 24px;
  margin-top: 20px;
  min-height: 420px;
}

.post-item {
  padding: 18px 0;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
}

.post-item:last-child {
  border-bottom: none;
}

.post-title {
  margin: 0 0 8px;
  font-size: 1.15rem;
}

.post-summary {
  margin: 0;
  color: var(--text-secondary);
  line-height: 1.7;
}

.post-meta {
  margin-top: 12px;
  display: flex;
  gap: 10px;
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
  margin-top: 24px;
}
</style>
