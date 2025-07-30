"use client"

import React, { useState } from "react"
import { AlertTriangle, Trash2 } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import { Alert, AlertDescription } from "@/components/ui/alert"
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
      <DialogContent className="sm:max-w-[425px]">
        <DialogHeader>
          <DialogTitle className="flex items-center gap-2 text-destructive">
            <AlertTriangle className="h-5 w-5" />
            确认删除用户
          </DialogTitle>
          <DialogDescription>
            此操作将永久删除用户账户，且无法撤销。
          </DialogDescription>
        </DialogHeader>

        <div className="space-y-4">
          <Alert className="border-orange-200 bg-orange-50">
            <AlertTriangle className="h-4 w-4 text-orange-600" />
            <AlertDescription className="text-orange-800">
              您即将删除用户{" "}
              <span className="font-semibold text-orange-900">
                {user.username}
              </span>
              。此操作不可撤销，请确认您要继续。
            </AlertDescription>
          </Alert>

          <div className="rounded-lg bg-gray-50 p-4 space-y-2">
            <div className="text-sm">
              <span className="font-medium text-gray-700">用户信息：</span>
            </div>
            <div className="grid grid-cols-2 gap-2 text-sm">
              <div>
                <span className="text-gray-500">用户名：</span>
                <span className="font-medium">{user.username}</span>
              </div>
              <div>
                <span className="text-gray-500">邮箱：</span>
                <span className="font-medium">{user.email}</span>
              </div>
              <div>
                <span className="text-gray-500">电话：</span>
                <span className="font-medium">{user.phone}</span>
              </div>
              <div>
                <span className="text-gray-500">创建时间：</span>
                <span className="font-medium">
                  {new Date(user.createTime).toLocaleDateString("zh-CN")}
                </span>
              </div>
            </div>
          </div>
        </div>

        <DialogFooter className="gap-2">
          <Button
            type="button"
            variant="outline"
            onClick={handleCancel}
            disabled={loading}
          >
            取消
          </Button>
          <Button
            type="button"
            variant="destructive"
            onClick={handleDelete}
            disabled={loading}
            className="gap-2"
          >
            {loading ? (
              <>
                <div className="h-4 w-4 animate-spin rounded-full border-2 border-current border-t-transparent" />
                删除中...
              </>
            ) : (
              <>
                <Trash2 className="h-4 w-4" />
                确认删除
              </>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export default DeleteUserDialog 