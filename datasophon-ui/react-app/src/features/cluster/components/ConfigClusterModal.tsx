import React, { useState, useEffect } from 'react';
import { Portal } from '@headlessui/react';
import { post } from '@/api/http';
import { showSuccess, showError, showWarning } from '@/utils/notification';
import API_PATHS from '@/api/apiPaths';

interface ClusterItem {
  id?: string | number;
  clusterName?: string;
  depType?: string;
  [key: string]: any;
}

interface ConfigClusterModalProps {
  visible: boolean;
  onClose: () => void;
  cluster: ClusterItem | null;
}

interface StepConfig {
  title: string;
  key: string;
  completed: boolean;
  current: boolean;
}

const ConfigClusterModal: React.FC<ConfigClusterModalProps> = ({
  visible,
  onClose,
  cluster
}) => {
  const [loading, setLoading] = useState(false);
  const [currentStep, setCurrentStep] = useState(0);
  const [loadingConfig, setLoadingConfig] = useState(true);
  const [configData, setConfigData] = useState<any>(null);
  
  // 步骤配置
  const steps: StepConfig[] = [
    { title: '选择服务', key: 'select-service', completed: false, current: true },
    { title: '分配角色', key: 'role-assignment', completed: false, current: false },
    { title: '配置参数', key: 'configure-params', completed: false, current: false },
    { title: '部署确认', key: 'deploy-confirm', completed: false, current: false }
  ];

  // 加载集群配置
  useEffect(() => {
    if (visible && cluster?.id) {
      loadClusterConfig();
    }
  }, [visible, cluster]);
  
  // 加载集群配置
  const loadClusterConfig = async () => {
    if (!cluster?.id) return;
    
    setLoadingConfig(true);
    
    try {
      const response = await post(API_PATHS.getClusterServiceConfigs, { clusterId: cluster.id });
      setConfigData(response);
      // 设置步骤
      determineCurrentStep(response);
    } catch (error) {
      console.error('加载集群配置失败:', error);
      showError('加载配置失败');
    } finally {
      setLoadingConfig(false);
    }
  };
  
  // 确定当前步骤
  const determineCurrentStep = (config: any) => {
    // 这里根据配置数据确定当前步骤
    // 实际实现可能需要根据API返回的数据结构来确定
    setCurrentStep(0); // 默认设置为第一步
  };
  
  // 开始部署
  const startDeploy = async () => {
    if (!cluster?.id) {
      showWarning('集群ID不存在');
      return;
    }
    
    setLoading(true);
    
    try {
      await post(API_PATHS.startClusterDeploy, { clusterId: cluster.id });
      showSuccess('部署任务已启动');
      onClose(); // 关闭对话框
    } catch (error) {
      console.error('启动部署失败:', error);
      showError('部署启动失败');
    } finally {
      setLoading(false);
    }
  };
  
  // 验证集群配置
  const validateConfig = async () => {
    if (!cluster?.id) {
      showWarning('集群ID不存在');
      return;
    }
    
    setLoading(true);
    
    try {
      await post(API_PATHS.validateClusterConfigs, { clusterId: cluster.id });
      showSuccess('配置验证通过');
      setCurrentStep(currentStep + 1);
    } catch (error) {
      console.error('配置验证失败:', error);
      showError('配置验证失败');
    } finally {
      setLoading(false);
    }
  };
  
  // 更新配置
  const updateConfig = async (config: any) => {
    if (!cluster?.id) {
      showWarning('集群ID不存在');
      return;
    }
    
    setLoading(true);
    
    try {
      await post(API_PATHS.updateClusterServiceConfigs, {
        clusterId: cluster.id,
        ...config
      });
      showSuccess('配置更新成功');
      setCurrentStep(currentStep + 1);
    } catch (error) {
      console.error('配置更新失败:', error);
      showError('配置更新失败');
    } finally {
      setLoading(false);
    }
  };
  
  // 处理下一步
  const handleNextStep = () => {
    const maxStep = steps.length - 1;
    
    if (currentStep < maxStep) {
      // 根据当前步骤执行不同的操作
      if (currentStep === 0) {
        // 选择服务后，更新配置
        updateConfig({ /* 配置数据 */ });
      } else if (currentStep === 1) {
        // 分配角色后，更新配置
        updateConfig({ /* 配置数据 */ });
      } else if (currentStep === 2) {
        // 配置参数后，验证配置
        validateConfig();
      } else if (currentStep === 3) {
        // 开始部署
        startDeploy();
      }
    }
  };
  
  // 处理上一步
  const handlePrevStep = () => {
    if (currentStep > 0) {
      setCurrentStep(currentStep - 1);
      
      // 更新步骤状态
      const updatedSteps = [...steps];
      updatedSteps[currentStep].current = false;
      updatedSteps[currentStep - 1].current = true;
    }
  };
  
  if (!visible) return null;

  return (
    <Portal>
      <div className="fixed inset-0 bg-black/30 backdrop-blur-sm z-50 flex items-center justify-center overflow-y-auto">
        <div className="bg-white rounded-xl shadow-xl overflow-hidden max-w-6xl w-full my-4 mx-4 animate-scale-in">
          {/* 顶部区域 */}
          <div className="flex justify-between items-center p-4 border-b border-gray-100">
            <h2 className="text-lg font-medium text-gray-900">
              配置集群 - {cluster?.clusterName || ''}
            </h2>
            <button 
              onClick={onClose}
              className="text-gray-400 hover:text-gray-500 transition-colors"
            >
              <div className="i-carbon-close w-5 h-5"></div>
            </button>
          </div>
          
          {/* 步骤指示器 */}
          <div className="px-6 pt-6">
            <div className="flex items-center justify-between">
              {steps.map((step, index) => (
                <React.Fragment key={step.key}>
                  {/* 步骤指示 */}
                  <div className="flex flex-col items-center">
                    <div className={`
                      relative w-10 h-10 rounded-full flex items-center justify-center 
                      ${index < currentStep ? 'bg-green-100' : ''}
                      ${index === currentStep ? 'bg-blue-100' : ''}
                      ${index > currentStep ? 'bg-gray-100' : ''}
                    `}>
                      <span className={`
                        text-sm font-medium
                        ${index < currentStep ? 'text-green-600' : ''}
                        ${index === currentStep ? 'text-blue-600' : ''}
                        ${index > currentStep ? 'text-gray-500' : ''}
                      `}>
                        {index < currentStep ? (
                          <div className="i-carbon-checkmark w-5 h-5 text-green-600"></div>
                        ) : (
                          index + 1
                        )}
                      </span>
                    </div>
                    <span className={`
                      mt-2 text-sm 
                      ${index < currentStep ? 'text-green-600 font-medium' : ''}
                      ${index === currentStep ? 'text-blue-600 font-medium' : ''}
                      ${index > currentStep ? 'text-gray-500' : ''}
                    `}>
                      {step.title}
                    </span>
                  </div>
                  
                  {/* 连接线 */}
                  {index < steps.length - 1 && (
                    <div className={`
                      h-0.5 flex-1 mx-2
                      ${index < currentStep ? 'bg-green-500' : 'bg-gray-200'}
                    `}></div>
                  )}
                </React.Fragment>
              ))}
            </div>
          </div>
          
          {/* 内容区域 */}
          <div className="p-6 max-h-[calc(80vh-170px)] overflow-y-auto">
            {loadingConfig ? (
              <div className="flex flex-col items-center justify-center py-12">
                <div className="mb-4">
                  <div className="w-10 h-10 border-4 border-blue-200 border-t-blue-500 rounded-full animate-spin"></div>
                </div>
                <p className="text-gray-600">正在加载配置向导...</p>
              </div>
            ) : (
              <div>
                {currentStep === 0 && (
                  <div className="service-selection">
                    <h3 className="text-lg font-medium mb-4">选择要安装的服务</h3>
                    {/* 服务选择组件 - 这里只展示UI示例，实际组件需要根据后端数据动态生成 */}
                    <div className="grid grid-cols-3 gap-4">
                      {['HDFS', 'YARN', 'Hive', 'Spark', 'HBase', 'Zookeeper'].map(service => (
                        <div 
                          key={service}
                          className="border border-gray-200 rounded-lg p-4 hover:border-blue-500 hover:shadow-sm cursor-pointer transition-all duration-300"
                        >
                          <div className="flex items-center justify-between">
                            <span className="font-medium">{service}</span>
                            <div className="w-5 h-5 rounded border border-gray-300"></div>
                          </div>
                        </div>
                      ))}
                    </div>
                  </div>
                )}
                
                {currentStep === 1 && (
                  <div className="role-assignment">
                    <h3 className="text-lg font-medium mb-4">分配服务角色</h3>
                    {/* 角色分配组件 */}
                    <div className="bg-gray-50 p-4 rounded-lg">
                      <p className="text-gray-500 text-center">角色分配组件将根据实际API数据生成</p>
                    </div>
                  </div>
                )}
                
                {currentStep === 2 && (
                  <div className="configure-params">
                    <h3 className="text-lg font-medium mb-4">配置服务参数</h3>
                    {/* 参数配置组件 */}
                    <div className="bg-gray-50 p-4 rounded-lg">
                      <p className="text-gray-500 text-center">参数配置组件将根据实际API数据生成</p>
                    </div>
                  </div>
                )}
                
                {currentStep === 3 && (
                  <div className="deploy-confirm">
                    <h3 className="text-lg font-medium mb-4">确认部署</h3>
                    <div className="bg-blue-50 border border-blue-100 rounded-lg p-6 text-center">
                      <div className="i-carbon-checkmark-filled w-12 h-12 text-blue-500 mx-auto mb-3"></div>
                      <p className="text-lg font-medium text-gray-800 mb-2">配置验证已通过</p>
                      <p className="text-gray-600 mb-6">集群配置已准备就绪，点击下方"开始部署"按钮启动部署流程</p>
                      
                      <div className="flex justify-center">
                        <button
                          onClick={startDeploy}
                          disabled={loading}
                          className="px-6 py-2.5 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors flex items-center gap-2 disabled:opacity-70 disabled:cursor-not-allowed"
                        >
                          {loading && (
                            <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin"></div>
                          )}
                          开始部署
                        </button>
                      </div>
                    </div>
                  </div>
                )}
              </div>
            )}
          </div>
          
          {/* 按钮区域 */}
          <div className="px-6 py-4 bg-gray-50 border-t border-gray-100 flex justify-between">
            <button
              onClick={onClose}
              className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors"
            >
              关闭
            </button>
            
            <div className="flex gap-3">
              {currentStep > 0 && (
                <button
                  onClick={handlePrevStep}
                  disabled={loading}
                  className="px-4 py-2 border border-gray-300 rounded-lg text-gray-700 hover:bg-gray-100 transition-colors disabled:opacity-70 disabled:cursor-not-allowed"
                >
                  上一步
                </button>
              )}
              
              {currentStep < steps.length - 1 && (
                <button
                  onClick={handleNextStep}
                  disabled={loading || loadingConfig}
                  className="px-4 py-2 bg-blue-600 text-white rounded-lg hover:bg-blue-700 transition-colors disabled:opacity-70 disabled:cursor-not-allowed flex items-center gap-2"
                >
                  {loading && (
                    <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin"></div>
                  )}
                  下一步
                </button>
              )}
            </div>
          </div>
        </div>
      </div>
    </Portal>
  );
};

export default ConfigClusterModal; 