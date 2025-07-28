import React, { useState, useEffect, useRef, useMemo } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { Portal } from '@headlessui/react';
import { getClusterList, deleteCluster, getServiceListByCluster, getFrameList } from '@/api/cluster';
import { changeRouter } from '@/utils/changeRouter';
import { showSuccess, showError, showWarning } from '@/utils/notification';
import useUserStore from '@/stores/useUserStore';

// 类型声明
type TimeoutId = ReturnType<typeof setTimeout>;

interface ClusterManager {
  id: string | number;
  username: string;
}

interface ClusterItem {
  id: string | number;
  clusterName: string;
  clusterCode?: string;
  clusterFrame?: string;
  depType?: 'PVM' | 'Kubernetes' | string;
  clusterState: string;
  clusterStateCode: number; // 1: 未配置, 2: 运行中, 3: 异常
  createTime: string;
  clusterManagerList: ClusterManager[];
  userManageName?: string;
}

interface FrameworkItem {
  frameCode: string;
  frameName?: string;
}

// 简化的模态框组件
const ModalWrapper: React.FC<{
  visible: boolean;
  onClose: () => void;
  children: React.ReactNode;
  maxWidth?: string;
}> = ({ visible, onClose, children, maxWidth = 'max-w-2xl' }) => {
  if (!visible) return null;
  
  return (
    <Portal>
      <div className="fixed inset-0 bg-black/30 backdrop-blur-sm z-50 flex items-center justify-center overflow-y-auto">
        <div className={`bg-white rounded-xl shadow-xl overflow-hidden ${maxWidth} w-full my-4 mx-4 animate-scale-in`}>
          {children}
        </div>
      </div>
    </Portal>
  );
};

const ClusterListPage = () => {
  const navigate = useNavigate();
  const { user } = useUserStore();
  const [clusters, setClusters] = useState<ClusterItem[]>([]);
  const [frameworks, setFrameworks] = useState<FrameworkItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeMenuClusterId, setActiveMenuClusterId] = useState<string | number | null>(null);
  const [addModalVisible, setAddModalVisible] = useState(false);
  const [authModalVisible, setAuthModalVisible] = useState(false);
  const [configModalVisible, setConfigModalVisible] = useState(false);
  const [currentCluster, setCurrentCluster] = useState<ClusterItem | null>(null);
  const [deleteConfirmVisible, setDeleteConfirmVisible] = useState(false);
  const [hoverClusterId, setHoverClusterId] = useState<string | number | null>(null);
  const [hoverButton, setHoverButton] = useState<string | null>(null);
  
  const menuPositions = useRef<Record<string | number, { top: number; left: number }>>({});
  const menuTimeouts = useRef<Record<string | number, NodeJS.Timeout>>({});
  const containerRef = useRef<HTMLDivElement>(null);

  // 加载集群列表
  const loadClusterList = async () => {
    setLoading(true);
    try {
      const response = await getClusterList({});
      
      // 处理集群管理员名称
      const processedClusters = response.map((item: ClusterItem) => {
        const managerNames = item.clusterManagerList?.map(manager => manager.username).filter(Boolean) || [];
        return {
          ...item,
          userManageName: managerNames.join(', ') || '未分配'
        };
      });
      
      setClusters(processedClusters);
    } catch (error) {
      console.error('加载集群列表失败', error);
      showError('加载集群列表失败');
    } finally {
      setLoading(false);
    }
  };

  // 加载框架列表
  const loadFrameworkList = async () => {
    try {
      const response = await getFrameList({});
      setFrameworks(response);
    } catch (error) {
      console.error('加载框架列表失败', error);
    }
  };

  // 首次加载
  useEffect(() => {
    loadClusterList();
    loadFrameworkList();
    
    // 添加全局点击事件关闭菜单
    const handleOutsideClick = (e: MouseEvent) => {
      // 如果点击的是菜单按钮，则不关闭菜单（按钮自己会处理）
      const target = e.target as HTMLElement;
      if (target.closest('.more-menu-button')) return;
      setActiveMenuClusterId(null);
    };
    
    document.addEventListener('click', handleOutsideClick);
    
    // 添加一些视觉效果
    if (containerRef.current) {
      containerRef.current.classList.add('animate-fade-in-down');
    }
    
    return () => {
      document.removeEventListener('click', handleOutsideClick);
      // 清理所有超时
      Object.values(menuTimeouts.current).forEach(timeout => clearTimeout(timeout));
    };
  }, []);

  // 进入集群
  const handleEnterCluster = async (cluster: ClusterItem) => {
    if (cluster.clusterStateCode === 1) {
      showWarning('当前集群未配置完成，无法访问');
      return;
    }
    
    try {
      const response = await getServiceListByCluster(cluster.id);
      changeRouter(response, cluster.id.toString());
      navigate({ to: '/' });  // 修改为根路由，由changeRouter处理具体导航
    } catch (error) {
      console.error('进入集群失败', error);
      showError('进入集群失败');
    }
  };

  // 配置集群
  const handleConfigCluster = (cluster: ClusterItem) => {
    if (cluster.clusterStateCode === 2) {
      showWarning('集群正在运行中，无法进行配置');
      return;
    }
    
    setCurrentCluster(cluster);
    setConfigModalVisible(true);
  };
  
  // 授权集群
  const handleAuthCluster = (cluster: ClusterItem) => {
    setCurrentCluster(cluster);
    setAuthModalVisible(true);
  };
  
  // 添加/编辑集群
  const handleAddEditCluster = (cluster?: ClusterItem) => {
    setCurrentCluster(cluster || null);
    setAddModalVisible(true);
  };
  
  // 删除集群
  const handleDeleteCluster = async (cluster: ClusterItem) => {
    if (cluster.clusterStateCode === 2) {
      showWarning('集群正在运行中，无法删除');
      return;
    }
    
    setCurrentCluster(cluster);
    setDeleteConfirmVisible(true);
  };
  
  // 确认删除集群
  const confirmDeleteCluster = async () => {
    if (!currentCluster) return;
    
    try {
      await deleteCluster(currentCluster.id);
      showSuccess('删除成功');
      loadClusterList();
      setDeleteConfirmVisible(false);
    } catch (error) {
      console.error('删除集群失败', error);
      showError('删除集群失败');
    }
  };
  
  // 切换更多菜单
  const handleToggleMoreMenu = (clusterId: string | number, event: React.MouseEvent) => {
    event.stopPropagation();
    event.preventDefault();
    
    // 如果点击的是当前打开的菜单，则关闭
    if (activeMenuClusterId === clusterId) {
      setActiveMenuClusterId(null);
      return;
    }
    
    // 否则，打开新菜单并计算位置
    setActiveMenuClusterId(clusterId);
    
    // 计算菜单位置
    const buttonElement = event.currentTarget as HTMLElement;
    const rect = buttonElement.getBoundingClientRect();
    
    menuPositions.current[clusterId] = {
      top: rect.bottom + window.scrollY + 5,
      left: rect.right - 140 + window.scrollX
    };
  };

  // 根据集群类型获取样式类
  const getClusterTypeClass = (depType?: string) => {
    switch (depType) {
      case 'PVM':
        return 'cluster-card-linux';
      case 'Kubernetes':
        return 'cluster-card-k8s';
      default:
        return 'cluster-card-default';
    }
  };
  
  // 根据集群状态获取样式类
  const getStatusClass = (statusCode: number) => {
    switch (statusCode) {
      case 2:
        return 'cluster-status-running';
      case 3:
        return 'cluster-status-error';
      default:
        return 'cluster-status-configured';
    }
  };
  
  // 获取集群类型文本
  const getClusterTypeText = (depType?: string) => {
    switch (depType) {
      case 'PVM':
        return '裸金属/虚拟机';
      case 'Kubernetes':
        return 'Kubernetes';
      default:
        return '未知';
    }
  };
  
  // 格式化日期
  const formatDate = (dateString: string) => {
    if (!dateString) return '-';
    const date = new Date(dateString);
    return date.toLocaleDateString('zh-CN', {
      year: 'numeric',
      month: '2-digit',
      day: '2-digit'
    });
  };
  
  // 获取菜单位置样式
  const getMenuPosition = (clusterId: string | number) => {
    const position = menuPositions.current[clusterId] || {};
    return {
      top: `${position.top || 0}px`,
      left: `${position.left || 0}px`
    };
  };

  // 获取状态徽章内容
  const getStatusBadgeContent = (statusCode: number, statusText: string) => {
    let icon = '';
    
    switch (statusCode) {
      case 2: // 运行中
        icon = 'i-carbon-checkmark-filled';
        break;
      case 3: // 异常
        icon = 'i-carbon-warning-filled';
        break;
      default: // 未配置
        icon = 'i-carbon-time';
        break;
    }
    
    return (
      <div className="flex items-center gap-1.5">
        <div className={icon}></div>
        <span>{statusText}</span>
      </div>
    );
  };

  // 获取卡片动画效果
  const getCardAnimationStyle = (clusterId: string | number) => {
    return {
      transform: hoverClusterId === clusterId ? 'translateY(-4px)' : 'translateY(0)',
      boxShadow: hoverClusterId === clusterId ? '0 12px 20px rgba(0, 0, 0, 0.1)' : '0 1px 3px rgba(0, 0, 0, 0.1)',
    };
  };

  return (
    <div ref={containerRef} className="px-6 py-8 min-h-screen bg-gray-50">
      {/* 页面头部横幅 */}
      <div className="relative overflow-hidden bg-white rounded-xl shadow-card backdrop-blur-apple p-8 mb-8 transform transition-all duration-300 hover:shadow-lg hover:-translate-y-0.5">
        {/* 顶部装饰线 */}
        <div className="absolute top-0 left-0 right-0 h-1 bg-gradient-to-r from-blue-400 via-primary to-indigo-400"></div>
        
        {/* 内部装饰效果 */}
        <div className="absolute top-0 right-0 w-64 h-64 bg-gradient-radial from-primary-100/30 to-transparent opacity-60 transform translate-x-1/2 -translate-y-1/2 rounded-full"></div>
        <div className="absolute bottom-0 left-0 w-40 h-40 bg-gradient-radial from-indigo-100/20 to-transparent opacity-60 transform -translate-x-1/2 translate-y-1/2 rounded-full"></div>
        
        <div className="relative z-10">
          <div className="flex items-center gap-3 mb-2">
            <div className="i-carbon-data-center text-primary w-7 h-7"></div>
            <h1 className="text-2xl font-semibold text-gray-900 flex items-center">
              集群管理
              <span className="ml-3 text-xs font-normal px-2 py-0.5 bg-blue-50 text-blue-600 rounded-full flex items-center gap-1">
                <div className="w-1.5 h-1.5 bg-blue-500 rounded-full animate-pulse"></div>
                {clusters.length} 个集群
              </span>
            </h1>
          </div>
          <p className="text-gray-600 pl-10 relative">
            管理和监控您的大数据集群，快速部署各类服务
            <span className="absolute left-0 top-1/2 transform -translate-y-1/2 w-8 h-px bg-gray-200"></span>
          </p>
        </div>
      </div>

      {/* 集群卡片网格 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* 现有集群卡片 */}
        {clusters.map(cluster => (
          <div
            key={cluster.id}
            className={`${getClusterTypeClass(cluster.depType)} transition-all duration-300`}
            style={getCardAnimationStyle(cluster.id)}
            onMouseEnter={() => setHoverClusterId(cluster.id)}
            onMouseLeave={() => setHoverClusterId(null)}
          >
            {/* 集群状态标签 */}
            <div className={`cluster-status-badge ${getStatusClass(cluster.clusterStateCode)} animate-fade-in-fast`}>
              {getStatusBadgeContent(cluster.clusterStateCode, cluster.clusterState)}
            </div>

            {/* 集群头部 */}
            <div className="cluster-header">
              <div className="flex items-center space-x-4">
                <div className="h-11 w-11 flex items-center justify-center rounded-full bg-gradient-to-br from-white/80 to-white/40 shadow-sm p-2 transition-all duration-300 hover:scale-105 hover:shadow-md">
                  {cluster.depType === 'PVM' && (
                    <img src="/linux-tux.svg" alt="Linux" className="w-7 h-7" />
                  )}
                  {cluster.depType === 'Kubernetes' && (
                    <img src="/kubernetes-logo.svg" alt="Kubernetes" className="w-7 h-7" />
                  )}
                  {(!cluster.depType || (cluster.depType !== 'PVM' && cluster.depType !== 'Kubernetes')) && (
                    <div className="i-carbon-cloud text-primary w-7 h-7"></div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-gray-900 truncate flex items-center gap-2">
                    {cluster.clusterName}
                    {cluster.clusterFrame && (
                      <span className="text-xs bg-gray-50 text-gray-500 px-1.5 py-0.5 rounded-md font-normal">
                        {cluster.clusterFrame}
                      </span>
                    )}
                  </h3>
                  <div className="flex flex-wrap gap-2 mt-1">
                    <span className="inline-flex items-center bg-gradient-to-r from-blue-50 to-indigo-50 px-2.5 py-0.5 rounded-md text-xs font-medium text-gray-700 border border-blue-100/50">
                      {getClusterTypeText(cluster.depType)}
                    </span>
                    <span className="text-xs text-gray-500 flex items-center">
                      <div className="i-carbon-calendar w-3.5 h-3.5 mr-1 text-gray-400"></div>
                      {formatDate(cluster.createTime)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* 集群内容 */}
            <div className="cluster-body">
              <div className="flex items-center">
                <div className="text-sm text-gray-500 mr-2 flex items-center gap-1.5">
                  <div className="i-carbon-user-admin text-gray-400 w-4 h-4"></div>
                  管理员
                </div>
                <div className="flex-1 px-3 py-1.5 text-sm bg-gray-50 rounded-md text-gray-900 border border-gray-100 hover:border-blue-200 transition-colors duration-300">
                  {cluster.userManageName || '未分配'}
                </div>
              </div>
            </div>

            {/* 卡片底部按钮区域 */}
            <div className="cluster-footer">
              <div className="flex flex-col space-y-3">
                {/* 主按钮 - 进入集群 */}
                <button
                  onClick={() => handleEnterCluster(cluster)}
                  disabled={cluster.clusterStateCode === 1}
                  className={`relative overflow-hidden ${cluster.clusterStateCode === 1 
                    ? 'cluster-button-disabled' 
                    : 'cluster-button-primary'}`}
                  onMouseEnter={() => setHoverButton(`enter-${cluster.id}`)}
                  onMouseLeave={() => setHoverButton(null)}
                >
                  {/* 按钮动画效果 */}
                  {hoverButton === `enter-${cluster.id}` && cluster.clusterStateCode !== 1 && (
                    <div className="absolute inset-0 w-full h-full">
                      <div className="absolute w-8 h-32 bg-white/30 -top-12 -left-4 transform rotate-12 translate-x-0 -translate-y-2 animate-shimmer"></div>
                    </div>
                  )}
                  
                  <span className="relative z-10 flex items-center gap-1.5 justify-center">
                    <div className="i-carbon-login w-4 h-4"></div>
                    进入集群
                    {cluster.clusterStateCode === 1 && (
                      <span className="ml-1 opacity-70">(未配置)</span>
                    )}
                  </span>
                </button>
                
                {/* 次要按钮组 */}
                <div className="grid grid-cols-3 gap-2">
                  <button
                    onClick={() => handleConfigCluster(cluster)}
                    disabled={cluster.clusterStateCode === 2}
                    className={`relative overflow-hidden ${
                      cluster.clusterStateCode === 2 
                        ? 'cluster-button-secondary-disabled'
                        : 'cluster-button-secondary-active'
                    }`}
                    onMouseEnter={() => setHoverButton(`config-${cluster.id}`)}
                    onMouseLeave={() => setHoverButton(null)}
                  >
                    {/* 悬停波纹效果 */}
                    {hoverButton === `config-${cluster.id}` && cluster.clusterStateCode !== 2 && (
                      <div className="absolute inset-0 w-full h-full flex justify-center items-center overflow-hidden">
                        <div className="w-5 h-5 rounded-full bg-primary/10 animate-ping"></div>
                      </div>
                    )}
                    
                    <span className="relative z-10 flex items-center gap-1 justify-center text-xs">
                      <div className="i-carbon-settings w-3.5 h-3.5"></div>
                      配置
                    </span>
                  </button>
                  
                  {user?.userType === 1 && (
                    <button
                      onClick={() => handleAuthCluster(cluster)}
                      className="cluster-button-secondary cluster-button-secondary-active relative overflow-hidden"
                      onMouseEnter={() => setHoverButton(`auth-${cluster.id}`)}
                      onMouseLeave={() => setHoverButton(null)}
                    >
                      {/* 悬停波纹效果 */}
                      {hoverButton === `auth-${cluster.id}` && (
                        <div className="absolute inset-0 w-full h-full flex justify-center items-center overflow-hidden">
                          <div className="w-5 h-5 rounded-full bg-primary/10 animate-ping"></div>
                        </div>
                      )}
                      
                      <span className="relative z-10 flex items-center gap-1 justify-center text-xs">
                        <div className="i-carbon-user-admin w-3.5 h-3.5"></div>
                        授权
                      </span>
                    </button>
                  )}
                  
                  {/* 更多按钮 (使用下拉菜单) */}
                  <div className="relative inline-block">
                    <button
                      onClick={(event) => handleToggleMoreMenu(cluster.id, event)}
                      className="more-menu-button cluster-button-secondary cluster-button-secondary-active w-full relative overflow-hidden"
                      onMouseEnter={() => setHoverButton(`more-${cluster.id}`)}
                      onMouseLeave={() => setHoverButton(null)}
                    >
                      {/* 悬停波纹效果 */}
                      {hoverButton === `more-${cluster.id}` && (
                        <div className="absolute inset-0 w-full h-full flex justify-center items-center overflow-hidden">
                          <div className="w-5 h-5 rounded-full bg-primary/10 animate-ping"></div>
                        </div>
                      )}
                      
                      <span className="relative z-10 flex items-center gap-1 justify-center text-xs">
                        <div className="i-carbon-overflow-menu-horizontal w-3.5 h-3.5"></div>
                        更多
                      </span>
                    </button>
                  </div>
                </div>
              </div>
            </div>
          </div>
        ))}
        
        {/* 创建新集群卡片 */}
        <div 
          onClick={() => handleAddEditCluster()}
          className="cluster-create-card group"
          onMouseEnter={() => setHoverButton('create')}
          onMouseLeave={() => setHoverButton(null)}
        >
          <div className="relative bg-gradient-to-br from-blue-50 to-indigo-50 w-16 h-16 rounded-full flex items-center justify-center mb-4 transition-all duration-300 group-hover:scale-110 group-hover:shadow-lg group-hover:shadow-blue-200/50">
            {/* 脉冲效果 */}
            <div className={`absolute inset-0 rounded-full bg-primary/30 opacity-0 ${hoverButton === 'create' ? 'animate-pulse' : ''}`}></div>
            <div className={`absolute inset-[-4px] rounded-full border-2 border-primary/20 opacity-0 ${hoverButton === 'create' ? 'opacity-100' : ''}`}></div>
            
            <div className="i-carbon-add w-8 h-8 text-primary relative z-10"></div>
            
            {/* 光晕效果 */}
            {hoverButton === 'create' && (
              <div className="absolute inset-0 bg-gradient-radial from-blue-400/20 via-transparent to-transparent animate-pulse"></div>
            )}
          </div>
          
          <h3 className="text-xl font-semibold text-gray-900 mb-2 transition-all duration-300 group-hover:text-primary group-hover:scale-105">创建新集群</h3>
          <p className="text-gray-600 mb-6">快速部署一个全新的大数据集群环境</p>
          <div className="flex flex-wrap justify-center gap-2">
            <span className="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded transition-all duration-300 group-hover:bg-blue-100">一键部署</span>
            <span className="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded transition-all duration-300 group-hover:bg-blue-100 group-hover:delay-75">智能配置</span>
            <span className="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded transition-all duration-300 group-hover:bg-blue-100 group-hover:delay-150">高效运维</span>
          </div>
          
          {/* 悬浮时显示的箭头 */}
          <div className={`mt-6 flex justify-center transition-all duration-300 ${hoverButton === 'create' ? 'opacity-100 translate-y-0' : 'opacity-0 -translate-y-4'}`}>
            <div className="w-8 h-8 rounded-full bg-blue-100 flex items-center justify-center">
              <div className="i-carbon-arrow-right w-4 h-4 text-primary"></div>
            </div>
          </div>
        </div>
      </div>

      {/* 下拉菜单 */}
      {activeMenuClusterId !== null && (
        <Portal>
          <div
            style={getMenuPosition(activeMenuClusterId)}
            className="glass-morphism-menu fixed z-50 w-40 rounded-xl shadow-menu overflow-hidden animate-scale-in origin-top"
          >
            <div className="py-1">
              {/* 编辑集群 */}
              {clusters.map(cluster => 
                cluster.id === activeMenuClusterId && (
                  <React.Fragment key={cluster.id}>
                    <button
                      className="flex w-full px-4 py-2.5 text-left text-sm hover:bg-gray-100 hover:text-gray-900 text-gray-700 transition-colors duration-150 items-center space-x-2"
                      onClick={() => {
                        setActiveMenuClusterId(null);
                        handleAddEditCluster(cluster);
                      }}
                    >
                      <div className="i-carbon-edit w-4 h-4"></div>
                      <span>编辑集群</span>
                    </button>
                    <button
                      className={`flex w-full px-4 py-2.5 text-left text-sm hover:bg-red-50 hover:text-red-700 text-red-600 transition-colors duration-150 items-center space-x-2 ${
                        cluster.clusterStateCode === 2 ? 'opacity-50 cursor-not-allowed' : ''
                      }`}
                      disabled={cluster.clusterStateCode === 2}
                      onClick={() => {
                        setActiveMenuClusterId(null);
                        if (cluster.clusterStateCode !== 2) {
                          handleDeleteCluster(cluster);
                        }
                      }}
                    >
                      <div className="i-carbon-trash-can w-4 h-4"></div>
                      <span>删除集群</span>
                    </button>
                  </React.Fragment>
                )
              )}
            </div>
          </div>
        </Portal>
      )}
      
      {/* 添加/编辑集群对话框 */}
      <ModalWrapper
        visible={addModalVisible}
        onClose={() => setAddModalVisible(false)}
        maxWidth="max-w-2xl"
      >
        <div className="p-6">
          <h2 className="text-2xl font-semibold text-gray-900 mb-4">
            {currentCluster ? '编辑集群' : '添加集群'}
          </h2>
          <div className="bg-blue-50 p-4 rounded-lg text-center my-4">
            <p>集群表单组件将在后续实现</p>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button 
              onClick={() => setAddModalVisible(false)}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              取消
            </button>
            <button 
              onClick={() => {
                showSuccess('操作成功');
                setAddModalVisible(false);
                loadClusterList();
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              确定
            </button>
          </div>
        </div>
      </ModalWrapper>
        
      {/* 授权集群对话框 */}
      <ModalWrapper
        visible={authModalVisible}
        onClose={() => setAuthModalVisible(false)}
        maxWidth="max-w-md"
      >
        <div className="p-6">
          <h2 className="text-2xl font-semibold text-gray-900 mb-4">
            授权集群
          </h2>
          <div className="bg-blue-50 p-4 rounded-lg text-center my-4">
            <p>授权组件将在后续实现</p>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button 
              onClick={() => setAuthModalVisible(false)}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              取消
            </button>
            <button 
              onClick={() => {
                showSuccess('授权成功');
                setAuthModalVisible(false);
                loadClusterList();
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              确定
            </button>
          </div>
        </div>
      </ModalWrapper>
        
      {/* 配置集群对话框 */}
      <ModalWrapper
        visible={configModalVisible}
        onClose={() => {
          setConfigModalVisible(false);
          loadClusterList();
        }}
        maxWidth="max-w-6xl"
      >
        <div className="p-6">
          <h2 className="text-2xl font-semibold text-gray-900 mb-4">
            配置集群 - {currentCluster?.clusterName || ''}
          </h2>
          <div className="bg-blue-50 p-4 rounded-lg text-center my-4">
            <p>配置向导将在后续实现</p>
          </div>
          <div className="flex justify-end gap-3 mt-6">
            <button 
              onClick={() => setConfigModalVisible(false)}
              className="px-4 py-2 border border-gray-300 rounded-lg hover:bg-gray-50"
            >
              取消
            </button>
            <button 
              onClick={() => {
                showSuccess('配置已保存');
                setConfigModalVisible(false);
                loadClusterList();
              }}
              className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700"
            >
              确定
            </button>
          </div>
        </div>
      </ModalWrapper>
      
      {/* 删除确认对话框 */}
      {deleteConfirmVisible && (
        <Portal>
          <div className="fixed inset-0 bg-black/30 backdrop-blur-sm z-50 flex items-center justify-center">
            <div className="bg-white rounded-xl shadow-xl p-6 max-w-md w-full mx-4 animate-scale-in">
              <div className="text-center mb-5">
                <div className="w-16 h-16 mx-auto bg-red-100 rounded-full flex items-center justify-center mb-4">
                  <div className="i-carbon-warning-filled w-8 h-8 text-red-500"></div>
                </div>
                <h3 className="text-xl font-semibold text-gray-900 mb-2">确认删除</h3>
                <p className="text-gray-600">确认删除集群 "{currentCluster?.clusterName}"？此操作无法撤销。</p>
              </div>
              
              <div className="flex justify-center gap-4 mt-6">
                <button
                  onClick={confirmDeleteCluster}
                  className="px-5 py-2.5 bg-red-600 text-white rounded-lg hover:bg-red-700 transition-colors duration-300 flex items-center gap-2"
                >
                  <div className="i-carbon-trash-can w-4 h-4"></div>
                  确认删除
                </button>
                <button
                  onClick={() => setDeleteConfirmVisible(false)}
                  className="px-5 py-2.5 border border-gray-300 text-gray-700 rounded-lg hover:bg-gray-100 transition-colors duration-300"
                >
                  取消
                </button>
              </div>
            </div>
          </div>
        </Portal>
      )}
      
      {/* 加载状态覆盖 */}
      {loading && (
        <div className="fixed inset-0 bg-white/50 backdrop-blur-sm z-50 flex items-center justify-center">
          <div className="bg-white rounded-xl shadow-xl p-6 flex items-center gap-4">
            <div className="w-8 h-8 border-4 border-t-primary border-blue-200 rounded-full animate-spin"></div>
            <div className="text-gray-600">正在加载集群数据...</div>
          </div>
        </div>
      )}
    </div>
  );
};

export default ClusterListPage; 