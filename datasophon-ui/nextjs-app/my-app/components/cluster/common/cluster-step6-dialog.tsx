"use client"

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { 
  Loader2, AlertCircle, Users, CheckCircle, ChevronLeft, ChevronRight
} from 'lucide-react'
import { Button } from "@/components/ui/button"
import { Checkbox } from "@/components/ui/checkbox"
import { Badge } from '@/components/ui/badge'
import { toast } from 'sonner'
import ClusterWizardSidebar from './cluster-wizard-sidebar'
import { DIALOG_STYLES } from './shared-styles'
import { getStepsByType, StepsType } from '@/lib/cluster-steps'
import { ClusterTypeUtil, ClusterType } from '@/types'
import { Dialog, DialogContent, DialogTitle } from '@/components/ui/dialog'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { clusterApiV1 } from '@/lib/api-utils-v1'

import type {
  ClusterStep6DialogProps,
  Step6Data,
  NonMasterRole,
  TableRowData,
  ServiceRoleHostMapping
} from '@/types/step6'

/**
 * 集群步骤6：分配服务Worker与Client角色对话框
 * 作者：任相鹏
 * 邮箱：635887935@qq.com
 */

const ClusterStep6Dialog: React.FC<ClusterStep6DialogProps> = ({
  open,
  onOpenChange,
  cluster,
  clusterType,
  step4Data,
  onComplete,
  onPrevious
}) => {
  const [loading, setLoading] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [roles, setRoles] = useState<NonMasterRole[]>([])
  const [hosts, setHosts] = useState<any[]>([])
  const [tableData, setTableData] = useState<TableRowData[]>([])
  const [roleNames, setRoleNames] = useState<string[]>([])

  // 计算当前步骤编号
  const isK8s = ClusterTypeUtil.isKubernetes(clusterType)
  const depType = isK8s ? ClusterType.KUBERNETES : ClusterType.PVM
  const steps = getStepsByType(StepsType.NORMAL, depType)
  const currentStepNumber = isK8s ? 5 : 6

  // 获取所有主机
  const getAllHosts = useCallback(async () => {
    if (!cluster?.id) return
    
    setLoading(true)
    try {
      const response = await clusterApiV1.serviceRole.getAllHosts({
        clusterId: cluster.id
      })
      
      if (response?.success && response?.data) {
        setHosts(response.data)
        return response.data
      } else {
        throw new Error(response?.message || '获取主机列表失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取主机列表失败'
      setError(errorMessage)
      toast.error(errorMessage)
      return []
    }
  }, [cluster?.id])

  // 获取非Master角色列表
  const getNonMasterRoleList = useCallback(async (hostList: any[]) => {
    if (!cluster?.id || !step4Data?.serviceIds?.length) return
    
    try {
      const serviceIds = step4Data.serviceIds.join(',')
      const response = await clusterApiV1.serviceRole.getNonMasterRoleList(
        cluster.id,
        serviceIds
      )
      
      if (response?.success && response?.data) {
        const roleData = response.data
        setRoles(roleData)
        
        // 提取角色名称
        const names = roleData.map((role: NonMasterRole) => role.serviceRoleName)
        setRoleNames(names)
        
        // 初始化表格数据
        const initialTableData: TableRowData[] = hostList.map((host: any) => {
          const rowData: TableRowData = {
            id: host.id,
            hostname: host.hostname,
            checkedList: []
          }
          
          // 为每个角色添加字段，并设置初始选中状态
          roleData.forEach((role: NonMasterRole) => {
            const isSelected = role.hosts.includes(host.hostname)
            rowData[role.serviceRoleName] = isSelected
            if (isSelected) {
              rowData.checkedList.push(role.serviceRoleName)
            }
          })
          
          return rowData
        })
        
        setTableData(initialTableData)
      } else {
        throw new Error(response?.message || '获取角色列表失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '获取角色列表失败'
      setError(errorMessage)
      toast.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [cluster?.id, step4Data?.serviceIds])

  // 初始化数据
  useEffect(() => {
    if (open && cluster?.id && step4Data?.serviceIds?.length) {
      const init = async () => {
        const hostList = await getAllHosts()
        if (hostList?.length) {
          await getNonMasterRoleList(hostList)
        }
      }
      init()
    }
  }, [open, cluster?.id, step4Data?.serviceIds, getAllHosts, getNonMasterRoleList])

  // 切换单个主机的角色选择
  const toggleHostRole = useCallback((hostIndex: number, roleName: string) => {
    setTableData(prev => {
      const newData = [...prev]
      const row = { ...newData[hostIndex] }
      
      const isCurrentlySelected = row[roleName]
      row[roleName] = !isCurrentlySelected
      
      if (isCurrentlySelected) {
        // 移除角色
        row.checkedList = row.checkedList.filter(role => role !== roleName)
      } else {
        // 添加角色
        row.checkedList = [...row.checkedList, roleName]
      }
      
      newData[hostIndex] = row
      return newData
    })
  }, [])

  // 切换角色的全选状态
  const toggleRoleSelectAll = useCallback((roleName: string) => {
    const selectedCount = tableData.filter(row => row[roleName]).length
    const shouldSelectAll = selectedCount < tableData.length
    
    setTableData(prev => {
      return prev.map(row => {
        const newRow = { ...row }
        
        if (shouldSelectAll) {
          // 全选
          if (!newRow[roleName]) {
            newRow[roleName] = true
            newRow.checkedList = [...new Set([...newRow.checkedList, roleName])]
          }
        } else {
          // 取消全选
          if (newRow[roleName]) {
            newRow[roleName] = false
            newRow.checkedList = newRow.checkedList.filter(role => role !== roleName)
          }
        }
        
        return newRow
      })
    })
  }, [tableData])

  // 获取角色的选择状态
  const getRoleSelectionState = useCallback((roleName: string) => {
    const selectedCount = tableData.filter(row => row[roleName]).length
    const totalCount = tableData.length
    
    return {
      allSelected: selectedCount === totalCount && totalCount > 0,
      someSelected: selectedCount > 0 && selectedCount < totalCount,
      noneSelected: selectedCount === 0
    }
  }, [tableData])

  // 提交数据
  const handleSubmit = useCallback(async () => {
    if (!cluster?.id) {
      toast.error('集群信息缺失')
      return
    }

    // 构建保存数据
    const saveData: ServiceRoleHostMapping[] = roleNames.map(roleName => {
      const hostsForRole = tableData
        .filter(row => row[roleName])
        .map(row => row.hostname)
      
      return {
        serviceRole: roleName,
        hosts: hostsForRole
      }
    })

    try {
      setLoading(true)
      const response = await clusterApiV1.serviceRole.saveMapping(cluster.id, saveData as any)
      
      if (response?.success) {
        toast.success('服务角色分配保存成功')
        
        // 构建完成数据
        const step6Data: Step6Data = {
          roleHostMappings: saveData,
          selectedRoles: roleNames,
          assignedHosts: tableData.map(row => ({
            hostname: row.hostname,
            roles: row.checkedList
          }))
        }
        
        onComplete(step6Data)
      } else {
        throw new Error(response?.message || '保存失败')
      }
    } catch (err) {
      const errorMessage = err instanceof Error ? err.message : '保存失败'
      toast.error(errorMessage)
    } finally {
      setLoading(false)
    }
  }, [cluster?.id, roleNames, tableData, onComplete])

  // 统计信息
  const stats = useMemo(() => {
    const totalAssignments = tableData.reduce((sum, row) => sum + row.checkedList.length, 0)
    const assignedHosts = tableData.filter(row => row.checkedList.length > 0).length
    
    return {
      totalHosts: tableData.length,
      assignedHosts,
      totalAssignments,
      totalRoles: roleNames.length
    }
  }, [tableData, roleNames])

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className={DIALOG_STYLES.content}>
        <DialogTitle className="sr-only">
          分配服务Worker与Client角色 - {cluster?.clusterName}
        </DialogTitle>
        
        <div className="flex h-full max-h-[min(calc(100vh-96px),900px)] sm:max-h-[min(95vh,900px)]">
          <ClusterWizardSidebar 
            steps={steps}
            currentStep={currentStepNumber}
            title="集群配置向导"
            clusterName={cluster?.clusterName || ''}
            isK8s={isK8s}
            onClose={() => onOpenChange(false)}
          />

          <div className="flex-1 flex flex-col min-w-0">
            {/* 标题区域 */}
            <div className="p-6 sm:p-8 border-b border-slate-200/70 bg-gradient-to-r from-white via-indigo-50/30 to-purple-50/30 relative">
              <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/60 to-transparent"></div>
              <div className="absolute bottom-0 left-6 right-6 h-px bg-gradient-to-r from-transparent via-indigo-200/80 to-transparent"></div>
              <div className="flex items-center justify-between relative z-10">
                <div>
                  <h2 className="text-lg sm:text-xl lg:text-2xl font-bold text-gray-900">
                    分配服务Worker与Client角色
                  </h2>
                  <p className="text-gray-600 mt-1">
                    请为每个服务的Worker和Client角色选择部署的主机
                  </p>
                </div>
                <Badge variant="outline" className="text-indigo-600 border-indigo-200 bg-white/80 backdrop-blur-sm">
                  步骤 {currentStepNumber}/{steps.length}
                </Badge>
              </div>
            </div>

            {/* 主要内容区域 */}
            <div className="flex-1 p-4 min-h-0">
              {loading ? (
                <div className="flex items-center justify-center h-full">
                  <div className="flex items-center space-x-3">
                    <Loader2 className="w-6 h-6 animate-spin text-blue-500" />
                    <span className="text-gray-600 font-medium">正在加载角色分配数据...</span>
                  </div>
                </div>
              ) : error ? (
                <div className="flex flex-col items-center justify-center h-full text-red-500">
                  <AlertCircle className="w-12 h-12 mb-4" />
                  <p className="text-lg font-semibold mb-2">加载失败</p>
                  <p className="text-gray-600">{error}</p>
                  <Button 
                    onClick={() => window.location.reload()}
                    className="mt-4"
                  >
                    重试
                  </Button>
                </div>
              ) : tableData.length === 0 ? (
                <div className="flex flex-col items-center justify-center h-full text-gray-500">
                  <Users className="w-12 h-12 mb-4" />
                  <p className="text-lg font-semibold">暂无数据</p>
                  <p>没有找到可分配的主机或角色</p>
                </div>
              ) : (
                <div className="h-full flex flex-col space-y-4">
                  {/* 统计信息 */}
                  <div className="flex items-center space-x-6 bg-white/70 backdrop-blur-xl rounded-2xl p-4 shadow-lg shadow-black/5 border border-white/20">
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-gradient-to-br from-blue-500 to-blue-600 rounded-xl flex items-center justify-center shadow-lg shadow-blue-500/25">
                        <Users className="w-4 h-4 text-white" />
                      </div>
                      <div>
                        <div className="text-sm font-bold text-gray-900">主机总数</div>
                        <div className="text-xl font-bold text-blue-600">{stats.totalHosts}</div>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-gradient-to-br from-green-500 to-green-600 rounded-xl flex items-center justify-center shadow-lg shadow-green-500/25">
                        <CheckCircle className="w-4 h-4 text-white" />
                      </div>
                      <div>
                        <div className="text-sm font-bold text-gray-900">已分配主机</div>
                        <div className="text-xl font-bold text-green-600">{stats.assignedHosts}</div>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-gradient-to-br from-purple-500 to-purple-600 rounded-xl flex items-center justify-center shadow-lg shadow-purple-500/25">
                        <span className="text-white font-bold text-xs">角色</span>
                      </div>
                      <div>
                        <div className="text-sm font-bold text-gray-900">角色总数</div>
                        <div className="text-xl font-bold text-purple-600">{stats.totalRoles}</div>
                      </div>
                    </div>
                    
                    <div className="flex items-center space-x-2">
                      <div className="w-8 h-8 bg-gradient-to-br from-orange-500 to-orange-600 rounded-xl flex items-center justify-center shadow-lg shadow-orange-500/25">
                        <span className="text-white font-bold text-xs">分配</span>
                      </div>
                      <div>
                        <div className="text-sm font-bold text-gray-900">总分配数</div>
                        <div className="text-xl font-bold text-orange-600">{stats.totalAssignments}</div>
                      </div>
                    </div>
                  </div>

                  {/* 角色分配表格 */}
                  <div className="flex-1 bg-white/90 backdrop-blur-xl rounded-2xl shadow-xl shadow-black/10 border border-white/40 overflow-hidden">
                    <div className="overflow-auto h-full">
                      <table className="w-full">
                        <thead className="bg-gray-50/80 backdrop-blur-sm sticky top-0 z-10">
                          <tr>
                            <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 border-b border-gray-200/80">
                              序号
                            </th>
                            <th className="px-4 py-3 text-left text-sm font-semibold text-gray-900 border-b border-gray-200/80">
                              主机名
                            </th>
                            {roleNames.map(roleName => {
                              const state = getRoleSelectionState(roleName)
                              return (
                                <th key={roleName} className="px-4 py-3 text-center text-sm font-semibold text-gray-900 border-b border-gray-200/80">
                                  <div className="flex items-center justify-center space-x-2">
                                    <Checkbox
                                      checked={state.allSelected}
                                      indeterminate={state.someSelected}
                                      onCheckedChange={() => toggleRoleSelectAll(roleName)}
                                      className="border-gray-300"
                                    />
                                    <span className="font-medium text-gray-900">{roleName}</span>
                                  </div>
                                </th>
                              )
                            })}
                          </tr>
                        </thead>
                        <tbody>
                          {tableData.map((row, index) => (
                            <tr key={row.id} className="hover:bg-gray-50/50 transition-colors">
                              <td className="px-4 py-3 text-sm text-gray-600 border-b border-gray-100">
                                {index + 1}
                              </td>
                              <td className="px-4 py-3 text-sm font-medium text-gray-900 border-b border-gray-100">
                                {row.hostname}
                              </td>
                              {roleNames.map(roleName => (
                                <td key={roleName} className="px-4 py-3 text-center border-b border-gray-100">
                                  <div className="flex justify-center">
                                    <Checkbox
                                      checked={row[roleName] || false}
                                      onCheckedChange={() => toggleHostRole(index, roleName)}
                                      className="border-gray-300"
                                    />
                                  </div>
                                </td>
                              ))}
                            </tr>
                          ))}
                        </tbody>
                      </table>
                    </div>
                  </div>
                </div>
              )}
            </div>

            {/* 底部操作栏 */}
            <div className="bg-white/95 backdrop-blur-md border-t border-gray-200/80 p-4 shadow-lg">
              <div className="flex items-center justify-between">
                {/* 左侧状态信息 */}
                <div className="flex items-center space-x-4">
                  <div className="flex items-center space-x-3">
                    <div className="w-3 h-3 rounded-full bg-blue-500 animate-pulse"></div>
                    <span className="text-sm font-medium text-gray-700">
                      已分配 
                      <span className="mx-1 px-2 py-0.5 bg-blue-100 text-blue-700 rounded-full text-xs font-semibold">
                        {stats.assignedHosts}
                      </span>
                      台主机
                    </span>
                  </div>
                  {stats.totalAssignments > 0 && (
                    <div className="flex items-center space-x-2 px-3 py-1.5 bg-green-50 rounded-lg border border-green-200">
                      <div className="w-2 h-2 rounded-full bg-green-500"></div>
                      <span className="text-sm font-medium text-green-700">
                        总计 {stats.totalAssignments} 个角色分配
                      </span>
                    </div>
                  )}
                </div>

                {/* 右侧按钮 */}
                <div className="flex items-center space-x-3">
                  <Button
                    onClick={() => {
                      if (onPrevious) {
                        onPrevious()
                      } else {
                        onOpenChange(false)
                      }
                    }}
                    variant="outline"
                    className="flex items-center px-5 py-2.5"
                    disabled={loading}
                  >
                    <ChevronLeft className="w-4 h-4 mr-2" />
                    上一步
                  </Button>
                  <Button
                    onClick={handleSubmit}
                    disabled={loading || stats.totalAssignments === 0}
                    className={`flex items-center px-6 py-2.5 rounded-xl text-sm font-medium transition-all duration-200 shadow-md hover:shadow-lg ${
                      loading || stats.totalAssignments === 0
                        ? 'bg-gray-200 text-gray-400 cursor-not-allowed'
                        : 'bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white transform hover:scale-105'
                    }`}
                  >
                    {loading ? (
                      <>
                        <Loader2 className="w-4 h-4 mr-2 animate-spin" />
                        保存中...
                      </>
                    ) : (
                      <>
                        下一步
                        <ChevronRight className="w-4 h-4 ml-2" />
                      </>
                    )}
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

export default ClusterStep6Dialog
