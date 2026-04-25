<template>
  <div class="archive-container">
    <div class="glass-panel archive-header">
      <div>
        <p class="archive-kicker">Tag</p>
        <h1>#{{ tag?.name || '标签帖子' }}</h1>
        <p class="archive-description">
          聚合这个标签下的全部讨论，便于继续追踪同主题内容。
        </p>
      </div>
      <el-tag size="large" type="warning">{{ total }} 篇帖子</el-tag>
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
            <div class="post-tags" v-if="post.tags?.length">
              <el-tag
                v-for="item in post.tags"
                :key="item.id"
                size="small"
                effect="plain"
                :type="item.id === Number(route.params.id) ? 'warning' : 'info'"
              >
                #{{ item.name }}
              </el-tag>
            </div>
            <div class="post-meta">
              <span>@{{ post.author?.nickname || post.author?.username }}</span>
              <span>·</span>
              <span>{{ formatDate(post.createdAt) }}</span>
              <span>·</span>
              <span>{{ post.likeCount }} 点赞</span>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="这个标签下还没有帖子" />

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
import { getPostPage } from '@/api/post'
import { getHotTags, getTags } from '@/api/tag'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()

const loading = ref(false)
const total = ref(0)
const postList = ref([])
const tags = ref([])

const queryParams = reactive({
  page: 1,
  size: 15,
  tagId: Number(route.params.id),
  sort: 'latest'
})

const tag = computed(() => tags.value.find(item => item.id === Number(route.params.id)) || null)

async function fetchTags() {
  const [allRes, hotRes] = await Promise.all([
    getTags({ page: 1, size: 500 }),
    getHotTags(20)
  ])
  const map = new Map()
  ;[...allRes.data, ...hotRes.data].forEach(item => map.set(item.id, item))
  tags.value = Array.from(map.values())
}

async function fetchPosts() {
  loading.value = true
  try {
    queryParams.tagId = Number(route.params.id)
    const res = await getPostPage(queryParams)
    postList.value = res.data.records
    total.value = res.data.total
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  await fetchTags()
  await fetchPosts()
})

watch(
  () => route.params.id,
  async () => {
    queryParams.page = 1
    await fetchTags()
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
  color: #e6a23c;
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

.post-tags {
  margin-top: 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 8px;
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
