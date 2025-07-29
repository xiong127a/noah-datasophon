"use client"

import type React from "react"
import { useState, useEffect, useRef } from "react"
import { useRouter } from "next/navigation"
import { User, Lock, Eye, EyeOff } from "lucide-react"
import Image from "next/image"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent } from "@/components/ui/card"

// 全新的网格星空背景组件
const NetworkBackground = () => {
  const canvasRef = useRef<HTMLCanvasElement>(null)
  const mouseRef = useRef({ x: 0, y: 0 })
  const animationRef = useRef<number | null>(null)

  useEffect(() => {
    const canvas = canvasRef.current
    if (!canvas) return

    const ctx = canvas.getContext("2d")
    if (!ctx) return
    
    // 星点和连接线
    const stars: Array<{
      x: number
      y: number
      size: number
      opacity: number
      velocity: {x: number, y: number}
    }> = []

    // 设置画布尺寸
    const resizeCanvas = () => {
      canvas.width = window.innerWidth
      canvas.height = window.innerHeight
      createStarryBackground() // 重新创建星空
    }
    resizeCanvas()
    window.addEventListener("resize", resizeCanvas)
    
    // 创建星空背景
    function createStarryBackground() {
      stars.length = 0
      
      if (!canvas) return
      
      // 创建较少的星点，更符合原始设计
      const starCount = Math.min(100, Math.floor(canvas.width * canvas.height / 15000))
      
      for (let i = 0; i < starCount; i++) {
        const x = Math.random() * canvas.width
        const y = Math.random() * canvas.height
        const size = Math.random() * 1.2 + 0.5
        const opacity = Math.random() * 0.4 + 0.2
        
        stars.push({
          x,
          y,
          size,
          opacity,
          velocity: { 
            x: (Math.random() - 0.5) * 0.05, 
            y: (Math.random() - 0.5) * 0.05 
          }
        })
      }
    }
    
    createStarryBackground()

    // 鼠标交互
    const handleMouseMove = (e: MouseEvent) => {
      mouseRef.current = { x: e.clientX, y: e.clientY }
    }
    window.addEventListener("mousemove", handleMouseMove)

    // 绘制网格背景
    function drawGrid() {
      if (!ctx || !canvas) return
      
      const gridSize = 40
      ctx.strokeStyle = "rgba(100, 130, 220, 0.07)"
      ctx.lineWidth = 0.3
      
      // 水平线
      for (let y = 0; y < canvas.height; y += gridSize) {
        ctx.beginPath()
        ctx.moveTo(0, y)
        ctx.lineTo(canvas.width, y)
        ctx.stroke()
      }
      
      // 垂直线
      for (let x = 0; x < canvas.width; x += gridSize) {
        ctx.beginPath()
        ctx.moveTo(x, 0)
        ctx.lineTo(x, canvas.height)
        ctx.stroke()
      }
    }

    // 动画循环
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height)
      
      // 绘制网格
      drawGrid()
      
      // 更新和绘制星点
      stars.forEach((star, index) => {
        // 缓慢移动
        star.x += star.velocity.x
        star.y += star.velocity.y
        
        // 确保星点不会移出画布
        if (star.x < 0 || star.x > canvas.width) star.velocity.x *= -1
        if (star.y < 0 || star.y > canvas.height) star.velocity.y *= -1
        
        // 绘制星点
        ctx.beginPath()
        ctx.arc(star.x, star.y, star.size, 0, Math.PI * 2)
        ctx.fillStyle = `rgba(160, 190, 255, ${star.opacity})`
        ctx.fill()
        
        // 连线 - 只连接附近的星点
        for (let j = index + 1; j < stars.length; j++) {
          const star2 = stars[j]
          const dx = star.x - star2.x
          const dy = star.y - star2.y
          const distance = Math.sqrt(dx * dx + dy * dy)
          
          // 只在一定距离内连线
          if (distance < 150) {
            // 距离越远线条越淡
            const opacity = 0.15 * (1 - distance / 150)
            
            ctx.beginPath()
            ctx.moveTo(star.x, star.y)
            ctx.lineTo(star2.x, star2.y)
            ctx.strokeStyle = `rgba(120, 150, 220, ${opacity})`
            ctx.lineWidth = 0.6
            ctx.stroke()
          }
        }
      })

      // 鼠标交互效果
      if (mouseRef.current.x && mouseRef.current.y) {
        const mouseRadius = 160
        
        // 绘制星点与鼠标的连线
        stars.forEach(star => {
          const dx = star.x - mouseRef.current.x
          const dy = star.y - mouseRef.current.y
          const distance = Math.sqrt(dx * dx + dy * dy)
          
          if (distance < mouseRadius) {
            const opacity = 0.25 * (1 - distance / mouseRadius)
            
            ctx.beginPath()
            ctx.moveTo(mouseRef.current.x, mouseRef.current.y)
            ctx.lineTo(star.x, star.y)
            ctx.strokeStyle = `rgba(140, 170, 240, ${opacity})`
            ctx.lineWidth = 0.7
            ctx.stroke()
          }
        })
      }

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
      style={{ background: "linear-gradient(to bottom, #0c1444 0%, #0c1e40 100%)" }}
    />
  )
}

export default function LoginPageEnhanced() {
  const [username, setUsername] = useState("admin")
  const [password, setPassword] = useState("admin123")
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
    <div className="fixed inset-0 overflow-hidden">
      {/* 动态科技背景 */}
      <NetworkBackground />

      {/* 渐变遮罩 */}
      <div className="absolute inset-0 bg-gradient-to-br from-slate-900/30 via-blue-900/20 to-indigo-900/30" />

      {/* 左上角公司Logo - 使用图片，放大 */}
      <div className="absolute left-6 top-6 z-20">
        <Image 
          src="/login-img/company.png" 
          alt="中兵数科" 
          width={160} 
          height={40}
          className="object-contain"
        />
      </div>

      {/* 主要内容 - 使用flex布局固定高度 */}
      <div className="relative z-10 flex flex-col items-center h-full pt-16 pb-16">
        {/* 中上方产品标题 - 使用图片中的样式 */}
        <div className="mt-4 mb-12 text-center">
          <div className="relative">
            <h1 className="text-5xl font-bold tracking-wide mb-2 bg-gradient-to-r from-blue-200 to-blue-300 bg-clip-text text-transparent drop-shadow-lg">
              Noah大数据基础平台
            </h1>
          </div>
        </div>
        
                  {/* 登录卡片 - 放在中央 */}
          <div className="w-full max-w-md">
            <Card className="bg-[#161d4f]/80 backdrop-blur-xl border border-[#2c355f] rounded-3xl shadow-2xl">
            <CardContent className="p-8">
              {/* Logo 使用SVG文件 */}
              <div className="text-center mb-8">
                <div className="w-20 h-20 flex items-center justify-center mx-auto mb-4">
                  <Image 
                    src="/login-img/logo.svg" 
                    alt="Datasophon Logo" 
                    width={65} 
                    height={65}
                    className="object-contain"
                  />
                </div>
                <p className="text-white/70 text-sm">一站式大数据平台部署与管理系统</p>
              </div>

              {/* 登录表单 */}
              <form onSubmit={handleLogin} className="space-y-5">
                {/* 用户名输入框 */}
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <User
                      className={`h-5 w-5 transition-colors duration-300 ${
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
                    className={`pl-12 h-12 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl transition-all duration-300 ${
                      focusedField === "username"
                        ? "bg-white/20 border-blue-400/50 shadow-lg"
                        : "hover:bg-white/15 hover:border-white/30"
                    }`}
                    required
                  />
                </div>

                {/* 密码输入框 */}
                <div className="relative">
                  <div className="absolute inset-y-0 left-0 pl-4 flex items-center pointer-events-none">
                    <Lock
                      className={`h-5 w-5 transition-colors duration-300 ${
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
                    className={`pl-12 pr-12 h-12 bg-white/10 border-white/20 text-white placeholder:text-white/50 rounded-2xl backdrop-blur-xl transition-all duration-300 ${
                      focusedField === "password"
                        ? "bg-white/20 border-blue-400/50 shadow-lg"
                        : "hover:bg-white/15 hover:border-white/30"
                    }`}
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
                  className="w-full h-12 bg-gradient-to-r from-blue-500 to-purple-600 hover:from-blue-600 hover:to-purple-700 text-white font-bold rounded-xl shadow-lg transition-all duration-300 border-0 mt-2"
                >
                  {isLoading ? (
                    <div className="flex items-center justify-center space-x-2">
                      <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                      <span>登录中...</span>
                    </div>
                  ) : (
                    <span className="flex items-center justify-center">登录 →</span>
                  )}
                </Button>
              </form>

              {/* 底部功能链接 */}
              <div className="flex justify-center space-x-6 mt-6 text-xs">
                <button className="text-white/60 hover:text-white transition-colors duration-200 flex items-center space-x-1">
                  <div className="w-1.5 h-1.5 bg-blue-400 rounded-full" />
                  <span className="ml-1">智能管理</span>
                </button>
                <button className="text-white/60 hover:text-white transition-colors duration-200 flex items-center space-x-1">
                  <div className="w-1.5 h-1.5 bg-purple-400 rounded-full" />
                  <span className="ml-1">高可用性</span>
                </button>
                <button className="text-white/60 hover:text-white transition-colors duration-200 flex items-center space-x-1">
                  <div className="w-1.5 h-1.5 bg-indigo-400 rounded-full" />
                  <span className="ml-1">多租户支持</span>
                </button>
              </div>
            </CardContent>
          </Card>
        </div>
        
        {/* 底部企业信息 - 还原为图片中的样式 */}
        <div className="text-center mt-auto pt-6">
          <p className="text-white/70 text-xs mb-2">北京中兵数科技集团有限公司 版权所有</p>
          <p className="text-white/70 text-xs mb-3">Copyright © {new Date().getFullYear()} Datasophon</p>
          <Image 
            src="/login-img/company.png" 
            alt="中兵数科" 
            width={80} 
            height={20}
            className="object-contain mx-auto"
          />
        </div>
      </div>
    </div>
  )
}
