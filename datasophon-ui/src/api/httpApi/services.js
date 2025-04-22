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
  getServiceList: path + '/api/frame/service/list', // 选择服务的列表
  deleteService: path + '/api/frame/service/delete', // 删除框架服务
  getServiceConfigOption: path + '/service/install/getServiceConfigOption', // 查询服务配置
  getServiceRoleList: path + '/api/frame/service/role/getServiceRoleList', // 查询服务对应的服务角色 
  getAllHost: path + '/api/cluster/host/all', // 查询集群所有主机  
  saveServiceRoleHostMapping: path + '/service/install/saveServiceRoleHostMapping', // 保存服务角色与主机对应关系 
  getNonMasterRoleList: path + '/api/frame/service/role/getNonMasterRoleList', // 查询服务对应的非Master角色 
  saveServiceConfig: path + '/service/install/saveServiceConfig', // 保存服务配置
  startExecuteCommand: path + '/api/cluster/service/command/startExecuteCommand', // 启动执行指令  
  generateCommand: path + '/api/cluster/service/command/generateCommand', // 生成服务操作指令  
  getServiceCommandlist: path + '/api/cluster/service/command/getServiceCommandlist', // 查询服务安装指令列表1  
  getServiceHostList: path + '/api/cluster/service/command/host/list', // 查询服务安装对应主机列表  
  getServiceRoleOrderList: path + '/api/cluster/service/command/host/command/list', // 查询主机上服务角色指令列表3
  getLog: path + '/cluster/service/role/instance/getLog', // 服务实例-查看日志
  getHostCommandLog: path + '/api/cluster/service/command/host/command/getHostCommandLog', // 查询主机上服务角色指令3日志
  getQueueList: path + '/cluster/yarn/queue/list', // 队列列表
  getCapacityList: path + '/cluster/queue/capacity/list', // 容量队列列表
  saveQueue: path + '/cluster/yarn/queue/save', // 队列保存
  deleteQueue: path + '/cluster/yarn/queue/delete', // 队列删除
  updateQueue: path + '/cluster/yarn/queue/update', // 更新队列
  refreshQueues: path + '/cluster/yarn/queue/refreshQueues', // 刷新队列到Yarn
  refreshQueuesYARN : path + '/cluster/queue/capacity/refreshToYarn',
  getConnectionInfo: path + '/cluster/service/instance/getConnectionInfo', // 获取服务连接信息
  getServiceConfigFiles: path + '/api/service/config/getConfigFiles', // 获取服务配置文件列表
  downloadServiceConfigFile: path + '/api/service/config/downloadFile', // 下载单个配置文件
  downloadAllServiceConfigFiles: path + '/api/service/config/downloadAllFiles', // 打包下载所有配置文件（支持ZIP、TAR、TAR.GZ、TAR.XZ、7Z、GZIP、BZIP2格式，ZIP密码保护需额外安装zip4j库）
  getCompressProgress: path + '/api/service/config/getCompressProgress', // 获取压缩打包进度
  previewServiceConfigFile: path + '/api/service/config/previewFile', // 预览配置文件内容
  getSupportedCompressFormats: path + '/api/service/config/getSupportedCompressFormats', // 获取系统支持的压缩格式列表

  // K8s相关接口
  getK8sConfigMaps: path + '/api/k8s/configmaps', // 获取ConfigMap列表
  getK8sConfigMapDetail: path + '/api/k8s/configmap/detail', // 获取ConfigMap详情
  updateK8sConfigMap: path + '/api/k8s/configmap/update', // 更新ConfigMap
  getK8sServices: path + '/api/k8s/services', // 获取Service列表
  getK8sServiceDetail: path + '/api/k8s/service/detail', // 获取Service详情
  updateK8sService: path + '/api/k8s/service/update', // 更新Service
  getK8sPvcs: path + '/api/k8s/pvcs', // 获取PVC列表
  getK8sPvcDetail: path + '/api/k8s/pvc/detail', // 获取PVC详情
  updateK8sPvc: path + '/api/k8s/pvc/update', // 更新PVC
  getK8sDeployments: path + '/api/k8s/deployments', // 获取Deployment列表
  getK8sDeploymentDetail: path + '/api/k8s/deployment/detail', // 获取Deployment详情
  updateK8sDeployment: path + '/api/k8s/deployment/update', // 更新Deployment
  getK8sStatefulSets: path + '/api/k8s/statefulsets', // 获取StatefulSet列表
  getK8sStatefulSetDetail: path + '/api/k8s/statefulset/detail', // 获取StatefulSet详情
  updateK8sStatefulSet: path + '/api/k8s/statefulset/update', // 更新StatefulSet
}