<template>
  <div class="macos-cluster-container">
    <!-- 集群网格 - 最大化利用屏幕空间 -->
    <div class="macos-cluster-grid">
      <!-- 现有集群卡片 -->
      <div 
        v-for="(item, index) in filteredDataSource" 
        :key="index"
        :class="[
          'macos-cluster-card',
          'cluster-card',
          getClusterTypeClass(item.depType)
        ]"
      >
        <!-- 集群信息卡片 -->
        <div class="cluster-card-content">
          <!-- 卡片头部 -->
          <div class="card-header">
            <div class="cluster-info">
              <div class="cluster-icon" :class="getClusterTypeClass(item.depType)">
                <img v-if="item.depType === 'PVM'" src="~@/assets/img/os-logos/linux-tux.svg" alt="Linux" />
                <img v-else-if="item.depType === 'Kubernetes'" src="~@/assets/images/kubernetes-logo.svg" alt="Kubernetes" />
                <svg v-else viewBox="0 0 24 24" fill="none">
                  <rect x="3" y="4" width="18" height="16" rx="2" stroke="currentColor" stroke-width="1.5"/>
                  <path d="M7 8h10M7 12h6" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
                </svg>
              </div>
              <div class="cluster-details">
                <h3 class="cluster-name">{{ item.clusterName }}</h3>
                <div class="cluster-status">
                  <div :class="['status-indicator', getStatusClass(item.clusterStateCode)]"></div>
                  <span class="status-text">{{ item.clusterState }}</span>
                </div>
                <div class="cluster-type">
                  <span class="type-badge" :class="getClusterTypeClass(item.depType)">
                    {{ getClusterTypeText(item.depType) }}
                  </span>
                </div>
              </div>
            </div>
          </div>

          <!-- 卡片内容 -->
          <div class="card-body">
            <div class="info-grid">
              <div class="info-item">
                <span class="info-label">管理员</span>
                <span class="info-value">{{ item.userManageName || '未分配' }}</span>
              </div>
              <div class="info-item">
                <span class="info-label">创建时间</span>
                <span class="info-value">{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>

          <!-- 卡片操作 -->
          <div class="card-actions">
            <div class="primary-actions">
              <button 
                class="macos-button primary"
                @click.stop="getInto(item)" 
                :disabled="item.clusterStateCode === 1"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="none">
                  <path d="M9 12l2 2 4-4" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
                  <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="2"/>
                </svg>
                进入集群
              </button>
            </div>
            <div class="secondary-actions">
              <button 
                v-if="user && user.userType === 1" 
                class="macos-button secondary"
                @click.stop="authCluster(item)"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="none">
                  <path d="M16 21v-2a4 4 0 0 0-4-4H5a4 4 0 0 0-4 4v2" stroke="currentColor" stroke-width="2"/>
                  <circle cx="8.5" cy="7" r="4" stroke="currentColor" stroke-width="2"/>
                  <path d="M20 8v6M23 11l-3 3-3-3" stroke="currentColor" stroke-width="2"/>
                </svg>
                授权
              </button>
              <button 
                class="macos-button secondary"
                @click.stop="addColony(item)" 
                :disabled="item.clusterStateCode === 2"
              >
                <svg class="button-icon" viewBox="0 0 24 24" fill="none">
                  <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7" stroke="currentColor" stroke-width="2"/>
                  <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z" stroke="currentColor" stroke-width="2"/>
                </svg>
                编辑
              </button>
              <div class="more-actions">
                <button class="macos-button icon-only" @click.stop="toggleDropdown(index)">
                  <svg viewBox="0 0 24 24" fill="none">
                    <circle cx="12" cy="12" r="1" fill="currentColor"/>
                    <circle cx="19" cy="12" r="1" fill="currentColor"/>
                    <circle cx="5" cy="12" r="1" fill="currentColor"/>
                  </svg>
                </button>
                <div v-if="activeDropdown === index" class="dropdown-menu">
                  <button 
                    class="dropdown-item"
                    @click.stop="configCluster(item)"
                    :disabled="item.clusterStateCode === 2"
                  >
                    <svg class="item-icon" viewBox="0 0 24 24" fill="none">
                      <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="2"/>
                      <path d="M19.4 15a1.65 1.65 0 0 0 .33 1.82l.06.06a2 2 0 0 1 0 2.83 2 2 0 0 1-2.83 0l-.06-.06a1.65 1.65 0 0 0-1.82-.33 1.65 1.65 0 0 0-1 1.51V21a2 2 0 0 1-2 2 2 2 0 0 1-2-2v-.09A1.65 1.65 0 0 0 9 19.4a1.65 1.65 0 0 0-1.82.33l-.06.06a2 2 0 0 1-2.83 0 2 2 0 0 1 0-2.83l.06-.06a1.65 1.65 0 0 0 .33-1.82 1.65 1.65 0 0 0-1.51-1H3a2 2 0 0 1-2-2 2 2 0 0 1 2-2h.09A1.65 1.65 0 0 0 4.6 9a1.65 1.65 0 0 0-.33-1.82l-.06-.06a2 2 0 0 1 0-2.83 2 2 0 0 1 2.83 0l.06.06a1.65 1.65 0 0 0 1.82.33H9a1.65 1.65 0 0 0 1 1.51V3a2 2 0 0 1 2-2 2 2 0 0 1 2 2v.09a1.65 1.65 0 0 0 1 1.51 1.65 1.65 0 0 0 1.82-.33l.06-.06a2 2 0 0 1 2.83 0 2 2 0 0 1 0 2.83l-.06.06a1.65 1.65 0 0 0-.33 1.82V9a1.65 1.65 0 0 0 1.51 1H21a2 2 0 0 1 2 2 2 2 0 0 1-2 2h-.09a1.65 1.65 0 0 0-1.51 1z" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    配置集群
                  </button>
                  <button 
                    class="dropdown-item danger"
                    @click.stop="delectColony(item)"
                    :disabled="item.clusterStateCode === 2"
                  >
                    <svg class="item-icon" viewBox="0 0 24 24" fill="none">
                      <polyline points="3,6 5,6 21,6" stroke="currentColor" stroke-width="2"/>
                      <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    删除集群
                  </button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 新建集群卡片 - 放在最后 -->
      <div class="macos-cluster-card create-cluster-card" @click="addColony({})">
        <div class="create-cluster-content">
          <div class="create-icon">
            <svg viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="10" stroke="currentColor" stroke-width="1.5"/>
              <path d="M12 8v8M8 12h8" stroke="currentColor" stroke-width="1.5" stroke-linecap="round"/>
            </svg>
          </div>
          <h3 class="create-title">创建新集群</h3>
          <p class="create-description">快速部署和管理您的大数据集群</p>
          <div class="create-features">
            <div class="feature-item">
              <svg viewBox="0 0 16 16" fill="none">
                <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z" fill="currentColor"/>
              </svg>
              <span>一键部署</span>
            </div>
            <div class="feature-item">
              <svg viewBox="0 0 16 16" fill="none">
                <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z" fill="currentColor"/>
              </svg>
              <span>智能配置</span>
            </div>
            <div class="feature-item">
              <svg viewBox="0 0 16 16" fill="none">
                <path d="M13.854 3.646a.5.5 0 0 1 0 .708l-7 7a.5.5 0 0 1-.708 0l-3.5-3.5a.5.5 0 1 1 .708-.708L6.5 10.293l6.646-6.647a.5.5 0 0 1 .708 0z" fill="currentColor"/>
              </svg>
              <span>高可用</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <!-- 配置集群的modal -->
    <a-modal 
      v-if="visible" 
      title="配置集群" 
      :visible="visible" 
      :maskClosable="false" 
      :closable="true" 
      :width="1576"
      :confirm-loading="confirmLoading" 
      @cancel="handleCancel" 
      :footer="null"
      class="macos-modal"
    >
      <div class="modal-content">
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
      activeDropdown: null, // 当前激活的下拉菜单
    };
  },

  computed: {
    ...mapGetters("account", ["user"]),
    // 过滤掉添加集群的占位项
    filteredDataSource() {
      return this.dataSource.filter(item => !item.add);
    }
  },

  mounted() {
    this.getColonyList();
    // 点击外部关闭下拉菜单
    document.addEventListener('click', this.handleClickOutside);
  },
  
  beforeDestroy() {
    document.removeEventListener('click', this.handleClickOutside);
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
      let width = 800; // 从1000px减小到800px
      let title = JSON.stringify(obj) !== "{}" ? "编辑集群配置" : "创建新集群";
      let content = (
        <AddColony detail={obj} callBack={() => self.getColonyList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        wrapClassName: 'apple-create-modal',
        okButtonProps: { style: { display: 'none' } }, // 隐藏默认按钮
        cancelButtonProps: { style: { display: 'none' } }, // 隐藏默认按钮
        maskClosable: false,
        centered: true,
        destroyOnClose: true,
        bodyStyle: { 
          padding: 0, 
          maxHeight: 'calc(100vh - 200px)', 
          overflow: 'auto' 
        }, // 限制高度并添加滚动
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
        // 移除添加集群的占位项，因为不再需要创建集群按钮
      });
    },
    // 集群授权
    authCluster(obj) {
      const self = this;
      let width = 460; // 更宽的模态框
      let title = null; // 不显示标题
      let content = (
        <AuthCluster detail={obj} callBack={() => self.getColonyList()} />
      );
      this.$confirm({
        width: width,
        title: title,
        content: content,
        closable: true,
        wrapClassName: 'auth-cluster-modal',
        okButtonProps: { style: { display: 'none' } },
        cancelButtonProps: { style: { display: 'none' } },
        maskClosable: false,
        centered: true,
        destroyOnClose: true,
        bodyStyle: { padding: 0 },
        style: { top: '10%' },
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
    // 切换下拉菜单
    toggleDropdown(index) {
      this.activeDropdown = this.activeDropdown === index ? null : index;
    },
    // 获取状态样式类
    getStatusClass(statusCode) {
      switch(statusCode) {
        case 2: return 'running';
        case 3: return 'error';
        default: return 'configured';
      }
    },
    // 格式化日期
    formatDate(dateString) {
      if (!dateString) return '-';
      const date = new Date(dateString);
      return date.toLocaleDateString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit'
      });
    },
    // 处理点击外部关闭下拉菜单
    handleClickOutside(event) {
      if (!event.target.closest('.more-actions')) {
        this.activeDropdown = null;
      }
    },
    // 获取集群类型样式类
    getClusterTypeClass(depType) {
      switch(depType) {
        case 'PVM': return 'linux-type';
        case 'Kubernetes': return 'k8s-type';
        default: return 'default-type';
      }
    },
    // 获取集群类型文本
    getClusterTypeText(depType) {
      switch(depType) {
        case 'PVM': return '裸金属/虚拟机';
        case 'Kubernetes': return 'Kubernetes';
        default: return '未知';
      }
    },
  }
};
</script>

<style lang="less" scoped>
/* 现代化浅色系设计系统 - 与AdminHeader保持一致 */
.macos-cluster-container {
  min-height: 100vh;
  background: linear-gradient(135deg, #fafbfc 0%, #f5f7fa 100%);
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'PingFang SC', 'Helvetica Neue', Helvetica, Arial, sans-serif;
  padding: 24px;
  margin: 0;
  box-sizing: border-box;
}

/* 现代化浅色系按钮系统 - 增强版 */
.macos-button {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 12px 20px;
  border-radius: 12px;
  font-size: 14px;
  font-weight: 600;
  border: none;
  cursor: pointer;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  position: relative;
  overflow: hidden;
  white-space: nowrap;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  
  /* 添加微妙的内阴影效果 */
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    border-radius: inherit;
    padding: 1px;
    background: linear-gradient(135deg, rgba(255,255,255,0.2) 0%, rgba(255,255,255,0.05) 100%);
    mask: linear-gradient(#fff 0 0) content-box, linear-gradient(#fff 0 0);
    mask-composite: exclude;
    -webkit-mask-composite: xor;
    pointer-events: none;
  }
  
  &.primary {
    background: linear-gradient(135deg, #007aff 0%, #0051d5 50%, #003db8 100%);
    color: white;
    box-shadow: 
      0 4px 16px rgba(0, 122, 255, 0.25),
      0 2px 8px rgba(0, 122, 255, 0.15),
      inset 0 1px 0 rgba(255, 255, 255, 0.2);
    
    &:hover:not(:disabled) {
      transform: translateY(-3px) scale(1.02);
      box-shadow: 
        0 8px 32px rgba(0, 122, 255, 0.35),
        0 4px 16px rgba(0, 122, 255, 0.25),
        inset 0 1px 0 rgba(255, 255, 255, 0.3);
      background: linear-gradient(135deg, #1a8cff 0%, #1a5ce6 50%, #0051d5 100%);
    }
    
    &:active:not(:disabled) {
      transform: translateY(-1px) scale(1.01);
      box-shadow: 
        0 4px 20px rgba(0, 122, 255, 0.4),
        0 2px 10px rgba(0, 122, 255, 0.3),
        inset 0 1px 0 rgba(255, 255, 255, 0.15);
      transition-duration: 0.1s;
    }
  }
  
  &.secondary {
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(248, 250, 252, 0.9) 100%);
    color: #2c2c2c;
    border: 1px solid rgba(0, 0, 0, 0.08);
    box-shadow: 
      0 2px 8px rgba(0, 0, 0, 0.04),
      inset 0 1px 0 rgba(255, 255, 255, 0.8);
    
    &:hover {
      background: linear-gradient(135deg, rgba(255, 255, 255, 1) 0%, rgba(250, 252, 255, 0.98) 100%);
      border-color: rgba(0, 122, 255, 0.25);
      color: #007aff;
      transform: translateY(-2px) scale(1.01);
      box-shadow: 
        0 6px 20px rgba(0, 122, 255, 0.12),
        0 2px 8px rgba(0, 0, 0, 0.06),
        inset 0 1px 0 rgba(255, 255, 255, 0.9);
    }
    
    &:active {
      background: linear-gradient(135deg, rgba(245, 248, 250, 1) 0%, rgba(240, 245, 251, 0.98) 100%);
      transform: translateY(-1px) scale(1.005);
      transition-duration: 0.1s;
    }
  }
  
  &.icon-only {
    padding: 10px;
    min-width: 36px;
    justify-content: center;
    background: linear-gradient(135deg, rgba(255, 255, 255, 0.9) 0%, rgba(248, 250, 252, 0.85) 100%);
    border: 1px solid rgba(0, 0, 0, 0.06);
    gap: 0;
    box-shadow: 
      0 2px 6px rgba(0, 0, 0, 0.03),
      inset 0 1px 0 rgba(255, 255, 255, 0.7);
    
    &:hover {
      background: linear-gradient(135deg, rgba(255, 255, 255, 1) 0%, rgba(250, 252, 255, 0.95) 100%);
      border-color: rgba(0, 122, 255, 0.2);
      color: #007aff;
      transform: translateY(-2px) scale(1.05);
      box-shadow: 
        0 4px 16px rgba(0, 122, 255, 0.08),
        0 2px 8px rgba(0, 0, 0, 0.04),
        inset 0 1px 0 rgba(255, 255, 255, 0.8);
    }
    
    &:active {
      transform: translateY(-1px) scale(1.02);
      transition-duration: 0.1s;
    }
  }
  
  &:disabled {
    opacity: 0.4;
    cursor: not-allowed;
    transform: none !important;
    box-shadow: none !important;
  }
  
  .button-icon {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
  }
  
  svg {
    width: 14px;
    height: 14px;
    flex-shrink: 0;
  }
}

/* 集群网格 - 最大化利用屏幕空间 */
.macos-cluster-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(380px, 1fr));
  gap: 24px;
  width: 100%;
  max-width: none;
}

/* 新建集群卡片样式 */
.create-cluster-card {
  background: linear-gradient(135deg, 
    rgba(0, 122, 255, 0.05) 0%, 
    rgba(0, 122, 255, 0.02) 50%, 
    rgba(255, 255, 255, 0.95) 100%);
  border: 2px dashed rgba(0, 122, 255, 0.2);
  cursor: pointer;
  position: relative;
  overflow: hidden;
  
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
    right: 0;
    bottom: 0;
    background: linear-gradient(135deg, 
      rgba(0, 122, 255, 0.08) 0%, 
      rgba(0, 122, 255, 0.03) 100%);
    opacity: 0;
    transition: opacity 0.3s ease;
  }
  
  &:hover {
    transform: translateY(-6px) scale(1.02);
    border-color: rgba(0, 122, 255, 0.4);
    box-shadow: 
      0 12px 40px rgba(0, 122, 255, 0.15),
      0 4px 16px rgba(0, 122, 255, 0.1);
    
    &::before {
      opacity: 1;
    }
    
    .create-icon {
      transform: scale(1.1) rotate(5deg);
      
      svg {
        color: #007aff;
      }
    }
    
    .create-title {
      color: #007aff;
    }
    
    .feature-item {
      transform: translateX(4px);
      
      svg {
        color: #007aff;
      }
    }
  }
  
  &:active {
    transform: translateY(-3px) scale(1.01);
    transition-duration: 0.1s;
  }
}

.create-cluster-content {
  padding: 32px 24px;
  text-align: center;
  height: 100%;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  position: relative;
  z-index: 1;
}

.create-icon {
  width: 64px;
  height: 64px;
  margin: 0 auto 20px;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, rgba(0, 122, 255, 0.1) 0%, rgba(0, 122, 255, 0.05) 100%);
  border-radius: 20px;
  transition: all 0.3s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  
  svg {
    width: 32px;
    height: 32px;
    color: #007aff;
    transition: all 0.3s ease;
  }
}

.create-title {
  font-size: 20px;
  font-weight: 700;
  color: #2c2c2c;
  margin: 0 0 8px 0;
  transition: color 0.3s ease;
}

.create-description {
  font-size: 14px;
  color: #666666;
  margin: 0 0 24px 0;
  line-height: 1.5;
}

.create-features {
  display: flex;
  flex-direction: column;
  gap: 8px;
  width: 100%;
}

.feature-item {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 13px;
  color: #555555;
  font-weight: 500;
  transition: all 0.3s ease;
  
  svg {
    width: 14px;
    height: 14px;
    color: #4caf50;
    flex-shrink: 0;
    transition: color 0.3s ease;
  }
}

/* 集群卡片 - 现代简约风格 */
.macos-cluster-card {
  background: rgba(255, 255, 255, 0.9);
  backdrop-filter: blur(16px);
  -webkit-backdrop-filter: blur(16px);
  border-radius: 12px;
  border: 1px solid rgba(0, 0, 0, 0.08);
  transition: all 0.25s cubic-bezier(0.25, 0.46, 0.45, 0.94);
  overflow: visible;
  position: relative;
  min-height: 260px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.04);
  
  &.cluster-card {
    &::after {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      right: 0;
      bottom: 0;
      background: linear-gradient(135deg, 
        rgba(255, 255, 255, 0.1) 0%, 
        rgba(255, 255, 255, 0.05) 100%);
      opacity: 0;
      transition: opacity 0.3s ease;
      pointer-events: none;
    }
    
    &:hover {
      transform: translateY(-6px) scale(1.01);
      box-shadow: 
        0 16px 48px rgba(0, 0, 0, 0.15),
        0 8px 24px rgba(0, 0, 0, 0.08);
      border-color: rgba(0, 122, 255, 0.25);
      background: rgba(255, 255, 255, 0.98);
      
      &::after {
        opacity: 1;
      }
      
      .cluster-icon {
        transform: scale(1.05) rotate(2deg);
        
        img, svg {
          filter: brightness(1.1) saturate(1.2);
        }
      }
      
      .cluster-name {
        color: #007aff;
      }
      
      .status-indicator {
        transform: scale(1.2);
        
        &.running {
          box-shadow: 0 0 12px rgba(76, 175, 80, 0.5);
        }
        
        &.error {
          box-shadow: 0 0 12px rgba(244, 67, 54, 0.5);
        }
        
        &.configured {
          box-shadow: 0 0 12px rgba(255, 152, 0, 0.5);
        }
      }
      
      .type-badge {
        transform: translateY(-1px);
        
        &.linux-type {
          box-shadow: 0 2px 8px rgba(255, 138, 101, 0.2);
        }
        
        &.k8s-type {
          box-shadow: 0 2px 8px rgba(66, 165, 245, 0.2);
        }
        
        &.default-type {
          box-shadow: 0 2px 8px rgba(144, 164, 174, 0.2);
        }
      }
      
      .info-value {
        color: #007aff;
      }
      
      .card-actions {
        transform: translateY(-2px);
      }
    }
    
    &:active {
      transform: translateY(-3px) scale(1.005);
      transition-duration: 0.1s;
    }
    
    &.linux-type {
      border-left: 4px solid #ff8a65;
      
      &:hover {
        border-color: rgba(255, 138, 101, 0.3);
        box-shadow: 0 8px 24px rgba(255, 138, 101, 0.1);
      }
    }
    
    &.k8s-type {
      border-left: 4px solid #42a5f5;
      
      &:hover {
        border-color: rgba(66, 165, 245, 0.3);
        box-shadow: 0 8px 24px rgba(66, 165, 245, 0.1);
      }
    }
    
    &.default-type {
      border-left: 4px solid #90a4ae;
      
      &:hover {
        border-color: rgba(144, 164, 174, 0.3);
        box-shadow: 0 8px 24px rgba(144, 164, 174, 0.1);
      }
    }
  }
}

/* 集群卡片内容 */
.cluster-card-content {
  padding: 20px;
  height: 100%;
  display: flex;
  flex-direction: column;
}

.card-header {
  margin-bottom: 14px;
  padding: 18px 20px 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.03);
  background: linear-gradient(135deg, rgba(250, 251, 252, 0.6) 0%, rgba(248, 249, 250, 0.4) 100%);
  
  .cluster-info {
    display: flex;
    align-items: flex-start;
    gap: 14px;
  }
  
  .cluster-icon {
    width: 42px;
    height: 42px;
    display: flex;
    align-items: center;
    justify-content: center;
    flex-shrink: 0;
    
    img {
      width: 36px;
      height: 36px;
      object-fit: contain;
    }
    
    svg {
      width: 32px;
      height: 32px;
      color: #666666;
    }
  }
  
  .cluster-details {
    flex: 1;
    min-width: 0;
  }
  
  .cluster-name {
    font-size: 18px;
    font-weight: 600;
    color: #2c2c2c;
    margin: 0 0 6px 0;
    word-break: break-word;
    line-height: 1.3;
  }
  
  .cluster-status {
    display: flex;
    align-items: center;
    gap: 6px;
    margin-bottom: 6px;
  }
  
  .cluster-type {
    margin-top: 4px;
    
    .type-badge {
      display: inline-block;
      padding: 4px 10px;
      border-radius: 6px;
      font-size: 11px;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.3px;
      
      &.linux-type {
        background: rgba(255, 138, 101, 0.08);
        color: #ff8a65;
        border: 1px solid rgba(255, 138, 101, 0.15);
      }
      
      &.k8s-type {
        background: rgba(66, 165, 245, 0.08);
        color: #42a5f5;
        border: 1px solid rgba(66, 165, 245, 0.15);
      }
      
      &.default-type {
        background: rgba(144, 164, 174, 0.08);
        color: #90a4ae;
        border: 1px solid rgba(144, 164, 174, 0.15);
      }
    }
  }
  
  .status-indicator {
    width: 7px;
    height: 7px;
    border-radius: 50%;
    box-shadow: 0 0 0 2px rgba(255, 255, 255, 0.8);
    
    &.running {
      background: #4caf50;
      box-shadow: 0 0 6px rgba(76, 175, 80, 0.3);
    }
    
    &.error {
      background: #f44336;
      box-shadow: 0 0 6px rgba(244, 67, 54, 0.3);
    }
    
    &.configured {
      background: #ff9800;
      box-shadow: 0 0 6px rgba(255, 152, 0, 0.3);
    }
  }
  
  .status-text {
    font-size: 12px;
    color: #757575;
    font-weight: 500;
  }
}

.card-body {
  flex: 1;
  margin-bottom: 16px;
  padding: 0 20px 16px;
  
  .info-grid {
    display: grid;
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .info-item {
    display: flex;
    flex-direction: column;
    gap: 3px;
    
    .info-label {
      font-size: 12px;
      color: #888888;
      font-weight: 500;
      text-transform: uppercase;
      letter-spacing: 0.6px;
      line-height: 1.2;
    }
    
    .info-value {
      font-size: 13px;
      color: #333333;
      font-weight: 600;
      word-break: break-all;
      line-height: 1.2;
    }
  }
}

.card-actions {
  display: flex;
  flex-direction: column;
  gap: 12px;
  padding: 16px 20px 20px;
  border-top: 1px solid rgba(0, 0, 0, 0.04);
  background: linear-gradient(135deg, rgba(248, 250, 252, 0.8) 0%, rgba(241, 245, 249, 0.6) 100%);
  position: relative;
  z-index: 10;
  
  .primary-actions {
    width: 100%;
    
    .macos-button {
      width: 100%;
      font-size: 13px;
      padding: 10px 16px;
      font-weight: 600;
      height: 36px;
      border-radius: 8px;
      transition: all 0.2s ease;
      
      &.primary {
        background: linear-gradient(135deg, #007aff 0%, #0056cc 100%);
        border: 1px solid rgba(0, 122, 255, 0.2);
        color: white;
        box-shadow: 0 2px 8px rgba(0, 122, 255, 0.25);
        
        &:hover:not(:disabled) {
          background: linear-gradient(135deg, #0056cc 0%, #003d99 100%);
          transform: translateY(-1px);
          box-shadow: 0 4px 12px rgba(0, 122, 255, 0.35);
        }
        
        &:disabled {
          background: rgba(0, 0, 0, 0.06);
          color: rgba(0, 0, 0, 0.3);
          border-color: rgba(0, 0, 0, 0.04);
          box-shadow: none;
          cursor: not-allowed;
        }
      }
    }
  }
  
  .secondary-actions {
    display: grid;
    grid-template-columns: 1fr 1fr auto;
    gap: 8px;
    align-items: center;
    
    .macos-button {
      font-size: 12px;
      padding: 8px 12px;
      height: 32px;
      border-radius: 6px;
      font-weight: 500;
      
      &.secondary {
        background: rgba(255, 255, 255, 0.9);
        border: 1px solid rgba(0, 0, 0, 0.08);
        color: #333333;
        backdrop-filter: blur(10px);
        
        &:hover {
          background: rgba(255, 255, 255, 1);
          border-color: rgba(0, 122, 255, 0.15);
          color: #007aff;
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
        }
      }
      
      &.icon-only {
        width: 32px;
        padding: 6px;
        background: rgba(255, 255, 255, 0.8);
        border: 1px solid rgba(0, 0, 0, 0.06);
        color: #86868b;
        
        &:hover {
          background: rgba(255, 255, 255, 1);
          border-color: rgba(0, 122, 255, 0.15);
          color: #007aff;
          transform: translateY(-1px);
          box-shadow: 0 2px 6px rgba(0, 0, 0, 0.08);
        }
        
        svg {
          width: 14px;
          height: 14px;
        }
      }
    }
  }
  
  .more-actions {
    position: relative;
    z-index: 100;
    
    .dropdown-trigger {
      background: rgba(255, 255, 255, 0.8);
      border: 1px solid rgba(0, 0, 0, 0.06);
      padding: 6px;
      border-radius: 6px;
      cursor: pointer;
      display: flex;
      align-items: center;
      justify-content: center;
      transition: all 0.2s ease;
      backdrop-filter: blur(10px);
      width: 32px;
      height: 32px;
      
      &:hover {
        background: rgba(255, 255, 255, 1);
        border-color: rgba(0, 122, 255, 0.15);
        transform: translateY(-1px);
        box-shadow: 0 3px 8px rgba(0, 0, 0, 0.06);
      }
      
      svg {
        width: 12px;
        height: 12px;
        color: #86868b;
      }
    }
    
    .dropdown-menu {
      position: absolute;
      top: 100%;
      right: 0;
      margin-top: 4px;
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(16px);
      border: 1px solid rgba(0, 0, 0, 0.08);
      border-radius: 8px;
      box-shadow: 0 8px 24px rgba(0, 0, 0, 0.12);
      min-width: 120px;
      z-index: 1000;
      overflow: hidden;
      
      .dropdown-item {
        display: block;
        width: 100%;
        padding: 8px 12px;
        border: none;
        background: none;
        text-align: left;
        font-size: 13px;
        color: #333333;
        cursor: pointer;
        transition: all 0.15s ease;
        font-weight: 500;
        
        &:hover {
          background: rgba(0, 122, 255, 0.08);
          color: #007aff;
        }
        
        &:first-child {
          border-top-left-radius: 8px;
          border-top-right-radius: 8px;
        }
        
        &:last-child {
          border-bottom-left-radius: 8px;
          border-bottom-right-radius: 8px;
        }
        
        .item-icon {
          width: 14px;
          height: 14px;
          margin-right: 8px;
          flex-shrink: 0;
        }
        
        &.danger {
          color: #ff3b30;
          
          &:hover {
            background: rgba(255, 59, 48, 0.08);
          }
        }
        
        &:disabled {
          color: #999999;
          cursor: not-allowed;
          
          &:hover {
            background: none;
          }
        }
      }
    }
  }
}

/* Modal 样式 - 苹果风格设计 */
/deep/ .macos-modal {
  .ant-modal {
    top: 50%;
    transform: translateY(-50%);
    
    .ant-modal-content {
      border-radius: 24px;
      overflow: hidden;
      box-shadow: 
        0 32px 80px rgba(0, 0, 0, 0.12),
        0 16px 40px rgba(0, 0, 0, 0.08),
        0 8px 20px rgba(0, 0, 0, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.2);
      background: rgba(255, 255, 255, 0.95);
      backdrop-filter: blur(40px);
      -webkit-backdrop-filter: blur(40px);
      position: relative;
      
      &::before {
        content: '';
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
        bottom: 0;
        background: linear-gradient(135deg, 
          rgba(255, 255, 255, 0.8) 0%, 
          rgba(255, 255, 255, 0.4) 50%, 
          rgba(255, 255, 255, 0.6) 100%);
        border-radius: inherit;
        pointer-events: none;
        z-index: -1;
      }
    }
    
    .ant-modal-header {
      background: linear-gradient(135deg, 
        rgba(255, 255, 255, 0.9) 0%, 
        rgba(248, 250, 252, 0.85) 100%);
      border-bottom: 1px solid rgba(0, 0, 0, 0.06);
      padding: 28px 32px 24px;
      border-radius: 24px 24px 0 0;
      
      .ant-modal-title {
        font-size: 22px;
        font-weight: 700;
        color: #1a1a1a;
        font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', sans-serif;
        letter-spacing: -0.02em;
      }
    }
    
    .ant-modal-body {
      padding: 28px 32px;
      background: rgba(255, 255, 255, 0.6);
    }
    
    .ant-modal-footer {
      padding: 24px 32px 28px;
      border-top: 1px solid rgba(0, 0, 0, 0.06);
      background: linear-gradient(135deg, 
        rgba(248, 250, 252, 0.9) 0%, 
        rgba(255, 255, 255, 0.8) 100%);
      border-radius: 0 0 24px 24px;
    }
    
    .ant-modal-close {
      top: 24px;
      right: 24px;
      width: 40px;
      height: 40px;
      border-radius: 8px;
      background: rgba(0, 0, 0, 0.04);
      border: none;
      transition: all 0.2s ease;
      
      &:hover {
        background: rgba(0, 0, 0, 0.08);
      }
      
      .ant-modal-close-x {
        width: 36px;
        height: 36px;
        line-height: 36px;
        font-size: 18px;
        color: #757575;
        transition: all 0.15s ease;
      }
    }
  }
}

.modal-content {
  padding: 0;
}

/* 响应式设计 */
@media (max-width: 768px) {
  .macos-cluster-container {
    padding: 16px;
  }
  
  .macos-cluster-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .macos-cluster-card {
    .cluster-card-content {
      padding: 20px;
    }
    
    .card-header {
      .cluster-info {
        gap: 12px;
      }
      
      .cluster-icon {
        width: 40px;
        height: 40px;
      }
      
      .cluster-name {
        font-size: 16px;
      }
    }
    
    .card-actions {
      padding: 12px 16px 16px;
      gap: 10px;
      
      .primary-actions {
        .macos-button {
          height: 34px;
          font-size: 12px;
          padding: 8px 14px;
        }
      }
      
      .secondary-actions {
        grid-template-columns: 1fr 1fr;
        gap: 6px;
        
        .more-actions {
          grid-column: span 2;
          display: flex;
          justify-content: center;
        }
        
        .macos-button {
          height: 30px;
          font-size: 11px;
          padding: 6px 10px;
          
          &.icon-only {
            width: 30px;
            padding: 5px;
          }
        }
      }
    }
  }
}

/* 授权弹窗专用样式 - 完全重构的苹果风格 */
/deep/ .apple-auth-modal {
  /* 覆盖全局下拉选项样式 */
  .ant-select-dropdown {
    border-radius: var(--apple-radius-large) !important;
    background: rgba(255, 255, 255, 0.98) !important;
    backdrop-filter: blur(20px) !important;
    box-shadow: var(--apple-shadow-large) !important;
    border: 1px solid rgba(0, 0, 0, 0.08) !important;
    padding: 12px 0 !important;
    animation: apple-dropdown-fade-in 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
    
    .ant-select-item {
      margin: 4px 12px !important;
      padding: 12px 16px !important;
      border-radius: var(--apple-radius-medium) !important;
      transition: all 0.2s cubic-bezier(0.4, 0, 0.2, 1) !important;
      font-size: 15px !important;
      font-weight: 400 !important;
      color: var(--apple-text-primary) !important;
      background: transparent !important;
      
      &:hover {
        background: var(--apple-blue-light-bg) !important;
        color: var(--apple-blue) !important;
        transform: translateX(4px) !important;
      }
      
      &.ant-select-item-option-selected {
        background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-blue-light) 100%) !important;
        color: white !important;
        font-weight: 600 !important;
        box-shadow: var(--apple-shadow-small) !important;
        
        &:hover {
          background: linear-gradient(135deg, var(--apple-blue-dark) 0%, var(--apple-blue) 100%) !important;
          transform: translateX(4px) scale(1.02) !important;
        }
      }
      
      &.ant-select-item-option-active {
        background: var(--apple-blue-light-bg) !important;
        color: var(--apple-blue) !important;
      }
      
      /* 用户选项特殊样式 */
      .user-option {
        display: flex !important;
        align-items: center !important;
        gap: 12px !important;
        
        .user-avatar {
          width: 32px !important;
          height: 32px !important;
          border-radius: 50% !important;
          background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-blue-light) 100%) !important;
          display: flex !important;
          align-items: center !important;
          justify-content: center !important;
          color: white !important;
          font-weight: 600 !important;
          font-size: 14px !important;
          box-shadow: var(--apple-shadow-small) !important;
          flex-shrink: 0 !important;
        }
        
        .user-name {
          font-size: 15px !important;
          font-weight: 500 !important;
          color: inherit !important;
          flex: 1 !important;
          overflow: hidden !important;
          text-overflow: ellipsis !important;
          white-space: nowrap !important;
        }
      }
    }
    
    /* 空状态样式 */
    .ant-empty {
      margin: 20px 0 !important;
      
      .ant-empty-description {
        color: var(--apple-text-tertiary) !important;
        font-size: 14px !important;
      }
    }
    
    /* 滚动条样式 */
    &::-webkit-scrollbar {
      width: 6px !important;
    }
    
    &::-webkit-scrollbar-track {
      background: transparent !important;
    }
    
    &::-webkit-scrollbar-thumb {
      background: rgba(0, 0, 0, 0.1) !important;
      border-radius: 3px !important;
      
      &:hover {
        background: rgba(0, 0, 0, 0.2) !important;
      }
    }
  }
  
  /* 下拉动画 */
  @keyframes apple-dropdown-fade-in {
    0% {
      opacity: 0;
      transform: translateY(-8px) scale(0.95);
    }
    100% {
      opacity: 1;
      transform: translateY(0) scale(1);
    }
  }
  
  /* 下拉箭头旋转动画 */
  .ant-select-open .ant-select-arrow {
    transform: rotate(180deg) !important;
  }
  .ant-modal {
    .ant-modal-content {
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.98);
      backdrop-filter: blur(40px);
      box-shadow: 
        0 32px 80px rgba(0, 0, 0, 0.12),
        0 16px 40px rgba(0, 0, 0, 0.08),
        0 8px 20px rgba(0, 0, 0, 0.04);
      border: 1px solid rgba(255, 255, 255, 0.2);
    }
    
    .ant-modal-header {
      background: transparent;
      border-bottom: none;
      padding: 0;
      
      .ant-modal-title {
        display: none; /* 隐藏默认标题，使用组件内部的标题 */
      }
    }
    
    .ant-modal-body {
      padding: 0;
      
      /* 重置所有可能影响的样式 */
      .apple-auth-container {
        padding: 32px;
      }
      
      /* 确保苹果风格样式不被覆盖 */
      .apple-select {
        border: 1px solid var(--apple-border) !important;
        border-radius: var(--apple-radius-large) !important;
        background: var(--apple-background) !important;
        min-height: 52px !important;
        
        &:hover {
          border-color: var(--apple-blue) !important;
          box-shadow: 0 0 0 1px var(--apple-blue) !important;
        }
        
        &.ant-select-focused {
          border-color: var(--apple-blue) !important;
          box-shadow: 0 0 0 3px rgba(0, 122, 255, 0.15) !important;
        }
        
        .ant-select-selector {
          border: none !important;
          background: transparent !important;
          box-shadow: none !important;
          padding: 12px 20px !important;
          min-height: 50px !important;
          border-radius: var(--apple-radius-large) !important;
          
          .ant-select-selection-placeholder {
            color: var(--apple-text-tertiary) !important;
            font-size: 15px !important;
            line-height: 26px !important;
            font-weight: 400 !important;
          }
          
          .ant-select-selection-item {
            background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-blue-light) 100%) !important;
            color: white !important;
            border: none !important;
            border-radius: var(--apple-radius-medium) !important;
            padding: 6px 14px !important;
            margin: 3px 6px 3px 0 !important;
            font-weight: 600 !important;
            font-size: 14px !important;
            height: auto !important;
            line-height: 1.4 !important;
            box-shadow: var(--apple-shadow-small) !important;
            
            .ant-select-selection-item-remove {
              color: rgba(255, 255, 255, 0.8) !important;
              font-size: 13px !important;
              margin-left: 8px !important;
              border-radius: 50% !important;
              width: 16px !important;
              height: 16px !important;
              display: flex !important;
              align-items: center !important;
              justify-content: center !important;
              transition: all 0.2s ease !important;
              
              &:hover {
                color: white !important;
                background: rgba(255, 255, 255, 0.2) !important;
              }
            }
          }
          
          .ant-select-selection-search {
            margin-left: 0 !important;
            
            .ant-select-selection-search-input {
              height: 26px !important;
              line-height: 26px !important;
              font-size: 15px !important;
            }
          }
        }
        
        .ant-select-arrow {
          color: var(--apple-text-secondary) !important;
          font-size: 16px !important;
          right: 20px !important;
          transition: all 0.2s ease !important;
          
          &:hover {
            color: var(--apple-blue) !important;
          }
        }
      }
      
      .apple-btn {
        border-radius: var(--apple-radius-medium) !important;
        font-weight: 600 !important;
        height: 44px !important;
        padding: 0 28px !important;
        font-size: 15px !important;
        border: none !important;
        transition: all 0.25s cubic-bezier(0.4, 0, 0.2, 1) !important;
        font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', sans-serif !important;
        letter-spacing: -0.01em !important;
        cursor: pointer !important;
        
        &.apple-btn-primary {
          background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-blue-light) 100%) !important;
          color: white !important;
          box-shadow: var(--apple-shadow-medium) !important;
          
          &:hover {
            background: linear-gradient(135deg, var(--apple-blue-dark) 0%, var(--apple-blue) 100%) !important;
            transform: translateY(-2px) !important;
            box-shadow: 0 8px 24px rgba(0, 122, 255, 0.4) !important;
          }
          
          &:active {
            transform: translateY(-1px) !important;
            box-shadow: var(--apple-shadow-medium) !important;
          }
          
          &.ant-btn-loading {
            background: linear-gradient(135deg, var(--apple-blue) 0%, var(--apple-blue-light) 100%) !important;
            
            .ant-btn-loading-icon {
              color: white !important;
            }
          }
        }
        
        &.apple-btn-secondary {
          background: var(--apple-background-secondary) !important;
          border: 1px solid var(--apple-border) !important;
          color: var(--apple-text-primary) !important;
          
          &:hover {
            background: var(--apple-gray-1) !important;
            border-color: var(--apple-gray-3) !important;
            color: var(--apple-text-primary) !important;
            transform: translateY(-2px) !important;
            box-shadow: var(--apple-shadow-small) !important;
          }
          
          &:active {
            transform: translateY(-1px) !important;
            background: var(--apple-gray-2) !important;
          }
        }
      }
      
      .ant-form-item-label {
        label {
          color: var(--apple-text-primary) !important;
          font-weight: 600 !important;
          font-size: 16px !important;
          font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'Helvetica Neue', sans-serif !important;
          letter-spacing: -0.01em !important;
        }
      }
    }
    
    .ant-modal-footer {
      display: none; /* 隐藏默认footer，使用组件内部的按钮 */
    }
    
    .ant-modal-close {
      top: 24px;
      right: 24px;
      width: 32px;
      height: 32px;
      border-radius: 50%;
      background: rgba(0, 0, 0, 0.05);
      backdrop-filter: blur(10px);
      transition: all 0.2s ease;
      
      &:hover {
        background: rgba(0, 0, 0, 0.1);
        transform: scale(1.1);
      }
      
      .ant-modal-close-x {
        width: 32px;
        height: 32px;
        line-height: 32px;
        font-size: 16px;
        color: var(--apple-text-secondary);
        
        &:hover {
          color: var(--apple-text-primary);
        }
      }
    }
  }
}

/* 创建/编辑集群弹窗专用样式 */
/deep/ .apple-create-modal {
  .ant-modal {
    .ant-modal-content {
      border-radius: 24px;
      background: rgba(255, 255, 255, 0.98);
      backdrop-filter: blur(40px);
      box-shadow: 
        0 32px 80px rgba(0, 0, 0, 0.12),
        0 16px 40px rgba(0, 0, 0, 0.08),
        0 8px 20px rgba(0, 0, 0, 0.04);
    }
    
    .ant-modal-header {
      background: linear-gradient(135deg, 
        rgba(52, 199, 89, 0.08) 0%, 
        rgba(255, 255, 255, 0.95) 100%);
      border-bottom: 1px solid rgba(52, 199, 89, 0.12);
      padding: 32px 36px 28px;
      
      .ant-modal-title {
        color: #248a3d;
        font-weight: 700;
        font-size: 24px;
        letter-spacing: -0.02em;
      }
    }
    
    .ant-modal-body {
      padding: 32px 36px;
      max-height: 70vh;
      overflow-y: auto;
      
      /* 自定义滚动条 */
      &::-webkit-scrollbar {
        width: 6px;
      }
      
      &::-webkit-scrollbar-track {
        background: rgba(0, 0, 0, 0.02);
        border-radius: 3px;
      }
      
      &::-webkit-scrollbar-thumb {
        background: rgba(52, 199, 89, 0.3);
        border-radius: 3px;
        
        &:hover {
          background: rgba(52, 199, 89, 0.5);
        }
      }
      
      .ant-form-item {
        margin-bottom: 24px;
        
        .ant-form-item-label {
          label {
            color: #248a3d;
            font-weight: 600;
            font-size: 16px;
          }
        }
        
        .ant-input, .ant-select {
          border-radius: 12px;
          border: 1.5px solid rgba(52, 199, 89, 0.2);
          background: rgba(255, 255, 255, 0.9);
          transition: all 0.3s ease;
          height: 44px;
          
          &:hover {
            border-color: rgba(52, 199, 89, 0.4);
            box-shadow: 0 4px 12px rgba(52, 199, 89, 0.1);
          }
          
          &:focus, &.ant-select-focused {
            border-color: #34c759;
            box-shadow: 0 0 0 3px rgba(52, 199, 89, 0.1);
          }
        }
        
        .ant-select {
          .ant-select-selector {
            border: none !important;
            background: transparent !important;
            box-shadow: none !important;
            height: 42px !important;
            padding: 8px 16px;
            
            .ant-select-selection-placeholder {
              color: rgba(52, 199, 89, 0.6);
              line-height: 26px;
            }
            
            .ant-select-selection-item {
              color: #248a3d;
              font-weight: 500;
              line-height: 26px;
            }
          }
        }
      }
      
      .ant-card {
        border-radius: 16px;
        border: 1px solid rgba(52, 199, 89, 0.15);
        background: rgba(255, 255, 255, 0.8);
        margin-bottom: 20px;
        
        .ant-card-head {
          background: linear-gradient(135deg, 
            rgba(52, 199, 89, 0.05) 0%, 
            rgba(255, 255, 255, 0.9) 100%);
          border-bottom: 1px solid rgba(52, 199, 89, 0.1);
          border-radius: 16px 16px 0 0;
          
          .ant-card-head-title {
            color: #248a3d;
            font-weight: 600;
            font-size: 18px;
          }
        }
        
        .ant-card-body {
          padding: 24px;
        }
      }
    }
    
    .ant-modal-footer {
      background: linear-gradient(135deg, 
        rgba(248, 250, 252, 0.95) 0%, 
        rgba(255, 255, 255, 0.9) 100%);
      border-top: 1px solid rgba(52, 199, 89, 0.08);
      padding: 28px 36px 32px;
      
      .ant-btn {
        border-radius: 12px;
        font-weight: 600;
        height: 44px;
        padding: 0 28px;
        font-size: 16px;
        
        &.ant-btn-primary {
          background: linear-gradient(135deg, #34c759 0%, #248a3d 100%);
          border: none;
          box-shadow: 0 4px 12px rgba(52, 199, 89, 0.3);
          
          &:hover {
            background: linear-gradient(135deg, #248a3d 0%, #1e7e34 100%);
            transform: translateY(-1px);
            box-shadow: 0 6px 16px rgba(52, 199, 89, 0.4);
          }
        }
        
        &.ant-btn-default {
          background: rgba(255, 255, 255, 0.8);
          border: 1.5px solid rgba(52, 199, 89, 0.2);
          color: #34c759;
          
          &:hover {
            background: rgba(52, 199, 89, 0.05);
            border-color: rgba(52, 199, 89, 0.4);
            color: #248a3d;
          }
        }
      }
    }
  }
}
</style>

<style scoped>
/* 确保这个样式在scoped style中添加 */
</style>

<style>
/* 全局样式覆盖 */
.apple-create-modal .ant-modal-content {
  border-radius: 16px;
  overflow: hidden;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15);
  border: 1px solid rgba(0, 0, 0, 0.05);
}

.apple-create-modal .ant-modal-header {
  background: #fff;
  border-bottom: 1px solid #f0f0f0;
  padding: 20px 24px;
}

.apple-create-modal .ant-modal-title {
  font-family: -apple-system, BlinkMacSystemFont, "SF Pro Display", "SF Pro Text", "PingFang SC", "Helvetica Neue", Helvetica, Arial, sans-serif;
  font-size: 18px;
  font-weight: 600;
  color: #000;
}

.apple-create-modal .ant-modal-body {
  padding: 0;
}

.apple-create-modal .ant-modal-close {
  top: 16px;
  right: 20px;
}

.apple-create-modal .ant-modal-close-x {
  width: 48px;
  height: 48px;
  line-height: 48px;
  font-size: 20px;
}

/* 选择框本身的圆角样式 */
.ant-select .ant-select-selector {
  border-radius: 12px !important;
  overflow: hidden;
}

/* 确保下拉箭头区域与框体融合 */
.ant-select .ant-select-arrow {
  right: 11px;
}

/* 确保多选模式下的选择框也是圆角的 */
.ant-select-multiple .ant-select-selector {
  border-radius: 12px !important;
  padding: 4px 8px !important;
}

/* 全局下拉菜单样式 */
.ant-select-dropdown {
  border-radius: 12px !important;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12) !important;
  padding: 6px !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  overflow: hidden !important;
}

/* 下拉菜单项容器 */
.ant-select-dropdown-menu,
.ant-select-dropdown ul {
  max-height: 240px !important;
  padding: 4px !important;
}

/* Ant Design Vue 1.7.2版本下拉菜单项样式 */
.ant-select-dropdown-menu-item,
.ant-select-dropdown-menu-item-selected,
.ant-select-dropdown-menu-item-active {
  border-radius: 8px !important;
  padding: 8px 12px !important;
  transition: background 0.2s !important;
  margin: 4px 0 !important;
}

/* 下拉菜单项悬停和选中效果 */
.ant-select-dropdown-menu-item:hover {
  background-color: #f0f7ff !important;
}

.ant-select-dropdown-menu-item-selected {
  background-color: rgba(10, 132, 255, 0.1) !important;
  color: #0A84FF !important;
  font-weight: 600 !important;
}

/* 修复下拉菜单在屏幕外的问题 */
.ant-select-dropdown {
  position: fixed !important;
}

/* 美化表单样式 */
.ant-form-item-required::before {
  display: none !important;
}

.ant-form-item-label > label.ant-form-item-required::after {
  display: inline-block !important;
  margin-left: 4px;
  content: '必填';
  font-size: 12px;
  line-height: 1;
  padding: 1px 5px;
  background-color: rgba(10, 132, 255, 0.1);
  color: #0A84FF;
  border-radius: 4px;
  font-weight: normal;
}

/* 圆角输入框和选择框 */
.ant-input,
.ant-btn {
  border-radius: 12px !important;
}

.ant-input:focus, 
.ant-input-focused {
  border-color: #0A84FF !important;
  box-shadow: 0 0 0 3px rgba(10, 132, 255, 0.1) !important;
}

.ant-input:hover, 
.ant-select:not(.ant-select-disabled):hover .ant-select-selector {
  border-color: #0A84FF !important;
}

/* 强制应用样式到下拉框选项上 */
.ant-select-dropdown-menu .ant-select-dropdown-menu-item {
  border-radius: 8px !important;
}
</style>