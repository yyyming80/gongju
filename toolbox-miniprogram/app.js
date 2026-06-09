App({
  onLaunch() {
    console.log('App Launch')
  },
  onShow() {
    console.log('App Show')
  },
  onHide() {
    console.log('App Hide')
  },
  globalData: {
    userInfo: null,
    apiBase: 'http://localhost:8080/api'
  }
})
