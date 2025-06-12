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


 * @describe: step6-服务配置
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-10-31 16:00:46
 * @FilePath: \ddh-ui\src\components\steps\step7.vue
-->
<template>
  <div class="steps7 steps" style="width: 100%;">
    <div class="service-config-container" style="width: 100%;">
      <div class="steps-title flex-bewteen-container" style="margin-top: -5px; margin-bottom: 15px; display: flex; align-items: center; justify-content: space-between; padding: 2px 0;">
        <div style="display: flex; align-items: center;">
          <span>配置参数</span>
          <a-button 
            size="small" 
            style="margin-left: 8px;"
            @click="toggleAllGroups"
          >
            <a-icon :type="isAllExpanded ? 'shrink' : 'arrows-alt'" style="font-size: 14px;" />
          </a-button>
        </div>
        <a-button 
          class="btn-save" 
          type="primary" 
          @click="handleSubmit" 
          style="display: flex; align-items: center; justify-content: center; height: 32px; line-height: 1; margin-top: 4px;"
        >
          保存
        </a-button>
      </div>
      
      <a-spin :spinning="loading" class="content-spin" style="margin-top: 5px;">
        <div class="main-content-area" style="margin-top: 5px;">
          <a-tabs v-model="serviceNameKey" @change="callback">
            <a-tab-pane v-for="item in SERVICENAMES" :key="item" :tab="item" :forceRender="true">
              <!-- 标签内容由下面的区域控制 -->
            </a-tab-pane>
          </a-tabs>
          
          <!-- 使用Ant Design的Collapse组件重构配置区域 -->
          <div class="content-wrapper">
            <div
              v-for="item in SERVICENAMES"
              :key="item"
              :class="['config-area', serviceNameKey === item ? '' : 'hidden']"
            >
              <div v-if="serviceNameKey === item" class="config-area-inner">
                <!-- 仅显示通用的Kubernetes配置（不属于特定角色的） -->
                <div v-if="false && kubernetesGroups[item] && Object.keys(kubernetesGroups[item]).length > 0" class="kubernetes-config-section">
                  <div class="kubernetes-tabs-header">Kubernetes 配置</div>
                  <a-tabs v-model="activeKubernetesTabs[item]">
                    <a-tab-pane 
                      v-for="(subGroup, subGroupName) in kubernetesGroups[item]"
                      :key="subGroupName"
                    >
                      <template slot="tab">
                        <span v-html="formatSubGroupName(subGroupName)"></span>
                      </template>
                      <FixedCommonTemplate
                        :ref="`CommonTemplateRef_${item}_Kubernetes_${subGroupName}`"
                        :steps4Data="steps4Data"
                        :templateData="subGroup.items"
                      />
                      
                      <!-- 添加模板内容显示框 -->
                      <div v-if="subGroup.templateContent" class="template-content-container">
                        <div class="template-content-title">{{ subGroup.displayName || '模板内容' }}:</div>
                        <a-textarea
                          :value="subGroup.templateContent"
                          :auto-size="{ minRows: 3, maxRows: 10 }"
                          read-only
                          class="template-content-textarea"
                        />
                      </div>
                    </a-tab-pane>
                  </a-tabs>
                </div>
                
                <!-- 非Kubernetes配置组 -->
                <a-collapse 
                  v-for="(group, groupName) in nonKubernetesGroups[item]"
                  :key="groupName"
                  :bordered="false"
                  expandIconPosition="right"
                  :activeKey="expandedKeys[item] || getActiveKeys(item)"
                  class="config-collapse"
                  @change="onCollapseChange($event, item)"
                >
                  <a-collapse-panel 
                    :key="groupName"
                    :showArrow="true"
                    :forceRender="true"
                    :class="['config-panel', isLastGroup(item, groupName) ? 'last-group' : '']"
                  >
                    <template slot="header">
                      <span class="panel-header-text">{{ convertGroupName(groupName) }}</span>
                    </template>
                    
                    <div class="panel-content">
                      <!-- 如果该组有Kubernetes配置，显示其Kubernetes配置在顶部 -->
                      <div v-if="group.hasKubernetesConfig && group.kubernetesSubGroups && Object.keys(group.kubernetesSubGroups).length > 0" 
                           class="group-kubernetes-section">
                        <div class="kubernetes-tabs-header">Kubernetes 配置</div>
                        <a-tabs v-model="activeKubernetesTabs[`${item}_${groupName}`]">
                          <a-tab-pane 
                            v-for="(subGroup, subGroupName) in group.kubernetesSubGroups"
                            :key="subGroupName"
                          >
                            <template slot="tab">
                              <span v-html="formatSubGroupName(subGroupName)"></span>
                            </template>
                            <FixedCommonTemplate
                              :ref="`CommonTemplateRef_${item}_${groupName}_${subGroupName}`"
                              :steps4Data="steps4Data"
                              :templateData="subGroup.items"
                            />
                            
                            <!-- 添加模板内容显示框 -->
                            <div v-if="subGroup.templateContent" class="template-content-container">
                              <div class="template-content-title">{{ subGroup.displayName || '模板内容' }}:</div>
                              <a-textarea
                                :value="subGroup.templateContent"
                                :auto-size="{ minRows: 3, maxRows: 10 }"
                                read-only
                                class="template-content-textarea"
                              />
                            </div>
                          </a-tab-pane>
                        </a-tabs>
                      </div>
                      
                      <!-- 常规配置表单 -->
                      <FixedCommonTemplate
                        :ref="`CommonTemplateRef_${item}_${groupName}`"
                        :steps4Data="steps4Data"
                        :templateData="group.items"
                      />
                      
                      <!-- 添加模板内容显示框 -->
                      <div v-if="group.templateContent" class="template-content-container">
                        <div class="template-content-title">{{ group.displayName || '模板内容' }}:</div>
                        <a-textarea
                          :value="group.templateContent"
                          :auto-size="{ minRows: 3, maxRows: 10 }"
                          read-only
                          class="template-content-textarea"
                        />
                      </div>
                    </div>
                  </a-collapse-panel>
                </a-collapse>
                
                <!-- 增加底部空白区域，确保最后一个分组完整显示 -->
                <div class="bottom-spacer"></div>
              </div>
            </div>
          </div>
        </div>
      </a-spin>
    </div>
  </div>
</template>
<script>
import FixedCommonTemplate from "@/components/steps/FixedCommonTemplate.vue";
import {mapActions, mapState} from "vuex";
import {de} from "date-fns/locale";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  components: {
    FixedCommonTemplate
  },
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      loading: false,
      templateData: [],
      // "ZOOKEEPER": [], "HDFS": [], "YARN": []
      templateObj: {},
      saveData: [],
      hostList: [],
      serviceNameKey: "",
      SERVICENAMES: [],
      groupedTemplateData: {},  // 按服务分组的配置数据 { service1: { group1: [], group2: [] }, ... }
      isGroupExpanded: {},      // 分组展开状态 { service1: { group1: true, group2: false }, ... }
      selectKeys: [],
      activeKubernetesTabs: {}, // 存储每个服务的Kubernetes Tab激活状态
      k8sSubGroupChineseNames: {
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
        'mount_path': '挂载路径',
        'storage': '存储容量'
      },
      // 存储每个服务的配置组展开状态
      expandedKeys: {},
      isAllExpanded: true,
    };
  },
  watch: {
    serviceContainerHeight(val) {
      console.log(val);
    },
  },
  computed: {
    ...mapState({
      steps: (state) => state.steps, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
    serviceContainerHeight() {
      const className = this.serviceNameKey + "warp";
      const height = document.getElementsByClassName(className)[0];
      return height;
    },
    // 分离Kubernetes和非Kubernetes分组
    nonKubernetesGroups() {
      const result = {};
      
      // 遍历每个服务的配置组
      Object.entries(this.groupedTemplateData).forEach(([serviceName, groups]) => {
        if (!result[serviceName]) {
          result[serviceName] = {};
        }
        
        // 处理常规分组（不是特殊的Kubernetes分组）
        Object.entries(groups).forEach(([groupName, group]) => {
          if (groupName !== 'Kubernetes') { // 排除专用的Kubernetes分组
            result[serviceName][groupName] = group;
          }
        });
      });
      
      return result;
    },
    
    // 获取每个服务的通用Kubernetes配置组
    kubernetesGroups() {
      const result = {};
      
      // 遍历每个服务的配置组
      Object.entries(this.groupedTemplateData).forEach(([serviceName, groups]) => {
        // 初始化该服务的Kubernetes配置结果对象
        result[serviceName] = {};
        
        // 遍历所有角色分组，收集它们的Kubernetes配置
        Object.entries(groups).forEach(([groupName, group]) => {
          // 如果角色组有Kubernetes配置，则处理
          if (group && group.hasKubernetesConfig && group.kubernetesSubGroups) {
            
            // 为每个子组添加roleGroup信息，并将其添加到结果中
            Object.entries(group.kubernetesSubGroups).forEach(([subGroupName, subGroup]) => {
              // 确保subGroup有items
              if (subGroup && subGroup.items && subGroup.items.length > 0) {
                // 添加roleGroup信息
                const enrichedSubGroup = {
                  ...subGroup,
                  roleGroup: groupName
                };
                
                // 将该子组添加到结果中
                result[serviceName][subGroupName] = enrichedSubGroup;
              }
            });
          }
        });
      });
      
      return result;
    },
    
    // 判断每个服务是否有通用Kubernetes配置
    hasKubernetesGroups() {
      const result = {};
      
      // 遍历每个服务的配置组
      Object.entries(this.groupedTemplateData).forEach(([serviceName, groups]) => {
        if (groups['Kubernetes'] && groups['Kubernetes'].subGroups) {
          result[serviceName] = Object.keys(groups['Kubernetes'].subGroups).length > 0;
        } else {
          result[serviceName] = false;
        }
      });
      
      return result;
    }
  },
  methods: {
    // 判断是否为最后一个分组
    isLastGroup(serviceName, groupName) {
      const groups = this.groupedTemplateData[serviceName] || {};
      const groupKeys = Object.keys(groups);
      return groupKeys[groupKeys.length - 1] === groupName;
    },
    // 获取当前服务的初始展开面板key
    getActiveKeys(serviceName) {
      if (!this.groupedTemplateData[serviceName]) return [];
      // 默认全部展开
      return Object.keys(this.groupedTemplateData[serviceName]);
    },
    templateProps(item) {
      return this.templateObj[item];
    },
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    callback(key) {
      this.serviceNameKey = key;
      if (this.selectKeys.includes(key)) return false;
      this.selectKeys.push(key);
      // 切换标签页时获取配置
      this.getServiceConfigOption();
    },
    // 去除字符串里面的数字
    deleteNum(str, key) {
      let reg = /[0-9]+/g;
      let str1 = str.replace(reg, "");
      let str2 = str1.replace(key, "");
      return str2;
    },
    convertGroupName(groupName) {
      if (groupName && typeof groupName === 'string') {
        if (groupName.startsWith('advanced_')) {
          return '高级 ' + groupName.substring('advanced_'.length);
        } else if (groupName.startsWith('custom_')) {
          return '自定义 ' + groupName.substring('custom_'.length);
        }
      }
      // 默认返回原始名称或稍作处理（例如，如果它是角色名）
      // 这里可以根据需要添加更多针对特定角色名的转换逻辑
      // 例如：if (groupName === 'ZkServer') return 'ZooKeeper 服务器';
      return groupName;
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
        const currentService = this.serviceNameKey;
        const allFormData = {};
        // 1. 收集所有分组表单数据（新版结构）
        if (this.groupedTemplateData[currentService]) {
          for (const groupName of Object.keys(this.groupedTemplateData[currentService])) {
            const group = this.groupedTemplateData[currentService][groupName];
            
            // 处理常规配置表单
            const refName = `CommonTemplateRef_${currentService}_${groupName}`;
            const formRef = this.$refs[refName]?.[0];
            if (formRef) {
              await formRef.form.validateFields();
              // 获取表单值，同时过滤掉slider相关的辅助表单项
              const formValues = formRef.form.getFieldsValue();
              const filteredValues = {};
              for (const key in formValues) {
                // 排除slider辅助输入框的值
                if (!key.endsWith('_value')) {
                  filteredValues[key] = formValues[key];
                }
              }
              Object.assign(allFormData, filteredValues);
            }
            
            // 处理该分组内的Kubernetes配置
            if (group.hasKubernetesConfig && group.kubernetesSubGroups) {
              for (const subGroupName of Object.keys(group.kubernetesSubGroups)) {
                const k8sRefName = `CommonTemplateRef_${currentService}_${groupName}_${subGroupName}`;
                const k8sFormRef = this.$refs[k8sRefName]?.[0];
                if (k8sFormRef) {
                  await k8sFormRef.form.validateFields();
                  // 获取表单值并过滤
                  const k8sFormValues = k8sFormRef.form.getFieldsValue();
                  const filteredK8sValues = {};
                  for (const key in k8sFormValues) {
                    if (!key.endsWith('_value')) {
                      filteredK8sValues[key] = k8sFormValues[key];
                    }
                  }
                  Object.assign(allFormData, filteredK8sValues);
                }
              }
            }
          }
          
          // 处理通用Kubernetes配置组
          if (this.kubernetesGroups[currentService]) {
            for (const subGroupName of Object.keys(this.kubernetesGroups[currentService])) {
              const k8sRefName = `CommonTemplateRef_${currentService}_Kubernetes_${subGroupName}`;
              const k8sFormRef = this.$refs[k8sRefName]?.[0];
              if (k8sFormRef) {
                await k8sFormRef.form.validateFields();
                // 获取表单值并过滤
                const k8sFormValues = k8sFormRef.form.getFieldsValue();
                const filteredK8sValues = {};
                for (const key in k8sFormValues) {
                  if (!key.endsWith('_value')) {
                    filteredK8sValues[key] = k8sFormValues[key];
                  }
                }
                Object.assign(allFormData, filteredK8sValues);
              }
            }
          }
        }
        
        // 2. 处理复合数据结构
        const mergedData = {
          ...allFormData,
          ...this.handlearrayWithData(allFormData),
          ...this.handleMultipleData(allFormData)
        };
        
        // 3. 安全更新配置项
        const param = (this.templateObj[currentService] || []).map(item => {
          if (item?.name) {
            const formKey = item.name.replace(/\./g, "!"); // 使用正则全局替换
            return {
              ...item,
              value: mergedData[formKey] ?? item.value
            };
          }
          return item;
        });
        
        // 4. 过滤有效参数
        let filterParam = param.filter(
            (item) => !(!item.required && item.hidden)
        );
        
        // 5. 提交保存
        const saveParam = {
          clusterId: this.setting.clusterId || this.clusterId,
          serviceName: currentService,
          serviceConfig: JSON.stringify(filterParam),
        };

        const res = await this.$axiosPost(global.API.saveServiceConfig, saveParam);
        if (res.code === 200) {
          this.$message.success("配置保存成功");
          await this.getServiceConfigOption();
          return true;
        }
        return false;
      } catch (error) {
        console.error('保存失败:', error);
        this.$notification.error({
          message: '保存失败',
          description: error.response?.data?.msg || error.message,
          duration: 4
        });
        return false;
      }
    },
    async getServiceConfigOption() {
      if (!this.SERVICENAMES || this.SERVICENAMES.length === 0) {
        this.$message.warning("未选择任何服务，无法获取配置");
        this.loading = false;
        return;
      }

      this.loading = true;
      
      // 获取当前选中服务的配置
      const currentService = this.serviceNameKey;
      if (!currentService) {
        this.$message.warning("当前未选择服务");
        this.loading = false;
        return;
      }

      try {
        const params = {
          clusterId: this.setting.clusterId || this.clusterId,
          serviceName: currentService,
        };

        const res = await this.$axiosPost(global.API.getServiceConfigOption, params);
        
        if (res.code === 200) {
          // 处理配置数据
          const processedGroups = this.handlerTemplate(currentService, res.data || {});
          this.$set(this.groupedTemplateData, currentService, processedGroups);
          
          // 保存原始配置数据以便后续提交
          // 将所有配置组的配置项合并为一个数组
          const allConfigs = [];
          Object.entries(res.data || {}).forEach(([groupName, configs]) => {
            if (Array.isArray(configs)) {
              configs.forEach(config => {
                allConfigs.push({
                  ...config,
                  configGroup: groupName
                });
          });
            }
          });
          
          this.templateObj[currentService] = allConfigs;
        } else {
          this.$message.error(`获取服务 ${currentService} 配置失败: ${res.msg}`);
        }
      } catch (error) {
        this.$message.error(`获取服务配置异常: ${error.message}`);
        console.error("获取配置异常:", error);
      } finally {
        this.loading = false;
      }
    },
    handlerTemplate(serviceName, configGroups) {
      const processedGroups = {};
      
      // 首先创建所有非Kubernetes配置组，确保它们已经存在于processedGroups中
      Object.entries(configGroups).forEach(([groupName, configs]) => {
        if (!groupName.startsWith('kubernetes.config.') && Array.isArray(configs)) {
          const visibleConfigs = configs.filter(item => !item.hidden);
          if (visibleConfigs.length > 0) {
            const processedConfigs = visibleConfigs.map(item => ({
              ...item,
              value: item.type === 'switch' || item.type === 'boolean' 
                ? String(item.value).toLowerCase() === 'true'
                : item.value,
              name: (item.name || '').toString().replaceAll(".", "!")
            }));
            
            processedGroups[groupName] = {
              items: processedConfigs,
              displayName: groupName, 
              templateContent: processedConfigs.find(item => item.templateContent)?.templateContent
            };
          }
        }
      });
      
      // 然后处理Kubernetes配置组
      Object.entries(configGroups).forEach(([groupName, configs]) => {
        // 如果不是数组或不是Kubernetes配置组，跳过处理
        if (!Array.isArray(configs) || !groupName.startsWith('kubernetes.config.')) {
          return;
        }
        
        // 只有当配置组中至少有一项不是hidden时才处理该组
        const visibleConfigs = configs.filter(item => !item.hidden);
        if (visibleConfigs.length === 0) {
          return;
        }
        
        // 处理配置项
        const processedConfigs = visibleConfigs.map(item => ({
          ...item,
          value: item.type === 'switch' || item.type === 'boolean' 
            ? String(item.value).toLowerCase() === 'true'
            : item.value,
          name: (item.name || '').toString().replaceAll(".", "!")
        }));
        
        // 直接从配置组名中提取角色名
        const parts = groupName.split('.');
        const targetRole = parts[parts.length - 1]; // 直接使用最后一部分作为角色名
        const subGroupName = parts.slice(0, parts.length - 1).join('.'); // 使用前面的部分作为子组名
        
        // 确保目标角色分组已经存在
        if (!processedGroups[targetRole]) {
          processedGroups[targetRole] = { 
            items: [], 
            displayName: targetRole, 
            templateContent: null
          };
        }
        
        // 确保hasKubernetesConfig和kubernetesSubGroups属性存在
        if (!processedGroups[targetRole].hasKubernetesConfig) {
          this.$set(processedGroups[targetRole], 'hasKubernetesConfig', true);
        }
        
        if (!processedGroups[targetRole].kubernetesSubGroups) {
          this.$set(processedGroups[targetRole], 'kubernetesSubGroups', {});
        }
        
        const shortSubGroupNameWithoutK8sPrefix = subGroupName.replace('kubernetes.config.', ''); 
        
        // 使用Vue的响应式方法添加子组
        this.$set(
          processedGroups[targetRole].kubernetesSubGroups, 
          shortSubGroupNameWithoutK8sPrefix, 
          {
            items: processedConfigs,
            displayName: this.formatSubGroupName(shortSubGroupNameWithoutK8sPrefix), 
            templateContent: processedConfigs.find(item => item.templateContent)?.templateContent
          }
        );
      });
      
      // 对配置组进行排序
      const sortedGroups = {};
      
      // 获取所有分组键
      const allKeys = Object.keys(processedGroups);
      
      // 对分组键进行排序
      const sortedKeys = this.sortConfigGroups(allKeys);
      
      // 按排序后的顺序构建结果
      sortedKeys.forEach(key => {
        sortedGroups[key] = processedGroups[key];
      });
      
      return sortedGroups;
    },
    
    // 添加一个配置组排序方法
    sortConfigGroups(groupNames) {
      // 将分组分类
      const roleGroups = [];
      const generalGroups = [];
      const advancedGroups = [];
      const customGroups = [];
      const otherGroups = [];
      
      // 对组名进行分类
      groupNames.forEach(name => {
        if (name === 'General' || name === 'CommonConfig') {
          generalGroups.push(name);
        } else if (name.startsWith('advanced_')) {
          advancedGroups.push(name);
        } else if (name.startsWith('custom_')) {
          customGroups.push(name);
        } else if (name.startsWith('kubernetes.config.')) {
          // Kubernetes配置组已经在前面单独处理了
          otherGroups.push(name);
        } else {
          // 假设其他都是角色分组
          roleGroups.push(name);
        }
      });
      
      // 对每个分组内部进行字母序排序
      roleGroups.sort();
      generalGroups.sort();
      advancedGroups.sort();
      customGroups.sort();
      otherGroups.sort();
      
      // 按照优先级顺序合并结果：角色 > 通用 > 高级 > 自定义 > 其他
      return [
        ...roleGroups,
        ...generalGroups,
        ...advancedGroups,
        ...customGroups,
        ...otherGroups
      ];
    },
    // 辅助方法：格式化子组名称
    formatSubGroupName(subGroupName) {
      // 将驼峰式或帕斯卡式的英文名转换为空格分隔的标题式英文名
      let readableEnglishName = subGroupName
        .replace(/([A-Z])/g, " $1") // 在大写字母前添加空格
        .replace(/^./, (str) => str.toUpperCase()) // 首字母大写
        .trim();
      if (!readableEnglishName && subGroupName) readableEnglishName = subGroupName; 
      else if (!readableEnglishName && !subGroupName) readableEnglishName = 'Unknown'; // 处理 subGroupName 为 null 或 undefined 的情况

      const chineseName = this.k8sSubGroupChineseNames[subGroupName];

      let displayText;
      if (chineseName) {
        displayText = chineseName;
      } else {
        // 如果没有特定的中文翻译，使用处理后的英文名作为主要的"中文"部分
        displayText = readableEnglishName; 
      }
      return `${displayText} <span class="k8s-subgroup-en">(${readableEnglishName})</span>`;
    },
    checkAllForm() {
      const self = this;
      let hasError = false;

      // 遍历所有服务
      for (const serviceName of self.SERVICENAMES) {
        // 获取该服务的所有配置组
        const groups = self.groupedTemplateData[serviceName] || {};

        // 遍历每个配置组
        for (const groupName of Object.keys(groups)) {
          const group = groups[groupName];
          
          // 验证常规配置表单
          const refName = `CommonTemplateRef_${serviceName}_${groupName}`;
          const formComponent = self.$refs[refName]?.[0];

          if (formComponent) {
          // 执行表单校验
          formComponent.form.validateFields((err) => {
            if (err) {
              hasError = true;
              self.serviceNameKey = serviceName; // 切换到错误页签
            }
          });

          if (hasError) break;
        }
          
          // 验证角色分组内的Kubernetes配置表单
          if (group.hasKubernetesConfig && group.kubernetesSubGroups) {
            for (const subGroupName of Object.keys(group.kubernetesSubGroups)) {
              const k8sRefName = `CommonTemplateRef_${serviceName}_${groupName}_${subGroupName}`;
              const k8sFormComponent = self.$refs[k8sRefName]?.[0];
              
              if (k8sFormComponent) {
                k8sFormComponent.form.validateFields((err) => {
                  if (err) {
                    hasError = true;
                    self.serviceNameKey = serviceName; // 切换到错误页签
                    // 确保展开包含错误的配置组
                    self.$set(self.isGroupExpanded, `${serviceName}_${groupName}`, true);
                    // 设置激活的Kubernetes标签页
                    self.$set(self.activeKubernetesTabs, `${serviceName}_${groupName}`, subGroupName);
                  }
                });

        if (hasError) break;
              }
            }
            
            if (hasError) break;
          }
        }

        if (hasError) break;
        
        // 验证通用Kubernetes配置
        if (self.kubernetesGroups[serviceName]) {
          for (const subGroupName of Object.keys(self.kubernetesGroups[serviceName])) {
            const k8sRefName = `CommonTemplateRef_${serviceName}_Kubernetes_${subGroupName}`;
            const k8sFormComponent = self.$refs[k8sRefName]?.[0];
            
            if (k8sFormComponent) {
              k8sFormComponent.form.validateFields((err) => {
                if (err) {
                  hasError = true;
                  self.serviceNameKey = serviceName; // 切换到错误页签
                  // 设置激活的Kubernetes标签页
                  self.$set(self.activeKubernetesTabs, serviceName, subGroupName);
                }
              });
              
              if (hasError) break;
            }
          }
          
          if (hasError) break;
        }
      }

      return hasError;
    },
    // 修改后的 submitAllServices 方法
    submitAllServices(callback) {
      const self = this;

      // 生成所有服务的Promise数组
      const promises = this.SERVICENAMES.map(serviceName =>
          new Promise((resolve) => {

            const processService = async () => {
              try {
                // 初始化所有表单数据
                const allFormData = {};

                const groups = self.groupedTemplateData[serviceName] || {}; // 获取当前服务的表单数据

                // 遍历每个配置组（`groupName`）
                for (const groupName of Object.keys(groups)) {
                  const group = groups[groupName];

                  // 处理常规配置表单
                  const refName = `CommonTemplateRef_${serviceName}_${groupName}`;
                  const formRef = self.$refs[refName]?.[0]; // 获取表单组件的引用

                  // 如果找到表单组件，处理表单数据
                  if (formRef) {
                  // 验证表单数据并收集字段值
                  await formRef.form.validateFields();  // 表单验证
                  const rawData = formRef.form.getFieldsValue(); // 获取表单字段值

                  // 处理字段名（替换"."为"!"）并过滤slider辅助输入框
                  const convertedData = Object.keys(rawData).reduce((acc, key) => {
                    // 排除slider辅助输入框的值
                    if (!key.endsWith('_value')) {
                    const newKey = key.replace(/\./g, '!'); // 关键转换逻辑
                    acc[newKey] = rawData[key];
                    }
                    return acc;
                  }, {});

                  // 合并所有表单数据
                  Object.assign(allFormData, convertedData);
                  }
                  
                  // 处理角色分组内的Kubernetes配置表单
                  if (group.hasKubernetesConfig && group.kubernetesSubGroups) {
                    for (const subGroupName of Object.keys(group.kubernetesSubGroups)) {
                      const k8sRefName = `CommonTemplateRef_${serviceName}_${groupName}_${subGroupName}`;
                      const k8sFormRef = self.$refs[k8sRefName]?.[0];
                      
                      if (k8sFormRef) {
                        // 验证并收集表单数据
                        await k8sFormRef.form.validateFields();
                        const k8sRawData = k8sFormRef.form.getFieldsValue();
                        
                        // 处理字段名并过滤
                        const convertedK8sData = Object.keys(k8sRawData).reduce((acc, key) => {
                          if (!key.endsWith('_value')) {
                            const newKey = key.replace(/\./g, '!');
                            acc[newKey] = k8sRawData[key];
                          }
                          return acc;
                        }, {});
                        
                        // 合并数据
                        Object.assign(allFormData, convertedK8sData);
                      }
                    }
                  }
                }
                
                // 处理通用Kubernetes配置
                if (self.kubernetesGroups[serviceName]) {
                  for (const subGroupName of Object.keys(self.kubernetesGroups[serviceName])) {
                    const k8sRefName = `CommonTemplateRef_${serviceName}_Kubernetes_${subGroupName}`;
                    const k8sFormRef = self.$refs[k8sRefName]?.[0];
                    
                    if (k8sFormRef) {
                      // 验证并收集表单数据
                      await k8sFormRef.form.validateFields();
                      const k8sRawData = k8sFormRef.form.getFieldsValue();
                      
                      // 处理字段名并过滤
                      const convertedK8sData = Object.keys(k8sRawData).reduce((acc, key) => {
                        if (!key.endsWith('_value')) {
                          const newKey = key.replace(/\./g, '!');
                          acc[newKey] = k8sRawData[key];
                        }
                        return acc;
                      }, {});
                      
                      // 合并数据
                      Object.assign(allFormData, convertedK8sData);
                    }
                  }
                }

                // 处理复合数据，调用外部函数处理数组和多重数据
                const mergedData = {
                  ...allFormData,
                  ...this.handlearrayWithData(allFormData),
                  ...this.handleMultipleData(allFormData)
                };

                // 构建提交的参数
                const param = (this.templateObj[serviceName] || []).map(item => {
                  const formKey = item.name.replace(/\./g, "!");

                  return {
                    ...item,
                    value: mergedData[formKey] ?? item.value // 将字段值替换为合并后的值
                  };
                });

                // 过滤不必要的字段（如未设置的必需字段和隐藏字段）
                const filterParam = param.filter(item => !(!item.required && item.hidden));

                // 提交表单数据到服务器保存服务配置
                const res = await this.$axiosPost(global.API.saveServiceConfig, {
                  clusterId: this.setting.clusterId || this.clusterId, // 获取集群ID
                  serviceName,
                  serviceConfig: JSON.stringify(filterParam) // 服务配置转为JSON格式
                });

                // 保存配置成功，返回结果
                resolve({...res, name: serviceName});
              } catch (error) {
                // 捕获并处理异常，保存失败时返回错误信息
                console.error(`[${serviceName}] 配置保存失败:`, error);
                resolve({ code: 500, name: serviceName, msg: error.message });
              }
            };

            // 执行服务处理函数
            processService();
          })
      );
      console.log("const promises after map:", promises);

      // 等待所有服务的Promise执行完毕
      Promise.all(promises).then(async (results) => {
        console.log("const results:", results);

        // 筛选出失败的服务
        const failedServices = results.filter(r => r.code !== 200);
        console.log("const failedServices:", failedServices);

        // 处理失败项：显示错误信息并调用回调
        if (failedServices.length > 0) {
          failedServices.forEach(({name, msg}) =>
              this.$message.error(`${name} 配置保存失败: ${msg}`)
          );
          callback?.({code: 500}); // 如果有失败项，调用回调返回失败状态
          return;
        }

        // 如果所有配置保存成功，进行后续命令处理
        try {
          const params = {
            clusterId: this.setting.clusterId || this.clusterId, // 获取集群ID
            serviceNames: this.SERVICENAMES, // 传递所有服务名称
            commandType: this.steps.commandType // 获取命令类型
          };

          // 生成执行命令
          const genCmdRes = await this.$axiosPost(global.API.generateCommand, params);

          this.setCommandIds(genCmdRes.data); // 保存生成的命令ID

          // 启动执行命令
          const execRes = await this.$axiosPost(global.API.startExecuteCommand, {
            ...params,
            commandIds: genCmdRes.data // 传递生成的命令ID以启动执行
          });

          // 执行命令成功，调用回调返回结果
          callback?.(execRes);
        } catch (error) {
          // 捕获并处理异常，命令执行失败时输出错误
          console.error('命令执行流程失败:', error);
          callback?.({code: 500}); // 如果执行失败，返回失败状态
        }
      });
  },
    //  从第七步进入第八步的请求
    async nextSteps(callback) {
      let res = {code: 0};
      const flag = this.checkAllForm();
      if (flag && callback) {
        callback(res);
        return false;
      }
      // 如果所有的表单校验成功了 那么就把所有的tab页去保存一下
      this.submitAllServices(callback);
    },
    // 处理折叠面板变化
    onCollapseChange(activeKeys, serviceName) {
      this.$set(this.expandedKeys, serviceName, activeKeys);
    },
    // 全部展开
    expandAllGroups() {
      const currentService = this.serviceNameKey;
      if (currentService && this.nonKubernetesGroups[currentService]) {
        const allKeys = Object.keys(this.nonKubernetesGroups[currentService]);
        this.$set(this.expandedKeys, currentService, allKeys);
        this.isAllExpanded = true;
      }
    },
    // 全部折叠
    collapseAllGroups() {
      const currentService = this.serviceNameKey;
      if (currentService) {
        this.$set(this.expandedKeys, currentService, []);
        this.isAllExpanded = false;
      }
    },
    toggleAllGroups() {
      this.isAllExpanded = !this.isAllExpanded;
      if (this.isAllExpanded) {
        this.expandAllGroups();
      } else {
        this.collapseAllGroups();
      }
    },
  },
  created() {
    if (this.steps4Data && this.steps4Data.serviceNames && this.steps4Data.serviceNames.length > 0) {
      this.SERVICENAMES = this.steps4Data.serviceNames.map(
          (item) => item.serviceName
      );
      this.serviceNameKey = this.SERVICENAMES[0];
      this.selectKeys.push(this.serviceNameKey);
      this.SERVICENAMES.forEach((item) => {
        this.templateObj[`${item}`] = [];
        // 初始化分组展开状态对象
        this.$set(this.isGroupExpanded, item, {});
        // 初始化服务级别的Kubernetes标签页激活状态
        this.$set(this.activeKubernetesTabs, item, '');
      });
    } else {
      this.$message.warning("未选择任何服务，请返回步骤4选择服务");
      this.SERVICENAMES = [];
      this.serviceNameKey = '';
    }
  },
  mounted() {
    this.getServiceConfigOption().then(() => {
      // 数据加载完成后，初始化角色分组级别的Kubernetes标签页激活状态
      this.SERVICENAMES.forEach(serviceName => {
        const groups = this.groupedTemplateData[serviceName] || {};
        Object.keys(groups).forEach(groupName => {
          const group = groups[groupName];
          if (group && group.hasKubernetesConfig && group.kubernetesSubGroups) {
            const subGroupKeys = Object.keys(group.kubernetesSubGroups);
            if (subGroupKeys.length > 0) {
              // 初始化该角色分组的Kubernetes标签页激活状态
              this.$set(this.activeKubernetesTabs, `${serviceName}_${groupName}`, subGroupKeys[0]);
            }
          }
        });
      });
    });
  },
};
</script>
<style lang="less" scoped>
/* 步骤组件样式 - 确保不会干扰父组件的按钮 */
.steps7.steps {
  position: relative;
  height: calc(100vh - 230px); /* 减小高度，给底部按钮留更多空间 */
  max-height: calc(100vh - 230px);
  width: 100%;
  display: flex;
  flex-direction: column;
  overflow: hidden; /* 保持hidden以控制整体容器边界 */
  padding-bottom: 20px; /* 增加底部填充 */
  margin-top: -10px;
}

/* 主容器 */
.service-config-container {
  position: relative;
  padding: 0 0 80px 0; /* 增加底部内边距，确保按钮有足够空间 */
  width: 100%;
  height: 100%;
  background-color: #fff;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1); 
  box-sizing: border-box;
}

/* 主内容区域容器 */
.main-content-area {
  display: flex;
  flex-direction: column;
  flex: 1;
  width: 100%;
  height: auto;
  min-height: 0; /* 允许容器缩小 */
  max-height: calc(100vh - 310px); /* 调整最大高度，确保内容不会溢出 */
  overflow-y: auto; /* 启用垂直滚动 */
  position: relative;
  box-sizing: border-box;
  padding-bottom: 20px; /* 增加底部间距 */
  margin-bottom: 25px; /* 增加底部外边距 */
  }

/* 标题样式 */
.steps-title {
  margin-bottom: 5px;
  font-size: 16px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
  padding: 0 16px;
  flex-shrink: 0;
}

/* 新增spin组件样式 */
.content-spin {
  position: relative; 
  flex: 1; 
  display: flex; 
  flex-direction: column; 
  overflow: hidden;
  height: 100%;
  margin-bottom: 0; /* 移除底部边距，由父容器控制 */
}

/* 内容包装区域 */
.content-wrapper {
  position: relative;
  flex: 1;
  width: 100%;
  height: auto;
  max-height: none; /* 移除最大高度限制，由父容器控制 */
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
  padding-bottom: 10px;
}

/* 配置面板样式 */
.config-area {
  flex: 1;
  width: 100%;
  height: auto;
  background-color: #fff;
  position: relative;
  box-sizing: border-box;
  
  &.hidden {
    display: none;
  }
  
  /* 内部容器，确保可以正确滚动 */
  .config-area-inner {
    position: relative;
    height: auto;
    width: 100%;
    padding: 0 8px;
    padding-bottom: 40px; /* 增加底部内边距 */
    box-sizing: border-box;
    }

  /* 底部空白区域，提供额外空间 */
  .bottom-spacer {
    height: 40px; /* 增加高度 */
    width: 100%;
    clear: both;
    flex-shrink: 0;
    position: relative;
    z-index: 1;
  }
  
  /* 使用Ant Design Collapse的自定义样式 */
  .config-collapse {
    width: 100%;
    background: transparent;
    overflow: visible;
    max-height: none;
    
    /deep/ .ant-collapse-item {
      margin-bottom: 8px;
      border: 1px solid #e8e8e8;
      border-radius: 2px;
      overflow: visible;
      
      &:last-child {
        margin-bottom: 20px;
      }
      
      /* 特殊处理最后一个分组 */
      &.last-group {
        margin-bottom: 30px;
        
        .ant-collapse-content {
          padding-bottom: 5px;
          
          .ant-collapse-content-box {
            padding-bottom: 10px;
          }
        }
      }
      
      /* 优化折叠面板标题样式 */
      .ant-collapse-header {
        padding: 8px 40px 8px 16px !important;
        background-color: #fff;
        font-weight: normal;
        color: rgba(0, 0, 0, 0.85);
        transition: all 0.3s;
        display: flex;
        align-items: center;
        word-break: break-all;
        white-space: normal;
        position: relative;
        line-height: 1.5;
        min-height: 36px;
        
        &:hover {
          background-color: #fafafa;
    }

        .ant-collapse-arrow {
          right: 16px !important;
          left: auto !important;
      position: absolute !important;
        }
      }
      
      /* 重要：处理折叠面板内容区，防止高度变化影响父容器 */
      .ant-collapse-content {
        border-top: 1px solid #e8e8e8;
        overflow: visible;
        max-height: none;
        background-color: #f5f7fa;
        
        .ant-collapse-content-box {
          padding: 16px;
          background-color: #f5f7fa;
          min-height: 50px;
          overflow: visible;
          /* 移除内部滚动设置 */
      }
    }
  }
}
}

/* 标签页相关样式 */
.steps7 {
  .ant-tabs {
    width: 100%;
    flex-shrink: 0;
    
    /deep/ .ant-tabs-bar {
      margin-bottom: 5px;
      border-bottom: 1px solid #e8e8e8;
    }
    
    /deep/ .ant-tabs-nav {
      padding: 0 16px !important;
      width: 100%;

      .ant-tabs-tab {
        font: 500 13px/1.5 "Helvetica Neue" !important;
        padding: 8px 16px !important;
        color: #868e96 !important;
        border: 1px solid transparent;

        &-active {
          color: #228be6 !important;
          background: rgba(34, 139, 230, 0.06) !important;
          border-color: #e7f5ff !important;
        }
      }

      .ant-tabs-ink-bar {
        background: #228be6 !important;
        height: 2px !important;
      }
    }
    
    /* 确保标签页内容不会超出 */
    /deep/ .ant-tabs-content {
      flex: 1;
      overflow: visible;
      width: 100%;
      height: auto;
  }
  }

  /* 保存按钮 */
  .btn-save {
    position: absolute;
    right: 32px;
    top: -5px;
    z-index: 1100; /* 提高z-index确保按钮始终可见 */
    box-shadow: 0 2px 6px rgba(0, 0, 0, 0.15);
  }
}

/* 修复CommonTemplate组件中的样式问题 */
/deep/ .ant-form-item {
  margin-bottom: 14px;
  max-width: 100%;
  width: 100%;
  display: flex;
  flex-wrap: wrap;
  
  .ant-form-item-label {
    width: 30%;
    max-width: 30%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    padding-right: 8px;
    box-sizing: border-box;
  }
  
  .ant-form-item-control-wrapper {
    width: 70%;
    max-width: 70%;
    flex: 1;
  }
  
  /* 输入控件宽度适应容器 */
  .ant-input, .ant-select, .ant-input-number, .ant-cascader-picker {
    width: 100%;
    max-width: 100%;
  }
  
  /* 多行文本框限制高度 */
  textarea.ant-input {
    max-height: 150px;
  }
  
  /* 输入框样式调整 */
  .ant-input {
    max-width: 100%;
    word-break: break-all;
    overflow-wrap: break-word;
    resize: vertical;
  }
}

/* 底部按钮区域样式 */
/deep/ .steps-action {
  margin-top: 0;
  padding: 8px 0;
  position: absolute;
  bottom: 0;
  right: 0;
  width: 100%;
  text-align: center;
  background-color: #fff;
  z-index: 10; /* 提高z-index确保按钮在顶层 */
  box-shadow: 0 -2px 8px rgba(0,0,0,0.05);
  border-top: 1px solid #f0f0f0;
  padding-bottom: 12px; /* 增加底部内边距 */
  padding-top: 12px; /* 增加顶部内边距 */
}

/* 添加面板内容区样式，确保与ServiceConfig.vue一致 */
.panel-content {
  background-color: #f5f7fa;
  border-radius: 0 0 4px 4px;
  padding: 12px;
  width: auto;
  min-width: 100%;
}

/* 添加面板标题文本样式 */
.panel-header-text {
  font-weight: normal;
  color: rgba(0, 0, 0, 0.85);
  font-size: 14px;
}

/* 模板内容容器样式 */
.template-content-container {
  margin-top: 20px;
  padding: 12px;
  border-top: 1px dashed #d9d9d9;
}

.template-content-title {
  font-weight: 500;
  margin-bottom: 8px;
  font-size: 14px;
  color: #595959;
}

.template-content-textarea {
  background-color: #fafafa;
  font-family: 'Courier New', Courier, monospace;
  color: #595959;
}

/* 添加面板内容区样式，确保与ServiceConfig.vue一致 */
.steps7 {
  .service-config-container {
    background-color: #fff;
    border-radius: 4px;
    padding: 20px;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
    
    .content-spin {
      min-height: 300px;
    }
    
    .config-area {
      &.hidden {
        display: none;
      }
      
      .config-collapse {
        background: transparent;
        
        .config-panel {
          border: 1px solid #ebeef5;
          border-radius: 4px;
          margin-bottom: 16px;
          background-color: #fff;
          
          &:last-child {
            margin-bottom: 0;
          }
          
          .panel-header-text {
            font-weight: 500;
            font-size: 15px;
            color: #333;
          }
          
          .panel-content {
            padding: 16px;
          }
        }
      }
    }
  }
}

/* 确保单位输入框样式与图片一致 */
.content-wrapper /deep/ .input-with-unit {
  display: flex;
  width: 100%;
  
  .ant-input {
    flex: 1;
    min-width: 0; /* 确保输入框可以缩小 */
    border-top-right-radius: 0;
    border-bottom-right-radius: 0;
    border-right: none;
  }
  
  .input-unit-suffix {
    display: flex;
    align-items: center;
    justify-content: center;
    min-width: 40px; /* 减小最小宽度 */
    max-width: 70px; /* 添加最大宽度 */
    height: 32px;
    padding: 0 8px; /* 减小内边距 */
    color: rgba(0, 0, 0, 0.65);
    font-size: 14px;
    text-align: center;
    background-color: #f5f5f5;
    border: 1px solid #d9d9d9;
    border-left: none;
    border-radius: 0 4px 4px 0;
    white-space: nowrap; /* 防止单位文本换行 */
    overflow: hidden; /* 隐藏溢出的文本 */
    text-overflow: ellipsis; /* 显示省略号 */
  }
  
  &:hover .input-unit-suffix {
    border-color: #40a9ff;
  }
}

/* 添加底部空间，确保内容不被按钮遮挡 */
.bottom-spacer {
  height: 80px;
}

/* 添加Kubernetes配置区域样式 */
.kubernetes-config-section {
  margin-top: 16px;
  padding: 16px;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 1px 4px rgba(0, 0, 0, 0.1);
  
  .kubernetes-tabs-header {
    font-size: 16px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
    margin-bottom: 16px;
    padding-bottom: 8px;
    border-bottom: 1px solid #f0f0f0;
  }
  
  :deep(.ant-tabs-nav) {
    margin-bottom: 16px;
  }
  
  :deep(.ant-tabs-tab) {
    padding: 8px 16px;
    transition: all 0.3s;
    
    &:hover {
      color: #1890ff;
    }
  }
  
  :deep(.ant-tabs-tab-active) {
    .ant-tabs-tab-btn {
      color: #1890ff;
      font-weight: 500;
    }
  }
  
  :deep(.ant-tabs-ink-bar) {
    background: #1890ff;
  }
}

/* 确保模板内容显示正确 */
.template-content-container {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
  
  .template-content-title {
    margin-bottom: 8px;
    font-weight: 500;
    color: rgba(0, 0, 0, 0.85);
  }
  
  .template-content-textarea {
    background: #fff;
  }
}

/* 添加角色分组内Kubernetes配置区域样式 */
.group-kubernetes-section {
  margin-top: 0;
  margin-bottom: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 4px;
  border: 1px solid #ebedf0;
  
  .kubernetes-tabs-header {
    font-size: 15px;
    font-weight: 500;
    color: #196cca;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e6f7ff;
  }
  
  :deep(.ant-tabs-nav) {
    margin-bottom: 15px;
  }
  
  :deep(.ant-tabs-tab) {
    padding: 8px 16px;
    transition: all 0.3s;
    font-size: 14px;
    line-height: 1.6;
    margin-right: 10px;
    border-radius: 4px;
    
    &:hover {
      color: #196cca;
      background-color: rgba(25, 108, 202, 0.05);
    }
    
    .ant-tabs-tab-btn {
      display: flex;
      align-items: center;
      
      span {
        display: inline-flex;
        align-items: center;
      }
    }
  }
  
  :deep(.ant-tabs-tab-active) {
    background-color: rgba(25, 108, 202, 0.08);
    
    .ant-tabs-tab-btn {
      color: #196cca;
      font-weight: 500;
    }
  }
  
  :deep(.ant-tabs-ink-bar) {
    background: #196cca;
    height: 3px;
  }
  
  .template-content-container {
    margin-top: 12px;
    padding: 12px;
    background: #f5f5f5;
    border-radius: 4px;
  }
}

/* 顶部Kubernetes配置区域样式 */
.kubernetes-config-section {
  margin-bottom: 20px;
  padding: 16px;
  background: #f9fafb;
  border-radius: 4px;
  border: 1px solid #ebedf0;
  width: auto;
  min-width: 100%;
  
  .kubernetes-tabs-header {
    font-size: 16px;
    font-weight: 500;
    color: #196cca;
    margin-bottom: 12px;
    padding-bottom: 8px;
    border-bottom: 1px solid #e6f7ff;
  }
  
  :deep(.ant-tabs-nav) {
    margin-bottom: 15px;
  }
  
  :deep(.ant-tabs-tab) {
    padding: 8px 16px;
    transition: all 0.3s;
    font-size: 14px;
    line-height: 1.6;
    margin-right: 10px;
    border-radius: 4px;
    
    &:hover {
      color: #196cca;
      background-color: rgba(25, 108, 202, 0.05);
    }
    
    .ant-tabs-tab-btn {
      display: flex;
      align-items: center;
      
      span {
        display: inline-flex;
        align-items: center;
      }
    }
  }
  
  :deep(.ant-tabs-tab-active) {
    background-color: rgba(25, 108, 202, 0.08);
    
    .ant-tabs-tab-btn {
      color: #196cca;
      font-weight: 500;
    }
  }
  
  :deep(.ant-tabs-ink-bar) {
    background: #196cca;
    height: 3px;
  }
}

/* 确保标签页内小字体正确显示 */
:deep(.ant-tabs-tab) {
  .ant-tabs-tab-btn {
    span {
      display: inline-block;
    }
  }
}

/* 设置英文部分的字体样式 */
.small-text {
  font-size: 60%;
  color: #aaa;
  font-weight: normal;
  font-family: Arial, sans-serif;
  margin-left: 4px;
  letter-spacing: 0;
  position: relative;
  top: -1px;
  display: inline-block;
  vertical-align: middle;
  line-height: 1.2;
}

/* 新增：Kubernetes子组Tab英文名样式 */
.steps7 /deep/ .ant-tabs-tab .k8s-subgroup-en {
  color: #E6A23C; /* 浅橙色 */
  font-size: 0.9em;   /* 辅助字体稍小 */
  font-weight: normal; /* 非粗体 */
  margin-left: 4px;    /* 括号前的空格 */
}

/* 确保标签页内容样式 */
:deep(.ant-tabs-tab) {
  .ant-tabs-tab-btn {
    span {
      display: inline-block;
      
      /* 强化中文部分的样式 */
      &:not(.small-text) {
        font-weight: 600;
        font-size: 14px;
        color: #333;
      }
    }
  }
}

/* 设置中文部分的字体样式 */
.main-text {
  font-weight: 600;
  color: #000;
  font-size: 15px;
  letter-spacing: 0.5px;
}

/* Kubernetes配置区域样式 */
.group-kubernetes-section {
  background-color: #f9fafc;
  border-radius: 8px;
  border: 1px solid #e8eaf1;
  margin-bottom: 24px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
  width: auto;
  min-width: 100%;
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

/* 添加媒体查询以处理不同的屏幕尺寸和缩放级别 */
@media screen and (max-width: 1200px) {
  /deep/ .ant-form-item {
    .ant-form-item-label {
      width: 40%;
      max-width: 40%;
    }
    
    .ant-form-item-control-wrapper {
      width: 60%;
      max-width: 60%;
    }
  }
}

@media screen and (max-width: 992px) {
  /deep/ .ant-form-item {
    .ant-form-item-label {
      width: 100%;
      max-width: 100%;
      text-align: left;
    }
    
    .ant-form-item-control-wrapper {
      width: 100%;
      max-width: 100%;
    }
  }
  
  .kubernetes-config-section,
  .group-kubernetes-section {
    padding: 12px 8px;
  }
  
  .panel-content {
    padding: 10px 6px;
  }
}
</style>


