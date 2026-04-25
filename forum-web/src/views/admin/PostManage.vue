<template>
  <div class="admin-page">
    <div class="toolbar glass-panel">
      <el-input v-model="filters.keyword" placeholder="搜索帖子标题/摘要" clearable @keyup.enter="handleSearch" />
      <el-select v-model="filters.categoryId" placeholder="分类" clearable>
        <el-option v-for="category in categories" :key="category.id" :label="category.name" :value="category.id" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option label="待审核" :value="0" />
        <el-option label="正常" :value="1" />
        <el-option label="隐藏" :value="2" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <div class="table-card glass-panel">
      <el-table :data="posts" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="帖子" min-width="320">
          <template #default="{ row }">
            <div class="post-cell">
              <strong>{{ row.title }}</strong>
              <p>{{ row.summary || '暂无摘要' }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="作者" width="150">
          <template #default="{ row }">{{ row.author?.nickname || row.author?.username || '-' }}</template>
        </el-table-column>
        <el-table-column prop="categoryName" label="分类" width="120" />
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="statusTagType(row.status)">{{ statusLabel(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="标记" width="160">
          <template #default="{ row }">
            <div class="flag-group">
              <el-tag v-if="row.isTop" type="warning">置顶</el-tag>
              <el-tag v-if="row.isEssence" type="success">精华</el-tag>
              <span v-if="!row.isTop && !row.isEssence" class="placeholder">无</span>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="互动" width="180">
          <template #default="{ row }">
            <span>{{ row.viewCount || 0 }} 浏览</span>
            <span class="metric">{{ row.commentCount || 0 }} 评论</span>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="360" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="toggleStatus(row, row.status === 2 ? 1 : 2)">
                {{ row.status === 2 ? '恢复' : '隐藏' }}
              </el-button>
              <el-button size="small" type="warning" @click="toggleTop(row)">
                {{ row.isTop ? '取消置顶' : '置顶' }}
              </el-button>
              <el-button size="small" type="success" @click="toggleEssence(row)">
                {{ row.isEssence ? '取消精华' : '设为精华' }}
              </el-button>
              <el-button size="small" type="danger" @click="removePost(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <div class="pager">
        <el-pagination
          background
          layout="total, prev, pager, next"
          :total="total"
          :current-page="filters.page"
          :page-size="filters.size"
          @current-change="handlePageChange"
        />
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { deleteAdminPost, getAdminPosts, updateAdminPost } from '@/api/admin'
import { getCategories } from '@/api/category'

const loading = ref(false)
const total = ref(0)
const posts = ref([])
const categories = ref([])
const filters = reactive({
  page: 1,
  size: 10,
  keyword: '',
  categoryId: undefined,
  status: undefined
})

function statusLabel(status) {
  return {
    0: '待审核',
    1: '正常',
    2: '隐藏'
  }[status] || '未知'
}

function statusTagType(status) {
  return {
    0: 'info',
    1: 'success',
    2: 'warning'
  }[status] || 'info'
}

async function fetchCategories() {
  const res = await getCategories()
  categories.value = res.data || []
}

async function fetchPosts() {
  loading.value = true
  try {
    const res = await getAdminPosts(filters)
    posts.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 1
  fetchPosts()
}

function handlePageChange(page) {
  filters.page = page
  fetchPosts()
}

async function toggleStatus(row, status) {
  await updateAdminPost(row.id, { status })
  fetchPosts()
}

async function toggleTop(row) {
  await updateAdminPost(row.id, { isTop: row.isTop ? 0 : 1 })
  fetchPosts()
}

async function toggleEssence(row) {
  await updateAdminPost(row.id, { isEssence: row.isEssence ? 0 : 1 })
  fetchPosts()
}

async function removePost(row) {
  await ElMessageBox.confirm(`确认删除帖子《${row.title}》吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteAdminPost(row.id)
  fetchPosts()
}

onMounted(async () => {
  await fetchCategories()
  await fetchPosts()
})
</script>

<style scoped>
.admin-page {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.toolbar,
.table-card {
  padding: 20px;
  border-radius: 24px;
}

.toolbar {
  display: grid;
  grid-template-columns: 1.8fr 180px 180px 120px;
  gap: 12px;
}

.post-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.post-cell p,
.placeholder {
  color: var(--text-secondary);
}

.flag-group,
.action-group {
  display: flex;
  gap: 8px;
  flex-wrap: wrap;
}

.metric {
  margin-left: 8px;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

@media (max-width: 960px) {
  .toolbar {
    grid-template-columns: 1fr;
  }
}
</style>
