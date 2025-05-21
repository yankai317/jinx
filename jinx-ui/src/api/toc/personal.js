import request from '@/utils/request'

/**
 * 获取用户个人中心信息
 * @returns {Promise} 请求结果
 */
export function getUserProfile() {
  return request({
    url: '/api/user/profile',
    method: 'get'
  })
}

/**
 * 获取用户学习记录
 * @param {Object} params 请求参数
 * @returns {Promise} 请求结果
 */
export function getUserLearningRecords(params) {
  return request({
    url: '/api/user/learning/records',
    method: 'get',
    params
  })
}

/**
 * 获取用户学习统计数据
 * @returns {Promise} 请求结果
 */
export function getUserLearningStatistics() {
  return request({
    url: '/api/toc/personal/learning/statistics',
    method: 'get'
  })
}

/**
 * 获取用户证书列表
 * @param {Object} params 请求参数
 * @returns {Promise} 请求结果
 */
export function getUserCertificates(params) {
  return request({
    url: '/api/user/certificates',
    method: 'get',
    params
  })
}

/**
 * 获取用户信息
 * @returns {Promise} 请求结果
 */
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

/**
 * 获取用户学习统计数据
 * @param {boolean} countSelective 是否选择性统计
 * @returns {Promise} 请求结果
 */
export function getUserLearningTotal(countSelective) {
  return request({
    url: '/api/user/learning/total',
    method: 'get',
    params: { countSelective }
  })
}

