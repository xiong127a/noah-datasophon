<template>
  <div class="cdh-service-page">
    <!-- 左侧服务列表 -->
    <div class="service-sidebar">
      <div class="service-title">
        <span>服务管理</span>
        <service-option class="service-more" />
      </div>
      <div class="service-list">
        <div v-for="(service, index) in menuList" :key="index" 
             class="service-item" 
             :class="{'active': isActiveService(service)}"
             @click="selectMenu(service)">
          <div class="flex-bewteen-container">
            <div class="flex-container service-item-left">
              <!-- 状态灯 -->
              <span :class="['circle-point', service.serviceStateCode === 1 ? 'hide-point' : 
                             service.serviceStateCode === 2 ? 'success-point': 
                             service.serviceStateCode === 3 ? 'configured-point': 'error-point']"></span>
              <div class="service-icon">
                <svg-icon :icon-class="service.icon || 'service-default'" />
              </div>
              <div class="service-name" :style="getServiceClassNameStyle(service)" :title="service.name">{{ service.name }}</div>
            </div>
            <div class="service-item-right">
              <!-- 告警详情 -->
              <span v-if="[3,4].includes(service.serviceStateCode) && service.alertNum > 0" 
                    :class="[service.serviceStateCode === 4 ? 'error-status-color': 'configured-status-color']" 
                    @click.stop="showAlarm(service)">
                <span>
                  <svg-icon class="icon-gj" icon-class="gaojing"></svg-icon>
                  {{service.alertNum || 0}}
                </span>
              </span>
              <!-- 配置变更提示 -->
              <a-icon v-if="service.needRestart" type="sync" class="menu-sub-icon" @click.stop="showConfigCompare(service)" />
              <!-- 更多选项 -->
              <a-popover trigger="hover" placement="rightTop" class="popover-index" overlayClassName="popover-index" :content="() => getMoreMenu(service)">
                <a-icon type="more" class="menu-sub-icon" />
              </a-popover>
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
      menuList: []
    };
  },
  computed: {
    ...mapState('setting', ['menuData', 'alarmManageVisible', 'clusterId']),
    currentServiceId() {
      return this.$route.params.serviceId;
    }
  },
  mounted() {
    this.loadMenuData();
  },
  methods: {
    ...mapMutations("setting", ["showClusterSetting"]),
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
            serviceId: (item.meta && item.meta.params && item.meta.params.serviceId) || '',
            // 添加服务状态相关的属性
            serviceStateCode: item.meta && item.meta.obj ? item.meta.obj.serviceStateCode : 1,
            alertNum: item.meta && item.meta.obj ? item.meta.obj.alertNum : 0,
            needRestart: item.meta && item.meta.obj ? item.meta.obj.needRestart : false,
            rawData: item.meta && item.meta.obj ? item.meta.obj : {}
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
                rawData: {}
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
        { id: '1', name: 'HDFS', icon: 'hdfs', path: '/service-manage/service-list/1', serviceId: '1', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {} },
        { id: '2', name: 'YARN', icon: 'yarn', path: '/service-manage/service-list/2', serviceId: '2', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {} },
        { id: '3', name: 'HBase', icon: 'hbase', path: '/service-manage/service-list/3', serviceId: '3', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {} },
        { id: '4', name: 'Hive', icon: 'hive', path: '/service-manage/service-list/4', serviceId: '4', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {} },
        { id: '5', name: 'ZooKeeper', icon: 'zookeeper', path: '/service-manage/service-list/5', serviceId: '5', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {} },
        { id: '6', name: 'Spark', icon: 'spark', path: '/service-manage/service-list/6', serviceId: '6', serviceStateCode: 2, alertNum: 0, needRestart: false, rawData: {} }
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
    display: flex;
    justify-content: space-between;
    align-items: center;
    
    .service-more {
      margin-left: auto;
    }
  }
  
  .service-list {
    padding: 8px 0;
  }
  
  .service-item {
    display: flex;
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

    &-left {
      .circle-point {
        width: 10px;
        height: 10px;
        border-radius: 50%;
        display: block;
        z-index: 1000;
        margin-right: 10px;
      }
      .hide-point {
        visibility: hidden;
      }
      .success-point {
        background: #52c41a;
      }
      .error-point {
        background: #f5222d;
      }
      .configured-point {
        background: #faad14;
      }
    }

    &-right {
      margin-left: auto;
      position: relative;
      display: flex;
      justify-content: space-between;
      align-items: center;
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
      overflow: hidden;
      white-space: nowrap;
      text-overflow: ellipsis;
    }
  }
}

.service-content {
  flex: 1;
  overflow: auto;
  background: #f5f6fa;
}

.flex-container {
  display: flex;
  align-items: center;
}

.flex-bewteen-container {
  display: flex;
  justify-content: space-between;
  align-items: center;
  width: 100%;
}

.menu-sub-icon {
  margin: 0 6px 0 8px;
  cursor: pointer;
}

.error-status-color {
  color: #f5222d;
}

.configured-status-color {
  color: #faad14;
}

.icon-gj {
  position: relative;
  top: -2px;
}

.popover-index {
  .more-menu-btn {
    font-size: 14px;
    color: #555555;
    letter-spacing: 0.39px;
    line-height: 32px;
    font-weight: 400;
    &:hover {
      color: #2F7FD1;
    }
  }
}

:deep(.ant-popover-inner-content) {
  text-align: left;
  padding: 12px 16px;
}
</style> 