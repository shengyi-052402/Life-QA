<template>
  <div class="comment-section">
    <h3 class="section-title">评论 <span class="count">{{ post.commentCount }}</span></h3>

    <div v-if="userStore.token" class="comment-publish-box mb-30">
      <el-avatar :size="40" :src="userStore.userInfo.avatar || defaultAvatar" />
      <div class="publish-input-wrap ml-15">
        <el-input
          v-model="publishContent"
          type="textarea"
          :rows="3"
          placeholder="发一条友善的评论..."
          resize="none"
          maxlength="1000"
          show-word-limit
        />
        <div class="publish-action mt-10">
          <el-button type="primary" :loading="isPublishing" @click="handlePublish(0, null)">发表评论</el-button>
        </div>
      </div>
    </div>
    <div v-else class="login-tip mb-30">
      <el-button type="primary" plain @click="$router.push('/login')">请先登录后发表评论</el-button>
    </div>

    <div v-loading="loading" class="comment-list">
      <template v-if="comments.length > 0">
        <div
          v-for="comment in comments"
          :key="comment.id"
          class="comment-item"
          :class="{ focused: focusCommentId === comment.id }"
          :data-comment-id="comment.id"
        >
          <el-avatar :size="40" :src="comment.author.avatar || defaultAvatar" />

          <div class="comment-main ml-15">
            <div class="comment-user">
              <span class="nickname">{{ comment.author.nickname || comment.author.username }}</span>
            </div>

            <div class="comment-content mt-10">{{ comment.content }}</div>

            <div class="comment-meta mt-10">
              <span class="time">{{ formatDate(comment.createdAt, true) }}</span>
              <span class="action-btn ml-15" :class="{ liked: comment.isLiked }" @click="handleLike(comment)">
                <el-icon><Pointer /></el-icon> <span v-if="comment.likeCount > 0">{{ comment.likeCount }}</span>
              </span>
              <span class="action-btn ml-15" @click="openReplyBox(comment.id, comment.author.id, comment.author.nickname)">
                <el-icon><ChatDotRound /></el-icon> 回复
              </span>
            </div>

            <div v-if="activeReplyId === comment.id && !replyingToSub" class="reply-box-inline mt-15">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                :placeholder="'回复 @' + replyTargetName + ':'"
                resize="none"
              />
              <div class="publish-action mt-10">
                <el-button size="small" @click="closeReplyBox">取消</el-button>
                <el-button size="small" type="primary" :loading="isPublishing" @click="handlePublish(comment.id, replyTargetId)">回复</el-button>
              </div>
            </div>

            <div v-if="comment.replyCount > 0" class="sub-comment-tree mt-15">
              <div
                v-for="sub in displayedReplies(comment)"
                :key="sub.id"
                class="sub-comment-item"
                :class="{ focused: focusCommentId === sub.id }"
                :data-comment-id="sub.id"
              >
                <el-avatar :size="24" :src="sub.author.avatar || defaultAvatar" />
                <div class="sub-comment-main ml-10">
                  <div class="sub-comment-content">
                    <span class="nickname">{{ sub.author.nickname || sub.author.username }}</span>
                    <span v-if="sub.replyToUser && sub.replyToUser.id !== comment.author.id" class="reply-to-text">
                      回复 <span class="nickname">@{{ sub.replyToUser.nickname || sub.replyToUser.username }}</span>
                    </span>
                    : {{ sub.content }}
                  </div>

                  <div class="comment-meta mt-5">
                    <span class="time">{{ formatDate(sub.createdAt, true) }}</span>
                    <span class="action-btn ml-10" :class="{ liked: sub.isLiked }" @click="handleLike(sub)">
                      <el-icon><Pointer /></el-icon> <span v-if="sub.likeCount > 0">{{ sub.likeCount }}</span>
                    </span>
                    <span class="action-btn ml-10" @click="openReplyBox(comment.id, sub.author.id, sub.author.nickname, true)">
                      <el-icon><ChatDotRound /></el-icon> 回复
                    </span>
                  </div>

                  <div v-if="activeReplyId === comment.id && replyingToSub && replyTargetId === sub.author.id" class="reply-box-inline mt-10">
                    <el-input
                      v-model="replyContent"
                      type="textarea"
                      :rows="2"
                      :placeholder="'回复 @' + replyTargetName + ':'"
                      resize="none"
                    />
                    <div class="publish-action mt-10">
                      <el-button size="small" @click="closeReplyBox">取消</el-button>
                      <el-button size="small" type="primary" :loading="isPublishing" @click="handlePublish(comment.id, replyTargetId)">回复</el-button>
                    </div>
                  </div>
                </div>
              </div>

              <div v-if="comment.replyCount > 2 && expandedRootId !== comment.id" class="view-more-replies mt-5">
                共 {{ comment.replyCount }} 条回复 <span class="click-text" @click="loadMoreReplies(comment, 1)">点击查看</span>
              </div>
              <div v-if="expandedRootId === comment.id && comment.subPage < Math.ceil(comment.replyCount / 10)" class="view-more-replies mt-5">
                <span class="click-text" @click="loadMoreReplies(comment, comment.subPage + 1)">加载下一页...</span>
              </div>
            </div>
          </div>
        </div>
      </template>
      <el-empty v-else description="暂无评论，快来抢沙发" />
    </div>

    <div v-if="total > 0" class="pagination-wrap mt-30">
      <el-pagination
        v-model:current-page="pageParams.page"
        v-model:page-size="pageParams.size"
        layout="prev, pager, next"
        :total="total"
        @current-change="handlePageChange"
      />
    </div>
  </div>
</template>

<script setup>
import { nextTick, onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { ChatDotRound, Pointer } from '@element-plus/icons-vue'
import { createComment, getCommentPage, toggleCommentLike } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { formatDate } from '@/utils/format'

const props = defineProps({
  post: { type: Object, required: true },
  initialPage: { type: Number, default: 1 },
  focusCommentId: { type: Number, default: null },
  rootCommentId: { type: Number, default: null },
  replyPage: { type: Number, default: 1 }
})

const emit = defineEmits(['comment-added'])
const userStore = useUserStore()
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const loading = ref(false)
const comments = ref([])
const total = ref(0)
const pageParams = reactive({
  page: props.initialPage,
  size: 10,
  postId: props.post.id,
  parentId: 0,
  sort: 'latest'
})

const publishContent = ref('')
const isPublishing = ref(false)
const activeReplyId = ref(null)
const replyingToSub = ref(false)
const replyTargetId = ref(null)
const replyTargetName = ref('')
const replyContent = ref('')
const expandedRootId = ref(null)

watch(
  () => props.initialPage,
  (page) => {
    if (page && pageParams.page !== page) {
      pageParams.page = page
      fetchComments()
    }
  }
)

async function fetchComments() {
  loading.value = true
  try {
    const res = await getCommentPage(pageParams)
    res.data.records.forEach(comment => {
      comment.allSubReplies = comment.replies ? [...comment.replies] : []
      comment.subPage = 1
    })
    comments.value = res.data.records
    total.value = res.data.total
    await nextTick()
    await focusTargetComment()
  } finally {
    loading.value = false
  }
}

function handlePageChange(page) {
  pageParams.page = page
  closeReplyBox()
  expandedRootId.value = null
  fetchComments()
}

function openReplyBox(rootCommentId, targetUserId, targetUserName, isSub = false) {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  activeReplyId.value = rootCommentId
  replyingToSub.value = isSub
  replyTargetId.value = targetUserId
  replyTargetName.value = targetUserName
  replyContent.value = ''
}

function closeReplyBox() {
  activeReplyId.value = null
  replyContent.value = ''
}

async function handlePublish(parentId = 0, replyToUserId = null) {
  const content = parentId === 0 ? publishContent.value : replyContent.value
  if (!content.trim()) {
    ElMessage.warning('评论内容不能为空')
    return
  }

  isPublishing.value = true
  try {
    await createComment({
      postId: props.post.id,
      parentId,
      replyToUserId,
      content
    })
    ElMessage.success('发布成功')
    if (parentId === 0) {
      publishContent.value = ''
      pageParams.page = 1
    } else {
      closeReplyBox()
    }
    await fetchComments()
    emit('comment-added')
  } finally {
    isPublishing.value = false
  }
}

function displayedReplies(comment) {
  return expandedRootId.value === comment.id ? comment.allSubReplies : comment.replies
}

async function loadMoreReplies(comment, page = 1) {
  expandedRootId.value = comment.id
  comment.subPage = page

  const res = await getCommentPage({
    page,
    size: 10,
    postId: props.post.id,
    parentId: comment.id,
    sort: 'latest'
  })

  comment.allSubReplies = page === 1
    ? res.data.records
    : [...comment.allSubReplies, ...res.data.records]

  await nextTick()
  await focusTargetComment()
}

async function handleLike(commentOrSub) {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  const res = await toggleCommentLike(commentOrSub.id)
  commentOrSub.isLiked = res.data
  commentOrSub.likeCount += res.data ? 1 : -1
}

async function focusTargetComment() {
  if (!props.focusCommentId) return

  let target = document.querySelector(`[data-comment-id="${props.focusCommentId}"]`)
  if (!target && props.rootCommentId) {
    const rootComment = comments.value.find(item => item.id === props.rootCommentId)
    if (rootComment && props.focusCommentId !== props.rootCommentId) {
      await loadMoreReplies(rootComment, props.replyPage || 1)
      target = document.querySelector(`[data-comment-id="${props.focusCommentId}"]`)
    }
  }

  if (target) {
    target.scrollIntoView({ behavior: 'smooth', block: 'center' })
  } else {
    document.getElementById('comments')?.scrollIntoView({ behavior: 'smooth', block: 'start' })
  }
}

onMounted(() => {
  fetchComments()
})
</script>

<style scoped>
.comment-section {
  padding-top: 20px;
}

.section-title {
  font-size: 1.3rem;
  margin-bottom: 25px;
  color: var(--text-primary);
}

.section-title .count {
  font-size: 1rem;
  color: var(--text-secondary);
  font-weight: normal;
  margin-left: 5px;
}

.comment-publish-box {
  display: flex;
  align-items: flex-start;
}

.publish-input-wrap {
  flex: 1;
}

.publish-action {
  display: flex;
  justify-content: flex-end;
}

.comment-item {
  display: flex;
  margin-bottom: 25px;
  padding-bottom: 20px;
  border-bottom: 1px solid var(--border-color);
  border-radius: 12px;
}

.comment-item.focused,
.sub-comment-item.focused {
  background: rgba(245, 158, 11, 0.08);
}

.comment-main {
  flex: 1;
}

.comment-user .nickname {
  font-weight: 600;
  color: var(--text-primary);
  font-size: 0.95rem;
}

.comment-content {
  font-size: 1rem;
  line-height: 1.6;
  color: var(--text-primary);
}

.comment-meta {
  font-size: 0.85rem;
  color: var(--text-secondary);
  display: flex;
  align-items: center;
}

.action-btn {
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  gap: 3px;
  transition: color 0.2s;
}

.action-btn:hover,
.action-btn.liked {
  color: var(--primary-color);
}

.sub-comment-tree {
  background-color: var(--bg-color);
  padding: 12px 15px;
  border-radius: var(--radius-md);
}

.sub-comment-item {
  display: flex;
  margin-bottom: 15px;
  border-radius: 10px;
}

.sub-comment-item:last-child {
  margin-bottom: 0;
}

.sub-comment-main {
  flex: 1;
}

.sub-comment-content {
  font-size: 0.95rem;
  line-height: 1.5;
  color: var(--text-primary);
}

.sub-comment-content .nickname {
  color: var(--text-secondary);
  font-weight: 500;
  margin-right: 5px;
}

.reply-to-text {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.reply-box-inline {
  background: var(--bg-color);
  padding: 15px;
  border-radius: var(--radius-md);
}

.view-more-replies {
  font-size: 0.85rem;
  color: var(--text-secondary);
}

.click-text {
  color: var(--primary-color);
  cursor: pointer;
  margin-left: 5px;
}

.click-text:hover {
  text-decoration: underline;
}

.pagination-wrap {
  display: flex;
  justify-content: center;
}

.mb-30 { margin-bottom: 30px; }
.ml-15 { margin-left: 15px; }
.ml-10 { margin-left: 10px; }
.mt-10 { margin-top: 10px; }
.mt-15 { margin-top: 15px; }
.mt-5 { margin-top: 5px; }
.mt-30 { margin-top: 30px; }
</style>
