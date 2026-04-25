<template>
  <div class="post-detail-container">
    <el-row :gutter="20">
      <el-col :span="18">
        <div class="glass-panel main-box" v-loading="loading">
          <template v-if="post">
            <h1 class="post-title">{{ post.title }}</h1>

            <div class="post-meta-header">
              <div class="meta-left">
                <el-avatar :size="40" :src="post.author?.avatar || defaultAvatar" />
                <div class="author-info">
                  <div class="author-name">{{ post.author?.nickname || post.author?.username }}</div>
                  <div class="post-time">
                    发布于 {{ formatDate(post.createdAt, true) }}
                    <span class="view-item ml-3"><el-icon><View /></el-icon> {{ post.viewCount }} 阅读</span>
                  </div>
                </div>
              </div>
              <div class="meta-right">
                <el-button v-if="isAuthor" type="primary" plain @click="handleEdit">编辑</el-button>
                <el-button v-if="isAuthor" type="danger" plain @click="handleDelete">删除</el-button>
              </div>
            </div>

            <div v-if="post.tags?.length" class="post-tags mt-15 mb-20">
              <el-tag
                v-for="tag in post.tags"
                :key="tag.id"
                class="mr-2 cursor-pointer"
                type="info"
                round
                @click="handleTagClick(tag)"
              >
                # {{ tag.name }}
              </el-tag>
            </div>

            <div class="post-content wangeditor-content" v-html="post.content"></div>

            <div class="post-actions mt-30">
              <el-button size="large" :type="post.isLiked ? 'primary' : 'default'" @click="handleLike">
                <el-icon class="mr-1"><Pointer /></el-icon>
                点赞 ({{ post.likeCount }})
              </el-button>
              <el-button size="large" :type="post.isFavorited ? 'warning' : 'default'" @click="handleFavorite">
                <el-icon class="mr-1"><Star /></el-icon>
                收藏 ({{ post.favoriteCount }})
              </el-button>
            </div>

            <el-divider />

            <div id="comments">
              <CommentSection
                :post="post"
                :initial-page="commentPage"
                :focus-comment-id="focusCommentId"
                :root-comment-id="rootCommentId"
                :reply-page="replyPage"
                @comment-added="fetchDetail"
              />
            </div>
          </template>
        </div>
      </el-col>

      <el-col :span="6">
        <div class="glass-panel sidebar-box">
          <h3>关于作者</h3>
          <div v-if="post" class="author-summary mt-20">
            <el-avatar :size="64" :src="post.author?.avatar || defaultAvatar" />
            <h4 class="mt-10">{{ post.author?.nickname || post.author?.username }}</h4>
            <p class="author-bio mt-10">{{ post.author?.bio || '这个人还没有留下简介。' }}</p>

            <div class="author-stats mt-20">
              <div class="stat-box">
                <div class="stat-value">{{ post.author?.postCount || 0 }}</div>
                <div class="stat-label">发帖</div>
              </div>
            </div>
          </div>
        </div>

        <div v-if="post" class="glass-panel sidebar-box mt-20">
          <h3>所属分类</h3>
          <el-tag size="large" type="success" class="mt-10">{{ post.categoryName }}</el-tag>
        </div>
      </el-col>
    </el-row>
  </div>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Pointer, Star, View } from '@element-plus/icons-vue'
import CommentSection from '@/components/CommentSection.vue'
import { togglePostFavorite, togglePostLike } from '@/api/comment'
import { deletePost, getPostDetail } from '@/api/post'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'
const postId = route.params.id

const loading = ref(true)
const post = ref(null)

const focusCommentId = computed(() => Number(route.query.commentId) || null)
const rootCommentId = computed(() => Number(route.query.rootCommentId) || null)
const commentPage = computed(() => Number(route.query.commentPage) || 1)
const replyPage = computed(() => Number(route.query.replyPage) || 1)

const isAuthor = computed(() => {
  return userStore.userInfo && post.value && userStore.userInfo.id === post.value.author?.id
})

async function fetchDetail() {
  loading.value = true
  try {
    const res = await getPostDetail(postId)
    post.value = res.data
    document.title = `${post.value.title} - 开发者论坛`
  } finally {
    loading.value = false
  }
}

function handleEdit() {
  router.push(`/post/edit/${postId}`)
}

function handleTagClick(tag) {
  router.push({ path: '/explore', query: { tagId: tag.id, tagName: tag.name } })
}

function handleDelete() {
  ElMessageBox.confirm('此操作将永久删除该帖子，是否继续？', '警告', {
    confirmButtonText: '确认删除',
    cancelButtonText: '取消',
    type: 'warning'
  }).then(async () => {
    await deletePost(postId)
    ElMessage.success('删除成功')
    router.replace('/explore')
  }).catch(() => {})
}

async function handleLike() {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }

  const res = await togglePostLike(postId)
  post.value.isLiked = res.data
  post.value.likeCount += res.data ? 1 : -1
}

async function handleFavorite() {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }

  const res = await togglePostFavorite(postId)
  post.value.isFavorited = res.data
  post.value.favoriteCount += res.data ? 1 : -1
}

onMounted(() => {
  fetchDetail()
})
</script>

<style scoped>
.main-box {
  padding: 30px;
  border-radius: var(--radius-lg);
  min-height: 800px;
}

.sidebar-box {
  padding: 20px;
  border-radius: var(--radius-lg);
}

.post-title {
  font-size: 2rem;
  font-weight: 700;
  color: var(--text-primary);
  line-height: 1.4;
  margin-bottom: 20px;
}

.post-meta-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
}

.meta-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.author-name {
  font-size: 1.1rem;
  font-weight: 500;
  color: var(--text-primary);
  margin-bottom: 4px;
}

.post-time {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.view-item {
  display: inline-flex;
  align-items: center;
  gap: 2px;
}

.post-content {
  font-size: 1.05rem;
  line-height: 1.8;
  color: var(--text-primary);
  word-break: break-word;
}

:deep(.post-content img) {
  max-width: 100%;
  height: auto;
  border-radius: var(--radius-md);
  margin: 10px 0;
}

.post-actions {
  display: flex;
  justify-content: center;
  gap: 20px;
}

.author-summary {
  display: flex;
  flex-direction: column;
  align-items: center;
  text-align: center;
}

.author-bio {
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.author-stats {
  display: flex;
  justify-content: center;
  width: 100%;
  padding-top: 15px;
  border-top: 1px dashed var(--border-color);
}

.stat-box {
  text-align: center;
}

.stat-value {
  font-size: 1.2rem;
  font-weight: 600;
  color: var(--text-primary);
}

.stat-label {
  font-size: 0.8rem;
  color: var(--text-secondary);
}

.mt-10 { margin-top: 10px; }
.mt-15 { margin-top: 15px; }
.mt-20 { margin-top: 20px; }
.mt-30 { margin-top: 30px; }
.mb-20 { margin-bottom: 20px; }
.ml-3 { margin-left: 12px; }
.mr-2 { margin-right: 8px; }
.mr-1 { margin-right: 4px; }
.cursor-pointer { cursor: pointer; }
</style>
