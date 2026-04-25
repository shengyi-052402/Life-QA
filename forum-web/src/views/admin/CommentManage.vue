<template>
  <div class="admin-page">
    <div class="toolbar glass-panel">
      <el-input v-model="filters.keyword" placeholder="搜索评论内容" clearable @keyup.enter="handleSearch" />
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="隐藏" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <div class="table-card glass-panel">
      <el-table :data="comments" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="评论内容" min-width="340">
          <template #default="{ row }">
            <div class="comment-cell">
              <strong>{{ row.author?.nickname || row.author?.username || '-' }}</strong>
              <p>{{ row.content }}</p>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="所属帖子" min-width="220">
          <template #default="{ row }">
            <span>{{ row.postTitle || `帖子 #${row.postId}` }}</span>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '正常' : '隐藏' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="createdAt" label="创建时间" min-width="180" />
        <el-table-column label="操作" width="120" fixed="right">
          <template #default="{ row }">
            <el-button v-if="row.status === 1" size="small" type="danger" @click="removeComment(row)">删除</el-button>
            <span v-else class="muted-text">已隐藏</span>
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
import { deleteAdminComment, getAdminComments } from '@/api/admin'

const loading = ref(false)
const total = ref(0)
const comments = ref([])
const filters = reactive({
  page: 1,
  size: 10,
  keyword: '',
  status: undefined
})

async function fetchComments() {
  loading.value = true
  try {
    const res = await getAdminComments(filters)
    comments.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 1
  fetchComments()
}

function handlePageChange(page) {
  filters.page = page
  fetchComments()
}

async function removeComment(row) {
  await ElMessageBox.confirm('确认删除这条评论吗？', '删除确认', {
    type: 'warning'
  })
  await deleteAdminComment(row.id)
  fetchComments()
}

onMounted(fetchComments)
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
  grid-template-columns: 1fr 160px 120px;
  gap: 12px;
}

.comment-cell {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.comment-cell p {
  color: var(--text-secondary);
}

.muted-text {
  color: var(--text-secondary);
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
