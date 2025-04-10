<template>
  <div class="service-list card-shadow">
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
      <a-tab-pane v-if="serviceName === 'YARN'" :key="5" tab="资源配置">
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
</template>

<script>
import ExampleList from "./exampleList.vue";
// import OverViewPage from "./overViewPage.vue";
const OverViewPage = () => import ('./overViewPage.vue')
import Setting from "./setting.vue";
import Queue from './queue.vue'
import ConnectInfo from './connectInfo/index.vue'

// 导入连接信息服务检测工具
import { checkServiceSupport } from './connectInfo/serviceSupport'

export default {
  name: "ServiceList",
  components: { ExampleList, Setting, OverViewPage, Queue, ConnectInfo },

  data() {
    return {
      tabKey: 1,
      serviceName: '',
      loading: false,
      tabList: ["总览", "实例", "配置", "连接信息"],
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
    this.getWebUis();
    this.getServiceName()
  },

  activated () {
    console.log('每次我只触发一次')
    this.serviceId = this.$route.params.serviceId;
    this.getWebUis();
    this.getServiceName()
    console.log(this.$route, 'sdadadasd')
  },

  deactivated () {
    console.log('每次我buxiang只触发一次')
  },

  methods: {
    getWebUILeftPosition() {
      let visibleTabs = 0;
      
      if (this.pageOverview) visibleTabs++;
      visibleTabs++; // 实例页签总是显示
      visibleTabs++; // 配置页签总是显示
      if (this.hasConnectionInfo && this.isServiceConnectionAvailable) visibleTabs++;
      if (this.serviceName === 'YARN') visibleTabs++;
      
      // 为每个标签分配更合理的空间
      const baseWidth = 60;   // 每个标签的基础宽度(减小)
      const tabSpacing = 16;  // 标签之间的间距(减小到16px，与CSS一致)
      const totalWidth = visibleTabs * baseWidth + (visibleTabs - 1) * tabSpacing;
      
      // 特殊处理某些服务
      if (this.serviceName === 'KRBCLIENT') return '140px';
      
      // 添加标准的16px间距，使WebUI与最后一个标签的间距与标签之间的间距一致
      return (totalWidth + tabSpacing) + 'px';
    },
    
    getWebUIWidth(serviceName) {
      return this.getWebUILeftPosition();
    },
    handleMenuClick(item) {
      let url = this.webUis[item.key].webUrl
      window.open(url)
    },
    callback(key) {
      console.log("Tab changed to:", key, "类型:", typeof key);
      this.tabKey = key;
      
      if (key === 4) {
        console.log("连接信息标签页激活，serviceId:", this.$route.params.serviceId);
        this.$nextTick(() => {
          if (this.$refs.connectInfoRef) {
            console.log("手动调用connectInfoRef.getConnectionInfo方法");
            try {
              if (typeof this.$refs.connectInfoRef.getConnectionInfo === 'function') {
                this.$refs.connectInfoRef.getConnectionInfo();
              } else if (typeof this.$refs.connectInfoRef.fetchServiceInfo === 'function') {
                this.$refs.connectInfoRef.fetchServiceInfo();
              }
            } catch (error) {
              console.error("调用连接信息刷新方法失败:", error);
            }
          } else {
            console.error("connectInfoRef不存在");
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
      this.hasConnectionInfo = checkServiceSupport(serviceName);
      
      if (this.hasConnectionInfo) {
        this.checkServiceConnectionAvailability();
      } else {
        this.isServiceConnectionAvailable = false;
      }
    },
    
    checkServiceConnectionAvailability() {
      if (!this.$route.params.serviceId) {
        this.isServiceConnectionAvailable = false;
        return;
      }
      
      this.$axiosPost(global.API.getConnectionInfo, {
        serviceInstanceId: this.$route.params.serviceId
      })
      .then(res => {
        if (res.code === 200 && res.data) {
          const hasBasicInfo = res.data.basicInfo && Object.keys(res.data.basicInfo).length > 0;
          const hasJdbcUrl = res.data.jdbcUrl || (res.data.jdbcUrls && res.data.jdbcUrls.length > 0);
          const hasCode = res.data.javaCode || res.data.pythonCode;
          const hasCommands = res.data.beelineCommand || res.data.cliCommand;
          
          this.isServiceConnectionAvailable = hasBasicInfo || hasJdbcUrl || hasCode || hasCommands;
        } else {
          this.isServiceConnectionAvailable = false;
        }
      })
      .catch(() => {
        this.isServiceConnectionAvailable = false;
      });
    }
  }
};
</script>

<style lang="less" scoped>
.service-list {
  background: #fff;
  padding: 0 20px 20px;
  position: relative;
  .webui {
    position: absolute;
    left: 200px;
    top: 12px;
  }
}
</style>