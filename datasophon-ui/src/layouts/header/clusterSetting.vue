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
  <div class="history-action-btn" @click="showSetting">
    <svg-icon icon-class="setting" />
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
      console.log('点击历史操作按钮');
      this.showClusterSetting(true)
    }
  }
};
</script>
<style lang="less" scoped>
.history-action-btn {
  width: 40px;
  height: 40px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 249, 250, 0.9) 100%);
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.06);
  cursor: pointer;
  backdrop-filter: blur(20px) saturate(180%);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.04),
    0 1px 3px rgba(0, 0, 0, 0.06),
    inset 0 1px 0 rgba(255, 255, 255, 0.8);
  position: relative;
  overflow: hidden;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  &:hover {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 251, 252, 0.95) 100%);
    transform: translateY(-2px) scale(1.05);
    box-shadow: 
      0 8px 24px rgba(0, 0, 0, 0.12),
      0 4px 12px rgba(0, 0, 0, 0.08),
      inset 0 1px 0 rgba(255, 255, 255, 0.9);
  }
  
  &:active {
    transform: translateY(-1px) scale(1.02);
    transition: all 0.15s ease;
  }

  .svg-icon {
    font-size: 20px;
    color: #007aff;
  }
}
</style>