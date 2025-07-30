"use client"

import React, { useState } from "react"
import { AlertTriangle, Trash2, User as UserIcon, Mail, Phone, Calendar, X, Shield } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { toast } from "sonner"
import { API_PATHS, api } from "@/lib/api-config"
import type { User } from "@/types/user"

interface DeleteUserDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  user: User | null
}

function DeleteUserDialog({
  open,
  onClose,
  onSuccess,
  user,
}: DeleteUserDialogProps) {
  const [loading, setLoading] = useState(false)

  // 删除用户
  const handleDelete = async () => {
    if (!user) return

    setLoading(true)
    try {
      const response = await api.post(API_PATHS.USER_DELETE, [user.id])

      if (response.data.code === 200) {
        toast.success("用户删除成功")
        onSuccess()
      } else {
        toast.error(response.data.message || "删除用户失败")
      }
    } catch (error) {
      console.error("删除用户失败:", error)
      toast.error("删除用户失败，请检查网络连接")
    } finally {
      setLoading(false)
    }
  }

  // 取消删除
  const handleCancel = () => {
    onClose()
  }

  if (!user) return null

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="w-[95vw] sm:max-w-[500px] max-h-[85vh] border-0 shadow-2xl bg-white/95 backdrop-blur-xl rounded-3xl overflow-hidden mx-auto my-4">
        {/* 装饰性背景 */}
        <div className="absolute inset-0 overflow-hidden">
          <div className="absolute -top-20 -right-20 w-40 h-40 bg-gradient-to-br from-red-400/20 to-pink-400/20 rounded-full blur-3xl animate-pulse" />
          <div className="absolute -bottom-20 -left-20 w-40 h-40 bg-gradient-to-tr from-orange-400/20 to-red-400/20 rounded-full blur-3xl animate-pulse" />
        </div>

        {/* 主要内容 */}
        <div className="relative z-10 p-6">
          {/* 标题区域 */}
          <div className="text-center mb-8">
            <div className="relative mb-6">
              <div className="flex items-center justify-center w-20 h-20 mx-auto mb-4 bg-gradient-to-br from-red-500 to-pink-600 rounded-full shadow-lg animate-pulse">
                <AlertTriangle className="h-10 w-10 text-white" />
              </div>
              <div className="absolute inset-0 w-20 h-20 mx-auto bg-gradient-to-br from-red-500/30 to-pink-600/30 rounded-full blur-xl animate-ping" />
            </div>
            <h2 className="text-2xl font-bold bg-gradient-to-r from-red-600 to-pink-600 bg-clip-text text-transparent mb-2">
              删除用户确认
            </h2>
            <p className="text-slate-600 text-sm leading-relaxed">
              此操作将永久删除用户账户和相关数据，<br />
              <span className="font-medium text-red-600">此操作不可撤销</span>
            </p>
          </div>

          {/* 用户信息卡片 */}
          <div className="bg-gradient-to-br from-slate-50 to-slate-100/50 rounded-2xl p-6 mb-6 border border-slate-200/50">
            <div className="flex items-start space-x-4">
              {/* 用户头像 */}
              <div className="relative">
                <Avatar className="w-16 h-16 ring-4 ring-red-100 ring-offset-2 ring-offset-white">
                  <AvatarImage src={user.avatar} alt={user.username} />
                  <AvatarFallback className="bg-gradient-to-br from-red-500 to-pink-600 text-white font-bold text-lg">
                    {user.username?.charAt(0).toUpperCase()}
                  </AvatarFallback>
                </Avatar>
                {user.userType === 1 && (
                  <div className="absolute -top-1 -right-1 w-6 h-6 bg-gradient-to-br from-amber-400 to-orange-500 rounded-full flex items-center justify-center shadow-lg">
                    <Shield className="h-3 w-3 text-white" />
                  </div>
                )}
              </div>
              
              {/* 用户详细信息 */}
              <div className="flex-1 space-y-3">
                <div className="flex items-center space-x-2">
                  <UserIcon className="h-4 w-4 text-slate-500" />
                  <span className="text-sm text-slate-500">用户名</span>
                  <span className="font-semibold text-slate-800">{user.username}</span>
                  {user.userType === 1 && (
                    <span className="px-2 py-1 text-xs bg-amber-100 text-amber-700 rounded-full font-medium">
                      管理员
                    </span>
                  )}
                </div>
                
                <div className="flex items-center space-x-2">
                  <Mail className="h-4 w-4 text-slate-500" />
                  <span className="text-sm text-slate-500">邮箱</span>
                  <span className="font-medium text-slate-700">{user.email || "暂无"}</span>
                </div>
                
                <div className="flex items-center space-x-2">
                  <Phone className="h-4 w-4 text-slate-500" />
                  <span className="text-sm text-slate-500">手机</span>
                  <span className="font-medium text-slate-700">{user.phone || "暂无"}</span>
                </div>
                
                <div className="flex items-center space-x-2">
                  <Calendar className="h-4 w-4 text-slate-500" />
                  <span className="text-sm text-slate-500">创建时间</span>
                  <span className="font-medium text-slate-700">
                    {new Date(user.createTime).toLocaleDateString("zh-CN")}
                  </span>
                </div>
              </div>
            </div>
          </div>

          {/* 警告提示 */}
          <div className="bg-gradient-to-r from-red-50 to-pink-50 border-2 border-red-200/50 rounded-2xl p-4 mb-6">
            <div className="flex items-start space-x-3">
              <div className="flex-shrink-0 w-8 h-8 bg-red-100 rounded-full flex items-center justify-center">
                <AlertTriangle className="h-4 w-4 text-red-600" />
              </div>
              <div className="flex-1">
                <h4 className="font-semibold text-red-800 mb-1">危险操作警告</h4>
                <p className="text-sm text-red-700 leading-relaxed">
                  删除用户 <span className="font-bold text-red-800">"{user.username}"</span> 将会：
                </p>
                <ul className="mt-2 text-sm text-red-600 space-y-1">
                  <li className="flex items-center space-x-2">
                    <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                    <span>永久删除用户账户和个人资料</span>
                  </li>
                  <li className="flex items-center space-x-2">
                    <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                    <span>清除所有相关权限和设置</span>
                  </li>
                  <li className="flex items-center space-x-2">
                    <div className="w-1.5 h-1.5 bg-red-500 rounded-full" />
                    <span>此操作无法撤销或恢复</span>
                  </li>
                </ul>
              </div>
            </div>
          </div>

          {/* 操作按钮 */}
          <div className="flex items-center justify-end space-x-4">
            <Button
              type="button"
              variant="outline"
              onClick={handleCancel}
              disabled={loading}
              className="h-12 px-6 rounded-2xl border-slate-200/50 bg-white/80 hover:bg-slate-50 transition-all duration-300 font-medium"
            >
              <X className="h-4 w-4 mr-2" />
              取消
            </Button>
            <Button
              type="button"
              onClick={handleDelete}
              disabled={loading}
              className="h-12 px-6 rounded-2xl bg-gradient-to-r from-red-500 to-pink-600 hover:from-red-600 hover:to-pink-700 text-white border-0 shadow-lg hover:shadow-xl transition-all duration-300 font-medium flex items-center space-x-2"
            >
              {loading ? (
                <>
                  <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                  <span>删除中...</span>
                </>
              ) : (
                <>
                  <Trash2 className="h-4 w-4" />
                  <span>确认删除</span>
                </>
              )}
            </Button>
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default DeleteUserDialog 