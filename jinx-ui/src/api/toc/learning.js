import request from '@/utils/request'

/**
 * 获取用户的学习任务列表
 * @param {Object} params - 查询参数
 * @param {string} params.type - 任务类型：train-培训，map-学习地图
 * @param {number} params.required - 是否必修：0-全部，1-必修，2-选修，默认0
 * @param {number} params.pageNum - 页码，默认1
 * @param {number} params.pageSize - 每页条数，默认10
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningTasks(params) {
  return request({
    url: '/api/learning/tasks',
    method: 'get',
    params
  })
}

/**
 * 获取学习中心数据
 * @param {Object} params - 请求参数
 * @param {string} params.type - 内容类型：course-课程，train-培训，map-学习地图，exam-考试，practice-练习，默认全部
 * @param {number} params.categoryId - 分类ID，默认全部
 * @param {string} params.contentTypes - 内容类型筛选：video-视频，document-文档，series-系列课，多个用逗号分隔，默认全部
 * @param {string} params.keyword - 搜索关键词
 * @param {number} params.pageNum - 页码，默认1
 * @param {number} params.pageSize - 每页条数，默认10
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningCenter(params) {
  return request({
    url: '/api/learning/center',
    method: 'get',
    params
  })
}

/**
 * 记录学习进度
 * @param {Object} data - 请求参数
 * @param {string} data.type - 内容类型：course-课程，train-培训内容，map-学习地图任务
 * @param {number} data.contentId - 内容ID
 * @param {number} data.progress - 学习进度(百分比)
 * @param {number} data.duration - 本次学习时长(秒)
 * @param {number} data.parentId - 父内容ID(培训ID或学习地图阶段ID)，可选
 * @returns {Promise} - 返回Promise对象，包含状态码、消息和数据(status, progress, isCompleted)
 */
export function recordLearningProgress(data) {
  return request({
    url: '/api/learning/record',
    method: 'post',
    data
  })
}

/**
 * 获取用户信息
 * @returns {Promise} - 返回Promise对象
 */
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}
