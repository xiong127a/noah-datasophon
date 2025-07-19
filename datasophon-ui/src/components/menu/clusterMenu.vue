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
 * @Date: 2022-06-20 20:34:13
 * @LastEditTime: 2023-03-17 17:19:31
 * @FilePath: \ddh-ui\src\components\menu\clusterMenu.vue
-->
<template>
  <a-menu class="cluster-menu cdh-cluster-menu" :mode="mode" :inlineCollapsed="collapsed" :theme="'light'" :defaultSelectedKeys="['overview']" :selectedKeys="selectedKeys" :openKeys="sOpenKeys" @click="handleClick" @openChange="openChange" :style="{'min-width': collapsed ? '50px' : '', background:'#fff', borderRight:'1px solid #e0e0e0'}">
    <template v-for="(item) in options">
      <template v-if="!item.children.length">
        <a-menu-item :key="item.fullPath" style="height:40px; line-height:40px;">
          <span v-if="collapsed">{{ item.name }}</span>
          <router-link :to="{ path: item.fullPath }">
            <span class="flex-container" v-if="!collapsed">
              <svg-icon :icon-class="item.meta.icon" class="collapsed-icon anticon" :style="{width: '16px',height: '16px',lineHeight: '0px', marginRight:'8px'}" />
              {{ item.name}}
            </span>
          </router-link>
        </a-menu-item>
      </template>
      <template v-else>
        <a-sub-menu :key="item.fullPath">
          <span slot="title">
            <div class="flex-bewteen-container">
              <span class="flex-container">
                <svg-icon :icon-class="item.meta.icon" class="collapsed-icon anticon" :style="{width: '16px',height: '16px',lineHeight: '0px', marginRight:'8px'}" />
                {{item.name}}
            </span>
            <div v-if="item.path === 'service-manage'">
              <serviceOption />
            </div>
            </div>
          </span>
          <a-menu-item v-for="(subItem) in item.children" :key="subItem.fullPath" style="padding-left: 24px; height:38px; line-height:38px;">
            <router-link :to="{ path: subItem.fullPath }">
              <div class="flex-bewteen-container cluster-menu-item">
                <div class="flex-container cluster-menu-item-left">
                  <span :class="['circle-point', 'mgr10', subItem.meta.obj? subItem.meta.obj.serviceStateCode === 1 ? 'hide-point' : subItem.meta.obj.serviceStateCode === 2 ? 'success-point': subItem.meta.obj.serviceStateCode === 3 ? 'configured-point': 'error-point' : '']"></span>
                  <span class="service-name" :style="getServiceClassNameStyle(subItem.meta.obj)" :title="subItem.label">{{subItem.label}}</span>
                </div>
                <div v-if="subItem.path.includes('service-list')" class="cluster-menu-item-right">
                  <span v-if="subItem.meta.obj && [3,4].includes(subItem.meta.obj.serviceStateCode) && subItem.meta.obj.alertNum > 0" :class="[subItem.meta.obj ? subItem.meta.obj.serviceStateCode === 4 ? 'error-status-color': 'configured-status-color':'']" @click="showGj(subItem.meta.obj)">
                   <span v-show="alarmManageVisible">
                    <svg-icon class="icon-gj" icon-class="gaojing"></svg-icon>
                    {{subItem.meta.obj ? subItem.meta.obj.alertNum ? subItem.meta.obj.alertNum : 0 : 0}}
                   </span>
                  </span>
                  <a-icon v-if="subItem.meta.obj && subItem.meta.obj.needRestart" type="sync" class="menu-sub-icon" @click="textCompare" />
                  <a-popover 
                    trigger="click" 
                    placement="rightTop" 
                    class="popover-index" 
                    overlayClassName="popover-index" 
                    :content="()=> getMoreMenu(subItem)"
                    :visible="popoverVisible[subItem.meta.obj.id]"
                    @visibleChange="(visible) => handlePopoverVisibleChange(visible, subItem.meta.obj.id)"
                  >
                    <!-- 替换RotatingIcon组件为内联SVG -->
                    <div 
                      class="rotating-icon-container"
                      ref="iconContainer" 
                      :data-id="subItem.meta.obj.id"
                      @mouseenter="(e) => onIconMouseEnter(e)"
                      @mouseleave="(e) => onIconMouseLeave(e)"
                      @click="togglePopover(subItem.meta.obj.id, $event)"
                    >
                      <svg 
                        viewBox="64 64 896 896" 
                        class="rotating-icon-svg"
                        data-icon="more" 
                        width="1em" 
                        height="1em" 
                        fill="currentColor" 
                        aria-hidden="true" 
                        focusable="false"
                      >
                        <path d="M456 231a56 56 0 1 0 112 0 56 56 0 1 0-112 0zm0 280a56 56 0 1 0 112 0 56 56 0 1 0-112 0zm0 280a56 56 0 1 0 112 0 56 56 0 1 0-112 0z"></path>
                      </svg>
                    </div>
                  </a-popover>
                </div>
              </div>
            </router-link>
          </a-menu-item>
        </a-sub-menu>
      </template>
    </template>
  </a-menu>
</template>

<script>
import fastEqual from "fast-deep-equal";
import serviceOption from './serviceOption.vue';
import _ from 'lodash';
import alarmModal from '@/components/alarmModal'
import TextCompare from './commponents/textCompare.vue'
import { mapMutations ,mapState} from 'vuex'
import { changeRouter } from '@/utils/changeRouter'

const toRoutesMap = (routes) => {
  const map = {};
  routes.forEach((route) => {
    map[route.fullPath] = route;
    if (route.children && route.children.length > 0) {
      const childrenMap = toRoutesMap(route.children);
      Object.assign(map, childrenMap);
    }
  });
  return map;
};
export default {
  components: { serviceOption },
  props: {
    options: {
      type: Array,
      required: true,
    },
    theme: {
      type: String,
      required: false,
      default: "dark",
    },
    mode: {
      type: String,
      required: false,
      default: "inline",
    },
    collapsed: {
      type: Boolean,
      required: false,
      default: false,
    },
    i18n: Object,
    openKeys: Array,
  },
  data() {
    return {
      selectedKeys: [],
      sOpenKeys: [],
      cachedOpenKeys: [],
      popoverVisible: {}, // 存储每个按钮的下拉框可见状态
    };
  },
  created() {
    this.updateMenu();
    
    // 初始化所有菜单的下拉框状态为关闭
    this.$nextTick(() => {
      if (this.options && this.options.length > 0) {
        this.options.forEach(item => {
          if (item.children && item.children.length > 0) {
            item.children.forEach(subItem => {
              if (subItem.meta && subItem.meta.obj && subItem.meta.obj.id) {
                this.$set(this.popoverVisible, subItem.meta.obj.id, false);
              }
            });
          }
        });
      }
    });
  },
  watch: {
    $route: function () {
      this.updateMenu();
    },
  },
  computed: {
    menuTheme() {
      return this.theme == "light" ? this.theme : "dark";
    },
    routesMap() {
      return toRoutesMap(this.options);
    },
    ...mapState('setting', ['alarmManageVisible', "clusterId"])
  },
  mounted() {
    this.updateMenuNames()
    // 添加全局点击事件监听
    document.addEventListener('click', this.handleGlobalClick);
    
    // 初始化图标状态
    this.$nextTick(() => {
      // 在下一个DOM更新周期执行，确保元素已渲染
      const icons = document.querySelectorAll('.rotating-icon-container');
      icons.forEach(icon => {
        const svg = icon.querySelector('svg');
        const id = icon.dataset.id;
        if (svg && id && this.popoverVisible[id]) {
          // 如果下拉框是打开的，图标保持旋转状态
          svg.style.transform = 'rotate(90deg) scale(1.1)';
          icon.style.color = '#1976d2';
        }
      });
    });
  },
  beforeDestroy() {
    // 移除全局点击事件监听
    document.removeEventListener('click', this.handleGlobalClick);
    
    // 清理可能存在的菜单元素
    try {
      const popoverMenus = document.querySelectorAll('.custom-dropdown-menu');
      popoverMenus.forEach(menu => {
        if (menu && menu.parentNode) {
          menu.parentNode.removeChild(menu);
        }
      });
    } catch (error) {
      console.warn('组件销毁时清理菜单错误:', error);
    }
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting" ]),
    textCompare(){
      const self = this;
      let width = 1200;
      let title = "服务版本对比";
      let serviceId = {id:this.$route.params.serviceId || ""}
      let content = (
        <TextCompare  serviceId={serviceId} callBack={() => self.updateMenu()} />
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
    getServiceClassNameStyle (obj) {
      // 如果有重启的tubiao没有告警的图标
      if (obj && obj.needRestart && (![3,4].includes(obj.serviceStateCode) && obj.alertNum === 0)) {
        return {
          'max-width': '116px'
        }
      }
      // 如果没有重启的tubiao有告警的图标
      if (obj && !obj.needRestart && ([3,4].includes(obj.serviceStateCode) && obj.alertNum > 0)) {
        return {
          'max-width': '116px'
        }
      }
      // 如果有重启的tubiao有告警的图标
      if (obj && obj.needRestart && ([3,4].includes(obj.serviceStateCode) && obj.alertNum > 0)) {
        return {
          'max-width': '106px'
        }
      }
      // 如果没有重启的tubiao没有告警的图标
      return {
        'max-width': '126px'
      }
    },
    updateMenu() {
      this.selectedKeys = this.getSelectedKeys();
      console.log(this.selectedKeys);
      let openKeys = this.selectedKeys.filter((item) => item !== "");
      openKeys = openKeys.slice(0, openKeys.length - 1);
      this.sOpenKeys = openKeys
      // 点击这几个模块 会展开服务管理菜单
      // if(this.selectedKeys.includes('/overview') ||this.selectedKeys.includes('/host-manage') ||this.selectedKeys.includes('/alarm-manage') ){
      //   this.sOpenKeys.push('/service-manage')
      // }

      if (!fastEqual(openKeys, this.sOpenKeys)) {
        this.collapsed || this.mode === "horizontal"
          ? (this.cachedOpenKeys = openKeys)
          : (this.sOpenKeys = openKeys);
      }
    },
    getSelectedKeys() {
      let matches = this.$route.matched;
      console.log(matches);
      let arr = []
      matches.map(item => {
        arr.push(item)
      })
      const route = matches[matches.length - 1];
      let chose = this.routesMap[route.path];
      if (chose && chose.meta && chose.meta.highlight) {
        chose = this.routesMap[chose.meta.highlight];
        const resolve = this.$router.resolve({ path: chose.fullPath });
        matches = (resolve.resolved && resolve.resolved.matched) || matches;
      }
      let selectedKeys = []
      if ((this.$route.params && this.$route.params.serviceId)) {
        let arr2 = arr.splice(0, matches.length-1)
        arr2.push(this.$route)
        arr2.map((item) => {
          selectedKeys.push(item.path)
        });
      } else {
        matches.map((item) => {
          selectedKeys.push(item.path)
        });
      }
      return selectedKeys
    },
    getMoreMenu(props) {
      let arr = [
        { name: "启动", key: "start" },
        { name: "停止", key: "stop" },
        { name: "重启", key: "restart" },
        { name: "删除", key: "del" },
        // { name: "添加角色实例", key: "add" },
        // { name: "下载客户端配置", key: "downLoad" },
      ];
      // if (props.meta.obj.needRestart) arr.splice(2, 0, { name: "重启", key: "restart" })
      return arr.map((item, index) => {
        return (
          <div 
            key={index} 
            style={{
              padding: '2px 0',
              animation: `menuItemAppear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) forwards`,
              animationDelay: `${index * 0.05}s`,
              opacity: 0,
              transform: 'translateX(-10px)'
            }}
          >
            <a
              class="more-menu-btn"
              style="border-width:0px;min-width:100px;padding:8px 12px;border-radius:8px;display:block;"
              onClick={(e) => {
                // 阻止事件冒泡
                e.stopPropagation();
                e.preventDefault();
                
                // 手动关闭下拉框
                this.$set(this.popoverVisible, props.meta.obj.id, false);
                
                // 执行对应操作
                this.openServices(item, props);
                
                // 重置图标状态
                const icons = document.querySelectorAll(`.rotating-icon-container[data-id="${props.meta.obj.id}"]`);
                if (icons.length > 0) {
                  const icon = icons[0];
                  const svg = icon.querySelector('svg');
                  if (svg) {
                    svg.style.transform = '';
                    icon.style.color = '';
                  }
                }
              }}
            >
              {item.name}
            </a>
          </div>
        );
      });
    },
    openServices(item, props) {
      // 确保任何可能的弹出菜单都被正确关闭
      try {
        const popoverMenus = document.querySelectorAll('.custom-dropdown-menu');
        popoverMenus.forEach(menu => {
          if (menu && menu.parentNode) {
            menu.parentNode.removeChild(menu);
          }
        });
      } catch (error) {
        console.warn('清理菜单时出现错误:', error);
      }

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
              {'确认' + (item.key=='start'?'开启':item.key=='stop'?'停止':item.key=='restart'?'重启':item.key=='del'?'删除':"") +'吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.optServices(item, props)}
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
    delService(id){
      this.$axiosPost('/ddh/cluster/service/instance/delete', {serviceInstanceId: id,}).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.getInto()
        }
      });
    },
    getInto() {
      this.$axiosPost(global.API.getServiceListByCluster, {
        clusterId: this.clusterId,
      }).then((res) => {
        changeRouter(res.data, this.clusterId)
        this.$router.push("/service-manage");
      });
    },
    optServices(item, props) {
      if(item.key === "del"){
        this.delService(props.meta.obj.id);
        return
      }
      let params = {
        clusterId: this.clusterId,
        commandType: item.key === "stop" ? "STOP_SERVICE" : item.key === "start" ? "START_SERVICE" : "RESTART_SERVICE",
        serviceInstanceIds: props.meta.obj.id,
      };
      this.$axiosPost(global.API.generateServiceCommand, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.showClusterSetting(true)
        }
      });
    },
    showGj (meunItem) {
      let width = 1000;
      let title = "告警详情";
      let content = (
        <alarmModal serviceInstanceId={meunItem.id} />
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
    handleClick(e) {
      this.selectedKeys = [];
      this.selectedKeys.push(e.key);
      this.$emit("select", e);
    },
    openChange (val) {
      this.sOpenKeys = val
    },
    updateMenuNames() {
      // 强制更新菜单项名称
      let menuData = JSON.parse(localStorage.getItem('menuData')) || []
      if(menuData.length > 0) {
        let updated = false
        // 查找并更新菜单名称
        menuData.forEach(item => {
          if(item.path === 'overview' && item.name !== '集群总览') {
            item.name = '集群总览'
            updated = true
          }
        })
        // 保存修改后的菜单数据
        if(updated) {
          localStorage.setItem('menuData', JSON.stringify(menuData))
          window.location.reload()
        }
      }
    },
    // 添加事件处理方法
    rotateIcon(e) {
      // 获取事件目标元素
      const target = e.currentTarget;
      
      // 查找图标组件内的SVG元素
      const svg = target.querySelector('svg');
      if (svg) {
        svg.style.transition = 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)';
        svg.style.transform = 'rotate(90deg) scale(1.1)';
      }
      
      // 设置颜色
      target.style.color = '#1976d2'; // 使用primary-color对应的颜色值
    },
    resetIcon(e) {
      // 获取事件目标元素
      const target = e.currentTarget;
      
      // 查找图标组件内的SVG元素
      const svg = target.querySelector('svg');
      if (svg) {
        svg.style.transform = '';
      }
      
      // 重置颜色
      target.style.color = '';
    },
    // 添加点击事件处理和popover控制方法
    togglePopover(id, event) {
      try {
        // 防止事件冒泡
        if (event) {
          event.stopPropagation();
          // 防止可能的默认行为
          event.preventDefault();
        }
        
        // 检查ID是否有效
        if (!id) {
          console.warn('切换下拉框失败：无效的ID');
          return;
        }
        
        // 切换当前按钮的下拉框状态
        const newState = !this.popoverVisible[id];
        console.log(`切换下拉框状态: ${id} => ${newState}`);
        
        // 先关闭所有下拉框，然后再打开当前下拉框
        Object.keys(this.popoverVisible).forEach(key => {
          if (key !== id.toString() && this.popoverVisible[key]) {
            this.$set(this.popoverVisible, key, false);
          }
        });
        
        // 设置当前下拉框状态
        this.$set(this.popoverVisible, id, newState);
        
        // 安全地更新图标状态
        try {
          // 找到当前点击的图标
          const icons = document.querySelectorAll('.rotating-icon-container');
          icons.forEach(icon => {
            if (!icon) return;
            
            const svg = icon.querySelector('svg');
            if (svg) {
              // 获取关联的ID
              const iconId = icon.dataset.id;
              if (!iconId) return;
              
              // 如果是当前点击的图标，根据下拉框状态设置旋转
              if (iconId === id.toString()) {
                if (newState) {
                  // 下拉框打开，图标旋转
                  svg.style.transform = 'rotate(90deg) scale(1.1)';
                  icon.style.color = '#1976d2';
                } else {
                  // 下拉框关闭，图标恢复
                  svg.style.transform = '';
                  icon.style.color = '';
                }
              }
              // 其他按钮不处理，已在前面全部关闭下拉框
            }
          });
        } catch (error) {
          console.warn('更新图标状态时出错:', error);
        }
        
        // 如果打开了下拉框，延迟一点添加全局点击事件监听
        if (newState) {
          try {
            setTimeout(() => {
              // 确保只添加一次事件监听
              document.removeEventListener('click', this.handleGlobalClick);
              document.addEventListener('click', this.handleGlobalClick);
            }, 10);
          } catch (error) {
            console.warn('添加全局点击事件监听出错:', error);
          }
        }
      } catch (error) {
        console.error('切换下拉框状态时出错:', error);
      }
    },

    // 处理popover可见性变化
    handlePopoverVisibleChange(visible, id) {
      try {
        // 验证参数
        if (id === undefined || visible === undefined) {
          console.warn('下拉框可见性变化处理：参数无效', { visible, id });
          return;
        }
        
        console.log(`下拉框可见性变化: ${id} => ${visible}`);
        
        if (this.popoverVisible[id] !== visible) {
          this.$set(this.popoverVisible, id, visible);
          
          try {
            // 找到对应的图标并设置其样式
            const icons = document.querySelectorAll(`.rotating-icon-container[data-id="${id}"]`);
            if (icons.length > 0) {
              const icon = icons[0];
              if (icon) {
                const svg = icon.querySelector('svg');
                if (svg) {
                  if (visible) {
                    svg.style.transform = 'rotate(90deg) scale(1.1)';
                    icon.style.color = '#1976d2';
                  } else {
                    svg.style.transform = '';
                    icon.style.color = '';
                  }
                }
              }
            }
          } catch (error) {
            console.warn('设置图标样式时出错:', error);
          }
        }
      } catch (error) {
        console.error('处理下拉框可见性变化时出错:', error);
      }
    },

    // 处理全局点击事件
    handleGlobalClick(event) {
      try {
        // 确保event和event.target存在
        if (!event || !event.target) {
          console.warn('事件对象不完整，忽略全局点击');
          return;
        }
        
        console.log("全局点击事件触发");
        
        // 使用安全的检查方法
        const isClickInsidePopover = event.target.closest && event.target.closest('.ant-popover');
        const isClickInsidePopoverIndex = event.target.closest && event.target.closest('.popover-index');
        const isClickInsideRotatingIcon = event.target.closest && event.target.closest('.rotating-icon-container');
        const isClickInsidePopoverContent = event.target.closest && event.target.closest('.ant-popover-inner-content');
        
        // 如果点击了菜单内部元素，不做处理
        if (isClickInsidePopover || isClickInsidePopoverIndex || 
            isClickInsideRotatingIcon || isClickInsidePopoverContent) {
          return;
        }
        
        // 点击发生在其他区域，关闭所有下拉菜单
        let hasChanges = false;
        Object.keys(this.popoverVisible).forEach(key => {
          if (this.popoverVisible[key]) {
            this.$set(this.popoverVisible, key, false);
            hasChanges = true;
          }
        });
        
        // 如果有改变，重置所有图标状态
        if (hasChanges) {
          // 使用安全的查询选择器方法
          try {
            const icons = document.querySelectorAll('.rotating-icon-container');
            icons.forEach(icon => {
              if (icon) {
                const svg = icon.querySelector('svg');
                if (svg) {
                  svg.style.transform = '';
                  icon.style.color = '';
                }
              }
            });
          } catch (error) {
            console.warn('重置图标状态时出错:', error);
          }
        }
      } catch (error) {
        console.error('全局点击事件处理出错:', error);
      }
    },

    // 添加鼠标事件处理方法
    onIconMouseEnter(e) {
      try {
        // 确保事件对象和目标元素存在
        if (!e || !e.currentTarget) {
          return;
        }
        
        const container = e.currentTarget;
        // 确保容器存在再查询SVG
        if (container) {
          const svg = container.querySelector('svg');
          if (svg) {
            svg.style.transition = 'all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)';
            svg.style.transform = 'rotate(90deg) scale(1.1)';
            container.style.color = '#1976d2';
          }
        }
      } catch (error) {
        console.warn('鼠标进入图标处理出错:', error);
      }
    },

    onIconMouseLeave(e) {
      try {
        // 确保事件对象和目标元素存在
        if (!e || !e.currentTarget) {
          return;
        }
        
        const container = e.currentTarget;
        // 确保容器存在再查询SVG
        if (container) {
          const svg = container.querySelector('svg');
          if (svg) {
            const id = container.dataset.id;
            // 只有当下拉框没有显示时才恢复图标状态
            if (!id || !this.popoverVisible[id]) {
              svg.style.transform = '';
              container.style.color = '';
            }
          }
        }
      } catch (error) {
        console.warn('鼠标离开图标处理出错:', error);
      }
    },
  },
};
</script>
<style lang="less" scoped>
.cluster-menu {
  .cluster-menu-item {
    &-left {
      .circle-point {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        display: block;
        z-index: 1000;
      }
      .service-name {
        cursor: pointer;
        max-width: 100px;
        overflow:hidden;
        white-space:nowrap;
        text-overflow:ellipsis;
      }
      .hide-point {
        visibility: hidden;
      }
      .success-point {
        background: @success-status-color;
      }
      .error-point {
        background: @error-status-color;
      }
      .configured-point {
        background: @configured-status-color;
      }
    }

    &-right {
      // width: 40px;
      position: relative;
      display: flex;
      justify-content: space-between;
      align-items: center;
      .icon-gj {
        position: relative;
        top: -2px;
      }
      .menu-sub-icon{
        position: relative;
        // top: 2px;
        margin: 0 6px 0 8px;
        transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
        
        &:hover {
          transform: rotate(90deg) scale(1.1);
          color: @primary-color;
        }
        
        /* 直接作用于SVG元素 */
        svg {
          transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
        }
        
        &:hover svg {
          transform: rotate(90deg) scale(1.1);
        }
      }
      .cluster-more {
        margin-right: 0px;
        margin-left: 0px;
        transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
        cursor: pointer;
        
        &:hover {
          transform: rotate(90deg) scale(1.1) !important;
          color: @primary-color !important;
        }
        
        /* 直接作用于SVG元素 */
        svg {
          transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
        }
        
        &:hover svg {
          transform: rotate(90deg) scale(1.1) !important;
        }
      }
      
      /* 添加新类专门用于旋转效果 */
      .rotate-on-hover {
        /* 图标容器样式 */
        transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
        
        /* 适用于所有子元素，包括SVG和path */
        * {
          transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
        }
        
        /* 悬停时旋转整个图标及其子元素 */
        &:hover {
          transform: rotate(90deg) scale(1.1) !important;
          color: @primary-color !important;
        }
      }
    }
  }
}
/deep/.ant-popover-placement-rightTop
  > .ant-popover-content
  > .ant-popover-arrow {
  display: none;
}
.popover-index {
  // margin-left: 5px;
  .more-menu-btn {
    font-size: 14px;
    color: #555555;
    letter-spacing: 0.39px;
    line-height: 32px;
    font-weight: 400;
    transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    position: relative;
    
    &:hover {
      color: @primary-color;
      background-color: rgba(0, 122, 255, 0.05);
      transform: translateX(3px);
    }
    
    &:active {
      transform: translateX(0) scale(0.98);
    }
  }
  /deep/ .ant-popover-inner-content {
    text-align: left;
    padding: 12px 16px;
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.2), 0 6px 16px rgba(0, 0, 0, 0.15), 0 2px 6px rgba(0, 0, 0, 0.1);
    border: 1px solid rgba(0, 0, 0, 0.08);
    animation: popoverFadeIn 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    transform-origin: top right;
    backdrop-filter: blur(10px);
    -webkit-backdrop-filter: blur(10px);
  }
}

@keyframes popoverFadeIn {
  0% {
    opacity: 0;
    transform: translateY(-15px) scale(0.95);
    filter: blur(4px);
  }
  50% {
    opacity: 0.8;
    transform: translateY(-5px) scale(0.98);
    filter: blur(1px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
    filter: blur(0);
  }
}

@keyframes menuItemAppear {
  0% {
    opacity: 0;
    transform: translateX(-15px) translateY(5px) scale(0.9);
    filter: blur(2px);
  }
  60% {
    opacity: 0.8;
    transform: translateX(2px) translateY(-1px) scale(1.02);
    filter: blur(0.5px);
  }
  100% {
    opacity: 1;
    transform: translateX(0) translateY(0) scale(1);
    filter: blur(0);
  }
}

/* 添加全局选择器确保能影响到Ant Design组件内部的元素 */
/deep/ .rotate-on-hover {
  &:hover {
    .anticon {
      transform: rotate(90deg) scale(1.1) !important;
    }
    
    svg {
      transform: rotate(90deg) scale(1.1) !important;
    }
  }
}

/* 新增旋转图标容器样式 */
.rotating-icon-container {
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  padding: 4px;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  margin: 0 6px 0 8px;
  position: relative;

  &:hover {
    background-color: rgba(25, 118, 210, 0.1);
  }
}

.rotating-icon-svg {
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  width: 16px;
  height: 16px;
}

/* 旋转中的类 */
.rotating {
  transform: rotate(90deg) scale(1.1);
  color: #1976d2;
}

/* 添加动画特效 */
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(25, 118, 210, 0.4);
  }
  70% {
    box-shadow: 0 0 0 5px rgba(25, 118, 210, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(25, 118, 210, 0);
  }
}
</style>
