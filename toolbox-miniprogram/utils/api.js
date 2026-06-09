// utils/api.js - 微信小程序API工具类
const BASE_URL = 'http://localhost:8080/api'

function request(url, method = 'GET', data) {
  return new Promise((resolve, reject) => {
    const token = wx.getStorageSync('token')
    const header = { 'content-type': 'application/json' }
    if (token) header['Authorization'] = 'Bearer ' + token
    
    wx.request({
      url: BASE_URL + url,
      method,
      data,
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          resolve(res.data)
        } else if (res.statusCode === 401) {
          wx.removeStorageSync('token')
          wx.showToast({ title: '请先登录', icon: 'none' })
          reject(res.data)
        } else {
          reject(res.data)
        }
      },
      fail: reject
    })
  })
}

function upload(url, filePath, name = 'file', formData = {}) {
  return new Promise((resolve, reject) => {
    const header = {}
    const token = wx.getStorageSync('token')
    if (token) header['Authorization'] = 'Bearer ' + token
    
    wx.uploadFile({
      url: BASE_URL + url,
      filePath,
      name,
      formData,
      header,
      success: (res) => {
        if (res.statusCode === 200) {
          try {
            resolve(JSON.parse(res.data))
          } catch (e) {
            resolve(res.data)
          }
        } else {
          reject({ message: '上传失败' })
        }
      },
      fail: reject
    })
  })
}

// 批量上传文件
function uploadMultiple(url, filePaths, name = 'files', formData = {}) {
  return new Promise((resolve, reject) => {
    const uploadTasks = filePaths.map((filePath, index) => {
      return new Promise((res, rej) => {
        wx.uploadFile({
          url: BASE_URL + url,
          filePath,
          name,
          formData: { ...formData, index },
          success: (uploadRes) => {
            if (uploadRes.statusCode === 200) {
              try {
                res(JSON.parse(uploadRes.data))
              } catch (e) {
                res(uploadRes.data)
              }
            } else {
              rej({ message: '上传失败' })
            }
          },
          fail: rej
        })
      })
    })
    
    Promise.all(uploadTasks).then(resolve).catch(reject)
  })
}

// ========== 工具相关API ==========

// 获取工具列表
export function getTools() {
  return request('/tools', 'GET')
}

// ========== PDF相关API ==========

// PDF合并
export function mergePdf(filePaths) {
  return new Promise((resolve, reject) => {
    if (filePaths.length < 2) {
      wx.showToast({ title: '请至少选择2个PDF', icon: 'none' })
      return reject({ message: '至少需要2个文件' })
    }
    
    let uploadedCount = 0
    let finalResult = null
    
    filePaths.forEach((filePath, index) => {
      wx.uploadFile({
        url: BASE_URL + '/pdf/merge',
        filePath,
        name: 'files',
        formData: { index },
        success: (res) => {
          uploadedCount++
          if (res.statusCode === 200) {
            try {
              const result = JSON.parse(res.data)
              finalResult = result
              if (uploadedCount === filePaths.length) {
                resolve(finalResult)
              }
            } catch (e) {
              if (uploadedCount === filePaths.length) {
                resolve(finalResult)
              }
            }
          } else {
            if (uploadedCount === filePaths.length) {
              resolve(finalResult)
            }
          }
        },
        fail: () => {
          uploadedCount++
          if (uploadedCount === filePaths.length) {
            resolve(finalResult)
          }
        }
      })
    })
  })
}

// PDF拆分
export function splitPdf(filePath, pageRange) {
  return upload('/pdf/split', filePath, 'file', { pageRange })
}

// Word转PDF
export function wordToPdf(filePath) {
  return upload('/pdf/word-to-pdf', filePath, 'file')
}

// 图片转PDF
export function imageToPdf(filePaths) {
  return new Promise((resolve, reject) => {
    let uploadedCount = 0
    let finalResult = null
    
    filePaths.forEach((filePath, index) => {
      wx.uploadFile({
        url: BASE_URL + '/pdf/image-to-pdf',
        filePath,
        name: 'files',
        formData: { index },
        success: (res) => {
          uploadedCount++
          if (res.statusCode === 200) {
            try {
              const result = JSON.parse(res.data)
              finalResult = result
              if (uploadedCount === filePaths.length) {
                resolve(finalResult)
              }
            } catch (e) {
              if (uploadedCount === filePaths.length) {
                resolve(finalResult)
              }
            }
          } else {
            if (uploadedCount === filePaths.length) {
              resolve(finalResult)
            }
          }
        },
        fail: () => {
          uploadedCount++
          if (uploadedCount === filePaths.length) {
            resolve(finalResult)
          }
        }
      })
    })
  })
}

// PDF转图片
export function pdfToImage(filePath, page = 1) {
  return upload('/pdf/to-image', filePath, 'file', { page })
}

// 获取PDF信息
export function getPdfInfo(filePath) {
  return upload('/pdf/info', filePath)
}

// ========== 图片相关API ==========

// 图片压缩
export function compressImage(filePath, width, quality) {
  return upload('/image/compress', filePath, 'file', { width, quality })
}

// 证件照换底色
export function changeBackground(filePath, color) {
  return upload('/image/background', filePath, 'file', { color })
}

// ========== 文件相关API ==========

// 上传文件
export function uploadFile(filePath) {
  return upload('/file/upload', filePath)
}

module.exports = {
  getTools,
  mergePdf,
  splitPdf,
  wordToPdf,
  imageToPdf,
  pdfToImage,
  getPdfInfo,
  compressImage,
  changeBackground,
  ocrImage,
  uploadFile,
  request,
  upload,
  // 客服API
  startSession: (data) => request('/customer/session/start', 'POST', data),
  endSession: (data) => request('/customer/session/end', 'POST', data),
  getSession: (sessionNo) => request(`/customer/session/${sessionNo}`),
  getMessages: (sessionNo) => request(`/customer/messages/${sessionNo}`),
  transferToHuman: (data) => request('/customer/transfer', 'POST', data)
}
