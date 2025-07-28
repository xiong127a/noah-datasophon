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
  getServiceListByCluster: path + '/api/cluster/service/instance/list', // 选择服务的列表
  instanceList: path + '/api/cluster/service/role/instance/list', // 选择服务的列表
  getConfigInfo: path + '/api/cluster/service/instance/config/info', // 查询服务配置
  getConfigVersion: path + '/api/cluster/service/instance/config/getConfigVersion', // 查询服务版本
  configVersionCompare:path + '/api/cluster/service/instance/configVersionCompare', // 服务版本比对 ,
  getHostListByPage: path + '/api/cluster/host/list', // 分页查询集群主机
  generateServiceCommand: path + '/api/cluster/service/command/generateServiceCommand', // 生成服务操作指令
  generateServiceRoleCommand: path + '/api/cluster/service/command/generateServiceRoleCommand', // 生成服务角色操作指令
  getAlertList: path + '/api/cluster/alert/history/getAlertList', // 查询服务告警列表
  deleteExample: path + '/api/cluster/service/role/instance/delete', // 删除角色实例
  restartObsoleteService:path + '/api/cluster/service/role/instance/restartObsoleteService', // 重启过时服务 ,
  decommissionNode:path + '/api/cluster/service/role/instance/decommissionNode', // 退役该节点 ,
  getWebUis: path + '/api/cluster/webuis/getWebUis', // 查询webuis
  getServiceRoleType:path + '/api/cluster/service/instance/getServiceRoleType', // 角色组 查询角色类型 ,
  getRoleGroupList:path + '/api/cluster/service/instance/role/group/list', // 角色组 查询角色组列表 ,
  editRoleGroupBind:path + '/api/cluster/service/instance/role/group/bind', // 角色组 分配角色组 ,
  addRoleGroupSave:path + '/api/cluster/service/instance/role/group/save', // 角色组 保存角色组 ,


  // 告警模块
  getAlarmGroupList: path + '/api/alert/group/list', // 告警组列表
  getAlarmMerticList: path + '/api/cluster/alert/quota/list', // 告警指标列表
  getAlarmCate: path + '/api/frame/service/list', // 查询服务列表 告警组类别
  getAlarmRole: path + '/api/frame/service/role/getServiceRoleByServiceName', // 查询服务列表 告警组类别
  saveGroup: path + '/api/alert/group/save', // 查询服务列表 告警组类别
  saveMetric: path + '/api/cluster/alert/quota/save', // 告警指标保存
  updateMetric: path + '/api/cluster/alert/quota/update', // 查询服务列表 告警组类别
  deleteGroup: path + '/api/alert/group/delete', // 告警组删除
  deleteMetric: path + '/api/cluster/alert/quota/delete', // 告警指标删除
  getAllAlertList: path + '/api/cluster/alert/history/getAllAlertList', // 查询所有告警
  quotaStart: path + '/api/cluster/alert/quota/start', // 启用告警指标
  quotaStop: path + '/api/cluster/alert/quota/stop', // 禁用告警指标

  //通知模块
  getNoticeGroupList: path + '/api/notice/group/list',
  saveNotice: path + '/api/notice/group/save',
  updateNotice: path + '/api/notice/group/update',
  deleteNotice: path + '/api/notice/group/delete',

  // 自动伸缩模块
  getAutoScaleTasks: path + '/api/autoScale/getAutoScaleTasks', // 获取自动伸缩任务列表
  createAutoScaleTask: path + '/api/autoScale/createAutoScaleTask', // 创建自动伸缩任务
  updateAutoScaleTask: path + '/api/autoScale/updateAutoScaleTask', // 更新自动伸缩任务
  deleteAutoScaleTask: path + '/api/autoScale/deleteAutoScaleTask', // 删除自动伸缩任务

}
