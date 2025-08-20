'use client'

import React, { useState, useEffect, useCallback } from 'react'

/**
 * 🔧 修复Long类型精度丢失的JSON解析器
 * 将超过JavaScript安全整数范围的数字保持为字符串
 */
function parseJsonWithLongSupport(jsonString: string): unknown {
  try {
    // 使用正则表达式找到可能导致精度丢失的长整数
    // JavaScript安全整数范围：-(2^53-1) 到 2^53-1
    const safeMaxInt = Number.MAX_SAFE_INTEGER; // 9007199254740991
    
    // 匹配超长数字的正则表达式（15位以上的整数）
    const longIntRegex = /"?(-?\d{15,})"?/g;
    
    // 将超长整数用引号包裹，确保解析为字符串
    const processedJson = jsonString.replace(longIntRegex, (match, number) => {
      const num = Math.abs(parseInt(number));
      if (num > safeMaxInt) {
        return `"${number}"`; // 强制转为字符串
      }
      return match; // 保持原样
    });
    
    return JSON.parse(processedJson);
  } catch (error) {
    console.warn('JSON解析失败，使用原生解析:', error);
    return JSON.parse(jsonString); // 回退到原生解析
  }
}
import { Card } from '../ui/card'
import { Button } from '../ui/button'
import { Input } from '../ui/input'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '../ui/select'
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from '../ui/table'
import { Badge } from '../ui/badge'
import { Search, Filter, RefreshCw } from 'lucide-react'
import { OperationLog, AuditLogFilters, AuditLogListResponse } from '../../types/audit'

export default function AuditLogManagement() {
  const [loading, setLoading] = useState(false)
  const [data, setData] = useState<OperationLog[]>([])
  const [moduleList, setModuleList] = useState<string[]>([])
  const [serviceNameList, setServiceNameList] = useState<string[]>([])
  const [filters, setFilters] = useState<AuditLogFilters>({})
  const [pagination, setPagination] = useState({
    current: 1,
    size: 10,
    total: 0
  })

  // 获取操作模块列表
  const fetchModuleList = async () => {
    try {
      // 🔧 修复：使用自定义JSON解析器防止Long类型精度丢失
      const response = await fetch('/ddh/api/log/moduleList')
      const responseText = await response.text()
      const result = parseJsonWithLongSupport(responseText) as { success: boolean; data: string[] }
      if (result.success) {
        setModuleList(result.data)
      }
    } catch (error) {
      console.error('获取模块列表失败:', error)
    }
  }

  // 获取服务名称列表
  const fetchServiceNameList = async () => {
    try {
      // 🔧 修复：避免精度丢失，使用字符串形式的clusterId
      const clusterId = localStorage.getItem('clusterId') || "-1"
      const response = await fetch(`/ddh/api/log/serviceNameList?clusterId=${clusterId}`)
      const responseText = await response.text()
      const result = parseJsonWithLongSupport(responseText) as { success: boolean; data: string[] }
      if (result.success) {
        setServiceNameList(result.data)
      }
    } catch (error) {
      console.error('获取服务名称列表失败:', error)
    }
  }

  // 获取日志列表
  const fetchLogList = useCallback(async (page?: number, pageSize?: number) => {
    const currentPage = page ?? pagination.current;
    const currentPageSize = pageSize ?? pagination.size;
    setLoading(true)
    try {
      const response = await fetch('/ddh/api/log/list', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json'
        },
        body: JSON.stringify({
          current: currentPage,
          size: currentPageSize,
          param: filters
        })
      })
      const responseText = await response.text()
      const result = parseJsonWithLongSupport(responseText) as { success: boolean; data: AuditLogListResponse }
      if (result.success) {
        const responseData: AuditLogListResponse = result.data
        setData(responseData.records)
        setPagination({
          current: responseData.current,
          size: responseData.size,
          total: responseData.total
        })
      }
    } catch (error) {
      console.error('获取日志列表失败:', error)
    } finally {
      setLoading(false)
    }
  }, [filters, pagination])

  // 搜索
  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }))
    fetchLogList(1, pagination.size)
  }

  // 重置筛选
  const handleReset = () => {
    setFilters({})
    setPagination(prev => ({ ...prev, current: 1 }))
    fetchLogList(1, pagination.size)
  }

  // 分页改变
  const handlePageChange = (page: number, pageSize?: number) => {
    const newSize = pageSize || pagination.size
    setPagination(prev => ({ ...prev, current: page, size: newSize }))
    fetchLogList(page, newSize)
  }

  // 获取状态徽章
  const getStatusBadge = (returnCode: number) => {
    if (returnCode === 200) {
      return <Badge variant="secondary" className="bg-green-100 text-green-800">成功</Badge>
    } else {
      return <Badge variant="destructive">失败</Badge>
    }
  }

  useEffect(() => {
    fetchModuleList()
    fetchServiceNameList()
    fetchLogList()
  }, [fetchLogList])

  return (
    <div className="p-6 space-y-6">
      <div className="flex items-center justify-between">
        <h1 className="text-2xl font-bold text-gray-900">日志审计</h1>
        <Button onClick={() => fetchLogList()} disabled={loading}>
          <RefreshCw className={`h-4 w-4 mr-2 ${loading ? 'animate-spin' : ''}`} />
          刷新
        </Button>
      </div>

      {/* 筛选卡片 */}
      <Card className="p-6">
        <div className="grid grid-cols-1 md:grid-cols-4 gap-4 items-end">
          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700">操作模块</label>
            <Select
              value={filters.operationModule || ''}
              onValueChange={(value) => setFilters(prev => ({ ...prev, operationModule: value || undefined }))}
            >
              <SelectTrigger>
                <SelectValue placeholder="选择操作模块" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">全部</SelectItem>
                {moduleList.map((module) => (
                  <SelectItem key={module} value={module}>
                    {module}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700">服务名称</label>
            <Select
              value={filters.serviceName || ''}
              onValueChange={(value) => setFilters(prev => ({ ...prev, serviceName: value || undefined }))}
            >
              <SelectTrigger>
                <SelectValue placeholder="选择服务名称" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="">全部</SelectItem>
                {serviceNameList.map((service) => (
                  <SelectItem key={service} value={service}>
                    {service}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="space-y-2">
            <label className="text-sm font-medium text-gray-700">操作用户</label>
            <Input
              placeholder="输入用户名"
              value={filters.operateUser || ''}
              onChange={(e) => setFilters(prev => ({ ...prev, operateUser: e.target.value || undefined }))}
            />
          </div>

          <div className="flex space-x-2">
            <Button onClick={handleSearch} className="flex-1">
              <Search className="h-4 w-4 mr-2" />
              搜索
            </Button>
            <Button variant="outline" onClick={handleReset}>
              <Filter className="h-4 w-4 mr-2" />
              重置
            </Button>
          </div>
        </div>
      </Card>

      {/* 表格卡片 */}
      <Card>
        <div className="p-6">
          <div className="rounded-md border">
            <Table>
              <TableHeader>
                <TableRow>
                  <TableHead className="w-16">序号</TableHead>
                  <TableHead>操作模块</TableHead>
                  <TableHead>操作类型</TableHead>
                  <TableHead>开始时间</TableHead>
                  <TableHead>结束时间</TableHead>
                  <TableHead>用户</TableHead>
                  <TableHead>服务名称</TableHead>
                  <TableHead>操作结果</TableHead>
                  <TableHead>状态</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {loading ? (
                  <TableRow>
                    <TableCell colSpan={9} className="text-center py-8">
                      <div className="flex items-center justify-center">
                        <RefreshCw className="h-4 w-4 animate-spin mr-2" />
                        加载中...
                      </div>
                    </TableCell>
                  </TableRow>
                ) : data.length === 0 ? (
                  <TableRow>
                    <TableCell colSpan={9} className="text-center py-8 text-gray-500">
                      暂无数据
                    </TableCell>
                  </TableRow>
                ) : (
                  data.map((log, index) => (
                    <TableRow key={log.id}>
                      <TableCell className="font-mono text-sm">
                        {(pagination.current - 1) * pagination.size + index + 1}
                      </TableCell>
                      <TableCell>{log.operationModule}</TableCell>
                      <TableCell>{log.operationType}</TableCell>
                      <TableCell className="font-mono text-sm">{log.startTime}</TableCell>
                      <TableCell className="font-mono text-sm">{log.endTime}</TableCell>
                      <TableCell>{log.operateUser}</TableCell>
                      <TableCell>{log.serviceName || '-'}</TableCell>
                      <TableCell className="max-w-xs truncate" title={log.returnMsg}>
                        {log.returnMsg}
                      </TableCell>
                      <TableCell>{getStatusBadge(log.returnCode)}</TableCell>
                    </TableRow>
                  ))
                )}
              </TableBody>
            </Table>
          </div>

          {/* 分页 */}
          {!loading && data.length > 0 && (
            <div className="flex items-center justify-between px-4 py-4">
              <div className="text-sm text-gray-500">
                共 {pagination.total} 条记录，每页 {pagination.size} 条
              </div>
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pagination.current <= 1}
                  onClick={() => handlePageChange(pagination.current - 1)}
                >
                  上一页
                </Button>
                <span className="text-sm px-3">
                  第 {pagination.current} 页，共 {Math.ceil(pagination.total / pagination.size)} 页
                </span>
                <Button
                  variant="outline"
                  size="sm"
                  disabled={pagination.current >= Math.ceil(pagination.total / pagination.size)}
                  onClick={() => handlePageChange(pagination.current + 1)}
                >
                  下一页
                </Button>
              </div>
            </div>
          )}
        </div>
      </Card>
    </div>
  )
}