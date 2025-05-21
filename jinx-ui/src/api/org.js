import request from '@/utils/request';

/**
 * 查询部门列表
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getDepartmentList(params = {}) {
  return request({
    url: '/api/org/queryOrgs',
    method: 'post',
    data: params
  });
}

/**
 * 查询部门树
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getDepartmentTree(params = {}) {
  return request({
    url: '/api/org/queryOrgTree',
    method: 'post',
    data: params
  });
}

/**
 * 查询部门下的用户
 * @param {Object} params - 查询参数
 * @param {Number} params.orgId - 部门ID
 * @returns {Promise} - 返回Promise对象
 */
export function getDepartmentUsers(params) {
  return request({
    url: '/api/org/queryOrgUsers',
    method: 'post',
    data: params
  });
}

/**
 * 查询部门下的用户ID列表
 * @param {Object} params - 查询参数
 * @param {Number} params.orgId - 部门ID
 * @returns {Promise} - 返回Promise对象
 */
export function getDepartmentUserIds(params) {
  return request({
    url: '/api/org/queryOrgUserIds',
    method: 'post',
    data: params
  });
}

/**
 * 查询部门的子部门
 * @param {Object} params - 查询参数
 * @param {Number} params.orgId - 部门ID
 * @returns {Promise} - 返回Promise对象
 */
export function getDepartmentChildren(params) {
  return request({
    url: '/api/org/queryOrgChildren',
    method: 'post',
    data: params
  });
}

/**
 * 批量获取用户的部门信息
 * @param {Array} userIds - 用户ID列表
 * @returns {Promise} - 返回Promise对象
 */
export function getUserDepartments(userIds) {
  // 确保userIds是数组
  const data = Array.isArray(userIds) ? userIds : [userIds];
  
  console.log('调用getUserDepartments API, 参数:', data);
  
  return request({
    url: '/api/org/queryUserOrgs',
    method: 'post',
    data: data
  }).then(response => {
    console.log('getUserDepartments API 原始响应:', response);
    
    // 进行数据预处理，并记录详细日志
    try {
      // 判断是否有有效响应数据
      if (response && response.code === 200) {
        const responseData = response.data; // 这是已经处理过的数据
        
        if (Array.isArray(responseData)) {
          // 记录有效的用户IDs
          const returnedUserIds = responseData.map(item => item.userId || item.id).filter(Boolean);
          console.log(`API返回了${responseData.length}个用户的部门数据, 用户IDs:`, returnedUserIds);
          
          // 比较发送的userIds和返回的userIds
          const missingIds = data.filter(id => !returnedUserIds.includes(id));
          if (missingIds.length > 0) {
            console.warn(`以下用户ID没有返回部门数据:`, missingIds);
          }
          
          // 记录一下每个用户的部门信息
          responseData.forEach(user => {
            if (user.departments && user.departments.length > 0) {
              console.log(`用户 ${user.userId} 有${user.departments.length}个部门:`, 
                         user.departments.map(d => d.name));
            } else {
              console.log(`用户 ${user.userId} 没有departments数组或为空`);
            }
          });
        }
      }
    } catch (e) {
      console.error('处理部门数据时出错:', e);
    }
    
    // 返回整个响应对象，让组件处理嵌套结构
    return response;
  }).catch(error => {
    console.error('getUserDepartments API 错误:', error);
    throw error;
  });
}
