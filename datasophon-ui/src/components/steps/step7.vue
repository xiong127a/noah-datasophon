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
  <div class="steps7 steps apple-style-container">
    <div class="steps-header">
      <div class="header-content">
        <div class="steps-title">
          <span>服务配置</span>
        </div>
        <div class="steps-subtitle">
          <span>配置各个服务组件的运行参数</span>
        </div>
      </div>
      <div class="header-actions">
        <a-button class="apple-button toggle-button" type="default" @click="toggleAllGroups">
          <a-icon :type="isAllExpanded ? 'shrink' : 'arrows-alt'" />
          {{ isAllExpanded ? '折叠全部' : '展开全部' }}
        </a-button>
        <a-button class="apple-button save-button" type="primary" @click="handleSubmit">
          <a-icon type="save" />
          保存配置
        </a-button>
      </div>
    </div>
    
    <div class="content-container">
      <a-spin :spinning="loading" class="apple-spin">
        <a-tabs 
          v-model="serviceNameKey" 
          @change="callback" 
          class="apple-tabs"
        >
          <a-tab-pane 
            v-for="item in SERVICENAMES" 
            :key="item" 
            :tab="item" 
            :forceRender="true"
          />
        </a-tabs>
        
        <div 
          v-for="item in SERVICENAMES" 
          :key="item"
          :class="[
            'service-config-container',
            serviceNameKey === item ? 'active-container' : 'inactive-container',
            item + 'warp'
          ]"
        >
          <div v-if="serviceNameKey === item" class="config-area-inner">
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
                          class="apple-template"
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
                    class="apple-template"
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
      </a-spin>
    </div>
  </div>
</template>
<script>
import FixedCommonTemplate from "@/components/steps/FixedCommonTemplate.vue";
import { mapActions, mapState } from "vuex";
import { de } from "date-fns/locale";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  components: { FixedCommonTemplate },
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
      selectKeys: [],
      groupedTemplateData: {},  // 按服务分组的配置数据 { service1: { group1: [], group2: [] }, ... }
      isGroupExpanded: {},      // 分组展开状态 { service1: { group1: true, group2: false }, ... }
      activeKubernetesTabs: {}, // 存储每个服务的Kubernetes Tab激活状态
      kubernetesSubGroupChineseNames: {
        'persistentVolumeClaims': '持久卷声明',
        'resources': '资源规格',
        'services': '服务暴露',
        'node_port_mappings': '节点端口映射',
        'cluster_port_mappings': '集群端口映射',
        'load_balancer_port_mappings': '负载均衡器端口映射',
        'requests_memory': '内存请求',
        'requests_cpu': 'CPU请求',
        'limits_memory': '内存限制',
        'limits_cpu': 'CPU限制',
        'storage_classes': '存储类',
        'mount_path': '挂载路径',
        'storage_size': '存储容量'
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
      // 返回空对象，不显示顶层的Kubernetes配置
      return {};
      
      // 原来的实现（已禁用）
      /*
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
      */
    },
    
    // 判断每个服务是否有通用Kubernetes配置
    hasKubernetesGroups() {
      // 始终返回false，不显示顶层的Kubernetes配置
      return {};
      
      // 原来的实现（已禁用）
      /*
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
      */
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
      console.log('Tab changed to:', key);
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
    handleSubmit() {
      const currentService = this.serviceNameKey;
      if (!currentService) {
        this.$message.error("未选择服务");
        return;
      }

      // 获取当前服务的所有配置组
      const groups = this.groupedTemplateData[currentService];
      if (!groups || Object.keys(groups).length === 0) {
        this.$message.error("当前服务没有配置项");
        return;
      }

      // 使用Promise.all来处理所有表单验证
      const formValidations = [];
      const formValues = {};

      // 遍历每个配置组收集表单验证Promise
      Object.entries(groups).forEach(([groupName, group]) => {
        // 处理常规配置表单
        const refName = `CommonTemplateRef_${currentService}_${groupName}`;
        const formComponent = this.$refs[refName]?.[0];
        
        if (formComponent && formComponent.form) {
          const formPromise = new Promise((resolve, reject) => {
            formComponent.form.validateFields((err, values) => {
              if (err) {
                reject(err);
              } else {
                // 合并表单值
                Object.assign(formValues, values);
                resolve();
              }
            });
          });
          formValidations.push(formPromise);
        }
        
        // 处理角色分组内的Kubernetes配置表单
        if (group.hasKubernetesConfig && group.kubernetesSubGroups) {
          Object.keys(group.kubernetesSubGroups).forEach(subGroupName => {
            const kubernetesRefName = `CommonTemplateRef_${currentService}_${groupName}_${subGroupName}`;
            const kubernetesFormComponent = this.$refs[kubernetesRefName]?.[0];
            
            if (kubernetesFormComponent && kubernetesFormComponent.form) {
              const kubernetesFormPromise = new Promise((resolve, reject) => {
                kubernetesFormComponent.form.validateFields((err, values) => {
                  if (err) {
                    reject(err);
                  } else {
                    // 合并表单值
                    Object.assign(formValues, values);
                    resolve();
                  }
                });
              });
              formValidations.push(kubernetesFormPromise);
            }
          });
        }
      });

      // 等待所有表单验证完成
      Promise.all(formValidations)
        .then(() => {
          // 处理表单数据
          const arrayWithData = this.handlearrayWithData(formValues);
          const multipleData = this.handleMultipleData(formValues);
          const formData = { ...formValues, ...arrayWithData, ...multipleData };
          
          // 更新模板数据
          let param = _.cloneDeep(this.templateObj[currentService] || []);
          for (let name in formData) {
            param.forEach((item) => {
              if (item.name === name) {
                item.value = formData[name];
              }
            });
          }
          
          // 处理名称中的特殊字符
          param.forEach((item) => {
            item.name = item.name.replaceAll("!", ".");
          });
          
          // 过滤不必要的项
          let filterParam = param.filter(
            (item) => !(!item.required && item.hidden)
          );
          
          // 构建保存参数
          let saveParam = {
            clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
            serviceName: currentService,
            serviceConfig: JSON.stringify(filterParam),
          };
          
          // 发送保存请求
          return this.$axiosPost(global.API.saveServiceConfig, saveParam);
        })
        .then(res => {
          if (res.code === 200) {
            this.$message.success("保存成功");
          } else {
            this.$message.error(`保存失败: ${res.msg || '未知错误'}`);
          }
        })
        .catch(err => {
          console.error('表单验证或保存失败:', err);
          this.$message.error(`操作失败: ${err.message || '表单验证错误'}`);
        });
    },
    getServiceConfigOption() {
      if (!this.SERVICENAMES || this.SERVICENAMES.length === 0) {
        console.warn('No services to get config for');
        return Promise.resolve(); // 返回一个已解析的Promise
      }

      this.loading = true;
      const self = this;
      console.log('Getting config for services:', this.SERVICENAMES);
      
      // 获取当前选中服务的配置
      const currentService = this.serviceNameKey;
      if (!currentService) {
        console.warn('No service selected');
        return Promise.resolve(); // 返回一个已解析的Promise
      }

      console.log('Getting config for service:', currentService);
      const params = {
        clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId,
        serviceName: currentService,
      };
      
      // 返回Promise
      return this.$axiosPost(global.API.getServiceConfigOption, params)
        .then((res) => {
          console.log('Response for service', currentService, ':', res);
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
            console.error('Failed to get config for service', currentService, ':', res);
          }
          self.loading = false;
          return res; // 返回结果以便链式调用
        })
        .catch(err => {
          console.error('Error getting config for service', currentService, ':', err);
          self.loading = false;
          return Promise.reject(err); // 返回被拒绝的Promise以便链式调用
        });
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
        
        const shortSubGroupNameWithoutKubernetesPrefix = subGroupName.replace('kubernetes.config.', '');
        
        // 使用Vue的响应式方法添加子组
        this.$set(
          processedGroups[targetRole].kubernetesSubGroups, 
          shortSubGroupNameWithoutKubernetesPrefix,
          {
            items: processedConfigs,
            displayName: this.formatSubGroupName(shortSubGroupNameWithoutKubernetesPrefix),
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
    checkAllForm() {
      const self = this;
      
      return new Promise((resolve) => {
        // 创建表单验证Promise数组
        const formValidations = [];
        let foundError = false;
        let errorServiceName = null;
        let errorGroupName = null;
        let errorSubGroupName = null;

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

            if (formComponent && formComponent.form) {
              const formPromise = new Promise((resolveForm) => {
                formComponent.form.validateFields((err) => {
                  if (err && !foundError) {
                    foundError = true;
                    errorServiceName = serviceName;
                    errorGroupName = groupName;
                  }
                  resolveForm();
                });
              });
              formValidations.push(formPromise);
            }
            
            // 验证角色分组内的Kubernetes配置表单
            if (group.hasKubernetesConfig && group.kubernetesSubGroups) {
              for (const subGroupName of Object.keys(group.kubernetesSubGroups)) {
                const kubernetesRefName = `CommonTemplateRef_${serviceName}_${groupName}_${subGroupName}`;
                const kubernetesFormComponent = self.$refs[kubernetesRefName]?.[0];
                
                if (kubernetesFormComponent && kubernetesFormComponent.form) {
                  const kubernetesFormPromise = new Promise((resolveForm) => {
                    kubernetesFormComponent.form.validateFields((err) => {
                      if (err && !foundError) {
                        foundError = true;
                        errorServiceName = serviceName;
                        errorGroupName = groupName;
                        errorSubGroupName = subGroupName;
                      }
                      resolveForm();
                    });
                  });
                  formValidations.push(kubernetesFormPromise);
                }
              }
            }
          }
          
          // 验证通用Kubernetes配置
          if (self.kubernetesGroups[serviceName]) {
            for (const subGroupName of Object.keys(self.kubernetesGroups[serviceName])) {
              const kubernetesRefName = `CommonTemplateRef_${serviceName}_Kubernetes_${subGroupName}`;
              const kubernetesFormComponent = self.$refs[kubernetesRefName]?.[0];
              
              if (kubernetesFormComponent && kubernetesFormComponent.form) {
                const kubernetesFormPromise = new Promise((resolveForm) => {
                  kubernetesFormComponent.form.validateFields((err) => {
                    if (err && !foundError) {
                      foundError = true;
                      errorServiceName = serviceName;
                      errorSubGroupName = subGroupName;
                    }
                    resolveForm();
                  });
                });
                formValidations.push(kubernetesFormPromise);
              }
            }
          }
        }

        // 等待所有表单验证完成
        Promise.all(formValidations).then(() => {
          if (foundError) {
            // 如果有错误，切换到相应的页签和配置组
            self.serviceNameKey = errorServiceName;
            
            if (errorGroupName) {
              // 确保展开包含错误的配置组
              self.$set(self.expandedKeys, errorServiceName, 
                [...(self.expandedKeys[errorServiceName] || []), errorGroupName]);
              
              if (errorSubGroupName) {
                // 设置激活的Kubernetes标签页
                self.$set(self.activeKubernetesTabs, 
                  `${errorServiceName}_${errorGroupName}`, errorSubGroupName);
              }
            } else if (errorSubGroupName) {
              // 设置激活的Kubernetes标签页（通用配置）
              self.$set(self.activeKubernetesTabs, errorServiceName, errorSubGroupName);
            }
          }
          
          resolve(foundError);
        });
      });
    },
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
                      const kubernetesRefName = `CommonTemplateRef_${serviceName}_${groupName}_${subGroupName}`;
                      const kubernetesFormRef = self.$refs[kubernetesRefName]?.[0];
                      
                      if (kubernetesFormRef) {
                        // 验证并收集表单数据
                        await kubernetesFormRef.form.validateFields();
                        const kubernetesRawData = kubernetesFormRef.form.getFieldsValue();
                        
                        // 处理字段名并过滤
                        const convertedKubernetesData = Object.keys(kubernetesRawData).reduce((acc, key) => {
                          if (!key.endsWith('_value')) {
                            const newKey = key.replace(/\./g, '!');
                            acc[newKey] = kubernetesRawData[key];
                          }
                          return acc;
                        }, {});
                        
                        // 合并数据
                        Object.assign(allFormData, convertedKubernetesData);
                      }
                    }
                  }
                }
                
                // 处理通用Kubernetes配置
                if (self.kubernetesGroups[serviceName]) {
                  for (const subGroupName of Object.keys(self.kubernetesGroups[serviceName])) {
                    const kubernetesRefName = `CommonTemplateRef_${serviceName}_Kubernetes_${subGroupName}`;
                    const kubernetesFormRef = self.$refs[kubernetesRefName]?.[0];
                    
                    if (kubernetesFormRef) {
                      // 验证并收集表单数据
                      await kubernetesFormRef.form.validateFields();
                      const kubernetesRawData = kubernetesFormRef.form.getFieldsValue();
                      
                      // 处理字段名并过滤
                      const convertedKubernetesData = Object.keys(kubernetesRawData).reduce((acc, key) => {
                        if (!key.endsWith('_value')) {
                          const newKey = key.replace(/\./g, '!');
                          acc[newKey] = kubernetesRawData[key];
                        }
                        return acc;
                      }, {});
                      
                      // 合并数据
                      Object.assign(allFormData, convertedKubernetesData);
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

      // 等待所有服务的Promise执行完毕
      Promise.all(promises).then(async (results) => {
        // 筛选出失败的服务
        const failedServices = results.filter(r => r.code !== 200);

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
      try {
        let res = { code: 0 };
        const hasError = await this.checkAllForm();
        
        if (hasError) {
          if (callback) {
            callback(res);
          }
          return false;
        }
        
        // 如果所有的表单校验成功了 那么就把所有的tab页去保存一下
        this.submitAllServices(callback);
      } catch (error) {
        console.error('验证表单时出错:', error);
        if (callback) {
          callback({ code: 500, msg: error.message || '验证表单时出错' });
        }
      }
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

      const chineseName = this.kubernetesSubGroupChineseNames[subGroupName];

      let displayText;
      if (chineseName) {
        displayText = chineseName;
      } else {
        // 如果没有特定的中文翻译，使用处理后的英文名作为主要的"中文"部分
        displayText = readableEnglishName; 
      }
      return `${displayText} <span class="kubernetes-subgroup-en">(${readableEnglishName})</span>`;
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
    console.log('Step7 created with steps4Data:', this.steps4Data);
    // 确保serviceNames存在且不为空
    if (this.steps4Data && this.steps4Data.serviceNames && this.steps4Data.serviceNames.length > 0) {
      this.SERVICENAMES = this.steps4Data.serviceNames.map(
        (item) => item.serviceName
      );
      console.log('Initialized SERVICENAMES:', this.SERVICENAMES);
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
      console.warn('No services selected in steps4Data');
      this.SERVICENAMES = [];
      this.serviceNameKey = '';
    }
  },
  mounted() {
    console.log('Step7 mounted, calling getServiceConfigOption');
    // 只要有选中的服务就获取配置
    if (this.SERVICENAMES && this.SERVICENAMES.length > 0) {
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
    } else {
      console.warn('No services to get config for');
    }
  },
};
</script>
<style lang="less" scoped>
// 苹果设计风格颜色变量
@apple-white: #ffffff;
@apple-blue: #0071e3;
@apple-blue-light: rgba(0, 113, 227, 0.1);
@apple-gray-100: #f5f5f7;
@apple-gray-200: #e5e5ea;
@apple-gray-300: #d2d2d7;
@apple-gray-400: #86868b;
@apple-gray-500: #6e6e73;
@apple-text: #1d1d1f;
@apple-border: rgba(0, 0, 0, 0.1);

.apple-style-container {
  font-family: "SF Pro Display", -apple-system, BlinkMacSystemFont, "Segoe UI", Roboto, Oxygen, Ubuntu, Cantarell, "Open Sans", sans-serif;
  color: @apple-text;
  margin: 0;
  padding: 0;
  background-color: @apple-white;
  min-height: 100%;
  position: relative;
}

.steps-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  padding: 0 0 24px 0;
  border-bottom: 1px solid @apple-border;
  margin-bottom: 24px;
  
  .header-content {
    .steps-title {
      font-size: 24px;
      font-weight: 600;
      margin-bottom: 8px;
      color: @apple-text;
      background: linear-gradient(120deg, @apple-text, #505050);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }
    
    .steps-subtitle {
      font-size: 16px;
      color: @apple-gray-500;
    }
  }
  
  .apple-button {
    height: 40px;
    padding: 0 20px;
    border-radius: 20px;
    font-size: 14px;
    font-weight: 500;
    display: flex;
    align-items: center;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    &.save-button {
      background: @apple-blue;
      border: none;
      color: white;
      box-shadow: 0 2px 6px rgba(0, 113, 227, 0.2);
      
      &:hover {
        background: darken(@apple-blue, 5%);
        transform: translateY(-1px);
        box-shadow: 0 4px 12px rgba(0, 113, 227, 0.3);
      }
      
      &:active {
        transform: translateY(0);
      }
      
      .anticon {
        margin-right: 6px;
        font-size: 14px;
      }
    }
  }
}

.content-container {
  position: relative;
  margin-top: 20px;
  
  .apple-spin {
    :deep(.ant-spin-dot) {
      .ant-spin-dot-item {
        background-color: @apple-blue;
      }
    }
    
    :deep(.ant-spin-text) {
      color: @apple-blue;
    }
  }
  
  .apple-tabs {
    :deep(.ant-tabs-bar) {
      border-bottom: none;
      margin: 0 0 24px 0;
      
      .ant-tabs-nav-container {
        font-size: 15px;
      }
      
      .ant-tabs-tab {
        padding: 12px 20px;
        margin: 0 8px 0 0;
        color: @apple-gray-500;
        font-weight: 500;
        transition: all 0.3s;
        border-radius: 8px;
        
        &:hover {
          color: @apple-blue;
        }
        
        &.ant-tabs-tab-active {
          color: @apple-blue;
          font-weight: 600;
          background: @apple-blue-light;
        }
      }
      
      .ant-tabs-ink-bar {
        display: none;
      }
    }
  }
  
  .service-config-container {
    background: @apple-white;
    border-radius: 12px;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    padding: 24px;
    margin-bottom: 24px;
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    &.active-container {
      opacity: 1;
      transform: translateY(0);
    }
    
    &.inactive-container {
      opacity: 0;
      transform: translateY(20px);
      pointer-events: none;
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
    }
    
    .apple-template {
      :deep(.ant-form-item) {
        margin-bottom: 24px;
        
        .ant-form-item-label {
          line-height: 1.5;
          
          label {
            color: @apple-text;
            font-weight: 500;
            font-size: 14px;
          }
        }
        
        .ant-form-item-control {
          line-height: 1.5;
          
          .ant-input {
            border-radius: 8px;
            border-color: @apple-gray-300;
            transition: all 0.3s;
            padding: 8px 12px;
            
            &:hover {
              border-color: @apple-blue;
            }
            
            &:focus {
              border-color: @apple-blue;
              box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
            }
          }
          
          .ant-select {
            .ant-select-selection {
              border-radius: 8px;
              border-color: @apple-gray-300;
              transition: all 0.3s;
              
              &:hover {
                border-color: @apple-blue;
              }
            }
            
            &.ant-select-focused .ant-select-selection {
              border-color: @apple-blue;
              box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
            }
          }
          
          .ant-checkbox-wrapper {
            .ant-checkbox {
              .ant-checkbox-inner {
                border-radius: 4px;
                border-color: @apple-gray-300;
                transition: all 0.3s;
                
                &:hover {
                  border-color: @apple-blue;
                }
              }
              
              &.ant-checkbox-checked .ant-checkbox-inner {
                background-color: @apple-blue;
                border-color: @apple-blue;
              }
            }
          }
        }
      }
    }
  }
}

// 动画效果
@keyframes fadeIn {
  from {
    opacity: 0;
    transform: translateY(20px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

@keyframes fadeOut {
  from {
    opacity: 1;
    transform: translateY(0);
  }
  to {
    opacity: 0;
    transform: translateY(20px);
  }
}

// Kubernetes相关样式
.kubernetes-config-section {
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

.kubernetes-subgroup-en {
  color: #E6A23C; /* 浅橙色 */
  font-size: 0.9em;   /* 辅助字体稍小 */
  font-weight: normal; /* 非粗体 */
  margin-left: 4px;    /* 括号前的空格 */
}

.group-kubernetes-section {
  background-color: #f9fafc;
  border-radius: 8px;
  border: 1px solid #e8eaf1;
  margin-bottom: 24px;
  padding: 16px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05);
}

.template-content-container {
  margin-top: 16px;
  padding: 16px;
  background: #fafafa;
  border-radius: 4px;
}

.template-content-title {
  margin-bottom: 8px;
  font-weight: 500;
  color: rgba(0, 0, 0, 0.85);
}

.template-content-textarea {
  background: #fff;
}

.config-collapse {
  margin-bottom: 16px;
}

.config-panel {
  border: 1px solid #ebeef5;
  border-radius: 4px;
  margin-bottom: 16px;
  background-color: #fff;
}

.panel-header-text {
  font-weight: 500;
  font-size: 15px;
  color: #333;
}

.panel-content {
  padding: 16px;
}

.bottom-spacer {
  height: 40px;
}

.header-actions {
  display: flex;
  gap: 12px;
}

.toggle-button {
  background-color: #f5f5f7;
  color: @apple-text;
  border: 1px solid @apple-gray-300;
  
  &:hover {
    background-color: darken(#f5f5f7, 2%);
    color: @apple-blue;
    border-color: @apple-blue;
  }
  
  .anticon {
    margin-right: 6px;
    font-size: 14px;
  }
}

.enhanced-arrow-container {
  width: 100%;
  height: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  position: relative;
  transform: translateY(10px); /* 调整箭头垂直位置 */
}

.enhanced-arrow-line {
  height: 3px;
  width: 100%;
  background-color: rgba(24, 144, 255, 0.4);
  position: relative;
  overflow: hidden;
  flex: 1;
  box-shadow: 0 0 3px rgba(24, 144, 255, 0.5);
  border-radius: 1.5px;
}

.enhanced-flow-effect {
  position: absolute;
  top: 0;
  height: 100%;
  width: 50px;
  background: linear-gradient(to right, rgba(24, 144, 255, 0), rgba(24, 144, 255, 1), rgba(24, 144, 255, 0));
  animation: enhanced-flow-animation 1s infinite linear;
  box-shadow: 0 0 15px rgba(24, 144, 255, 0.9);
  filter: blur(0.5px);
}

.enhanced-arrow-head {
  width: 0;
  height: 0;
  border-top: 8px solid transparent;
  border-bottom: 8px solid transparent;
  border-left: 12px solid rgba(24, 144, 255, 0.8);
  margin-left: 0;
  filter: drop-shadow(0 0 4px rgba(24, 144, 255, 0.9));
}

/* 修复箭头列垂直对齐问题 */
.arrow-column {
  display: flex;
  align-items: center;
  justify-content: center;
  height: 32px;
  padding: 0;
  position: relative;
}

/* 为第一行的箭头添加特殊样式，考虑标签的高度 */
.port-mapping-row:first-child .arrow-column .enhanced-arrow-container {
  transform: translateY(25px); /* 为第一行的箭头添加更多的向下偏移 */
}

/* 为其他行的箭头添加正常的垂直居中样式 */
.port-mapping-row:not(:first-child) .arrow-column .enhanced-arrow-container {
  transform: translateY(10px);
}

/* 添加特定于端口映射的样式 */
.port-mapping-row {
  display: flex;
  align-items: flex-end; /* 改为底部对齐，这样即使有标签也能对齐 */
  margin-bottom: 16px;
}

/* 确保输入框容器垂直居中 */
.port-input-left, .port-input-right {
  display: flex;
  flex-direction: column;
  justify-content: flex-end; /* 改为底部对齐 */
}

/* 确保标签不影响布局 */
.port-label-wrapper {
  margin-bottom: 5px;
  height: 20px; /* 固定标签高度 */
}

/* 修复箭头动画效果 */
@keyframes enhanced-flow-animation {
  0% {
    left: -50px;
    opacity: 0.7;
  }
  50% {
    opacity: 1;
  }
  100% {
    left: 100%;
    opacity: 0.7;
  }
}
</style> 