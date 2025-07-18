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
 * @Date: 2022-06-23 15:24:29
 * @LastEditTime: 2022-10-25 20:09:13
 * @FilePath: \ddh-ui\src\components\menu\serviceOption.vue
-->
<template>
  <div @click.stop class="service-option-wrapper">
    <a-dropdown 
      :trigger="['click']" 
      placement="bottomRight" 
      overlayClassName="apple-style-dropdown" 
      :getPopupContainer="triggerNode => triggerNode.parentNode"
    >
      <button class="apple-style-more-btn">
        <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
          <path d="M12 6C13.1046 6 14 5.10457 14 4C14 2.89543 13.1046 2 12 2C10.8954 2 10 2.89543 10 4C10 5.10457 10.8954 6 12 6Z" fill="#1976d2"/>
          <path d="M12 14C13.1046 14 14 13.1046 14 12C14 10.8954 13.1046 10 12 10C10.8954 10 10 10.8954 10 12C10 13.1046 10.8954 14 12 14Z" fill="#1976d2"/>
          <path d="M12 22C13.1046 22 14 21.1046 14 20C14 18.8954 13.1046 18 12 18C10.8954 18 10 18.8954 10 20C10 21.1046 10.8954 22 12 22Z" fill="#1976d2"/>
        </svg>
      </button>
      <a-menu slot="overlay" class="apple-style-menu">
        <a-menu-item key="addService" @click="addService">
          <a-icon type="plus-circle" />
          <span>添加服务</span>
        </a-menu-item>
        <a-menu-item key="startAll" @click="() => optServices({key: 'startAll'})">
          <a-icon type="caret-right" />
          <span>启动所有</span>
        </a-menu-item>
        <a-menu-item key="stopAll" @click="() => optServices({key: 'stopAll'})">
          <a-icon type="pause-circle" />
          <span>停止所有</span>
        </a-menu-item>
        <a-menu-item key="restartAll" @click="() => optServices({key: 'restartAll'})">
          <a-icon type="reload" />
          <span>重启所有需要重启的服务</span>
        </a-menu-item>
      </a-menu>
    </a-dropdown>
    <!-- 配置集群的modal -->
    <a-modal v-if="visible" title :visible="visible" class="service-option-modal" :maskClosable="false" :closable="false" :width="1576" :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
      <Steps :clusterId="clusterId" stepsType="addService" />
    </a-modal>
  </div>
</template>
<script>
import Steps from "@/components/steps";
import { mapMutations, mapState } from 'vuex'

export default {
  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: () => {},
    };
  },
  components: { Steps },
  data() {
    return {
      visible: false,
      confirmLoading: false,
      clusterId: Number(localStorage.getItem("clusterId") || -1),
    };
  },
  computed: {
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    handleCancel(e) {
      this.visible = false;
    },
    // 添加服务
    addService() {
      this.visible = true;
    },
    optServices(item) {
      this.$confirm({
        width: 450,
        title: () => {
          return (
            <div style="font-size: 22px;">
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content: (
          <div style="margin-top:20px">
            <div style="padding:0 65px;font-size: 16px;color: #555555;">
              {'确认' + (item.key=='startAll'?'启动所有':item.key=='stopAll'?'停止所有':item.key=='restartAll'?'重启所有需要重启的服务':"") +'吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.openServices(item)}
              >
                确定
              </a-button>
              <a-button
                style="margin-right:10px;"
                onClick={() => this.$destroyAll()}
              >
                取消
              </a-button>
            </div>
          </div>
        ),
        icon: () => {
          return <div />;
        },
        closable: true,
      });
    
    },
    openServices(item) {
      let params = {
        clusterId: this.setting.clusterId,
        commandType: item.key === "stopAll" ? "STOP_SERVICE" : item.key === "startAll" ? "START_SERVICE" : "RESTART_SERVICE",
        serviceInstanceIds: "",
      };
      let serviceInstanceIds = [];
      const menuData = JSON.parse(localStorage.getItem("menuData")) || [];
      const arr =
        menuData.filter((item) => item.path === "service-manage") || [];
      if (arr.length > 0) {
        arr[0].children.map((child) => {
          if (item.key === "restartAll") {
            if (child.meta.obj.needRestart) {
              serviceInstanceIds.push(child.meta.obj.id);
            }
          } else {
            serviceInstanceIds.push(child.meta.obj.id);
          }
        });
      }
      params.serviceInstanceIds = serviceInstanceIds.join(",");
      this.$axiosPost(global.API.generateServiceCommand, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          // todo: 打开头部那个setting栏
          this.$destroyAll()
          this.showClusterSetting(true)
        }
      });
    },
  },
};
</script>
<style lang="less" scoped>
.popover-service {
  // margin-left: 31px;
  .more-menu-btn {
    font-size: 14px;
    color: #555555;
    letter-spacing: 0.39px;
    line-height: 32px;
    font-weight: 400;
    &:hover {
      color: @primary-color;
    }
  }
}

.apple-style-more-btn {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: transparent;
  border: none;
  transition: all 0.2s ease;
  padding: 0;
  outline: none;
  cursor: pointer;
  position: relative;
  
  &:hover {
    background-color: rgba(25, 118, 210, 0.08);
  }
  
  &:active {
    background-color: rgba(25, 118, 210, 0.16);
    transform: scale(0.96);
  }
  
  &:focus {
    box-shadow: 0 0 0 2px rgba(25, 118, 210, 0.2);
  }
  
  &::after {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    border-radius: 8px;
    box-shadow: 0 0 0 0 rgba(25, 118, 210, 0.3);
    transition: box-shadow 0.3s ease;
  }
  
  &:active::after {
    box-shadow: 0 0 0 4px rgba(25, 118, 210, 0.2);
  }
  
  .anticon {
    font-size: 16px;
    color: #1976d2;
  }
}

.service-option-wrapper {
  position: relative;
  display: inline-block;
}

:global(.apple-style-dropdown) {
  animation: fade-in 0.15s ease-out;
  
  .ant-dropdown-menu {
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12), 
                0 4px 10px rgba(0, 0, 0, 0.06);
    overflow: hidden;
    min-width: 220px;
    border: 1px solid rgba(233, 233, 233, 0.8);
    padding: 8px 4px;
  }
  
  .ant-dropdown-menu-item {
    margin: 2px 4px;
    border-radius: 8px;
    padding: 10px 14px;
    
    &:hover {
      background-color: #f0f7ff;
    }
    
    &:active {
      background-color: #e6f0ff;
    }
    
    .anticon {
      margin-right: 10px;
      font-size: 16px;
      color: #1976d2;
    }
    
    span {
      color: #333;
    }
    
    &:hover span {
      color: #1976d2;
    }
  }
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

.service-option-modal {
  /deep/ .ant-modal {
    top: 61px;
    .ant-modal-body {
      padding: 0;
    }
  }
  /deep/ .ant-modal-content {
    border-radius: 4px;
  }
}
</style>