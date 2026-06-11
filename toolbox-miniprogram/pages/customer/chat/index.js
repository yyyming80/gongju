const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

console.error('★★★★★ CUSTOMER CHAT FILE LOADED ★★★★★');
console.error('api对象:', api);
console.error('auth对象:', auth);

// 立即检查auth.getUserInfo()
const testUserInfo = auth.getUserInfo();
console.error('测试userInfo:', testUserInfo);

console.error('★★★★★ BEFORE PAGE ★★★★★');
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
    connected: false,
    waitAgent: false // 是否在等待人工客服
  },

  onLoad(options) {
    console.error('★★★★★ ONLOAD ★★★★★');
    console.error('options:', options);
    
    const userInfo = auth.getUserInfo();
    console.log('userInfo:', userInfo);
    
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
    }
    
    console.error('★★★★★ STARTCHAT ★★★★★');
    // 自动开始聊天
    this.startChat();
  },

  onUnload() {
    console.log('========== onUnload ==========');
    this.closeWebSocket();
  },

  onShow() {
    console.log('========== onShow ==========');
  },

  onHide() {
    console.log('========== onHide ==========');
    this.closeWebSocket();
  },

  // 开始聊天
  startChat() {
    console.error('③ startChat执行');
    const userInfo = this.data.userInfo;
    console.error('startChat userInfo:', userInfo);
    
    // 检查登录状态
    if (!userInfo || !userInfo.openid) {
      console.error('用户未登录或为游客，无openid');
      wx.showModal({
        title: '提示',
        content: '请先登录后使用客服功能',
        showCancel: false,
        success: () => {
          wx.navigateTo({
            url: '/pages/login/index'
          });
        }
      });
      return;
    }
    
    // 调用API开始会话
    api.startSession({
      userId: userInfo.openid,
      userNickname: userInfo.nickname || '用户',
      userAvatar: userInfo.avatar || ''
    }).then(res => {
      console.error('④ startSession成功');
      console.error('res:', res);
      
      if (res.code === 200) {
        const session = res.data;
        console.error('⑤ sessionNo=', session.session_no);
        
        this.setData({
          sessionNo: session.session_no,
          sessionId: session.id
        });
        
        console.error('⑥ loadMessages开始');
        // 加载历史消息
        this.loadMessages();
        
        console.error('⑦ initWebSocket开始');
        // 建立WebSocket连接
        this.initWebSocket();
        
        // 如果是转人工类型，直接触发转人工
        if (this.data.type === 'human') {
          // 延迟一下再转人工，确保WebSocket连接成功
          setTimeout(() => {
            this.onTransferToHuman();
          }, 500);
        }
      } else {
        console.error('开始会话失败', res);
        wx.showToast({
          title: res.message || '连接失败',
          icon: 'none'
        });
      }
    }).catch(err => {
      console.error('========== startChat失败 ==========');
      console.error('连接失败', err);
      wx.showToast({
        title: '连接失败',
        icon: 'none'
      });
    });
  },

  // 加载历史消息
  loadMessages() {
    console.log('========== loadMessages ==========');
    console.log('sessionNo:', this.data.sessionNo);
    
    if (!this.data.sessionNo) {
      console.log('loadMessages: sessionNo为空，跳过');
      return;
    }
    
    api.getMessages(this.data.sessionNo).then(res => {
      console.log('========== loadMessages接口返回 ==========');
      console.log('res:', res);
      console.log('消息数量:', res.data ? res.data.length : 0);
      
      if (res.code === 200) {
        console.log('========== 加载历史消息成功 ==========');
        this.setData({
          messages: res.data || []
        });
        console.log('messages:', this.data.messages);
        this.scrollToBottom();
      }
    }).catch(err => {
      console.error('loadMessages失败:', err);
    });
  },

  // 初始化WebSocket
  initWebSocket() {
    console.error('★★★★★ INITWS ★★★★★');
    console.error('sessionNo=', this.data.sessionNo);
    console.error('userInfo=', this.data.userInfo);
    
    const userInfo = this.data.userInfo;
    
    // 检查必要参数
    if (!this.data.sessionNo) {
      console.error('initWebSocket失败: sessionNo为空');
      return;
    }
    if (!userInfo || !userInfo.openid) {
      console.error('initWebSocket失败: userInfo.openid为空');
      return;
    }
    
    const wsUrl = `ws://localhost:8080/ws/customer?sessionNo=${this.data.sessionNo}&userId=${userInfo.openid}`;
    
    console.error('⑨ connectSocket调用, wsUrl=', wsUrl);
    
    const ws = wx.connectSocket({
      url: wsUrl,
      success: () => {
        console.error('⑩ connectSocket成功');
      },
      fail: (err) => {
        console.error('⑩ connectSocket失败:', err);
      }
    });
    
    // 重要：同时保存到 this.ws 和 this.data.ws
    this.ws = ws;
    this.setData({ ws: ws });  // 添加到data中
    
    console.error('SocketTask对象:', ws);

    ws.onOpen(() => {
      console.error('⑪ WS onOpen 连接成功');
      console.error('readyState:', ws.readyState);
      this.setData({ connected: true, ws: ws });  // 同时更新ws和connected
    });

    ws.onMessage((res) => {
      console.error('⑫ WS onMessage 收到消息:', res.data);
      const data = JSON.parse(res.data);
      this.handleMessage(data);
    });

    ws.onError((err) => {
      console.error('⑬ WS onError 错误:', err);
    });

    ws.onClose(() => {
      console.error('⑭ WS onClose 关闭');
      this.setData({ connected: false, ws: null });
    });
    
    console.error('⑮ initWebSocket执行完成');
  },

  // 处理消息
  handleMessage(data) {
    console.log('========== handleMessage收到消息 ==========');
    console.log('data:', data);
    console.log('data.type:', data.type);
    console.log('data.sender_type:', data.sender_type);
    console.log('data.content:', data.content);
    console.log('====================================');
    
    // 系统消息类型 (转人工成功后的系统消息)
    if (data.type === 'system' || data.sender_type === 4) {
      console.log('匹配到系统消息类型');
      const newMsg = {
        msg_id: data.msg_id || 'msg_' + Date.now(),
        msg_type: 1,
        sender_type: 4,
        sender_nickname: data.senderNickname || '系统助手',
        content: data.content,
        create_time: new Date(data.timestamp || Date.now())
      };
      console.log('构造的新消息:', newMsg);
      
      const messages = [...this.data.messages, newMsg];
      console.log('最终消息列表:', messages);
      
      this.setData({ messages });
      this.scrollToBottom();
      console.log('setData完成');
      return;
    }
    
    // AI消息类型
    if (data.type === 'ai' || data.sender_type === 2) {
      console.log('匹配到AI消息类型');
      const messages = [...this.data.messages, {
        msg_id: data.msg_id || 'msg_' + Date.now(),
        content: data.content,
        msg_type: data.msg_type || 1,
        sender_type: 2,
        sender_nickname: data.senderNickname || '智能助手',
        create_time: new Date(data.timestamp || Date.now())
      }];
      
      this.setData({ 
        messages,
        showTransferHint: true
      });
      this.scrollToBottom();
      return;
    }
    
    // 客服消息类型（支持type="agent"或sender_type=3）
    if (data.type === 'agent' || data.sender_type === 3) {
      console.log('匹配到客服消息类型');
      const messages = [...this.data.messages, {
        msg_id: data.msg_id || 'msg_' + Date.now(),
        content: data.content,
        msg_type: data.msg_type || 1,
        sender_type: 3,
        sender_nickname: data.senderNickname || '人工客服',
        create_time: new Date(data.timestamp || Date.now())
      }];
      
      this.setData({ 
        messages,
        waitAgent: false // 客服已接入，不再等待
      });
      this.scrollToBottom();
      return;
    }
    
    // 用户消息
    if (data.type === 'user' || data.sender_type === 1) {
      console.log('匹配到用户消息类型');
      const messages = [...this.data.messages, {
        msg_id: data.msg_id || 'msg_' + Date.now(),
        content: data.content,
        msg_type: data.msg_type || 1,
        sender_type: 1,
        sender_nickname: this.data.userInfo.nickname || '我',
        create_time: new Date(data.timestamp || Date.now())
      }];
      
      this.setData({ messages });
      this.scrollToBottom();
    }
  },

  // 发送消息
  sendMessage() {
    const content = this.data.inputValue.trim();
    if (!content) return;

    // 清空输入框
    this.setData({ inputValue: '' });

    // 添加用户消息到列表
    const messages = [...this.data.messages, {
      msg_id: 'msg_' + Date.now(),
      content: content,
      msg_type: 1,
      sender_type: 1,
      sender_nickname: this.data.userInfo.nickname || '我',
      create_time: new Date()
    }];
    
    this.setData({ messages });
    this.scrollToBottom();

    // 发送消息到WebSocket
    if (this.data.ws && this.data.connected) {
      const msg = {
        action: 'send_message',
        sessionNo: this.data.sessionNo,
        msgId: 'msg_' + Date.now(),
        content: content,
        senderId: this.data.userInfo.openid,
        senderNickname: this.data.userInfo.nickname || '用户'
      };

      this.data.ws.send({
        data: JSON.stringify(msg)
      });
    } else {
      // 如果WebSocket未连接，使用API发送
      wx.request({
        url: 'http://localhost:8080/api/chat/send',
        method: 'POST',
        data: {
          message: content,
          sessionNo: this.data.sessionNo,
          userId: this.data.userInfo.openid,
          userNickname: this.data.userInfo.nickname,
          userAvatar: this.data.userInfo.avatar
        },
        success: (res) => {
          if (res.data.code === 200) {
            // 检查是否需要转人工
            if (res.data.data.transfer) {
              // 转人工，返回的消息包含系统消息
              const transferData = res.data.data;
              if (transferData.systemMessage) {
                this.handleMessage({
                  type: 'system',
                  content: transferData.systemMessage.content,
                  senderNickname: '系统助手'
                });
              }
            } else if (res.data.data.reply) {
              // AI回复
              this.handleMessage({
                type: 'ai',
                content: res.data.data.reply,
                senderNickname: '智能助手'
              });
            }
          }
        }
      });
    }
  },

  // 转人工
  onTransferToHuman() {
    // 添加一条用户发送的"转人工"消息
    const messages = [...this.data.messages, {
      msg_id: 'msg_' + Date.now(),
      content: '转人工',
      msg_type: 1,
      sender_type: 1,
      sender_nickname: this.data.userInfo.nickname || '我',
      create_time: new Date()
    }];
    
    this.setData({ 
      messages,
      waitAgent: true
    });
    this.scrollToBottom();

    // 发送转人工请求
    const msg = {
      action: 'transfer_to_human',
      sessionNo: this.data.sessionNo,
      reason: '用户主动请求'
    };

    if (this.data.ws && this.data.connected) {
      this.data.ws.send({
        data: JSON.stringify(msg)
      });
    }
    
    // 同时通过HTTP请求确保转人工成功
    wx.request({
      url: 'http://localhost:8080/api/chat/send',
      method: 'POST',
      data: {
        message: '转人工',
        sessionNo: this.data.sessionNo,
        userId: this.data.userInfo.openid,
        userNickname: this.data.userInfo.nickname,
        userAvatar: this.data.userInfo.avatar
      },
      success: (res) => {
        console.log('========== 转人工接口返回 ==========');
        console.log('res:', res);
        console.log('res.data:', res.data);
        console.log('transfer:', res.data && res.data.data && res.data.data.transfer);
        console.log('systemMessage:', res.data && res.data.data && res.data.data.systemMessage);
        console.log('===================================');
        
        if (res.data.code === 200 && res.data.data.transfer) {
          // 显示系统消息
          const systemMsg = res.data.data.systemMessage;
          console.log('准备显示系统消息:', systemMsg);
          if (systemMsg) {
            this.handleMessage({
              type: 'system',
              content: systemMsg.content,
              senderNickname: '系统助手'
            });
            console.log('系统消息已处理');
          }
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

  // 返回
  goBack() {
    wx.navigateBack();
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
    console.log('========== closeWebSocket ==========');
    console.log('this.ws:', this.ws);
    console.log('this.data.ws:', this.data.ws);
    
    if (this.ws) {
      console.log('关闭WebSocket连接');
      this.ws.close();
      this.ws = null;
      this.setData({ ws: null, connected: false });
    }
  }
});

console.error('★★★★★ AFTER PAGE ★★★★★');
console.error('CUSTOMER CHAT PAGE 注册完成');
console.error('等待页面跳转...');