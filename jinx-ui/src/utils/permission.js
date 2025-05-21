import {getUserPermissions} from '@/api/permission'
/**
 * 获取用户权限存储本地
 * @returns Promise
 */
export const fetchAndStorePermissions = () => {
  return new Promise(async (resolve)=>{
    try {
      /** 没有获取过权限，获取 */
      const res = await getUserPermissions();
      if (res.code === 200 && res.data) {
        const permissions = res.data.permissionCodes || [];
        sessionStorage.setItem('permissions', JSON.stringify(permissions));
        resolve(permissions)
      }
    } catch (error) {
      console.error('获取用户权限失败:', error);
      resolve([])
    }
  })
};

/**
 * 清除权限
 */
export const clearPermissions = ()=>{
  sessionStorage.removeItem('permissions');
}


/**
 * 检查是否有权限
 * @param {*} permissionCode 需要检查的权限code
 * @returns
 */
export const hasPermission = (permissionCode) => {
  try {
    /** 判断权限 */
    const localPermissions = JSON.parse(sessionStorage.getItem('permissions') || "[]");

    return localPermissions?.includes(permissionCode);
  } catch (error) {
    console.error('判断用户权限失败:', error);
    return false
  }
};

/**
 * 检查是否有权限集合
 * @param {*} permissionCode 需要检查的权限code
 * @param {boolean} allMatch 是否需要全部匹配
 * @returns
 */
export const matchPermission = (permissionCodes, allMatch = false) => {
  try {
    /** 判断权限 */
    const localPermissions = JSON.parse(sessionStorage.getItem('permissions') || "[]");

    if (allMatch) {
      return permissionCodes.every(permission => localPermissions.includes(permission));
    } else {
      return permissionCodes.some(permission => localPermissions.includes(permission));
    }
  } catch (error) {
    console.error('判断用户权限失败:', error);
    return false
  }
};
