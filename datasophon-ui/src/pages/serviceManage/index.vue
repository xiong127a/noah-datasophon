<template>
  <div class="service-detail">
    <div class="service-header" v-if="serviceName">
      <div class="header-title">
        <span>{{ serviceName }}</span>
      </div>
    </div>
    <div class="service-content card-shadow">
      <a-tabs v-model="tabKey" @change="callback">
        <a-tab-pane :key="1" tab="总览" v-if="pageOverview">
          <OverViewPage :serviceId="serviceId" />
        </a-tab-pane>
        <a-tab-pane :key="2" tab="实例">
          <ExampleList ref="ExampleListRef" :serviceId="serviceId" />
        </a-tab-pane>
        <a-tab-pane :key="3" tab="配置">
          <Setting />
        </a-tab-pane>
        <a-tab-pane :key="4" tab="连接信息" v-if="hasConnectionInfo && isServiceConnectionAvailable">
          <ConnectInfo :serviceId="$route.params.serviceId" ref="connectInfoRef" />
        </a-tab-pane>
        <a-tab-pane :key="5" tab="组件介绍">
          <ComponentIntro :serviceId="$route.params.serviceId" />
        </a-tab-pane>
        <a-tab-pane :key="6" tab="用户指南">
          <UserGuide :serviceId="$route.params.serviceId" />
        </a-tab-pane>
        <a-tab-pane v-if="serviceName === 'YARN'" :key="7" tab="资源配置">
          <Queue />
        </a-tab-pane>
      </a-tabs>
      <a-dropdown class="webui" :style="{left: getWebUILeftPosition()}" v-if="webUis.length > 0">
        <a-menu slot="overlay" @click="handleMenuClick">
          <a-menu-item v-for="(item, index) in webUis" :key="index">{{item.name}}</a-menu-item>
        </a-menu>
        <div class="mgr12">
          WebUI
          <a-icon type="down" />
        </div>
      </a-dropdown>
      <div v-else class="webui" :style="{left: getWebUILeftPosition()}">
        WebUI
        <a-icon type="down" />
      </div>
    </div>
  </div>
</template>

<script>
import ExampleList from "./exampleList.vue";
// import OverViewPage from "./overViewPage.vue";
const OverViewPage = () => import ('./overViewPage.vue')
import Setting from "./setting.vue";
import Queue from './queue.vue'
import ConnectInfo from './connectInfo/index.vue'
import ComponentIntro from './helpInfo/componentIntro.vue'
import UserGuide from './helpInfo/userGuide.vue'

// 导入连接信息服务检测工具
import { checkServiceSupport } from './connectInfo/serviceSupport'

export default {
  name: "ServiceList",
  components: { ExampleList, Setting, OverViewPage, Queue, ConnectInfo, ComponentIntro, UserGuide },

  data() {
    return {
      tabKey: 1,
      serviceName: '',
      loading: false,
      tabList: ["总览", "实例", "配置", "连接信息", "组件介绍", "用户指南"],
      serviceId: "",
      webUis: [],
      pageOverview: true,
      hasConnectionInfo: false, // 是否显示连接信息标签
      isServiceConnectionAvailable: false, // 服务是否有可用的连接信息内容
      tableColumns: [
        { title: "序号", key: "index" },
        { title: "角色类型", key: "serviceName" },
        { title: "主机", key: "serviceVersion" },
        { title: "状态", key: "serviceDesc" },
        { title: "操作", key: "action" },
      ],
    };
  },

  watch: {
    $route: function (val, oldVal) {
      if (this.$store.state.setting.serviceId === val.params.serviceId) return false
      this.$store.commit('setting/setServiceId', val.params.serviceId)
      this.serviceId = val.params.serviceId;
    }
  },

  mounted() {
    this.serviceId = this.$route.params.serviceId;
    this.getWebUis();
    this.getServiceName();
  },

  activated () {
    this.serviceId = this.$route.params.serviceId;
    this.getWebUis();
    this.getServiceName();
  },

  methods: {
    getWebUILeftPosition() {
      // 在服务详情页中，使用固定位置
      // 对于KRBCLIENT服务，返回固定位置
      if (this.serviceName === 'KRBCLIENT') return '140px';
      
      // 对于其他服务，使用固定位置
      return '540px';
    },
    
    handleMenuClick(item) {
      let url = this.webUis[item.key].webUrl
      window.open(url)
    },
    callback(key) {
      this.tabKey = key;
      
      if (key === 4) {
        this.$nextTick(() => {
          if (this.$refs.connectInfoRef) {
            try {
              // 优先使用 getConnectionInfo 方法，如果不存在则使用 fetchServiceInfo，避免两个都调用
              const hasGetConnectionInfo = typeof this.$refs.connectInfoRef.getConnectionInfo === 'function';
              const hasFetchServiceInfo = typeof this.$refs.connectInfoRef.fetchServiceInfo === 'function';
              
              if (hasGetConnectionInfo) {
                this.$refs.connectInfoRef.getConnectionInfo();
              } else if (hasFetchServiceInfo) {
                this.$refs.connectInfoRef.fetchServiceInfo();
              }
            } catch (error) {
              console.error("调用连接信息刷新方法失败:", error);
            }
          }
        });
      }
    },
    getWebUis() {
      this.$axiosPost(global.API.getWebUis, {
        serviceInstanceId: this.$route.params.serviceId,
      }).then((res) => {
        if (res.code === 200) {
          this.webUis = res.data || [];
        }
      });
    },
    getServiceName () {
      if (this.$route && this.$route.params && this.$route.params.serviceId) {
        let name = ''
        const serviceId = this.$route.params.serviceId || ''
        const menuData = JSON.parse(localStorage.getItem('menuData')) || []
        const arr = menuData.filter(item => item.path === 'service-manage')
        if (arr.length > 0) {
          arr[0].children.map(item => {
            if (item.meta.params.serviceId == serviceId) {
              name = item.name
              this.pageOverview = (item.meta.obj.dashboardUrl != undefined && item.meta.obj.dashboardUrl != "")
              this.tabKey = (this.pageOverview ? 1 : 2);
            }
          })
          this.serviceName = name
          
          this.checkConnectionInfoSupport(name);
        }
      }
    },
    
    checkConnectionInfoSupport(serviceName) {
      // 只通过服务名称判断是否支持连接信息，不再调用API
      this.hasConnectionInfo = checkServiceSupport(serviceName);
      
      // 默认将isServiceConnectionAvailable设置为与hasConnectionInfo相同
      this.isServiceConnectionAvailable = this.hasConnectionInfo;
    }
  }
};
</script>

<style lang="less" scoped>
.service-detail {
  padding: 20px;
  
  .service-header {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 20px;
    padding-bottom: 12px;
    border-bottom: 1px solid #e0e0e0;
    
    .header-title {
      font-size: 22px;
      font-weight: 600;
      color: #1976d2;
    }
  }
  
  .service-content {
    background: #fff;
    padding: 20px;
    border-radius: 4px;
    position: relative;
    
    .webui {
      position: absolute;
      left: 400px;
      top: 12px;
      cursor: pointer;
      color: #1976d2;
      z-index: 10;
    }
  }
}

/* 额外添加WebUI按钮样式 */
.mgr12 {
  cursor: pointer;
  padding: 4px 8px;
  border-radius: 4px;
  color: #1976d2;
  &:hover {
    background-color: #f0f6ff;
  }
}
</style>