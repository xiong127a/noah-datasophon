<template>
  <div class="cdh-service-page">
    <!-- 左侧服务列表 -->
    <div class="service-sidebar">
      <div class="service-title">服务管理</div>
      <div class="service-list">
        <div v-for="(service, index) in menuList" :key="index" 
             class="service-item" 
             :class="{'active': isActiveService(service)}"
             @click="selectMenu(service)">
          <div class="service-icon">
            <svg-icon :icon-class="service.icon || 'service-default'" />
          </div>
          <div class="service-name">{{ service.name }}</div>
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
import { mapState } from 'vuex';

export default {
  name: "ServiceLayout",
  data() {
    return {
      menuList: []
    };
  },
  computed: {
    ...mapState('setting', ['menuData']),
    currentServiceId() {
      return this.$route.params.serviceId;
    }
  },
  mounted() {
    this.loadMenuData();
  },
  methods: {
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
          return {
            id: String(index + 1),
            name: item.name,
            path: item.fullPath,
            icon: (item.meta && item.meta.icon) || 'service-default',
            serviceId: (item.meta && item.meta.params && item.meta.params.serviceId) || ''
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
                path: `/service-manage/service-list/${item.id}`
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
        { id: '1', name: 'HDFS', icon: 'hdfs', path: '/service-manage/service-list/1', serviceId: '1' },
        { id: '2', name: 'YARN', icon: 'yarn', path: '/service-manage/service-list/2', serviceId: '2' },
        { id: '3', name: 'HBase', icon: 'hbase', path: '/service-manage/service-list/3', serviceId: '3' },
        { id: '4', name: 'Hive', icon: 'hive', path: '/service-manage/service-list/4', serviceId: '4' },
        { id: '5', name: 'ZooKeeper', icon: 'zookeeper', path: '/service-manage/service-list/5', serviceId: '5' },
        { id: '6', name: 'Spark', icon: 'spark', path: '/service-manage/service-list/6', serviceId: '6' }
      ];
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
  width: 220px;
  background: #fff;
  border-right: 1px solid #e0e0e0;
  overflow-y: auto;
  
  .service-title {
    padding: 16px;
    font-size: 16px;
    font-weight: 600;
    border-bottom: 1px solid #e0e0e0;
    position: sticky;
    top: 0;
    z-index: 10;
    background: #fff;
  }
  
  .service-list {
    padding: 8px 0;
  }
  
  .service-item {
    display: flex;
    align-items: center;
    padding: 12px 16px;
    cursor: pointer;
    transition: background-color 0.2s;
    
    &:hover {
      background-color: #f0f6ff;
    }
    
    &.active {
      background-color: #e6f7ff;
      border-right: 3px solid #1976d2;
    }
    
    .service-icon {
      margin-right: 12px;
      
      .svg-icon {
        font-size: 20px;
      }
    }
    
    .service-name {
      flex: 1;
      font-size: 14px;
    }
  }
}

.service-content {
  flex: 1;
  overflow: auto;
  background: #f5f6fa;
}
</style> 