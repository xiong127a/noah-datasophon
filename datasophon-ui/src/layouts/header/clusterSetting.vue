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


 * @describe: 
 * @Date: 2022-06-23 14:21:08
 * @LastEditTime: 2022-06-28 17:58:24
 * @FilePath: \ddh-ui\src\layouts\header\clusterSetting.vue
-->
<template>
  <div class="cluster-setting mgr10">
    <div class="icon-wrapper" @click="showSetting">
      <svg-icon class="cluster-setting-icon" icon-class="setting" />
    </div>
    <!-- 配置集群的modal -->
    <a-modal v-if="clusterSettingVisible" title :visible="clusterSettingVisible" :maskClosable="false" :closable="false" :width="1344" :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
      <Steps8 :clusterId="clusterId" stepsType="cluster-setting" />
    </a-modal>
  </div>
</template>
<script>
import Steps8 from "@/components/steps/step8";
import { mapState, mapMutations } from 'vuex'
export default {
  components: { Steps8 },
  data() {
    return {
      visible: false,
      confirmLoading: false,
      clusterId: Number(localStorage.getItem("clusterId") || -1) ,
    }
  },
  computed: {
    ...mapState('setting', ['clusterSettingVisible'])
  },
  provide() {
    return {
      clusterId: this.clusterId,
      handleCancel: this.handleCancel
    };
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    handleCancel () {
      this.showClusterSetting(false)
    },
    showSetting () {
      console.log('点击设置图标');
      this.showClusterSetting(true)
    }
  }
};
</script>
<style lang="less" scoped>
.cluster-setting {
  &-icon {
    color: #222b45;
    font-size: 16px;
  }

  .icon-wrapper {
    cursor: pointer;
    display: flex;
    align-items: center;
    justify-content: center;
    padding: 4px;
    border-radius: 4px;
    transition: background-color 0.3s;

    &:hover {
      background-color: rgba(0, 0, 0, 0.05);
    }
  }
}
</style>