const API_BASE_URL = 'http://localhost:8080/api';

Page({
    data: {
        imagePath: '',
        selectedColor: 'blue',
        loading: false,
        resultImage: ''
    },

    colorMap: {
        white: '#ffffff',
        blue: '#4385f5',
        red: '#ff4b4b'
    },

    onLoad() {
        console.log('证件照换底色页面加载');
        console.log('API地址:', API_BASE_URL);
    },

    chooseImage() {
        wx.chooseImage({
            count: 1,
            sizeType: ['compressed'],
            sourceType: ['album', 'camera'],
            success: (res) => {
                console.log('选择图片成功:', res.tempFilePaths);
                this.setData({
                    imagePath: res.tempFilePaths[0],
                    resultImage: ''
                });
            },
            fail: (err) => {
                console.error('选择图片失败:', err);
                wx.showToast({
                    title: '选择图片失败',
                    icon: 'none'
                });
            }
        });
    },

    selectColor(e) {
        const color = e.currentTarget.dataset.color;
        console.log('选择颜色:', color);
        this.setData({
            selectedColor: color
        });
    },

    async processImage() {
        if (!this.data.imagePath) {
            wx.showToast({
                title: '请先上传图片',
                icon: 'none'
            });
            return;
        }

        this.setData({ loading: true });

        try {
            console.log('开始处理图片...');
            await this.uploadAndProcess();
        } catch (error) {
            console.error('处理失败:', error);
            let errorMsg = '处理失败';
            if (error.errMsg) {
                errorMsg = error.errMsg;
            } else if (error.message) {
                errorMsg = error.message;
            }
            wx.showModal({
                title: '提示',
                content: errorMsg + '\n\n请确认：\n1. Spring Boot后端已启动\n2. Rembg服务已启动\n3. 已勾选"不校验合法域名"',
                showCancel: false
            });
        } finally {
            this.setData({ loading: false });
        }
    },

    uploadAndProcess() {
        return new Promise((resolve, reject) => {
            console.log('开始上传图片...');

            wx.uploadFile({
                url: API_BASE_URL + '/image/background',
                filePath: this.data.imagePath,
                name: 'file',
                formData: {
                    color: this.data.selectedColor
                },
                success: (res) => {
                    console.log('上传成功，原始响应:', res);

                    try {
                        const data = JSON.parse(res.data);
                        console.log('解析后的数据:', data);

                        if (data.code === 200 && data.data) {
                            console.log('处理成功，文件地址:', data.data.fileUrl);
                            this.downloadResultImage(data.data.fileUrl, resolve, reject);
                        } else {
                            reject(new Error(data.msg || '服务器返回错误'));
                        }
                    } catch (e) {
                        console.error('JSON解析失败:', e);
                        reject(new Error('服务器响应格式错误: ' + res.data));
                    }
                },
                fail: (err) => {
                    console.error('上传失败:', err);
                    reject(err);
                }
            });
        });
    },

    downloadResultImage(fileUrl, resolve, reject) {
        console.log('开始下载结果图片:', fileUrl);
        
        // 拼接完整URL
        let downloadUrl = fileUrl;
        if (!fileUrl.startsWith('http')) {
            downloadUrl = API_BASE_URL + fileUrl;
        }
        console.log('完整下载URL:', downloadUrl);

        wx.downloadFile({
            url: downloadUrl,
            success: (res) => {
                console.log('下载完成:', res);
                if (res.statusCode === 200) {
                    this.setData({
                        resultImage: res.tempFilePath
                    });
                    wx.showToast({
                        title: '处理成功',
                        icon: 'success'
                    });
                    resolve();
                } else {
                    reject(new Error('下载失败，状态码: ' + res.statusCode));
                }
            },
            fail: (err) => {
                console.error('下载失败:', err);
                reject(err);
            }
        });
    },

    saveImage() {
        if (!this.data.resultImage) {
            wx.showToast({
                title: '暂无图片',
                icon: 'none'
            });
            return;
        }

        wx.saveImageToPhotosAlbum({
            filePath: this.data.resultImage,
            success: () => {
                wx.showToast({
                    title: '保存成功',
                    icon: 'success'
                });
            },
            fail: (err) => {
                console.error('保存失败:', err);
                if (err.errMsg && err.errMsg.includes('auth deny')) {
                    wx.showModal({
                        title: '提示',
                        content: '需要您授权保存图片到相册',
                        success: (modalRes) => {
                            if (modalRes.confirm) {
                                wx.openSetting();
                            }
                        }
                    });
                } else {
                    wx.showToast({
                        title: '保存失败',
                        icon: 'none'
                    });
                }
            }
        });
    }
});
