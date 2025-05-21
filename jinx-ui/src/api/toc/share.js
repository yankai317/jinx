import request from '@/utils/request';

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
    params: { type, id }
  });
}
