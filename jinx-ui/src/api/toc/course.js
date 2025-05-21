import request from '@/utils/request';

/**
 * 获取课程详情
 * @param {Number} id - 课程ID
 * @returns {Promise} - 返回Promise对象
 */
export function getCourseDetail(id) {
  return request({
    url: '/api/toc/course/detail',
    method: 'get',
    params: { id }
  });
}
