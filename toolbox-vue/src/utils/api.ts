import axios from 'axios'
import { ElMessage } from 'element-plus'
import router from '../router'

const api = axios.create({
  baseURL: '/api',
  timeout: 30000
})

api.interceptors.request.use(config => {
  const token = localStorage.getItem('token')
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  response => response.data,
  error => {
    if (error.response?.status === 401) {
      localStorage.removeItem('token')
      router.push('/login')
    }
    ElMessage.error(error.response?.data?.message || '请求失败')
    return Promise.reject(error)
  }
)

// 客服相关API
export const customerApi = {
  // 登录
  login: (username, password) => api.post('/customer/login', { username, password }),
  
  // 会话列表
  getSessions: (params) => api.get('/customer/sessions', { params }),
  
  // 等待中的会话
  getWaitingSessions: () => api.get('/customer/sessions/waiting'),
  
  // 会话详情
  getSession: (sessionNo) => api.get(`/customer/session/${sessionNo}`),
  
  // 消息列表
  getMessages: (sessionNo, params) => api.get(`/customer/messages/${sessionNo}`, { params }),
  
  // 开始会话
  startSession: (data) => api.post('/customer/session/start', data),
  
  // 结束会话
  endSession: (data) => api.post('/customer/session/end', data),
  
  // 转人工
  transfer: (data) => api.post('/customer/transfer', data),
  
  // 客服列表
  getAgents: () => api.get('/customer/agents'),
  
  // 在线客服
  getOnlineAgents: () => api.get('/customer/agents/online'),
  
  // 更新客服状态
  updateAgentStatus: (data) => api.post('/customer/agent/status', data),
  
  // 统计数据
  getStatistics: () => api.get('/customer/statistics'),
  
  // 接受会话
  acceptSession: (data) => api.post('/customer/session/accept', data)
}

export default api
