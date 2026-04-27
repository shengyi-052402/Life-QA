<template>
  <div class="landing-container" ref="containerRef" @mousemove="onMouseMove" @wheel.prevent="onWheel" @touchstart="onTouchStart" @touchmove.prevent="onTouchMove">
    <header class="landing-header" :class="{ 'is-globe': globeOpacity > 0.5 }">
      <div class="logo">Life Q&A</div>
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
      <div class="loading-text">加载时空隧道...</div>
    </div>

    <!-- 视角容器 -->
    <main class="viewport" v-show="!loading">
      <!-- 带有视差旋转的 3D 卡片场景 (Z-Axis Scroll) -->
      <div 
        class="scene" 
        :style="{ 
          transform: `translate(-50%, -50%) rotateX(${sceneRotateX}deg) rotateY(${sceneRotateY}deg)`,
          opacity: sceneOpacity,
          pointerEvents: scenePointerEvents
        }"
      >
        <div 
          v-for="(post, index) in posts" 
          :key="post.id"
          class="card-wrapper"
          :ref="el => { if (el) cardRefs[index] = el }"
          @click="handlePostClick(post.id)"
          @mouseenter="hoveredIndex = index"
          @mouseleave="hoveredIndex = null"
        >
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

      <!-- 终章：3D 互动地球 (Globe.gl) -->
      <div 
        class="globe-wrapper" 
        :style="{ 
          opacity: globeOpacity, 
          pointerEvents: globePointerEvents,
          transform: `scale(${globeScale})`
        }"
      >
        <div ref="globeContainer" class="globe-container"></div>
        
        <!-- 地球的文字覆盖层 -->
        <div class="globe-overlay" :class="{ 'is-visible': globeOpacity > 0.8 }">
          <h2>全人类的疑问<br/>都在这里</h2>
          <p>拖拽、缩放，探索来自世界各地的思考</p>
          <el-button color="#fff" style="color: #000; margin-top: 20px;" round size="large" @click="$router.push('/explore')">
            立即探索
          </el-button>
        </div>

        <!-- 鼠标悬停预览卡（跟随光标） -->
        <Transition name="popup">
          <div
            v-if="hoveredGlobePost"
            class="post-hover-card"
            :style="{ top: hoverCardPos.y + 'px', left: hoverCardPos.x + 'px' }"
            @click="navigateToPost(hoveredGlobePost)"
          >
            <img
              :src="hoveredGlobePost.coverImage || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=400'"
              class="hover-card-cover"
            />
            <div class="hover-card-body">
              <div class="hover-card-location">📍 {{ hoveredGlobePost.locationName }}</div>
              <div class="hover-card-title">{{ hoveredGlobePost.title }}</div>
              <div class="hover-card-author">@{{ hoveredGlobePost.authorNickname }}</div>
              <div class="hover-card-hint">点击查看全文 →</div>
            </div>
          </div>
        </Transition>
      </div>
    </main>
    
    <!-- 底部滚动指示器 -->
    <div class="scroll-indicator" v-if="!loading && posts.length > 0" :style="{ opacity: sceneOpacity }">
      <div class="progress-bar">
        <div class="progress-fill" :style="{ width: `${Math.min(100, (targetScroll / (maxScroll - SCROLL_THRESHOLD_FOR_GLOBE)) * 100) || 0}%` }"></div>
      </div>
      <div>Scroll to explore</div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, onBeforeUnmount, nextTick, shallowRef, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { getPostPage, getGlobePosts } from '@/api/post'
import { ElMessage } from 'element-plus'
import Globe from 'globe.gl'

const router = useRouter()
const userStore = useUserStore()

const containerRef = ref(null)
const posts = ref([])
const loading = ref(true)

// 3D 卡片场景状态
const cardRefs = ref([])
const hoveredIndex = ref(null)
const sceneOpacity = ref(1)
const scenePointerEvents = ref('auto')

// 视差变量
const mouseX = ref(0)
const mouseY = ref(0)
const sceneRotateX = ref(0)
const sceneRotateY = ref(0)

// 3D 地球状态
const globeContainer = ref(null)
const globeInstance = shallowRef(null)
const globeOpacity = ref(0)
const globePointerEvents = ref('none')
const globeScale = ref(0.8)
// 帖子悬停预览卡
const hoveredGlobePost = ref(null)
const hoverCardPos = reactive({ x: 0, y: 0 })

// 滚动条变量
let targetScroll = 0
let currentScroll = 0
let maxScroll = 1000
const SCROLL_THRESHOLD_FOR_GLOBE = 2000

// 动画句柄
let rafId = null

const fetchPosts = async () => {
  loading.value = true
  try {
    const res = await getPostPage({ page: 1, size: 20, sort: 'latest' })
    posts.value = res.data.records
    const cardDepth = Math.max(100, (posts.value.length - 1) * 800)
    maxScroll = cardDepth + SCROLL_THRESHOLD_FOR_GLOBE
  } catch (error) {
    console.error('Failed to fetch posts:', error)
  } finally {
    loading.value = false
    nextTick(() => {
      initGlobe()
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
  if (globeInstance.value) {
    try { globeInstance.value._destructor() } catch(e) {}
  }
})

const handlePostClick = (postId) => {
  if (!userStore.token) {
    ElMessage.warning('探索这篇内容，请先登录或注册您的专属账号')
    router.push('/login')
  } else {
    router.push(`/post/${postId}`)
  }
}

// 地球光点点击跳转（需登录）
const navigateToPost = (postData) => {
  if (!postData) return
  if (!userStore.token) {
    ElMessage.warning('请先登录再查看帖子')
    router.push('/login')
  } else {
    router.push(`/post/${postData.id}`)
  }
}

// 初始化 3D 地球
const initGlobe = async () => {
  if (!globeContainer.value) return

  // 拉取真实帖子的地理数据
  let globePostsData = []
  try {
    const res = await getGlobePosts()
    globePostsData = res.data || []
  } catch(e) {
    console.warn('Globe posts fetch failed, globe will have no real data points')
  }

  // 构建光点数据
  const pointsData = globePostsData.map(p => ({
    lat: parseFloat(p.latitude),
    lng: parseFloat(p.longitude),
    size: 0.5 + Math.min(2, (p.likeCount || 0) / 10) * 0.5,
    color: '#60A5FA',
    postData: p
  }))

  // 如果没有真实数据，放几个演示光点
  if (pointsData.length === 0) {
    pointsData.push(
      { lat: 39.9, lng: 116.4, size: 0.8, color: '#60A5FA', postData: null },
      { lat: 35.6, lng: 139.6, size: 0.7, color: '#60A5FA', postData: null },
      { lat: 51.5, lng: -0.1, size: 0.6, color: '#60A5FA', postData: null },
      { lat: 40.7, lng: -74.0, size: 0.9, color: '#F59E0B', postData: null },
      { lat: -33.8, lng: 151.2, size: 0.5, color: '#60A5FA', postData: null },
    )
  }

  const myGlobe = Globe()(globeContainer.value)
    .globeImageUrl('//unpkg.com/three-globe/example/img/earth-dark.jpg')
    .bumpImageUrl('//unpkg.com/three-globe/example/img/earth-topology.png')
    .backgroundImageUrl('//unpkg.com/three-globe/example/img/night-sky.png')
    .pointsData(pointsData)
    .pointLat('lat')
    .pointLng('lng')
    .pointColor('color')
    .pointRadius('size')
    .pointAltitude(0.01)
    .pointLabel(() => '') // 禁用内置 label，用自定义 hover 卡
    .onPointHover((point, prevPoint) => {
      const controls = myGlobe.controls()
      if (point && point.postData) {
        hoveredGlobePost.value = point.postData
        controls.autoRotateSpeed = 0 // 悬停光点时停止转动
        // 游标位置由全局 mousemove 更新
      } else {
        hoveredGlobePost.value = null
        controls.autoRotateSpeed = 0.4 // 移开时光点恢复转动
      }
    })
    .onPointClick((point) => {
      if (point && point.postData) {
        navigateToPost(point.postData)
      }
    })

  myGlobe.pointOfView({ lat: 25, lng: 110, altitude: 2 })
  const controls = myGlobe.controls()
  controls.enableZoom = true
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.4
  controls.minDistance = 150
  controls.maxDistance = 600

  globeInstance.value = myGlobe
  
  setTimeout(onResize, 100)
}

const lerp = (start, end, factor) => start + (end - start) * factor

const startRenderLoop = () => {
  sceneRotateX.value = lerp(sceneRotateX.value, -mouseY.value * 7, 0.05)
  sceneRotateY.value = lerp(sceneRotateY.value, mouseX.value * 10, 0.05)
  currentScroll = lerp(currentScroll, targetScroll, 0.08)

  const cardsEndScroll = maxScroll - SCROLL_THRESHOLD_FOR_GLOBE

  if (currentScroll > cardsEndScroll) {
    const progress = Math.min(1, (currentScroll - cardsEndScroll) / 1000)
    
    sceneOpacity.value = Math.max(0, 1 - progress * 2)
    scenePointerEvents.value = sceneOpacity.value < 0.1 ? 'none' : 'auto'
    
    globeOpacity.value = progress
    globePointerEvents.value = globeOpacity.value > 0.5 ? 'auto' : 'none'
    globeScale.value = 0.8 + (progress * 0.2)
    
    if (containerRef.value) {
      containerRef.value.style.backgroundColor = `hsl(0,0%,${Math.max(0, 2 - progress * 2)}%)`
    }
  } else {
    sceneOpacity.value = 1
    scenePointerEvents.value = 'auto'
    globeOpacity.value = 0
    globePointerEvents.value = 'none'
    globeScale.value = 0.8
    if (containerRef.value) {
      containerRef.value.style.backgroundColor = '#030303'
    }
  }

  cardRefs.value.forEach((card, index) => {
    if (!card) return
    const cardZBase = -(index * 800)
    let currentZ = cardZBase + currentScroll
    const offsetX = (index % 2 === 0 ? 1 : -1) * 350 + (index % 3) * 50
    const offsetY = (index % 2 === 0 ? -1 : 1) * 150

    let opacity = 1
    if (currentZ > 400) opacity = Math.max(0, 1 - (currentZ - 400) / 200)
    if (currentZ < -3000) opacity = Math.max(0, 1 - Math.abs(currentZ + 3000) / 4000)

    card.style.transform = `translate3d(${offsetX}px, ${offsetY}px, ${currentZ}px) rotateY(${index % 2===0 ? -10 : 10}deg)`
    card.style.opacity = opacity

    if (currentZ > -200 && currentZ < 200 && scenePointerEvents.value === 'auto') {
      card.style.pointerEvents = 'auto'
      card.style.filter = 'brightness(1.1) drop-shadow(0 20px 40px rgba(0,0,0,0.8))'
    } else {
      card.style.pointerEvents = 'none'
      card.style.filter = 'brightness(0.3)'
    }
  })

  rafId = requestAnimationFrame(startRenderLoop)
}

const onMouseMove = (e) => {
  mouseX.value = (e.clientX / window.innerWidth) * 2 - 1
  mouseY.value = (e.clientY / window.innerHeight) * 2 - 1
  // 悬停卡跟随光标（偏移避免遮住光点）
  hoverCardPos.x = e.clientX + 20
  hoverCardPos.y = e.clientY - 80
}

const onWheel = (e) => {
  // 如果地球已经出现且指针在地球上，让 globe.gl 自己处理缩放
  if (globeOpacity.value > 0.8) return
  targetScroll += e.deltaY * 1.5 
  targetScroll = Math.max(0, Math.min(targetScroll, maxScroll))
}

let touchStartY = 0
const onTouchStart = (e) => { touchStartY = e.touches[0].clientY }
const onTouchMove = (e) => {
  const currentY = e.touches[0].clientY
  const delta = touchStartY - currentY
  targetScroll += delta * 3
  targetScroll = Math.max(0, Math.min(targetScroll, maxScroll))
  touchStartY = currentY
}

const onResize = () => {
  if (globeInstance.value && globeContainer.value) {
    globeInstance.value.width(window.innerWidth).height(window.innerHeight)
  }
}
</script>

<style scoped>
@import url('https://fonts.googleapis.com/css2?family=Space+Grotesk:wght@300;400;600;700&display=swap');
@import url('https://fonts.googleapis.com/css2?family=Noto+Serif+SC:wght@400;600;900&display=swap');

.landing-container {
  height: 100vh;
  width: 100vw;
  background-color: #030303;
  color: #fff;
  font-family: 'Space Grotesk', sans-serif;
  overflow: hidden;
  position: relative;
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
  z-index: 2000;
  pointer-events: auto;
  transition: all 0.5s ease;
}

.logo {
  font-size: 2.2rem;
  font-weight: 700;
  letter-spacing: -1px;
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
.login-btn:hover { color: #fff; }

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

/* ================== Z 轴透视引擎 ================== */
.viewport {
  position: absolute;
  top: 0; left: 0;
  width: 100vw;
  height: 100vh;
  perspective: 1000px;
  overflow: hidden;
}

.scene {
  position: absolute;
  top: 50%;
  left: 50%;
  width: 100%;
  height: 100%;
  transform-style: preserve-3d; 
  transition: opacity 0.5s ease;
}

.card-wrapper {
  position: absolute;
  top: 50%;
  left: 50%;
  margin-top: -250px;
  margin-left: -180px;
  width: 360px;
  height: 500px;
  cursor: pointer;
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
  transform: scale(1.1);
}

.card-info {
  position: absolute;
  bottom: 0; left: 0;
  width: 100%;
  padding: 30px;
  background: linear-gradient(to top, rgba(0,0,0,0.9) 0%, rgba(0,0,0,0) 100%);
  color: #fff;
  transform: translateY(40%);
  transition: transform 0.4s cubic-bezier(0.2, 0.8, 0.2, 1);
  display: flex;
  flex-direction: column;
}
.card-wrapper:hover .card-info { transform: translateY(0); }

.title {
  font-family: 'Noto Serif SC', serif;
  font-size: 1.6rem;
  font-weight: 900;
  line-height: 1.2;
  margin-bottom: 10px;
  letter-spacing: 0.05em;
}

.summary {
  font-size: 0.9rem;
  color: #aaa;
  margin-bottom: 20px;
  line-height: 1.5;
  opacity: 0;
  transition: opacity 0.3s 0.1s;
}
.card-wrapper:hover .summary { opacity: 1; }

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

/* ================== 3D 地球引擎 ================== */
.globe-wrapper {
  position: absolute;
  top: 0; left: 0;
  width: 100%;
  height: 100%;
  z-index: 100;
  transition: opacity 1.5s ease, transform 1.5s cubic-bezier(0.2, 0.8, 0.2, 1);
}

.globe-container {
  width: 100%;
  height: 100%;
  cursor: grab;
}
.globe-container:active { cursor: grabbing; }

.globe-overlay {
  position: absolute;
  top: 50%;
  left: 8%;
  max-width: 420px;
  pointer-events: none;
  opacity: 0;
  transform: translateY(-40%);
  transition: opacity 1s 0.5s ease, transform 1s 0.5s ease;
}
.globe-overlay.is-visible {
  opacity: 1;
  pointer-events: auto;
  transform: translateY(-50%);
}
.globe-overlay h2 {
  font-family: 'Noto Serif SC', serif;
  font-size: 3.5rem;
  line-height: 1.1;
  font-weight: 900;
  margin-bottom: 20px;
  text-shadow: 0 4px 20px rgba(0,0,0,0.5);
}
.globe-overlay p {
  font-size: 1.1rem;
  color: #aaa;
  letter-spacing: 1px;
}

/* 帖子悬停预览卡（跟随鼠标，点击跳转） */
.post-hover-card {
  position: fixed;
  width: 260px;
  background: rgba(10, 10, 16, 0.96);
  border: 1px solid rgba(255,255,255,0.12);
  border-radius: 14px;
  overflow: hidden;
  z-index: 600;
  backdrop-filter: blur(24px);
  box-shadow: 0 20px 60px rgba(0,0,0,0.9), 0 0 0 1px rgba(96,165,250,0.15);
  cursor: pointer;
  pointer-events: auto;
  transition: box-shadow 0.2s;
}
.post-hover-card:hover {
  box-shadow: 0 24px 70px rgba(0,0,0,0.95), 0 0 0 2px rgba(96,165,250,0.5);
}

.hover-card-cover {
  width: 100%;
  height: 130px;
  object-fit: cover;
  opacity: 0.75;
  display: block;
  transition: opacity 0.3s;
}
.post-hover-card:hover .hover-card-cover { opacity: 1; }

.hover-card-body {
  padding: 12px 14px 14px;
}

.hover-card-location {
  font-size: 0.7rem;
  color: #60A5FA;
  letter-spacing: 1px;
  margin-bottom: 5px;
  text-transform: uppercase;
}

.hover-card-title {
  font-size: 0.9rem;
  font-weight: 700;
  line-height: 1.35;
  margin-bottom: 5px;
  color: #fff;
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
}

.hover-card-author {
  font-size: 0.72rem;
  color: #666;
  margin-bottom: 10px;
}

.hover-card-hint {
  font-size: 0.78rem;
  font-weight: 600;
  color: #60A5FA;
  letter-spacing: 0.5px;
}

/* Popup 动画 */
.popup-enter-active, .popup-leave-active {
  transition: all 0.2s cubic-bezier(0.2, 0.8, 0.2, 1);
}
.popup-enter-from, .popup-leave-to {
  opacity: 0;
  transform: scale(0.92) translateY(6px);
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
  transition: opacity 0.5s ease;
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
  transition: width 0.1s;
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
