'use client'

import React, { createContext, useContext, useState, useEffect, ReactNode } from 'react'
import { clusterApiV1 } from '@/lib/api-utils-v1'
import { createClusterHeaders } from '@/lib/cluster-id-header'

// 简化的配置进度状态
export interface SimpleProgressState {
  clusterId: number | null
  currentStep: number
  completedStep: number
  configStatus: string
  canEnterCluster: boolean
  isLoading: boolean
  error: string | null
}

// Context类型 - 大幅简化
interface SimpleProgressContextType {
  state: SimpleProgressState
  refreshProgress: () => Promise<void>
}

// 创建Context
const SimpleProgressContext = createContext<SimpleProgressContextType | undefined>(undefined)

// Provider组件 - 大幅简化
interface SimpleProgressProviderProps {
  children: ReactNode
  clusterId?: number
}

export function SimpleProgressProvider({ children, clusterId }: SimpleProgressProviderProps) {
  const [state, setState] = useState<SimpleProgressState>({
    clusterId: null,
    currentStep: 1,
    completedStep: 0,
    configStatus: 'UNCONFIGURED',
    canEnterCluster: false,
    isLoading: false,
    error: null
  })

  // 获取配置进度 - 唯一的方法
  const refreshProgress = async () => {
    if (!clusterId) return
    
    setState(prev => ({ ...prev, isLoading: true, error: null }))
    
    try {
      const response = await clusterApiV1.unifiedHost.getConfigProgress(
        createClusterHeaders(clusterId)
      )
      
      if (response.success && response.data) {
        setState(prev => ({
          ...prev,
          clusterId,
          currentStep: response.data.currentStep || 1,
          completedStep: response.data.completedStep || 0,
          configStatus: response.data.configStatus || 'UNCONFIGURED',
          canEnterCluster: response.data.canEnterCluster || false,
          isLoading: false
        }))
      } else {
        setState(prev => ({
          ...prev,
          error: response.message || '获取配置进度失败',
          isLoading: false
        }))
      }
    } catch (error: any) {
      console.error('获取配置进度失败:', error)
      setState(prev => ({
        ...prev,
        error: error.message || '获取配置进度失败',
        isLoading: false
      }))
    }
  }

  // 自动初始化
  useEffect(() => {
    if (clusterId && clusterId !== state.clusterId) {
      refreshProgress()
    }
  }, [clusterId])

  const contextValue: SimpleProgressContextType = {
    state,
    refreshProgress
  }

  return (
    <SimpleProgressContext.Provider value={contextValue}>
      {children}
    </SimpleProgressContext.Provider>
  )
}

// Hook for using the context - 简化版
export function useSimpleProgress() {
  const context = useContext(SimpleProgressContext)
  if (context === undefined) {
    throw new Error('useSimpleProgress must be used within a SimpleProgressProvider')
  }
  return context
}

// 保持向后兼容的导出
export const useConfigProgress = useSimpleProgress
export const ConfigProgressProvider = SimpleProgressProvider

// 步骤信息配置 - 保持不变
export const STEP_CONFIG = {
  1: { name: '安装主机', description: '配置主机列表和SSH连接' },
  2: { name: '主机环境校验', description: '检查主机环境和依赖' },
  3: { name: '主机Agent分发', description: '分发和启动主机Agent' },
  4: { name: '选择服务', description: '选择要安装的大数据服务' },
  5: { name: '分配服务Master角色', description: '配置服务的Master节点' },
  6: { name: '分配服务Worker与Client角色', description: '配置服务的Worker和Client节点' },
  7: { name: '服务配置', description: '配置服务参数和依赖关系' },
  8: { name: '安装并启动服务', description: '执行服务安装和启动' }
} as const

// 辅助函数
export const getStepName = (stepNumber: number): string => {
  return STEP_CONFIG[stepNumber as keyof typeof STEP_CONFIG]?.name || `步骤${stepNumber}`
}

export const getStepDescription = (stepNumber: number): string => {
  return STEP_CONFIG[stepNumber as keyof typeof STEP_CONFIG]?.description || ''
}