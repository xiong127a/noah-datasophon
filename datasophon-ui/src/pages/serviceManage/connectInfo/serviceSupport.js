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

console.log('支持连接信息的服务类型:', SUPPORTED_SERVICES);

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
 * 获取所有支持连接信息的服务类型列表
 * @returns {Array<string>} 支持的服务类型列表
 */
export function getSupportedServices() {
  return [...SUPPORTED_SERVICES];
} 