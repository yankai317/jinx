import request from '@/utils/request'

/**
 * 获取用户证书列表
 * @param {Object} params - 请求参数
 * @param {number} [params.status=0] - 证书状态：0-全部，1-有效，2-已过期，3-已撤销，默认0
 * @param {number} [params.pageNum=1] - 页码，默认1
 * @param {number} [params.pageSize=10] - 每页条数，默认10
 * @returns {Promise} - 返回Promise对象
 */
export function getUserCertificates(params) {
  return request({
    url: '/api/user/certificates',
    method: 'get',
    params
  })
}

/**
 * 获取证书详情
 * @param {number} certificateId - 用户证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function getCertificateDetail(certificateId) {
  return request({
    url: `/api/user/certificate/${certificateId}`,
    method: 'get'
  })
}

/**
 * 下载证书
 * @param {number} certificateId - 用户证书ID
 * @returns {Promise} - 返回Promise对象
 */
export function downloadCertificate(certificateId) {
  return request({
    url: `/api/user/certificate/${certificateId}/download`,
    method: 'get'
  })
}
