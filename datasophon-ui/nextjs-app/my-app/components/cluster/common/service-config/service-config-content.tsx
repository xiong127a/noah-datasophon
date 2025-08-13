"use client"

import React, { useCallback, useMemo } from 'react'
import { 
  ChevronDown, ChevronUp, Save, Loader2
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
// import { ScrollArea } from '@/components/ui/scroll-area' // 暂时注释，使用div代替
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'

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
      <Card key={groupName} className="
        relative overflow-hidden border-0 shadow-lg hover:shadow-xl 
        transition-all duration-500 ease-out rounded-2xl
        bg-gradient-to-br from-white via-gray-50/30 to-white
        ring-1 ring-gray-200/40 hover:ring-blue-200/60
        backdrop-blur-xl
      ">
        <CardHeader 
          className="
            cursor-pointer transition-all duration-500 pb-4 rounded-t-2xl
            hover:bg-gradient-to-r hover:from-blue-50/60 hover:via-indigo-50/40 hover:to-purple-50/30
            border-b border-gray-100/60
          "
          onClick={() => handleGroupToggle(groupName)}
        >
          <div className="flex items-center justify-between">
            <CardTitle className="text-base font-semibold text-gray-800">
              {group.displayName}
            </CardTitle>
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
        </CardHeader>
        
        {isExpanded && (
          <CardContent className="pt-0 pb-6 bg-gradient-to-b from-transparent to-gray-50/20">
            {/* 普通配置项 */}
            {group.configs && group.configs.length > 0 && (
              <div className="space-y-5 p-4 rounded-xl bg-white/40 backdrop-blur-sm border border-white/60">
                {group.configs.map((config: ConfigItem) => (
                  <div key={config.name} className="
                    p-4 rounded-xl 
                    bg-gradient-to-r from-white/80 to-gray-50/40
                    border border-gray-200/30
                    hover:shadow-md hover:border-blue-200/50
                    transition-all duration-300
                  ">
                    <ConfigItemRenderer
                      config={config}
                      value={formData[(config.name || '').replace(/\./g, '!')] ?? config.value ?? config.defaultValue ?? ''}
                      error={validationErrors[config.name!]}
                      onChange={(value) => handleFieldChange((config.name || '').replace(/\./g, '!'), value)}
                    />
                  </div>
                ))}
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
                
                <Tabs defaultValue={Object.keys(group.subGroups)[0]} className="w-full">
                  <TabsList className="
                    grid w-full grid-cols-2 lg:grid-cols-3 mb-6 
                    bg-gradient-to-r from-gray-50/80 to-blue-50/60
                    border border-gray-200/40 rounded-xl p-1
                    backdrop-blur-sm
                  ">
                    {Object.entries(group.subGroups).map(([subGroupName, subGroup]: [string, any]) => (
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
                        {subGroup.displayName}
                      </TabsTrigger>
                    ))}
                  </TabsList>
                  
                  {Object.entries(group.subGroups).map(([subGroupName, subGroup]: [string, any]) => (
                    <TabsContent key={subGroupName} value={subGroupName} className="mt-0">
                      <div className="space-y-4 p-4 rounded-xl bg-gradient-to-br from-white/60 to-blue-50/20 border border-blue-200/30 backdrop-blur-sm">
                        {subGroup.configs?.map((config: ConfigItem) => (
                          <div key={config.name} className="
                            p-4 rounded-lg 
                            bg-white/80 border border-white/60
                            hover:shadow-md hover:border-blue-200/40
                            transition-all duration-300
                          ">
                            <ConfigItemRenderer
                              config={config}
                              value={formData[(config.name || '').replace(/\./g, '!')] ?? config.value ?? config.defaultValue ?? ''}
                              error={validationErrors[config.name!]}
                              onChange={(value) => handleFieldChange((config.name || '').replace(/\./g, '!'), value)}
                            />
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

  const allGroupsExpanded = expandedGroups.size === Object.keys(configGroups.groups).length

  return (
    <div className="h-full flex flex-col">
      {/* 苹果风格的操作栏 */}
      <div className="
        flex items-center justify-between mb-6 p-4 
        bg-gradient-to-r from-white via-gray-50/50 to-white
        border border-gray-200/40 rounded-2xl shadow-sm
        backdrop-blur-sm
      ">
        <h3 className="font-semibold text-gray-800 text-lg">{serviceName}</h3>
        
        <div className="flex items-center gap-3">
          <Button
            variant="outline"
            size="sm"
            onClick={toggleAllGroups}
            className="
              border-gray-200/60 bg-white/80 hover:bg-gray-50/80 
              hover:border-blue-200/60 transition-all duration-300
              rounded-xl shadow-sm hover:shadow-md
            "
          >
            {allGroupsExpanded ? (
              <>
                <ChevronUp className="h-4 w-4 mr-1.5" />
                折叠全部
              </>
            ) : (
              <>
                <ChevronDown className="h-4 w-4 mr-1.5" />
                展开全部
              </>
            )}
          </Button>
          
          <Button
            variant="default"
            size="sm"
            onClick={handleSave}
            disabled={saving}
            className="
              bg-gradient-to-r from-blue-600 to-blue-700 hover:from-blue-700 hover:to-blue-800
              border-0 shadow-md hover:shadow-lg
              rounded-xl transition-all duration-300
            "
          >
            {saving ? (
              <Loader2 className="h-4 w-4 mr-1.5 animate-spin" />
            ) : (
              <Save className="h-4 w-4 mr-1.5" />
            )}
            保存配置
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
