import request from '@/utils/request';

/**
 * 获取课程列表
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getCourseList(params) {
  return request({
    url: '/api/course/list',
    method: 'post',
    data: params
  });
}

/**
 * 获取课程详情
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function getCourseDetail(id) {
  return request({
    url: `/api/course/detail/${id}`,
    method: 'get'
  });
}

/**
 * 创建课程
 * @param {Object} data - 课程数据
 * @returns {Promise} - 返回Promise对象
 */
export function createCourse(data) {
  return request({
    url: '/api/course/create',
    method: 'post',
    data
  });
}

/**
 * 更新课程
 * @param {Object} data - 课程数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateCourse(data) {
  return request({
    url: '/api/course/update',
    method: 'put',
    data
  });
}

/**
 * 删除课程
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteCourse(id) {
  return request({
    url: `/api/course/delete/${id}`,
    method: 'post'
  });
}

/**
 * 批量删除课程
 * @param {Array} ids - 课程ID数组
 * @returns {Promise} - 返回Promise对象
 */
export function batchDeleteCourse(ids) {
  return request({
    url: '/api/course/batchDelete',
    method: 'post',
    data: { ids }
  });
}

/**
 * 发布课程
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function publishCourse(id) {
  return request({
    url: `/api/course/publish/${id}`,
    method: 'post'
  });
}

/**
 * 取消发布课程
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function unpublishCourse(id) {
  return request({
    url: `/api/course/unpublish/${id}`,
    method: 'put'
  });
}

/**
 * 获取课程学习人员列表
 * @param {Number} id - 课程ID
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getCourseLearners(id, params) {
  return request({
    url: `/api/course/learners/${id}`,
    method: 'post',
    data: params
  });
}

/**
 * 标记课程为已完成
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function markCourseComplete(id) {
  return request({
    url: `/api/course/complete/${id}`,
    method: 'post'
  });
}

/**
 * 获取课程统计数据
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function getCourseStatistics(id) {
  return request({
    url: `/api/course/statistics/${id}`,
    method: 'get'
  });
}

/**
 * 获取课程学习人员统计列表
 * @param {Number} id - 课程ID
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getCourseLearnerStatistics(id, params) {
  return request({
    url: `/api/course/statistics/learners/${id}`,
    method: 'post',
    data: params
  });
}

/**
 * 获取关联内容
 * @param {Object}  - 业务Id bizId 业务类型 bizType
 * @returns {Promise} - 返回Promise对象
 */
export function getRelatedContent(params) {
  return request({
    url: `/api/relate/query`,
    method: 'get',
    params
  });
}

/**
 * 批量更新课程创建人
 * @param {Object} data
 * @param {Array<number>} data.courseIds - 课程ID列表
 * @param {number} data.newCreatorId - 新的创建人ID
 * @returns {Promise<ApiResponse>}
 */
export function batchUpdateCreator(data) {
  return request({
    url: '/api/course/batchUpdateCreator',
    method: 'post',
    data
  });
}
