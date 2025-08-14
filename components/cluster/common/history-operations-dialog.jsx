'use client'

import React, { useState, useEffect, useCallback } from 'react'
import { Button } from '../../ui/button'
import { History, Search, RefreshCw, Clock, CheckCircle, XCircle, Loader, AlertCircle } from 'lucide-react'

// 简化版的API调用（需要根据实际项目结构调整）
const API_BASE_URL = 'http://localhost:8081/ddh'

const createClusterHeaders = (clusterId) => ({
  'X-Api-Version': 'v1',
  'X-Cluster-Id': clusterId,
  'Content-Type': 'application/json'
})

const apiCall = async (url, options = {}) => {
  const response = await fetch(`${API_BASE_URL}${url}`, {
    ...options,
    headers: {
      ...options.headers,
    }
  })
  return response.json()
}

// 状态颜色映射
const getStateColor = (state) => {
  switch (state) {
    case 0: return 'default' // 待运行
    case 1: return 'warning' // 正在运行
    case 2: return 'success' // 成功
    case 3: return 'destructive' // 失败
    case 4: return 'secondary' // 取消
    default: return 'default'
  }
}

// 状态图标映射
const getStateIcon = (state) => {
  switch (state) {
    case 0: return <Clock className="h-3 w-3" />
    case 1: return <Loader className="h-3 w-3 animate-spin" />
    case 2: return <CheckCircle className="h-3 w-3" />
    case 3: return <XCircle className="h-3 w-3" />
    case 4: return <AlertCircle className="h-3 w-3" />
    default: return <Clock className="h-3 w-3" />
  }
}

export default function HistoryOperationsDialog({ clusterId, children }) {
  console.log('HistoryOperationsDialog 渲染，收到的clusterId:', clusterId);
  
  const [open, setOpen] = useState(false)
  console.log('当前 open 状态:', open);
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState([])
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0
  })
  const [searchText, setSearchText] = useState('')
  const [stateFilter, setStateFilter] = useState('all')
  const [typeFilter, setTypeFilter] = useState('all')
  
  // 临时测试：如果没有clusterId，使用一个测试值
  const effectiveClusterId = clusterId || '313499261352448000'
  
  // 获取历史操作列表
  const fetchHistoryData = useCallback(async () => {
    if (!effectiveClusterId) {
      console.warn('No clusterId provided for history operations')
      return
    }

    console.log('开始获取历史数据，clusterId:', effectiveClusterId);
    setLoading(true)
    try {
      const headers = createClusterHeaders(effectiveClusterId)
      const params = new URLSearchParams({
        page: pagination.current.toString(),
        pageSize: pagination.pageSize.toString()
      })
      
      const response = await apiCall(
        `/api/v1/cluster/service/command/list?${params}`,
        { headers }
      )
      
      if (response?.code === 200) {
        const responseData = response.data || {}
        setData(responseData.records || [])
        setPagination(prev => ({
          ...prev,
          total: parseInt(responseData.total) || 0
        }))
      } else {
        console.error('Failed to fetch history data:', response?.message)
      }
    } catch (error) {
      console.error('Error fetching history data:', error)
    } finally {
      setLoading(false)
    }
  }, [effectiveClusterId, pagination.current, pagination.pageSize])

  // 处理分页变化
  const handlePageChange = (page) => {
    setPagination(prev => ({ ...prev, current: page }))
  }

  // 刷新数据
  const handleRefresh = () => {
    fetchHistoryData()
  }

  // 过滤数据
  const filteredData = data.filter(record => {
    const matchesSearch = !searchText || 
      record.commandName.toLowerCase().includes(searchText.toLowerCase()) ||
      (record.serviceName && record.serviceName.toLowerCase().includes(searchText.toLowerCase()))
    
    const matchesState = stateFilter === 'all' || record.commandState.toString() === stateFilter
    const matchesType = typeFilter === 'all' || record.commandType.toString() === typeFilter
    
    return matchesSearch && matchesState && matchesType
  })

  // 对话框打开时获取数据
  useEffect(() => {
    if (open && effectiveClusterId) {
      fetchHistoryData()
    }
  }, [open, effectiveClusterId, fetchHistoryData])

  return (
    <>
      {/* 调试信息 */}
      <div style={{ 
        position: 'fixed', 
        top: '10px', 
        right: '10px', 
        background: 'yellow', 
        padding: '5px', 
        fontSize: '12px',
        zIndex: 9999,
        border: '1px solid black'
      }}>
        调试: open={open.toString()}, clusterId={clusterId || 'null'}
      </div>
      
      {/* 触发按钮 */}
      {children || (
        <button
          className="h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-slate-100 inline-flex items-center justify-center"
          onClick={(e) => {
            e.preventDefault();
            e.stopPropagation();
            console.log('=== 历史操作按钮被点击 ===');
            console.log('clusterId:', clusterId);
            console.log('effectiveClusterId:', effectiveClusterId);
            console.log('当前open状态:', open);
            setOpen(true);
            console.log('设置open为true后');
          }}
          onMouseEnter={() => console.log('鼠标进入历史操作按钮')}
          onMouseLeave={() => console.log('鼠标离开历史操作按钮')}
        >
          <History className="h-5 w-5 text-slate-600" />
          <span className="sr-only">历史操作</span>
        </button>
      )}

      {/* 模态框 */}
      {open && (
        <div 
          className="fixed inset-0 z-50 flex items-center justify-center"
          style={{ backgroundColor: 'rgba(0, 0, 0, 0.5)' }}
          onClick={(e) => {
            console.log('模态框背景被点击');
            if (e.target === e.currentTarget) {
              setOpen(false);
            }
          }}
        >
          {console.log('=== 模态框正在渲染 ===', { open, effectiveClusterId })}
          {/* 背景遮罩 */}
          <div 
            className="fixed inset-0 bg-black bg-opacity-50"
            onClick={() => {
              console.log('背景遮罩被点击，关闭模态框');
              setOpen(false);
            }}
          />
          
          {/* 对话框内容 */}
          <div className="relative bg-white rounded-lg shadow-xl max-w-6xl w-full max-h-[80vh] m-4 flex flex-col">
            {/* 标题栏 */}
            <div className="px-6 py-4 border-b flex items-center justify-between">
              <div className="flex items-center gap-2">
                <History className="h-5 w-5" />
                <h2 className="text-lg font-semibold">历史操作记录</h2>
                {effectiveClusterId && (
                  <span className="ml-2 px-2 py-1 bg-gray-100 text-sm rounded border">
                    集群 {effectiveClusterId}
                  </span>
                )}
              </div>
              <button
                onClick={() => setOpen(false)}
                className="text-gray-400 hover:text-gray-600 p-1"
              >
                <XCircle className="h-5 w-5" />
              </button>
            </div>
            
            {/* 内容区域 */}
            <div className="p-6 flex-1 overflow-hidden">
              {/* 工具栏 */}
              <div className="flex items-center justify-between gap-4 mb-6">
                <div className="flex items-center gap-3">
                  {/* 搜索 */}
                  <div className="relative">
                    <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-gray-400" />
                    <input
                      type="text"
                      placeholder="搜索命令名称或服务名称..."
                      value={searchText}
                      onChange={(e) => setSearchText(e.target.value)}
                      className="pl-10 w-64 px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                    />
                  </div>
                  
                  {/* 状态过滤 */}
                  <select 
                    value={stateFilter} 
                    onChange={(e) => setStateFilter(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="all">全部状态</option>
                    <option value="0">待运行</option>
                    <option value="1">正在运行</option>
                    <option value="2">成功</option>
                    <option value="3">失败</option>
                    <option value="4">取消</option>
                  </select>
                  
                  {/* 类型过滤 */}
                  <select 
                    value={typeFilter} 
                    onChange={(e) => setTypeFilter(e.target.value)}
                    className="px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-blue-500"
                  >
                    <option value="all">全部类型</option>
                    <option value="1">安装服务</option>
                    <option value="2">启动服务</option>
                    <option value="3">停止服务</option>
                    <option value="4">重启服务</option>
                    <option value="5">卸载服务</option>
                    <option value="6">配置服务</option>
                  </select>
                </div>
                
                <Button
                  variant="outline"
                  size="sm"
                  onClick={handleRefresh}
                  disabled={loading}
                  className="gap-2"
                >
                  <RefreshCw className={`h-4 w-4 ${loading ? 'animate-spin' : ''}`} />
                  刷新
                </Button>
              </div>

              {/* 内容区域 */}
              <div className="h-[500px] overflow-y-auto">
                {loading ? (
                  <div className="flex items-center justify-center h-32">
                    <Loader className="h-6 w-6 animate-spin mr-2" />
                    <span>加载中...</span>
                  </div>
                ) : filteredData.length === 0 ? (
                  <div className="flex flex-col items-center justify-center h-32 text-gray-500">
                    <History className="h-12 w-12 mb-2 opacity-50" />
                    <p>暂无历史操作记录</p>
                  </div>
                ) : (
                  <div className="grid gap-3">
                    {filteredData.map((record, index) => (
                      <div key={`${record.commandId || index}-${record.createTime}`} className="p-4 border border-gray-200 rounded-lg hover:shadow-md transition-shadow bg-white">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center gap-3">
                            <span className={`inline-flex items-center gap-1 px-2 py-1 text-sm rounded-full ${
                              record.commandState === 0 ? 'bg-gray-100 text-gray-800' :
                              record.commandState === 1 ? 'bg-yellow-100 text-yellow-800' :
                              record.commandState === 2 ? 'bg-green-100 text-green-800' :
                              record.commandState === 3 ? 'bg-red-100 text-red-800' :
                              record.commandState === 4 ? 'bg-gray-100 text-gray-800' :
                              'bg-gray-100 text-gray-800'
                            }`}>
                              {getStateIcon(record.commandState)}
                              {record.commandStateText}
                            </span>
                            <div>
                              <h4 className="font-medium">{record.commandName}</h4>
                              <p className="text-sm text-gray-500">
                                {record.commandTypeText}
                                {record.serviceName && ` · ${record.serviceName}`}
                              </p>
                            </div>
                          </div>
                          
                          <div className="text-right text-sm text-gray-500">
                            <p>{record.createTimeFormatted}</p>
                            <p>耗时: {record.durationTime}</p>
                            {record.commandProgress && (
                              <p>进度: {record.commandProgress}%</p>
                            )}
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </div>

              {/* 分页 */}
              {pagination.total > 0 && (
                <div className="flex items-center justify-between mt-4 pt-4 border-t">
                  <div className="text-sm text-gray-500">
                    共 {pagination.total} 条记录
                  </div>
                  <div className="flex items-center gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handlePageChange(pagination.current - 1)}
                      disabled={pagination.current <= 1}
                    >
                      上一页
                    </Button>
                    <span className="text-sm">
                      第 {pagination.current} 页，共 {Math.ceil(pagination.total / pagination.pageSize)} 页
                    </span>
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => handlePageChange(pagination.current + 1)}
                      disabled={pagination.current >= Math.ceil(pagination.total / pagination.pageSize)}
                    >
                      下一页
                    </Button>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      )}
    </>
  )
}
