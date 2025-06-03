<!--
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


 * @describe: 配置参数
 * @Date: 2022-10-27
 * @FilePath: \datasophon-ui\src\pages\serviceManage\components\ServiceConfig.vue
-->
<template>
  <div>
    <div class="service-config-container">    <!-- 顶部过滤器栏 -->
    <div class="filter-bar">
      <!-- 对比模式下的过滤器 -->
      <template v-if="compareMode">
        <div class="version-compare-container">
          <!-- 版本比较行 -->
          <div class="version-compare-row">
            <div class="compare-header-section">
              <span class="compare-text">Comparing Changes in:</span>
              
              <!-- 第一个版本选择器 -->
              <a-dropdown :trigger="['click']" overlayClassName="version-dropdown">
                <a-button style="width: 120px" class="filter-select">
                  <span v-if="currentVersion !== undefined">
                    <span class="dropdown-title">版本</span> {{ currentVersion }}
                  </span>
                  <span v-else>选择版本</span>
                  <a-icon type="down" />
                </a-button>
                <div slot="overlay" class="version-list-container" @click.stop>
                  <div class="version-list-header">
                    <a-input-search 
                      placeholder="搜索" 
                      style="width: 100%"
                      @change="onVersionSearchChange"
                      v-model="versionSearchKeyword"
                      @click.stop
                    />
                  </div>
                  <div class="version-list">
                    <div 
                      v-for="(versionItem, index) in filteredVersionList" 
                      :key="index"
                      class="version-item"
                      :class="{ 'version-item-active': currentVersion === versionItem.version }"
                    >
                      <div class="version-item-content" @click="changeVersion(versionItem.version)">
                        <div class="version-item-header">
                          <span class="version-number"><span class="dropdown-title">版本</span> {{ versionItem.version }}</span>
                          <span v-if="versionItem.isCurrent" class="version-tag">当前使用</span>
                        </div>
                        <div class="version-item-description">{{ versionItem.description }}</div>
                        <div class="version-item-footer">
                          <span>{{ versionItem.editor }}</span>
                          <span>编辑于 {{ versionItem.editTime }}</span>
                        </div>
                        <a-icon v-if="currentVersion === versionItem.version" type="check" class="version-selected-icon" />
                      </div>
                    </div>
                  </div>
                </div>
              </a-dropdown>
              
              <span class="compare-text">with</span>
              
              <!-- 第二个版本选择器 -->
              <a-dropdown :trigger="['click']" overlayClassName="version-dropdown">
                <a-button style="width: 120px" class="filter-select">
                  <span v-if="compareVersion !== undefined">
                    <span class="dropdown-title">版本</span> {{ compareVersion }}
                  </span>
                  <span v-else>选择版本</span>
                  <a-icon type="down" />
                </a-button>
                <div slot="overlay" class="version-list-container" @click.stop>
                  <div class="version-list-header">
                    <a-input-search 
                      placeholder="搜索" 
                      style="width: 100%"
                      @change="onCompareVersionSearchChange"
                      v-model="compareVersionSearchKeyword"
                      @click.stop
                    />
                  </div>
                  <div class="version-list">
                    <div 
                      v-for="(versionItem, index) in filteredCompareVersionList" 
                      :key="index"
                      class="version-item"
                      :class="{ 'version-item-active': compareVersion === versionItem.version }"
                    >
                      <div class="version-item-content" @click="changeCompareVersion(versionItem.version)">
                        <div class="version-item-header">
                          <span class="version-number"><span class="dropdown-title">版本</span> {{ versionItem.version }}</span>
                          <span v-if="versionItem.isCurrent" class="version-tag">当前使用</span>
                        </div>
                        <div class="version-item-description">{{ versionItem.description }}</div>
                        <div class="version-item-footer">
                          <span>{{ versionItem.editor }}</span>
                          <span>编辑于 {{ versionItem.editTime }}</span>
                        </div>
                        <a-icon v-if="compareVersion === versionItem.version" type="check" class="version-selected-icon" />
                      </div>
                    </div>
                  </div>
                </div>
              </a-dropdown>
              
              <!-- 关闭对比按钮 -->
              <a-button 
                type="link" 
                class="close-compare-btn"
                @click="closeCompareMode"
              >
                <a-icon type="close" />
              </a-button>
            </div>
          </div>
          
          <!-- 角色组、显示模式和筛选放在下方 -->
          <div class="filter-controls">
            <!-- 角色组选择 -->
            <div class="filter-control-item">
              <a-dropdown :trigger="['click']" overlayClassName="config-group-dropdown">
                <a-button style="width: 200px" class="filter-select">
                  <span v-if="currentId !== undefined && getGroupName(currentId)">
                    <span class="dropdown-title">角色组</span> {{ getGroupName(currentId) }}
                  </span>
                  <span v-else>角色组</span>
                  <a-icon type="down" />
                </a-button>
                <div slot="overlay" class="config-group-list-container" @click.stop>
                  <div class="config-group-list">
                    <div 
                      v-for="item in GroupList" 
                      :key="item.id"
                      class="config-group-item"
                      :class="{ 'config-group-item-active': currentId === item.id }"
                      @click="changeConfigGroup(item.id)"
                    >
                      <div class="config-group-item-header">
                        <span class="config-group-name"><span class="dropdown-title">角色组</span> {{ item.roleGroupName }}</span>
                      </div>
                      <a-icon v-if="currentId === item.id" type="check" class="config-group-selected-icon" />
                    </div>
                  </div>
                </div>
              </a-dropdown>
            </div>
            
            <!-- 显示模式选择 -->
            <div class="filter-control-item">
              <a-radio-group 
                v-model="showOnlyDifferences" 
                @change="handleShowModeChange"
                buttonStyle="solid"
                size="default"
              >
                <a-radio-button :value="true">只显示差异</a-radio-button>
                <a-radio-button :value="false">显示全部</a-radio-button>
              </a-radio-group>
            </div>
            
            <!-- 筛选框 -->
            <div class="filter-control-item">
              <a-input-search
                v-model="searchKeyword"
                placeholder="筛选中"
                style="width: 200px"
                class="filter-select"
                @search="handleSearch"
                @change="handleInputChange"
                allowClear
              />
            </div>
          </div>
        </div>
      </template>
      
      <!-- 非对比模式下的过滤器 -->
      <template v-else>
        <div class="filter-item">
          <a-dropdown :trigger="['click']" overlayClassName="version-dropdown">
            <a-button style="width: 200px" class="filter-select">
              <span v-if="currentVersion !== undefined">
                <span class="dropdown-title">版本</span> {{ currentVersion }}
              </span>
              <span v-else>选择版本</span>
              <a-icon type="down" />
            </a-button>
            <div slot="overlay" class="version-list-container" @click.stop>
              <div class="version-list-header">
                <a-input-search 
                  placeholder="搜索" 
                  style="width: 100%"
                  @change="onVersionSearchChange"
                  v-model="versionSearchKeyword"
                  @click.stop
                />
              </div>
              <div class="version-list">
                <div 
                  v-for="(versionItem, index) in filteredVersionList" 
                  :key="index"
                  class="version-item"
                  :class="{ 'version-item-active': currentVersion === versionItem.version }"
                >
                  <div class="version-item-content" @click="changeVersion(versionItem.version)">
                    <div class="version-item-header">
                      <span class="version-number"><span class="dropdown-title">版本</span> {{ versionItem.version }}</span>
                      <span v-if="versionItem.isCurrent" class="version-tag">当前使用</span>
                    </div>
                    <div class="version-item-description">{{ versionItem.description }}</div>
                    <div class="version-item-footer">
                      <span>{{ versionItem.editor }}</span>
                      <span>编辑于 {{ versionItem.editTime }}</span>
                    </div>
                    <a-icon v-if="currentVersion === versionItem.version" type="check" class="version-selected-icon" />
                  </div>
                  <!-- 操作按钮区域 -->
                  <div class="version-actions">
                    <!-- 对比按钮 -->
                    <a-tooltip title="对比当前版本" placement="top">
                      <div 
                        v-if="currentVersion !== versionItem.version" 
                        class="version-action-btn version-compare-btn"
                        @click="startCompareWithVersion(versionItem.version)"
                      >
                        <a-icon type="swap" />
                      </div>
                    </a-tooltip>
                  </div>
                </div>
              </div>
            </div>
          </a-dropdown>
        </div>
        
        <!-- 添加保存/恢复按钮 -->
        <div class="filter-item" v-if="currentVersion !== undefined">
          <a-button @click="handleSaveOrRestore()" class="save-button">
            {{ isCurrentVersionActive ? '保存' : '恢复' }}
          </a-button>
        </div>
        
        <div class="filter-item">
          <a-dropdown :trigger="['click']" overlayClassName="config-group-dropdown">
            <a-button style="width: 200px" class="filter-select">
              <span v-if="currentId !== undefined && getGroupName(currentId)">
                <span class="dropdown-title">角色组</span> {{ getGroupName(currentId) }}
              </span>
              <span v-else>角色组</span>
              <a-icon type="down" />
            </a-button>
            <div slot="overlay" class="config-group-list-container" @click.stop>
              <div class="config-group-list">
                <div 
                  v-for="item in GroupList" 
                  :key="item.id"
                  class="config-group-item"
                  :class="{ 'config-group-item-active': currentId === item.id }"
                  @click="changeConfigGroup(item.id)"
                >
                  <div class="config-group-item-header">
                    <span class="config-group-name"><span class="dropdown-title">角色组</span> {{ item.roleGroupName }}</span>
                  </div>
                  <a-icon v-if="currentId === item.id" type="check" class="config-group-selected-icon" />
                </div>
              </div>
            </div>
          </a-dropdown>
        </div>
        
        <div class="filter-item">
          <a-input-search
            v-model="searchKeyword"
            placeholder="筛选中"
            style="width: 200px"
            class="filter-select"
            @search="handleSearch"
            @change="handleInputChange"
            allowClear
          />
        </div>
      </template>
    </div>
    
    <!-- 配置组区域 - 扁平化设计 -->
    <div class="config-area">
      <a-spin :spinning="loading">
        <!-- 对比模式下的内容 -->
        <template v-if="compareMode && !loading">
          <div class="config-panel compare-header-panel">
            <table class="compare-table">
              <thead>
                <tr class="compare-header-row">
                  <th class="compare-header-cell attribute-header">属性名称</th>
                  <th class="compare-header-cell">
                    <div class="version-header">
                      <span class="version-title"><span class="dropdown-title">版本</span> {{ currentVersion }}</span>
                      <span v-if="isCurrentVersionActive" class="version-tag">当前使用</span>
                    </div>
                  </th>
                  <th class="compare-header-cell">
                    <div class="version-header">
                      <span class="version-title"><span class="dropdown-title">版本</span> {{ compareVersion }}</span>
                      <span v-if="isCompareVersionActive" class="version-tag">当前使用</span>
                    </div>
                  </th>
                </tr>
              </thead>
            </table>
          </div>
          
          <!-- 按分组显示配置项 -->
          <div 
            v-for="(group, groupName) in filteredCompareData"
            :key="groupName"
            class="config-panel"
          >
            <div 
              class="panel-header" 
              @click="toggleGroup(groupName)"
            >
              {{ isKubernetesConfig(groupName) ? formatK8sGroupTitle(groupName) : convertGroupName(groupName) }}
              <a-icon 
                :type="isGroupExpanded[groupName] ? 'up' : 'right'" 
                class="toggle-icon" 
              />
            </div>
            
            <div v-show="isGroupExpanded[groupName]" class="panel-content">
              <table class="compare-table">
                <tbody>
                  <tr 
                    v-for="(item, index) in group" 
                    :key="index"
                    class="compare-row-simple"
                    :class="{ 
                      'different': item.isDifferent,
                      'k8s-config-divider': item.isDivider,
                      'k8s-config-first-divider': item.isFirstDivider
                    }"
                  >
                    <!-- 如果是分隔符，显示分隔标题 -->
                    <template v-if="item.isDivider || item.isFirstDivider">
                      <td colspan="3" class="k8s-config-divider-cell">
                        <div class="k8s-divider-label" v-html="item.dividerLabel">
                        </div>
                      </td>
                    </template>
                    
                    <!-- 如果是普通配置项，显示正常的比较行 -->
                    <template v-else>
                      <td class="compare-cell-simple attribute-cell">{{ item.name }}</td>
                      <td class="compare-cell-simple value-cell">{{ item[currentVersion] }}</td>
                      <td class="compare-cell-simple value-cell">{{ item[compareVersion] }}</td>
                    </template>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </template>
        
        <!-- 非对比模式下的内容 -->
        <template v-else>
          <!-- 渲染角色分组，包括常规配置和Kubernetes配置 -->
          <div 
            v-for="(group, groupName) in filteredVisibleGroups"
            :key="groupName"
            class="config-panel"
          >
            <div 
              class="panel-header" 
              @click="toggleGroup(groupName)"
            >
              {{ convertGroupName(groupName) }}
              <a-icon 
                :type="isGroupExpanded[groupName] ? 'up' : 'right'" 
                class="toggle-icon" 
              />
            </div>
            
            <div v-show="isGroupExpanded[groupName]" class="panel-content">
              <!-- 处理新的数据结构：同时包含常规配置和Kubernetes配置的角色分组 -->
              <template v-if="Array.isArray(group) && group.length === 1 && group[0].hasKubernetesConfig">
                <!-- 先显示Kubernetes配置（使用标签页）-->
                <div class="kubernetes-section" v-if="group[0].kubernetesSubGroups && Object.keys(group[0].kubernetesSubGroups).length > 0">
                  <div class="kubernetes-tabs-header">
                    Kubernetes 配置
                  </div>
                  
                  <a-tabs 
                    v-model="activeKubernetesTabs[groupName]" 
                    class="kubernetes-tabs"
                  >
                    <a-tab-pane 
                      v-for="(configs, subGroupName) in group[0].kubernetesSubGroups" 
                      :key="subGroupName" 
                    >
                      <template slot="tab">
                        <span v-html="formatSubGroupName(subGroupName)"></span>
                      </template>
                      <CommonTemplate
                        :ref="`template_${groupName}_${subGroupName}`"
                        :steps4Data="steps4Data"
                        :templateData="configs"
                      />
                    </a-tab-pane>
                  </a-tabs>
                </div>
                
                <!-- 再显示常规配置 -->
                <CommonTemplate
                  :ref="`template_${groupName}`"
                  :steps4Data="steps4Data"
                  :templateData="group[0].regularConfigs"
                  v-if="group[0].regularConfigs && group[0].regularConfigs.length > 0"
                />
              </template>
              <!-- 处理常规配置 -->
              <template v-else-if="Array.isArray(group) || (group.items && Array.isArray(group.items))">
                <CommonTemplate
                    :ref="`template_${groupName}`"
                    :steps4Data="steps4Data"
                    :templateData="Array.isArray(group) ? group : group.items"
                />
                
                <!-- 添加模板内容显示框 -->
                <div v-if="!Array.isArray(group) && group.templateContent" class="template-content-container">
                  <div class="template-content-title">{{ group.displayName || '模板内容' }}:</div>
                  <a-textarea
                    :value="group.templateContent"
                    :auto-size="{ minRows: 3, maxRows: 10 }"
                    readonly
                    class="template-content-textarea"
                  />
                </div>
              </template>
              <div v-else class="config-error-message">
                配置数据格式错误，无法显示表单
              </div>
            </div>
          </div>
        </template>
      </a-spin>
    </div>
    </div>

    <!-- 添加备注弹框 -->
    <a-modal
      v-model="saveDialogVisible"
      title="保存配置"
      :maskClosable="false"
      @ok="confirmSave"
      okText="确定"
      cancelText="取消"
    >
      <div>
        <a-form-item label="备注" :labelCol="{ span: 4 }" :wrapperCol="{ span: 20 }">
          <a-textarea
            v-model="configDescription"
            placeholder="请输入配置备注信息"
            :rows="4"
            style="width: 100%"
          />
          <div class="placeholder-hint">（如不填写，系统将自动生成包含修改内容的备注）</div>
        </a-form-item>
      </div>
    </a-modal>
    
    <!-- 添加恢复版本弹框 -->
    <a-modal
      v-model="restoreDialogVisible"
      title="恢复版本确认"
      :maskClosable="false"
      @ok="confirmRestore"
      okText="确定"
      cancelText="取消"
    >
      <div>
        <a-form-item label="备注" :labelCol="{ span: 4 }" :wrapperCol="{ span: 20 }">
          <a-textarea
            v-model="restoreDescription"
            placeholder="请输入恢复版本的备注信息"
            :rows="4"
            style="width: 100%"
          />
          <div class="placeholder-hint">（如不填写，系统将自动生成包含恢复版本的备注）</div>
        </a-form-item>
      </div>
    </a-modal>
  </div>
</template>
<script>
import CommonTemplate from "@/components/steps/FixedCommonTemplate.vue";
import {mapActions, mapState} from "vuex";
import {getServiceName} from "@/utils/util";
import _ from 'lodash';

export default {
  name: "ServiceConfig",
  components: { CommonTemplate },
  props: {
    steps4Data: Object,
    serviceId: {
      type: [String, Number],
      required: true
    },
    serviceName: {
      type: String,
      default: ''
    }
  },
  data() {
    return {
      loading: false,
      templateData: {},
      verSionList: [],
      GroupList: [],
      currentId: undefined,
      currentVersion: undefined,
      compareMode: false,
      compareVersion: undefined,
      compareData: null, // 存储对比数据
      isGroupExpanded: {}, // 存储每个配置组的展开状态
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      labelCol: {
        xs: {span: 24},
        sm: {span: 5},
      },
      wrapperCol: {
        xs: {span: 24},
        sm: {span: 19},
      },
      radioStyle: {
        display: 'block',
        height: '30px',
        lineHeight: '30px',
        marginTop: '5px',
      },
      value: 0,
      // 新增过滤相关数据
      searchKeyword: '',
      // 新增备注弹框相关数据
      saveDialogVisible: false,
      configDescription: '',
      placeholderText: '请输入配置备注信息\n（如不填写，系统将自动生成包含修改内容的备注）',
      versionSearchKeyword: '',
      compareVersionSearchKeyword: '',
      isCurrentVersionActive: false, // 当前版本是否为激活版本
      isCompareVersionActive: false, // 对比版本是否为激活版本
      showOnlyDifferences: true, // 是否只显示差异项，默认为true
      // 新增恢复版本相关数据
      restoreDialogVisible: false, // 控制恢复确认对话框的显示
      restoreDescription: '', // 存储恢复版本的备注
      activeKubernetesTabs: {}, // 存储每个角色组的Kubernetes标签页激活状态
      k8sSubGroupChineseNames: { // Copied from step7.vue
        'persistentVolumeClaims': '持久卷声明',
        'resources': '资源规格',
        'services': '服务暴露',
        'node_port_mappings': '节点端口映射',
        'cluster_port_mappings': '集群端口映射',
        'requests_memory': '内存请求',
        'requests_cpu': 'CPU请求',
        'limits_memory': '内存限制',
        'limits_cpu': 'CPU限制',
        'storage_classes': '存储类',
        'mountPath': '挂载路径',
        'storage': '存储容量',
        // Add more mappings as needed
      },
    };
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    // 添加计算属性，根据筛选条件过滤配置项
    filteredTemplateData() {
      if (!this.templateData || Object.keys(this.templateData).length === 0) {
        return {};
      }

      // 如果没有搜索关键字，返回原始数据
      if (!this.searchKeyword) {
        return this.templateData;
      }

      const result = {};
      
      // 遍历所有配置组
      Object.entries(this.templateData).forEach(([groupName, configData]) => {
        // 检查配置组的数据结构
        if (Array.isArray(configData) && configData.length === 1 && configData[0].hasKubernetesConfig) {
          // 这是一个包含常规配置和Kubernetes配置的角色分组
          const roleGroup = configData[0];
          const regularConfigs = roleGroup.regularConfigs || [];
          const kubernetesSubGroups = roleGroup.kubernetesSubGroups || {};
          
          // 过滤常规配置
          const filteredRegularConfigs = regularConfigs.filter(item => {
            const keyword = this.searchKeyword.toLowerCase();
            const label = (item.label || '').toLowerCase();
            const name = (item.name || '').replaceAll('!', '.').toLowerCase();
            const value = String(item.value || '').toLowerCase();
            const description = (item.description || '').toLowerCase();
            
            return label.includes(keyword) || 
                   name.includes(keyword) || 
                   value.includes(keyword) || 
                   description.includes(keyword);
          });
          
          // 过滤Kubernetes配置
          const filteredK8sSubGroups = {};
          let hasMatchingK8sConfig = false;
          
          Object.entries(kubernetesSubGroups).forEach(([subGroupName, configs]) => {
            const filteredConfigs = configs.filter(item => {
              const keyword = this.searchKeyword.toLowerCase();
              const label = (item.label || '').toLowerCase();
              const name = (item.name || '').replaceAll('!', '.').toLowerCase();
              const value = String(item.value || '').toLowerCase();
              const description = (item.description || '').toLowerCase();
              
              return label.includes(keyword) || 
                     name.includes(keyword) || 
                     value.includes(keyword) || 
                     description.includes(keyword);
            });
            
            if (filteredConfigs.length > 0) {
              filteredK8sSubGroups[subGroupName] = filteredConfigs;
              hasMatchingK8sConfig = true;
            }
          });
          
          // 如果有匹配的配置项，则添加到结果中
          if (filteredRegularConfigs.length > 0 || hasMatchingK8sConfig) {
            result[groupName] = [{
              hasKubernetesConfig: true,
              regularConfigs: filteredRegularConfigs,
              kubernetesSubGroups: filteredK8sSubGroups
            }];
          }
        } else if (configData && configData.isKubernetesGroup) {
          // 处理旧格式的Kubernetes配置组（向后兼容）
          const filteredSubGroups = {};
          let hasMatchingConfig = false;
          
          // 过滤每个子组内的配置项
          Object.entries(configData.subGroups).forEach(([subGroupName, configs]) => {
            const filteredConfigs = configs.filter(item => {
              const keyword = this.searchKeyword.toLowerCase();
              const label = (item.label || '').toLowerCase();
              const name = (item.name || '').replaceAll('!', '.').toLowerCase();
              const value = String(item.value || '').toLowerCase();
              const description = (item.description || '').toLowerCase();
              
              return label.includes(keyword) || 
                     name.includes(keyword) || 
                     value.includes(keyword) || 
                     description.includes(keyword);
            });
            
            if (filteredConfigs.length > 0) {
              filteredSubGroups[subGroupName] = filteredConfigs;
              hasMatchingConfig = true;
            }
          });
          
          // 如果至少有一个子组内有匹配的配置项，则添加到结果中
          if (hasMatchingConfig) {
            result[groupName] = {
              ...configData,
              subGroups: filteredSubGroups
            };
          }
        } else {
          // 处理普通配置组
        // 处理不同的数据结构
        const configItems = Array.isArray(configData) ? configData : (configData.items || []);
        
        // 过滤符合条件的配置项
        const filteredItems = configItems.filter(item => {
          // 搜索label、name、value和description
          const keyword = this.searchKeyword.toLowerCase();
          const label = (item.label || '').toLowerCase();
          const name = (item.name || '').replaceAll('!', '.').toLowerCase();
          const value = String(item.value || '').toLowerCase();
          const description = (item.description || '').toLowerCase();
          
          return label.includes(keyword) || 
                 name.includes(keyword) || 
                 value.includes(keyword) || 
                 description.includes(keyword);
        });
        
        // 如果过滤后有配置项，添加到结果中
        if (filteredItems.length > 0) {
          if (Array.isArray(configData)) {
            result[groupName] = filteredItems;
          } else {
            // 保留原有结构
            result[groupName] = {
              ...configData,
              items: filteredItems
            };
            }
          }
        }
      });
      
      return result;
    },
    // 将配置组分为kubernetes配置和非kubernetes配置
    kubernetesGroups() {
      const groups = this.filteredTemplateData || {};
      return Object.entries(groups)
        .filter(([_, group]) => group && 
                (Array.isArray(group) ? 
                  (group.length === 1 && group[0].hasKubernetesConfig) : 
                  group.isKubernetesGroup))
        .reduce((acc, [key, value]) => {
          acc[key] = value;
          return acc;
        }, {});
    },
    // 非kubernetes配置组
    nonKubernetesGroups() {
      const groups = this.filteredTemplateData || {};
      return Object.entries(groups)
        .filter(([_, group]) => !group || 
                (Array.isArray(group) ? 
                  !(group.length === 1 && group[0].hasKubernetesConfig) : 
                  !group.isKubernetesGroup))
        .reduce((acc, [key, value]) => {
          acc[key] = value;
          return acc;
        }, {});
    },
    filteredVersionList() {
      if (!this.verSionList || this.verSionList.length === 0) {
        return [];
      }

      // 确保versionSearchKeyword是字符串
      const searchKeyword = this.versionSearchKeyword || '';
      const keyword = typeof searchKeyword === 'string' ? searchKeyword.toLowerCase() : '';
      
      if (!keyword) {
        // 如果没有搜索关键字，返回所有版本（除了对比模式下已选择的compareVersion）
        return this.verSionList.filter(item => {
          const shouldFilter = this.compareMode && this.compareVersion && item.version === this.compareVersion;
          return !shouldFilter;
        });
      }
      
      return this.verSionList.filter(item => {
        // 搜索版本号
        const version = String(item.version || '').toLowerCase();
        // 搜索描述
        const description = (item.description || '').toLowerCase();
        // 搜索用户名
        const editor = (item.editor || '').toLowerCase();
        // 搜索修改时间
        const editTime = (item.editTime || '').toLowerCase();
        
        // 添加过滤条件：在对比模式下过滤掉已选择的compareVersion
        const matchesKeyword = 
          version.includes(keyword) || 
          description.includes(keyword) || 
          editor.includes(keyword) || 
          editTime.includes(keyword);
          
        const shouldFilter = this.compareMode && this.compareVersion && item.version === this.compareVersion;
        
        return matchesKeyword && !shouldFilter;
      });
    },
    filteredCompareVersionList() {
      if (!this.verSionList || this.verSionList.length === 0) {
        return [];
      }

      // 确保compareVersionSearchKeyword是字符串
      const searchKeyword = this.compareVersionSearchKeyword || '';
      const keyword = typeof searchKeyword === 'string' ? searchKeyword.toLowerCase() : '';
      
      if (!keyword) {
        // 如果没有搜索关键字，返回所有版本（除了当前选择的版本）
        return this.verSionList.filter(item => item.version !== this.currentVersion);
      }
      
      // 过滤掉当前已选择的版本，防止自己和自己比较
      return this.verSionList.filter(item => {
        // 搜索版本号
        const version = String(item.version || '').toLowerCase();
        // 搜索描述
        const description = (item.description || '').toLowerCase();
        // 搜索用户名
        const editor = (item.editor || '').toLowerCase();
        // 搜索修改时间
        const editTime = (item.editTime || '').toLowerCase();
        
        // 添加过滤条件：排除当前选择的版本
        const matchesKeyword = 
          version.includes(keyword) || 
          description.includes(keyword) || 
          editor.includes(keyword) || 
          editTime.includes(keyword);
          
        return matchesKeyword && item.version !== this.currentVersion;
      });
    },
    // 添加计算属性，用于过滤对比数据
    filteredCompareData() {
      if (!this.compareData) return {};
      
      // 如果没有搜索关键字，返回原始数据
      if (!this.searchKeyword) {
        return this.compareData;
      }
      
      // 过滤符合搜索条件的配置项
      const keyword = this.searchKeyword.toLowerCase();
      const result = {};
      
      Object.entries(this.compareData).forEach(([groupName, items]) => {
        const filteredItems = items.filter(item => {
          const name = (item.name || '').toLowerCase();
          const valueA = String(item[this.currentVersion] || '').toLowerCase();
          const valueB = String(item[this.compareVersion] || '').toLowerCase();
          
          return name.includes(keyword) || 
                 valueA.includes(keyword) || 
                 valueB.includes(keyword);
        });
        
        if (filteredItems.length > 0) {
          result[groupName] = filteredItems;
        }
      });
      
      return result;
    },
    filteredVisibleGroups() {
      const groups = this.filteredTemplateData || {}; // Changed from this.nonKubernetesGroups
      return Object.entries(groups)
        .filter(([groupName, group]) => {
          // 1. 处理新格式：同时包含常规配置和Kubernetes配置的角色分组
          if (Array.isArray(group) && group.length === 1 && group[0].hasKubernetesConfig) {
            const roleGroup = group[0];
            const hasVisibleRegularConfigs = (roleGroup.regularConfigs || []).some(item => !item.hidden);
            
            let hasVisibleK8sConfigs = false;
            if (roleGroup.kubernetesSubGroups) {
              hasVisibleK8sConfigs = Object.values(roleGroup.kubernetesSubGroups).some(configs => 
                (configs || []).some(item => !item.hidden)
              );
            }
            return hasVisibleRegularConfigs || hasVisibleK8sConfigs;
          }
          
          // 2. 处理旧格式：Kubernetes配置组 (isKubernetesGroup flag)
          // This case might become redundant if handlerTemplate standardizes everything
          // to the new hasKubernetesConfig structure, but keep for now for safety.
          else if (group && group.isKubernetesGroup) {
            let hasVisibleConfig = false;
            Object.values(group.subGroups || {}).forEach(configs => {
              if ((configs || []).some(item => !item.hidden)) {
                hasVisibleConfig = true;
              }
            });
            return hasVisibleConfig;
          }
          
          // 3. 处理普通数组格式的配置组
          else if (Array.isArray(group)) {
            // 只有当配置组中至少有一项不是hidden时才显示该组
            return group.some(item => !item.hidden);
          }
          
          // 4. 处理包含items属性的配置组
          else if (group && group.items && Array.isArray(group.items)) {
            return group.items.some(item => !item.hidden);
          }
          
          return false;
        })
        .reduce((acc, [key, value]) => {
          acc[key] = value;
          return acc;
        }, {});
    },
  },
  methods: {
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    getMoreMenu(props) {
      let arr = [
        {name: "重命名", key: "rename"},
        {name: "删除", key: "del"},
      ];
      // if (props.meta.obj.needRestart) arr.splice(2, 0, { name: "重启", key: "restart" })
      return arr.map((item, index) => {
        return (
            <div key={index}>
              <a
                  class="more-menu-btn"
                  style="border-width:0px;min-width:100px;color: #333;"
                  onClick={() => this.batchOpt(item, props)}
              >
                {item.name}
              </a>
            </div>
        );
      });
    },
    toggleGroup(groupName) {
      this.$set(this.isGroupExpanded, groupName, !this.isGroupExpanded[groupName])
    },
    changeName(params) {
      this.GroupList.forEach(item => {
        if (item.id === params.roleGroupId) {
          item.roleGroupName = params.roleGroupName
        }
      })
    },
    renameCharacter(props) {
      const self = this;
      let width = 520;
      let title = "重命名";
      let content = (
          <RenameGroup grouopObj={props} callBack={(params) => self.changeName(params)}/>
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div/>;
        },
      });
    },
    batchOpt(item, props) {
      if (item.key === 'rename') {
        this.renameCharacter(props)
        return false
      }
      this.$confirm({
        width: 450,
        title: () => {
          return (
              <div style="font-size: 22px;">
                <a-icon
                    type="question-circle"
                    style="color:#2F7FD1 !important;margin-right:10px"
                />
                提示
              </div>
          );
        },
        content: (
            <div style="margin-top:20px">
              <div style="padding:0 65px;font-size: 16px;color: #555555;">
                {'确认删除吗？'}
              </div>
              <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
                <a-button
                    style="margin-right:10px;"
                    type="primary"
                    onClick={() => this.confirmDel(item, props)}
                >
                  确定
                </a-button>
                <a-button
                    style="margin-right:10px;"
                    onClick={() => this.$destroyAll()}
                >
                  取消
                </a-button>
              </div>
            </div>
        ),
        icon: () => {
          return <div/>;
        },
        closable: true,
      });
    },
    confirmDel(item, props) {
      let serviceName = getServiceName(this.serviceId);

      let params = {
        roleGroupId: props.id,
        serviceName: serviceName
      };
      this.$axiosPost(global.API.delGroup, params).then((res) => {
        this.$destroyAll();
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.GroupList = this.GroupList.filter(item => item.id !== props.id)
          if (this.GroupList.length > 0 && this.currentId === props.id) {
            this.currentId = this.GroupList[0].id;
          }
        }
      });
    },
    handlerClick(item, childIndex) {
      console.log(item);
      this.currentId = item.id
      this.getConfigVersion()
    },
    // 新增配置组切换方法
    changeConfigGroup(val) {
      this.currentId = val;
      // 重置筛选状态
      this.searchKeyword = '';
      // 折叠所有分组
      Object.keys(this.isGroupExpanded).forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, false);
      });
      this.getConfigVersion();
    },
    // 新增搜索方法
    handleSearch(value) {
      this.searchKeyword = value;
      // 搜索后自动展开包含匹配项的分组
      this.$nextTick(() => {
        this.expandMatchingGroups();
      });
    },
    
    // 处理输入变化
    handleInputChange(e) {
      this.searchKeyword = e.target.value;
      // 输入变化后自动展开包含匹配项的分组
      this.$nextTick(() => {
        this.expandMatchingGroups();
      });
    },
    
    // 展开包含匹配项的分组
    expandMatchingGroups() {
      // 如果没有搜索关键字，则折叠所有分组
      if (!this.searchKeyword) {
        Object.keys(this.templateData).forEach(groupName => {
          this.$set(this.isGroupExpanded, groupName, false);
        });
        return;
      }
      
      // 获取筛选后的分组
      const filteredGroups = Object.keys(this.filteredTemplateData);
      
      // 展开包含匹配项的分组
      filteredGroups.forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, true);
      });
    },
    
    handlearrayWithData(a) {
      let obj = {};
      let arr = [];
      for (let k in a) {
        if (k.includes("arrayWith")) {
          let key = "";
          if (k.includes("arrayWithKey")) {
            key = k.split("arrayWithKey")[0];
            arr.push(key);
          }
          if (k.includes("arrayWithVal")) {
            key = k.split("arrayWithVal")[0];
            arr.push(key);
          }
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      for (let f in obj) {
        let keys = [];
        let vals = [];
        for (let i in a) {
          if (i.includes(f)) {
            if (i.includes("arrayWithKey")) {
              keys.push(i);
            }
            if (i.includes("arrayWithVal")) {
              vals.push(i);
            }
          }
        }
        keys.map((item, index) => {
          obj[f].push({
            [`${a[item]}`]: a[vals[index]],
          });
        });
      }
      return obj;
    },
    handleMultipleData(a) {
      let obj = {};
      let arr = [];
      for (let k in a) {
        if (k.includes("multiple")) {
          let key = k.split("multiple")[0];
          arr.push(key);
          arr = [...new Set(arr)];
        }
      }
      arr.map((item) => {
        obj[item] = [];
      });
      // obj{ a: , b: }
      for (let f in obj) {
        let vals = [];
        for (let i in a) {
          if (i.includes(f)) {
            if (i.includes("multiple")) {
              vals.push(i);
            }
          }
        }
        vals.map((item, index) => {
          obj[f].push(a[vals[index]]);
        });
      }
      return obj;
    },
    // 单个标签页的保存
    async handleSubmit() {
      try {
        // 1. 获取所有表单引用
        const allFormRefs = [];
        
        // 处理所有配置组
        Object.entries(this.templateData).forEach(([groupName, group]) => {
          // 处理新结构：同时包含常规配置和Kubernetes配置的角色分组
          if (Array.isArray(group) && group.length === 1 && group[0].hasKubernetesConfig) {
            // 添加常规配置表单
            const regularFormRef = this.$refs[`template_${groupName}`]?.[0];
            if (regularFormRef) {
              allFormRefs.push(regularFormRef);
            }
            
            // 添加Kubernetes配置表单
            Object.keys(group[0].kubernetesSubGroups).forEach(subGroupName => {
              const k8sFormRef = this.$refs[`template_${groupName}_${subGroupName}`]?.[0];
              if (k8sFormRef) {
                allFormRefs.push(k8sFormRef);
              }
            });
          } 
          // 处理旧结构：独立的Kubernetes配置组
          else if (group && group.isKubernetesGroup) {
            Object.keys(group.subGroups).forEach(subGroupName => {
              const ref = this.$refs[`template_${groupName}_${subGroupName}`]?.[0];
              if (ref) {
                allFormRefs.push(ref);
              }
            });
          } 
          // 处理常规配置组
          else {
            const formRef = this.$refs[`template_${groupName}`]?.[0];
            if (formRef) {
              allFormRefs.push(formRef);
            }
          }
        });

        // 2. 并行验证所有表单
        await Promise.all(
            allFormRefs.map(form =>
                new Promise((resolve, reject) => {
                  form.form.validateFields()
                      .then(values => resolve(values))
                      .catch(error => reject(error))
                })
            )
        );

        // 3. 合并所有配置数据
        const mergedValues = allFormRefs.reduce((acc, form) => {
          return { ...acc, ...form.form.getFieldsValue() };
        }, {});

        // 4. 准备所有配置项
        const allConfigItems = [];
        
        // 收集所有配置项
        Object.entries(this.templateData).forEach(([groupName, group]) => {
          // 处理新结构：同时包含常规配置和Kubernetes配置的角色分组
          if (Array.isArray(group) && group.length === 1 && group[0].hasKubernetesConfig) {
            // 添加常规配置
            allConfigItems.push(...group[0].regularConfigs);
            
            // 添加Kubernetes配置
            Object.values(group[0].kubernetesSubGroups).forEach(configs => {
              allConfigItems.push(...configs);
            });
          } 
          // 处理旧结构：独立的Kubernetes配置组
          else if (group && group.isKubernetesGroup) {
            Object.values(group.subGroups).forEach(configs => {
              allConfigItems.push(...configs);
            });
          } 
          // 处理常规配置组
          else if (Array.isArray(group)) {
            allConfigItems.push(...group);
          } else if (group && group.items && Array.isArray(group.items)) {
            allConfigItems.push(...group.items);
          }
        });

        // 5. 更新配置项值
        Object.entries(mergedValues).forEach(([name, value]) => {
          const targetItem = allConfigItems.find(item => item.name === name);
          if (targetItem) {
            targetItem.value = value;
          }
        });

        // 6. 处理配置项名称
        const processedItems = allConfigItems.map(item => ({
          ...item,
          name: item.name.replaceAll("!", ".")
        }));

        // 7. 过滤不需要的配置项
        const filterParam = processedItems.filter(
            item => !(!item.required && item.hidden)
        );

        // 8. 获取当前用户信息
        const userStr = localStorage.getItem(process.env.VUE_APP_USER_KEY);
        const currentUser = userStr ? JSON.parse(userStr) : {};

        // 9. 构建保存参数
        const saveParam = {
          clusterId: this.clusterId,
          serviceName: this.serviceName,
          serviceConfig: JSON.stringify(filterParam),
          roleGroupId: this.currentId,
          description: this.configDescription,
          userId: currentUser.id,
          username: currentUser.username
        };

        // 10. 提交保存
        const res = await this.$axiosPost(global.API.saveServiceConfig, saveParam);
        console.log("保存配置响应:", res);
        
        if (res.code === 200) {
          // 根据返回的versionCreated字段确定是否创建了新版本
          const versionCreated = res.versionCreated;
          console.log("是否创建新版本:", versionCreated);
          
          if (versionCreated === false) {
            // 配置没有变更，未创建新版本
            this.$message.info("配置未发生变更，未生成新版本");
          } else {
            // 配置有变更，保存成功
            this.$message.success("保存成功，已生成新版本");
          }
          this.getConfigVersion();
          this.saveDialogVisible = false;
          this.configDescription = '';
        } else {
          this.$message.error(res.msg || "保存失败");
        }

      } catch (error) {
        if (error.errorFields) {
          const firstError = error.errorFields[0];
          const fieldPath = firstError.name.join('.');
          this.$message.error(`配置验证失败：[${fieldPath}] ${firstError.errors[0]}`);
        } else {
          this.$message.error(`保存失败: ${error.message || '未知错误'}`);
        }
      }
    },
    changeVersion(val) {
      if (this.currentVersion === val) return;
      this.currentVersion = val;
      
      // 检查是否为当前使用版本
      const versionItem = this.verSionList.find(v => v.version === val);
      if (versionItem) {
        this.isCurrentVersionActive = versionItem.isCurrent;
      }
      
      // 重置筛选状态
      this.searchKeyword = '';
      
      // 折叠所有分组
      Object.keys(this.isGroupExpanded).forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, false);
      });
      
      if (this.compareMode && this.compareVersion) {
        this.loadCompareData();
      } else {
        this.getServiceConfigOption(true);
      }
    },
    changeCasting(val) {
      console.log(val.target.value);
      this.currentId = val.target.value
      // 重置筛选状态
      this.searchKeyword = '';
      // 折叠所有分组
      Object.keys(this.isGroupExpanded).forEach(groupName => {
        this.$set(this.isGroupExpanded, groupName, false);
      });
      this.getConfigVersion()
    },

    //获取角色组
    getServiceRoleType() {
      this.loading = true;
      const params = {
        serviceInstanceId: this.serviceId,
      }
      this.$axiosPost(global.API.getRoleGroupList, params).then((res) => {
        if (res.code !== 200) return  //this.$message.error('获取角色组列表失败')
        this.GroupList = res.data
        if (this.GroupList.length > 0) {
          this.currentId = this.GroupList[0].id;
          //this.getServiceConfigOption( true);
        }
        this.getConfigVersion()
      })
    },
    // 获取服务版本
    getConfigVersion() {
      this.loading = true;
      const params = {
        serviceInstanceId: this.serviceId,
        roleGroupId: JSON.stringify(this.currentId) || '',
      };
      this.$axiosPost(global.API.getConfigVersion, params).then((res) => {
        if (res.code === 200) {
          this.verSionList = res.data;
          if (this.verSionList.length > 0) {
            this.currentVersion = this.verSionList[0].version;
            
            // 标记当前使用的版本
            const currentVersionItem = this.verSionList.find(v => v.isCurrent);
            if (currentVersionItem) {
              this.isCurrentVersionActive = this.currentVersion === currentVersionItem.version;
            }
            
            this.getServiceConfigOption(true);
          }
        }
      });
    },
    // 在获取配置的方法中增加初始化逻辑
    async getServiceConfigOption(loading) {
      if (!loading) this.loading = true;
      const self = this;
      const params = {
        serviceInstanceId: this.serviceId,
        page: 1,
        pageSize: 10000,
        "version": this.currentVersion || '',
        "roleGroupId": JSON.stringify(this.currentId) || '',
      };
      const res = await this.$axiosPost(global.API.getConfigInfo, params);

      if (res.code === 200) {
        this.templateData = this.handlerTemplate(res.data);

        // 初始化所有分组为收起状态
        Object.keys(this.templateData).forEach(name => {
          if (!(name in this.isGroupExpanded)) {
            this.$set(this.isGroupExpanded, name, false);
          } else {
            this.$set(this.isGroupExpanded, name, false);
          }
        });
        
        // 重置筛选状态，确保数据加载后筛选功能正常工作
        this.searchKeyword = '';
        
        // 初始化Kubernetes Tab状态
        this.$nextTick(() => {
          this.updateActiveKubernetesTabs();
        });
      }
      this.loading = false;
    },
    handlerTemplate(data) {
      const finalResult = {};
      const kubernetesConfigsByBaseRole = {}; // { "ZkServer": { "k8s.config.pvc": [...] } }
      const regularConfigsByBaseRole = {};  // { "ZkServer": [...], "General": [...] }

      try {
        if (!data || typeof data !== 'object') {
          console.error('Invalid data passed to handlerTemplate:', data);
          return { General: [] };
        }

        // Phase 1: Collect and categorize configs
        Object.entries(data).forEach(([originalKey, configList]) => {
          const cleanedOriginalKey = originalKey?.trim().replace(/^"|"$/g, '') || 'UnknownGroup';
          
          if (!Array.isArray(configList)) {
            console.error(`ConfigList for key ${cleanedOriginalKey} is not an array:`, configList);
            return; 
          }

          const processedConfigList = configList.map(item => ({
            ...item,
            name: (item.name || '').replaceAll(".", "!")
          }));

          if (cleanedOriginalKey.startsWith('kubernetes.config.')) {
            const parts = cleanedOriginalKey.split('.');
            const baseRoleName = parts[parts.length - 1]; // Assumes format k8s.config.subgroup.Role
            const k8sSubGroupName = parts.slice(0, -1).join('.'); // e.g., kubernetes.config.persistentVolumeClaims
            
            if (!kubernetesConfigsByBaseRole[baseRoleName]) {
              kubernetesConfigsByBaseRole[baseRoleName] = {};
            }
            if (!kubernetesConfigsByBaseRole[baseRoleName][k8sSubGroupName]) {
              kubernetesConfigsByBaseRole[baseRoleName][k8sSubGroupName] = [];
            }
            kubernetesConfigsByBaseRole[baseRoleName][k8sSubGroupName].push(...processedConfigList);
          } else {
            // Regular group or role-specific regular configs
            const baseRoleName = cleanedOriginalKey;
            const configWithTemplate = processedConfigList.find(item => item.templateContent && item.templateContent.trim() !== '');
            if (configWithTemplate) {
              regularConfigsByBaseRole[baseRoleName] = {
                items: processedConfigList,
                displayName: configWithTemplate.displayName || '',
                templateContent: configWithTemplate.templateContent || ''
              };
            } else {
              regularConfigsByBaseRole[baseRoleName] = processedConfigList;
            }
          }
        });

        // Phase 2: Assemble finalResult
        const allBaseRoleNames = new Set([
          ...Object.keys(regularConfigsByBaseRole),
          ...Object.keys(kubernetesConfigsByBaseRole)
        ]);

        allBaseRoleNames.forEach(baseRoleName => {
          const k8sSubGroups = kubernetesConfigsByBaseRole[baseRoleName];
          const regularContent = regularConfigsByBaseRole[baseRoleName];

          if (k8sSubGroups) {
            let regs = [];
            if (regularContent) {
              regs = Array.isArray(regularContent) ? regularContent : (regularContent.items || []);
            }
            finalResult[baseRoleName] = [{
              hasKubernetesConfig: true,
              roleGroupName: baseRoleName,
              regularConfigs: regs,
              kubernetesSubGroups: k8sSubGroups
            }];
          } else if (regularContent) {
            finalResult[baseRoleName] = regularContent; // This is already an array or {items, templateContent}
          } else {
            // Should not happen if keys are from collected data, but as a fallback:
            finalResult[baseRoleName] = []; 
          }
        });

        if (!('General' in finalResult)) {
          finalResult.General = [];
        }

      } catch (error) {
        console.error('Error in handlerTemplate:', error);
        return { General: [] }; // Fallback
      }
      return finalResult;
    },
    // 添加配置组名称转换方法
    convertGroupName(groupName) {

      
      // 处理前缀类型
      if (groupName.startsWith('advanced_')) {
        // 提取配置文件名称
        const configFile = groupName.replace('advanced_', '');
        return `高级 ${configFile}`;
      } else if (groupName.startsWith('custom_')) {
        // 提取配置文件名称
        const configFile = groupName.replace('custom_', '');
        return `自定义 ${configFile}`;
      }
      
      // 默认返回原始名称
      return groupName;
    },
    // 处理保存或恢复操作
    handleSaveOrRestore() {
      if (this.isCurrentVersionActive) {
        // 如果是当前版本，则显示保存对话框
        this.showSaveDialog();
      } else {
        // 如果不是当前版本，则显示恢复对话框
        this.showRestoreDialog(this.currentVersion);
      }
    },
    
    // 显示保存对话框
    showSaveDialog() {
      this.saveDialogVisible = true;
    },
    
    // 确认保存配置
    confirmSave() {
      this.handleSubmit();
    },
    onVersionSearchChange(value) {
      // 检查是否为事件对象，如果是则从event.target.value中提取值
      if (value && typeof value === 'object' && value.target) {
        this.versionSearchKeyword = value.target.value;
      } else {
        this.versionSearchKeyword = value;
      }
    },
    onCompareVersionSearchChange(value) {
      // 检查是否为事件对象，如果是则从event.target.value中提取值
      if (value && typeof value === 'object' && value.target) {
        this.compareVersionSearchKeyword = value.target.value;
      } else {
        this.compareVersionSearchKeyword = value;
      }
    },
    // 获取配置组名称
    getGroupName(id) {
      if (!this.GroupList || this.GroupList.length === 0) {
        return '';
      }
      const group = this.GroupList.find(item => item.id === id);
      return group ? group.roleGroupName : '';
    },
    handleCompareModeChange(value) {
      this.compareMode = value;
      if (!value) {
        // 退出对比模式时清空对比数据
        this.compareData = null;
      }
    },
    closeCompareMode() {
      this.compareMode = false;
      this.compareData = null;
    },
    changeCompareVersion(version) {
      // 添加验证，防止选择与当前版本相同的版本
      if (version === this.currentVersion) {
        this.$message.warning('不能选择相同的版本进行对比');
        return;
      }
      
      if (this.compareVersion === version) return;
      this.compareVersion = version;
      // 检查是否为当前使用版本
      const versionItem = this.verSionList.find(v => v.version === version);
      if (versionItem) {
        this.isCompareVersionActive = versionItem.isCurrent;
      }
      this.loadCompareData();
    },
    // 加载对比数据
    loadCompareData() {
      if (!this.currentVersion || !this.compareVersion) return;
      
      this.loading = true;
      const params = {
        serviceInstanceId: this.serviceId,
        roleGroupId: JSON.stringify(this.currentId) || '',
        versionA: this.currentVersion,
        versionB: this.compareVersion,
        showOnlyDifferences: this.showOnlyDifferences
      };
      
      this.$axiosPost(global.API.configVersionCompare, params).then(res => {
        if (res.code === 200) {
          // 预处理数据，合并同一服务的k8s配置
          this.compareData = this.preprocessCompareGroups(res.data);
          
          // 自动展开所有分组
          Object.keys(this.compareData).forEach(groupName => {
            this.$set(this.isGroupExpanded, groupName, true);
          });
        } else {
          this.$message.error(res.msg || '获取对比数据失败');
        }
        this.loading = false;
      }).catch(error => {
        console.error('对比数据加载失败:', error);
        this.$message.error('对比数据加载失败');
        this.loading = false;
      });
    },
    startCompareWithVersion(version) {
      // 添加验证，防止选择与当前版本相同的版本
      if (version === this.currentVersion) {
        this.$message.warning('不能选择相同的版本进行对比');
        return;
      }
      
      this.compareMode = true;
      this.compareVersion = version;
      
      // 检查是否为当前使用版本
      const versionItem = this.verSionList.find(v => v.version === version);
      if (versionItem) {
        this.isCompareVersionActive = versionItem.isCurrent;
      }
      
      this.loadCompareData();
    },
    handleShowModeChange() {
      // 重新加载对比数据
      this.loadCompareData();
    },
    showRestoreDialog(version) {
      this.restoreDialogVisible = true;
      this.restoreDescription = `从版本 ${version} 创建的恢复版本`;
    },
    confirmRestore() {
      this.loading = true;
      
      // 执行与保存相同的逻辑
      try {
        // 1. 获取所有常规配置组表单实例
        const normalFormRefs = Object.keys(this.nonKubernetesGroups)
            .map(groupName => this.$refs[`template_${groupName}`]?.[0])
            .filter(Boolean);

        // 2. 获取所有Kubernetes配置组表单实例
        const kubernetesFormRefs = [];
        Object.entries(this.kubernetesGroups).forEach(([groupName, group]) => {
          Object.keys(group.subGroups).forEach(subGroupName => {
            const ref = this.$refs[`template_${groupName}_${subGroupName}`]?.[0];
            if (ref) {
              kubernetesFormRefs.push(ref);
            }
          });
        });
        
        // 3. 合并所有表单引用
        const allFormRefs = [...normalFormRefs, ...kubernetesFormRefs];

        // 4. 并行验证所有表单
        Promise.all(
            allFormRefs.map(form =>
                new Promise((resolve, reject) => {
                  form.form.validateFields()
                      .then(values => resolve(values))
                      .catch(error => reject(error))
                })
            )
        ).then(() => {
          // 5. 合并所有配置数据
          const mergedValues = allFormRefs.reduce((acc, form) => {
            return { ...acc, ...form.form.getFieldsValue() };
          }, {});

          // 6. 准备所有配置项
          const allConfigItems = [];
          
          // 添加非kubernetes配置
          Object.values(this.nonKubernetesGroups).forEach(group => {
            if (Array.isArray(group)) {
              allConfigItems.push(...group);
            } else if (group.items && Array.isArray(group.items)) {
              allConfigItems.push(...group.items);
            }
          });
          
          // 添加kubernetes配置
          Object.values(this.kubernetesGroups).forEach(group => {
            Object.values(group.subGroups).forEach(configList => {
              allConfigItems.push(...configList);
            });
          });

          // 7. 更新配置项值
          Object.entries(mergedValues).forEach(([name, value]) => {
            const targetItem = allConfigItems.find(item => item.name === name);
            if (targetItem) {
              targetItem.value = value;
            }
          });

          // 8. 处理配置项名称
          const processedItems = allConfigItems.map(item => ({
            ...item,
            name: item.name.replaceAll("!", ".")
          }));

          // 9. 过滤不需要的配置项
          const filterParam = processedItems.filter(
              item => !(!item.required && item.hidden)
          );

          // 10. 获取当前用户信息
          const userStr = localStorage.getItem(process.env.VUE_APP_USER_KEY);
          const currentUser = userStr ? JSON.parse(userStr) : {};

          // 11. 构建保存参数
          const saveParam = {
            clusterId: this.clusterId,
            serviceName: this.serviceName,
            serviceConfig: JSON.stringify(filterParam),
            roleGroupId: this.currentId,
            description: this.restoreDescription,
            userId: currentUser.id,
            username: currentUser.username
          };

          // 12. 提交保存
          this.$axiosPost(global.API.saveServiceConfig, saveParam).then(res => {
            if (res.code === 200) {
              this.$message.success("恢复成功");
              this.getConfigVersion();
              this.restoreDialogVisible = false;
              this.restoreDescription = '';
            } else {
              this.$message.error(res.msg || "恢复失败");
            }
            this.loading = false;
          }).catch(error => {
            console.error('恢复失败:', error);
            this.$message.error('恢复失败');
            this.loading = false;
          });
        }).catch(error => {
          if (error.errorFields) {
            const firstError = error.errorFields[0];
            const fieldPath = firstError.name.join('.');
            this.$message.error(`配置验证失败：[${fieldPath}] ${firstError.errors[0]}`);
          } else {
            this.$message.error(`恢复失败: ${error.message || '未知错误'}`);
          }
          this.loading = false;
        });
      } catch (error) {
        this.$message.error(`恢复失败: ${error.message || '未知错误'}`);
        this.loading = false;
      }
    },
    // 添加loadData方法，供父组件调用
    loadData() {
      console.log('ServiceConfig loadData 被调用，加载配置参数数据');
      return this.getServiceRoleType();
    },
    // 更新activeKubernetesTabs, 确保所有角色组都有活动的Tab
    updateActiveKubernetesTabs() {
      const kubernetesAwareGroups = this.kubernetesGroups || {}; // Uses the computed property
      
      Object.entries(kubernetesAwareGroups).forEach(([roleName, groupArray]) => {
        // groupArray is expected to be like [{ hasKubernetesConfig: true, kubernetesSubGroups: {...} }]
        if (Array.isArray(groupArray) && groupArray.length === 1 && groupArray[0].hasKubernetesConfig) {
          const k8sData = groupArray[0];
          if (k8sData.kubernetesSubGroups) {
            const subGroupNames = Object.keys(k8sData.kubernetesSubGroups);
            // Set the first subGroup as active if no tab is currently active for this roleName
            if (!this.activeKubernetesTabs[roleName] && subGroupNames.length > 0) {
              this.$set(this.activeKubernetesTabs, roleName, subGroupNames[0]);
            }
          }
        }
      });
    },
    // Correctly placed and updated formatSubGroupName
    formatSubGroupName(subGroupName) {
      // 预期 subGroupName 格式为 "kubernetes.config.actualSubGroupName"
      // 或直接是 "actualSubGroupName" (如果已预处理)
      let actualSubGroupName = subGroupName;
      if (subGroupName && subGroupName.startsWith('kubernetes.config.')) {
        const parts = subGroupName.split('.');
        actualSubGroupName = parts[parts.length - 1];
      }

      // 将驼峰式或帕斯卡式的英文名转换为空格分隔的标题式英文名
      let readableEnglishName = actualSubGroupName
        .replace(/([A-Z])/g, " $1") // 在大写字母前添加空格
        .replace(/^./, (str) => str.toUpperCase()) // 首字母大写
        .trim();
      if (!readableEnglishName && actualSubGroupName) readableEnglishName = actualSubGroupName; 
      else if (!readableEnglishName && !actualSubGroupName) readableEnglishName = 'Unknown';

      const chineseName = this.k8sSubGroupChineseNames[actualSubGroupName];

      let displayText;
      if (chineseName) {
        displayText = chineseName;
      } else {
        // 如果没有特定的中文翻译，使用处理后的英文名作为主要的"中文"部分
        displayText = readableEnglishName; 
      }
      // 返回HTML字符串，其中英文部分用特定class包裹
      return `${displayText} <span class="k8s-subgroup-en">(${readableEnglishName})</span>`;
    },
    isKubernetesConfig(groupName) {
      return groupName && groupName.startsWith('kubernetes.config.');
    },
    formatK8sGroupTitle(groupName) {
      if (!groupName || !groupName.startsWith('kubernetes.config.')) {
        return groupName;
      }
      
      const parts = groupName.split('.');
      if (parts.length < 4) {
        return groupName;
      }
      
      // 提取角色名（服务名），如ZkServer
      const roleName = parts[3]; 
      
      // 返回简化的标题，只按服务分组
      return `${roleName}的Kubernetes配置`;
    },
    // 添加辅助方法，用于对比时预处理分组
    preprocessCompareGroups(data) {
      if (!data) return {};
      
      const result = {};
      const roleBasedGroups = {}; // 按服务名（角色）分组
      
      // 第一步：收集所有k8s配置，按服务名分组
      Object.entries(data).forEach(([groupName, items]) => {
        if (this.isKubernetesConfig(groupName)) {
          const parts = groupName.split('.');
          if (parts.length >= 4) {
            const configType = parts[2]; // 配置类型，如persistentVolumeClaims
            const roleName = parts[3]; // 提取服务名，如ZkServer
            
            if (!roleBasedGroups[roleName]) {
              roleBasedGroups[roleName] = {};
            }
            
            if (!roleBasedGroups[roleName][configType]) {
              roleBasedGroups[roleName][configType] = [];
            }
            
            // 为每个配置项添加类型标记
            const itemsWithType = items.map(item => ({
              ...item,
              k8sConfigType: configType,
              k8sConfigTypeLabel: this.k8sSubGroupChineseNames[configType] || configType
            }));
            
            roleBasedGroups[roleName][configType].push(...itemsWithType);
          } else {
            // 如果解析失败，保持原样
            result[groupName] = items;
          }
        } else {
          // 非k8s配置保持原样
          result[groupName] = items;
        }
      });
      
      // 第二步：将收集的k8s配置添加到结果中，并添加分隔符项
      Object.entries(roleBasedGroups).forEach(([roleName, configTypes]) => {
        const newGroupName = `kubernetes.config.combined.${roleName}`;
        const combinedItems = [];
        
        // 按配置类型添加配置项，并在不同类型间添加分隔标记
        Object.entries(configTypes).forEach(([configType, items], index) => {
          // 格式化英文配置类型名称
          const readableEnglishName = configType
            .replace(/([A-Z])/g, " $1") // 在大写字母前添加空格
            .replace(/^./, (str) => str.toUpperCase()) // 首字母大写
            .trim();
            
          // 获取中文显示名称
          const chineseName = this.k8sSubGroupChineseNames[configType] || readableEnglishName;
          
          // 组合显示标签
          const dividerLabel = `${chineseName} <span class="k8s-subgroup-en">(${readableEnglishName})</span>`;
          
          // 如果不是第一个配置类型，添加分隔符
          if (index > 0) {
            combinedItems.push({
              name: `k8s-divider-${configType}`,
              isDivider: true,
              dividerLabel,
              [this.currentVersion]: '',
              [this.compareVersion]: '',
            });
          } else {
            // 第一个配置类型也添加标签，但不显示分隔线
            combinedItems.push({
              name: `k8s-divider-${configType}`,
              isFirstDivider: true,
              dividerLabel,
              [this.currentVersion]: '',
              [this.compareVersion]: '',
            });
          }
          
          // 添加该类型的所有配置项
          combinedItems.push(...items);
        });
        
        result[newGroupName] = combinedItems;
      });
      
      return result;
    },
  },
  created() {
    console.log('ServiceConfig 创建, serviceId:', this.serviceId);
    console.log('serviceName:', this.serviceName);
  }
};
</script>
<style lang="less" scoped>
.service-config-container {
  position: relative;
  padding: 0;
  height: 100%;
  overflow-y: auto;
  background-color: #fff;
}

/* 过滤器栏样式 */
.filter-bar {
  display: flex;
  gap: 12px;
  margin-bottom: 24px;
  padding: 10px 0;
  background-color: #fff;
  
  .filter-item {
    display: flex;
    align-items: center;
    
    .filter-label {
      margin-right: 8px;
      color: rgba(0, 0, 0, 0.85);
      font-size: 14px;
      min-width: 42px; /* 确保所有标签宽度一致 */
    }
  }
  
  .filter-select {
    /deep/ .ant-select-selection {
      background-color: #fff;
      border: 1px solid #d9d9d9;
      border-radius: 2px;
      height: 32px;
      
      &:hover {
        border-color: #40a9ff;
      }
      
      .ant-select-selection__rendered {
        line-height: 30px;
      }
    }
    
    /* 添加搜索框样式 */
    /deep/ .ant-select-search {
      width: 100%;
      
      .ant-select-search__field {
        padding: 4px 11px;
        width: 100% !important;
      }
    }
    
    /* 输入框样式 */
    /deep/ .ant-input {
      height: 32px;
      border-radius: 2px;
      
      &:hover {
        border-color: #40a9ff;
      }
      
      &:focus {
        border-color: #40a9ff;
        box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.2);
      }
    }
    
    /* 搜索按钮样式 */
    /deep/ .ant-input-search-button {
      height: 32px;
      border-radius: 0 2px 2px 0;
    }
  }
}

/* 版本对比相关样式 */
.compare-switch {
  margin-right: 8px;
}

.version-compare-container {
  width: 100%;
  margin-bottom: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.version-compare-row {
  display: flex;
  align-items: center;
  background-color: #f5f7fa;
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
}

.compare-header-section {
  display: flex;
  align-items: center;
  gap: 10px;
  flex-wrap: wrap;
}

.filter-controls {
  padding: 12px 16px;
  background-color: #fff;
  display: flex;
  flex-direction: row;
  align-items: center;
  gap: 16px;
}

.filter-control-item {
  display: flex;
  align-items: center;
}

.filter-label {
  width: 70px;
  color: rgba(0, 0, 0, 0.85);
  font-size: 14px;
  text-align: right;
}

.compare-text {
  color: rgba(0, 0, 0, 0.65);
  white-space: nowrap;
  font-weight: 500;
}

.close-compare-btn {
  margin-left: auto;
  color: rgba(0, 0, 0, 0.45);
  font-size: 16px;
  
  &:hover {
    color: rgba(0, 0, 0, 0.65);
  }
}

/* Kubernetes配置区域样式 */
.kubernetes-section {
  background-color: #f9fafc;
  border-radius: 8px;
  border: 1px solid #e8eaf1;
  margin-bottom: 24px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.kubernetes-tabs-header {
  font-size: 16px;
  font-weight: bold;
  color: #1890ff;
  margin-bottom: 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #e8eaf1;
  display: flex;
  align-items: center;
}

.kubernetes-tabs-header::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 16px;
  background-color: #1890ff;
  margin-right: 8px;
  border-radius: 2px;
}

/* 标签页样式优化 */
/deep/ .ant-tabs-nav .ant-tabs-tab {
  padding: 12px 16px;
  transition: all 0.3s;
}

/deep/ .ant-tabs-nav .ant-tabs-tab-active {
  background-color: #e6f7ff;
  border-radius: 4px 4px 0 0;
}

/* 美化端口映射配置区域 */
/deep/ .ant-form-item-label label[title="Kubernetes NodePort端口映射"] {
  font-weight: bold;
  color: #1890ff;
  font-size: 14px;
}

/deep/ .ant-form-item-children input[placeholder*="containerPort"] {
  border-color: #1890ff;
  border-radius: 4px;
}

/* 添加端口映射按钮美化 */
/deep/ .ant-btn-dashed.ant-btn-sm {
  border-color: #1890ff;
  color: #1890ff;
}

/deep/ .ant-btn-dashed.ant-btn-sm:hover {
  border-color: #40a9ff;
  color: #40a9ff;
}

/* 对比模式表头 */
.compare-header {
  display: flex;
  padding: 16px;
  background-color: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  font-weight: 500;
}

.compare-header-item {
  &.attribute {
    flex: 2;
  }
  
  &:not(.attribute) {
    flex: 1;
    text-align: center;
  }
}

.version-header {
  display: flex;
  flex-direction: column;
  align-items: center;
  
  .version-title {
    font-weight: 500;
    margin-bottom: 4px;
  }
}

/* 对比内容样式 */
.compare-header-panel {
  margin-top: 0;
  border-top: none;
  margin-bottom: 16px;
}

.compare-table {
  width: 100%;
  border-collapse: collapse;
  table-layout: fixed;
  
  .compare-header-row {
    background-color: #fafafa;
    border-bottom: 1px solid #e8e8e8;
    
    .compare-header-cell {
      padding: 16px;
      font-weight: 500;
      text-align: left;
      
      &.attribute-header {
        width: 30%;
      }
    }
  }
  
  .compare-row-simple {
    border-bottom: 1px solid #f0f0f0;
    
    &:nth-child(even) {
      background-color: #fafafa;
    }
    
    &:hover {
      background-color: #f5f5f5;
    }
    
    &.different {
      background-color: #fffbe6;
      
      &:hover {
        background-color: #fff8d8;
      }
    }
  }
  
  .compare-cell-simple {
    padding: 12px 16px;
    vertical-align: top;
    word-break: break-all;
    
    &.attribute-cell {
      width: 30%;
      font-weight: 500;
    }
    
    &.value-cell {
      width: 35%;
    }
  }
}

/* 悬浮提示样式 */
/deep/ .filter-tooltip {
  .filter-tooltip-title {
    font-weight: 500;
    margin-bottom: 4px;
  }
  
  .filter-tooltip-content {
    color: rgba(255, 255, 255, 0.85);
    font-size: 12px;
  }
}

/* 配置面板样式 - 扁平化设计 */
.config-area {
  background-color: #fff;
  
  .config-panel {
    border-top: 1px solid #e8e8e8;
    background-color: #fff;
    margin-bottom: 10px;
    
    &:last-child {
      border-bottom: 1px solid #e8e8e8;
    }
    
    .panel-header {
      padding: 16px;
      background-color: #fff;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      font-weight: normal;
      color: rgba(0, 0, 0, 0.85);
      transition: all 0.3s;
      
      &:hover {
        background-color: #fafafa;
      }
      
      .toggle-icon {
        color: rgba(0, 0, 0, 0.45);
        font-size: 12px;
        transition: transform 0.2s;
      }
    }
    
    .panel-content {
      padding: 16px;
      background-color: #f5f7fa;
      border-top: 1px solid #e8e8e8;
      
      /deep/ .ant-form-item {
        margin-bottom: 14px;
        
        &:last-child {
          margin-bottom: 0;
        }
      }
    }
  }
}

/* 底部按钮区域 */
.footer {
  margin-top: 20px;
  padding: 16px 0;
  text-align: right;
  
  button {
    min-width: 80px;
    height: 32px;
    font-size: 14px;
  }
}

.placeholder-hint {
  font-size: 12px;
  color: #999;
  margin-top: 4px;
}

/* 版本列表样式 */
.version-dropdown {
  width: 500px;
  
  .ant-dropdown-menu {
    padding: 0;
  }
}

.version-list-container {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 500px;
}

.version-list-header {
  padding: 8px 12px;
  border-bottom: 1px solid #f0f0f0;
}

.version-list {
  max-height: 400px;
  overflow-y: auto;
}

.version-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  display: flex;
  justify-content: space-between;
  align-items: center;
  
  &:hover {
    background-color: #f5f5f5;
  }
  
  &-active {
    background-color: #e6f7ff;
  }
  
  &-content {
    flex: 1;
    cursor: pointer;
  }
  
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 8px;
  }
  
  &-description {
    color: rgba(0, 0, 0, 0.65);
    margin-bottom: 8px;
    word-break: break-all;
  }
  
  &-footer {
    display: flex;
    justify-content: space-between;
    font-size: 12px;
    color: rgba(0, 0, 0, 0.45);
  }
}

.version-number {
  font-weight: 700;
  font-size: 16px;
  color: #1890ff;
}

.version-tag {
  background-color: #52c41a;
  color: #fff;
  padding: 2px 8px;
  border-radius: 2px;
  font-size: 12px;
}

.version-selected-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #1890ff;
  font-size: 16px;
}

.version-compare-btn {
  width: 32px;
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  cursor: pointer;
  margin-left: 8px;
  
  &:hover {
    color: #1890ff;
    border-color: #1890ff;
  }
}

/* 版本操作按钮样式 */
.version-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.version-action-btn {
  width: 32px;
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  border: 1px solid #d9d9d9;
  border-radius: 2px;
  cursor: pointer;
  
  &:hover {
    color: #1890ff;
    border-color: #1890ff;
  }
}

.version-restore-btn {
  &:hover {
    color: #52c41a;
    border-color: #52c41a;
  }
}

/* 添加保存按钮样式 */
.save-button {
  background-color: #fff;
  color: #1890ff;
  border: 1px solid #1890ff;
  font-size: 14px;
  height: 32px;
  padding: 0 15px;
  
  &:hover, &:focus {
    background-color: #fff;
    color: #40a9ff;
    border-color: #40a9ff;
  }
}

/* 配置组下拉列表样式 */
.config-group-dropdown {
  width: 300px;
  
  .ant-dropdown-menu {
    padding: 0;
  }
}

/* 下拉标题样式 */
.dropdown-title {
  color: #1890ff;
  font-weight: bold;
  background-color: #e6f7ff;
  padding: 2px 6px;
  border-radius: 3px;
  margin-right: 4px;
}

.config-group-list-container {
  background-color: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
  width: 300px;
}

.config-group-list {
  max-height: 300px;
  overflow-y: auto;
}

.config-group-item {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  cursor: pointer;
  position: relative;
  
  &:hover {
    background-color: #f5f5f5;
  }
  
  &-active {
    background-color: #e6f7ff;
  }
  
  &-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
  }
}

.config-group-name {
  font-weight: 500;
  font-size: 14px;
}

.config-group-selected-icon {
  position: absolute;
  right: 16px;
  top: 50%;
  transform: translateY(-50%);
  color: #1890ff;
  font-size: 16px;
}

/* 模板内容显示相关样式 */
.template-content-container {
  margin-top: 16px;
  padding: 12px;
  background-color: #f9f9f9;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.template-content-title {
  font-weight: 500;
  margin-bottom: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}

.template-content-textarea {
  width: 100%;
  background-color: #f5f5f5;
  color: rgba(0, 0, 0, 0.65);
  font-family: 'SFMono-Regular', Consolas, 'Liberation Mono', Menlo, Courier, monospace;
  font-size: 13px;
  line-height: 1.5;
  border-color: #d9d9d9;
}

/* 错误消息样式 */
.config-error-message {
  padding: 12px;
  background-color: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 4px;
  color: #f5222d;
  font-size: 14px;
  line-height: 1.5;
  text-align: center;
  margin: 8px 0;
}

/* Kubernetes配置相关样式 */
.kubernetes-config-panel {
  margin-top: 16px;
  padding: 12px;
  background-color: #fff;
  border-radius: 4px;
  border: 1px solid #e8e8e8;
}

.kubernetes-tabs-header {
  font-weight: 500;
  margin-bottom: 8px;
  font-size: 14px;
  color: rgba(0, 0, 0, 0.85);
}

.kubernetes-tabs {
  margin-top: 16px;
}

.kubernetes-tabs .ant-tabs-nav {
  margin-bottom: 0;
}

.kubernetes-tabs .ant-tabs-tab {
  padding: 8px 16px;
}

.kubernetes-section {
  margin-top: 20px;
  border-top: 1px solid #e8e8e8;
  padding-top: 15px;
}

.kubernetes-tabs-header {
  font-weight: bold;
  margin-bottom: 10px;
  font-size: 14px;
  color: #1890ff;
}

.kubernetes-tabs {
  margin-bottom: 15px;
}

.config-panel {
  margin-bottom: 20px;
  border: 1px solid #e8e8e8;
  border-radius: 4px;
  overflow: hidden;
}

.panel-header {
  padding: 10px 15px;
  background-color: #f9f9f9;
  font-weight: bold;
  cursor: pointer;
  display: flex;
  justify-content: space-between;
  align-items: center;
}

.panel-content {
  padding: 15px;
}

.toggle-icon {
  font-size: 12px;
}

.k8s-subgroup-en {
  font-size: 0.8em;
  color: #999;
}

/* Kubernetes子组Tab英文名样式 (类似 step7.vue) */
/deep/ .k8s-subgroup-en {
  color: #E6A23C; /* 浅橙色 */
  font-size: 0.9em;   /* 辅助字体稍小 */
  font-weight: normal; /* 非粗体 */
  margin-left: 4px;    /* 括号前的空格 */
}

/* 分隔符样式 */
.k8s-config-divider-cell {
  text-align: left;
  padding: 10px 16px;
  background-color: #f5f7fa;
}

/* 分隔符标签样式 */
.k8s-divider-label {
  padding: 4px 0;
  font-weight: 500;
  color: #1890ff;
  font-size: 14px;
  display: flex;
  align-items: center;
}

.k8s-divider-label::before {
  content: '';
  display: inline-block;
  width: 4px;
  height: 14px;
  background-color: #1890ff;
  margin-right: 8px;
  border-radius: 2px;
}

/* 分隔符样式 */
.k8s-config-divider {
  background-color: transparent;
  border-top: 1px dashed #d9d9d9;
}

/* 第一个配置类型标题样式 */
.k8s-config-first-divider {
  background-color: transparent;
}
</style> 