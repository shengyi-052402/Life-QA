<template>
  <div class="admin-page">
    <div class="split-grid">
      <div class="glass-panel form-card">
        <div class="section-head">
          <h3>{{ editingId ? '编辑分类' : '新增分类' }}</h3>
          <span>支持名称、描述、图标与排序</span>
        </div>

        <el-form :model="form" label-position="top">
          <el-form-item label="分类名称">
            <el-input v-model="form.name" maxlength="50" />
          </el-form-item>
          <el-form-item label="描述">
            <el-input v-model="form.description" type="textarea" :rows="4" maxlength="255" show-word-limit />
          </el-form-item>
          <el-form-item label="图标">
            <el-input v-model="form.icon" maxlength="50" />
          </el-form-item>
          <el-form-item label="排序">
            <el-input-number v-model="form.sortOrder" :min="0" />
          </el-form-item>
          <div class="action-row">
            <el-button type="primary" @click="submitCategory">{{ editingId ? '保存修改' : '创建分类' }}</el-button>
            <el-button @click="resetForm">重置</el-button>
          </div>
        </el-form>
      </div>

      <div class="glass-panel table-card">
        <div class="section-head">
          <h3>分类列表</h3>
          <span>当前共 {{ categories.length }} 个分类</span>
        </div>

        <el-table :data="categories">
          <el-table-column prop="id" label="ID" width="70" />
          <el-table-column prop="name" label="名称" min-width="140" />
          <el-table-column prop="description" label="描述" min-width="220" />
          <el-table-column prop="postCount" label="帖子数" width="90" />
          <el-table-column prop="sortOrder" label="排序" width="90" />
          <el-table-column label="操作" width="180">
            <template #default="{ row }">
              <div class="action-group">
                <el-button size="small" @click="startEdit(row)">编辑</el-button>
                <el-button size="small" type="danger" @click="removeCategory(row)">删除</el-button>
              </div>
            </template>
          </el-table-column>
        </el-table>
      </div>
    </div>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessageBox } from 'element-plus'
import { createAdminCategory, deleteAdminCategory, updateAdminCategory } from '@/api/admin'
import { getCategories } from '@/api/category'

const categories = ref([])
const editingId = ref(null)
const initialForm = () => ({
  name: '',
  description: '',
  icon: '',
  sortOrder: 0
})
const form = reactive(initialForm())

async function fetchCategories() {
  const res = await getCategories()
  categories.value = res.data || []
}

function resetForm() {
  editingId.value = null
  Object.assign(form, initialForm())
}

function startEdit(row) {
  editingId.value = row.id
  Object.assign(form, {
    name: row.name,
    description: row.description,
    icon: row.icon,
    sortOrder: row.sortOrder || 0
  })
}

async function submitCategory() {
  if (editingId.value) {
    await updateAdminCategory(editingId.value, form)
  } else {
    await createAdminCategory(form)
  }
  resetForm()
  fetchCategories()
}

async function removeCategory(row) {
  await ElMessageBox.confirm(`确认删除分类「${row.name}」吗？`, '删除确认', {
    type: 'warning'
  })
  await deleteAdminCategory(row.id)
  fetchCategories()
}

onMounted(fetchCategories)
</script>

<style scoped>
.split-grid {
  display: grid;
  grid-template-columns: 360px 1fr;
  gap: 18px;
}

.form-card,
.table-card {
  padding: 20px;
  border-radius: 24px;
}

.section-head {
  display: flex;
  justify-content: space-between;
  align-items: baseline;
  margin-bottom: 18px;
}

.section-head span {
  color: var(--text-secondary);
}

.action-row,
.action-group {
  display: flex;
  gap: 10px;
}

@media (max-width: 960px) {
  .split-grid {
    grid-template-columns: 1fr;
  }
}
</style>
