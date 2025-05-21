import request from '@/utils/request';

/**
 * 获取培训详情
 * @param {number} id - 培训ID
 * @returns {Promise} - 返回培训详情
 */
export function getTrainDetail(id) {
  return request({
    url: '/api/train/detail',
    method: 'get',
    params: { id }
  });
}
