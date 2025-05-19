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
  <div class="steps7 steps">
    <div class="service-config-container">
    <div class="steps-title flex-bewteen-container" style="margin-top: -5px; margin-bottom: 15px;">
      <span>服务配置</span>
    </div>
      
    <a-button class="btn-save" type="primary" @click="handleSubmit" style="z-index: 1000; margin-top: 5px;">保存</a-button>
      
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
                <!-- 使用expandIconPosition="right"确保图标在右侧 -->
                <a-collapse 
                  :bordered="false"
                  expandIconPosition="right"
                  :defaultActiveKey="getActiveKeys(item)"
                  class="config-collapse"
                >
                  <a-collapse-panel 
                    v-for="(group, groupName, index) in groupedTemplateData[item]"
              :key="groupName"
                    :showArrow="true"
                    :forceRender="true"
                    :class="['config-panel', isLastGroup(item, index) ? 'last-group' : '']"
                  >
                    <template slot="header">
                      <span class="panel-header-text">{{ convertGroupName(groupName) }}</span>
                    </template>
                    
                    <div class="panel-content">
              <CommonTemplate
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
                          readonly
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
    <!-- 移除多余的底部填充区域 -->
  </div>
</template>
<script>
import CommonTemplate from "@/components/commonTemplate/index";
import {mapActions, mapState} from "vuex";
import {de} from "date-fns/locale";

export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  components: {CommonTemplate},
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
      // serviceContainerHeight: 0,
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
  },
  methods: {
    // 判断是否为最后一个分组
    isLastGroup(serviceName, index) {
      const groups = this.groupedTemplateData[serviceName] || {};
      const groupKeys = Object.keys(groups);
      return index === groupKeys.length - 1;
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
          await Promise.all(
              Object.keys(this.groupedTemplateData[currentService]).map(async (groupName) => {
                const refName = `CommonTemplateRef_${currentService}_${groupName}`;
                const formRef = this.$refs[refName]?.[0];
                if (formRef) {
                  await formRef.form.validateFields();
                  Object.assign(allFormData, formRef.form.getFieldsValue());
                }
              })
          );
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
          // 直接使用API返回的分组结构
          this.$set(this.groupedTemplateData, currentService, 
              this.handlerTemplate(currentService, res.data || {})
          );
          
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
      // 直接处理API返回的分组对象
      const processedGroups = {};
      
      // 处理每个配置组
      Object.entries(configGroups).forEach(([groupName, configs]) => {
        if (!Array.isArray(configs)) {
          return; // 跳过非数组的值
        }
        
        // 处理每个配置项
        const processedConfigs = configs.map(item => {
            let value = item.value;

          // 转换开关和布尔类型
            if (item.type === 'switch' || item.type === 'boolean') {
              value = String(value).toLowerCase() === 'true';
            }

            return {
              ...item,
            value,
            name: (item.name || '').toString().replaceAll(".", "!") // 替换名称中的点为感叹号
            };
          });
        
        // 检查是否有配置项包含模板内容
        const configWithTemplate = processedConfigs.find(item => item.templateContent && item.templateContent.trim() !== '');
        
        // 保存处理后的配置组，并附加模板信息（如果有）
        processedGroups[groupName] = {
          items: processedConfigs,
          // 如果找到了带模板的配置项，则保存模板信息
          displayName: configWithTemplate?.displayName || '',
          templateContent: configWithTemplate?.templateContent || ''
        };
      });

      // 初始化分组展开状态
      this.$set(this.isGroupExpanded, serviceName, {});
      Object.keys(processedGroups).forEach(groupName => {
        this.$set(this.isGroupExpanded[serviceName], groupName, true);
      });
      
      return processedGroups;
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
    checkAllForm() {
      const self = this;
      let hasError = false;

      // 遍历所有服务
      for (const serviceName of self.SERVICENAMES) {
        // 获取该服务的所有配置组
        const groups = self.groupedTemplateData[serviceName] || {};

        // 遍历每个配置组
        for (const groupName of Object.keys(groups)) {
          // 生成正确的 Ref 名称
          const refName = `CommonTemplateRef_${serviceName}_${groupName}`;
          const formComponent = self.$refs[refName]?.[0];

          if (!formComponent) {
            console.warn(`找不到表单组件: ${refName}`);
            continue;
          }

          // 执行表单校验
          formComponent.form.validateFields((err) => {
            if (err) {
              hasError = true;
              self.serviceNameKey = serviceName; // 切换到错误页签
            }
          });

          if (hasError) break;
        }

        if (hasError) break;
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
                  // 动态生成表单组件的引用名
                  const refName = `CommonTemplateRef_${serviceName}_${groupName}`;

                  const formRef = self.$refs[refName]?.[0]; // 获取表单组件的引用

                  // 如果找不到表单组件，输出警告并跳过该组
                  if (!formRef) {
                    console.warn(`[${serviceName}] 缺失表单组件: ${refName}`);
                    continue;
                  }

                  // 验证表单数据并收集字段值
                  await formRef.form.validateFields();  // 表单验证
                  const rawData = formRef.form.getFieldsValue(); // 获取表单字段值

                  // 处理字段名（替换"."为"!"）
                  const convertedData = Object.keys(rawData).reduce((acc, key) => {
                    const newKey = key.replace(/\./g, '!'); // 关键转换逻辑
                    acc[newKey] = rawData[key];
                    return acc;
                  }, {});

                  // 合并所有表单数据
                  Object.assign(allFormData, convertedData);
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
      });
    } else {
      this.$message.warning("未选择任何服务，请返回步骤4选择服务");
      this.SERVICENAMES = [];
      this.serviceNameKey = '';
    }
  },
  mounted() {
    this.getServiceConfigOption();
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
  overflow-x: hidden;
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
  overflow: visible;
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
  overflow: visible;
  
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
    overflow: visible;
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
  
  .ant-form-item-label {
    max-width: 30%;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
  }
  
  .ant-form-item-control-wrapper {
    max-width: 70%;
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
</style>


