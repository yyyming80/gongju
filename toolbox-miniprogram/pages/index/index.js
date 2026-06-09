// pages/index/index.js
const auth = require('../../utils/auth.js');

Page({
  data: {
    userInfo: null,
    isLoggedIn: false
  },

  onLoad() {
    console.log('ToolBox 首页加载');
  },

  onShow() {
    // 检查登录状态
    this.setData({
      isLoggedIn: auth.isLoggedIn(),
      userInfo: auth.getUserInfo()
    });
  },

  /**
   * 导航到功能页面（带登录校验）
   */
  navigateTo(e) {
    const url = e.currentTarget.dataset.url;
    
    if (!url) {
      wx.showToast({ title: '功能开发中', icon: 'none' });
      return;
    }

    // 检查登录状态
    if (!auth.checkLogin(url)) {
      console.log('未登录，跳转登录页');
      return;
    }

    // 已登录，跳转到目标页面
    wx.navigateTo({ url: url });
  },

  /**
   * 跳转到我的页面
   */
  goToProfile() {
    wx.switchTab({ url: '/pages/profile/profile' });
  }
});