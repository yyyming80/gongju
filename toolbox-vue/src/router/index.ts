import { createRouter, createWebHistory } from 'vue-router'
import AgentLogin from '../views/customer/AgentLogin.vue'

// 路由配置
const routes = [
  // 根路径重定向
  { 
    path: '/', 
    redirect: '/customer/login' 
  },
  
  // 客服登录页
  { 
    path: '/customer/login', 
    component: AgentLogin 
  },
  
  // 工作台布局
  { 
    path: '/customer', 
    component: () => import('../views/customer/Dashboard.vue'),
    meta: { requiresAuth: true },
    children: [
      // 在线客服工作台（实时聊天）
      { 
        path: 'online', 
        component: () => import('../views/customer/OnlineService.vue')
      },
      // 会话管理（历史记录，只读）
      { 
        path: 'sessions', 
        component: () => import('../views/customer/SessionList.vue')
      },
      // 会话详情（历史记录，只读）
      { 
        path: 'session/:sessionNo', 
        component: () => import('../views/customer/SessionDetail.vue')
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

// 路由守卫
router.beforeEach((to, from, next) => {
  // 登录页直接通过
  console.log('路由跳转:', to.path)
  console.log('登录状态:', localStorage.getItem('agentInfo') ? '已登录' : '未登录')
  
  if (to.path === '/customer/login') {
    next()
    return
  }
  
  if (to.path.startsWith('/customer')) {
    const agentInfo = localStorage.getItem('agentInfo')
    console.log('检查登录状态')
    if (!agentInfo) {
      console.log('未登录，跳转登录页')
      next('/customer/login')
      return
    }
  }
  next()
})

export default router