import request from '@/utils/request';

/**
 * 获取操作日志
 * @param {Object} params - 查询参数
 * @param {String} params.businessType - 业务类型
 * @param {Number} params.businessId - 业务ID
 * @param {Number} params.pageNum - 页码
 * @param {Number} params.pageSize - 每页条数
 * @returns {Promise} - 返回Promise对象
 */
export function getOperationLogs(params) {
  return request({
    url: '/api/operation/logs',
    method: 'post',
    data: params
  });
}

/**
 * 获取指派记录
 * @param {Number} businessId - 业务ID
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getAssignRecords(businessId, params) {
  return request({
    url: `/api/common/assign/records/${businessId}`,
    method: 'post',
    data: params
  });
}

/**
 * 获取指派记录
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function queryAssignRecords(params) {
  return request({
    url: '/api/assignment/records/query',
    method: 'post',
    data: params
  });
}
