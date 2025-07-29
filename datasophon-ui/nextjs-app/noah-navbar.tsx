"use client"

import { useState } from "react"
import {
  ChevronDown,
  Database,
  Home,
  Server,
  AlertTriangle,
  Settings,
  Layers,
  Users,
  History,
  Bell,
  LogOut,
  UserCircle,
  Shield,
  MessageSquare,
  BarChart3,
  HelpCircle,
  Building,
  Tag,
  FileText,
  List,
  HardDrive,
  Boxes,
} from "lucide-react"
import Link from "next/link"

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import {
  NavigationMenu,
  NavigationMenuContent,
  NavigationMenuItem,
  NavigationMenuLink,
  NavigationMenuList,
  NavigationMenuTrigger,
} from "@/components/ui/navigation-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"

export default function Component() {
  const [notifications] = useState(3)

  return (
    <header className="sticky top-0 z-50 w-full">
      {/* 主导航栏 */}
      <div className="relative">
        {/* 背景层 */}
        <div className="absolute inset-0 bg-gradient-to-r from-slate-50/95 via-white/95 to-slate-50/95 backdrop-blur-2xl border-b border-slate-200/50" />

        {/* 内容层 */}
        <div className="relative flex h-20 items-center justify-between px-8">
          {/* 左侧 Logo 和主要导航 */}
          <div className="flex items-center space-x-10">
            {/* Logo */}
            <Link href="/" className="flex items-center space-x-3 group">
              <div className="relative">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-purple-600 rounded-2xl blur-sm opacity-75 group-hover:opacity-100 transition-opacity" />
                <div className="relative flex h-12 w-12 items-center justify-center rounded-2xl bg-gradient-to-br from-blue-500 to-purple-600 shadow-lg">
                  <Database className="h-6 w-6 text-white" />
                </div>
              </div>
              <div className="flex flex-col">
                <span className="text-2xl font-bold bg-gradient-to-r from-slate-800 to-slate-600 bg-clip-text text-transparent">
                  Noah
                </span>
                <span className="text-sm text-slate-500 -mt-1">大数据基础平台</span>
              </div>
            </Link>

            {/* 主要导航菜单 */}
            <NavigationMenu>
              <NavigationMenuList className="space-x-2">
                {/* 主页 */}
                <NavigationMenuItem>
                  <NavigationMenuLink asChild>
                    <Link
                      href="/"
                      className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 hover:shadow-lg hover:shadow-blue-100/50 focus:bg-gradient-to-r focus:from-blue-50 focus:to-purple-50 focus:outline-none"
                    >
                      <Home className="mr-2 h-4 w-4 text-slate-600 group-hover:text-blue-600 transition-colors" />
                      <span className="text-slate-700 group-hover:text-slate-900">主页</span>
                    </Link>
                  </NavigationMenuLink>
                </NavigationMenuItem>

                {/* 主机管理 */}
                <NavigationMenuItem>
                  <NavigationMenuLink asChild>
                    <Link
                      href="/hosts"
                      className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 hover:shadow-lg hover:shadow-green-100/50 focus:bg-gradient-to-r focus:from-green-50 focus:to-emerald-50 focus:outline-none"
                    >
                      <Server className="mr-2 h-4 w-4 text-slate-600 group-hover:text-green-600 transition-colors" />
                      <span className="text-slate-700 group-hover:text-slate-900">主机管理</span>
                    </Link>
                  </NavigationMenuLink>
                </NavigationMenuItem>

                {/* 告警管理 */}
                <NavigationMenuItem>
                  <NavigationMenuTrigger className="h-12 rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50 hover:shadow-lg hover:shadow-orange-100/50 data-[state=open]:bg-gradient-to-r data-[state=open]:from-orange-50 data-[state=open]:to-red-50">
                    <AlertTriangle className="mr-2 h-4 w-4 text-slate-600 group-hover:text-orange-600 transition-colors" />
                    <span className="text-slate-700">告警管理</span>
                  </NavigationMenuTrigger>
                  <NavigationMenuContent className="left-0 top-0 w-full data-[motion^=from-]:animate-in data-[motion^=to-]:animate-out data-[motion^=from-]:fade-in data-[motion^=to-]:fade-out data-[motion=from-end]:slide-in-from-right-52 data-[motion=from-start]:slide-in-from-left-52 data-[motion=to-end]:slide-out-to-right-52 data-[motion=to-start]:slide-out-to-left-52">
                    <div className="absolute left-1/2 top-full mt-3 -translate-x-1/2">
                      <div className="grid w-72 gap-2 p-4 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50">
                        <NavigationMenuLink asChild>
                          <Link
                            href="/alerts/notification-groups"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:shadow-md group"
                          >
                            <MessageSquare className="h-5 w-5 text-blue-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">通知组管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/alerts/alert-groups"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50 hover:shadow-md group"
                          >
                            <Shield className="h-5 w-5 text-orange-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">告警组管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/alerts/metrics"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-purple-50 hover:to-pink-50 hover:shadow-md group"
                          >
                            <BarChart3 className="h-5 w-5 text-purple-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">告警指标管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/alerts/help"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 hover:shadow-md group"
                          >
                            <HelpCircle className="h-5 w-5 text-green-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">使用帮助</span>
                          </Link>
                        </NavigationMenuLink>
                      </div>
                    </div>
                  </NavigationMenuContent>
                </NavigationMenuItem>

                {/* 系统管理 */}
                <NavigationMenuItem>
                  <NavigationMenuTrigger className="h-12 rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-lg hover:shadow-slate-100/50 data-[state=open]:bg-gradient-to-r data-[state=open]:from-slate-50 data-[state=open]:to-gray-50">
                    <Settings className="mr-2 h-4 w-4 text-slate-600 group-hover:text-slate-600 transition-colors" />
                    <span className="text-slate-700">系统管理</span>
                  </NavigationMenuTrigger>
                  <NavigationMenuContent className="left-0 top-0 w-full data-[motion^=from-]:animate-in data-[motion^=to-]:animate-out data-[motion^=from-]:fade-in data-[motion^=to-]:fade-out data-[motion=from-end]:slide-in-from-right-52 data-[motion=from-start]:slide-in-from-left-52 data-[motion=to-end]:slide-out-to-right-52 data-[motion=to-start]:slide-out-to-left-52">
                    <div className="absolute left-1/2 top-full mt-3 -translate-x-1/2">
                      <div className="grid w-72 gap-2 p-4 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50">
                        <NavigationMenuLink asChild>
                          <Link
                            href="/system/tenants"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:shadow-md group"
                          >
                            <Building className="h-5 w-5 text-blue-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">租户管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/system/users"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 hover:shadow-md group"
                          >
                            <Users className="h-5 w-5 text-green-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">用户管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/system/racks"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-purple-50 hover:to-pink-50 hover:shadow-md group"
                          >
                            <Server className="h-5 w-5 text-purple-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">机架管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/system/tags"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50 hover:shadow-md group"
                          >
                            <Tag className="h-5 w-5 text-orange-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">标签管理</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/system/audit"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-md group"
                          >
                            <FileText className="h-5 w-5 text-slate-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">日志审计</span>
                          </Link>
                        </NavigationMenuLink>
                      </div>
                    </div>
                  </NavigationMenuContent>
                </NavigationMenuItem>
              </NavigationMenuList>
            </NavigationMenu>
          </div>

          {/* 右侧菜单和用户区域 */}
          <div className="flex items-center space-x-6">
            {/* 右侧导航菜单 */}
            <NavigationMenu>
              <NavigationMenuList className="space-x-2">
                {/* 集群管理 */}
                <NavigationMenuItem>
                  <NavigationMenuTrigger className="h-12 rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-cyan-50 hover:to-blue-50 hover:shadow-lg hover:shadow-cyan-100/50 data-[state=open]:bg-gradient-to-r data-[state=open]:from-cyan-50 data-[state=open]:to-blue-50">
                    <Layers className="mr-2 h-4 w-4 text-slate-600 group-hover:text-cyan-600 transition-colors" />
                    <span className="text-slate-700">集群管理</span>
                  </NavigationMenuTrigger>
                  <NavigationMenuContent className="left-0 top-0 w-full data-[motion^=from-]:animate-in data-[motion^=to-]:animate-out data-[motion^=from-]:fade-in data-[motion^=to-]:fade-out data-[motion=from-end]:slide-in-from-right-52 data-[motion=from-start]:slide-in-from-left-52 data-[motion=to-end]:slide-out-to-right-52 data-[motion=to-start]:slide-out-to-left-52">
                    <div className="absolute left-1/2 top-full mt-3 -translate-x-1/2">
                      <div className="grid w-72 gap-2 p-4 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50">
                        <NavigationMenuLink asChild>
                          <Link
                            href="/clusters/list"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 hover:shadow-md group"
                          >
                            <List className="h-5 w-5 text-blue-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">集群列表</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/clusters/storage"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 hover:shadow-md group"
                          >
                            <HardDrive className="h-5 w-5 text-green-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">集群存储库</span>
                          </Link>
                        </NavigationMenuLink>
                        <NavigationMenuLink asChild>
                          <Link
                            href="/clusters/framework"
                            className="flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-purple-50 hover:to-pink-50 hover:shadow-md group"
                          >
                            <Boxes className="h-5 w-5 text-purple-500" />
                            <span className="font-medium text-slate-700 group-hover:text-slate-900">集群框架</span>
                          </Link>
                        </NavigationMenuLink>
                      </div>
                    </div>
                  </NavigationMenuContent>
                </NavigationMenuItem>

                {/* 用户管理 */}
                <NavigationMenuItem>
                  <NavigationMenuLink asChild>
                    <Link
                      href="/user-management"
                      className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-indigo-50 hover:to-purple-50 hover:shadow-lg hover:shadow-indigo-100/50 focus:bg-gradient-to-r focus:from-indigo-50 focus:to-purple-50 focus:outline-none"
                    >
                      <Users className="mr-2 h-4 w-4 text-slate-600 group-hover:text-indigo-600 transition-colors" />
                      <span className="text-slate-700 group-hover:text-slate-900">用户管理</span>
                    </Link>
                  </NavigationMenuLink>
                </NavigationMenuItem>
              </NavigationMenuList>
            </NavigationMenu>

            {/* 功能图标区域 */}
            <div className="flex items-center space-x-3">
              {/* 历史操作 */}
              <Button
                variant="ghost"
                size="sm"
                className="h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-lg hover:shadow-slate-100/50"
              >
                <History className="h-5 w-5 text-slate-600" />
                <span className="sr-only">历史操作</span>
              </Button>

              {/* 告警通知 */}
              <Button
                variant="ghost"
                size="sm"
                className="relative h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50 hover:shadow-lg hover:shadow-orange-100/50"
              >
                <Bell className="h-5 w-5 text-slate-600" />
                {notifications > 0 && (
                  <Badge className="absolute -right-1 -top-1 h-6 w-6 rounded-full p-0 text-xs bg-gradient-to-r from-red-500 to-pink-500 border-2 border-white shadow-lg">
                    {notifications}
                  </Badge>
                )}
                <span className="sr-only">告警通知</span>
              </Button>
            </div>

            {/* 用户中心 */}
            <DropdownMenu>
              <DropdownMenuTrigger asChild>
                <Button
                  variant="ghost"
                  className="relative h-12 rounded-2xl px-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-lg hover:shadow-slate-100/50"
                >
                  <div className="flex items-center space-x-3">
                    <Avatar className="h-8 w-8 ring-2 ring-slate-200 ring-offset-2">
                      <AvatarImage src="/placeholder.svg?height=32&width=32" alt="用户头像" />
                      <AvatarFallback className="text-sm bg-gradient-to-br from-blue-500 to-purple-600 text-white font-medium">
                        张三
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-sm font-medium text-slate-700">张三</span>
                    <ChevronDown className="h-4 w-4 text-slate-400" />
                  </div>
                </Button>
              </DropdownMenuTrigger>
              <DropdownMenuContent
                className="w-64 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50 p-2"
                align="end"
                forceMount
              >
                <div className="flex items-center justify-start gap-3 p-4 rounded-2xl bg-gradient-to-r from-slate-50 to-gray-50">
                  <Avatar className="h-12 w-12 ring-2 ring-white ring-offset-2">
                    <AvatarImage src="/placeholder.svg?height=48&width=48" alt="用户头像" />
                    <AvatarFallback className="bg-gradient-to-br from-blue-500 to-purple-600 text-white font-medium">
                      张三
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-col space-y-1">
                    <p className="font-semibold text-slate-900">张三</p>
                    <p className="text-sm text-slate-500">zhangsan@noah.com</p>
                  </div>
                </div>
                <DropdownMenuSeparator className="my-2" />
                <DropdownMenuItem className="rounded-2xl p-3 transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50">
                  <UserCircle className="mr-3 h-5 w-5 text-blue-500" />
                  <span className="font-medium">个人信息</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator className="my-2" />
                <DropdownMenuItem className="rounded-2xl p-3 text-red-600 transition-all duration-200 hover:bg-gradient-to-r hover:from-red-50 hover:to-pink-50">
                  <LogOut className="mr-3 h-5 w-5" />
                  <span className="font-medium">退出登录</span>
                </DropdownMenuItem>
              </DropdownMenuContent>
            </DropdownMenu>
          </div>
        </div>
      </div>
    </header>
  )
}
