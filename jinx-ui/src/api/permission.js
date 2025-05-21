import request from '@/utils/request';

/**
 * 获取当前用户的所有权限
 * @returns {Promise} - 返回Promise对象
 */
export function getUserPermissions() {
  return request({
    url: '/api/permission/getUserPermissions',
    method: 'get'
  });
}

/**
 * 检查用户是否有特定权限
 * @param {String} permissionCode - 权限编码
 * @returns {Promise} - 返回Promise对象
 */
export function checkPermission(permissionCode) {
  return request({
    url: '/api/permission/checkPermission',
    method: 'get',
    params: { permissionCode }
  });
}

/**
 * 获取所有权限列表
 * @returns {Promise} - 返回Promise对象
 */
export function getAllPermissions() {
  return request({
    url: '/api/permission/getAllPermissions',
    method: 'get'
  });
}
