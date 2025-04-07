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


 * @describe: step5-分配服务Master角色 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:09
 * @FilePath: \ddh-ui\src\components\steps\step5.vue
-->
<template>
  <div class="steps5">
    <div class="hero-section">
      <h1 class="hero-title">服务部署</h1>
      <p class="hero-subtitle">配置服务部署方案，为每个服务角色选择合适的主机</p>
    </div>
    
    <div class="service-config-container">
      <a-tabs v-model="activeKey" @change="callback" animated class="apple-tabs">
        <a-tab-pane :key="item.serviceName" v-for="(item) in serviceNames">
          <span slot="tab" class="tab-title">{{ item.serviceName }}</span>
          <a-form-model
            ref="ruleForm"
            :model="formState"
            :rules="rules"
            v-bind="layout"
          >
            <a-form-model-item>
              <div class="role-host-container">
                <a-table
                  :columns="columns"
                  :data-source="tableData"
                  :pagination="false"
                  class="apple-table"
                >
                  <template #bodyCell="{ column, text, record }">
                    <template v-if="column.dataIndex === 'action'">
                      <a-select
                        class="apple-select"
                        style="width: 100%"
                        mode="multiple"
                        placeholder="请选择主机"
                        option-filter-prop="children"
                        @change="e => handleChange(e, record.id)"
                        :value="record.hostname ? record.hostname.split(',') : []"
                      >
                        <a-select-option
                          v-for="item in hostList"
                          :key="item.hostname"
                          :value="item.hostname"
                        >
                          {{ item.hostname }}({{ item.ip }})
                        </a-select-option>
                      </a-select>
                    </template>
                  </template>
                </a-table>
              </div>
            </a-form-model-item>
          </a-form-model>
        </a-tab-pane>
      </a-tabs>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps4Data: Object,
  },
  data() {
    return {
      activeKey: "",
      serviceNames: [],
      tableData: [],
      formState: {},
      layout: {
        labelCol: { span: 4 },
        wrapperCol: { span: 14 },
      },
      rules: {},
      columns: [
        {
          title: "角色",
          dataIndex: "name",
          key: "name",
        },
        {
          title: "描述",
          dataIndex: "desc",
          key: "desc",
        },
        {
          title: "主机",
          dataIndex: "action",
          key: "action",
        },
      ],
      hostList: [],
    };
  },
  methods: {
    callback(key) {
      this.getRoleHostList(key);
    },
    getRoleHostList(serviceName) {
      const params = {
        clusterId: this.clusterId,
        serviceId: this.serviceNames.filter(
          (item) => item.serviceName === serviceName
        )[0].serviceId,
      };
      this.$axiosPost(global.API.getRoleHostList, params).then((res) => {
        if (res.code === 200) {
          this.tableData = res.data;
        }
      });
    },
    getHostList() {
      const params = {
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.getHostListByCluster, params).then((res) => {
        this.hostList = res.data;
      });
    },
    handleChange(e, id) {
      let obj = this.tableData.filter((item) => item.id === id)[0];
      obj.hostname = e.join(",");
    },
    handleSubmit(callback) {
      const params = {
        clusterId: this.clusterId,
        roleHostList: this.tableData,
      };
      this.$axiosPost(global.API.updateRoleHost, params).then((res) => {
        if (callback) {
          callback(res);
        }
      });
    },
  },
  mounted() {
    this.serviceNames = this.steps4Data.serviceNames;
    if (this.serviceNames[0]) {
      this.activeKey = this.serviceNames[0].serviceName;
      this.getRoleHostList(this.activeKey);
    }
    this.getHostList();
  },
};
</script>
<style lang="less" scoped>
// 苹果设计系统颜色
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;
@apple-red: #ff453a;
@apple-green: #34c759;
@apple-yellow: #ffd60a;
@apple-orange: #ff9f0a;

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps5 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
  
  .hero-section {
    text-align: center;
    margin-bottom: 2.5rem;
    position: relative;

    .hero-title {
      .apple-font();
      font-size: 2.5rem;
      font-weight: 600;
      line-height: 1.1;
      letter-spacing: -0.022em;
      color: @apple-black;
      margin-bottom: 0.8rem;
      background: linear-gradient(120deg, @apple-black, #505050);
      -webkit-background-clip: text;
      -webkit-text-fill-color: transparent;
    }

    .hero-subtitle {
      .apple-font();
      font-size: 1.2rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0 auto 1.5rem;
      max-width: 600px;
    }
  }
  
  .service-config-container {
    border-radius: 12px;
    margin: 0 auto;
    max-width: 1200px;
    overflow: hidden;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    
    // 自定义标签页样式
    :deep(.apple-tabs) {
      .apple-font();
      
      .ant-tabs-bar {
        border-bottom: 1px solid #f0f0f0;
        margin-bottom: 24px;
      }
      
      .ant-tabs-nav {
        margin-bottom: 20px;
        
        .ant-tabs-tab {
          padding: 12px 16px;
          margin-right: 24px;
          transition: all 0.3s;
          
          .tab-title {
            font-size: 15px;
            font-weight: 500;
            color: @apple-gray;
            transition: color 0.3s;
          }
          
          &:hover {
            .tab-title {
              color: darken(@apple-gray, 15%);
            }
          }
          
          &.ant-tabs-tab-active {
            .tab-title {
              color: @apple-blue;
              font-weight: 600;
            }
          }
        }
        
        .ant-tabs-ink-bar {
          background-color: @apple-blue;
          height: 3px;
          border-radius: 3px;
        }
      }
      
      .ant-tabs-content {
        padding: 0 16px;
      }
    }
    
    // 角色主机容器样式
    .role-host-container {
      background-color: @apple-white;
      border-radius: 12px;
      box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
      overflow: hidden;
    }
    
    // 表格样式
    :deep(.apple-table) {
      .apple-font();
      
      .ant-table-thead > tr > th {
        background-color: @apple-gray-light;
        font-weight: 600;
        font-size: 0.95rem;
        color: @apple-black;
        padding: 16px 20px;
        border-bottom: 1px solid rgba(0,0,0,0.05);
        white-space: nowrap;
      }
      
      .ant-table-tbody > tr > td {
        padding: 14px 20px;
        border-bottom: 1px solid rgba(0,0,0,0.03);
        transition: background-color 0.3s;
      }
      
      .ant-table-tbody > tr:hover:not(.ant-table-expanded-row) > td {
        background-color: fadeout(@apple-gray-light, 50%);
      }
    }
    
    // 下拉选择框样式
    :deep(.apple-select) {
      .apple-font();
      
      .ant-select-selection {
        border-radius: 8px;
        border: 1px solid #d9d9d9;
        transition: all 0.3s;
        
        &:hover {
          border-color: @apple-blue-hover;
        }
        
        &:focus,
        &.ant-select-focused {
          border-color: @apple-blue;
          box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
        }
      }
      
      .ant-select-selection__choice {
        background-color: fadeout(@apple-blue, 90%);
        border: 1px solid fadeout(@apple-blue, 70%);
        border-radius: 4px;
        color: @apple-blue;
        margin-top: 4px;
        margin-bottom: 4px;
        
        .ant-select-selection__choice__remove {
          color: @apple-blue;
          
          &:hover {
            color: darken(@apple-blue, 10%);
          }
        }
      }
    }
  }
}

// 动画
@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

@keyframes slideUp {
  from { transform: translateY(20px); opacity: 0; }
  to { transform: translateY(0); opacity: 1; }
}
</style>