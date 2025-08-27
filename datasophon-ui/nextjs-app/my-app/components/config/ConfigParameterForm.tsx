"use client"

import React, { useState, useEffect, useCallback } from 'react'
import { 
  Settings, ChevronDown, ChevronUp, FileText, Save, Loader2
} from 'lucide-react'

import { Button } from '@/components/ui/button'

import { Badge } from '@/components/ui/badge'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Separator } from '@/components/ui/separator'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { toast } from 'sonner'

import { ConfigItemRenderer } from './ConfigItemRenderer'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { apiV1, API_PATHS_V1 } from '@/lib/api-config-v1'

interface ConfigItem {
  name: string
  value: unknown
  type: string
  label?: string
  description?: string
  required?: boolean
  hidden?: boolean
  defaultValue?: unknown
  options?: Array<{ label: string; value: unknown }>
  selectValue?: string[]
  templateContent?: string
  minValue?: number
  maxValue?: number
  unit?: string
  placeholder?: string
  heightMultiple?: number
  configType?: string
  configGroup?: string
}

interface ConfigGroup {
  items?: ConfigItem[]
  displayName?: string
  templateContent?: string | null
  hasKubernetesConfig?: boolean
  kubernetesSubGroups?: Record<string, ConfigItem[]>
}

interface ConfigParameterFormProps {
  serviceId: string
  serviceName: string
  currentVersion?: number
  currentRoleGroup?: number
  compareMode?: boolean
  compareVersion?: number
  searchKeyword?: string
  onSave?: (configItems: ConfigItem[]) => void
  className?: string
}

export default function ConfigParameterForm({
  serviceId,
  currentVersion,
  currentRoleGroup,
  searchKeyword = '',
  onSave,
  className = ''
}: ConfigParameterFormProps) {
  // 状态管理
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)
  const [configData, setConfigData] = useState<Record<string, ConfigGroup>>({})
  const [formData, setFormData] = useState<Record<string, unknown>>({})
  const [expandedGroups, setExpandedGroups] = useState<Set<string>>(new Set())
  const [validationErrors, setValidationErrors] = useState<Record<string, string>>({})

  
  // 集群ID
  const clusterId = typeof window !== 'undefined' 
    ? new URLSearchParams(window.location.search).get('clusterId') || localStorage.getItem('clusterId') || '1'
    : '1'



  // 处理配置数据结构
  const processConfigData = useCallback((rawData: unknown): Record<string, ConfigGroup> => {
    console.log('🔄 processConfigData 开始处理原始数据:', rawData)
    
    if (!rawData || typeof rawData !== 'object') {
      console.log('❌ 原始数据为空或格式不正确')
      return {}
    }

    const result: Record<string, ConfigGroup> = {}
    const kubernetesConfigsByRole: Record<string, Record<string, ConfigItem[]>> = {}
    const regularConfigsByRole: Record<string, ConfigItem[]> = {}

    // 第一步：分类配置
    Object.entries(rawData as Record<string, unknown>).forEach(([originalKey, configList]) => {
      const cleanedKey = originalKey?.trim().replace(/^"|"$/g, '') || 'UnknownGroup'
      console.log(`📂 处理配置组: ${cleanedKey}`, configList)
      
      if (!Array.isArray(configList)) {
        console.warn(`⚠️ 配置列表不是数组格式: ${cleanedKey}`, configList)
        return
      }

      const processedConfigs = configList.map((item: unknown) => ({
        ...(item as ConfigItem),
        name: ((item as ConfigItem).name || '').replaceAll('.', '!') // 转换名称格式
      }))
      
      console.log(`✅ 处理后的配置项 [${cleanedKey}]:`, processedConfigs)

      // 分离 Kubernetes 配置和常规配置
      const kubernetesConfigs: ConfigItem[] = []
      const normalConfigs: ConfigItem[] = []
      
      processedConfigs.forEach(config => {
        if (config.configType === 'kubernetes' && config.configGroup?.includes('kubernetes.config.')) {
          kubernetesConfigs.push(config)
        } else {
          normalConfigs.push(config)
        }
      })

      // 处理 Kubernetes 配置
      if (kubernetesConfigs.length > 0) {
        const kubernetesConfigsBySubGroup: Record<string, ConfigItem[]> = {}
        
        kubernetesConfigs.forEach(config => {
          const subGroupName = config.configGroup || 'kubernetes.config.default'
          if (!kubernetesConfigsBySubGroup[subGroupName]) {
            kubernetesConfigsBySubGroup[subGroupName] = []
          }
          kubernetesConfigsBySubGroup[subGroupName].push(config)
        })
        
        if (!kubernetesConfigsByRole[cleanedKey]) {
          kubernetesConfigsByRole[cleanedKey] = {}
        }
        Object.assign(kubernetesConfigsByRole[cleanedKey], kubernetesConfigsBySubGroup)
      }

      // 处理常规配置
      if (normalConfigs.length > 0) {
        regularConfigsByRole[cleanedKey] = normalConfigs
      }
    })

    // 第二步：组装最终结果，保持原始顺序
    const originalKeys = Object.keys(rawData as Record<string, unknown>)
    
    originalKeys.forEach(originalKey => {
      const roleName = originalKey?.trim().replace(/^"|"$/g, '') || 'UnknownGroup'
      const kubernetesSubGroups = kubernetesConfigsByRole[roleName]
      const regularConfigs = regularConfigsByRole[roleName]

      if (kubernetesSubGroups && Object.keys(kubernetesSubGroups).length > 0) {
        // 包含Kubernetes配置的角色组
        result[roleName] = {
          hasKubernetesConfig: true,
          items: regularConfigs || [],
          kubernetesSubGroups: kubernetesSubGroups,
          displayName: roleName
        }
      } else if (regularConfigs && regularConfigs.length > 0) {
        // 纯常规配置
        const configWithTemplate = regularConfigs.find(item => item.templateContent && item.templateContent.trim() !== '')
        if (configWithTemplate) {
          result[roleName] = {
            items: regularConfigs,
            displayName: roleName,
            templateContent: configWithTemplate.templateContent
          }
        } else {
          result[roleName] = {
            items: regularConfigs,
            displayName: roleName
          }
        }
      }
    })

    console.log('🎯 processConfigData 最终结果:', result)
    console.log('📊 结果统计:', {
      总分组数: Object.keys(result).length,
      分组详情: Object.entries(result).map(([key, group]) => ({
        分组名: key,
        常规配置数: group.items?.length || 0,
        K8s子组数: group.kubernetesSubGroups ? Object.keys(group.kubernetesSubGroups).length : 0
      }))
    })
    
    return result
  }, [])

  // 初始化表单数据
  const initializeFormData = useCallback((configData: Record<string, ConfigGroup>) => {
    const initialFormData: Record<string, unknown> = {}

    Object.entries(configData).forEach(([, group]) => {
      // 处理常规配置项
      if (group.items) {
        group.items.forEach(item => {
          if (!item.hidden) {
            initialFormData[item.name] = getInitialValue(item)
          }
        })
      }

      // 处理Kubernetes配置项
      if (group.kubernetesSubGroups) {
        Object.entries(group.kubernetesSubGroups).forEach(([, configs]) => {
          configs.forEach(item => {
            if (!item.hidden) {
              initialFormData[item.name] = getInitialValue(item)
            }
          })
        })
      }
    })

    setFormData(initialFormData)
  }, [])

  // 获取配置项初始值
  const getInitialValue = (item: ConfigItem) => {
    if (['multipleWithKey', 'multiple'].includes(item.type)) {
      if (!item.value || (Array.isArray(item.value) && item.value.length === 0)) {
        if (item.type === 'multipleWithKey') {
          return [{ key: '', value: '' }]
        } else {
          return ['']
        }
      }
      
      if (item.type === 'multipleWithKey' && Array.isArray(item.value)) {
        // 转换对象格式 [{key: value}] 到 KeyValuePair[] 格式
        return item.value.map((obj: unknown) => {
          if (typeof obj === 'object' && obj !== null) {
            const entries = Object.entries(obj as Record<string, unknown>)
            if (entries.length > 0) {
              const [key, value] = entries[0]
              return { key: String(key), value: String(value) }
            }
          }
          return { key: '', value: '' }
        }).filter(pair => pair.key !== '' || pair.value !== '')
      }
      
      return item.value
    }
    
    return item.value !== null && item.value !== undefined && item.value !== ''
      ? item.value
      : item.defaultValue || ''
  }

  // 初始化展开状态
  const initializeExpandedState = useCallback((configData: Record<string, ConfigGroup>) => {
    const expanded = new Set<string>()
    
    // 默认展开第一个分组
    const groupNames = Object.keys(configData)
    if (groupNames.length > 0) {
      expanded.add(groupNames[0])
    }
    
    setExpandedGroups(expanded)
  }, [])

  // 获取配置数据
  const fetchConfigData = useCallback(async () => {
    console.log('🔍 ConfigParameterForm fetchConfigData called with:', {
      serviceId,
      clusterId,
      currentRoleGroup,
      currentVersion
    })
    
    if (!serviceId || !clusterId || !currentRoleGroup || !currentVersion) {
      console.log('❌ fetchConfigData 参数不完整，跳过API调用')
      return
    }
    
    setLoading(true)
    try {
      const headers = createClusterHeaders(clusterId)
      const params = {
        serviceInstanceId: serviceId,
        version: currentVersion || '',
        roleGroupId: currentRoleGroup || ''
      }
      console.log('📡 调用API (GET):', API_PATHS_V1.GET_SERVICE_CONFIG)
      console.log('📦 查询参数:', params)
      
      const response = await apiV1.get(API_PATHS_V1.GET_SERVICE_CONFIG, params, { headers })
      
      console.log('📥 API响应:', response.data)
      
      if (response.data.code === 200) {
        console.log('✅ API调用成功，外层数据:', response.data.data)
        // 检查数据结构，确保正确提取配置数据
        const rawConfigData = response.data.data?.data || response.data.data
        console.log('🔍 提取的配置数据:', rawConfigData)
        const processedData = processConfigData(rawConfigData)
        console.log('🔄 处理后的数据:', processedData)
        setConfigData(processedData)
        initializeFormData(processedData)
        initializeExpandedState(processedData)
      } else {
        console.log('❌ API返回错误:', response.data.msg)
        toast.error(response.data.msg || '获取配置数据失败')
      }
    } catch (error) {
      console.error('🚨 获取配置数据失败:', error)
      toast.error('获取配置数据失败')
    } finally {
      setLoading(false)
    }
  }, [serviceId, clusterId, currentRoleGroup, currentVersion, processConfigData, initializeFormData, initializeExpandedState])

  // 处理表单字段变化
  const handleFieldChange = useCallback((fieldName: string, value: unknown) => {
    setFormData(prev => ({
      ...prev,
      [fieldName]: value
    }))

    // 清除验证错误
    if (validationErrors[fieldName]) {
      setValidationErrors(prev => {
        const newErrors = { ...prev }
        delete newErrors[fieldName]
        return newErrors
      })
    }
  }, [validationErrors])

  // 切换分组展开状态
  const toggleGroup = useCallback((groupName: string) => {
    setExpandedGroups(prev => {
      const newExpanded = new Set(prev)
      if (newExpanded.has(groupName)) {
        newExpanded.delete(groupName)
      } else {
        newExpanded.add(groupName)
      }
      return newExpanded
    })
  }, [])



  // 验证表单（仅在保存时调用，暂时注释）
  // const validateForm = useCallback((): boolean => {
  //   const errors: Record<string, string> = {}
  //   let isValid = true
  //   Object.entries(configData).forEach(([, group]) => {
  //     if (group.items) {
  //       group.items.forEach(item => {
  //         if (!item.hidden && item.required) {
  //           const value = formData[item.name]
  //           if (!value || (Array.isArray(value) && value.length === 0)) {
  //             errors[item.name] = `${item.label || item.name}不能为空`
  //             isValid = false
  //           }
  //         }
  //       })
  //     }
  //     if (group.kubernetesSubGroups) {
  //       Object.entries(group.kubernetesSubGroups).forEach(([, configs]) => {
  //         configs.forEach(item => {
  //           if (!item.hidden && item.required) {
  //             const value = formData[item.name]
  //             if (!value || (Array.isArray(value) && value.length === 0)) {
  //               errors[item.name] = `${item.label || item.name}不能为空`
  //               isValid = false
  //             }
  //           }
  //         })
  //       })
  //     }
  //   })
  //   setValidationErrors(errors)
  //   return isValid
  // }, [configData, formData])

  // 在组件内部处理保存
  const handleSave = useCallback(async () => {
    try {
      setSaving(true)
      // 先检查是否有onSave回调
      if (onSave) {
        // 构建完整的配置项数组，合并表单数据和原始元数据
        const configItems: ConfigItem[] = []
        
        Object.entries(configData).forEach(([, group]) => {
          group.items?.forEach(item => {
            // 创建完整的配置项，保留所有元数据，只更新value
            const completeItem: ConfigItem = {
              ...item,
              value: formData[item.name] !== undefined ? formData[item.name] : item.value
            }
            configItems.push(completeItem)
          })
        })
        
        await onSave(configItems)
      } else {
        toast.success('配置已保存')
      }
    } catch (error) {
      console.error('保存配置失败:', error)
      toast.error('保存配置失败')
    } finally {
      setSaving(false)
    }
  }, [formData, onSave, configData])

  // 过滤配置项
  const filterConfigItems = useCallback((items: ConfigItem[]): ConfigItem[] => {
    if (!searchKeyword) return items.filter(item => !item.hidden)
    
    const keyword = searchKeyword.toLowerCase()
    return items.filter(item => {
      if (item.hidden) return false
      
      const label = (item.label || '').toLowerCase()
      const name = (item.name || '').replaceAll('!', '.').toLowerCase()
      const description = (item.description || '').toLowerCase()
      const value = String(item.value || '').toLowerCase()
      
      return label.includes(keyword) || 
             name.includes(keyword) || 
             description.includes(keyword) ||
             value.includes(keyword)
    })
  }, [searchKeyword])

  // 渲染Kubernetes子组标签（现代化样式）
  const renderKubernetesSubGroupTabs = (groupName: string, subGroups: Record<string, ConfigItem[]>) => {
    const subGroupNames = Object.keys(subGroups)

    return (
      <Tabs defaultValue={subGroupNames[0]} className="w-full">
        <TabsList className="
          grid w-full grid-cols-2 lg:grid-cols-3 mb-6 
          bg-gradient-to-r from-gray-50/80 to-blue-50/60
          border border-gray-200/40 rounded-xl p-1
          backdrop-blur-sm
        ">
          {subGroupNames.map((subGroupName) => (
            <TabsTrigger 
              key={subGroupName} 
              value={subGroupName} 
              className="
                text-sm font-medium rounded-lg
                data-[state=active]:bg-white data-[state=active]:shadow-md
                data-[state=active]:text-blue-700
                hover:bg-white/60 transition-all duration-300
              "
            >
              {formatSubGroupName(subGroupName)}
            </TabsTrigger>
          ))}
        </TabsList>
        
        {subGroupNames.map((subGroupName) => (
          <TabsContent key={subGroupName} value={subGroupName} className="mt-0">
            <div className="space-y-4 p-4 rounded-xl bg-gradient-to-br from-white/60 to-blue-50/20 border border-blue-200/30 backdrop-blur-sm">
              {subGroups[subGroupName].map((item, index) => (
                <div 
                  key={`${groupName}_${subGroupName}_${item.name}_${index}`} 
                  className="
                    p-4 rounded-lg 
                    bg-white/80 border border-white/60
                    hover:shadow-md hover:border-blue-200/40
                    transition-all duration-300
                  "
                >
                  <ConfigItemRenderer
                    item={item}
                    value={formData[item.name]}
                    onChange={(value) => handleFieldChange(item.name, value)}
                    error={validationErrors[item.name]}
                  />
                </div>
              ))}
            </div>
          </TabsContent>
        ))}
      </Tabs>
    )
  }

  // 格式化分组名称
  const formatGroupName = (groupName: string): string => {
    // 处理advanced_和custom_前缀
    if (groupName.startsWith('advanced_')) {
      const serviceName = groupName.replace('advanced_', '')
      return `${serviceName.charAt(0).toUpperCase() + serviceName.slice(1)} 高级配置`
    }
    
    if (groupName.startsWith('custom_')) {
      const serviceName = groupName.replace('custom_', '')
      return `${serviceName.charAt(0).toUpperCase() + serviceName.slice(1)} 自定义配置`
    }

    // 处理常见的服务名称
    const commonNames: Record<string, string> = {
      'NodeExporter': 'Node Exporter',
      'Prometheus': 'Prometheus',
      'General': '通用配置'
    }

    return commonNames[groupName] || groupName
  }

  // 格式化子组名称
  const formatSubGroupName = (subGroupName: string): string => {
    const chineseNames: Record<string, string> = {
      'kubernetes.config.persistent-volume-claims': '持久卷声明',
      'kubernetes.config.persistentVolumeClaims': '持久卷声明',
      'kubernetes.config.resources': '资源规格',
      'kubernetes.config.services': '服务暴露',
      'kubernetes.config.configMaps': '配置映射',
      'kubernetes.config.secrets': '密钥管理',
      'kubernetes.config.nodeSelector': '节点选择',
      'kubernetes.config.affinity': '亲和性',
      'kubernetes.config.tolerations': '容忍度'
    }

    // 支持带角色名的子组格式，如：kubernetes.config.resources.NodeExporter
    const baseGroupName = subGroupName.replace(/\.[A-Z][a-zA-Z]*$/, '')
    return chineseNames[baseGroupName] || chineseNames[subGroupName] || subGroupName.split('.').pop() || subGroupName
  }

  // 渲染配置项（现代化样式）
  const renderConfigItems = (items: ConfigItem[], keyPrefix: string = '') => {
    const filteredItems = filterConfigItems(items)
    
    if (filteredItems.length === 0) {
      return (
        <div className="text-center py-8">
          <Settings className="w-12 h-12 text-gray-300 mx-auto mb-3" />
          <p className="text-gray-500 text-sm">
            {searchKeyword ? '没有找到匹配的配置项' : '暂无配置项'}
          </p>
        </div>
      )
    }

    return (
      <div className="space-y-4">
        {filteredItems.map((item, index) => (
          <div 
            key={`${keyPrefix}_${item.name}_${index}`} 
            className="
              p-4 rounded-xl 
              bg-gradient-to-r from-white/80 to-gray-50/40
              border border-gray-200/30
              hover:shadow-md hover:border-blue-200/50
              transition-all duration-300
            "
          >
            <ConfigItemRenderer
              item={item}
              value={formData[item.name]}
              onChange={(value) => handleFieldChange(item.name, value)}
              error={validationErrors[item.name]}
            />
          </div>
        ))}
      </div>
    )
  }

  // 展开/折叠所有组
  const toggleAllGroups = useCallback(() => {
    const allGroupNames = Object.keys(configData)
    if (expandedGroups.size === allGroupNames.length) {
      // 全部折叠
      setExpandedGroups(new Set())
    } else {
      // 全部展开
      setExpandedGroups(new Set(allGroupNames))
    }
  }, [configData, expandedGroups.size])

  // 效果钩子 - 监听关键参数变化
  useEffect(() => {
    console.log('🔄 useEffect triggered, parameters:', {
      serviceId,
      currentVersion,
      currentRoleGroup,
      clusterId
    })
    
    if (serviceId && currentVersion && currentRoleGroup && clusterId) {
      console.log('✅ 所有参数就绪，开始调用fetchConfigData')
      fetchConfigData()
    } else {
      console.log('⏳ 等待参数完整...', {
        hasServiceId: !!serviceId,
        hasCurrentVersion: !!currentVersion,
        hasCurrentRoleGroup: !!currentRoleGroup,
        hasClusterId: !!clusterId
      })
    }
  }, [serviceId, currentVersion, currentRoleGroup, clusterId, fetchConfigData])

  // 监听配置保存成功事件，刷新配置数据
  useEffect(() => {
    const handleConfigSaved = (event: CustomEvent) => {
      const { serviceId: eventServiceId, clusterId: eventClusterId, currentRoleGroup: eventRoleGroup } = event.detail
      // 只有当前服务的配置保存成功时才刷新
      if (eventServiceId === serviceId && eventClusterId === clusterId && eventRoleGroup === currentRoleGroup) {
        console.log('🔄 配置保存成功，刷新配置数据')
        fetchConfigData()
      }
    }

    window.addEventListener('configSaved', handleConfigSaved as EventListener)
    
    return () => {
      window.removeEventListener('configSaved', handleConfigSaved as EventListener)
    }
  }, [serviceId, clusterId, currentRoleGroup, fetchConfigData])

  // 渲染主内容
  if (loading) {
    return (
      <div className={`flex items-center justify-center h-64 ${className}`}>
        <div className="text-center">
          <div className="animate-spin w-8 h-8 border-3 border-blue-500 border-t-transparent rounded-full mx-auto mb-4"></div>
          <p className="text-gray-600">加载配置数据中...</p>
        </div>
      </div>
    )
  }

  // 渲染配置组（采用现代化样式）
  const renderConfigGroup = (groupName: string, group: ConfigGroup, isExpanded: boolean, hasVisibleItems: boolean, hasKubernetesConfig: boolean) => {
    return (
      <Card 
        key={groupName} 
        className="
          relative overflow-hidden border-0 shadow-lg hover:shadow-xl 
          transition-all duration-500 ease-out rounded-2xl
          bg-gradient-to-br from-white via-gray-50/30 to-white
          ring-1 ring-gray-200/40 hover:ring-blue-200/60
          backdrop-blur-xl
        "
      >
        <CardHeader 
          className="
            cursor-pointer transition-all duration-500 pb-4 rounded-t-2xl
            hover:bg-gradient-to-r hover:from-blue-50/60 hover:via-indigo-50/40 hover:to-purple-50/30
            border-b border-gray-100/60
          "
          onClick={() => toggleGroup(groupName)}
        >
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-semibold text-gray-800">
              {formatGroupName(groupName)}
            </CardTitle>
            <div className="flex items-center space-x-2">
              <Badge variant="outline" className="text-xs bg-blue-50/50 text-blue-700 border-blue-200/50">
                {(hasVisibleItems ? filterConfigItems(group.items!).length : 0) + 
                 (hasKubernetesConfig ? Object.values(group.kubernetesSubGroups!).reduce((count, configs) => count + configs.length, 0) : 0)} 项
              </Badge>
              <div className={`
                p-1.5 rounded-lg transition-all duration-300
                ${isExpanded 
                  ? 'bg-blue-100/80 text-blue-600 rotate-180' 
                  : 'bg-gray-100/60 text-gray-500 hover:bg-blue-50/80 hover:text-blue-500'
                }
              `}>
                <ChevronDown className="h-4 w-4" />
              </div>
            </div>
          </div>
        </CardHeader>
        
        {isExpanded && (
          <CardContent className="pt-0 pb-6 bg-gradient-to-b from-transparent to-gray-50/20">
            {/* 常规配置项 */}
            {hasVisibleItems && (
              <div className="space-y-5 p-4 rounded-xl bg-white/40 backdrop-blur-sm border border-white/60">
                {hasKubernetesConfig && (
                  <div className="flex items-center text-sm font-medium text-gray-600 mb-4">
                    <FileText className="w-4 h-4 mr-2" />
                    常规配置
                  </div>
                )}
                {renderConfigItems(group.items!, groupName)}
              </div>
            )}

            {/* Kubernetes配置 */}
            {hasKubernetesConfig && (
              <div className="mt-8">
                <div className="relative">
                  <Separator className="mb-6 bg-gradient-to-r from-transparent via-gray-300/60 to-transparent" />
                  <div className="absolute left-1/2 top-0 transform -translate-x-1/2 -translate-y-1/2 px-4 bg-white">
                    <div className="px-3 py-1.5 rounded-lg bg-gradient-to-r from-blue-50 to-indigo-50 border border-blue-200/40">
                      <h4 className="text-sm font-semibold text-blue-800">Kubernetes 配置</h4>
                    </div>
                  </div>
                </div>
                
                {renderKubernetesSubGroupTabs(groupName, group.kubernetesSubGroups!)}
              </div>
            )}

            {/* 模板内容 */}
            {group.templateContent && (
              <div className="template-content-section mt-6 p-4 bg-gradient-to-br from-blue-50/20 to-indigo-50/20 rounded-xl border border-blue-200/30 backdrop-blur-sm">
                <label className="text-sm font-medium text-blue-800 mb-2 block flex items-center">
                  <FileText className="w-4 h-4 mr-2" />
                  {formatGroupName(groupName)} 模板内容:
                </label>
                <pre className="text-sm text-gray-700 whitespace-pre-wrap overflow-auto max-h-40 bg-white/60 p-3 rounded-lg border border-white/80">
                  {group.templateContent}
                </pre>
              </div>
            )}
          </CardContent>
        )}
      </Card>
    )
  }

  const groupEntries = Object.entries(configData)
  const allGroupsExpanded = expandedGroups.size === groupEntries.length

  if (groupEntries.length === 0) {
    return (
      <div className={`text-center py-16 ${className}`}>
        <Settings className="w-16 h-16 text-gray-300 mx-auto mb-4" />
        <h3 className="text-xl font-semibold text-gray-700 mb-2">暂无配置参数</h3>
        <p className="text-gray-500">当前服务版本和角色组下没有可配置的参数</p>
      </div>
    )
  }

  return (
    <div className="h-full flex flex-col relative">
      {/* 浮动操作按钮组 */}
      <div className="absolute top-4 right-4 z-10 flex items-center gap-2">
        {onSave && (
          <Button
            type="button"
            variant="ghost"
            size="sm"
            onClick={handleSave}
            disabled={saving}
            className="
              h-9 px-3 
              bg-green-50/80 backdrop-blur-xl border border-green-200/60
              hover:bg-green-100/90 hover:border-green-300/70
              text-green-700 hover:text-green-800
              rounded-xl shadow-lg hover:shadow-xl
              transition-all duration-300 transform hover:scale-[1.02]
              ring-1 ring-green-100/40 hover:ring-green-200/60
              disabled:opacity-50 disabled:transform-none
            "
          >
            {saving ? (
              <>
                <Loader2 className="h-3.5 w-3.5 mr-1 animate-spin" />
                <span className="text-xs font-medium">保存中...</span>
              </>
            ) : (
              <>
                <Save className="h-3.5 w-3.5 mr-1" />
                <span className="text-xs font-medium">保存配置</span>
              </>
            )}
          </Button>
        )}
        <Button
          type="button"
          variant="ghost"
          size="sm"
          onClick={toggleAllGroups}
          className="
            h-9 px-3 
            bg-white/80 backdrop-blur-xl border border-gray-200/60
            hover:bg-gray-50/90 hover:border-blue-200/70
            text-gray-700 hover:text-blue-700
            rounded-xl shadow-lg hover:shadow-xl
            transition-all duration-300 transform hover:scale-[1.02]
            ring-1 ring-gray-100/40 hover:ring-blue-100/60
          "
        >
          {allGroupsExpanded ? (
            <>
              <ChevronUp className="h-3.5 w-3.5 mr-1" />
              <span className="text-xs font-medium">折叠全部</span>
            </>
          ) : (
            <>
              <ChevronDown className="h-3.5 w-3.5 mr-1" />
              <span className="text-xs font-medium">展开全部</span>
            </>
          )}
        </Button>
      </div>

      {/* 配置组列表 */}
      <div className="flex-1 min-h-0 pt-2">
        <div className="h-full overflow-y-auto">
          <div className="space-y-4 pr-4 pt-12">
            {groupEntries.map(([groupName, group]) => {
              const isExpanded = expandedGroups.has(groupName)
              const hasVisibleItems = Boolean(group.items && filterConfigItems(group.items).length > 0)
              const hasKubernetesConfig = Boolean(group.kubernetesSubGroups && Object.keys(group.kubernetesSubGroups).length > 0)

              // 如果没有可见内容，跳过这个组
              if (!hasVisibleItems && !hasKubernetesConfig) {
                return null
              }

              return renderConfigGroup(groupName, group, isExpanded, hasVisibleItems, hasKubernetesConfig)
            })}
          </div>
        </div>
      </div>
    </div>
  )
}
