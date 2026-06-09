const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    sessionNo: '',
    sessionId: null,
    type: 'ai', // ai or human
    isHuman: false,
    messages: [],
    inputValue: '',
    scrollTop: 0,
    showTransferHint: false,
    userInfo: null,
    userAvatar: '我',
    ws: null,
    connected: false
  },

  onLoad(options) {
    const userInfo = auth.getUserInfo();
    this.setData({
      type: options.type || 'ai',
      isHuman: options.type === 'human',
      userInfo: userInfo,
      userAvatar: userInfo && userInfo.nickname ? userInfo.nickname.charAt(0) : '我'
    });

    // 如果有预设问题
    if (options.question) {
      this.setData({
        inputValue: decodeURIComponent(options.question)
      });
      this.startChat();
    }
  },

  onUnload() {
    this.closeWebSocket();
  },

  // 开始聊天
  startChat() {
    const userInfo = this.data.userInfo;
    
    // 调用API开始会话
    api.startSession({
      userId: userInfo.openid,
      userNickname: userInfo.nickname || '用户',
      userAvatar: userInfo.avatar || ''
    }).then(res => {
      if (res.code === 0) {
        const session = res.data;
        this.setData({
          sessionNo: session.session_no,
          sessionId: session.id
        });
        
        // 加载历史消息
        this.loadMessages();
        
        // 建立WebSocket连接
        this.initWebSocket();
        
        // 如果是转人工，直接触发转人工
        if (this.data.type === 'human') {
          this.onTransferToHuman();
        }
      }
    }).catch(err => {
      console.error('连接失败', err);
      wx.showToast({
        title: '连接失败',
        icon: 'none'
      });
    });
  },

  // 加载历史消息
  loadMessages() {
    if (!this.data.sessionNo) return;
    
    api.getMessages(this.data.sessionNo).then(res => {
      if (res.code === 0) {
        this.setData({
          messages: res.data || []
        });
        this.scrollToBottom();
      }
    });
  },

  // 初始化WebSocket
  initWebSocket() {
    const userInfo = this.data.userInfo;
    const wsUrl = `ws://localhost:8080/ws/customer?sessionNo=${this.data.sessionNo}&userId=${userInfo.openid}`;
    
    this.ws = wx.connectSocket({
      url: wsUrl,
      success: () => {
        console.log('WebSocket连接成功');
        this.setData({ connected: true });
      }
    });

    this.ws.onSocketOpen(() => {
      console.log('WebSocket已打开');
    });

    this.ws.onSocketMessage((res) => {
      const data = JSON.parse(res.data);
      this.handleMessage(data);
    });

    this.ws.onSocketError((err) => {
      console.error('WebSocket错误', err);
    });

    this.ws.onSocketClose(() => {
      console.log('WebSocket已关闭');
      this.setData({ connected: false });
    });
  },

  // 处理消息
  handleMessage(data) {
    if (data.type === 'ai' || data.type === 'system') {
      const messages = [...this.data.messages, {
        msg_id: 'msg_' + Date.now(),
        content: data.content,
        sender_type: data.type === 'ai' ? 2 : 'system',
        sender_nickname: data.senderNickname,
        create_time: new Date(data.timestamp)
      }];
      
      this.setData({
        messages,
        showTransferHint: data.type === 'ai'
      });
      
      this.scrollToBottom();
    }
  },

  // 发送消息
  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content || !this.data.connected) return;

    // 清空输入框
    this.setData({ inputValue: '' });

    // 添加用户消息到列表
    const messages = [...this.data.messages, {
      msg_id: 'msg_' + Date.now(),
      content: content,
      sender_type: 1,
      sender_nickname: this.data.userInfo.nickname || '我',
      create_time: new Date()
    }];
    
    this.setData({ messages });
    this.scrollToBottom();

    // 发送消息
    const msg = {
      action: 'send_message',
      sessionNo: this.data.sessionNo,
      msgId: 'msg_' + Date.now(),
      content: content,
      senderId: this.data.userInfo.openid,
      senderNickname: this.data.userInfo.nickname || '用户'
    };

    this.ws.send({
      data: JSON.stringify(msg)
    });
  },

  // 转人工
  onTransferToHuman() {
    wx.showModal({
      title: '提示',
      content: '确定转接人工客服吗？',
      success: (res) => {
        if (res.confirm) {
          const msg = {
            action: 'transfer_to_human',
            sessionNo: this.data.sessionNo,
            reason: '用户主动请求'
          };
          
          this.ws.send({
            data: JSON.stringify(msg)
          });

          this.setData({
            isHuman: true
          });

          wx.showToast({
            title: '已转接人工客服',
            icon: 'success'
          });
        }
      }
    });
  },

  // 输入事件
  onInput(e) {
    this.setData({
      inputValue: e.detail.value
    });
  },

  // 加载更多消息
  loadMore() {
    // 实现分页加载
  },

  // 返回
  goBack() {
    wx.navigateBack();
  },

  // 显示更多
  showMore() {
    wx.showActionSheet({
      itemList: ['结束会话', '清空记录'],
      success: (res) => {
        if (res.tapIndex === 0) {
          this.endSession();
        } else if (res.tapIndex === 1) {
          this.clearHistory();
        }
      }
    });
  },

  // 结束会话
  endSession() {
    if (!this.data.sessionNo) return;

    api.endSession({
      sessionNo: this.data.sessionNo
    }).then(res => {
      wx.showToast({
        title: '会话已结束',
        icon: 'success'
      });
      setTimeout(() => {
        wx.navigateBack();
      }, 1500);
    });
  },

  // 清空历史
  clearHistory() {
    wx.showModal({
      title: '提示',
      content: '确定清空聊天记录吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            messages: []
          });
        }
      }
    });
  },

  // 格式化时间
  formatTime(time) {
    if (!time) return '';
    const date = new Date(time);
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  // 滚动到底部
  scrollToBottom() {
    this.setData({
      scrollTop: this.data.messages.length * 1000
    });
  },

  // 关闭WebSocket
  closeWebSocket() {
    if (this.ws) {
      this.ws.close();
    }
  }
});