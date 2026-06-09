// pages/pdf/split/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    fileName: '',
    filePath: '',
    fileSize: 0,
    loading: false,
    pageCount: 0,
    resultFiles: [],
    pageRange: ''
  },
  
  chooseFile: function() {
    const that = this;
    wx.chooseMessageFile({
      count: 1,
      type: 'file',
      extension: ['pdf'],
      success: (res) => {
        const file = res.tempFiles[0];
        that.setData({
          fileName: file.name,
          filePath: file.path,
          fileSize: file.size,
          resultFiles: [],
          pageRange: ''
        });
        
        // 获取PDF页数
        that.getPdfInfo();
      },
      fail: () => {
        wx.showToast({ title: '选择失败', icon: 'none' });
      }
    });
  },

  getPdfInfo() {
    wx.showLoading({ title: '正在分析...' });
    
    wx.uploadFile({
      url: API_BASE_URL + '/api/pdf/info',
      filePath: this.data.filePath,
      name: 'file',
      success: (res) => {
        wx.hideLoading();
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && data.data) {
            this.setData({
              pageCount: data.data.pages
            });
          }
        } catch (e) {
          console.error('解析失败:', e);
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '分析失败', icon: 'none' });
      }
    });
  },
  
  onPageRangeInput: function(e) {
    this.setData({
      pageRange: e.detail.value
    });
  },

  splitPdf: function() {
    if (!this.data.filePath) {
      wx.showToast({ title: '请先选择PDF文件', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    wx.showLoading({ title: '正在拆分...' });

    wx.uploadFile({
      url: API_BASE_URL + '/api/pdf/split',
      filePath: this.data.filePath,
      name: 'file',
      formData: {
        pageRange: this.data.pageRange || ''
      },
      success: (res) => {
        this.setData({ loading: false });
        wx.hideLoading();
        
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && data.data) {
            this.setData({
              resultFiles: data.data.files || []
            });
            wx.showToast({ title: '拆分成功', icon: 'success' });
          } else {
            wx.showToast({ title: data.msg || '拆分失败', icon: 'none' });
          }
        } catch (e) {
          wx.showToast({ title: '解析失败', icon: 'none' });
        }
      },
      fail: (err) => {
        this.setData({ loading: false });
        wx.hideLoading();
        wx.showToast({ title: '拆分失败', icon: 'none' });
        console.error('拆分失败:', err);
      }
    });
  },

  previewPdf: function(e) {
    const url = e.currentTarget.dataset.url;
    if (!url) return;
    
    wx.showLoading({ title: '正在加载...' });
    wx.downloadFile({
      url: url,
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            fileType: 'pdf',
            showMenu: true,
            success: () => console.log('预览成功'),
            fail: () => wx.showToast({ title: '预览失败', icon: 'none' })
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '加载失败', icon: 'none' });
      }
    });
  },

  downloadAll: function() {
    const files = this.data.resultFiles;
    if (!files || files.length === 0) {
      wx.showToast({ title: '暂无文件', icon: 'none' });
      return;
    }
    
    wx.showToast({ title: '开始下载...', icon: 'none' });
    
    // 提示用户分别保存
    wx.showModal({
      title: '提示',
      content: 'PDF已生成，请点击各个文件单独保存',
      showCancel: false
    });
  },

  clearFile: function() {
    this.setData({
      fileName: '',
      filePath: '',
      fileSize: 0,
      pageCount: 0,
      resultFiles: [],
      pageRange: ''
    });
  },

  formatSize: function(bytes) {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
});