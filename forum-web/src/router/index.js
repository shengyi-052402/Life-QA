import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/',
    component: () => import('@/components/layout/BaseLayout.vue'),
    children: [
      {
        path: '',
        name: 'Home',
        component: () => import('@/views/Home.vue'),
        meta: { title: '首页' }
      },
      {
        path: 'post/create',
        name: 'PostCreate',
        component: () => import('@/views/post/PostCreate.vue'),
        meta: { title: '发布新帖' }
      },
      {
        path: 'post/:id',
        name: 'PostDetail',
        component: () => import('@/views/post/PostDetail.vue'),
        meta: { title: '帖子详情' }
      }
    ]
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/Register.vue'),
    meta: { title: '注册' }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 全局路由守卫
router.beforeEach((to, from, next) => {
  // 设置页面标题
  if (to.meta.title) {
    document.title = `${to.meta.title} - 开发者论坛`
  }
  
  const hasToken = getToken()
  
  // 需要鉴权的页面名单
  const authRoutes = ['/settings', '/post/create']
  
  if (authRoutes.includes(to.path) && !hasToken) {
    next(`/login?redirect=${to.path}`)
  } else {
    next()
  }
})

export default router
