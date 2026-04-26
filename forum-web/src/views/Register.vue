<template>
  <div class="register-container">
    <div class="register-box glass-panel">
      <div class="register-header">
        <span class="auth-kicker">Life Q&A</span>
        <h2 class="text-gradient">创建你的问答坐标</h2>
        <p>注册账号，发布问题、收藏灵感、加入讨论</p>
      </div>
      
      <el-form :model="registerForm" :rules="rules" ref="registerFormRef" label-width="0">
        <el-form-item prop="username">
          <el-input 
            v-model="registerForm.username" 
            placeholder="用户名 (3-50字符)" 
            :prefix-icon="User"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="email">
          <el-input 
            v-model="registerForm.email" 
            placeholder="邮箱" 
            :prefix-icon="Message"
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="registerForm.password" 
            type="password" 
            placeholder="密码 (6-20字符)" 
            :prefix-icon="Lock"
            show-password
            size="large"
          />
        </el-form-item>
        
        <el-form-item prop="confirmPassword">
          <el-input 
            v-model="registerForm.confirmPassword" 
            type="password" 
            placeholder="确认密码" 
            :prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleRegister"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" class="register-btn" size="large" :loading="loading" @click="handleRegister">
            注 册
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="register-footer">
        <span>已有账号？</span>
        <router-link to="/login">直接登录</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { User, Lock, Message } from '@element-plus/icons-vue'
import { register } from '@/api/auth'
import { ElMessage } from 'element-plus'

const router = useRouter()
const registerFormRef = ref(null)
const loading = ref(false)

const registerForm = reactive({
  username: '',
  email: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = (rule, value, callback) => {
  if (value === '') {
    callback(new Error('请再次输入密码'))
  } else if (value !== registerForm.password) {
    callback(new Error('两次输入密码不一致!'))
  } else {
    callback()
  }
}

const rules = reactive({
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { min: 3, max: 50, message: '长度在 3 到 50 个字符', trigger: 'blur' }
  ],
  email: [
    { required: true, message: '请输入邮箱地址', trigger: 'blur' },
    { type: 'email', message: '请输入正确的邮箱地址', trigger: ['blur', 'change'] }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, max: 20, message: '长度在 6 到 20 个字符', trigger: 'blur' }
  ],
  confirmPassword: [
    { required: true, validator: validateConfirmPassword, trigger: 'blur' }
  ]
})

const handleRegister = () => {
  registerFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const { username, email, password } = registerForm
        await register({ username, email, password })
        ElMessage.success('注册成功，请登录')
        router.push('/login')
      } catch (error) {
        // 请求通用配置已处理异常提示
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.register-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 18% 78%, rgba(77, 216, 255, 0.2), transparent 26rem),
    radial-gradient(circle at 78% 18%, rgba(155, 124, 255, 0.22), transparent 24rem),
    var(--bg-color);
  overflow: hidden;
}

.register-container::before {
  content: '';
  position: fixed;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(126, 214, 255, 0.06) 1px, transparent 1px),
    linear-gradient(90deg, rgba(126, 214, 255, 0.06) 1px, transparent 1px);
  background-size: 42px 42px;
  transform: perspective(700px) rotateX(58deg) translateY(18%);
  transform-origin: center bottom;
}

.register-box {
  width: min(440px, 100%);
  padding: 42px;
  border-radius: 24px;
  text-align: center;
}

.register-header {
  margin-bottom: 30px;
}

.register-header h2 {
  font-family: 'Space Grotesk', 'Inter', sans-serif;
  font-size: 1.75rem;
  margin-bottom: 8px;
}

.register-header p {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.register-btn {
  width: 100%;
  border-radius: var(--radius-md);
}

.auth-kicker {
  display: inline-block;
  margin-bottom: 10px;
  color: var(--primary-color);
  font-family: 'Space Grotesk', 'Inter', sans-serif;
  font-size: 0.82rem;
  font-weight: 800;
  letter-spacing: 0.14em;
  text-transform: uppercase;
}

.register-footer {
  margin-top: 20px;
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.register-footer a {
  color: var(--primary-color);
  font-weight: 500;
}

@media (max-width: 480px) {
  .register-box {
    padding: 28px 22px;
  }
}
</style>
