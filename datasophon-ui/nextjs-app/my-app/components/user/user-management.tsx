"use client"

import React, { useState, useEffect } from "react"
import { Search, Plus, Edit, Trash2, UserPlus } from "lucide-react"
import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardDescription, CardHeader, CardTitle } from "@/components/ui/card"
import {
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from "@/components/ui/table"
import { Badge } from "@/components/ui/badge"
import { toast } from "sonner"
import { API_PATHS, api } from "@/lib/api-config"
import type { User, UserListParams, UserListResponse } from "@/types/user"
import AddUserDialog from "@/components/user/add-user-dialog"
import DeleteUserDialog from "@/components/user/delete-user-dialog"
import FinalNavbar from "../layout/navbar-final"

export default function UserManagement() {
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [searchTerm, setSearchTerm] = useState("")
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  // 对话框状态
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  const [selectedUser, setSelectedUser] = useState<User | null>(null)

  // 搜索用户
  const handleSearch = () => {
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  // 重置搜索
  const handleReset = () => {
    setSearchTerm("")
    setPagination(prev => ({ ...prev, current: 1 }))
  }

  // 翻页处理
  const handlePageChange = (page: number) => {
    setPagination(prev => ({ ...prev, current: page }))
  }

  // 页面大小改变
  const handlePageSizeChange = (pageSize: number) => {
    setPagination(prev => ({ ...prev, pageSize, current: 1 }))
  }

  // 添加用户
  const handleAddUser = () => {
    setSelectedUser(null)
    setAddDialogOpen(true)
  }

  // 编辑用户
  const handleEditUser = (user: User) => {
    setSelectedUser(user)
    setEditDialogOpen(true)
  }

  // 删除用户
  const handleDeleteUser = (user: User) => {
    setSelectedUser(user)
    setDeleteDialogOpen(true)
  }

  // 强制刷新数据
  const [refreshTrigger, setRefreshTrigger] = useState(0)
  
  // 用户操作成功后刷新列表
  const handleUserOperationSuccess = () => {
    setRefreshTrigger(prev => prev + 1) // 触发数据刷新
    setAddDialogOpen(false)
    setEditDialogOpen(false)
    setDeleteDialogOpen(false)
    setSelectedUser(null)
  }

  // 格式化日期
  const formatDate = (dateString: string) => {
    return new Date(dateString).toLocaleString("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    })
  }

  // 判断用户权限
  const isAdmin = (userType?: number) => userType === 1

  // 获取用户列表
  useEffect(() => {
    const fetchUsers = async () => {
      setLoading(true)
      try {
        const params: UserListParams = {
          page: pagination.current,
          pageSize: pagination.pageSize,
          username: searchTerm || undefined,
        }

        const response = await api.post(API_PATHS.USER_LIST, params)
        
        if (response.data.code === 200) {
          setUsers(response.data.data || [])
          setPagination(prev => ({
            ...prev,
            total: response.data.total || 0,
          }))
        } else {
          toast.error(response.data.message || "获取用户列表失败")
        }
      } catch (error) {
        console.error("获取用户列表失败:", error)
        toast.error("获取用户列表失败，请检查网络连接")
      } finally {
        setLoading(false)
      }
    }

    fetchUsers()
  }, [pagination.current, pagination.pageSize, searchTerm, refreshTrigger])

  return (
    <div>
      <FinalNavbar />
      <div className="space-y-6 p-6">
        {/* 页面头部 */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold tracking-tight">用户管理</h1>
          <p className="text-muted-foreground">
            管理系统用户账户，包括添加、编辑和删除用户
          </p>
        </div>
        <div className="flex items-center space-x-2">
          <Button onClick={handleAddUser} className="gap-2">
            <UserPlus className="h-4 w-4" />
            添加用户
          </Button>
        </div>
      </div>

      {/* 搜索区域 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">搜索用户</CardTitle>
          <CardDescription>
            输入用户名进行搜索
          </CardDescription>
        </CardHeader>
        <CardContent>
          <div className="flex items-center space-x-2">
            <div className="flex-1">
              <Input
                placeholder="请输入用户名..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                onKeyPress={(e) => e.key === "Enter" && handleSearch()}
                className="max-w-sm"
              />
            </div>
            <Button onClick={handleSearch} variant="default" className="gap-2">
              <Search className="h-4 w-4" />
              搜索
            </Button>
            <Button onClick={handleReset} variant="outline">
              重置
            </Button>
          </div>
        </CardContent>
      </Card>

      {/* 用户列表 */}
      <Card>
        <CardHeader>
          <CardTitle className="text-lg">用户列表</CardTitle>
          <CardDescription>
            共 {pagination.total} 个用户
          </CardDescription>
        </CardHeader>
        <CardContent>
          <Table>
            <TableHeader>
              <TableRow>
                <TableHead className="w-[100px]">序号</TableHead>
                <TableHead>用户名</TableHead>
                <TableHead>邮箱</TableHead>
                <TableHead>电话</TableHead>
                <TableHead>用户类型</TableHead>
                <TableHead>创建时间</TableHead>
                <TableHead className="text-right">操作</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8">
                    加载中...
                  </TableCell>
                </TableRow>
              ) : users.length === 0 ? (
                <TableRow>
                  <TableCell colSpan={7} className="text-center py-8 text-muted-foreground">
                    暂无用户数据
                  </TableCell>
                </TableRow>
              ) : (
                users.map((user, index) => (
                  <TableRow key={user.id}>
                    <TableCell className="font-medium">
                      {(pagination.current - 1) * pagination.pageSize + index + 1}
                    </TableCell>
                    <TableCell className="font-medium">{user.username}</TableCell>
                    <TableCell>{user.email}</TableCell>
                    <TableCell>{user.phone}</TableCell>
                    <TableCell>
                      <Badge variant={isAdmin(user.userType) ? "default" : "secondary"}>
                        {isAdmin(user.userType) ? "管理员" : "普通用户"}
                      </Badge>
                    </TableCell>
                    <TableCell>{formatDate(user.createTime)}</TableCell>
                    <TableCell className="text-right">
                      <div className="flex items-center justify-end space-x-2">
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleEditUser(user)}
                          className="h-8 w-8 p-0"
                        >
                          <Edit className="h-4 w-4" />
                        </Button>
                        <Button
                          variant="ghost"
                          size="sm"
                          onClick={() => handleDeleteUser(user)}
                          className="h-8 w-8 p-0 text-destructive hover:text-destructive"
                          disabled={isAdmin(user.userType)}
                        >
                          <Trash2 className="h-4 w-4" />
                        </Button>
                      </div>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>

          {/* 分页控件 */}
          {pagination.total > 0 && (
            <div className="flex items-center justify-between space-x-2 py-4">
              <div className="text-sm text-muted-foreground">
                显示第 {(pagination.current - 1) * pagination.pageSize + 1} 到{" "}
                {Math.min(pagination.current * pagination.pageSize, pagination.total)} 条，
                共 {pagination.total} 条记录
              </div>
              <div className="flex items-center space-x-2">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(pagination.current - 1)}
                  disabled={pagination.current === 1}
                >
                  上一页
                </Button>
                <div className="flex items-center space-x-1">
                  <span className="text-sm">第</span>
                  <span className="text-sm font-medium">{pagination.current}</span>
                  <span className="text-sm">页，共</span>
                  <span className="text-sm font-medium">
                    {Math.ceil(pagination.total / pagination.pageSize)}
                  </span>
                  <span className="text-sm">页</span>
                </div>
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => handlePageChange(pagination.current + 1)}
                  disabled={
                    pagination.current >= Math.ceil(pagination.total / pagination.pageSize)
                  }
                >
                  下一页
                </Button>
              </div>
            </div>
          )}
        </CardContent>
      </Card>

      {/* 添加用户对话框 */}
      <AddUserDialog
        open={addDialogOpen}
        onClose={() => setAddDialogOpen(false)}
        onSuccess={handleUserOperationSuccess}
        mode="add"
      />

      {/* 编辑用户对话框 */}
      <AddUserDialog
        open={editDialogOpen}
        onClose={() => setEditDialogOpen(false)}
        onSuccess={handleUserOperationSuccess}
        mode="edit"
        user={selectedUser}
      />

      {/* 删除用户对话框 */}
      <DeleteUserDialog
        open={deleteDialogOpen}
        onClose={() => setDeleteDialogOpen(false)}
        onSuccess={handleUserOperationSuccess}
        user={selectedUser}
      />
      </div>
    </div>
  )
} 