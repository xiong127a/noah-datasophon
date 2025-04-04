/**
 * 任务状态管理器
 * 用于存储和管理正在进行中的任务状态
 */

// 任务类型枚举
export const TASK_TYPE = {
  HOSTNAME_SETTING: 'HOSTNAME_SETTING', // 设置主机名任务
  SYNC_HOSTS_FILE: 'SYNC_HOSTS_FILE'    // 同步hosts文件任务
};

// 任务状态信息
const taskState = {
  // 当前进行中的任务ID
  [TASK_TYPE.HOSTNAME_SETTING]: {
    taskId: null,
    clusterId: null
  },
  [TASK_TYPE.SYNC_HOSTS_FILE]: {
    taskId: null,
    clusterId: null
  }
};

/**
 * 任务状态管理器
 */
export default {
  /**
   * 记录任务ID
   * @param {String} taskType 任务类型
   * @param {String|Number} taskId 任务ID
   * @param {String|Number} clusterId 集群ID
   */
  setTaskId(taskType, taskId, clusterId) {
    if (!taskType || !taskId) return;
    
    taskState[taskType] = {
      taskId,
      clusterId
    };
    
    // 保存到本地存储以便页面刷新后仍能恢复
    try {
      localStorage.setItem(`noah_${taskType}_state`, JSON.stringify({
        taskId,
        clusterId,
        timestamp: Date.now()
      }));
    } catch (e) {
      console.error('保存任务状态失败:', e);
    }
  },
  
  /**
   * 获取任务ID
   * @param {String} taskType 任务类型
   * @param {String|Number} clusterId 集群ID，用于验证任务是否属于当前集群
   * @returns {String|Number|null} 任务ID或null
   */
  getTaskId(taskType, clusterId) {
    // 首先尝试从内存中获取
    if (taskState[taskType] && 
        taskState[taskType].taskId && 
        taskState[taskType].clusterId === clusterId) {
      return taskState[taskType].taskId;
    }
    
    // 如果内存中没有，尝试从本地存储恢复
    try {
      const savedState = localStorage.getItem(`noah_${taskType}_state`);
      if (savedState) {
        const state = JSON.parse(savedState);
        
        // 验证集群ID是否匹配
        if (state.clusterId === clusterId) {
          // 验证任务是否过期（超过24小时视为过期）
          const taskAge = Date.now() - (state.timestamp || 0);
          if (taskAge < 24 * 60 * 60 * 1000) {
            // 恢复到内存中
            taskState[taskType] = {
              taskId: state.taskId,
              clusterId: state.clusterId
            };
            return state.taskId;
          }
        }
      }
    } catch (e) {
      console.error('恢复任务状态失败:', e);
    }
    
    return null;
  },
  
  /**
   * 清除任务ID
   * @param {String} taskType 任务类型
   */
  clearTaskId(taskType) {
    if (!taskType) return;
    
    // 清除内存中的状态
    if (taskState[taskType]) {
      taskState[taskType] = {
        taskId: null,
        clusterId: null
      };
    }
    
    // 清除本地存储
    try {
      localStorage.removeItem(`noah_${taskType}_state`);
    } catch (e) {
      console.error('清除任务状态失败:', e);
    }
  }
}; 