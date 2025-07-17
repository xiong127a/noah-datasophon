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
  <div class="steps4 steps apple-style-container">
    <div class="steps-header">
      <div class="steps-title">
        <span>选择服务</span>
      </div>
      <div class="steps-subtitle">
        <span>请从下方列表中选择您需要的服务组件</span>
      </div>
    </div>
    
    <!-- 只有从集群进入(stepsType:cluster) step4才会有选择服务下拉框 同时table数据也变 -->
    <div class="filter-section" v-if="stepsType == 'cluster'">
      <a-row type="flex" align="middle">
        <a-col :span="22">
          <a-select 
            allowClear 
            showSearch 
            placeholder="请选择" 
            class="apple-select" 
            v-model="params.type"
            @change="(value) => getVal(value, 'type')"
          >
            <a-select-option v-for="(item, index) in serveList" :key="index" :value="item">{{ item }}</a-select-option>
          </a-select>
        </a-col>
      </a-row>
    </div>
    
    <div class="table-container">
      <a-table 
        @change="tableChange" 
        :columns="columns" 
        :loading="loading" 
        :pagination="false" 
        :dataSource="dataSource"
        :rowSelection="{ 
          selectedRowKeys: stepsType == 'cluster' ? selectedRowKeysArr : selectedRowKeys, 
          onChange: onSelectChange, 
          getCheckboxProps: getCheckboxProps 
        }"
        rowKey="id"
        class="apple-table"
      ></a-table>
    </div>
  </div>
</template>
<script>
export default {
  inject: ["handleCancel", "currentStepsAdd", "currentStepsSub", "clusterId"],
  props: {
    steps4Data: Object,
    stepsType: String,
    depType:String,
  },
  data () {
    return {
      params: { type: '' },
      selectedRowKeys: [],
      selectedRowKeysArr: [],
      selectedRowNames: [],
      selectedRowNamesArr: [],
      pagination: {
        total: 0,
        pageSize: 10,
        current: 1,
        showSizeChanger: true,
        pageSizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],
      serveList: ['custom', 'datalake'],
      loading: false,
      columns: [
        {
          title: "序号",
          key: "index",
          width: 70,
          customRender: (text, row, index) => {
            return (
                <span>
                {parseInt(
                    this.pagination.current === 1
                        ? index + 1
                        : index +
                        1 +
                        this.pagination.pageSize * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { title: "服务", key: "label", dataIndex: "label" },
        {
          title: "描述",
          key: "serviceDesc",
          dataIndex: "serviceDesc",
        },
        {
          title: "版本",
          key: "serviceVersion",
          dataIndex: "serviceVersion",
        },
      ],
    };
  },
  methods: {
    getVal (val, filed) {
      console.log('Service type selected:', val);
      this.params[`${filed}`] = val
      this.getListWithRequired()
    },
    tableChange (pagination) {
      this.pagination.current = pagination.current;
      this.pagination.pageSize = pagination.pageSize
      if (this.stepsType == 'cluster') {
        this.getListWithRequired()
      } else {
        this.getServiceList();
      }
    },
    getServiceList () {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      const self = this;
      // todo：这个接口地址需要替换
      this.$axiosPost(global.API.getServiceList, params).then((res) => {
        this.loading = false;
        this.dataSource = res.data;
        let arr = this.dataSource.filter(item => item.installed)
        if (arr.length > 0) {
          arr.map(childItem => {
            this.selectedRowKeys.push(childItem.id)
            this.selectedRowNames.push({
              serviceId: childItem.id,
              serviceName: childItem.serviceName
            })
          })
        }
        self.steps4Data.serviceIds.map(item => {
          this.selectedRowKeys.push(item)
        })
        self.steps4Data.serviceNames.map(item => {
          this.selectedRowNames.push({
            serviceId: item.id,
            serviceName: item.serviceName
          })
        })
      });
    },
    getCheckboxProps (record) {
      return {
        props: {
          disabled: this.depType == 'Kubernetes' ? false : record.installed || record.isRequired //临时
        }
      }
    },
    //表格选择
    onSelectChange (selectedRowKeys, row) {
      console.log('Selected rows:', row);
      this.selectedRowNamesArr = []
      this.selectedRowKeys = selectedRowKeys
      this.selectedRowKeysArr = selectedRowKeys
      let arr = [];
      row.map((item) => {
        arr.push({
          serviceName: item.serviceName,
          serviceId: item.id
        });
      });
      this.selectedRowNames = arr;
      if (this.depType == 'Kubernetes') { //Kubernetes模式下 配置服务只传重新勾选的serviceName
        row.forEach(e => {
          this.selectedRowNamesArr.push({
            serviceId: e.id,
            serviceName: e.serviceName
          })
        });
      } else {
        // 非Kubernetes模式下,确保选中的服务被正确记录
        this.selectedRowNamesArr = arr;
      }
      
      // 同步到steps4Data
      this.steps4Data.serviceIds = [...new Set(this.selectedRowKeysArr)];
      this.steps4Data.serviceNames = this.selectedRowNamesArr;
      
      console.log('Updated steps4Data:', {
        serviceIds: this.steps4Data.serviceIds,
        serviceNames: this.steps4Data.serviceNames
      });
    },
    getListWithRequired () {
      const self = this;
      console.log('Getting service list with type:', this.params.type);
      this.$axiosGet('/ddh/api/frame/service/listWithRequired', { 
        type: this.params.type || '', 
        clusterId: this.clusterId 
      }).then((res) => {
        console.log('Service list response:', res);
        this.dataSource = res.data;
        let arr = this.dataSource.filter(item => item.installed == false && item.isRequired == true)
        if (arr.length > 0) {
          arr.map(childItem => {
            if (this.depType !== 'Kubernetes') {
              this.selectedRowKeysArr.push(childItem.id)
              this.selectedRowNamesArr.push({
                serviceId: childItem.id,
                serviceName: childItem.serviceName
              })
            }
          })
        }

        // 确保之前选中的服务保持选中状态
        self.steps4Data.serviceIds.map(item => {
          if (this.depType !== 'Kubernetes' && !this.selectedRowKeysArr.includes(item)) {
            this.selectedRowKeysArr.push(item)
          }
        })

        self.steps4Data.serviceNames.map(item => {
          if (this.depType !== 'Kubernetes' && !this.selectedRowNamesArr.some(x => x.serviceId === item.serviceId)) {
            this.selectedRowNamesArr.push({
              serviceId: item.serviceId,
              serviceName: item.serviceName
            })
          }
        })

        // 更新steps4Data以确保数据同步
        self.steps4Data.serviceIds = [...new Set(this.selectedRowKeysArr)];
        self.steps4Data.serviceNames = this.selectedRowNamesArr;
        
        // 打印最终的数据
        console.log('Final steps4Data:', {
          serviceIds: self.steps4Data.serviceIds,
          serviceNames: self.steps4Data.serviceNames
        });
      });
    },
  },
  mounted () {
    if (this.stepsType == 'cluster') {
      this.getListWithRequired()
    } else {
      this.getServiceList();
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
}

.steps-header {
  padding: 0 0 24px 0;
  border-bottom: 1px solid @apple-border;
  margin-bottom: 24px;
  
  .steps-title {
    font-size: 24px;
    font-weight: 600;
    margin-bottom: 8px;
    color: @apple-text;
  }
  
  .steps-subtitle {
    font-size: 16px;
    color: @apple-gray-500;
  }
}

.filter-section {
  margin-bottom: 24px;
  
  .apple-select {
    width: 220px;
    
    :deep(.ant-select-selection) {
      border-radius: 8px;
      border-color: @apple-gray-300;
      transition: all 0.3s;
      height: 40px;
      
      &:hover {
        border-color: @apple-blue;
      }
      
      .ant-select-selection__rendered {
        line-height: 38px;
        margin-left: 12px;
      }
    }
    
    :deep(.ant-select-focused .ant-select-selection),
    :deep(.ant-select-selection:focus),
    :deep(.ant-select-selection:active) {
      border-color: @apple-blue;
      box-shadow: 0 0 0 2px rgba(0, 113, 227, 0.2);
    }
  }
}

.table-container {
  margin-top: 12px;
  
  .apple-table {
    border-radius: 12px;
    overflow: hidden;
    box-shadow: 0 4px 16px rgba(0, 0, 0, 0.04);
    
    :deep(.ant-table-thead > tr > th) {
      background-color: @apple-gray-100;
      color: @apple-text;
      font-weight: 500;
      border-bottom: 1px solid @apple-border;
      padding: 16px 12px;
      
      &:first-child {
        border-top-left-radius: 12px;
      }
      
      &:last-child {
        border-top-right-radius: 12px;
      }
    }
    
    :deep(.ant-table-tbody > tr > td) {
      border-bottom: 1px solid @apple-border;
      padding: 14px 12px;
      transition: background 0.3s;
    }
    
    :deep(.ant-table-tbody > tr) {
      &:hover > td {
        background-color: @apple-gray-100;
      }
      
      &:last-child > td {
        border-bottom: none;
        
        &:first-child {
          border-bottom-left-radius: 12px;
        }
        
        &:last-child {
          border-bottom-right-radius: 12px;
        }
      }
    }
    
    // 选择框样式
    :deep(.ant-checkbox-wrapper) {
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
    
    // 加载状态
    :deep(.ant-spin) {
      color: @apple-blue;
    }
  }
}

.edit {
  display: flex;
  justify-content: space-between;
}
</style>