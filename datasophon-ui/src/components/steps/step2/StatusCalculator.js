/**
 * 状态计算工具类
 * 用于计算主机状态和检查项状态相关逻辑
 */
export default {
  /**
   * 计算主机的整体状态
   * @param {Object} host 主机对象
   * @returns {String} 状态名称
   */
  calculateHostStatus(host) {
    // 如果主机已经有状态则返回
    if (host.statusStr || host.status) return host.statusStr || host.status;

    // 没有检查项则返回空状态
    const checkItems = host.checkItems || [];
    if (checkItems.length === 0) return null;

    // 按优先级顺序检查状态：修复中 > 等待修复 > 检查中 > 等待检查 > 失败 > 跳过 > 成功
    
    // 如果有修复中的项，则状态为"修复中"
    if (checkItems.some(item => item.status === 'FIXING')) {
      return 'FIXING';
    }
    
    // 如果有等待修复的项，则状态为"等待修复" 
    if (checkItems.some(item => item.status === 'WAITING_FIX')) {
      return 'WAITING_FIX';
    }

    // 如果有检查中的项，则状态为"检查中"
    if (checkItems.some(item => item.status === 'CHECKING')) {
      return 'CHECKING';
    }

    // 如果有等待检查的项，则状态为"等待检查"
    if (checkItems.some(item => item.status === 'WAITING')) {
      return 'WAITING';
    }

    // 如果有失败的项，则状态为"未通过"
    if (checkItems.some(item => item.status === 'FAILED')) {
      return 'FAILED';
    }

    // 如果所有项都是"跳过"，则状态为"已跳过"
    if (checkItems.every(item => item.status === 'SKIPPED')) {
      return 'SKIPPED';
    }

    // 如果有的是跳过有的是成功，则状态为"部分通过"
    if (checkItems.some(item => item.status === 'SKIPPED') &&
        checkItems.some(item => item.status === 'SUCCESS')) {
      return 'MIXED';
    }

    // 默认情况：所有项都通过
    return 'SUCCESS';
  },

  /**
   * 检查状态是否匹配目标状态
   * 支持status为collecting或loading时与loading目标状态匹配
   * @param {string} status 当前状态
   * @param {string} targetStatus 目标状态
   * @returns {boolean} 是否匹配
   */
  checkStatus(status, targetStatus) {
    if (!status) return false;

    // 处理大小写兼容
    const statusLower = status.toLowerCase();
    const targetLower = targetStatus.toLowerCase();

    // 特殊处理loading状态，collecting也视为loading
    if (targetLower === 'loading') {
      return statusLower === 'loading' || statusLower === 'collecting';
    }

    return statusLower === targetLower;
  },

  /**
   * 解析SSH错误消息，提取错误代码和解决方案
   * @param {string} message 错误消息
   * @returns {object} 渲染节点
   */
  parseSSHErrorMessage(message, h) {
    if (!message) return null;
    
    // 检查是否包含错误代码，格式如 [SSH_AUTH_ERROR]
    const codeMatch = message.match(/\[(SSH_[A-Z_]+)\]/);
    const errorCode = codeMatch ? codeMatch[1] : null;
    
    // 检查是否包含解决方案，格式如 - 请检查SSH用户名和密码是否正确
    const solutionMatch = message.match(/- (.*?)(\(|$)/);
    const solution = solutionMatch ? solutionMatch[1].trim() : null;
    
    // 如果没有解析到结构化信息，返回null
    if (!errorCode && !solution) return null;
    
    // 创建结构化展示组件
    return h('div', { class: 'ssh-error-parsed' }, [
      errorCode ? h('div', { class: 'ssh-error-code' }, [errorCode]) : null,
      solution ? h('div', { class: 'ssh-error-solution' }, [solution]) : null
    ]);
  },

  /**
   * 获取主机检查状态文本
   * @param {string} status 状态码
   * @returns {string} 状态文本
   */
  getStatusText(status) {
    if (!status) return '未知';

    const statusMap = {
      'SUCCESS': '成功',
      'FAILED': '失败',
      'CHECKING': '检查中',
      'FIXING': '修复中', 
      'WAITING': '等待检查',
      'SKIPPED': '已跳过',
      'TERMINATING': '终止中',
      'MIXED': '部分通过'
    };

    return statusMap[status] || '未知';
  },

  /**
   * 获取状态样式类
   * @param {string} status 状态码
   * @returns {string} 样式类名
   */
  getStatusStyle(status) {
    if (!status) return '';

    const styleMap = {
      'SUCCESS': 'success-status',
      'FAILED': 'failed-status',
      'CHECKING': 'checking-status',
      'FIXING': 'fixing-status',
      'WAITING': 'waiting-status',
      'SKIPPED': 'skipped-status',
      'TERMINATING': 'terminating-status',
      'MIXED': 'mixed-status'
    };

    return styleMap[status] || '';
  },

  /**
   * 更新检查状态
   * 判断是否有主机正在进行检查，更新按钮状态
   * @param {Array} hostList 主机列表
   * @returns {boolean} 是否有检查中的主机
   */
  hasCheckingHost(hostList) {
    if (!hostList || hostList.length === 0) {
      return false;
    }

    // 判断是否有主机正在检查
    return hostList.some(host => {
      // 检查主机状态
      if (host.status === 'CHECKING' || host.statusStr === 'CHECKING') {
        return true;
      }

      // 检查所有检查项状态
      const checkItems = host.checkItems || [];
      return checkItems.some(item => item.status === 'CHECKING');
    });
  }
}; 