Page({
  data: {
    products: [
      {
        id: 1,
        name: '月度VIP',
        price: '29.9',
        period: '30天',
        features: ['无限使用所有工具', '无广告', '高级模板'],
        recommended: false
      },
      {
        id: 2,
        name: '年度VIP',
        price: '299',
        period: '365天',
        features: ['无限使用所有工具', '无广告', '高级模板', '专属客服'],
        recommended: true
      },
      {
        id: 3,
        name: '永久VIP',
        price: '999',
        period: '永久',
        features: ['永久无限使用', '无广告', '高级模板', '专属客服'],
        recommended: false
      }
    ]
  },
  
  selectProduct: function(e) {
    const id = e.currentTarget.dataset.id
    const products = this.data.products.map(p => ({
      ...p,
      recommended: p.id === id
    }))
    this.setData({ products })
  },
  
  buyVip: function(e) {
    const product = e.currentTarget.dataset.product
    wx.showToast({ title: '购买功能开发中', icon: 'none' })
  }
})
