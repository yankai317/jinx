import request from '@/utils/request'

/**
 * 获取首页轮播图列表
 * @returns {Promise<AxiosResponse<any>>}
 */
export function getBanners() {
  return request({
    url: '/api/toc/home/banners',
    method: 'get'
  })
}
