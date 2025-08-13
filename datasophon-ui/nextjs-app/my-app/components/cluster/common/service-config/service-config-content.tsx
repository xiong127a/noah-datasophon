"use client"

import React, { useCallback, useMemo } from 'react'
import { 
  ChevronDown, ChevronUp, Save, Package, Database, 
  Settings, AlertTriangle, Info, Loader2
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
// import { ScrollArea } from '@/components/ui/scroll-area' // 暂时注释，使用div代替
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { Badge } from '@/components/ui/badge'
import { Separator } from '@/components/ui/separator'
import { toast } from 'sonner'

import ConfigItemRenderer from './config-item-renderer'
import type { 
  ConfigItem, 
  ConfigGroup, 
  FormData, 
  ServiceConfigGroupData 
} from '@/types/service-config'

interface ServiceConfigContentProps {
  serviceName: string
  configGroups: ServiceConfigGroupData
  formData: FormData
  validationErrors: Record<string, string>
  expandedGroups: Set<string>
  onFormDataChange: (formData: FormData) => void
  onExpandedGroupsChange: (expandedGroups: Set<string>) => void
  onSave: () => Promise<void>
  saving: boolean
}

/**
 * 服务配置内容组件 - 现代化设计
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ServiceConfigContent: React.FC<ServiceConfigContentProps> = ({
  serviceName,
  configGroups,
  formData,
  validationErrors,
  expandedGroups,
  onFormDataChange,
  onExpandedGroupsChange,
  onSave,
  saving
}) => {
  // 处理表单字段变化
  const handleFieldChange = useCallback((fieldName: string, value: unknown) => {
    onFormDataChange({
      ...formData,
      [fieldName]: value
    })
  }, [formData, onFormDataChange])

  // 处理配置组展开/折叠
  const handleGroupToggle = useCallback((groupName: string) => {
    const newExpanded = new Set(expandedGroups)
    if (newExpanded.has(groupName)) {
      newExpanded.delete(groupName)
    } else {
      newExpanded.add(groupName)
    }
    onExpandedGroupsChange(newExpanded)
  }, [expandedGroups, onExpandedGroupsChange])

  // 展开/折叠所有组
  const toggleAllGroups = useCallback(() => {
    const allGroupNames = Object.keys(configGroups.groups)
    if (expandedGroups.size === allGroupNames.length) {
      // 全部折叠
      onExpandedGroupsChange(new Set())
    } else {
      // 全部展开
      onExpandedGroupsChange(new Set(allGroupNames))
    }
  }, [configGroups.groups, expandedGroups.size, onExpandedGroupsChange])

  // 排序配置组
  const sortedGroups = useMemo(() => {
    return Object.entries(configGroups.groups).sort(([groupName1], [groupName2]) => {
      const getGroupPriority = (groupName: string): number => {
        if (groupName === 'General') return 1
        if (groupName.startsWith('advanced_')) return 3
        if (groupName.startsWith('custom_')) return 4
        if (groupName.startsWith('高级')) return 3
        if (groupName.startsWith('自定义')) return 4
        return 2
      }

      const priority1 = getGroupPriority(groupName1)
      const priority2 = getGroupPriority(groupName2)
      
      if (priority1 !== priority2) {
        return priority1 - priority2
      }
      
      return groupName1.localeCompare(groupName2)
    })
  }, [configGroups.groups])

  // 处理保存
  const handleSave = async () => {
    try {
      await onSave()
      toast.success(`${serviceName} 配置保存成功`)
    } catch (error) {
      const err = error as { message?: string }
      toast.error(`${serviceName} 配置保存失败: ${err.message || '未知错误'}`)
    }
  }

  // 渲染配置组
  const renderConfigGroup = (groupName: string, group: any) => {
    const isExpanded = expandedGroups.has(groupName)
    const hasKubernetesConfig = group.subGroups && Object.keys(group.subGroups).length > 0

    return (
      <Card key={groupName} className="border border-gray-200/60 shadow-sm hover:shadow-md transition-all duration-300 bg-white/80 backdrop-blur-sm rounded-xl">
        <CardHeader 
          className="cursor-pointer hover:bg-gradient-to-r hover:from-gray-50/80 hover:to-blue-50/30 transition-all duration-300 pb-3 rounded-t-xl"
          onClick={() => handleGroupToggle(groupName)}
        >
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-medium flex items-center gap-2">
              <Package className="h-4 w-4 text-gray-600" />
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
            {group.configs && group.configs.length > 0 && (
              <div className="space-y-6">
                {group.configs.map((config: ConfigItem) => (
                  <ConfigItemRenderer
                    key={config.name}
                    config={config}
                    value={formData[(config.name || '').replace(/\./g, '!')] ?? config.value ?? config.defaultValue ?? ''}
                    error={validationErrors[config.name!]}
                    onChange={(value) => handleFieldChange((config.name || '').replace(/\./g, '!'), value)}
                  />
                ))}
              </div>
            )}

            {/* Kubernetes配置 */}
            {hasKubernetesConfig && (
              <div className="mt-6">
                <Separator className="mb-4" />
                <div className="flex items-center gap-2 mb-4">
                  <Database className="h-4 w-4 text-blue-600" />
                  <h4 className="text-sm font-medium text-gray-900">Kubernetes 配置</h4>
                </div>
                
                <Tabs defaultValue={Object.keys(group.subGroups)[0]} className="w-full">
                  <TabsList className="grid w-full grid-cols-2 lg:grid-cols-3 mb-4">
                    {Object.entries(group.subGroups).map(([subGroupName, subGroup]: [string, any]) => (
                      <TabsTrigger key={subGroupName} value={subGroupName} className="text-xs">
                        {subGroup.displayName}
                      </TabsTrigger>
                    ))}
                  </TabsList>
                  
                  {Object.entries(group.subGroups).map(([subGroupName, subGroup]: [string, any]) => (
                    <TabsContent key={subGroupName} value={subGroupName} className="mt-4">
                      <div className="space-y-6">
                        {subGroup.configs?.map((config: ConfigItem) => (
                          <ConfigItemRenderer
                            key={config.name}
                            config={config}
                            value={formData[(config.name || '').replace(/\./g, '!')] ?? config.value ?? config.defaultValue ?? ''}
                            error={validationErrors[config.name!]}
                            onChange={(value) => handleFieldChange((config.name || '').replace(/\./g, '!'), value)}
                          />
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

  const allGroupsExpanded = expandedGroups.size === Object.keys(configGroups.groups).length

  return (
    <div className="h-full flex flex-col">
      {/* 简洁的操作栏 */}
      <div className="flex items-center justify-between mb-4">
        <h3 className="font-medium text-gray-900">{serviceName}</h3>
        
        <div className="flex items-center gap-2">
          <Button
            variant="outline"
            size="sm"
            onClick={toggleAllGroups}
          >
            {allGroupsExpanded ? '折叠全部' : '展开全部'}
          </Button>
          
          <Button
            variant="default"
            size="sm"
            onClick={handleSave}
            disabled={saving}
          >
            {saving ? (
              <Loader2 className="h-4 w-4 mr-1 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-1" />
            )}
            保存
          </Button>
        </div>
      </div>

      {/* 配置组列表 */}
      <div className="flex-1 min-h-0">
        <div className="h-full overflow-y-auto">
          <div className="space-y-4 pr-4">
            {sortedGroups.map(([groupName, group]) => 
              renderConfigGroup(groupName, group)
            )}
          </div>
        </div>
      </div>
    </div>
  )
}

export default ServiceConfigContent
