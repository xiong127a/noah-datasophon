"use client"

import React, { useState, useEffect, useCallback } from 'react'
import { 
  Loader2, AlertTriangle, Package, ChevronRight
} from 'lucide-react'
import { toast } from 'sonner'
import { Dialog, DialogTitle } from '@/components/ui/dialog'
import { Alert, AlertDescription } from '@/components/ui/alert'

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
  SaveConfigResponse,
  ServiceConfigGroupData
} from '@/types/service-config'
import { CommandType } from '@/types/service-config'
import type { Step6Data } from '@/types/worker-role-assign'

// 子组件导入
import ServiceConfigNavigation from './service-config/service-config-navigation'
import ServiceConfigContent from './service-config/service-config-content'

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
  
  // UI状态
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
    
    // 每次切换都重新获取最新配置数据
    await getServiceConfigOption(serviceName)
  }, [getServiceConfigOption])

  // 保存指定服务配置
  const saveServiceConfig = async (serviceName: string): Promise<SaveConfigResponse> => {
    if (!serviceName || !cluster?.id) {
      throw new Error('缺少必要参数')
    }

    const configs = serviceTemplates[serviceName] || []
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

    // 根据新项目的后端接口适配：
    // @ClusterId Integer clusterId - 从请求头获取，无需在参数中传递
    // @RequestParam("serviceName") - 需要作为请求参数
    // @RequestParam("serviceConfig") - 需要作为请求参数
    const requestData = new URLSearchParams()
    requestData.append('serviceName', serviceName)
    requestData.append('serviceConfig', JSON.stringify(filteredConfigs))

    const headers = createClusterHeaders(cluster.id)
    const response = await apiV1.post(API_PATHS_V1.SAVE_SERVICE_CONFIG, requestData, { headers })
    
    return {
      ...response.data,
      name: serviceName
    }
  }

  // 保存当前活动服务配置（为了保持向后兼容）
  const saveCurrentServiceConfig = async (): Promise<SaveConfigResponse> => {
    return await saveServiceConfig(activeService)
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
        
        // 直接保存指定服务，不依赖activeService状态更新
        const result = await saveServiceConfig(serviceName)
        results.push(result)
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

  // 保存服务角色主机映射（修复缺失步骤）
  const saveServiceRoleHostMapping = async (): Promise<void> => {
    if (!cluster?.id) {
      throw new Error('缺少集群ID')
    }

    // 从 step6Data 中提取角色主机映射数据
    const stepData = typedStep6Data as unknown as Record<string, unknown>
    const roleHostMappings = stepData?.roleHostMappings as Array<{
      serviceName?: string
      serviceRole: string
      hostname: string
      [key: string]: unknown
    }> || []

    if (!roleHostMappings || roleHostMappings.length === 0) {
      console.warn('未找到角色主机映射数据，可能影响服务安装')
      return
    }

    // 转换数据格式：将角色主机映射转换为API期望的格式
    const mappingMap = new Map<string, string[]>()
    
    roleHostMappings.forEach(mapping => {
      const serviceRole = mapping.serviceRole
      const hostname = mapping.hostname
      
      if (serviceRole && hostname) {
        if (!mappingMap.has(serviceRole)) {
          mappingMap.set(serviceRole, [])
        }
        const hosts = mappingMap.get(serviceRole)!
        if (!hosts.includes(hostname)) {
          hosts.push(hostname)
        }
      }
    })

    // 构建API请求数据
    const requestData = Array.from(mappingMap.entries()).map(([serviceRole, hosts]) => ({
      serviceRole,
      hosts
    }))

    if (requestData.length === 0) {
      console.warn('转换后的映射数据为空，可能影响服务安装')
      return
    }

    try {
      const headers = createClusterHeaders(cluster.id)
      const response = await apiV1.post(
        API_PATHS_V1.SAVE_SERVICE_ROLE_HOST_MAPPING_V2, 
        requestData, 
        { headers }
      )
      
      if (response.data?.code === 200) {
        console.log('服务角色主机映射保存成功')
      } else {
        throw new Error(response.data?.message || '保存服务角色主机映射失败')
      }
    } catch (error: unknown) {
      const err = error as { response?: { data?: { message?: string } }; message?: string }
      const errorMsg = err.response?.data?.message || err.message || '保存服务角色主机映射失败'
      console.error('保存服务角色主机映射失败:', errorMsg)
      throw new Error(errorMsg)
    }
  }

  // 生成安装命令（暂时保留，未来可能使用）
  // eslint-disable-next-line @typescript-eslint/no-unused-vars
  const generateInstallCommand = async (): Promise<string> => {
    if (!cluster?.id) {
      throw new Error('缺少集群ID')
    }

    // 检查services是否为空
    if (!services || services.length === 0) {
      throw new Error('没有可安装的服务，请检查服务选择步骤')
    }

    // 根据新项目规范：
    // @ClusterId Integer clusterId - 从请求头获取
    // @RequestParam CommandType commandType - 查询参数
    // @RequestBody List<String> serviceNames - 请求体（JSON数组）
    const url = `${API_PATHS_V1.GENERATE_SERVICE_INSTALL_COMMAND}?commandType=${CommandType.INSTALL_SERVICE}`
    const requestBody = services  // 服务名称列表作为JSON数组
    const headers = createClusterHeaders(cluster.id)

    const response = await apiV1.post(url, requestBody, { headers })
    
    if (response.data?.code === 200) {
      return response.data.data || ''
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
      
      // 保存服务角色主机映射（修复缺失步骤）
      await saveServiceRoleHostMapping()
      
      // 🔧 修复：不在这里生成安装命令，移到service-install-dialog中
      // 传递所有选择的服务，而不是只传递已配置的服务
      console.log('🔍 所有选择的服务:', services)
      console.log('🔍 已配置的服务:', Object.keys(serviceTemplates))
      
      const allServicesConfig: Record<string, any[]> = {}
      services.forEach(serviceName => {
        // 传递所有选择的服务，即使某些服务的配置加载失败也要包含
        allServicesConfig[serviceName] = serviceTemplates[serviceName] || []
      })
      
      console.log('🔍 最终传递的服务:', Object.keys(allServicesConfig))
      
      const step7Data: Step7Data = {
        serviceConfigs: allServicesConfig, // 包含所有选择的服务
        commandIds: undefined, // 不再在这里生成commandIds
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

  // 初始加载活动服务配置
  useEffect(() => {
    if (open && activeService && cluster?.id) {
      getServiceConfigOption(activeService)
    }
  }, [open, activeService, cluster?.id, getServiceConfigOption])

  // 动作栏配置
  const actionBar = (
    <ClusterWizardActionBar
      buttons={[
        ...(onPrevious ? [{
          text: "上一步",
          onClick: onPrevious,
          disabled: saving,
          variant: "secondary" as const
        }] : []),
        {
          text: "下一步",
          onClick: handleNext,
          disabled: saving,
          loading: saving,
          loadingText: "处理中...",
          variant: "primary" as const,
          icon: ChevronRight
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
        <div className="flex-1 flex flex-col min-h-0 p-6 bg-gradient-to-br from-gray-50/30 via-white/50 to-blue-50/20">
          {/* 错误提示 */}
          {error && (
            <Alert variant="destructive" className="mb-6">
              <AlertTriangle className="h-4 w-4" />
              <AlertDescription>{error}</AlertDescription>
            </Alert>
          )}

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
        </div>
      </ClusterWizardLayout>
    </Dialog>
  )
}

export default ServiceConfigDialog
