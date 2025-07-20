<template>
  <div class="modern-header">
    <!-- 毛玻璃背景层 -->
    <div class="header-backdrop"></div>
    
    <div class="header-container">
      <!-- 左侧区域：Logo + 左对齐菜单 -->
      <div class="header-left">
        <!-- Logo区域 -->
        <div class="logo-section" @click="goToHome">
          <div class="logo-wrapper">
            <img src="@/assets/img/logo.png" alt="Logo" />
          </div>
          <div class="brand-info">
            <h1 class="brand-title">{{systemName}}</h1>
          </div>
        </div>
        
        <!-- 左对齐导航菜单 -->
        <nav class="left-navigation">
          <ul class="nav-menu">
            <li
              v-for="item in regularMenus"
              :key="item.fullPath"
              :class="[
                'nav-item', 
                { 
                  'active': item.fullPath === activeFirstMenuKey, 
                  'has-submenu': hasChildren(item) && item.path !== 'service-manage',
                  'expanded': hoveredMenu === item.fullPath || expandedMenu === item.fullPath
                }
              ]"
              @mouseenter="handleMenuEnter(item)"
              @mouseleave="handleMenuLeave(item)"
              @click="onLeftMenuClick(item)"
            >
              <div class="nav-link">
                <div class="nav-icon">
                  <svg-icon v-if="item.path === 'service-manage'" icon-class="home" />
                  <svg-icon v-else-if="item.meta && item.meta.icon" :icon-class="item.meta.icon" />
                </div>
                <span class="nav-text">
                  {{ item.path === 'service-manage' ? '主页' : item.name }}
                </span>
                <div class="nav-arrow" v-if="hasChildren(item) && item.path !== 'service-manage'">
                  <a-icon type="down" />
                </div>
              </div>
              
              <!-- 子菜单下拉面板 -->
              <transition name="submenu-slide">
                <div
                  v-if="(hoveredMenu === item.fullPath || expandedMenu === item.fullPath) && item.path !== 'service-manage' && hasChildren(item)"
                  class="submenu-panel"
                  @mouseenter="keepSubmenuOpen(item)"
                  @mouseleave="handleSubmenuLeave(item)"
                >
                  <div class="submenu-container">
                    <div class="submenu-header">
                      <span class="submenu-title">{{ item.name }}</span>
                      <div class="submenu-close" @click.stop="closeSubmenu(item)">
                        <a-icon type="close" />
                      </div>
                    </div>
                    <ul class="submenu-list">
                      <li
                        v-for="subItem in getMenuChildren(item)"
                        :key="subItem.fullPath"
                        :class="['submenu-item', { 'active': $route.path.includes(subItem.fullPath) }]"
                        @click.stop="onSubMenuSelect(subItem, $event)"
                      >
                        <a :href="'#' + subItem.fullPath" class="submenu-link">
                          <div class="submenu-icon">
                            <svg-icon :icon-class="subItem.meta && subItem.meta.icon ? subItem.meta.icon : 'menu-default'" />
                          </div>
                          <span class="submenu-text">{{subItem.name || subItem.label}}</span>
                          <div class="submenu-badge" v-if="subItem.badge">
                            <span>{{ subItem.badge }}</span>
                          </div>
                        </a>
                      </li>
                    </ul>
                  </div>
                </div>
              </transition>
            </li>
          </ul>
        </nav>
      </div>

      <!-- 中间区域：服务状态指示器 -->
      <div class="header-center">
        <transition name="service-fade">
          <div class="service-status-indicator" v-show="shouldShowTitle">
            <div class="service-card">
              <div class="service-icon-wrapper">
                <div class="service-icon">
                  <svg-icon v-if="isOverviewPage" icon-class="dashboard" />
                  <svg-icon v-else-if="currentServiceIcon !== 'service-default'" :icon-class="currentServiceIcon" />
                </div>
                <div 
                  v-if="!isOverviewPage && currentServiceName" 
                  class="service-status-dot" 
                  :class="{
                    'status-running': currentServiceStatus === 2, 
                    'status-warning': currentServiceStatus === 3, 
                    'status-error': currentServiceStatus === 4
                  }"
                ></div>
              </div>
              <div class="service-details">
                <span class="service-title">{{ displayTitle }}</span>
                <span class="service-subtitle" v-if="!isOverviewPage && currentServiceName">
                  {{ getServiceStatusText(currentServiceStatus) }}
                </span>
              </div>
            </div>
          </div>
        </transition>
      </div>

      <!-- 右侧区域：管理菜单 + 快捷操作 -->
      <div class="header-right">
        <!-- 右对齐管理菜单 -->
        <nav class="right-navigation">
          <ul class="admin-menu">
            <li
              v-for="item in adminMenus"
              :key="item.fullPath"
              :class="[
                'admin-item', 
                { 
                  'active': item.fullPath === activeFirstMenuKey, 
                  'has-submenu': hasChildren(item),
                  'expanded': hoveredMenu === item.fullPath || expandedMenu === item.fullPath
                }
              ]"
              @mouseenter="handleMenuEnter(item)"
              @mouseleave="handleMenuLeave(item)"
              @click="onLeftMenuClick(item)"
            >
              <div class="admin-link">
                <div class="admin-icon">
                  <svg-icon v-if="item.path === 'colony-manage'" icon-class="colony" />
                  <svg-icon v-else-if="item.path === 'security-center'" icon-class="user_manager" />
                </div>
                <span class="admin-text">{{item.name}}</span>
                <div class="admin-arrow" v-if="hasChildren(item)">
                  <a-icon type="down" />
                </div>
              </div>
              
              <!-- 管理子菜单下拉面板 -->
              <transition name="submenu-slide">
                <div
                  v-if="(hoveredMenu === item.fullPath || expandedMenu === item.fullPath) && hasChildren(item)"
                  class="admin-submenu-panel"
                  @mouseenter="keepSubmenuOpen(item)"
                  @mouseleave="handleSubmenuLeave(item)"
                >
                  <div class="admin-submenu-container">
                    <div class="submenu-header">
                      <span class="submenu-title">{{ item.name }}</span>
                      <div class="submenu-close" @click.stop="closeSubmenu(item)">
                        <a-icon type="close" />
                      </div>
                    </div>
                    <ul class="admin-submenu-list">
                      <li
                        v-for="subItem in getMenuChildren(item)"
                        :key="subItem.fullPath"
                        :class="['admin-submenu-item', { 'active': $route.path.includes(subItem.fullPath) }]"
                        @click.stop="onSubMenuSelect(subItem, $event)"
                      >
                        <a :href="'#' + subItem.fullPath" class="admin-submenu-link">
                          <div class="submenu-icon">
                            <svg-icon :icon-class="subItem.meta && subItem.meta.icon ? subItem.meta.icon : 'menu-default'" />
                          </div>
                          <span class="submenu-text">{{subItem.name || subItem.label}}</span>
                          <div class="submenu-badge" v-if="subItem.badge">
                            <span>{{ subItem.badge }}</span>
                          </div>
                        </a>
                      </li>
                    </ul>
                  </div>
                </div>
              </transition>
            </li>
          </ul>
        </nav>
        
        <!-- 快捷操作区域 -->
        <div class="quick-actions">
          <!-- 集群选择器 -->
          <a-dropdown class="action-dropdown cluster-selector" placement="bottomRight">
            <div class="action-button">
              <div class="action-icon">
                <svg-icon icon-class="cluster" />
              </div>
              <span class="action-text">{{ currentCluster.name || 'bdp' }}</span>
              <div class="action-arrow">
                <a-icon type="down" />
              </div>
            </div>
            <a-menu slot="overlay" class="cluster-menu">
              <a-menu-item 
                v-for="item in runningCluster" 
                :key="item.value" 
                @click="changeCluster({key: item.value})"
                :class="{ 'selected': item.value === clusterId }"
              >
                <div class="cluster-item">
                  <svg-icon icon-class="cluster" />
                  <span>{{ item.label }}</span>
                  <a-icon v-if="item.value === clusterId" type="check" class="check-icon" />
                </div>
              </a-menu-item>
            </a-menu>
          </a-dropdown>

          <!-- 设置按钮 -->
          <div class="action-button settings-action" v-if="isCluster === 'isCluster'">
            <cluster-setting />
          </div>

          <!-- 告警按钮 -->
          <div class="action-button alarm-action" v-if="isCluster === 'isCluster'">
            <alarm-manage />
          </div>

          <!-- 用户头像 -->
          <div class="action-button user-action">
            <header-avatar />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import HeaderAvatar from "./HeaderAvatar";
// import IMenu from "@/components/menu/menu";
import ClusterSetting from './clusterSetting';
import AlarmManage from './alarmManage.vue'
import { mapState, mapMutations, mapGetters } from "vuex";
export default {
  name: "AdminHeader",
  components: { HeaderAvatar, ClusterSetting, AlarmManage },
  props: {
    firstMenu: Array,
    activeFirstMenuKey: String
  },
  data() {
    return {
      langList: [
        { key: "CN", name: "简体中文", alias: "简体" },
        // {key: 'HK', name: '繁體中文', alias: '繁體'},
        { key: "US", name: "English", alias: "English" },
      ],
      searchActive: false,
      hoveredMenu: '', // 当前鼠标悬浮的菜单
      expandedMenu: '', // 当前展开的菜单（点击展开）
      menuTimeouts: {}, // 存储菜单延迟关闭的定时器
      cachedServiceData: null, // 缓存服务数据
      cachedMenuData: null, // 缓存菜单数据
    };
  },
  computed: {
    ...mapGetters('setting', ['isCluster', 'runningCluster', 'clusterId']),
    ...mapState("setting", [
      "theme",
      "isMobile",
      "layout",
      "systemName",
      "lang",
      "pageWidth",
      "serviceId"
    ]),
    headerTheme() {
      if (
        this.layout == "side" &&
        this.theme.mode == "dark" &&
        !this.isMobile
      ) {
        return "light";
      }
      return this.theme.mode;
    },
    langAlias() {
      let lang = this.langList.find((item) => item.key == this.lang);
      return lang.alias;
    },
    menuWidth() {
      const { layout, searchActive } = this;
      const headWidth = layout === "head" ? "100% - 188px" : "100%";
      const extraWidth = searchActive ? "600px" : "400px";
      return `calc(${headWidth} - ${extraWidth})`;
    },
    currentCluster () {
      let arr = this.runningCluster.filter(item => item.value === Number(this.clusterId)) || []
      return {
        name: arr.length > 0 ? arr[0].label : '',
        clusterId: this.clusterId
      }
    },
    regularMenus() {
      if (!this.firstMenu) return [];
      // 排除集群管理、用户管理以及Datasophon总览
      const excludePaths = ['colony-manage', 'security-center', 'datasophon-overview'];
      return this.firstMenu.filter(item => !excludePaths.includes(item.path));
    },
    adminMenus() {
      if (!this.firstMenu) return [];
      // 仅包含集群管理和用户管理，确保服务管理不会包含在内
      const adminMenuPaths = ['colony-manage', 'security-center'];
      return this.firstMenu.filter(item => adminMenuPaths.includes(item.path));
    },
    currentServiceName() {
      // 首先尝试从Vuex中获取serviceId
      let serviceId = this.serviceId;
      
      // 如果Vuex中没有，尝试从路由参数中获取
      if (!serviceId && this.$route.params && this.$route.params.serviceId) {
        serviceId = this.$route.params.serviceId;
        // 在计算属性外部更新Vuex
        this.$nextTick(() => {
          this.$store.commit('setting/setServiceId', serviceId);
        });
      }
      
      // 如果有serviceId并且路径包含服务列表
      if (serviceId && this.$route.path.includes('/service-manage/service-list/')) {
        const serviceData = this.getCachedServiceData(serviceId);
        // 优先使用displayName，然后是name，最后是serviceName
        if (serviceData && serviceData.service) {
          return serviceData.service.displayName || serviceData.service.name || '';
        }
        return '';
      }
      return '';
    },
    currentServiceIcon() {
      // 获取当前服务图标
      let serviceId = this.serviceId || (this.$route.params && this.$route.params.serviceId);
      
      if (serviceId && this.$route.path.includes('/service-manage/service-list/')) {
        const serviceData = this.getCachedServiceData(serviceId);
        if (serviceData && serviceData.service) {
          // 优先使用serviceName，因为图标通常基于技术名称
          const iconName = serviceData.service.serviceName || serviceData.service.name || '';
          return iconName.toLowerCase() || 'service-default';
        }
        return 'service-default';
      }
      return 'service-default';
    },
    currentServiceStatus() {
      // 获取当前服务状态
      let serviceId = this.serviceId || (this.$route.params && this.$route.params.serviceId);
      
      if (serviceId && this.$route.path.includes('/service-manage/service-list/')) {
        const serviceData = this.getCachedServiceData(serviceId);
        return serviceData && serviceData.service && serviceData.service.meta && serviceData.service.meta.obj 
          ? serviceData.service.meta.obj.serviceStateCode 
          : 1;
      }
      return 1;
    },
    shouldShowTitle() {
      // 当为总览页面或有服务名称时显示标题
      return this.isOverviewPage || !!this.currentServiceName;
    },
    displayTitle() {
      if (this.isOverviewPage) {
        return '集群总览';
      }
      // 如果是服务详情页，显示服务名称
      if (this.$route.path.includes('/service-manage/service-list/') && this.currentServiceName) {
        return this.currentServiceName;
      }
      return '';
    },
    shouldShowServiceTitle() {
      return !!this.currentServiceName && this.$route.path.includes('/service-manage/service-list/');
    },
    isOverviewPage() {
      return this.$route.path === '/service-manage' || this.$route.path === '/service-manage/';
    }
  },
  methods: {
    // 获取缓存的菜单数据
    getCachedMenuData() {
      if (!this.cachedMenuData) {
        this.cachedMenuData = JSON.parse(localStorage.getItem('menuData')) || [];
      }
      return this.cachedMenuData;
    },
    
    // 获取缓存的服务数据
    getCachedServiceData(serviceId) {
      // 如果缓存不存在或者serviceId变化，则重新获取
      if (!this.cachedServiceData || this.cachedServiceData.serviceId !== serviceId) {
        const menuData = this.getCachedMenuData();
        
        const serviceManageMenu = menuData.find(item => item.path === 'service-manage');
        
        if (serviceManageMenu && serviceManageMenu.children) {
          
          // 直接查找匹配的服务
          const service = serviceManageMenu.children.find(
            item => item.meta && item.meta.params && item.meta.params.serviceId === serviceId
          );
          
          
          // 如果没有找到匹配的服务，尝试使用字符串匹配
          if (!service && serviceId) {
            const serviceWithStringId = serviceManageMenu.children.find(
              item => item.meta && item.meta.params && item.meta.params.serviceId && 
                     item.meta.params.serviceId.toString() === serviceId.toString()
            );
            
            if (serviceWithStringId) {
              this.cachedServiceData = {
                serviceId,
                service: serviceWithStringId
              };
              return this.cachedServiceData;
            }
          } else if (service) {
            this.cachedServiceData = {
              serviceId,
              service
            };
            return this.cachedServiceData;
          }
        } else {
          // 从serviceList尝试获取
          try {
            const serviceList = JSON.parse(localStorage.getItem('serviceList') || '[]');
            if (serviceList.length > 0) {
              
              const service = serviceList.find(s => s.id.toString() === serviceId.toString());
              
              if (service) {
                
                this.cachedServiceData = {
                  serviceId,
                  service: {
                    name: service.serviceName,
                    label: service.label || service.serviceName,
                    meta: { obj: service }
                  }
                };
                return this.cachedServiceData;
              }
            }
          } catch (e) {
            // 忽略错误
          }
        }
      }
      
      return this.cachedServiceData;
    },
    
    toggleCollapse() {
      this.$emit("toggleCollapse");
    },
    onSelect(obj) {
      this.$emit("menuSelect", obj);
    },
    // 处理菜单悬浮
    handleMenuEnter(item) {
      // 清除任何现有的关闭定时器
      if (this.menuTimeouts[item.fullPath]) {
        clearTimeout(this.menuTimeouts[item.fullPath]);
        delete this.menuTimeouts[item.fullPath];
      }
      
      // 设置当前悬浮菜单
      this.hoveredMenu = item.fullPath;
    },
    // 处理菜单离开
    handleMenuLeave(item) {
      // 设置延迟关闭，以便用户有时间移动到子菜单
      this.menuTimeouts[item.fullPath] = setTimeout(() => {
        if (this.hoveredMenu === item.fullPath) {
          this.hoveredMenu = '';
        }
      }, 300);
    },
    // 处理菜单点击
    onLeftMenuClick(item) {
      // 如果是主页菜单，使用与logo点击相同的处理方法
      if (item.path === 'service-manage') {
        this.goToHome(); // 直接调用goToHome方法确保行为一致
        this.$emit('firstMenuSelect', '/service-manage'); // 确保使用完整路径作为参数
        return;
      }
      
      // 如果没有子菜单，直接发出选择事件
      if (!this.hasChildren(item)) {
        this.$emit('firstMenuSelect', item.fullPath);
      } else {
        // 如果有子菜单，切换展开状态
        if (this.expandedMenu === item.fullPath) {
          // 如果已展开，则折叠
          this.expandedMenu = '';
          this.hoveredMenu = '';
        } else {
          // 如果未展开，则展开
          this.expandedMenu = item.fullPath;
          this.hoveredMenu = item.fullPath;
        }
      }
    },
    
    // 保持子菜单打开状态
    keepSubmenuOpen(item) {
      // 清除关闭定时器
      if (this.menuTimeouts[item.fullPath]) {
        clearTimeout(this.menuTimeouts[item.fullPath]);
        delete this.menuTimeouts[item.fullPath];
      }
    },
    
    // 处理子菜单离开
    handleSubmenuLeave(item) {
      // 只有在非展开状态下才设置延迟关闭
      if (this.expandedMenu !== item.fullPath) {
        this.menuTimeouts[item.fullPath] = setTimeout(() => {
          if (this.hoveredMenu === item.fullPath) {
            this.hoveredMenu = '';
          }
        }, 300);
      }
    },
    
    // 关闭子菜单
    closeSubmenu(item) {
      this.expandedMenu = '';
      this.hoveredMenu = '';
    },
    
    // 获取服务状态文本
    getServiceStatusText(status) {
      const statusMap = {
        1: '未知',
        2: '运行中',
        3: '警告',
        4: '错误',
        5: '停止'
      };
      return statusMap[status] || '未知';
    },
    
    // 检查菜单项是否有子菜单
    hasChildren(item) {
      // 对于主页菜单，始终返回false
      if (item.path === 'service-manage') {
        return false;
      }
      
      // 从完整的firstMenu中查找对应项
      const menuItem = this.firstMenu.find(m => m.fullPath === item.fullPath);
      return menuItem && menuItem.children && menuItem.children.length > 0;
    },
    
    // 获取菜单的子菜单
    getMenuChildren(item) {
      // 从完整的firstMenu中查找对应项的children
      const menuItem = this.firstMenu.find(m => m.fullPath === item.fullPath);
      return menuItem && menuItem.children ? menuItem.children : [];
    },
    
    // 处理子菜单点击
    onSubMenuSelect(subItem, event) {
      // 打印完整的子菜单项对象，以便调试
      // 关闭所有下拉菜单
      this.hoveredMenu = '';
      
      // 设置当前激活的一级菜单
      const parentMenu = this.firstMenu.find(item => 
        item.children && item.children.some(child => child.fullPath === subItem.fullPath)
      );
      if (parentMenu) {
        this.$emit('firstMenuSelect', parentMenu.fullPath);
      }
      
      // 通知父组件路由已经改变
      this.$emit('routeChanged', subItem.fullPath);
    },
    // 切换运行中的集群
    changeCluster (val) {
      if (this.clusterId === val.key) return false
      this.setClusterId(val.key)
      // 刷新服务列表
      this.$store.dispatch('setting/getRunningClusterList')
      // 使用goToHome确保行为一致
      this.goToHome();
    },
    ...mapMutations("setting", ["setLang", "setClusterId"]),
    goToHome() {
      // 跳转到主页
      if (this.$route.path !== '/service-manage') {
        this.$router.push("/service-manage").catch(err => {
          // 忽略重复导航错误
          if (err.name !== 'NavigationDuplicated') {
            throw err;
          }
        });
      }
    },
  },
  created() {
    if (this.firstMenu && this.firstMenu.length > 0) {
      this.activeFirstMenuKey = this.firstMenu[0].fullPath
    }
    
    // 如果当前路径是服务详情页面，预加载服务数据
    if (this.$route.path.includes('/service-manage/service-list/') && this.$route.params.serviceId) {
      const serviceId = this.$route.params.serviceId;
      
      // 确保serviceId已设置
      if (this.serviceId !== serviceId) {
        this.$store.commit('setting/setServiceId', serviceId);
      }
      
      // 预加载服务数据
      this.getCachedServiceData(serviceId);
    }
    
    // 添加调试信息
    // 菜单数据: this.firstMenu
    // 当前路径: this.$route.path
    // 当前serviceId: this.serviceId
    // 路由参数: this.$route.params
    
    setTimeout(() => {
      // 菜单数据(延迟检查): this.firstMenu
      // regularMenus: this.regularMenus
      // 当前服务名称: this.currentServiceName
      // 当前服务图标: this.currentServiceIcon
      // 当前服务状态: this.currentServiceStatus
    }, 1000);
  },
  watch: {
    '$route': {
      handler(to) {
        
        // 如果路由是服务详情页面，刷新serviceId
        if (to.path.includes('/service-manage/service-list/') && to.params.serviceId) {
          const serviceId = to.params.serviceId;
          
          // 如果当前serviceId不同，则更新
          if (this.serviceId !== serviceId) {
            this.$store.commit('setting/setServiceId', serviceId);
          }
          
          // 强制刷新计算属性
          this.cachedServiceData = null;
          this.cachedMenuData = null;
          
          // 预加载服务数据
          this.$nextTick(() => {
            const serviceData = this.getCachedServiceData(serviceId);
            // 当前服务名称: this.currentServiceName
          });
        }
      },
      immediate: true
    },
    // 监听serviceId变化
    serviceId: {
      handler(newVal, oldVal) {
        if (newVal && newVal !== oldVal) {
          // 清除缓存，强制重新获取服务数据
          this.cachedServiceData = null;
          
          // 预加载服务数据
          this.$nextTick(() => {
            const serviceData = this.getCachedServiceData(newVal);
            // 当前服务名称: this.currentServiceName
          });
        }
      }
    }
  }
};
</script>

<style lang="less" scoped>
@import "index";

/* 现代化导航栏 - 重新设计 */
.modern-header {
  position: relative;
  height: 64px;
  z-index: 1000;
  border-bottom: 1px solid rgba(0, 0, 0, 0.06);
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.02);
}

/* 毛玻璃背景层 */
.header-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px) saturate(180%);
  -webkit-backdrop-filter: blur(20px) saturate(180%);
  z-index: -1;
}

/* 主容器布局 */
.header-container {
  display: flex;
  align-items: center;
  height: 100%;
  width: 100%;
  padding: 0 24px;
  position: relative;
}

/* 左侧区域：Logo + 左对齐菜单 */
.header-left {
  display: flex;
  align-items: center;
  position: absolute;
  left: 24px;
  gap: 24px;
}

/* Logo区域样式 */
.logo-section {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 8px 12px;
  border-radius: 10px;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  &:hover {
    background: rgba(0, 0, 0, 0.03);
    transform: translateY(-1px);
  }
  
  &:active {
    transform: translateY(0);
    background: rgba(0, 0, 0, 0.05);
  }
}

.logo-wrapper {
  width: 32px;
  height: 32px;
  border-radius: 6px;
  overflow: hidden;
  margin-right: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: transparent; /* 移除背景色，保持原始Logo */
  
  img {
    width: 100%;
    height: 100%;
    object-fit: contain;
  }
}

.brand-info {
  .brand-title {
    font-size: 18px;
    font-weight: 600;
    color: #1a1a1a;
    margin: 0;
    line-height: 1.3;
    letter-spacing: -0.3px;
  }
}

/* 左对齐导航菜单 */
.left-navigation {
  .nav-menu {
    display: flex;
    align-items: center;
    list-style: none;
    margin: 0;
    padding: 0;
    gap: 2px;
  }
  
  .nav-item {
    position: relative;
    
    &.active .nav-link {
      background: linear-gradient(135deg, #007aff 0%, #0056d3 100%);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
      
      .nav-icon {
        color: #ffffff;
      }
      
      .nav-arrow {
        color: #ffffff;
      }
    }
    
    &.expanded .nav-link {
      background: rgba(0, 122, 255, 0.1);
      color: #007aff;
      
      .nav-icon {
        color: #007aff;
      }
      
      .nav-arrow {
        color: #007aff;
        transform: rotate(180deg);
      }
    }
    
    &:hover:not(.active):not(.expanded) .nav-link {
      background: rgba(0, 0, 0, 0.04);
      transform: translateY(-1px);
    }
  }
  
  .nav-link {
    display: flex;
    align-items: center;
    padding: 10px 16px;
    border-radius: 8px;
    text-decoration: none;
    color: #2c2c2c;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    cursor: pointer;
    gap: 8px;
    min-height: 40px;
    
    &:hover {
      text-decoration: none;
    }
  }
  
  .nav-icon {
    font-size: 16px;
    color: #666666;
    transition: all 0.3s ease;
    width: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
  }
  
  .nav-text {
    white-space: nowrap;
    font-weight: 500;
  }
  
  .nav-arrow {
    font-size: 10px;
    color: #999999;
    transition: all 0.3s ease;
    margin-left: 4px;
    width: 12px;
    height: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 3px;
    background: rgba(0, 0, 0, 0.04);
    
    .anticon {
      transition: transform 0.3s ease;
      font-size: 8px;
    }
  }
}

/* 中间区域：服务状态指示器 */
.header-center {
  display: flex;
  align-items: center;
  justify-content: center;
  position: absolute;
  left: 50%;
  transform: translateX(-50%);
}

.service-status-indicator {
  .service-card {
    display: flex;
    align-items: center;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
    backdrop-filter: blur(20px);
    border: 1px solid rgba(0, 122, 255, 0.15);
    border-radius: 20px;
    padding: 12px 20px;
    box-shadow: 
      0 4px 20px rgba(0, 122, 255, 0.08),
      0 1px 3px rgba(0, 0, 0, 0.05);
    gap: 12px;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    
    &:hover {
      transform: translateY(-1px);
      box-shadow: 
        0 6px 25px rgba(0, 122, 255, 0.12),
        0 2px 8px rgba(0, 0, 0, 0.08);
    }
  }
  
  .service-icon-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 36px;
    height: 36px;
    background: linear-gradient(135deg, #007aff 0%, #0056d3 100%);
    border-radius: 10px;
    box-shadow: 0 2px 8px rgba(0, 122, 255, 0.25);
  }
  
  .service-icon {
    font-size: 18px;
    color: #ffffff;
  }
  
  .service-status-dot {
    position: absolute;
    top: -2px;
    right: -2px;
    width: 12px;
    height: 12px;
    border-radius: 50%;
    border: 2px solid #ffffff;
    box-shadow: 0 1px 3px rgba(0, 0, 0, 0.2);
    
    &.status-running {
      background: #34c759;
      animation: pulse-success 2s infinite;
    }
    
    &.status-warning {
      background: #ff9500;
      animation: pulse-warning 2s infinite;
    }
    
    &.status-error {
      background: #ff3b30;
      animation: pulse-error 2s infinite;
    }
  }
  
  .service-details {
    display: flex;
    flex-direction: column;
    gap: 2px;
  }
  
  .service-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    white-space: nowrap;
    line-height: 1.2;
  }
  
  .service-subtitle {
    font-size: 12px;
    font-weight: 500;
    color: #666666;
    white-space: nowrap;
    line-height: 1.2;
  }
}

/* 右侧区域：管理菜单 + 快捷操作 */
.header-right {
  display: flex;
  align-items: center;
  position: absolute;
  right: 24px;
  gap: 20px;
}

/* 右对齐管理菜单 */
.right-navigation {
  .admin-menu {
    display: flex;
    align-items: center;
    list-style: none;
    margin: 0;
    padding: 0;
    gap: 2px;
  }
  
  .admin-item {
    position: relative;
    
    &.active .admin-link {
      background: linear-gradient(135deg, #007aff 0%, #0056d3 100%);
      color: #ffffff;
      box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
      
      .admin-icon {
        color: #ffffff;
      }
      
      .admin-arrow {
        color: #ffffff;
      }
    }
    
    &.expanded .admin-link {
      background: rgba(0, 122, 255, 0.1);
      color: #007aff;
      
      .admin-icon {
        color: #007aff;
      }
      
      .admin-arrow {
        color: #007aff;
        transform: rotate(180deg);
      }
    }
    
    &:hover:not(.active):not(.expanded) .admin-link {
      background: rgba(0, 0, 0, 0.04);
      transform: translateY(-1px);
    }
  }
  
  .admin-link {
    display: flex;
    align-items: center;
    padding: 10px 16px;
    border-radius: 8px;
    color: #2c2c2c;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    cursor: pointer;
    gap: 8px;
    min-height: 40px;
  }
  
  .admin-icon {
    font-size: 16px;
    color: #666666;
    transition: all 0.3s ease;
    width: 18px;
    height: 18px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
  }
  
  .admin-text {
    white-space: nowrap;
    font-weight: 500;
  }
  
  .admin-arrow {
    font-size: 10px;
    color: #999999;
    transition: all 0.3s ease;
    margin-left: 4px;
    width: 12px;
    height: 12px;
    display: flex;
    align-items: center;
    justify-content: center;
    border-radius: 3px;
    background: rgba(0, 0, 0, 0.04);
    
    .anticon {
      transition: transform 0.3s ease;
      font-size: 8px;
    }
  }
}

/* 快捷操作区域 */
.quick-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  
  .action-dropdown {
    &.cluster-selector {
      .action-button {
        background: rgba(0, 0, 0, 0.04);
        border: 1px solid rgba(0, 0, 0, 0.08);
        padding: 8px 12px;
        border-radius: 10px;
        display: flex;
        align-items: center;
        gap: 8px;
        cursor: pointer;
        transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
        min-height: 40px;
        
        &:hover {
          background: rgba(0, 0, 0, 0.06);
          transform: translateY(-1px);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
        }
        
        .action-icon {
          font-size: 14px;
          color: #666666;
          width: 16px;
          height: 16px;
          display: flex;
          align-items: center;
          justify-content: center;
          flex-shrink: 0;
        }
        
        .action-text {
          font-size: 13px;
          font-weight: 500;
          color: #2c2c2c;
          white-space: nowrap;
        }
        
        .action-arrow {
          font-size: 10px;
          color: #999999;
          transition: transform 0.3s ease;
        }
      }
    }
  }
  
  .action-button {
    &.settings-action,
    &.alarm-action,
    &.user-action {
      width: 40px;
      height: 40px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      background: rgba(0, 0, 0, 0.04);
      border: 1px solid rgba(0, 0, 0, 0.06);
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
      cursor: pointer;
      
      &:hover {
        background: rgba(0, 0, 0, 0.06);
        transform: translateY(-1px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
      
      .anticon,
      .svg-icon {
        font-size: 16px;
        color: #666666;
        width: 18px;
        height: 18px;
        display: flex;
        align-items: center;
        justify-content: center;
      }
    }
  }
}

/* 集群选择下拉菜单 */
.cluster-menu {
  .cluster-item {
    display: flex;
    align-items: center;
    gap: 8px;
    padding: 8px 12px;
    
    .svg-icon {
      font-size: 14px;
      color: #666666;
    }
    
    span {
      flex: 1;
      font-size: 14px;
      color: #2c2c2c;
    }
    
    .check-icon {
      font-size: 12px;
      color: #007aff;
    }
  }
  
  .ant-menu-item.selected {
    background: rgba(0, 122, 255, 0.08);
    
    .cluster-item {
      .svg-icon {
        color: #007aff;
      }
      
      span {
        color: #007aff;
        font-weight: 500;
      }
    }
  }
}

/* 子菜单面板样式 */
.submenu-panel {
  position: absolute;
  top: calc(100% + 8px);
  left: 0;
  min-width: 180px;
  max-width: 200px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.12),
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 1000;
  overflow: hidden;
  transform-origin: top left;
}

.admin-submenu-panel {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 160px;
  max-width: 180px;
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(24px) saturate(180%);
  border: 1px solid rgba(0, 0, 0, 0.06);
  border-radius: 16px;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.12),
    0 8px 24px rgba(0, 0, 0, 0.08),
    0 2px 8px rgba(0, 0, 0, 0.04);
  z-index: 1000;
  overflow: hidden;
  transform-origin: top right;
}

.submenu-container,
.admin-submenu-container {
  padding: 0;
}

.submenu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px 12px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.02) 0%, rgba(0, 122, 255, 0.05) 100%);
  
  .submenu-title {
    font-size: 15px;
    font-weight: 600;
    color: #1a1a1a;
    letter-spacing: -0.2px;
  }
  
  .submenu-close {
    width: 24px;
    height: 24px;
    border-radius: 6px;
    display: flex;
    align-items: center;
    justify-content: center;
    cursor: pointer;
    transition: all 0.3s ease;
    color: #666666;
    
    &:hover {
      background: rgba(0, 0, 0, 0.06);
      color: #333333;
    }
    
    .anticon {
      font-size: 12px;
    }
  }
}

.submenu-list,
.admin-submenu-list {
  list-style: none;
  margin: 0;
  padding: 12px;
}

.submenu-item,
.admin-submenu-item {
  border-radius: 10px;
  overflow: hidden;
  margin-bottom: 4px;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  &:last-child {
    margin-bottom: 0;
  }
  
  &.active {
    background: linear-gradient(135deg, #007aff 0%, #0056d3 100%);
    box-shadow: 0 4px 16px rgba(0, 122, 255, 0.25);
    transform: translateY(-1px);
    
    .submenu-link,
    .admin-submenu-link {
      color: #ffffff;
      
      .submenu-icon .svg-icon {
        color: #ffffff;
      }
      
      .submenu-badge {
        background: rgba(255, 255, 255, 0.2);
        color: #ffffff;
      }
    }
  }
  
  &:hover:not(.active) {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.06) 0%, rgba(0, 122, 255, 0.10) 100%);
    transform: translateY(-1px) translateX(4px);
    box-shadow: 0 2px 12px rgba(0, 122, 255, 0.15);
    
    .submenu-link,
    .admin-submenu-link {
      color: #007aff;
      
      .submenu-icon .svg-icon {
        color: #007aff;
      }
    }
  }
}

.submenu-link,
.admin-submenu-link {
  display: flex;
  align-items: center;
  padding: 14px 16px;
  color: #2c2c2c;
  text-decoration: none;
  font-size: 14px;
  font-weight: 500;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  gap: 12px;
  position: relative;
  
  .submenu-icon {
    width: 16px;
    height: 16px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    
    .svg-icon {
      font-size: 14px;
      color: #666666;
      transition: color 0.3s ease;
    }
  }
  
  .submenu-text {
    flex: 1;
    white-space: nowrap;
    font-weight: 500;
    letter-spacing: -0.1px;
  }
  
  .submenu-badge {
    padding: 2px 8px;
    background: rgba(0, 122, 255, 0.1);
    color: #007aff;
    border-radius: 12px;
    font-size: 11px;
    font-weight: 600;
    transition: all 0.3s ease;
    
    span {
      white-space: nowrap;
    }
  }
  
  &:hover {
    text-decoration: none;
  }
}

/* 子菜单动画 */
.submenu-slide-enter-active,
.submenu-slide-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.submenu-slide-enter {
  opacity: 0;
  transform: translateY(-12px) scale(0.95);
}

.submenu-slide-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.submenu-slide-leave {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.submenu-slide-leave-to {
  opacity: 0;
  transform: translateY(-8px) scale(0.98);
}

/* 下拉动画效果 */
.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.dropdown-enter {
  opacity: 0;
  transform: translateX(-50%) translateY(-16px) scale(0.92);
  filter: blur(4px);
}

.dropdown-enter-to {
  opacity: 1;
  transform: translateX(-50%) translateY(0) scale(1);
  filter: blur(0);
}

.dropdown-leave {
  opacity: 1;
  transform: translateX(-50%) translateY(0) scale(1);
  filter: blur(0);
}

.dropdown-leave-to {
  opacity: 0;
  transform: translateX(-50%) translateY(-12px) scale(0.95);
  filter: blur(2px);
}

/* 菜单项悬停动画 */
@keyframes menuItemPulse {
  0% {
    box-shadow: 0 0 0 0 rgba(0, 122, 255, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(0, 122, 255, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(0, 122, 255, 0);
  }
}

.nav-item.active .nav-link,
.admin-item.active .admin-link {
  animation: menuItemPulse 2s infinite;
}

/* 服务状态指示器动画 */
@keyframes statusGlow {
  0%, 100% {
    box-shadow: 0 0 0 0 rgba(0, 122, 255, 0.4);
  }
  50% {
    box-shadow: 0 0 0 8px rgba(0, 122, 255, 0);
  }
}

.service-status-indicator:hover {
  animation: statusGlow 2s ease-in-out;
}

/* 服务状态动画 */
.service-status-dot {
  &.status-running {
    background: linear-gradient(135deg, #34c759 0%, #30d158 100%);
    animation: pulseGreen 2.5s ease-in-out infinite;
  }
  
  &.status-warning {
    background: linear-gradient(135deg, #ff9500 0%, #ffb340 100%);
    animation: pulseOrange 2.5s ease-in-out infinite;
  }
  
  &.status-error {
    background: linear-gradient(135deg, #ff3b30 0%, #ff6b6b 100%);
    animation: pulseRed 2.5s ease-in-out infinite;
  }
  
  &.status-stopped {
    background: linear-gradient(135deg, #8e8e93 0%, #aeaeb2 100%);
    animation: pulseGray 3s ease-in-out infinite;
  }
}

@keyframes pulseGreen {
  0%, 100% {
    box-shadow: 
      0 0 0 0 rgba(52, 199, 89, 0.6),
      0 0 0 0 rgba(52, 199, 89, 0.3);
    transform: scale(1);
  }
  50% {
    box-shadow: 
      0 0 0 8px rgba(52, 199, 89, 0),
      0 0 0 16px rgba(52, 199, 89, 0);
    transform: scale(1.1);
  }
}

@keyframes pulseOrange {
  0%, 100% {
    box-shadow: 
      0 0 0 0 rgba(255, 149, 0, 0.6),
      0 0 0 0 rgba(255, 149, 0, 0.3);
    transform: scale(1);
  }
  50% {
    box-shadow: 
      0 0 0 8px rgba(255, 149, 0, 0),
      0 0 0 16px rgba(255, 149, 0, 0);
    transform: scale(1.1);
  }
}

@keyframes pulseRed {
  0%, 100% {
    box-shadow: 
      0 0 0 0 rgba(255, 59, 48, 0.6),
      0 0 0 0 rgba(255, 59, 48, 0.3);
    transform: scale(1);
  }
  50% {
    box-shadow: 
      0 0 0 8px rgba(255, 59, 48, 0),
      0 0 0 16px rgba(255, 59, 48, 0);
    transform: scale(1.1);
  }
}

@keyframes pulseGray {
  0%, 100% {
    box-shadow: 
      0 0 0 0 rgba(142, 142, 147, 0.4),
      0 0 0 0 rgba(142, 142, 147, 0.2);
    transform: scale(1);
  }
  50% {
    box-shadow: 
      0 0 0 6px rgba(142, 142, 147, 0),
      0 0 0 12px rgba(142, 142, 147, 0);
    transform: scale(1.05);
  }
}

.service-status {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background-color: #d1d1d6;
  transition: all 0.3s ease;
  
  &.running {
    background-color: #34c759;
    box-shadow: 0 0 6px rgba(52, 199, 89, 0.4);
    animation: pulse-green 2s infinite;
  }
  
  &.warning {
    background-color: #ff9500;
    box-shadow: 0 0 6px rgba(255, 149, 0, 0.4);
    animation: pulse-orange 2s infinite;
  }
  
  &.error {
    background-color: #ff3b30;
    box-shadow: 0 0 6px rgba(255, 59, 48, 0.4);
    animation: pulse-red 2s infinite;
  }
}

@keyframes pulse-green {
  0% {
    box-shadow: 0 0 0 0 rgba(52, 199, 89, 0.4);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(52, 199, 89, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(52, 199, 89, 0);
  }
}

@keyframes pulse-orange {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 149, 0, 0.4);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(255, 149, 0, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 149, 0, 0);
  }
}

@keyframes pulse-red {
  0% {
    box-shadow: 0 0 0 0 rgba(255, 59, 48, 0.4);
  }
  70% {
    box-shadow: 0 0 0 4px rgba(255, 59, 48, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(255, 59, 48, 0);
  }
}

/* 服务状态淡入淡出动画 */
.service-fade-enter-active,
.service-fade-leave-active {
  transition: all 0.4s cubic-bezier(0.25, 0.46, 0.45, 0.94);
}

.service-fade-enter {
  opacity: 0;
  transform: translateY(-8px) scale(0.95);
}

.service-fade-enter-to {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.service-fade-leave {
  opacity: 1;
  transform: translateY(0) scale(1);
}

.service-fade-leave-to {
  opacity: 0;
  transform: translateY(-4px) scale(0.98);
}

/* 响应式设计 */
@media (max-width: 1400px) {
  .header-container {
    max-width: 1200px;
    padding: 0 20px;
  }
  
  .header-left {
    gap: 24px;
  }
  
  .left-navigation .nav-menu {
    gap: 1px;
  }
  
  .right-navigation .admin-menu {
    gap: 1px;
  }
}

@media (max-width: 1200px) {
  .header-container {
    padding: 0 16px;
  }
  
  .header-left {
    gap: 20px;
  }
  
  .nav-link,
  .admin-link {
    padding: 8px 12px;
    font-size: 13px;
  }
  
  .service-status-indicator .service-card {
    padding: 10px 16px;
    
    .service-details {
      .service-title {
        font-size: 14px;
      }
      
      .service-subtitle {
        font-size: 11px;
      }
    }
  }
}

@media (max-width: 992px) {
  .modern-header {
    height: 56px;
  }
  
  .header-container {
    padding: 0 12px;
  }
  
  .brand-info {
    display: none;
  }
  
  .nav-text,
  .admin-text {
    display: none;
  }
  
  .service-details {
    display: none;
  }
  
  .quick-actions {
    gap: 6px;
    
    .action-dropdown.cluster-selector .action-button {
      .action-text {
        display: none;
      }
    }
  }
}

@media (max-width: 768px) {
  .modern-header {
    height: 52px;
  }
  
  .header-container {
    padding: 0 8px;
    gap: 12px;
  }
  
  .left-navigation {
    display: none;
  }
  
  .right-navigation .admin-menu {
    gap: 2px;
  }
  
  .service-status-indicator .service-card {
    padding: 8px 12px;
    
    .service-icon-wrapper {
      width: 28px;
      height: 28px;
    }
  }
  
  .quick-actions {
    gap: 4px;
    
    .action-button {
      width: 36px;
      height: 36px;
    }
  }
}

@media (max-width: 480px) {
  .header-container {
    padding: 0 6px;
    gap: 8px;
  }
  
  .logo-section {
    padding: 6px 8px;
  }
  
  .service-status-indicator .service-card {
    padding: 6px 10px;
  }
  
  .quick-actions {
    gap: 2px;
    
    .action-button {
      width: 32px;
      height: 32px;
    }
  }
}

/* 深色模式支持 */
@media (prefers-color-scheme: dark) {
  .modern-header {
    border-bottom-color: rgba(255, 255, 255, 0.08);
  }
  
  .header-backdrop {
    background: rgba(28, 28, 30, 0.90);
    backdrop-filter: blur(20px) saturate(180%);
  }
  
  .brand-info .brand-title {
    color: #f2f2f7;
  }
  
  .nav-link,
  .admin-link {
    color: #f2f2f7;
    
    &:hover:not(.active) {
      background: rgba(255, 255, 255, 0.08);
    }
  }
  
  .nav-icon,
  .admin-icon {
    color: #8e8e93;
  }
  
  .nav-text,
  .admin-text {
    color: #f2f2f7;
  }
  
  .nav-item.active .nav-link,
  .admin-item.active .admin-link {
    background: linear-gradient(135deg, #0a84ff 0%, #007aff 100%);
    color: #ffffff;
    
    .nav-icon,
    .admin-icon {
      color: #ffffff;
    }
  }
  
  .service-status-indicator .service-card {
    background: linear-gradient(135deg, rgba(44, 44, 46, 0.95) 0%, rgba(28, 28, 30, 0.95) 100%);
    border-color: rgba(255, 255, 255, 0.12);
    
    .service-title {
      color: #f2f2f7;
    }
    
    .service-subtitle {
      color: #8e8e93;
    }
  }
  
  .submenu-panel,
  .admin-submenu-panel {
    background: rgba(28, 28, 30, 0.95);
    border-color: rgba(255, 255, 255, 0.08);
    
    .submenu-header {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.12) 100%);
      border-bottom-color: rgba(255, 255, 255, 0.08);
      
      .submenu-title {
        color: #f2f2f7;
      }
      
      .submenu-close {
        color: #8e8e93;
        
        &:hover {
          background: rgba(255, 255, 255, 0.08);
          color: #f2f2f7;
        }
      }
    }
  }
  
  .submenu-link,
  .admin-submenu-link {
    color: #f2f2f7;
    
    .submenu-icon {
      background: rgba(255, 255, 255, 0.08);
      
      .svg-icon {
        color: #8e8e93;
      }
    }
    
    .submenu-text {
      color: #f2f2f7;
    }
  }
  
  .submenu-item:hover:not(.active),
  .admin-submenu-item:hover:not(.active) {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.12) 0%, rgba(0, 122, 255, 0.16) 100%);
    
    .submenu-link,
    .admin-submenu-link {
      color: #0a84ff;
      
      .submenu-icon .svg-icon {
        color: #0a84ff;
      }
    }
  }
  
  .quick-actions {
    .action-dropdown.cluster-selector .action-button {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.12);
      
      &:hover {
        background: rgba(255, 255, 255, 0.12);
      }
      
      .action-icon {
        color: #8e8e93;
      }
      
      .action-text {
        color: #f2f2f7;
      }
      
      .action-arrow {
        color: #8e8e93;
      }
    }
    
    .action-button {
      background: rgba(255, 255, 255, 0.08);
      border-color: rgba(255, 255, 255, 0.08);
      
      &:hover {
        background: rgba(255, 255, 255, 0.12);
      }
      
      .anticon,
      .svg-icon {
        color: #8e8e93;
      }
    }
  }
  
  .cluster-menu {
    background: rgba(28, 28, 30, 0.95);
    border-color: rgba(255, 255, 255, 0.08);
    
    .cluster-item {
      .svg-icon {
        color: #8e8e93;
      }
      
      span {
        color: #f2f2f7;
      }
      
      .check-icon {
        color: #0a84ff;
      }
    }
    
    .ant-menu-item.selected {
      background: rgba(0, 122, 255, 0.12);
      
      .cluster-item {
        .svg-icon {
          color: #0a84ff;
        }
        
        span {
          color: #0a84ff;
        }
      }
    }
  }
}
</style>