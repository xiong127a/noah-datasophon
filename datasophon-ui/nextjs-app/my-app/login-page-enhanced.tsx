"use client"

import type React from "react"
import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import { User, Lock, Eye, EyeOff } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"

// 动态背景组件
const DynamicBackground = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const mouseRef = useRef({ x: 0, y: 0 })
  const animationRef = useRef<number>()

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext("2d")
    if (!ctx) return

    // 设置画布尺寸
    const resizeCanvas = () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
    }
    resizeCanvas()
    window.addEventListener("resize", resizeCanvas)

    // 粒子系统
    const particles: Array<{
      x: number
      y: number
      vx: number
      vy: number
      size: number
      opacity: number
      connections: number[]
    }> = []

    // 创建粒子
    const createParticles = () => {
      const particleCount = Math.min(150, Math.floor((canvas.width * canvas.height) / 15000))
      particles.length = 0

      for (let i = 0; i < particleCount; i++) {
        particles.push({
          x: Math.random() * canvas.width,
          y: Math.random() * canvas.height,
          vx: (Math.random() - 0.5) * 0.5,
          vy: (Math.random() - 0.5) * 0.5,
          size: Math.random() * 2 + 1,
          opacity: Math.random() * 0.5 + 0.2,
          connections: [],
        })
      }
    }

    createParticles()

    // 鼠标交互
    const handleMouseMove = (e: MouseEvent) => {
      mouseRef.current = { x: e.clientX, y: e.clientY }
    }
    window.addEventListener("mousemove", handleMouseMove)

    // 动画循环
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)

      // 更新粒子位置
      particles.forEach((particle, i) => {
        // 基础移动
        particle.x += particle.vx
        particle.y += particle.vy

        // 边界反弹
        if (particle.x < 0 || particle.x > canvas.width) particle.vx *= -1
        if (particle.y < 0 || particle.y > canvas.height) particle.vy *= -1

        // 鼠标吸引效果
        const mouseDistance = Math.sqrt(
          Math.pow(mouseRef.current.x - particle.x, 2) + Math.pow(mouseRef.current.y - particle.y, 2),
        )

        if (mouseDistance < 150) {
          const force = (150 - mouseDistance) / 150
          const angle = Math.atan2(mouseRef.current.y - particle.y, mouseRef.current.x - particle.x)
          particle.vx += Math.cos(angle) * force * 0.02
          particle.vy += Math.sin(angle) * force * 0.02
        }

        // 绘制粒子
        ctx.beginPath()
        ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(59, 130, 246, ${particle.opacity})`
        ctx.fill()

        // 粒子发光效果
        if (mouseDistance < 100) {
          ctx.beginPath()
          ctx.arc(particle.x, particle.y, particle.size * 3, 0, Math.PI * 2)
          ctx.fillStyle = `rgba(59, 130, 246, ${(0.1 * (100 - mouseDistance)) / 100})`
          ctx.fill()
        }
      })

      // 绘制连接线
      particles.forEach((particle, i) => {
        particles.slice(i + 1).forEach((otherParticle, j) => {
          const distance = Math.sqrt(
            Math.pow(particle.x - otherParticle.x, 2) + Math.pow(particle.y - otherParticle.y, 2),
          )

          if (distance < 120) {
            const opacity = ((120 - distance) / 120) * 0.3

            // 鼠标附近的连线加强
            const mouseToLine = Math.min(
              Math.sqrt(Math.pow(mouseRef.current.x - particle.x, 2) + Math.pow(mouseRef.current.y - particle.y, 2)),
              Math.sqrt(
                Math.pow(mouseRef.current.x - otherParticle.x, 2) + Math.pow(mouseRef.current.y - otherParticle.y, 2),
              ),
            )

            const enhancedOpacity = mouseToLine < 100 ? opacity * (2 + (100 - mouseToLine) / 50) : opacity

            ctx.beginPath()
            ctx.moveTo(particle.x, particle.y)
            ctx.lineTo(otherParticle.x, otherParticle.y)
            ctx.strokeStyle = `rgba(59, 130, 246, ${enhancedOpacity})`
            ctx.lineWidth = mouseToLine < 100 ? 2 : 1
            ctx.stroke()
          }
        })
      })

      // 鼠标光晕效果
      const gradient = ctx.createRadialGradient(
        mouseRef.current.x,
        mouseRef.current.y,
        0,
        mouseRef.current.x,
        mouseRef.current.y,
        100,
      )
      gradient.addColorStop(0, "rgba(59, 130, 246, 0.1)")
      gradient.addColorStop(1, "rgba(59, 130, 246, 0)")

      ctx.beginPath()
      ctx.arc(mouseRef.current.x, mouseRef.current.y, 100, 0, Math.PI * 2)
      ctx.fillStyle = gradient
      ctx.fill()

      animationRef.current = requestAnimationFrame(animate)
    }

    animate()

    return () => {
      window.removeEventListener("resize", resizeCanvas)
      window.removeEventListener("mousemove", handleMouseMove)
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current)
      }
    }
  }, [])

  return (
    <canvas
      ref={canvasRef}
      className="absolute inset-0 w-full h-full"
      style={{ background: "linear-gradient(135deg, #0f172a 0%, #1e293b 25%, #334155 50%, #1e293b 75%, #0f172a 100%)" }}
    />
  )
}

export default function LoginPageEnhanced() {
  const [username, setUsername] = useState("")
  const [password, setPassword] = useState("")
  const [showPassword, setShowPassword] = useState(false)
  const [isLoading, setIsLoading] = useState(false)
  const [focusedField, setFocusedField] = useState("")
  const router = useRouter()

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    setIsLoading(true)

    // 模拟登录延迟
    await new Promise((resolve) => setTimeout(resolve, 1500))

    // 开发阶段：任何用户名密码都可以登录
    if (username && password) {
      router.push("/")
    }

    setIsLoading(false)
  }

  return (
    <div className="min-h-screen relative overflow-hidden">
      {/* 动态科技背景 */}
      <DynamicBackground />

      {/* 渐变遮罩 */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-900/50 via-blue-900/30 to-indigo-900/50" />

      {/* 主要内容 */}
      <div className="relative z-10 flex items-center justify-center min-h-screen p-8">
        <div className="w-full max-w-md">
          {/* 顶部标题 */}
          <div className="text-center mb-12">
            <div className="flex items-center justify-center mb-8">
              <div className="relative group">
                <div className="absolute inset-0 bg-gradient-to-r from-blue-500 to-purple-600 rounded-3xl blur-xl opacity-75 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative w-20 h-20 bg-gradient-to-br from-blue-500 via-purple-600 to-indigo-700 rounded-3xl flex items-center justify-center border border-white/20 shadow-2xl">
                  <span className="text-2xl font-bold text-white">中兵</span>
                </div>
              </div>
            </div>
            <h1 className="text-5xl font-bold text-white mb-4 tracking-wide">
              <span className="bg-gradient-to-r from-blue-400 via-purple-400 to-indigo-400 bg-clip-text text-transparent">
                Noah
              </span>
              <span className="text-white/90"> 大数据基础平台</span>
            </h1>
            <div className="w-32 h-1 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full mx-auto mb-4" />
            <p className="text-white/70 text-lg">智能化数据处理 • 企业级安全保障</p>
          </div>

          {/* 登录卡片 */}
          <Card className="bg-white/10 backdrop-blur-2xl border border-white/20 rounded-3xl shadow-2xl hover:shadow-3xl transition-all duration-500 group">
            <CardContent className="p-10">
              {/* Datasophon Logo */}
              <div className="text-center mb-10">
                <div className="relative group/logo">
                  <div className="absolute inset-0 bg-gradient-to-br from-blue-500 to-purple-600 rounded-3xl blur-lg opacity-75 group-hover/logo:opacity-100 transition-opacity duration-300" />
                  <div className="relative w-24 h-24 bg-gradient-to-br from-blue-500 via-purple-600 to-indigo-700 rounded-3xl flex items-center justify-center mx-auto mb-6 shadow-2xl border border-white/30">
                    <span className="text-3xl font-bold text-white">D</span>
                  </div>
                </div>
                <h2 className="text-3xl font-bold text-white mb-3">Datasophon</h2>
                <p className="text-white/70 text-base">一站式大数据平台部署与管理系统</p>
              </div>

              {/* 登录表单 */}
              <form onSubmit={handleLogin} className="space-y-8">
                {/* 用户名输入框 */}
                <div className="relative group">
                  <div className="absolute inset-y-0 left-0 pl-5 flex items-center pointer-events-none">
                    <User
                      className={`h-6 w-6 transition-colors duration-300 ${
                        focusedField === "username" || username ? "text-blue-400" : "text-white/50"
                      }`}
                    />
                  </div>
                  <Input
                    type="text"
                    placeholder="用户名"
                    value={username}
                    onChange={(e) => setUsername(e.target.value)}
                    onFocus={() => setFocusedField("username")}
                    onBlur={() => setFocusedField("")}
                    className={`pl-14 h-16 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl transition-all duration-300 text-lg ${
                      focusedField === "username" || username
                        ? "bg-white/20 border-blue-400/50 ring-2 ring-blue-400/30 shadow-lg shadow-blue-500/20"
                        : "hover:bg-white/15 hover:border-white/30"
                    }`}
                    required
                  />
                  {username && (
                    <div className="absolute right-4 top-1/2 transform -translate-y-1/2 w-3 h-3 bg-green-400 rounded-full animate-pulse shadow-lg shadow-green-400/50" />
                  )}
                </div>

                {/* 密码输入框 */}
                <div className="relative group">
                  <div className="absolute inset-y-0 left-0 pl-5 flex items-center pointer-events-none">
                    <Lock
                      className={`h-6 w-6 transition-colors duration-300 ${
                        focusedField === "password" || password ? "text-blue-400" : "text-white/50"
                      }`}
                    />
                  </div>
                  <Input
                    type={showPassword ? "text" : "password"}
                    placeholder="密码"
                    value={password}
                    onChange={(e) => setPassword(e.target.value)}
                    onFocus={() => setFocusedField("password")}
                    onBlur={() => setFocusedField("")}
                    className={`pl-14 pr-14 h-16 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl transition-all duration-300 text-lg ${
                      focusedField === "password" || password
                        ? "bg-white/20 border-blue-400/50 ring-2 ring-blue-400/30 shadow-lg shadow-blue-500/20"
                        : "hover:bg-white/15 hover:border-white/30"
                    }`}
                    required
                  />
                  <button
                    type="button"
                    onClick={() => setShowPassword(!showPassword)}
                    className="absolute inset-y-0 right-0 pr-5 flex items-center group/eye"
                  >
                    {showPassword ? (
                      <EyeOff className="h-6 w-6 text-white/50 group-hover/eye:text-white/70 transition-colors duration-200" />
                    ) : (
                      <Eye className="h-6 w-6 text-white/50 group-hover/eye:text-white/70 transition-colors duration-200" />
                    )}
                  </button>
                  {password && (
                    <div className="absolute right-16 top-1/2 transform -translate-y-1/2 w-3 h-3 bg-green-400 rounded-full animate-pulse shadow-lg shadow-green-400/50" />
                  )}
                </div>

                {/* 登录按钮 */}
                <Button
                  type="submit"
                  disabled={isLoading}
                  className="w-full h-16 bg-gradient-to-r from-blue-500 via-purple-600 to-indigo-700 hover:from-blue-600 hover:via-purple-700 hover:to-indigo-800 text-white font-bold rounded-2xl shadow-2xl hover:shadow-3xl transition-all duration-500 border-0 text-xl relative overflow-hidden group/btn"
                >
                  <div className="absolute inset-0 bg-gradient-to-r from-white/0 via-white/20 to-white/0 transform -skew-x-12 -translate-x-full group-hover/btn:translate-x-full transition-transform duration-1000" />
                  {isLoading ? (
                    <div className="flex items-center space-x-3">
                      <div className="w-6 h-6 border-3 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>登录中...</span>
                    </div>
                  ) : (
                    <span className="relative z-10">登录 →</span>
                  )}
                </Button>
              </form>

              {/* 底部链接 */}
              <div className="flex justify-center space-x-8 mt-10 text-sm">
                <button className="text-white/70 hover:text-white transition-colors duration-200 flex items-center space-x-2 group/link">
                  <div className="w-2 h-2 bg-blue-400 rounded-full group-hover/link:animate-pulse" />
                  <span>智能管理</span>
                </button>
                <button className="text-white/70 hover:text-white transition-colors duration-200 flex items-center space-x-2 group/link">
                  <div className="w-2 h-2 bg-purple-400 rounded-full group-hover/link:animate-pulse" />
                  <span>高可用性</span>
                </button>
                <button className="text-white/70 hover:text-white transition-colors duration-200 flex items-center space-x-2 group/link">
                  <div className="w-2 h-2 bg-indigo-400 rounded-full group-hover/link:animate-pulse" />
                  <span>多租户支持</span>
                </button>
              </div>
            </CardContent>
          </Card>

          {/* 底部版权信息 */}
          <div className="text-center mt-12 space-y-4">
            <p className="text-white/60 text-sm">北京中兵数科技集团有限公司 版权所有</p>
            <p className="text-white/60 text-sm">Copyright © 2025 Datasophon</p>
            <div className="flex items-center justify-center mt-6">
              <div className="relative group">
                <div className="absolute inset-0 bg-gradient-to-r from-blue-500 to-purple-600 rounded-xl blur-md opacity-75 group-hover:opacity-100 transition-opacity duration-300" />
                <div className="relative w-12 h-12 bg-white/10 backdrop-blur-xl rounded-xl flex items-center justify-center border border-white/20">
                  <span className="text-xs font-bold text-white">中兵数科</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  )
}
