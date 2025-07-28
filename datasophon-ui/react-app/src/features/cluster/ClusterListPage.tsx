import React, { useState, useEffect, useRef } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { Portal } from '@headlessui/react';
import { getClusterList, deleteCluster, getServiceListByCluster } from '@/api/cluster';
import { changeRouter } from '@/utils/changeRouter';
import { showSuccess, showError, showWarning } from '@/utils/notification';
import useUserStore from '@/stores/useUserStore';

interface ClusterManager {
  id: string | number;
  username: string;
}

interface ClusterItem {
  id: string | number;
  clusterName: string;
  depType?: 'PVM' | 'Kubernetes' | string;
  clusterState: string;
  clusterStateCode: number; // 1: 未配置, 2: 运行中, 3: 异常
  createTime: string;
  clusterManagerList: ClusterManager[];
  userManageName?: string;
}

const ClusterListPage = () => {
  const navigate = useNavigate();
  const { user } = useUserStore();
  const [clusters, setClusters] = useState<ClusterItem[]>([]);
  const [loading, setLoading] = useState(false);
  const [activeMenuClusterId, setActiveMenuClusterId] = useState<string | number | null>(null);
  const menuPositions = useRef<Record<string | number, { top: number; left: number }>>({});
  const menuTimeouts = useRef<Record<string | number, NodeJS.Timeout>>({});

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

  // 首次加载
  useEffect(() => {
    loadClusterList();
    
    // 添加全局点击事件关闭菜单
    const handleOutsideClick = () => {
      setActiveMenuClusterId(null);
    };
    
    document.addEventListener('click', handleOutsideClick);
    
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
      navigate({ to: '/service-manage' });
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
    
    // TODO: 打开集群配置对话框
    showWarning('集群配置功能开发中');
  };
  
  // 授权集群
  const handleAuthCluster = (cluster: ClusterItem) => {
    // TODO: 打开授权对话框
    showWarning('集群授权功能开发中');
  };
  
  // 添加/编辑集群
  const handleAddEditCluster = (cluster?: ClusterItem) => {
    if (cluster?.id) {
      // 编辑现有集群
      // TODO: 打开编辑对话框
      showWarning('集群编辑功能开发中');
    } else {
      // 添加新集群
      // TODO: 打开添加对话框
      showWarning('创建集群功能开发中');
    }
  };
  
  // 删除集群
  const handleDeleteCluster = async (cluster: ClusterItem) => {
    if (cluster.clusterStateCode === 2) {
      showWarning('集群正在运行中，无法删除');
      return;
    }
    
    // 确认删除
    if (!window.confirm(`确认删除集群 "${cluster.clusterName}" 吗？`)) {
      return;
    }
    
    try {
      await deleteCluster(cluster.id);
      showSuccess('删除成功');
      loadClusterList();
    } catch (error) {
      console.error('删除集群失败', error);
      showError('删除集群失败');
    }
  };
  
  // 切换更多菜单
  const handleToggleMoreMenu = (clusterId: string | number, event: React.MouseEvent) => {
    event.stopPropagation();
    
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

  return (
    <div className="px-6 py-8 min-h-screen bg-gray-50">
      {/* 页面头部横幅 */}
      <div className="bg-white rounded-xl shadow-card backdrop-blur-md p-8 mb-8">
        <div>
          <h1 className="text-2xl font-semibold text-gray-900 mb-2">集群管理</h1>
          <p className="text-gray-600">管理和监控您的大数据集群，快速部署各类服务</p>
        </div>
      </div>

      {/* 集群卡片网格 */}
      <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-6">
        {/* 现有集群卡片 */}
        {clusters.map(cluster => (
          <div
            key={cluster.id}
            className={getClusterTypeClass(cluster.depType)}
          >
            {/* 集群状态标签 */}
            <div className={`cluster-status-badge ${getStatusClass(cluster.clusterStateCode)}`}>
              {cluster.clusterState}
            </div>

            {/* 集群头部 */}
            <div className="cluster-header">
              <div className="flex items-center space-x-4">
                <div className="h-10 w-10 flex items-center justify-center">
                  {cluster.depType === 'PVM' && (
                    <img src="/linux-tux.svg" alt="Linux" className="w-9 h-9" />
                  )}
                  {cluster.depType === 'Kubernetes' && (
                    <img src="/kubernetes-logo.svg" alt="Kubernetes" className="w-9 h-9" />
                  )}
                  {(!cluster.depType || (cluster.depType !== 'PVM' && cluster.depType !== 'Kubernetes')) && (
                    <div className="i-carbon-cloud text-primary w-9 h-9"></div>
                  )}
                </div>
                <div className="flex-1 min-w-0">
                  <h3 className="text-lg font-semibold text-gray-900 truncate">
                    {cluster.clusterName}
                  </h3>
                  <div className="flex flex-wrap gap-2 mt-1">
                    <span className="inline-flex items-center bg-gray-100 px-2.5 py-0.5 rounded-md text-xs font-medium text-gray-800">
                      {getClusterTypeText(cluster.depType)}
                    </span>
                    <span className="text-xs text-gray-500">
                      {formatDate(cluster.createTime)}
                    </span>
                  </div>
                </div>
              </div>
            </div>

            {/* 集群内容 */}
            <div className="cluster-body">
              <div className="flex items-center">
                <div className="text-sm text-gray-500 mr-2">管理员</div>
                <div className="flex-1 px-3 py-1 text-sm bg-gray-50 rounded-md text-gray-900">
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
                  className={cluster.clusterStateCode === 1 
                    ? 'cluster-button-disabled' 
                    : 'cluster-button-primary'}
                >
                  <span>进入集群</span>
                  {cluster.clusterStateCode === 1 && (
                    <span className="ml-1 opacity-70">(未配置)</span>
                  )}
                </button>
                
                {/* 次要按钮组 */}
                <div className="grid grid-cols-3 gap-2">
                  <button
                    onClick={() => handleConfigCluster(cluster)}
                    disabled={cluster.clusterStateCode === 2}
                    className={`cluster-button-secondary ${
                      cluster.clusterStateCode === 2 
                        ? 'cluster-button-secondary-disabled'
                        : 'cluster-button-secondary-active'
                    }`}
                  >
                    <span>配置集群</span>
                  </button>
                  
                  {user?.userType === 1 && (
                    <button
                      onClick={() => handleAuthCluster(cluster)}
                      className="cluster-button-secondary cluster-button-secondary-active"
                    >
                      <span>授权</span>
                    </button>
                  )}
                  
                  {/* 更多按钮 (使用下拉菜单) */}
                  <div className="relative inline-block">
                    <button
                      onClick={(event) => handleToggleMoreMenu(cluster.id, event)}
                      className="cluster-button-secondary cluster-button-secondary-active w-full"
                    >
                      更多
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
          className="cluster-create-card"
        >
          <div className="bg-blue-50 w-16 h-16 rounded-full flex items-center justify-center mb-4">
            <div className="i-carbon-add w-8 h-8 text-primary"></div>
          </div>
          <h3 className="text-xl font-semibold text-gray-900 mb-2">创建新集群</h3>
          <p className="text-gray-600 mb-6">快速部署一个全新的大数据集群环境</p>
          <div className="flex flex-wrap justify-center gap-2">
            <span className="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded">一键部署</span>
            <span className="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded">智能配置</span>
            <span className="bg-blue-50 text-primary text-xs font-medium px-2.5 py-1 rounded">高效运维</span>
          </div>
        </div>
      </div>

      {/* 下拉菜单 */}
      {activeMenuClusterId !== null && (
        <Portal>
          <div
            style={getMenuPosition(activeMenuClusterId)}
            className="glass-morphism-menu fixed z-50 w-40 rounded-xl shadow-menu overflow-hidden"
          >
            <div className="py-1">
              {/* 编辑集群 */}
              {clusters.map(cluster => 
                cluster.id === activeMenuClusterId && (
                  <React.Fragment key={cluster.id}>
                    <button
                      className="flex w-full px-4 py-2 text-left text-sm hover:bg-gray-100 hover:text-gray-900 text-gray-700"
                      onClick={() => {
                        setActiveMenuClusterId(null);
                        handleAddEditCluster(cluster);
                      }}
                    >
                      编辑集群
                    </button>
                    <button
                      className={`flex w-full px-4 py-2 text-left text-sm hover:bg-red-50 hover:text-red-700 text-red-600 ${
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
                      删除集群
                    </button>
                  </React.Fragment>
                )
              )}
            </div>
          </div>
        </Portal>
      )}
    </div>
  );
};

export default ClusterListPage; 