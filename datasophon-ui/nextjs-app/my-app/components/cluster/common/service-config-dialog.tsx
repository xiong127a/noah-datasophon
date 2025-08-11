"use client"

import React, { useState, useEffect, useCallback } from 'react'
import { 
  Loader2, ChevronUp, ChevronDown, Save, ExpandIcon, ShrinkIcon,
  AlertCircle, Package, Database, Wrench
} from 'lucide-react'
import { Input } from "@/components/ui/input"
import { Button } from "@/components/ui/button"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import { Tabs, TabsContent, TabsList, TabsTrigger } from "@/components/ui/tabs"
import { Dialog, DialogTitle } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Checkbox } from '@/components/ui/checkbox'
import { toast } from 'sonner'
import { apiV1, API_PATHS_V1 } from "@/lib/api-config-v1"
import { createClusterHeaders } from '@/lib/cluster-id-header'
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'

import type { 
  ServiceConfigDialogProps,
  ConfigItem,
  ConfigGroup,
  GroupedTemplateData,
  ServiceTemplate,
  FormData,
  ValidationErrors,
  ExpandedKeys,
  KubernetesTabState,
  Step7Data,
  SaveConfigParams,
  SaveConfigResponse,
  GenerateCommandParams
} from '@/types/service-config'
import { 
  ConfigType,
  CommandType,
  KUBERNETES_SUBGROUP_CHINESE_NAMES
} from '@/types/service-config'
import type { Step6Data } from '@/types/worker-role-assign'

/**
 * 集群步骤7：服务配置对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
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
  // 类型断言确保step6Data是正确的类型
  const typedStep6Data = step6Data as Step6Data
  // 基础状态
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [saveLoading, setSaveLoading] = useState(false)

  // 服务和配置数据
  const [serviceNames, setServiceNames] = useState<string[]>([])
  const [activeService, setActiveService] = useState<string>('')
  const [serviceTemplate, setServiceTemplate] = useState<ServiceTemplate>({})
  const [groupedTemplateData, setGroupedTemplateData] = useState<GroupedTemplateData>({})

  // UI状态
  const [expandedKeys, setExpandedKeys] = useState<ExpandedKeys>({})
  const [isAllExpanded, setIsAllExpanded] = useState(true)
  const [kubernetesTabState, setKubernetesTabState] = useState<KubernetesTabState>({})

  // 表单数据和验证
  const [formData, setFormData] = useState<FormData>({})
  const [validationErrors, setValidationErrors] = useState<ValidationErrors>({})

  // 计算当前步骤编号
  const isK8s = clusterType?.toLowerCase() === 'kubernetes'
  const currentStepNumber = isK8s ? 6 : 7

  // 从step6Data中提取服务列表
  useEffect(() => {
    // 优先从serviceNames字段获取服务列表
    const stepData = typedStep6Data as any
    let services: string[] = []
    
    if (stepData?.serviceNames && Array.isArray(stepData.serviceNames)) {
      // 如果有serviceNames字段，优先使用
      services = stepData.serviceNames.map((item: any) => 
        typeof item === 'string' ? item : item.serviceName || item.name
      )
    } else if (stepData?.roleHostMappings) {
      // 否则尝试从roleHostMappings中提取
      services = Array.from(new Set(
        stepData.roleHostMappings.map((mapping: any) => {
          return mapping.serviceName || mapping.serviceRole?.split('_')[0] || 'Unknown'
        })
      )).filter(s => s !== 'Unknown') as string[]
    }
    
    console.log('服务配置 - 提取到的服务列表:', services)
    
    if (services.length > 0) {
      setServiceNames(services)
      if (!activeService) {
        setActiveService(services[0])
      }
    } else {
      console.warn('服务配置 - 未能提取到服务列表', stepData)
      setError('无法获取服务列表，请检查前面步骤的数据')
    }
  }, [typedStep6Data, activeService])

  // 分组配置数据
  const groupConfigsByService = useCallback((serviceName: string, configs: ConfigItem[]): Record<string, ConfigGroup> => {
    const groups: Record<string, ConfigGroup> = {}
    
    // 按配置组分类
    configs.forEach(config => {
      const groupName = extractGroupName(config.name || config.label || 'General')
      
      if (!groups[groupName]) {
        groups[groupName] = {
          items: [],
          displayName: convertGroupName(groupName),
          hasKubernetesConfig: false,
          kubernetesSubGroups: {}
        }
      }
      
      // 处理配置项名称，替换特殊字符
      const processedConfig = {
        ...config,
        name: (config.name || '').replace(/\./g, '!')
      }
      
      // 判断是否为Kubernetes配置
      if (config.name?.startsWith('kubernetes.config.')) {
        handleKubernetesConfig(groups, groupName, processedConfig)
      } else {
        groups[groupName].items.push(processedConfig)
      }
    })

    // 排序分组
    return sortConfigGroups(groups)
  }, [])

  // 获取服务配置选项
  const getServiceConfigOption = useCallback(async () => {
    if (!activeService || !cluster?.id) return

    setLoading(true)
    setError(null)

    try {
      const headers = createClusterHeaders(cluster.id)
      const response = await apiV1.post(API_PATHS_V1.GET_SERVICE_CONFIG_OPTION, {
        clusterId: cluster.id,
        serviceName: activeService
      }, { headers })

      if (response.data?.code === 200 && response.data?.data) {
        const configs = response.data.data as ConfigItem[]
        
        // 更新服务模板
        setServiceTemplate(prev => ({
          ...prev,
          [activeService]: configs
        }))

        // 处理和分组配置数据
        const groupedData = groupConfigsByService(activeService, configs)
        setGroupedTemplateData(prev => ({
          ...prev,
          [activeService]: groupedData
        }))

        // 初始化表单数据
        initializeFormData(activeService, configs)

        // 设置默认展开状态
        const groupKeys = Object.keys(groupedData)
        setExpandedKeys(prev => ({
          ...prev,
          [activeService]: groupKeys
        }))

      } else {
        throw new Error(response.data?.message || '获取服务配置失败')
      }
    } catch (err: any) {
      const errorMsg = err.response?.data?.message || err.message || '获取服务配置失败'
      setError(errorMsg)
      toast.error(errorMsg)
    } finally {
      setLoading(false)
    }
  }, [activeService, cluster?.id, groupConfigsByService])

  // 处理Kubernetes配置
  const handleKubernetesConfig = (groups: Record<string, ConfigGroup>, targetGroup: string, config: ConfigItem) => {
    if (!groups[targetGroup].hasKubernetesConfig) {
      groups[targetGroup].hasKubernetesConfig = true
      groups[targetGroup].kubernetesSubGroups = {}
    }

    const parts = (config.name || '').split('.')
    const subGroupName = parts.slice(2, -1).join('.')
    const displayName = KUBERNETES_SUBGROUP_CHINESE_NAMES[subGroupName] || subGroupName

    if (!groups[targetGroup].kubernetesSubGroups![subGroupName]) {
      groups[targetGroup].kubernetesSubGroups![subGroupName] = {
        items: [],
        displayName
      }
    }

    groups[targetGroup].kubernetesSubGroups![subGroupName].items.push(config)
  }

  // 提取组名
  const extractGroupName = (name: string): string => {
    if (name.includes('.')) {
      const parts = name.split('.')
      return parts[0] || 'General'
    }
    return 'General'
  }

  // 转换组名显示
  const convertGroupName = (groupName: string): string => {
    if (groupName.startsWith('advanced_')) {
      return '高级 ' + groupName.substring('advanced_'.length)
    } else if (groupName.startsWith('custom_')) {
      return '自定义 ' + groupName.substring('custom_'.length)
    }
    return groupName
  }

  // 排序配置组
  const sortConfigGroups = (groups: Record<string, ConfigGroup>): Record<string, ConfigGroup> => {
    const sortedKeys = Object.keys(groups).sort((a, b) => {
      // 通用配置排在前面
      if (a === 'General') return -1
      if (b === 'General') return 1
      
      // 角色配置按字母排序
      return a.localeCompare(b)
    })

    const sortedGroups: Record<string, ConfigGroup> = {}
    sortedKeys.forEach(key => {
      sortedGroups[key] = groups[key]
    })

    return sortedGroups
  }

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

  // 处理表单字段变化
  const handleFieldChange = (fieldName: string, value: any) => {
    setFormData(prev => ({
      ...prev,
      [fieldName]: value
    }))

    // 清除相关验证错误
    if (validationErrors[activeService]?.[fieldName]) {
      setValidationErrors(prev => {
        const newErrors = { ...prev }
        if (newErrors[activeService]?.[fieldName]) {
          delete newErrors[activeService][fieldName]
        }
        return newErrors
      })
    }
  }

  // 切换展开/折叠所有组
  const toggleAllGroups = () => {
    const groups = groupedTemplateData[activeService] || {}
    const groupKeys = Object.keys(groups)
    
    setExpandedKeys(prev => ({
      ...prev,
      [activeService]: isAllExpanded ? [] : groupKeys
    }))
    setIsAllExpanded(!isAllExpanded)
  }

  // 处理单个组的展开/折叠
  const handleGroupToggle = (groupName: string) => {
    setExpandedKeys(prev => {
      const currentKeys = prev[activeService] || []
      const isExpanded = currentKeys.includes(groupName)
      
      const newKeys = isExpanded
        ? currentKeys.filter(key => key !== groupName)
        : [...currentKeys, groupName]
      
      return {
        ...prev,
        [activeService]: newKeys
      }
    })
  }

  // 保存当前服务配置
  const saveCurrentServiceConfig = async (): Promise<SaveConfigResponse> => {
    if (!activeService || !cluster?.id) {
      throw new Error('缺少必要参数')
    }

    const configs = serviceTemplate[activeService] || []
    const updatedConfigs = configs.map(config => {
      const fieldName = (config.name || '').replace(/\./g, '!')
      return {
        ...config,
        value: formData[fieldName] ?? config.value,
        name: config.name // 恢复原始名称
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
    
    return {
      ...response.data,
      name: activeService
    }
  }

  // 保存所有服务配置
  const saveAllConfigurations = async () => {
    setSaveLoading(true)
    const results: SaveConfigResponse[] = []

    try {
      for (const serviceName of serviceNames) {
        setActiveService(serviceName)
        // 确保该服务的配置已加载
        if (!serviceTemplate[serviceName]) {
          await getServiceConfigOption()
        }
        
        const result = await saveCurrentServiceConfig()
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

    } catch (error: any) {
      toast.error(error.message || '保存配置失败')
      throw error
    } finally {
      setSaveLoading(false)
    }
  }

  // 生成安装命令
  const generateInstallCommand = async (): Promise<string> => {
    if (!cluster?.id) {
      throw new Error('缺少集群ID')
    }

    const params: GenerateCommandParams = {
      clusterId: cluster.id,
      serviceNames: serviceNames,
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
      setSaveLoading(true)
      
      // 保存所有配置
      await saveAllConfigurations()
      
      // 生成安装命令
      const commandIds = await generateInstallCommand()
      
      // 构建步骤7数据
      const step7Data: Step7Data = {
        serviceConfigs: serviceTemplate,
        commandIds,
        commandType: CommandType.INSTALL_SERVICE
      }
      
      onComplete(step7Data)
      
    } catch (error: any) {
      console.error('步骤7完成失败:', error)
      toast.error(error.message || '配置保存或命令生成失败')
    } finally {
      setSaveLoading(false)
    }
  }

  // 渲染配置项
  const renderConfigItem = (config: ConfigItem) => {
    const fieldName = (config.name || '').replace(/\./g, '!')
    const value = formData[fieldName] ?? config.value ?? config.defaultValue ?? ''
    const hasError = validationErrors[activeService]?.[config.name!]

    const commonProps = {
      value,
      onChange: (newValue: any) => handleFieldChange(fieldName, newValue),
      disabled: loading,
      className: hasError ? 'border-red-500' : ''
    }

    switch (config.type) {
      case ConfigType.SELECT:
        return (
          <Select value={value} onValueChange={commonProps.onChange}>
            <SelectTrigger className={commonProps.className}>
              <SelectValue placeholder={config.placeholder || `请选择${config.label}`} />
            </SelectTrigger>
            <SelectContent>
              {config.options?.map(option => (
                <SelectItem key={option.value} value={option.value}>
                  {option.label}
                </SelectItem>
              ))}
            </SelectContent>
          </Select>
        )

      case ConfigType.SWITCH:
      case ConfigType.BOOLEAN:
        return (
          <div className="flex items-center space-x-2">
            <Checkbox
              checked={value === true || value === 'true'}
              onCheckedChange={(checked) => commonProps.onChange(checked)}
              disabled={commonProps.disabled}
            />
            <span className="text-sm text-gray-600">启用</span>
          </div>
        )

      case ConfigType.TEXTAREA:
        return (
          <Textarea
            {...commonProps}
            placeholder={config.placeholder || `请输入${config.label}`}
            rows={4}
          />
        )

      case ConfigType.NUMBER:
        return (
          <div className="flex items-center space-x-2">
            <Input
              type="number"
              {...commonProps}
              min={config.minValue}
              max={config.maxValue}
              placeholder={config.placeholder || `请输入${config.label}`}
            />
            {config.unit && (
              <span className="text-sm text-gray-500">{config.unit}</span>
            )}
          </div>
        )

      case ConfigType.PASSWORD:
        return (
          <Input
            type="password"
            {...commonProps}
            placeholder={config.placeholder || `请输入${config.label}`}
          />
        )

      default:
        return (
          <Input
            {...commonProps}
            placeholder={config.placeholder || `请输入${config.label}`}
          />
        )
    }
  }

  // 渲染配置组
  const renderConfigGroup = (groupName: string, group: ConfigGroup) => {
    const isExpanded = expandedKeys[activeService]?.includes(groupName) ?? true

    return (
      <Card key={groupName} className="border border-gray-200 shadow-sm">
        <CardHeader 
          className="cursor-pointer hover:bg-gray-50 transition-colors"
          onClick={() => handleGroupToggle(groupName)}
        >
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-medium flex items-center gap-2">
              <Package className="h-4 w-4 text-gray-500" />
              {group.displayName}
            </CardTitle>
            {isExpanded ? (
              <ChevronUp className="h-4 w-4 text-gray-400" />
            ) : (
              <ChevronDown className="h-4 w-4 text-gray-400" />
            )}
          </div>
        </CardHeader>
        
        {isExpanded && (
          <CardContent className="pt-0">
            {/* 普通配置项 */}
            {group.items.length > 0 && (
              <div className="space-y-4">
                {group.items.map(config => (
                  <div key={config.name} className="space-y-2">
                    <label className="block text-sm font-medium text-gray-700">
                      {config.label || config.name}
                      {config.required && <span className="text-red-500 ml-1">*</span>}
                    </label>
                    {config.description && (
                      <p className="text-xs text-gray-500">{config.description}</p>
                    )}
                    {renderConfigItem(config)}
                  </div>
                ))}
              </div>
            )}

            {/* Kubernetes配置 */}
            {group.hasKubernetesConfig && group.kubernetesSubGroups && (
              <div className="mt-6">
                <Separator className="mb-4" />
                <h4 className="text-sm font-medium text-gray-700 mb-3 flex items-center gap-2">
                  <Database className="h-4 w-4" />
                  Kubernetes 配置
                </h4>
                
                <Tabs 
                  value={kubernetesTabState[`${activeService}_${groupName}`] || Object.keys(group.kubernetesSubGroups)[0]}
                  onValueChange={(value) => setKubernetesTabState(prev => ({
                    ...prev,
                    [`${activeService}_${groupName}`]: value
                  }))}
                  className="w-full"
                >
                  <TabsList className="grid w-full grid-cols-2 lg:grid-cols-3">
                    {Object.entries(group.kubernetesSubGroups).map(([subGroupName, subGroup]) => (
                      <TabsTrigger key={subGroupName} value={subGroupName} className="text-xs">
                        {subGroup.displayName}
                      </TabsTrigger>
                    ))}
                  </TabsList>
                  
                  {Object.entries(group.kubernetesSubGroups).map(([subGroupName, subGroup]) => (
                    <TabsContent key={subGroupName} value={subGroupName} className="mt-4">
                      <div className="space-y-4">
                        {subGroup.items.map(config => (
                          <div key={config.name} className="space-y-2">
                            <label className="block text-sm font-medium text-gray-700">
                              {config.label || config.name}
                              {config.required && <span className="text-red-500 ml-1">*</span>}
                            </label>
                            {config.description && (
                              <p className="text-xs text-gray-500">{config.description}</p>
                            )}
                            {renderConfigItem(config)}
                          </div>
                        ))}
                      </div>
                    </TabsContent>
                  ))}
                </Tabs>
              </div>
            )}
          </CardContent>
        )}
      </Card>
    )
  }

  // 初始获取配置
  useEffect(() => {
    if (open && activeService && cluster?.id) {
      getServiceConfigOption()
    }
  }, [open, activeService, cluster?.id, getServiceConfigOption])

  const actionBar = (
    <ClusterWizardActionBar
      buttons={[
        ...(onPrevious ? [{
          text: "上一步",
          onClick: onPrevious,
          disabled: saveLoading,
          icon: ChevronUp
        }] : []),
        {
          text: "保存配置并继续",
          onClick: handleNext,
          disabled: saveLoading,
          loading: saveLoading,
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
        stepDescription="配置各个服务组件的运行参数"
        currentStep={currentStepNumber}
        dialogTitle={`服务配置 - ${cluster?.clusterName}`}
        actionBar={actionBar}
      >
        <div className="flex-1 flex flex-col min-h-0">
          {error && (
            <div className="bg-red-50 border border-red-200 rounded-lg p-4 mb-6 flex items-center gap-2">
              <AlertCircle className="h-5 w-5 text-red-500 flex-shrink-0" />
              <span className="text-red-700">{error}</span>
            </div>
          )}

          {/* 操作栏 */}
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center gap-4">
              <Badge variant="outline" className="text-blue-600 border-blue-200">
                共 {serviceNames.length} 个服务
              </Badge>
              {loading && (
                <div className="flex items-center gap-2 text-sm text-gray-500">
                  <Loader2 className="h-4 w-4 animate-spin" />
                  加载配置中...
                </div>
              )}
            </div>
            
            <div className="flex items-center gap-2">
              <Button
                variant="outline"
                size="sm"
                onClick={toggleAllGroups}
                disabled={loading}
              >
                {isAllExpanded ? (
                  <>
                    <ShrinkIcon className="h-4 w-4 mr-1" />
                    折叠全部
                  </>
                ) : (
                  <>
                    <ExpandIcon className="h-4 w-4 mr-1" />
                    展开全部
                  </>
                )}
              </Button>
              
              <Button
                variant="default"
                size="sm"
                onClick={saveCurrentServiceConfig}
                disabled={loading || saveLoading}
              >
                {saveLoading ? (
                  <Loader2 className="h-4 w-4 mr-1 animate-spin" />
                ) : (
                  <Save className="h-4 w-4 mr-1" />
                )}
                保存当前服务
              </Button>
            </div>
          </div>

          {/* 服务标签页 */}
          <Tabs value={activeService} onValueChange={setActiveService} className="flex-1 flex flex-col">
            <TabsList className="grid grid-cols-2 lg:grid-cols-4 gap-1 mb-6">
              {serviceNames.map(serviceName => (
                <TabsTrigger 
                  key={serviceName} 
                  value={serviceName}
                  className="flex items-center gap-2"
                >
                  <Wrench className="h-4 w-4" />
                  {serviceName}
                </TabsTrigger>
              ))}
            </TabsList>

            {serviceNames.map(serviceName => (
              <TabsContent 
                key={serviceName} 
                value={serviceName} 
                className="flex-1 overflow-y-auto data-[state=active]:flex data-[state=active]:flex-col"
              >
                {loading && serviceName === activeService ? (
                  <div className="flex-1 flex items-center justify-center">
                    <div className="text-center">
                      <Loader2 className="h-8 w-8 animate-spin mx-auto mb-4 text-blue-500" />
                      <p className="text-gray-500">加载 {serviceName} 配置中...</p>
                    </div>
                  </div>
                ) : (
                  <div className="space-y-4">
                    {Object.entries(groupedTemplateData[serviceName] || {}).map(([groupName, group]) =>
                      renderConfigGroup(groupName, group)
                    )}
                  </div>
                )}
              </TabsContent>
            ))}
          </Tabs>
        </div>
      </ClusterWizardLayout>
    </Dialog>
  )
}

export default ServiceConfigDialog
