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
            <img src="@/assets/logo.svg" alt="Logo" />
          </div>
          <div class="brand-info">
            <h1 class="brand-title">Noah大数据基础平台</h1>
          </div>
        </div>
        
        <!-- 左对齐导航菜单 -->
        <nav class="left-navigation">
          <ul class="nav-menu">
            <li
              v-for="item in regularMenus"
              :key="item.path"
              class="nav-item"
              :class="{
                'active': item.path === activeFirstMenuKey, 
                'has-submenu': hasSubMenu(item) && item.path !== 'home',
                'expanded': hoveredMenu === item.path || expandedMenu === item.path
              }"
              @mouseenter="handleMenuEnter(item)"
              @mouseleave="handleMenuLeave(item)"
              @click="onLeftMenuClick(item)"
            >
              <div class="nav-link">
                <div class="nav-icon">
                  <svg-icon v-if="item.path === 'home'" icon-class="home" />
                  <svg-icon v-else :icon-class="item.icon" />
                </div>
                <span class="nav-text">
                  {{ item.title }}
                </span>
                <div class="nav-arrow" v-if="hasSubMenu(item) && item.path !== 'home'">
                  <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </div>
              </div>
              
              <!-- 子菜单下拉面板 -->
              <transition name="submenu-slide">
                <div
                  v-if="(hoveredMenu === item.path || expandedMenu === item.path) && item.path !== 'home' && hasSubMenu(item)"
                  class="submenu-panel"
                  @mouseenter="keepSubmenuOpen(item)"
                  @mouseleave="handleSubmenuLeave(item)"
                >
                  <div class="submenu-container">
                    <div class="submenu-header">
                      <span class="submenu-title">{{ item.title }}</span>
                      <div class="submenu-close" @click.stop="closeSubmenu(item)">
                        <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </div>
                    </div>
                    <ul class="submenu-list">
                      <li
                        v-for="subItem in getSubMenuItems(item)"
                        :key="subItem.path"
                        class="submenu-item"
                        :class="{ 'active': isActive(subItem.path) }"
                        @click.stop="onSubMenuSelect(subItem, $event)"
                      >
                        <a :href="'#' + subItem.path" class="submenu-link">
                          <div class="submenu-icon">
                            <svg-icon :icon-class="subItem.icon || 'default'" />
                          </div>
                          <span class="submenu-text">{{subItem.title}}</span>
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
        <transition name="fade">
          <div class="service-status-indicator" v-if="showOverviewStatus">
            <div class="service-card">
              <div class="service-icon-wrapper">
                <div class="service-icon">
                  <svg-icon v-if="isOverviewPage" icon-class="overview" />
                  <svg-icon v-else :icon-class="currentService.icon" />
                </div>
                <div 
                  class="service-status-dot" 
                  :class="{
                    'status-running': currentService.status === 'running', 
                    'status-warning': currentService.status === 'warning', 
                    'status-error': currentService.status === 'error'
                  }"
                ></div>
              </div>
              <div class="service-details">
                <span class="service-title">{{ currentService.title }}</span>
                <span class="service-subtitle">
                  {{ getStatusText(currentService.status) }}
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
              :key="item.path"
              class="admin-item"
              :class="{
                'active': item.path === activeFirstMenuKey, 
                'has-submenu': hasSubMenu(item),
                'expanded': hoveredMenu === item.path || expandedMenu === item.path
              }"
              @mouseenter="handleMenuEnter(item)"
              @mouseleave="handleMenuLeave(item)"
              @click="onLeftMenuClick(item)"
            >
              <div class="admin-link">
                <div class="admin-icon">
                  <svg-icon v-if="item.path === 'colony-manage'" icon-class="colony" />
                  <svg-icon v-else-if="item.path === 'user-manage'" icon-class="user_manager" />
                  <svg-icon v-else :icon-class="item.icon" />
                </div>
                <span class="admin-text">{{item.title}}</span>
                <div class="admin-arrow" v-if="hasSubMenu(item)">
                  <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                    <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                  </svg>
                </div>
              </div>
              
              <!-- 管理子菜单下拉面板 -->
              <transition name="submenu-slide">
                <div
                  v-if="(hoveredMenu === item.path || expandedMenu === item.path) && hasSubMenu(item)"
                  class="admin-submenu-panel"
                  @mouseenter="keepSubmenuOpen(item)"
                  @mouseleave="handleSubmenuLeave(item)"
                >
                  <div class="admin-submenu-container">
                    <div class="submenu-header">
                      <span class="submenu-title">{{ item.title }}</span>
                      <div class="submenu-close" @click.stop="closeSubmenu(item)">
                        <svg class="w-3 h-3" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                          <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M6 18L18 6M6 6l12 12" />
                        </svg>
                      </div>
                    </div>
                    <ul class="admin-submenu-list">
                      <li
                        v-for="subItem in getSubMenuItems(item)"
                        :key="subItem.path"
                        class="admin-submenu-item"
                        :class="{ 'active': isActive(subItem.path) }"
                        @click.stop="onSubMenuSelect(subItem, $event)"
                      >
                        <a :href="'#' + subItem.path" class="admin-submenu-link">
                          <div class="submenu-icon">
                            <svg-icon :icon-class="subItem.icon || 'default'" />
                          </div>
                          <span class="submenu-text">{{subItem.title}}</span>
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
          <div class="cluster-selector-wrapper" ref="clusterDropdown">
            <button 
              @click="toggleClusterMenu"
              class="cluster-selector-btn"
            >
              <div class="cluster-icon">
                <svg-icon v-if="isK8sCluster" icon-class="kubernetes" />
                <svg-icon v-else icon-class="linux" />
              </div>
              <div class="cluster-info">
                <span class="cluster-name">{{ currentCluster?.name || '选择集群' }}</span>
                <span class="cluster-type">{{ isK8sCluster ? 'K8S' : 'Linux' }}</span>
              </div>
              <div class="cluster-arrow">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </div>
            </button>
            
            <!-- 集群下拉菜单 -->
            <transition name="dropdown">
              <div v-if="isClusterMenuOpen" class="cluster-dropdown-menu">
                <div class="dropdown-header">
                  <span>选择集群</span>
                </div>
                <div class="dropdown-content">
                  <div 
                    v-for="cluster in availableClusters" 
                    :key="cluster.id"
                    class="cluster-option"
                    :class="{ 'active': currentCluster?.id === cluster.id }"
                    @click="selectCluster(cluster)"
                  >
                    <div class="option-icon">
                      <svg-icon v-if="cluster.depType === 'K8S'" icon-class="kubernetes" />
                      <svg-icon v-else icon-class="linux" />
                      <div 
                        class="status-indicator" 
                        :class="{
                          'status-running': cluster.status === 'running',
                          'status-warning': cluster.status === 'warning',
                          'status-error': cluster.status === 'error'
                        }"
                      ></div>
                    </div>
                    <div class="option-info">
                      <span class="option-name">{{ cluster.name }}</span>
                      <span class="option-type">{{ cluster.depType }}</span>
                    </div>
                  </div>
                </div>
              </div>
            </transition>
          </div>
          
          <!-- 历史操作图标 -->
          <button class="action-btn" @click="openHistoryOperations">
            <div class="action-icon">
              <svg-icon icon-class="history" />
              <span class="indicator-dot"></span>
            </div>
          </button>
          
          <!-- 告警图标 -->
          <button class="action-btn" @click="openAlarmManagement">
            <div class="action-icon">
              <svg-icon icon-class="bell" />
              <span class="badge">{{ alarmCount }}</span>
            </div>
          </button>
          
          <!-- 用户中心 -->
          <div class="user-section" ref="userDropdown">
            <button 
              @click="toggleUserMenu"
              class="user-btn"
            >
              <div class="avatar">
                <img 
                  :src="userInfo?.avatar" 
                  @error="onAvatarError"
                  alt="User Avatar" 
                />
              </div>
              <div class="user-info">
                <span class="username">{{ userInfo?.name || 'admin' }}</span>
              </div>
              <div class="user-arrow">
                <svg class="w-4 h-4" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                  <path stroke-linecap="round" stroke-linejoin="round" stroke-width="2" d="M19 9l-7 7-7-7" />
                </svg>
              </div>
            </button>
            
            <!-- 用户下拉菜单 -->
            <transition name="dropdown">
              <div v-if="isUserMenuOpen" class="user-dropdown-menu">
                <div class="user-dropdown-header">
                  <div class="user-dropdown-avatar">
                    <img 
                      :src="userInfo?.avatar" 
                      @error="onAvatarError"
                      alt="User Avatar" 
                    />
                  </div>
                  <div class="user-dropdown-info">
                    <span class="user-dropdown-name">{{ userInfo?.name || 'admin' }}</span>
                    <span class="user-dropdown-role">{{ userInfo?.role || '管理员' }}</span>
                  </div>
                </div>
                <div class="user-dropdown-menu-items">
                  <a class="user-dropdown-item" @click="viewUserInfo">
                    <svg-icon icon-class="user" />
                    <span>个人信息</span>
                  </a>
                  <a class="user-dropdown-item" @click="logout">
                    <svg-icon icon-class="logout" />
                    <span>退出登录</span>
                  </a>
                </div>
              </div>
            </transition>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { useUserStore } from '../../stores/user'
import { useSettingsStore } from '../../stores/settings'
import SvgIcon from '../../components/SvgIcon.vue'

export default {
  name: 'AppHeader',
  components: {
    SvgIcon  // 直接使用导入的组件，不要使用异步导入
  },
  props: {
    firstMenu: {
      type: Array,
      default: () => []
    },
    activeFirstMenuKey: {
      type: String,
      default: ''
    }
  },
  emits: ['firstMenuSelect', 'routeChanged'],
  data() {
    return {
      // 系统名称
      systemName: "Noah大数据基础平台",
      
      // 菜单激活状态
      activeMenuKey: this.activeFirstMenuKey || '',
      
      // 菜单管理
      hoveredMenu: null,
      expandedMenu: null,
      menuHoverTimers: {},
      
      // 集群相关
      isClusterMenuOpen: false,
      availableClusters: [
        { id: '1', name: '测试集群1', depType: 'K8S', status: 'running' },
        { id: '2', name: '生产环境', depType: 'Linux', status: 'running' },
        { id: '3', name: '开发环境', depType: 'Linux', status: 'warning' }
      ],
      
      // 用户菜单
      isUserMenuOpen: false,
      
      // 告警数量
      alarmCount: 3
    }
  },
  computed: {
    // 页面相关
    isOverviewPage() {
      return this.$route.path === '/overview' || this.$route.path === '/'
    },
    
    // 显示标题
    displayTitle() {
      if (this.isOverviewPage) return '集群总览';
      if (this.$route.meta?.title) return String(this.$route.meta.title);
      return '主页';
    },
    
    // 当前服务
    currentService() {
      return {
        title: this.displayTitle,
        icon: this.isOverviewPage ? 'overview' : 'home',
        status: 'running'
      };
    },
    
    // 菜单过滤
    regularMenus() {
      // 只显示非右侧的菜单项
      return this.firstMenu.filter(item => !item.rightSide);
    },
    
    adminMenus() {
      // 只显示右侧的菜单项
      return this.firstMenu.filter(item => item.rightSide);
    },
    
    // 集群总览显示控制
    showOverviewStatus() {
      // 判断是否有右侧的集群总览菜单项
      const overviewItem = this.firstMenu.find(item => item.path === '/overview');
      return !!overviewItem;
    },
    
    // 用户信息
    userInfo() {
      return {
        name: 'admin',
        role: '管理员',
        avatar: ''
      }
    },
    
    // 当前集群
    currentCluster() {
      return this.availableClusters[0];
    },
    
    // 是否为K8S集群
    isK8sCluster() {
      if (!this.currentCluster) return false;
      return this.currentCluster.depType === 'K8S';
    }
  },
  mounted() {
    document.addEventListener('click', this.handleClickOutside);
  },
  beforeUnmount() {
    document.removeEventListener('click', this.handleClickOutside);
  },
  methods: {
    // 判断路由是否激活
    isActive(path) {
      if (path === '/dashboard' && this.$route.path === '/') return true;
      return this.$route.path.startsWith(path);
    },
    
    // 处理菜单悬浮
    handleMenuEnter(item) {
      // 清除任何现有的计时器
      if (this.menuHoverTimers[item.path]) {
        window.clearTimeout(this.menuHoverTimers[item.path]);
      }
      
      // 设置新的计时器
      this.menuHoverTimers[item.path] = window.setTimeout(() => {
        this.hoveredMenu = item.path;
      }, 100);
    },
    
    // 处理菜单离开
    handleMenuLeave(item) {
      // 清除计时器
      if (this.menuHoverTimers[item.path]) {
        window.clearTimeout(this.menuHoverTimers[item.path]);
      }
      
      // 设置关闭计时器
      this.menuHoverTimers[item.path] = window.setTimeout(() => {
        if (this.hoveredMenu === item.path) {
          this.hoveredMenu = null;
        }
      }, 300);
    },
    
    // 保持子菜单打开状态
    keepSubmenuOpen(item) {
      if (this.menuHoverTimers[item.path]) {
        clearTimeout(this.menuHoverTimers[item.path]);
      }
      this.hoveredMenu = item.path;
    },
    
    // 处理子菜单离开
    handleSubmenuLeave(item) {
      if (this.expandedMenu !== item.path) {
        this.menuHoverTimers[item.path] = window.setTimeout(() => {
          if (this.hoveredMenu === item.path) {
            this.hoveredMenu = null;
          }
        }, 300);
      }
    },
    
    // 关闭子菜单
    closeSubmenu(item) {
      this.hoveredMenu = null;
      this.expandedMenu = null;
      if (this.menuHoverTimers[item.path]) {
        window.clearTimeout(this.menuHoverTimers[item.path]);
      }
    },
    
    // 判断是否有子菜单
    hasSubMenu(item) {
      // 对于主页菜单，始终返回false
      if (item.path === 'home' && item.title === '主页') {
        return false;
      }
      
      return item.children && item.children.length > 0;
    },
    
    // 获取子菜单项
    getSubMenuItems(item) {
      return item.children || [];
    },
    
    // 处理子菜单点击
    onSubMenuSelect(subItem, event) {
      // 关闭所有下拉菜单
      this.hoveredMenu = '';
      this.expandedMenu = '';
      
      // 清除所有菜单定时器
      Object.keys(this.menuHoverTimers).forEach(key => {
        clearTimeout(this.menuHoverTimers[key]);
        delete this.menuHoverTimers[key];
      });
      
      // 设置当前激活的一级菜单
      const parentMenu = [...this.regularMenus, ...this.adminMenus].find(item => 
        item.children && item.children.some(child => child.path === subItem.path)
      );
      
      if (parentMenu) {
        this.$emit('firstMenuSelect', parentMenu.path);
        this.activeMenuKey = parentMenu.path;
      }
      
      // 通知父组件路由已经改变
      this.$emit('routeChanged', subItem.path);
      
      // 处理特殊的路径映射，确保兼容旧路径格式
      let targetPath = subItem.path;
      
      // 导航到子菜单路径
      this.$router.push(targetPath).catch(err => {
        console.error('路由导航错误:', err);
        
        // 如果导航失败，尝试一些替代方案
        if (targetPath === '/colony-manage/list') {
          // 尝试导航到集群列表的替代路径
          this.$router.push('/cluster/list').catch(e => console.error('替代导航也失败:', e));
        }
      });
    },
    
    // 点击左侧菜单
    onLeftMenuClick(item) {
      // 如果是主页菜单，使用与logo点击相同的处理方法
      if (item.path === '/home' && item.title === '主页') {
        this.goToHome();
        this.$emit('firstMenuSelect', '/home');
        return;
      }
      
      // 如果没有子菜单，直接发出选择事件
      if (!this.hasSubMenu(item)) {
        this.$emit('firstMenuSelect', item.path);
        this.activeMenuKey = item.path;
        this.$router.push(item.path);
      } else {
        // 如果有子菜单，切换展开状态
        if (this.expandedMenu === item.path) {
          // 如果已展开，则折叠
          this.expandedMenu = '';
          this.hoveredMenu = '';
        } else {
          // 如果未展开，则展开
          this.expandedMenu = item.path;
          this.hoveredMenu = item.path;
        }
      }
    },
    
    // 回到主页
    goToHome() {
      this.$router.push('/home');
    },
    
    // 切换集群菜单
    toggleClusterMenu() {
      this.isClusterMenuOpen = !this.isClusterMenuOpen;
    },
    
    // 选择集群
    selectCluster(cluster) {
      // 在实际应用中，这里应该调用Vuex/Pinia store的方法
      // this.$store.dispatch('setCurrentCluster', cluster);
      this.isClusterMenuOpen = false;
      
      // 跳转到概览页面
      this.$router.push('/overview');
    },
    
    // 切换用户菜单
    toggleUserMenu() {
      this.isUserMenuOpen = !this.isUserMenuOpen;
    },
    
    // 打开用户信息
    viewUserInfo() {
      this.isUserMenuOpen = false;
      // 可以在这里实现打开用户信息弹窗的功能
    },
    
    // 退出登录
    logout() {
      // 在实际应用中，这里应该调用Vuex/Pinia store的方法
      // this.$store.dispatch('logout');
      this.$router.push('/login');
    },
    
    // 头像加载失败时使用默认图标
    onAvatarError(e) {
      const target = e.target;
      target.src = 'data:image/svg+xml;base64,PHN2ZyB4bWxucz0iaHR0cDovL3d3dy53My5vcmcvMjAwMC9zdmciIHZpZXdCb3g9IjAgMCAyNCAyNCIgZmlsbD0iI2NjY2NjYyI+PHBhdGggZD0iTTEyIDJDNi40OCAyIDIgNi40OCAyIDEyczQuNDggMTAgMTAgMTAgMTAtNC40OCAxMC0xMFMxNy41MiAyIDEyIDJ6bTAgM2MyLjIzIDAgNCAxLjc3IDQgNCAwIDIuMjItMS43NyA0LTQgNC0yLjIyIDAtNC0xLjc4LTQtNCAwLTIuMjMgMS43OC00IDQtNHptMCAxNC45YzIuOTcgMCA2LjEtMS40NiA2LjEtMi4xOXYtMS4xNmMtMS4wNS45NS0zLjEgMS42OS01LjQgMS42OS0yLjMxIDAtNC4zNS0uNzQtNS40LTEuNjl2MS4xNmMwIC43NCAzLjEzIDIuMiA2LjEgMi4yeiI+PC9wYXRoPjwvc3ZnPg==';
    },
    
    // 获取状态文本
    getStatusText(status) {
      switch (status) {
        case 'running': return '运行中';
        case 'warning': return '警告';
        case 'error': return '错误';
        default: return '未知';
      }
    },
    
    // 打开历史操作
    openHistoryOperations() {
      // 实现历史操作功能
    },
    
    // 打开告警管理
    openAlarmManagement() {
      // 实现告警管理功能
      this.$router.push('/alarm-manage/metric');
    },
    
    // 点击外部关闭菜单
    handleClickOutside(event) {
      const clusterDropdown = this.$refs.clusterDropdown;
      const userDropdown = this.$refs.userDropdown;
      
      if (this.isClusterMenuOpen && clusterDropdown && !clusterDropdown.contains(event.target)) {
        this.isClusterMenuOpen = false;
      }
      
      if (this.isUserMenuOpen && userDropdown && !userDropdown.contains(event.target)) {
        this.isUserMenuOpen = false;
      }
    }
  }
}
</script>

<style scoped>
.modern-header {
  position: relative;
  height: 60px;
  width: 100%;
  z-index: 100;
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Text', 'SF Pro Display', 'Helvetica Neue', Arial, sans-serif;
}

.header-backdrop {
  position: absolute;
  top: 0;
  left: 0;
  width: 100%;
  height: 100%;
  background-color: rgba(255, 255, 255, 0.85);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-bottom: 1px solid rgba(229, 231, 235, 0.3);
  z-index: -1;
}

.header-container {
  display: flex;
  align-items: center;
  justify-content: space-between;
  height: 100%;
  padding: 0 24px;
  max-width: 100%;
  margin: 0;
}

/* 左侧区域样式 */
.header-left {
  display: flex;
  align-items: center;
  height: 100%;
}

.logo-section {
  display: flex;
  align-items: center;
  cursor: pointer;
  margin-right: 40px;
  transition: all 0.3s ease;
}

.logo-section:hover {
  opacity: 0.8;
  transform: scale(1.02);
}

.logo-wrapper {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.logo-wrapper img {
  height: 34px;
  width: auto;
  transition: transform 0.3s ease;
}

.logo-section:hover .logo-wrapper img {
  transform: rotate(-5deg);
}

.brand-title {
  font-size: 17px;
  font-weight: 500;
  color: #1d1d1f;
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.left-navigation {
  height: 100%;
}

.nav-menu {
  display: flex;
  height: 100%;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 4px;
}

.nav-item {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 16px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 12px;
  margin: 0 2px;
}

.nav-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.nav-item.active {
  color: #0066cc;
  font-weight: 500;
}

.nav-item.active::after {
  display: none; /* 移除选中菜单的小点指示器 */
}

.nav-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.nav-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: inherit;
  transition: transform 0.3s ease;
}

.nav-item:hover .nav-icon {
  transform: translateY(-1px);
}

.nav-text {
  font-size: 14px;
  font-weight: 400;
  letter-spacing: -0.01em;
  transition: all 0.3s ease;
}

.nav-item:hover .nav-text {
  transform: translateY(-1px);
}

.nav-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  transition: transform 0.3s ease;
  opacity: 0.6;
}

.nav-item.expanded .nav-arrow,
.nav-item:hover .nav-arrow {
  transform: rotate(180deg);
  opacity: 1;
}

/* 子菜单面板样式 */
.submenu-panel {
  position: absolute;
  top: calc(100% - 8px);
  left: 0;
  min-width: 220px;
  background-color: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 2px 6px rgba(0, 0, 0, 0.04),
              0 0 1px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid rgba(229, 231, 235, 0.4);
  transform-origin: top center;
  opacity: 0;
  transform: translateY(10px) scale(0.98);
  animation: submenuAppear 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

@keyframes submenuAppear {
  0% {
    opacity: 0;
    transform: translateY(10px) scale(0.98);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1);
  }
}

.submenu-container {
  padding: 8px 0;
}

.submenu-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 10px 16px;
  border-bottom: 1px solid rgba(229, 231, 235, 0.4);
  margin-bottom: 6px;
}

.submenu-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.submenu-close {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border-radius: 50%;
  cursor: pointer;
  color: #86868b;
  transition: all 0.2s ease;
}

.submenu-close:hover {
  background-color: rgba(0, 0, 0, 0.05);
  color: #1d1d1f;
  transform: rotate(90deg);
}

.submenu-list {
  list-style: none;
  margin: 0;
  padding: 6px 0;
}

.submenu-item {
  display: block;
  margin: 2px 8px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.submenu-link {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  color: #1d1d1f;
  text-decoration: none;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.submenu-link:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateX(2px);
}

.submenu-item.active .submenu-link {
  color: #0066cc;
  background-color: rgba(0, 102, 204, 0.06);
  font-weight: 500;
}

.submenu-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 10px;
  color: inherit;
}

.submenu-text {
  font-size: 14px;
  letter-spacing: -0.01em;
}

/* 中间区域样式 */
.header-center {
  flex: 1;
  display: flex;
  justify-content: center;
  align-items: center;
  min-width: 0;
}

.service-status-indicator {
  display: flex;
  align-items: center;
  max-width: 100%;
}

.service-card {
  display: flex;
  align-items: center;
  padding: 6px 16px;
  border-radius: 30px;
  background-color: rgba(0, 0, 0, 0.03);
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.service-card:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.service-icon-wrapper {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
}

.service-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  color: #0066cc;
}

.service-status-dot {
  position: absolute;
  bottom: 0;
  right: 0;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  border: 1px solid white;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.1);
}

.status-running {
  background-color: #34c759;
}

.status-warning {
  background-color: #ff9f0a;
}

.status-error {
  background-color: #ff3b30;
}

.service-details {
  display: flex;
  flex-direction: column;
  min-width: 0;
}

.service-title {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.service-subtitle {
  font-size: 12px;
  color: #86868b;
  white-space: nowrap;
}

/* 右侧区域样式 */
.header-right {
  display: flex;
  align-items: center;
  height: 100%;
}

.right-navigation {
  height: 100%;
}

.admin-menu {
  display: flex;
  height: 100%;
  list-style: none;
  margin: 0;
  padding: 0;
  gap: 4px;
}

.admin-item {
  position: relative;
  display: flex;
  align-items: center;
  height: 100%;
  padding: 0 16px;
  cursor: pointer;
  transition: all 0.4s cubic-bezier(0.16, 1, 0.3, 1);
  border-radius: 12px;
  margin: 0 2px;
}

.admin-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
}

.admin-item.active {
  color: #0066cc;
  font-weight: 500;
}

.admin-item.active::after {
  display: none; /* 移除选中菜单的小点指示器 */
}

.admin-link {
  display: flex;
  align-items: center;
  gap: 8px;
}

.admin-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  color: inherit;
  transition: transform 0.3s ease;
}

.admin-item:hover .admin-icon {
  transform: translateY(-1px);
}

.admin-text {
  font-size: 14px;
  font-weight: 400;
  letter-spacing: -0.01em;
  transition: all 0.3s ease;
}

.admin-item:hover .admin-text {
  transform: translateY(-1px);
}

.admin-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-left: 4px;
  transition: transform 0.3s ease;
  opacity: 0.6;
}

.admin-item.expanded .admin-arrow,
.admin-item:hover .admin-arrow {
  transform: rotate(180deg);
  opacity: 1;
}

/* 管理子菜单面板样式 */
.admin-submenu-panel {
  position: absolute;
  top: calc(100% - 8px);
  left: 0;
  min-width: 220px;
  background-color: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 2px 6px rgba(0, 0, 0, 0.04),
              0 0 1px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid rgba(229, 231, 235, 0.4);
  transform-origin: top center;
  opacity: 0;
  transform: translateY(10px) scale(0.98);
  animation: submenuAppear 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.admin-submenu-container {
  padding: 8px 0;
}

.admin-submenu-list {
  list-style: none;
  margin: 0;
  padding: 6px 0;
}

.admin-submenu-item {
  display: block;
  margin: 2px 8px;
  border-radius: 8px;
  transition: background-color 0.2s ease;
}

.admin-submenu-link {
  display: flex;
  align-items: center;
  padding: 10px 12px;
  color: #1d1d1f;
  text-decoration: none;
  transition: all 0.3s ease;
  border-radius: 8px;
}

.admin-submenu-link:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateX(2px);
}

.admin-submenu-item.active .admin-submenu-link {
  color: #0066cc;
  background-color: rgba(0, 102, 204, 0.06);
  font-weight: 500;
}

/* 快捷操作区域样式 */
.quick-actions {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-left: 24px;
}

/* 集群选择器样式 */
.cluster-selector-wrapper {
  position: relative;
}

.cluster-selector-btn {
  display: flex;
  align-items: center;
  padding: 8px 14px;
  border: none;
  border-radius: 12px;
  background-color: rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.cluster-selector-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.cluster-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  margin-right: 10px;
  color: #0066cc;
}

.cluster-info {
  display: flex;
  flex-direction: column;
  margin-right: 8px;
  text-align: left;
}

.cluster-name {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.cluster-type {
  font-size: 11px;
  color: #86868b;
  white-space: nowrap;
}

.cluster-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86868b;
  transition: transform 0.3s ease;
}

.cluster-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background-color: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 2px 6px rgba(0, 0, 0, 0.04),
              0 0 1px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid rgba(229, 231, 235, 0.4);
  transform-origin: top center;
  opacity: 0;
  transform: translateY(10px) scale(0.98);
  animation: submenuAppear 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.dropdown-header {
  padding: 14px 16px;
  font-size: 15px;
  font-weight: 500;
  color: #1d1d1f;
  border-bottom: 1px solid rgba(229, 231, 235, 0.4);
  letter-spacing: -0.01em;
}

.dropdown-content {
  max-height: 320px;
  overflow-y: auto;
  padding: 6px 0;
}

.cluster-option {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  cursor: pointer;
  transition: all 0.3s ease;
  margin: 2px 8px;
  border-radius: 10px;
}

.cluster-option:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateX(2px);
}

.cluster-option.active {
  background-color: rgba(0, 102, 204, 0.06);
  color: #0066cc;
}

.option-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  margin-right: 12px;
  background-color: rgba(0, 0, 0, 0.03);
  border-radius: 8px;
  transition: transform 0.3s ease;
}

.cluster-option:hover .option-icon {
  transform: scale(1.05);
}

.status-indicator {
  position: absolute;
  bottom: -2px;
  right: -2px;
  width: 10px;
  height: 10px;
  border-radius: 50%;
  border: 2px solid white;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.1);
}

.option-info {
  display: flex;
  flex-direction: column;
}

.option-name {
  font-size: 14px;
  font-weight: 500;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.option-type {
  font-size: 12px;
  color: #86868b;
}

/* 操作按钮样式 */
.action-btn {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: none;
  border-radius: 50%;
  background-color: rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.action-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.action-icon {
  position: relative;
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  color: #1d1d1f;
}

.indicator-dot {
  position: absolute;
  top: -2px;
  right: -2px;
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background-color: #ff3b30;
  border: 1px solid white;
  box-shadow: 0 0 4px rgba(0, 0, 0, 0.1);
}

.badge {
  position: absolute;
  top: -4px;
  right: -4px;
  min-width: 16px;
  height: 16px;
  padding: 0 4px;
  border-radius: 8px;
  background-color: #ff3b30;
  color: white;
  font-size: 10px;
  font-weight: 600;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 0 4px rgba(255, 59, 48, 0.4);
}

/* 用户中心样式 */
.user-section {
  position: relative;
}

.user-btn {
  display: flex;
  align-items: center;
  padding: 6px 12px;
  border: none;
  border-radius: 12px;
  background-color: rgba(0, 0, 0, 0.03);
  cursor: pointer;
  transition: all 0.3s ease;
  border: 1px solid transparent;
}

.user-btn:hover {
  background-color: rgba(0, 0, 0, 0.05);
  border-color: rgba(0, 0, 0, 0.06);
  transform: translateY(-1px);
}

.avatar {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 8px;
  background-color: #f5f5f7;
  border: 1px solid rgba(0, 0, 0, 0.05);
  transition: all 0.3s ease;
}

.user-btn:hover .avatar {
  transform: scale(1.05);
}

.avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-info {
  display: flex;
  flex-direction: column;
  margin-right: 8px;
}

.username {
  font-size: 13px;
  font-weight: 500;
  color: #1d1d1f;
  white-space: nowrap;
  letter-spacing: -0.01em;
}

.user-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  color: #86868b;
  transition: transform 0.3s ease;
}

.user-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  width: 280px;
  background-color: rgba(255, 255, 255, 0.96);
  backdrop-filter: blur(30px) saturate(180%);
  -webkit-backdrop-filter: blur(30px) saturate(180%);
  border-radius: 14px;
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.08), 
              0 2px 6px rgba(0, 0, 0, 0.04),
              0 0 1px rgba(0, 0, 0, 0.1);
  z-index: 1000;
  overflow: hidden;
  border: 1px solid rgba(229, 231, 235, 0.4);
  transform-origin: top center;
  opacity: 0;
  transform: translateY(10px) scale(0.98);
  animation: submenuAppear 0.25s cubic-bezier(0.16, 1, 0.3, 1) forwards;
}

.user-dropdown-header {
  display: flex;
  align-items: center;
  padding: 16px;
  border-bottom: 1px solid rgba(229, 231, 235, 0.4);
}

.user-dropdown-avatar {
  width: 40px;
  height: 40px;
  border-radius: 50%;
  overflow: hidden;
  margin-right: 12px;
  background-color: #f5f5f7;
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.user-dropdown-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.user-dropdown-info {
  display: flex;
  flex-direction: column;
}

.user-dropdown-name {
  font-size: 15px;
  font-weight: 500;
  color: #1d1d1f;
  letter-spacing: -0.01em;
}

.user-dropdown-role {
  font-size: 12px;
  color: #86868b;
  margin-top: 2px;
}

.user-dropdown-menu-items {
  padding: 8px 0;
}

.user-dropdown-item {
  display: flex;
  align-items: center;
  padding: 12px 16px;
  color: #1d1d1f;
  text-decoration: none;
  cursor: pointer;
  transition: all 0.3s ease;
  margin: 2px 8px;
  border-radius: 10px;
}

.user-dropdown-item:hover {
  background-color: rgba(0, 0, 0, 0.04);
  transform: translateX(2px);
}

.user-dropdown-item svg-icon {
  margin-right: 12px;
  width: 20px;
  height: 20px;
  color: #1d1d1f;
}

/* 动画效果 */
.fade-enter-active,
.fade-leave-active {
  transition: opacity 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.fade-enter-from,
.fade-leave-to {
  opacity: 0;
}

.submenu-slide-enter-active,
.submenu-slide-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.submenu-slide-enter-from,
.submenu-slide-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}

.dropdown-enter-active,
.dropdown-leave-active {
  transition: all 0.3s cubic-bezier(0.16, 1, 0.3, 1);
}

.dropdown-enter-from,
.dropdown-leave-to {
  opacity: 0;
  transform: translateY(-10px) scale(0.98);
}
</style> 