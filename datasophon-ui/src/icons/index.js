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
import SvgIcon from './SvgIcon'// svg component

// register globally
Vue.component('svg-icon', SvgIcon)
const common = require.context('./common', false, /\.svg$/)
const colony = require.context('./colony', false, /\.svg$/)
// 删除对旧目录的引用，因为文件已移至common目录
const requireAll = requireContext => requireContext.keys().map(requireContext)
requireAll(common)
requireAll(colony)
// 删除对旧服务图标目录的引用
