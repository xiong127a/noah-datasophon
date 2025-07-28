import React, { useEffect, useState } from 'react';
import { Portal } from '@headlessui/react';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { zodResolver } from '@hookform/resolvers/zod';
import { saveCluster, updateCluster } from '@/api/cluster';
import { showSuccess, showError } from '@/utils/notification';
import useUserStore from '@/stores/useUserStore';

// 表单验证模式
const clusterSchema = z.object({
  clusterName: z.string().min(1, '请输入集群名称').max(10, '名称不能超过10个字符'),
  clusterCode: z.string().min(1, '请输入集群编码').max(10, '编码不能超过10个字符'),
  clusterFrame: z.string().min(1, '请选择集群框架'),
  depType: z.string().min(1, '请选择部署方式')
});

type ClusterFormData = z.infer<typeof clusterSchema>;

interface FrameworkItem {
  frameCode: string;
  frameName?: string;
}

interface ClusterItem {
  id?: string | number;
  clusterName?: string;
  clusterCode?: string;
  clusterFrame?: string;
  depType?: string;
  [key: string]: any;
}

interface AddClusterModalProps {
  visible: boolean;
  onClose: () => void;
  onSuccess: () => void;
  cluster: ClusterItem | null;
  frameworkList: FrameworkItem[];
}

const AddClusterModal: React.FC<AddClusterModalProps> = ({
  visible,
  onClose,
  onSuccess,
  cluster,
  frameworkList
}) => {
  const { user } = useUserStore();
  const [loading, setLoading] = useState(false);
  const isEdit = !!cluster?.id;
  
  const {
    register,
    handleSubmit,
    setValue,
    watch,
    reset,
    formState: { errors, isDirty, isValid }
  } = useForm<ClusterFormData>({
    resolver: zodResolver(clusterSchema),
    defaultValues: {
      clusterName: '',
      clusterCode: '',
      clusterFrame: '',
      depType: ''
    }
  });
  
  const depType = watch('depType');

  // 初始化表单数据
  useEffect(() => {
    if (visible && cluster) {
      setValue('clusterName', cluster.clusterName || '');
      setValue('clusterCode', cluster.clusterCode || '');
      setValue('clusterFrame', cluster.clusterFrame || '');
      setValue('depType', cluster.depType || '');
    } else if (visible) {
      reset({
        clusterName: '',
        clusterCode: '',
        clusterFrame: '',
        depType: ''
      });
    }
  }, [visible, cluster, setValue, reset]);
  
  // 处理提交
  const onSubmit = async (data: ClusterFormData) => {
    if (!user) {
      showError('无法获取用户信息，请重新登录后再试');
      return;
    }
    
    const params = {
      ...data,
      createBy: user.username,
      clusterManagerList: [{ id: user.userId }]
    };
    
    // 如果是编辑模式，添加ID
    if (isEdit && cluster?.id) {
      params.id = cluster.id;
    }
    
    setLoading(true);
    
    try {
      // 调用不同的API
      const res = isEdit 
        ? await updateCluster(params)
        : await saveCluster(params);
      
      showSuccess(isEdit ? '更新成功' : '创建成功');
      onSuccess();
    } catch (error) {
      console.error('保存集群失败:', error);
      showError('保存失败');
    } finally {
      setLoading(false);
    }
  };

  // 切换部署类型
  const handleDepTypeSelect = (type: string) => {
    setValue('depType', type, { shouldValidate: true, shouldDirty: true });
  };

  if (!visible) return null;

  return (
    <Portal>
      <div className="fixed inset-0 bg-black/30 backdrop-blur-sm z-50 flex items-center justify-center overflow-y-auto">
        <div className="bg-white rounded-xl shadow-xl overflow-hidden max-w-2xl w-full my-4 mx-4 animate-scale-in">
          {/* 顶部区域 */}
          <div className="relative overflow-hidden bg-gradient-to-r from-blue-500 to-blue-700">
            {/* 径向渐变效果 */}
            <div className="absolute -top-40 -right-40 w-80 h-80 bg-gradient-radial from-white/20 to-transparent opacity-70 transform -rotate-30"></div>
            <div className="relative z-10 text-center py-6">
              <h2 className="text-xl font-semibold text-white mb-1">
                {isEdit ? '编辑集群' : '创建新集群'}
              </h2>
              <p className="text-sm text-white/90">
                {isEdit ? '修改集群配置信息' : '配置您的大数据平台集群信息'}
              </p>
            </div>
          </div>
          
          {/* 内容区域 */}
          <div className="p-6 bg-gray-50 max-h-[calc(80vh-120px)] overflow-y-auto">
            <form onSubmit={handleSubmit(onSubmit)} className="space-y-6">
              {/* 基本信息部分 */}
              <div className="bg-white rounded-xl p-5 shadow-sm hover:-translate-y-0.5 hover:shadow transition duration-300 ease-out">
                <div className="flex items-center mb-2.5">
                  <div className="w-4.5 h-4.5 bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                    <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
                  </div>
                  <h2 className="text-base font-semibold text-gray-800">基本信息</h2>
                </div>
                
                <p className="text-xs text-gray-500 mb-4 ml-7">设置集群的基本标识信息</p>
                
                <div className="flex flex-wrap gap-5">
                  {/* 集群名称 */}
                  <div className="flex-1 min-w-[300px]">
                    <label className="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                      集群名称
                      <span className="text-red-500">*</span>
                    </label>
                    
                    <input
                      {...register('clusterName')}
                      type="text"
                      placeholder="请输入集群名称"
                      maxLength={10}
                      className={`
                        w-full h-[38px] rounded-lg border px-3 py-1 text-sm
                        transition duration-300 ease-out
                        focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                        hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5
                        ${errors.clusterName ? 'border-red-500' : 'border-gray-300'}
                      `}
                    />
                    {errors.clusterName && (
                      <div className="mt-1.5 flex items-center text-xs text-red-500">
                        <div className="i-carbon-warning-alt mr-1 w-3.5 h-3.5"></div>
                        {errors.clusterName.message}
                      </div>
                    )}
                  </div>
                  
                  {/* 集群编码 */}
                  <div className="flex-1 min-w-[300px]">
                    <label className="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                      集群编码
                      <span className="text-red-500">*</span>
                    </label>
                    
                    <input
                      {...register('clusterCode')}
                      type="text"
                      placeholder="请输入集群编码"
                      maxLength={10}
                      className={`
                        w-full h-[38px] rounded-lg border px-3 py-1 text-sm
                        transition duration-300 ease-out
                        focus:border-blue-500 focus:outline-none focus:ring-2 focus:ring-blue-500/20
                        hover:border-blue-500 hover:shadow-sm hover:shadow-blue-500/5
                        ${errors.clusterCode ? 'border-red-500' : 'border-gray-300'}
                      `}
                    />
                    {errors.clusterCode && (
                      <div className="mt-1.5 flex items-center text-xs text-red-500">
                        <div className="i-carbon-warning-alt mr-1 w-3.5 h-3.5"></div>
                        {errors.clusterCode.message}
                      </div>
                    )}
                  </div>
                </div>
              </div>

              {/* 集群框架部分 */}
              <div className="bg-white rounded-xl p-5 shadow-sm hover:-translate-y-0.5 hover:shadow transition duration-300 ease-out">
                <div className="flex items-center mb-2.5">
                  <div className="w-4.5 h-4.5 bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                    <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
                  </div>
                  <h2 className="text-base font-semibold text-gray-800">集群框架</h2>
                </div>
                
                <p className="text-xs text-gray-500 mb-4 ml-7">选择集群所使用的框架类型</p>
                
                <div className="relative w-full">
                  <label className="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                    集群框架
                    <span className="text-red-500">*</span>
                  </label>
                  
                  <select
                    {...register('clusterFrame')}
                    className={`
                      w-full rounded-xl bg-white py-2.5 pl-3.5 pr-10 text-left border
                      focus:outline-none focus:border-blue-500 focus:ring-2 focus:ring-blue-500/20
                      hover:border-blue-400 transition duration-200 text-sm appearance-none
                      ${errors.clusterFrame ? 'border-red-500' : 'border-gray-300'}
                    `}
                  >
                    <option value="">请选择集群框架</option>
                    {frameworkList.map(framework => (
                      <option key={framework.frameCode} value={framework.frameCode}>
                        {framework.frameCode}
                      </option>
                    ))}
                  </select>
                  
                  <div className="pointer-events-none absolute right-3 top-[38px] flex items-center text-gray-400">
                    <div className="i-carbon-chevron-down w-5 h-5"></div>
                  </div>
                  
                  {errors.clusterFrame && (
                    <div className="mt-1.5 flex items-center text-xs text-red-500">
                      <div className="i-carbon-warning-alt mr-1 w-3.5 h-3.5"></div>
                      {errors.clusterFrame.message}
                    </div>
                  )}
                </div>
              </div>
              
              {/* 部署方式部分 */}
              <div className="bg-white rounded-xl p-5 shadow-sm hover:-translate-y-0.5 hover:shadow transition duration-300 ease-out">
                <div className="flex items-center mb-2.5">
                  <div className="w-4.5 h-4.5 bg-gradient-to-b from-blue-400 to-blue-600 rounded-full mr-2.5 relative shadow-blue-400/30 shadow-sm">
                    <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-2 h-2 bg-white rounded-full"></div>
                  </div>
                  <h2 className="text-base font-semibold text-gray-800">部署方式</h2>
                </div>
                
                <div className="mb-2">
                  <label className="flex items-center gap-1 text-sm font-medium text-gray-700 mb-2">
                    部署方式
                    <span className="text-red-500">*</span>
                  </label>
                </div>
                
                <p className="text-xs text-gray-500 mb-4">选择集群部署方式（传统部署或Kubernetes容器化部署）</p>
                
                <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
                  {/* 传统部署选择 */}
                  <div 
                    onClick={() => !isEdit && handleDepTypeSelect('PVM')}
                    className={`
                      relative flex items-center border rounded-xl p-4.5 transition-all duration-300 
                      ${isEdit ? 'cursor-default' : 'cursor-pointer hover:border-blue-400 hover:-translate-y-0.5 hover:shadow-sm'}
                      ${depType === 'PVM' ? 'border-blue-500 bg-blue-50/50 shadow-md' : 'border-gray-200'}
                    `}
                  >
                    {/* 图标 */}
                    <div className={`
                      w-12 h-12 flex items-center justify-center bg-gray-50 rounded-full mr-4 flex-shrink-0
                      transition duration-300 ease-out shadow-sm
                      ${depType === 'PVM' ? 'bg-blue-100/70 shadow-blue-500/20' : ''}
                    `}>
                      <img src="/linux-tux.svg" alt="Linux" className="w-6 h-6" />
                    </div>
                    
                    {/* 内容 */}
                    <div className="flex-1">
                      <h3 className={`
                        text-[15px] font-semibold mb-1.5 transition duration-300
                        ${depType === 'PVM' ? 'text-blue-600' : 'text-gray-800'}
                      `}>
                        传统部署
                      </h3>
                      <p className="text-xs text-gray-500 leading-snug">
                        传统部署，适合大规模稳定业务
                      </p>
                    </div>
                    
                    {/* 选中标记 */}
                    {depType === 'PVM' && (
                      <div className="absolute top-4 right-4 w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center shadow-md shadow-blue-500/30">
                        <div className="i-carbon-checkmark text-white w-3.5 h-3.5"></div>
                      </div>
                    )}
                  </div>
                  
                  {/* Kubernetes选择 */}
                  <div 
                    onClick={() => !isEdit && handleDepTypeSelect('Kubernetes')}
                    className={`
                      relative flex items-center border rounded-xl p-4.5 transition-all duration-300
                      ${isEdit ? 'cursor-default' : 'cursor-pointer hover:border-blue-400 hover:-translate-y-0.5 hover:shadow-sm'}
                      ${depType === 'Kubernetes' ? 'border-blue-500 bg-blue-50/50 shadow-md' : 'border-gray-200'}
                    `}
                  >
                    {/* 图标 */}
                    <div className={`
                      w-12 h-12 flex items-center justify-center bg-gray-50 rounded-full mr-4 flex-shrink-0
                      transition duration-300 ease-out shadow-sm
                      ${depType === 'Kubernetes' ? 'bg-blue-100/70 shadow-blue-500/20' : ''}
                    `}>
                      <img src="/kubernetes-logo.svg" alt="Kubernetes" className="w-6 h-6" />
                    </div>
                    
                    {/* 内容 */}
                    <div className="flex-1">
                      <h3 className={`
                        text-[15px] font-semibold mb-1.5 transition duration-300
                        ${depType === 'Kubernetes' ? 'text-blue-600' : 'text-gray-800'}
                      `}>
                        Kubernetes
                      </h3>
                      <p className="text-xs text-gray-500 leading-snug">
                        容器化部署，支持自动化和弹性伸缩
                      </p>
                    </div>
                    
                    {/* 选中标记 */}
                    {depType === 'Kubernetes' && (
                      <div className="absolute top-4 right-4 w-6 h-6 bg-blue-500 rounded-full flex items-center justify-center shadow-md shadow-blue-500/30">
                        <div className="i-carbon-checkmark text-white w-3.5 h-3.5"></div>
                      </div>
                    )}
                  </div>
                </div>
                
                {/* 隐藏的输入字段，用于表单验证 */}
                <input type="hidden" {...register('depType')} />
                
                {errors.depType && (
                  <div className="mt-3 flex items-center text-xs text-red-500">
                    <div className="i-carbon-warning-alt mr-1 w-3.5 h-3.5"></div>
                    {errors.depType.message}
                  </div>
                )}
              </div>
            </form>
          </div>
          
          {/* 按钮区域 */}
          <div className="flex justify-center py-5 px-7 bg-white border-t border-gray-100 gap-4">
            <button
              onClick={handleSubmit(onSubmit)}
              disabled={loading}
              className={`
                min-w-[120px] h-10 rounded-full text-white font-medium text-sm
                bg-gradient-to-r from-blue-500 to-blue-700 
                shadow-md shadow-blue-500/30 relative overflow-hidden
                transition-all duration-300 ease-out hover:-translate-y-0.5 hover:shadow-lg hover:shadow-blue-500/40
                focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                active:translate-y-0 active:shadow-sm active:shadow-blue-500/30
                disabled:opacity-70 disabled:cursor-not-allowed
              `}
            >
              <span className="relative z-10 flex items-center justify-center">
                {loading && (
                  <div className="w-4 h-4 border-2 border-white/20 border-t-white rounded-full animate-spin mr-2"></div>
                )}
                {isEdit ? '保存修改' : '创建集群'}
              </span>
              <div className="absolute inset-0 bg-gradient-to-br from-white/30 to-transparent opacity-0 hover:opacity-100 transition-opacity duration-300"></div>
            </button>
            
            <button
              onClick={onClose}
              className={`
                min-w-[120px] h-10 rounded-full text-gray-600 font-medium text-sm
                border border-gray-200 bg-white
                transition-all duration-300 ease-out hover:-translate-y-0.5 hover:border-blue-500 hover:text-blue-600 hover:shadow-sm
                focus:outline-none focus:ring-2 focus:ring-blue-500/50 focus:ring-offset-2
                active:translate-y-0
              `}
            >
              取消
            </button>
          </div>
        </div>
      </div>
    </Portal>
  );
};

export default AddClusterModal; 