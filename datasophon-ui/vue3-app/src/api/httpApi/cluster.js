import api from '../index';

// 集群管理API
export default {
  /**
   * 获取集群列表
   * @param {Object} params - 查询参数
   * @returns {Promise}
   */
  getClusterList(params) {
    return api.get('/colony/queryColony', params);
  },

  /**
   * 创建新集群
   * @param {Object} data - 集群数据
   * @returns {Promise}
   */
  createCluster(data) {
    return api.post('/colony/addColony', data);
  },

  /**
   * 获取集群详情
   * @param {string|number} id - 集群ID
   * @returns {Promise}
   */
  getClusterDetail(id) {
    return api.get(`/colony/getColonyInfoById`, { id });
  },

  /**
   * 更新集群信息
   * @param {Object} data - 集群数据
   * @returns {Promise}
   */
  updateCluster(data) {
    return api.put('/colony/updateColony', data);
  },

  /**
   * 删除集群
   * @param {string|number} id - 集群ID
   * @returns {Promise}
   */
  deleteCluster(id) {
    return api.delete('/colony/delColony', { id });
  }
}; 