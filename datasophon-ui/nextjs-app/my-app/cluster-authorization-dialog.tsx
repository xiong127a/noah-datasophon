"use client"

import { useState } from "react"
import { Search, X, Check } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"

// 模拟用户数据
const users = [
  { id: 1, username: "admin", email: "admin@noah.com", role: "管理员" },
  { id: 2, username: "noahx", email: "noahx@noah.com", role: "普通用户" },
  { id: 3, username: "zhangsan", email: "zhangsan@noah.com", role: "普通用户" },
  { id: 4, username: "lisi", email: "lisi@noah.com", role: "普通用户" },
  { id: 5, username: "wangwu", email: "wangwu@noah.com", role: "普通用户" },
]

interface ClusterAuthorizationDialogProps {
  open: boolean
  onOpenChange: (open: boolean) => void
  clusterName?: string
}

export default function ClusterAuthorizationDialog({
  open,
  onOpenChange,
  clusterName = "生产环境集群",
}: ClusterAuthorizationDialogProps) {
  const [searchTerm, setSearchTerm] = useState("")
  const [selectedUser, setSelectedUser] = useState<string>("")

  const filteredUsers = users.filter(
    (user) =>
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()),
  )

  const handleConfirm = () => {
    if (selectedUser) {
      console.log(`授权集群 ${clusterName} 给用户: ${selectedUser}`)
      onOpenChange(false)
      setSelectedUser("")
      setSearchTerm("")
    }
  }

  const handleCancel = () => {
    onOpenChange(false)
    setSelectedUser("")
    setSearchTerm("")
  }

  return (
    <Dialog open={open} onOpenChange={onOpenChange}>
      <DialogContent className="rounded-3xl border-0 shadow-2xl max-w-md">
        <DialogHeader className="relative">
          <button
            onClick={handleCancel}
            className="absolute right-0 top-0 w-8 h-8 rounded-full bg-slate-100 hover:bg-slate-200 flex items-center justify-center transition-colors"
          >
            <X className="h-4 w-4 text-slate-600" />
          </button>
          <DialogTitle className="text-xl font-bold text-slate-800 pr-10">授权</DialogTitle>
          <DialogDescription className="text-slate-600">为集群 "{clusterName}" 选择管理员</DialogDescription>
        </DialogHeader>

        <div className="py-4 space-y-4">
          {/* 搜索框 */}
          <div className="space-y-2">
            <label className="text-sm font-medium text-slate-700">集群管理员：</label>
            <div className="relative">
              <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
              <Input
                placeholder="请选择集群管理员"
                value={searchTerm}
                onChange={(e) => setSearchTerm(e.target.value)}
                className="pl-10 rounded-2xl border-slate-200 h-12"
              />
            </div>
          </div>

          {/* 用户列表 */}
          <div className="max-h-48 overflow-y-auto space-y-2">
            {filteredUsers.map((user) => (
              <div
                key={user.id}
                onClick={() => setSelectedUser(user.username)}
                className={`flex items-center justify-between p-3 rounded-2xl cursor-pointer transition-all duration-200 ${
                  selectedUser === user.username
                    ? "bg-blue-50 border-2 border-blue-200"
                    : "bg-slate-50 hover:bg-slate-100 border-2 border-transparent"
                }`}
              >
                <div className="flex items-center space-x-3">
                  <div className="w-8 h-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white text-sm font-medium">
                    {user.username.charAt(0).toUpperCase()}
                  </div>
                  <div>
                    <p className="font-medium text-slate-800">{user.username}</p>
                    <p className="text-xs text-slate-500">{user.email}</p>
                  </div>
                </div>
                {selectedUser === user.username && <Check className="h-5 w-5 text-blue-500" />}
              </div>
            ))}
            {filteredUsers.length === 0 && (
              <div className="text-center py-8 text-slate-500">
                <p>未找到匹配的用户</p>
              </div>
            )}
          </div>
        </div>

        <DialogFooter className="flex space-x-3">
          <Button
            variant="outline"
            onClick={handleCancel}
            className="flex-1 rounded-2xl border-slate-200 bg-transparent"
          >
            取消
          </Button>
          <Button
            onClick={handleConfirm}
            disabled={!selectedUser}
            className="flex-1 rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0"
          >
            确认
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
