<template>
  <div class="login-container">
    <div class="login-box glass-panel">
      <div class="login-header">
        <span class="auth-kicker">Life-Q&A</span>
        <h2 class="text-gradient">欢迎回到 Life-Q&A</h2>
        <p>登录后继续探索来自全球的思考与答案</p>
      </div>
      
      <el-form :model="loginForm" :rules="rules" ref="loginFormRef" label-width="0">
        <el-form-item prop="username">
          <el-input 
            v-model="loginForm.username" 
            placeholder="用户名" 
            :prefix-icon="User"
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item prop="password">
          <el-input 
            v-model="loginForm.password" 
            type="password" 
            placeholder="密码" 
            :prefix-icon="Lock"
            show-password
            size="large"
            @keyup.enter="handleLogin"
          />
        </el-form-item>
        
        <el-form-item>
          <el-button type="primary" class="login-btn" size="large" :loading="loading" @click="handleLogin">
            登 录
          </el-button>
        </el-form-item>
      </el-form>
      
      <div class="login-footer">
        <span>还没有账号？</span>
        <router-link to="/register">立即注册</router-link>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { User, Lock } from '@element-plus/icons-vue'
import { useUserStore } from '@/stores/user'
import { ElMessage } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loginFormRef = ref(null)
const loading = ref(false)

const loginForm = reactive({
  username: '',
  password: ''
})

const rules = reactive({
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
})

const handleLogin = () => {
  loginFormRef.value.validate(async (valid) => {
    if (valid) {
      loading.value = true
      try {
        const success = await userStore.login(loginForm)
        if (success) {
          ElMessage.success('登录成功')
          const redirect = route.query.redirect || '/'
          router.push(redirect)
        }
      } catch (error) {
        // 请求配置中已有通用错误提示
      } finally {
        loading.value = false
      }
    }
  })
}
</script>

<style scoped>
.login-container {
  height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 24px;
  background:
    radial-gradient(circle at 20% 20%, rgba(77, 216, 255, 0.22), transparent 26rem),
    radial-gradient(circle at 82% 72%, rgba(155, 124, 255, 0.2), transparent 24rem),
    var(--bg-color);
  overflow: hidden;
}

.login-container::before {
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

.login-box {
  width: min(420px, 100%);
  padding: 42px;
  border-radius: 24px;
  text-align: center;
}

.login-header {
  margin-bottom: 30px;
}

.login-header h2 {
  font-family: 'Space Grotesk', 'Inter', sans-serif;
  font-size: 1.75rem;
  margin-bottom: 8px;
}

.login-header p {
  color: var(--text-secondary);
  font-size: 0.9rem;
}

.login-btn {
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

.login-footer {
  margin-top: 20px;
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.login-footer a {
  color: var(--primary-color);
  font-weight: 500;
}

@media (max-width: 480px) {
  .login-box {
    padding: 28px 22px;
  }
}
</style>
