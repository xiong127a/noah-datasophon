<template>
  <div class="card-list card-shadow">
    <a-list :grid="{ gutter: 24, lg: 3, md: 2, sm: 1, xs: 1 }" :dataSource="dataSource">
      <a-list-item slot="renderItem" slot-scope="item">
        <template v-if="item.add">
          <div class="new-btn" @click="addColony({})">
            <div class="add-icon">
              <svg-icon icon-class="add-cluster" style="font-size: 80px"></svg-icon>
            </div>
            <div>创建集群</div>
          </div>
        </template>
        <template v-else>
          <div :class="['colony-card', item.clusterStateCode === 2 ? 'colony-running-card' : 'colony-configured-card']">
            <div class="card-header flex-bewteen-container">
              <div class="flex-container">
                <div :class="['colony-icon-warp', item.clusterStateCode === 2 ? 'running-status-bg' : 'configured-status-bg']">
                  <svg-icon :class="['colony-icon', item.clusterStateCode === 2 ? 'running-status-color' : item.clusterStateCode === 3 ? 'error-status-color' : 'configured-status-color']" icon-class="colony"></svg-icon>
                </div>
                <div class="colony-title">{{ item.clusterName }}</div>
              </div>
              <div :class="['colony-status']">
                <svg-icon :class="['colony-status-icon', item.clusterStateCode === 2 ? 'running-status-color' : item.clusterStateCode === 3 ? 'error-status-color' : 'configured-status-color']" :icon-class="item.clusterStateCode === 2 ? 'running-status' : 'configured-status'"></svg-icon>
                <span class="mgl5">{{item.clusterState}}</span>
              </div>
            </div>
            <div class="card-content">
              <div>
                集群管理员： 
                <span>{{item.userManageName || '-'}}</span>
              </div>
              <div>
                创建时间：
                <span>{{item.createTime}}</span>
              </div>
            </div>
            <div class="card-footer flex-bewteen-container">
              <a-button v-if="user && user.userType === 1" type="link" @click="authCluster(item)">授权</a-button>
              <a-button type="link" @click="addColony(item)" :disabled="item.clusterStateCode === 2">编辑</a-button>
              <a-button type="link" @click="getInto(item)" :disabled="item.clusterStateCode === 1">进入</a-button>
              <a-button type="link" :disabled="item.clusterStateCode === 2" @click="configCluster(item)">配置集群</a-button>
              <a-button type="link" @click="delectColony(item)" :disabled="item.clusterStateCode === 2">删除集群</a-button>
            </div>
          </div>
        </template>
      </a-list-item>
    </a-list>
    <!-- 配置集群的modal -->
    <a-modal v-if="visible" title :visible="visible" :maskClosable="false" :closable="false" :width="1576"
      :confirm-loading="confirmLoading" @cancel="handleCancel" :footer="null">
      <div style="width: 100%; box-sizing: border-box;">
        <Steps :clusterId="clusterId" :depType="depType" />
      </div>
    </a-modal>
  </div>
</template>

<script>
import AddColony from "./commponents/addColony.vue";
import AuthCluster from "./commponents/authCluster.vue";
import DelectColony from "./commponents/delectColony.vue";
import { mapGetters, mapActions, mapMutations } from "vuex";
import Steps from "@/components/steps";
import { changeRouter } from '@/utils/changeRouter'
export default {
  name: "COLONYLIST",

  provide() {
    return {
      handleCancel: this.handleCancel,
      onSearch: null
    };
  },

  components: { Steps },

  data() {
    return {
      visible: false,
      dataSource: [],
      confirmLoading: false,
      clusterId: "", // 操作的集群Id
    };
  },

  computed: {
    ...mapGetters("account", ["user"]),
  },

  mounted() {
    this.getColonyList();
  },

  methods: {
    ...mapMutations("setting", ["setIsCluster", "setMenuData", "setClusterId"]),
    // 进入
    getInto(row) {
      this.$axiosPost(global.API.getServiceListByCluster, {
        clusterId: row.id,
      }).then((res) => {
        changeRouter(res.data, row.id)
        this.$router.push("/service-manage");
      });
    },
    addColony(obj) {
      const self = this;
      let width = 900;
      let title = JSON.stringify(obj) !== "{}" ? "编辑集群" : "创建集群";
      let content = (
        <AddColony detail={obj} callBack={() => self.getColonyList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    delectColony(obj) {
      const self = this;
      let width = 400;
      let content = (
        <DelectColony
          sysTypeTxt="集群"
          detail={obj}
          callBack={() => self.getColonyList()}
        />
      );
      this.$confirm({
        width: width,
        title: () => {
          return (
            <div>
              <a-icon
                type="question-circle"
                style="color:#2F7FD1 !important;margin-right:10px"
              />
              提示
            </div>
          );
        },
        content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    getColonyList() {
      this.$axiosPost(global.API.getColonyList, {}).then((res) => {
        this.dataSource = res.data;
        this.dataSource.forEach((item) => {
          let arr = [];
          item.clusterManagerList.map((childItem) => {
            arr.push(childItem.username);
          });
          item["userManageName"] = arr.join(",");
        });
        this.dataSource.push({
          add: true,
        });
      });
    },
    // 集群授权
    authCluster(obj) {
      const self = this;
      let width = 520;
      let title = "授权";
      let content = (
        <AuthCluster detail={obj} callBack={() => self.getColonyList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        icon: () => {
          return <div />;
        },
      });
    },
    // 配置集群
    configCluster(row) {
      this.clusterId = row.id;
      this.setClusterId(row.id)
      this.visible = true;
      this.depType = row.depType
    },
    handleCancel(e) {
      this.visible = false;
      this.getColonyList()
    },
  }
};
</script>

<style lang="less" scoped>
/deep/ .ant-modal-body {
  padding: 0;
  width: 100%;
  overflow: visible;
}
/deep/ .ant-modal {
  top: 62px;
  .ant-modal-content {
    border-radius: 12px;
    width: 100%;
    overflow: visible;
    box-shadow: 0 16px 48px rgba(0, 0, 0, 0.2);
    border: none;
  }
  
  .ant-modal-header {
    border-radius: 12px 12px 0 0;
    border-bottom: 1px solid #D1D1D6;
    
    .ant-modal-title {
      font-family: "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
      font-weight: 600;
      color: #1D1D1F;
    }
  }
}
.card-list {
  padding: 20px 10px;
  background: #fff;
  /deep/ .ant-row {
    margin: 0 !important;
  }
  /deep/ .ant-col {
    padding-left: 10px !important;
    padding-right: 10px !important;
  }
}
.colony-card {
  display: flex;
  flex-direction: column;
  justify-content: space-between;
  height: 220.48px;
  padding: 24px 16px 0px;
  background: #FFFFFF;
  border: 1px solid #D1D1D6;
  border-radius: 12px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  font-family: "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  .card-header {
    padding: 0 10px;
    .colony-icon-warp {
      width: 50px;
      height: 50px;
      border-radius: 50%;
      text-align: center;
      line-height: 50px;
      .colony-icon {
        // color: @primary-color;
        font-size: 24px;
        cursor: pointer;
      }
    }
    .colony-title {
      margin-left: 20px;
      font-size: 16px;
      color: #333333;
      letter-spacing: 0;
      font-weight: 600;
    }
    .colony-status {
      .colony-status-icon {
        font-size: 14px;
      }
    }
  }
  .card-content {
    margin-left: 70px;
    div {
      margin-top: 10px;
      margin-bottom: 6px;
      font-size: 14px;
      color: #666666;
      letter-spacing: 0;
      font-weight: 400;
      span {
        color: #333333;
        word-break: break-all;
        white-space: normal;
      }
    }
  }
  .card-footer {
    border-top: 1px solid #e3e4e6;
    height: 50px;
    line-height: 50px;
    /deep/ .ant-btn-link {
      width: 20%;
      margin: 12px 0;
      border-radius: 0;
      font-size: 14px;
      color: #555555;
      letter-spacing: 0;
      font-weight: 400;
      border: none;
    }
    /deep/ .ant-btn-link:not(:last-child) {
      border: none;
      border-right: 1px solid#e3e4e6;
    }
    /deep/ .ant-btn-link:not(:last-child):hover,
    .ant-btn-link:not(:last-child):focus {
      border: none;
      border-right: 1px solid#e3e4e6;
    }
  }
  /deep/ .ant-btn-link:not(.ant-btn-link[disabled]):hover {
    color: @primary-color;
  }
  /deep/ .ant-btn-link[disabled] {
    background: #fff;
    color: #bbb;
  }
}
.colony-running-card:hover {
  border: 1px solid @running-status-color;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
}
.colony-configured-card:hover {
  border: 1px solid @configured-status-color;
  box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
  transform: translateY(-4px);
}
.card-avatar {
  width: 48px;
  height: 48px;
  border-radius: 48px;
}
.new-btn {
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  width: 100%;
  height: 220.48px;
  border-radius: 12px;
  text-align: center;
  font-size: 16px;
  font-weight: 500;
  font-family: "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  background: linear-gradient(135deg, #F2F2F7, #FAFAFA);
  border: 2px dashed #D1D1D6;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  color: #1D1D1F;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, rgba(0, 122, 255, 0.05), rgba(0, 122, 255, 0.1));
    opacity: 0;
    transition: opacity 0.3s ease;
    border-radius: 10px;
  }
  
  .add-icon {
    margin-bottom: 16px;
    transform: scale(1);
    transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    
    /deep/ .svg-icon {
      color: #007AFF;
      font-size: 80px;
      transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
    }
  }
  
  &:hover {
    color: #007AFF;
    background: linear-gradient(135deg, #F8F9FA, #FFFFFF);
    border: 2px dashed #007AFF;
    box-shadow: 0 8px 24px rgba(0, 122, 255, 0.15);
    transform: translateY(-4px);
    
    &::before {
      opacity: 1;
    }
    
    .add-icon {
      transform: scale(1.1);
      
      /deep/ .svg-icon {
        color: #007AFF;
        transform: rotate(90deg);
      }
    }
  }
  
  &:active {
    transform: translateY(-2px);
    box-shadow: 0 4px 16px rgba(0, 122, 255, 0.2);
  }
}
.meta-content {
  position: relative;
  overflow: hidden;
  text-overflow: ellipsis;
  display: -webkit-box;
  height: 64px;
  -webkit-line-clamp: 3;
  -webkit-box-orient: vertical;
}
</style>