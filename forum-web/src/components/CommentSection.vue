<template>
  <div class="comment-section">
    <h3 class="section-title">评论 <span class="count">{{ post.commentCount }}</span></h3>
    
    <!-- 顶部发布框 -->
    <div class="comment-publish-box mb-30" v-if="userStore.token">
      <el-avatar :size="40" :src="userStore.userInfo.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
      <div class="publish-input-wrap ml-15">
        <el-input
          v-model="publishContent"
          type="textarea"
          :rows="3"
          placeholder="发一条友善的评论吧..."
          resize="none"
          maxlength="1000"
          show-word-limit
        />
        <div class="publish-action mt-10">
          <el-button type="primary" :loading="isPublishing" @click="handlePublish(0, null)">发布评论</el-button>
        </div>
      </div>
    </div>
    <div class="login-tip mb-30" v-else>
      <el-button type="primary" plain @click="$router.push('/login')">请先登录后发表评论</el-button>
    </div>

    <!-- 评论列表 -->
    <div class="comment-list" v-loading="loading">
      <template v-if="comments.length > 0">
        <div class="comment-item" v-for="(comment, index) in comments" :key="comment.id">
          <el-avatar :size="40" :src="comment.author.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
          
          <div class="comment-main ml-15">
            <div class="comment-user">
              <span class="nickname">{{ comment.author.nickname || comment.author.username }}</span>
            </div>
            
            <div class="comment-content mt-10">{{ comment.content }}</div>
            
            <div class="comment-meta mt-10">
              <span class="time">{{ formatDate(comment.createdAt) }}</span>
              <span class="action-btn ml-15" :class="{ 'liked': comment.isLiked }" @click="handleLike(comment)">
                <el-icon><Pointer /></el-icon> <span v-if="comment.likeCount > 0">{{ comment.likeCount }}</span>
              </span>
              <span class="action-btn ml-15" @click="openReplyBox(comment.id, comment.author.id, comment.author.nickname)">
                <el-icon><ChatDotRound /></el-icon> 回复
              </span>
            </div>

            <!-- 顶级评论下的直接回复框 (针对根级) -->
            <div class="reply-box-inline mt-15" v-if="activeReplyId === comment.id && !replyingToSub">
              <el-input
                v-model="replyContent"
                type="textarea"
                :rows="2"
                :placeholder="'回复 @' + replyTargetName + ' :'"
                resize="none"
              />
              <div class="publish-action mt-10">
                <el-button size="small" @click="closeReplyBox">取消</el-button>
                <el-button size="small" type="primary" :loading="isPublishing" @click="handlePublish(comment.id, replyTargetId)">回复</el-button>
              </div>
            </div>

            <!-- 二级评论预览树 -->
            <div class="sub-comment-tree mt-15" v-if="comment.replyCount > 0">
              <!-- 前2条预览 -->
              <div class="sub-comment-item" v-for="sub in (expandedRootId === comment.id ? comment.allSubReplies : comment.replies)" :key="sub.id">
                <el-avatar :size="24" :src="sub.author.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
                <div class="sub-comment-main ml-10">
                  <div class="sub-comment-content">
                    <span class="nickname">{{ sub.author.nickname || sub.author.username }}</span>
                    <span class="reply-to-text" v-if="sub.replyToUser && sub.replyToUser.id !== comment.author.id">
                      回复 <span class="nickname">@{{ sub.replyToUser.nickname || sub.replyToUser.username }}</span>
                    </span>
                    : {{ sub.content }}
                  </div>
                  
                  <div class="comment-meta mt-5">
                    <span class="time">{{ formatDate(sub.createdAt) }}</span>
                    <span class="action-btn ml-10" :class="{ 'liked': sub.isLiked }" @click="handleLike(sub)">
                      <el-icon><Pointer /></el-icon> <span v-if="sub.likeCount > 0">{{ sub.likeCount }}</span>
                    </span>
                    <span class="action-btn ml-10" @click="openReplyBox(comment.id, sub.author.id, sub.author.nickname, true)">
                      <el-icon><ChatDotRound /></el-icon> 回复
                    </span>
                  </div>

                  <!-- 二级评论的盖楼回复框 -->
                  <div class="reply-box-inline mt-10" v-if="activeReplyId === comment.id && replyingToSub && replyTargetId === sub.author.id">
                    <el-input
                      v-model="replyContent"
                      type="textarea"
                      :rows="2"
                      :placeholder="'回复 @' + replyTargetName + ' :'"
                      resize="none"
                    />
                    <div class="publish-action mt-10">
                      <el-button size="small" @click="closeReplyBox">取消</el-button>
                      <el-button size="small" type="primary" :loading="isPublishing" @click="handlePublish(comment.id, replyTargetId)">回复</el-button>
                    </div>
                  </div>
                </div>
              </div>
              
              <!-- 展开更多 -->
              <div class="view-more-replies mt-5" v-if="comment.replyCount > 2 && expandedRootId !== comment.id">
                共 {{ comment.replyCount }} 条回复, <span class="click-text" @click="loadMoreReplies(comment, index)">点击查看</span>
              </div>
              <div class="view-more-replies mt-5" v-if="expandedRootId === comment.id && comment.subPage < Math.ceil(comment.replyCount / 10)">
                <span class="click-text" @click="loadMoreReplies(comment, index)">加载下一页...</span>
              </div>
            </div>

          </div>
        </div>
      </template>
      <el-empty v-else description="暂无评论，快来抢沙发吧~" />
    </div>

    <!-- 顶级评论分页 -->
    <div class="pagination-wrap mt-30" v-if="total > 0">
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
import { ref, reactive, onMounted } from 'vue'
import { getCommentPage, createComment, toggleCommentLike } from '@/api/comment'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'
import { Pointer, ChatDotRound } from '@element-plus/icons-vue'

const props = defineProps({
  post: {
    type: Object,
    required: true
  }
})

const emit = defineEmits(['comment-added'])

const userStore = useUserStore()

const loading = ref(false)
const comments = ref([])
const total = ref(0)
const pageParams = reactive({
  page: 1,
  size: 10,
  postId: props.post.id,
  parentId: 0,
  sort: 'latest'
})

// 发布顶级评论
const publishContent = ref('')
const isPublishing = ref(false)

// 回复内联框状态控制
const activeReplyId = ref(null) // 当前激活回复框的根级评论ID
const replyingToSub = ref(false) // 区分是在给根评论回复，还是给二级评论回复
const replyTargetId = ref(null) // 被@的作者ID
const replyTargetName = ref('') // 被@的作者昵称
const replyContent = ref('')

// 展开二级评论相关
const expandedRootId = ref(null) // 当前展开的根评论ID

const fetchComments = async () => {
  loading.value = true
  try {
    const res = await getCommentPage(pageParams)
    // 为每个根评论初始化二级分页状态
    res.data.records.forEach(c => {
      c.allSubReplies = c.replies ? [...c.replies] : []
      c.subPage = 1
    })
    comments.value = res.data.records
    total.value = res.data.total
  } catch (err) {
    console.error(err)
  } finally {
    loading.value = false
  }
}

const handlePageChange = (page) => {
  pageParams.page = page
  closeReplyBox()
  expandedRootId.value = null
  fetchComments()
}

const openReplyBox = (rootCommentId, targetUserId, targetUserName, isSub = false) => {
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

const closeReplyBox = () => {
  activeReplyId.value = null
  replyContent.value = ''
}

const handlePublish = async (parentId = 0, replyToUserId = null) => {
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
      fetchComments()
      emit('comment-added')
    } else {
      closeReplyBox()
      // 如果是为了保持B站流畅体验，可以单独重新拉取这个根评论的二级数据
      // 这里简便处理，重新拉取整个列表
      fetchComments()
      emit('comment-added')
    }
  } catch (err) {
    console.error(err)
  } finally {
    isPublishing.value = false
  }
}

// 加载更多回复 (针对某一条根评论)
const loadMoreReplies = async (comment, index) => {
  if (expandedRootId.value !== comment.id) {
    // 首次展开，从第1页重新拉取确保顺序
    expandedRootId.value = comment.id
    comment.subPage = 1
    comment.allSubReplies = []
  } else {
    // 翻下一页
    comment.subPage++
  }

  try {
    const res = await getCommentPage({
      page: comment.subPage,
      size: 10,
      postId: props.post.id,
      parentId: comment.id,
      sort: 'latest' // 二级一般按时间升序更合理，后端现在默认是desc，如果想B站体验需要后端传asc
    })
    
    // 如果是第一页直接覆盖，不是的话追加
    if (comment.subPage === 1) {
      comment.allSubReplies = res.data.records
    } else {
      comment.allSubReplies.push(...res.data.records)
    }
  } catch (err) {
    console.error(err)
  }
}

const handleLike = async (commentOrSub) => {
  if (!userStore.token) {
    ElMessage.warning('请先登录')
    return
  }
  try {
    const res = await toggleCommentLike(commentOrSub.id)
    commentOrSub.isLiked = res.data
    commentOrSub.likeCount += res.data ? 1 : -1
  } catch (err) {
    console.error(err)
  }
}

const formatDate = (dateStr) => {
  if (!dateStr) return ''
  const date = new Date(dateStr)
  return `${date.getFullYear()}-${String(date.getMonth() + 1).padStart(2, '0')}-${String(date.getDate()).padStart(2, '0')} ${String(date.getHours()).padStart(2, '0')}:${String(date.getMinutes()).padStart(2, '0')}`
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

.action-btn:hover {
  color: var(--primary-color);
}

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
