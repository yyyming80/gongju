// pages/image/convert/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    imagePath: '',
    imageName: '',
    imageSize: 0,
    width: 0,
    height: 0,
    loading: false,
    resultImage: '',
    targetFormat: 'png',
    formats: ['png', 'jpg', 'bmp']
  },

  onLoad() {
    console.log('图片格式转换页面加载');
  },

  chooseImage() {
    const that = this;
    wx.chooseImage({
      count: 1,
      sizeType: ['original'],
      sourceType: ['album', 'camera'],
      success(res) {
        console.log('选择图片成功');
        const file = res.tempFiles[0];
        that.setData({
          imagePath: file.path,
          imageName: file.name,
          imageSize: file.size,
          resultImage: ''
        });
        
        wx.getImageInfo({
          src: file.path,
          success(info) {
            that.setData({
              width: info.width,
              height: info.height
            });
          }
        });
      },
      fail() {
        wx.showToast({ title: '选择失败', icon: 'none' });
      }
    });
  },

  onFormatChange(e) {
    this.setData({
      targetFormat: e.currentTarget.dataset.format
    });
  },

  convertImage() {
    if (!this.data.imagePath) {
      wx.showToast({ title: '请先选择图片', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    wx.showLoading({ title: '转换中...' });

    wx.uploadFile({
      url: API_BASE_URL + '/api/image-convert/convert',
      filePath: this.data.imagePath,
      name: 'file',
      formData: {
        format: this.data.targetFormat
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ loading: false });
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && data.data) {
            this.setData({
              resultImage: data.data.fileUrl
            });
            wx.showToast({ title: '转换成功', icon: 'success' });
          } else {
            wx.showToast({ title: data.msg || '转换失败', icon: 'none' });
          }
        } catch (e) {
          wx.showToast({ title: '解析失败', icon: 'none' });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        this.setData({ loading: false });
        wx.showToast({ title: '上传失败', icon: 'none' });
        console.error('上传失败:', err);
      }
    });
  },

  previewResult() {
    if (!this.data.resultImage) return;
    wx.previewImage({
      urls: [this.data.resultImage],
      success: () => wx.showToast({ title: '预览成功', icon: 'success' }),
      fail: () => wx.showToast({ title: '预览失败', icon: 'none' })
    });
  },

  saveImage() {
    if (!this.data.resultImage) {
      wx.showToast({ title: '暂无图片', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '保存中...' });
    wx.downloadFile({
      url: this.data.resultImage,
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.saveImageToPhotosAlbum({
            filePath: res.tempFilePath,
            success: () => wx.showToast({ title: '保存成功', icon: 'success' }),
            fail: () => wx.showToast({ title: '保存失败', icon: 'none' })
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '下载失败', icon: 'none' });
      }
    });
  },

  clearImage() {
    this.setData({
      imagePath: '',
      imageName: '',
      imageSize: 0,
      width: 0,
      height: 0,
      resultImage: ''
    });
  },

  formatSize(bytes) {
    if (!bytes) return '0 B';
    const k = 1024;
    const sizes = ['B', 'KB', 'MB', 'GB'];
    const i = Math.floor(Math.log(bytes) / Math.log(k));
    return Math.round(bytes / Math.pow(k, i) * 100) / 100 + ' ' + sizes[i];
  }
});