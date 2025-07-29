import React, { useState } from "react";
import Link from "next/link";
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
  UserCircle
} from "lucide-react";
import { cn } from "../lib/utils";
import { Button } from "./ui/button";

export default function NoahNavbar() {
  const [notifications] = useState(3);

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
            <nav className="hidden md:flex space-x-6">
              <NavItem href="/" icon={<Home className="h-4 w-4" />} label="主页" />
              <NavItem href="/hosts" icon={<Server className="h-4 w-4" />} label="主机管理" />
              <NavDropdown 
                icon={<AlertTriangle className="h-4 w-4" />} 
                label="告警管理"
                items={[
                  { href: "/alerts/notification-groups", label: "通知组管理" },
                  { href: "/alerts/alert-groups", label: "告警组管理" },
                  { href: "/alerts/metrics", label: "告警指标管理" },
                  { href: "/alerts/help", label: "使用帮助" },
                ]}
              />
              <NavDropdown 
                icon={<Settings className="h-4 w-4" />} 
                label="系统管理"
                items={[
                  { href: "/system/tenants", label: "租户管理" },
                  { href: "/system/users", label: "用户管理" },
                  { href: "/system/racks", label: "机架管理" },
                  { href: "/system/tags", label: "标签管理" },
                  { href: "/system/audit", label: "日志审计" },
                ]}
              />
            </nav>
          </div>

          {/* 右侧菜单和用户区域 */}
          <div className="flex items-center space-x-6">
            {/* 右侧导航菜单 */}
            <nav className="hidden md:flex space-x-6">
              <NavDropdown 
                icon={<Layers className="h-4 w-4" />} 
                label="集群管理"
                items={[
                  { href: "/clusters/list", label: "集群列表" },
                  { href: "/clusters/storage", label: "集群存储库" },
                  { href: "/clusters/framework", label: "集群框架" },
                ]}
              />
              <NavItem href="/user-management" icon={<Users className="h-4 w-4" />} label="用户管理" />
            </nav>

            {/* 功能图标区域 */}
            <div className="flex items-center space-x-3">
              {/* 历史操作 */}
              <Button
                variant="ghost"
                size="sm"
                className="h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-slate-100"
              >
                <History className="h-5 w-5 text-slate-600" />
                <span className="sr-only">历史操作</span>
              </Button>

              {/* 告警通知 */}
              <Button
                variant="ghost"
                size="sm"
                className="relative h-12 w-12 rounded-2xl p-0 transition-all duration-200 hover:bg-slate-100"
              >
                <Bell className="h-5 w-5 text-slate-600" />
                {notifications > 0 && (
                  <span className="absolute -right-1 -top-1 h-6 w-6 rounded-full p-0 text-xs bg-gradient-to-r from-red-500 to-pink-500 border-2 border-white shadow-lg flex items-center justify-center text-white">
                    {notifications}
                  </span>
                )}
                <span className="sr-only">告警通知</span>
              </Button>
            </div>

            {/* 用户中心 */}
            <div className="relative group">
              <Button
                variant="ghost"
                className="relative h-12 rounded-2xl px-4 transition-all duration-200 hover:bg-slate-100"
              >
                <div className="flex items-center space-x-3">
                  <div className="h-8 w-8 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white font-medium ring-2 ring-slate-200 ring-offset-2">
                    张
                  </div>
                  <span className="text-sm font-medium text-slate-700">张三</span>
                  <ChevronDown className="h-4 w-4 text-slate-400" />
                </div>
              </Button>
              
              {/* 用户菜单 */}
              <div className="absolute right-0 top-full mt-1 w-64 opacity-0 invisible group-hover:opacity-100 group-hover:visible transition-all duration-200 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50 p-2">
                <div className="flex items-center justify-start gap-3 p-4 rounded-2xl bg-gradient-to-r from-slate-50 to-gray-50">
                  <div className="h-12 w-12 rounded-full bg-gradient-to-br from-blue-500 to-purple-600 flex items-center justify-center text-white font-medium ring-2 ring-white ring-offset-2">
                    张
                  </div>
                  <div className="flex flex-col space-y-1">
                    <p className="font-semibold text-slate-900">张三</p>
                    <p className="text-sm text-slate-500">zhangsan@noah.com</p>
                  </div>
                </div>
                <div className="my-2 h-px bg-slate-200/50"></div>
                <div className="rounded-2xl p-3 transition-all duration-200 hover:bg-blue-50">
                  <Link href="/profile" className="flex items-center">
                    <UserCircle className="mr-3 h-5 w-5 text-blue-500" />
                    <span className="font-medium">个人信息</span>
                  </Link>
                </div>
                <div className="my-2 h-px bg-slate-200/50"></div>
                <div className="rounded-2xl p-3 transition-all duration-200 hover:bg-red-50">
                  <Link href="/logout" className="flex items-center text-red-600">
                    <LogOut className="mr-3 h-5 w-5" />
                    <span className="font-medium">退出登录</span>
                  </Link>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </header>
  );
}

// 导航项组件
function NavItem({ href, icon, label }) {
  return (
    <Link
      href={href}
      className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-slate-100"
    >
      <span className="mr-2 text-slate-600 group-hover:text-blue-600 transition-colors">{icon}</span>
      <span className="text-slate-700 group-hover:text-slate-900">{label}</span>
    </Link>
  );
}

// 导航下拉菜单组件
function NavDropdown({ icon, label, items }) {
  const [isOpen, setIsOpen] = useState(false);
  
  return (
    <div 
      className="relative"
      onMouseEnter={() => setIsOpen(true)}
      onMouseLeave={() => setIsOpen(false)}
    >
      <button 
        className="group inline-flex h-12 items-center justify-center rounded-2xl px-6 py-2 text-sm font-medium transition-all duration-200 hover:bg-slate-100"
      >
        <span className="mr-2 text-slate-600 group-hover:text-blue-600 transition-colors">{icon}</span>
        <span className="text-slate-700 group-hover:text-slate-900">{label}</span>
        <ChevronDown className={cn(
          "ml-2 h-4 w-4 text-slate-500 transition-transform", 
          isOpen && "rotate-180"
        )} />
      </button>
      
      {isOpen && (
        <div className="absolute top-full left-0 mt-1 min-w-60 bg-white/95 backdrop-blur-xl rounded-3xl shadow-2xl border border-slate-200/50 z-50">
          <div className="p-3">
            {items.map((item, index) => (
              <Link
                key={index}
                href={item.href}
                className="flex items-center space-x-3 rounded-2xl p-3 transition-all duration-200 hover:bg-slate-100"
              >
                <span className="font-medium text-slate-700 hover:text-slate-900">{item.label}</span>
              </Link>
            ))}
          </div>
        </div>
      )}
    </div>
  );
} 