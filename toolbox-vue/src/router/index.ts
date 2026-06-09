import { createRouter, createWebHistory } from 'vue-router'
import AgentLogin from '../views/customer/AgentLogin.vue'

// 路由守卫
const routes = [
  // 根路径直接跳转到客服登录
  { 
    path: '/', 
    redirect: '/customer/login' 
  },
  // 客服登录页
  { 
    path: '/customer/login', 
    component: AgentLogin 
  },
  // 客服工作台（需要登录）
  { 
    path: '/customer/dashboard', 
    component: () => import('../views/customer/Dashboard.vue'),
    meta: { requiresAuth: true }
  },
  // 会话列表
  { 
    path: '/customer/sessions', 
    component: () => import('../views/customer/SessionList.vue'),
    meta: { requiresAuth: true }
  },
  // 会话详情
  { 
    path: '/customer/session/:sessionNo', 
    component: () => import('../views/customer/SessionDetail.vue'),
    meta: { requiresAuth: true }
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫：检查是否已登录
router.beforeEach((to, from, next) => {
  if (to.meta.requiresAuth) {
    const agentInfo = localStorage.getItem('agentInfo')
    if (!agentInfo) {
      next('/customer/login')
      return
    }
  }
  next()
})

export default router