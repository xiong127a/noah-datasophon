<template>
  <div class="modern-cluster-container">
    <!-- 页面头部横幅 -->
    <div class="page-header">
      <div class="header-content">
        <div class="page-title">集群管理</div>
        <div class="page-description">管理和监控您的大数据集群，快速部署各类服务</div>
      </div>
    </div>

    <!-- 集群卡片网格 -->
    <div class="cluster-grid">
      <!-- 现有集群卡片 -->
      <div 
        v-for="(item, index) in filteredDataSource" 
        :key="index"
        :class="['cluster-card', getClusterTypeClass(item.depType)]"
      >
        <!-- 集群状态标签 -->
        <div :class="['status-badge', getStatusClass(item.clusterStateCode)]">
          {{ item.clusterState }}
        </div>

        <!-- 集群头部 -->
          <div class="card-header">
          <div class="cluster-icon">
                <img v-if="item.depType === 'PVM'" src="~@/assets/img/os-logos/linux-tux.svg" alt="Linux" />
                <img v-else-if="item.depType === 'Kubernetes'" src="~@/assets/images/kubernetes-logo.svg" alt="Kubernetes" />
            <svg-icon v-else icon-class="colony" />
              </div>
          <div class="cluster-info">
                <h3 class="cluster-name">{{ item.clusterName }}</h3>
            <div class="cluster-meta">
              <span class="cluster-type">{{ getClusterTypeText(item.depType) }}</span>
              <span class="cluster-date">{{ formatDate(item.createTime) }}</span>
              </div>
            </div>
          </div>

        <!-- 集群内容 -->
          <div class="card-body">
          <div class="info-row">
            <div class="info-label">管理员</div>
            <div class="info-value">{{ item.userManageName || '未分配' }}</div>
            </div>
          </div>

        <!-- 集群操作 -->
        <div class="card-footer">
          <div class="mac-button-group" ref="buttonContainer">
            <!-- 进入集群按钮独占一行 -->
            <a-button 
              type="primary" 
              class="mac-btn primary-btn"
              @click="getInto(item)" 
                :disabled="item.clusterStateCode === 1"
              block
            >
              <a-icon type="login" />
              <span>进入集群</span>
            </a-button>
            
            <!-- 第二行按钮 - 均匀分布 -->
            <div class="secondary-buttons">
              <a-button 
                class="mac-btn"
                @click="addColony(item)" 
                :disabled="item.clusterStateCode === 2"
              >
                <a-icon type="edit" />
                <span>编辑</span>
              </a-button>
              <a-button 
                v-if="user && user.userType === 1"
                class="mac-btn"
                @click="authCluster(item)"
              >
                <a-icon type="safety" />
                <span>授权</span>
              </a-button>
              <a-dropdown :trigger="['click']" placement="bottomRight" overlayClassName="mac-dropdown-overlay">
                <a-button class="mac-btn more-btn">
                  <a-icon type="ellipsis" />
                  <span>更多</span>
                </a-button>
                <a-menu slot="overlay" class="mac-dropdown-menu">
                  <a-menu-item @click="configCluster(item)" :disabled="item.clusterStateCode === 2">
                    <a-icon type="setting" />
                    <span>配置集群</span>
                  </a-menu-item>
                  <a-menu-item @click="delectColony(item)" :disabled="item.clusterStateCode === 2" class="danger">
                    <a-icon type="delete" />
                    <span>删除集群</span>
                  </a-menu-item>
                </a-menu>
              </a-dropdown>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 创建新集群卡片 - 放在最后位置 -->
      <div class="cluster-card create-card" @click="addColony({})">
        <div class="create-card-inner">
          <div class="create-card-content">
          <div class="create-icon">
              <a-icon type="plus" />
          </div>
          <h3 class="create-title">创建新集群</h3>
            <p class="create-desc">部署新的大数据集群环境</p>
          <div class="create-features">
            <div class="feature-item">
                <a-icon type="check-circle" theme="filled" />
              <span>一键部署</span>
            </div>
            <div class="feature-item">
                <a-icon type="check-circle" theme="filled" />
              <span>智能配置</span>
            </div>
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
    >
      <div class="modal-content">
        <Steps :clusterId="clusterId" :depType="depType" />
      </div>
    </a-modal>

    <!-- 使用a-modal代替$confirm -->
    <a-modal
      v-model="authModalVisible"
      :footer="null"
      :closable="false"
      :maskClosable="false"
      centered
      destroyOnClose
      :width="450"
      :bodyStyle="{ padding: 0 }"
      class="clean-modal auth-modal auth-cluster-modal"
    >
      <AuthCluster 
        v-if="currentClusterForAuth" 
        :detail="currentClusterForAuth" 
        :callBack="handleAuthComplete" 
        @cancel="handleAuthModalClose"
      />
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

  components: {
    Steps,
    AuthCluster
  },

  data() {
    return {
      visible: false,
      dataSource: [],
      confirmLoading: false,
      clusterId: "", // 操作的集群Id
      depType: "",
      activeDropdown: null,
      dropdownPositions: {}, // 存储每个下拉菜单的位置
      authModalVisible: false,
      currentClusterForAuth: null,
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
    // 确保样式一致性
    document.body.classList.add('colony-manage-page');
    this.ensureStyleConsistency();
    
    // 添加点击外部关闭下拉菜单事件
    document.addEventListener('click', this.closeDropdownOnOutsideClick);
  },
  
  beforeDestroy() {
    // 移除样式标记和事件监听
    document.body.classList.remove('colony-manage-page');
    document.removeEventListener('click', this.closeDropdownOnOutsideClick);
  },

  methods: {
    ...mapMutations("setting", ["setIsCluster", "setMenuData", "setClusterId"]),
    
    // 打开/关闭下拉菜单
    toggleDropdown(index, event) {
      // 阻止事件冒泡，这样点击事件不会传递到document上
      event.stopPropagation();
      
      if (this.activeDropdown === index) {
        this.activeDropdown = null;
      } else {
        this.activeDropdown = index;
        // 设置下拉菜单位置
        this.$nextTick(() => {
          const button = event.target.closest('.more-btn');
          if (button) {
            const buttonRect = button.getBoundingClientRect();
            // 保存位置信息到索引对应的位置
            this.dropdownPositions[index] = {
              right: '0px',
              top: buttonRect.height + 'px'
            };
          }
        });
      }
    },
    
    // 获取下拉菜单样式
    getDropdownStyle(index) {
      return this.dropdownPositions[index] || { right: '0px', top: '32px' };
    },
    
    // 点击外部关闭下拉菜单
    closeDropdownOnOutsideClick(event) {
      if (this.activeDropdown !== null && !event.target.closest('.dropdown-wrapper')) {
        this.activeDropdown = null;
      }
    },
    
    // 确保样式一致性，解决刷新页面样式变化问题
    ensureStyleConsistency() {
      // 强制重新计算样式
      setTimeout(() => {
        const cards = document.querySelectorAll('.cluster-card');
        cards.forEach(card => {
          card.style.display = 'none';
          setTimeout(() => {
            card.style.display = '';
          }, 0);
        });
      }, 100);
    },
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
      let width = 800;
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
        okButtonProps: { style: { display: 'none' } },
        cancelButtonProps: { style: { display: 'none' } },
        maskClosable: false,
        centered: true,
        destroyOnClose: true,
        bodyStyle: { 
          padding: 0, 
          maxHeight: 'calc(100vh - 200px)', 
          overflow: 'auto' 
        },
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
      // 关闭下拉菜单
      this.activeDropdown = null;
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
      });
    },
    // 集群授权
    authCluster(obj) {
      this.currentClusterForAuth = obj;
      this.authModalVisible = true;
    },
    
    handleAuthModalClose() {
      this.authModalVisible = false;
      this.currentClusterForAuth = null;
    },
    
    handleAuthComplete() {
      this.getColonyList();
      this.handleAuthModalClose();
    },
    // 独立方法，便于多次调用
    removeQuestionIcons() {
      // 定位所有问号图标，包括SVG
      const iconSelectors = [
        '.auth-cluster-modal .anticon-question-circle',
        '.auth-cluster-modal i.anticon',
        '.auth-cluster-modal svg[data-icon="question-circle"]',
        '.auth-cluster-modal .ant-modal-confirm-title + i',
        '.auth-cluster-modal .ant-modal-confirm-body i',
        '.auth-cluster-modal .ant-modal-header i',
        '.auth-cluster-modal .ant-modal-body i.anticon',
        '.auth-cluster-modal .anticon',
      ];
      
      iconSelectors.forEach(selector => {
        const icons = document.querySelectorAll(selector);
        icons.forEach(icon => {
          if (icon && icon.parentNode) {
            // 1. 首先将其隐藏
            icon.style.display = 'none';
            icon.style.visibility = 'hidden';
            icon.style.width = '0';
            icon.style.height = '0';
            icon.style.position = 'absolute';
            icon.style.top = '-9999px';
            icon.style.left = '-9999px';
            
            // 2. 然后尝试从DOM中移除
            try {
              icon.parentNode.removeChild(icon);
            } catch (e) {
              console.log('移除图标失败，但已隐藏');
            }
          }
        });
      });
    },
    // 配置集群
    configCluster(row) {
      this.clusterId = row.id;
      this.setClusterId(row.id);
      this.visible = true;
      this.depType = row.depType;
      // 关闭下拉菜单
      this.activeDropdown = null;
    },
    handleCancel(e) {
      this.visible = false;
      this.getColonyList();
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
.modern-cluster-container {
  padding: 24px;
  min-height: 100vh;
  background-color: #f6f6f6; // 苹果风格浅灰色背景
  font-family: -apple-system, BlinkMacSystemFont, 'SF Pro Display', 'SF Pro Text', 'Helvetica Neue', Helvetica, Arial, sans-serif;
}

/* 页面头部样式 - 苹果风格 */
.page-header {
  background: rgba(255, 255, 255, 0.8);
  padding: 32px;
  border-radius: 16px;
  margin-bottom: 24px;
  border: none;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  
  .header-content {
    .page-title {
      font-size: 28px;
      font-weight: 600;
      color: #1d1d1f; // 苹果典型标题色
      margin: 0 0 8px 0;
      letter-spacing: -0.025em;
    }

    .page-description {
      color: #86868b; // 苹果次要文本色
      margin: 0;
      font-size: 16px;
      line-height: 1.4;
    }
  }
}

.cluster-grid {
  display: grid;
  grid-template-columns: repeat(auto-fill, minmax(350px, 1fr));
  gap: 24px;
}

/* 集群卡片 - 苹果设计风格 */
.cluster-card {
  position: relative;
  background: rgba(255, 255, 255, 0.8); // 半透明背景
  border-radius: 16px;
  box-shadow: 0 2px 10px rgba(0, 0, 0, 0.05);
  overflow: hidden;
  transition: all 0.35s cubic-bezier(0.25, 0.1, 0.25, 1.0);
  border: none;
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  
  &:hover {
    transform: translateY(-6px) scale(1.01);
    box-shadow: 0 12px 28px rgba(0, 0, 0, 0.12), 0 2px 4px rgba(0, 0, 0, 0.05);
  }
  
  &.linux-type {
  &::before {
    content: '';
    position: absolute;
    top: 0;
    left: 0;
      height: 100%;
      width: 4px;
      background: linear-gradient(to bottom, #ff9500, #ff2d55);
    }
  }
  
  &.k8s-type {
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
  height: 100%;
      width: 4px;
      background: linear-gradient(to bottom, #007aff, #5ac8fa);
    }
  }
  
  &.default-type {
    &::before {
      content: '';
      position: absolute;
      top: 0;
      left: 0;
      height: 100%;
      width: 4px;
      background: linear-gradient(to bottom, #8e8e93, #aeaeb2);
    }
  }
}

.status-badge {
  position: absolute;
  top: 16px;
  right: 16px;
  padding: 4px 10px;
  border-radius: 20px;
  font-size: 12px;
  font-weight: 500;
  backdrop-filter: blur(10px);
  -webkit-backdrop-filter: blur(10px);
  z-index: 2;
        
        &.running {
    background: rgba(52, 199, 89, 0.15);
    color: #28a745;
        }
        
        &.error {
    background: rgba(255, 59, 48, 0.15);
    color: #ff3b30;
        }
        
        &.configured {
    background: rgba(255, 149, 0, 0.15);
    color: #ff9500;
      }
}

.card-header {
  padding: 24px;
    display: flex;
  align-items: center;
  gap: 16px;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  }
  
  .cluster-icon {
  width: 40px;
  height: 40px;
    display: flex;
    align-items: center;
    justify-content: center;
  background: transparent;
  padding: 0;
    
    img {
      width: 36px;
      height: 36px;
      object-fit: contain;
    }
  }
  
.cluster-info {
    flex: 1;
    min-width: 0;
  }
  
  .cluster-name {
  margin: 0 0 6px;
    font-size: 18px;
    font-weight: 600;
  color: #1d1d1f;
    line-height: 1.3;
  letter-spacing: -0.01em;
  }
  
.cluster-meta {
    display: flex;
  flex-wrap: wrap;
  gap: 10px;
  font-size: 13px;
  color: #86868b;
  }
  
  .cluster-type {
  padding: 2px 10px;
  border-radius: 10px;
  font-size: 12px;
  background: rgba(0, 0, 0, 0.04);
  color: #1d1d1f;
      font-weight: 500;
}

.cluster-date {
  color: #86868b;
}

.card-body {
  padding: 14px 24px;
  background: transparent;
  border-bottom: 1px solid rgba(0, 0, 0, 0.05);
  }
  
.info-row {
    display: flex;
  align-items: center;
  margin-bottom: 0;
    
    .info-label {
    width: auto;
    margin-right: 10px;
    color: #86868b;
    font-size: 14px;
      font-weight: 500;
    }
    
    .info-value {
    flex: 1;
    min-width: 0;
    font-size: 14px;
    color: #1d1d1f;
    font-weight: 500;
    word-break: break-word;
    padding: 5px 10px;
    background: rgba(0, 0, 0, 0.025);
    border-radius: 6px;
  }
}

/* 卡片操作区域 */
.card-footer {
  padding: 16px 24px 20px;
  border-top: none;
  display: flex;
  justify-content: flex-start;
  background: transparent;
  margin-top: auto; /* 将按钮推到卡片底部 */
}

/* Mac风格按钮组 */
.mac-button-group {
  display: flex;
  flex-direction: column;
  gap: 12px;
    width: 100%;
}
    
/* 二级按钮组 */
.secondary-buttons {
  display: flex;
      width: 100%;
  gap: 8px;
  
  .mac-btn {
    flex: 1; /* 让按钮均匀分布 */
  }
}

/* 重置 Ant Design 按钮样式 */
:deep(.mac-btn) {
      height: 36px;
  border: 1px solid #e6e6e6;
  background: #ffffff;
  color: #333333;
      border-radius: 8px;
  font-size: 14px;
  font-weight: 500;
  box-shadow: 0 1px 2px rgba(0, 0, 0, 0.05);
  display: inline-flex;
  align-items: center;
  justify-content: center;
      
  &.ant-btn-primary, &.primary-btn {
    background: linear-gradient(to bottom, #1890ff, #096dd9);
        color: white;
    border: none;
    box-shadow: 0 2px 4px rgba(24, 144, 255, 0.3);
        
    &:hover, &:focus {
      background: linear-gradient(to bottom, #40a9ff, #1890ff);
      color: white;
    }
    
    &:active {
      background: linear-gradient(to bottom, #096dd9, #0050b3);
      box-shadow: inset 0 2px 4px rgba(0, 0, 0, 0.1);
        }
        
    &[disabled] {
      background: #f5f5f5;
      color: rgba(0, 0, 0, 0.25);
      border: 1px solid #d9d9d9;
          box-shadow: none;
        }
      }
  
  &:hover, &:focus {
    background: #fafafa;
    color: #1890ff;
    border-color: #1890ff;
  }
  
  &:active {
    background: #f0f0f0;
    color: #0050b3;
    border-color: #0050b3;
      }
      
  &[disabled] {
    background: #f5f5f5;
    color: rgba(0, 0, 0, 0.25);
    border-color: #d9d9d9;
  }
  
  .anticon {
    font-size: 14px;
    margin-right: 6px;
        }
        
  &.more-btn {
    min-width: auto; /* 取消最小宽度限制 */
        }
      }

/* 下拉菜单包装器 */
.dropdown-wrapper {
    position: relative;
  display: inline-block;
      }
      
/* 下拉菜单 */
    .dropdown-menu {
      position: absolute;
      right: 0;
  top: 100%;
      margin-top: 4px;
  background: white;
  border-radius: 4px;
  box-shadow: 0 3px 6px rgba(0, 0, 0, 0.15);
  min-width: 140px;
      z-index: 1000;
  border: 1px solid #eee;
      overflow: hidden;
}
      
      .dropdown-item {
        padding: 8px 12px;
  display: flex;
  align-items: center;
        cursor: pointer;
  transition: background 0.2s;
  
  .anticon {
    font-size: 14px;
    margin-right: 8px;
    width: 16px;
    text-align: center;
        }
        
  &:hover:not(.disabled) {
    background: #f5f5f5;
    color: #0080ff;
        }
        
  &.danger:hover:not(.disabled) {
    background: #fff1f0;
    color: #ff4d4f;
        }
        
  &.disabled {
    opacity: 0.5;
          cursor: not-allowed;
    color: #bbb;
  }
}

/* 淡入淡出动画 */
.fade-enter-active, .fade-leave-active {
  transition: opacity 0.2s, transform 0.2s;
      }
.fade-enter, .fade-leave-to {
  opacity: 0;
  transform: translateY(-4px);
      }

// 响应式设计
@media (max-width: 768px) {
  .modern-cluster-container {
    padding: 16px;
    }
    
  .page-header {
    padding: 24px;
    
    .header-content {
      .page-title {
        font-size: 24px;
}

      .page-description {
        font-size: 15px;
      }
    }
  }
  
  .cluster-grid {
    grid-template-columns: 1fr;
    gap: 16px;
  }
  
  .button-row {
    flex-wrap: wrap;
    gap: 8px;
    
    .action-btn {
      flex-grow: 1;
    }
  }
}

// 创建新集群卡片样式 - 修复丢失的样式
.create-card {
  cursor: pointer;
  background: white;
  
  .create-card-inner {
    padding: 24px;
    height: 100%;
    display: flex;
    align-items: center;
    justify-content: center;
    box-sizing: border-box;
    }
    
  .create-card-content {
    text-align: center;
    max-width: 250px;
  }
  
  .create-icon {
    width: 48px;
    height: 48px;
    border-radius: 24px;
    background: #f0f7ff;
    color: #007aff;
    font-size: 24px;
          display: flex;
    align-items: center;
          justify-content: center;
    margin: 0 auto 16px;
  }
  
  .create-title {
    font-size: 18px;
    font-weight: 600;
    color: #1d1d1f;
    margin: 0 0 8px;
          }
  
  .create-desc {
    font-size: 14px;
    color: #86868b;
    margin: 0 0 20px;
    line-height: 1.4;
  }
  
  .create-features {
    text-align: left;
    
    .feature-item {
      display: flex;
      align-items: center;
      gap: 8px;
      margin-bottom: 8px;
      
      .anticon {
        color: #007aff;
        font-size: 16px;
      }
      
      span {
        font-size: 14px;
        color: #1d1d1f;
      }
    }
  }
  
  &:hover {
    .create-icon {
      background: #007aff;
      color: white;
    }
    
    .create-title {
      color: #007aff;
        }
      }
}

/* 集群卡片样式 */
.cluster-card {
  position: relative;
  background: #FFFFFF;
  border-radius: 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.05), 0 1px 2px rgba(0, 0, 0, 0.07);
  border: 1px solid #E5E5E5;
  overflow: hidden;
  transition: all 0.3s ease;
  display: flex;
  flex-direction: column;
  min-height: 220px; /* 确保卡片高度足够 */
  
  &:hover {
    box-shadow: 0 3px 6px rgba(0, 0, 0, 0.08), 0 3px 6px rgba(0, 0, 0, 0.12);
    transform: translateY(-2px);
      }
    }
    
/* 样式一致性修复 */
:deep(.ant-btn) {
  border-radius: 4px;
  font-weight: 500;
}

/* 全局样式，确保下拉菜单和按钮样式一致 */
.mac-dropdown-overlay {
  z-index: 1050 !important; /* 确保不被覆盖 */
    }
    
/* 修复下拉菜单容器问题 */
.ant-dropdown.mac-dropdown-overlay {
  padding: 0 !important;
      background: transparent !important;
  border: none !important;
  box-shadow: none !important;
      }

.mac-dropdown-overlay .ant-dropdown-menu {
  border-radius: 8px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.15);
  padding: 4px;
  background-color: #ffffff;
  border: 1px solid #f0f0f0;
  width: auto;
    }
    
.mac-dropdown-overlay .ant-dropdown-menu-item {
  border-radius: 6px;
  padding: 8px 12px;
  margin: 2px 0;
  font-size: 14px;
  min-width: 120px;
      }
      
.mac-dropdown-overlay .ant-dropdown-menu-item:hover {
  background-color: #f0f7ff;
  color: #1890ff;
}

.mac-dropdown-overlay .ant-dropdown-menu-item.danger:hover {
  background-color: #fff1f0;
  color: #ff4d4f;
}

.mac-dropdown-overlay .ant-dropdown-menu-item .anticon {
  margin-right: 8px;
        }
        
.mac-dropdown-overlay .ant-dropdown-menu-item[disabled] {
  color: rgba(0, 0, 0, 0.25);
        }
        
/* 改进的按钮样式，确保全局一致 */
body.colony-manage-page .ant-btn.mac-btn {
  height: 36px !important;
  border-radius: 8px !important;
  font-weight: 500 !important;
          }
          
/* 图标单独定位，让文字可以居中 */
.apple-create-modal .ant-btn .anticon {
  position: absolute !important;
  left: 16px !important;
  top: 50% !important;
  transform: translateY(-50%) !important;
            font-size: 14px !important;
}

/* 编辑集群弹窗样式 */
.apple-create-modal .ant-btn {
  height: 40px !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
  box-shadow: 0 2px 6px rgba(0, 0, 0, 0.05) !important;
              display: flex !important;
              align-items: center !important;
              justify-content: center !important;
  padding: 0 16px !important;
  min-width: 120px !important;
  position: relative !important;
          }
          
.apple-create-modal .ant-btn:has(.anticon) span {
            margin-left: 0 !important;
  margin-right: 0 !important;
  width: 100% !important;
  text-align: center !important;
        }
        
.apple-create-modal .ant-btn-primary {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%) !important;
        border: none !important;
          color: white !important;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3) !important;
          }
          
.apple-create-modal .ant-btn-primary:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%) !important;
            transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4) !important;
}

.apple-create-modal .ant-modal-footer {
  display: flex !important;
  justify-content: center !important;
  gap: 16px !important;
  padding: 20px 24px !important;
  background-color: #fafafa !important;
  border-top: 1px solid #f0f0f0 !important;
      }
      
.apple-create-modal .ant-modal-footer .ant-btn {
  margin-left: 0 !important;
  margin-right: 0 !important;
  min-width: 120px !important;
  height: 40px !important;
  border-radius: 10px !important;
          font-weight: 600 !important;
}

/* 授权模态框整体样式，干净简洁无多余区域 */
.auth-cluster-modal {
  max-width: 100% !important;
}

/* 极端方式隐藏所有问号图标，包括SVG */
.no-icon-modal .anticon-question-circle,
.no-icon-modal i.anticon,
.no-icon-modal i.anticon-question-circle,
.no-icon-modal svg[data-icon="question-circle"],
.no-icon-modal .anticon-question-circle svg,
.no-icon-modal .ant-modal-confirm-title + i,
.no-icon-modal .ant-modal-confirm-body i,
.no-icon-modal .ant-modal-header i,
.no-icon-modal .ant-modal-body i.anticon,
.no-icon-modal .ant-modal-confirm i,
.no-icon-modal .ant-modal-confirm-body > .anticon,
.no-icon-modal .ant-modal-confirm-body > .anticon-question-circle,
.no-icon-modal .ant-modal-confirm .ant-modal-confirm-body > .anticon,
.no-icon-modal .ant-modal-body .anticon-info-circle,
.no-icon-modal .anticon,
.no-icon-modal svg,
.no-icon-modal [aria-label="icon: question-circle"],
.no-icon-modal [data-icon="question-circle"] {
  /* 极端隐藏 - 多重方法确保不可见 */
  display: none !important;
  opacity: 0 !important;
  visibility: hidden !important;
  width: 0 !important;
  height: 0 !important;
  padding: 0 !important;
  margin: 0 !important;
            border: none !important;
  overflow: hidden !important;
  position: absolute !important;
  top: -9999px !important;
  left: -9999px !important;
  pointer-events: none !important;
  max-width: 0 !important;
  max-height: 0 !important;
  clip: rect(0, 0, 0, 0) !important;
  clip-path: inset(100%) !important;
            }
            
/* 移除可能存放图标的父元素边距 */
.no-icon-modal .ant-modal-confirm-body,
.no-icon-modal .ant-modal-confirm-title-wrap,
.no-icon-modal .ant-modal-header,
.no-icon-modal .ant-modal-body,
.no-icon-modal .ant-modal-confirm-content {
  padding-left: 0 !important;
  padding-right: 0 !important;
  margin-left: 0 !important;
  margin-right: 0 !important;
}

/* 隐藏标题区域和页脚区域 */
.no-icon-modal .ant-modal-header,
.no-icon-modal .ant-modal-footer,
.no-icon-modal .ant-modal-confirm-btns {
  display: none !important;
          }

/* 修复可能的内容布局问题 */
.no-icon-modal .ant-modal-body,
.no-icon-modal .ant-modal-content {
  padding: 0 !important;
  overflow: hidden !important;
  background-color: white !important;
  border-radius: 16px !important;
}

/* 特别处理 - 移除图标后的空间调整 */
.no-icon-modal .ant-modal-confirm-body > span:first-child:empty,
.no-icon-modal .ant-modal-confirm-body > span:first-child:blank {
  display: none !important;
}

/* 解决可能的焦点问题 */
.focus-trap-disabled [aria-hidden="true"],
.focus-trap-disabled [tabindex="-1"] {
  display: none !important;
  visibility: hidden !important;
  width: 0 !important;
  height: 0 !important;
  overflow: hidden !important;
  position: absolute !important;
  pointer-events: none !important;
  top: -9999px !important;
  left: -9999px !important;
}

/* 主要内容区域居中 */
.auth-cluster-modal .ant-modal-content {
  border-radius: 16px !important;
  overflow: hidden !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15) !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
  background-color: white !important;
  width: 100% !important;
  max-width: 450px !important;
  margin: 0 auto !important;
}

/* 适用于info模态框的标题和内容样式 */
.auth-cluster-modal .ant-modal-header {
  display: none !important;
}

.auth-cluster-modal .ant-modal-body {
  padding: 0 !important;
  margin: 0 !important;
  overflow: hidden !important;
}

/* 隐藏info模态框的默认底部按钮 */
.auth-cluster-modal .ant-modal-footer {
  display: none !important;
}

/* 确保内容居中，并占用正确宽度 */
.auth-cluster-modal .cluster-auth-content {
  width: 100% !important;
  margin: 0 auto !important;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
}

/* 确保没有多余的图标和内边距 */
.auth-cluster-modal .ant-modal-confirm-body {
  padding: 0 !important;
  margin: 0 !important;
  display: flex !important;
  flex-direction: column !important;
  align-items: center !important;
  width: 100% !important;
}

.auth-cluster-modal .ant-modal-confirm-title {
  display: none !important;
}

/* 确保确认内容没有内边距 */
.auth-cluster-modal .ant-modal-confirm-content {
  margin: 0 !important;
  padding: 0 !important;
  width: 100% !important;
}

/* 设置授权弹窗所有按钮样式 */
.auth-cluster-modal .auth-btns {
  display: flex !important;
  justify-content: center !important;
  gap: 16px !important;
  padding: 20px 24px !important;
  background-color: #fafafa !important;
  border-top: 1px solid #f0f0f0 !important;
  width: 100% !important;
}

/* 确保弹窗按钮样式 */
.auth-cluster-modal .auth-btn,
.auth-cluster-modal .auth-btns .ant-btn {
  min-width: 120px !important;
  height: 40px !important;
  border-radius: 10px !important;
  font-weight: 600 !important;
  font-size: 14px !important;
  letter-spacing: 0.3px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
  padding: 0 16px !important;
  box-sizing: border-box !important;
}

.auth-cluster-modal .auth-btn-primary,
.auth-cluster-modal .auth-btns .ant-btn-primary,
.auth-cluster-modal .auth-btns .ant-btn-type-primary {
  background: linear-gradient(135deg, #1890ff 0%, #096dd9 100%) !important;
  border: none !important;
  color: white !important;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.3) !important;
}

.auth-cluster-modal .auth-btn-primary:hover,
.auth-cluster-modal .auth-btns .ant-btn-primary:hover,
.auth-cluster-modal .auth-btns .ant-btn-type-primary:hover {
  background: linear-gradient(135deg, #40a9ff 0%, #1890ff 100%) !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 4px 12px rgba(24, 144, 255, 0.4) !important;
}

.auth-cluster-modal .auth-btn-default,
.auth-cluster-modal .auth-btns .ant-btn-default {
  background: #ffffff !important;
  color: #464646 !important;
  border: 1px solid #e6e6e6 !important;
}

.auth-cluster-modal .auth-btn-default:hover,
.auth-cluster-modal .auth-btns .ant-btn-default:hover {
  background: white !important;
  color: #1890ff !important;
  border-color: #1890ff !important;
  transform: translateY(-2px) !important;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.05) !important;
}
</style>

<style>
/* 清除之前的CSS，使用新的样式 */
.clean-modal .ant-modal-content,
.clean-modal .ant-modal-body {
  padding: 0 !important;
  margin: 0 !important;
}

.clean-modal .ant-modal-close {
  display: none !important;
}

/* 添加圆角和阴影样式 */
.clean-modal .ant-modal-content {
  border-radius: 16px !important;
  overflow: hidden !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.15) !important;
  border: 1px solid rgba(0, 0, 0, 0.05) !important;
}

.auth-modal {
  border-radius: 16px !important;
}

/* 为模态框添加Mac风格的毛玻璃效果 */
.auth-modal .ant-modal-content {
  backdrop-filter: blur(10px) !important;
  -webkit-backdrop-filter: blur(10px) !important;
}

/* 保留其他CSS以防其他地方仍使用$confirm */
.auth-cluster-modal .ant-modal-confirm-body {
  display: block !important;
  width: 100% !important;
  padding: 0 !important;
  margin: 0 !important;
}

/* 彻底消除紫色区域 */
.auth-cluster-modal .ant-modal-confirm-body > .anticon {
  display: none !important;
  width: 0 !important;
  height: 0 !important;
  margin: 0 !important;
  padding: 0 !important;
  position: absolute !important;
  left: -9999px !important;
}

/* 去除所有问号图标相关的空间 */
.auth-cluster-modal .ant-modal-confirm-btns {
  display: none !important;
}

/* 确保内容区域占满宽度 */
.auth-cluster-modal .ant-modal-confirm-body-wrapper {
  padding: 0 !important;
  margin: 0 !important;
  width: 100% !important;
  display: block !important;
}

.auth-cluster-modal .ant-modal-body {
  padding: 0 !important;
}</style>