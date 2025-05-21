import request from '@/utils/request';

/**
 * 获取证书列表
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getCertificateList(params) {
  return request({
    url: '/api/certificate/list',
    method: 'post',
    data: params
  });
}

/**
 * 获取证书详情
 * @param {Number} id - 证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function getCertificateDetail(id) {
  return request({
    url: `/api/certificate/detail/${id}`,
    method: 'get'
  });
}

/**
 * 创建证书
 * @param {Object} data - 证书数据
 * @returns {Promise} - 返回Promise对象
 */
export function createCertificate(data) {
  return request({
    url: '/api/certificate/create',
    method: 'post',
    data
  });
}

/**
 * 更新证书
 * @param {Object} data - 证书数据
 * @returns {Promise} - 返回Promise对象
 */
export function updateCertificate(data) {
  return request({
    url: '/api/certificate/update',
    method: 'put',
    data
  });
}

/**
 * 删除证书
 * @param {Number} id - 证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function deleteCertificate(id) {
  return request({
    url: `/api/certificate/delete/${id}`,
    method: 'delete'
  });
}

/**
 * 批量删除证书
 * @param {Array} ids - 证书ID数组
 * @returns {Promise} - 返回Promise对象
 */
export function batchDeleteCertificates(ids) {
  return request({
    url: '/api/certificate/batchDelete',
    method: 'post',
    data: { ids }
  });
}

/**
 * 获取证书获得者列表
 * @param {Number} id - 证书ID
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function getCertificateUsers(id, params) {
  return request({
    url: `/api/certificate/users/${id}`,
    method: 'post',
    data: params
  });
}

/**
 * 撤销用户证书
 * @param {Number} id - 证书ID
 * @param {Object} data - 撤销数据
 * @returns {Promise} - 返回Promise对象
 */
export function revokeCertificate(id, data) {
  return request({
    url: `/api/certificate/revoke/${id}`,
    method: 'post',
    data
  });
}

/**
 * 批量撤销用户证书
 * @param {Number} id - 证书ID
 * @param {Object} data - 批量撤销数据
 * @returns {Promise} - 返回Promise对象
 */
export function batchRevokeCertificate(id, data) {
  return request({
    url: `/api/certificate/batchRevoke/${id}`,
    method: 'post',
    data
  });
}

/**
 * 导出证书用户列表
 * @param {Number} id - 证书ID
 * @param {Object} params - 查询参数
 * @returns {Promise} - 返回Promise对象
 */
export function exportCertificateUsers(id, params) {
  return request({
    url: `/api/certificate/export/${id}`,
    method: 'get',
    params,
    responseType: 'blob'
  });
}

/**
 * 启用证书
 * @param {Number} id - 证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function publishCertificate(id) {
  return request({
    url: `/api/certificate/publish/${id}`,
    method: 'put'
  });
}

/**
 * 禁用证书
 * @param {Number} id - 证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function unpublishCertificate(id) {
  return request({
    url: `/api/certificate/unpublish/${id}`,
    method: 'put'
  });
}

/**
 * 获取证书关联内容
 * @param {Number} id - 证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function getCertificateRelatedContent(params) {
  return request({
    url: `/api/relate/query`,
    method: 'get',
    params
  });
}
