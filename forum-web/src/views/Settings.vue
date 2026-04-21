<template>
  <div class="settings-page">
    <div class="glass-panel settings-card">
      <div class="settings-head">
        <div>
          <h1>账号设置</h1>
          <p>更新头像、昵称和个人简介。</p>
        </div>
      </div>

      <el-form
        ref="formRef"
        :model="form"
        :rules="rules"
        label-position="top"
        class="settings-form"
      >
        <el-form-item label="头像">
          <div class="avatar-block">
            <el-avatar :size="88" :src="form.avatar || defaultAvatar" />
            <div class="avatar-actions">
              <el-upload
                action="/api/files/upload"
                :headers="uploadHeaders"
                :show-file-list="false"
                :before-upload="beforeAvatarUpload"
                :on-success="handleAvatarSuccess"
              >
                <el-button type="primary" plain>上传新头像</el-button>
              </el-upload>
              <span class="upload-tip">支持 jpg / png / gif / webp，最大 5MB</span>
            </div>
          </div>
        </el-form-item>

        <el-form-item label="昵称" prop="nickname">
          <el-input v-model="form.nickname" maxlength="30" show-word-limit />
        </el-form-item>

        <el-form-item label="个人简介" prop="bio">
          <el-input
            v-model="form.bio"
            type="textarea"
            :rows="5"
            maxlength="200"
            show-word-limit
            placeholder="写一点介绍，让别人更快认识你。"
          />
        </el-form-item>

        <div class="form-actions">
          <el-button @click="router.push(`/user/${userStore.userInfo?.id}`)">返回个人中心</el-button>
          <el-button type="primary" :loading="saving" @click="submit">保存修改</el-button>
        </div>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { computed, reactive, ref, watch } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/stores/user'

const router = useRouter()
const userStore = useUserStore()
const formRef = ref(null)
const saving = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const form = reactive({
  nickname: '',
  bio: '',
  avatar: ''
})

const rules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 30, message: '昵称长度需在 2 到 30 个字符之间', trigger: 'blur' }
  ],
  bio: [
    { max: 200, message: '个人简介不能超过 200 个字符', trigger: 'blur' }
  ]
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

watch(
  () => userStore.userInfo,
  (info) => {
    if (!info) return
    form.nickname = info.nickname || ''
    form.bio = info.bio || ''
    form.avatar = info.avatar || ''
  },
  { immediate: true }
)

function beforeAvatarUpload(file) {
  const isImage = file.type.startsWith('image/')
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImage) {
    ElMessage.error('只能上传图片文件')
  }
  if (!isLt5M) {
    ElMessage.error('图片大小不能超过 5MB')
  }
  return isImage && isLt5M
}

function handleAvatarSuccess(res) {
  if (res.code !== 200) {
    ElMessage.error(res.message || '上传失败')
    return
  }
  form.avatar = res.data.url
  ElMessage.success('头像上传成功')
}

function submit() {
  formRef.value.validate(async (valid) => {
    if (!valid) return

    saving.value = true
    try {
      await userStore.updateProfile({
        nickname: form.nickname.trim(),
        bio: form.bio.trim(),
        avatar: form.avatar
      })
      ElMessage.success('资料已更新')
    } finally {
      saving.value = false
    }
  })
}
</script>

<style scoped>
.settings-page {
  max-width: 880px;
  margin: 0 auto;
}

.settings-card {
  padding: 30px;
  border-radius: var(--radius-lg);
}

.settings-head h1 {
  margin: 0;
  font-size: 1.7rem;
}

.settings-head p {
  margin: 8px 0 0;
  color: var(--text-secondary);
}

.settings-form {
  margin-top: 28px;
}

.avatar-block {
  display: flex;
  align-items: center;
  gap: 20px;
}

.avatar-actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

.upload-tip {
  color: var(--text-secondary);
  font-size: 0.85rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .avatar-block {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
