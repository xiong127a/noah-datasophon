"use client"

import type React from "react"

import { useState } from "react"
import { useRouter } from "next/navigation"
import { User, Lock, Eye, EyeOff } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"

export default function LoginPage() {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const router = useRouter()

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)

    // 模拟登录延迟
    await new Promise((resolve) => setTimeout(resolve, 1000))

    // 开发阶段：任何用户名密码都可以登录
    if (username && password) {
      router.push("/")
    }

    setIsLoading(false)
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* 背景图片 */}
      <div
        className="absolute inset-0 bg-cover bg-center bg-no-repeat"
        style={{
          backgroundImage: "url('/images/login-bg.png')",
        }}
      />

      {/* 深色遮罩 */}
      <div className="absolute inset-0 bg-gradient-to-br from-blue-900/80 via-purple-900/80 to-indigo-900/80" />

      {/* 动态背景效果 */}
      <div className="absolute inset-0">
        <div className="absolute top-20 left-20 w-72 h-72 bg-blue-500/10 rounded-full blur-3xl animate-pulse" />
        <div className="absolute bottom-20 right-20 w-96 h-96 bg-purple-500/10 rounded-full blur-3xl animate-pulse delay-1000" />
        <div className="absolute top-1/2 left-1/2 transform -translate-x-1/2 -translate-y-1/2 w-64 h-64 bg-cyan-500/10 rounded-full blur-3xl animate-pulse delay-500" />
      </div>

      {/* 主要内容 */}
      <div className="relative z-10 flex items-center justify-center min-h-screen p-8">
        <div className="w-full max-w-md">
          {/* 顶部标题 */}
          <div className="text-center mb-12">
            <div className="flex items-center justify-center mb-6">
              <div className="w-16 h-16 bg-white/10 backdrop-blur-xl rounded-2xl flex items-center justify-center border border-white/20">
                <span className="text-2xl font-bold text-white">中兵数科</span>
              </div>
            </div>
            <h1 className="text-4xl font-bold text-white mb-2">Noah大数据基础平台</h1>
          </div>

          {/* 登录卡片 */}
          <Card className="bg-white/10 backdrop-blur-2xl border border-white/20 rounded-3xl shadow-2xl">
            <CardContent className="p-8">
              {/* Datasophon Logo */}
              <div className="text-center mb-8">
                <div className="w-20 h-20 bg-gradient-to-br from-blue-500 to-purple-600 rounded-3xl flex items-center justify-center mx-auto mb-4 shadow-2xl">
                  <span className="text-2xl font-bold text-white">D</span>
                </div>
                <h2 className="text-2xl font-bold text-white mb-2">Datasophon</h2>
                <p className="text-white/70 text-sm">一站式大数据平台部署与管理系统</p>
              </div>

              {/* 登录表单 */}
              <form onSubmit={handleLogin} className="space-y-6">
                {/* 用户名输入框 */}
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <User className="h-5 w-5 text-white/50" />
                  </div>
                  <Input
                    type="text"
                    placeholder="用户名"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    className="pl-12 h-14 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl focus:bg-white/20 focus:border-white/40 transition-all duration-200"
                    required
                  />
                </div>

                {/* 密码输入框 */}
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <Lock className="h-5 w-5 text-white/50" />
                  </div>
                  <Input
                    type={showPassword ? "text" : "password"}
                    placeholder="密码"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    className="pl-12 pr-12 h-14 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl focus:bg-white/20 focus:border-white/40 transition-all duration-200"
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-4 flex items-center"
                  >
                    {showPassword ? (
                      <EyeOff className="h-5 w-5 text-white/50 hover:text-white/70 transition-colors" />
                    ) : (
                      <Eye className="h-5 w-5 text-white/50 hover:text-white/70 transition-colors" />
                    )}
                  </button>
                </div>

                {/* 登录按钮 */}
                <Button
                  type="submit"
                  disabled={isLoading}
                  className="w-full h-14 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white font-semibold rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 border-0 text-lg"
                >
                  {isLoading ? (
                    <div className="flex items-center space-x-2">
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>登录中...</span>
                    </div>
                  ) : (
                    "登录 →"
                  )}
                </Button>
              </form>

              {/* 底部链接 */}
              <div className="flex justify-center space-x-6 mt-8 text-sm">
                <button className="text-white/70 hover:text-white transition-colors">• 智能管理</button>
                <button className="text-white/70 hover:text-white transition-colors">• 高可用性</button>
                <button className="text-white/70 hover:text-white transition-colors">• 多租户支持</button>
              </div>
            </CardContent>
          </Card>

          {/* 底部版权信息 */}
          <div className="text-center mt-12 space-y-2">
            <p className="text-white/60 text-sm">北京中兵数科技集团有限公司 版权所有</p>
            <p className="text-white/60 text-sm">Copyright © 2025 Datasophon</p>
            <div className="flex items-center justify-center mt-4">
              <div className="w-8 h-8 bg-white/10 backdrop-blur-xl rounded-lg flex items-center justify-center border border-white/20">
                <span className="text-xs font-bold text-white">中兵数科</span>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
