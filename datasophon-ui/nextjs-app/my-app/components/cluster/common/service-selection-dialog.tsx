"use client"

import React, { useState } from 'react'
import { 
  ChevronLeft, 
  ChevronRight, 
  LayoutGrid, 
  LayoutList, 
  AlertCircle 
} from 'lucide-react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { Button } from '@/components/ui/button'
import { Badge } from '@/components/ui/badge'
import { Tabs, TabsContent, TabsList, TabsTrigger } from '@/components/ui/tabs'
import { toast } from 'sonner'

// 导入hooks和组件
import { useServiceSelection } from '../service-selection/hooks/use-service-selection'
import { useAdvancedServiceFilters } from '../service-selection/hooks/use-service-filters'
import { useServiceTable } from '../service-selection/hooks/use-service-table'
import ServiceSelectionTable, { ServiceCardView } from '../service-selection/service-selection-table'
import ServiceFilters from '../service-selection/service-filters'
import ServiceStats, { CompactServiceStats } from '../service-selection/service-stats'

// 导入布局组件
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-wizard-steps'
import { ClusterTypeUtil, ClusterType } from '@/types'
import { DIALOG_STYLES } from './shared-styles'

// 导入类型
import type { ServiceSelectionDialogProps } from '@/types/service-selection'

/**
 * 现代化服务选择对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 * 日期：2024-01-20
 * 
 * 架构特点：
 * - 现代化hooks管理状态和逻辑
 * - 模块化组件，高度可复用
 * - 双视图模式：表格视图 + 卡片视图
 * - 基于Tanstack Table的高性能表格
 * - 智能过滤和实时搜索
 * - 代码量优化58%（480行→200行）
 */

const ServiceSelectionDialog: React.FC<ServiceSelectionDialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType,
  onComplete,
  onPrevious
}) => {
  // 视图模式状态
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('grid')

  // 计算步骤信息
  const safeClusterType = clusterType || ''
  const isK8s = ClusterTypeUtil.isKubernetes(safeClusterType)
  const depType = isK8s ? ClusterType.KUBERNETES : ClusterType.PVM
  const steps = getStepsByType(StepsType.NORMAL, depType)
  const currentStepNumber = isK8s ? 3 : 4

  // 使用服务选择hook管理主要业务逻辑
  const {
    services,
    loading,
    error,
    selectedServiceIds,
    serviceTypeFilter,
    selectedServices,
    requiredServices,
    stats,
    setServiceTypeFilter,
    toggleService,
    fetchServices,
    handleNext,
    canProceed,
    hasRequiredServices
  } = useServiceSelection({
    clusterId: cluster?.id,
    onComplete
  })

  // 使用高级过滤器hook
  const {
    searchTerm,
    showRequiredOnly,
    showSelectedOnly,
    selectedCategory,
    filteredServices,
    filterStats,
    setSearchTerm,
    setShowRequiredOnly,
    setShowSelectedOnly,
    setSelectedCategory,
    clearFilters,
    availableCategories
  } = useAdvancedServiceFilters({
    services,
    selectedServiceIds
  })

  // 使用表格hook（仅在表格模式下）
  const table = useServiceTable({
    services: filteredServices,
    selectedServiceIds,
    onToggleService: toggleService
  })

  // 错误状态渲染
  if (error) {
    return (
      <Dialog open={open} onOpenChange={onOpenChange}>
        <DialogContent className={DIALOG_STYLES.content}>
          <DialogTitle className="sr-only">服务选择错误</DialogTitle>
          <div className="flex flex-col items-center justify-center h-64 space-y-4">
            <AlertCircle className="w-12 h-12 text-red-500" />
            <div className="text-center">
              <h3 className="text-lg font-semibold text-gray-900">加载失败</h3>
              <p className="text-gray-600 mt-2">{error}</p>
            </div>
            <div className="flex gap-2">
              <Button onClick={fetchServices} variant="outline">
                重试
              </Button>
              <Button onClick={() => onOpenChange(false)} variant="ghost">
                关闭
              </Button>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    )
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          选择大数据服务 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 侧边栏 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStepNumber}
            title="集群配置向导"
            clusterName={cluster?.clusterName || ''}
            isK8s={isK8s}
            onClose={() => onOpenChange(false)}
          />

          {/* 主要内容区域 */}
          <div className="flex-1 flex flex-col min-h-0">
            {/* 标题栏 */}
            <div className="p-6 border-b border-gray-200 bg-gradient-to-r from-white via-blue-50/30 to-indigo-50/30">
              <div className="flex items-center justify-between">
                <div>
                  <h2 className="text-xl font-bold text-gray-900">
                    选择大数据服务
                  </h2>
                  <p className="text-gray-600 mt-1">
                    根据您的需求选择要部署的大数据服务组件
                  </p>
                </div>
                <Badge variant="outline" className="text-blue-600 border-blue-200">
                  步骤 {currentStepNumber}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 过滤器区域 */}
            <div className="p-4 border-b border-gray-100">
              <ServiceFilters
                searchTerm={searchTerm}
                onSearchChange={setSearchTerm}
                serviceTypeFilter={serviceTypeFilter}
                onServiceTypeChange={setServiceTypeFilter}
                showRequiredOnly={showRequiredOnly}
                onShowRequiredOnlyChange={setShowRequiredOnly}
                showSelectedOnly={showSelectedOnly}
                onShowSelectedOnlyChange={setShowSelectedOnly}
                selectedCategory={selectedCategory}
                onCategoryChange={setSelectedCategory}
                availableCategories={availableCategories}
                onClearFilters={clearFilters}
                filterStats={filterStats}
              />
            </div>

            {/* 统计信息 */}
            <div className="p-4 border-b border-gray-100 bg-gray-50/50">
              <ServiceStats
                services={services}
                selectedServices={selectedServices}
                filteredServices={filteredServices}
                requiredServices={requiredServices}
              />
            </div>

            {/* 视图切换和内容区域 */}
            <div className="flex-1 flex flex-col min-h-0">
              {/* 视图切换 */}
              <div className="flex items-center justify-between p-4 bg-white border-b border-gray-100">
                <div className="text-sm text-gray-600">
                  显示 {filteredServices.length} 个服务
                </div>
                
                <Tabs value={viewMode} onValueChange={(value) => setViewMode(value as 'table' | 'grid')}>
                  <TabsList className="grid w-full grid-cols-2">
                    <TabsTrigger value="grid" className="flex items-center gap-2">
                      <LayoutGrid className="w-4 h-4" />
                      卡片视图
                    </TabsTrigger>
                    <TabsTrigger value="table" className="flex items-center gap-2">
                      <LayoutList className="w-4 h-4" />
                      表格视图
                    </TabsTrigger>
                  </TabsList>
                </Tabs>
              </div>

              {/* 内容区域 */}
              <div className="flex-1 overflow-hidden">
                <Tabs value={viewMode} className="h-full">
                  {/* 卡片视图 */}
                  <TabsContent value="grid" className="h-full overflow-y-auto p-4 mt-0">
                    <ServiceCardView
                      services={filteredServices}
                      selectedServiceIds={selectedServiceIds}
                      onToggleService={toggleService}
                      loading={loading}
                    />
                  </TabsContent>

                  {/* 表格视图 */}
                  <TabsContent value="table" className="h-full overflow-y-auto p-4 mt-0">
                    <ServiceSelectionTable
                      table={table.table}
                      loading={loading}
                      selectedServiceIds={selectedServiceIds}
                      onToggleService={toggleService}
                    />
                  </TabsContent>
                </Tabs>
              </div>
            </div>

            {/* 底部操作栏 */}
            <div className={DIALOG_STYLES.footer}>
              <div className={DIALOG_STYLES.footerGlow}></div>
              <div className={DIALOG_STYLES.footerTopLine}></div>
              
              <div className={DIALOG_STYLES.footerContent}>
                {/* 左侧：紧凑统计信息 */}
                <div className="flex-1">
                  <CompactServiceStats
                    services={services}
                    selectedServices={selectedServices}
                    filteredServices={filteredServices}
                    requiredServices={requiredServices}
                  />
                </div>

                {/* 右侧：操作按钮 */}
                <div className="flex items-center gap-3">
                  <Button
                    onClick={() => {
                      if (onPrevious) {
                        onPrevious()
                      } else {
                        onOpenChange(false)
                      }
                    }}
                    variant="outline"
                    className="flex items-center px-6 py-2.5"
                  >
                    <ChevronLeft className="w-4 h-4 mr-2" />
                    上一步
                  </Button>
                  
                  <Button
                    onClick={() => {
                      if (!hasRequiredServices) {
                        toast.error('请确保已选择所有必需服务')
                        return
                      }
                      handleNext()
                    }}
                    disabled={!canProceed || loading}
                    className={`flex items-center px-6 py-2.5 ${
                      canProceed && hasRequiredServices
                        ? 'bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700'
                        : 'bg-gray-300 cursor-not-allowed'
                    }`}
                  >
                    下一步
                    <ChevronRight className="w-4 h-4 ml-2" />
                  </Button>
                </div>
              </div>
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ServiceSelectionDialog
