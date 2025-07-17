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
  <div class="steps-container" :class="{'collapsed': collapsed}">
    <div class="lf" :class="{'collapsed': collapsed}">
      <!-- 添加折叠按钮 -->
      <div class="collapse-toggle" @click="toggleCollapse">
        <a-icon :type="collapsed ? 'menu-unfold' : 'menu-fold'" />
      </div>
      
      <a-steps direction="vertical" :current="currentSteps - 1">
        <a-step v-for="(item, index) in stepsList" :key="item" :title="item">
          <!-- 添加序号显示 -->
          <div slot="icon" class="custom-step-icon">
            <span>{{ index + 1 }}</span>
          </div>
        </a-step>
      </a-steps>
    </div>
    <div class="rf" :class="{'expanded': collapsed}">
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
      collapsed: false, // 导航栏折叠状态
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
    // 切换导航栏折叠状态
    toggleCollapse() {
      this.collapsed = !this.collapsed;
    },
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
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    position: relative;
    
    // 导航栏折叠状态
    &.collapsed {
      width: 80px;
      padding: 40px 8px;
      
      // 折叠状态下隐藏标题
      /deep/ .ant-steps-item-title {
        display: none;
      }
      
      // 折叠状态下优化步骤项样式
      /deep/ .ant-steps-vertical {
        .ant-steps-item {
          padding-bottom: 8px;
          margin-bottom: 8px;
          
          .ant-steps-item-container {
            .ant-steps-item-tail {
              top: 28px;
              left: 13px !important;
              height: calc(100% - 12px);
            }
            
            .ant-steps-item-icon {
              width: 26px;
              height: 26px;
              margin: 0;
              line-height: 26px;
              
              .custom-step-icon {
                span {
                  font-size: 12px;
                }
              }
            }
            
            .ant-steps-item-content {
              display: none;
            }
          }
        }
      }
      
      // 折叠状态下优化完成状态的样式
      /deep/ .ant-steps-item-finish {
        .ant-steps-item-icon {
          transform: none;
          box-shadow: none;
        }
      }
      
      // 折叠状态下优化当前步骤的样式
      /deep/ .ant-steps-item-process {
        position: relative;
        
        &:before {
          content: '';
          position: absolute;
          top: -4px;
          bottom: -4px;
          left: -8px;
          right: -8px;
          background: rgba(0, 122, 255, 0.05);
          border-radius: 8px;
          z-index: 0;
        }
        
        .ant-steps-item-icon {
          background: linear-gradient(135deg, #007AFF 0%, #0050DD 100%) !important;
          transform: scale(1.2) !important;
          box-shadow: 0 4px 12px rgba(0, 122, 255, 0.5), 0 0 0 2px rgba(0, 122, 255, 0.2) !important;
          position: relative !important;
          z-index: 1 !important;
          overflow: visible !important;
          
          &::after {
            content: '' !important;
            position: absolute !important;
            top: -6px !important;
            left: -6px !important;
            right: -6px !important;
            bottom: -6px !important;
            border-radius: 50% !important;
            background: radial-gradient(circle, rgba(0, 122, 255, 0.2) 0%, rgba(0, 122, 255, 0) 70%) !important;
            z-index: -1 !important;
            animation: pulseEffect 2s infinite ease-in-out !important;
          }
          
          .custom-step-icon span {
            font-weight: 700 !important;
            color: white !important;
            text-shadow: 0 1px 3px rgba(0, 0, 0, 0.3) !important;
            animation: brightPulse 1.5s infinite alternate ease-in-out !important;
          }
        }
      }
    }
    
    // 折叠按钮样式
    .collapse-toggle {
      position: absolute;
      top: 8px;
      right: 8px;
      z-index: 10;
      width: 28px;
      height: 28px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 8px;
      background: @apple-gray-light;
      cursor: pointer;
      transition: all 0.3s;
      
      &:hover {
        background: darken(@apple-gray-light, 3%);
        transform: translateY(-1px);
      }
      
      &:active {
        transform: translateY(0);
      }
      
      .anticon {
        font-size: 14px;
        color: @apple-gray;
      }
    }
    
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
            
            // 自定义序号图标样式
            .custom-step-icon {
              display: flex;
              align-items: center;
              justify-content: center;
              width: 100%;
              height: 100%;
              
              span {
                display: inline-block;
                font-size: 14px;
                font-weight: 600;
              }
            }
          }
          
          .ant-steps-item-content {
            margin-top: 4px;
            transition: all 0.3s;
            
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
        position: relative;
        
        &:before {
          content: '';
          position: absolute;
          top: -8px;
          bottom: -8px;
          left: -16px;
          right: -16px;
          background: rgba(0, 122, 255, 0.05);
          border-radius: 12px;
          z-index: 0;
        }
        
        .ant-steps-item-icon {
          background: linear-gradient(135deg, #007AFF 0%, #0050DD 100%) !important;
          border-color: transparent !important;
          transform: scale(1.15) !important;
          box-shadow: 0 6px 20px rgba(0, 122, 255, 0.45) !important;
          position: relative !important;
          z-index: 2 !important;
          
          &::before {
            content: '' !important;
            position: absolute !important;
            top: -3px !important;
            left: -3px !important;
            right: -3px !important;
            bottom: -3px !important;
            background: radial-gradient(circle, rgba(255, 255, 255, 0.3) 0%, rgba(255, 255, 255, 0) 70%) !important;
            border-radius: 50% !important;
            z-index: -1 !important;
          }
          
          .ant-steps-icon, .custom-step-icon span {
            color: white !important;
            font-weight: 700 !important;
            text-shadow: 0 1px 2px rgba(0, 0, 0, 0.25) !important;
            transform: scale(1) !important;
            animation: scalePulse 1.5s infinite alternate ease-in-out !important;
          }
        }
        
        .ant-steps-item-content {
          .ant-steps-item-title {
            color: #1d1d1f !important;
            font-weight: 700 !important;
            font-size: 16px !important;
            position: relative !important;
            padding-left: 10px !important;
            margin-left: 8px !important;
            
            &::before {
              content: '' !important;
              position: absolute !important;
              width: 4px !important;
              left: 0 !important;
              top: 0 !important;
              height: 100% !important;
              background: #007AFF !important;
              border-radius: 2px !important;
              box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2) !important;
            }
          }
        }
        
        .ant-steps-item-tail::after {
          background: rgba(0, 122, 255, 0.2) !important;
          height: 2px !important;
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
          
          // 自定义序号样式（等待步骤）
          .custom-step-icon span {
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
    transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
    
    &.expanded {
      padding-left: 20px;
    }
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

@keyframes pulseGlow {
  0% {
    opacity: 0.4;
    transform: scale(0.95);
  }
  100% {
    opacity: 0.8;
    transform: scale(1.05);
  }
}

@keyframes pulseScale {
  0% {
    transform: scale(1);
  }
  100% {
    transform: scale(1.08);
  }
}

@keyframes pulseEffect {
  0% {
    opacity: 0.6;
    transform: scale(0.95);
  }
  50% {
    opacity: 1;
    transform: scale(1.05);
  }
  100% {
    opacity: 0.6;
    transform: scale(0.95);
  }
}

@keyframes scalePulse {
  0% {
    transform: scale(1);
  }
  100% {
    transform: scale(1.15);
  }
}

@keyframes brightPulse {
  0% {
    opacity: 0.9;
  }
  100% {
    opacity: 1;
  }
}
</style>