<template>
  <div class="cdh-service-page">
    <!-- 左侧服务列表 - CDH风格 -->
    <div class="service-sidebar cdh-style" :class="{'collapsed': sidebarCollapsed}">
      <!-- 头部标题区域 -->
      <div class="service-title">
        <div class="cluster-info" v-if="!sidebarCollapsed && clusterInfo">
          <div class="cluster-name">{{ clusterInfo.name }}</div>
          <div class="cluster-details">
            <span class="cluster-frame">{{ clusterInfo.frame }}</span>
            <span class="cluster-mode">{{ clusterInfo.mode }}</span>
          </div>
        </div>
        <div class="loading-text" v-if="!sidebarCollapsed && !clusterInfo">加载中...</div>
        <div class="sidebar-controls">
          <service-option class="service-more" v-if="!sidebarCollapsed" />
          <div class="sidebar-collapse-btn" @click.stop="toggleSidebar">
            <a-icon :type="sidebarCollapsed ? 'menu-unfold' : 'menu-fold'" />
          </div>
        </div>
      </div>
      
      <!-- 核心服务组 -->
      <div class="service-group">
        <div class="group-title" @click="toggleGroupCollapse('core')" :title="sidebarCollapsed ? 'Core Service' : ''">
          <a-icon type="cluster" class="group-icon" />
          <span>Core Service</span>
          <div class="collapse-icon-wrapper">
            <a-icon :type="coreGroupCollapsed ? 'caret-right' : 'caret-down'" class="collapse-icon" />
          </div>
        </div>
        <div class="service-list" v-show="!coreGroupCollapsed">
          <a-popover 
            v-for="(service, index) in coreServices" 
            :key="index"
            placement="right"
            :visible="sidebarCollapsed && service.popoverVisible"
            overlayClassName="service-popover"
            @visibleChange="(visible) => handlePopoverVisibleChange(visible, service)"
            :mouseEnterDelay="0.3"
            :mouseLeaveDelay="0.1"
            :title="null"
          >
            <template slot="content">
              <div 
                class="service-popover-content"
                @mouseenter="handlePopoverContentEnter(service)"
                @mouseleave="handlePopoverContentLeave(service)"
              >
                <div class="service-popover-header">
                  <div class="service-popover-icon">
                    <img v-if="service.serviceName === 'DATASOPHON'" src="~@/assets/img/logo.svg" alt="logo" style="width:24px;height:24px;" />
                    <svg-icon v-else :icon-class="service.icon || 'service-default'" />
                  </div>
                  <div class="service-popover-title">
                    <div class="service-popover-name">{{ service.name }}</div>
                    <div class="service-popover-version" v-if="service.rawData && service.rawData.version">{{ service.rawData.version }}</div>
                  </div>
                </div>
                
                <div class="service-popover-info">
                  <div class="service-popover-status">
                    <span class="info-label">状态：</span>
                    <span v-if="service.serviceStateCode === 2" class="info-value success">正常运行</span>
                    <span v-else-if="service.serviceStateCode === 3" class="info-value warning">需要注意</span>
                    <span v-else-if="service.serviceStateCode === 4" class="info-value error">异常</span>
                    <span v-else class="info-value">未知</span>
                  </div>
                  
                  <div class="service-popover-alerts" v-if="service.alertNum > 0">
                    <span class="info-label">告警：</span>
                    <div class="info-value-with-action">
                      <span :class="['info-value', service.serviceStateCode === 4 ? 'error' : 'warning']">{{ service.alertNum }}个</span>
                      <a-button type="link" size="small" class="action-button" @click="showAlarm(service, $event)">
                        <a-icon type="eye" /> 查看
                      </a-button>
                    </div>
                  </div>
                  
                  <div class="service-popover-config" v-if="service.needRestart">
                    <span class="info-label">配置变更：</span>
                    <div class="info-value-with-action">
                      <span class="info-value warning">需要重启</span>
                      <a-button type="link" size="small" class="action-button" @click="showConfigCompare(service, $event)">
                        <a-icon type="eye" /> 查看
                      </a-button>
                    </div>
                  </div>
                </div>
                
                <div class="service-popover-actions" v-if="service.serviceName !== 'DATASOPHON'">
                  <div class="button-row">
                    <a-button type="primary" size="small" @click="handleServiceAction({key: 'start'}, service, $event)">
                      <a-icon type="caret-right" />启动
                    </a-button>
                    <a-button type="primary" size="small" @click="handleServiceAction({key: 'stop'}, service, $event)">
                      <a-icon type="pause" />停止
                    </a-button>
                  </div>
                  <div class="button-row">
                    <a-button type="primary" size="small" @click="handleServiceAction({key: 'restart'}, service, $event)">
                      <a-icon type="reload" />重启
                    </a-button>
                    <a-button type="danger" size="small" @click="handleServiceAction({key: 'del'}, service, $event)">
                      <a-icon type="delete" />删除
                    </a-button>
                  </div>
                </div>
              </div>
            </template>
            <div 
               class="service-item" 
               :class="{'active': isActiveService(service)}"
              @click="handleServiceItemClick(service)"
              @mouseenter="sidebarCollapsed && handleServiceMouseEnter(service)"
              @mouseleave="sidebarCollapsed && handleServiceMouseLeave(service)"
            >
            <!-- 状态指示灯 -->
            <div class="status-indicator">
              <a-icon v-if="service.serviceStateCode === 2" type="check-circle" theme="filled" class="status-icon success" />
              <a-icon v-else-if="service.serviceStateCode === 3" type="warning" theme="filled" class="status-icon warning" />
              <a-icon v-else-if="service.serviceStateCode === 4" type="close-circle" theme="filled" class="status-icon error" />
              <span v-else class="status-icon empty"></span>
            </div>
            
            <!-- 服务名称(带图标) -->
            <div class="service-name-container">
              <!-- 服务图标 -->
              <div class="service-icon" v-if="service.serviceName !== 'DATASOPHON'">
                <svg-icon :icon-class="service.icon || 'service-default'" />
              </div>
              <div class="service-icon" v-else>
                <img src="@/assets/img/logo.svg" alt="logo" style="width:16px;height:16px;" />
              </div>
              
              <!-- 服务名称 -->
                <div class="service-name" v-if="!sidebarCollapsed">
                {{ service.name }}
              </div>
            </div>
            
            <!-- 告警指示器 -->
            <div class="alert-indicators">
              <!-- 告警数量 -->
              <div v-if="service.alertNum > 0" class="alert-badge" @click="showAlarm(service, $event)">
                <a-icon type="exclamation-circle" theme="filled" :class="service.serviceStateCode === 4 ? 'error-color' : 'warning-color'" />
                <span :class="['alert-count', service.serviceStateCode === 4 ? 'error-color' : 'warning-color']">{{ service.alertNum }}</span>
              </div>
              
              <!-- 配置变更指示器 -->
              <a-icon 
                v-if="service.needRestart" 
                type="tool" 
                class="restart-icon"
                @click="showConfigCompare(service, $event)" 
              />
            </div>
            
            <!-- 展开按钮 -->
            <div class="expand-icon" v-if="service.serviceName !== 'PLATFORM' && service.serviceName !== 'DATASOPHON'">
              <div class="modern-action-btn" 
                   :class="{ 'menu-active': service.menuVisible }" 
                   @click.stop="showActionMenuForService(service, $event)">
                <a-icon type="more" />
              </div>
            </div>
          </div>
          </a-popover>
        </div>
      </div>
      
      <!-- 管理服务组 -->
      <div class="service-group management-group">
        <div class="group-title" @click="toggleGroupCollapse('management')" :title="sidebarCollapsed ? 'Management' : ''">
          <a-icon type="control" class="group-icon" />
          <span>Management</span>
          <div class="collapse-icon-wrapper">
            <a-icon :type="managementGroupCollapsed ? 'caret-right' : 'caret-down'" class="collapse-icon" />
          </div>
        </div>
        <div class="service-list" v-show="!managementGroupCollapsed">
          <a-popover 
            v-for="(service, index) in managementServices" 
            :key="'mgmt-'+index"
            placement="right"
            :visible="sidebarCollapsed && service.popoverVisible"
            overlayClassName="service-popover"
            @visibleChange="(visible) => handlePopoverVisibleChange(visible, service)"
            :mouseEnterDelay="0.3"
            :mouseLeaveDelay="0.1"
            :title="null"
          >
            <template slot="content">
              <div 
                class="service-popover-content"
                @mouseenter="handlePopoverContentEnter(service)"
                @mouseleave="handlePopoverContentLeave(service)"
              >
                <div class="service-popover-header">
                  <div class="service-popover-icon">
                    <img v-if="service.serviceName === 'DATASOPHON'" src="~@/assets/img/logo.svg" alt="logo" style="width:24px;height:24px;" />
                    <svg-icon v-else :icon-class="service.icon || 'service-default'" />
                  </div>
                  <div class="service-popover-title">
                    <div class="service-popover-name">{{ service.name }}</div>
                    <div class="service-popover-version" v-if="service.rawData && service.rawData.version">{{ service.rawData.version }}</div>
                  </div>
                </div>
                
                <div class="service-popover-info">
                  <div class="service-popover-status">
                    <span class="info-label">状态：</span>
                    <span v-if="service.serviceStateCode === 2" class="info-value success">正常运行</span>
                    <span v-else-if="service.serviceStateCode === 3" class="info-value warning">需要注意</span>
                    <span v-else-if="service.serviceStateCode === 4" class="info-value error">异常</span>
                    <span v-else class="info-value">未知</span>
                  </div>
                  
                  <div class="service-popover-alerts" v-if="service.alertNum > 0">
                    <span class="info-label">告警：</span>
                    <div class="info-value-with-action">
                      <span :class="['info-value', service.serviceStateCode === 4 ? 'error' : 'warning']">{{ service.alertNum }}个</span>
                      <a-button type="link" size="small" class="action-button" @click="showAlarm(service, $event)">
                        <a-icon type="eye" /> 查看
                      </a-button>
                    </div>
                  </div>
                  
                  <div class="service-popover-config" v-if="service.needRestart">
                    <span class="info-label">配置变更：</span>
                    <div class="info-value-with-action">
                      <span class="info-value warning">需要重启</span>
                      <a-button type="link" size="small" class="action-button" @click="showConfigCompare(service, $event)">
                        <a-icon type="eye" /> 查看
                      </a-button>
                    </div>
                  </div>
                </div>
                
                <div class="service-popover-actions" v-if="service.serviceName !== 'PLATFORM'">
                  <div class="button-row">
                    <a-button type="primary" size="small" @click="handleServiceAction({key: 'start'}, service, $event)">
                      <a-icon type="caret-right" />启动
                    </a-button>
                    <a-button type="primary" size="small" @click="handleServiceAction({key: 'stop'}, service, $event)">
                      <a-icon type="pause" />停止
                    </a-button>
                  </div>
                  <div class="button-row">
                    <a-button type="primary" size="small" @click="handleServiceAction({key: 'restart'}, service, $event)">
                      <a-icon type="reload" />重启
                    </a-button>
                    <a-button type="danger" size="small" @click="handleServiceAction({key: 'del'}, service, $event)">
                      <a-icon type="delete" />删除
                    </a-button>
                  </div>
                </div>
              </div>
            </template>
            <div 
               class="service-item" 
               :class="{'active': isActiveService(service)}"
              @click="handleServiceItemClick(service)"
              @mouseenter="sidebarCollapsed && handleServiceMouseEnter(service)"
              @mouseleave="sidebarCollapsed && handleServiceMouseLeave(service)"
            >
            <!-- 状态指示灯 -->
            <div class="status-indicator">
              <a-icon v-if="service.serviceStateCode === 2" type="check-circle" theme="filled" class="status-icon success" />
              <a-icon v-else-if="service.serviceStateCode === 3" type="warning" theme="filled" class="status-icon warning" />
              <a-icon v-else-if="service.serviceStateCode === 4" type="close-circle" theme="filled" class="status-icon error" />
              <span v-else class="status-icon empty"></span>
            </div>
            
            <!-- 服务名称(带图标) -->
            <div class="service-name-container">
              <!-- 服务图标 -->
              <div class="service-icon">
                <svg-icon :icon-class="service.icon || 'service-default'" />
              </div>
              
              <!-- 服务名称 -->
                <div class="service-name" v-if="!sidebarCollapsed">
                {{ service.name }}
              </div>
            </div>
            
            <!-- 告警指示器 -->
            <div class="alert-indicators">
              <!-- 告警数量 -->
              <div v-if="service.alertNum > 0" class="alert-badge" @click="showAlarm(service, $event)">
                <a-icon type="exclamation-circle" theme="filled" :class="service.serviceStateCode === 4 ? 'error-color' : 'warning-color'" />
                <span :class="['alert-count', service.serviceStateCode === 4 ? 'error-color' : 'warning-color']">{{ service.alertNum }}</span>
              </div>
              
              <!-- 配置变更指示器 -->
              <a-icon 
                v-if="service.needRestart" 
                type="tool" 
                class="restart-icon"
                @click="showConfigCompare(service, $event)" 
              />
            </div>
            
            <!-- 展开按钮 -->
            <div class="expand-icon" v-if="service.serviceName !== 'PLATFORM' && service.serviceName !== 'DATASOPHON'">
              <div class="modern-action-btn" :class="{'menu-active': service.menuVisible}" @click.stop="showActionMenuForService(service, $event)">
                <a-icon type="more" />
              </div>
            </div>
          </div>
          </a-popover>
        </div>
      </div>
    </div>

    <!-- 右侧内容区 -->
    <div class="service-content">
      <!-- 路由视图 - 用于显示服务详情或总览 -->
      <router-view />
    </div>
    
    <!-- 全局服务操作菜单 -->
    <div class="service-action-menu" v-if="activeService" v-show="showActionMenu" :style="actionMenuStyle">
      <div class="menu-header">
        <span>{{ activeService.name }}</span>
        <a-icon type="close" @click="hideActionMenu" />
      </div>
      <div class="menu-items">
        <div class="menu-item" @click="handleServiceAction({key: 'start'}, activeService)">
          <a-icon type="caret-right" />
          <span>启动</span>
        </div>
        <div class="menu-item" @click="handleServiceAction({key: 'stop'}, activeService)">
          <a-icon type="pause" />
          <span>停止</span>
        </div>
        <div class="menu-item" @click="handleServiceAction({key: 'restart'}, activeService)">
          <a-icon type="reload" />
          <span>重启</span>
        </div>
        <div class="menu-divider"></div>
        <div class="menu-item danger" @click="handleServiceAction({key: 'del'}, activeService)">
          <a-icon type="delete" />
          <span>删除</span>
        </div>
      </div>
    </div>
  </div>
</template>

<script>
import { mapState, mapMutations } from 'vuex';
import ServiceOption from '@/components/menu/serviceOption.vue';

export default {
  name: "ServiceLayout",
  components: { ServiceOption },
  data() {
    return {
      menuList: [],
      activeMenu: null,
      clusterData: null,
      managementServiceNames: ['ALERTMANAGER', 'PROMETHEUS', 'GRAFANA', 'PUSHGATEWAY', 'DATASOPHON'],
      coreGroupCollapsed: false,
      managementGroupCollapsed: false,
      sidebarCollapsed: false,
      popoverTimeoutMap: {}, // 用于存储popover延迟隐藏的定时器
      switchingService: false, // 标记是否正在切换服务，防止在切换过程中显示悬浮窗
      menuContainer: null, // 存储下拉菜单容器DOM引用
      activeService: null, // 当前激活的服务
      showActionMenu: false, // 是否显示全局服务操作菜单
      actionMenuStyle: { // 全局服务操作菜单样式
        position: 'fixed',
        top: '50%',
        left: '50%',
        transform: 'translate(-50%, -50%)',
        background: 'white',
        padding: '20px',
        borderRadius: '10px',
        boxShadow: '0 4px 20px rgba(0, 0, 0, 0.1)',
        zIndex: 1000
      }
    };
  },
  computed: {
    ...mapState('setting', ['menuData', 'alarmManageVisible', 'clusterId']),
    currentServiceId() {
      return this.$route.params.serviceId;
    },
    clusterInfo() {
      if (this.clusterData) {
        const depTypeDisplay = this.clusterData.depType === 'PVM' ? '物理机模式' : 
                              this.clusterData.depType === 'K8S' ? 'Kubernetes模式' : 
                              this.clusterData.depType;
        return {
          name: this.clusterData.clusterName,
          frame: this.clusterData.clusterFrame,
          mode: depTypeDisplay
        };
      }
      return null;
    },
    coreServices() {
      return this.menuList.filter(service => {
        // 使用serviceName字段进行过滤，而不是name字段
        const serviceNameForFilter = service.serviceName || service.name;
        // 确保大小写一致的比较
        return !this.managementServiceNames.includes(serviceNameForFilter.toUpperCase());
      });
    },
    managementServices() {
      // 创建硬编码的大数据基础平台服务项
      const platformService = { 
        id: '0', 
        name: '大数据基础平台', 
        serviceName: 'DATASOPHON', 
        icon: 'logo', 
        path: '/service-manage', 
        serviceId: '', 
        serviceStateCode: 2, 
        alertNum: 0, 
        needRestart: false, 
        rawData: {}, 
        menuVisible: false, 
        popoverVisible: false, 
        popoverInContent: false 
      };
      
      // 过滤出管理服务
      const filteredServices = this.menuList.filter(service => {
        // 使用serviceName字段进行过滤，而不是name字段
        const serviceNameForFilter = service.serviceName || service.name;
        const isManagementService = this.managementServiceNames.includes(serviceNameForFilter.toUpperCase());
        return isManagementService;
      });
      
      // 如果没有找到大数据基础平台，添加它
      const hasPlatform = filteredServices.some(s => (s.serviceName || '').toUpperCase() === 'DATASOPHON');
      if (!hasPlatform) {
        filteredServices.unshift(platformService);
      }
      
      return filteredServices;
    }
  },
  mounted() {
    this.getClusterInfo();
    this.loadMenuData();
    
    // 获取菜单容器DOM引用
    this.menuContainer = document.querySelector('.cdh-service-page');
    
    // 从 localStorage 加载折叠状态
    const savedCollapsedState = localStorage.getItem('sidebarCollapsed');
    if (savedCollapsedState !== null) {
      this.sidebarCollapsed = savedCollapsedState === 'true';
    }
    
    // 添加点击外部关闭菜单的事件监听
    document.addEventListener('click', this.closeAllMenus);
    
    // 添加路由变化监听，关闭所有悬浮窗
    this.$watch('$route', () => {
      this.closeAllPopovers();
      // 设置切换锁定，防止在切换过程中显示悬浮窗
      this.switchingService = true;
      setTimeout(() => {
        this.switchingService = false;
      }, 100);
    });
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('click', this.closeAllMenus);
    document.removeEventListener('click', this.handleOutsideClick);
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    
    // 修复 getPopupContainer 方法
    getPopupContainer() {
      // 确保返回当前组件的DOM元素作为容器，而不是document.body
      return this.$el;
    },
    
    // 切换侧边栏折叠状态
    toggleSidebar() {
      this.sidebarCollapsed = !this.sidebarCollapsed;
      // 保存折叠状态到 localStorage
      localStorage.setItem('sidebarCollapsed', this.sidebarCollapsed);
    },
    
    // 切换服务组的折叠状态
    toggleGroupCollapse(groupType) {
      if (groupType === 'core') {
        this.coreGroupCollapsed = !this.coreGroupCollapsed;
      } else if (groupType === 'management') {
        this.managementGroupCollapsed = !this.managementGroupCollapsed;
      }
    },
    
    // 获取集群信息
    getClusterInfo() {
      // 从后端API获取集群信息
      this.$axiosGet('/ddh/api/cluster/runningClusterList').then(res => {
        if (res.code === 200 && res.data && res.data.length > 0) {
          this.clusterData = res.data[0]; // 获取第一个集群数据
        } else {
          console.warn('未获取到集群数据，使用默认数据');
          // 如果API调用失败，使用默认数据作为后备
          this.clusterData = {
            id: 1,
            clusterName: "bdp",
            clusterCode: "bdp",
            clusterFrame: "DDP-1.2.1",
            depType: "PVM",
            clusterState: "正在运行"
          };
        }
      }).catch(error => {
        console.error('获取集群数据失败:', error);
        // API调用失败时使用默认数据
        this.clusterData = {
          id: 1,
          clusterName: "bdp",
          clusterCode: "bdp",
          clusterFrame: "DDP-1.2.1",
          depType: "PVM",
          clusterState: "正在运行"
        };
      });
    },
    
    // 关闭所有菜单
    closeAllMenus() {
      if (this.menuList) {
        this.menuList.forEach(item => {
          item.menuVisible = false;
        });
      }
    },
    
    // 加载菜单数据
    loadMenuData() {
      // 尝试从localStorage获取菜单数据
      const menuData = JSON.parse(localStorage.getItem('menuData') || '[]');
      
      // 首先尝试从传统菜单数据中获取
      const serviceMenus = menuData.filter(item => {
        return item.path === 'service-manage' && item.children && item.children.length > 0;
      });
      
      if (serviceMenus.length > 0 && serviceMenus[0].children) {
        // 从菜单数据中提取服务列表
        this.menuList = serviceMenus[0].children.map((item, index) => {
          // 使用label字段作为显示名称，使用name字段作为serviceName
          const displayName = item.label || item.name;
          const serviceName = item.name;
          
          // 将服务名称转为小写作为图标名称
          const iconName = serviceName ? serviceName.toLowerCase() : 'service-default';
          
          return {
            id: String(index + 1),
            name: displayName, // 使用label字段作为显示名称
            serviceName: serviceName, // 保留原始服务名称
            path: item.fullPath,
            icon: iconName, // 使用小写服务名称作为图标名
            serviceId: (item.meta && item.meta.params && item.meta.params.serviceId) || '',
            // 添加服务状态相关的属性
            serviceStateCode: item.meta && item.meta.obj ? item.meta.obj.serviceStateCode : 1,
            alertNum: item.meta && item.meta.obj ? item.meta.obj.alertNum : 0,
            needRestart: item.meta && item.meta.obj ? item.meta.obj.needRestart : false,
            rawData: item.meta && item.meta.obj ? item.meta.obj : {},
            menuVisible: false,
            popoverVisible: false, // 提示框可见性控制
            popoverInContent: false, // 标记鼠标是否在提示框内容区域
          };
        });
      } else {
        // 尝试从直接保存的服务列表获取
        try {
          const serviceList = JSON.parse(localStorage.getItem('serviceList') || '[]');
          if (serviceList.length > 0) {
            this.menuList = serviceList.map((item, index) => {
              // 使用label字段作为显示名称，使用serviceName字段作为服务名称
              const displayName = item.label || item.serviceName;
              const serviceName = item.serviceName;
              
              return {
                id: String(index + 1),
                name: displayName, // 使用label字段作为显示名称
                serviceName: serviceName, // 使用serviceName字段作为服务名称
                icon: serviceName ? serviceName.toLowerCase() : 'service-default', // 图标名称使用serviceName
                serviceId: item.id,
                path: `/service-manage/service-list/${item.id}`,
                // 添加服务状态相关的默认属性
                serviceStateCode: 1,
                alertNum: 0,
                needRestart: false,
                rawData: {},
                menuVisible: false,
                popoverVisible: false, // 提示框可见性控制
                popoverInContent: false, // 标记鼠标是否在提示框内容区域
              };
            });
          } else {
            // 使用默认菜单数据
            this.useDefaultMenus();
          }
        } catch (e) {
          console.error('加载服务列表失败:', e);
          this.useDefaultMenus();
        }
      }
    },
    
    // 使用默认菜单数据
    useDefaultMenus() {
      // 首先创建不同的服务分组列表
      const coreServicesList = [
        { id: '1', name: 'HDFS 分布式文件系统', serviceName: 'HDFS', icon: 'hdfs', path: '/service-manage/service-list/1', serviceId: '1', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '2', name: 'YARN 资源调度系统', serviceName: 'YARN', icon: 'yarn', path: '/service-manage/service-list/2', serviceId: '2', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '3', name: 'HBASE 分布式数据库', serviceName: 'HBASE', icon: 'hbase', path: '/service-manage/service-list/3', serviceId: '3', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '4', name: 'HIVE 数据仓库', serviceName: 'HIVE', icon: 'hive', path: '/service-manage/service-list/4', serviceId: '4', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '5', name: 'ZOOKEEPER 分布式协调服务', serviceName: 'ZOOKEEPER', icon: 'zookeeper', path: '/service-manage/service-list/5', serviceId: '5', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '6', name: 'SPARK 分布式计算引擎', serviceName: 'SPARK', icon: 'spark', path: '/service-manage/service-list/6', serviceId: '6', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false }
      ];
      
      const managementServicesList = [
        // 添加大数据基础平台作为管理服务分组的第一个
        { id: '0', name: '大数据基础平台', serviceName: 'DATASOPHON', icon: 'logo', path: '/service-manage', serviceId: '', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '7', name: 'ALERTMANAGER 告警管理', serviceName: 'ALERTMANAGER', icon: 'alertmanager', path: '/service-manage/service-list/7', serviceId: '7', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '8', name: 'PROMETHEUS 监控系统', serviceName: 'PROMETHEUS', icon: 'prometheus', path: '/service-manage/service-list/8', serviceId: '8', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '9', name: 'GRAFANA 可视化平台', serviceName: 'GRAFANA', icon: 'grafana', path: '/service-manage/service-list/9', serviceId: '9', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '10', name: 'PUSHGATEWAY 数据推送', serviceName: 'PUSHGATEWAY', icon: 'pushgateway', path: '/service-manage/service-list/10', serviceId: '10', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false }
      ];
      
      // 合并两个列表
      this.menuList = [...coreServicesList, ...managementServicesList];
    },
    
    // 判断当前服务是否激活
    isActiveService(service) {
      if (!this.currentServiceId && !service.serviceId) {
        // 总览页面
        return this.$route.path === '/service-manage';
      }
      return service.serviceId === this.currentServiceId;
    },
    
    // 选择服务菜单
    selectMenu(menu) {
      // 更新Vuex中的serviceId
      if (menu.serviceId) {
        // 直接同步设置serviceId
        this.$store.commit('setting/setServiceId', menu.serviceId);
        
      // 导航到对应的服务详情页面
        const targetPath = menu.path || `/service-manage/service-list/${menu.serviceId}`;
        this.$router.push(targetPath).catch(err => {
          if (err.name !== 'NavigationDuplicated') {
            throw err;
          }
        });
      } else {
        // 没有serviceId，直接导航
        if (menu.path) {
          this.$router.push(menu.path).catch(err => {
          if (err.name !== 'NavigationDuplicated') {
            throw err;
          }
        });
        }
      }
    },
    
    // 显示告警详情
    showAlarm(service, event) {
      // 阻止事件冒泡
      if (event && event.stopPropagation) {
        event.stopPropagation();
      }
      
      // 显示告警详情弹窗
      if (!service.serviceId) return;
      
      const AlarmModal = () => import('@/components/alarmModal/index.vue');
      
      this.$confirm({
        width: 1000,
        title: "告警详情",
        content: h => h(AlarmModal, {
          props: {
            serviceInstanceId: service.serviceId
          }
        }),
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    
    // 显示配置变更
    showConfigCompare(service, event) {
      // 阻止事件冒泡
      if (event && event.stopPropagation) {
        event.stopPropagation();
      }
      
      // 显示配置变更对比弹窗
      if (!service.serviceId) return;

      const TextCompare = () => import('@/components/menu/commponents/textCompare.vue');
      
      this.$confirm({
        width: 1200,
        title: "服务版本对比",
        content: h => h(TextCompare, {
          props: {
            serviceId: {
              id: service.serviceId
            },
            callBack: () => this.loadMenuData()
          }
        }),
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    
    // 更多菜单选项
    getMoreMenu(service) {
      const arr = [
        { name: "启动", key: "start" },
        { name: "停止", key: "stop" },
        { name: "重启", key: "restart" },
        { name: "删除", key: "del" }
      ];
      
      return arr.map((item, index) => {
        return (
          <div key={index}>
            <a
              class="more-menu-btn"
              style="border-width:0px;min-width:100px;"
              onClick={() => this.handleServiceAction(item, service)}
            >
              {item.name}
            </a>
          </div>
        );
      });
    },
    
    // 处理服务操作
    handleServiceAction(action, service, event) {
      // 阻止事件冒泡
      if (event && event.stopPropagation) {
        event.stopPropagation();
      }
      
      // 隐藏菜单
      this.hideActionMenu();
      
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
              {'确认' + (action.key=='start'?'开启':action.key=='stop'?'停止':action.key=='restart'?'重启':action.key=='del'?'删除':"") + service.name +'吗？'}
            </div>
            <div style="margin-top:20px;text-align:right;padding:0 30px 30px 30px">
              <a-button
                style="margin-right:10px;"
                type="primary"
                onClick={() => this.executeServiceAction(action, service)}
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
    
    // 执行服务操作
    executeServiceAction(action, service) {
      if (action.key === "del") {
        if (!service.serviceId) return;
        
        this.$axiosPost('/ddh/cluster/service/instance/delete', {
          serviceInstanceId: service.serviceId,
        }).then((res) => {
          if (res.code === 200) {
            this.$message.success("操作成功");
            this.$destroyAll();
            this.$router.push("/service-manage");
            this.loadMenuData();
          }
        });
        return;
      }
      
      if (!service.serviceId) return;
      
      const params = {
        clusterId: this.clusterId,
        commandType: action.key === "stop" ? "STOP_SERVICE" : action.key === "start" ? "START_SERVICE" : "RESTART_SERVICE",
        serviceInstanceIds: service.serviceId,
      };
      
      this.$axiosPost(global.API.generateServiceCommand, params).then((res) => {
        if (res.code === 200) {
          this.$message.success("操作成功");
          this.$destroyAll();
          this.showClusterSetting(true);
        }
      });
    },
    
    // 计算服务名称的样式
    getServiceClassNameStyle(service) {
      // 如果有重启的图标没有告警的图标
      if (service.needRestart && (![3,4].includes(service.serviceStateCode) && service.alertNum === 0)) {
        return { 'max-width': '116px' };
      }
      // 如果没有重启的图标有告警的图标
      if (!service.needRestart && ([3,4].includes(service.serviceStateCode) && service.alertNum > 0)) {
        return { 'max-width': '116px' };
      }
      // 如果有重启的图标有告警的图标
      if (service.needRestart && ([3,4].includes(service.serviceStateCode) && service.alertNum > 0)) {
        return { 'max-width': '106px' };
      }
      // 如果没有重启的图标没有告警的图标
      return { 'max-width': '126px' };
    },
    
    // 切换服务操作菜单的可见性
    toggleServiceMenu(service, event) {
      // 阻止事件冒泡
      if (event && event.stopPropagation) {
        event.stopPropagation();
      }
      
      // 关闭所有其他菜单
      this.menuList.forEach(item => {
        if (item !== service) {
          this.$set(item, 'menuVisible', false);
        }
      });
      
      // 切换当前菜单的可见性
      this.$set(service, 'menuVisible', !service.menuVisible);
      
      // 如果菜单变为可见，确保任何可能的悬浮窗都被关闭
      if (service.menuVisible) {
        this.$set(service, 'popoverVisible', false);
        this.$set(service, 'popoverInContent', false);
      }
    },
    
    // 处理服务操作菜单的可见性变化
    handleVisibleChange(visible, service) {
      // 如果下拉菜单变为不可见，更新状态
      if (!visible) {
        this.$set(service, 'menuVisible', false);
      } else {
        // 如果变为可见，确保其他菜单都已关闭
        this.menuList.forEach(item => {
          if (item !== service) {
            this.$set(item, 'menuVisible', false);
          }
        });
      }
    },
    
    // 处理服务项点击事件
    handleServiceItemClick(service) {
      // 立即关闭所有服务的悬浮窗
      this.closeAllPopovers();
      
      // 设置切换锁定，防止在切换过程中显示悬浮窗
      this.switchingService = true;
      
      // 保持折叠状态
      const wasSidebarCollapsed = this.sidebarCollapsed;
      
      // 调用原有的服务选择方法
      this.selectMenu(service);
      
      // 恢复原有的折叠状态，并在切换完成后解除锁定
      this.$nextTick(() => {
        this.sidebarCollapsed = wasSidebarCollapsed;
        
        // 延迟100ms解除锁定，确保切换过程完成
        setTimeout(() => {
          this.switchingService = false;
        }, 100);
      });
    },
    
    // 处理服务项鼠标进入事件
    handleServiceMouseEnter(service) {
      // 如果正在切换服务，不显示悬浮窗
      if (this.switchingService) {
        return;
      }
      
      // 关闭所有其他服务的悬浮框
      this.closeAllPopoversExcept(service.id);
      
      // 清除当前服务的定时器
      if (this.popoverTimeoutMap[service.id]) {
        clearTimeout(this.popoverTimeoutMap[service.id]);
        delete this.popoverTimeoutMap[service.id];
      }
      
      // 延迟显示悬浮框，避免快速移动时频繁触发
      this.popoverTimeoutMap[service.id] = setTimeout(() => {
        if (!this.switchingService) {
          service.popoverVisible = true;
        }
        delete this.popoverTimeoutMap[service.id];
      }, 200);
    },
    
    // 处理服务项鼠标离开事件
    handleServiceMouseLeave(service) {
      // 如果正在切换服务，立即关闭悬浮窗
      if (this.switchingService) {
        service.popoverVisible = false;
        return;
      }
      
      // 清除显示定时器
      if (this.popoverTimeoutMap[service.id]) {
        clearTimeout(this.popoverTimeoutMap[service.id]);
        delete this.popoverTimeoutMap[service.id];
      }
      
      // 设置延迟关闭
      this.popoverTimeoutMap[service.id] = setTimeout(() => {
        if (!service.popoverInContent) {
          service.popoverVisible = false;
        }
        delete this.popoverTimeoutMap[service.id];
      }, 300);
    },
    
    // 处理Popover可见性变化
    handlePopoverVisibleChange(visible, service) {
      // 如果正在切换服务或不可见，强制关闭悬浮窗
      if (this.switchingService || !visible) {
        service.popoverVisible = false;
        return;
      }
      
      if (!service.popoverInContent) {
        service.popoverVisible = visible;
      }
    },
    
    // 处理Popover内容进入事件
    handlePopoverContentEnter(service) {
      // 清除已存在的定时器
      if (this.popoverTimeoutMap[service.id]) {
        clearTimeout(this.popoverTimeoutMap[service.id]);
        delete this.popoverTimeoutMap[service.id];
      }
      
      // 标记鼠标在内容区域
      service.popoverInContent = true;
      
      // 保持popover可见
      service.popoverVisible = true;
    },
    
    // 处理Popover内容离开事件
    handlePopoverContentLeave(service) {
      // 如果正在切换服务，立即关闭悬浮窗
      if (this.switchingService) {
        service.popoverVisible = false;
        service.popoverInContent = false;
        return;
      }
      
      // 标记鼠标离开内容区域
      service.popoverInContent = false;
      
      // 延迟100ms关闭
      this.popoverTimeoutMap[service.id] = setTimeout(() => {
        service.popoverVisible = false;
        delete this.popoverTimeoutMap[service.id];
      }, 100);
    },
    
    // 关闭所有服务的悬浮窗
    closeAllPopovers() {
      // 清除所有计时器
      Object.keys(this.popoverTimeoutMap).forEach(id => {
        clearTimeout(this.popoverTimeoutMap[id]);
        delete this.popoverTimeoutMap[id];
      });
      
      // 关闭所有服务的悬浮窗
      if (this.menuList && this.menuList.length > 0) {
        this.menuList.forEach(service => {
          service.popoverVisible = false;
          service.popoverInContent = false;
        });
      }
    },
    
    // 关闭除指定服务外的所有悬浮框
    closeAllPopoversExcept(exceptServiceId) {
      // 清除其他服务的计时器
      Object.keys(this.popoverTimeoutMap).forEach(id => {
        if (id !== exceptServiceId.toString()) {
          clearTimeout(this.popoverTimeoutMap[id]);
          delete this.popoverTimeoutMap[id];
        }
      });
      
      // 关闭其他服务的悬浮窗
      if (this.menuList && this.menuList.length > 0) {
        this.menuList.forEach(service => {
          if (service.id !== exceptServiceId) {
            service.popoverVisible = false;
            service.popoverInContent = false;
          }
        });
      }
    },
    
    // 显示全局服务操作菜单
    toggleActionMenu() {
      this.activeService = this.menuList.find(service => service.serviceId === this.currentServiceId);
      this.showActionMenu = true;
    },
    
    // 隐藏全局服务操作菜单
    hideActionMenu() {
      this.showActionMenu = false;
      // 关闭所有服务的菜单状态
      if (this.activeService) {
        this.$set(this.activeService, 'menuVisible', false);
      }
      this.coreServices.forEach(item => {
        this.$set(item, 'menuVisible', false);
      });
      this.managementServices.forEach(item => {
        this.$set(item, 'menuVisible', false);
      });
      this.activeService = null;
      // 移除事件监听器
      document.removeEventListener('click', this.handleOutsideClick);
    },
    
    // 显示服务操作菜单
    showActionMenuForService(service, event) {
      // 阻止事件冒泡
      if (event && event.stopPropagation) {
        event.stopPropagation();
      }
      
      // 如果当前服务的菜单已经显示，则关闭它（二次点击关闭功能）
      if (service.menuVisible && this.activeService === service) {
        this.$set(service, 'menuVisible', false);
        this.hideActionMenu();
        return;
      }
      
      // 关闭其他所有服务的菜单
      this.coreServices.forEach(item => {
        this.$set(item, 'menuVisible', false);
      });
      this.managementServices.forEach(item => {
        this.$set(item, 'menuVisible', false);
      });
      
      // 设置当前服务菜单为显示状态
      this.$set(service, 'menuVisible', true);
      
      // 设置当前激活的服务
      this.activeService = service;
      
      // 计算菜单位置
      if (event && event.target) {
        // 获取按钮元素
        const buttonElement = event.target.closest('.modern-action-btn');
        if (buttonElement) {
          const rect = buttonElement.getBoundingClientRect();
          
          // 计算菜单位置，确保它显示在按钮的右侧
          const menuTop = rect.top;
          const menuLeft = rect.right + 10;
          
          // 检查是否超出屏幕边界
          const viewportWidth = window.innerWidth;
          const viewportHeight = window.innerHeight;
          
          // 如果超出右侧边界，则显示在左侧
          const finalLeft = menuLeft + 180 > viewportWidth ? rect.left - 190 : menuLeft;
          
          // 如果超出底部边界，则向上移动
          const finalTop = menuTop + 250 > viewportHeight ? viewportHeight - 260 : menuTop;
          
          this.actionMenuStyle = {
            position: 'fixed',
            top: `${finalTop}px`,
            left: `${finalLeft}px`,
            transform: 'none',
            background: 'rgba(255, 255, 255, 0.98)',
            backdropFilter: 'blur(20px)',
            WebkitBackdropFilter: 'blur(20px)',
            padding: '16px',
            borderRadius: '16px',
            boxShadow: '0 10px 30px rgba(0, 0, 0, 0.12), 0 6px 16px rgba(0, 0, 0, 0.08), 0 2px 6px rgba(0, 0, 0, 0.06)',
            border: '1px solid rgba(255, 255, 255, 0.6)',
            zIndex: 1500,
            minWidth: '180px',
            animation: 'menu-appear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1)'
          };
        }
      }
      
      // 显示菜单
      this.showActionMenu = true;
      
      // 添加点击外部关闭菜单的事件监听
      setTimeout(() => {
        document.addEventListener('click', this.handleOutsideClick);
      }, 0);
    },
    
    // 处理点击外部关闭菜单
    handleOutsideClick(event) {
      if (this.showActionMenu && this.$el && !this.$el.querySelector('.service-action-menu').contains(event.target)) {
        this.hideActionMenu();
      }
    },
  }
};
</script>

<style lang="less" scoped>
.cdh-service-page {
  display: flex;
  height: calc(100vh - 56px);
  background: #f5f6fa;
  width: 100%;
  max-width: 100%;
  overflow: visible !important;
}

.service-sidebar {
  width: 280px; /* 增加宽度以显示完整服务名称 */
  background: linear-gradient(180deg, rgba(248, 249, 250, 0.95), rgba(255, 255, 255, 0.95));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border-right: 1px solid rgba(224, 224, 224, 0.6);
  overflow-y: auto;
  flex-shrink: 0;
  transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  box-shadow: inset -1px 0 0 rgba(255, 255, 255, 0.5);
  
  /* 折叠状态 - 彻底重构 */
  &.collapsed {
    width: 72px !important;
    min-width: 72px !important;
    max-width: 72px !important;
    overflow: hidden !important;
    
    .service-title {
      padding: 12px 8px !important;
      display: flex !important;
      justify-content: center !important;
      align-items: center !important;
      
      .cluster-info,
      .loading-text {
        display: none !important;
      }
      
      .sidebar-controls {
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        width: 100% !important;
        margin: 0 !important;
        
        .sidebar-toggle {
          width: 44px !important;
          height: 44px !important;
          min-width: 44px !important;
          min-height: 44px !important;
          border-radius: 12px !important;
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(88, 86, 214, 0.08)) !important;
          backdrop-filter: blur(10px) !important;
          -webkit-backdrop-filter: blur(10px) !important;
          border: 1px solid rgba(0, 122, 255, 0.1) !important;
          box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1) !important;
          transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
          display: flex !important;
          justify-content: center !important;
          align-items: center !important;
          
          &:hover {
            background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15)) !important;
            transform: scale(1.05) !important;
            box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2) !important;
            border-color: rgba(0, 122, 255, 0.2) !important;
          }
          
          .anticon {
            color: #007AFF !important;
            font-size: 18px !important;
            filter: drop-shadow(0 1px 2px rgba(0, 122, 255, 0.2)) !important;
          }
        }
      }
    }
    
    .group-title {
      padding: 8px !important;
      margin: 8px 2px 12px 2px !important;
      display: flex !important;
      justify-content: center !important;
      align-items: center !important;
      min-height: 48px !important;
      border-radius: 12px !important;
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.05), rgba(88, 86, 214, 0.05)) !important;
      backdrop-filter: blur(10px) !important;
      -webkit-backdrop-filter: blur(10px) !important;
      border: 1px solid rgba(0, 122, 255, 0.08) !important;
      box-shadow: 0 2px 6px rgba(0, 122, 255, 0.08) !important;
      transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
      cursor: pointer !important;
      position: relative !important;
      
      span {
        display: none !important;
      }
      
      .collapse-icon,
      .collapse-icon-wrapper {
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        width: 14px !important;
        height: 14px !important;
        font-size: 10px !important;
        color: #007AFF !important;
        position: absolute !important;
        top: 6px !important;
        right: 6px !important;
        background: transparent !important;
        border: none !important;
        box-shadow: none !important;
        border-radius: 50% !important;
        outline: none !important;
      }
      
      .collapse-icon-wrapper .anticon,
      .collapse-icon.anticon {
        background: transparent !important;
        border: none !important;
        border-radius: 50% !important;
        box-shadow: none !important;
        outline: none !important;
      }
      
      .group-icon {
        margin: 0 !important;
        font-size: 20px !important;
        color: #007AFF !important;
        filter: drop-shadow(0 1px 2px rgba(0, 122, 255, 0.2)) !important;
        transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
      }
      
      &:hover {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1)) !important;
        transform: scale(1.02) !important;
        box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15) !important;
        border-color: rgba(0, 122, 255, 0.15) !important;
        
        .group-icon {
          color: #0056CC !important;
          transform: scale(1.1) !important;
        }
      }
    }
    
    .service-item {
      padding: 0 !important;
      margin: 2px 2px !important;
      display: flex !important;
      justify-content: center !important;
      align-items: center !important;
      min-height: 48px !important;
      border-radius: 12px !important;
      position: relative !important;
      overflow: visible !important;
      transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
      
      .status-indicator,
      .alert-indicators,
      .modern-action-btn {
        display: none !important;
      }
      
      .service-name-container {
        display: flex !important;
        justify-content: center !important;
        align-items: center !important;
        margin: 0 !important;
        width: 100% !important;
        height: 44px !important;
        max-width: none !important;
        padding: 0 !important;
        
        .service-name {
          display: none !important;
        }
        
        .service-icon {
          margin: 0 auto !important;
          width: 44px !important;
          height: 44px !important;
          min-width: 44px !important;
          min-height: 44px !important;
          border-radius: 12px !important;
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(88, 86, 214, 0.08)) !important;
          backdrop-filter: blur(10px) !important;
          -webkit-backdrop-filter: blur(10px) !important;
          border: 1px solid rgba(0, 122, 255, 0.1) !important;
          box-shadow: 0 2px 6px rgba(0, 122, 255, 0.1) !important;
          transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
          display: flex !important;
          justify-content: center !important;
          align-items: center !important;
          overflow: visible !important;
          position: relative !important;
          
          .svg-icon,
          img {
            font-size: 20px !important;
            width: 20px !important;
            height: 20px !important;
            color: #007AFF !important;
            filter: drop-shadow(0 1px 2px rgba(0, 122, 255, 0.2)) !important;
            transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
            flex-shrink: 0 !important;
            display: block !important;
            margin: 0 auto !important;
            vertical-align: middle !important;
          }
        }
      }
      
      &:hover {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.06), rgba(88, 86, 214, 0.06)) !important;
        transform: scale(1.02) !important;
        
        .service-icon {
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15)) !important;
          transform: scale(1.05) !important;
          box-shadow: 0 4px 12px rgba(0, 122, 255, 0.2) !important;
          border-color: rgba(0, 122, 255, 0.2) !important;
          
          .svg-icon,
          img {
            color: #0056CC !important;
            transform: scale(1.05) !important;
            width: 20px !important;
            height: 20px !important;
          }
        }
      }
      
      &.active {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1)) !important;
        
        .service-icon {
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2)) !important;
          box-shadow: 0 3px 10px rgba(0, 122, 255, 0.25) !important;
          border-color: rgba(0, 122, 255, 0.25) !important;
          
          .svg-icon {
            color: #0056CC !important;
            width: 20px !important;
            height: 20px !important;
          }
        }
      }
    }
  }
  
  /* 允许侧边栏滚动但使用玻璃效果滚动条 */
  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: rgba(0, 0, 0, 0.02);
    border-radius: 4px;
  }
  
  &::-webkit-scrollbar-thumb {
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.08), rgba(0, 0, 0, 0.12));
    border-radius: 4px;
    border: 2px solid transparent;
    background-clip: content-box;
    backdrop-filter: blur(10px);
  }
  
  &::-webkit-scrollbar-thumb:hover {
    background: linear-gradient(180deg, rgba(0, 0, 0, 0.15), rgba(0, 0, 0, 0.2));
    background-clip: content-box;
  }
  
  /* Firefox兼容 */
  scrollbar-width: thin;
  scrollbar-color: rgba(0, 0, 0, 0.1) transparent;
  
  &.cdh-style {
    background: linear-gradient(180deg, rgba(245, 247, 248, 0.95), rgba(255, 255, 255, 0.95));
    backdrop-filter: blur(20px);
    -webkit-backdrop-filter: blur(20px);
    border-right: 1px solid rgba(221, 228, 229, 0.6);
    
    .service-title {
      padding: 20px 16px 16px 24px;
      border-bottom: 1px solid rgba(221, 228, 229, 0.6);
      position: sticky;
      top: 0;
      z-index: 10;
      background: linear-gradient(180deg, rgba(245, 247, 248, 0.95), rgba(255, 255, 255, 0.95));
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      display: flex;
      justify-content: space-between;
      align-items: center;
      gap: 16px;
      
      .cluster-info {
        flex: 1;
        min-width: 0; /* 允许flex子项收缩 */
        
        .cluster-name {
          font-size: 18px;
          font-weight: 700;
          color: #1D1D1F;
          letter-spacing: -0.5px;
          text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
          margin-bottom: 4px;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
        }
        
        .cluster-details {
          display: flex;
          gap: 12px;
          align-items: center;
          
          .cluster-frame {
            font-size: 13px;
            font-weight: 600;
            color: #007AFF;
            background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
            padding: 2px 8px;
            border-radius: 6px;
            border: 1px solid rgba(0, 122, 255, 0.2);
            white-space: nowrap;
          }
          
          .cluster-mode {
            font-size: 12px;
            font-weight: 500;
            color: #34C759;
            background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 176, 199, 0.1));
            padding: 2px 8px;
            border-radius: 6px;
            border: 1px solid rgba(52, 199, 89, 0.2);
            white-space: nowrap;
          }
        }
      }
      
      .loading-text {
        font-size: 16px;
        font-weight: 500;
        color: #8E8E93;
        letter-spacing: -0.3px;
      }
      
      .sidebar-controls {
        display: flex;
        align-items: center;
        gap: 8px;
        flex-shrink: 0; /* 防止控制按钮被压缩 */
      }
    }
    
    .group-title {
      padding: 20px 24px 16px;
      font-size: 16px;
      font-weight: 700;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 249, 250, 0.95));
      backdrop-filter: blur(30px);
      -webkit-backdrop-filter: blur(30px);
      color: #1D1D1F;
      border: none;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      position: relative;
      letter-spacing: -0.4px;
      text-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
      transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
      margin: 8px 12px;
      border-radius: 16px;
      box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06), 0 1px 3px rgba(0, 0, 0, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.8);
      overflow: hidden;
      
      /* 苹果风格的动态背景 */
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.03), rgba(88, 86, 214, 0.03));
        opacity: 0;
        transition: opacity 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
        border-radius: 16px;
      }
      
      /* 苹果风格的左侧装饰条 */
      &::after {
        content: '';
        position: absolute;
        left: 0;
        top: 20%;
        bottom: 20%;
        width: 4px;
        background: linear-gradient(180deg, #007AFF, #5856D6, #AF52DE);
        border-radius: 0 2px 2px 0;
        opacity: 0;
        transform: scaleY(0);
        transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
      }
      
      &:hover {
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.98), rgba(248, 249, 250, 0.98));
        transform: translateY(-2px) scale(1.02);
        box-shadow: 0 8px 25px rgba(0, 122, 255, 0.12), 0 3px 8px rgba(0, 0, 0, 0.08);
        border-color: rgba(0, 122, 255, 0.2);
        
        &::before {
          opacity: 1;
        }
        
        &::after {
          opacity: 1;
          transform: scaleY(1);
        }
        
        .group-icon {
          transform: scale(1.15) rotate(5deg);
          color: #007AFF;
          filter: drop-shadow(0 2px 4px rgba(0, 122, 255, 0.3));
        }
        
        .collapse-icon {
          transform: scale(1.1) rotate(180deg);
          color: #007AFF;
        }
        
        span {
          color: #007AFF;
        }
      }
      
      &:active {
        transform: translateY(-1px) scale(0.98);
        box-shadow: 0 4px 15px rgba(0, 122, 255, 0.15), 0 2px 5px rgba(0, 0, 0, 0.1);
      }
      
      .group-icon {
        font-size: 20px;
        color: #34C759;
        margin-right: 12px;
        transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
        background: linear-gradient(135deg, rgba(52, 199, 89, 0.1), rgba(48, 176, 199, 0.1));
        width: 36px;
        height: 36px;
        border-radius: 12px;
        display: flex;
        align-items: center;
        justify-content: center;
        position: relative;
        z-index: 2;
        box-shadow: 0 2px 8px rgba(52, 199, 89, 0.2);
      }
      
      span {
        position: relative;
        z-index: 2;
        transition: color 0.3s ease;
        font-weight: 700;
      }
      
      .collapse-icon-wrapper {
        display: flex;
        align-items: center;
        justify-content: center;
        width: 32px;
        height: 32px;
        border-radius: 10px;
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(88, 86, 214, 0.08));
        backdrop-filter: blur(10px);
        -webkit-backdrop-filter: blur(10px);
        border: 1px solid rgba(0, 122, 255, 0.1);
        box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
        transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
        cursor: pointer;
        position: relative;
        z-index: 2;
        
        &:hover {
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15));
          transform: scale(1.1) rotate(5deg);
          box-shadow: 0 6px 20px rgba(0, 122, 255, 0.25);
          border-color: rgba(0, 122, 255, 0.2);
        }
        
        .collapse-icon {
          font-size: 14px;
          color: #007AFF;
          transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
          filter: drop-shadow(0 1px 2px rgba(0, 122, 255, 0.2));
        }
      }
    }
    
    /* 管理服务组的特殊样式 */
    .management-group .group-title {
      .group-icon {
        color: #FF9500;
        background: linear-gradient(135deg, rgba(255, 149, 0, 0.1), rgba(255, 204, 0, 0.1));
        box-shadow: 0 2px 8px rgba(255, 149, 0, 0.2);
      }
      
      &:hover .group-icon {
        color: #FF9500;
        filter: drop-shadow(0 2px 4px rgba(255, 149, 0, 0.3));
      }
    }
    
    .service-list {
      padding: 0;
    }
    
    .service-item {
      display: flex;
      align-items: center;
      padding: 12px 16px;
      cursor: pointer;
      transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
      position: relative;
      min-height: 56px;
      border: none;
      margin: 4px 12px;
      border-radius: 16px;
      gap: 12px;
      background: linear-gradient(135deg, rgba(255, 255, 255, 0.8), rgba(248, 249, 250, 0.8));
      backdrop-filter: blur(20px);
      -webkit-backdrop-filter: blur(20px);
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.04), 0 1px 2px rgba(0, 0, 0, 0.02);
      border: 1px solid rgba(255, 255, 255, 0.6);
      overflow: hidden;
      
      /* 苹果风格的动态背景层 */
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.03), rgba(88, 86, 214, 0.03));
        opacity: 0;
        transition: opacity 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
        border-radius: 16px;
      }
      
      /* 苹果风格的左侧激活条 */
      &::after {
        content: '';
        position: absolute;
        left: 0;
        top: 25%;
        bottom: 25%;
        width: 0;
        background: linear-gradient(180deg, #007AFF, #5856D6, #AF52DE);
        border-radius: 0 3px 3px 0;
        transition: width 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
        box-shadow: 2px 0 8px rgba(0, 122, 255, 0.3);
      }
      
      &:hover {
        background: linear-gradient(135deg, rgba(255, 255, 255, 0.95), rgba(248, 249, 250, 0.95));
        transform: translateY(-3px) scale(1.02);
        box-shadow: 0 8px 25px rgba(0, 122, 255, 0.15), 0 3px 8px rgba(0, 0, 0, 0.08);
        border-color: rgba(0, 122, 255, 0.2);
        
        &::before {
          opacity: 1;
        }
        
        &::after {
          width: 5px;
        }
      }
      
      &:active {
        transform: translateY(-1px) scale(0.98);
        box-shadow: 0 4px 15px rgba(0, 122, 255, 0.2), 0 2px 5px rgba(0, 0, 0, 0.1);
      }
      
      &.active {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(88, 86, 214, 0.08));
        border-color: rgba(0, 122, 255, 0.3);
        box-shadow: 0 4px 16px rgba(0, 122, 255, 0.2), 0 2px 4px rgba(0, 0, 0, 0.08);
        
        &::before {
          opacity: 1;
        }
        
        &::after {
          width: 5px;
        }
      }
      
      .status-indicator {
        width: 24px;
        text-align: center;
        flex-shrink: 0;
        display: flex;
        align-items: center;
        justify-content: center;
        
        .status-icon {
          font-size: 16px;
          transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
          position: relative;
          
          &.success {
            color: #34C759;
            filter: drop-shadow(0 2px 4px rgba(52, 199, 89, 0.3));
            
            &::before {
              content: '';
              position: absolute;
              top: 50%;
              left: 50%;
              transform: translate(-50%, -50%);
              width: 20px;
              height: 20px;
              background: radial-gradient(circle, rgba(52, 199, 89, 0.15) 0%, transparent 70%);
              border-radius: 50%;
              opacity: 0;
              transition: opacity 0.3s ease;
            }
            
            &:hover::before {
              opacity: 1;
            }
          }
          
          &.warning {
            color: #FF9500;
            filter: drop-shadow(0 2px 4px rgba(255, 149, 0, 0.3));
            
            &::before {
              content: '';
              position: absolute;
              top: 50%;
              left: 50%;
              transform: translate(-50%, -50%);
              width: 20px;
              height: 20px;
              background: radial-gradient(circle, rgba(255, 149, 0, 0.15) 0%, transparent 70%);
              border-radius: 50%;
              opacity: 0;
              transition: opacity 0.3s ease;
            }
            
            &:hover::before {
              opacity: 1;
            }
          }
          
          &.error {
            color: #FF3B30;
            filter: drop-shadow(0 2px 4px rgba(255, 59, 48, 0.3));
            
            &::before {
              content: '';
              position: absolute;
              top: 50%;
              left: 50%;
              transform: translate(-50%, -50%);
              width: 20px;
              height: 20px;
              background: radial-gradient(circle, rgba(255, 59, 48, 0.15) 0%, transparent 70%);
              border-radius: 50%;
              opacity: 0;
              transition: opacity 0.3s ease;
            }
            
            &:hover::before {
              opacity: 1;
            }
          }
          
          &.empty {
            width: 16px;
            height: 16px;
            display: inline-block;
            background: linear-gradient(135deg, rgba(142, 142, 147, 0.1), rgba(174, 174, 178, 0.1));
            border-radius: 50%;
            border: 1px solid rgba(142, 142, 147, 0.2);
          }
        }
      }
      
      .service-name-container {
        flex: 1;
        display: flex;
        align-items: center;
        min-width: 0;
        overflow: hidden;
        max-width: 160px;
        
        .service-icon {
          margin-right: 12px;
          width: 28px;
          height: 28px;
          display: flex;
          align-items: center;
          justify-content: center;
          border-radius: 12px;
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(88, 86, 214, 0.08));
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
          border: 1px solid rgba(0, 122, 255, 0.1);
          box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
          position: relative;
          overflow: hidden;
          
          &::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.2), rgba(248, 249, 250, 0.2));
            opacity: 0;
            transition: opacity 0.3s ease;
            border-radius: 12px;
          }
          
          .svg-icon {
            font-size: 16px;
            color: #007AFF;
            transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
            position: relative;
            z-index: 2;
            filter: drop-shadow(0 1px 2px rgba(0, 122, 255, 0.2));
          }
        }
        
        .service-name {
          flex: 1;
          font-size: 14px;
          color: #1D1D1F;
          font-weight: 600;
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          padding-right: 4px;
          letter-spacing: -0.2px;
          transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
          text-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
          min-width: 0;
          max-width: 120px;
        }
      }
      
      &:hover {
        .status-indicator .status-icon {
          transform: scale(1.2);
          filter: drop-shadow(0 2px 4px rgba(0, 0, 0, 0.2));
        }
        
        .service-name-container {
          .service-icon {
            background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15));
            transform: scale(1.1) rotate(5deg);
            box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
            border-color: rgba(0, 122, 255, 0.2);
            
            &::before {
              opacity: 1;
            }
            
            .svg-icon {
              color: #0056CC;
              transform: scale(1.1);
            }
          }
          
          .service-name {
            color: #007AFF;
            transform: translateX(2px);
          }
        }
        
        .alert-indicators {
          transform: translateX(-2px);
        }
        
        .modern-action-btn {
          transform: scale(1.1) rotate(90deg);
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15));
          box-shadow: 0 4px 12px rgba(0, 122, 255, 0.25);
        }
      }
      
      &.active .service-name-container {
        .service-icon {
          background: linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2));
          box-shadow: 0 4px 12px rgba(0, 122, 255, 0.3);
          border-color: rgba(0, 122, 255, 0.3);
          
          &::before {
            opacity: 1;
          }
          
          .svg-icon {
            color: #0056CC;
          }
        }
        
        .service-name {
          color: #007AFF;
          font-weight: 700;
        }
      }
      
      .alert-indicators {
        display: flex;
        align-items: center;
        flex-shrink: 0;
        gap: 6px;
        
        .alert-badge {
          display: flex;
          align-items: center;
          cursor: pointer;
          line-height: 1;
          padding: 4px 8px;
          border-radius: 12px;
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 249, 250, 0.9));
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
          gap: 4px;
          border: 1px solid rgba(255, 255, 255, 0.6);
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06), 0 1px 2px rgba(0, 0, 0, 0.04);
          position: relative;
          overflow: hidden;
          
          &::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: linear-gradient(135deg, rgba(255, 255, 255, 0.3), rgba(248, 249, 250, 0.3));
            opacity: 0;
            transition: opacity 0.3s ease;
            border-radius: 12px;
          }
          
          &:hover {
            transform: scale(1.1) translateY(-1px);
            box-shadow: 0 4px 15px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.08);
            border-color: rgba(255, 255, 255, 0.8);
            
            &::before {
              opacity: 1;
            }
          }
          
          &:active {
            transform: scale(1.05);
            box-shadow: 0 2px 8px rgba(0, 0, 0, 0.15);
          }
          
          .error-color {
            color: #FF3B30;
            font-size: 14px;
            filter: drop-shadow(0 1px 2px rgba(255, 59, 48, 0.3));
            transition: all 0.3s ease;
          }
          
          .warning-color {
            color: #FF9500;
            font-size: 14px;
            filter: drop-shadow(0 1px 2px rgba(255, 149, 0, 0.3));
            transition: all 0.3s ease;
          }
          
          .alert-count {
            font-size: 12px;
            font-weight: 700;
            position: relative;
            top: 0;
            transition: all 0.3s ease;
            
            &.warning-color {
              color: #FF9500;
              text-shadow: 0 1px 2px rgba(255, 149, 0, 0.2);
            }
            
            &.error-color {
              color: #FF3B30;
              text-shadow: 0 1px 2px rgba(255, 59, 48, 0.2);
            }
          }
          
          &:hover {
            .error-color {
              color: #E6342A;
              transform: scale(1.1);
              filter: drop-shadow(0 2px 4px rgba(255, 59, 48, 0.4));
            }
            
            .warning-color {
              color: #E6850E;
              transform: scale(1.1);
              filter: drop-shadow(0 2px 4px rgba(255, 149, 0, 0.4));
            }
            
            .alert-count {
              transform: scale(1.05);
              
              &.warning-color {
                color: #E6850E;
              }
              
              &.error-color {
                color: #E6342A;
              }
            }
          }
        }
        
        .restart-icon {
          font-size: 16px;
          color: #FF9500;
          cursor: pointer;
          padding: 6px;
          border-radius: 10px;
          background: linear-gradient(135deg, rgba(255, 149, 0, 0.12), rgba(255, 193, 7, 0.08));
          backdrop-filter: blur(10px);
          -webkit-backdrop-filter: blur(10px);
          border: 1px solid rgba(255, 149, 0, 0.2);
          transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
          filter: drop-shadow(0 2px 4px rgba(255, 149, 0, 0.2));
          position: relative;
          overflow: hidden;
          
          &::before {
            content: '';
            position: absolute;
            top: 0;
            left: 0;
            right: 0;
            bottom: 0;
            background: linear-gradient(135deg, rgba(255, 149, 0, 0.2), rgba(255, 193, 7, 0.15));
            opacity: 0;
            transition: opacity 0.3s ease;
            border-radius: 10px;
          }
          
          &:hover {
            background: linear-gradient(135deg, rgba(255, 149, 0, 0.18), rgba(255, 193, 7, 0.12));
            transform: scale(1.15) rotate(180deg);
            box-shadow: 0 4px 12px rgba(255, 149, 0, 0.3), 0 2px 4px rgba(0, 0, 0, 0.08);
            border-color: rgba(255, 149, 0, 0.4);
            color: #E6850E;
            
            &::before {
              opacity: 1;
            }
          }
          
          &:active {
            transform: scale(1.05) rotate(180deg);
            box-shadow: 0 2px 8px rgba(255, 149, 0, 0.4);
          }
        }
      }
      
      .expand-icon {
        margin-left: 8px;
        position: relative;
        cursor: pointer;
        display: flex;
        align-items: center;
        flex-shrink: 0;
        
        .modern-action-btn {
          width: 32px;
          height: 32px;
          border-radius: 50%;
          display: flex;
          align-items: center;
          justify-content: center;
          background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 249, 250, 0.9));
          backdrop-filter: blur(20px);
          -webkit-backdrop-filter: blur(20px);
          border: 1px solid rgba(0, 0, 0, 0.08);
          transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
          opacity: 1;
          position: relative;
          overflow: hidden;
          box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
          cursor: pointer;
          
          &:hover {
            background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
            transform: scale(1.05) translateY(-1px);
            box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15), 0 2px 4px rgba(0, 0, 0, 0.08);
            border-color: rgba(0, 122, 255, 0.2);
            color: #007AFF;
            
            .anticon {
              transform: rotate(90deg) scale(1.1);
              color: #007AFF;
            }
          }
          
          &:active {
            transform: scale(0.95);
            box-shadow: 0 1px 4px rgba(0, 122, 255, 0.2);
          }
          
          // 当菜单显示时的激活状态
          &.menu-active {
            background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15));
            border-color: rgba(0, 122, 255, 0.3);
            box-shadow: 0 4px 16px rgba(0, 122, 255, 0.2), 0 2px 4px rgba(0, 0, 0, 0.08);
            
            .anticon {
              transform: rotate(90deg) scale(1.1);
              color: #007AFF;
            }
          }
          
          .anticon {
            font-size: 16px;
            color: #555;
            transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
          }
        }
        
        .ant-dropdown {
          min-width: 140px;
        }
      }
    }
    
    /* 当鼠标悬停在服务项上时显示下拉按钮 */
    .service-item:hover .expand-icon .modern-action-btn {
      opacity: 1;
    }
  }
}

:deep(.ant-menu-item) {
  font-size: 13px;
  padding: 8px 16px;
  
  &:hover {
    color: #0076ce; /* CDH风格的蓝色 */
  }
}

/* 现代化下拉菜单样式 */
:deep(.modern-dropdown-menu) {
  border-radius: 16px;
  overflow: hidden;
  background: rgba(255, 255, 255, 0.95);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  border: 1px solid rgba(255, 255, 255, 0.2);
  box-shadow: 0 8px 32px rgba(0, 0, 0, 0.12), 0 2px 8px rgba(0, 0, 0, 0.08);
  
  .ant-menu-item {
    border-radius: 12px;
    margin: 4px 8px;
    padding: 12px 16px;
    height: auto;
    line-height: 1.3;
    display: flex;
    align-items: center;
    transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    font-size: 14px;
    font-weight: 500;
    position: relative;
    overflow: hidden;
    background-color: transparent;
    border: none;
    
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      width: 3px;
      height: 100%;
      background: transparent;
      transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
      border-radius: 0 2px 2px 0;
    }
    
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.05), rgba(88, 86, 214, 0.05));
      opacity: 0;
      transition: opacity 0.3s ease;
      border-radius: 12px;
    }
    
    .anticon {
      margin-right: 12px;
      font-size: 16px;
      transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
      width: 32px;
      height: 32px;
      border-radius: 10px;
      display: flex;
      align-items: center;
      justify-content: center;
      color: #007AFF;
      position: relative;
      z-index: 2;
    }
    
    span {
      position: relative;
      z-index: 2;
      color: #1D1D1F;
      transition: color 0.3s ease;
    }
    
    &:hover {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.08), rgba(88, 86, 214, 0.08));
      color: #007AFF;
      transform: translateX(3px) translateY(-1px);
      box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15);
      
      &::before {
        background: linear-gradient(135deg, #007AFF, #5856D6);
        width: 4px;
      }
      
      &::after {
        opacity: 1;
      }
      
      .anticon {
        transform: scale(1.1) rotate(5deg);
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2));
        color: #0056CC;
        box-shadow: 0 2px 8px rgba(0, 122, 255, 0.3);
      }
      
      span {
        color: #007AFF;
      }
    }
    
    &:active {
      transform: translateX(2px) scale(0.98);
      box-shadow: 0 2px 6px rgba(0, 122, 255, 0.2);
    }
  }
  
  .ant-menu-item-divider {
    margin: 6px 12px;
    background: #f0f0f0;
    height: 1px;
  }
  
  /* 菜单项出现动画 */
  .ant-menu-item:nth-child(1) {
    animation: menu-item-appear 0.4s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    animation-delay: 0.05s;
    opacity: 0;
  }
  
  .ant-menu-item:nth-child(2) {
    animation: menu-item-appear 0.4s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    animation-delay: 0.1s;
    opacity: 0;
  }
  
  .ant-menu-item:nth-child(3) {
    animation: menu-item-appear 0.4s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    animation-delay: 0.15s;
    opacity: 0;
  }
  
  .ant-menu-item:nth-child(5) {
    animation: menu-item-appear 0.4s cubic-bezier(0.25, 0.1, 0.25, 1) forwards;
    animation-delay: 0.2s;
    opacity: 0;
  }
}

.service-content {
  flex: 1;
  overflow: visible !important;
  background: #f5f6fa;
  max-width: calc(100% - 240px);
  width: calc(100% - 240px);
  position: relative;
  transition: width 0.3s ease, max-width 0.3s ease;
  
  /* 折叠时调整右侧内容区域 */
  .collapsed + & {
    max-width: calc(100% - 64px);
    width: calc(100% - 64px);
  }
  
  /* 隐藏所有滚动条 */
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

/* 重写所有内部元素的滚动行为 */
/deep/ .ant-tabs-content, 
/deep/ .ant-tabs-tabpane, 
/deep/ .service-detail, 
/deep/ .service-content, 
/deep/ .example-page, 
/deep/ .service-setting,
/deep/ .overview-page {
  overflow: visible !important;
  
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    display: none !important;
  }
  
  scrollbar-width: none !important;
  -ms-overflow-style: none !important;
}

/* 侧边栏控制样式 */
.sidebar-controls {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-left: 8px;
}

.sidebar-collapse-btn {
  width: 32px;
  height: 32px;
  border-radius: 12px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.9), rgba(248, 249, 250, 0.9));
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  color: #1D1D1F;
  border: 1px solid rgba(0, 0, 0, 0.08);
  position: relative;
  overflow: hidden;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08), 0 1px 2px rgba(0, 0, 0, 0.04);
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.05), rgba(88, 86, 214, 0.05));
    opacity: 0;
    transition: opacity 0.3s ease;
    border-radius: 12px;
  }
  
  &:hover {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
    transform: scale(1.05) translateY(-1px);
    box-shadow: 0 4px 16px rgba(0, 122, 255, 0.15), 0 2px 4px rgba(0, 0, 0, 0.08);
    border-color: rgba(0, 122, 255, 0.2);
    color: #007AFF;
    
    &::before {
      opacity: 1;
    }
  }
  
  &:active {
    transform: scale(0.95);
    box-shadow: 0 1px 4px rgba(0, 122, 255, 0.2);
  }
  
  .anticon {
    font-size: 16px;
    transition: all 0.3s ease;
    position: relative;
    z-index: 2;
  }
}

/* 苹果风格服务提示样式 */
:deep(.service-popover) {
  .ant-popover-inner-content {
    padding: 0;
  }
  
  .ant-popover-inner {
    border-radius: 24px;
    box-shadow: 0 32px 64px rgba(0, 0, 0, 0.12), 
                0 16px 32px rgba(0, 0, 0, 0.08),
                0 8px 16px rgba(0, 0, 0, 0.04),
                0 2px 8px rgba(0, 0, 0, 0.02);
    overflow: hidden;
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(40px) saturate(180%);
    -webkit-backdrop-filter: blur(40px) saturate(180%);
    border: 1px solid rgba(255, 255, 255, 0.4);
    transform: scale(0.95);
    opacity: 0;
    transition: all 0.4s cubic-bezier(0.25, 0.1, 0.25, 1);
  }
  
  &.ant-popover-open .ant-popover-inner {
    transform: scale(1);
    opacity: 1;
  }
  
  .ant-popover-arrow {
    display: none;
  }
}

.service-popover-content {
  width: 280px;
  background: transparent;
  border-radius: 8px;
  overflow: hidden;
}

.service-popover-header {
  display: flex;
  padding: 16px 20px;
  align-items: center;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.03), rgba(88, 86, 214, 0.03));
  border-bottom: 1px solid rgba(232, 232, 232, 0.6);
}

.service-popover-icon {
  flex-shrink: 0;
  width: 44px;
  height: 44px;
  border-radius: 12px;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 16px;
  box-shadow: 0 4px 12px rgba(0, 122, 255, 0.15);
  
  .svg-icon {
    font-size: 24px;
    color: #007AFF;
  }
}

.service-popover-title {
  flex: 1;
}

.service-popover-name {
  font-size: 18px;
  font-weight: 600;
  color: #1D1D1F;
  margin-bottom: 4px;
  letter-spacing: -0.3px;
}

.service-popover-version {
  font-size: 13px;
  color: #8E8E93;
  font-weight: 500;
}

.service-popover-info {
  padding: 16px 20px;
  
  .service-popover-status, 
  .service-popover-alerts,
  .service-popover-config {
    margin-bottom: 14px;
    display: flex;
    align-items: center;
    padding: 8px 12px;
    border-radius: 10px;
    background: rgba(248, 249, 250, 0.8);
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(0, 122, 255, 0.05);
      transform: translateY(-1px);
    }
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .info-label {
    font-size: 14px;
    color: #6D6D70;
    margin-right: 12px;
    width: 80px;
    flex-shrink: 0;
    font-weight: 500;
  }
  
  .info-value {
    font-size: 14px;
    color: #1D1D1F;
    font-weight: 500;
    
    &.success {
      color: #34C759;
    }
    
    &.warning {
      color: #FF9500;
    }
    
    &.error {
      color: #FF3B30;
    }
  }
  
  .info-value-with-action {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex: 1;
    
    .action-button {
      padding: 4px 12px;
      height: 28px;
      font-size: 12px;
      line-height: 20px;
      border-radius: 14px;
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
      color: #007AFF;
      transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
      font-weight: 500;
      border: 1px solid rgba(0, 122, 255, 0.2);
      
      &:hover {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.15), rgba(88, 86, 214, 0.15));
        transform: scale(1.05);
        box-shadow: 0 2px 8px rgba(0, 122, 255, 0.2);
      }
    }
  }
}

.service-popover-actions {
  padding: 16px 20px;
  background: linear-gradient(135deg, rgba(248, 249, 250, 0.8), rgba(255, 255, 255, 0.8));
  border-top: 1px solid rgba(240, 240, 240, 0.6);
  display: flex;
  flex-direction: column;
  gap: 10px;
  
  .button-row {
    display: flex;
    gap: 10px;
    width: 100%;
    
    .ant-btn {
      flex: 1;
      font-size: 13px;
      height: 36px;
      padding: 0 12px;
      transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
      border-radius: 12px;
      box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
      font-weight: 500;
      border: 1px solid rgba(0, 0, 0, 0.1);
      
      &.ant-btn-primary {
        background: linear-gradient(135deg, #007AFF, #5856D6);
        border-color: transparent;
        color: white;
        
        &:hover {
          background: linear-gradient(135deg, #0056CC, #4A44B8);
          transform: translateY(-2px) scale(1.02);
          box-shadow: 0 6px 16px rgba(0, 122, 255, 0.3);
        }
      }
      
      &.ant-btn-default {
        background: rgba(255, 255, 255, 0.9);
        color: #1D1D1F;
        
        &:hover {
          background: rgba(0, 122, 255, 0.05);
          color: #007AFF;
          border-color: rgba(0, 122, 255, 0.3);
          transform: translateY(-2px) scale(1.02);
          box-shadow: 0 6px 16px rgba(0, 122, 255, 0.15);
        }
      }
      
      &.ant-btn-danger {
        background: linear-gradient(135deg, #FF3B30, #FF2D55);
        border-color: transparent;
        color: white;
        
        &:hover {
          background: linear-gradient(135deg, #D70015, #E6002D);
          transform: translateY(-2px) scale(1.02);
          box-shadow: 0 6px 16px rgba(255, 59, 48, 0.3);
        }
      }
      
      &:active {
        transform: translateY(0) scale(0.98);
      }
      
      .anticon {
        font-size: 14px;
        margin-right: 6px;
      }
    }
  }
}

/* 添加动画和过渡效果 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.fade-enter, .fade-leave-to {
  opacity: 0;
  transform: translateY(-10px);
}

.scale-enter-active, .scale-leave-active {
  transition: opacity 0.2s, transform 0.2s;
}

.scale-enter, .scale-leave-to {
  opacity: 0;
  transform: scale(0.9);
}

/* 按钮悬停动画 */
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(0, 118, 206, 0.4);
  }
  70% {
    box-shadow: 0 0 0 6px rgba(0, 118, 206, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(0, 118, 206, 0);
  }
}

/* 现代化的按钮悬停效果 */
.modern-action-btn:hover {
  animation: pulse 1.5s infinite;
}

/* 现代化下拉菜单容器 */
:deep(.modern-dropdown) {
  .ant-dropdown-menu {
    border-radius: 24px;
    box-shadow: 0 20px 40px rgba(0, 0, 0, 0.12), 
                0 8px 16px rgba(0, 0, 0, 0.08),
                0 4px 8px rgba(0, 0, 0, 0.06),
                0 1px 2px rgba(0, 0, 0, 0.04);
    padding: 20px 16px;
    animation: dropdown-smooth-appear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(30px);
    -webkit-backdrop-filter: blur(30px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    overflow: visible;
    transform-origin: top right !important;
    min-width: 200px;
    z-index: 1500 !important;
    position: relative;
    margin-top: 10px;
  }
  
  .ant-menu-item {
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
    
    &:hover {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
      transform: translateX(4px) translateY(-1px) scale(1.01);
      box-shadow: 0 6px 20px rgba(0, 122, 255, 0.15), 0 3px 10px rgba(0, 122, 255, 0.08);
      
      &::before {
        width: 5px;
      }
    }
    
    &:active {
      transform: translateX(2px) scale(0.98);
      box-shadow: 0 2px 8px rgba(0, 122, 255, 0.1);
    }
    
    .anticon {
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
    
    span {
      color: #1D1D1F;
      font-weight: 500;
      font-size: 14px;
      transition: color 0.3s ease;
    }
    
    &:hover {
      .anticon {
        background: linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2));
        color: #0056CC;
        transform: scale(1.1);
      }
      
      span {
        color: #007AFF;
      }
    }
  }
}

@keyframes dropdown-fade-in {
  0% {
    opacity: 0;
    transform: translateY(12px) scale(0.95) rotateX(-10deg);
    filter: blur(4px);
  }
  50% {
    opacity: 0.8;
    transform: translateY(4px) scale(0.98) rotateX(-2deg);
    filter: blur(1px);
  }
  100% {
    opacity: 1;
    transform: translateY(0) scale(1) rotateX(0deg);
    filter: blur(0);
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

@keyframes menu-item-appear {
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

/* 增强现代感的额外样式 */
.service-item {
  transition: all 0.3s cubic-bezier(0.4, 0, 0.2, 1);
  
  &:hover {
    transform: translateX(2px);
  }
  
  &.active {
    box-shadow: 0 2px 8px rgba(0, 118, 206, 0.15);
  }
}

/* 增强按钮的现代感 */
.modern-action-btn {
  position: relative;
  overflow: hidden;
  
  &::after {
    content: '';
    position: absolute;
    top: 50%;
    left: 50%;
    width: 5px;
    height: 5px;
    background: rgba(0, 118, 206, 0.3);
    opacity: 0;
    border-radius: 100%;
    transform: scale(1, 1) translate(-50%, -50%);
    transform-origin: 50% 50%;
  }
  
  &:hover::after {
    animation: ripple 1s ease-out;
  }
}

@keyframes ripple {
  0% {
    transform: scale(0, 0);
    opacity: 0.5;
  }
  20% {
    transform: scale(25, 25);
    opacity: 0.3;
  }
  100% {
    opacity: 0;
    transform: scale(40, 40);
  }
}

/* 危险操作按钮样式 */
:deep(.modern-dropdown-menu) .ant-menu-item.danger-item {
  color: #1D1D1F;
  
  .anticon {
    background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
    color: #FF3B30;
  }
  
  span {
    color: #1D1D1F;
  }
  
  &:hover {
    background: linear-gradient(135deg, rgba(255, 59, 48, 0.08), rgba(255, 45, 85, 0.08));
    color: #FF3B30;
    transform: translateX(3px) translateY(-1px);
    box-shadow: 0 4px 12px rgba(255, 59, 48, 0.2);
    
    &::before {
      background: linear-gradient(135deg, #FF3B30, #FF2D55);
      width: 4px;
    }
    
    &::after {
      opacity: 1;
      background: linear-gradient(135deg, rgba(255, 59, 48, 0.05), rgba(255, 45, 85, 0.05));
    }
    
    .anticon {
      color: #D70015;
      background: linear-gradient(135deg, rgba(255, 59, 48, 0.2), rgba(255, 45, 85, 0.2));
      transform: scale(1.1) rotate(5deg);
      box-shadow: 0 2px 8px rgba(255, 59, 48, 0.3);
    }
    
    span {
      color: #FF3B30;
    }
  }
  
  &:active {
    transform: translateX(2px) scale(0.98);
    box-shadow: 0 2px 6px rgba(255, 59, 48, 0.25);
  }
}

:deep(.service-action-dropdown) {
  z-index: 1500 !important; /* 确保服务操作下拉菜单显示在最上层 */
  position: fixed !important;
}

:deep(.modern-dropdown) {
  .ant-dropdown-menu {
    border-radius: 12px;
    box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12), 
                0 6px 16px rgba(0, 0, 0, 0.08),
                0 2px 6px rgba(0, 0, 0, 0.06);
    padding: 8px;
    animation: dropdown-smooth-appear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
    background: rgba(255, 255, 255, 0.98);
    backdrop-filter: blur(30px);
    -webkit-backdrop-filter: blur(30px);
    border: 1px solid rgba(255, 255, 255, 0.6);
    overflow: visible;
    transform-origin: top right !important;
    min-width: 140px;
    z-index: 1500 !important;
    position: relative;
    margin-top: 4px;
  }
}

:deep(.ant-dropdown) {
  z-index: 1500 !important;
}

:deep(.service-action-dropdown) {
  z-index: 1500 !important; /* 确保服务操作下拉菜单显示在最上层 */
  position: absolute !important;
  top: 40px !important;
}

.service-action-menu {
  position: fixed;
  top: 50%;
  left: 50%;
  transform: translate(-50%, -50%);
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  padding: 16px;
  border-radius: 16px;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12), 
              0 6px 16px rgba(0, 0, 0, 0.08),
              0 2px 6px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(255, 255, 255, 0.6);
  z-index: 1500;
  min-width: 180px;
  animation: menu-appear 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.menu-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  margin-bottom: 12px;
  padding-bottom: 8px;
  border-bottom: 1px solid rgba(240, 240, 240, 0.8);
  
  span {
    font-size: 16px;
    font-weight: 600;
    color: #1D1D1F;
    letter-spacing: -0.3px;
  }
  
  .anticon {
    font-size: 16px;
    color: #8E8E93;
    cursor: pointer;
    padding: 4px;
    border-radius: 50%;
    transition: all 0.3s ease;
    
    &:hover {
      background: rgba(0, 0, 0, 0.05);
      color: #1D1D1F;
      transform: scale(1.1);
    }
  }
}

.menu-items {
  display: flex;
  flex-direction: column;
  gap: 4px;
}

.menu-item {
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 12px 16px;
  border-radius: 12px;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  position: relative;
  
  &::before {
    content: '';
    position: absolute;
    left: 0;
    top: 0;
    bottom: 0;
    width: 0;
    background: linear-gradient(135deg, #007AFF, #5856D6);
    border-radius: 12px 0 0 12px;
    transition: width 0.3s cubic-bezier(0.25, 0.1, 0.25, 1);
  }
  
  .anticon {
    font-size: 16px;
    color: #007AFF;
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.1), rgba(88, 86, 214, 0.1));
    width: 32px;
    height: 32px;
    border-radius: 10px;
    display: flex;
    align-items: center;
    justify-content: center;
    transition: all 0.3s ease;
    position: relative;
    z-index: 1;
  }
  
  span {
    font-size: 14px;
    font-weight: 500;
    color: #1D1D1F;
    transition: color 0.3s ease;
    position: relative;
    z-index: 1;
  }
  
  &:hover {
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.05), rgba(88, 86, 214, 0.05));
    transform: translateX(5px);
    box-shadow: 0 4px 12px rgba(0, 122, 255, 0.1);
    
    &::before {
      width: 4px;
    }
    
    .anticon {
      background: linear-gradient(135deg, rgba(0, 122, 255, 0.2), rgba(88, 86, 214, 0.2));
      transform: scale(1.1);
      color: #0056CC;
    }
    
    span {
      color: #007AFF;
    }
  }
  
  &.danger {
    .anticon {
      color: #FF3B30;
      background: linear-gradient(135deg, rgba(255, 59, 48, 0.1), rgba(255, 45, 85, 0.1));
    }
    
    &:hover {
      background: linear-gradient(135deg, rgba(255, 59, 48, 0.05), rgba(255, 45, 85, 0.05));
      
      &::before {
        background: linear-gradient(135deg, #FF3B30, #FF2D55);
      }
      
      .anticon {
        background: linear-gradient(135deg, rgba(255, 59, 48, 0.2), rgba(255, 45, 85, 0.2));
        color: #D70015;
      }
      
      span {
        color: #FF3B30;
      }
    }
  }
}

.menu-divider {
  height: 1px;
  background: linear-gradient(90deg, transparent, rgba(0, 0, 0, 0.05), transparent);
  margin: 4px 8px;
}

@keyframes menu-appear {
  0% {
    opacity: 0;
    transform: translate(-50%, -48%) scale(0.95);
    filter: blur(4px);
  }
  70% {
    opacity: 0.8;
    transform: translate(-50%, -51%) scale(1.02);
    filter: blur(1px);
  }
  100% {
    opacity: 1;
    transform: translate(-50%, -50%) scale(1);
    filter: blur(0);
  }
}
</style>