"use client"

import React from 'react'
import { X, CheckCircle } from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { DialogTitle } from "@/components/ui/dialog"
import { CLUSTER_TYPE_LABELS, ClusterType } from '@/types/cluster-type'

interface Step {
  number: number
  title: string
  description: string
}

interface ClusterWizardSidebarProps {
  steps: Step[]
  currentStep: number
  title: string
  description?: string
  clusterName: string
  isK8s: boolean
  onClose: () => void
}

const ClusterWizardSidebar: React.FC<ClusterWizardSidebarProps> = ({
  steps,
  currentStep,
  title,
  description,
  clusterName,
  isK8s,
  onClose
}) => {
  // 获取正确的集群类型显示名称
  const clusterType = isK8s ? ClusterType.KUBERNETES : ClusterType.PVM
  const clusterTypeLabel = CLUSTER_TYPE_LABELS[clusterType]
  
  return (
    <div className="w-48 sm:w-56 lg:w-64 bg-gradient-to-br from-slate-50 via-white to-blue-50 border-r border-slate-200 flex flex-col min-h-0 relative shadow-xl">
      {/* 装饰性光效边框 */}
      <div className="absolute inset-y-0 right-0 w-px bg-gradient-to-b from-transparent via-blue-300/50 to-transparent"></div>
      
      {/* 头部信息 - 美化设计 */}
      <div className="p-5 sm:p-6 border-b border-slate-200 bg-gradient-to-r from-white via-blue-50 to-white relative">
        {/* 装饰性光效 */}
        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-blue-100/25 to-transparent"></div>
        
        <DialogTitle className="text-lg font-bold text-gray-900 mb-2 relative z-10">{title}</DialogTitle>
        {description && (
          <p className="text-sm text-gray-600 mb-3 leading-relaxed relative z-10">
            {description}
          </p>
        )}
        <div className="flex items-center text-sm text-gray-600 relative z-10">
          <span className="font-medium truncate">{clusterName}</span>
          <Badge className="ml-2 flex-shrink-0 shadow-sm" variant={isK8s ? "default" : "secondary"}>
            {clusterTypeLabel}
          </Badge>
        </div>
      </div>
      
      {/* 步骤列表 - 美化设计 */}
      <div className="flex-1 p-5 sm:p-6 overflow-y-auto min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-slate-300/50 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-slate-400/70">
        <div className="space-y-2">
          {steps.map((step, index) => (
            <div key={step.number} className="relative">
              {/* 步骤项 */}
              <div className={`relative flex items-start p-3.5 rounded-xl transition-all duration-300 hover:scale-[1.02] hover:shadow-lg ${
                currentStep === step.number
                  ? 'bg-gradient-to-r from-blue-50 via-indigo-50 to-blue-50 border border-blue-200 shadow-md'
                  : currentStep > step.number
                  ? 'bg-gradient-to-r from-green-50 via-emerald-50 to-green-50 border border-green-200 shadow-sm'
                  : 'bg-white border border-slate-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-blue-50 hover:border-slate-300'
              }`}>
                {/* 微妙的内部光效 */}
                {currentStep === step.number && (
                  <div className="absolute inset-0 bg-gradient-to-r from-transparent via-blue-100/30 to-transparent rounded-xl"></div>
                )}
                
                {/* 步骤指示器 */}
                <div className="flex-shrink-0 mr-3 mt-0.5 relative z-10">
                  <div className={`w-7 h-7 rounded-full flex items-center justify-center text-xs font-bold transition-all duration-300 relative ${
                    currentStep === step.number
                      ? 'bg-gradient-to-r from-blue-600 to-indigo-600 text-white shadow-lg shadow-blue-500/30'
                      : currentStep > step.number
                      ? 'bg-gradient-to-r from-green-600 to-emerald-600 text-white shadow-lg shadow-green-500/30'
                      : 'bg-gradient-to-r from-slate-300 to-slate-400 text-slate-700 shadow-md'
                  }`}>
                    {/* 指示器光环效果 */}
                    {currentStep === step.number && (
                      <div className="absolute inset-0 rounded-full bg-gradient-to-r from-blue-600 to-indigo-600 opacity-30 animate-pulse"></div>
                    )}
                    {currentStep > step.number ? (
                      <CheckCircle className="w-4 h-4 relative z-10" />
                    ) : (
                      <span className="relative z-10">{step.number}</span>
                    )}
                  </div>
                </div>
                
                {/* 步骤信息 */}
                <div className="flex-1 min-w-0 relative z-10">
                  <div className={`font-semibold text-sm leading-tight mb-1 ${
                    currentStep >= step.number ? 'text-gray-900' : 'text-gray-600'
                  }`}>
                    {step.title}
                  </div>
                  <div className="text-xs text-gray-500 leading-relaxed">
                    {step.description}
                  </div>
                </div>
              </div>
              
              {/* 美化连接线 */}
              {index < steps.length - 1 && (
                <div className="flex justify-start ml-3.5 relative">
                  <div className={`w-px h-5 ml-3.5 relative ${
                    currentStep > step.number 
                      ? 'bg-gradient-to-b from-green-400 to-green-300' 
                      : currentStep === step.number
                      ? 'bg-gradient-to-b from-blue-400 to-blue-300'
                      : 'bg-gradient-to-b from-slate-300 to-slate-200'
                  }`}>
                    {/* 连接线光效 */}
                    {(currentStep > step.number || currentStep === step.number) && (
                      <div className={`absolute inset-0 w-px ${
                        currentStep > step.number 
                          ? 'bg-gradient-to-b from-green-400/50 to-green-300/50 animate-pulse' 
                          : 'bg-gradient-to-b from-blue-400/50 to-blue-300/50 animate-pulse'
                      }`}></div>
                    )}
                  </div>
                </div>
              )}
            </div>
          ))}
        </div>
      </div>
      
      {/* 关闭按钮 - 美化设计 */}
      <div className="p-5 sm:p-6 pb-6 sm:pb-8 border-t border-slate-200 bg-gradient-to-r from-white via-slate-50 to-white relative">
        {/* 装饰性分割线光效 */}
        <div className="absolute top-0 left-4 right-4 h-px bg-gradient-to-r from-transparent via-slate-300/60 to-transparent"></div>
        
        <Button 
          variant="outline" 
          onClick={onClose}
          className="w-full py-3 rounded-xl bg-white border border-slate-300 hover:bg-gradient-to-r hover:from-slate-50 hover:to-blue-50 hover:border-slate-400 hover:shadow-lg transition-all duration-300 text-slate-700 hover:text-slate-900 hover:scale-[1.02] shadow-md"
        >
          <X className="w-4 h-4 mr-2" />
          关闭
        </Button>
      </div>
    </div>
  )
}

export default ClusterWizardSidebar