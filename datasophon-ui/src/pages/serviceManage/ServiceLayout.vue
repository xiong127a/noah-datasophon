<template>
  <div class="cdh-service-page">
    <!-- 左侧服务列表 - CDH风格 -->
    <div class="service-sidebar cdh-style">
      <!-- 头部标题区域 -->
      <div class="service-title">
        <span>{{ clusterInfo }}</span>
        <service-option class="service-more" />
      </div>
      
      <!-- 核心服务组 -->
      <div class="service-group">
        <div class="group-title" @click="toggleGroupCollapse('core')">
          <span>Core Service</span>
          <a-icon :type="coreGroupCollapsed ? 'right' : 'down'" class="collapse-icon" />
        </div>
        <div class="service-list" v-show="!coreGroupCollapsed">
          <div v-for="(service, index) in coreServices" :key="index" 
               class="service-item" 
               :class="{'active': isActiveService(service)}"
               @click="selectMenu(service)">
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
              <div class="service-name">
                {{ service.name }}
              </div>
            </div>
            
            <!-- 告警指示器 -->
            <div class="alert-indicators">
              <!-- 告警数量 -->
              <div v-if="service.alertNum > 0" class="alert-badge" @click.stop="showAlarm(service)">
                <a-icon type="exclamation-circle" theme="filled" :class="service.serviceStateCode === 4 ? 'error-color' : 'warning-color'" />
                <span :class="['alert-count', service.serviceStateCode === 4 ? 'error-color' : 'warning-color']">{{ service.alertNum }}</span>
              </div>
              
              <!-- 配置变更指示器 -->
              <a-icon 
                v-if="service.needRestart" 
                type="tool" 
                class="restart-icon"
                @click.stop="showConfigCompare(service)" 
              />
            </div>
            
            <!-- 展开按钮 -->
            <div class="expand-icon" @click.stop="toggleServiceMenu(service, $event)">
              <div class="cdh-dropdown-btn">
                <a-icon type="caret-down" />
              </div>
              <!-- 服务操作菜单 -->
              <a-dropdown :visible="service.menuVisible" placement="bottomRight" @visibleChange="(visible) => handleVisibleChange(visible, service)">
                <a class="ant-dropdown-link"></a>
                <a-menu slot="overlay">
                  <a-menu-item key="start" @click.stop="handleServiceAction({key: 'start'}, service)">启动</a-menu-item>
                  <a-menu-item key="stop" @click.stop="handleServiceAction({key: 'stop'}, service)">停止</a-menu-item>
                  <a-menu-item key="restart" @click.stop="handleServiceAction({key: 'restart'}, service)">重启</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" @click.stop="handleServiceAction({key: 'del'}, service)">删除</a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 管理服务组 -->
      <div class="service-group management-group" v-if="managementServices.length > 0">
        <div class="group-title" @click="toggleGroupCollapse('management')">
          <span>Management Service</span>
          <a-icon :type="managementGroupCollapsed ? 'right' : 'down'" class="collapse-icon" />
        </div>
        <div class="service-list" v-show="!managementGroupCollapsed">
          <div v-for="(service, index) in managementServices" :key="'mgmt-'+index" 
               class="service-item" 
               :class="{'active': isActiveService(service)}"
               @click="selectMenu(service)">
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
              <div class="service-name">
                {{ service.name }}
              </div>
            </div>
            
            <!-- 告警指示器 -->
            <div class="alert-indicators">
              <!-- 告警数量 -->
              <div v-if="service.alertNum > 0" class="alert-badge" @click.stop="showAlarm(service)">
                <a-icon type="exclamation-circle" theme="filled" :class="service.serviceStateCode === 4 ? 'error-color' : 'warning-color'" />
                <span :class="['alert-count', service.serviceStateCode === 4 ? 'error-color' : 'warning-color']">{{ service.alertNum }}</span>
              </div>
              
              <!-- 配置变更指示器 -->
              <a-icon 
                v-if="service.needRestart" 
                type="tool" 
                class="restart-icon"
                @click.stop="showConfigCompare(service)" 
              />
            </div>
            
            <!-- 展开按钮 -->
            <div class="expand-icon" @click.stop="toggleServiceMenu(service, $event)">
              <div class="cdh-dropdown-btn">
                <a-icon type="caret-down" />
              </div>
              <!-- 服务操作菜单 -->
              <a-dropdown :visible="service.menuVisible" placement="bottomRight" @visibleChange="(visible) => handleVisibleChange(visible, service)">
                <a class="ant-dropdown-link"></a>
                <a-menu slot="overlay">
                  <a-menu-item key="start" @click.stop="handleServiceAction({key: 'start'}, service)">启动</a-menu-item>
                  <a-menu-item key="stop" @click.stop="handleServiceAction({key: 'stop'}, service)">停止</a-menu-item>
                  <a-menu-item key="restart" @click.stop="handleServiceAction({key: 'restart'}, service)">重启</a-menu-item>
                  <a-menu-divider />
                  <a-menu-item key="delete" @click.stop="handleServiceAction({key: 'del'}, service)">删除</a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
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
      managementGroupCollapsed: false
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
      return this.menuList.filter(service => !this.managementServiceNames.includes(service.name.toUpperCase()));
    },
    managementServices() {
      return this.menuList.filter(service => this.managementServiceNames.includes(service.name.toUpperCase()));
    }
  },
  mounted() {
    this.getClusterInfo();
    this.loadMenuData();
    
    // 添加点击外部关闭菜单的事件监听
    document.addEventListener('click', this.closeAllMenus);
  },
  beforeDestroy() {
    // 移除事件监听
    document.removeEventListener('click', this.closeAllMenus);
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
    
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
          console.log('服务名称:', item.name, '图标名称:', (item.meta && item.meta.icon) || 'service-default');
          
          // 将服务名称转为小写作为图标名称
          const iconName = item.name ? item.name.toLowerCase() : 'service-default';
          console.log('使用图标名称:', iconName);
          
          return {
            id: String(index + 1),
            name: item.name,
            path: item.fullPath,
            icon: iconName, // 使用小写服务名称作为图标名
            serviceId: (item.meta && item.meta.params && item.meta.params.serviceId) || '',
            // 添加服务状态相关的属性
            serviceStateCode: item.meta && item.meta.obj ? item.meta.obj.serviceStateCode : 1,
            alertNum: item.meta && item.meta.obj ? item.meta.obj.alertNum : 0,
            needRestart: item.meta && item.meta.obj ? item.meta.obj.needRestart : false,
            rawData: item.meta && item.meta.obj ? item.meta.obj : {},
            menuVisible: false
          };
        });
      } else {
        // 尝试从直接保存的服务列表获取
        try {
          const serviceList = JSON.parse(localStorage.getItem('serviceList') || '[]');
          if (serviceList.length > 0) {
            this.menuList = serviceList.map((item, index) => {
              return {
                id: String(index + 1),
                name: item.serviceName,
                icon: item.serviceName.toLowerCase(),
                serviceId: item.id,
                path: `/service-manage/service-list/${item.id}`,
                // 添加服务状态相关的默认属性
                serviceStateCode: 1,
                alertNum: 0,
                needRestart: false,
                rawData: {},
                menuVisible: false
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
        { id: '1', name: 'HDFS', icon: 'hdfs', path: '/service-manage/service-list/1', serviceId: '1', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '2', name: 'YARN', icon: 'yarn', path: '/service-manage/service-list/2', serviceId: '2', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '3', name: 'HBASE', icon: 'hbase', path: '/service-manage/service-list/3', serviceId: '3', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '4', name: 'HIVE', icon: 'hive', path: '/service-manage/service-list/4', serviceId: '4', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '5', name: 'ZOOKEEPER', icon: 'zookeeper', path: '/service-manage/service-list/5', serviceId: '5', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '6', name: 'SPARK', icon: 'spark', path: '/service-manage/service-list/6', serviceId: '6', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '7', name: 'ALERTMANAGER', icon: 'alertmanager', path: '/service-manage/service-list/7', serviceId: '7', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '8', name: 'PROMETHEUS', icon: 'prometheus', path: '/service-manage/service-list/8', serviceId: '8', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '9', name: 'GRAFANA', icon: 'grafana', path: '/service-manage/service-list/9', serviceId: '9', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
        { id: '10', name: 'PUSHGATEWAY', icon: 'pushgateway', path: '/service-manage/service-list/10', serviceId: '10', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {}, menuVisible: false },
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
      // 导航到对应的服务详情页面
      if (menu.path) {
        this.$router.push(menu.path).catch(err => {
          if (err.name !== 'NavigationDuplicated') {
            throw err;
          }
        });
      } else if (menu.serviceId) {
        this.$router.push(`/service-manage/service-list/${menu.serviceId}`).catch(err => {
          if (err.name !== 'NavigationDuplicated') {
            throw err;
          }
        });
      }
    },
    
    // 显示告警详情
    showAlarm(service) {
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
    showConfigCompare(service) {
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
    handleServiceAction(action, service) {
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
      event.stopPropagation();
      service.menuVisible = !service.menuVisible;
    },
    
    // 处理服务操作菜单的可见性变化
    handleVisibleChange(visible, service) {
      if (!visible) {
        service.menuVisible = false;
      }
    }
  }
};
</script>

<style lang="less" scoped>
.cdh-service-page {
  display: flex;
  height: calc(100vh - 56px);
  background: #f5f6fa;
}

.service-sidebar {
  width: 240px; /* 增加宽度以显示完整服务名称 */
  background: #fff;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
  
  /* 完全隐藏滚动条 */
  &::-webkit-scrollbar {
    width: 0 !important;
    height: 0 !important;
    background-color: transparent !important;
    display: none !important;
  }
  
  &::-webkit-scrollbar-track {
    background-color: transparent !important;
    display: none !important;
  }
  
  &::-webkit-scrollbar-thumb {
    display: none !important;
  }
  
  &::-webkit-scrollbar-button {
    display: none !important;
  }
  
  /* Firefox兼容 */
  scrollbar-width: none !important;
  
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
          background-color: #f8f8f8;
          border: 1px solid #ccc;
          transition: all 0.2s ease;
          visibility: visible;
          
          &:hover {
            background-color: #e6e6e6;
            border-color: #adadad;
          }
          
          &:active {
            background-color: #e6e6e6;
            border-color: #adadad;
            box-shadow: inset 0 1px 1px rgba(0,0,0,0.1);
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
  overflow: auto;
  background: #f5f6fa;
}
</style> 