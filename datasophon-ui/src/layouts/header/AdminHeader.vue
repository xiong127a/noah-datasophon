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
                          <div class="submenu-icon" v-html="subItem.inlineSvg"></div>
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
                          <div class="submenu-icon" v-html="subItem.inlineSvg"></div>
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
        
        <!-- 快捷操作区域 - 重新设计 -->

        <div class="quick-actions">
          <!-- 集群选择器 - 彻底重构 -->
          <div class="cluster-selector-wrapper" ref="clusterDropdown">
            <a-dropdown 
              class="cluster-dropdown" 
              placement="bottomCenter" 
              v-if="isCluster === 'isCluster'"
              :trigger="['click']"
              overlayClassName="custom-cluster-dropdown"
              :getPopupContainer="() => $refs.clusterDropdown"
            >
            <div class="cluster-selector">
                <div class="cluster-icon">
                  <img v-if="currentCluster.isK8s" src="@/assets/images/kubernetes-logo.svg" class="cluster-svg-icon" />
                  <img v-else src="@/assets/img/os-logos/linux-tux.svg" class="cluster-svg-icon" />
                </div>
                <span class="cluster-name">{{ currentCluster.name || 'bdp' }}</span>
                <a-icon type="down" class="dropdown-icon" />
              </div>
              
            <a-menu slot="overlay" class="cluster-menu">
              <a-menu-item 
                v-for="item in runningCluster" 
                :key="item.value" 
                @click="changeCluster({key: item.value})"
                :class="{ 'selected': item.value === clusterId }"
              >
                <div class="cluster-item">
                    <img 
                      v-if="isK8sCluster(item)"
                      src="@/assets/images/kubernetes-logo.svg" 
                      class="menu-svg-icon" 
                    />
                    <img 
                      v-else
                      src="@/assets/img/os-logos/linux-tux.svg" 
                      class="menu-svg-icon" 
                    />
                  <span>{{ item.label }}</span>
                  <a-icon v-if="item.value === clusterId" type="check" class="check-icon" />
                </div>
              </a-menu-item>
            </a-menu>
          </a-dropdown>
          </div>

          <!-- 操作按钮组 -->
          <div class="action-buttons">
            <!-- 历史操作按钮 -->
            <div class="action-btn alarm-btn" v-if="isCluster === 'isCluster'" title="历史操作">
              <cluster-setting />
            </div>

            <!-- 告警按钮 -->
            <div class="action-btn alarm-btn" v-if="isCluster === 'isCluster'" title="告警管理">
              <alarm-manage />
            </div>

            <!-- 用户信息区域 - 重新设计 -->
            <div class="user-section">
              <header-avatar />
            </div>
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
      hoveredMenu: null,
      expandedMenu: null,
      menuHoverTimer: null,
      menuCloseTimer: null,
      menuHoverDelay: 100,   // 悬浮触发延迟（毫秒）
      menuCloseDelay: 300,   // 关闭延迟（毫秒）
      menuTimeouts: {}, // 存储菜单延迟关闭的定时器
      cachedServiceData: null, // 缓存服务数据
      cachedMenuData: null, // 缓存菜单数据
      currentClusterDetails: null, // 当前集群详细信息
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
      console.log("所有集群数据:", JSON.stringify(this.runningCluster));
      let arr = this.runningCluster.filter(item => item.value === Number(this.clusterId)) || []
      console.log("当前选中集群:", arr.length > 0 ? JSON.stringify(arr[0]) : "无数据");
      console.log("集群详细信息:", this.currentClusterDetails ? JSON.stringify(this.currentClusterDetails) : "无详细数据");
      
      const currentItem = arr.length > 0 ? arr[0] : null;
      
      // 判断是否是k8s类型
      let isK8s = false;
      
      // 优先使用详细信息中的depType
      if (this.currentClusterDetails && this.currentClusterDetails.depType) {
        isK8s = this.isK8sCluster({depType: this.currentClusterDetails.depType});
        console.log("使用详细信息判断集群类型:", this.currentClusterDetails.depType, "是K8s:", isK8s);
      } else if (currentItem) {
        // 如果没有详细信息，使用列表中的信息
        isK8s = this.isK8sCluster(currentItem);
        console.log("使用列表信息判断集群类型:", currentItem, "是K8s:", isK8s);
      }
      
      return {
        name: currentItem ? currentItem.label : '',
        clusterId: this.clusterId,
        isK8s: isK8s
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
      clearTimeout(this.menuCloseTimer);
      
      if (this.hoveredMenu !== item.fullPath) {
        this.menuHoverTimer = setTimeout(() => {
      this.hoveredMenu = item.fullPath;
        }, this.menuHoverDelay);
      }
    },
    // 处理菜单离开
    handleMenuLeave(item) {
      clearTimeout(this.menuHoverTimer);
      
      if (this.expandedMenu !== item.fullPath) {
        this.menuCloseTimer = setTimeout(() => {
        if (this.hoveredMenu === item.fullPath) {
            this.hoveredMenu = null;
        }
        }, this.menuCloseDelay);
      }
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
      clearTimeout(this.menuCloseTimer);
      this.hoveredMenu = item.fullPath;
    },
    
    // 处理子菜单离开
    handleSubmenuLeave(item) {
      if (this.expandedMenu !== item.fullPath) {
        this.menuCloseTimer = setTimeout(() => {
          if (this.hoveredMenu === item.fullPath) {
            this.hoveredMenu = null;
          }
        }, this.menuCloseDelay);
      }
    },
    
    // 关闭子菜单
    closeSubmenu(item) {
      this.hoveredMenu = null;
      this.expandedMenu = null;
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
      let children = menuItem && menuItem.children ? menuItem.children : [];
      
      // 为子菜单项创建临时内联SVG图标
      const createInlineSvg = (iconType) => {
        switch(iconType) {
          case 'cluster':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><rect x="2" y="2" width="20" height="8" rx="2" ry="2"></rect><rect x="2" y="14" width="20" height="8" rx="2" ry="2"></rect><line x1="6" y1="6" x2="6.01" y2="6"></line><line x1="6" y1="18" x2="6.01" y2="18"></line></svg>`;
            
          case 'user':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20 21v-2a4 4 0 0 0-4-4H8a4 4 0 0 0-4 4v2"></path><circle cx="12" cy="7" r="4"></circle></svg>`;
            
          case 'settings':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="3"></circle><path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1-1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z"></path></svg>`;
            
          case 'file':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z"></path><polyline points="14 2 14 8 20 8"></polyline><line x1="16" y1="13" x2="8" y2="13"></line><line x1="16" y1="17" x2="8" y2="17"></line><polyline points="10 9 9 9 8 9"></polyline></svg>`;
            
          case 'alert':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M10.29 3.86L1.82 18a2 2 0 0 0 1.71 3h16.94a2 2 0 0 0 1.71-3L13.71 3.86a2 2 0 0 0-3.42 0z"></path><line x1="12" y1="9" x2="12" y2="13"></line><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
            
          case 'notification':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M18 8A6 6 0 0 0 6 8c0 7-3 9-3 9h18s-3-2-3-9"></path><path d="M13.73 21a2 2 0 0 1-3.46 0"></path></svg>`;
            
          case 'tag':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M20.59 13.41l-7.17 7.17a2 2 0 0 1-2.83 0L2 12V2h10l8.59 8.59a2 2 0 0 1 0 2.82z"></path><line x1="7" y1="7" x2="7.01" y2="7"></line></svg>`;
            
          case 'log':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M14 3v4a1 1 0 0 0 1 1h4"></path><path d="M17 21h-10a2 2 0 0 1 -2 -2v-14a2 2 0 0 1 2 -2h7l5 5v11a2 2 0 0 1 -2 2z"></path><line x1="9" y1="9" x2="10" y2="9"></line><line x1="9" y1="13" x2="15" y2="13"></line><line x1="9" y1="17" x2="15" y2="17"></line></svg>`;
            
          case 'help':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><path d="M9.09 9a3 3 0 0 1 5.83 1c0 2-3 3-3 3"></path><line x1="12" y1="17" x2="12.01" y2="17"></line></svg>`;
            
          case 'metric':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><line x1="18" y1="20" x2="18" y2="10"></line><line x1="12" y1="20" x2="12" y2="4"></line><line x1="6" y1="20" x2="6" y2="14"></line></svg>`;
            
          case 'tenant':
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><path d="M17 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2"></path><circle cx="9" cy="7" r="4"></circle><path d="M23 21v-2a4 4 0 0 0-3-3.87"></path><path d="M16 3.13a4 4 0 0 1 0 7.75"></path></svg>`;
            
          default:
            return `<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"><circle cx="12" cy="12" r="10"></circle><line x1="12" y1="16" x2="12" y2="12"></line><line x1="12" y1="8" x2="12.01" y2="8"></line></svg>`;
        }
      };
      
      // 为每个子菜单添加合适的图标
      children = children.map(child => {
        // 创建meta对象（如果不存在）
        if (!child.meta) {
          child.meta = {};
        }
        
        // 设置内联SVG
        const pathSegment = child.path.split('/').pop();
        const name = (child.name || '').toLowerCase();
        
        // 根据菜单路径和名称分配图标类型
        let iconType = 'default';
        
        if (pathSegment === 'colony-list' || name.includes('集群管理')) {
          iconType = 'cluster';
        } 
        else if (pathSegment === 'colony-parcel' || name.includes('存储库')) {
          iconType = 'file';
        }
        else if (pathSegment === 'colony-frame' || name.includes('集群框架')) {
          iconType = 'cluster';
        }
        else if (pathSegment === 'notice' || name.includes('通知')) {
          iconType = 'notification';
        }
        else if (pathSegment === 'group' || name.includes('告警组')) {
          iconType = 'alert';
        }
        else if (pathSegment === 'metric' || name.includes('指标')) {
          iconType = 'metric';
        }
        else if (pathSegment === 'help' || name.includes('帮助')) {
          iconType = 'help';
        }
        else if (pathSegment === 'tenant' || name.includes('租户')) {
          iconType = 'tenant';
        }
        else if (pathSegment === 'user' || name.includes('用户')) {
          iconType = 'user';
        }
        else if (pathSegment === 'frame' || name.includes('机架')) {
          iconType = 'cluster';
        }
        else if (pathSegment === 'tag' || name.includes('标签')) {
          iconType = 'tag';
        }
        else if (pathSegment === 'log' || name.includes('日志')) {
          iconType = 'log';
        }
        else if (name.includes('设置') || pathSegment.includes('setting')) {
          iconType = 'settings';
        }
        
        // 设置内联SVG作为自定义属性
        child.inlineSvg = createInlineSvg(iconType);
        
        // 输出调试信息
        console.log(`菜单 ${child.name}(${pathSegment}) 设置图标类型: ${iconType}`);
        
        return child;
      });
      
      return children;
    },
    
    // 处理子菜单点击
    onSubMenuSelect(subItem, event) {
      // 打印完整的子菜单项对象，以便调试
      // 关闭所有下拉菜单
      this.hoveredMenu = '';
      this.expandedMenu = '';
      
      // 清除所有菜单定时器
      Object.keys(this.menuTimeouts).forEach(key => {
        clearTimeout(this.menuTimeouts[key]);
        delete this.menuTimeouts[key];
      });
      
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
    changeCluster ({key}) {
      // 如果点击的是当前已选集群，不做任何操作
      if (Number(this.clusterId) === Number(key)) {
        this.clusterVisible = false;
        return;
      }
      
      console.log("切换到集群ID:", key);
      
      // 更新集群ID
      this.$store.commit('setting/setClusterId', key);
      
      // 设置isCluster为true
      this.$store.commit('setting/setIsCluster', 'isCluster');
      
      // 关闭下拉菜单
      this.clusterVisible = false;
      
      // 刷新集群列表和详情
      this.$store.dispatch('setting/getRunningClusterList');
      
      // 跳转到概览页面
      this.$router.push('/overview');
    },
    ...mapMutations("setting", ["setLang", "setClusterId"]),
    goToHome() {
      // 跳转到主页
      this.$router.push('/');
      // 设置主页菜单为激活状态
      this.activeFirstMenuKey = '/service-manage';
      // 更新当前集群状态到主页菜单
      this.$emit('firstMenuSelect', '/service-manage');
      
      // 如果有集群ID，则获取集群状态
      if (this.clusterId) {
        this.updateClusterStatus(this.clusterId);
      }
    },
    
    // 更新集群状态
    updateClusterStatus(clusterId) {
      // 如果有接口获取集群状态，可以在这里调用
      // 例如：this.$axiosGet(`/cluster/status/${clusterId}`).then(...)
      
      // 临时解决方案：直接刷新页面以获取最新状态
      setTimeout(() => {
        this.$forceUpdate();
      }, 100);
    },
    
    // 判断集群类型
    isK8sCluster(item) {
      if (!item) return false;
      
      // 检查不同的属性和命名
      if (item.depType === 'Kubernetes' || 
          item.depType === 'kubernetes' || 
          item.depType === 'k8s' ||
          item.deployType === 'Kubernetes' ||
          item.deployType === 'kubernetes' ||
          item.deployType === 'k8s') {
        return true;
    }
    
      // 检查描述字段
      if (item.desc && typeof item.desc === 'string' && 
         (item.desc.toLowerCase().includes('kubernetes') || 
          item.desc.toLowerCase().includes('k8s'))) {
        return true;
      }
      
      // 检查名称字段
      if (item.label && typeof item.label === 'string' && 
         (item.label.toLowerCase().includes('kubernetes') || 
          item.label.toLowerCase().includes('k8s'))) {
        return true;
      }
      
      return false;
    },
    updateClusterList() {
      // 获取集群列表
      this.$store.dispatch('setting/getRunningClusterList');
      
      // 获取当前集群的详细信息
      this.fetchCurrentClusterDetails();
    },
    
    // 获取当前集群的详细信息
    fetchCurrentClusterDetails() {
      if (!this.clusterId) return;
      
      // 使用cluster/info API获取详细信息
      this.$axiosGet(`/ddh/api/cluster/info/${this.clusterId}`).then(res => {
        if (res.code === 200 && res.data) {
          console.log("当前集群详细信息:", res.data);
          
          // 存储当前集群的详细信息，包括depType
          this.currentClusterDetails = res.data;
          
          // 强制更新视图
          this.$forceUpdate();
        }
      }).catch(error => {
        console.error("获取集群详细信息失败:", error);
      });
    },
  },
  created() {
    let _this = this;
    document.addEventListener("click", _this.handleClickOutside);
    this.updateClusterList();
    
    // 监听集群ID变化，更新详细信息
    this.$watch('clusterId', (newVal, oldVal) => {
      if (newVal && newVal !== oldVal) {
        this.fetchCurrentClusterDetails();
      }
    });
  },
  mounted() {
    // 初始加载时获取当前集群详细信息
    if (this.clusterId) {
      this.fetchCurrentClusterDetails();
    }
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
  left: 16px;
  gap: 16px;
}

/* Logo区域样式 */
.logo-section {
  display: flex;
  align-items: center;
  cursor: pointer;
  padding: 6px 8px;
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
  width: 28px;
  height: 28px;
  border-radius: 6px;
  overflow: hidden;
  margin-right: 8px;
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
    font-size: 16px;
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
    gap: 1px;
  }
  
  .nav-item {
    position: relative;
    
    &.active .nav-link {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.12) 0%, rgba(0, 122, 255, 0.18) 100%);
      color: #007aff;
      box-shadow: 
        0 2px 12px rgba(0, 122, 255, 0.15),
        0 1px 4px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.9);
      transform: translateY(-1px);
      border: 1px solid rgba(0, 122, 255, 0.2);
      
      .nav-icon {
        color: #007aff;
        transform: scale(1.05);
      }
      
      .nav-arrow {
        color: #007aff;
        background: rgba(0, 122, 255, 0.15);
        box-shadow: 0 1px 3px rgba(0, 122, 255, 0.2);
      }
    }
    
    &.expanded .nav-link {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.1) 0%, rgba(0, 122, 255, 0.15) 100%);
      color: #007aff;
      box-shadow: 
        0 2px 12px rgba(0, 122, 255, 0.15),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
      transform: translateY(-1px);
      
      .nav-icon {
        color: #007aff;
        transform: scale(1.05);
      }
      
      .nav-arrow {
        color: #007aff;
        background: rgba(0, 122, 255, 0.15);
        box-shadow: 0 1px 3px rgba(0, 122, 255, 0.2);
        transform: rotate(180deg);
      }
    }
    
    &:hover .nav-link {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.12) 100%);
      transform: translateY(-2px) scale(1.02);
      box-shadow: 
        0 4px 12px rgba(0, 122, 255, 0.15),
        0 2px 6px rgba(0, 0, 0, 0.08);
      border: 1px solid rgba(0, 122, 255, 0.2);
      
      .nav-icon {
        transform: scale(1.1);
        color: #007aff;
      }
      
      .nav-text {
        color: #007aff;
        font-weight: 600;
      }
      
      .nav-arrow {
        background: rgba(0, 122, 255, 0.1);
        color: #007aff;
        transform: scale(1.1);
      }
    }
    
    &:active .nav-link {
      transform: translateY(0) scale(0.98);
      transition: all 0.15s ease;
    }
    
    &.has-submenu {
      position: relative;
      
      .submenu-panel {
        position: absolute;
        top: 100%;
        left: 0;
        z-index: 1000;
        min-width: 200px;
        background: rgba(255, 255, 255, 0.95);
        backdrop-filter: blur(20px);
        border-radius: 12px;
        box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12), 0 0 1px rgba(0, 0, 0, 0.08);
        overflow: hidden;
        margin-top: 4px;
      }
    }
  }
  
  .nav-link {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-radius: 12px;
    color: #2c2c2c;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    cursor: pointer;
    gap: 10px;
    min-height: 40px;
    position: relative;
    text-decoration: none;
    border: 1px solid transparent;
    
    .nav-icon {
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #4b5563;
      transition: all 0.3s ease;
    }
    
    .nav-text {
      font-weight: 500;
      color: #2c2c2c;
      transition: all 0.3s ease;
    }
    
    .nav-arrow {
      width: 18px;
      height: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: rgba(0, 0, 0, 0.04);
      margin-left: 4px;
      transition: all 0.3s ease;
      
      .anticon {
        font-size: 10px;
        color: #4b5563;
      }
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
    background: rgba(255, 255, 255, 0.6);
    backdrop-filter: blur(16px) saturate(150%);
    border: 1px solid rgba(0, 0, 0, 0.04);
    border-radius: 20px;
    padding: 8px 16px;
    box-shadow: 
      0 2px 12px rgba(0, 0, 0, 0.06),
      0 1px 4px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.7);
    gap: 10px;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    min-height: 40px;
    
    &:hover {
      background: rgba(255, 255, 255, 0.8);
      transform: translateY(-1px);
      box-shadow: 
        0 4px 20px rgba(0, 0, 0, 0.08),
        0 2px 8px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
      border-color: rgba(0, 122, 255, 0.1);
    }
  }
  
  .service-icon-wrapper {
    position: relative;
    display: flex;
    align-items: center;
    justify-content: center;
    width: 28px;
    height: 28px;
    background: rgba(0, 122, 255, 0.08);
    border-radius: 8px;
    transition: all 0.3s ease;
  }
  
  .service-icon {
    font-size: 16px;
    color: #007aff;
    transition: all 0.3s ease;
  }
  
  .service-status-dot {
    position: absolute;
    top: -1px;
    right: -1px;
    width: 8px;
    height: 8px;
    border-radius: 50%;
    border: 1.5px solid rgba(255, 255, 255, 0.9);
    transition: all 0.3s ease;
    
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
    gap: 1px;
  }
  
  .service-title {
    font-size: 13px;
    font-weight: 600;
    color: #1d1d1f;
    white-space: nowrap;
    line-height: 1.2;
    letter-spacing: -0.1px;
  }
  
  .service-subtitle {
    font-size: 11px;
    font-weight: 500;
    color: #86868b;
    white-space: nowrap;
    line-height: 1.2;
  }
}

/* 右侧区域：管理菜单 + 快捷操作 */
.header-right {
  display: flex;
  align-items: center;
  position: absolute;
  right: 16px;
  gap: 12px;
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
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.12) 0%, rgba(0, 122, 255, 0.18) 100%);
      color: #007aff;
      box-shadow: 
        0 2px 12px rgba(0, 122, 255, 0.15),
        0 1px 4px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.9);
      transform: translateY(-1px);
      border: 1px solid rgba(0, 122, 255, 0.2);
      
      .admin-icon {
        color: #007aff;
        transform: scale(1.05);
      }
      
      .admin-arrow {
        color: #007aff;
        background: rgba(0, 122, 255, 0.15);
        box-shadow: 0 1px 3px rgba(0, 122, 255, 0.2);
      }
    }
    
    &.expanded .admin-link {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.1) 0%, rgba(0, 122, 255, 0.15) 100%);
      color: #007aff;
      box-shadow: 
        0 2px 12px rgba(0, 122, 255, 0.15),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
      transform: translateY(-1px);
      
      .admin-icon {
        color: #007aff;
        transform: scale(1.05);
      }
      
      .admin-arrow {
        color: #007aff;
        background: rgba(0, 122, 255, 0.15);
        box-shadow: 0 1px 3px rgba(0, 122, 255, 0.2);
        transform: rotate(180deg);
      }
    }
    



    &:hover .admin-link {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.12) 100%);
      transform: translateY(-2px) scale(1.02);
      box-shadow: 
        0 4px 12px rgba(0, 122, 255, 0.15),
        0 2px 6px rgba(0, 0, 0, 0.08);
      border: 1px solid rgba(0, 122, 255, 0.2);
      
      .admin-icon {
        transform: scale(1.1);
        color: #007aff;
      }
      
      .admin-text {
        color: #007aff;
        font-weight: 600;
      }
      
      .admin-arrow {
        background: rgba(0, 122, 255, 0.1);
        color: #007aff;
        transform: scale(1.1);
      }
    }
    
    &:active .admin-link {
      transform: translateY(0) scale(0.98);
      transition: all 0.15s ease;
    }
  }
  
  .admin-link {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    border-radius: 12px;
    color: #2c2c2c;
    font-size: 14px;
    font-weight: 500;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    cursor: pointer;
    gap: 10px;
    min-height: 40px;
    position: relative;
    text-decoration: none;
    border: 1px solid transparent;
    
    .admin-icon {
      width: 20px;
      height: 20px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #4b5563;
      transition: all 0.3s ease;
    }
    
    .admin-text {
      font-weight: 500;
      color: #2c2c2c;
      transition: all 0.3s ease;
    }
    
    .admin-arrow {
      width: 18px;
      height: 18px;
      display: flex;
      align-items: center;
      justify-content: center;
      border-radius: 50%;
      background: rgba(0, 0, 0, 0.04);
      margin-left: 4px;
      transition: all 0.3s ease;
      
      .anticon {
        font-size: 10px;
        color: #4b5563;
      }
    }
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





/* 快捷操作区域 - 全新苹果风格设计 */
.quick-actions {
  /* 强制所有操作按钮内容居中 */
  .action-btn * {
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
  }
  display: flex !important;
  align-items: center !important;
  gap: 10px !important;
  flex-wrap: nowrap !important;
  min-width: 0 !important;
  flex-shrink: 0 !important;
  
  /* 强制所有直接子元素不换行 */
  > * {
    flex-shrink: 0 !important;
    white-space: nowrap !important;
  }
  
  /* 集群选择器 - 重新设计 */
  .cluster-selector-wrapper,
  .ant-dropdown.cluster-selector-wrapper {
    margin-right: 10px !important;
    
    .cluster-selector {
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%) !important;
        border: 1px solid rgba(0, 0, 0, 0.08) !important;
        border-radius: 20px !important;
      padding: 6px 12px !important;
        display: flex !important;
        align-items: center !important;
      justify-content: center !important;
        cursor: pointer !important;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
        backdrop-filter: blur(24px) saturate(180%) !important;
        box-shadow: 
        0 2px 8px rgba(0, 0, 0, 0.05),
        0 1px 3px rgba(0, 0, 0, 0.03),
          inset 0 1px 0 rgba(255, 255, 255, 0.9) !important;
      min-width: 120px !important;
      max-width: 180px !important;
        white-space: nowrap !important;
        flex-shrink: 0 !important;
      
      &:hover {
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 252, 254, 0.98) 100%) !important;
        transform: translateY(-2px) !important;
        box-shadow: 
          0 6px 16px rgba(0, 0, 0, 0.08),
          0 3px 6px rgba(0, 0, 0, 0.05),
          inset 0 1px 0 rgba(255, 255, 255, 0.95) !important;
        border-color: rgba(0, 122, 255, 0.2) !important;
        
        .dropdown-arrow {
          background: rgba(0, 122, 255, 0.1) !important;
          color: #007aff !important;
          
          .anticon {
            color: #007aff !important;
          }
        }
      }
      
      &:active {
        transform: translateY(-1px) !important;
        transition: all 0.15s ease !important;
      }
      
      .cluster-info {
          display: flex !important;
          align-items: center !important;
        justify-content: center !important;
        gap: 8px !important;
        
        .cluster-icon {
          width: 24px !important;
          height: 24px !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          background: linear-gradient(135deg, #007aff 0%, #0056d3 100%) !important;
          border-radius: 8px !important;
          flex-shrink: 0 !important;
          
          .svg-icon {
            font-size: 14px !important;
            color: #ffffff !important;
          }
        }
        
        .cluster-name {
          font-size: 14px !important;
          font-weight: 500 !important;
          color: #1d1d1f !important;
          letter-spacing: -0.2px !important;
          line-height: 1.2 !important;
          white-space: nowrap !important;
          overflow: hidden !important;
          text-overflow: ellipsis !important;
          flex: 1 !important;
      }
      
      .dropdown-arrow {
          font-size: 12px !important;
        color: #999999 !important;
        transition: all 0.3s ease !important;
          width: 20px !important;
          height: 20px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
          border-radius: 4px !important;
        background: rgba(0, 0, 0, 0.04) !important;
        flex-shrink: 0 !important;
        
        .anticon {
          transition: transform 0.3s ease !important;
            font-size: 12px !important;
            color: #666666 !important;
        }
        }
      }
    }
  }
  
  /* 操作按钮组 */
  .action-buttons {
    display: flex !important;
    align-items: center !important;
    gap: 12px !important;
    flex-shrink: 0 !important;
    
    .action-btn,
    div.action-btn {
      &.settings-btn,
      &.alarm-btn {
        width: 40px !important;
        height: 40px !important;
        border-radius: 12px !important;
        display: flex !important;
        align-items: center !important;
        justify-content: center !important;
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 249, 250, 0.9) 100%) !important;
        border: 1px solid rgba(0, 0, 0, 0.06) !important;
        transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
        cursor: pointer !important;
        backdrop-filter: blur(20px) saturate(180%) !important;
        box-shadow: 
          0 2px 8px rgba(0, 0, 0, 0.04),
          0 1px 3px rgba(0, 0, 0, 0.06),
          inset 0 1px 0 rgba(255, 255, 255, 0.8) !important;
        position: relative !important;
        overflow: hidden !important;
        margin-right: 8px !important;
        
        /* 强制子组件中的所有元素居中 */
        * {
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
        }
        
        /* 针对Vue组件的深度选择器 */
        :deep(*) {
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
        }
        
        /* 恢复原始图标尺寸 */
        .settings-component {
          width: 36px !important;
          height: 36px !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          
          .svg-icon, .icon-gj, .cluster-setting-icon, .icon-wrapper {
            font-size: 20px !important;
            width: 20px !important;
            height: 20px !important;
            color: #007aff !important;
            display: flex !important;
            align-items: center !important;
            justify-content: center !important;
          }
        }
        
        &:hover {
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 251, 252, 0.95) 100%);
          transform: translateY(-2px) scale(1.05);
          box-shadow: 
            0 8px 24px rgba(0, 0, 0, 0.12),
            0 4px 12px rgba(0, 0, 0, 0.08),
            inset 0 1px 0 rgba(255, 255, 255, 0.9);
        }
      }
    }
    
    /* 用户信息区域 */
    .user-section {
      margin-left: 8px;
      
      /* 重置用户头像组件的样式 */
      .header-avatar {
        padding: 10px 16px !important;
        border-radius: 16px !important;
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%) !important;
        border: 1px solid rgba(0, 0, 0, 0.08) !important;
        backdrop-filter: blur(24px) saturate(180%) !important;
        box-shadow: 
          0 4px 20px rgba(0, 0, 0, 0.06),
          0 2px 8px rgba(0, 0, 0, 0.04),
          inset 0 1px 0 rgba(255, 255, 255, 0.9) !important;
        transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
        min-width: auto !important;
        max-width: none !important;
        
        &:hover {
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 252, 254, 0.98) 100%) !important;
          transform: translateY(-2px) scale(1.02) !important;
          box-shadow: 
            0 8px 32px rgba(0, 0, 0, 0.1),
            0 4px 16px rgba(0, 0, 0, 0.06),
            inset 0 1px 0 rgba(255, 255, 255, 0.95) !important;
          border-color: rgba(0, 122, 255, 0.2) !important;
        }
        
        &:active {
          transform: translateY(-1px) scale(1.01) !important;
          transition: all 0.15s ease !important;
        }
        
        .avatar-icon {
          font-size: 16px !important;
          margin-right: 16px !important;
          color: #007aff !important;
          width: 20px !important;
          height: 20px !important;
        }
        
        .name {
          font-size: 14px !important;
          font-weight: 600 !important;
          color: #1d1d1f !important;
          letter-spacing: -0.1px !important;
          white-space: nowrap !important;
        }
      }
    }
  }
}

/* 集群选择下拉菜单 - 苹果风格重新设计 */
.cluster-menu {
  background: rgba(255, 255, 255, 0.95) !important;
  backdrop-filter: blur(30px) saturate(180%) !important;
  border: 1px solid rgba(0, 0, 0, 0.06) !important;
  border-radius: 16px !important;
  box-shadow: 
    0 20px 60px rgba(0, 0, 0, 0.15),
    0 8px 25px rgba(0, 0, 0, 0.1),
    0 2px 8px rgba(0, 0, 0, 0.06) !important;
  padding: 8px !important;
  min-width: 200px !important;
  
  .ant-menu-item {
    border-radius: 12px !important;
    margin-bottom: 4px !important;
    padding: 0 !important;
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
    
    &:last-child {
      margin-bottom: 0 !important;
    }
    
    &:hover {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.08) 0%, rgba(0, 122, 255, 0.12) 100%) !important;
      transform: translateX(4px) !important;
    }
    
    &.selected {
      background: linear-gradient(135deg, #007aff 0%, #0056d3 100%) !important;
      box-shadow: 0 4px 16px rgba(0, 122, 255, 0.25) !important;
      
      .cluster-item {
        .svg-icon {
          color: #ffffff !important;
        }
        
        span {
          color: #ffffff !important;
          font-weight: 600 !important;
        }
        
        .check-icon {
          color: #ffffff !important;
        }
      }
    }
  }
  
  .cluster-item {
    display: flex;
    align-items: center;
    padding: 8px 12px;
    gap: 12px;
    transition: all 0.3s ease;
    
    .menu-svg-icon {
      width: 20px;
      height: 20px;
      margin-right: 10px;
      object-fit: contain;
    }
    
    span {
      font-size: 14px;
      color: rgba(0, 0, 0, 0.85);
      letter-spacing: -0.2px;
      transition: all 0.3s ease;
    }
    
    .cluster-type {
      margin-left: 8px;
      font-size: 12px;
      color: rgba(0, 0, 0, 0.45);
    }
    
    .check-icon {
      margin-left: auto;
      font-size: 14px;
      color: #1890ff;
      transition: all 0.3s ease;
    }
  }
}

/* 子菜单面板样式 */
.submenu-panel {
  position: absolute;
  top: 100%;
  left: 0;
  z-index: 1000;
  min-width: 200px;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  border-radius: 12px;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12), 0 0 1px rgba(0, 0, 0, 0.08);
  overflow: hidden;
  margin-top: 4px;
  
  .submenu-container {
    padding: 8px 0;
    
    .submenu-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 8px 16px;
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      margin-bottom: 4px;
      
      .submenu-title {
        font-weight: 500;
        font-size: 14px;
        color: #1d1d1f;
      }
      
      .submenu-close {
        cursor: pointer;
        width: 24px;
        height: 24px;
        display: flex;
        align-items: center;
        justify-content: center;
        border-radius: 50%;
        transition: background-color 0.2s;
        
        &:hover {
          background-color: rgba(0, 0, 0, 0.05);
        }
        
        .anticon {
          font-size: 12px;
          color: #666;
        }
      }
    }
    
    .submenu-list {
      list-style: none;
      padding: 0;
      margin: 0;
      
      .submenu-item {
        margin: 2px 8px;
        border-radius: 8px;
        
        &:hover, &.active {
          background-color: rgba(0, 0, 0, 0.06) !important; // 与主菜单选中颜色保持一致
          
          .submenu-text {
            color: rgba(0, 0, 0, 0.85) !important; // 保持与常规文本相似的颜色，稍微加深
            font-weight: 500 !important; // 使用中等加粗
          }
        }
        
        &:hover:not(.active) {
          background-color: rgba(0, 0, 0, 0.04) !important;
        }
        
        .submenu-link {
          display: flex;
          align-items: center;
          padding: 10px 12px;
          color: #1d1d1f;
          text-decoration: none;
          
          .submenu-icon {
            margin-right: 12px;
            width: 20px;
            height: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
            background: rgba(0, 122, 255, 0.08);
            border-radius: 8px;
            padding: 6px;
            
            svg {
              width: 16px;
              height: 16px;
              color: #007aff;
            }
          }
          
          .submenu-text {
            flex: 1;
            font-size: 14px;
          }
          
          .submenu-badge {
            background-color: #ff3b30;
            color: white;
            font-size: 11px;
            padding: 0 6px;
            border-radius: 10px;
            height: 20px;
            min-width: 20px;
            display: flex;
            align-items: center;
            justify-content: center;
          }
        }
      }
    }
  }
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
    background-color: rgba(0, 0, 0, 0.06) !important; // 与主菜单选中颜色保持一致
      
    .submenu-text {
      color: rgba(0, 0, 0, 0.85) !important; // 保持与常规文本相似的颜色，稍微加深
      font-weight: 500 !important; // 使用中等加粗
    }
  }
  
  &:hover:not(.active) {
    background-color: rgba(0, 0, 0, 0.04) !important;
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
      margin-right: 12px;
      width: 24px;
      height: 24px;
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      background: rgba(0, 122, 255, 0.08);
      border-radius: 8px;
      padding: 6px;
      flex-shrink: 0;
      
      svg {
        width: 16px;
        height: 16px;
      color: #007aff;
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
}

.admin-submenu-link .submenu-icon, 
.submenu-link .submenu-icon {
  background: rgba(0, 122, 255, 0.1);
  border-radius: 8px;
  width: 24px;
  height: 24px;
  margin-right: 12px;
  flex-shrink: 0;
  display: flex !important;
  align-items: center !important;
  justify-content: center !important;
}

.admin-submenu-link .submenu-icon svg,
.submenu-link .submenu-icon svg {
  width: 16px;
  height: 16px;
  color: #007aff;
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
    margin-right: 12px;
    width: 24px;
    height: 24px;
    display: flex !important;
    align-items: center !important;
    justify-content: center !important;
    background: rgba(0, 122, 255, 0.08);
    border-radius: 8px;
    padding: 6px;
    flex-shrink: 0;
    
    svg {
    width: 16px;
    height: 16px;
      color: #007aff;
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
  transition: opacity 0.2s, transform 0.2s;
}

.submenu-slide-enter {
  opacity: 0;
  transform: translateY(-10px);
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
    gap: 4px !important;
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
    gap: 2px !important;
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
  
  /* 深色模式下的快捷操作样式已移至主样式中 */
  
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

.submenu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 8px;
  background-color: rgba(24, 144, 255, 0.1);
  border-radius: 4px;
  width: 24px;
  height: 24px;
  min-width: 24px;
  min-height: 24px;
  padding: 4px;
  
  /deep/ svg {
    width: 16px !important;
    height: 16px !important;
    color: #1890ff;
    stroke-width: 2px;
  }
}

// 二级菜单链接
.submenu-link, .admin-submenu-link {
  display: flex;
  align-items: center;
  padding: 8px 12px;
  color: rgba(0, 0, 0, 0.65);
  text-decoration: none;
  border-radius: 4px;
  margin: 2px 0;
  transition: all 0.3s;
  
  &:hover {
    background-color: rgba(24, 144, 255, 0.05);
    color: #1890ff;
    
    .submenu-icon {
      background-color: rgba(24, 144, 255, 0.2);
      
      /deep/ svg {
        color: #1890ff;
      }
    }
  }
}

/* 修复下拉菜单样式 */
.cluster-menu-overlay {
  margin-top: 10px !important;
  padding: 0 !important;
  background: transparent !important;
  
  .ant-dropdown-menu {
    margin: 0 !important;
    padding: 0 !important;
  }
  
  /* 清除多余容器 */
  .cluster-menu {
    background: rgba(255, 255, 255, 0.98) !important;
    backdrop-filter: blur(30px) saturate(180%) !important;
    border: 1px solid rgba(0, 0, 0, 0.06) !important;
    border-radius: 16px !important;
    box-shadow: 
      0 20px 60px rgba(0, 0, 0, 0.15),
      0 8px 25px rgba(0, 0, 0, 0.1),
      0 2px 8px rgba(0, 0, 0, 0.06) !important;
    padding: 8px !important;
    min-width: 200px !important;
    animation: dropdownFadeIn 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
  }
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

.cluster-selector-wrapper {
  position: relative;
}

.cluster-selector {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  padding: 6px 12px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.95) 100%);
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  backdrop-filter: blur(24px) saturate(180%);
  box-shadow: 
    0 2px 8px rgba(0, 0, 0, 0.05),
    0 1px 3px rgba(0, 0, 0, 0.03),
    inset 0 1px 0 rgba(255, 255, 255, 0.9);
  min-width: 120px;
  max-width: 180px;
  white-space: nowrap;
  flex-shrink: 0;
  margin-right: 10px;
  
  .cluster-icon {
    width: 24px;
    height: 24px;
    display: flex;
    align-items: center;
    justify-content: center;
    background: transparent;
    border-radius: 8px;
    flex-shrink: 0;
    overflow: hidden;
    
    .cluster-svg-icon {
      width: 20px;
      height: 20px;
      object-fit: contain;
    }
  }
  
  .cluster-name {
    font-size: 14px;
    font-weight: 500;
    color: #1d1d1f;
    letter-spacing: -0.2px;
    line-height: 1.2;
    white-space: nowrap;
    overflow: hidden;
    text-overflow: ellipsis;
    max-width: 80px;
  }
  
  .dropdown-icon {
    font-size: 14px;
    color: #666;
    margin-left: 5px;
  }
  
  &:hover {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.98) 0%, rgba(250, 252, 254, 0.98) 100%);
    transform: translateY(-2px);
    box-shadow: 
      0 6px 16px rgba(0, 0, 0, 0.08),
      0 3px 6px rgba(0, 0, 0, 0.05),
      inset 0 1px 0 rgba(255, 255, 255, 0.95);
    border-color: rgba(0, 122, 255, 0.2);
    
    .dropdown-icon {
      color: #007aff;
    }
  }
}

/* 操作按钮组 - 恢复原始大小 */
.action-buttons {
  display: flex !important;
  align-items: center !important;
  gap: 12px !important;
  flex-shrink: 0 !important;
  
  .action-btn,
  div.action-btn {
    &.settings-btn,
    &.alarm-btn {
      width: 40px !important; /* 恢复原始尺寸 */
      height: 40px !important; /* 恢复原始尺寸 */
      border-radius: 12px !important;
      display: flex !important;
      align-items: center !important;
      justify-content: center !important;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 249, 250, 0.9) 100%) !important;
      border: 1px solid rgba(0, 0, 0, 0.06) !important;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
      cursor: pointer !important;
      backdrop-filter: blur(20px) saturate(180%) !important;
      box-shadow: 
        0 2px 8px rgba(0, 0, 0, 0.04),
        0 1px 3px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.8) !important;
      position: relative !important;
      overflow: hidden !important;
      margin-right: 8px !important;
      
      /* 移除旧的历史操作图标样式 */
      
      &:hover {
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(250, 251, 252, 0.95) 100%);
        transform: translateY(-2px) scale(1.05);
        box-shadow: 
          0 8px 24px rgba(0, 0, 0, 0.12),
          0 4px 12px rgba(0, 0, 0, 0.08),
          inset 0 1px 0 rgba(255, 255, 255, 0.9);
      }
    }
  }
}

.custom-cluster-dropdown {
  margin-top: 10px !important;
  padding: 0 !important;
  background: transparent !important;
  
  .ant-dropdown-menu {
    margin: 0 !important;
    padding: 0 !important;
  }
  
  /* 清除多余容器 */
  .cluster-menu {
    background: rgba(255, 255, 255, 0.98) !important;
    backdrop-filter: blur(30px) saturate(180%) !important;
    border: 1px solid rgba(0, 0, 0, 0.06) !important;
    border-radius: 16px !important;
    box-shadow: 
      0 20px 60px rgba(0, 0, 0, 0.15),
      0 8px 25px rgba(0, 0, 0, 0.1),
      0 2px 8px rgba(0, 0, 0, 0.06) !important;
    padding: 8px !important;
    min-width: 200px !important;
    animation: dropdownFadeIn 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94) !important;
  }
}

@keyframes dropdownFadeIn {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 覆盖Ant Design下拉菜单样式，修复双重容器问题 */
:deep(.ant-dropdown) {
  padding: 0 !important;
  background: transparent !important;
  border: none !important;
  box-shadow: none !important;
  
  &.ant-dropdown-placement-bottomCenter {
    margin-top: 10px !important;
  }
}

:deep(.ant-dropdown-menu-root) {
  padding: 0 !important;
  background: transparent !important;
}

/* 确保集群下拉菜单正确定位 */
.custom-cluster-dropdown {
  z-index: 1050 !important;
  
  &.ant-dropdown-placement-bottomCenter {
    margin-top: 0 !important;
    left: 50% !important;
    transform: translateX(-50%) !important;
  }
}
</style>