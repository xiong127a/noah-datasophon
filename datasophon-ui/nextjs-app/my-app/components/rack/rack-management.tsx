"use client"

import { useState, useEffect } from 'react'
import { Plus, Server } from 'lucide-react'
import { Button } from '@/components/ui/button'
import { Card, CardContent, CardHeader, CardTitle } from '@/components/ui/card'
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@/components/ui/table'
import { toast } from 'sonner'
import { Rack } from '../../types/rack'
import AddRackDialog from '@/components/rack/add-rack-dialog'
import DeleteRackDialog from '@/components/rack/delete-rack-dialog'
import FinalNavbar from '../layout/navbar-final'

const RackManagement = () => {
  const [loading, setLoading] = useState(false)
  const [racks, setRacks] = useState<Rack[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedRack, setSelectedRack] = useState<Rack | null>(null)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  const clusterId = typeof window !== 'undefined' 
    ? Number(localStorage.getItem('clusterId') || -1) 
    : -1

  const getRackList = async () => {
    setLoading(true)
    try {
      const params = {
        clusterId: clusterId
      }
      // 这里需要替换为实际的API调用
      // const response = await fetch('/api/racks', {
      //   method: 'POST',
      //   headers: { 'Content-Type': 'application/json' },
      //   body: JSON.stringify(params)
      // })
      // const res = await response.json()
      
      // 暂时使用模拟数据，后续需要使用实际API
      console.log('获取机架列表参数:', params)
      const res = {
        data: [
          { id: 1, rack: 'rack-001', clusterId: clusterId },
          { id: 2, rack: 'rack-002', clusterId: clusterId },
          { id: 3, rack: 'rack-003', clusterId: clusterId }
        ]
      }
      
      setRacks(res.data)
      setPagination(prev => ({
        ...prev,
        total: res.data.length
      }))
    } catch {
      toast.error('获取机架列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleAdd = () => {
    setAddDialogOpen(true)
  }

  const handleDelete = (rack: Rack) => {
    setSelectedRack(rack)
    setDeleteDialogOpen(true)
  }

  const handleAddSuccess = () => {
    setAddDialogOpen(false)
    getRackList()
  }

  const handleDeleteSuccess = () => {
    setDeleteDialogOpen(false)
    setSelectedRack(null)
    getRackList()
  }

  // 计算当前页显示的数据
  const startIndex = (pagination.current - 1) * pagination.pageSize
  const currentPageData = racks.slice(startIndex, startIndex + pagination.pageSize)

  useEffect(() => {
    getRackList()
  }, []) // eslint-disable-line react-hooks/exhaustive-deps

  return (
    <div className="min-h-screen bg-gradient-to-b from-gray-50 to-gray-100">
      <FinalNavbar />
      
      <div className="container mx-auto p-6 space-y-6">
        {/* 页面标题和操作栏 */}
        <div className="flex items-center justify-between">
          <div className="flex items-center space-x-3">
            <div className="p-2 bg-blue-100 rounded-lg">
              <Server className="h-6 w-6 text-blue-600" />
            </div>
            <div>
              <h1 className="text-2xl font-bold text-gray-900">机架管理</h1>
              <p className="text-gray-600">管理集群中的机架配置</p>
            </div>
          </div>
          <Button onClick={handleAdd} className="flex items-center space-x-2">
            <Plus className="h-4 w-4" />
            <span>添加机架</span>
          </Button>
        </div>

        {/* 统计卡片 */}
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <Card>
            <CardHeader className="flex flex-row items-center justify-between space-y-0 pb-2">
              <CardTitle className="text-sm font-medium">总机架数</CardTitle>
              <Server className="h-4 w-4 text-muted-foreground" />
            </CardHeader>
            <CardContent>
              <div className="text-2xl font-bold">{racks.length}</div>
              <p className="text-xs text-muted-foreground">
                当前集群配置的机架总数
              </p>
            </CardContent>
          </Card>
        </div>

        {/* 机架列表表格 */}
        <Card>
          <CardHeader>
            <CardTitle>机架列表</CardTitle>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex items-center justify-center h-32">
                <div className="text-gray-500">加载中...</div>
              </div>
            ) : (
              <div className="space-y-4">
                <Table>
                  <TableHeader>
                    <TableRow>
                      <TableHead className="w-20">序号</TableHead>
                      <TableHead>机架名称</TableHead>
                      <TableHead className="w-32">操作</TableHead>
                    </TableRow>
                  </TableHeader>
                  <TableBody>
                    {currentPageData.length === 0 ? (
                      <TableRow>
                        <TableCell colSpan={3} className="text-center py-8 text-gray-500">
                          暂无机架数据
                        </TableCell>
                      </TableRow>
                    ) : (
                      currentPageData.map((rack, index) => (
                        <TableRow key={rack.id}>
                          <TableCell className="font-medium">
                            {startIndex + index + 1}
                          </TableCell>
                          <TableCell>
                            <div className="flex items-center space-x-2">
                              <Server className="h-4 w-4 text-gray-400" />
                              <span>{rack.rack}</span>
                            </div>
                          </TableCell>
                          <TableCell>
                            <Button
                              variant="destructive"
                              size="sm"
                              onClick={() => handleDelete(rack)}
                            >
                              删除
                            </Button>
                          </TableCell>
                        </TableRow>
                      ))
                    )}
                  </TableBody>
                </Table>

                {/* 分页信息 */}
                {racks.length > pagination.pageSize && (
                  <div className="flex items-center justify-between text-sm text-gray-600">
                    <div>
                      共 {racks.length} 条记录
                    </div>
                    <div>
                      第 {pagination.current} 页
                    </div>
                  </div>
                )}
              </div>
            )}
          </CardContent>
        </Card>

        {/* 对话框 */}
        <AddRackDialog
          open={addDialogOpen}
          onCancel={() => setAddDialogOpen(false)}
          onSuccess={handleAddSuccess}
          clusterId={clusterId}
        />

        <DeleteRackDialog
          open={deleteDialogOpen}
          onCancel={() => setDeleteDialogOpen(false)}
          onSuccess={handleDeleteSuccess}
          rack={selectedRack}
        />
      </div>
    </div>
  )
}

export default RackManagement