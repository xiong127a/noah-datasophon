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


 * @describe: step6-分配服务Worker与Client角色 
 * @Date: 2022-06-13 16:35:02
 * @LastEditTime: 2022-08-15 14:07:14
 * @FilePath: \ddh-ui\src\components\steps\step6.vue
-->
<template>
  <div class="steps6 steps apple-style-container">
    <div class="steps-header">
      <div class="steps-title">
        <span>分配服务Worker与Client角色</span>
      </div>
      <div class="steps-subtitle">
        <span>请为每个服务的Worker和Client角色选择部署的主机</span>
      </div>
    </div>
    
    <div class="table-container">
      <a-table 
        :pagination="false" 
        :columns="columns" 
        :loading="loading" 
        rowKey="id" 
        :dataSource="dataSource"
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
  },
  data() {
    return {
      selectedRowKeys: [],
      pagination: {
        total: 0,
        pageSize: 100,
        current: 1,
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],
      loading: false,
      hostNamesList: [],
      hostList: [],
      workNameList: [],
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
        { title: "主机名", key: "hostname", dataIndex: "hostname" }
      ],
    };
  },
  methods: {
    async handleSubmit(callback) {
      const self = this
      // 处理表单数据 将相同的key处理成数组
      let formData = {};
      let saveParam = [];
      self.workNameList.map(item => {
        formData[`${item}`] = []
        self.dataSource.map(childItem => {
          if (childItem.checkedList.includes(item)) {
            formData[`${item}`].push(childItem.hostname)
          }
        })
      })
      for (var label in formData) {
        saveParam.push({
          serviceRole: label,
          hosts: formData[label],
        });
      }
      // 等待网络请求结束
      let res = await this.$axiosJsonPost(
          global.API.saveServiceRoleHostMapping + `/${this.clusterId}`,
          saveParam
      );
      // 网络请求结束后才执行下边的语句  如果传入的callback方法为空或者没传内容也不会去执行，这样也不会影响此方法在别处的调用
      if (callback) {
        callback(res);
      }
    },
    getNonMasterRoleList() {
      const self = this;
      const params = {
        clusterId: this.clusterId,
        serviceIds: this.steps4Data.serviceIds.join(",") || "",
        // serviceIds: "6,7,8,9",
      };
      this.$axiosPost(global.API.getNonMasterRoleList, params).then((res) => {
        let arr = [];
        res.data.map((item) => {
          arr.push(item.serviceRoleName);
        });
        this.workNameList = arr;
        self.tableHeaderData = res.data;
        res.data.map((item) => {
          this.columns.push({
            title: (text, row, index) => {
              return (
                  <div class="column-header-with-checkbox">
                    <a-checkbox
                        class="column-header-checkbox"
                        checked={this.getAllCheckedStatus(item.serviceRoleName)}
                        indeterminate={this.getCheckedStatus(item.serviceRoleName)}
                        onChange={() => this.changeheaderHost(item.serviceRoleName)}
                    />
                    <span class="column-header-text">{item.serviceRoleName}</span>
                  </div>
              );
            },
            key: item.serviceRoleName,
            dataIndex: item.serviceRoleName,
            customRender: (text, row, index) => {
              return (
                  <div class="centered-checkbox">
                    <a-checkbox
                        class="apple-checkbox"
                        checked={row[`${item.serviceRoleName}`]}
                        onChange={() =>
                            this.changeHost(row, index, item.serviceRoleName)
                        }
                    ></a-checkbox>
                  </div>
              );
            },
          });
        });
        this.hostList.map((item) => {
          let obj = {};
          res.data.map((keyItem) => {
            let flag = keyItem.hosts.includes(item.hostname)
            obj[`${keyItem.serviceRoleName}`] = flag;
            if (flag) obj.checkedList = [keyItem.serviceRoleName]
          });
          this.dataSource.push({
            isChildSelected: false,
            isAllSelected: false,
            checkedList: [],
            // DataNode: true,
            hostname: item.hostname,
            id: item.id,
            ...obj,
          });
        });
        self.loading = false;
      });
    },
    getAllHost() {
      this.loading = true;
      const params = {
        clusterId: this.clusterId,
      };
      this.$axiosPost(global.API.getAllHost, params).then((res) => {
        let arr = [];
        res.data.map((item) => {
          arr.push(item.hostname);
        });
        this.hostList = res.data;
        this.hostNamesList = arr;
        this.getNonMasterRoleList();
      });
    },
    changeHost(row, index, key) {
      let arr = this.dataSource;
      const item = arr[index];
      const hostIndex = item.checkedList.findIndex((item) => item === key);
      if (hostIndex !== -1) {
        item.checkedList.splice(hostIndex, 1);
      } else {
        item.checkedList.push(key);
      }
      item[`${key}`] = !item[`${key}`];
      this.dataSource = arr;
    },
    changeheaderHost(key) {
      let num = 0;
      this.dataSource.map((item) => {
        if (item[`${key}`]) num++;
      });
      this.dataSource.forEach((item) => {
        const hostIndex = item.checkedList.findIndex((item) => item === key);
        // 没有全选的时候，让他全选
        if (num < this.dataSource.length) {
          if (hostIndex === -1) {
            item.checkedList.push(key);
          }
          item[`${key}`] = true;
        } else {
          // 取消取消操作
          if (hostIndex !== -1) {
            item.checkedList.splice(hostIndex, 1);
          }
          item[`${key}`] = false;
        }
      });
    },
    // 获取的半选状态
    getCheckedStatus(key) {
      let num = 0;
      this.dataSource.map((item) => {
        if (item[`${key}`]) num++;
      });
      return num > 0 && num < this.dataSource.length;
    },
    getAllCheckedStatus(key) {
      let num = 0;
      this.dataSource.map((item) => {
        if (item[`${key}`]) num++;
      });
      return num === this.dataSource.length;
    },
  },
  mounted() {
    this.getAllHost();
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
  }
}

// 表头中的复选框样式
.column-header-with-checkbox {
  display: flex;
  align-items: center;
  
  .column-header-checkbox {
    margin-right: 8px;
    
    :deep(.ant-checkbox) {
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
      
      &.ant-checkbox-indeterminate .ant-checkbox-inner::after {
        background-color: @apple-blue;
      }
    }
  }
  
  .column-header-text {
    font-weight: 500;
    color: @apple-text;
  }
}

// 表格中的复选框样式
.centered-checkbox {
  display: flex;
  justify-content: center;
  
  .apple-checkbox {
    :deep(.ant-checkbox) {
      .ant-checkbox-inner {
        border-radius: 4px;
        border-color: @apple-gray-300;
        transition: all 0.3s;
        width: 18px;
        height: 18px;
        
        &:hover {
          border-color: @apple-blue;
        }
      }
      
      &.ant-checkbox-checked .ant-checkbox-inner {
        background-color: @apple-blue;
        border-color: @apple-blue;
        
        &::after {
          transform: rotate(45deg) scale(1) translate(-50%, -60%);
        }
      }
    }
  }
}

// 加载状态
:deep(.ant-spin) {
  .ant-spin-dot {
    .ant-spin-dot-item {
      background-color: @apple-blue;
    }
  }
  
  .ant-spin-text {
    color: @apple-blue;
  }
}
</style>