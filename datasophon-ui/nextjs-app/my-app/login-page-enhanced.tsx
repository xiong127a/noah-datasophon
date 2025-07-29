"use client"

import type React from "react"
import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import { User, Lock, Eye, EyeOff } from "lucide-react"
import Image from "next/image"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"

// 增强的动态背景组件 - 更清晰的粒子效果和连线
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

    // 创建粒子 - 增加粒子数量
    const createParticles = () => {
      const particleCount = Math.min(200, Math.floor((canvas.width * canvas.height) / 10000))
      particles.length = 0

      for (let i = 0; i < particleCount; i++) {
        particles.push({
          x: Math.random() * canvas.width,
          y: Math.random() * canvas.height,
          vx: (Math.random() - 0.5) * 0.6, // 增加速度
          vy: (Math.random() - 0.5) * 0.6, // 增加速度
          size: Math.random() * 2.5 + 1.5, // 增大粒子
          opacity: Math.random() * 0.7 + 0.3, // 提高不透明度
          connections: [],
        })
      }
    }

    createParticles()

    // 鼠标交互 - 增强效果
    const handleMouseMove = (e: MouseEvent) => {
      mouseRef.current = { x: e.clientX, y: e.clientY }
    }
    window.addEventListener("mousemove", handleMouseMove)

    // 动画循环 - 增强粒子连线和交互效果
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

        // 增强鼠标吸引效果
        const mouseDistance = Math.sqrt(
          Math.pow(mouseRef.current.x - particle.x, 2) + Math.pow(mouseRef.current.y - particle.y, 2),
        )

        if (mouseDistance < 200) { // 增加影响范围
          const force = (200 - mouseDistance) / 200
          const angle = Math.atan2(mouseRef.current.y - particle.y, mouseRef.current.x - particle.x)
          particle.vx += Math.cos(angle) * force * 0.05 // 增加影响力
          particle.vy += Math.sin(angle) * force * 0.05 // 增加影响力
        }

        // 绘制粒子 - 使用渐变颜色
        const gradient = ctx.createRadialGradient(
          particle.x, particle.y, 0, 
          particle.x, particle.y, particle.size * 2
        )
        gradient.addColorStop(0, `rgba(59, 130, 246, ${particle.opacity})`)
        gradient.addColorStop(1, `rgba(147, 51, 234, ${particle.opacity * 0.5})`)
        
        ctx.beginPath()
        ctx.arc(particle.x, particle.y, particle.size, 0, Math.PI * 2)
        ctx.fillStyle = gradient
        ctx.fill()

        // 清除之前的连接
        particle.connections = []

        // 寻找邻近粒子并连线 - 增强连线效果
        for (let j = i + 1; j < particles.length; j++) {
          const otherParticle = particles[j]
          const dx = particle.x - otherParticle.x
          const dy = particle.y - otherParticle.y
          const distance = Math.sqrt(dx * dx + dy * dy)

          if (distance < 150) { // 增加连线距离
            particle.connections.push(j)
            
            // 绘制连线 - 更清晰的渐变线条
            ctx.beginPath()
            ctx.moveTo(particle.x, particle.y)
            ctx.lineTo(otherParticle.x, otherParticle.y)
            
            // 距离越近，线条越粗越亮
            const lineOpacity = 1 - distance / 150
            const lineWidth = 1.5 - distance / 150
            
            ctx.strokeStyle = `rgba(147, 197, 253, ${lineOpacity * 0.8})`
            ctx.lineWidth = lineWidth
            ctx.stroke()
          }
        }
      })

      // 鼠标周围绘制光环效果
      if (mouseRef.current.x > 0) {
        const mouseGlow = ctx.createRadialGradient(
          mouseRef.current.x, mouseRef.current.y, 0,
          mouseRef.current.x, mouseRef.current.y, 150
        )
        mouseGlow.addColorStop(0, "rgba(59, 130, 246, 0.1)")
        mouseGlow.addColorStop(0.5, "rgba(147, 51, 234, 0.05)")
        mouseGlow.addColorStop(1, "rgba(0, 0, 0, 0)")
        
        ctx.beginPath()
        ctx.arc(mouseRef.current.x, mouseRef.current.y, 150, 0, Math.PI * 2)
        ctx.fillStyle = mouseGlow
        ctx.fill()
      }

      animationRef.current = requestAnimationFrame(animate)
    }

    animate()

    return () => {
      if (animationRef.current) {
        cancelAnimationFrame(animationRef.current)
      }
      window.removeEventListener("resize", resizeCanvas)
      window.removeEventListener("mousemove", handleMouseMove)
    }
  }, [])

  return <canvas ref={canvasRef} className="absolute inset-0 z-0" />
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

      {/* 左上角Logo */}
      <div className="absolute left-12 top-8 z-20">
        <Image 
          src="/images/login/company.png" 
          alt="中兵数科" 
          width={150} 
          height={50} 
          className="opacity-90 hover:opacity-100 transition-opacity"
        />
      </div>

      {/* 主要内容 */}
      <div className="relative z-10 flex items-center justify-center min-h-screen p-8">
        <div className="w-full max-w-md">
          {/* 顶部标题 */}
          <div className="text-center mb-12">
            <div className="flex items-center justify-center mb-8">
              <Image 
                src="/images/login/product.png" 
                alt="Noah大数据基础平台" 
                width={350} 
                height={80} 
                className="opacity-90 hover:opacity-100 transition-opacity"
              />
            </div>
            <div className="w-32 h-1 bg-gradient-to-r from-blue-500 to-purple-600 rounded-full mx-auto mb-4" />
            <p className="text-white/70 text-lg">一站式大数据平台部署与管理系统</p>
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
                    className={`pl-14 h-16 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl transition-all duration-300 text-lg ${
                      focusedField === "password" || password
                        ? "bg-white/20 border-blue-400/50 ring-2 ring-blue-400/30 shadow-lg shadow-blue-500/20"
                        : "hover:bg-white/15 hover:border-white/30"
                    }`}
                    required
                  />
                  <div
                    className="absolute inset-y-0 right-0 pr-5 flex items-center cursor-pointer"
                    onClick={() => setShowPassword(!showPassword)}
                  >
                    {showPassword ? (
                      <EyeOff className="h-6 w-6 text-white/50 hover:text-blue-400 transition-colors duration-300" />
                    ) : (
                      <Eye className="h-6 w-6 text-white/50 hover:text-blue-400 transition-colors duration-300" />
                    )}
                  </div>
                  {password && (
                    <div className="absolute right-14 top-1/2 transform -translate-y-1/2 w-3 h-3 bg-green-400 rounded-full animate-pulse shadow-lg shadow-green-400/50" />
                  )}
                </div>

                {/* 登录按钮 */}
                <Button
                  type="submit"
                  disabled={isLoading}
                  className="w-full h-14 text-lg font-medium bg-gradient-to-r from-blue-600 to-indigo-600 hover:from-blue-500 hover:to-indigo-500 rounded-2xl transform transition-all duration-500 hover:scale-[1.02] disabled:opacity-70 disabled:cursor-not-allowed disabled:transform-none"
                >
                  {isLoading ? (
                    <div className="flex items-center justify-center space-x-2">
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>正在登录...</span>
                    </div>
                  ) : (
                    "登 录 →"
                  )}
                </Button>

                {/* 功能介绍 */}
                <div className="flex justify-center space-x-8 pt-4">
                  <div className="flex items-center space-x-2 text-white/70 text-sm">
                    <span className="w-2 h-2 bg-blue-400 rounded-full"></span>
                    <span>智能管理</span>
                  </div>
                  <div className="flex items-center space-x-2 text-white/70 text-sm">
                    <span className="w-2 h-2 bg-purple-400 rounded-full"></span>
                    <span>高可用性</span>
                  </div>
                  <div className="flex items-center space-x-2 text-white/70 text-sm">
                    <span className="w-2 h-2 bg-indigo-400 rounded-full"></span>
                    <span>多用户支持</span>
                  </div>
                </div>
              </form>
            </CardContent>
          </Card>
        </div>
      </div>
      
      {/* 底部版权信息 */}
      <div className="absolute bottom-5 left-0 right-0 text-center text-white/70 z-20">
        北京中兵数字科技集团有限公司 版权所有 <br />
        Copyright © 2025 Datasophon
      </div>
    </div>
  )
}
