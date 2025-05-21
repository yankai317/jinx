import request from '@/utils/request'

/**
 * 获取学习排行榜数据
 * @param {Object} params 请求参数
 * @param {string} [params.type] 排行榜类型：all-全员，department-部门，默认全员
 * @param {number} [params.limit] 返回数量限制，默认10
 * @returns {Promise} 请求Promise对象
 */
export function getRankings(params) {
  return request({
    url: '/api/toc/rankings',
    method: 'get',
    params
  })
}
