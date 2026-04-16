<template>
  <div class="post-create-container">
    <div class="glass-panel main-editor-box">
      <h2 class="page-title">发布新帖</h2>
      
      <el-form :model="postForm" :rules="rules" ref="postFormRef" label-position="top">
        <el-form-item prop="title" label="标题">
          <el-input v-model="postForm.title" placeholder="请输入帖子标题 (简明扼要)" size="large" maxlength="200" show-word-limit />
        </el-form-item>
        
        <el-row :gutter="20">
          <el-col :span="12">
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
          <el-col :span="12">
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
                @change="handleTagChange"
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
        </el-row>
        
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
              @onChange="handleChange"
            />
          </div>
        </el-form-item>
        
        <div class="form-actions mt-20">
          <el-button @click="$router.back()">取 消</el-button>
          <el-button type="primary" :loading="loading" @click="submitPost">发 布</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import '@wangeditor/editor/dist/css/style.css' // 引入 css
import { onBeforeUnmount, ref, reactive, shallowRef, onMounted } from 'vue'
import { Editor, Toolbar } from '@wangeditor/editor-for-vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { getCategories } from '@/api/category'
import { getTags } from '@/api/tag'
import { createPost } from '@/api/post'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()

// 表单相关
const postFormRef = ref(null)
const loading = ref(false)
const categories = ref([])
const allTags = ref([])

const postForm = reactive({
  title: '',
  categoryId: null,
  tagIds: [],
  newTags: [],
  content: ''
})

const rules = {
  title: [
    { required: true, message: '请输入标题', trigger: 'blur' },
    { min: 5, max: 200, message: '长度在 5 到 200 个字符', trigger: 'blur' }
  ],
  categoryId: [
    { required: true, message: '请选择分类', trigger: 'change' }
  ],
  content: [
    { required: true, message: '请输入正文内容', trigger: 'blur' }
  ]
}

// 富文本编辑器相关
const mode = 'default'
const editorRef = shallowRef()
const toolbarConfig = {
  excludeKeys: [
    'fullScreen', 'video' // 根据需要配置不需要的工具栏按钮
  ]
}
const editorConfig = { 
  placeholder: '请输入内容...',
  MENU_CONF: {
    uploadImage: {
      server: '/api/files/upload', // 后端上传图片的接口
      fieldName: 'file',
      maxFileSize: 5 * 1024 * 1024,
      headers: {
        Authorization: `Bearer ${userStore.token}`
      },
      customInsert(res, insertFn) {
        // res 是服务端的返回结果
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
  editorRef.value = editor // 记录 editor 实例，重要！
}

const handleChange = (editor) => {
  // editor onChange 触发校验或者什么逻辑
  // 如果空内容可以判定为 '<p><br></p>'
}

// 销毁编辑器
onBeforeUnmount(() => {
  const editor = editorRef.value
  if (editor == null) return
  editor.destroy()
})

const handleTagChange = (val) => {
  // val 可能是 tagId(Long) 或者是 newly created tag name(String)
  // 我们需要在提交时分离开
}

const fetchData = async () => {
  try {
    const [catRes, tagRes] = await Promise.all([
      getCategories(),
      getTags({ page: 1, size: 500 }) // 暂时拉取一页比较多作为备选
    ])
    categories.value = catRes.data
    allTags.value = tagRes.data.records
  } catch (error) {
    console.error(error)
  }
}

onMounted(() => {
  fetchData()
})

const submitPost = () => {
  postFormRef.value.validate(async (valid) => {
    if (valid) {
      if (postForm.content.trim() === '' || postForm.content === '<p><br></p>') {
        ElMessage.warning('内容不能为空')
        return
      }

      loading.value = true
      // 分离已有 tagId 和新 tagName
      const finalTagIds = []
      const finalNewTags = []
      
      postForm.tagIds.forEach(item => {
        if (typeof item === 'number') {
          finalTagIds.push(item)
        } else {
          finalNewTags.push(item)
        }
      })

      const submitData = {
        title: postForm.title,
        categoryId: postForm.categoryId,
        content: postForm.content,
        tagIds: finalTagIds,
        newTags: finalNewTags
      }

      try {
        const res = await createPost(submitData)
        ElMessage.success('发布成功')
        router.push(`/post/${res.data}`)
      } catch (error) {
        console.error(error)
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.post-create-container {
  max-width: 900px;
  margin: 0 auto;
}

.main-editor-box {
  padding: 30px;
  border-radius: var(--radius-lg);
  min-height: 800px;
}

.page-title {
  margin-bottom: 25px;
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
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 15px;
}
</style>
