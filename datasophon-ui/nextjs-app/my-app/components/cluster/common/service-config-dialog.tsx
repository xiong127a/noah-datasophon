"use client"

import React, { useState, useEffect, useCallback } from 'react'
import { 
  Loader2, Save, AlertTriangle, Package
} from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogTitle } from '@/components/ui/dialog'
import { Alert, AlertDescription } from '@/components/ui/alert'
import { Progress } from '@/components/ui/progress'

import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'
import { apiV1, API_PATHS_V1 } from "@/lib/api-config-v1"
import { createClusterHeaders } from '@/lib/cluster-id-header'

import type { 
  ServiceConfigDialogProps,
  ConfigItem,
  ServiceTemplate,
  FormData,
  ValidationErrors,
  Step7Data,
  SaveConfigParams,
  SaveConfigResponse,
  GenerateCommandParams,
  ServiceConfigGroupData
} from '@/types/service-config'
import { CommandType } from '@/types/service-config'
import type { Step6Data } from '@/types/worker-role-assign'

// 子组件导入
import ServiceConfigNavigation from './service-config/service-config-navigation'
import ServiceConfigContent from './service-config/service-config-content'
import ConfigurationSummary from './service-config/configuration-summary'

/**
 * 服务配置对话框 - 重构版本（现代化设计）
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-31
 * 
 * 重构特点：
 * - 组件化架构，职责清晰
 * - 现代化UI设计
 * - 优化的用户体验
 * - 更好的性能表现
 */

const ServiceConfigDialog: React.FC<ServiceConfigDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType,
  step6Data,
  onComplete,
  onPrevious
}) => {
  // 类型安全处理
  const typedStep6Data = step6Data as Step6Data
  
  // 核心状态
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [error, setError] = useState<string | null>(null)
  
  // 服务配置状态
  const [services, setServices] = useState<string[]>([])
  const [activeService, setActiveService] = useState<string>('')
  const [serviceTemplates, setServiceTemplates] = useState<ServiceTemplate>({})
  const [serviceConfigGroups, setServiceConfigGroups] = useState<Record<string, ServiceConfigGroupData>>({})
  
  // 表单状态
  const [formData, setFormData] = useState<FormData>({})
  const [validationErrors] = useState<ValidationErrors>({})
  const [configurationStatus, setConfigurationStatus] = useState<Record<string, 'pending' | 'configured' | 'saved'>>({})
  
  // UI状态
  const [showSummary] = useState(false)
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())

  // 计算步骤信息
  const isK8s = clusterType?.toLowerCase() === 'kubernetes'
  const currentStepNumber = isK8s ? 6 : 7

  // 从step6Data提取服务列表
  useEffect(() => {
    const stepData = typedStep6Data as unknown as Record<string, unknown>
    let extractedServices: string[] = []
    
    if (stepData?.serviceNames && Array.isArray(stepData.serviceNames)) {
      extractedServices = stepData.serviceNames.map((item: unknown) => 
        typeof item === 'string' ? item : 
        (item as Record<string, unknown>)?.serviceName as string || 
        (item as Record<string, unknown>)?.name as string || ''
      ).filter(Boolean)
    } else if (stepData?.roleHostMappings && Array.isArray(stepData.roleHostMappings)) {
      extractedServices = Array.from(new Set(
        stepData.roleHostMappings.map((mapping: unknown) => {
          const mappingObj = mapping as Record<string, unknown>
          return mappingObj.serviceName as string || 
                 (mappingObj.serviceRole as string)?.split('_')[0] || 'Unknown'
        })
      )).filter(s => s !== 'Unknown')
    }
    
    if (extractedServices.length > 0) {
      setServices(extractedServices)
      if (!activeService) {
        setActiveService(extractedServices[0])
      }
      // 初始化配置状态
      const initialStatus: Record<string, 'pending' | 'configured' | 'saved'> = {}
      extractedServices.forEach(service => {
        initialStatus[service] = 'pending'
      })
      setConfigurationStatus(initialStatus)
    } else {
      setError('无法获取服务列表，请检查前面步骤的数据')
    }
  }, [typedStep6Data, activeService])

  // 获取服务配置选项
  const getServiceConfigOption = useCallback(async (serviceName: string) => {
    if (!serviceName || !cluster?.id) return
    
    setLoading(true)
    setError(null)
    
    try {
      const headers = createClusterHeaders(cluster.id)
      const response = await apiV1.get(API_PATHS_V1.GET_SERVICE_CONFIG_OPTION, {
        serviceName
      }, { headers })

      if (response.data?.code === 200 && response.data?.data) {
        const configGroupData = response.data.data as ServiceConfigGroupData
        
        // 存储配置组数据
        setServiceConfigGroups(prev => ({
          ...prev,
          [serviceName]: configGroupData
        }))

        // 提取所有配置项
        const allConfigs = Object.values(configGroupData.groups).flatMap(group => {
          const mainConfigs = group.configs || []
          const subGroupConfigs = group.subGroups ? 
            Object.values(group.subGroups).flatMap(subGroup => subGroup.configs || []) : []
          return [...mainConfigs, ...subGroupConfigs]
        })
        
        // 更新服务模板
        setServiceTemplates(prev => ({
          ...prev,
          [serviceName]: allConfigs
        }))

        // 初始化表单数据
        initializeFormData(serviceName, allConfigs)
        
        // 更新配置状态
        setConfigurationStatus(prev => ({
          ...prev,
          [serviceName]: 'configured'
        }))

      } else {
        throw new Error(response.data?.message || '获取服务配置失败')
      }
    } catch (err: unknown) {
      const error = err as { response?: { data?: { message?: string } }; message?: string }
      const errorMsg = error.response?.data?.message || error.message || '获取服务配置失败'
      setError(errorMsg)
      toast.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }, [cluster?.id])

  // 初始化表单数据
  const initializeFormData = (serviceName: string, configs: ConfigItem[]) => {
    const initialData: FormData = {}
    
    configs.forEach(config => {
      const fieldName = (config.name || '').replace(/\./g, '!')
      initialData[fieldName] = config.value ?? config.defaultValue ?? ''
    })

    setFormData(prev => ({
      ...prev,
      ...initialData
    }))
  }

  // 处理服务切换
  const handleServiceChange = useCallback(async (serviceName: string) => {
    setActiveService(serviceName)
    
    // 如果该服务配置尚未加载，则加载
    if (!serviceTemplates[serviceName]) {
      await getServiceConfigOption(serviceName)
    }
  }, [serviceTemplates, getServiceConfigOption])

  // 保存当前服务配置
  const saveCurrentServiceConfig = async (): Promise<SaveConfigResponse> => {
    if (!activeService || !cluster?.id) {
      throw new Error('缺少必要参数')
    }

    const configs = serviceTemplates[activeService] || []
    const updatedConfigs = configs.map(config => {
      const fieldName = (config.name || '').replace(/\./g, '!')
      return {
        ...config,
        value: formData[fieldName] ?? config.value,
        name: config.name
      }
    })

    // 过滤隐藏的非必填字段
    const filteredConfigs = updatedConfigs.filter(config => 
      !(config.hidden && !config.required)
    )

    const params: SaveConfigParams = {
      clusterId: cluster.id,
      serviceName: activeService,
      serviceConfig: JSON.stringify(filteredConfigs)
    }

    const headers = createClusterHeaders(cluster.id)
    const response = await apiV1.post(API_PATHS_V1.SAVE_SERVICE_CONFIG, params, { headers })
    
    // 更新保存状态
    setConfigurationStatus(prev => ({
      ...prev,
      [activeService]: 'saved'
    }))
    
    return {
      ...response.data,
      name: activeService
    }
  }

  // 保存所有服务配置
  const saveAllConfigurations = async () => {
    setSaving(true)
    const results: SaveConfigResponse[] = []

    try {
      for (const serviceName of services) {
        // 确保该服务的配置已加载
        if (!serviceTemplates[serviceName]) {
          await getServiceConfigOption(serviceName)
        }
        
        // 临时设置为当前服务以便保存
        const originalActive = activeService
        setActiveService(serviceName)
        
        const result = await saveCurrentServiceConfig()
        results.push(result)
        
        // 恢复原来的活动服务
        setActiveService(originalActive)
      }

      const failedServices = results.filter(r => r.code !== 200)
      
      if (failedServices.length > 0) {
        failedServices.forEach(service => {
          toast.error(`${service.name} 配置保存失败: ${service.message || service.msg}`)
        })
        throw new Error('部分服务配置保存失败')
      }

      toast.success('所有服务配置保存成功')
      return results

    } catch (error: unknown) {
      const err = error as { message?: string }
      toast.error(err.message || '保存配置失败')
      throw err
    } finally {
      setSaving(false)
    }
  }

  // 生成安装命令
  const generateInstallCommand = async (): Promise<string> => {
    if (!cluster?.id) {
      throw new Error('缺少集群ID')
    }

    const params: GenerateCommandParams = {
      clusterId: cluster.id,
      serviceNames: services,
      commandType: CommandType.INSTALL_SERVICE
    }

    const headers = createClusterHeaders(cluster.id)
    const response = await apiV1.post(API_PATHS_V1.GENERATE_SERVICE_INSTALL_COMMAND, params, { headers })
    
    if (response.data?.code === 200) {
      return response.data.data?.commandIds || ''
    } else {
      throw new Error(response.data?.message || '生成命令失败')
    }
  }

  // 处理下一步
  const handleNext = async () => {
    try {
      setSaving(true)
      
      // 保存所有配置
      await saveAllConfigurations()
      
      // 生成安装命令
      const commandIds = await generateInstallCommand()
      
      // 构建步骤7数据
      const step7Data: Step7Data = {
        serviceConfigs: serviceTemplates,
        commandIds,
        commandType: CommandType.INSTALL_SERVICE
      }
      
      onComplete(step7Data)
      
    } catch (error: unknown) {
      const err = error as { message?: string }
      toast.error(err.message || '配置保存或命令生成失败')
    } finally {
      setSaving(false)
    }
  }

  // 计算配置进度
  const configurationProgress = () => {
    const total = services.length
    const configured = Object.values(configurationStatus).filter(status => status !== 'pending').length
    return total > 0 ? (configured / total) * 100 : 0
  }

  // 初始加载活动服务配置
  useEffect(() => {
    if (open && activeService && cluster?.id && !serviceTemplates[activeService]) {
      getServiceConfigOption(activeService)
    }
  }, [open, activeService, cluster?.id, serviceTemplates, getServiceConfigOption])

  // 动作栏配置
  const actionBar = (
    <ClusterWizardActionBar
      statusInfo={{
        text: "配置进度",
        value: Math.round(configurationProgress()),
        total: `${services.length} 个服务`,
        pulse: saving
      }}
      statusBadge={{
        text: configurationProgress() === 100 ? "配置完成" : "配置中",
        variant: configurationProgress() === 100 ? "success" : "info",
        show: true
      }}
      buttons={[
        ...(onPrevious ? [{
          text: "上一步",
          onClick: onPrevious,
          disabled: saving,
          variant: "secondary" as const
        }] : []),
        {
          text: "保存配置并继续",
          onClick: handleNext,
          disabled: saving || configurationProgress() < 100,
          loading: saving,
          loadingText: "保存中...",
          variant: "primary" as const,
          icon: Save
        }
      ]}
    />
  )

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogTitle className="sr-only">服务配置 - {cluster?.clusterName}</DialogTitle>
      <ClusterWizardLayout
        open={open}
        onClose={() => onOpenChange(false)}
        clusterName={cluster?.clusterName || ''}
        clusterType={clusterType}
        stepTitle="服务配置"
        stepDescription="服务配置 - 配置各个服务组件的运行参数和资源分配"
        currentStep={currentStepNumber}
        dialogTitle={`服务配置 - ${cluster?.clusterName}`}
        actionBar={actionBar}
      >
        <div className="flex-1 flex flex-col min-h-0 p-6">
          {/* 错误提示 */}
          {error && (
            <Alert variant="destructive" className="mb-6">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

          {/* 使用提示 */}
          <div className="mb-4 bg-gradient-to-r from-blue-50/80 to-indigo-50/60 border border-blue-200/60 rounded-xl p-3">
            <div className="flex items-center gap-2 text-sm text-blue-700">
              <div className="w-2 h-2 bg-blue-500 rounded-full animate-pulse"></div>
              <span className="font-medium">提示：悬停配置项标签可查看详细说明</span>
            </div>
          </div>

          {/* 简洁的进度条 */}
          <div className="mb-4">
            <Progress value={configurationProgress()} className="h-2" />
          </div>

          {/* 主要内容区域 */}
          <div className="flex-1 flex gap-6 min-h-0">
            {/* 左侧服务导航 */}
            <div className="w-64 flex-shrink-0">
              <ServiceConfigNavigation
                services={services}
                activeService={activeService}
                onServiceChange={handleServiceChange}
                loading={loading}
              />
            </div>

            {/* 右侧配置内容 */}
            <div className="flex-1 min-w-0">
              {loading && activeService ? (
                <div className="flex-1 flex items-center justify-center">
                  <div className="text-center">
                    <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-500" />
                    <p className="text-gray-500">加载 {activeService} 配置中...</p>
                  </div>
                </div>
              ) : activeService && serviceConfigGroups[activeService] ? (
                <ServiceConfigContent
                  serviceName={activeService}
                  configGroups={serviceConfigGroups[activeService]}
                  formData={formData}
                  validationErrors={(validationErrors[activeService] as unknown as Record<string, string>) || {}}
                  expandedGroups={expandedGroups}
                  onFormDataChange={setFormData}
                  onExpandedGroupsChange={setExpandedGroups}
                  onSave={async () => { await saveCurrentServiceConfig() }}
                  saving={saving}
                />
              ) : (
                <div className="flex-1 flex items-center justify-center">
                  <div className="text-center">
                    <Package className="h-12 w-12 text-gray-300 mx-auto mb-4" />
                    <p className="text-gray-500">请选择一个服务开始配置</p>
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 配置摘要（可选） */}
          {showSummary && (
            <div className="mt-6 border-t pt-6">
              <ConfigurationSummary
                services={services}
                configurationStatus={configurationStatus}
                serviceTemplates={serviceTemplates}
                formData={formData}
              />
            </div>
          )}
        </div>
      </ClusterWizardLayout>
    </Dialog>
  )
}

export default ServiceConfigDialog
