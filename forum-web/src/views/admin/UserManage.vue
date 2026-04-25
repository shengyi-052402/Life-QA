<template>
  <div class="admin-page">
    <div class="toolbar glass-panel">
      <el-input v-model="filters.keyword" placeholder="搜索用户名 / 昵称 / 邮箱" clearable @keyup.enter="handleSearch" />
      <el-select v-model="filters.role" placeholder="角色" clearable>
        <el-option label="普通用户" :value="0" />
        <el-option label="管理员" :value="1" />
      </el-select>
      <el-select v-model="filters.status" placeholder="状态" clearable>
        <el-option label="正常" :value="1" />
        <el-option label="禁用" :value="0" />
      </el-select>
      <el-button type="primary" @click="handleSearch">查询</el-button>
    </div>

    <div class="table-card glass-panel">
      <el-table :data="users" v-loading="loading">
        <el-table-column prop="id" label="ID" width="80" />
        <el-table-column label="用户">
          <template #default="{ row }">
            <div class="user-cell">
              <el-avatar :src="row.avatar" :size="36">{{ (row.nickname || row.username || 'U').slice(0, 1) }}</el-avatar>
              <div>
                <strong>{{ row.nickname || row.username }}</strong>
                <p>@{{ row.username }}</p>
              </div>
            </div>
          </template>
        </el-table-column>
        <el-table-column prop="email" label="邮箱" min-width="220" />
        <el-table-column label="角色" width="110">
          <template #default="{ row }">
            <el-tag :type="row.role === 1 ? 'danger' : 'info'">{{ row.role === 1 ? '管理员' : '普通用户' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="100">
          <template #default="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'warning'">{{ row.status === 1 ? '正常' : '禁用' }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="postCount" label="帖子数" width="100" />
        <el-table-column prop="createdAt" label="注册时间" min-width="180" />
        <el-table-column label="操作" width="240" fixed="right">
          <template #default="{ row }">
            <div class="action-group">
              <el-button size="small" @click="toggleRole(row)">
                {{ row.role === 1 ? '取消管理员' : '设为管理员' }}
              </el-button>
              <el-button size="small" :type="row.status === 1 ? 'warning' : 'success'" @click="toggleStatus(row)">
                {{ row.status === 1 ? '禁用' : '启用' }}
              </el-button>
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
import { updateAdminUser, getAdminUsers } from '@/api/admin'

const loading = ref(false)
const total = ref(0)
const users = ref([])
const filters = reactive({
  page: 1,
  size: 10,
  keyword: '',
  role: undefined,
  status: undefined
})

async function fetchUsers() {
  loading.value = true
  try {
    const res = await getAdminUsers(filters)
    users.value = res.data?.records || []
    total.value = res.data?.total || 0
  } finally {
    loading.value = false
  }
}

function handleSearch() {
  filters.page = 1
  fetchUsers()
}

function handlePageChange(page) {
  filters.page = page
  fetchUsers()
}

async function toggleRole(row) {
  await updateAdminUser(row.id, { role: row.role === 1 ? 0 : 1 })
  fetchUsers()
}

async function toggleStatus(row) {
  await updateAdminUser(row.id, { status: row.status === 1 ? 0 : 1 })
  fetchUsers()
}

onMounted(fetchUsers)
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
  grid-template-columns: 1.6fr repeat(2, 160px) 120px;
  gap: 12px;
}

.user-cell {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-cell p {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.action-group {
  display: flex;
  gap: 8px;
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
