import request from '@/utils/request';

/**
 * 创建培训
 * @param {Object} data - 培训数据
 * @returns {Promise} - 返回Promise对象
 */
export function createTrain(data) {
  return request({
    url: '/api/train/create',
    method: 'post',
    data
  });
}

/**
 * 更新培训
 * @param {Object} data - 培训数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateTrain(data) {
  return request({
    url: '/api/train/update',
    method: 'put',
    data
  });
}

/**
 * 获取培训详情
 * @param {Number} id - 培训ID
 * @returns {Promise} - 返回Promise对象
 */
export function getTrainDetail(id) {
  return request({
    url: `/api/train/detail/${id}`,
    method: 'get'
  });
}

/**
 * 指派培训
 * @param {Object} data - 指派培训请求参数
 * @returns {Promise} - 返回Promise对象
 */
export function assignTrain(data) {
  return request({
    url: '/api/common/assign',
    method: 'post',
    data
  });
}

/**
 * 获取培训列表
 * @param {Object} data - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getTrainList(data) {
  return request({
    url: '/api/train/list',
    method: 'post',
    data
  });
}

/**
 * 获取培训统计数据
 * @param {Number} id - 培训ID
 * @returns {Promise} - 返回Promise对象
 */
export function getTrainStatistics(id) {
  return request({
    url: `/api/train/statistics/${id}`,
    method: 'get'
  });
}

/**
 * 获取培训学习人员列表
 * @param {Number} id - 培训ID
 * @param {Object} data - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getTrainLearners(id, data) {
  return request({
    url: `/api/train/learners/${id}`,
    method: 'post',
    data
  });
}

/**
 * 发布培训
 * @param {Number} id - 培训ID
 * @returns {Promise} - 返回Promise对象
 */
export function publishTrain(id) {
  return request({
    url: `/api/train/publish/${id}`,
    method: 'put'
  });
}

/**
 * 取消发布培训
 * @param {Number} id - 培训ID
 * @returns {Promise} - 返回Promise对象
 */
export function unpublishTrain(id) {
  return request({
    url: `/api/train/unpublish/${id}`,
    method: 'put'
  });
}

/**
 * 删除培训
 * @param {Number} id - 培训ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteTrain(id) {
  return request({
    url: `/api/train/delete/${id}`,
    method: 'delete'
  });
}

/**
 * 批量删除培训
 * @param {Array} ids - 培训ID数组
 * @returns {Promise} - 返回Promise对象
 */
export function batchDeleteTrain(ids) {
  return request({
    url: '/api/train/batchDelete',
    method: 'post',
    data: { ids }
  });
}

/**
 * 获取培训学员学习详情
 * @param {Number} trainId - 培训ID
 * @param {Number} userId - 用户ID
 * @returns {Promise} - 返回Promise对象
 */
export function getTrainLearnerDetail(trainId, userId) {
  return request({
    url: '/api/train/learner/detail',
    method: 'get',
    params: { trainId, userId }
  });
}

/**
 * 发送学习提醒
 * @param {Number} trainId - 培训ID
 * @param {Number} userId - 用户ID
 * @returns {Promise} - 返回Promise对象
 */
export function sendLearningReminder(trainId, userId) {
  return request({
    url: '/api/train/learner/remind',
    method: 'post',
    params: { trainId, userId }
  });
}

/**
 * 重置学习进度
 * @param {Number} trainId - 培训ID
 * @param {Number} userId - 用户ID
 * @returns {Promise} - 返回Promise对象
 */
export function resetLearningProgress(trainId, userId) {
  return request({
    url: '/api/train/learner/reset',
    method: 'post',
    params: { trainId, userId }
  });
}

/**
 * 获取指派详情
 * @param {Object} params - 请求参数
 * @returns {Promise} - 返回指派详情
 */
export function getAssignmentDetail(params) {
  return request({
    url: '/api/assignment/detail',
    method: 'post',
    data: params
  });
}

/**
 * 批量更新培训创建人
 * @param {Object} data
 * @param {Array<number>} data.courseIds - 培训ID列表
 * @param {number} data.newCreatorId - 新的创建人ID
 * @returns {Promise<ApiResponse>}
 */
export function batchUpdateCreator(data) {
  return request({
    url: '/api/train/batchUpdateCreator',
    method: 'post',
    data
  });
}
