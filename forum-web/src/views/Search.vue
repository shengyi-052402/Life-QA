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
            <h3 class="post-title" v-html="post.titleHighlight || post.title"></h3>
            <p class="post-summary" v-html="post.summaryHighlight || post.summary"></p>

            <div v-if="post.tags?.length" class="tag-list mt-10">
              <el-tag
                v-for="tag in post.tags"
                :key="tag.id"
                size="small"
                type="info"
                effect="plain"
              >
                #{{ tag.name }}
              </el-tag>
            </div>
            
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
import { reactive, ref, watch } from 'vue'
import { useRoute } from 'vue-router'
import { getPostPage } from '@/api/post'
import { formatDate } from '@/utils/format'

const route = useRoute()

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

const handlePageChange = (page) => {
  queryParams.page = page
  fetchSearchResults()
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

.tag-list {
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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

:deep(em) {
  color: #f56c6c;
  font-style: normal;
  font-weight: 700;
  background-color: rgba(245, 108, 108, 0.1);
  padding: 0 2px;
  border-radius: 2px;
}
</style>
