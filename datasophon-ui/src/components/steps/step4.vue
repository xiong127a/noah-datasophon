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


 * @describe: step4-选择服务
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-06-28 15:34:21
 * @FilePath: \ddh-ui\src\components\steps\step4.vue 
-->
<template>
  <div class="steps4">
    <div class="hero-section">
      <h1 class="hero-title">选择服务</h1>
      <p class="hero-subtitle">请选择需要部署的服务组件</p>
    </div>
    
    <div class="service-table-container">
      <a-table
        :rowSelection="{
          selectedRowKeys: selectedRowKeys,
          onChange: onSelectChange,
          getCheckboxProps: getCheckboxProps,
        }"
        :columns="columns"
        :dataSource="dataSource"
        :rowKey="depType=='K8S'? 'name': 'id'"
        :pagination="false"
        class="apple-table"
      >
        <template #bodyCell="{ column, text, record }">
          <div v-if="column.dataIndex === 'action'">
            <a
              v-if="record.installStatus !== 1"
              class="service-action-link"
              @click="showDetail(record)"
            >查看详情</a>
          </div>
        </template>
      </a-table>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps2Data: {
      type: Object,
      default: () => {},
    },
    steps1Data: {
      type: Object,
      default: () => {},
    },
    depType: {
      type: String,
      default: '',
    },
  },
  data() {
    return {
      selectedRowKeys: [],
      selectedRowKeysArr: [],
      selectedRowNames: [],
      selectedRowNamesArr: [],
      columns: [
        {
          title: "服务名称",
          dataIndex: "serviceName",
          key: "serviceName",
          width: "15%",
        },
        {
          title: "版本",
          dataIndex: "version",
          key: "version",
          width: "15%",
        },
        {
          title: "应用场景",
          dataIndex: "scene",
          key: "scene",
          width: "25%",
        },
        {
          title: "已安装",
          dataIndex: "installed",
          key: "installed",
          width: "15%",
          customRender: (text, row) => {
            const h = this.$createElement;
            
            return h('div', { class: 'installed-status' }, [
              h('span', { 
                class: text ? 'status-badge installed' : 'status-badge not-installed'
              }),
              h('span', { class: 'status-text' }, [text ? '是' : '否'])
            ]);
          },
        },
        {
          title: "操作",
          dataIndex: "action",
          key: "action",
          width: "15%",
        },
      ],
      tableHeight: 0,
      tableMarginTop: 0,
      dataSource: [],
    };
  },
  methods: {
    async loadServiceTable() {
      let data={
        clusterId: this.clusterId
      }
      if (this.depType == 'K8S') {
        data.depType = this.depType
        const response = await this.$axiosPost(
          'ddh/k8snamespace/list/service',
          data
        );

        if (response.code === 200) {
          this.dataSource = response.data.list;
          if (this.dataSource.length > 0) {
            this.dataSource = this.dataSource.map(item => {
              let obj = {};
              obj.id = item.name;
              obj.key = item.name;
              obj.version = item.tag;
              obj.serviceName = item.name;
              return obj;
            })
          }

        } else {
          this.$message.error('无法获取K8S服务列表。请稍后再试。');
        }
      } else {
        this.$axiosPost(
          'ddh/service/install/listServiceTab',
          data
        ).then((res) => {
          const response = res;
          if (response.code === 200) {
            const serviceList = response.data || [];
            this.dataSource = serviceList;
            // 检查是否有已安装服务
            const installedServices = serviceList.filter(
              (service) => service.installed
            );
            if (installedServices.length > 0) {
              // 预选已安装服务
              this.selectedRowKeys = installedServices.map(
                (service) => service.id
              );
              this.selectedRowKeysArr=installedServices.map(
                (service) => service.id
              );;
              this.selectedRowNames = installedServices.map((service) => ({
                serviceId: service.id,
                serviceName: service.serviceName,
              }));
              this.selectedRowNamesArr=installedServices.map((service) => ({
                serviceId: service.id,
                serviceName: service.serviceName,
              }));
            }
          } else {
            this.$message.error('无法获取服务列表。请稍后再试。');
          }
        });
      }
    },
    showDetail(record) {
      if (this.depType !== 'K8S') {
        if (record.installed) {
          this.$router.push({
            path: `/service-manage/service-list/${record.id}`,
          });
        } else {
          this.$message.info("该服务尚未安装");
        }
      }
    },
    onSelectChange(selectedRowKeys, selectedRows) {
      this.selectedRowKeys = selectedRowKeys;
      if (this.depType == 'K8S') {
        this.selectedRowNames = selectedRows;
      } else {
        this.selectedRowNames = selectedRows.map((row) => ({
          serviceId: row.id,
          serviceName: row.serviceName,
        }));
      }
    },
    getCheckboxProps(record) {
      return {
        props: {
          defaultValue: record.installed,
          disabled: this.depType === 'K8S' ? false : record.installed,
        },
      };
    },
  },
  mounted() {
    // 加载服务表格数据
    this.loadServiceTable();
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

.steps4 {
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
  
  .service-table-container {
    border-radius: 12px;
    margin: 0 auto;
    max-width: 1200px;
    overflow: hidden;
    animation: slideUp 0.6s ease-out;
    animation-fill-mode: both;
    animation-delay: 0.2s;
    box-shadow: 0 4px 20px rgba(0, 0, 0, 0.05);
    
    // 自定义表格样式
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
      
      // 自定义复选框样式
      .ant-checkbox-wrapper {
        .ant-checkbox {
          .ant-checkbox-inner {
            border-radius: 4px;
            border-color: #d9d9d9;
            transition: all 0.2s;
            
            &:after {
              transition: all 0.2s;
            }
          }
          
          &.ant-checkbox-checked {
            .ant-checkbox-inner {
              background-color: @apple-blue;
              border-color: @apple-blue;
            }
          }
        }
      }
    }
    
    // 已安装状态样式
    .installed-status {
      display: flex;
      align-items: center;
      
      .status-badge {
        display: inline-block;
        width: 8px;
        height: 8px;
        border-radius: 50%;
        margin-right: 8px;
        
        &.installed {
          background-color: @apple-green;
        }
        
        &.not-installed {
          background-color: @apple-gray;
        }
      }
      
      .status-text {
        font-size: 14px;
        color: @apple-black;
      }
    }
    
    // 服务操作链接样式
    .service-action-link {
      color: @apple-blue;
      font-size: 14px;
      font-weight: 500;
      transition: color 0.2s;
      
      &:hover {
        color: @apple-blue-hover;
        text-decoration: none;
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