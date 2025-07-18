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
      overlayClassName="apple-style-dropdown service-option-dropdown" 
      :getPopupContainer="() => document.body"
      :align="{offset: [-12, 12]}"
    >
      <button class="apple-style-more-btn" @click.stop>
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
      dropdownVisible: false, // 添加控制下拉菜单显示的状态
      menuContainer: null, // 存储下拉菜单容器DOM引用
    };
  },
  computed: {
    ...mapState({
      setting: (state) => state.setting, //深拷贝的意义在于watch里面可以在Watch里面监听他的newval和oldVal的变化
    }),
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    // 修复 getPopupContainer 方法
    getPopupContainer() {
      // 确保返回当前组件的DOM元素作为容器，而不是document.body
      return this.$el;
    },
    // 处理下拉菜单可见性变化
    handleDropdownVisibleChange(visible) {
      this.dropdownVisible = visible;
    },
    // 切换下拉菜单显示状态
    toggleDropdown(e) {
      e.stopPropagation();
      this.dropdownVisible = !this.dropdownVisible;
    },
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
  mounted() {
    // 获取菜单容器DOM引用
    this.menuContainer = document.querySelector('.cdh-service-page');
    
    // 添加点击外部关闭下拉菜单的处理
    document.addEventListener('click', (e) => {
      if (!this.$el.contains(e.target)) {
        this.dropdownVisible = false;
      }
    });
  },
  beforeDestroy() {
    // 移除事件监听器
    document.removeEventListener('click', this.handleOutsideClick);
  }
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
  width: 32px;
  height: 32px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 249, 250, 0.9));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  padding: 0;
  outline: none;
  cursor: pointer;
  position: relative;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  
  &:hover {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
    transform: translateY(-2px) scale(1.05);
    box-shadow: 0 8px 20px rgba(0, 122, 255, 0.2);
    border-color: rgba(0, 122, 255, 0.3);
  }
  
  &:active {
    transform: translateY(0) scale(0.98);
    box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15);
  }
  
  &:focus {
    box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.2), 0 2px 8px rgba(0, 0, 0, 0.08);
  }
  
  &::before {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 0;
    height: 0;
    background: radial-gradient(circle, rgba(0, 122, 255, 0.3) 0%, transparent 70%);
    border-radius: 50%;
    transform: translate(-50%, -50%);
    transition: all 0.3s ease;
    opacity: 0;
  }
  
  &:hover::before {
    width: 60px;
    height: 60px;
    opacity: 1;
  }
  
  svg {
    width: 20px;
    height: 20px;
    transition: all 0.3s ease;
    z-index: 1;
    
    path {
      fill: #007AFF;
      transition: fill 0.3s ease;
    }
  }
  
  &:hover svg {
    transform: rotate(90deg) scale(1.1);
    
    path {
      fill: #0056CC;
    }
  }
}

.service-option-wrapper {
  position: relative;
  display: inline-block;
  z-index: 100; /* 确保下拉菜单在其他元素之上 */
}

:global(.service-option-dropdown) {
  z-index: 1500 !important; /* 提高z-index确保显示在最上层 */
}

:global(.ant-dropdown) {
  z-index: 1500 !important;
}

:global(.apple-style-dropdown) {
  animation: dropdown-appear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  z-index: 1500 !important; /* 提高z-index确保显示在最上层 */
}

:global(.apple-style-dropdown .ant-dropdown-menu) {
    border-radius: 24px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12), 
                0 8px 16px rgba(0, 0, 0, 0.08),
                0 4px 8px rgba(0, 0, 0, 0.06),
                0 1px 2px rgba(0, 0, 0, 0.04);
    overflow: visible;
    min-width: 260px;
    max-width: 300px;
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(30px);
    -webkit-backdrop-filter: blur(30px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    padding: 20px 16px;
    transform-origin: top right;
    z-index: 1500;
    animation: dropdown-smooth-appear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    position: relative;
    margin-top: 10px; /* 增加顶部间距，防止叠在一起 */
  }
  
:global(.apple-style-dropdown .ant-dropdown-menu-item) {
    margin: 6px 8px;
    border-radius: 18px;
    padding: 16px 20px;
    position: relative;
    transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    animation: menu-item-smooth-slide-in 0.4s ease-out;
    animation-fill-mode: both;
    
    &:nth-child(1) { animation-delay: 0.1s; }
    &:nth-child(2) { animation-delay: 0.15s; }
    &:nth-child(3) { animation-delay: 0.2s; }
    &:nth-child(4) { animation-delay: 0.25s; }
    
    &::before {
      content: '';
      position: absolute;
      left: 0;
      top: 0;
      bottom: 0;
      width: 0;
      background: linear-gradient(135deg, #007AFF, #5856D6);
      border-radius: 18px 0 0 18px;
      transition: width 0.4s cubic-bezier(0.34, 1.56, 0.64, 1);
    }
    
}

:global(.apple-style-dropdown .ant-dropdown-menu-item:hover) {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
  transform: translateX(4px) translateY(-1px) scale(1.01);
  box-shadow: 0 6px 20px rgba(0, 122, 255, 0.15), 0 3px 10px rgba(0, 122, 255, 0.08);
}

:global(.apple-style-dropdown .ant-dropdown-menu-item:hover::before) {
  width: 5px;
}

:global(.apple-style-dropdown .ant-dropdown-menu-item:active) {
  transform: translateX(2px) scale(0.98);
  box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
}

:global(.apple-style-dropdown .ant-dropdown-menu-item .anticon) {
  margin-right: 12px;
  font-size: 16px;
  color: #007AFF;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 6px;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
  transition: all 0.3s ease;
}

:global(.apple-style-dropdown .ant-dropdown-menu-item span) {
  color: #1D1D1F;
  font-weight: 500;
  font-size: 14px;
  transition: color 0.3s ease;
}

:global(.apple-style-dropdown .ant-dropdown-menu-item:hover .anticon) {
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2));
  color: #0056CC;
  transform: scale(1.1);
}

:global(.apple-style-dropdown .ant-dropdown-menu-item:hover span) {
  color: #007AFF;
}

@keyframes fade-in {
  from {
    opacity: 0;
  }
  to {
    opacity: 1;
  }
}

@keyframes dropdown-smooth-appear {
  0% {
    opacity: 0;
    transform: scale(0.85) translateY(-15px) rotateX(-10deg);
    filter: blur(8px);
  }
  60% {
    opacity: 0.9;
    transform: scale(1.05) translateY(-3px) rotateX(-2deg);
    filter: blur(2px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0) rotateX(0deg);
    filter: blur(0);
  }
}

@keyframes menu-item-smooth-slide-in {
  0% {
    opacity: 0;
    transform: translateX(-30px) translateY(10px) scale(0.9);
    filter: blur(4px);
  }
  70% {
    opacity: 0.8;
    transform: translateX(3px) translateY(-2px) scale(1.02);
    filter: blur(1px);
  }
  100% {
    opacity: 1;
    transform: translateX(0) translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes dropdown-appear {
  0% {
    opacity: 0;
    transform: scale(0.9) translateY(-10px);
  }
  50% {
    opacity: 0.8;
    transform: scale(1.02) translateY(-2px);
  }
  100% {
    opacity: 1;
    transform: scale(1) translateY(0);
  }
}

@keyframes menu-item-slide-in {
  0% {
    opacity: 0;
    transform: translateX(-20px);
  }
  100% {
    opacity: 1;
    transform: translateX(0);
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