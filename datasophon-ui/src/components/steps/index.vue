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


 * @Date: 2022-06-13 14:04:05
 * @LastEditTime: 2022-10-28 11:34:34
 * @FilePath: \ddh-ui\src\components\steps\index.vue
-->
<template>
  <div class="steps-container">
    <div class="lf">
        <a-steps direction="vertical" :current="currentSteps - 1">
        <a-step v-for= "(item) in stepsList" :key="item" :title="item"></a-step>
        <!-- <a-step title="安装主机"></a-step>
        <a-step title="主机环境校验" />
        <a-step title="主机Agent分发" />
        <a-step title="选择服务" />
        <a-step title="分配服务Master角色" />
        <a-step title="分配服务Worker与Client角色" />
        <a-step title="服务配置" />
        <a-step title="安装并启动服务" /> -->
      </a-steps>
    </div>
    <div class="rf">
      <StepsRf :currentSteps="currentSteps" :stepsType="stepsType" :interval="interval" :stepsList="stepsList"
        :serviceData="steps4Data" :depType="depType"/>
    </div>
  </div>
</template>
<script>
import StepsRf from './stpesRf.vue'
import { mapActions, mapState } from "vuex";

export default {
  name: "ConfigCluster",
  props: { 
    stepsType: {
      type: String,
      default: 'cluster',
    },
    steps4Data: Object,
    clusterId: Number,
    depType:String,
  },
  components: {StepsRf},
  provide () {
    return {
      currentStepsAdd: this.currentStepsAdd,
      currentStepsSub: this.currentStepsSub,
      clusterId: this.setting.clusterId ? this.setting.clusterId : this.clusterId // 需要更换
    }
  },
  watch: {
    stepsType: {
      handler(val) {
        let list = ['安装主机', '主机环境校验', '主机Agent分发', '选择服务', '分配服务Master角色', '分配服务Worker与Client角色', '服务配置', '安装并启动服务' ]
        if (this.stepsType === 'hostManage') {
          list = list.splice(0, 3)
          this.currentSteps = 1
        }
        if (this.stepsType === 'addService') {
          // this.currentSteps = 4t
          this.interval = 3
        }
        if (this.stepsType === 'service-example') {
          this.interval = 4
        }
      },
      immediate: true
    }
  },
  mounted () {
    console.log(this.setting, 'setting', this.clusterId)
  },
  data() {
    return {
      interval: 0,
      currentSteps: 1,
    };
  },
  computed: {
    stepsList () {
      let list = ['安装主机', '主机环境校验', '主机Agent分发', '选择服务', '分配服务Master角色', '分配服务Worker与Client角色', '服务配置', '安装并启动服务' ]
      if (this.stepsType === 'hostManage')list =  list.splice(0, 3)
      if (this.stepsType === 'addService')list =  list.splice(3, list.length)
      if (this.stepsType === 'service-example')list =  list.splice(4, list.length)
      if (this.depType == 'K8S') {
        list = list.filter(item => item !== '主机Agent分发')
      }
      return list
    },
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    currentStepsAdd () {
      this.currentSteps ++
    },
    currentStepsSub () {
      this.currentSteps --
    }
  }
};
</script>
<style lang="less" scoped>
.steps-container {
  display: flex;
  height: 860px;
  
  // 苹果设计系统颜色
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
  
  .lf {
    width: 280px;
    background-color: @apple-white;
    border-right: 1px solid rgba(0, 0, 0, 0.06);
    padding: 40px 30px;
    overflow-y: auto;
    animation: fadeIn 0.8s ease-out;
    
    /deep/ .ant-steps-vertical {
      .ant-steps-item {
        position: relative;
        margin-bottom: 22px;
        padding-bottom: 16px;
        
        &:last-child {
          margin-bottom: 0;
        }
        
        .ant-steps-item-container {
          display: flex;
          align-items: flex-start;
          
          .ant-steps-item-tail {
            position: absolute;
            top: 40px;
            left: 16px;
            height: calc(100% - 24px);
            padding: 0 !important;
            
            &::after {
              height: 100%;
              border-radius: 1px;
              transition: background 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            }
          }
          
          .ant-steps-item-icon {
            width: 32px;
            height: 32px;
            margin-right: 16px;
            line-height: 32px;
            text-align: center;
            border: none;
            border-radius: 50%;
            font-size: 14px;
            font-weight: 500;
            transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
            background: @apple-gray-light;
            color: @apple-gray;
            box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05);
          }
          
          .ant-steps-item-content {
            margin-top: 4px;
            
            .ant-steps-item-title {
              .apple-font();
              font-size: 15px;
              line-height: 1.4;
              color: @apple-gray;
              font-weight: 500;
              transition: all 0.3s;
              padding-right: 8px;
              
              &::after {
                display: none;
              }
            }
            
            .ant-steps-item-description {
              .apple-font();
              font-size: 13px;
              color: @apple-gray;
              padding-right: 0;
            }
          }
        }
      }
      
      // 成功完成的步骤
      .ant-steps-item-finish {
        .ant-steps-item-icon {
          background-color: rgba(0, 113, 227, 0.1);
          border-color: transparent;
          
          .ant-steps-icon {
            color: @apple-blue;
          }
        }
        
        .ant-steps-item-content {
          .ant-steps-item-title {
            color: @apple-black;
          }
        }
        
        .ant-steps-item-tail::after {
          background-color: @apple-blue;
        }
      }
      
      // 当前激活的步骤
      .ant-steps-item-process {
        .ant-steps-item-icon {
          background-color: @apple-blue;
          border-color: transparent;
          transform: scale(1.05);
          box-shadow: 0 4px 12px rgba(0, 113, 227, 0.3);
          
          .ant-steps-icon {
            color: @apple-white;
            font-weight: 600;
            font-size: 15px;
            text-shadow: 0 1px 1px rgba(0, 0, 0, 0.1);
          }
        }
        
        .ant-steps-item-content {
          .ant-steps-item-title {
            color: @apple-black;
            font-weight: 600;
            transform: translateX(4px);
          }
        }
        
        .ant-steps-item-tail::after {
          background-color: @apple-gray-light;
        }
      }
      
      // 等待的步骤
      .ant-steps-item-wait {
        .ant-steps-item-icon {
          background-color: @apple-gray-light;
          border-color: transparent;
          
          .ant-steps-icon {
            color: @apple-gray;
          }
        }
        
        .ant-steps-item-content {
          .ant-steps-item-title {
            color: @apple-gray;
          }
        }
        
        .ant-steps-item-tail::after {
          background-color: @apple-gray-light;
        }
      }
    }
  }
  
  .rf {
    flex: 1;
    padding: 32px 0 32px 30px;
  }
}

// 动画
@keyframes fadeIn {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}
</style>