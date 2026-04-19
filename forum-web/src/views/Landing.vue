<template>
  <div class="landing-container" @mousemove="onMouseMove" @wheel="onWheel" @touchstart="onTouchStart" @touchmove="onTouchMove">
    <header class="landing-header">
      <div class="logo">unveil.</div>
      <div class="actions">
        <template v-if="userStore.token">
          <el-button color="#fff" style="color: #000" round @click="$router.push('/explore')">进入论坛</el-button>
          <div class="user-profile" @click="$router.push(`/user/${userStore.userInfo?.id}`)">
            <el-avatar :size="36" :src="userStore.userInfo?.avatar || 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'" />
          </div>
        </template>
        <template v-else>
          <el-button link class="login-btn" @click="$router.push('/login')">Log IN</el-button>
          <el-button color="#fff" style="color: #000" round @click="$router.push('/register')">Sign UP</el-button>
        </template>
      </div>
    </header>

    <div v-if="loading" class="loading-state">
      <div class="loader"></div>
      <div class="loading-text">LOADING 3D SCENE...</div>
    </div>

    <!-- 视角容器 -->
    <main class="viewport" v-else>
      <!-- 带有视差旋转的 3D 场景 -->
      <div class="scene" :style="{ transform: `translate(-50%, -50%) rotateX(${sceneRotateX}deg) rotateY(${sceneRotateY}deg)` }">
        
        <div 
          v-for="(post, index) in posts" 
          :key="post.id"
          class="card-wrapper"
          :ref="el => { if (el) cardRefs[index] = el }"
          @click="handlePostClick(post.id)"
          @mouseenter="hoveredIndex = index"
          @mouseleave="hoveredIndex = null"
        >
          <!-- 卡片的 3D 层 -->
          <div class="card-inner" :class="{ 'is-hovered': hoveredIndex === index }">
            <img :src="post.coverImage || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=2564&auto=format&fit=crop'" alt="Cover" class="cover-img" />
            
            <div class="card-info">
              <h2 class="title">{{ post.title }}</h2>
              <p class="summary">{{ post.summary }}</p>
              <div class="author-meta">
                <span>@{{ post.author?.nickname || post.author?.username }}</span>
                <div class="view-btn">View Post ↗</div>
              </div>
            </div>
          </div>
        </div>

      </div>
    </main>
    
    <!-- 底部滚动指示器 -->
    <div class="scroll-indicator" v-if="!loading && posts.length > 0">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${(targetScroll / maxScroll * 100) || 0}%` }"></div>
      </div>
      <div>Scroll to explore</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getPostPage } from '@/api/post'
import { ElMessage } from 'element-plus'

const router = useRouter()
const userStore = useUserStore()

const posts = ref([])
const loading = ref(true)

// 3D 逻辑相关的状态
const cardRefs = ref([])
const hoveredIndex = ref(null)

// 视差变量
const mouseX = ref(0)
const mouseY = ref(0)
const sceneRotateX = ref(0)
const sceneRotateY = ref(0)

// 滚动条变量
let targetScroll = 0
let currentScroll = 0
let maxScroll = 1000

// 动画句柄
let rafId = null

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await getPostPage({ page: 1, size: 20, sort: 'latest' })
    posts.value = res.data.records
    // 根据卡片数量设定最大滚动距离
    maxScroll = Math.max(100, (posts.value.length - 1) * 800)
  } catch (error) {
    console.error('Failed to fetch posts:', error)
  } finally {
    loading.value = false
    nextTick(() => {
      startRenderLoop()
    })
  }
}

onMounted(() => {
  if (userStore.token && !userStore.userInfo) {
    userStore.fetchUserInfo()
  }
  fetchPosts()
  window.addEventListener('resize', onResize)
})

onBeforeUnmount(() => {
  if (rafId) cancelAnimationFrame(rafId)
  window.removeEventListener('resize', onResize)
})

const handlePostClick = (postId) => {
  if (!userStore.token) {
    ElMessage.warning('探索这篇内容，请先登录或注册您的专属账号')
    router.push('/login')
  } else {
    router.push(`/post/${postId}`)
  }
}

// 线性插值计算 (Lerp)，用于丝滑动画
const lerp = (start, end, factor) => {
  return start + (end - start) * factor
}

// 阻尼更新循环
const startRenderLoop = () => {
  // 更新视差旋转
  sceneRotateX.value = lerp(sceneRotateX.value, -mouseY.value * 7, 0.05) // Y负数表示抬头
  sceneRotateY.value = lerp(sceneRotateY.value, mouseX.value * 10, 0.05)

  // 更新滚动进度
  currentScroll = lerp(currentScroll, targetScroll, 0.08)

  // 核心 3D 引擎：更新每张卡片的空间位置
  cardRefs.value.forEach((card, index) => {
    if (!card) return
    
    // 我们将卡片沿 Z轴深渊排列，每一张卡片距离间隔为 800px
    const cardZBase = -(index * 800)
    // 基础偏移加上当前滚动产生的推进
    let currentZ = cardZBase + currentScroll

    // x, y 的轻微打散，产生空间的错落感 (可以通过 index 进行交错)
    const offsetX = (index % 2 === 0 ? 1 : -1) * 350 + (index % 3) * 50
    const offsetY = (index % 2 === 0 ? -1 : 1) * 150

    let opacity = 1
    // 如果卡片跑到相机后面（z > 500）则淡出
    if (currentZ > 400) {
      opacity = Math.max(0, 1 - (currentZ - 400) / 200)
    }
    // 如果卡片在非常远的深渊，也微调透明度和亮度
    if (currentZ < -3000) {
      opacity = Math.max(0, 1 - Math.abs(currentZ + 3000) / 4000)
    }

    // 通过 CSS Transform 应用真实的 3D 渲染
    card.style.transform = `translate3d(${offsetX}px, ${offsetY}px, ${currentZ}px) rotateY(${index % 2===0 ? -10 : 10}deg)`
    card.style.opacity = opacity

    // 当图片来到面前且最清晰时添加聚焦样式(可以提升 z-index 等)
    if (currentZ > -200 && currentZ < 200) {
      card.style.pointerEvents = 'auto'
      card.style.filter = 'brightness(1.1) drop-shadow(0 20px 40px rgba(0,0,0,0.8))'
    } else {
      card.style.pointerEvents = 'none' // 远处的阻断防误触
      card.style.filter = 'brightness(0.3)'
    }
  })

  rafId = requestAnimationFrame(startRenderLoop)
}

// 鼠标追踪
const onMouseMove = (e) => {
  // 计算归一化的鼠标位置 (-1 到 1)
  mouseX.value = (e.clientX / window.innerWidth) * 2 - 1
  mouseY.value = (e.clientY / window.innerHeight) * 2 - 1
}

// 滚轮控制向前进
const onWheel = (e) => {
  // 放大滚动感受度
  targetScroll += e.deltaY * 1.5 
  // 限制滚动范围
  targetScroll = Math.max(0, Math.min(targetScroll, maxScroll + 800))
}

// 移动端支持
let touchStartY = 0
const onTouchStart = (e) => {
  touchStartY = e.touches[0].clientY
}
const onTouchMove = (e) => {
  const currentY = e.touches[0].clientY
  const delta = touchStartY - currentY
  targetScroll += delta * 3
  targetScroll = Math.max(0, Math.min(targetScroll, maxScroll + 800))
  touchStartY = currentY
}

const onResize = () => {
  // 处理尺寸变更时重置
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300;400;600;700&display=swap');

.landing-container {
  height: 100vh;
  width: 100vw;
  background-color: #030303;
  color: #fff;
  font-family: 'Space Grotesk', sans-serif;
  overflow: hidden; /* 关闭外层滚动，使用监听器控制 */
  position: relative;
  /* 非常暗淡的高级网格背景 */
  background-image: 
    linear-gradient(rgba(255, 255, 255, 0.02) 1px, transparent 1px),
    linear-gradient(90deg, rgba(255, 255, 255, 0.02) 1px, transparent 1px);
  background-size: 100px 100px;
}

.landing-header {
  position: fixed;
  top: 0;
  left: 0;
  width: 100%;
  padding: 30px 50px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  z-index: 1000;
  pointer-events: auto;
}

.logo {
  font-size: 2.2rem;
  font-weight: 700;
  letter-spacing: -1.5px;
  text-transform: uppercase;
}

.actions {
  display: flex;
  align-items: center;
  gap: 20px;
}

.login-btn {
  color: #b0b0b0;
  font-weight: 600;
  font-size: 1rem;
  text-transform: uppercase;
  letter-spacing: 1px;
}

.login-btn:hover {
  color: #fff;
}

.user-profile {
  cursor: pointer;
  transition: transform 0.3s ease;
  border: 2px solid rgba(255,255,255,0.2);
  border-radius: 50%;
}
.user-profile:hover {
  transform: scale(1.1);
  border-color: #fff;
}

/* ================== 重头戏 3D 透视引擎 ================== */
.viewport {
  position: absolute;
  top: 0;
  left: 0;
  width: 100vw;
  height: 100vh;
  perspective: 1000px; /* 控制镜头远近畸变 */
  overflow: hidden;
}

.scene {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  /* 保留深渊效果 */
  transform-style: preserve-3d; 
  /* 允许稍微偏移出视角一点点 */
  pointer-events: none;
}

.card-wrapper {
  position: absolute;
  /* 使元素以自身核心为锚点 */
  top: 50%;
  left: 50%;
  margin-top: -250px; /* 高度的一半 */
  margin-left: -180px; /* 宽度的一半 */
  width: 360px;
  height: 500px;
  cursor: pointer;
  /* 启用 GPU 加速 */
  will-change: transform, opacity, filter;
  transition: filter 0.3s;
}

.card-inner {
  width: 100%;
  height: 100%;
  position: relative;
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 0 0 1px rgba(255,255,255,0.1);
  background: #111;
  transition: transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.card-wrapper:hover .card-inner {
  transform: scale(1.02) translateZ(20px);
  box-shadow: 0 0 0 2px rgba(255,255,255,0.8), 0 30px 60px rgba(0,0,0,0.9);
}

.cover-img {
  width: 100%;
  height: 100%;
  object-fit: cover;
  opacity: 0.6;
  transition: opacity 0.4s, transform 0.6s;
}

.card-wrapper:hover .cover-img {
  opacity: 0.3;
  transform: scale(1.1); /* 图片轻微深陷放大 */
}

/* 隐藏在下方或需要 hover 才展示的详细信息 */
.card-info {
  position: absolute;
  bottom: 0;
  left: 0;
  width: 100%;
  padding: 30px;
  background: linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0) 100%);
  color: #fff;
  transform: translateY(40%);
  transition: transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  display: flex;
  flex-direction: column;
}

.card-wrapper:hover .card-info {
  transform: translateY(0);
}

.title {
  font-size: 1.6rem;
  font-weight: 700;
  line-height: 1.1;
  margin-bottom: 10px;
  text-transform: uppercase;
}

.summary {
  font-size: 0.9rem;
  color: #aaa;
  margin-bottom: 20px;
  line-height: 1.5;
  opacity: 0;
  transition: opacity 0.3s 0.1s; /* 延迟出现 */
}

.card-wrapper:hover .summary {
  opacity: 1;
}

.author-meta {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-size: 0.8rem;
  color: #777;
  font-weight: 600;
}

.view-btn {
  padding: 6px 12px;
  background: #fff;
  color: #000;
  border-radius: 20px;
  font-weight: 700;
  opacity: 0;
  transform: translateX(-10px);
  transition: all 0.3s 0.2s;
}

.card-wrapper:hover .view-btn {
  opacity: 1;
  transform: translateX(0);
}


/* Scroll Indicator */
.scroll-indicator {
  position: fixed;
  bottom: 40px;
  left: 50%;
  transform: translateX(-50%);
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 12px;
  font-size: 0.75rem;
  color: #888;
  letter-spacing: 2px;
  text-transform: uppercase;
  z-index: 100;
  pointer-events: none;
}

.progress-bar {
  width: 200px;
  height: 2px;
  background: rgba(255,255,255,0.1);
  border-radius: 2px;
  overflow: hidden;
}

.progress-fill {
  height: 100%;
  background: #fff;
  transition: width 0.1s; /* 只做轻微缓动，Lerp已经在控制了 */
}

.loading-state {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  height: 100vh;
  gap: 20px;
}

.loader {
  border: 2px solid rgba(255,255,255,0.1);
  border-top-color: #fff;
  border-radius: 50%;
  width: 50px;
  height: 50px;
  animation: spin 1s linear infinite;
}

.loading-text {
  letter-spacing: 4px;
  color: #888;
  font-size: 0.8rem;
}

@keyframes spin {
  to { transform: rotate(360deg); }
}
</style>
