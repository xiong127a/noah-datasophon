"use client"

import { useState, useEffect } from 'react'
import { Table, Button, Card, message, Row, Col } from 'antd'
import type { ColumnsType, TablePaginationConfig } from 'antd/es/table'
import { Rack } from '../../types/rack'
import AddRackDialog from './add-rack-dialog'
import DeleteRackDialog from './delete-rack-dialog'

const RackManagement = () => {
  const [loading, setLoading] = useState(false)
  const [dataSource, setDataSource] = useState<Rack[]>([])
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedRack, setSelectedRack] = useState<Rack | null>(null)
  const [pagination, setPagination] = useState<TablePaginationConfig>({
    total: 0,
    pageSize: 10,
    current: 1,
    showSizeChanger: true,
    pageSizeOptions: ['10', '20', '50', '100'],
    showTotal: (total) => `共 ${total} 条`,
  })

  const clusterId = typeof window !== 'undefined' 
    ? Number(localStorage.getItem('clusterId') || -1) 
    : -1

  const columns: ColumnsType<Rack> = [
    {
      title: '序号',
      key: 'index',
      width: 70,
      render: (_, __, index) => {
        const currentPage = pagination.current || 1
        const pageSize = pagination.pageSize || 10
        return currentPage === 1 
          ? index + 1 
          : index + 1 + pageSize * (currentPage - 1)
      }
    },
    {
      title: '机架名称',
      key: 'rack',
      dataIndex: 'rack'
    },
    {
      title: '操作',
      key: 'action',
      width: 180,
      render: (_, record) => (
        <span className="flex-container">
          <a 
            className="btn-opt text-red-500 hover:text-red-700" 
            onClick={() => handleDelete(record)}
          >
            删除
          </a>
        </span>
      )
    }
  ]

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
      
      // 暂时使用模拟数据
      const res = {
        data: [
          { id: 1, rack: 'rack-001', clusterId: clusterId },
          { id: 2, rack: 'rack-002', clusterId: clusterId }
        ]
      }
      
      setDataSource(res.data)
      setPagination(prev => ({
        ...prev,
        total: res.data.length
      }))
    } catch (error) {
      message.error('获取机架列表失败')
    } finally {
      setLoading(false)
    }
  }

  const handleTableChange = (paginationInfo: TablePaginationConfig) => {
    setPagination(prev => ({
      ...prev,
      current: paginationInfo.current,
      pageSize: paginationInfo.pageSize
    }))
    getRackList()
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

  useEffect(() => {
    getRackList()
  }, [])

  return (
    <div className="frame-list bg-gray-50 min-h-screen p-4">
      <Row justify="end" align="middle" className="mb-4">
        <Col>
          <Button 
            type="primary" 
            onClick={handleAdd}
          >
            添加机架
          </Button>
        </Col>
      </Row>
      
      <Card className="shadow-sm">
        <div className="table-info">
          <Table
            columns={columns}
            loading={loading}
            dataSource={dataSource}
            rowKey="id"
            pagination={pagination}
            onChange={handleTableChange}
          />
        </div>
      </Card>

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
  )
}

export default RackManagement