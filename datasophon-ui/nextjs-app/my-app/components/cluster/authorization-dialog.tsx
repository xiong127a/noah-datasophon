"use client"

import { DialogFooter } from "@/components/ui/dialog"

import { useState, useEffect } from "react"
import { 
  Search, 
  X, 
  Check, 
  Users, 
  Shield, 
  Sparkles,
  UserCheck,
  LoaderIcon
} from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Dialog, DialogContent, DialogDescription, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { DIALOG_STYLES } from "./common/shared-styles"
import { Badge } from "@/components/ui/badge"
import { apiClient, API_PATHS } from "@/lib/api"

// 用户数据类型定义
interface User {
  id: number
  username: string
  email: string
  role: string
  roleIcon: any
  roleColor: string
  avatarColor: string
}

// 生成用户头像颜色的工具函数
const getUserAvatarColor = (index: number) => {
  const colors = [
    "from-blue-500 to-blue-600",
    "from-green-500 to-green-600", 
    "from-purple-500 to-purple-600",
    "from-pink-500 to-pink-600",
    "from-indigo-500 to-indigo-600",
    "from-amber-500 to-amber-600",
    "from-teal-500 to-teal-600",
    "from-orange-500 to-orange-600"
  ]
  return colors[index % colors.length]
}

interface ClusterAuthorizationDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  clusterName?: string
  clusterId?: string | number
}

export default function ClusterAuthorizationDialogSuper({
  open,
  onOpenChange,
  clusterName = "生产环境集群",
  clusterId,
}: ClusterAuthorizationDialogProps) {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedUsers, setSelectedUsers] = useState<number[]>([])
  const [users, setUsers] = useState<User[]>([])
  const [loading, setLoading] = useState(false)
  const [saving, setSaving] = useState(false)

  // 获取用户列表
  const fetchUsers = async () => {
    setLoading(true)
    try {
      // 使用 queryAllUser API，就像Vue2项目中一样
      const response = await apiClient.post(API_PATHS.USER_ALL, {})
      
      if (response.data && response.data.code === 200) {
        const userData = response.data.data.map((user: any, index: number) => ({
          id: user.id,
          username: user.username,
          email: user.email || '',
          role: user.role || '普通用户',
          avatarColor: getUserAvatarColor(index)
        }))
        setUsers(userData)
      } else {
        console.error('获取用户列表失败:', response.data?.msg)
      }
    } catch (error) {
      console.error('获取用户列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  // 当对话框打开时获取用户列表
  useEffect(() => {
    if (open) {
      fetchUsers()
      setSelectedUsers([])
      setSearchTerm("")
    }
  }, [open, clusterId])

  const filteredUsers = users.filter((user: User) => {
    const matchesSearch = 
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase())
    return matchesSearch
  })

  const handleUserToggle = (userId: number) => {
    setSelectedUsers((prev) => 
      prev.includes(userId) 
        ? prev.filter((u) => u !== userId) 
        : [...prev, userId]
    )
  }

  const handleConfirm = async () => {
    if (selectedUsers.length > 0) {
      setSaving(true)
      try {
        // 使用GET请求，就像Vue2项目中一样，通过URL参数传递数据
        const clusterIdValue = Number(clusterId || localStorage.getItem("clusterId") || -1)
        const userIdsString = selectedUsers.join(',')
        const url = `${API_PATHS.CLUSTER_AUTH}?clusterId=${clusterIdValue}&userIds=${userIdsString}`
        
        const response = await apiClient.get(url)
        
        if (response.data && response.data.code === 200) {
          console.log(`授权集群 ${clusterName} 给用户成功`)
          onOpenChange(false)
          setSelectedUsers([])
          setSearchTerm("")
        } else {
          console.error('集群授权失败:', response.data?.msg)
          alert(response.data?.msg || '集群授权失败')
        }
      } catch (error) {
        console.error('集群授权失败:', error)
        alert('集群授权失败，请稍后重试')
      } finally {
        setSaving(false)
      }
    }
  }

  const handleCancel = () => {
    onOpenChange(false)
    setSelectedUsers([])
    setSearchTerm("")
  }

  return (
    <Dialog open={open} onOpenChange={() => {}}>
      <DialogContent
        className={`${DIALOG_STYLES.content} max-w-lg bg-white/95 backdrop-blur-xl overflow-hidden [&>button]:hidden`}
        onPointerDownOutside={(e) => e.preventDefault()}
        onEscapeKeyDown={(e) => e.preventDefault()}
      >
        {/* 背景装饰 - 框架化样式 */}
        <div className="absolute inset-0 bg-gradient-to-br from-blue-50/80 via-purple-50/50 to-pink-50/80" />
        <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-blue-400/20 to-purple-400/20 rounded-full blur-3xl transform translate-x-16 -translate-y-16" />
        <div className="absolute bottom-0 left-0 w-24 h-24 bg-gradient-to-tr from-pink-400/20 to-amber-400/20 rounded-full blur-2xl transform -translate-x-12 translate-y-12" />

        <DialogHeader className="relative z-10">
          <Button
            onClick={handleCancel}
            variant="ghost"
            size="sm"
            className="absolute right-0 top-0 w-10 h-10 rounded-full bg-white/90 hover:bg-white shadow-lg hover:shadow-xl border border-white/50 z-20 group"
          >
            <X className="h-4 w-4 text-gray-600 group-hover:text-gray-700 transition-colors" />
          </Button>
          
          <div className="flex items-center space-x-3 pr-12">
            <div className="w-12 h-12 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center shadow-xl">
              <Shield className="h-6 w-6 text-white" />
            </div>
            <div>
              <DialogTitle className="text-2xl font-bold bg-gradient-to-r from-gray-800 to-gray-600 bg-clip-text text-transparent">
                集群授权
              </DialogTitle>
              <DialogDescription className="text-slate-600 mt-1">
                为集群 <span className="font-semibold text-blue-600">&quot;{clusterName}&quot;</span> 分配管理权限
              </DialogDescription>
            </div>
          </div>
        </DialogHeader>

        <div className="py-6 space-y-6 relative z-10">
          {/* 搜索和筛选区域 */}
          <div className="space-y-4">
            {/* 搜索框 */}
            <div className="relative">
              <Search className="absolute left-4 top-1/2 transform -translate-y-1/2 h-5 w-5 text-slate-400" />
              <Input
                placeholder="搜索用户名或邮箱..."
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-12 pr-4 h-12 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm shadow-sm hover:shadow-md transition-all duration-300 focus:bg-white focus:shadow-lg"
              />
              <div className="absolute right-3 top-1/2 transform -translate-y-1/2">
                <Sparkles className="h-4 w-4 text-blue-400 animate-pulse" />
              </div>
            </div>


          </div>

          {/* 已选择用户展示 */}
          {selectedUsers.length > 0 && (
            <div className="bg-gradient-to-r from-blue-50 to-purple-50 rounded-2xl p-4 border border-blue-200/50 shadow-sm">
              <div className="flex items-center space-x-2 mb-2">
                <UserCheck className="h-4 w-4 text-blue-600" />
                <span className="text-sm font-semibold text-blue-700">
                  已选择 {selectedUsers.length} 位管理员
                </span>
              </div>
              <div className="flex flex-wrap gap-2">
                {selectedUsers.map((userId) => {
                  const user = users.find(u => u.id === userId)
                  return (
                    <Badge
                      key={userId}
                      className="bg-blue-500 text-white hover:bg-blue-600 transition-colors"
                    >
                      {user?.username || `用户${userId}`}
                      <button
                        onClick={() => handleUserToggle(userId)}
                        className="ml-1 hover:bg-blue-600 rounded-full p-0.5"
                      >
                        <X className="h-3 w-3" />
                      </button>
                    </Badge>
                  )
                })}
              </div>
            </div>
          )}

          {/* 用户列表 */}
          <div className="max-h-64 overflow-y-auto space-y-3 pr-2 scrollbar-thin scrollbar-thumb-slate-300 scrollbar-track-transparent">
            {loading ? (
              <div className="text-center py-8">
                <LoaderIcon className="h-8 w-8 mx-auto mb-3 text-blue-500 animate-spin" />
                <p className="text-slate-500">正在加载用户列表...</p>
              </div>
            ) : filteredUsers.map((user, index) => {
              const isSelected = selectedUsers.includes(user.id)
              
              return (
                <div
                  key={user.id}
                  onClick={() => handleUserToggle(user.id)}
                  style={{ animationDelay: `${index * 50}ms` }}
                  className={`flex items-center justify-between p-4 rounded-2xl cursor-pointer transition-all duration-300 animate-fade-in group relative overflow-hidden ${
                    isSelected
                      ? "bg-gradient-to-r from-blue-50 to-purple-50 border-2 border-blue-300 shadow-lg scale-105"
                      : "bg-white/80 hover:bg-white border-2 border-transparent hover:shadow-lg hover:scale-102"
                  }`}
                >
                  {/* 选中时的背景光效 */}
                  {isSelected && (
                    <div className="absolute inset-0 bg-gradient-to-r from-blue-400/10 to-purple-400/10 rounded-2xl" />
                  )}
                  
                  <div className="flex items-center space-x-4 relative z-10">
                    {/* 用户头像 */}
                    <div className="relative">
                      <div className={`w-12 h-12 rounded-full bg-gradient-to-br ${user.avatarColor} flex items-center justify-center text-white text-lg font-bold shadow-lg group-hover:scale-110 transition-transform duration-300`}>
                        {user.username.charAt(0).toUpperCase()}
                      </div>
                      {/* 用户图标 */}
                      <div className="absolute -bottom-1 -right-1 w-6 h-6 rounded-full bg-gradient-to-br from-slate-500 to-slate-600 flex items-center justify-center shadow-lg">
                        <Users className="h-3 w-3 text-white" />
                      </div>
                    </div>
                    
                    {/* 用户信息 */}
                    <div className="space-y-1">
                      <div className="flex items-center space-x-2">
                        <p className="font-semibold text-slate-800 group-hover:text-slate-900 transition-colors">
                          {user.username}
                        </p>
                        {user.role && (
                          <Badge 
                            variant="outline" 
                            className="text-xs bg-gradient-to-r from-slate-500 to-slate-600 text-white border-0 shadow-sm"
                          >
                            {user.role}
                          </Badge>
                        )}
                      </div>
                      <p className="text-sm text-slate-500">{user.email}</p>
                    </div>
                  </div>
                  
                  {/* 选中状态指示 */}
                  <div className="relative z-10">
                    {isSelected ? (
                      <div className="w-8 h-8 rounded-full bg-gradient-to-r from-blue-500 to-purple-600 flex items-center justify-center shadow-lg animate-pulse">
                        <Check className="h-5 w-5 text-white" />
                      </div>
                    ) : (
                      <div className="w-8 h-8 rounded-full border-2 border-slate-300 group-hover:border-blue-400 transition-colors" />
                    )}
                  </div>
                </div>
              )
            })}
            
            {/* 空状态 */}
            {filteredUsers.length === 0 && (
              <div className="text-center py-12 text-slate-500">
                <Users className="h-12 w-12 mx-auto mb-3 text-slate-300" />
                <p className="text-lg font-medium">未找到匹配的用户</p>
                <p className="text-sm">请尝试调整搜索条件或筛选器</p>
              </div>
            )}
          </div>
        </div>

        <DialogFooter className="relative z-10">
                      <Button
              onClick={handleConfirm}
              disabled={selectedUsers.length === 0 || saving}
              className={`w-full h-12 rounded-2xl text-white border-0 font-semibold transition-all duration-300 relative overflow-hidden ${
                selectedUsers.length > 0 && !saving
                  ? "bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 shadow-lg hover:shadow-xl hover:scale-105"
                  : "bg-slate-300 cursor-not-allowed"
              }`}
            >
              {/* 按钮光效 */}
              {selectedUsers.length > 0 && !saving && (
                <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/25 to-white/0 translate-x-[-100%] hover:translate-x-[100%] transition-transform duration-1000" />
              )}
              <span className="relative z-10 flex items-center justify-center">
                {saving ? (
                  <>
                    <LoaderIcon className="mr-2 h-4 w-4 animate-spin" />
                    授权中...
                  </>
                ) : (
                  <>
                    <Shield className="mr-2 h-4 w-4" />
                    确认授权 {selectedUsers.length > 0 && `(${selectedUsers.length})`}
                  </>
                )}
              </span>
            </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
} 