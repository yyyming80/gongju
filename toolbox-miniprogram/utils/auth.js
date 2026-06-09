/**
 * 登录工具类
 * 提供统一的登录校验和用户信息管理
 */

/**
 * 检查登录状态
 * @returns {boolean} 是否已登录
 */
function isLoggedIn() {
  try {
    const isLoggedIn = wx.getStorageSync('isLoggedIn');
    const token = wx.getStorageSync('token');
    return !!(isLoggedIn && token);
  } catch (e) {
    console.error('检查登录状态失败', e);
    return false;
  }
}

/**
 * 检查是否为游客
 * @returns {boolean} 是否为游客
 */
function isGuest() {
  try {
    const isGuest = wx.getStorageSync('isGuest');
    return !!isGuest;
  } catch (e) {
    return false;
  }
}

/**
 * 获取用户信息
 * @returns {object|null} 用户信息
 */
function getUserInfo() {
  try {
    const userInfo = wx.getStorageSync('userInfo');
    return userInfo || null;
  } catch (e) {
    console.error('获取用户信息失败', e);
    return null;
  }
}

/**
 * 获取Token
 * @returns {string|null} Token
 */
function getToken() {
  try {
    return wx.getStorageSync('token') || null;
  } catch (e) {
    return null;
  }
}

/**
 * 检查登录状态，如果未登录则跳转登录页
 * @param {string} redirectUrl 登录成功后跳转的页面
 * @returns {boolean} 是否已登录
 */
function checkLogin(redirectUrl) {
  if (!isLoggedIn()) {
    // 获取当前页面路径
    const pages = getCurrentPages();
    const currentPage = pages[pages.length - 1];
    let redirect = '';
    
    if (redirectUrl) {
      redirect = encodeURIComponent(redirectUrl);
    } else if (currentPage) {
      const path = '/' + currentPage.route;
      const query = currentPage.options || {};
      const queryString = Object.keys(query)
        .map(key => key + '=' + query[key])
        .join('&');
      redirect = encodeURIComponent(path + (queryString ? '?' + queryString : ''));
    }

    wx.navigateTo({
      url: '/pages/login/index' + (redirect ? '?redirect=' + redirect : '')
    });
    return false;
  }
  return true;
}

/**
 * 退出登录
 */
function logout() {
  try {
    wx.removeStorageSync('token');
    wx.removeStorageSync('userInfo');
    wx.removeStorageSync('isLoggedIn');
    wx.removeStorageSync('isGuest');
    
    wx.showToast({ title: '已退出登录', icon: 'success' });
    
    // 返回首页
    setTimeout(() => {
      wx.switchTab({ url: '/pages/index/index' });
    }, 1000);
  } catch (e) {
    console.error('退出登录失败', e);
  }
}

/**
 * 更新用户信息
 * @param {object} userInfo 用户信息
 */
function updateUserInfo(userInfo) {
  try {
    const oldInfo = getUserInfo() || {};
    const newInfo = { ...oldInfo, ...userInfo };
    wx.setStorageSync('userInfo', newInfo);
  } catch (e) {
    console.error('更新用户信息失败', e);
  }
}

/**
 * 初始化登录状态
 * 在App.onLaunch中调用
 */
function initLoginStatus() {
  // 检查本地存储的登录状态
  const token = getToken();
  const userInfo = getUserInfo();
  
  console.log('初始化登录状态', { token: !!token, userInfo });
  
  return {
    isLoggedIn: isLoggedIn(),
    isGuest: isGuest(),
    userInfo: userInfo
  };
}

module.exports = {
  isLoggedIn,
  isGuest,
  getUserInfo,
  getToken,
  checkLogin,
  logout,
  updateUserInfo,
  initLoginStatus
};