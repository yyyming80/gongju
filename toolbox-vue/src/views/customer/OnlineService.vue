<template>
  <div class="online-service">
    <!-- 左侧会话列表 -->
    <div class="session-list">
      <div class="list-header">
        <h3>在线客服工作台</h3>
      </div>
      
      <!-- 等待接入 -->
      <div class="session-section">
        <div class="section-title">
          <span>等待接入</span>
          <el-badge :value="waitingSessions.length" :hidden="waitingSessions.length === 0" type="warning" />
        </div>
        <div class="session-items">
          <div 
            v-for="session in waitingSessions" 
            :key="session.session_no"
            :class="['session-item', { active: currentSession && currentSession.session_no === session.session_no }]"
            @click="selectSession(session)"
          >
            <div class="session-avatar">
              {{ getAvatar(session.user_nickname) }}
            </div>
            <div class="session-info">
              <div class="session-name">{{ session.user_nickname || '游客' }}</div>
              <div class="session-preview">{{ session.last_message || '暂无消息' }}</div>
            </div>
            <div class="session-time">
              {{ formatTime(session.transfer_time || session.create_time) }}
            </div>
          </div>
          <div v-if="waitingSessions.length === 0" class="empty-tip">
            暂无等待会话
          </div>
        </div>
      </div>
      
      <!-- 服务中 -->
      <div class="session-section">
        <div class="section-title">
          <span>服务中</span>
          <el-badge :value="servingSessions.length" :hidden="servingSessions.length === 0" type="success" />
        </div>
        <div class="session-items">
          <div 
            v-for="session in servingSessions" 
            :key="session.session_no"
            :class="['session-item', { active: currentSession && currentSession.session_no === session.session_no }]"
            @click="selectSession(session)"
          >
            <div class="session-avatar serving">
              {{ getAvatar(session.user_nickname) }}
            </div>
            <div class="session-info">
              <div class="session-name">
                {{ session.user_nickname || '游客' }}
                <el-tag size="mini" type="success">服务中</el-tag>
              </div>
              <div class="session-preview">{{ session.last_message || '暂无消息' }}</div>
            </div>
            <div class="session-unread" v-if="session.agent_unread_count > 0">
              {{ session.agent_unread_count }}
            </div>
          </div>
          <div v-if="servingSessions.length === 0" class="empty-tip">
            暂无服务中会话
          </div>
        </div>
      </div>
    </div>
    
    <!-- 右侧聊天窗口 -->
    <div class="chat-area">
      <!-- 未选择会话 -->
      <div v-if="!currentSession" class="no-session">
        <div class="no-session-content">
          <div class="no-session-icon">💬</div>
          <div class="no-session-text">请选择会话开始服务</div>
        </div>
      </div>
      
      <!-- 聊天界面 -->
      <div v-else class="chat-container">
        <!-- 聊天头部 -->
        <div class="chat-header">
          <div class="chat-user-info">
            <span class="user-name">{{ currentSession.user_nickname || '游客' }}</span>
            <el-tag :type="currentSession.status === 1 ? 'warning' : 'success'" size="small">
              {{ currentSession.status === 1 ? '等待接入' : '服务中' }}
            </el-tag>
          </div>
          <div class="chat-actions">
            <el-button v-if="currentSession.status === 1" type="primary" size="small" @click="acceptSession">
              接入会话
            </el-button>
            <el-button v-if="currentSession.status === 2" type="danger" size="small" @click="endSession">
              结束会话
            </el-button>
          </div>
        </div>
        
        <!-- 消息列表 -->
        <div class="message-list" ref="messageListRef">
          <div v-if="messages.length === 0" class="empty-messages">
            暂无消息记录
          </div>
          <div 
            v-for="msg in messages" 
            :key="msg.msg_id"
            :class="['message', getMessageClass(msg.sender_type)]"
          >
            <div class="message-avatar">
              {{ getAvatar(msg.sender_nickname) }}
            </div>
            <div class="message-content">
              <div class="message-header">
                <span class="sender-name">{{ msg.sender_nickname }}</span>
                <span class="message-time">{{ formatTime(msg.create_time) }}</span>
              </div>
              <div class="message-body">
                <div v-if="msg.sender_type === 4" class="system-message">
                  {{ msg.content }}
                </div>
                <div v-else class="normal-message">
                  {{ msg.content }}
                </div>
              </div>
            </div>
          </div>
        </div>
        
        <!-- 输入区域 -->
        <div class="input-area">
          <el-input
            v-model="messageContent"
            type="textarea"
            :rows="2"
            placeholder="输入消息..."
            @keyup.enter.ctrl="sendMessage"
            :disabled="currentSession.status !== 2"
          />
          <el-button type="primary" @click="sendMessage" :disabled="!messageContent.trim() || currentSession.status !== 2">
            发送
          </el-button>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'OnlineService',
  data() {
    return {
      waitingSessions: [],  // 等待接入的会话
      servingSessions: [], // 服务中的会话
      currentSession: null, // 当前选中的会话
      messages: [],        // 当前会话的消息
      messageContent: '',  // 输入的消息
      ws: null,           // WebSocket连接
      refreshTimer: null   // 刷新定时器
    }
  },
  mounted() {
    this.loadSessions()
    this.initWebSocket()
    // 定时刷新会话列表
    this.refreshTimer = setInterval(() => {
      this.loadSessions()
    }, 5000)
  },
  beforeDestroy() {
    if (this.refreshTimer) {
      clearInterval(this.refreshTimer)
    }
    if (this.ws) {
      this.ws.close()
    }
  },
  methods: {
    // 加载会话列表
    async loadSessions() {
      try {
        // 加载等待接入的会话
        const waitingRes = await axios.get('/api/customer/sessions/waiting')
        if (waitingRes.data.code === 200) {
          this.waitingSessions = waitingRes.data.data || []
        }
        
        // 加载服务中的会话（当前客服接待的）
        const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}')
        const allRes = await axios.get('/api/customer/sessions', { 
          params: { status: 2 }  // 状态2=服务中
        })
        if (allRes.data.code === 200) {
          this.servingSessions = allRes.data.data || []
        }
      } catch (error) {
        console.error('加载会话列表失败', error)
      }
    },
    
    // 选择会话
    async selectSession(session) {
      this.currentSession = session
      await this.loadMessages()
    },
    
    // 加载消息
    async loadMessages() {
      if (!this.currentSession) return
      
      try {
        const res = await axios.get(`/api/customer/messages/${this.currentSession.session_no}`)
        if (res.data.code === 200) {
          this.messages = res.data.data || []
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        console.error('加载消息失败', error)
      }
    },
    
    // 接入会话
    async acceptSession() {
      if (!this.currentSession) return
      
      try {
        const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}')
        const res = await axios.post('/api/customer/session/accept', {
          sessionNo: this.currentSession.session_no,
          agentId: agentInfo.id,
          agentNickname: agentInfo.nickname
        })
        
        if (res.data.code === 200) {
          this.$message.success('已接入会话')
          // 更新会话状态
          this.currentSession.status = 2
          // 重新加载会话列表
          await this.loadSessions()
          // 重新加载消息（包含欢迎消息）
          await this.loadMessages()
        }
      } catch (error) {
        console.error('接入会话失败', error)
        this.$message.error('接入会话失败')
      }
    },
    
    // 结束会话
    async endSession() {
      if (!this.currentSession) return
      
      try {
        await this.$confirm('确定要结束此会话吗？', '提示', {
          confirmButtonText: '确定',
          cancelButtonText: '取消',
          type: 'warning'
        })
        
        await axios.post('/api/customer/session/end', {
          sessionNo: this.currentSession.session_no
        })
        
        this.$message.success('会话已结束')
        this.currentSession = null
        this.messages = []
        await this.loadSessions()
      } catch (error) {
        if (error !== 'cancel') {
          console.error('结束会话失败', error)
        }
      }
    },
    
    // 发送消息
    async sendMessage() {
      console.log('sendMessage被调用')
      
      if (!this.messageContent.trim() || !this.currentSession) return
      if (this.currentSession.status !== 2) {
        this.$message.warning('请先接入会话')
        return
      }
      
      const content = this.messageContent.trim()
      this.messageContent = ''
      
      try {
        const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}')
        const msg = {
          action: 'send_message',
          sessionNo: this.currentSession.session_no,
          msgId: 'msg_' + Date.now(),
          content: content,
          senderId: agentInfo.id,
          senderNickname: agentInfo.nickname
        }
        
        // 诊断日志
        console.log('准备发送WebSocket消息', msg)
        console.log('WebSocket状态', this.ws ? this.ws.readyState : 'ws为null')
        console.log('发送消息', {
          sessionNo: this.currentSession.session_no,
          content: content
        })
        
        // WebSocket发送
        if (this.ws && this.ws.readyState === 1) {
          console.log('准备调用ws.send()')
          this.ws.send(JSON.stringify(msg))
          console.log('ws.send()执行完成')
        } else {
          console.warn('WebSocket未连接或状态异常', this.ws ? this.ws.readyState : 'ws为null')
        }
        
        // 本地显示消息
        this.messages.push({
          msg_id: msg.msgId,
          msg_type: 1,
          sender_type: 3,
          sender_id: agentInfo.id,
          sender_nickname: agentInfo.nickname,
          content: content,
          create_time: new Date()
        })
        
        this.$nextTick(() => {
          this.scrollToBottom()
        })
      } catch (error) {
        console.error('发送消息失败', error)
        this.$message.error('发送消息失败')
      }
    },
    
    // 初始化WebSocket
    initWebSocket() {
      const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}')
      const wsUrl = `ws://localhost:8080/ws/customer?userId=${agentInfo.id}&userType=agent`
      
      console.log('初始化WebSocket, URL:', wsUrl)
      
      this.ws = new WebSocket(wsUrl)
      
      this.ws.onopen = () => {
        console.log('========== WebSocket onopen ==========')
        console.log('WebSocket连接成功')
        console.log('readyState:', this.ws.readyState, '(1=已连接)')
        console.log('=====================================')
      }
      
      this.ws.onmessage = (event) => {
        console.log('========== WebSocket onmessage ==========')
        console.log('收到原始数据:', event.data)
        const data = JSON.parse(event.data)
        console.log('解析后数据:', data)
        console.log('type:', data.type)
        console.log('sender_type:', data.sender_type)
        console.log('content:', data.content)
        console.log('=========================================')
        
        // 用户发来的消息
        if (data.type === 'user' || data.sender_type === 1) {
          // 如果是当前会话的消息，更新显示
          if (this.currentSession && data.sessionNo === this.currentSession.session_no) {
            this.messages.push({
              msg_id: data.msg_id || 'msg_' + Date.now(),
              msg_type: 1,
              sender_type: 1,
              sender_id: data.senderId,
              sender_nickname: data.senderNickname,
              content: data.content,
              create_time: new Date(data.timestamp)
            })
            this.$nextTick(() => {
              this.scrollToBottom()
            })
          }
          // 刷新会话列表
          this.loadSessions()
        }
        
        // 新会话通知
        if (data.action === 'new_session') {
          this.$notify({
            title: '新会话',
            message: '有用户请求人工客服',
            type: 'warning'
          })
          this.loadSessions()
        }
      }
      
      this.ws.onerror = (error) => {
        console.error('========== WebSocket onerror ==========')
        console.error('WebSocket错误', error)
        console.error('======================================')
      }
      
      this.ws.onclose = () => {
        console.log('========== WebSocket onclose ==========')
        console.log('WebSocket连接关闭')
        console.log('======================================')
        // 3秒后重连
        setTimeout(() => {
          this.initWebSocket()
        }, 3000)
      }
    },
    
    // 获取消息样式类
    getMessageClass(senderType) {
      const classes = {
        1: 'message-user',
        2: 'message-ai',
        3: 'message-agent',
        4: 'message-system'
      }
      return classes[senderType] || ''
    },
    
    // 获取头像字母
    getAvatar(name) {
      return name ? name.charAt(0).toUpperCase() : '?'
    },
    
    // 格式化时间
    formatTime(time) {
      if (!time) return ''
      const date = new Date(time)
      const now = new Date()
      const diff = now - date
      
      if (diff < 60000) return '刚刚'
      if (diff < 3600000) return Math.floor(diff / 60000) + '分钟前'
      if (diff < 86400000) {
        return date.getHours().toString().padStart(2, '0') + ':' + 
               date.getMinutes().toString().padStart(2, '0')
      }
      return (date.getMonth() + 1) + '-' + date.getDate()
    },
    
    // 滚动到底部
    scrollToBottom() {
      if (this.$refs.messageListRef) {
        this.$refs.messageListRef.scrollTop = this.$refs.messageListRef.scrollHeight
      }
    }
  }
}
</script>

<style scoped>
.online-service {
  height: calc(100vh - 60px);
  display: flex;
  background: #f5f5f5;
}

.session-list {
  width: 300px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  display: flex;
  flex-direction: column;
}

.list-header {
  padding: 15px;
  border-bottom: 1px solid #e0e0e0;
}

.list-header h3 {
  margin: 0;
  font-size: 16px;
  color: #333;
}

.session-section {
  flex: 1;
  overflow-y: auto;
}

.section-title {
  padding: 10px 15px;
  font-size: 14px;
  color: #666;
  display: flex;
  align-items: center;
  gap: 8px;
  background: #f5f5f5;
}

.session-items {
  padding: 10px;
}

.session-item {
  display: flex;
  align-items: center;
  padding: 12px;
  border-radius: 8px;
  cursor: pointer;
  margin-bottom: 8px;
  transition: background 0.2s;
}

.session-item:hover {
  background: #f0f0f0;
}

.session-item.active {
  background: #e8f0fe;
}

.session-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 18px;
  margin-right: 10px;
}

.session-avatar.serving {
  background: #67C23A;
}

.session-info {
  flex: 1;
  overflow: hidden;
}

.session-name {
  font-size: 14px;
  font-weight: 500;
  color: #333;
  display: flex;
  align-items: center;
  gap: 6px;
}

.session-preview {
  font-size: 12px;
  color: #999;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
}

.session-time {
  font-size: 11px;
  color: #999;
}

.session-unread {
  background: #f56c6c;
  color: #fff;
  border-radius: 10px;
  padding: 2px 6px;
  font-size: 11px;
}

.empty-tip {
  text-align: center;
  padding: 20px;
  color: #999;
  font-size: 13px;
}

.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.no-session {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
}

.no-session-content {
  text-align: center;
}

.no-session-icon {
  font-size: 60px;
  margin-bottom: 15px;
}

.no-session-text {
  color: #999;
  font-size: 14px;
}

.chat-container {
  flex: 1;
  display: flex;
  flex-direction: column;
}

.chat-header {
  padding: 15px 20px;
  background: #fff;
  border-bottom: 1px solid #e0e0e0;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.chat-user-info {
  display: flex;
  align-items: center;
  gap: 10px;
}

.user-name {
  font-size: 16px;
  font-weight: 500;
}

.chat-actions {
  display: flex;
  gap: 10px;
}

.message-list {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}

.empty-messages {
  text-align: center;
  color: #999;
  padding: 50px;
}

.message {
  display: flex;
  margin-bottom: 20px;
}

.message-user {
  flex-direction: row-reverse;
}

.message-system {
  justify-content: center;
}

.message-avatar {
  width: 36px;
  height: 36px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 14px;
  margin: 0 10px;
}

.message-agent .message-avatar {
  background: #67C23A;
}

.message-content {
  max-width: 60%;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 5px;
}

.message-user .message-header {
  flex-direction: row-reverse;
}

.sender-name {
  font-size: 12px;
  color: #666;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-body {
  padding: 10px 15px;
  border-radius: 10px;
  line-height: 1.5;
}

.message-user .message-body {
  background: #409EFF;
  color: #fff;
}

.message-agent .message-body {
  background: #67C23A;
  color: #fff;
}

.message-ai .message-body {
  background: #f0f0f0;
  color: #333;
}

.system-message {
  background: #fff3e0;
  color: #666;
  padding: 8px 15px;
  border-radius: 8px;
  font-size: 13px;
  text-align: center;
}

.input-area {
  padding: 15px 20px;
  background: #fff;
  border-top: 1px solid #e0e0e0;
  display: flex;
  gap: 10px;
  align-items: flex-end;
}

.input-area .el-textarea {
  flex: 1;
}

.input-area .el-button {
  height: 60px;
}
</style>