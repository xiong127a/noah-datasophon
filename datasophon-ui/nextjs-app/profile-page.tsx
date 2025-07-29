"use client"

import { useState } from "react"
import { Camera, User, Mail, Phone, Calendar, Edit, Save, X } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import FinalNavbar from "./noah-navbar-final"

// 内置头像选项
const builtInAvatars = [
  "/placeholder.svg?height=80&width=80&text=👨‍💼",
  "/placeholder.svg?height=80&width=80&text=👩‍💼",
  "/placeholder.svg?height=80&width=80&text=👨‍💻",
  "/placeholder.svg?height=80&width=80&text=👩‍💻",
  "/placeholder.svg?height=80&width=80&text=🧑‍🔬",
  "/placeholder.svg?height=80&width=80&text=👨‍🎓",
  "/placeholder.svg?height=80&width=80&text=👩‍🎓",
  "/placeholder.svg?height=80&width=80&text=🧑‍🚀",
]

const AvatarSelector = ({ currentAvatar, onSelect }: { currentAvatar: string; onSelect: (avatar: string) => void }) => {
  const [open, setOpen] = useState(false)

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button
          variant="outline"
          size="sm"
          className="absolute bottom-0 right-0 w-10 h-10 rounded-full bg-white shadow-lg border-2 border-white hover:bg-slate-50"
        >
          <Camera className="h-4 w-4" />
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-3xl border-0 shadow-2xl max-w-md">
        <DialogHeader>
          <DialogTitle>选择头像</DialogTitle>
          <DialogDescription>选择一个内置头像或上传自定义头像</DialogDescription>
        </DialogHeader>
        <div className="py-4">
          <div className="grid grid-cols-4 gap-4 mb-6">
            {builtInAvatars.map((avatar, index) => (
              <button
                key={index}
                onClick={() => {
                  onSelect(avatar)
                  setOpen(false)
                }}
                className={`w-16 h-16 rounded-2xl overflow-hidden border-2 transition-all duration-200 hover:scale-110 ${
                  currentAvatar === avatar ? "border-blue-500 ring-2 ring-blue-200" : "border-slate-200"
                }`}
              >
                <img
                  src={avatar || "/placeholder.svg"}
                  alt={`头像 ${index + 1}`}
                  className="w-full h-full object-cover"
                />
              </button>
            ))}
          </div>
          <div className="border-t pt-4">
            <Label htmlFor="avatar-upload" className="text-sm font-medium text-slate-700 mb-2 block">
              或上传自定义头像
            </Label>
            <Input
              id="avatar-upload"
              type="file"
              accept="image/*"
              className="rounded-2xl border-slate-200"
              onChange={(e) => {
                const file = e.target.files?.[0]
                if (file) {
                  const reader = new FileReader()
                  reader.onload = (e) => {
                    const result = e.target?.result as string
                    onSelect(result)
                    setOpen(false)
                  }
                  reader.readAsDataURL(file)
                }
              }}
            />
          </div>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default function ProfilePage() {
  const [isEditing, setIsEditing] = useState(false)
  const [userInfo, setUserInfo] = useState({
    username: "admin",
    email: "xxx@163.com",
    phone: "1865xxx",
    avatar: "/placeholder.svg?height=120&width=120&text=Admin",
    bio: "Noah大数据基础平台管理员",
    joinDate: "2022-05-10",
    lastLogin: "2025-01-29 14:30:25",
  })

  const [editForm, setEditForm] = useState(userInfo)

  const handleSave = () => {
    setUserInfo(editForm)
    setIsEditing(false)
  }

  const handleCancel = () => {
    setEditForm(userInfo)
    setIsEditing(false)
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      <FinalNavbar />

      {/* 页面头部 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-blue-50/50 via-white to-purple-50/50" />
        <div className="relative max-w-4xl mx-auto px-8 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-2">
                个人信息
              </h1>
              <p className="text-slate-600 text-lg">管理您的账户信息和偏好设置</p>
            </div>
            <div className="flex space-x-3">
              {isEditing ? (
                <>
                  <Button onClick={handleCancel} variant="outline" className="rounded-2xl bg-transparent">
                    <X className="mr-2 h-4 w-4" />
                    取消
                  </Button>
                  <Button onClick={handleSave} className="rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600">
                    <Save className="mr-2 h-4 w-4" />
                    保存
                  </Button>
                </>
              ) : (
                <Button
                  onClick={() => setIsEditing(true)}
                  className="rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600"
                >
                  <Edit className="mr-2 h-4 w-4" />
                  编辑信息
                </Button>
              )}
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容 */}
      <div className="max-w-4xl mx-auto px-8 py-12">
        <div className="grid grid-cols-1 lg:grid-cols-3 gap-8">
          {/* 左侧头像卡片 */}
          <div className="lg:col-span-1">
            <Card className="rounded-3xl border-0 shadow-lg bg-white overflow-hidden">
              <CardContent className="p-8 text-center">
                <div className="relative inline-block mb-6">
                  <Avatar className="w-32 h-32 ring-4 ring-slate-100 ring-offset-4">
                    <AvatarImage src={isEditing ? editForm.avatar : userInfo.avatar} alt="用户头像" />
                    <AvatarFallback className="text-2xl bg-gradient-to-br from-blue-500 to-purple-600 text-white font-bold">
                      {userInfo.username.charAt(0).toUpperCase()}
                    </AvatarFallback>
                  </Avatar>
                  {isEditing && (
                    <AvatarSelector
                      currentAvatar={editForm.avatar}
                      onSelect={(avatar) => setEditForm({ ...editForm, avatar })}
                    />
                  )}
                </div>
                <h2 className="text-2xl font-bold text-slate-800 mb-2">{userInfo.username}</h2>
                <p className="text-slate-600 mb-4">{userInfo.bio}</p>
                <div className="space-y-3 text-sm text-slate-500">
                  <div className="flex items-center justify-center space-x-2">
                    <Calendar className="h-4 w-4" />
                    <span>加入时间: {userInfo.joinDate}</span>
                  </div>
                  <div className="flex items-center justify-center space-x-2">
                    <User className="h-4 w-4" />
                    <span>最后登录: {userInfo.lastLogin}</span>
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>

          {/* 右侧信息表单 */}
          <div className="lg:col-span-2">
            <Card className="rounded-3xl border-0 shadow-lg bg-white overflow-hidden">
              <CardContent className="p-8">
                <div className="flex items-center space-x-3 mb-8">
                  <div className="p-3 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600">
                    <User className="h-6 w-6 text-white" />
                  </div>
                  <div>
                    <h3 className="text-xl font-bold text-slate-800">基本信息</h3>
                    <p className="text-slate-600">更新您的个人资料信息</p>
                  </div>
                </div>

                <div className="space-y-6">
                  {/* 用户名 */}
                  <div className="space-y-2">
                    <Label htmlFor="username" className="text-sm font-medium text-slate-700">
                      用户名
                    </Label>
                    {isEditing ? (
                      <Input
                        id="username"
                        value={editForm.username}
                        onChange={(e) => setEditForm({ ...editForm, username: e.target.value })}
                        className="rounded-2xl border-slate-200 h-12"
                      />
                    ) : (
                      <div className="flex items-center space-x-3 p-3 bg-slate-50 rounded-2xl">
                        <User className="h-5 w-5 text-slate-400" />
                        <span className="text-slate-700">{userInfo.username}</span>
                      </div>
                    )}
                  </div>

                  {/* 邮箱 */}
                  <div className="space-y-2">
                    <Label htmlFor="email" className="text-sm font-medium text-slate-700">
                      邮箱地址
                    </Label>
                    {isEditing ? (
                      <Input
                        id="email"
                        type="email"
                        value={editForm.email}
                        onChange={(e) => setEditForm({ ...editForm, email: e.target.value })}
                        className="rounded-2xl border-slate-200 h-12"
                      />
                    ) : (
                      <div className="flex items-center space-x-3 p-3 bg-slate-50 rounded-2xl">
                        <Mail className="h-5 w-5 text-slate-400" />
                        <span className="text-slate-700">{userInfo.email}</span>
                      </div>
                    )}
                  </div>

                  {/* 手机号 */}
                  <div className="space-y-2">
                    <Label htmlFor="phone" className="text-sm font-medium text-slate-700">
                      手机号码
                    </Label>
                    {isEditing ? (
                      <Input
                        id="phone"
                        value={editForm.phone}
                        onChange={(e) => setEditForm({ ...editForm, phone: e.target.value })}
                        className="rounded-2xl border-slate-200 h-12"
                      />
                    ) : (
                      <div className="flex items-center space-x-3 p-3 bg-slate-50 rounded-2xl">
                        <Phone className="h-5 w-5 text-slate-400" />
                        <span className="text-slate-700">{userInfo.phone}</span>
                      </div>
                    )}
                  </div>

                  {/* 个人简介 */}
                  <div className="space-y-2">
                    <Label htmlFor="bio" className="text-sm font-medium text-slate-700">
                      个人简介
                    </Label>
                    {isEditing ? (
                      <Textarea
                        id="bio"
                        value={editForm.bio}
                        onChange={(e) => setEditForm({ ...editForm, bio: e.target.value })}
                        className="rounded-2xl border-slate-200 min-h-[100px]"
                        placeholder="介绍一下自己..."
                      />
                    ) : (
                      <div className="p-3 bg-slate-50 rounded-2xl">
                        <span className="text-slate-700">{userInfo.bio}</span>
                      </div>
                    )}
                  </div>
                </div>
              </CardContent>
            </Card>
          </div>
        </div>
      </div>
    </div>
  )
}
