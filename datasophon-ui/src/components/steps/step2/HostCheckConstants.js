/**
 * 主机检查相关常量定义
 */

// 主机状态常量
export const HOST_STATUS = {
  WAITING: 'WAITING',     // 等待检查
  CHECKING: 'CHECKING',   // 检查中
  SUCCESS: 'SUCCESS',     // 检查成功
  FAILED: 'FAILED',       // 检查失败
  TERMINATING: 'TERMINATING', // 终止中
  SKIPPED: 'SKIPPED',     // 已跳过
  FIXING: 'FIXING',       // 修复中
  WAITING_FIX: 'WAITING_FIX' // 等待修复
};

// 主机状态文本映射
export const HOST_STATUS_TEXT = {
  [HOST_STATUS.WAITING]: '待检查',
  [HOST_STATUS.CHECKING]: '检查中',
  [HOST_STATUS.SUCCESS]: '成功',
  [HOST_STATUS.FAILED]: '失败',
  [HOST_STATUS.TERMINATING]: '终止中',
  [HOST_STATUS.SKIPPED]: '已跳过',
  [HOST_STATUS.FIXING]: '修复中',
  [HOST_STATUS.WAITING_FIX]: '等待修复'
};

// 主机状态样式类映射
export const HOST_STATUS_CLASS = {
  [HOST_STATUS.WAITING]: 'waiting-status',
  [HOST_STATUS.CHECKING]: 'checking-status',
  [HOST_STATUS.SUCCESS]: 'success-status',
  [HOST_STATUS.FAILED]: 'failed-status',
  [HOST_STATUS.TERMINATING]: 'terminating-status',
  [HOST_STATUS.SKIPPED]: 'skipped-status',
  [HOST_STATUS.FIXING]: 'fixing-status',
  [HOST_STATUS.WAITING_FIX]: 'waiting-fix-status'
};

// 主机状态图标映射
export const HOST_STATUS_ICON = {
  [HOST_STATUS.WAITING]: 'clock-circle',
  [HOST_STATUS.CHECKING]: 'loading',
  [HOST_STATUS.SUCCESS]: 'check-circle',
  [HOST_STATUS.FAILED]: 'close-circle',
  [HOST_STATUS.TERMINATING]: 'stop',
  [HOST_STATUS.SKIPPED]: 'warning',
  [HOST_STATUS.FIXING]: 'tool',
  [HOST_STATUS.WAITING_FIX]: 'hourglass'
};

// 主机状态颜色映射
export const HOST_STATUS_COLOR = {
  [HOST_STATUS.WAITING]: '#FF9500',
  [HOST_STATUS.CHECKING]: '#007AFF',
  [HOST_STATUS.SUCCESS]: '#34C759',
  [HOST_STATUS.FAILED]: '#FF3B30',
  [HOST_STATUS.TERMINATING]: '#FF9500',
  [HOST_STATUS.SKIPPED]: '#8E8E93',
  [HOST_STATUS.FIXING]: '#5856D6',
  [HOST_STATUS.WAITING_FIX]: '#FF9F0A'
};

// 主机状态背景色映射 (rgba格式)
export const HOST_STATUS_BG_COLOR = {
  [HOST_STATUS.WAITING]: 'rgba(255, 149, 0, 0.1)',
  [HOST_STATUS.CHECKING]: 'rgba(0, 122, 255, 0.1)',
  [HOST_STATUS.SUCCESS]: 'rgba(52, 199, 89, 0.1)',
  [HOST_STATUS.FAILED]: 'rgba(255, 59, 48, 0.1)',
  [HOST_STATUS.TERMINATING]: 'rgba(255, 149, 0, 0.1)',
  [HOST_STATUS.SKIPPED]: 'rgba(142, 142, 147, 0.1)',
  [HOST_STATUS.FIXING]: 'rgba(88, 86, 214, 0.1)',
  [HOST_STATUS.WAITING_FIX]: 'rgba(255, 159, 10, 0.1)'
};

// 可操作状态定义
export const ACTIONABLE_STATUSES = {
  // 可重试的状态
  RETRYABLE: [HOST_STATUS.FAILED, HOST_STATUS.SUCCESS, HOST_STATUS.SKIPPED],
  // 可终止的状态
  STOPPABLE: [HOST_STATUS.CHECKING],
  // 可修复的状态
  FIXABLE: [HOST_STATUS.FAILED],
  // 可跳过的状态
  SKIPPABLE: [HOST_STATUS.FAILED],
  // 可查看日志的状态 (除了WAITING以外的所有状态)
  LOGGABLE: [
    HOST_STATUS.CHECKING, 
    HOST_STATUS.SUCCESS, 
    HOST_STATUS.FAILED, 
    HOST_STATUS.TERMINATING, 
    HOST_STATUS.SKIPPED, 
    HOST_STATUS.FIXING
  ]
};

// 全局默认设置
export const DEFAULT_SETTINGS = {
  // 刷新间隔时间(毫秒)
  REFRESH_INTERVAL: 5000,
  // 自动刷新间隔(毫秒) - 快速刷新
  FAST_REFRESH_INTERVAL: 1000,
  // 表格每页显示的记录数
  PAGE_SIZE: 10,
  // 等待时间(毫秒)
  WAIT_TIMEOUT: 1000,
  // 显示"更多"的阈值
  MORE_THRESHOLD: 5,
  // 日志行数限制
  LOG_LINE_LIMIT: 1000
}; 