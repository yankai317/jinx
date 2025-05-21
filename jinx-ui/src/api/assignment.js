import request from '@/utils/request'

/**
 * 查询指派记录
 * @param {Object} params 查询参数
 * @returns {Promise} 请求Promise
 */
export function queryAssignRecords(params) {
  return request({
    url: '/api/assignment/records/query',
    method: 'post',
    data: params
  })
}

/**
 * 查询指派记录明细
 * @param {Object} params 查询参数
 * @returns {Promise} 请求Promise
 */
export function queryAssignRecordDetails(params) {
  return request({
    url: '/api/assignment/records/detail',
    method: 'post',
    data: params
  })
}
