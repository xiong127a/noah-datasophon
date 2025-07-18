<!--
/*
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
 */
-->
<template>
  <div class="overview-page">
    <div v-if="!dashboardUrl" class="loading-container">
      <a-spin size="large" />
      <p>正在加载大数据基础平台...</p>
    </div>
    <OverViewComponent v-else :dashboardUrl="dashboardUrl" />
  </div>
</template>

<script>
const OverViewComponent = () => import("@/components/overview");
import { mapState } from "vuex";
import API from "@/api/httpApi";

export default {
  name: "datasophonOverview",
  components: { OverViewComponent },
  data() {
    return {
      reloadIframe: false,
      iframeUrl: "",
      dashboardUrl: "",
    };
  },
  mounted() {
    console.log("Datasophon总览组件已挂载");
    this.getDatasophonDashboard();
  },
  methods: {
    getDatasophonDashboard() {
      const clusterId = localStorage.getItem('clusterId');
      console.log("开始获取Datasophon总览，clusterId:", clusterId);
      
      // 检查API是否正确导入
      if (!API || !API.getDatasophonDashboardUrl) {
        console.error("API对象或getDatasophonDashboardUrl未定义");
        this.$message.error("API接口未正确加载，请刷新页面重试");
        return;
      }
      
      console.log("API地址:", API.getDatasophonDashboardUrl);
      
      this.$axiosPost(API.getDatasophonDashboardUrl, {
        clusterId: clusterId,
      }).then(res => {
        console.log("API响应:", res);
        if (res.code === 200) {
          this.dashboardUrl = res.data;
          console.log("获取Datasophon总览成功:", this.dashboardUrl);
        } else {
          this.$message.error("获取Datasophon总览失败: " + res.msg);
        }
      }).catch(err => {
        this.$message.error("获取Datasophon总览数据失败: " + (err.message || JSON.stringify(err)));
        console.error("获取Datasophon总览数据失败:", err);
      });
    }
  }
};
</script>

<style lang="less" scoped>
.overview-page {
  background: #fff;
  padding: 20px;
  height: calc(100vh - 100px);
  position: relative;
}

.loading-container {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  height: 100%;
  
  p {
    margin-top: 16px;
    color: #666;
  }
}
</style> 