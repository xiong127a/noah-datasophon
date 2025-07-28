import React, { useState, useEffect } from 'react';
import { Portal } from '@headlessui/react';
import { authCluster } from '@/api/cluster';
import { showSuccess, showError } from '@/utils/notification';
import { get } from '@/api/http';
import API_PATHS from '@/api/apiPaths';

interface User {
  id: string | number;
  username: string;
}

interface ClusterManager {
  id: string | number;
  username: string;
}

interface ClusterItem {
  id?: string | number;
  clusterName?: string;
  clusterManagerList?: ClusterManager[];
  [key: string]: any;
}

interface AuthClusterModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess: () => void;
  cluster: ClusterItem | null;
}

const AuthClusterModal: React.FC<AuthClusterModalProps> = ({
  visible,
  onClose,
  onSuccess,
  cluster
}) => {
  const [loading, setLoading] = useState(false);
  const [userList, setUserList] = useState<User[]>([]);
  const [selectedUserIds, setSelectedUserIds] = useState<(string | number)[]>([]);
  const [userListLoaded, setUserListLoaded] = useState(false);
  
  // 获取所有用户
  const queryAllUsers = async () => {
    try {
      const response = await get(API_PATHS.queryAllUser);
      setUserList(response);
      setUserListLoaded(true);
    } catch (error) {
      console.error('获取用户列表失败:', error);
      showError('获取用户列表失败');
    }
  };
  
  // 初始化当前已选用户
  useEffect(() => {
    if (visible) {
      queryAllUsers();
      
      // 如果有集群，设置当前已选用户
      if (cluster && cluster.clusterManagerList) {
        const userIds = cluster.clusterManagerList.map(manager => manager.id);
        setSelectedUserIds(userIds);
      } else {
        setSelectedUserIds([]);
      }
    }
  }, [visible, cluster]);
  
  // 处理用户选择变更
  const handleUserSelect = (userId: string | number) => {
    setSelectedUserIds(prev => {
      if (prev.includes(userId)) {
        return prev.filter(id => id !== userId);
      } else {
        return [...prev, userId];
      }
    });
  };
  
  // 处理表单提交
  const handleSubmit = async () => {
    if (!cluster?.id) {
      showError('缺少集群ID参数');
      return;
    }
    
    setLoading(true);
    
    try {
      await authCluster({
        clusterId: cluster.id,
        userIds: selectedUserIds.join(',')
      });
      
      showSuccess(selectedUserIds.length > 0 ? '授权成功' : '取消授权成功');
      onSuccess();
    } catch (error) {
      console.error('授权失败:', error);
      showError('授权失败');
    } finally {
      setLoading(false);
    }
  };
  
  if (!visible) return null;

  return (
    <Portal>
      <div className="fixed inset-0 bg-black/30 backdrop-blur-sm z-50 flex items-center justify-center overflow-y-auto">
        <div className="bg-white rounded-xl shadow-xl overflow-hidden max-w-md w-full my-4 mx-4 animate-scale-in">
          {/* 顶部区域 */}
          <div className="relative overflow-hidden bg-gradient-to-br from-blue-400 to-blue-600">
            {/* 背景装饰 */}
            <div className="absolute -top-full -left-full right-0 bottom-0 bg-radial-gradient opacity-70 transform -rotate-35 z-0"></div>
            
            <div className="relative z-10 flex items-center py-7 px-8">
              {/* 用户图标 */}
              <div className="relative mr-5">
                {/* 脉冲动画圈 */}
                <div className="absolute inset-[-4px] rounded-full border-2 border-white/40 animate-pulse-slow"></div>
                <div className="absolute inset-[-8px] rounded-full border border-white/20 animate-pulse-slow delay-500"></div>
                
                <div className="w-13 h-13 bg-white/25 rounded-full flex items-center justify-center relative overflow-hidden shadow-md">
                  <div className="absolute inset-0 bg-gradient-to-br from-white/40 to-transparent"></div>
                  <div className="i-carbon-user-admin w-6 h-6 text-white relative z-10"></div>
                </div>
              </div>
              
              {/* 标题 */}
              <div className="text-white">
                <h1 className="text-xl font-semibold mb-2 drop-shadow-sm">集群授权管理</h1>
                <p className="text-sm opacity-95 drop-shadow-sm">
                  为集群 <span className="font-semibold bg-white/25 rounded-md px-2 py-1 mx-1 shadow-sm">{cluster?.clusterName || '未知集群'}</span> 分配管理员权限
                </p>
              </div>
            </div>
          </div>
          
          {/* 内容区域 */}
          <div className="p-6 bg-gradient-to-b from-gray-50 to-gray-100 flex-1 relative">
            {/* 顶部分割线效果 */}
            <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-white/80 to-transparent"></div>

            <div className="max-w-lg mx-auto bg-white rounded-lg shadow-sm p-7 border border-blue-500/10">
              <div className="mb-5">
                <label className="block mb-3 text-sm font-medium text-gray-800 relative pl-3">
                  {/* 蓝色垂直指示条 */}
                  <span className="absolute left-0 top-1/2 -translate-y-1/2 w-1 h-[18px] bg-gradient-to-b from-blue-400 to-blue-600 rounded"></span>
                  选择管理员：
                </label>
                
                <div className="relative">
                  {/* 用户选择区域 */}
                  {userListLoaded ? (
                    <div className="border border-gray-300 rounded-lg max-h-60 overflow-y-auto divide-y divide-gray-100">
                      {userList.length === 0 ? (
                        <div className="py-4 px-3 text-center text-gray-500">
                          暂无可选用户
                        </div>
                      ) : (
                        userList.map(user => (
                          <div 
                            key={user.id}
                            onClick={() => handleUserSelect(user.id)}
                            className={`
                              flex items-center px-3 py-2.5 cursor-pointer transition-colors duration-200
                              ${selectedUserIds.includes(user.id) ? 'bg-blue-50' : 'hover:bg-gray-50'}
                            `}
                          >
                            <div className={`
                              w-5 h-5 border rounded mr-3 flex items-center justify-center transition-all duration-200
                              ${selectedUserIds.includes(user.id) 
                                ? 'bg-blue-500 border-blue-500' 
                                : 'border-gray-300 bg-white'}
                            `}>
                              {selectedUserIds.includes(user.id) && (
                                <div className="i-carbon-checkmark w-3 h-3 text-white"></div>
                              )}
                            </div>
                            <span className="text-gray-700">{user.username}</span>
                          </div>
                        ))
                      )}
                    </div>
                  ) : (
                    <div className="flex items-center justify-center h-11 px-4 bg-blue-50 border border-blue-200/50 rounded-lg text-gray-500">
                      <div className="w-4 h-4 border-2 border-blue-100 border-t-blue-500 rounded-full mr-2.5 animate-spin"></div>
                      <span>加载用户数据中...</span>
                    </div>
                  )}
                </div>
              </div>
            </div>
          </div>
          
          {/* 底部按钮 */}
          <div className="px-7 py-6 bg-white border-t border-gray-200 flex justify-center">
            <div className="flex gap-4.5">
              <button 
                onClick={handleSubmit}
                disabled={loading}
                className={`
                  min-w-[130px] h-[42px] rounded-full text-white font-medium text-sm
                  bg-gradient-to-r from-blue-400 via-blue-500 to-blue-600 
                  shadow-md shadow-blue-500/25 relative overflow-hidden
                  transition-all duration-300 transform hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/30
                  focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                  active:translate-y-0 active:shadow-md active:shadow-blue-500/25
                  disabled:opacity-70 disabled:cursor-not-allowed
                `}
              >
                <span className="relative z-10 flex items-center justify-center">
                  {loading && (
                    <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin mr-2"></div>
                  )}
                  确认授权
                </span>
                <div className="absolute inset-0 bg-gradient-to-br from-white/30 to-transparent opacity-0 hover:opacity-100 transition-opacity duration-300"></div>
              </button>
              
              <button 
                onClick={onClose}
                className={`
                  min-w-[130px] h-[42px] rounded-full text-gray-600 font-medium text-sm
                  border border-gray-200 bg-white
                  transition-all duration-300 transform hover:-translate-y-0.5 hover:border-blue-500 hover:text-blue-600 hover:shadow-md
                  focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                  active:translate-y-0
                `}
              >
                <span className="relative z-10">取消</span>
              </button>
            </div>
          </div>
        </div>
      </div>
    </Portal>
  );
};

export default AuthClusterModal; 