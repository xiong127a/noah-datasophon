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
  <div class="steps6 steps">
    <div class="hero-section">
      <h1 class="hero-title">服务配置</h1>
      <p class="hero-subtitle">配置集群各组件参数，优化系统运行性能</p>
    </div>
    
    <div class="table-info mgt16 steps-body">
      <a-table :pagination="false" :columns="columns" :loading="loading" rowKey="id" :dataSource="dataSource"></a-table>
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
                <div>
                  <a-checkbox
                    class="mgr12"
                    checked={this.getAllCheckedStatus(item.serviceRoleName)}
                    indeterminate={this.getCheckedStatus(item.serviceRoleName)}
                    onChange={() => this.changeheaderHost(item.serviceRoleName)}
                  />
                  {item.serviceRoleName}
                </div>
              );
            },
            key: item.serviceRoleName,
            dataIndex: item.serviceRoleName,
            customRender: (text, row, index) => {
              return (
                <div>
                  <a-checkbox
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
// 添加苹果设计系统颜色和字体定义
@apple-white: #ffffff;
@apple-black: #1d1d1f;
@apple-gray-light: #f5f5f7;
@apple-gray: #86868b;
@apple-blue: #0071e3;
@apple-blue-hover: #147CE5;

// 苹果设计系统字体
.apple-font() {
  font-family: "SF Pro Display", "SF Pro Icons", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  -webkit-font-smoothing: antialiased;
}

.steps6 {
  margin: 0;
  max-width: 100%;
  background-color: @apple-white;
  overflow: hidden;
  animation: fadeIn 0.8s ease-out;
  
  .hero-section {
    text-align: center;
    margin-bottom: 3.5rem;
    
    .hero-title {
      .apple-font();
      font-size: 2.8rem;
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
      font-size: 1.4rem;
      line-height: 1.4;
      letter-spacing: 0;
      font-weight: 400;
      color: @apple-gray;
      margin: 0;
      max-width: 760px;
      margin: 0 auto;
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}
</style>