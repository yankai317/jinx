
import request from '@/utils/request'

/**
 * 获取视频播放信息
 * @param {Object} params - 请求参数
 * @param {Number} params.courseId - 课程ID
 * @param {Number} params.trainId - 培训ID（可选）
 * @param {Number} params.mapId - 学习地图ID（可选）
 * @param {Number} params.stageId - 阶段ID（可选）
 * @returns {Promise} - 返回Promise对象
 */
export function getVideoPlayInfo(params) {
  return request({
    url: '/api/video/play',
    method: 'get',
    params
  })
}

/**
 * 记录学习进度
 * @param {Object} data - 请求参数
 * @param {String} data.type - 内容类型：course-课程，train-培训内容，map-学习地图任务
 * @param {Number} data.contentId - 内容ID
 * @param {Number} data.progress - 学习进度(百分比)
 * @param {Number} data.duration - 本次学习时长(秒)
 * @param {Number} data.parentId - 父内容ID（培训ID或学习地图阶段ID，可选）
 * @returns {Promise} - 返回Promise对象
 */
export function recordLearningProgress(data) {
  return request({
    url: '/api/learning/record',
    method: 'post',
    data
  })
}

/**
 * 获取内容分享信息
 * @param {String} type - 内容类型：course-课程，train-培训，map-学习地图
 * @param {Number} id - 内容ID
 * @returns {Promise} - 返回Promise对象
 */
export function getShareContent(type, id) {
  return request({
    url: '/api/share',
    method: 'get',
    params: { 
      type,
      id 
    }
  })
}

/**
 * 获取视频分享信息
 * @param {Number} courseId - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function getVideoShareInfo(courseId) {
  return getShareContent('course', courseId);
}
