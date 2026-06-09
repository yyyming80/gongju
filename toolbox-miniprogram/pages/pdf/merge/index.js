// pages/pdf/merge/index.js
const API_BASE_URL = 'http://localhost:8080';

Page({
  data: {
    fileList: [],
    tempFilePaths: [],
    loading: false,
    resultPdf: '',
    resultFileName: '',
    resultFileSize: '',
    sessionId: ''  // 添加sessionId到data中
  },

  chooseFiles() {
    const that = this;
    wx.chooseMessageFile({
      count: 20,
      type: 'file',
      extension: ['pdf'],
      success(res) {
        console.log('选择文件成功:', res.tempFiles.length);
        const newFiles = res.tempFiles.map(file => ({
          name: file.name,
          size: file.size,
          path: file.path
        }));
        that.setData({
          fileList: [...that.data.fileList, ...newFiles],
          sessionId: 'pdf_merge_' + Date.now()  // 选择文件时生成sessionId
        });
        console.log('生成了新的SessionId:', that.data.sessionId);
      },
      fail: (err) => {
        console.error('选择文件失败:', err);
        wx.showToast({ title: '选择失败', icon: 'none' });
      }
    });
  },

  deleteFile(e) {
    const index = e.currentTarget.dataset.index;
    const fileList = this.data.fileList;
    fileList.splice(index, 1);
    this.setData({ fileList });
  },

  clearAll() {
    wx.showModal({
      title: '提示',
      content: '确定清空所有文件吗？',
      success: (res) => {
        if (res.confirm) {
          this.setData({
            fileList: [],
            resultPdf: '',
            resultFileName: '',
            resultFileSize: '',
            sessionId: ''
          });
        }
      }
    });
  },

  async mergePdf() {
    if (this.data.fileList.length < 2) {
      wx.showToast({ title: '至少需要2个PDF文件', icon: 'none' });
      return;
    }

    // 检查sessionId
    if (!this.data.sessionId) {
      wx.showToast({ title: '请先选择文件', icon: 'none' });
      return;
    }

    this.setData({ loading: true });

    try {
      await this.uploadAndMerge();
    } catch (error) {
      console.error('合并失败:', error);
      wx.showModal({
        title: '提示',
        content: error.message || '合并失败，请重试',
        showCancel: false
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  uploadAndMerge() {
    return new Promise((resolve, reject) => {
      const fileList = this.data.fileList;
      const total = fileList.length;
      const sessionId = this.data.sessionId;  // 使用data中的sessionId
      let uploadedCount = 0;

      console.log('=== 开始上传 ===');
      console.log('SessionId:', sessionId);
      console.log('文件数量:', total);

      wx.showLoading({ title: '正在上传第1张...', mask: true });

      const uploadNext = (index) => {
        if (index >= total) {
          // 所有文件上传完成，调用合并接口
          console.log('=== 所有文件上传完成，准备合并 ===');
          wx.showLoading({ title: '正在合并PDF...', mask: true });
          this.callMergeApi(sessionId).then(resolve).catch(reject);
          return;
        }

        console.log('上传第' + (index + 1) + '张:', fileList[index].name);
        console.log('filePath:', fileList[index].path);

        wx.uploadFile({
          url: API_BASE_URL + '/api/pdf-merge/upload',
          filePath: fileList[index].path,
          name: 'file',
          formData: {
            sessionId: sessionId,  // 使用统一的sessionId
            index: index.toString()
          },
          success: (res) => {
            console.log('上传第' + (index + 1) + '张成功, 响应:', res.data);
            uploadedCount++;
            wx.showLoading({
              title: '已上传 ' + uploadedCount + '/' + total,
              mask: true
            });
            uploadNext(index + 1);
          },
          fail: (err) => {
            wx.hideLoading();
            console.error('上传失败:', err);
            reject(new Error('上传失败'));
          }
        });
      };

      uploadNext(0);
    });
  },

  callMergeApi(sessionId) {
    return new Promise((resolve, reject) => {
      console.log('=== 调用合并接口 ===');
      console.log('SessionId:', sessionId);

      wx.request({
        url: API_BASE_URL + '/api/pdf-merge/merge',
        method: 'GET',
        data: {
          sessionId: sessionId  // 使用传入的sessionId
        },
        success: (res) => {
          console.log('合并结果:', res);
          wx.hideLoading();
          if (res.statusCode === 200 && res.data) {
            const data = res.data;
            if (data.code === 200 && data.data) {
              this.setData({
                resultPdf: data.data.fileUrl,
                resultFileName: data.data.fileName,
                resultFileSize: data.data.fileSize
              });
              wx.showToast({ title: '合并成功', icon: 'success' });
              resolve();
            } else {
              reject(new Error(data.msg || '合并失败'));
            }
          } else {
            reject(new Error('服务器响应错误'));
          }
        },
        fail: (err) => {
          wx.hideLoading();
          console.error('合并请求失败:', err);
          reject(new Error('合并请求失败'));
        }
      });
    });
  },

  previewPdf() {
    if (!this.data.resultPdf) {
      wx.showToast({ title: '暂无PDF文件', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '正在加载...', mask: true });
    wx.downloadFile({
      url: this.data.resultPdf,
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.openDocument({
            filePath: res.tempFilePath,
            fileType: 'pdf',
            showMenu: true,
            success: () => console.log('PDF预览成功'),
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

  downloadPdf() {
    if (!this.data.resultPdf) {
      wx.showToast({ title: '暂无文件', icon: 'none' });
      return;
    }

    wx.showLoading({ title: '正在保存...', mask: true });
    wx.downloadFile({
      url: this.data.resultPdf,
      success: (res) => {
        wx.hideLoading();
        if (res.statusCode === 200) {
          wx.saveFile({
            tempFilePath: res.tempFilePath,
            success: (saveRes) => {
              wx.showModal({
                title: '保存成功',
                content: '文件已保存到: ' + saveRes.savedFilePath,
                showCancel: false
              });
            },
            fail: () => {
              wx.showModal({
                title: '提示',
                content: '文件已生成，请在预览页面右上角分享',
                showCancel: false
              });
            }
          });
        }
      },
      fail: () => {
        wx.hideLoading();
        wx.showToast({ title: '保存失败', icon: 'none' });
      }
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