import request from '@/utils/request';

/**
 * 获取类目列表
 * @param {Object} params - 请求参数
 * @returns {Promise} - 返回Promise对象
 */
export function getCategoryList(params) {
  return request({
    url: '/api/category/list',
    method: 'get',
    params
  });
}

/**
 * 获取类目列表（POST方式）
 * @param {Object} data - 请求数据
 * @returns {Promise} - 返回Promise对象
 */
export function getCategoryListPost(data) {
  return request({
    url: '/api/category/list',
    method: 'post',
    data
  });
}

/**
 * 创建类目
 * @param {Object} data - 类目数据
 * @returns {Promise} - 返回Promise对象
 */
export function createCategory(data) {
  return request({
    url: '/api/category/create',
    method: 'post',
    data
  });
}

/**
 * 更新类目
 * @param {Object} data - 类目数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateCategory(data) {
  return request({
    url: '/api/category/update',
    method: 'post',
    data
  });
}

/**
 * 更新类目排序
 * @param {Object} data - 类目数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateCategorySort(data) {
  return request({
    url: '/api/category/sort/update',
    method: 'post',
    data
  });
}


/**
 * 删除类目
 * @param {Number} id - 类目ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteCategory(id) {
  return request({
    url: '/api/category/delete',
    method: 'post',
    params: { id }
  });
}

/**
 * 删除类目（DELETE方式）
 * @param {Number} id - 类目ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteCategoryById(id) {
  return request({
    url: `/api/category/delete/${id}`,
    method: 'delete'
  });
}
