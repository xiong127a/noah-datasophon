"use client"

import React from 'react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { Badge } from '@/components/ui/badge'
import { DIALOG_STYLES } from './shared-styles'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { getStepsByType, StepsType } from '@/lib/cluster-wizard-steps'
import { ClusterTypeUtil, ClusterType } from '@/types'

/**
 * 统一的集群步骤页面布局组件
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

interface ClusterWizardLayoutProps {
  /** 是否显示弹窗 */
  open: boolean
  /** 关闭弹窗回调 */
  onClose: () => void
  /** 集群名称 */
  clusterName: string
  /** 集群类型 */
  clusterType?: string
  /** 当前步骤标题 */
  stepTitle: string
  /** 当前步骤描述 */
  stepDescription: string
  /** 当前步骤编号 */
  currentStep: number
  /** Dialog标题（用于无障碍） */
  dialogTitle: string
  /** 主要内容 */
  children: React.ReactNode
  /** 底部操作栏 */
  actionBar: React.ReactNode
}

const ClusterWizardLayout: React.FC<ClusterWizardLayoutProps> = ({
  open,
  onClose,
  clusterName,
  clusterType = '',
  stepTitle,
  stepDescription,
  currentStep,
  dialogTitle,
  children,
  actionBar,
}) => {
  // 步骤配置
  const isK8s = ClusterTypeUtil.isKubernetes(clusterType)
  const depType = isK8s ? ClusterType.KUBERNETES : ClusterType.PVM
  const steps = getStepsByType(StepsType.NORMAL, depType)
  
  // 获取当前步骤标题
  const currentStepInfo = steps.find(step => step.number === currentStep)
  const currentStepTitle = currentStepInfo?.title || stepTitle

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          {dialogTitle}
        </DialogTitle>

        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          {/* 左侧导航 */}
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStep}
            title={currentStepTitle}
            clusterName={clusterName}
            isK8s={isK8s}
            onClose={onClose}
          />

          {/* 右侧内容区域 */}
          <div className="flex-1 flex flex-col h-full">
            {/* 当前步骤标题 */}
            <div className="flex-shrink-0 p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white via-indigo-50/30 to-purple-50/30 relative">
              {/* 装饰性光效 */}
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              {/* 分割线光效 */}
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    {stepTitle}
                  </h2>
                  <p className="text-gray-600 mt-1">
                    {stepDescription}
                  </p>
                </div>
                <Badge variant="outline" className="text-indigo-600 border-indigo-200 bg-white/80 backdrop-blur-sm">
                  步骤 {currentStep}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 步骤内容 */}
            <div className="flex-1 min-h-0 bg-gradient-to-b from-white to-slate-50/50 overflow-y-auto" style={{ overflowX: 'visible' }}>
              {children}
            </div>

            {/* 底部操作栏 */}
            <div className="flex-shrink-0">
              {actionBar}
            </div>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default ClusterWizardLayout
