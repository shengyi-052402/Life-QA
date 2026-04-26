<template>
  <div class="dashboard-page">
    <section class="stats-grid">
      <article v-for="item in statCards" :key="item.label" class="stat-card glass-panel">
        <p>{{ item.label }}</p>
        <h3>{{ item.value }}</h3>
        <span>{{ item.extra }}</span>
      </article>
    </section>

    <section class="overview-grid">
      <article class="glass-panel overview-card">
        <div class="section-head">
          <h3>今日增长</h3>
          <span>实时读取数据库聚合</span>
        </div>
        <div class="trend-list">
          <div class="trend-item">
            <strong>{{ stats.todayNewUsers || 0 }}</strong>
            <span>新增用户</span>
          </div>
          <div class="trend-item">
            <strong>{{ stats.todayNewPosts || 0 }}</strong>
            <span>新增帖子</span>
          </div>
          <div class="trend-item">
            <strong>{{ stats.todayNewComments || 0 }}</strong>
            <span>新增评论</span>
          </div>
        </div>
      </article>

      <article class="glass-panel overview-card">
        <div class="section-head">
          <h3>当前进度</h3>
          <span>第三优先级已进入搜索引擎建设</span>
        </div>
        <ul class="progress-list">
          <li>独立搜索接口已接通</li>
          <li>ES 同步链路已接入发帖/编辑/删帖/评论</li>
          <li>顶部搜索建议已上线</li>
          <li>可通过后台按钮触发全量重建索引</li>
        </ul>
        <div class="action-row">
          <el-button type="primary" :loading="reindexing" @click="handleReindex">重建搜索索引</el-button>
        </div>
      </article>
    </section>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive } from 'vue'
import { ref } from 'vue'
import { ElMessage } from 'element-plus'
import { getAdminStats, reindexSearch } from '@/api/admin'

const stats = reactive({
  totalUsers: 0,
  totalPosts: 0,
  totalComments: 0,
  todayNewUsers: 0,
  todayNewPosts: 0,
  todayNewComments: 0
})
const reindexing = ref(false)

const statCards = computed(() => [
  { label: '总用户数', value: stats.totalUsers || 0, extra: `今日 +${stats.todayNewUsers || 0}` },
  { label: '总帖子数', value: stats.totalPosts || 0, extra: `今日 +${stats.todayNewPosts || 0}` },
  { label: '总评论数', value: stats.totalComments || 0, extra: `今日 +${stats.todayNewComments || 0}` }
])

async function fetchStats() {
  const res = await getAdminStats()
  Object.assign(stats, res.data || {})
}

async function handleReindex() {
  reindexing.value = true
  try {
    await reindexSearch()
    ElMessage.success('已触发全量重建搜索索引')
  } finally {
    reindexing.value = false
  }
}

onMounted(fetchStats)
</script>

<style scoped>
.dashboard-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.stats-grid,
.overview-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 20px;
}

.overview-grid {
  grid-template-columns: repeat(2, minmax(0, 1fr));
}

.stat-card,
.overview-card {
  padding: 24px;
  border-radius: 24px;
}

.stat-card p,
.section-head span,
.trend-item span,
.progress-list {
  color: var(--text-secondary);
}

.stat-card h3 {
  margin: 10px 0 6px;
  font-family: 'Space Grotesk', 'Inter', sans-serif;
  font-size: 2rem;
  color: var(--text-primary);
}

.section-head {
  display: flex;
  align-items: baseline;
  justify-content: space-between;
  margin-bottom: 20px;
}

.trend-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.trend-item {
  padding: 18px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.055);
  border: 1px solid var(--border-color);
}

.trend-item strong {
  display: block;
  font-size: 1.6rem;
  margin-bottom: 6px;
}

.progress-list {
  padding-left: 18px;
  line-height: 1.9;
}

.action-row {
  margin-top: 18px;
}

@media (max-width: 960px) {
  .stats-grid,
  .overview-grid,
  .trend-list {
    grid-template-columns: 1fr;
  }
}
</style>
