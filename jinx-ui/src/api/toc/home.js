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

/**
 * 获取首页轮播图列表
 * @returns {Promise} 请求Promise对象
 */
export function getBanners(params) {
  return request({
    url: '/api/toc/home/banners',
    method: 'get',
    params
  })
}

/**
 * 获取学习任务列表
 * @param {Object} params 请求参数
 * @param {string} [params.type] 任务类型：train-培训，map-学习地图，默认全部
 * @param {string} [params.status] 状态筛选：all-全部，required-必修，elective-选修，默认全部
 * @param {number} [params.pageNum] 页码，默认1
 * @param {number} [params.pageSize] 每页条数，默认10
 * @returns {Promise} 请求Promise对象
 */
export function getLearningTasks(params) {
  return request({
    url: '/api/toc/learning/tasks',
    method: 'get',
    params
  })
}

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
