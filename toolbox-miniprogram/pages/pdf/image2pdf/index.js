// pages/pdf/image2pdf/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    images: [],
    tempFilePaths: [],
    loading: false,
    resultPdf: '',
    pageCount: 0,
    sessionId: ''
  },

  onLoad() {
    // 生成会话ID
    this.setData({
      sessionId: 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
    });
    console.log('图片转PDF页面加载，Session:', this.data.sessionId);
  },

  onUnload() {
    // 页面卸载时清除会话
    if (this.data.sessionId) {
      this.clearSession();
    }
  },

  chooseImages() {
    const currentCount = this.data.tempFilePaths.length;
    const remaining = 20 - currentCount;
    
    if (remaining <= 0) {
      wx.showToast({
        title: '最多选择20张图片',
        icon: 'none'
      });
      return;
    }

    wx.chooseImage({
      count: remaining,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: (res) => {
        console.log('选择图片成功:', res.tempFilePaths.length);
        const newPaths = [...this.data.tempFilePaths, ...res.tempFilePaths];
        this.setData({
          tempFilePaths: newPaths,
          resultPdf: ''
        });
      },
      fail: (err) => {
        console.error('选择图片失败:', err);
        wx.showToast({
          title: '选择失败',
          icon: 'none'
        });
      }
    });
  },

  removeImage(e) {
    const index = e.currentTarget.dataset.index;
    const tempFilePaths = this.data.tempFilePaths;
    tempFilePaths.splice(index, 1);
    this.setData({
      tempFilePaths: tempFilePaths,
      resultPdf: ''
    });
  },

  moveImage(e) {
    const index = e.currentTarget.dataset.index;
    const tempFilePaths = this.data.tempFilePaths;
    const direction = e.currentTarget.dataset.direction;
    
    if (direction === 'up' && index > 0) {
      const temp = tempFilePaths[index];
      tempFilePaths[index] = tempFilePaths[index - 1];
      tempFilePaths[index - 1] = temp;
    } else if (direction === 'down' && index < tempFilePaths.length - 1) {
      const temp = tempFilePaths[index];
      tempFilePaths[index] = tempFilePaths[index + 1];
      tempFilePaths[index + 1] = temp;
    }
    
    this.setData({
      tempFilePaths: tempFilePaths
    });
  },

  async convertToPdf() {
    if (this.data.tempFilePaths.length === 0) {
      wx.showToast({
        title: '请先选择图片',
        icon: 'none'
      });
      return;
    }

    this.setData({ loading: true });

    try {
      console.log('开始上传并转换...');
      await this.uploadAndConvert();
    } catch (error) {
      console.error('转换失败:', error);
      let errorMsg = '转换失败';
      if (error.message) {
        errorMsg = error.message;
      }
      wx.showModal({
        title: '提示',
        content: errorMsg,
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  uploadAndConvert() {
    return new Promise((resolve, reject) => {
      const tempFilePaths = this.data.tempFilePaths;
      const sessionId = this.data.sessionId;
      
      wx.showLoading({
        title: '正在上传第1张...',
        mask: true
      });

      // 逐个上传图片
      let uploadedCount = 0;
      const total = tempFilePaths.length;

      const uploadNext = (index) => {
        if (index >= total) {
          // 所有图片上传完成，开始转换
          wx.showLoading({
            title: '正在生成PDF...',
            mask: true
          });
          
          // 调用批量转换接口
          wx.request({
            url: API_BASE_URL + '/api/pdf-batch/convert',
            method: 'GET',
            data: {
              sessionId: sessionId,
              fileCount: total
            },
            success: (res) => {
              console.log('转换结果:', res);
              if (res.statusCode === 200 && res.data) {
                const data = res.data;
                if (data.code === 200 && data.data) {
                  const pdfUrl = data.data.fileUrl;
                  this.setData({
                    resultPdf: pdfUrl,
                    pageCount: data.data.pageCount
                  });
                  wx.hideLoading();
                  
                  // 提示用户
                  wx.showModal({
                    title: 'PDF已生成',
                    content: '正在打开PDF预览，请通过右上角"..."菜单分享或另存',
                    showCancel: false,
                    success: () => {
                      // 自动打开PDF预览
                      this.openPdfPreview(pdfUrl);
                    }
                  });
                  resolve();
                } else {
                  wx.hideLoading();
                  reject(new Error(data.msg || '转换失败'));
                }
              } else {
                wx.hideLoading();
                reject(new Error('服务器响应错误: ' + res.statusCode));
              }
            },
            fail: (err) => {
              wx.hideLoading();
              console.error('转换失败:', err);
              reject(err);
            }
          });
          return;
        }

        // 上传单张图片
        wx.uploadFile({
          url: API_BASE_URL + '/api/pdf-batch/upload-image',
          filePath: tempFilePaths[index],
          name: 'file',
          formData: {
            sessionId: sessionId
          },
          success: (res) => {
            console.log('上传第' + (index + 1) + '张成功');
            uploadedCount++;
            wx.showLoading({
              title: '正在上传第' + (index + 2) + '张...',
              mask: true
            });
            uploadNext(index + 1);
          },
          fail: (err) => {
            wx.hideLoading();
            console.error('上传第' + (index + 1) + '张失败:', err);
            reject(new Error('上传失败'));
          }
        });
      };

      // 开始上传
      uploadNext(0);
    });
  },

  /**
   * 打开PDF预览
   * @param {string} pdfUrl - PDF下载地址
   */
  openPdfPreview(pdfUrl) {
    wx.showLoading({
      title: '正在打开PDF...',
      mask: true
    });

    // 下载PDF到临时目录
    wx.downloadFile({
      url: pdfUrl,
      success: (res) => {
        wx.hideLoading();
        
        if (res.statusCode === 200) {
          console.log('PDF下载成功，准备打开:', res.tempFilePath);
          
          // 打开PDF预览
          wx.openDocument({
            filePath: res.tempFilePath,
            fileType: 'pdf',
            showMenu: true,
            success: () => {
              console.log('PDF预览成功');
            },
            fail: (err) => {
              console.error('PDF预览失败:', err);
              wx.showModal({
                title: '打开失败',
                content: '无法打开PDF文件，请检查是否安装了PDF阅读器',
                showCancel: false
              });
            }
          });
        } else {
          console.error('PDF下载失败，状态码:', res.statusCode);
          wx.showModal({
            title: '下载失败',
            content: 'PDF文件下载失败，请重试',
            showCancel: false
          });
        }
      },
      fail: (err) => {
        wx.hideLoading();
        console.error('下载失败:', err);
        wx.showModal({
          title: '下载失败',
          content: 'PDF文件下载失败，请检查网络连接后重试',
          showCancel: false
        });
      }
    });
  },

  /**
   * 预览PDF（兼容旧调用）
   */
  previewPdf() {
    if (!this.data.resultPdf) {
      wx.showToast({
        title: '暂无PDF文件',
        icon: 'none'
      });
      return;
    }
    this.openPdfPreview(this.data.resultPdf);
  },

  /**
   * 分享/保存PDF
   * 微信小程序没有直接保存PDF的API，通过打开PDF后让用户手动分享
   */
  downloadPdf() {
    if (!this.data.resultPdf) {
      wx.showToast({
        title: '暂无PDF文件',
        icon: 'none'
      });
      return;
    }

    wx.showModal({
      title: '保存PDF',
      content: 'PDF将打开预览，请点击右上角"..."菜单选择"分享"或"保存"',
      confirmText: '打开PDF',
      cancelText: '取消',
      success: (res) => {
        if (res.confirm) {
          this.openPdfPreview(this.data.resultPdf);
        }
      }
    });
  },

  clearAll() {
    wx.showModal({
      title: '确认清空',
      content: '确定要清空所有图片吗？',
      success: (res) => {
        if (res.confirm) {
          this.clearSession();
          this.setData({
            tempFilePaths: [],
            resultPdf: '',
            pageCount: 0,
            sessionId: 'session_' + Date.now() + '_' + Math.random().toString(36).substr(2, 9)
          });
        }
      }
    });
  },

  clearSession() {
    if (this.data.sessionId) {
      wx.request({
        url: API_BASE_URL + '/api/pdf-batch/clear',
        method: 'DELETE',
        data: {
          sessionId: this.data.sessionId
        },
        fail: (err) => {
          console.error('清除会话失败:', err);
        }
      });
    }
  }
});