// pages/login/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    loading: false,
    redirectUrl: ''
  },

  onLoad(options) {
    if (options.redirect) {
      this.setData({ redirectUrl: decodeURIComponent(options.redirect) });
    }
    console.log('登录页面加载', options);
  },

  /**
   * 微信登录按钮点击
   */
  onWechatLogin() {
    this.setData({ loading: true });

    // 1. 获取微信登录凭证
    wx.login({
      success: (loginRes) => {
        console.log('wx.login 成功', loginRes);

        if (!loginRes.code) {
          wx.showToast({ title: '获取登录凭证失败', icon: 'none' });
          this.setData({ loading: false });
          return;
        }

        // 2. 获取用户信息
        wx.getUserProfile({
          desc: '用于完善用户资料',
          success: (userRes) => {
            console.log('getUserProfile 成功', userRes);
            this.doLogin(loginRes.code, userRes.userInfo);
          },
          fail: (err) => {
            console.log('getUserProfile 拒绝', err);
            // 用户拒绝授权，使用基础信息登录
            wx.showModal({
              title: '提示',
              content: '是否使用基础信息登录？',
              success: (res) => {
                if (res.confirm) {
                  this.doLogin(loginRes.code, null);
                } else {
                  this.setData({ loading: false });
                }
              }
            });
          }
        });
      },
      fail: (err) => {
        console.error('wx.login 失败', err);
        wx.showToast({ title: '微信登录失败', icon: 'none' });
        this.setData({ loading: false });
      }
    });
  },

  /**
   * 执行登录
   */
  doLogin(code, userInfo) {
    // 构建请求数据
    const requestData = {
      code: code
    };

    // 添加用户信息
    if (userInfo) {
      requestData.nickname = userInfo.nickName || '';
      requestData.avatar = userInfo.avatarUrl || '';
      requestData.gender = userInfo.gender || 0;
    }

    console.log('发送登录请求', requestData);

    wx.request({
      url: API_BASE_URL + '/api/auth/wx-login',
      method: 'POST',
      header: {
        'Content-Type': 'application/json'
      },
      data: requestData,
      success: (res) => {
        console.log('登录响应', res);

        if (res.statusCode === 200 && res.data) {
          if (res.data.code === 200) {
            // 保存用户信息
            const { token, userInfo: savedUserInfo } = res.data.data;

            wx.setStorageSync('token', token);
            wx.setStorageSync('userInfo', savedUserInfo);
            wx.setStorageSync('isLoggedIn', true);

            wx.showToast({
              title: '登录成功',
              icon: 'success',
              duration: 1000
            });

            // 跳转到之前的页面或首页
            setTimeout(() => {
              if (this.data.redirectUrl) {
                wx.redirectTo({ url: this.data.redirectUrl });
              } else {
                wx.switchTab({ url: '/pages/index/index' });
              }
            }, 1000);

          } else {
            wx.showToast({
              title: res.data.msg || '登录失败',
              icon: 'none'
            });
            this.setData({ loading: false });
          }
        } else {
          wx.showToast({ title: '服务器错误', icon: 'none' });
          this.setData({ loading: false });
        }
      },
      fail: (err) => {
        console.error('登录请求失败', err);
        wx.showToast({ title: '网络错误，请重试', icon: 'none' });
        this.setData({ loading: false });
      }
    });
  },

  /**
   * 游客模式
   */
  onGuestMode() {
    const guestUser = {
      id: 0,
      nickname: '游客',
      avatar: '/images/default-avatar.png',
      isGuest: true
    };

    wx.setStorageSync('token', 'guest_token');
    wx.setStorageSync('userInfo', guestUser);
    wx.setStorageSync('isLoggedIn', true);
    wx.setStorageSync('isGuest', true);

    wx.showToast({
      title: '已进入游客模式',
      icon: 'success',
      duration: 1000
    });

    setTimeout(() => {
      if (this.data.redirectUrl) {
        wx.redirectTo({ url: this.data.redirectUrl });
      } else {
        wx.switchTab({ url: '/pages/index/index' });
      }
    }, 1000);
  }
});