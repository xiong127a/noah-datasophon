"use client"

import React from 'react'
import { X, CheckCircle } from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Badge } from "@/components/ui/badge"
import { DialogTitle } from "@/components/ui/dialog"

interface Step {
  number: number
  title: string
  description: string
}

interface ClusterWizardSidebarProps {
  steps: Step[]
  currentStep: number
  title: string
  clusterName: string
  isK8s: boolean
  onClose: () => void
}

const ClusterWizardSidebar: React.FC<ClusterWizardSidebarProps> = ({
  steps,
  currentStep,
  title,
  clusterName,
  isK8s,
  onClose
}) => {
  return (
    <div className="w-48 sm:w-56 lg:w-64 bg-gradient-to-b from-slate-50/80 via-white/90 to-slate-100/80 backdrop-blur-sm border-r border-slate-200/50 flex flex-col min-h-0 relative">
      {/* 装饰性渐变边框 */}
      <div className="absolute inset-y-0 right-0 w-px bg-gradient-to-b from-indigo-200/0 via-indigo-300/60 to-purple-200/0"></div>
      
      {/* 头部信息 */}
      <div className="p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white/80 to-indigo-50/50 relative">
        {/* 装饰性光效 */}
        <div className="absolute inset-0 bg-gradient-to-r from-transparent via-indigo-100/20 to-transparent"></div>
        <DialogTitle className="text-lg sm:text-xl font-bold text-gray-900 mb-2 relative z-10">{title}</DialogTitle>
        <div className="flex items-center text-sm text-gray-600 relative z-10">
          <span className="font-medium">{clusterName}</span>
          <Badge className="ml-2" variant={isK8s ? "default" : "secondary"}>
            {isK8s ? 'Kubernetes' : '传统部署'}
          </Badge>
        </div>
      </div>
      
      {/* 步骤列表 */}
      <div className="flex-1 p-6 sm:p-8 overflow-y-auto min-h-0 [&::-webkit-scrollbar]:w-2 [&::-webkit-scrollbar-track]:bg-transparent [&::-webkit-scrollbar-thumb]:bg-slate-300/50 [&::-webkit-scrollbar-thumb]:rounded-full [&::-webkit-scrollbar-thumb:hover]:bg-slate-400/70 [&::-webkit-scrollbar]:transition-all">
        <div className="space-y-3">
          {steps.map((step, index) => (
            <div
              key={step.number}
              className={`relative flex items-center p-3 rounded-lg transition-all duration-200 ${
                currentStep === step.number
                  ? 'bg-indigo-50 border border-indigo-200 shadow-sm'
                  : currentStep > step.number
                  ? 'bg-green-50 border border-green-200'
                  : 'bg-white border border-slate-200'
              }`}
            >
              {/* 步骤图标 */}
              <div className={`flex items-center justify-center w-8 h-8 rounded-full text-sm font-bold mr-3 ${
                currentStep === step.number
                  ? 'bg-indigo-600 text-white'
                  : currentStep > step.number
                  ? 'bg-green-600 text-white'
                  : 'bg-slate-200 text-slate-600'
              }`}>
                {currentStep > step.number ? (
                  <CheckCircle className="w-4 h-4" />
                ) : (
                  step.number
                )}
              </div>
              
              {/* 步骤信息 */}
              <div className="flex-1">
                <div className={`font-medium text-sm ${
                  currentStep >= step.number ? 'text-gray-900' : 'text-gray-500'
                }`}>
                  {step.title}
                </div>
                <div className="text-xs text-gray-500 mt-1">
                  {step.description}
                </div>
              </div>
              
              {/* 连接线 */}
              {index < steps.length - 1 && (
                <div className={`absolute left-4 top-[52px] w-px h-6 ${
                  currentStep > step.number ? 'bg-green-300' : 'bg-slate-300'
                }`} />
              )}
            </div>
          ))}
        </div>
      </div>
      
      {/* 关闭按钮 */}
      <div className="p-6 sm:p-8 border-t border-slate-200/70 bg-white/95 backdrop-blur-sm relative">
        {/* 装饰性分割线光效 */}
        <div className="absolute top-0 left-4 right-4 h-px bg-gradient-to-r from-transparent via-indigo-200/60 to-transparent"></div>
        <Button 
          variant="outline" 
          onClick={onClose}
          className="w-full py-3 rounded-2xl bg-white border border-slate-300 hover:bg-slate-50 hover:shadow-lg transition-all duration-300 text-slate-700 hover:text-slate-900 hover:scale-105 shadow-sm"
        >
          <X className="w-4 h-4 mr-2" />
          关闭
        </Button>
      </div>
    </div>
  )
}

export default ClusterWizardSidebar