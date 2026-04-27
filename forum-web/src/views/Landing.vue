<template>
  <div class="landing-container" ref="containerRef" @mousemove="onMouseMove" @wheel.prevent.stop="onWheel" @touchstart="onTouchStart" @touchmove.prevent="onTouchMove">
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
        <div class="globe-overlay" :class="{ 'is-visible': globeOpacity > 0.05 }">
          <h2>全人类的疑问<br/>都在这里</h2>
          <p>拖拽、缩放，探索来自世界各地的思考</p>
          <el-button color="#fff" style="color: #000; margin-top: 20px;" round size="large" @click="$router.push('/explore')">
            立即探索
          </el-button>
        </div>

        <!-- 鼠标悬停预览卡（跟随光标） -->
        <Transition name="popup">
          <div
            v-if="hoveredGlobeMarker"
            class="post-hover-card"
            :class="{ 'is-cluster': hoveredGlobeMarker.isCluster }"
            :style="{ top: hoverCardPos.y + 'px', left: hoverCardPos.x + 'px' }"
            @click="handleHoverCardClick"
            @mouseenter="onHoverCardEnter"
            @mouseleave="onHoverCardLeave"
          >
            <img
              v-if="!hoveredGlobeMarker.isCluster"
              :src="hoveredGlobeMarker.postData.coverImage || 'https://images.unsplash.com/photo-1618005182384-a83a8bd57fbe?q=80&w=400'"
              class="hover-card-cover"
            />
            <div class="hover-card-body">
              <template v-if="hoveredGlobeMarker.isCluster">
                <div class="hover-card-location">📍 {{ hoveredGlobeMarker.locationName || '附近位置' }}</div>
                <div class="cluster-title">共 {{ hoveredGlobeMarker.count }} 篇帖子</div>
                <div class="cluster-post-list">
                  <button
                    v-for="post in hoveredGlobeMarker.posts.slice(0, CLUSTER_CARD_LIMIT)"
                    :key="post.id"
                    class="cluster-post-item"
                    type="button"
                    @click.stop="navigateToPost(post)"
                  >
                    <span class="cluster-post-title">{{ post.title }}</span>
                    <span class="cluster-post-author">@{{ post.authorNickname }}</span>
                  </button>
                </div>
                <div v-if="hoveredGlobeMarker.count > CLUSTER_CARD_LIMIT" class="cluster-more">
                  还有 {{ hoveredGlobeMarker.count - CLUSTER_CARD_LIMIT }} 篇
                </div>
              </template>
              <template v-else>
                <div class="hover-card-location">📍 {{ hoveredGlobeMarker.postData.locationName }}</div>
              <div class="hover-card-title">{{ hoveredGlobeMarker.postData.title }}</div>
              <div class="hover-card-author">@{{ hoveredGlobeMarker.postData.authorNickname }}</div>
                <div class="hover-card-hint">点击查看全文 →</div>
              </template>
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
import * as THREE from 'three'

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
const hoveredGlobeMarker = ref(null)
const hoverCardPos = reactive({ x: 0, y: 0 })
const lastPointerPos = reactive({ x: window.innerWidth / 2, y: window.innerHeight / 2 })
let isHoverCardActive = false
let hoverCardCloseTimer = null

// 滚动条变量
let targetScroll = 0
let currentScroll = 0
let maxScroll = 1000
const SCROLL_THRESHOLD_FOR_GLOBE = 2000
const CLUSTER_DISTANCE_KM = 30
const CLUSTER_CARD_LIMIT = 5
const EARTH_RADIUS_KM = 6371
const GLOBE_MIN_CAMERA_DISTANCE = 150
const GLOBE_MAX_CAMERA_DISTANCE = 600

// 动画句柄
let rafId = null
let glowTexture = null

const cancelHoverCardClose = () => {
  if (!hoverCardCloseTimer) return
  clearTimeout(hoverCardCloseTimer)
  hoverCardCloseTimer = null
}

const scheduleHoverCardClose = () => {
  cancelHoverCardClose()
  hoverCardCloseTimer = setTimeout(() => {
    if (!isHoverCardActive) {
      hoveredGlobeMarker.value = null
      hoverCardCloseTimer = null
    }
  }, 260)
}

const placeHoverCard = () => {
  const cardWidth = 300
  const cardHeight = 260
  const margin = 16
  const preferredX = lastPointerPos.x + 20
  const preferredY = lastPointerPos.y - 80

  hoverCardPos.x = Math.min(
    window.innerWidth - cardWidth - margin,
    Math.max(margin, preferredX)
  )
  hoverCardPos.y = Math.min(
    window.innerHeight - cardHeight - margin,
    Math.max(margin, preferredY)
  )
}

const zoomGlobeByWheel = (deltaY) => {
  const controls = globeInstance.value?.controls()
  const camera = controls?.object
  const target = controls?.target
  if (!controls || !camera || !target) return

  const offset = camera.position.clone().sub(target)
  const currentDistance = offset.length()
  const zoomFactor = Math.exp(Math.min(Math.abs(deltaY), 240) * 0.0025)
  const nextDistance = deltaY > 0
    ? currentDistance / zoomFactor
    : currentDistance * zoomFactor
  const clampedDistance = Math.max(
    GLOBE_MIN_CAMERA_DISTANCE,
    Math.min(GLOBE_MAX_CAMERA_DISTANCE, nextDistance)
  )

  offset.setLength(clampedDistance)
  camera.position.copy(target).add(offset)
  controls.update()
}

const getGlowTexture = () => {
  if (glowTexture) return glowTexture

  const canvas = document.createElement('canvas')
  canvas.width = 128
  canvas.height = 128
  const ctx = canvas.getContext('2d')
  const gradient = ctx.createRadialGradient(64, 64, 0, 64, 64, 64)
  gradient.addColorStop(0, 'rgba(255, 255, 255, 1)')
  gradient.addColorStop(0.18, 'rgba(255, 235, 170, 0.95)')
  gradient.addColorStop(0.42, 'rgba(78, 179, 255, 0.35)')
  gradient.addColorStop(1, 'rgba(78, 179, 255, 0)')
  ctx.fillStyle = gradient
  ctx.fillRect(0, 0, 128, 128)

  glowTexture = new THREE.CanvasTexture(canvas)
  glowTexture.colorSpace = THREE.SRGBColorSpace
  return glowTexture
}

const toRadians = (degrees) => degrees * Math.PI / 180

const getDistanceKm = (pointA, pointB) => {
  const latDistance = toRadians(pointB.lat - pointA.lat)
  const lngDistance = toRadians(pointB.lng - pointA.lng)
  const latA = toRadians(pointA.lat)
  const latB = toRadians(pointB.lat)
  const a = Math.sin(latDistance / 2) ** 2
    + Math.cos(latA) * Math.cos(latB) * Math.sin(lngDistance / 2) ** 2
  return EARTH_RADIUS_KM * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a))
}

const buildPostMarker = (post) => ({
  lat: parseFloat(post.latitude),
  lng: parseFloat(post.longitude),
  size: 0.5 + Math.min(2, (post.likeCount || 0) / 10) * 0.5,
  color: '#7DD3FC',
  isCluster: false,
  postData: post,
  posts: [post],
  count: 1,
  locationName: post.locationName
})

const buildClusterMarkers = (postMarkers) => {
  const clusters = []

  postMarkers
    .filter(marker => Number.isFinite(marker.lat) && Number.isFinite(marker.lng))
    .forEach((marker) => {
      const cluster = clusters.find(item => getDistanceKm(item, marker) <= CLUSTER_DISTANCE_KM)
      if (cluster) {
        cluster.posts.push(marker.postData)
        cluster.lat = cluster.posts.reduce((sum, post) => sum + parseFloat(post.latitude), 0) / cluster.posts.length
        cluster.lng = cluster.posts.reduce((sum, post) => sum + parseFloat(post.longitude), 0) / cluster.posts.length
        cluster.count = cluster.posts.length
        cluster.size = Math.min(2.4, 0.9 + Math.log2(cluster.count + 1) * 0.45)
        cluster.color = '#FDE68A'
        cluster.isCluster = true
        cluster.postData = null
      } else {
        clusters.push({ ...marker })
      }
    })

  return clusters.map(marker => {
    if (marker.count > 1) {
      return {
        ...marker,
        isCluster: true,
        color: '#FDE68A',
        locationName: marker.posts[0]?.locationName || marker.locationName,
        primaryPost: marker.posts[0],
        postData: null
      }
    }
    return marker
  })
}

const getCountTexture = (count) => {
  const canvas = document.createElement('canvas')
  canvas.width = 128
  canvas.height = 128
  const ctx = canvas.getContext('2d')
  ctx.fillStyle = 'rgba(8, 12, 20, 0.92)'
  ctx.strokeStyle = 'rgba(255, 255, 255, 0.86)'
  ctx.lineWidth = 6
  ctx.beginPath()
  ctx.arc(64, 64, 42, 0, Math.PI * 2)
  ctx.fill()
  ctx.stroke()
  ctx.fillStyle = '#ffffff'
  ctx.font = '700 42px Space Grotesk, Arial, sans-serif'
  ctx.textAlign = 'center'
  ctx.textBaseline = 'middle'
  ctx.fillText(String(count), 64, 66)

  const texture = new THREE.CanvasTexture(canvas)
  texture.colorSpace = THREE.SRGBColorSpace
  return texture
}

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
  cancelHoverCardClose()
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

const handleHoverCardClick = () => {
  if (!hoveredGlobeMarker.value || hoveredGlobeMarker.value.isCluster) return
  navigateToPost(hoveredGlobeMarker.value.postData)
}

const onHoverCardEnter = () => {
  isHoverCardActive = true
  cancelHoverCardClose()
}

const onHoverCardLeave = () => {
  isHoverCardActive = false
  scheduleHoverCardClose()
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
  let pointsData = buildClusterMarkers(globePostsData.map(buildPostMarker))

  // 如果没有真实数据，放几个演示光点
  if (pointsData.length === 0) {
    pointsData.push(
      { lat: 39.9, lng: 116.4, size: 0.8, color: '#7DD3FC', isCluster: false, count: 1, posts: [], postData: null },
      { lat: 35.6, lng: 139.6, size: 0.7, color: '#7DD3FC', isCluster: false, count: 1, posts: [], postData: null },
      { lat: 51.5, lng: -0.1, size: 0.6, color: '#7DD3FC', isCluster: false, count: 1, posts: [], postData: null },
      { lat: 40.7, lng: -74.0, size: 0.9, color: '#FDE68A', isCluster: false, count: 1, posts: [], postData: null },
      { lat: -33.8, lng: 151.2, size: 0.5, color: '#7DD3FC', isCluster: false, count: 1, posts: [], postData: null },
    )
  }

  const myGlobe = Globe()(globeContainer.value)
    .globeImageUrl('//unpkg.com/three-globe/example/img/earth-blue-marble.jpg')
    .bumpImageUrl('//unpkg.com/three-globe/example/img/earth-topology.png')
    .backgroundImageUrl('//unpkg.com/three-globe/example/img/night-sky.png')
    .showAtmosphere(true)
    .atmosphereColor('#9DDCFF')
    .atmosphereAltitude(0.18)
    .ringsData(pointsData)
    .ringLat('lat')
    .ringLng('lng')
    .ringAltitude(0.006)
    .ringColor(d => [
      `${d.color}00`,
      `${d.color}B8`,
      `${d.color}55`,
      `${d.color}00`
    ])
    .ringMaxRadius(d => 1.4 + d.size * 1.7)
    .ringPropagationSpeed(d => 0.55 + d.size * 0.2)
    .ringRepeatPeriod(d => 1600 + d.size * 360)
    .customLayerData(pointsData)
    .customThreeObject(d => {
      const group = new THREE.Group()
      const glowSprite = new THREE.Sprite(new THREE.SpriteMaterial({
        map: getGlowTexture(),
        color: new THREE.Color(d.color),
        transparent: true,
        opacity: 0.95,
        depthWrite: false,
        blending: THREE.AdditiveBlending
      }))
      const scale = 3.2 + d.size * 2.4
      glowSprite.scale.set(scale, scale, 1)
      group.add(glowSprite)

      if (d.isCluster) {
        const countSprite = new THREE.Sprite(new THREE.SpriteMaterial({
          map: getCountTexture(d.count),
          transparent: true,
          depthWrite: false
        }))
        countSprite.scale.set(2.4, 2.4, 1)
        countSprite.position.set(scale * 0.28, scale * 0.28, 0.2)
        group.add(countSprite)
      }

      group.userData.pointData = d
      return group
    })
    .customThreeObjectUpdate((obj, d) => {
      const coords = myGlobe.getCoords(d.lat, d.lng, 0.018)
      Object.assign(obj.position, coords)
      obj.userData.pointData = d
    })
    .customLayerLabel(() => '')
    .onCustomLayerHover((point) => {
      const controls = myGlobe.controls()
      if (point && (point.postData || point.isCluster)) {
        cancelHoverCardClose()
        placeHoverCard()
        hoveredGlobeMarker.value = point
        controls.autoRotateSpeed = 0 // 悬停光点时停止转动
      } else {
        scheduleHoverCardClose()
        controls.autoRotateSpeed = 0.4 // 移开时光点恢复转动
      }
    })
    .onCustomLayerClick((point) => {
      if (point?.isCluster) {
        cancelHoverCardClose()
        placeHoverCard()
        hoveredGlobeMarker.value = point
      } else if (point && point.postData) {
        navigateToPost(point.postData)
      }
    })

  myGlobe.pointOfView({ lat: 25, lng: 110, altitude: 2 })
  const controls = myGlobe.controls()
  controls.enableZoom = true
  controls.autoRotate = true
  controls.autoRotateSpeed = 0.4
  controls.minDistance = GLOBE_MIN_CAMERA_DISTANCE
  controls.maxDistance = GLOBE_MAX_CAMERA_DISTANCE

  const globeMaterial = myGlobe.globeMaterial()
  globeMaterial.color = new THREE.Color(0xffffff)
  globeMaterial.bumpScale = 4
  globeMaterial.shininess = 0.18

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
  lastPointerPos.x = e.clientX
  lastPointerPos.y = e.clientY
}

const onWheel = (e) => {
  // 地球阶段反转默认滚轮方向：向下滚动拉近，向上滚动拉远
  if (globeOpacity.value > 0.8) {
    zoomGlobeByWheel(e.deltaY)
    return
  }
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
  transition: opacity 0.35s ease, transform 0.35s ease;
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
.post-hover-card.is-cluster {
  width: 300px;
  cursor: default;
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

.cluster-title {
  font-size: 1rem;
  font-weight: 800;
  color: #fff;
  margin-bottom: 10px;
}

.cluster-post-list {
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.cluster-post-item {
  width: 100%;
  border: 1px solid rgba(255,255,255,0.08);
  border-radius: 8px;
  background: rgba(255,255,255,0.05);
  color: #fff;
  cursor: pointer;
  padding: 8px 10px;
  text-align: left;
  transition: border-color 0.2s, background 0.2s;
}
.cluster-post-item:hover {
  background: rgba(125,211,252,0.12);
  border-color: rgba(125,211,252,0.45);
}

.cluster-post-title {
  display: -webkit-box;
  -webkit-line-clamp: 1;
  -webkit-box-orient: vertical;
  overflow: hidden;
  font-size: 0.82rem;
  font-weight: 700;
  line-height: 1.35;
}

.cluster-post-author {
  display: block;
  margin-top: 3px;
  font-size: 0.68rem;
  color: #8a8a8a;
}

.cluster-more {
  margin-top: 9px;
  font-size: 0.72rem;
  color: #FDE68A;
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
