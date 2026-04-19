<template>
  <div class="search-container">
    <div class="glass-panel search-header">
      <h2>搜索结果: <span class="highlight-text">"{{ queryKeyword }}"</span></h2>
      <p class="result-count">共找到 {{ total }} 条相关帖子</p>
    </div>

    <div class="glass-panel search-content mt-20" v-loading="loading">
      <template v-if="postList.length > 0">
        <div v-for="post in postList" :key="post.id" class="post-item" @click="$router.push(`/post/${post.id}`)">
          <div class="post-content-wrap">
            <!-- 采用原生 Vue 配合前端渲染进行高亮展示伪效果 -->
            <h3 class="post-title" v-html="highlight(post.title)"></h3>
            <p class="post-summary" v-html="highlight(post.summary)"></p>
            
            <div class="post-meta mt-10">
              <span class="author-name">@{{ post.author.nickname || post.author.username }}</span>
              <span class="divider">·</span>
              <span class="post-time">{{ formatDate(post.createdAt) }}</span>
              <span class="divider">·</span>
              <el-tag size="small" type="info">{{ post.categoryName }}</el-tag>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else :description="'没有找到与 \'' + queryKeyword + '\' 相关的帖子'" />

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
  </div>
</template>

<script setup>
import { ref, reactive, watch, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { getPostPage } from '@/api/post'

const route = useRoute()
const router = useRouter()

const queryKeyword = ref('')
const loading = ref(false)
const postList = ref([])
const total = ref(0)

const queryParams = reactive({
  page: 1,
  size: 15,
  keyword: ''
})

const fetchSearchResults = async () => {
  if (!queryParams.keyword) return
  loading.value = true
  try {
    const res = await getPostPage(queryParams)
    postList.value = res.data.records
    total.value = res.data.total
  } catch (error) {
    console.error(error)
  } finally {
    loading.value = false
  }
}

// 模拟前端高亮替换函数
const highlight = (text) => {
  if (!text) return ''
  if (!queryKeyword.value) return text
  
  // 忽略大小写进行全局替换，添加CSS高亮样式
  const regex = new RegExp(`(${queryKeyword.value})`, 'gi')
  return text.replace(regex, '<span class="keyword-highlight">$1</span>')
}

const handlePageChange = (page) => {
  queryParams.page = page
  fetchSearchResults()
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

watch(
  () => route.query.q,
  (newKeyword) => {
    if (newKeyword) {
      queryKeyword.value = newKeyword
      queryParams.keyword = newKeyword
      queryParams.page = 1
      fetchSearchResults()
    }
  },
  { immediate: true }
)
</script>

<style scoped>
.search-container {
  max-width: 900px;
  margin: 0 auto;
}

.search-header {
  padding: 30px;
  text-align: center;
  border-radius: var(--radius-lg);
}

.highlight-text {
  color: var(--primary-color);
}

.result-count {
  margin-top: 10px;
  color: var(--text-secondary);
}

.search-content {
  padding: 20px;
  border-radius: var(--radius-lg);
  min-height: 500px;
}

.post-item {
  padding: 20px 10px;
  border-bottom: 1px solid var(--border-color);
  cursor: pointer;
  transition: var(--transition);
}

.post-item:hover {
  background-color: var(--bg-color);
  border-radius: var(--radius-md);
}

.post-title {
  font-size: 1.15rem;
  margin-bottom: 10px;
  color: var(--text-primary);
}

.post-summary {
  color: var(--text-secondary);
  line-height: 1.6;
  font-size: 0.95rem;
}

.post-meta {
  color: var(--text-secondary);
  font-size: 0.85rem;
  display: flex;
  align-items: center;
}

.divider {
  margin: 0 8px;
}

.pagination-wrap {
  margin-top: 30px;
  display: flex;
  justify-content: center;
}

.mt-20 {
  margin-top: 20px;
}
.mt-10 {
  margin-top: 10px;
}

/* 高亮样式（因为 v-html 渲染，需使用 :deep() 或直接设置非 scoped 样式，这里我们在全局或用 :deep 可以生效，使用 :deep） */
:deep(.keyword-highlight) {
  color: #f56c6c;
  font-weight: bold;
  background-color: rgba(245, 108, 108, 0.1);
  padding: 0 2px;
  border-radius: 2px;
}
</style>
