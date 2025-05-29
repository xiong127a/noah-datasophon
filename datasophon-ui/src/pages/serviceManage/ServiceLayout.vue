<template>
  <div class="cdh-service-page">
    <!-- 左侧服务列表 - CDH风格 -->
    <div class="service-sidebar cdh-style" :class="{'collapsed': sidebarCollapsed}">
      <!-- 头部标题区域 -->
      <div class="service-title">
        <span v-if="!sidebarCollapsed">{{ clusterInfo }}</span>
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
          <a-icon type="appstore" class="group-icon" />
          <span>Core Service</span>
          <a-icon :type="coreGroupCollapsed ? 'right' : 'down'" class="collapse-icon" />
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
            :mouseLeaveDelay="0.5"
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
                    <svg-icon :icon-class="service.icon || 'service-default'" />
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
                
                <div class="service-popover-actions">
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
            <div class="expand-icon" @click="toggleServiceMenu(service, $event)">
              <div class="cdh-dropdown-btn">
                <a-icon type="caret-down" />
              </div>
              <!-- 服务操作菜单 -->
              <a-dropdown :visible="service.menuVisible" placement="bottomRight" @visibleChange="(visible) => handleVisibleChange(visible, service)">
                <a class="ant-dropdown-link"></a>
                <a-menu slot="overlay">
                  <a-menu-item key="start" @click="handleServiceAction({key: 'start'}, service, $event)">启动</a-menu-item>
                  <a-menu-item key="stop" @click="handleServiceAction({key: 'stop'}, service, $event)">停止</a-menu-item>
                  <a-menu-item key="restart" @click="handleServiceAction({key: 'restart'}, service, $event)">重启</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" @click="handleServiceAction({key: 'del'}, service, $event)">删除</a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
          </a-popover>
        </div>
      </div>
      
      <!-- 管理服务组 -->
      <div class="service-group management-group" v-if="managementServices.length > 0">
        <div class="group-title" @click="toggleGroupCollapse('management')" :title="sidebarCollapsed ? 'Management Service' : ''">
          <a-icon type="control" class="group-icon" />
          <span>Management Service</span>
          <a-icon :type="managementGroupCollapsed ? 'right' : 'down'" class="collapse-icon" />
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
            :mouseLeaveDelay="0.5"
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
                    <svg-icon :icon-class="service.icon || 'service-default'" />
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
                
                <div class="service-popover-actions">
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
            <div class="expand-icon" @click="toggleServiceMenu(service, $event)">
              <div class="cdh-dropdown-btn">
                <a-icon type="caret-down" />
              </div>
              <!-- 服务操作菜单 -->
              <a-dropdown :visible="service.menuVisible" placement="bottomRight" @visibleChange="(visible) => handleVisibleChange(visible, service)">
                <a class="ant-dropdown-link"></a>
                <a-menu slot="overlay">
                  <a-menu-item key="start" @click="handleServiceAction({key: 'start'}, service, $event)">启动</a-menu-item>
                  <a-menu-item key="stop" @click="handleServiceAction({key: 'stop'}, service, $event)">停止</a-menu-item>
                  <a-menu-item key="restart" @click="handleServiceAction({key: 'restart'}, service, $event)">重启</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" @click="handleServiceAction({key: 'del'}, service, $event)">删除</a-menu-item>
                </a-menu>
              </a-dropdown>
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
      managementServiceNames: ['ALERTMANAGER', 'PROMETHEUS', 'GRAFANA', 'PUSHGATEWAY'],
      coreGroupCollapsed: false,
      managementGroupCollapsed: false,
      sidebarCollapsed: false,
      popoverTimeoutMap: {} // 用于存储popover延迟隐藏的定时器
    };
  },
  computed: {
    ...mapState('setting', ['menuData', 'alarmManageVisible', 'clusterId']),
    currentServiceId() {
      return this.$route.params.serviceId;
    },
    clusterInfo() {
      if (this.clusterData) {
        return `${this.clusterData.clusterName} (${this.clusterData.clusterFrame}, ${this.clusterData.depType.toLowerCase()})`;
      }
      return '加载中...';
    },
    coreServices() {
      return this.menuList.filter(service => {
        // 使用serviceName字段进行过滤，而不是name字段
        const serviceNameForFilter = service.serviceName || service.name;
        return !this.managementServiceNames.includes(serviceNameForFilter.toUpperCase());
      });
    },
    managementServices() {
      return this.menuList.filter(service => {
        // 使用serviceName字段进行过滤，而不是name字段
        const serviceNameForFilter = service.serviceName || service.name;
        return this.managementServiceNames.includes(serviceNameForFilter.toUpperCase());
      });
    }
  },
  mounted() {
    this.getClusterInfo();
    this.loadMenuData();
    
    // 从 localStorage 加载折叠状态
    const savedCollapsedState = localStorage.getItem('sidebarCollapsed');
    if (savedCollapsedState !== null) {
      this.sidebarCollapsed = savedCollapsedState === 'true';
    }
    
    // 添加点击外部关闭菜单的事件监听
    document.addEventListener('click', this.closeAllMenus);
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('click', this.closeAllMenus);
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    
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
      // 由于API可能无法在开发环境直接访问，我们模拟一个集群数据
      // 在生产环境可以替换为实际API调用
      // this.$axios.get('/ddh/api/cluster/runningClusterList')
      
      // 模拟API返回的数据
      const mockClusterData = {
        id: 1,
        clusterName: "bdp",
        clusterCode: "bdp",
        clusterFrame: "DDP-1.2.1",
        depType: "PVM",
        clusterState: "正在运行"
      };
      
      this.clusterData = mockClusterData;
      
      /* 实际环境使用下面的代码
      this.$axiosGet('/ddh/api/cluster/runningClusterList').then(res => {
        if (res.code === 200 && res.data && res.data.length > 0) {
          this.clusterData = res.data[0]; // 获取第一个集群数据
        }
      }).catch(error => {
        console.error('获取集群数据失败:', error);
      });
      */
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
          // 添加控制台日志以便调试
          console.log('服务数据:', item);
          
          // 使用label字段作为显示名称，使用name字段作为serviceName
          const displayName = item.label || item.name;
          const serviceName = item.name;
          
          // 将服务名称转为小写作为图标名称
          const iconName = serviceName ? serviceName.toLowerCase() : 'service-default';
          console.log('使用图标名称:', iconName);
          
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
            // 添加日志，显示服务列表数据结构
            console.log('服务列表数据结构:', JSON.stringify(serviceList[0], null, 2));
            console.log('服务列表是否包含label字段:', serviceList.some(item => item.label));
            
            this.menuList = serviceList.map((item, index) => {
              // 记录每个服务的数据
              console.log(`服务${index+1}:`, {
                id: item.id,
                serviceName: item.serviceName,
                label: item.label
              });
              
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
      this.menuList = [
        { id: '1', name: 'HDFS 分布式文件系统', serviceName: 'HDFS', icon: 'hdfs', path: '/service-manage/service-list/1', serviceId: '1', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '2', name: 'YARN 资源调度系统', serviceName: 'YARN', icon: 'yarn', path: '/service-manage/service-list/2', serviceId: '2', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '3', name: 'HBASE 分布式数据库', serviceName: 'HBASE', icon: 'hbase', path: '/service-manage/service-list/3', serviceId: '3', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '4', name: 'HIVE 数据仓库', serviceName: 'HIVE', icon: 'hive', path: '/service-manage/service-list/4', serviceId: '4', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '5', name: 'ZOOKEEPER 分布式协调服务', serviceName: 'ZOOKEEPER', icon: 'zookeeper', path: '/service-manage/service-list/5', serviceId: '5', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '6', name: 'SPARK 分布式计算引擎', serviceName: 'SPARK', icon: 'spark', path: '/service-manage/service-list/6', serviceId: '6', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '7', name: 'ALERTMANAGER 告警管理', serviceName: 'ALERTMANAGER', icon: 'alertmanager', path: '/service-manage/service-list/7', serviceId: '7', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '8', name: 'PROMETHEUS 监控系统', serviceName: 'PROMETHEUS', icon: 'prometheus', path: '/service-manage/service-list/8', serviceId: '8', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '9', name: 'GRAFANA 可视化平台', serviceName: 'GRAFANA', icon: 'grafana', path: '/service-manage/service-list/9', serviceId: '9', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
        { id: '10', name: 'PUSHGATEWAY 数据推送', serviceName: 'PUSHGATEWAY', icon: 'pushgateway', path: '/service-manage/service-list/10', serviceId: '10', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false, popoverVisible: false, popoverInContent: false },
      ];
      
      // 打印默认图标配置
      console.log('默认菜单数据:', this.menuList);
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
        console.log('选择服务菜单 - 设置serviceId:', menu.serviceId, '服务名称:', menu.name);
        
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
      service.menuVisible = !service.menuVisible;
    },
    
    // 处理服务操作菜单的可见性变化
    handleVisibleChange(visible, service) {
      if (!visible) {
        service.menuVisible = false;
      }
    },
    
    // 处理服务项点击事件
    handleServiceItemClick(service) {
      // 保持折叠状态
      const wasSidebarCollapsed = this.sidebarCollapsed;
      
      // 调用原有的服务选择方法
      this.selectMenu(service);
      
      // 恢复原有的折叠状态
      this.$nextTick(() => {
        this.sidebarCollapsed = wasSidebarCollapsed;
      });
    },
    
    // 处理服务项鼠标进入事件
    handleServiceMouseEnter(service) {
      // 清除已存在的定时器
      if (this.popoverTimeoutMap[service.id]) {
        clearTimeout(this.popoverTimeoutMap[service.id]);
        delete this.popoverTimeoutMap[service.id];
      }
      
      // 设置当前服务的popoverVisible为true
      service.popoverVisible = true;
    },
    
    // 处理服务项鼠标离开事件
    handleServiceMouseLeave(service) {
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
      if (!visible && !service.popoverInContent) {
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
      // 标记鼠标离开内容区域
      service.popoverInContent = false;
      
      // 延迟300ms关闭，避免鼠标在popover和触发元素之间移动时闪烁
      this.popoverTimeoutMap[service.id] = setTimeout(() => {
        service.popoverVisible = false;
        delete this.popoverTimeoutMap[service.id];
      }, 300);
    }
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
  width: 240px; /* 增加宽度以显示完整服务名称 */
  background: #fff;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
  flex-shrink: 0;
  transition: width 0.3s ease;
  
  /* 折叠状态 */
  &.collapsed {
    width: 64px;
    
    .service-title {
      padding: 12px 8px;
      justify-content: center;
    }
    
    .group-title {
      padding: 10px 8px;
      justify-content: center;
      
      span, .collapse-icon {
        display: none;
      }
      
      .group-icon {
        margin-right: 0;
        font-size: 18px; /* 折叠状态下稍微增大图标尺寸 */
      }
    }
    
    .service-item {
      padding: 6px 8px;
      justify-content: center;
      
      .status-indicator, 
      .alert-indicators, 
      .expand-icon {
        display: none !important; /* 强制隐藏这些元素 */
      }
      
      &.active {
        padding-left: 5px;
      }
      
      .service-name-container {
        justify-content: center;
        margin-right: 0; /* 移除右侧边距 */
      }
      
      .service-icon {
        margin-right: 0; /* 移除图标右侧边距 */
      }
    }
  }
  
  /* 允许侧边栏滚动但使用玻璃效果滚动条 */
  &::-webkit-scrollbar {
    width: 8px;
    height: 8px;
  }
  
  &::-webkit-scrollbar-track {
    background: transparent;
  }
  
  &::-webkit-scrollbar-thumb {
    background: rgba(180, 180, 180, 0.3);
    border-radius: 4px;
    backdrop-filter: blur(10px);
  }
  
  &::-webkit-scrollbar-thumb:hover {
    background: rgba(180, 180, 180, 0.5);
  }
  
  /* Firefox兼容 */
  scrollbar-width: thin;
  scrollbar-color: rgba(180, 180, 180, 0.3) transparent;
  
  &.cdh-style {
    background: #f5f7f8;
    border-right: 1px solid #dde4e5;
    
    .service-title {
      padding: 12px 16px;
      font-size: 14px;
      font-weight: 600;
      border-bottom: 1px solid #dde4e5;
      position: sticky;
      top: 0;
      z-index: 10;
      background: #f5f7f8;
      display: flex;
      justify-content: space-between;
      align-items: center;
      color: #0076ce; /* 使用CDH风格的蓝色 */
      
      .service-more {
        margin-left: auto;
      }
    }
    
    .group-title {
      padding: 10px 16px;
      font-size: 13px;
      font-weight: 500;
      background-color: #e8eef0;
      color: #333;
      border-bottom: 1px solid #dde4e5;
      border-top: 1px solid #dde4e5;
      cursor: pointer;
      display: flex;
      justify-content: space-between;
      align-items: center;
      
      &:hover {
        background-color: #dde4e5;
      }
      
      .group-icon {
        font-size: 16px;
        color: #0076ce;
        margin-right: 8px;
      }
      
      .collapse-icon {
        font-size: 12px;
        color: #666;
      }
    }
    
    .service-list {
      padding: 0;
    }
    
    .service-item {
      display: flex;
      align-items: center;
      padding: 6px 12px 6px 16px;
      cursor: pointer;
      transition: background-color 0.2s;
      position: relative;
      height: 36px;
      border-bottom: 1px solid #e8eef0;
      
      &:hover {
        background-color: #edf2f4;
      }
      
      &.active {
        background-color: #d7e8f7;
        border-left: 3px solid #0076ce; /* CDH风格的蓝色边框 */
        padding-left: 13px; /* 3px border compensated */
      }
      
      .status-indicator {
        margin-right: 8px;
        width: 16px;
        text-align: center;
        
        .status-icon {
          font-size: 14px;
          
          &.success {
            color: #52c41a;
          }
          
          &.warning {
            color: #f0a400; /* CDH风格的警告黄色 */
          }
          
          &.error {
            color: #db1d00; /* CDH风格的错误红色 */
          }
          
          &.empty {
            width: 14px;
            height: 14px;
            display: inline-block;
          }
        }
      }
      
      .service-name-container {
        flex: 1;
        display: flex;
        align-items: center;
        
        .service-icon {
          margin-right: 8px;
          width: 20px;
          height: 20px;
          display: flex;
          align-items: center;
          justify-content: center;
          
          .svg-icon {
            font-size: 20px;
            color: #0076ce;
          }
        }
        
        .service-name {
          flex: 1;
          font-size: 13px;
          color: #0076ce; /* 服务名称使用CDH风格的蓝色 */
          overflow: hidden;
          text-overflow: ellipsis;
          white-space: nowrap;
          padding-right: 8px;
        }
      }
      
      .alert-indicators {
        display: flex;
        align-items: center;
        margin-right: 8px;
        
        .alert-badge {
          display: flex;
          align-items: center;
          margin-right: 4px;
          cursor: pointer;
          line-height: 1;
          
          .error-color {
            color: #db1d00; /* CDH风格的错误红色 */
            font-size: 14px;
          }
          
          .warning-color {
            color: #f0a400; /* CDH风格的警告黄色 */
            font-size: 14px;
          }
          
          .alert-count {
            font-size: 14px;
            margin-left: 2px;
            font-weight: 500;
            position: relative;
            top: 1px;
            
            &.warning-color {
              color: #f0a400; /* CDH风格的警告黄色 */
            }
            
            &.error-color {
              color: #db1d00; /* CDH风格的错误红色 */
            }
          }
        }
        
        .restart-icon {
          font-size: 14px;
          color: #f0a400; /* CDH风格的警告黄色 */
          cursor: pointer;
        }
      }
      
      .expand-icon {
        font-size: 12px;
        color: #999;
        margin-left: 6px;
        position: relative;
        cursor: pointer;
        display: flex;
        align-items: center;
        
        .cdh-dropdown-btn {
          width: 20px;
          height: 20px;
          border-radius: 2px;
          display: flex;
          align-items: center;
          justify-content: center;
          background-color: transparent;
          border: none;
          transition: all 0.2s ease;
          opacity: 0; /* 默认隐藏 */
          
          &:hover {
            background-color: transparent;
            border-color: transparent;
          }
          
          &:active {
            background-color: transparent;
            border-color: transparent;
            box-shadow: none;
          }
          
          .anticon {
            font-size: 11px;
            color: #0076ce; /* CDH风格的蓝色 */
          }
        }
        
        .ant-dropdown {
          min-width: 120px;
        }
      }
    }
    
    /* 当鼠标悬停在服务项上时显示下拉按钮 */
    .service-item:hover .expand-icon .cdh-dropdown-btn {
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

:deep(.ant-dropdown-menu) {
  padding: 4px 0;
  border-radius: 2px;
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
}

.sidebar-collapse-btn {
  width: 24px;
  height: 24px;
  border-radius: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  background-color: #f0f6ff;
  cursor: pointer;
  transition: all 0.2s ease;
  color: #0076ce;
  
  &:hover {
    background-color: #d7e8f7;
  }
  
  .anticon {
    font-size: 16px;
  }
}

/* 服务提示样式 */
:deep(.service-popover) {
  .ant-popover-inner-content {
    padding: 0;
  }
  
  .ant-popover-inner {
    border-radius: 8px;
    box-shadow: 0 6px 16px -8px rgba(0, 0, 0, 0.08), 
                0 9px 28px 0 rgba(0, 0, 0, 0.05), 
                0 12px 48px 16px rgba(0, 0, 0, 0.03);
    overflow: hidden;
  }
}

.service-popover-content {
  width: 260px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
}

.service-popover-header {
  display: flex;
  padding: 12px;
  align-items: center;
  background-color: #f5f7fa;
  border-bottom: 1px solid #e8e8e8;
}

.service-popover-icon {
  flex-shrink: 0;
  width: 36px;
  height: 36px;
  border-radius: 8px;
  background-color: #e6effc;
  display: flex;
  align-items: center;
  justify-content: center;
  margin-right: 12px;
  
  .svg-icon {
    font-size: 24px;
    color: #0076ce;
  }
}

.service-popover-title {
  flex: 1;
}

.service-popover-name {
  font-size: 16px;
  font-weight: 500;
  color: #222b45;
  margin-bottom: 2px;
}

.service-popover-version {
  font-size: 12px;
  color: #8f959e;
}

.service-popover-info {
  padding: 12px;
  
  .service-popover-status, 
  .service-popover-alerts,
  .service-popover-config {
    margin-bottom: 10px;
    display: flex;
    align-items: center;
    
    &:last-child {
      margin-bottom: 0;
    }
  }
  
  .info-label {
    font-size: 13px;
    color: #5f6369;
    margin-right: 8px;
    width: 70px;
    flex-shrink: 0;
  }
  
  .info-value {
    font-size: 13px;
    color: #222b45;
    
    &.success {
      color: #52c41a;
    }
    
    &.warning {
      color: #f0a400;
    }
    
    &.error {
      color: #db1d00;
    }
  }
  
  .info-value-with-action {
    display: flex;
    align-items: center;
    justify-content: space-between;
    flex: 1;
    
    .action-button {
      padding: 0 8px;
      height: 24px;
      font-size: 12px;
      line-height: 24px;
      border-radius: 12px;
      background-color: #f0f6ff;
      color: #0076ce;
      transition: all 0.3s ease;
      
      &:hover {
        background-color: #d7e8f7;
        color: #0057a6;
      }
    }
  }
}

.service-popover-actions {
  padding: 12px;
  background-color: #fafafa;
  border-top: 1px solid #f0f0f0;
  display: flex;
  flex-direction: column;
  gap: 8px;
  
  .button-row {
    display: flex;
    gap: 8px;
    width: 100%;
    
    .ant-btn {
      flex: 1;
      font-size: 12px;
      height: 28px;
      padding: 0 8px;
      transition: all 0.3s ease;
      border-radius: 4px;
      box-shadow: 0 2px 4px rgba(0, 0, 0, 0.05);
      
      &:hover {
        transform: translateY(-1px);
        box-shadow: 0 2px 8px rgba(0, 0, 0, 0.1);
      }
      
      &:active {
        transform: translateY(0);
      }
      
      .anticon {
        font-size: 12px;
        margin-right: 4px;
      }
    }
  }
}
</style> 