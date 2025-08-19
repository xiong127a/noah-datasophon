"use client"

import React, { useState, useEffect } from "react"
import { Button } from "../ui/button"
import { Card, CardContent, CardHeader, CardTitle } from "../ui/card"
import { Input } from "../ui/input"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "../ui/table"
import { Badge } from "../ui/badge"
import { Trash2, Plus, Search, Loader2 } from "lucide-react"
import { Tag, TagListResponse } from "../../types/tag"
import AddTagDialog from "./add-tag-dialog"
import DeleteTagDialog from "./delete-tag-dialog"

export default function TagManagement() {
  const [tags, setTags] = useState<Tag[]>([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [currentPage, setCurrentPage] = useState(1)
  const [pageSize, setPageSize] = useState(10)
  const [total, setTotal] = useState(0)
  const [showAddDialog, setShowAddDialog] = useState(false)
  const [showDeleteDialog, setShowDeleteDialog] = useState(false)
  const [selectedTag, setSelectedTag] = useState<Tag | null>(null)

  // 🔧 修复：避免Number()转换导致20位长整型精度丢失，使用字符串
  // 🔧 修复：集群配置阶段应从组件props获取clusterId，而不是localStorage
  const clusterId = typeof window !== 'undefined' ? 
    (localStorage.getItem("clusterId") || "-1") : "-1"

  // 获取标签列表
  const fetchTags = async () => {
    setLoading(true)
    try {
      const response = await fetch('/api/cluster/node/label/list', {
        method: 'POST',
        headers: {
          'Content-Type': 'application/json',
        },
        body: JSON.stringify({
          clusterId: clusterId
        }),
      })
      
      const result: TagListResponse = await response.json()
      
      if (result.code === 200) {
        setTags(result.data)
        setTotal(result.data.length)
      }
    } catch (error) {
      console.error('获取标签列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 过滤标签
  const filteredTags = tags.filter(tag =>
    tag.nodeLabel.toLowerCase().includes(searchTerm.toLowerCase())
  )

  // 分页数据
  const paginatedTags = filteredTags.slice(
    (currentPage - 1) * pageSize,
    currentPage * pageSize
  )

  // 处理添加标签
  const handleAddTag = () => {
    setSelectedTag(null)
    setShowAddDialog(true)
  }

  // 处理删除标签
  const handleDeleteTag = (tag: Tag) => {
    setSelectedTag(tag)
    setShowDeleteDialog(true)
  }

  // 标签操作成功后刷新列表
  const handleOperationSuccess = () => {
    fetchTags()
    setShowAddDialog(false)
    setShowDeleteDialog(false)
    setSelectedTag(null)
  }

  // 页面变化处理
  const handlePageChange = (page: number) => {
    setCurrentPage(page)
  }

  useEffect(() => {
    fetchTags()
  }, [clusterId])

  return (
    <div className="space-y-6">
      {/* 页面标题和操作区 */}
      <div className="flex justify-between items-center">
        <h1 className="text-2xl font-bold text-gray-900">标签管理</h1>
        <Button onClick={handleAddTag} className="bg-blue-600 hover:bg-blue-700">
          <Plus className="w-4 h-4 mr-2" />
          添加标签
        </Button>
      </div>

      {/* 搜索区域 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">搜索标签</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="flex items-center space-x-4">
            <div className="relative flex-1 max-w-sm">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 text-gray-400 w-4 h-4" />
              <Input
                placeholder="搜索标签名称..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10"
              />
            </div>
            <Badge variant="secondary">
              共 {filteredTags.length} 个标签
            </Badge>
          </div>
        </CardContent>
      </Card>

      {/* 标签列表 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">标签列表</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="flex justify-center items-center py-8">
              <Loader2 className="w-6 h-6 animate-spin text-blue-600" />
              <span className="ml-2 text-gray-600">加载中...</span>
            </div>
          ) : (
            <>
              <Table>
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-20">序号</TableHead>
                    <TableHead>标签名称</TableHead>
                    <TableHead className="w-32">操作</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {paginatedTags.map((tag, index) => (
                    <TableRow key={tag.id}>
                      <TableCell className="font-medium">
                        {(currentPage - 1) * pageSize + index + 1}
                      </TableCell>
                      <TableCell>
                        <Badge variant="outline" className="bg-blue-50 text-blue-700 border-blue-200">
                          {tag.nodeLabel}
                        </Badge>
                      </TableCell>
                      <TableCell>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteTag(tag)}
                          className="text-red-600 hover:text-red-700 hover:bg-red-50"
                        >
                          <Trash2 className="w-4 h-4" />
                        </Button>
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>

              {/* 分页 */}
              {filteredTags.length > pageSize && (
                <div className="flex justify-center items-center space-x-4 mt-6">
                  <Button
                    variant="outline"
                    onClick={() => handlePageChange(currentPage - 1)}
                    disabled={currentPage === 1}
                  >
                    上一页
                  </Button>
                  <span className="text-sm text-gray-600">
                    第 {currentPage} 页，共 {Math.ceil(filteredTags.length / pageSize)} 页
                  </span>
                  <Button
                    variant="outline"
                    onClick={() => handlePageChange(currentPage + 1)}
                    disabled={currentPage >= Math.ceil(filteredTags.length / pageSize)}
                  >
                    下一页
                  </Button>
                </div>
              )}

              {paginatedTags.length === 0 && !loading && (
                <div className="text-center py-8 text-gray-500">
                  {searchTerm ? `没有找到包含 "${searchTerm}" 的标签` : '暂无标签数据'}
                </div>
              )}
            </>
          )}
        </CardContent>
      </Card>

      {/* 对话框 */}
      <AddTagDialog
        open={showAddDialog}
        onClose={() => setShowAddDialog(false)}
        onSuccess={handleOperationSuccess}
        clusterId={clusterId}
      />

      <DeleteTagDialog
        open={showDeleteDialog}
        onClose={() => setShowDeleteDialog(false)}
        onSuccess={handleOperationSuccess}
        tag={selectedTag}
      />
    </div>
  )
}