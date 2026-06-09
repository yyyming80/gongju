// pages/image/compress/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    imagePath: '',
    imageName: '',
    imageSize: 0,
    width: 0,
    height: 0,
    quality: 80,
    loading: false,
    resultImage: '',
    originalSize: 0,
    compressedSize: 0,
    compressionRate: 0
  },

  onLoad() {
    console.log('图片压缩页面加载');
  },

  /**
   * 格式化文件大小
   * @param {number} bytes - 字节数
   * @returns {string} - 格式化后的大小，如 "1.2MB"
   */
  formatSize(bytes) {
    if (!bytes || bytes <= 0) return '0 B';
    
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let unitIndex = 0;
    
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }
    
    // 保留2位小数，去掉多余的0
    const formatted = parseFloat(size.toFixed(2));
    return formatted + ' ' + units[unitIndex];
  },

  chooseImage() {
    const that = this;
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed', 'original'],
      sourceType: ['album', 'camera'],
      success(res) {
        console.log('选择图片成功');
        const file = res.tempFiles[0];
        that.setData({
          imagePath: file.path,
          imageName: file.name,
          imageSize: file.size,
          resultImage: '',
          resultSize: 0,
          compressionRate: 0
        });
        
        // 获取图片信息
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

  onQualityChange(e) {
    this.setData({
      quality: parseInt(e.detail.value)
    });
  },

  compressImage() {
    if (!this.data.imagePath) {
      wx.showToast({ title: '请先选择图片', icon: 'none' });
      return;
    }

    this.setData({ loading: true });
    wx.showLoading({ title: '正在压缩...' });

    wx.uploadFile({
      url: API_BASE_URL + '/api/image/compress',
      filePath: this.data.imagePath,
      name: 'file',
      formData: {
        quality: this.data.quality.toString()
      },
      success: (res) => {
        wx.hideLoading();
        this.setData({ loading: false });
        try {
          const data = JSON.parse(res.data);
          if (data.code === 200 && data.data) {
            const result = data.data;
            this.setData({
              resultImage: result.fileUrl,
              originalSize: result.sourceSize || result.originalSize || 0,
              compressedSize: result.outputSize || result.compressedSize || 0,
              compressionRate: result.compressionRate || '0%'
            });
            wx.showToast({ title: '压缩成功', icon: 'success' });
          } else {
            wx.showToast({ title: data.message || data.msg || '压缩失败', icon: 'none' });
          }
        } catch (e) {
          console.error('解析失败:', e);
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
    
    wx.showLoading({ title: '加载中...' });
    wx.previewImage({
      urls: [this.data.resultImage],
      success: () => wx.hideLoading(),
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '预览失败', icon: 'none' });
      }
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
      resultImage: '',
      resultSize: 0,
      resultWidth: 0,
      resultHeight: 0,
      compressionRate: 0
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