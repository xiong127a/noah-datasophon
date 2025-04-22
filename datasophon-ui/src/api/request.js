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



import Vue from 'vue'
import axios from 'axios'
// import '@/api/interceptors'

// post数据处理
const handleParams = function(data) {
  const params = new FormData()
  for(let key in data) {
    params.append(key, data[key])
  }
  return params
}

const axiosGet = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    // 检查URL是否存在
    if (!url) {
      console.error('axiosGet: URL未定义');
      reject(new Error('URL未定义'));
      return;
    }
    
    // K8s仪表盘接口特殊处理
    if (url.indexOf('/api/k8s/dashboard/') !== -1) {
      console.log('axiosGet发送K8s仪表盘请求:', url, params);
      
      // 确保clusterId存在
      if (!params.clusterId) {
        console.warn('K8s请求缺少clusterId参数，尝试从localStorage获取');
        params.clusterId = window.localStorage.getItem('clusterId');
      }
    }
    
    axios({
      method: 'get',
      url: url,
      params: params,
    }).then(res => {
      // 检查特定URL的响应
      if (url.indexOf('/api/k8s/dashboard/') !== -1) {
        console.log('K8s接口响应:', url, res.status, res.data);
      }
      
      resolve(res.data)
    }).catch(error => {
      // 增强错误处理
      console.error('请求失败:', url, error);
      
      // 对于K8s仪表盘接口，返回一个空的成功响应，避免UI崩溃
      if (url.indexOf('/api/k8s/dashboard/') !== -1) {
        console.warn('K8s接口请求失败，返回空数据');
        resolve({
          code: 200,
          msg: 'fallback response',
          data: {}
        });
        return;
      }
      
      reject(error)
    })
  })
}
const axiosDelete = function (url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'delete',
      url: url,
      params: params,
    })
      .then((res) => {
        resolve(res.data)
      })
      .catch((error) => {
        reject(error)
      })
  })
}
const axiosPost = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'post',
      url: url,
      data: handleParams(params),
      ContentType:"application/json;charset=UTF-8"
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}
const axiosJsonPost = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'post',
      url: url,
      data: params,
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

// 文件上传，params为form-data
const axiosPostUpload = function(url, params = {}) {
  return new Promise((resolve, reject) => {
    axios({
      method: 'post',
      url: url,
      data: params
    }).then(res => {
      resolve(res.data)
    }).catch(error => {
      reject(error)
    })
  })
}

Vue.prototype.$axiosGet = axiosGet// get请求
Vue.prototype.$axiosPost = axiosPost// post请求
Vue.prototype.$axiosPostUpload = axiosPostUpload// 文件上传-post请求
Vue.prototype.$axiosJsonPost = axiosJsonPost// jsonpost请求
Vue.prototype.$axiosDelete = axiosDelete// delete请求
