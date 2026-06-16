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
    savedSize: 0,
    compressionRate: '',
    originalSizeText: '',
    compressedSizeText: '',
    savedSizeText: ''
  },

  onLoad() {
    console.log('图片压缩页面加载');
  },

  /**
   * 格式化文件大小
   * @param {number} bytes - 字节数
   * @returns {string} - 格式化后的大小，如 "123.84 KB"
   */
  formatSize(bytes) {
    if (bytes === undefined || bytes === null || bytes <= 0) return '-';
    
    const units = ['B', 'KB', 'MB', 'GB'];
    let size = bytes;
    let unitIndex = 0;
    
    while (size >= 1024 && unitIndex < units.length - 1) {
      size /= 1024;
      unitIndex++;
    }
    
    // 保留2位小数
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
        console.log('选择图片成功', res);
        const file = res.tempFiles[0];
        
        // 重置结果数据
        that.setData({
          imagePath: file.path,
          imageName: file.name || '未命名图片',
          imageSize: 0,
          width: 0,
          height: 0,
          resultImage: '',
          originalSize: 0,
          compressedSize: 0,
          savedSize: 0,
          compressionRate: ''
        });
        
        // 使用 getFileSystemManager 获取原图文件大小
        const fs = wx.getFileSystemManager();
        fs.getFileInfo({
          filePath: file.path,
          success(fileInfo) {
            console.log('获取原图大小成功:', fileInfo.size);
            that.setData({
              imageSize: fileInfo.size,
              originalSize: fileInfo.size
            });
          },
          fail(err) {
            console.error('获取原图大小失败:', err);
            // 如果获取失败，使用chooseImage返回的大小
            const fallbackSize = file.size || 0;
            that.setData({
              imageSize: fallbackSize,
              originalSize: fallbackSize
            });
          }
        });
        
        // 获取图片信息（宽高）
        wx.getImageInfo({
          src: file.path,
          success(info) {
            that.setData({
              width: info.width,
              height: info.height
            });
          },
          fail(err) {
            console.error('获取图片信息失败:', err);
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

    const that = this;
    const originalSize = this.data.originalSize;

    console.log('开始压缩，原图大小:', originalSize);

    wx.uploadFile({
      url: API_BASE_URL + '/api/image/compress',
      filePath: this.data.imagePath,
      name: 'file',
      formData: {
        quality: this.data.quality.toString()
      },
      success: (res) => {
        wx.hideLoading();
        that.setData({ loading: false });
        try {
          const data = JSON.parse(res.data);
          console.log('上传成功，返回数据:', data);
          
          if (data.code === 200 && data.data) {
            const result = data.data;
            
            // 设置压缩后的图片URL，同时保留originalSize
            that.setData({
              resultImage: result.fileUrl,
              originalSize: originalSize  // 关键：确保originalSize被设置
            });
            
            // 尝试获取压缩后文件大小
            if (result.fileUrl) {
              that.getCompressedFileSize(result.fileUrl, originalSize);
            } else {
              // 如果没有文件URL，使用后端返回的大小
              const compressedSize = result.outputSize || result.compressedSize || 0;
              that.calculateCompressionRate(originalSize, compressedSize);
            }
            
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
        that.setData({ loading: false });
        wx.showToast({ title: '上传失败', icon: 'none' });
        console.error('上传失败:', err);
      }
    });
  },

  /**
   * 获取压缩后文件大小
   * @param {string} fileUrl - 文件URL
   * @param {number} originalSize - 原图大小
   */
  getCompressedFileSize(fileUrl, originalSize) {
    const that = this;
    
    console.log('开始获取压缩后文件大小, 原图大小:', originalSize);
    
    // 先下载文件到本地
    wx.downloadFile({
      url: fileUrl,
      success(downloadRes) {
        console.log('下载文件成功, 临时路径:', downloadRes.tempFilePath);
        
        if (downloadRes.statusCode === 200) {
          // 使用 getFileSystemManager 获取文件大小
          const fs = wx.getFileSystemManager();
          fs.getFileInfo({
            filePath: downloadRes.tempFilePath,
            success(fileInfo) {
              console.log('压缩后文件大小:', fileInfo.size);
              that.calculateCompressionRate(originalSize, fileInfo.size);
            },
            fail(err) {
              console.error('获取压缩后文件大小失败:', err);
              wx.showToast({ title: '文件大小获取失败', icon: 'none' });
              that.setData({
                compressedSize: 0,
                savedSize: 0,
                compressionRate: '0%'
              });
            }
          });
        } else {
          console.error('下载文件失败:', downloadRes.statusCode);
          wx.showToast({ title: '文件大小获取失败', icon: 'none' });
          that.setData({
            compressedSize: 0,
            savedSize: 0,
            compressionRate: '0%'
          });
        }
      },
      fail(err) {
        console.error('下载文件失败:', err);
        wx.showToast({ title: '文件大小获取失败', icon: 'none' });
        that.setData({
          compressedSize: 0,
          savedSize: 0,
          compressionRate: '0%'
        });
      }
    });
  },

  /**
   * 计算压缩率和节省空间
   * @param {number} originalSize - 原图大小
   * @param {number} compressedSize - 压缩后大小
   */
  calculateCompressionRate(originalSize, compressedSize) {
    console.log('计算压缩率:', originalSize, compressedSize);
    
    let savedSize = 0;
    let compressionRate = '0%';
    
    if (originalSize > 0 && compressedSize > 0) {
      // 计算节省空间
      savedSize = originalSize - compressedSize;
      if (savedSize < 0) savedSize = 0;
      
      // 计算压缩率：((原图大小 - 压缩后大小) / 原图大小) × 100
      compressionRate = ((originalSize - compressedSize) / originalSize * 100).toFixed(2) + '%';
    }
    
    console.log('计算结果: savedSize=', savedSize, 'compressionRate=', compressionRate);
    
    this.setData({
      compressedSize: compressedSize,
      savedSize: savedSize,
      compressionRate: compressionRate,
      originalSizeText: this.formatSize(originalSize),
      compressedSizeText: this.formatSize(compressedSize),
      savedSizeText: this.formatSize(savedSize)
    });
    
    console.log('=== 调试信息 ===');
    console.log('originalSize=', this.data.originalSize);
    console.log('compressedSize=', compressedSize);
    console.log('savedSize=', savedSize);
    console.log('compressionRate=', compressionRate);
    console.log('格式化后originalSizeText=', this.formatSize(originalSize));
    console.log('格式化后compressedSizeText=', this.formatSize(compressedSize));
    console.log('格式化后savedSizeText=', this.formatSize(savedSize));
    console.log('this.data=', this.data);
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
      originalSize: 0,
      compressedSize: 0,
      savedSize: 0,
      compressionRate: ''
    });
  }
});