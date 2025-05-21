import request from '@/utils/request'

/**
 * 获取首页轮播图列表
 * @returns {Promise} 请求Promise对象
 */
export function getBanners() {
  return request({
    url: '/api/toc/home/banners',
    method: 'get'
  })
}
