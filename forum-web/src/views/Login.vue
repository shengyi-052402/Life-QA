<template>
  <div class="login-container">
    <div class="login-box glass-panel">
      <div class="login-header">
        <h2 class="text-gradient">欢迎回到开发者论坛</h2>
        <p>登录以继续</p>
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
  background: var(--bg-color);
  background-image: radial-gradient(var(--border-color) 1px, transparent 1px);
  background-size: 20px 20px;
}

.login-box {
  width: 400px;
  padding: 40px;
  border-radius: var(--radius-lg);
  text-align: center;
}

.login-header {
  margin-bottom: 30px;
}

.login-header h2 {
  font-size: 1.5rem;
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

.login-footer {
  margin-top: 20px;
  font-size: 0.9rem;
  color: var(--text-secondary);
}

.login-footer a {
  color: var(--primary-color);
  font-weight: 500;
}
</style>
