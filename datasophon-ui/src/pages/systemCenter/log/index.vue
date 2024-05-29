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


 * @Date: 2022-06-09 10:11:22
 * @LastEditTime: 2023-03-15 17:35:00
 * @FilePath: \ddh-ui\src\pages\securityCenter\user.vue
-->

<template>
  <div class="tag-list">
    <a-card class="mgb16 card-shadow">
      <a-row type="flex" align="middle">
        <a-col :span="22">
          <a-select placeholder="操作模块" class="w252 mgr12" allowClear showSearch
            @change="(value) => getVal(value, 'operationModule')">
            <a-select-option :value="item" v-for="(item, index) in moduleList" :key="index">{{ item
            }}</a-select-option>
          </a-select>
          <a-select placeholder="服务名称" class="w252 mgr12" allowClear showSearch
            @change="(value) => getVal(value, 'serviceName')">
            <a-select-option :value="item" v-for="(item, index) in serviceNameList" :key="index">{{ item
            }}</a-select-option>
          </a-select>
          <!-- <a-input placeholder="请输入关键字" class="w252 mgr12" @change="(value) => getVal(value, 'groupName')" allowClear /> -->
          <a-button class type="primary" icon="search" @click="onSearch"></a-button>
        </a-col>
      </a-row>
    </a-card>
    <a-card class="card-shadow">
      <div class="table-info steps-body">
        <a-table @change="(pagination) => { this.tableChange(pagination) }" :columns="columns" :loading="loading"
          :dataSource="dataSource" rowKey="id" :pagination="pagination"></a-table>
      </div>
    </a-card>
  </div>
</template>

<script>
import AddTag from "./commponents/addTag.vue";
import DeleteTag from "./commponents/deleteTag.vue";
import { mapGetters, mapState, mapMutations } from "vuex";

export default {
  name: "TAG",
  data () {
    return {
      clusterId: Number(localStorage.getItem("clusterId") || -1),
      params: {},
      pagination: {
        total: 0,
        size: 10,
        current: 1,
        showSizeChanger: true,
        sizeOptions: ["10", "20", "50", "100"],
        showTotal: (total) => `共 ${total} 条`,
      },
      dataSource: [],

      moduleList: [],
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
                    this.pagination.size * (this.pagination.current - 1)
                )}
              </span>
            );
          },
        },
        { title: "操作模块", key: "operationModule", dataIndex: "operationModule" },
        { title: "操作类型", key: "operationType", dataIndex: "operationType" },
        { title: "开始时间", key: "startTime", dataIndex: "startTime" },
        { title: "结束时间", key: "endTime", dataIndex: "endTime" },
        { title: "用户", key: "operateUser", dataIndex: "operateUser" },
        { title: "服务名称", key: "serviceName", dataIndex: "serviceName" },
        { title: "操作结果", key: "returnMsg", dataIndex: "returnMsg" },

      ],
    };
  },
  computed: {
    ...mapGetters("account", ["user"]),
  },
  methods: {
    tableChange (pagination, key) {
      this.pagination.current = pagination.current;
      this.pagination.size = pagination.size
      this.getLabelList();
    },
    getVal (val, filed) {
      this.params[`${filed}`] = val;
      this.getLabelList()
    },
    //   查询
    onSearch (key) {
      this.pagination.current = 1;
      this.getLabelList();
    },
    createTag (obj, key) {
      const self = this;
      let width = 520;
      let title = JSON.stringify(obj) === "{}" ? "添加标签" : "编辑标签";
      let content = (
        <AddTag detail={obj} callBack={() => self.getLabelList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    deleteTag (obj, key) {
      const self = this;
      let width = 400;
      let content = (
        <DeleteTag
          sysTypeTxt="标签"
          detail={obj}
          callBack={() => self.getLabelList()}
        />
      );
      this.$confirm({
        width: width,
        title: () => {
          return (
            <div>
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    getLabelList () {
      this.loading = true;
      let params = {
        size: this.pagination.size,
        current: this.pagination.current,
        param: {
          ...this.params,
          operateUser: this.user.username,
        }

      };
      this.$axiosJsonPost('/ddh/api/log/list', params).then((res) => {
        this.loading = false;
        this.dataSource = res.data.records;
        this.pagination.total = res.data.length;
      });
    },
    getModuleList () {
      this.$axiosGet('/ddh/api/log/moduleList',).then((res) => {
        this.moduleList = res.data;
      });
    },
    getServiceNameList () {
      this.$axiosGet('/ddh/api/log/serviceNameList', { clusterId: this.clusterId, }).then((res) => {
        this.serviceNameList = res.data;
      });
    },
  },
  mounted () {
    this.getLabelList();
    this.getModuleList()
    this.getServiceNameList()
  },
};
</script>

<style lang="less" scoped>
.tag-list {
  background: #f5f7f8;

  .btn-opt {
    border-radius: 1px;
    font-size: 12px;
    color: #0264c8;
    letter-spacing: 0;
    font-weight: 400;
    margin: 0 5px;
  }
}
</style>
