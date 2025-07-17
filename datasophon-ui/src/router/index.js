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
import Router from 'vue-router'
import { formatRoutes } from '@/utils/routerUtil'
import store from '@/store'
// import _this from '../main.js'
//  console.log('_this', _this)
Vue.use(Router)

// 不需要登录拦截的路由配置
const loginIgnore = {
  names: ['404', '403'], //根据路由名称匹配
  paths: ['/login'], //根据路由fullPath匹配
  /**
   * 判断路由是否包含在该配置中
   * @param route vue-router 的 route 对象
   * @returns {boolean}
   */
  includes(route) {
    return this.names.includes(route.name) || this.paths.includes(route.path)
  },
}

/**
 * 初始化路由实例
 * @param isAsync 是否异步路由模式
 * @returns {VueRouter}
 */
function initRouter(isAsync) {
  const options = require('./config').default
  formatRoutes(options.routes)
  const router = new Router({...options,mode:'hash'})

  // 路由守卫
  router.beforeEach((to, from, next) => {
    // 如果路由是服务详情页面，更新serviceId
    if (to.path.includes('/service-manage/service-list/') && to.params.serviceId) {
      // 直接同步设置serviceId，不使用setTimeout
      console.log('路由守卫直接设置serviceId:', to.params.serviceId);
      store.commit('setting/setServiceId', to.params.serviceId);
    }
    
    // 继续路由导航
    next();
  })

  return router
}
export { loginIgnore, initRouter }
