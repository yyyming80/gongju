// pages/chat/chat/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    messages: [],
    inputText: '',
    loading: false,
    scrollTop: 0
  },

  onLoad() {
    console.log('AI客服页面加载');
    
    // 检查服务状态
    this.checkService();
  },

  /**
   * 检查服务状态
   */
  checkService() {
    wx.request({
      url: API_BASE_URL + '/api/chat/check',
      method: 'GET',
      success: (res) => {
        if (res.data?.code === 200) {
          if (!res.data.data.available) {
            wx.showToast({
              title: 'AI服务未启动',
              icon: 'none',
              duration: 3000
            });
          }
        }
      }
    });
  },

  /**
   * 输入监听
   */
  onInput(e) {
    this.setData({
      inputText: e.detail.value
    });
  },

  /**
   * 发送消息
   */
  sendMessage() {
    const message = this.data.inputText.trim();
    if (!message) return;
    if (this.data.loading) return;

    // 添加用户消息
    const messages = [...this.data.messages, {
      role: 'user',
      content: message,
      time: this.formatTime(new Date())
    }];
    
    this.setData({
      messages,
      inputText: '',
      loading: true
    });

    this.scrollToBottom();

    // 发送请求
    wx.request({
      url: API_BASE_URL + '/api/chat/send',
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: {
        message: message
      },
      success: (res) => {
        this.setData({ loading: false });
        
        if (res.statusCode === 200 && res.data) {
          if (res.data.code === 200) {
            // 添加AI回复
            const aiMessages = [...this.data.messages, {
              role: 'ai',
              content: res.data.data.reply,
              time: this.formatTime(new Date())
            }];
            this.setData({ messages: aiMessages });
            this.scrollToBottom();
          } else {
            this.addErrorMessage(res.data.msg || '回复失败');
          }
        } else {
          this.addErrorMessage('服务器错误');
        }
      },
      fail: (err) => {
        this.setData({ loading: false });
        console.error('请求失败:', err);
        this.addErrorMessage('网络请求失败，请检查后端服务是否启动');
      }
    });
  },

  /**
   * 发送快捷问题
   */
  sendQuickQuestion(e) {
    const question = e.currentTarget.dataset.question;
    this.setData({ inputText: question });
    this.sendMessage();
  },

  /**
   * 添加错误消息
   */
  addErrorMessage(msg) {
    const messages = [...this.data.messages, {
      role: 'ai',
      content: '抱歉，' + msg + '。请确保Ollama服务已启动（运行命令：ollama serve）',
      time: this.formatTime(new Date())
    }];
    this.setData({ messages });
    this.scrollToBottom();
  },

  /**
   * 滚动到底部
   */
  scrollToBottom() {
    setTimeout(() => {
      this.setData({ scrollTop: 999999 });
    }, 100);
  },

  /**
   * 格式化时间
   */
  formatTime(date) {
    const hours = date.getHours().toString().padStart(2, '0');
    const minutes = date.getMinutes().toString().padStart(2, '0');
    return `${hours}:${minutes}`;
  },

  /**
   * 页面滚动回调
   */
  onScrollTop() {
    // 可以在这里加载历史消息
  }
});