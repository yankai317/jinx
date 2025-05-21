import request from '@/utils/request';

/**
 * 查询范围配置
 * @param {Object} params - 查询参数
 * @param {String} params.businessType - 业务类型（如course, train, learnMap）
 * @param {Number} params.businessId - 业务ID
 * @param {String} params.functionType - 功能类型（如visibility, collaborate, assign）
 * @returns {Promise} - 返回Promise对象
 */
export function queryRange(params) {
  return request({
    url: '/api/common/range/query',
    method: 'get',
    params
  });
}

/**
 * 更新范围配置
 * @param {Object} data - 更新参数
 * @param {String} data.modelType - 功能模块类型（如visibility, collaborate, assign）
 * @param {String} data.type - 业务模块类型（如course, train, learnMap）
 * @param {Number} data.typeId - 业务模块ID
 * @param {Object} data.targetTypeAndIds - 目标范围类型和IDs的映射，key为目标类型，value为目标IDs列表
 * @returns {Promise} - 返回Promise对象
 */
export function updateRange(data) {
  return request({
    url: '/api/common/range/update',
    method: 'post',
    data
  });
}

/**
 * 删除范围配置
 * @param {Object} data - 删除参数
 * @param {String} data.modelType - 功能模块类型（如visibility, collaborate, assign）
 * @param {String} data.type - 业务模块类型（如course, train, learnMap）
 * @param {Number} data.typeId - 业务模块ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteRange(data) {
  return request({
    url: '/api/common/range/delete',
    method: 'post',
    data
  });
}

/**
 * 指派对象给用户
 * @param {Object} data - 指派参数
 * @param {String} data.bizType - 业务类型（如course, train, learnMap）
 * @param {Number} data.bizId - 业务ID
 * @param {Array} data.userIds - 用户ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function assign(data) {
  return request({
    url: '/api/common/assign',
    method: 'post',
    data
  });
}

/**
 * 查询组织架构树
 * @param {Object} data - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function queryOrgTree(data = {}) {
  return request({
    url: '/api/org/queryOrgTree',
    method: 'post',
    data
  });
}

/**
 * 查询组织列表
 * @param {Object} data - 查询参数
 * @param {Array} data.ids - 部门ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function queryOrgs(data) {
  return request({
    url: '/api/org/queryOrgs',
    method: 'post',
    data
  });
}

/**
 * 查询角色列表
 * @param {Object} data - 查询参数
 * @param {Array} data.ids - 角色ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function queryRoles(data = {}) {
  return request({
    url: '/api/role/queryRoles',
    method: 'post',
    data
  });
}

/**
 * 查询角色树
 * @param {Object} data - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function queryRoleTree(data = {}) {
  return request({
    url: '/api/role/queryRoleTree',
    method: 'post',
    data
  });
}

/**
 * 查询用户列表
 * @param {Object} data - 查询参数
 * @param {String} data.keyword - 关键字（用户名/工号）
 * @param {Array} data.userIds - 用户ID列表
 * @param {Array} data.employeeNos - 工号列表
 * @returns {Promise} - 返回Promise对象
 */
export function queryUsers(data) {
  return request({
    url: '/api/user/query',
    method: 'post',
    data
  });
}

/**
 * 查询部门下的用户
 * @param {Object} data - 请求参数
 * @param {Number} data.id - 部门ID (必须)
 * @returns {Promise} - 返回Promise对象
 */
export function queryOrgUsers(data) {
  // 确保 id 有值且为数字
  if (!data || !data.id || isNaN(parseInt(data.id, 10))) {
    console.error('调用queryOrgUsers时部门ID不能为空');
    return Promise.reject(new Error('部门ID不能为空'));
  }
  console.log(data);
  return request({
    url: '/api/org/queryOrgUsers',
    method: 'post',
    data
  });
}

/**
 * 请求入参 {"bizId":111, "bizType":"", "categoryIds":"1,2,3"}
 * @param {Object} data - 请求参数
 * @param {Number} data.bizId - 业务ID
 * @param {String} data.bizType - 业务类型
 * @param {String} data.categoryIds - 分类ID列表
 */
export function changeCategory(data) {
  return request({
    url: '/api/common/category/change',
    method: 'post',
    data
  });
}
