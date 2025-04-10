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

/**
 * 服务支持检测工具
 * 用于检测当前服务是否支持显示连接信息
 */

// 使用webpack的require.context功能扫描services目录
const servicesContext = require.context('./services', false, /\.vue$/);

// 提取所有支持的服务类型
const SUPPORTED_SERVICES = servicesContext.keys().map(fileName => {
  // 从文件名中提取服务类型名称 (例如 './Hive.vue' -> 'HIVE')
  return fileName.replace(/^\.\/(.*?)\.vue$/, '$1').toUpperCase();
});

// 高可用模式常量定义
export const HA_MODE = {
  STANDALONE: 'standalone',
  ZOOKEEPER: 'zookeeper',  // 动态服务发现(负载均衡)
  ZOOKEEPER_HA: 'zooKeeperHA', // 主备切换
  HTTP: 'httpHA', // HTTP负载均衡
  SENTINEL: 'sentinel', // Redis哨兵模式
  CLUSTER: 'cluster'  // Redis集群模式
};

// 不同服务支持的高可用模式
export const SERVICE_HA_MODES = {
  'HIVE': [HA_MODE.STANDALONE, HA_MODE.ZOOKEEPER, HA_MODE.ZOOKEEPER_HA, HA_MODE.HTTP],
  'SPARK': [HA_MODE.STANDALONE, HA_MODE.ZOOKEEPER],
  'HDFS': [HA_MODE.STANDALONE, HA_MODE.ZOOKEEPER_HA],
  'HBASE': [HA_MODE.STANDALONE, HA_MODE.ZOOKEEPER_HA],
  'REDIS': [HA_MODE.STANDALONE, HA_MODE.SENTINEL, HA_MODE.CLUSTER],
  'REDISSENTINEL': [HA_MODE.SENTINEL]  // Redis哨兵专用组件
  // 可以继续添加其他服务支持的高可用模式
};

/**
 * 检查服务是否支持连接信息
 * @param {string} serviceName 服务名称
 * @returns {boolean} 是否支持连接信息
 */
export function checkServiceSupport(serviceName) {
  if (!serviceName) return false;
  
  // 转换为大写进行比较
  const upperServiceName = serviceName.toUpperCase();
  
  // 检查是否在支持列表中
  return SUPPORTED_SERVICES.includes(upperServiceName);
}

/**
 * 获取服务支持的高可用模式列表
 * @param {string} serviceName 服务名称
 * @returns {Array<string>} 支持的高可用模式列表
 */
export function getServiceHAModes(serviceName) {
  if (!serviceName) return [];
  
  const upperServiceName = serviceName.toUpperCase();
  return SERVICE_HA_MODES[upperServiceName] || [HA_MODE.STANDALONE];
}

/**
 * 获取高可用模式中文名称
 * @param {string} haMode 高可用模式代码
 * @returns {string} 高可用模式中文名称
 */
export function getHAModeDisplayName(haMode) {
  switch (haMode) {
    case HA_MODE.ZOOKEEPER:
      return 'ZooKeeper服务发现(负载均衡)';
    case HA_MODE.ZOOKEEPER_HA:
      return 'ZooKeeper主备切换(Active-Passive)';
    case HA_MODE.HTTP:
      return 'HTTP负载均衡';
    case HA_MODE.SENTINEL:
      return 'Redis哨兵模式';
    case HA_MODE.CLUSTER:
      return 'Redis集群模式';
    case HA_MODE.STANDALONE:
    default:
      return '单实例模式';
  }
}

/**
 * 获取所有支持连接信息的服务类型列表
 * @returns {Array<string>} 支持的服务类型列表
 */
export function getSupportedServices() {
  return [...SUPPORTED_SERVICES];
} 