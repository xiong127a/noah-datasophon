"use client"

import React, { useState, useEffect } from "react"
import { Search, Plus, Edit, Trash2, UserPlus, Users, Crown, Clock, FileText } from "lucide-react"
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
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { toast } from "sonner"
import { API_PATHS, api } from "@/lib/api"
import type { User, UserListResponse } from "@/types/user"
import { UserType } from "@/types/user"
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
  const isAdmin = (userType?: number) => userType === UserType.ADMIN
  
  // 格式化上次登录时间
  const formatLastLoginTime = (time?: string) => {
    if (!time) return "从未登录"
    return new Intl.DateTimeFormat("zh-CN", {
      year: "numeric",
      month: "2-digit",
      day: "2-digit",
      hour: "2-digit",
      minute: "2-digit",
    }).format(new Date(time))
  }

  // 获取用户列表
  useEffect(() => {
    const fetchUsers = async () => {
      setLoading(true)
      try {
        // 构建查询参数
        const params = new URLSearchParams({
          page: pagination.current.toString(),
          pageSize: pagination.pageSize.toString(),
        })
        
        // 如果有搜索词，添加username参数
        if (searchTerm) {
          params.append('username', searchTerm)
        }

        const response = await api.get(`${API_PATHS.USER_LIST}?${params.toString()}`)
        
        if (response.data.code === 200) {
          // 数据应该在根级别的data和total字段中
          const userData = response.data.data || []
          const userTotal = response.data.total || 0
          
          setUsers(userData)
          setPagination(prev => ({
            ...prev,
            total: userTotal,
          }))
        } else {
          toast.error(response.data.msg || "获取用户列表失败")
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
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50/30 to-indigo-50/50 relative overflow-hidden">
      <FinalNavbar />
      
      {/* 背景装饰 */}
      <div className="absolute top-0 left-0 w-96 h-96 bg-gradient-to-br from-blue-400/10 to-indigo-400/10 rounded-full blur-3xl transform -translate-x-48 -translate-y-48" />
      <div className="absolute bottom-0 right-0 w-80 h-80 bg-gradient-to-br from-purple-400/10 to-pink-400/10 rounded-full blur-3xl transform translate-x-40 translate-y-40" />

      {/* 页面头部 - 仿照集群管理的设计 */}
      <div className="relative overflow-hidden bg-white/80 backdrop-blur-xl border-b border-slate-200/50 shadow-lg">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50/80 via-white/90 to-purple-50/80" />
        <div className="relative w-full px-4 sm:px-6 lg:px-8 xl:px-12 py-12">
          <div className="flex flex-col lg:flex-row lg:items-center lg:justify-between space-y-6 lg:space-y-0">
            <div className="space-y-2">
              <h1 className="text-3xl lg:text-4xl font-bold bg-gradient-to-r from-slate-800 via-slate-700 to-slate-600 bg-clip-text text-transparent">
                用户管理中心
              </h1>
              <p className="text-base lg:text-lg text-slate-600">统一管理系统用户账户和权限设置</p>
              <div className="flex items-center space-x-2 pt-2">
                <div className="w-2 h-2 bg-green-400 rounded-full animate-pulse" />
                <span className="text-sm text-slate-500">实时管理 • 权限控制 • 安全保障</span>
              </div>
            </div>
            
            <div className="flex items-center justify-center lg:justify-end">
              <div className="bg-white/90 backdrop-blur-sm rounded-3xl p-4 lg:p-6 shadow-xl border border-white/50">
                <div className="flex flex-col sm:flex-row items-center space-y-3 sm:space-y-0 sm:space-x-4">
                  <Badge className="px-4 sm:px-6 py-2 sm:py-3 rounded-2xl border-blue-200 text-blue-700 bg-blue-50/80 text-base lg:text-lg font-semibold w-full sm:w-auto text-center">
                    <Users className="h-4 lg:h-5 w-4 lg:w-5 mr-2 lg:mr-3 text-blue-600" />
                    总用户: {pagination.total}
                  </Badge>
                  <Badge className="px-4 sm:px-6 py-2 sm:py-3 rounded-2xl border-amber-200 text-amber-700 bg-amber-50/80 text-base lg:text-lg font-semibold w-full sm:w-auto text-center">
                    <Crown className="h-4 lg:h-5 w-4 lg:w-5 mr-2 lg:mr-3 text-amber-600" />
                    管理员: {users.filter(u => isAdmin(u.userType)).length}
                  </Badge>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* 搜索栏 */}
      <div className="w-full px-4 sm:px-6 lg:px-8 xl:px-12 py-6">
        <Card className="border-0 shadow-xl bg-white/90 backdrop-blur-xl rounded-3xl w-full max-w-none">
          <CardHeader>
            <CardTitle className="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
              搜索用户
            </CardTitle>
            <CardDescription>
              根据用户名快速查找系统用户
            </CardDescription>
          </CardHeader>
          <CardContent>
            <div className="flex flex-col sm:flex-row gap-4">
              <div className="flex-1">
                <Input
                  placeholder="请输入用户名..."
                  value={searchTerm}
                  onChange={(e) => setSearchTerm(e.target.value)}
                  onKeyPress={(e) => e.key === "Enter" && handleSearch()}
                  className="h-12 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm focus:border-blue-400 focus:ring-2 focus:ring-blue-100"
                />
              </div>
              <div className="flex gap-4">
                <Button 
                  onClick={handleSearch} 
                  className="h-12 px-6 rounded-2xl bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white border-0 shadow-lg hover:shadow-xl transition-all duration-300 flex-1 sm:flex-none"
                >
                  <Search className="h-4 w-4 mr-2" />
                  搜索
                </Button>
                <Button 
                  variant="outline" 
                  onClick={handleReset}
                  className="h-12 px-6 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm hover:bg-white flex-1 sm:flex-none"
                >
                  重置
                </Button>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>

      {/* 用户列表 */}
      <div className="w-full px-4 sm:px-6 lg:px-8 xl:px-12 pb-8">
        <Card className="border-0 shadow-xl bg-white/90 backdrop-blur-xl rounded-3xl w-full max-w-none">
          <CardHeader className="pb-6">
            <div className="flex items-center justify-between">
              <div>
                <CardTitle className="text-xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                  用户列表
                </CardTitle>
                <CardDescription className="mt-2">
                  共 {pagination.total} 个用户，其中管理员 {users.filter(u => isAdmin(u.userType)).length} 个
                </CardDescription>
              </div>
              <Button 
                onClick={handleAddUser} 
                className="h-12 px-6 rounded-2xl bg-gradient-to-r from-indigo-500 via-purple-600 to-pink-500 hover:from-indigo-600 hover:via-purple-700 hover:to-pink-600 text-white border-0 shadow-lg hover:shadow-xl transition-all duration-300 group"
              >
                <UserPlus className="h-4 w-4 mr-2 group-hover:scale-110 transition-transform duration-300" />
                添加用户
              </Button>
            </div>
          </CardHeader>
          <CardContent>
            {loading ? (
              <div className="flex items-center justify-center py-16">
                <div className="text-center">
                  <div className="animate-spin h-10 w-10 border-4 rounded-full border-blue-600 border-t-transparent mx-auto mb-4"></div>
                  <p className="text-lg text-slate-600 font-medium">正在加载用户数据...</p>
                </div>
              </div>
            ) : (
              <>
                <div className="rounded-2xl border border-slate-200/50 bg-white/50 backdrop-blur-sm overflow-hidden">
                  <div className="overflow-x-auto">
                    <Table className="min-w-[1200px]">
                      <TableHeader className="bg-gradient-to-r from-slate-50 to-slate-100/80">
                        <TableRow className="border-slate-200/50 hover:bg-slate-50/80">
                          <TableHead className="font-semibold text-slate-700 w-[180px]">用户信息</TableHead>
                          <TableHead className="font-semibold text-slate-700 w-[250px]">邮箱</TableHead>
                          <TableHead className="font-semibold text-slate-700 w-[140px]">手机号</TableHead>
                          <TableHead className="font-semibold text-slate-700 w-[120px]">用户类型</TableHead>
                          <TableHead className="font-semibold text-slate-700 w-[180px]">上次登录</TableHead>
                          <TableHead className="font-semibold text-slate-700 w-[160px]">创建时间</TableHead>
                          <TableHead className="text-right font-semibold text-slate-700 w-[120px]">操作</TableHead>
                        </TableRow>
                      </TableHeader>
                      <TableBody>
                        {users.length === 0 ? (
                          <TableRow>
                            <TableCell colSpan={7} className="text-center py-12 text-slate-500">
                              <div className="flex flex-col items-center space-y-3">
                                <Users className="h-12 w-12 text-slate-300" />
                                <p className="text-lg">暂无用户数据</p>
                                <Button 
                                  onClick={handleAddUser}
                                  variant="outline"
                                  className="rounded-2xl border-slate-200/50 bg-white/80"
                                >
                                  <UserPlus className="h-4 w-4 mr-2" />
                                  添加第一个用户
                                </Button>
                              </div>
                            </TableCell>
                          </TableRow>
                        ) : (
                          users.map((user, index) => (
                            <TableRow 
                              key={user.id} 
                              className="border-slate-200/50 hover:bg-slate-50/50 transition-colors duration-200"
                            >
                              {/* 用户信息（头像+用户名） */}
                              <TableCell className="font-medium">
                                <div className="flex items-center space-x-3">
                                  <Avatar className="h-10 w-10 ring-2 ring-slate-200">
                                    <AvatarImage 
                                      src={user.avatar} 
                                      alt={user.username}
                                    />
                                    <AvatarFallback className={`text-sm font-semibold text-white ${
                                      isAdmin(user.userType) 
                                        ? "bg-gradient-to-br from-amber-500 to-orange-600" 
                                        : "bg-gradient-to-br from-blue-500 to-purple-600"
                                    }`}>
                                      {user.username.charAt(0).toUpperCase()}
                                    </AvatarFallback>
                                  </Avatar>
                                  <div>
                                    <div className="text-slate-900 font-medium">{user.username}</div>
                                    <div className="text-xs text-slate-500">ID: {user.id}</div>
                                  </div>
                                </div>
                              </TableCell>
                              
                              {/* 邮箱 */}
                              <TableCell className="text-slate-700">{user.email || "-"}</TableCell>
                              
                              {/* 手机号 */}
                              <TableCell className="text-slate-700">{user.phone || "-"}</TableCell>
                              
                              {/* 用户类型 */}
                              <TableCell>
                                <Badge 
                                  className={
                                    isAdmin(user.userType) 
                                      ? "bg-amber-100 text-amber-700 border-amber-200 border-0 rounded-full px-3 py-1 font-medium shadow-sm" 
                                      : "bg-blue-100 text-blue-700 border-blue-200 border-0 rounded-full px-3 py-1 font-medium shadow-sm"
                                  }
                                >
                                  {isAdmin(user.userType) ? "管理员" : "普通用户"}
                                </Badge>
                              </TableCell>
                              
                              
                              {/* 上次登录时间 */}
                              <TableCell className="text-slate-700">
                                <div className="flex items-center space-x-1">
                                  <Clock className="h-3 w-3 text-slate-400" />
                                  <span className="text-sm">{formatLastLoginTime(user.previousLoginTime)}</span>
                                </div>
                              </TableCell>
                              
                              {/* 创建时间 */}
                              <TableCell className="text-slate-700 text-sm">{formatDate(user.createTime)}</TableCell>
                              <TableCell className="text-right">
                                <div className="flex items-center justify-end space-x-2">
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleEditUser(user)}
                                    className="group relative h-8 w-8 p-0 rounded-xl border-blue-200/60 bg-gradient-to-br from-blue-50 to-indigo-50 hover:from-blue-100 hover:to-indigo-100 hover:border-blue-300 hover:shadow-lg hover:shadow-blue-200/50 hover:scale-110 active:scale-95 transition-all duration-300"
                                  >
                                    <Edit className="h-4 w-4 text-blue-500 group-hover:text-blue-600 group-hover:scale-110 group-hover:-rotate-12 transition-all duration-300" />
                                    {/* 悬停时的装饰效果 */}
                                    <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-blue-400/20 to-indigo-400/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300 animate-pulse" />
                                  </Button>
                                  <Button
                                    variant="outline"
                                    size="sm"
                                    onClick={() => handleDeleteUser(user)}
                                    className={`group relative h-8 w-8 p-0 rounded-xl transition-all duration-300 ${
                                      isAdmin(user.userType)
                                        ? "border-slate-200/50 bg-slate-100/50 cursor-not-allowed opacity-50"
                                        : "border-red-200/60 bg-gradient-to-br from-red-50 to-pink-50 hover:from-red-100 hover:to-pink-100 hover:border-red-300 hover:shadow-lg hover:shadow-red-200/50 hover:scale-110 active:scale-95"
                                    }`}
                                    disabled={isAdmin(user.userType)}
                                  >
                                    <Trash2 className={`h-4 w-4 transition-all duration-300 ${
                                      isAdmin(user.userType)
                                        ? "text-slate-400"
                                        : "text-red-500 group-hover:text-red-600 group-hover:scale-110 group-hover:rotate-12"
                                    }`} />
                                    {/* 悬停时的装饰效果 */}
                                    {!isAdmin(user.userType) && (
                                      <div className="absolute inset-0 rounded-xl bg-gradient-to-br from-red-400/20 to-pink-400/20 opacity-0 group-hover:opacity-100 transition-opacity duration-300 animate-pulse" />
                                    )}
                                  </Button>
                                </div>
                              </TableCell>
                            </TableRow>
                          ))
                        )}
                      </TableBody>
                    </Table>
                  </div>
                </div>
              </>
            )}
          </CardContent>
        </Card>
      </div>

      {/* 分页组件 - 现代化设计 */}
      {!loading && pagination.total > pagination.pageSize && (
        <div className="w-full px-4 sm:px-6 lg:px-8 xl:px-12 pb-8">
          <div className="flex justify-center">
            <Card className="border-0 shadow-xl bg-white/90 backdrop-blur-xl rounded-3xl">
              <CardContent className="p-6">
                <div className="flex items-center space-x-6">
                  <div className="text-sm text-slate-600">
                    显示第 {(pagination.current - 1) * pagination.pageSize + 1} 到{" "}
                    {Math.min(pagination.current * pagination.pageSize, pagination.total)} 条，
                    共 {pagination.total} 条记录
                  </div>
                  <div className="flex items-center space-x-2">
                    <Button
                      variant="outline"
                      onClick={() => handlePageChange(pagination.current - 1)}
                      disabled={pagination.current <= 1}
                      className="h-10 px-4 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm hover:bg-white disabled:opacity-50"
                    >
                      上一页
                    </Button>
                    <span className="px-4 py-2 rounded-2xl bg-gradient-to-r from-blue-500 to-indigo-600 text-white font-medium min-w-[120px] text-center">
                      第 {pagination.current} / {Math.ceil(pagination.total / pagination.pageSize)} 页
                    </span>
                    <Button
                      variant="outline"
                      onClick={() => handlePageChange(pagination.current + 1)}
                      disabled={pagination.current >= Math.ceil(pagination.total / pagination.pageSize)}
                      className="h-10 px-4 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm hover:bg-white disabled:opacity-50"
                    >
                      下一页
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      )}

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
  )
} 