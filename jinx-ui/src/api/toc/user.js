import request from '@/utils/request'

/**
 * 获取当前登录用户信息
 * @returns {Promise<Object>} 用户信息响应
 */
export function getUserInfo() {
  return request({
    url: '/api/user/info',
    method: 'get'
  })
}

/**
 * 获取用户个人中心信息
 * @returns {Promise<Object>} 用户个人中心信息响应
 */
export function getUserProfile() {
  return request({
    url: '/api/user/profile',
    method: 'get'
  })
}
