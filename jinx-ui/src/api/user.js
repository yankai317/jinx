import request from '@/utils/request';

/**
 * 获取用户信息
 * @returns {Promise} - 返回Promise对象
 */
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  });
}

/**
 * 获取用户列表
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getUserList(params) {
  return request({
    url: '/api/user/list',
    method: 'post',
    data: params
  });
}
