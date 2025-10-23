"use client"

import React from 'react'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
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
  /** 步骤类型 */
  stepsType?: StepsType
  /** 当前步骤标题 */
  stepTitle: string
  /** 当前步骤描述 */
  stepDescription?: string
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
  stepsType = StepsType.NORMAL,
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
  const steps = getStepsByType(stepsType, depType)
  
  // 优先使用传入的具体步骤标题，如果没有再使用通用标题
  const currentStepInfo = steps.find(step => step.number === currentStep)
  const currentStepTitle = stepTitle || currentStepInfo?.title || '集群配置'

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
            {/* 顶栏描述区域 - 简洁现代样式 */}
            {stepDescription && (
              <div className="bg-white border-b border-gray-200 shadow-sm flex-shrink-0">
                <div className="w-full px-4 py-3 sm:px-6 sm:py-4">
                  <div className="space-y-1">
                    <h1 className="text-lg sm:text-xl font-semibold text-gray-900 leading-tight">
                      {stepDescription.split(' - ')[0]}
                    </h1>
                    {stepDescription.includes(' - ') && (
                      <p className="text-sm sm:text-base text-gray-600 leading-relaxed">
                        {stepDescription.split(' - ')[1]}
                      </p>
                    )}
                  </div>
                </div>
              </div>
            )}

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
