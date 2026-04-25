import { createRouter, createWebHistory } from 'vue-router'
import { getToken } from '@/utils/auth'

const routes = [
  {
    path: '/',
    name: 'Landing',
    component: () => import('@/views/Landing.vue'),
    meta: { title: 'Welcome' }
  },
  {
    path: '/',
    component: () => import('@/components/layout/BaseLayout.vue'),
    children: [
      {
        path: 'explore',
        name: 'Explore',
        component: () => import('@/views/Home.vue'),
        meta: { title: '探索大厅' }
      },
      {
        path: 'post/create',
        name: 'PostCreate',
        component: () => import('@/views/post/PostCreate.vue'),
        meta: { title: '发布帖子', requiresAuth: true }
      },
      {
        path: 'post/edit/:id',
        name: 'PostEdit',
        component: () => import('@/views/post/PostEdit.vue'),
        meta: { title: '编辑帖子', requiresAuth: true }
      },
      {
        path: 'post/:id',
        name: 'PostDetail',
        component: () => import('@/views/post/PostDetail.vue'),
        meta: { title: '帖子详情' }
      },
      {
        path: 'search',
        name: 'Search',
        component: () => import('@/views/Search.vue'),
        meta: { title: '搜索' }
      },
      {
        path: 'category/:id',
        name: 'CategoryPosts',
        component: () => import('@/views/CategoryPosts.vue'),
        meta: { title: '分类帖子' }
      },
      {
        path: 'tag/:id',
        name: 'TagPosts',
        component: () => import('@/views/TagPosts.vue'),
        meta: { title: '标签帖子' }
      },
      {
        path: 'user/:id',
        name: 'UserCenter',
        component: () => import('@/views/UserCenter.vue'),
        meta: { title: '个人中心' }
      },
      {
        path: 'settings',
        name: 'Settings',
        component: () => import('@/views/Settings.vue'),
        meta: { title: '账号设置', requiresAuth: true }
      },
      {
        path: 'notifications',
        name: 'Notifications',
        component: () => import('@/views/Notifications.vue'),
        meta: { title: '通知中心', requiresAuth: true }
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

router.beforeEach((to, from, next) => {
  if (to.meta.title) {
    document.title = `${to.meta.title} - 开发者论坛`
  }

  const hasToken = getToken()
  const requiresAuth = to.matched.some(record => record.meta?.requiresAuth)

  if (requiresAuth && !hasToken) {
    next(`/login?redirect=${to.fullPath}`)
    return
  }

  next()
})

export default router
