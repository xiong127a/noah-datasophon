"use client"

import type React from "react"
import { useState, useRef, useEffect } from "react"
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
import { useRouter } from "next/navigation"
import { logout } from "./login-page-new" // 导入退出登录函数

import { Button } from "@/components/ui/button"
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu"
import { Avatar, AvatarFallback, AvatarImage } from "@/components/ui/avatar"
import { Badge } from "@/components/ui/badge"

// 全局状态管理 - 确保同一时间只有一个下拉菜单打开
let activeDropdown: string | null = null
const dropdownCallbacks: { [key: string]: (isOpen: boolean) => void } = {}

// 修复的下拉菜单组件 - 解决多个菜单同时显示的问题
const FinalDropdown = ({
  trigger,
  children,
  className = "",
  id,
}: {
  trigger: React.ReactNode
  children: React.ReactNode
  className?: string
  id: string
}) => {
  const [isOpen, setIsOpen] = useState(false)
  const timeoutRef = useRef<NodeJS.Timeout>()
  const containerRef = useRef<HTMLDivElement>(null)

  // 注册回调函数
  useEffect(() => {
    dropdownCallbacks[id] = (shouldOpen: boolean) => {
      if (!shouldOpen && isOpen) {
        setIsOpen(false)
      }
    }
    return () => {
      delete dropdownCallbacks[id]
    }
  }, [id, isOpen])

  const handleMouseEnter = () => {
    if (timeoutRef.current) {
      clearTimeout(timeoutRef.current)
    }

    // 关闭其他所有下拉菜单
    if (activeDropdown && activeDropdown !== id) {
      Object.keys(dropdownCallbacks).forEach((key) => {
        if (key !== id) {
          dropdownCallbacks[key]?.(false)
        }
      })
    }

    activeDropdown = id
    setIsOpen(true)
  }

  const handleMouseLeave = (e: React.MouseEvent) => {
    const rect = containerRef.current?.getBoundingClientRect()
    if (rect) {
      const { clientX, clientY } = e
      const buffer = 10
      const isInArea =
        clientX >= rect.left - buffer &&
        clientX <= rect.right + buffer &&
        clientY >= rect.top - buffer &&
        clientY <= rect.bottom + 300

      if (!isInArea) {
        timeoutRef.current = setTimeout(() => {
          setIsOpen(false)
          if (activeDropdown === id) {
            activeDropdown = null
          }
        }, 100)
      }
    }
  }

  useEffect(() => {
    return () => {
      if (timeoutRef.current) {
        clearTimeout(timeoutRef.current)
      }
    }
  }, [])

  return (
    <div ref={containerRef} className="relative" onMouseEnter={handleMouseEnter} onMouseLeave={handleMouseLeave}>
      <div className="cursor-pointer">{trigger}</div>
      {isOpen && (
        <div className={`absolute top-full left-1/2 -translate-x-1/2 mt-1 z-50 ${className}`}>
          <div className="grid w-72 gap-2 p-4 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50 animate-in fade-in-0 zoom-in-95 duration-200">
            {children}
          </div>
        </div>
      )}
    </div>
  )
}

const MenuLink = ({
  href,
  icon: Icon,
  children,
  colorClass = "blue",
}: {
  href: string
  icon: any
  children: React.ReactNode
  colorClass?: string
}) => {
  const getColorClasses = (color: string) => {
    switch (color) {
      case "blue":
        return {
          hover: "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50",
          icon: "text-blue-500",
        }
      case "orange":
        return {
          hover: "hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50",
          icon: "text-orange-500",
        }
      case "purple":
        return {
          hover: "hover:bg-gradient-to-r hover:from-purple-50 hover:to-pink-50",
          icon: "text-purple-500",
        }
      case "green":
        return {
          hover: "hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50",
          icon: "text-green-500",
        }
      case "slate":
        return {
          hover: "hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50",
          icon: "text-slate-500",
        }
      default:
        return {
          hover: "hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50",
          icon: "text-blue-500",
        }
    }
  }

  const colors = getColorClasses(colorClass)

  return (
    <Link
      href={href}
      className={`flex items-center space-x-3 rounded-2xl p-4 transition-all duration-200 ${colors.hover} hover:shadow-md group`}
    >
      <Icon className={`h-5 w-5 ${colors.icon}`} />
      <span className="font-medium text-slate-700 group-hover:text-slate-900">{children}</span>
    </Link>
  )
}

export default function FinalNavbar() {
  const [notifications] = useState(3)
  const router = useRouter()

  const handleLogout = () => {
    // 调用退出登录函数
    logout()
    // 不需要在这里手动跳转，logout函数会处理跳转
    // router.push("/login")
  }

  const handleProfile = () => {
    router.push("/profile")
  }

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
            <div className="flex items-center space-x-2">
              {/* 主页 */}
              <Link
                href="/"
                className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-purple-50 hover:shadow-lg hover:shadow-blue-100/50 focus:bg-gradient-to-r focus:from-blue-50 focus:to-purple-50 focus:outline-none"
              >
                <Home className="mr-2 h-4 w-4 text-slate-600 group-hover:text-blue-600 transition-colors" />
                <span className="text-slate-700 group-hover:text-slate-900">主页</span>
              </Link>

              {/* 主机管理 */}
              <Link
                href="/hosts"
                className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-green-50 hover:to-emerald-50 hover:shadow-lg hover:shadow-green-100/50 focus:bg-gradient-to-r focus:from-green-50 focus:to-emerald-50 focus:outline-none"
              >
                <Server className="mr-2 h-4 w-4 text-slate-600 group-hover:text-green-600 transition-colors" />
                <span className="text-slate-700 group-hover:text-slate-900">主机管理</span>
              </Link>

              {/* 告警管理 */}
              <FinalDropdown
                id="alerts"
                trigger={
                  <div className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50 hover:shadow-lg hover:shadow-orange-100/50">
                    <AlertTriangle className="mr-2 h-4 w-4 text-slate-600 group-hover:text-orange-600 transition-colors" />
                    <span className="text-slate-700 group-hover:text-slate-900">告警管理</span>
                    <ChevronDown className="ml-1 h-3 w-3 text-slate-400 group-hover:rotate-180 transition-transform duration-200" />
                  </div>
                }
              >
                <MenuLink href="/alerts/notification-groups" icon={MessageSquare} colorClass="blue">
                  通知组管理
                </MenuLink>
                <MenuLink href="/alerts/alert-groups" icon={Shield} colorClass="orange">
                  告警组管理
                </MenuLink>
                <MenuLink href="/alerts/metrics" icon={BarChart3} colorClass="purple">
                  告警指标管理
                </MenuLink>
                <MenuLink href="/alerts/help" icon={HelpCircle} colorClass="green">
                  使用帮助
                </MenuLink>
              </FinalDropdown>

              {/* 系统管理 */}
              <FinalDropdown
                id="system"
                trigger={
                  <div className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-lg hover:shadow-slate-100/50">
                    <Settings className="mr-2 h-4 w-4 text-slate-600 group-hover:text-slate-600 group-hover:rotate-90 transition-all duration-200" />
                    <span className="text-slate-700 group-hover:text-slate-900">系统管理</span>
                    <ChevronDown className="ml-1 h-3 w-3 text-slate-400 group-hover:rotate-180 transition-transform duration-200" />
                  </div>
                }
              >
                <MenuLink href="/system/tenants" icon={Building} colorClass="blue">
                  租户管理
                </MenuLink>
                <MenuLink href="/system/users" icon={Users} colorClass="green">
                  用户管理
                </MenuLink>
                <MenuLink href="/system/racks" icon={Server} colorClass="purple">
                  机架管理
                </MenuLink>
                <MenuLink href="/system/tags" icon={Tag} colorClass="orange">
                  标签管理
                </MenuLink>
                <MenuLink href="/system/audit" icon={FileText} colorClass="slate">
                  日志审计
                </MenuLink>
              </FinalDropdown>
            </div>
          </div>

          {/* 右侧菜单和用户区域 */}
          <div className="flex items-center space-x-6">
            {/* 右侧导航菜单 */}
            <div className="flex items-center space-x-2">
              {/* 集群管理 */}
              <FinalDropdown
                id="clusters"
                trigger={
                  <div className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-cyan-50 hover:to-blue-50 hover:shadow-lg hover:shadow-cyan-100/50">
                    <Layers className="mr-2 h-4 w-4 text-slate-600 group-hover:text-cyan-600 transition-colors" />
                    <span className="text-slate-700 group-hover:text-slate-900">集群管理</span>
                    <ChevronDown className="ml-1 h-3 w-3 text-slate-400 group-hover:rotate-180 transition-transform duration-200" />
                  </div>
                }
              >
                <MenuLink href="/clusters/list" icon={List} colorClass="blue">
                  集群列表
                </MenuLink>
                <MenuLink href="/clusters/storage" icon={HardDrive} colorClass="green">
                  集群存储库
                </MenuLink>
                <MenuLink href="/clusters/framework" icon={Boxes} colorClass="purple">
                  集群框架
                </MenuLink>
              </FinalDropdown>

              {/* 用户管理 */}
              <Link
                href="/user-management"
                className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-gradient-to-r hover:from-indigo-50 hover:to-purple-50 hover:shadow-lg hover:shadow-indigo-100/50 focus:bg-gradient-to-r focus:from-indigo-50 focus:to-purple-50 focus:outline-none"
              >
                <Users className="mr-2 h-4 w-4 text-slate-600 group-hover:text-indigo-600 transition-colors" />
                <span className="text-slate-700 group-hover:text-slate-900">用户管理</span>
              </Link>
            </div>

            {/* 功能图标区域 */}
            <div className="flex items-center space-x-3">
              {/* 历史操作 */}
              <Button
                variant="ghost"
                size="sm"
                className="h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-lg hover:shadow-slate-100/50 group"
              >
                <History className="h-5 w-5 text-slate-600 group-hover:rotate-12 transition-transform duration-200" />
                <span className="sr-only">历史操作</span>
              </Button>

              {/* 告警通知 */}
              <Button
                variant="ghost"
                size="sm"
                className="relative h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-gradient-to-r hover:from-orange-50 hover:to-red-50 hover:shadow-lg hover:shadow-orange-100/50 group"
              >
                <Bell className="h-5 w-5 text-slate-600 group-hover:animate-pulse" />
                {notifications > 0 && (
                  <Badge className="absolute -right-1 -top-1 h-6 w-6 rounded-full p-0 text-xs bg-gradient-to-r from-red-500 to-pink-500 border-2 border-white shadow-lg animate-bounce">
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
                  className="relative h-12 rounded-2xl px-4 transition-all duration-200 hover:bg-gradient-to-r hover:from-slate-50 hover:to-gray-50 hover:shadow-lg hover:shadow-slate-100/50 group"
                >
                  <div className="flex items-center space-x-3">
                    <Avatar className="h-8 w-8 ring-2 ring-slate-200 ring-offset-2 group-hover:ring-blue-300 transition-colors">
                      <AvatarImage src="/placeholder.svg?height=32&width=32" alt="用户头像" />
                      <AvatarFallback className="text-sm bg-gradient-to-br from-blue-500 to-purple-600 text-white font-medium">
                        Admin
                      </AvatarFallback>
                    </Avatar>
                    <span className="text-sm font-medium text-slate-700">Admin</span>
                    <ChevronDown className="h-4 w-4 text-slate-400 group-hover:rotate-180 transition-transform duration-200" />
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
                      Admin
                    </AvatarFallback>
                  </Avatar>
                  <div className="flex flex-col space-y-1">
                    <p className="font-semibold text-slate-900">Admin</p>
                    <p className="text-sm text-slate-500">admin@noah.com</p>
                  </div>
                </div>
                <DropdownMenuSeparator className="my-2" />
                <DropdownMenuItem
                  onClick={handleProfile}
                  className="rounded-2xl p-3 transition-all duration-200 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 cursor-pointer"
                >
                  <UserCircle className="mr-3 h-5 w-5 text-blue-500" />
                  <span className="font-medium">个人信息</span>
                </DropdownMenuItem>
                <DropdownMenuSeparator className="my-2" />
                <DropdownMenuItem
                  onClick={handleLogout}
                  className="rounded-2xl p-3 text-red-600 transition-all duration-200 hover:bg-gradient-to-r hover:from-red-50 hover:to-pink-50 cursor-pointer"
                >
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
