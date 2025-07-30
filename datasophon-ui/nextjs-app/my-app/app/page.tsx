"use client"

import { useEffect } from 'react'
import { useRouter } from 'next/navigation'

export default function Home() {
  const router = useRouter()

  useEffect(() => {
    // 检查用户是否已登录
    const token = localStorage.getItem('jwt_token')
    
    if (token) {
      // 如果已登录，重定向到集群列表页面
      router.push('/clusters/list')
    } else {
      // 如果未登录，重定向到登录页面
      router.push('/login')
    }
  }, [router])

  // 显示加载中状态，防止页面闪烁
  return (
    <div className="min-h-screen flex items-center justify-center bg-gradient-to-br from-blue-50 to-indigo-100">
      <div className="text-center">
        <div className="animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600 mx-auto mb-4"></div>
        <p className="text-gray-600">正在加载...</p>
      </div>
    </div>
  )
}