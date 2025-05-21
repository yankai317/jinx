import request from '@/utils/request'

/**
 * 搜索课程、培训和学习地图内容
 * @param {Object} params - 查询参数
 * @param {string} params.keyword - 搜索关键词
 * @param {string} params.type - 内容类型：all-全部，course-课程，train-培训，map-学习地图，默认all
 * @param {number} params.pageNum - 页码，默认1
 * @param {number} params.pageSize - 每页条数，默认10
 * @returns {Promise} - 返回Promise对象
 */
export function searchContent(params) {
  return request({
    url: '/api/search',
    method: 'get',
    params
  })
}

/**
 * 搜索课程、培训和学习地图内容（POST方法）
 * @param {Object} data - 请求参数
 * @param {string} data.keyword - 搜索关键词
 * @param {string} data.type - 内容类型：all-全部，course-课程，train-培训，map-学习地图，默认all
 * @param {number} data.pageNum - 页码，默认1
 * @param {number} data.pageSize - 每页条数，默认10
 * @returns {Promise} - 返回Promise对象
 */
export function searchContentPost(data) {
  return request({
    url: '/api/search',
    method: 'post',
    data
  })
}
