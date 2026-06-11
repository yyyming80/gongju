<template>
  <div class="session-detail">
    <!-- 头部 -->
    <div class="detail-header">
      <el-button icon="el-icon-arrow-left" @click="goBack">返回</el-button>
      <h3>会话详情</h3>
      <div class="header-info">
        <el-tag :type="getStatusType(session?.status)">{{ getStatusText(session?.status) }}</el-tag>
      </div>
    </div>
    
    <!-- 会话信息 -->
    <el-card class="info-card" v-if="session">
      <div class="info-grid">
        <div class="info-item">
          <span class="label">会话编号：</span>
          <span class="value">{{ session.session_no }}</span>
        </div>
        <div class="info-item">
          <span class="label">用户：</span>
          <span class="value">{{ session.user_nickname || '游客' }}</span>
        </div>
        <div class="info-item">
          <span class="label">接待客服：</span>
          <span class="value">{{ session.agent_nickname || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">开始时间：</span>
          <span class="value">{{ formatTime(session.start_time) }}</span>
        </div>
        <div class="info-item">
          <span class="label">结束时间：</span>
          <span class="value">{{ formatTime(session.end_time) || '-' }}</span>
        </div>
        <div class="info-item">
          <span class="label">会话时长：</span>
          <span class="value">{{ formatDuration(session.duration) }}</span>
        </div>
        <div class="info-item" v-if="session.rating">
          <span class="label">用户评分：</span>
          <el-rate v-model="session.rating" disabled text-color="#ff9900" />
        </div>
      </div>
    </el-card>
    
    <!-- 聊天记录 -->
    <el-card class="chat-card">
      <div slot="header" class="chat-header">
        <span>聊天记录</span>
        <span class="message-count">共 {{ messages.length }} 条消息</span>
      </div>
      
      <div class="message-list" ref="messageListRef">
        <div v-if="messages.length === 0" class="empty-messages">
          暂无聊天记录
        </div>
        
        <div 
          v-for="msg in messages" 
          :key="msg.msg_id"
          :class="['message', getMessageClass(msg.sender_type)]">
          <div class="message-avatar">
            {{ getAvatar(msg.sender_nickname) }}
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ msg.sender_nickname || '未知' }}</span>
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
    </el-card>
  </div>
</template>

<script>
import axios from 'axios'

export default {
  name: 'SessionDetail',
  data() {
    return {
      sessionNo: '',
      session: null,
      messages: [],
      loading: false
    }
  },
  mounted() {
    this.sessionNo = this.$route.params.sessionNo
    this.loadSessionDetail()
    this.loadMessages()
  },
  methods: {
    async loadSessionDetail() {
      try {
        const response = await axios.get(`/api/customer/session/${this.sessionNo}`)
        if (response.data.code === 200) {
          this.session = response.data.data
        }
      } catch (error) {
        console.error('加载会话详情失败', error)
        this.$message.error('加载会话详情失败')
      }
    },
    
    async loadMessages() {
      this.loading = true
      try {
        const response = await axios.get(`/api/customer/messages/${this.sessionNo}`)
        if (response.data.code === 200) {
          this.messages = response.data.data || []
          this.$nextTick(() => {
            this.scrollToBottom()
          })
        }
      } catch (error) {
        console.error('加载消息失败', error)
        this.$message.error('加载消息失败')
      } finally {
        this.loading = false
      }
    },
    
    goBack() {
      this.$router.push('/customer/sessions')
    },
    
    getMessageClass(senderType) {
      const classes = {
        1: 'message-user',
        2: 'message-ai',
        3: 'message-agent',
        4: 'message-system'
      }
      return classes[senderType] || ''
    },
    
    getStatusType(status) {
      const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '' }
      return types[status] || ''
    },
    
    getStatusText(status) {
      const texts = { 0: 'AI接待', 1: '等待转人工', 2: '人工接待', 3: '已结束' }
      return texts[status] || '未知'
    },
    
    getAvatar(name) {
      return name ? name.charAt(0).toUpperCase() : '?'
    },
    
    formatTime(time) {
      if (!time) return '-'
      const date = new Date(time)
      return date.toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit'
      })
    },
    
    formatDuration(seconds) {
      if (!seconds) return '进行中'
      const minutes = Math.floor(seconds / 60)
      const secs = seconds % 60
      return `${minutes}分${secs}秒`
    },
    
    scrollToBottom() {
      if (this.$refs.messageListRef) {
        this.$refs.messageListRef.scrollTop = this.$refs.messageListRef.scrollHeight
      }
    }
  }
}
</script>

<style scoped>
.session-detail {
  padding: 20px;
  height: calc(100vh - 60px);
  overflow-y: auto;
  background: #f5f5f5;
}

.detail-header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}

.detail-header h3 {
  margin: 0;
  flex: 1;
}

.info-card {
  margin-bottom: 20px;
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 15px;
}

.info-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.info-item .label {
  color: #666;
  font-size: 13px;
}

.info-item .value {
  color: #333;
  font-size: 13px;
}

.chat-card {
  min-height: 400px;
}

.chat-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.message-count {
  font-size: 12px;
  color: #999;
}

.message-list {
  max-height: 500px;
  overflow-y: auto;
  padding: 10px;
}

.empty-messages {
  text-align: center;
  padding: 50px;
  color: #999;
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

.message-system .message-content {
  text-align: center;
}

.message-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #409EFF;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 16px;
  flex-shrink: 0;
}

.message-agent .message-avatar {
  background: #67C23A;
}

.message-ai .message-avatar {
  background: #909399;
}

.message-content {
  max-width: 70%;
  margin: 0 10px;
}

.message-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}

.message-user .message-header {
  flex-direction: row-reverse;
}

.sender-name {
  font-size: 12px;
  color: #666;
  font-weight: 500;
}

.message-time {
  font-size: 11px;
  color: #999;
}

.message-body {
  padding: 10px 15px;
  border-radius: 10px;
  line-height: 1.6;
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
}
</style>