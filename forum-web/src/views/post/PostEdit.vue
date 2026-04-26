<template>
  <div class="post-create-container">
    <div class="glass-panel main-editor-box" v-loading="pageLoading">
      <h2 class="page-title">编辑帖子</h2>

      <el-form :model="postForm" :rules="rules" ref="postFormRef" label-position="top">
        <el-form-item prop="title" label="标题">
          <el-input v-model="postForm.title" placeholder="请输入帖子标题 (简明扼要)" size="large" maxlength="200" show-word-limit />
        </el-form-item>

        <el-form-item prop="coverImage" label="背景封面图 (必需)">
          <el-upload
            class="cover-uploader"
            action="/api/files/upload"
            :headers="uploadHeaders"
            :show-file-list="false"
            :on-success="handleCoverSuccess"
            :before-upload="beforeCoverUpload"
          >
            <img v-if="postForm.coverImage" :src="postForm.coverImage" class="cover-preview" />
            <el-icon v-else class="cover-uploader-icon"><Plus /></el-icon>
          </el-upload>
          <div class="el-upload__tip">请上传一张高质量图片，将作为首页展示背景</div>
        </el-form-item>

        <el-form-item prop="summary" label="核心介绍 (必需)">
          <el-input
            v-model="postForm.summary"
            type="textarea"
            :rows="3"
            placeholder="请填写一段精炼的基本介绍，这将在用户试图查看帖子背景图时展示。"
            maxlength="500"
            show-word-limit
          />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="8">
            <el-form-item prop="categoryId" label="分类">
              <el-select v-model="postForm.categoryId" placeholder="请选择分类" size="large" class="w-full">
                <el-option
                  v-for="cat in categories"
                  :key="cat.id"
                  :label="cat.name"
                  :value="cat.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="标签">
              <el-select
                v-model="postForm.tagIds"
                multiple
                filterable
                allow-create
                default-first-option
                :reserve-keyword="false"
                placeholder="请选择或输入新标签并回车添加"
                size="large"
                class="w-full"
              >
                <el-option
                  v-for="tag in allTags"
                  :key="tag.id"
                  :label="tag.name"
                  :value="tag.id"
                />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="📍 发帖地区 (可选，将在地球上显示)">
              <el-select
                v-model="citySelection"
                filterable
                remote
                clearable
                :remote-method="onCitySearch"
                placeholder='搜索城市，如"北京"、"Tokyo"'
                size="large"
                class="w-full"
                value-key="name"
                @change="onCitySelect"
              >
                <el-option
                  v-for="city in cityOptions"
                  :key="`${city.name}-${city.lng}-${city.lat}`"
                  :label="`${city.nameZh}（${city.name}）`"
                  :value="city"
                />
              </el-select>
              <div v-if="postForm.locationName" class="location-hint">
                📌 {{ postForm.locationName }} · {{ postForm.latitude?.toFixed(4) }}, {{ postForm.longitude?.toFixed(4) }}
              </div>
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="16">
            <el-form-item label="精确地址 (可选)">
              <el-autocomplete
                v-model="postForm.address"
                :fetch-suggestions="searchAmapAddress"
                placeholder="搜索或输入详细地址"
                size="large"
                class="w-full"
                maxlength="255"
                value-key="value"
                @select="selectAmapAddress"
              />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="高德 POI ID (可选)">
              <el-input
                v-model="postForm.placeId"
                placeholder="选择地址后自动填充"
                size="large"
                maxlength="128"
              />
            </el-form-item>
          </el-col>
        </el-row>

        <div class="amap-picker">
          <div v-if="amapAvailable" ref="amapContainerRef" class="amap-container"></div>
          <div v-else class="amap-fallback">
            配置 VITE_AMAP_KEY 后可使用地图搜索和点击定位；当前可手动填写地址、经纬度。
          </div>
        </div>

        <el-form-item prop="content" label="正文">
          <div class="editor-container">
            <Toolbar
              style="border-bottom: 1px solid var(--border-color)"
              :editor="editorRef"
              :defaultConfig="toolbarConfig"
              :mode="mode"
            />
            <Editor
              style="height: 500px; overflow-y: hidden;"
              v-model="postForm.content"
              :defaultConfig="editorConfig"
              :mode="mode"
              @onCreated="handleCreated"
            />
          </div>
        </el-form-item>

        <div class="form-actions mt-20">
          <el-button @click="$router.back()">取 消</el-button>
          <el-button type="primary" :loading="loading" @click="submitPost">保 存</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css'
import { nextTick, onBeforeUnmount, onMounted, reactive, ref, shallowRef } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { Plus } from '@element-plus/icons-vue'
import { ElMessage } from 'element-plus'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'
import { getPostEditDetail, updatePost } from '@/api/post'
import { useUserStore } from '@/stores/user'
import { hasAmapConfig, loadAmap } from '@/utils/amap'

const route = useRoute()
const router = useRouter()
const userStore = useUserStore()

const postId = route.params.id
const postFormRef = ref(null)
const loading = ref(false)
const pageLoading = ref(true)
const categories = ref([])
const allTags = ref([])
const cityOptions = ref([])
const citySelection = ref(null)
const amapContainerRef = ref(null)
const amapAvailable = ref(hasAmapConfig())
let AMapApi = null
let amapMap = null
let amapMarker = null
let amapGeocoder = null
let amapPlaceSearch = null
let searchCitiesFn = null

async function getSearchCities() {
  if (!searchCitiesFn) {
    const mod = await import('@/data/cities')
    searchCitiesFn = mod.searchCities
  }
  return searchCitiesFn
}

const postForm = reactive({
  title: '',
  coverImage: '',
  summary: '',
  categoryId: null,
  tagIds: [],
  content: '',
  locationName: null,
  address: '',
  placeId: '',
  latitude: null,
  longitude: null
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 200, message: '长度在 5 到 200 个字符', trigger: 'blur' }
  ],
  coverImage: [
    { required: true, message: '必须上传背景图', trigger: 'change' }
  ],
  summary: [
    { required: true, message: '请输入一段介绍', trigger: 'blur' },
    { max: 500, message: '长度不能超过 500 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入正文内容', trigger: 'blur' }
  ]
}

const uploadHeaders = {
  Authorization: `Bearer ${userStore.token}`
}

const handleCoverSuccess = (res) => {
  if (res.code === 200) {
    postForm.coverImage = res.data.url
    ElMessage.success('背景图上传成功')
  } else {
    ElMessage.error(res.message || '上传失败')
  }
}

const beforeCoverUpload = (file) => {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件！')
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB！')
  }
  return isImage && isLt5M
}

const onCitySearch = async (query) => {
  if (!query?.trim()) {
    cityOptions.value = []
    return
  }
  const searchCities = await getSearchCities()
  cityOptions.value = searchCities(query)
}

const onCitySelect = (city) => {
  if (city && typeof city === 'object') {
    postForm.latitude = city.lat
    postForm.longitude = city.lng
    postForm.locationName = city.nameZh
    postForm.address = postForm.address || city.nameZh
    syncAmapMarker([city.lng, city.lat])
  } else {
    postForm.latitude = null
    postForm.longitude = null
    postForm.locationName = null
  }
}

const searchAmapAddress = async (query, callback) => {
  if (!query?.trim() || !amapPlaceSearch) {
    callback([])
    return
  }

  amapPlaceSearch.search(query.trim(), (status, result) => {
    if (status !== 'complete') {
      callback([])
      return
    }

    const pois = result?.poiList?.pois || []
    callback(pois
      .filter(item => item.location)
      .slice(0, 8)
      .map(item => ({
        value: item.address ? `${item.name} - ${item.address}` : item.name,
        poi: item
      })))
  })
}

const selectAmapAddress = (item) => {
  const poi = item?.poi
  if (!poi?.location) {
    return
  }

  const lng = poi.location.lng
  const lat = poi.location.lat
  postForm.locationName = poi.cityname || poi.adname || poi.name
  postForm.address = poi.address ? `${poi.name} - ${poi.address}` : poi.name
  postForm.placeId = poi.id || ''
  postForm.longitude = lng
  postForm.latitude = lat
  syncAmapMarker([lng, lat])
}

function syncAmapMarker(position) {
  if (!AMapApi || !amapMap || !position?.length) {
    return
  }

  if (!amapMarker) {
    amapMarker = new AMapApi.Marker({
      position,
      draggable: true
    })
    amapMarker.on('dragend', event => applyAmapPosition(event.lnglat))
    amapMap.add(amapMarker)
  } else {
    amapMarker.setPosition(position)
  }

  amapMap.setCenter(position)
  amapMap.setZoom(Math.max(amapMap.getZoom(), 15))
}

function applyAmapPosition(lnglat) {
  const lng = typeof lnglat.getLng === 'function' ? lnglat.getLng() : lnglat.lng
  const lat = typeof lnglat.getLat === 'function' ? lnglat.getLat() : lnglat.lat
  postForm.longitude = lng
  postForm.latitude = lat
  syncAmapMarker([lng, lat])

  if (!amapGeocoder) {
    return
  }

  amapGeocoder.getAddress([lng, lat], (status, result) => {
    const regeocode = result?.regeocode
    if (status !== 'complete' || !regeocode) {
      return
    }
    const addressComponent = regeocode.addressComponent || {}
    postForm.address = regeocode.formattedAddress || postForm.address
    postForm.locationName = addressComponent.city || addressComponent.province || addressComponent.district || postForm.locationName
    postForm.placeId = ''
  })
}

async function initAmapPicker() {
  if (!hasAmapConfig()) {
    amapAvailable.value = false
    return
  }

  try {
    AMapApi = await loadAmap()
    await nextTick()
    if (!amapContainerRef.value) {
      return
    }

    const initialPosition = postForm.longitude && postForm.latitude
      ? [postForm.longitude, postForm.latitude]
      : [116.397428, 39.90923]

    amapMap = new AMapApi.Map(amapContainerRef.value, {
      viewMode: '3D',
      zoom: postForm.longitude && postForm.latitude ? 15 : 4,
      center: initialPosition
    })
    amapMap.addControl(new AMapApi.Scale())
    amapMap.addControl(new AMapApi.ToolBar())
    amapMap.on('click', event => applyAmapPosition(event.lnglat))

    amapGeocoder = new AMapApi.Geocoder()
    amapPlaceSearch = new AMapApi.PlaceSearch({ city: '全国' })

    if (postForm.longitude && postForm.latitude) {
      syncAmapMarker([postForm.longitude, postForm.latitude])
    }
  } catch (error) {
    console.warn('Amap picker init failed:', error)
    amapAvailable.value = false
  }
}

const mode = 'default'
const editorRef = shallowRef()
const toolbarConfig = {
  excludeKeys: ['fullScreen', 'video']
}
const editorConfig = {
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/files/upload',
      fieldName: 'file',
      maxFileSize: 5 * 1024 * 1024,
      headers: {
        Authorization: `Bearer ${userStore.token}`
      },
      customInsert(res, insertFn) {
        if (res.code === 200) {
          insertFn(res.data.url, '', res.data.url)
        } else {
          ElMessage.error(res.message || '上传失败')
        }
      }
    }
  }
}

const handleCreated = (editor) => {
  editorRef.value = editor
}

onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor != null) {
    editor.destroy()
  }
  if (amapMap) {
    amapMap.destroy()
    amapMap = null
  }
})

async function fillCitySelection(post) {
  if (!post.locationName || post.latitude == null || post.longitude == null) {
    citySelection.value = null
    cityOptions.value = []
    return
  }

  const searchCities = await getSearchCities()
  const matchedCity = searchCities(post.locationName).find(city => city.nameZh === post.locationName || city.name === post.locationName)
  const city = matchedCity || {
    name: post.locationName,
    nameZh: post.locationName,
    lat: post.latitude,
    lng: post.longitude
  }
  citySelection.value = city
  cityOptions.value = [city]
}

async function fetchData() {
  pageLoading.value = true
  try {
    const [catRes, tagRes, postRes] = await Promise.all([
      getCategories(),
      getTags({ page: 1, size: 500 }),
      getPostEditDetail(postId)
    ])

    categories.value = catRes.data
    allTags.value = tagRes.data

    const post = postRes.data
    postForm.title = post.title
    postForm.coverImage = post.coverImage
    postForm.summary = post.summary
    postForm.categoryId = post.categoryId
    postForm.tagIds = (post.tags || []).map(tag => tag.id)
    postForm.content = post.content
    postForm.locationName = post.locationName
    postForm.address = post.address || ''
    postForm.placeId = post.placeId || ''
    postForm.latitude = post.latitude
    postForm.longitude = post.longitude
    await fillCitySelection(post)
  } finally {
    pageLoading.value = false
  }
}

function buildSubmitData() {
  const finalTagIds = []
  const finalNewTags = []

  postForm.tagIds.forEach(item => {
    if (typeof item === 'number') {
      finalTagIds.push(item)
    } else {
      finalNewTags.push(item)
    }
  })

  return {
    title: postForm.title,
    coverImage: postForm.coverImage,
    summary: postForm.summary,
    categoryId: postForm.categoryId,
    content: postForm.content,
    tagIds: finalTagIds,
    newTags: finalNewTags,
    locationName: postForm.locationName || null,
    address: postForm.address || null,
    placeId: postForm.placeId || null,
    latitude: postForm.latitude ?? null,
    longitude: postForm.longitude ?? null
  }
}

function submitPost() {
  postFormRef.value.validate(async (valid) => {
    if (!valid) {
      return
    }
    if (postForm.content.trim() === '' || postForm.content === '<p><br></p>') {
      ElMessage.warning('内容不能为空')
      return
    }

    loading.value = true
    try {
      await updatePost(postId, buildSubmitData())
      ElMessage.success('修改成功')
      router.push(`/post/${postId}`)
    } finally {
      loading.value = false
    }
  })
}

onMounted(async () => {
  await fetchData()
  initAmapPicker()
})
</script>

<style scoped>
.post-create-container {
  max-width: 1000px;
  margin: 0 auto;
}

.main-editor-box {
  padding: 30px;
  border-radius: var(--radius-lg);
  min-height: 800px;
}

.page-title {
  margin-bottom: 25px;
  font-family: 'Space Grotesk', 'Inter', sans-serif;
  font-size: 1.5rem;
  color: var(--text-primary);
}

.w-full {
  width: 100%;
}

.editor-container {
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  z-index: 10;
  width: 100%;
  overflow: hidden;
  background: rgba(3, 8, 18, 0.72);
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
}

.location-hint {
  margin-top: 6px;
  font-size: 0.78rem;
  color: var(--text-secondary);
  letter-spacing: 0.5px;
}

.amap-picker {
  margin-bottom: 18px;
}

.amap-container,
.amap-fallback {
  width: 100%;
  height: 260px;
  border: 1px solid var(--border-color);
  border-radius: var(--radius-md);
  overflow: hidden;
}

.amap-fallback {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 20px;
  color: var(--text-secondary);
  background: rgba(3, 8, 18, 0.5);
  text-align: center;
}

:deep(.cover-uploader .el-upload) {
  border: 1px dashed var(--border-color);
  border-radius: var(--radius-md);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--transition);
  width: 300px;
  height: 168px;
  display: flex;
  justify-content: center;
  align-items: center;
  background:
    radial-gradient(circle at 78% 22%, rgba(77, 216, 255, 0.24), transparent 42%),
    linear-gradient(135deg, rgba(12, 39, 66, 0.9), rgba(37, 18, 86, 0.85));
}

:deep(.cover-uploader .el-upload:hover) {
  border-color: var(--primary-color);
  box-shadow: var(--glow-cyan);
}

.cover-uploader-icon {
  font-size: 28px;
  color: var(--primary-color);
}

.cover-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
  display: block;
}

.mt-20 {
  margin-top: 20px;
}
</style>
