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

        <!-- 卡片底部按钮区域 -->
        <div class="card-footer">
          <div :class="buttonGroupClass" ref="macButtonGroup">
            <!-- 主按钮 - 移除图标 -->
            <a-button
              type="primary"
              class="mac-btn primary-btn"
              @click="getInto(item)"
                :disabled="item.clusterStateCode === 1"
              >
              <span>进入集群</span>
            </a-button>
            
            <!-- 次要按钮组 - 移除所有图标 -->
            <div class="secondary-buttons">
              <a-button
                class="mac-btn"
                @click="addColony(item)"
                :disabled="item.clusterStateCode === 2"
              >
                <span>编辑</span>
              </a-button>
              <a-button 
                v-if="user && user.userType === 1"
                class="mac-btn"
                @click="authCluster(item)"
              >
                <span>授权</span>
              </a-button>
              
              <!-- 更多按钮 - 重写以解决无反应问题 -->
              <div class="dropdown-container">
                <a-button 
                  class="mac-btn more-btn"
                  @click.stop="showMoreOptions(item, $event)"
                >
                  <span>更多</span>
                </a-button>
              </div>
            </div>
          </div>
        </div>
      </div>
      
      <!-- 创建新集群卡片 - 放在最后位置 -->
      <div class="cluster-card create-card" @click="addColony({})">
          <div class="create-icon">
          <svg viewBox="0 0 24 24" fill="none" xmlns="http://www.w3.org/2000/svg">
            <path d="M12 5V19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            <path d="M5 12H19" stroke="currentColor" stroke-width="2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
          </div>
        <div class="create-content">
          <h3 class="create-title">创建新集群</h3>
          <p class="create-desc">快速部署一个全新的大数据集群环境</p>
        </div>
          <div class="create-features">
          <span class="feature-tag">一键部署</span>
          <span class="feature-tag">智能配置</span>
          <span class="feature-tag">高效运维</span>
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
    
    <!-- 新增：编辑集群模态框 -->
    <a-modal
      v-model="editModalVisible"
      :title="currentEditObj && JSON.stringify(currentEditObj) !== '{}' ? '编辑集群配置' : '创建新集群'"
      :footer="null"
      :closable="true"
      :maskClosable="false"
      centered
      destroyOnClose
      :width="800"
      :style="{ top: '30px', height: 'auto', maxHeight: 'calc(100vh - 60px)' }"
      :bodyStyle="{ padding: 0, maxHeight: 'calc(100vh - 170px)', overflowY: 'auto' }"
      class="edit-colony-modal"
      @cancel="handleEditModalClose"
      ref="editModal"
    >
      <AddColony 
        v-if="editModalVisible" 
        :detail="currentEditObj || {}" 
        :callBack="handleEditComplete" 
        @cancel="handleEditModalClose"
        @success="handleEditComplete"
        ref="addColonyForm"
      />
    </a-modal>
    
    <!-- 全局下拉菜单，固定在body上 -->
    <div v-if="activeDropdown !== null" class="custom-dropdown-container" ref="globalDropdown">
      <div class="custom-dropdown">
        <div 
          class="dropdown-item" 
          @click="configCluster(activeClusterData)"
          :class="{ disabled: activeClusterData && activeClusterData.clusterStateCode === 2 }"
        >
          配置集群
        </div>
        <div 
          class="dropdown-item danger" 
          @click="delectColony(activeClusterData)"
          :class="{ disabled: activeClusterData && activeClusterData.clusterStateCode === 2 }"
        >
          删除集群
        </div>
      </div>
    </div>
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
    AuthCluster,
    AddColony
  },

  data() {
    return {
      visible: false,
      dataSource: [],
      confirmLoading: false,
      clusterId: "", // 操作的集群Id
      depType: "",
      activeDropdown: null, // 当前打开的下拉菜单
      activeClusterData: null, // 当前选中的集群数据
      dropdownPositions: {}, // 存储每个下拉菜单的位置
      authModalVisible: false,
      currentClusterForAuth: null,
      // 增加辅助属性，避免直接操作DOM
      styleConsistencyApplied: false,
      editModalVisible: false, // 新增：控制编辑模态框的显示
      currentEditObj: null // 新增：存储当前编辑的集群数据
    };
  },

  computed: {
    ...mapGetters("account", ["user"]),
    // 过滤掉添加集群的占位项
    filteredDataSource() {
      return this.dataSource.filter(item => !item.add);
    },
    // 修复2: 使用计算属性处理样式，而非直接操作DOM
    buttonGroupClass() {
      return {
        'mac-button-group': true
      };
    },
    dropdownClass() {
      return {
        'mac-dropdown': true
      };
    }
  },

  mounted() {
    this.getColonyList();
    
    // 监听全局点击，用于关闭下拉菜单
    document.addEventListener('click', this.closeDropdownOnOutsideClick);
    
    // 添加MutationObserver监听DOM变化
    this.observer = new MutationObserver(this.handleDOMChanges);
    this.observer.observe(document.body, {
      childList: true,
      subtree: true
    });
    
    // 应用初始样式一致性
    this.$nextTick(() => {
      this.ensureStyleConsistency();
      this.fixAccessibilityIssues();
    });
  },
  
  beforeDestroy() {
    // 移除全局事件监听器
    document.removeEventListener('click', this.closeDropdownOnOutsideClick);
    
    // 停止观察DOM变化
    if (this.observer) {
      this.observer.disconnect();
    }
  },

  methods: {
    ...mapMutations("setting", ["setIsCluster", "setMenuData", "setClusterId"]),
    
    // 切换下拉菜单
    toggleDropdown(item, event) {
      if (this.activeDropdown === item.id) {
        this.activeDropdown = null;
      } else {
        this.activeDropdown = item.id;
        
        // 改进的位置计算，考虑屏幕边界
        this.$nextTick(() => {
          const buttonElement = event.currentTarget;
          const dropdown = document.querySelector('.custom-dropdown');
          
          if (buttonElement && dropdown) {
            const rect = buttonElement.getBoundingClientRect();
            const viewportWidth = window.innerWidth;
            const viewportHeight = window.innerHeight;
            const dropdownWidth = dropdown.offsetWidth || 120;
            const dropdownHeight = dropdown.offsetHeight || 100;
            
            // 默认右对齐，但如果右边超出屏幕则左对齐
            let posX = rect.right - dropdownWidth;
            if (posX < 10) posX = rect.left; // 如果左边也不够，则右对齐
            
            // 默认显示在按钮下方，但如果下方空间不足则显示在上方
            let posY = rect.bottom + 5;
            if (posY + dropdownHeight > viewportHeight - 10) {
              posY = rect.top - dropdownHeight - 5;
            }
            
            // 应用位置
            dropdown.style.position = 'fixed';
            dropdown.style.top = `${posY}px`;
            dropdown.style.left = `${posX}px`;
            dropdown.style.right = 'auto';
            dropdown.style.zIndex = '9999';
            dropdown.style.boxShadow = '0 10px 30px rgba(0, 0, 0, 0.12), 0 6px 16px rgba(0, 0, 0, 0.08)';
            dropdown.style.animation = 'dropdown-fade-in 0.2s ease-out';
          }
        });
      }
      
      // 阻止事件冒泡，避免立即被document点击事件关闭
      event.stopPropagation();
    },
    
    // 关闭下拉菜单
    closeDropdown() {
      this.activeDropdown = null;
    },
    
    // 点击外部关闭菜单 - 修正版本
    closeDropdownOnOutsideClick(e) {
      // 如果菜单已打开
      if (this.activeDropdown !== null) {
        const dropdownContainer = document.querySelector('.custom-dropdown-container');
        const dropdownButtons = document.querySelectorAll('.more-btn');
        
        // 检查点击是否在菜单容器或任何"更多"按钮上
        let clickOnDropdownOrButton = false;
        
        if (dropdownContainer && dropdownContainer.contains(e.target)) {
          clickOnDropdownOrButton = true;
        }
        
        if (!clickOnDropdownOrButton) {
          dropdownButtons.forEach(button => {
            if (button.contains(e.target)) {
              clickOnDropdownOrButton = true;
            }
          });
        }
        
        // 如果点击在菜单外部，则关闭菜单
        if (!clickOnDropdownOrButton) {
          this.activeDropdown = null;
          this.activeClusterData = null;
        }
      }
    },
    
    // 确保样式一致性，但不直接操作DOM
    ensureStyleConsistency() {
      // 仅标记状态变化，不操作DOM
      this.styleConsistencyApplied = true;
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
      this.editModalVisible = true;
      this.currentEditObj = obj;
      
      // 确保模态框内容正确渲染
      this.$nextTick(() => {
        // 使用多个setTimeout以确保在Vue渲染完成后执行
        setTimeout(() => {
          this.forceRefreshModal();
        }, 50);
        
        setTimeout(() => {
          this.forceRefreshModal();
        }, 200);
        
        setTimeout(() => {
          this.forceRefreshModal();
        }, 500);
      });
    },
    
    handleEditModalClose() {
      this.editModalVisible = false;
      this.currentEditObj = null;
    },
    
    handleEditComplete() {
      this.getColonyList();
      this.handleEditModalClose();
    },
    
    // 强制刷新模态框内容
    forceRefreshModal() {
      if (!this.$refs.editModal) return;
      
      const modalEl = this.$refs.editModal.$el;
      if (!modalEl) return;
      
      // 强制刷新模态框DOM
      const modalBody = modalEl.querySelector('.ant-modal-body');
      if (modalBody) {
        modalBody.style.display = 'block';
        modalBody.style.visibility = 'visible';
        modalBody.style.opacity = '1';
      }
      
      // 确保表单组件正确渲染
      if (this.$refs.addColonyForm && this.$refs.addColonyForm.$el) {
        const formContainer = this.$refs.addColonyForm.$el;
        formContainer.style.display = 'block';
        formContainer.style.visibility = 'visible';
        formContainer.style.opacity = '1';
        formContainer.style.height = 'auto';
        formContainer.style.minHeight = '300px';
      }
    },
    delectColony(obj) {
      // 直接导入DelectColony组件，避免在components中注册
      const DelectColony = require('./commponents/delectColony.vue').default;
      const self = this;
      let width = 300;
      let content = (
        <DelectColony
          sysTypeTxt="集群"
          detail={obj}
          callBack={() => self.getColonyList()}
        />
      );
      this.$confirm({
        width: width,
        title: '提示',
        content: content,
        closable: true,
        icon: () => null, // 移除图标
        wrapClassName: 'clean-modal delete-confirm compact-modal', // 添加自定义类名
        maskClosable: false,
        centered: true,
        okButtonProps: { style: { display: 'none' } }, // 隐藏默认按钮
        cancelButtonProps: { style: { display: 'none' } }, // 隐藏默认按钮
        bodyStyle: { padding: 0 },
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
      // 只更新状态，不触发DOM操作
      this.currentClusterForAuth = obj;
      this.authModalVisible = true;
      
      // 使用轻量级修复，不影响DOM结构
      this.$nextTick(() => {
        this.fixAccessibilityIssues();
      });
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

    // 修复aria-hidden警告 - 更彻底的方案
    fixAccessibilityIssues() {
      this.$nextTick(() => {
        // 尝试使用多种方法修复可访问性问题
        setTimeout(() => {
          try {
            // 方法1：移除有问题的元素
            const trapFocus = document.querySelectorAll('[aria-hidden="true"] [tabindex="0"]');
            trapFocus.forEach(el => {
              if (el.parentNode) {
                el.parentNode.removeChild(el);
              }
            });
            
            // 方法2：为有aria-hidden的元素添加inert属性
            const hiddenElements = document.querySelectorAll('[aria-hidden="true"]');
            hiddenElements.forEach(el => {
              // 设置inert属性，防止元素获得焦点
              el.setAttribute('inert', '');
              
              // 移除所有tabindex属性
              const focusables = el.querySelectorAll('[tabindex]');
              focusables.forEach(focusable => {
                focusable.removeAttribute('tabindex');
              });
            });
            
            // 方法3：修复特定的问题元素
            const specificTrapFocus = document.querySelector('div[tabindex="0"][aria-hidden="true"][style*="width: 0px"]');
            if (specificTrapFocus && specificTrapFocus.parentNode) {
              specificTrapFocus.parentNode.removeChild(specificTrapFocus);
            }
          } catch (error) {
            console.error('可访问性修复出错:', error);
          }
        }, 300);
      });
    },
    // 获取下拉菜单容器，确保下拉菜单显示在正确位置
    getPopupContainer(triggerNode) {
      // 返回按钮的父元素，确保相对于按钮定位
      return triggerNode.parentElement;
    },
    // 新方法：显示更多选项 - 完全重写
    showMoreOptions(item, event) {
      console.log('点击更多按钮:', item.id);
      
      // 如果当前按钮已激活，则关闭菜单
      if (this.activeDropdown === item.id) {
        this.activeDropdown = null;
        this.activeClusterData = null;
        return;
      }
      
      // 打开新的下拉菜单
      this.activeDropdown = item.id;
      this.activeClusterData = item; // 存储当前操作的集群数据
      
      // 使用更可靠的方法设置下拉菜单位置
      this.$nextTick(() => {
        const buttonElement = event.currentTarget;
        
        if (!buttonElement) {
          console.error('找不到按钮元素');
          return;
        }
        
        // 获取按钮位置
        const rect = buttonElement.getBoundingClientRect();
        const viewportWidth = window.innerWidth;
        const viewportHeight = window.innerHeight;
        
        // 创建DOM元素以确保它存在
        this.$nextTick(() => {
          // 为确保DOM已更新，添加一个短暂延时
          setTimeout(() => {
            const dropdownContainer = this.$refs.globalDropdown;
            
            if (!dropdownContainer) {
              console.error('找不到下拉菜单容器，重试中');
              return;
            }
            
            // 计算理想位置 - 按钮右下方
            let left = rect.right - 140; // 默认距右边界140px
            let top = rect.bottom + 5;   // 默认在按钮下方5px
            
            // 确保不超出屏幕边界
            if (left < 10) left = 10;
            if (left + 160 > viewportWidth) left = viewportWidth - 170;
            if (top + 100 > viewportHeight) top = rect.top - 110;
            
            // 设置下拉菜单容器样式
            dropdownContainer.style.position = 'fixed';
            dropdownContainer.style.top = top + 'px';
            dropdownContainer.style.left = left + 'px';
            dropdownContainer.style.zIndex = '9999';
            dropdownContainer.style.display = 'block';
            
            console.log('下拉菜单位置设置完成:', {top, left});
          }, 50);
        });
      });
      
      // 阻止事件冒泡
      event.stopPropagation();
    },
    
    // 监听DOM变化，修复样式问题
    handleDOMChanges(mutations) {
      let needsFix = false;
      
      mutations.forEach(mutation => {
        if (mutation.addedNodes && mutation.addedNodes.length) {
          for (let i = 0; i < mutation.addedNodes.length; i++) {
            const node = mutation.addedNodes[i];
            // 检查是否添加了模态框
            if (node.nodeType === 1 && 
                (node.classList && 
                 (node.classList.contains('ant-modal-root') || 
                  node.classList.contains('edit-colony-modal')))) {
              needsFix = true;
              break;
            }
            
            // 检查子节点
            if (node.querySelector && 
                (node.querySelector('.ant-modal-root') || 
                 node.querySelector('.edit-colony-modal'))) {
              needsFix = true;
              break;
            }
          }
        }
      });
      
      if (needsFix) {
        // 使用nextTick和setTimeout确保在Vue更新DOM后应用样式
        this.$nextTick(() => {
          setTimeout(() => {
            this.fixEditModalStyles();
          }, 50);
        });
      }
    },
    
    // 修复编辑集群模态框样式
    fixEditModalStyles() {
      // 查找编辑集群模态框
      const editModals = document.querySelectorAll('.edit-colony-modal');
      
      editModals.forEach(modal => {
        // 确保模态框内容区域可见
        const modalBody = modal.querySelector('.ant-modal-body');
        if (modalBody) {
          modalBody.style.maxHeight = 'calc(100vh - 170px)';
          modalBody.style.overflowY = 'auto';
        }
        
        // 确保表单容器可见
        const formContainer = modal.querySelector('.apple-form-container');
        if (formContainer) {
          formContainer.style.display = 'block';
          formContainer.style.visibility = 'visible';
          formContainer.style.minHeight = '300px';
        }
        
        // 确保表单头部可见
        const formHeader = modal.querySelector('.form-header');
        if (formHeader) {
          formHeader.style.display = 'block';
        }
        
        // 确保卡片区域可见
        const formCards = modal.querySelector('.form-cards');
        if (formCards) {
          formCards.style.display = 'block';
        }
      });
    },
  },
  // 修复1: 先定义debounce函数，不要放在methods里
  debounce(fn, delay) {
    let timer = null;
    return function() {
      const context = this;
      const args = arguments;
      if (timer) {
        clearTimeout(timer);
      }
      timer = setTimeout(() => {
        fn.apply(context, args);
        timer = null;
      }, delay);
    };
  },

  // 在页面活动时也确保样式一致性，但简化操作
  activated() {
    this.styleConsistencyApplied = true;
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

/* 增强创建新集群卡片的样式 */
.create-card {
  height: 100%;
  border: 2px dashed #e8e8e8;
  border-radius: 16px;
  background: linear-gradient(135deg, rgba(255, 255, 255, 0.95) 0%, rgba(247, 250, 255, 0.95) 100%);
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 24px;
  text-align: center;
  transition: all 0.3s ease;
  cursor: pointer;
      position: relative;
  overflow: hidden;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.03);
      
      &::before {
    content: "";
        position: absolute;
        top: 0;
        left: 0;
        right: 0;
    height: 0;
    background: linear-gradient(135deg, rgba(24, 144, 255, 0.1) 0%, rgba(104, 189, 255, 0.1) 100%);
    transition: height 0.3s ease;
        z-index: -1;
  }

  &:hover {
    border-color: #1890ff;
    transform: translateY(-4px);
    box-shadow: 0 12px 24px rgba(24, 144, 255, 0.12);

    &::before {
      height: 100%;
    }

    .create-icon {
      transform: scale(1.1) rotate(180deg);
      color: #1890ff;
      box-shadow: 0 6px 16px rgba(24, 144, 255, 0.3);
    }

    .create-title {
      color: #1890ff;
    }
  }

  &:active {
    transform: translateY(-2px);
  }

  .create-icon {
    width: 64px;
    height: 64px;
    background: white;
    border-radius: 50%;
    display: flex;
    align-items: center;
    justify-content: center;
    margin-bottom: 20px;
    color: #999;
    transition: all 0.3s ease;
    box-shadow: 0 4px 12px rgba(0, 0, 0, 0.08);
    border: 1px solid #f0f0f0;

    svg {
      width: 24px;
      height: 24px;
      transition: all 0.3s ease;
    }
  }

  .create-content {
    margin-bottom: 16px;
  }

  .create-title {
    font-size: 18px;
    font-weight: 600;
    margin-bottom: 8px;
    color: #333;
    transition: color 0.3s ease;
  }

  .create-desc {
    font-size: 14px;
    color: #888;
    margin-bottom: 16px;
    max-width: 220px;
    line-height: 1.5;
  }

  .create-features {
    display: flex;
    flex-wrap: wrap;
    justify-content: center;
    gap: 8px;
  }

  .feature-tag {
    display: inline-block;
    padding: 3px 10px;
    background: rgba(24, 144, 255, 0.1);
    color: #1890ff;
    border-radius: 12px;
          font-size: 12px;
    font-weight: 500;
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

/* 样式一致性类 */
.consistent-style {
  display: flex;
  flex-direction: column;
  gap: 10px;
  width: 100%;
}

.mac-btn {
  height: 40px !important; /* 使用!important确保覆盖其他样式 */
  border-radius: 10px !important;
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 下拉菜单位置修复 */
.dropdown-right {
  position: relative;
}

/* 自定义下拉菜单样式 */
.dropdown-container {
  position: relative;
  display: inline-block;
}

.custom-dropdown {
  position: fixed !important; /* 改为固定定位，避免被父元素限制 */
  z-index: 9999 !important; /* 确保最高层级 */
  background: rgba(255, 255, 255, 0.98) !important;
  backdrop-filter: blur(20px) !important;
  -webkit-backdrop-filter: blur(20px) !important;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12), 
              0 6px 16px rgba(0, 0, 0, 0.08),
              0 2px 6px rgba(0, 0, 0, 0.06) !important;
  border: 1px solid rgba(220, 220, 220, 0.5) !important;
  border-radius: 12px !important;
  padding: 6px !important;
  min-width: 140px !important;
  overflow: visible !important;
  animation: dropdown-fade-in 0.25s cubic-bezier(0.25, 0.1, 0.25, 1) !important;
}

.dropdown-item {
  padding: 10px 14px !important;
  cursor: pointer !important;
  border-radius: 8px !important;
  transition: all 0.2s ease !important;
  color: #333 !important;
  font-size: 14px !important;
  white-space: nowrap !important;
  margin: 2px 0 !important;
  font-weight: 400 !important;
  display: flex !important;
  align-items: center !important;
}

.dropdown-item:hover {
  background-color: rgba(24, 144, 255, 0.1) !important;
  color: #1890ff !important;
  transform: translateY(-1px) !important;
}

.dropdown-item.disabled {
  color: #ccc !important;
  cursor: not-allowed !important;
  opacity: 0.7 !important;
}

.dropdown-item.disabled:hover {
  background-color: transparent !important;
  color: #ccc !important;
  transform: none !important;
}

/* 添加下拉菜单动画 */
@keyframes dropdown-fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}

/* 确保所有按钮宽度相似 */
.mac-btn {
  min-width: 80px;
  padding: 0 16px;
}
</style>

<style>
/* 模态框样式 */
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

/* 确保内容区域占满宽度 */
.auth-cluster-modal .ant-modal-body {
  padding: 0 !important;
}

/* 下拉菜单样式修复 */
.mac-dropdown-menu {
  border-radius: 12px !important;
  overflow: hidden !important;
  box-shadow: 0 6px 16px rgba(0, 0, 0, 0.12) !important;
  padding: 4px !important;
  min-width: 140px !important;
}

.mac-dropdown-menu .ant-dropdown-menu-item {
  border-radius: 8px !important;
  margin: 2px 0 !important;
  padding: 10px 12px !important;
}

/* 全局样式，修复下拉菜单位置和样式 */
.mac-dropdown-overlay {
  position: absolute !important; 
  z-index: 1050 !important;
  box-shadow: 0 3px 6px -4px rgba(0, 0, 0, 0.12), 
              0 6px 16px 0 rgba(0, 0, 0, 0.08),
              0 9px 28px 8px rgba(0, 0, 0, 0.05) !important;
  transform-origin: center top !important;
}

/* 确保所有按钮的图标可见 */
.ant-btn.mac-btn .anticon {
  display: inline-block !important;
  visibility: visible !important;
  opacity: 1 !important;
}

/* 确保按钮内容居中 */
.ant-btn.mac-btn {
  display: inline-flex !important;
  align-items: center !important;
  justify-content: center !important;
}

/* 按钮图标右侧留白 */
.ant-btn.mac-btn .anticon + span {
  margin-left: 4px !important;
}

/* 防止样式被覆盖 */
body .ant-btn.mac-btn .anticon {
  display: inline-block !important;
}

/* 下拉菜单定位修复 */
.ant-dropdown {
  top: auto !important;
}

.ant-dropdown.ant-dropdown-placement-bottomRight {
  left: auto !important;
  right: 0 !important;
}

/* 去掉小箭头 */
.ant-dropdown.ant-dropdown-placement-bottomRight > .ant-dropdown-arrow {
  display: none !important;
}

/* 确保mac-dropdown-menu内的项正常显示 */
.mac-dropdown-menu .ant-dropdown-menu-item .anticon {
  margin-right: 8px !important;
}

/* 全局下拉菜单容器样式 */
.custom-dropdown-container {
  position: fixed;
  z-index: 9999;
  display: block;
}

/* 全局下拉菜单样式 */
.custom-dropdown {
  background: rgba(255, 255, 255, 0.98);
  backdrop-filter: blur(20px);
  -webkit-backdrop-filter: blur(20px);
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12),
              0 6px 16px rgba(0, 0, 0, 0.08),
              0 2px 6px rgba(0, 0, 0, 0.06);
  border: 1px solid rgba(220, 220, 220, 0.5);
  border-radius: 12px;
  padding: 6px;
  min-width: 140px;
  animation: dropdown-fade-in 0.25s cubic-bezier(0.25, 0.1, 0.25, 1);
}

.custom-dropdown .dropdown-item {
  padding: 10px 14px;
  cursor: pointer;
  border-radius: 8px;
  transition: all 0.2s ease;
  color: #333;
  font-size: 14px;
  white-space: nowrap;
  margin: 2px 0;
  font-weight: 400;
  display: flex;
  align-items: center;
}

.custom-dropdown .dropdown-item:hover {
  background-color: rgba(24, 144, 255, 0.1);
  color: #1890ff;
  transform: translateY(-1px);
}

/* 删除按钮特殊样式 - 增强样式优先级 */
body .custom-dropdown .dropdown-item.danger {
  color: #ff4d4f !important;
}

body .custom-dropdown .dropdown-item.danger:hover {
  background-color: rgba(255, 77, 79, 0.1) !important;
  color: #ff4d4f !important;
  font-weight: 500 !important;
}

.custom-dropdown .dropdown-item.disabled {
  color: #ccc;
  cursor: not-allowed;
  opacity: 0.7;
}

.custom-dropdown .dropdown-item.disabled:hover {
  background-color: transparent;
  color: #ccc;
  transform: none;
}

@keyframes dropdown-fade-in {
  from {
    opacity: 0;
    transform: translateY(-10px);
  }
  to {
    opacity: 1;
    transform: translateY(0);
  }
}
</style>