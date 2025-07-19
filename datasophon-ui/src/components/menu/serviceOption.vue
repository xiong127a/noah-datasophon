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
  <div class="service-option-wrapper" ref="serviceOptionWrapper">
    <!-- 菜单按钮 -->
    <button class="apple-style-more-btn" @click.stop="toggleMenu">
      <svg width="20" height="20" viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
        <path d="M12 6C13.1046 6 14 5.10457 14 4C14 2.89543 13.1046 2 12 2C10.8954 2 10 2.89543 10 4C10 5.10457 10.8954 6 12 6Z" fill="#1976d2"/>
        <path d="M12 14C13.1046 14 14 13.1046 14 12C14 10.8954 13.1046 10 12 10C10.8954 10 10 10.8954 10 12C10 13.1046 10.8954 14 12 14Z" fill="#1976d2"/>
        <path d="M12 22C13.1046 22 14 21.1046 14 20C14 18.8954 13.1046 18 12 18C10.8954 18 10 18.8954 10 20C10 21.1046 10.8954 22 12 22Z" fill="#1976d2"/>
      </svg>
    </button>
    
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
      menuVisible: false, // 控制下拉菜单显示的状态
      menuStyle: {} // 菜单样式，将在toggleMenu方法中动态设置
    };
  },
  computed: {
    ...mapState({
      setting: (state) => state.setting,
    }),
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    
    // 切换菜单显示状态
    toggleMenu(e) {
      if (e) e.stopPropagation();
      this.menuVisible = !this.menuVisible;
      
      // 如果菜单显示，创建菜单元素并附加到body
      if (this.menuVisible) {
        this.$nextTick(() => {
          // 获取按钮元素
          const buttonElement = this.$refs.serviceOptionWrapper.querySelector('.apple-style-more-btn');
          if (buttonElement) {
            const rect = buttonElement.getBoundingClientRect();
            const menuWidth = 260; // 菜单宽度约为260px
            const screenWidth = window.innerWidth;
            const screenHeight = window.innerHeight;
            
            // 计算菜单位置，确保它显示在按钮的右侧
            const menuTop = rect.top;
            const menuLeft = rect.right + 10; // 在按钮右侧10px处
            
            // 检查是否超出屏幕边界
            // 如果超出右侧边界，则显示在左侧
            const finalLeft = menuLeft + menuWidth > screenWidth ? rect.left - menuWidth - 10 : menuLeft;
            
            // 如果超出底部边界，则向上移动
            const finalTop = menuTop + 250 > screenHeight ? screenHeight - 260 : menuTop;
            
            // 创建菜单元素
            const menuElement = document.createElement('div');
            menuElement.className = 'custom-dropdown-menu';
            menuElement.id = 'service-option-menu';
            menuElement.style.position = 'fixed';
            menuElement.style.top = `${finalTop}px`;
            menuElement.style.left = `${finalLeft}px`;
            menuElement.style.zIndex = '9999999'; // 使用非常高的z-index
            menuElement.style.minWidth = '260px';
            menuElement.style.maxWidth = '300px';
            menuElement.style.background = 'rgba(255, 255, 255, 1)';
            menuElement.style.backdropFilter = 'blur(30px)';
            menuElement.style.webkitBackdropFilter = 'blur(30px)';
            menuElement.style.borderRadius = '16px';
            menuElement.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.2), 0 6px 16px rgba(0, 0, 0, 0.15), 0 2px 6px rgba(0, 0, 0, 0.1)';
            menuElement.style.border = '2px solid rgba(0, 122, 255, 0.3)';
            menuElement.style.padding = '12px';
            menuElement.style.pointerEvents = 'auto';
            menuElement.style.visibility = 'visible';
            menuElement.style.overflow = 'visible';
            
            // 添加动画效果
            menuElement.style.opacity = '0';
            menuElement.style.transform = 'translateX(-10px)';
            menuElement.style.transition = 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)';
            
            // 添加菜单项
            const menuItems = [
              { icon: 'plus-circle', text: '添加服务', action: 'addService' },
              { icon: 'caret-right', text: '启动所有', action: 'startAll' },
              { icon: 'pause-circle', text: '停止所有', action: 'stopAll' },
              { icon: 'reload', text: '重启所有需要重启的服务', action: 'restartAll' }
            ];
            
            menuItems.forEach(item => {
              const menuItem = document.createElement('div');
              menuItem.className = 'menu-item';
              menuItem.style.display = 'flex';
              menuItem.style.alignItems = 'center';
              menuItem.style.padding = '12px 16px';
              menuItem.style.margin = '4px 0';
              menuItem.style.borderRadius = '12px';
              menuItem.style.cursor = 'pointer';
              menuItem.style.transition = 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)';
              menuItem.style.position = 'relative';
              menuItem.style.backgroundColor = 'rgba(255, 255, 255, 1)';
              
              // 添加图标
              const iconDiv = document.createElement('div');
              iconDiv.className = 'menu-icon';
              iconDiv.style.marginRight = '12px';
              iconDiv.style.fontSize = '16px';
              iconDiv.style.color = '#007AFF';
              iconDiv.style.width = '32px';
              iconDiv.style.height = '32px';
              iconDiv.style.borderRadius = '10px';
              iconDiv.style.background = 'linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1))';
              iconDiv.style.display = 'flex';
              iconDiv.style.alignItems = 'center';
              iconDiv.style.justifyContent = 'center';
              iconDiv.style.transition = 'all 0.3s ease';
              iconDiv.style.position = 'relative';
              iconDiv.style.zIndex = '1';
              
              // 使用Ant Design的图标
              const icon = document.createElement('i');
              icon.className = `anticon anticon-${item.icon}`;
              
              // 根据不同的图标类型设置不同的SVG内容
              let svgContent = '';
              if (item.icon === 'plus-circle') {
                svgContent = '<svg viewBox="64 64 896 896" data-icon="plus-circle" width="1em" height="1em" fill="currentColor" aria-hidden="true" focusable="false"><path d="M696 480H544V328c0-4.4-3.6-8-8-8h-48c-4.4 0-8 3.6-8 8v152H328c-4.4 0-8 3.6-8 8v48c0 4.4 3.6 8 8 8h152v152c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V544h152c4.4 0 8-3.6 8-8v-48c0-4.4-3.6-8-8-8z"></path><path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372z"></path></svg>';
              } else if (item.icon === 'caret-right') {
                svgContent = '<svg viewBox="0 0 1024 1024" data-icon="caret-right" width="1em" height="1em" fill="currentColor" aria-hidden="true" focusable="false"><path d="M715.8 493.5L335 165.1c-14.2-12.2-35-1.2-35 18.5v656.8c0 19.7 20.8 30.7 35 18.5l380.8-328.4c10.9-9.4 10.9-27.6 0-37z"></path></svg>';
              } else if (item.icon === 'pause-circle') {
                svgContent = '<svg viewBox="64 64 896 896" data-icon="pause-circle" width="1em" height="1em" fill="currentColor" aria-hidden="true" focusable="false"><path d="M512 64C264.6 64 64 264.6 64 512s200.6 448 448 448 448-200.6 448-448S759.4 64 512 64zm0 820c-205.4 0-372-166.6-372-372s166.6-372 372-372 372 166.6 372 372-166.6 372-372 372zm-88-532h-48c-4.4 0-8 3.6-8 8v304c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V360c0-4.4-3.6-8-8-8zm224 0h-48c-4.4 0-8 3.6-8 8v304c0 4.4 3.6 8 8 8h48c4.4 0 8-3.6 8-8V360c0-4.4-3.6-8-8-8z"></path></svg>';
              } else if (item.icon === 'reload') {
                svgContent = '<svg viewBox="64 64 896 896" data-icon="reload" width="1em" height="1em" fill="currentColor" aria-hidden="true" focusable="false"><path d="M909.1 209.3l-56.4 44.1C775.8 155.1 656.2 92 521.9 92 290 92 102.3 279.5 102 511.5 101.7 743.7 289.8 932 521.9 932c181.3 0 335.8-115 394.6-276.1 1.5-4.2-.7-8.9-4.9-10.3l-56.7-19.5a8 8 0 0 0-10.1 4.8c-1.8 5-3.8 10-5.9 14.9-17.3 41-42.1 77.8-73.7 109.4A344.77 344.77 0 0 1 655.9 829c-42.3 17.9-87.4 27-133.8 27-46.5 0-91.5-9.1-133.8-27A341.5 341.5 0 0 1 279 755.2a342.16 342.16 0 0 1-73.7-109.4c-17.9-42.4-27-87.4-27-133.9s9.1-91.5 27-133.9c17.3-41 42.1-77.8 73.7-109.4 31.6-31.6 68.4-56.4 109.3-73.8 42.3-17.9 87.4-27 133.8-27 46.5 0 91.5 9.1 133.8 27a341.5 341.5 0 0 1 109.3 73.8c9.9 9.9 19.2 20.4 27.8 31.4l-60.2 47a8 8 0 0 0 3 14.1l175.6 43c5 1.2 9.9-2.6 9.9-7.7l.8-180.9c-.1-6.6-7.8-10.3-13-6.2z"></path></svg>';
              }
              
              icon.innerHTML = svgContent;
              iconDiv.appendChild(icon);
              
              // 添加文本
              const textSpan = document.createElement('span');
              textSpan.className = 'menu-text';
              textSpan.style.color = '#1D1D1F';
              textSpan.style.fontWeight = '500';
              textSpan.style.fontSize = '14px';
              textSpan.style.transition = 'color 0.3s ease';
              textSpan.style.position = 'relative';
              textSpan.style.zIndex = '1';
              textSpan.style.flex = '1';
              textSpan.textContent = item.text;
              
              menuItem.appendChild(iconDiv);
              menuItem.appendChild(textSpan);
              
              // 添加悬停效果
              menuItem.addEventListener('mouseover', () => {
                menuItem.style.background = 'linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1))';
                menuItem.style.transform = 'translateX(4px)';
                menuItem.style.boxShadow = '0 4px 12px rgba(0, 122, 255, 0.1)';
                iconDiv.style.background = 'linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2))';
                iconDiv.style.color = '#0056CC';
                iconDiv.style.transform = 'scale(1.1)';
                textSpan.style.color = '#007AFF';
              });
              
              menuItem.addEventListener('mouseout', () => {
                menuItem.style.background = 'rgba(255, 255, 255, 1)';
                menuItem.style.transform = 'none';
                menuItem.style.boxShadow = 'none';
                iconDiv.style.background = 'linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1))';
                iconDiv.style.color = '#007AFF';
                iconDiv.style.transform = 'none';
                textSpan.style.color = '#1D1D1F';
              });
              
              // 添加点击事件
              menuItem.addEventListener('click', () => {
                // 移除菜单
                document.body.removeChild(menuElement);
                this.menuVisible = false;
                
                // 执行相应的操作
                if (item.action === 'addService') {
                  this.addService();
                } else if (item.action === 'startAll') {
                  this.optServices({ key: 'startAll' });
                } else if (item.action === 'stopAll') {
                  this.optServices({ key: 'stopAll' });
                } else if (item.action === 'restartAll') {
                  this.optServices({ key: 'restartAll' });
                }
              });
              
              menuElement.appendChild(menuItem);
            });
            
            // 将菜单附加到body
            document.body.appendChild(menuElement);
            
            // 触发动画
            setTimeout(() => {
              menuElement.style.opacity = '1';
              menuElement.style.transform = 'translateX(0)';
            }, 10);
            
            // 添加点击外部关闭菜单的事件
            const handleOutsideClick = (event) => {
              try {
                // 首先检查所有变量是否存在
                if (!menuElement || !this.$refs.serviceOptionWrapper) {
                  console.warn('菜单元素或包装元素不存在，取消事件监听');
                  document.removeEventListener('click', handleOutsideClick);
                  this.menuVisible = false;
                  return;
                }
                
                // 确保menuElement和this.$refs.serviceOptionWrapper不是null或undefined再调用contains方法
                const isClickInsideMenu = menuElement && menuElement.contains && menuElement.contains(event.target);
                const isClickInsideButton = this.$refs.serviceOptionWrapper && 
                                           this.$refs.serviceOptionWrapper.contains && 
                                           this.$refs.serviceOptionWrapper.contains(event.target);
                
                if (!isClickInsideMenu && !isClickInsideButton) {
                  // 添加安全检查，确保元素存在且是document.body的子元素
                  if (menuElement && menuElement.parentNode === document.body) {
                    try {
                      document.body.removeChild(menuElement);
                    } catch (error) {
                      console.warn('移除菜单时出错:', error);
                    }
                  }
                  document.removeEventListener('click', handleOutsideClick);
                  this.menuVisible = false;
                }
              } catch (error) {
                console.error('handleOutsideClick发生错误:', error);
                // 出现任何错误，都移除事件监听并设置菜单为不可见
                document.removeEventListener('click', handleOutsideClick);
                this.menuVisible = false;
              }
            };
            
            // 延迟添加事件监听器，避免立即触发
            setTimeout(() => {
              document.addEventListener('click', handleOutsideClick);
            }, 0);
          }
        });
      } else {
        // 如果菜单隐藏，移除菜单元素
        const menuElement = document.getElementById('service-option-menu');
        if (menuElement && menuElement.parentNode === document.body) {
          try {
            document.body.removeChild(menuElement);
          } catch (error) {
            console.warn('移除菜单时出错:', error);
          }
        }
      }
    },
    
    // 处理点击外部关闭菜单
    handleOutsideClick(e) {
      if (this.$refs.serviceOptionWrapper && !this.$refs.serviceOptionWrapper.contains(e.target)) {
        this.menuVisible = false;
        document.removeEventListener('click', this.handleOutsideClick);
      }
    },
    
    handleCancel(e) {
      this.visible = false;
    },
    
    // 添加服务
    addService() {
      this.menuVisible = false;
      this.visible = true;
    },
    
    optServices(item) {
      this.menuVisible = false;
      
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
  beforeDestroy() {
    // 移除事件监听器和菜单元素
    // document.removeEventListener('click', this.handleOutsideClick);
    const menuElement = document.getElementById('service-option-menu');
    if (menuElement && menuElement.parentNode === document.body) {
      try {
        document.body.removeChild(menuElement);
      } catch (error) {
        console.warn('移除菜单时出错:', error);
      }
    }
  },
};
</script>
<style lang="less" scoped>
.service-option-wrapper {
  position: relative;
  display: inline-block;
  z-index: 1500; /* 确保下拉菜单在其他元素之上 */
  overflow: visible; /* 确保子元素不会被裁剪 */
}

.apple-style-more-btn {
  width: 32px;
  height: 32px;
  border-radius: 50%; /* 修改为圆形，与ServiceLayout.vue中的按钮一致 */
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
    transform: scale(1.05) translateY(-1px); /* 修改悬停效果，与ServiceLayout.vue中的按钮一致 */
    box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15), 0 2px 4px rgba(0, 0, 0, 0.08);
    border-color: rgba(0, 122, 255, 0.2);
  }
  
  &:active {
    transform: scale(0.95);
    box-shadow: 0 1px 4px rgba(0, 122, 255, 0.2);
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