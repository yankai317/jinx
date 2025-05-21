import request from '@/utils/request';

/**
 * 获取角色列表
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getRoleList(params) {
  return request({
    url: '/api/role/list',
    method: 'get',
    params
  });
}

/**
 * 创建角色
 * @param {Object} data - 角色数据
 * @returns {Promise} - 返回Promise对象
 */
export function createRole(data) {
  return request({
    url: '/api/role/create',
    method: 'post',
    data
  });
}

/**
 * 更新角色
 * @param {Object} data - 角色数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateRole(data) {
  return request({
    url: '/api/role/update',
    method: 'post',
    data
  });
}

/**
 * 删除角色
 * @param {Object} data - 角色ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteRole(data) {
  return request({
    url: '/api/role/delete',
    method: 'post',
    data
  });
}

/**
 * 给角色添加用户
 * @param {Object} data - 角色ID和用户ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function addUsersToRole(data) {
  return request({
    url: '/api/role/addUsers',
    method: 'post',
    data
  });
}

/**
 * 从角色中移除用户
 * @param {Object} data - 角色ID和用户ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function removeUsersFromRole(data) {
  return request({
    url: '/api/role/removeUsers',
    method: 'post',
    data
  });
}

/**
 * 获取角色下的用户列表
 * @param {Object} params - 查询参数，包含角色ID
 * @returns {Promise} - 返回Promise对象
 */
export function getRoleUsers(params) {
  return request({
    url: '/api/role/users',
    method: 'get',
    params
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
