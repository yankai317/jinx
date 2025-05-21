import request from '@/utils/request'

/**
 * 获取首页数据
 * @returns {Promise<any>} 首页数据
 */
export function getHomeData() {
  return request({
    url: '/api/toc/home/data',
    method: 'get'
  })
}
