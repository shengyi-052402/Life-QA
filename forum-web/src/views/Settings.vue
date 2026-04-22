<template>
  <div class="settings-page">
    <div class="glass-panel settings-card">
      <div class="settings-head">
        <div>
          <h1>账户设置</h1>
          <p>更新头像、昵称、个人简介和登录密码。</p>
        </div>
      </div>

      <div class="settings-section">
        <div class="section-title">
          <h2>个人资料</h2>
          <p>这些信息会展示在你的个人主页。</p>
        </div>

        <el-form
          ref="profileFormRef"
          :model="profileForm"
          :rules="profileRules"
          label-position="top"
          class="settings-form"
        >
          <el-form-item label="头像">
            <div class="avatar-block">
              <el-avatar :size="88" :src="profileForm.avatar || defaultAvatar" />
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
            <el-input v-model="profileForm.nickname" maxlength="30" show-word-limit />
          </el-form-item>

          <el-form-item label="个人简介" prop="bio">
            <el-input
              v-model="profileForm.bio"
              type="textarea"
              :rows="5"
              maxlength="200"
              show-word-limit
              placeholder="写一点介绍，让别人更快认识你。"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button @click="goToUserCenter">返回个人中心</el-button>
            <el-button type="primary" :loading="savingProfile" @click="submitProfile">保存资料</el-button>
          </div>
        </el-form>
      </div>

      <el-divider />

      <div class="settings-section">
        <div class="section-title">
          <h2>修改密码</h2>
          <p>修改成功后将退出当前登录状态，请重新登录。</p>
        </div>

        <el-form
          ref="passwordFormRef"
          :model="passwordForm"
          :rules="passwordRules"
          label-position="top"
          class="settings-form"
        >
          <el-form-item label="旧密码" prop="oldPassword">
            <el-input
              v-model="passwordForm.oldPassword"
              type="password"
              show-password
              placeholder="请输入当前密码"
            />
          </el-form-item>

          <el-form-item label="新密码" prop="newPassword">
            <el-input
              v-model="passwordForm.newPassword"
              type="password"
              show-password
              placeholder="请输入新密码"
            />
          </el-form-item>

          <el-form-item label="确认新密码" prop="confirmPassword">
            <el-input
              v-model="passwordForm.confirmPassword"
              type="password"
              show-password
              placeholder="请再次输入新密码"
            />
          </el-form-item>

          <div class="form-actions">
            <el-button type="primary" :loading="savingPassword" @click="submitPassword">更新密码</el-button>
          </div>
        </el-form>
      </div>
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
const profileFormRef = ref(null)
const passwordFormRef = ref(null)
const savingProfile = ref(false)
const savingPassword = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const profileForm = reactive({
  nickname: '',
  bio: '',
  avatar: ''
})

const passwordForm = reactive({
  oldPassword: '',
  newPassword: '',
  confirmPassword: ''
})

const validateConfirmPassword = (_, value, callback) => {
  if (!value) {
    callback(new Error('请再次输入新密码'))
    return
  }
  if (value !== passwordForm.newPassword) {
    callback(new Error('两次输入的新密码不一致'))
    return
  }
  callback()
}

const profileRules = {
  nickname: [
    { required: true, message: '请输入昵称', trigger: 'blur' },
    { min: 2, max: 30, message: '昵称长度需在 2 到 30 个字符之间', trigger: 'blur' }
  ],
  bio: [
    { max: 200, message: '个人简介不能超过 200 个字符', trigger: 'blur' }
  ]
}

const passwordRules = {
  oldPassword: [
    { required: true, message: '请输入旧密码', trigger: 'blur' }
  ],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, max: 20, message: '密码长度需在 6 到 20 个字符之间', trigger: 'blur' }
  ],
  confirmPassword: [
    { validator: validateConfirmPassword, trigger: 'blur' }
  ]
}

const uploadHeaders = computed(() => ({
  Authorization: `Bearer ${userStore.token}`
}))

watch(
  () => userStore.userInfo,
  (info) => {
    if (!info) return
    profileForm.nickname = info.nickname || ''
    profileForm.bio = info.bio || ''
    profileForm.avatar = info.avatar || ''
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
  profileForm.avatar = res.data.url
  ElMessage.success('头像上传成功')
}

function resetPasswordForm() {
  passwordForm.oldPassword = ''
  passwordForm.newPassword = ''
  passwordForm.confirmPassword = ''
  passwordFormRef.value?.clearValidate()
}

function goToUserCenter() {
  if (!userStore.userInfo?.id) return
  router.push(`/user/${userStore.userInfo.id}`)
}

async function submitProfile() {
  const valid = await profileFormRef.value.validate().catch(() => false)
  if (!valid) return

  savingProfile.value = true
  try {
    await userStore.updateProfile({
      nickname: profileForm.nickname.trim(),
      bio: profileForm.bio.trim(),
      avatar: profileForm.avatar
    })
    ElMessage.success('资料已更新')
  } finally {
    savingProfile.value = false
  }
}

async function submitPassword() {
  const valid = await passwordFormRef.value.validate().catch(() => false)
  if (!valid) return
  if (passwordForm.oldPassword === passwordForm.newPassword) {
    ElMessage.warning('新密码不能与旧密码相同')
    return
  }

  savingPassword.value = true
  try {
    await userStore.changePassword({
      oldPassword: passwordForm.oldPassword,
      newPassword: passwordForm.newPassword
    })
    ElMessage.success('密码修改成功，请重新登录')
    userStore.logout()
    resetPasswordForm()
    router.push('/login')
  } finally {
    savingPassword.value = false
  }
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

.settings-head h1,
.section-title h2 {
  margin: 0;
}

.settings-head h1 {
  font-size: 1.7rem;
}

.settings-head p,
.section-title p,
.upload-tip {
  color: var(--text-secondary);
}

.settings-head p,
.section-title p {
  margin: 8px 0 0;
}

.settings-section + .settings-section {
  margin-top: 4px;
}

.section-title {
  margin-bottom: 20px;
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
  font-size: 0.85rem;
}

.form-actions {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 24px;
}

@media (max-width: 768px) {
  .settings-card {
    padding: 22px;
  }

  .avatar-block {
    flex-direction: column;
    align-items: flex-start;
  }

  .form-actions {
    flex-direction: column;
  }
}
</style>
