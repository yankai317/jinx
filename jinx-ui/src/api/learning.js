import request from '@/utils/request'

/**
 * 获取学习任务列表
 * @param {Object} params 查询参数
 * @returns {Promise} 请求结果
 */
export function getLearningTasks(params) {
  return request({
    url: '/api/toc/learning/tasks',
    method: 'get',
    params
  })
}

/**
 * 获取学习中心数据
 * @param {Object} data 请求参数
 * @returns {Promise} 请求结果
 */
export function getLearningCenter(data) {
  return request({
    url: '/api/toc/learning/center',
    method: 'post',
    data
  })
}

