<template>
  <div class="home-container">
    <el-row :gutter="20">
      <el-col :span="18">
        <div class="glass-panel content-box">
          <div class="filter-header">
            <el-tabs v-model="queryParams.sort" class="sort-tabs" @tab-change="handleFilter">
              <el-tab-pane label="最新发布" name="latest"></el-tab-pane>
              <el-tab-pane label="最热讨论" name="hot"></el-tab-pane>
              <el-tab-pane label="点赞最多" name="most_liked"></el-tab-pane>
            </el-tabs>
          </div>

          <div v-loading="loading" class="post-list">
            <template v-if="postList.length > 0">
              <div v-for="post in postList" :key="post.id" class="post-item" @click="$router.push(`/post/${post.id}`)">
                <div class="post-content-wrap">
                  <h3 class="post-title">
                    <el-tag v-if="post.isTop" type="danger" size="small" effect="dark" class="mr-2">置顶</el-tag>
                    <el-tag v-if="post.isEssence" type="warning" size="small" effect="dark" class="mr-2">精华</el-tag>
                    {{ post.title }}
                  </h3>
                  <p class="post-summary">{{ post.summary }}</p>
                  
                  <div class="post-meta">
                    <div class="meta-left">
                      <el-avatar :size="24" :src="post.author.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                      <span class="author-name">{{ post.author.nickname || post.author.username }}</span>
                      <span class="divider">|</span>
                      <span class="post-time">{{ formatDate(post.createdAt) }}</span>
                      <span class="divider">|</span>
                      <el-tag size="small" type="info">{{ post.categoryName }}</el-tag>
                      
                      <div class="tags-group ml-3">
                        <el-tag v-for="tag in post.tags" :key="tag.id" size="small" class="mr-1" round>#{{ tag.name }}</el-tag>
                      </div>
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
            发布新帖
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
              @click="handleCategoryChange(cat.id)"
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
              @click="handleTagChange(tag.id)"
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
import { ref, reactive, onMounted } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { getPostPage } from '@/api/post'
import { getCategories } from '@/api/category'
import { getHotTags } from '@/api/tag'
import { View, ChatDotRound, Pointer } from '@element-plus/icons-vue'

const router = useRouter()
const route = useRoute()

const loading = ref(false)
const postList = ref([])
const total = ref(0)
const categories = ref([])
const hotTags = ref([])

const queryParams = reactive({
  page: 1,
  size: 15,
  sort: 'latest',
  categoryId: null,
  tagId: null,
  keyword: ''
})

const fetchPosts = async () => {
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

const fetchCategories = async () => {
  try {
    const res = await getCategories()
    categories.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const fetchHotTags = async () => {
  try {
    const res = await getHotTags(10)
    hotTags.value = res.data
  } catch (error) {
    console.error(error)
  }
}

const handleFilter = () => {
  queryParams.page = 1
  fetchPosts()
}

const handleCategoryChange = (id) => {
  queryParams.categoryId = id
  handleFilter()
}

const handleTagChange = (id) => {
  // TODO 标签筛选暂未在后端写完连表，仅传参
  queryParams.tagId = id
  handleFilter()
}

const handlePageChange = (page) => {
  queryParams.page = page
  fetchPosts()
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')}`
}

onMounted(() => {
  fetchCategories()
  fetchHotTags()
  fetchPosts()
})
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

.tags-group {
  display: flex;
  align-items: center;
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

.category-item:hover {
  background-color: var(--bg-color);
  color: var(--primary-color);
}

.category-item.active {
  background-color: var(--primary-color);
  color: white;
}

.category-item.active .count {
  background-color: rgba(255,255,255,0.2);
}

.count {
  background-color: var(--border-color);
  padding: 2px 8px;
  border-radius: 10px;
  font-size: 0.8rem;
}

.w-full {
  width: 100%;
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

.mr-1 {
  margin-right: 4px;
}

.mb-2 {
  margin-bottom: 8px;
}

.ml-3 {
  margin-left: 12px;
}

.cursor-pointer {
  cursor: pointer;
}
</style>
