import request from '@/utils/request';

/**
 * 获取文档查看信息
 * @param {Object} data请求参数
 * @returns {Promise} 请求结果
 */
export function getDocumentViewInfo(data) {
  return request({
    url: '/api/toc/document/view',
    method: 'post',
    data
  });
}

/**
 * 更新文档阅读进度
 * @param {Object} data 请求参数
 * @param {Number} page 当前页码
 * @param {Number} progress 阅读进度（百分比）
 * @returns {Promise} 请求结果
 */
export function updateDocumentProgress(data, page, progress) {
  return request({
    url: `/api/toc/document/progress?page=${page}&progress=${progress}`,
    method: 'post',
    data
  });
}

/**
 * 生成文档分享信息
 * @param {Number} courseId 课程ID
 * @returns {Promise} 请求结果
 */
export function generateShareInfo(courseId) {
  return request({
    url: `/api/toc/document/share?courseId=${courseId}`,
    method: 'get'
  });
}
