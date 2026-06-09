const api = require('../../../utils/api.js');
const auth = require('../../../utils/auth.js');

Page({
  data: {
    userInfo: null
  },

  onLoad() {
    this.setData({
      userInfo: auth.getUserInfo()
    });
  },

  // 开始AI聊天
  startChat() {
    wx.navigateTo({
      url: '/pages/customer/chat/index?type=ai'
    });
  },

  // 转人工客服
  transferToHuman() {
    wx.navigateTo({
      url: '/pages/customer/chat/index?type=human'
    });
  },

  // FAQ点击
  onFaqTap(e) {
    const question = e.currentTarget.dataset.question;
    wx.navigateTo({
      url: `/pages/customer/chat/index?type=ai&question=${encodeURIComponent(question)}`
    });
  },

  // 查看历史记录
  viewHistory() {
    wx.navigateTo({
      url: '/pages/customer/history/index'
    });
  }
});