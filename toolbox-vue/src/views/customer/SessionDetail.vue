<template>
  <div class="session-detail">
    <div class="header">
      <el-button icon="el-icon-arrow-left" @click="goBack">返回</el-button>
      <h3>会话详情</h3>
      <div class="actions">
        <el-button v-if="session && session.status === 1" type="primary" @click="acceptSession">
          接受会话
        </el-button>
        <el-button v-if="session && session.status === 2" type="danger" @click="endSession">
          结束会话
        </el-button>
      </div>
    </div>

    <el-card class="session-info" v-if="session">
      <div class="info-grid">
        <div class="info-item">
          <span class="label">会话编号：</span>
          <span class="value">{{ session.session_no }}</span>
        </div>
        <div class="info-item">
          <span class="label">用户：</span>
          <span class="value">{{ session.user_nickname || '匿名用户' }}</span>
        </div>
        <div class="info-item">
          <span class="label">状态：</span>
          <el-tag :type="getStatusType(session.status)">{{ getStatusText(session.status) }}</el-tag>
        </div>
        <div class="info-item">
          <span class="label">接待客服：</span>
          <span class="value">{{ session.agent_nickname || '未分配' }}</span>
        </div>
        <div class="info-item">
          <span class="label">开始时间：</span>
          <span class="value">{{ formatTime(session.start_time) }}</span>
        </div>
        <div class="info-item">
          <span class="label">会话时长：</span>
          <span class="value">{{ formatDuration(session.duration) }}</span>
        </div>
      </div>
    </el-card>

    <el-card class="chat-area">
      <div class="messages" ref="messagesRef">
        <div v-for="msg in messages" :key="msg.msg_id" 
             :class="['message', getMessageClass(msg.sender_type)]">
          <div class="message-avatar">
            <img v-if="msg.sender_avatar" :src="msg.sender_avatar" alt="">
            <span v-else>{{ getSenderInitial(msg.sender_nickname) }}</span>
          </div>
          <div class="message-content">
            <div class="message-header">
              <span class="sender-name">{{ msg.sender_nickname }}</span>
              <span class="message-time">{{ formatTime(msg.create_time) }}</span>
            </div>
            <div class="message-body">
              <template v-if="msg.msg_type === 1">{{ msg.content }}</template>
              <img v-else-if="msg.msg_type === 2" :src="msg.media_url" class="message-image">
            </div>
          </div>
        </div>
      </div>

      <div class="input-area" v-if="session && session.status === 2">
        <el-input
          v-model="messageContent"
          placeholder="输入消息..."
          @keyup.enter.native="sendMessage"
          clearable>
        </el-input>
        <el-button type="primary" @click="sendMessage" :loading="sending">发送</el-button>
      </div>
      <div class="input-area disabled" v-else>
        <el-input
          v-model="messageContent"
          placeholder="等待人工接待中..."
          disabled>
        </el-input>
      </div>
    </el-card>
  </div>
</template>

<script>
import axios from 'axios';

export default {
  name: 'SessionDetail',
  data() {
    return {
      sessionNo: '',
      session: null,
      messages: [],
      messageContent: '',
      sending: false,
      ws: null
    };
  },
  mounted() {
    this.sessionNo = this.$route.params.sessionNo;
    this.loadSessionDetail();
    this.loadMessages();
    this.initWebSocket();
  },
  beforeDestroy() {
    if (this.ws) {
      this.ws.close();
    }
  },
  methods: {
    async loadSessionDetail() {
      try {
        const response = await axios.get(`/api/customer/session/${this.sessionNo}`);
        if (response.data.code === 0) {
          this.session = response.data.data;
        }
      } catch (error) {
        console.error('加载会话详情失败', error);
      }
    },
    async loadMessages() {
      try {
        const response = await axios.get(`/api/customer/messages/${this.sessionNo}`);
        if (response.data.code === 0) {
          this.messages = response.data.data || [];
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      } catch (error) {
        console.error('加载消息失败', error);
      }
    },
    initWebSocket() {
      const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}');
      const wsUrl = `ws://localhost:8080/ws/customer?userId=${agentInfo.id}&userType=agent&sessionNo=${this.sessionNo}`;
      
      this.ws = new WebSocket(wsUrl);
      
      this.ws.onopen = () => {
        console.log('WebSocket连接成功');
      };
      
      this.ws.onmessage = (event) => {
        const data = JSON.parse(event.data);
        if (data.type === 'user' || data.type === 'system') {
          this.messages.push({
            msg_id: 'msg_' + Date.now(),
            content: data.content,
            sender_type: 1,
            sender_id: data.senderId,
            sender_nickname: data.senderNickname,
            create_time: new Date(data.timestamp)
          });
          this.$nextTick(() => {
            this.scrollToBottom();
          });
        }
      };
      
      this.ws.onerror = (error) => {
        console.error('WebSocket错误', error);
      };
      
      this.ws.onclose = () => {
        console.log('WebSocket连接关闭');
      };
    },
    async sendMessage() {
      if (!this.messageContent.trim() || this.sending) return;
      
      this.sending = true;
      const msgId = 'msg_' + Date.now();
      
      try {
        const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}');
        const message = {
          action: 'send_message',
          sessionNo: this.sessionNo,
          msgId: msgId,
          content: this.messageContent,
          senderId: agentInfo.id,
          senderNickname: agentInfo.nickname
        };
        
        this.ws.send(JSON.stringify(message));
        
        this.messages.push({
          msg_id: msgId,
          content: this.messageContent,
          sender_type: 3,
          sender_id: agentInfo.id,
          sender_nickname: agentInfo.nickname,
          create_time: new Date()
        });
        
        this.messageContent = '';
        this.$nextTick(() => {
          this.scrollToBottom();
        });
      } catch (error) {
        console.error('发送消息失败', error);
        this.$message.error('发送消息失败');
      } finally {
        this.sending = false;
      }
    },
    async acceptSession() {
      try {
        const agentInfo = JSON.parse(localStorage.getItem('agentInfo') || '{}');
        await axios.post('/api/customer/session/accept', {
          sessionNo: this.sessionNo,
          agentId: agentInfo.id,
          agentNickname: agentInfo.nickname
        });
        this.$message.success('已接受会话');
        this.loadSessionDetail();
      } catch (error) {
        console.error('接受会话失败', error);
        this.$message.error('接受会话失败');
      }
    },
    async endSession() {
      try {
        await axios.post('/api/customer/session/end', {
          sessionNo: this.sessionNo
        });
        this.$message.success('会话已结束');
        this.loadSessionDetail();
      } catch (error) {
        console.error('结束会话失败', error);
        this.$message.error('结束会话失败');
      }
    },
    goBack() {
      this.$router.push('/customer/sessions');
    },
    getStatusType(status) {
      const types = { 0: 'info', 1: 'warning', 2: 'success', 3: '' };
      return types[status] || '';
    },
    getStatusText(status) {
      const texts = { 0: 'AI接待中', 1: '等待转人工', 2: '人工接待中', 3: '已结束' };
      return texts[status] || '未知';
    },
    getMessageClass(senderType) {
      const classes = { 1: 'message-user', 2: 'message-ai', 3: 'message-agent', system: 'message-system' };
      return classes[senderType] || '';
    },
    getSenderInitial(name) {
      return name ? name.charAt(0) : '?';
    },
    formatTime(time) {
      if (!time) return '-';
      const date = new Date(time);
      return date.toLocaleString('zh-CN', { hour12: false });
    },
    formatDuration(seconds) {
      if (!seconds) return '进行中';
      const minutes = Math.floor(seconds / 60);
      const secs = seconds % 60;
      return `${minutes}分${secs}秒`;
    },
    scrollToBottom() {
      if (this.$refs.messagesRef) {
        this.$refs.messagesRef.scrollTop = this.$refs.messagesRef.scrollHeight;
      }
    }
  }
};
</script>

<style scoped>
.session-detail {
  padding: 20px;
  height: calc(100vh - 120px);
  display: flex;
  flex-direction: column;
}
.header {
  display: flex;
  align-items: center;
  gap: 20px;
  margin-bottom: 20px;
}
.header h3 {
  margin: 0;
  flex: 1;
}
.session-info {
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
}
.info-item .label {
  color: #666;
}
.chat-area {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}
.messages {
  flex: 1;
  overflow-y: auto;
  padding: 20px;
}
.message {
  display: flex;
  margin-bottom: 20px;
}
.message-user {
  flex-direction: row;
}
.message-ai, .message-agent {
  flex-direction: row-reverse;
}
.message-system {
  justify-content: center;
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
  font-size: 18px;
  margin: 0 10px;
}
.message-content {
  max-width: 70%;
}
.message-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 5px;
}
.sender-name {
  font-weight: bold;
  color: #333;
}
.message-time {
  font-size: 12px;
  color: #999;
}
.message-body {
  padding: 10px 15px;
  border-radius: 10px;
  background: #f5f5f5;
}
.message-user .message-body {
  background: #409EFF;
  color: #fff;
}
.message-agent .message-body {
  background: #67C23A;
  color: #fff;
}
.message-image {
  max-width: 200px;
  max-height: 200px;
}
.input-area {
  display: flex;
  gap: 10px;
  padding: 20px;
  border-top: 1px solid #eee;
}
.input-area.disabled {
  opacity: 0.6;
}
</style>