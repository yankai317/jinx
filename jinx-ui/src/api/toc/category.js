import request from '@/utils/request'

/**
 * 获取分类列表，支持树形结构
 * @param {Object} params 请求参数
 * @param {Number} [params.parentId=0] 父分类ID，默认0表示获取顶级分类
 * @param {Boolean} [params.tree=false] 是否返回树形结构，默认false
 * @returns {Promise} 返回Promise对象
 */
export function getCategories(params) {
  return request({
    url: '/api/categories',
    method: 'get',
    params
  })
}
