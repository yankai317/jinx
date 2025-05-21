import request from '@/utils/request'

/**
 * 获取学习地图详情
 * @param {number} id - 学习地图ID
 * @returns {Promise} - 返回学习地图详情数据
 */
export function getMapDetail(id) {
  return request({
    url: '/api/toc/map/detail',
    method: 'get',
    params: { id }
  })
}

/**
 * 获取学习地图阶段详情
 * @param {number} mapId - 学习地图ID
 * @param {number} stageId - 阶段ID
 * @returns {Promise} - 返回学习地图阶段详情数据
 */
export function getMapStageDetail(mapId, stageId) {
  return request({
    url: '/api/map/stage/detail',
    method: 'get',
    params: { mapId, stageId }
  })
}
