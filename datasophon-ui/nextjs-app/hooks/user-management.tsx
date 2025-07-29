"use client"

import { useState } from "react"
import { Users, Search, Plus, Edit, Trash2, Mail, Phone, Calendar } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Table, TableBody, TableCell, TableHead, TableHeader, TableRow } from "@/components/ui/table"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  DialogTrigger,
} from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import FinalNavbar from "./noah-navbar-final"

// 用户数据
const users = [
  {
    id: 1,
    username: "admin",
    email: "xxx@163.com",
    phone: "1865xxx",
    createdAt: "2022-05-10 16:05:18",
    role: "管理员",
    status: "active",
  },
  {
    id: 2,
    username: "noahx",
    email: "noahx@noah.com",
    phone: "1999999999",
    createdAt: "2025-07-11 11:03:01",
    role: "普通用户",
    status: "active",
  },
]

const UserRow = ({
  user,
  onEdit,
  onDelete,
}: { user: any; onEdit: (user: any) => void; onDelete: (id: number) => void }) => {
  return (
    <TableRow className="hover:bg-slate-50/50 transition-colors group">
      <TableCell className="font-medium text-slate-700">{user.id}</TableCell>
      <TableCell>
        <div className="flex items-center space-x-3">
          <div className="w-10 h-10 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white font-medium shadow-lg">
            {user.username.charAt(0).toUpperCase()}
          </div>
          <div>
            <p className="font-semibold text-slate-800">{user.username}</p>
            <Badge variant={user.role === "管理员" ? "default" : "secondary"} className="mt-1 text-xs rounded-full">
              {user.role}
            </Badge>
          </div>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center space-x-2">
          <Mail className="h-4 w-4 text-slate-400" />
          <span className="text-slate-600">{user.email}</span>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center space-x-2">
          <Phone className="h-4 w-4 text-slate-400" />
          <span className="text-slate-600">{user.phone}</span>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex items-center space-x-2">
          <Calendar className="h-4 w-4 text-slate-400" />
          <span className="text-slate-600 text-sm">{user.createdAt}</span>
        </div>
      </TableCell>
      <TableCell>
        <div className="flex space-x-2 opacity-0 group-hover:opacity-100 transition-opacity">
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onEdit(user)}
            className="text-blue-600 hover:text-blue-700 hover:bg-blue-50 rounded-xl"
          >
            <Edit className="h-4 w-4 mr-1" />
            编辑
          </Button>
          <Button
            variant="ghost"
            size="sm"
            onClick={() => onDelete(user.id)}
            className="text-red-600 hover:text-red-700 hover:bg-red-50 rounded-xl"
          >
            <Trash2 className="h-4 w-4 mr-1" />
            删除
          </Button>
        </div>
      </TableCell>
    </TableRow>
  )
}

const AddUserDialog = ({ onAdd }: { onAdd: (user: any) => void }) => {
  const [open, setOpen] = useState(false)
  const [formData, setFormData] = useState({
    username: "",
    email: "",
    phone: "",
    role: "普通用户",
  })

  const handleSubmit = () => {
    onAdd({
      ...formData,
      id: Date.now(),
      createdAt: new Date().toLocaleString("zh-CN"),
      status: "active",
    })
    setFormData({ username: "", email: "", phone: "", role: "普通用户" })
    setOpen(false)
  }

  return (
    <Dialog open={open} onOpenChange={setOpen}>
      <DialogTrigger asChild>
        <Button className="rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white border-0 shadow-lg">
          <Plus className="mr-2 h-4 w-4" />
          添加用户
        </Button>
      </DialogTrigger>
      <DialogContent className="rounded-3xl border-0 shadow-2xl max-w-md">
        <DialogHeader>
          <DialogTitle className="text-xl font-bold">添加新用户</DialogTitle>
          <DialogDescription>填写用户基本信息创建新账户</DialogDescription>
        </DialogHeader>
        <div className="space-y-4 py-4">
          <div className="space-y-2">
            <Label htmlFor="username">用户名</Label>
            <Input
              id="username"
              value={formData.username}
              onChange={(e) => setFormData({ ...formData, username: e.target.value })}
              className="rounded-2xl border-slate-200"
              placeholder="请输入用户名"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="email">邮箱</Label>
            <Input
              id="email"
              type="email"
              value={formData.email}
              onChange={(e) => setFormData({ ...formData, email: e.target.value })}
              className="rounded-2xl border-slate-200"
              placeholder="请输入邮箱地址"
            />
          </div>
          <div className="space-y-2">
            <Label htmlFor="phone">手机号</Label>
            <Input
              id="phone"
              value={formData.phone}
              onChange={(e) => setFormData({ ...formData, phone: e.target.value })}
              className="rounded-2xl border-slate-200"
              placeholder="请输入手机号码"
            />
          </div>
        </div>
        <DialogFooter>
          <Button variant="outline" onClick={() => setOpen(false)} className="rounded-2xl">
            取消
          </Button>
          <Button onClick={handleSubmit} className="rounded-2xl bg-gradient-to-r from-blue-500 to-purple-600">
            创建用户
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

export default function UserManagement() {
  const [searchTerm, setSearchTerm] = useState("")
  const [userList, setUserList] = useState(users)

  const filteredUsers = userList.filter(
    (user) =>
      user.username.toLowerCase().includes(searchTerm.toLowerCase()) ||
      user.email.toLowerCase().includes(searchTerm.toLowerCase()),
  )

  const handleEdit = (user: any) => {
    console.log("编辑用户:", user)
  }

  const handleDelete = (id: number) => {
    setUserList(userList.filter((user) => user.id !== id))
  }

  const handleAdd = (user: any) => {
    setUserList([...userList, user])
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-white to-slate-50">
      <FinalNavbar />

      {/* 页面头部 */}
      <div className="relative overflow-hidden bg-white border-b border-slate-200/50">
        <div className="absolute inset-0 bg-gradient-to-r from-indigo-50/50 via-white to-purple-50/50" />
        <div className="relative max-w-7xl mx-auto px-8 py-12">
          <div className="flex items-center justify-between">
            <div>
              <h1 className="text-4xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent mb-2">
                用户管理
              </h1>
              <p className="text-slate-600 text-lg">管理系统用户账户和权限</p>
            </div>
            <div className="flex items-center space-x-4">
              <Badge variant="outline" className="px-4 py-2 rounded-full border-green-200 text-green-700 bg-green-50">
                <Users className="w-4 h-4 mr-2" />
                {userList.length} 个用户
              </Badge>
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容 */}
      <div className="max-w-7xl mx-auto px-8 py-12">
        {/* 搜索和操作栏 */}
        <div className="flex items-center justify-between mb-8">
          <div className="relative flex-1 max-w-md">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 h-4 w-4 text-slate-400" />
            <Input
              placeholder="请输入用户名"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 rounded-2xl border-slate-200 bg-white h-12"
            />
          </div>
          <AddUserDialog onAdd={handleAdd} />
        </div>

        {/* 用户表格 */}
        <Card className="rounded-3xl border-0 shadow-lg bg-white overflow-hidden">
          <CardContent className="p-0">
            <div className="p-6 bg-gradient-to-r from-blue-50 to-purple-50 border-b border-slate-100">
              <div className="flex items-center space-x-3">
                <div className="p-3 rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600">
                  <Users className="h-6 w-6 text-white" />
                </div>
                <div>
                  <h2 className="text-xl font-bold text-slate-800">用户列表</h2>
                  <p className="text-slate-600">共 {filteredUsers.length} 个用户</p>
                </div>
              </div>
            </div>

            <Table>
              <TableHeader>
                <TableRow className="border-slate-100">
                  <TableHead className="font-semibold text-slate-700">序号</TableHead>
                  <TableHead className="font-semibold text-slate-700">用户名</TableHead>
                  <TableHead className="font-semibold text-slate-700">邮箱</TableHead>
                  <TableHead className="font-semibold text-slate-700">电话</TableHead>
                  <TableHead className="font-semibold text-slate-700">创建时间</TableHead>
                  <TableHead className="font-semibold text-slate-700">操作</TableHead>
                </TableRow>
              </TableHeader>
              <TableBody>
                {filteredUsers.map((user) => (
                  <UserRow key={user.id} user={user} onEdit={handleEdit} onDelete={handleDelete} />
                ))}
              </TableBody>
            </Table>

            {/* 分页 */}
            <div className="flex items-center justify-between p-6 border-t border-slate-100">
              <div className="text-sm text-slate-600">共 {filteredUsers.length} 条</div>
              <div className="flex items-center space-x-2">
                <Button variant="outline" size="sm" className="rounded-xl bg-transparent">
                  上一页
                </Button>
                <Button variant="outline" size="sm" className="rounded-xl bg-blue-50 text-blue-600 border-blue-200">
                  1
                </Button>
                <Button variant="outline" size="sm" className="rounded-xl bg-transparent">
                  下一页
                </Button>
                <span className="text-sm text-slate-600 ml-4">10 条/页</span>
              </div>
            </div>
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
