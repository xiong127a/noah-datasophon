/**
 * 主机检查服务
 * 封装所有与后端API相关的调用
 */
export default {
  /**
   * 获取主机列表
   * @param {Object} vm Vue实例
   * @param {Object} params 请求参数
   * @returns {Promise} 请求Promise
   */
  getEnvironmentList(vm, params) {
    return vm.$axiosPost(global.API.analysisHostList, params);
  },

  /**
   * 启动主机检查
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @returns {Promise} 请求Promise
   */
  startHostCheck(vm, clusterId) {
    return vm.$axiosPost(global.API.startHostCheck, { clusterId });
  },

  /**
   * 终止主机检查
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @returns {Promise} 请求Promise
   */
  stopHostCheckByIp(vm, clusterId, ip) {
    return vm.$axiosPost(global.API.stopHostCheck, {
      clusterId,
      ip
    });
  },

  /**
   * 批量检查主机
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {Array} ips 主机IP列表
   * @returns {Promise} 请求Promise
   */
  batchCheckHosts(vm, clusterId, ips) {
    return vm.$axiosJsonPost(global.API.batchCheckHosts + '?clusterId=' + clusterId, ips);
  },

  /**
   * 重试主机环境检查
   * @param {Object} vm Vue实例
   * @param {String} ips 主机IP，逗号分隔
   * @param {String} clusterId 集群ID
   * @param {String} sshUser SSH用户名
   * @param {String} sshPort SSH端口
   * @returns {Promise} 请求Promise
   */
  rehostCheck(vm, ips, clusterId, sshUser, sshPort) {
    return vm.$axiosPost(global.API.rehostCheck, {
      ips,
      clusterId,
      sshUser,
      sshPort
    });
  },

  /**
   * 检查主机环境检查是否完成
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @returns {Promise} 请求Promise
   */
  hostCheckCompleted(vm, clusterId) {
    return vm.$axiosPost(global.API.hostCheckCompleted, { clusterId });
  },

  /**
   * 保存K8S主机
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {Array} params 主机参数
   * @returns {Promise} 请求Promise
   */
  saveK8sHost(vm, clusterId, params) {
    return vm.$axiosJsonPost(global.API.saveK8sHost + '?clusterId=' + clusterId, params);
  },

  /**
   * 获取主机校验项
   * @param {Object} vm Vue实例
   * @param {String} ip 主机IP
   * @param {String} clusterId 集群ID
   * @returns {Promise} 请求Promise
   */
  getHostCheckItems(vm, ip, clusterId) {
    return vm.$axiosGet(global.API.getHostCheckItems + '?ip=' + ip + '&clusterId=' + clusterId);
  },

  /**
   * 获取检查项确认信息
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {String} itemId 检查项ID
   * @returns {Promise} 请求Promise
   */
  getCheckItemConfirmInfo(vm, clusterId, ip, itemId) {
    return vm.$axiosGet(global.API.getCheckItemConfirmInfo, {
      clusterId,
      ip,
      itemId
    });
  },

  /**
   * 修复检查项
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {String} itemId 检查项ID
   * @param {Boolean} skipConfirm 是否跳过确认
   * @returns {Promise} 请求Promise
   */
  fixCheckItem(vm, clusterId, ip, itemId, skipConfirm) {
    return vm.$axiosPost(global.API.fixCheckItem, {
      clusterId,
      ip,
      itemId,
      skipConfirm
    });
  },

  /**
   * 修复所有检查项
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @returns {Promise} 请求Promise
   */
  fixAllCheckItems(vm, clusterId, ip) {
    return vm.$axiosPost(global.API.fixAllCheckItems, {
      clusterId,
      ip
    });
  },

  /**
   * 修复选中的检查项
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {String} itemIds 检查项ID，逗号分隔
   * @returns {Promise} 请求Promise
   */
  fixSelectedCheckItems(vm, clusterId, ip, itemIds) {
    return vm.$axiosPost(global.API.fixSelectedCheckItems, {
      clusterId,
      ip,
      itemIds
    });
  },

  /**
   * 重试检查项
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {Array} itemNames 检查项名称列表
   * @returns {Promise} 请求Promise
   */
  retryCheckItems(vm, clusterId, ip, itemNames) {
    return vm.$axiosPost(global.API.retryCheckItems, {
      clusterId,
      ip,
      itemNames
    });
  },

  /**
   * 跳过检查项
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {String} itemId 检查项ID
   * @returns {Promise} 请求Promise
   */
  skipCheckItem(vm, clusterId, ip, itemId) {
    return vm.$axiosPost(global.API.skipCheckItem, {
      clusterId,
      ip,
      itemId
    });
  },

  /**
   * 更新主机名
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {String} hostname 新主机名
   * @returns {Promise} 请求Promise
   */
  updateHostname(vm, clusterId, ip, hostname) {
    return vm.$axiosPost(global.API.updateHostname, {
      clusterId,
      ip,
      hostname
    });
  },

  /**
   * 生成hosts文件预览
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @returns {Promise} 请求Promise
   */
  generateHostsFilePreview(vm, clusterId) {
    return vm.$axiosGet(global.API.generateHostsFilePreview + '?clusterId=' + clusterId);
  },

  /**
   * 同步hosts文件到所有主机
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @returns {Promise} 请求Promise
   */
  syncHostsFile(vm, clusterId) {
    return vm.$axiosPost(global.API.syncHostsFile, { clusterId });
  },

  /**
   * 更新hosts文件
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} ip 主机IP
   * @param {String} hostsFileContent hosts文件内容
   * @returns {Promise} 请求Promise
   */
  updateHostsFile(vm, clusterId, ip, hostsFileContent) {
    return vm.$axiosPost(global.API.updateHostsFile, {
      clusterId,
      ip,
      hostsFileContent
    });
  },

  /**
   * 批量设置主机名
   * @param {Object} vm Vue实例
   * @param {String} clusterId 集群ID
   * @param {String} prefix 前缀
   * @param {Number} zeroCount 中间0的位数
   * @param {String} separator 分隔符
   * @param {String} suffix 后缀
   * @returns {Promise} 请求Promise
   */
  batchSetHostname(vm, clusterId, prefix, zeroCount, separator, suffix) {
    return vm.$axiosPost(global.API.batchSetHostname, {
      clusterId,
      prefix,
      zeroCount,
      separator,
      suffix
    });
  }
}; 