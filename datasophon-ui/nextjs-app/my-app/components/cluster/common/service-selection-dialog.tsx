"use client"

import React, { useState } from 'react'
import { 
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


// 导入布局组件
import ClusterWizardLayout from './cluster-wizard-layout'
import ClusterWizardActionBar from './cluster-wizard-action-bar'
import { getStepsByType, StepsType } from '@/lib/cluster-wizard-steps'
import { ClusterTypeUtil, ClusterType } from '@/types'
import { DIALOG_STYLES, BUTTON_STYLES } from './shared-styles'

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
  onPrevious,
  isAddServiceMode = false
}) => {
  // 视图模式状态
  const [viewMode, setViewMode] = useState<'table' | 'grid'>('grid')

  // 计算步骤信息 - 支持添加服务模式
  const safeClusterType = clusterType || ''
  const isK8s = ClusterTypeUtil.isKubernetes(safeClusterType)
  const depType = isK8s ? ClusterType.KUBERNETES : ClusterType.PVM
  
  // 根据模式选择步骤类型
  const stepsType = isAddServiceMode ? StepsType.ADD_SERVICE : StepsType.NORMAL
  const steps = getStepsByType(stepsType, depType)
  
  // 添加服务模式下，当前步骤是第1步（选择服务）；正常模式下是第3/4步
  const currentStepNumber = isAddServiceMode ? 1 : (isK8s ? 3 : 4)

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
    isServiceDisabled,
    fetchServices,
    handleNext,
    canProceed,
    hasRequiredServices
  } = useServiceSelection({
    clusterId: cluster?.id,
    onComplete,
    isAddServiceMode
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

  // 创建统一的ActionBar
  const actionBar = (
    <ClusterWizardActionBar
      statusInfo={{
        text: "服务选择配置",
        value: selectedServices.length,
        total: stats.total,
        pulse: true
      }}
      buttons={[
        ...(onPrevious ? [{
          text: "上一步",
          onClick: () => {
            if (onPrevious) {
              onPrevious()
            } else {
              onOpenChange(false)
            }
          },
          variant: 'secondary' as const,
          disabled: loading
        }] : []),
        {
          text: "下一步",
          onClick: () => {
            if (!hasRequiredServices) {
              toast.error('请确保已选择所有必需服务')
              return
            }
            handleNext()
          },
          disabled: !canProceed || loading,
          loading: false
        }
      ]}
    />
  )

  return (
    <ClusterWizardLayout
      open={open}
      onClose={() => onOpenChange(false)}
      clusterName={cluster?.clusterName || ''}
      clusterType={safeClusterType}
      stepTitle={isAddServiceMode ? "添加服务" : "选择大数据服务"}
      stepDescription={isAddServiceMode 
        ? "添加服务 - 选择要添加到现有集群的大数据服务组件"
        : "选择大数据服务 - 根据您的需求选择要部署的大数据服务组件"
      }
      currentStep={currentStepNumber}
      dialogTitle={`${isAddServiceMode ? "添加服务" : "选择大数据服务"} - ${cluster?.clusterName}`}
      actionBar={actionBar}
    >
      {/* 过滤器区域 */}
      <div className="p-3 border-b border-gray-100">
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
          hideServiceTypeFilter={isAddServiceMode}
        />
      </div>

      {/* 视图切换和内容区域 */}
      <div className="flex-1 flex flex-col min-h-0">
        {/* 视图切换 */}
        <div className="flex items-center justify-end p-2 bg-white border-b border-gray-100">
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
            <TabsContent value="grid" className="h-full overflow-y-auto p-3 mt-0">
              <ServiceCardView
                services={filteredServices}
                selectedServiceIds={selectedServiceIds}
                onToggleService={toggleService}
                isServiceDisabled={isServiceDisabled}
                loading={loading}
              />
            </TabsContent>

            {/* 表格视图 */}
            <TabsContent value="table" className="h-full overflow-y-auto p-3 mt-0">
              <ServiceSelectionTable
                table={table.table}
                loading={loading}
                selectedServiceIds={selectedServiceIds}
                onToggleService={toggleService}
                isServiceDisabled={isServiceDisabled}
              />
            </TabsContent>
          </Tabs>
        </div>
      </div>
    </ClusterWizardLayout>
  )
}

export default ServiceSelectionDialog
