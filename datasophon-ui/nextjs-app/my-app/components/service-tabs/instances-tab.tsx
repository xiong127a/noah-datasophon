"use client"

/**
 * 服务实例管理页面 - 完整功能迁移
 * 
 * @author 任相鹏
 * @email 635887935@qq.com
 * @date 2025-01-20
 */

import React, { useState, useEffect } from 'react'
import { useSearchParams } from 'next/navigation'
import { 
  Server, 
  Play, 
  Pause, 
  RotateCcw, 
  Trash2, 
  Plus, 
  Search,
  RefreshCw,
  Settings,
  Eye,
  Users,
  Activity,
  AlertTriangle,
  CheckCircle,
  XCircle,
  Clock,
  Zap
} from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '@/components/ui/table'
import { Checkbox } from '@/components/ui/checkbox'
import { Badge } from '@/components/ui/badge'
import { 
  DropdownMenu, 
  DropdownMenuContent, 
  DropdownMenuItem, 
  DropdownMenuTrigger,
  DropdownMenuSeparator 
} from '@/components/ui/dropdown-menu'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import { Dialog, DialogContent, DialogHeader, DialogTitle } from '@/components/ui/dialog'
import { Textarea } from '@/components/ui/textarea'
import { apiV1, API_PATHS_V1 } from '@/lib/api-config-v1'

interface InstancesTabProps {
  serviceId: string
  serviceName: string
}

interface Instance {
  id: string
  serviceRoleName: string
  hostname: string
  roleGroupId: string
  roleGroupName: string
  serviceRoleState: string
  serviceRoleStateCode: number
  needRestart: boolean
  alertNum: number
}

interface RoleGroup {
  id: string
  roleGroupName: string
}

interface ServiceRole {
  serviceRoleName: string
}

// 状态配置
const SERVICE_STATES = [
  { id: "1", key: "正在运行", color: "success" },
  { id: "2", key: "停止", color: "error" },
  { id: "3", key: "告警", color: "warning" },
  { id: "4", key: "退役中", color: "info" },
  { id: "5", key: "已退役", color: "secondary" },
]

export default function InstancesTab({ serviceId, serviceName }: InstancesTabProps) {
  const searchParams = useSearchParams()
  const clusterId = searchParams.get('clusterId') || ''
  
  // 基础状态
  const [mounted, setMounted] = useState(false)
  const [loading, setLoading] = useState(false)
  const [instances, setInstances] = useState<Instance[]>([])
  const [totalCount, setTotalCount] = useState(0)
  const [selectedRowKeys, setSelectedRowKeys] = useState<string[]>([])
  
  // 分页状态
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })
  
  // 筛选状态
  const [filters, setFilters] = useState({
    hostname: '',
    serviceRoleName: 'all',
    roleGroupId: 'all',
    serviceRoleState: 'all',
  })
  
  // 下拉选项数据
  const [roleGroups, setRoleGroups] = useState<RoleGroup[]>([])
  const [serviceRoles, setServiceRoles] = useState<ServiceRole[]>([])
  
  // 自动伸缩状态（针对SEATUNNEL）
  const [autoScaleEnabled, setAutoScaleEnabled] = useState(false)
  
  // 对话框状态
  const [logDialogVisible, setLogDialogVisible] = useState(false)
  const [currentInstanceId, setCurrentInstanceId] = useState('')
  
  // 定时器
  const [timer, setTimer] = useState<NodeJS.Timeout | null>(null)

  // 同步clusterId到localStorage，供拦截器使用
  useEffect(() => {
    if (clusterId && typeof window !== 'undefined') {
      localStorage.setItem('clusterId', clusterId)
    }
  }, [clusterId])

  // 组件挂载
  useEffect(() => {
    let isMounted = true
    
    const initialize = async () => {
      if (isMounted) {
        setMounted(true)
        await Promise.all([
          loadRoleGroups(),
          loadServiceRoles(),
          loadAutoScaleStatus()
        ])
        startPolling()
      }
    }
    
    initialize()
    
    return () => {
      isMounted = false
      if (timer) clearInterval(timer)
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [serviceId])

  // 分页变更时重新加载数据
  useEffect(() => {
    if (mounted) {
      loadInstances()
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [pagination.current, pagination.pageSize])



  // 获取角色组列表
  const loadRoleGroups = async () => {
    if (!serviceId) {
      console.error('serviceId is required for loadRoleGroups')
      return
    }
    
    try {
      const finalUrl = `${API_PATHS_V1.CLUSTER_SERVICE_ROLE_GROUP_LIST}?serviceInstanceId=${serviceId}`
      console.log('loadRoleGroups called with serviceId:', serviceId, 'Final URL:', finalUrl)
      const response = await apiV1.get(finalUrl)
      if (response.data.code === 200) {
        const roleGroupsData = response.data.data || []
        console.log('角色组数据:', roleGroupsData)
        setRoleGroups(roleGroupsData)
      }
    } catch (error) {
      console.error('获取角色组列表失败:', error)
    }
  }

  // 获取服务角色类型
  const loadServiceRoles = async () => {
    if (!serviceId) {
      console.error('serviceId is required for loadServiceRoles')
      return
    }
    
    try {
      const finalUrl = `${API_PATHS_V1.CLUSTER_SERVICE_ROLE_TYPE_LIST}?serviceInstanceId=${serviceId}`
      console.log('loadServiceRoles called with serviceId:', serviceId, 'Final URL:', finalUrl)
      const response = await apiV1.get(finalUrl)
      if (response.data.code === 200) {
        const serviceRolesData = response.data.data || []
        console.log('服务角色类型数据:', serviceRolesData)
        setServiceRoles(serviceRolesData)
      }
    } catch (error) {
      console.error('获取服务角色类型失败:', error)
    }
  }

  // 获取自动伸缩状态
  const loadAutoScaleStatus = async () => {
    if (serviceName !== 'SEATUNNEL') return
    
    try {
      const response = await apiV1.get(API_PATHS_V1.AUTO_SCALE_STATUS, {
        clusterId: parseInt(clusterId),
        page: 1,
        pageSize: 10
      })
      if (response.data.code === 200) {
        // 如果有自动伸缩任务记录，说明已启用
        const tasks = response.data.data?.records || []
        setAutoScaleEnabled(tasks.length > 0)
      }
    } catch (error) {
      console.error('获取自动伸缩状态失败:', error)
    }
  }

  // 获取实例列表
  const loadInstances = async (silent = false) => {
    if (!serviceId) {
      console.error('serviceId is required for loadInstances')
      return
    }
    
    if (!silent) setLoading(true)
    
    try {
      console.log('loadInstances called with serviceId:', serviceId)
      // 构建查询参数
      const queryParams = new URLSearchParams({
        serviceInstanceId: serviceId,
        page: pagination.current.toString(),
        pageSize: pagination.pageSize.toString()
      })
      
      // 添加可选参数
      if (filters.hostname) {
        queryParams.append('hostname', filters.hostname)
      }
      if (filters.serviceRoleState && filters.serviceRoleState !== 'all') {
        queryParams.append('serviceRoleState', filters.serviceRoleState)
      }
      if (filters.roleGroupId && filters.roleGroupId !== 'all') {
        queryParams.append('roleGroupId', filters.roleGroupId)
      }
      if (filters.serviceRoleName && filters.serviceRoleName !== 'all') {
        queryParams.append('serviceRoleName', filters.serviceRoleName)
      }
      
      const finalUrl = `${API_PATHS_V1.CLUSTER_SERVICE_ROLE_INSTANCE_LIST}?${queryParams.toString()}`
      console.log('Final API URL:', finalUrl)
      const response = await apiV1.get(finalUrl)
      
      if (response.data.code === 200) {
        const pageData = response.data.data || {}
        const instances = pageData.records || []
        console.log('实例分页数据:', pageData)
        console.log('实例记录:', instances)
        setInstances(instances)
        setPagination(prev => ({
          ...prev,
          total: parseInt(pageData.total) || 0,
          current: parseInt(pageData.current) || 1,
          pageSize: parseInt(pageData.size) || 10
        }))
        setTotalCount(parseInt(pageData.total) || 0)
      } else {
        console.error('API返回错误码:', response.data.code, response.data.msg)
        setInstances([])
        setTotalCount(0)
      }
    } catch (error) {
      console.error('获取实例列表失败:', error)
      setInstances([])
      setTotalCount(0)
    } finally {
      if (!silent) setLoading(false)
    }
  }

  // 启动轮询
  const startPolling = () => {
    // 立即加载一次
    loadInstances()
    
    // 清除现有定时器
    if (timer) clearInterval(timer)
    
    // 设置新的定时器
    const newTimer = setInterval(() => {
      loadInstances(true) // 静默刷新
    }, 10000) // 10秒刷新间隔
    
    setTimer(newTimer)
  }

  // 搜索处理
  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }))
    loadInstances()
  }

  // 筛选变更
  const handleFilterChange = (field: string, value: string) => {
    setFilters(prev => ({ ...prev, [field]: value }))
  }

  // 表格选择变更
  const handleSelectionChange = (instanceId: string, checked: boolean) => {
    if (checked) {
      setSelectedRowKeys(prev => [...prev, instanceId])
    } else {
      setSelectedRowKeys(prev => prev.filter(key => key !== instanceId))
    }
  }

  // 全选/取消全选
  const handleSelectAll = (checked: boolean) => {
    if (checked && Array.isArray(instances)) {
      setSelectedRowKeys(instances.map(item => item.id))
    } else {
      setSelectedRowKeys([])
    }
  }

  // 批量操作
  const handleBatchOperation = async (operation: string) => {
    if (selectedRowKeys.length === 0) {
      alert('请至少选择一个实例')
      return
    }

    // 操作确认
    const operationNames: Record<string, string> = {
      start: '启动',
      stop: '停止',
      restart: '重启',
      delete: '删除',
      decommission: '退役'
    }

    const confirmed = confirm(`确认${operationNames[operation]}选中的 ${selectedRowKeys.length} 个实例吗？`)
    if (!confirmed) return

    const operationMap: Record<string, string> = {
      start: 'START_SERVICE',
      stop: 'STOP_SERVICE',
      restart: 'RESTART_SERVICE'
    }

    try {
      if (operation === 'delete') {
        const response = await apiV1.delete(API_PATHS_V1.CLUSTER_SERVICE_ROLE_INSTANCE_DELETE, {
          data: selectedRowKeys
        })
        if (response.data.code === 200) {
          alert('删除成功')
          setSelectedRowKeys([])
          loadInstances()
        } else {
          alert(response.data.msg || '删除失败')
        }
      } else if (operation === 'decommission') {
        // 退役功能，仅支持HDFS和YARN服务
        const response = await apiV1.post(API_PATHS_V1.DECOMMISSION_NODE, {
          serviceRoleInstanceIds: selectedRowKeys.join(',')
        })
        if (response.data.code === 200) {
          alert('退役操作成功')
          setSelectedRowKeys([])
          loadInstances()
        } else {
          alert(response.data.msg || '退役操作失败')
        }
      } else {
              const response = await apiV1.post(API_PATHS_V1.GENERATE_SERVICE_ROLE_COMMAND, {
          commandType: operationMap[operation],
          serviceInstanceId: parseInt(serviceId),
          serviceRoleInstancesIds: selectedRowKeys
        })
        if (response.data.code === 200) {
          alert('操作成功')
          setSelectedRowKeys([])
          loadInstances()
        } else {
          alert(response.data.msg || '操作失败')
        }
      }
    } catch (error) {
      console.error('批量操作失败:', error)
      alert('操作失败，请重试')
    }
  }

  // 查看日志
  const handleViewLog = async (instanceId: string) => {
    try {
      setCurrentInstanceId(instanceId)
      setLogDialogVisible(true)
      
      // TODO: 实现真实的日志获取逻辑
      // 根据Vue项目的实现，这里应该调用API获取实时日志
      console.log('查看日志功能被调用，instanceId:', instanceId)
      
      // 暂时显示开发中的提示
      setTimeout(() => {
        alert('日志功能正在开发中，将支持实时日志流显示')
      }, 500)
      
    } catch (error) {
      console.error('获取日志失败:', error)
      alert('获取日志失败，请重试')
    }
  }

  // 切换自动伸缩
  const toggleAutoScale = async () => {
    const apiPath = autoScaleEnabled ? API_PATHS_V1.AUTO_SCALE_UPDATE : API_PATHS_V1.AUTO_SCALE_CREATE
    
    try {
      const response = await apiV1.post(apiPath, {})
      if (response.data.code === 200) {
        setAutoScaleEnabled(!autoScaleEnabled)
        alert('操作成功')
        loadInstances()
      }
    } catch (error) {
      console.error('自动伸缩操作失败:', error)
      alert('操作失败，请重试')
    }
  }

  // 添加新实例
  const handleAddInstance = () => {
    // 这里应该打开一个模态框或跳转到添加实例页面
    // 根据Vue项目的实现，这个功能需要跳转到安装向导
    alert('添加新实例功能开发中，敬请期待！')
    console.log('添加新实例功能被调用，serviceId:', serviceId)
    // TODO: 实现添加实例的模态框或页面跳转
  }

  // 添加角色组  
  const handleAddRoleGroup = () => {
    // 这里应该打开添加角色组的模态框
    alert('添加角色组功能开发中，敬请期待！')
    console.log('添加角色组功能被调用，serviceId:', serviceId)
    // TODO: 实现添加角色组的模态框
  }

  // 获取状态徽章
  const getStatusBadge = (stateCode: number, state: string) => {
    const stateConfig = SERVICE_STATES.find(s => s.id === stateCode.toString())
    const color = stateConfig?.color || 'secondary'
    let variant: 'default' | 'destructive' | 'outline' | 'secondary' = 'secondary'
    
    switch (color) {
      case 'success':
        variant = 'default'
        break
      case 'error':
        variant = 'destructive'
        break
      case 'warning':
        variant = 'outline'
        break
      default:
        variant = 'secondary'
        break
    }
    
    return (
      <Badge variant={variant} className="flex items-center gap-1">
        {stateCode === 1 && <CheckCircle className="w-3 h-3" />}
        {stateCode === 2 && <XCircle className="w-3 h-3" />}
        {stateCode === 3 && <AlertTriangle className="w-3 h-3" />}
        {stateCode === 4 && <Clock className="w-3 h-3" />}
        {stateCode === 5 && <XCircle className="w-3 h-3" />}
        {state}
      </Badge>
    )
  }

  if (!mounted) return null

  return (
    <div className="h-full bg-gradient-to-br from-slate-50 to-blue-100/60 p-6 relative overflow-hidden">
      {/* 苹果风格装饰元素 */}
      <div className="absolute inset-0 pointer-events-none">
        <div className="absolute top-20 right-16 w-32 h-32 bg-blue-200/20 rounded-full blur-3xl animate-pulse"></div>
        <div className="absolute bottom-20 left-16 w-24 h-24 bg-indigo-200/15 rounded-full blur-2xl animate-pulse" style={{animationDelay: '1s'}}></div>
        <div className="absolute top-8 left-8 w-1 h-1 bg-blue-300/60 rounded-full animate-bounce"></div>
        <div className="absolute bottom-16 right-20 w-1.5 h-1.5 bg-slate-300/50 rounded-full animate-bounce" style={{animationDelay: '0.5s'}}></div>
      </div>

      <div className="relative z-10 h-full">
        {/* 页面标题 */}
        <div className="mb-6">
          <h1 className="text-2xl font-bold text-gray-900 flex items-center gap-3">
            <div className="p-2 bg-blue-500/10 rounded-xl">
              <Server className="w-6 h-6 text-blue-600" />
            </div>
            服务实例管理
          </h1>
          <p className="text-gray-600 mt-2">管理 {serviceName} 服务的所有实例</p>
        </div>

        {/* 搜索和筛选区域 */}
        <Card className="mb-6 backdrop-blur-xl bg-white/95 border-white/40 shadow-xl">
          <CardContent className="p-6">
            <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-5 gap-4 items-end">
              <div className="space-y-2">
                <label className="text-sm font-medium text-gray-700">主机名</label>
                <Input
                  placeholder="请输入主机名"
                  value={filters.hostname}
                  onChange={(e) => handleFilterChange('hostname', e.target.value)}
                  className="bg-white/80"
                />
              </div>
              
              <div className="space-y-2">
                <label className="text-sm font-medium text-gray-700">角色类型</label>
                <Select value={filters.serviceRoleName} onValueChange={(value) => handleFilterChange('serviceRoleName', value)}>
                  <SelectTrigger className="bg-white/80">
                    <SelectValue placeholder="请选择角色类型" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">全部</SelectItem>
                    {serviceRoles.map(role => (
                      <SelectItem key={role.serviceRoleName} value={role.serviceRoleName}>
                        {role.serviceRoleName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              
              <div className="space-y-2">
                <label className="text-sm font-medium text-gray-700">角色组</label>
                <Select value={filters.roleGroupId} onValueChange={(value) => handleFilterChange('roleGroupId', value)}>
                  <SelectTrigger className="bg-white/80">
                    <SelectValue placeholder="请选择角色组" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">全部</SelectItem>
                    {roleGroups.map(group => (
                      <SelectItem key={group.id} value={group.id}>
                        {group.roleGroupName}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              
              <div className="space-y-2">
                <label className="text-sm font-medium text-gray-700">状态</label>
                <Select value={filters.serviceRoleState} onValueChange={(value) => handleFilterChange('serviceRoleState', value)}>
                  <SelectTrigger className="bg-white/80">
                    <SelectValue placeholder="请选择状态" />
                  </SelectTrigger>
                  <SelectContent>
                    <SelectItem value="all">全部</SelectItem>
                    {SERVICE_STATES.map(state => (
                      <SelectItem key={state.id} value={state.id}>
                        {state.key}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>
              
              <Button onClick={handleSearch} className="bg-blue-600 hover:bg-blue-700">
                <Search className="w-4 h-4 mr-2" />
                搜索
              </Button>
            </div>
          </CardContent>
        </Card>

        {/* 操作工具栏 */}
        <Card className="mb-6 backdrop-blur-xl bg-white/95 border-white/40 shadow-xl">
          <CardContent className="p-4">
            <div className="flex items-center justify-between flex-wrap gap-4">
              <div className="flex items-center gap-3 flex-wrap">
                <DropdownMenu>
                  <DropdownMenuTrigger asChild>
                    <Button variant="outline" className="bg-white/80">
                      <Settings className="w-4 h-4 mr-2" />
                      选择操作
                    </Button>
                  </DropdownMenuTrigger>
                  <DropdownMenuContent>
                    <DropdownMenuItem onClick={() => handleBatchOperation('start')}>
              <Play className="w-4 h-4 mr-2" />
                      启动
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleBatchOperation('stop')}>
              <Pause className="w-4 h-4 mr-2" />
                      停止
                    </DropdownMenuItem>
                    <DropdownMenuItem onClick={() => handleBatchOperation('restart')}>
              <RotateCcw className="w-4 h-4 mr-2" />
                      重启
                    </DropdownMenuItem>
                    <DropdownMenuSeparator />
                    {(serviceName === 'HDFS' || serviceName === 'YARN') && (
                      <DropdownMenuItem onClick={() => handleBatchOperation('decommission')}>
                        <XCircle className="w-4 h-4 mr-2" />
                        退役
                      </DropdownMenuItem>
                    )}
                    <DropdownMenuItem onClick={() => handleBatchOperation('delete')} className="text-red-600">
                      <Trash2 className="w-4 h-4 mr-2" />
                      删除
                    </DropdownMenuItem>
                  </DropdownMenuContent>
                </DropdownMenu>
                
                {serviceName === 'SEATUNNEL' && (
                  <Button 
                    onClick={toggleAutoScale}
                    variant={autoScaleEnabled ? "destructive" : "default"}
                    className="bg-orange-500 hover:bg-orange-600 text-white"
                  >
                    <Zap className="w-4 h-4 mr-2" />
                    {autoScaleEnabled ? '关闭自动伸缩' : '开启自动伸缩'}
            </Button>
                )}
          </div>
              
              <div className="flex items-center gap-3">
                <Button variant="outline" className="bg-white/80" onClick={handleAddInstance}>
                  <Plus className="w-4 h-4 mr-2" />
                  添加新实例
                </Button>
                <Button variant="outline" className="bg-white/80" onClick={handleAddRoleGroup}>
                  <Users className="w-4 h-4 mr-2" />
                  添加角色组
                </Button>
                <Button onClick={() => loadInstances()} size="sm" variant="ghost">
                  <RefreshCw className="w-4 h-4" />
          </Button>
        </div>
      </div>
          </CardContent>
        </Card>

        {/* 实例表格 */}
        <Card className="backdrop-blur-xl bg-white/95 border-white/40 shadow-xl">
          <CardHeader>
            <CardTitle className="flex items-center justify-between">
              <span>实例列表</span>
              <Badge variant="secondary" className="bg-blue-100 text-blue-800">
                共 {totalCount} 个实例
              </Badge>
            </CardTitle>
          </CardHeader>
          <CardContent>
            <div className="rounded-lg border bg-white/50 overflow-hidden">
              <Table>
                <TableHeader>
                  <TableRow className="bg-gray-50/50">
                    <TableHead className="w-12">
                      <Checkbox 
                        checked={selectedRowKeys.length === instances.length && instances.length > 0}
                        onCheckedChange={handleSelectAll}
                      />
                    </TableHead>
                    <TableHead>序号</TableHead>
                    <TableHead>角色类型</TableHead>
                    <TableHead>主机</TableHead>
                    <TableHead>角色组</TableHead>
                    <TableHead>状态</TableHead>
                    <TableHead className="text-right">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {loading ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center py-8">
                        <div className="flex items-center justify-center">
                          <RefreshCw className="w-4 h-4 mr-2 animate-spin" />
                          加载中...
                        </div>
                      </TableCell>
                    </TableRow>
                  ) : !Array.isArray(instances) || instances.length === 0 ? (
                    <TableRow>
                      <TableCell colSpan={7} className="text-center py-8">
                        <div className="flex flex-col items-center gap-2">
                          <Server className="w-8 h-8 text-gray-400" />
                          <span className="text-gray-500">暂无实例数据</span>
                        </div>
                      </TableCell>
                    </TableRow>
                  ) : (
                    instances.map((instance, index) => (
                      <TableRow key={instance.id} className="hover:bg-blue-50/50 transition-colors">
                        <TableCell>
                          <Checkbox 
                            checked={selectedRowKeys.includes(instance.id)}
                            onCheckedChange={(checked) => handleSelectionChange(instance.id, checked as boolean)}
                          />
                        </TableCell>
                        <TableCell>{(pagination.current - 1) * pagination.pageSize + index + 1}</TableCell>
                        <TableCell>
                          <div className="flex items-center gap-2">
                            <div className={`w-2 h-2 rounded-full ${
                              instance.serviceRoleStateCode === 1 ? 'bg-green-500' :
                              instance.serviceRoleStateCode === 2 ? 'bg-red-500' :
                              'bg-yellow-500'
                            }`} />
                            {instance.serviceRoleName}
                            {instance.needRestart && (
                              <RotateCcw className="w-3 h-3 text-orange-500" />
                            )}
                          </div>
                        </TableCell>
                        <TableCell className="font-mono text-sm">{instance.hostname}</TableCell>
                        <TableCell>{instance.roleGroupName}</TableCell>
                        <TableCell>
                          {getStatusBadge(instance.serviceRoleStateCode, instance.serviceRoleState)}
                        </TableCell>
                        <TableCell className="text-right">
                          <div className="flex items-center justify-end gap-2">
                            <Button 
                              variant="ghost" 
                              size="sm"
                              onClick={() => handleViewLog(instance.id)}
                              className="hover:bg-blue-100"
                            >
                              <Eye className="w-4 h-4 mr-1" />
                              日志
                            </Button>
                          </div>
                        </TableCell>
                      </TableRow>
                    ))
                  )}
                </TableBody>
              </Table>
            </div>
            
            {/* 分页 */}
            {instances.length > 0 && (
              <div className="flex items-center justify-between mt-4">
                <div className="text-sm text-gray-500">
                  显示 {(pagination.current - 1) * pagination.pageSize + 1} 到{' '}
                  {Math.min(pagination.current * pagination.pageSize, totalCount)} 条，共 {totalCount} 条
                </div>
                <div className="flex items-center gap-2">
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setPagination(prev => ({ ...prev, current: prev.current - 1 }))
                    }}
                    disabled={pagination.current <= 1}
                  >
                    上一页
                  </Button>
                  <span className="text-sm px-3 py-1 bg-blue-100 rounded">
                    {pagination.current} / {Math.ceil(totalCount / pagination.pageSize) || 1}
                  </span>
                  <Button
                    variant="outline"
                    size="sm"
                    onClick={() => {
                      setPagination(prev => ({ ...prev, current: prev.current + 1 }))
                    }}
                    disabled={pagination.current >= Math.ceil(totalCount / pagination.pageSize)}
                  >
                    下一页
                  </Button>
        </div>
        </div>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 日志查看对话框 */}
      <Dialog open={logDialogVisible} onOpenChange={setLogDialogVisible}>
        <DialogContent className="max-w-4xl max-h-[80vh]">
          <DialogHeader>
            <DialogTitle className="flex items-center gap-2">
              <Activity className="w-5 h-5" />
              查看日志
            </DialogTitle>
          </DialogHeader>
          <div className="flex flex-col gap-4">
            <div className="flex items-center justify-between">
              <span className="text-sm text-gray-600">实例ID: {currentInstanceId}</span>
              <Button 
                variant="outline" 
                size="sm"
                onClick={() => handleViewLog(currentInstanceId)}
              >
                <RefreshCw className="w-4 h-4 mr-2" />
                刷新日志
              </Button>
            </div>
            <Textarea
              value="日志功能开发中..."
              readOnly
              className="min-h-[400px] font-mono text-sm bg-gray-950 text-green-400 border-gray-700"
              placeholder="暂无日志数据"
            />
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}
