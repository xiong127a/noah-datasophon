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
  <div class="steps4 steps">
    <div class="hero-section">
      <h1 class="hero-title">选择服务</h1>
      <p class="hero-subtitle">选择需要部署的大数据组件服务和版本</p>
    </div>
    
    <!-- 只有从集群进入(stepsType:cluster) step4才会有选择服务下拉框 同时table数据也变 -->
    <div class="select-section" v-if="stepsType == 'cluster'">
      <a-select allowClear showSearch placeholder="请选择服务类型" class="service-select" v-model="params.type"
        @change="(value) => getVal(value, 'type')">
        <a-select-option v-for="(item, index) in serveList" :key="index" :value="item">{{ item }}</a-select-option>
      </a-select>
    </div>
    
    <div class="table-info mgt16 steps-body pdr30">
      <a-table @change="tableChange" :columns="columns" :loading="loading" :pagination="false" :dataSource="dataSource"
        :rowSelection="{ selectedRowKeys: stepsType == 'cluster' ? selectedRowKeysArr : selectedRowKeys, onChange: onSelectChange, getCheckboxProps: getCheckboxProps }"
        rowKey="id"></a-table>
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
          disabled: this.depType == 'K8S' ? false : record.installed || record.isRequired //临时
        }
      }
    },
    //表格选择
    onSelectChange (selectedRowKeys, row) {
      this.selectedRowNamesArr = [] 
      this.selectedRowKeys = selectedRowKeys
      this.selectedRowKeysArr = selectedRowKeys
      // this.selectedRowKeys = this.selectedRowKeys.concat(selectedRowKeys);
      // this.selectedRowKeysArr = this.selectedRowKeysArr.concat(selectedRowKeys) ;
      let arr = [];
      row.map((item) => {
        arr.push({
          serviceName: item.serviceName,
          serviceId: item.id
        });
      });
      this.selectedRowNames = arr;
      if (this.depType == 'K8S') { //k8s模式下 配置服务只传重新勾选的serviceName
        row.forEach(e => {
          this.selectedRowNamesArr.push({
            serviceId: e.id,
            serviceName: e.serviceName
          })
        });
      }
    },
    getListWithRequired () {
      const self = this;
      this.$axiosGet('/ddh/api/frame/service/listWithRequired', { type: this.params.type || '', clusterId: this.clusterId }).then((res) => {
        this.dataSource = res.data;
        let arr = this.dataSource.filter(item => item.installed == false && item.isRequired == true)
        if (arr.length > 0) {
          arr.map(childItem => {
            if (this.depType !== 'K8S') {
              this.selectedRowKeysArr.push(childItem.id)
              this.selectedRowNamesArr.push({
                serviceId: childItem.id,
                serviceName: childItem.serviceName
              })
            }
          })
        }
        self.steps4Data.serviceIds.map(item => {
          if (this.depType !== 'K8S') {
            this.selectedRowKeysArr.push(item)
          }
        })

        self.steps4Data.serviceNames.map(item => {
          if (this.depType !== 'K8S') {
          this.selectedRowNamesArr.push({
            serviceId: item.id,
            serviceName: item.serviceName
          })
        }
        })
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

.steps4 {
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
  
  .select-section {
    margin-bottom: 24px;
    padding: 0 30px;
    
    .service-select {
      width: 252px;
    }
  }
}

@keyframes fadeIn {
  from { opacity: 0; }
  to { opacity: 1; }
}

// 保留原有样式
</style>