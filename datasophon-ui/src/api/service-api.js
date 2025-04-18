/*
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
 */

import { request } from '@/utils/request'
import API from '@/api/httpApi/services'

// Kubernetes Deployment API
export const k8sDeploymentApi = {
  // 获取Deployment详情
  getDeploymentDetails(params) {
    return request({
      url: API.getK8sDeploymentDetail,
      method: 'get',
      params
    })
  },
  
  // 获取Deployment的Pods
  getDeploymentPods(params) {
    return request({
      url: API.getK8sPods, // 使用getK8sPods API配合Deployment筛选
      method: 'get',
      params: {
        ...params,
        selector: `app=${params.name}`
      }
    })
  },
  
  // 获取资源相关事件
  getResourceEvents(params) {
    return request({
      url: API.getK8sDeploymentEvents,
      method: 'get',
      params
    })
  }
}

// Kubernetes Service API
export const k8sServiceApi = {
  // 获取Service详情
  getServiceDetails(params) {
    return request({
      url: API.getK8sServiceDetail,
      method: 'get',
      params
    })
  },
  
  // 获取Service的端点
  getServiceEndpoints(params) {
    return request({
      url: API.getK8sServiceDetail + '/endpoints',
      method: 'get',
      params
    })
  },
  
  // 获取资源相关事件
  getResourceEvents(params) {
    return request({
      url: API.getK8sServiceDetail + '/events',
      method: 'get',
      params
    })
  }
} 