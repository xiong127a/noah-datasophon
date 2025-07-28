/*
/*
 *
 *  Licensed to the Apache Software Foundation (ASF) under one or more
 *  contributor license agreements.  See the NOTICE file distributed with
 *  this work for additional information regarding copyright ownership.
 *  The ASF licenses this file to You under the Apache License, Version 2.0
 *  (the "License"); you may not use this file except in compliance with
 *  the License.  You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 *
 */

import paths from '@/api/baseUrl'// 后台服务地址

let path = paths.path() + '/ddh'
export default {
  reStartDispatcherHostAgent: path + '/api/host/install/reStartDispatcherHostAgent', // 主机agent分发重试
  dispatcherHostAgentList: path + '/api/host/install/dispatcherHostAgentList', // 主机agent分发进度列表
  rehostCheck: path + '/api/host/check/rehostCheck', // 重试主机环境校验
  analysisHostList: path + '/api/host/install/analysisHostList', // 解析主机列表
  saveKubernetesHost: path + '/api/cluster/host/saveKubernetesHost', // 保存Kubernetes主机
  saveKubernetesHostDirect: path + '/api/cluster/host/saveKubernetesHostDirect', // 直接保存Kubernetes主机（完整信息）
  getK8sHostsWithHardwareInfo: path + '/api/cluster/host/getK8sHostsWithHardwareInfo', // 获取K8S完整硬件信息
  hostCheckCompleted: path + '/api/host/install/hostCheckCompleted', // 查询主机环境校验是否完成
  cleanupHostCheckResources: path + '/api/host/install/cleanupHostCheckResources', // 清理主机检查资源
  dispatcherHostAgentCompleted:
    path + '/api/host/install/dispatcherHostAgentCompleted', // 查询主机agent分发是否完成
  getRack: path + '/api/cluster/host/getRack', // 查询机架
  updateRack: path + '/api/cluster/host/update', // 分配机架
  deleteRack: path + '/api/cluster/host/delete', // 分配机架
  getRoleListByHostname: path + '/api/cluster/host/getRoleListByHostname', // 根据主机查询角色列表
  generateHostAgentCommand: path + '/api/host/install/generateHostAgentCommand', // 主机 Worker 管理
  generateHostServiceCommand: path + '/api/host/install/generateHostServiceCommand', // 主机 Worker Service 管理
  fixCheckItem: path + '/api/host/check/fixCheckItem', // 修复单个检查项
  fixAllCheckItems: path + '/api/host/check/fixAllCheckItems', // 修复所有检查项
  fixSelectedCheckItems: path + '/api/host/check/fixSelectedCheckItems', // 修复选中的检查项
  batchCheckHosts: path + '/api/host/check/batchCheckHosts', // 批量检查主机
  getCheckItemLog: path + '/api/host/check/getCheckItemLog', // 获取检查项日志
  getHostCheckItems: path + '/api/host/check/getHostCheckItems', // 获取主机检查项列表
  stopCheckItem: path + '/api/host/check/stopCheckItem', // 终止单个检查项检查
  skipCheckItem: path + '/api/host/check/skipCheckItem', // 跳过检查项
  stopHostCheck: path + '/api/host/check/stopHostCheck', // 终止主机所有检查项
  startHostCheck: path + '/api/host/check/startHostCheck', // 开始主机检查
  retryCheckItems: path + '/api/host/check/retryCheckItems', // 重试检查项
  getCheckItemConfirmInfo: path + '/api/host/check/getCheckItemConfirmInfo', // 获取检查项确认信息
  queueManager: path + '/api/host/check/queueManager', // 控制队列系统
  getLogLevels: path + '/api/host/check/log-levels', // 获取可用的日志级别
  getLogTypes: path + '/api/host/check/log-types', // 获取可用的日志类型
  queueSystemDetails: path + '/api/host/check/queueSystemDetails', // 获取队列系统详情
  updateTaskInterval: path + '/api/host/check/updateTaskInterval', // 修改定时任务执行间隔
  updateQueueHealthMonitorInterval: path + '/api/host/check/updateQueueHealthMonitorInterval', // 更新队列健康监控间隔
  updateTaskTimeoutMonitorInterval: path + '/api/host/check/updateTaskTimeoutMonitorInterval', // 更新任务超时监控间隔
  updateHostname: path + '/api/host/check/updateHostname', // 更新主机名
  generateHostsFilePreview: path + '/api/host/check/generateHostsFilePreview', // 生成hosts文件预览
  syncHostsFile: path + '/api/host/check/syncHostsFile', // 同步hosts文件到所有主机
  updateHostsFile: path + '/api/host/check/updateHostsFile', // 更新hosts文件
  batchSetHostname: path + '/api/host/check/batchSetHostname', // 批量设置主机名
  getTaskProgress: path + '/api/host/check/getTaskProgress', // 获取任务进度
  fixAllFailedItems: path + '/api/host/check/fixAllFailedItems', // 一键修复所有失败项
  skipAllFailedItems: path + '/api/host/check/skipAllFailedItems', // 一键跳过所有失败项
  getWorkerLog: path + '/api/host/install/getWorkerLog', // 获取主机最近日志
  listServiceTab: path + '/api/service/install/listServiceTab', // 获取安装服务选项卡列表
}
