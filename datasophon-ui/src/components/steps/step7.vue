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
    <div class="steps-title flex-bewteen-container">
      <span>服务配置</span>
    </div>
    <a-button class="btn-save" type="primary" @click="handleSubmit">保存</a-button>
    <a-spin :spinning="loading" style="position: relative;">
      <a-tabs v-model="serviceNameKey" @change="callback" style="max-width: 1330px; position: relative;">
        <a-tab-pane v-for="item in SERVICENAMES" :key="item" :tab="item" :forceRender="true">
          <!-- <div class="mgt16 steps-body">
            <CommonTemplate :ref="'CommonTemplateRef'+item" :steps4Data="steps4Data" :templateData="templateProps(item)" />
          </div>-->
        </a-tab-pane>
      </a-tabs>
      <!-- 修改后的配置组展示区域 -->
      <div
          v-for="item in SERVICENAMES"
          :key="item"
          :class="['steps-body', serviceNameKey === item ? 'steps-container' : '']"
      >
        <div
            v-if="serviceNameKey === item"
            class="config-group-container"
        >
          <div
              v-for="(group, groupName) in groupedTemplateData[item]"
              :key="groupName"
              class="config-group"
          >
            <h3 class="group-title" @click="toggleGroup(item, groupName)">
              {{ groupName }}
              <span class="arrow" :class="{ 'arrow-up': isGroupExpanded[item]?.[groupName] }">▶</span>
            </h3>
            <div v-show="isGroupExpanded[item]?.[groupName]">
              <CommonTemplate
                  :ref="`CommonTemplateRef_${item}_${groupName}`"
                  :steps4Data="steps4Data"
                  :templateData="group"
              />
            </div>
          </div>
        </div>
      </div>
    </a-spin>
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
    templateProps(item) {
      return this.templateObj[item];
    },
    ...mapActions("steps", ["setCommandType", "setCommandIds"]),
    callback(key) {
      this.serviceNameKey = key;
      if (this.selectKeys.includes(key)) return false;
      this.selectKeys.push(key);
      // this.getServiceConfigOption();
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
      this.loading = true;
      this.SERVICENAMES.forEach(serviceName => {
        const params = {
          clusterId: this.setting.clusterId || this.clusterId,
          serviceName: serviceName,
        };

        this.$axiosPost(global.API.getServiceConfigOption, params).then(res => {
          if (res.code === 200) {
            // 处理对象结构数据
            const configGroups = res.data || {};

            // 将对象转换为配置项数组
            const allConfigs = [];
            Object.keys(configGroups).forEach(groupName => {
              const groupConfigs = Array.isArray(configGroups[groupName])
                  ? configGroups[groupName]
                  : [];
              allConfigs.push(...groupConfigs.map(item => ({
                ...item,
                configGroup: groupName // 保留分组信息
              })));
            });

            this.$set(this.groupedTemplateData, serviceName,
                this.handlerTemplate(serviceName, allConfigs)
            );
            this.templateObj[serviceName] = allConfigs;
          }
          this.loading = false;
        }).catch(error => {
          console.error('API请求失败:', error);
          this.loading = false;
        });
      });
    },
    handlerTemplate(serviceName, data) {
      // 数据校验加强版
      const validData = (Array.isArray(data) ? data : [])
          .filter(item => {
            const isValid = item &&
                typeof item === 'object' &&
                'name' in item &&
                'configGroup' in item;
            if (!isValid) {
              console.warn('Invalid config item:', item);
            }
            return isValid;
          })
          .map(item => ({
            ...item,
            name: (item.name || '').toString(),
            configGroup: (item.configGroup || 'CommonConfig').toString()
          }));

      // 分组处理
      const groupedData = _.groupBy(validData, item => {
        return item.configGroup
            .replace(/^"+|"+$/g, '') // 去除首尾引号
            .trim() || 'CommonConfig';
      });

      // 初始化分组状态
      this.$set(this.isGroupExpanded, serviceName, {});
      Object.keys(groupedData).forEach(groupName => {
        this.$set(this.isGroupExpanded[serviceName], groupName, false); // 默认展开
      });

      return groupedData;
    },
    // 新增分组切换方法
    toggleGroup(serviceName, groupName) {
      this.$set(this.isGroupExpanded[serviceName], groupName,
          !this.isGroupExpanded[serviceName][groupName]
      );
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

      // 1. 生成所有服务的Promise数组
      const promises = this.SERVICENAMES.map(serviceName =>
          new Promise((resolve) => {
            // 2. 定义异步处理函数
            const processService = async () => {
              try {
                // 3. 收集所有分组的表单数据
                const allFormData = {};
                const groups = self.groupedTemplateData[serviceName] || {};

                // 遍历每个配置组
                for (const groupName of Object.keys(groups)) {
                  const refName = `CommonTemplateRef_${serviceName}_${groupName}`;
                  const formRef = self.$refs[refName]?.[0];

                  // 4. 校验分组表单是否存在
                  if (!formRef) {
                    console.warn(`[${serviceName}] 缺失表单组件: ${refName}`);
                    continue;
                  }

                  // 5. 执行表单校验
                  await formRef.form.validateFields();
                  Object.assign(allFormData, formRef.form.getFieldsValue());
                }

                // 6. 处理复合数据结构
                const mergedData = {
                  ...allFormData,
                  ...this.handlearrayWithData(allFormData),
                  ...this.handleMultipleData(allFormData)
                };

                // 7. 构建提交参数
                const param = (this.templateObj[serviceName] || []).map(item => ({
                  ...item,
                  value: mergedData[item.name.replace(/\./g, "!")] ?? item.value
                }));

                // 8. 过滤有效参数
                const filterParam = param.filter(
                    item => !(!item.required && item.hidden)
                );

                // 9. 提交保存
                const res = await this.$axiosPost(global.API.saveServiceConfig, {
                  clusterId: this.setting.clusterId || this.clusterId,
                  serviceName,
                  serviceConfig: JSON.stringify(filterParam)
                });

                resolve({...res, name: serviceName});
              } catch (error) {
                // 10. 统一错误处理
                console.error(`[${serviceName}] 配置保存失败:`, error);
                resolve({
                  code: 500,
                  name: serviceName,
                  msg: error.message || error.msg
                });
              }
            };

            // 11. 执行异步处理
            processService().catch(error =>
                resolve({code: 500, name: serviceName, msg: error.message})
            );
          })
      );

      // 12. 处理所有结果
      Promise.all(promises).then(async (results) => {
        const failedServices = results.filter(r => r.code !== 200);

        // 13. 处理失败项
        if (failedServices.length > 0) {
          failedServices.forEach(({name, msg}) =>
              this.$message.error(`${name} 配置保存失败: ${msg}`)
          );
          callback?.({code: 500});
          return;
        }

        // 14. 后续流程处理
        try {
          const params = {
            clusterId: this.setting.clusterId || this.clusterId,
            serviceNames: this.SERVICENAMES,
            commandType: this.steps.commandType
          };

          // 15. 生成执行命令
          const genCmdRes = await this.$axiosPost(global.API.generateCommand, params);
          this.setCommandIds(genCmdRes.data);

          // 16. 启动执行
          const execRes = await this.$axiosPost(global.API.startExecuteCommand, {
            ...params,
            commandIds: genCmdRes.data
          });

          callback?.(execRes);
        } catch (error) {
          console.error('命令执行流程失败:', error);
          callback?.({code: 500});
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
    this.SERVICENAMES = this.steps4Data.serviceNames.map(
        (item) => item.serviceName
    );
    this.serviceNameKey = this.SERVICENAMES[0];
    this.selectKeys.push(this.serviceNameKey);
    this.SERVICENAMES.map((item) => {
      this.templateObj[`${item}`] = [];
    });
  },
  mounted() {
    this.getServiceConfigOption();
  },
};
</script>
<style lang="less" scoped>

.config-group-container {
  padding: 16px 0 !important;  /* 垂直间距保持，去除水平留白 */
  background: transparent !important;  /* 移除容器背景色 */
}

.service-title {
  padding: 0 24px;  /* 保持水平的 padding，但去除底部的间隙 */
  border-bottom: 1px solid #e9ecef;  /* 底部线条 */

  h2 {
    font: 500 16px/24px "Helvetica Neue", sans-serif;
    color: #343a40;
    margin: 0;  /* 移除外边距 */
    padding: 0;  /* 去除内边距，确保没有多余的间隙 */
    line-height: 24px;  /* 保持标题行高一致 */
  }

  .sub-title {
    font: 13px/20px "PingFang SC";
    color: #868e96;
    letter-spacing: 0.5px;
  }
}

.config-group {
  margin-bottom: 0px;  /* 调整配置项之间的间距 */
  background: #ffffff;
  padding: 0px;
  overflow: hidden;  /* 防止出现滚动条 */

  .group-title {
    background: #F7F9FC;
    padding: 10px;
    color: #303133;
    font-size: 14px;
    font-weight: 500;
    border-radius: 6px;
    cursor: pointer;
    display: flex;
    justify-content: space-between;
    align-items: center;
    position: relative;
    transition: background 0.3s ease, transform 0.3s ease;
    left: 16px;

    /* 左边垂直条 */
    &::before {
      content: "";
      position: absolute;
      left: 0;
      top: 50%;
      transform: translateY(-50%);
      width: 3px;
      height: 18px;
      background: #1890ff;
      z-index: 2;
    }

    /* 默认状态下 z-index 设置为 1 */
    z-index: 1;

    &:hover {
      background: #E1E9F7;  /* 更柔和的背景色 */
      transform: scale(1.02);  /* 悬停时放大 */
      z-index: 2;  /* 增加 z-index 以确保它显示在最上面 */
    }

    &.active {
      background: #E0F7FF;  /* 激活状态时的柔和蓝色背景 */
    }

    .arrow {
      position: absolute !important;
      right: 40px;  /* 修改这里：箭头往左移动24px */
      top: 50%;
      transform: translateY(-50%) rotate(0deg);
      transition: transform 0.3s ease;
      z-index: 3;  /* 提升箭头的 z-index，确保它在最上层 */
      color: #909399;
      &.arrow-up {
        transform: translateY(-50%) rotate(90deg);  /* 点击后箭头旋转 */
      }
    }
  }
}

.steps7 {
  .ant-tabs {
    /deep/ .ant-tabs-nav {
      padding: 0 24px !important;

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
  }

  .steps-body {
    border-radius: 8px !important;
    margin: 0 24px !important;
  }

  .btn-save {
    position: absolute;
    right: 32px;
    z-index: 1000;
  }
}

</style>


