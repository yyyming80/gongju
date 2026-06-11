// pages/profile/profile.js
const auth = require('../../utils/auth.js');

Page({
  data: {
    userInfo: {
      avatar: '/images/default-avatar.png',
      nickname: '未登录',
      isVip: false
    },
    isGuest: false,
    serviceInfo: {
      wechat: 'toolbox_service',
      email: 'service@toolbox.com'
    }
  },

  onLoad() {
    console.log('我的页面加载');
    this.loadUserInfo();
  },

  onShow() {
    // 每次显示页面时刷新用户信息
    this.loadUserInfo();
  },

  /**
   * 加载用户信息
   */
  loadUserInfo() {
    const userInfo = auth.getUserInfo();
    const isGuest = auth.isGuest();
    
    if (userInfo) {
      this.setData({
        userInfo: userInfo,
        isGuest: isGuest
      });
    } else {
      this.setData({
        userInfo: {
          avatar: '/images/default-avatar.png',
          nickname: '未登录',
          isVip: false
        },
        isGuest: false
      });
    }
  },

  /**
   * 跳转登录页
   */
  goToLogin() {
    wx.navigateTo({
      url: '/pages/login/index'
    });
  },

  /**
   * 跳转AI客服
   */
  goToChat() {
    // 需要登录
    if (!auth.checkLogin('/pages/customer/chat/index')) {
      return;
    }
    wx.navigateTo({
      url: '/pages/customer/chat/index?type=ai'
    });
  },

  /**
   * 联系客服（旧方法，保留兼容性）
   */
  contactService() {
    this.goToChat();
  },

  /**
   * 退出登录
   */
  logout() {
    wx.showModal({
      title: '确认退出登录？',
      content: '',
      cancelText: '取消',
      confirmText: '确认',
      success: (res) => {
        if (res.confirm) {
          auth.logout();
          this.loadUserInfo();
        }
      }
    });
  },

  /**
   * 复制邮箱
   */
  copyEmail() {
    wx.setClipboardData({
      data: this.data.serviceInfo.email,
      success: () => {
        wx.showToast({
          title: '邮箱已复制',
          icon: 'success'
        });
      }
    });
  },

  /**
   * 复制微信
   */
  copyWechat() {
    wx.setClipboardData({
      data: this.data.serviceInfo.wechat,
      success: () => {
        wx.showToast({
          title: '微信已复制',
          icon: 'success'
        });
      }
    });
  }
});