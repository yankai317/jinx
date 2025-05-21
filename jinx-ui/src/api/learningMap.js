import request from '@/utils/request';

/**
 * 获取学习地图列表
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningMapList(params) {
  return request({
    url: '/api/learningMap/list',
    method: 'post',
    data: params
  });
}

/**
 * 获取学习地图详情
 * @param {Number} id - 学习地图ID
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningMapDetail(id) {
  return request({
    url: `/api/learningMap/detail/${id}`,
    method: 'get'
  });
}

/**
 * 创建学习地图
 * @param {Object} data - 学习地图数据
 * @returns {Promise} - 返回Promise对象
 */
export function createLearningMap(data) {
  return request({
    url: '/api/learningMap/create',
    method: 'post',
    data
  });
}

/**
 * 更新学习地图
 * @param {Object} data - 学习地图数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateLearningMap(data) {
  return request({
    url: '/api/learningMap/update',
    method: 'put',
    data
  });
}

/**
 * 更新学习地图置顶状态
 * @param {Object} data - 学习地图数据
 * @param {Number} data.id - 学习地图ID
 * @param {Number} data.isTop - 是否置顶：0-否，1-是
 * @returns {Promise} - 返回Promise对象
 */
export function updateLearningMapTopStatus(data) {
  return request({
    url: '/api/learningMap/updateTopStatus',
    method: 'put',
    data
  });
}

/**
 * 删除学习地图
 * @param {Number} id - 学习地图ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteLearningMap(id) {
  return request({
    url: `/api/learningMap/delete/${id}`,
    method: 'delete'
  });
}

/**
 * 批量删除学习地图
 * @param {Array} ids - 学习地图ID数组
 * @returns {Promise} - 返回Promise对象
 */
export function batchDeleteLearningMap(ids) {
  return request({
    url: '/api/learningMap/batchDelete',
    method: 'post',
    data: { ids }
  });
}

/**
 * 获取学习地图统计数据
 * @param {Number} id - 学习地图ID
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningMapStatistics(id) {
  return request({
    url: `/api/learningMap/statistics/${id}`,
    method: 'get'
  });
}

/**
 * 获取学习地图学习人员列表
 * @param {Number} id - 学习地图ID
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningMapLearners(id, params) {
  return request({
    url: `/api/learningMap/learners/${id}`,
    method: 'post',
    data: params
  });
}

/**
 * 获取学习地图学员学习详情
 * @param {Number} mapId - 学习地图ID
 * @param {Number} userId - 用户ID
 * @returns {Promise} - 返回Promise对象
 */
export function getLearningMapLearnerDetail(mapId, userId) {
  return request({
    url: '/api/learningMap/learner/detail',
    method: 'get',
    params: { mapId, userId }
  });
}

/**
 * 发送学习提醒
 * @param {Number} mapId - 学习地图ID
 * @param {Number} userId - 用户ID
 * @param {String} content - 提醒内容
 * @returns {Promise} - 返回Promise对象
 */
export function sendLearningReminder(mapId, userId, content) {
  return request({
    url: '/api/learningMap/learner/remind',
    method: 'post',
    params: { mapId, userId, content }
  });
}

/**
 * 指派学习地图
 * @param {Object} data - 指派参数
 * @returns {Promise} - 返回Promise对象
 */
export function assignLearningMap(data) {
  return request({
    url: '/api/common/assign',
    method: 'post',
    data
  });
}

/**
 * 导出学习地图数据
 * @param {Number} id - 学习地图ID
 * @param {Object} params - 导出参数
 * @returns {Promise} - 返回Promise对象
 */
export function exportLearningMapData(id, params = {}) {
  return request({
    url: `/api/learningMap/export/${id}`,
    method: 'post',
    data: params,
    responseType: 'blob'
  });
}

/**
 * 一键提醒未完成学员
 * @param {Number} id - 学习地图ID
 * @param {String} content - 提醒内容
 * @returns {Promise} - 返回Promise对象
 */
export function remindAllLearners(id, content) {
  return request({
    url: `/api/learningMap/remind/all/${id}`,
    method: 'post',
    data: { content }
  });
}

/**
 * 提醒部门主管督促学习
 * @param {Number} id - 学习地图ID
 * @param {Object} data - 提醒参数
 * @param {String} data.content - 提醒内容
 * @param {Array} data.departmentIds - 部门ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function remindDepartmentManagers(id, data) {
  return request({
    url: `/api/learningMap/remind/managers/${id}`,
    method: 'post',
    data
  });
}

/**
 * 重置学习进度
 * @param {Number} mapId - 学习地图ID
 * @param {Number} userId - 用户ID
 * @returns {Promise} - 返回Promise对象
 */
export function resetLearningProgress(mapId, userId) {
  return request({
    url: '/api/learningMap/reset/progress',
    method: 'post',
    data: { mapId, userId }
  });
}

/**
 * 更新学习地图自动指派状态
 * @param {Object} data - 请求参数
 * @param {Number} data.id - 学习地图ID
 * @param {Boolean} data.enableAutoAssign - 是否启用自动指派
 * @returns {Promise} - 返回Promise对象
 */
export function updateLearningMapAutoAssign(data) {
  return request({
    url: '/api/learning/map/autoAssign',
    method: 'post',
    data
  });
}

/**
 * 批量更新学习地图创建人
 * @param {Object} data
 * @param {Array<number>} data.mapIds - 学习地图ID列表
 * @param {number} data.newCreatorId - 新的创建人ID
 * @returns {Promise<ApiResponse>}
 */
export function batchUpdateCreator(data) {
  return request({
    url: '/api/learningMap/batchUpdateCreator',
    method: 'post',
    data
  });
}
