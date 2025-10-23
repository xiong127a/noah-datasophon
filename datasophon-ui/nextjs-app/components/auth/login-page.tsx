"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { User, Lock, Eye, EyeOff } from "lucide-react";
import Image from "next/image";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import ParticleCanvas from "@/components/login/ParticleCanvas";
import LoginBackground from "@/components/login/LoginBackground";
import { apiClient, API_PATHS } from "@/lib/api"

// 退出登录函数 (全局可用)
export const logout = async () => {
  try {
    // 获取token用于调用退出接口
    const token = localStorage.getItem('jwt_token');
    if (token) {
      // 确保调用退出接口时带上token
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
      // 调用后端的登出接口
      await apiClient.post(API_PATHS.LOGOUT);
    }
  } catch (err) {
    console.error("退出登录时发生错误:", err);
  } finally {
    // 清除存储的令牌和用户信息
    if (typeof window !== 'undefined') {
      localStorage.removeItem('jwt_token');
      localStorage.removeItem('refresh_token');
      localStorage.removeItem('user_info');
      
      // 清除axios请求头
      delete apiClient.defaults.headers.common['Authorization'];
      
      // 重定向到登录页
      window.location.href = '/login';
    }
  }
};

export default function LoginPageNew() {
  // 设置默认登录凭据
  const [username, setUsername] = useState("admin");
  const [password, setPassword] = useState("admin123");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [activeField, setActiveField] = useState<string | null>(null);
  const [btnHover, setBtnHover] = useState(false);
  const [btnActive, setBtnActive] = useState(false);
  const [logoHover, setLogoHover] = useState(false);
  const [hoverTag, setHoverTag] = useState<string | null>(null);
  // 移除轮播旋转效果相关代码
  const [activeFeature, setActiveFeature] = useState<string | null>(null);
  const loginCardRef = useRef<HTMLDivElement>(null);
  const titleRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  // 检查是否已经登录
  useEffect(() => {
    const token = localStorage.getItem('jwt_token');
    if (token) {
      // 用户已登录，可以重定向到首页
      // router.push('/');
    }
  }, [router]);

  // 3D标题动画效果保留
  useEffect(() => {
    if (!titleRef.current) return;
    
    // 设置自动动画 - 更大幅度、更快速度
    const animateTitle = () => {
      if (!titleRef.current) return;
      
      const time = Date.now() / 1200; // 加快动画速度
      const moveX = Math.sin(time) * 12; // 增大旋转幅度
      const moveY = Math.cos(time * 1.2) * 8; // 增大旋转幅度
      const translateZ = Math.sin(time * 0.8) * 10 + 30; // 更大的Z轴移动
      
      titleRef.current.style.transform = `perspective(800px) rotateX(${moveY}deg) rotateY(${moveX}deg) translateZ(${translateZ}px) scale(1.05)`;
    };
    
    const interval = setInterval(animateTitle, 16); // 提高刷新率到约60fps
    return () => clearInterval(interval);
  }, []);

  // 处理JWT Token的保存
  const saveToken = (token: string, refreshToken: string) => {
    if (typeof window !== 'undefined') {
      // 保存到localStorage
      localStorage.setItem('jwt_token', token);
      localStorage.setItem('refresh_token', refreshToken);
      
      // 设置axios默认请求头
      apiClient.defaults.headers.common['Authorization'] = `Bearer ${token}`;
    }
  };

  // 处理用户信息的保存
  const saveUserInfo = (userInfo: any) => {
    if (typeof window !== 'undefined') {
      localStorage.setItem('user_info', JSON.stringify(userInfo));
    }
  };

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      // 校验用户输入
      if (!username.trim() || !password.trim()) {
        throw new Error("请输入用户名和密码");
      }

      // 发送登录请求
      const response = await apiClient.post(API_PATHS.LOGIN, {
        username,
        password
      });

      // 检查响应
      if (response.data && response.data.code === 200 && response.data.success) {
        // 登录成功
        const { token, refreshToken, user, roles } = response.data.data;
        
        // 保存JWT Token、刷新令牌和用户信息
        saveToken(token, refreshToken);
        saveUserInfo({
          ...user,
          roles
        });
        
        // 登录成功后跳转到集群列表页面
        router.push("/clusters/list");
      } else {
        // 处理业务逻辑错误
        throw new Error(response.data?.meta?.msg || response.data?.msg || "登录失败，请检查用户名和密码");
      }
    } catch (err: any) {
      console.error("登录失败:", err);
      
      // 处理不同类型的错误
      let errorMessage = "登录失败，请稍后再试";
      
      if (err.response) {
        // 服务器响应了错误状态码
        if (err.response.status === 401) {
          errorMessage = "用户名或密码不正确";
        } else if (err.response.status === 403) {
          errorMessage = "账号已被锁定，请联系管理员";
        } else if (err.response.data) {
          // 尝试从后端错误响应提取消息
          errorMessage = err.response.data?.meta?.msg || 
                         err.response.data?.msg ||
                         err.response.data?.message || 
                         "登录验证失败";
        }
      } else if (err.message) {
        // 请求未发送成功或其他客户端错误
        errorMessage = err.message;
      }
      
      setError(errorMessage);
      
      // 添加卡片震动效果
      if (loginCardRef.current) {
        loginCardRef.current.classList.add("animate-shake");
        setTimeout(() => {
          if (loginCardRef.current) {
            loginCardRef.current.classList.remove("animate-shake");
          }
        }, 820);
      }
    } finally {
      setIsLoading(false);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  // 扩展功能列表 - 删除一键启停功能点
  const features = [
    { id: "manage", name: "智能管理", icon: "🧠", color: "blue" },
    { id: "ha", name: "高可用性", icon: "⚡", color: "purple" },
    { id: "tenant", name: "多租户支持", icon: "👥", color: "indigo" },
    { id: "cluster", name: "多集群支持", icon: "🌐", color: "cyan" },
    { id: "visual", name: "可视化配置", icon: "📊", color: "teal" },
    { id: "deploy", name: "一键部署", icon: "🚀", color: "orange" },
  ];

  return (
    <div className="fixed inset-0 flex items-center justify-center overflow-hidden">
      <LoginBackground />
      <ParticleCanvas />
      
      {/* 高科技装饰线和扫描效果 */}
      <div className="fixed inset-0 z-0 overflow-hidden">
        {/* 水平扫描线 */}
        <div className="absolute left-0 right-0 h-px bg-blue-400/20 animate-scan-vertical"></div>
        
        {/* 垂直扫描线 */}
        <div className="absolute top-0 bottom-0 w-px bg-blue-400/20 animate-scan-horizontal"></div>
        
        {/* 角落装饰 */}
        <div className="absolute top-0 left-0 w-24 h-24 border-l-2 border-t-2 border-blue-400/30"></div>
        <div className="absolute top-0 right-0 w-24 h-24 border-r-2 border-t-2 border-purple-400/30"></div>
        <div className="absolute bottom-0 left-0 w-24 h-24 border-l-2 border-b-2 border-cyan-400/30"></div>
        <div className="absolute bottom-0 right-0 w-24 h-24 border-r-2 border-b-2 border-indigo-400/30"></div>
        
        {/* 雷达扫描效果 */}
        <div className="absolute bottom-16 right-16 w-64 h-64 rounded-full border border-blue-400/20">
          <div className="absolute inset-0 rounded-full border border-blue-400/10"></div>
          <div className="absolute inset-4 rounded-full border border-blue-400/15"></div>
          <div className="absolute inset-8 rounded-full border border-blue-400/10"></div>
          <div className="absolute inset-0 origin-center rounded-full animate-radar-beam"></div>
        </div>
      </div>
      
      <div className="absolute left-8 top-8 z-10">
        <Image 
          src="/images/login/company.png" 
          alt="中兵数科" 
          width={120} 
          height={40} 
          className="h-10 w-auto opacity-80" 
        />
      </div>
      
      {/* 增强3D标题效果 */}
      <div 
        ref={titleRef}
        className="absolute top-14 left-1/2 transform -translate-x-1/2 z-10 transition-transform duration-100"
      >
        <div className="relative">
          {/* 强化光晕效果 */}
          <div className="absolute -inset-8 bg-blue-500/10 rounded-full blur-xl"></div>
          <div className="absolute -inset-4 bg-indigo-500/5 rounded-full blur-3xl animate-pulse-slow"></div>
          
          {/* 增强3D文字效果 */}
          <h1 className="text-5xl font-bold tracking-wide bg-gradient-to-r from-blue-300 via-indigo-300 to-blue-400 bg-clip-text text-transparent relative">
            Noah大数据基础平台
            {/* 文字阴影层 */}
            <span className="absolute inset-0 bg-gradient-to-r from-blue-300 via-indigo-300 to-blue-400 bg-clip-text text-transparent blur-sm opacity-70 translate-y-[2px] translate-x-[2px]"></span>
            <span className="absolute inset-0 bg-gradient-to-r from-blue-300 via-indigo-300 to-blue-400 bg-clip-text text-transparent blur-md opacity-50 translate-y-[4px] translate-x-[4px]"></span>
            <span className="absolute inset-0 bg-gradient-to-r from-blue-300 via-indigo-300 to-blue-400 bg-clip-text text-transparent blur-lg opacity-30 translate-y-[6px] translate-x-[6px]"></span>
          </h1>
          
          {/* 增强下划线效果 */}
          <div className="h-px bg-gradient-to-r from-transparent via-blue-400 to-transparent w-full mt-2 animate-shimmer"></div>
          <div className="h-px bg-gradient-to-r from-transparent via-indigo-400/70 to-transparent w-4/5 mx-auto mt-1 animate-shimmer delay-150"></div>
          
          {/* 科技感装饰增强 */}
          <div className="absolute -right-16 -top-12 w-12 h-12 border-t-2 border-r-2 border-blue-400/40 opacity-70"></div>
          <div className="absolute -left-16 -bottom-6 w-12 h-12 border-b-2 border-l-2 border-indigo-400/40 opacity-70"></div>
          <div className="absolute -right-4 -bottom-8 w-8 h-8 border-b border-r border-blue-400/30 opacity-50"></div>
          <div className="absolute -left-4 -top-8 w-8 h-8 border-t border-l border-indigo-400/30 opacity-50"></div>
          
          {/* 数字雨点效果 */}
          <div className="absolute -right-24 top-0 bottom-0 w-12 text-xs text-blue-400/30 overflow-hidden">
            <div className="animate-matrix-code">10010110<br/>01101001<br/>11001010<br/>00101101<br/>10110010<br/>01001011</div>
          </div>
          <div className="absolute -left-24 top-0 bottom-0 w-12 text-xs text-indigo-400/30 overflow-hidden">
            <div className="animate-matrix-code-slow">01101001<br/>10010110<br/>00101101<br/>11001010<br/>01001011<br/>10110010</div>
          </div>
        </div>
      </div>

      <div
        ref={loginCardRef}
        className="w-full max-w-md z-50 transition-all duration-300 animate-scale-in"
      >
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl border border-white/20 shadow-2xl p-10 transition-all duration-300 hover:shadow-blue-500/10 relative overflow-hidden">
          {/* 动态边框光效 */}
          <div className="absolute inset-0 rounded-3xl border-2 border-transparent bg-transparent">
            <div className="absolute inset-[-2px] rounded-3xl bg-gradient-to-r from-blue-500/30 via-indigo-500/30 to-purple-500/30 animate-border-flow"></div>
          </div>
          
          {/* 卡片内部光晕 */}
          <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
          
          <div className="text-center mb-8">
            <div 
              className="relative mx-auto mb-4 w-18 h-18 transition-transform duration-300 hover:scale-110"
              onMouseEnter={() => setLogoHover(true)}
              onMouseLeave={() => setLogoHover(false)}
            >
              <div className={`absolute -inset-3 border border-blue-500/10 rounded-full transition-all duration-500 ${logoHover ? 'border-blue-500/30 animate-spin-slow' : ''}`}>
                {[0, 90, 180, 270].map((deg) => (
                  <div 
                    key={deg}
                    className={`absolute w-1.5 h-1.5 bg-blue-400/50 rounded-full top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 opacity-0 transition-opacity duration-300 ${logoHover ? 'opacity-100' : ''}`}
                    style={{ 
                      transform: `translate(-50%, -50%) rotate(${deg}deg) translateX(46px)` 
                    }}
                  ></div>
                ))}
              </div>
              
              <div className="relative w-20 h-20 mx-auto flex items-center justify-center">
                <div className="absolute inset-0 bg-gradient-to-br from-blue-500/30 to-purple-600/30 rounded-2xl blur-lg"></div>
                <Image 
                  src="/images/login/logo.svg" 
                  alt="Datasophon Logo" 
                  width={70} 
                  height={70}
                  className={`relative z-10 transition-transform duration-300 ${logoHover ? 'scale-110' : ''}`} 
                />
              </div>
            </div>
            
            <p className="text-white/70 text-sm mt-3">
              一站式大数据平台部署与管理系统
            </p>
            <div className="mt-2 h-0.5 bg-gradient-to-r from-transparent via-blue-400/40 to-transparent"></div>
          </div>
          
          {error && (
            <div className="mb-6 rounded-xl overflow-hidden relative">
              <div className="p-4 border border-red-500/30 bg-gradient-to-r from-red-500/10 to-red-900/10 backdrop-blur-lg relative z-10 flex items-center">
                <svg className="w-5 h-5 mr-3 text-red-400" fill="currentColor" viewBox="0 0 20 20" xmlns="http://www.w3.org/2000/svg">
                  <path fillRule="evenodd" d="M8.257 3.099c.765-1.36 2.722-1.36 3.486 0l5.58 9.92c.75 1.334-.213 2.98-1.742 2.98H4.42c-1.53 0-2.493-1.646-1.743-2.98l5.58-9.92zM11 13a1 1 0 11-2 0 1 1 0 012 0zm-1-8a1 1 0 00-1 1v3a1 1 0 002 0V6a1 1 0 00-1-1z" clipRule="evenodd" />
                </svg>
                <span className="text-red-200 text-sm">{error}</span>
              </div>
              <div className="absolute inset-0 bg-red-500/5 blur-xl"></div>
            </div>
          )}
          
          <form onSubmit={handleLogin} className="space-y-5">
            <div className="relative">
              <div className={`
                relative rounded-xl transition-all duration-300 overflow-hidden
                ${activeField === "username" ? "bg-blue-900/30 shadow-md shadow-blue-900/20 scale-105" : "bg-white/5 hover:bg-white/10 hover:scale-102"}
              `}>
                <div className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none">
                  <User className={`
                    w-5 h-5 transition-all duration-300
                    ${activeField === "username" ? "text-blue-400 scale-125" : "text-white/60"}
                  `} />
                </div>
                
                <div className={`
                  absolute pointer-events-none left-12 transition-all duration-300
                  ${username || activeField === "username" 
                    ? "top-2 text-xs text-blue-300" 
                    : "top-1/2 -translate-y-1/2 text-sm text-white/60"}
                `}>
                  用户名
                </div>
                
                <input
                  type="text"
                  autoComplete="username"
                  value={username}
                  onChange={(e) => setUsername(e.target.value)}
                  className={`
                    w-full bg-transparent border-0 outline-none rounded-xl
                    ${username || activeField === "username" ? "pt-8 pb-4" : "py-6"}
                    px-12 text-white placeholder-white/30
                  `}
                  placeholder={activeField === "username" ? "请输入用户名" : ""}
                  onFocus={() => setActiveField("username")}
                  onBlur={() => setActiveField(null)}
                />
                
                <div className={`
                  absolute bottom-0 left-3 right-3 h-px transition-transform duration-300 origin-left
                  ${activeField === "username" ? "scale-x-100 bg-blue-400" : "scale-x-0 bg-white/20"}
                `}></div>
              </div>
            </div>
            
            <div className="relative">
              <div className={`
                relative rounded-xl transition-all duration-300 overflow-hidden
                ${activeField === "password" ? "bg-blue-900/30 shadow-md shadow-blue-900/20 scale-105" : "bg-white/5 hover:bg-white/10 hover:scale-102"}
              `}>
                <div className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none">
                  <Lock className={`
                    w-5 h-5 transition-all duration-300
                    ${activeField === "password" ? "text-blue-400 scale-125" : "text-white/60"}
                  `} />
                </div>
                
                <div className={`
                  absolute pointer-events-none left-12 transition-all duration-300
                  ${password || activeField === "password" 
                    ? "top-2 text-xs text-blue-300" 
                    : "top-1/2 -translate-y-1/2 text-sm text-white/60"}
                `}>
                  密码
                </div>
                
                <input
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  value={password}
                  onChange={(e) => setPassword(e.target.value)}
                  className={`
                    w-full bg-transparent border-0 outline-none rounded-xl
                    ${password || activeField === "password" ? "pt-8 pb-4" : "py-6"}
                    px-12 pr-12 text-white placeholder-white/30
                  `}
                  placeholder={activeField === "password" ? "请输入密码" : ""}
                  onFocus={() => setActiveField("password")}
                  onBlur={() => setActiveField(null)}
                />
                
                <div 
                  className="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer hover:scale-110 transition-transform"
                  onClick={togglePasswordVisibility}
                >
                  {showPassword ? (
                    <EyeOff className="w-5 h-5 text-white/60 hover:text-blue-400 transition-colors duration-300" />
                  ) : (
                    <Eye className="w-5 h-5 text-white/60 hover:text-blue-400 transition-colors duration-300" />
                  )}
                </div>
                
                <div className={`
                  absolute bottom-0 left-3 right-3 h-px transition-transform duration-300 origin-left
                  ${activeField === "password" ? "scale-x-100 bg-blue-400" : "scale-x-0 bg-white/20"}
                `}></div>
              </div>
            </div>

            <div className="mt-6">
              <Button
                type="submit"
                disabled={isLoading}
                onMouseEnter={() => setBtnHover(true)}
                onMouseLeave={() => setBtnHover(false)}
                onMouseDown={() => setBtnActive(true)}
                onMouseUp={() => setBtnActive(false)}
                className={`
                  relative w-full py-7 bg-gradient-to-r from-blue-500 to-purple-600 rounded-xl shadow-lg border-0
                  transition-all duration-300 overflow-hidden
                  ${btnHover ? "shadow-blue-500/25" : ""}
                  ${btnActive ? "scale-98" : btnHover ? "scale-102" : ""}
                `}
              >
                {/* 按钮内部扫描线 */}
                <div className="absolute inset-0 overflow-hidden">
                  <div className="absolute top-0 -left-full right-full h-px bg-white/50 animate-scan-btn"></div>
                </div>
                
                {/* 按钮光晕效果 */}
                <div className={`
                  absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent transform translate-x-[-100%] transition-transform duration-1000
                  ${btnHover ? "translate-x-[100%]" : ""}
                `} />
                
                {/* 按钮内容 */}
                {isLoading ? (
                  <div className="flex items-center justify-center space-x-2">
                    <div className="w-5 h-5 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    <span className="text-white font-semibold">登录中...</span>
                  </div>
                ) : (
                  <div className={`
                    flex items-center justify-center space-x-1 transition-transform duration-300
                    ${btnHover ? "translate-x-1" : ""}
                  `}>
                    <span className="text-white font-semibold">登录</span>
                    <svg className="w-5 h-5 text-white transform transition-transform duration-300" fill="none" viewBox="0 0 24 24" stroke="currentColor">
                      <path strokeLinecap="round" strokeLinejoin="round" strokeWidth={2} d="M14 5l7 7m0 0l-7 7m7-7H3" />
                    </svg>
                  </div>
                )}
              </Button>
            </div>
          </form>

          {/* 简单网格布局的功能点展示 - 减小尺寸，更紧凑 */}
          <div className="mt-4 grid grid-cols-3 gap-2">
            {features.map((feature) => (
              <div
                key={feature.id}
                className={`
                  p-2 rounded-lg cursor-pointer transition-all duration-300 relative overflow-hidden
                  bg-white/5 hover:bg-white/10 hover:scale-105
                  ${activeFeature === feature.id ? 'bg-gradient-to-br shadow-lg scale-105' : ''}
                  ${activeFeature === feature.id && feature.color === 'blue' ? 'from-blue-800/20 to-blue-900/20 shadow-blue-500/20' : ''}
                  ${activeFeature === feature.id && feature.color === 'purple' ? 'from-purple-800/20 to-purple-900/20 shadow-purple-500/20' : ''}
                  ${activeFeature === feature.id && feature.color === 'indigo' ? 'from-indigo-800/20 to-indigo-900/20 shadow-indigo-500/20' : ''}
                  ${activeFeature === feature.id && feature.color === 'cyan' ? 'from-cyan-800/20 to-cyan-900/20 shadow-cyan-500/20' : ''}
                  ${activeFeature === feature.id && feature.color === 'teal' ? 'from-teal-800/20 to-teal-900/20 shadow-teal-500/20' : ''}
                  ${activeFeature === feature.id && feature.color === 'orange' ? 'from-orange-800/20 to-orange-900/20 shadow-orange-500/20' : ''}
                  group
                `}
                onMouseEnter={() => setActiveFeature(feature.id)}
                onMouseLeave={() => setActiveFeature(null)}
              >
                {/* 背景光效 */}
                <div className={`
                  absolute inset-0 opacity-0 transition-opacity duration-500 bg-gradient-to-b 
                  ${activeFeature === feature.id ? 'opacity-100' : 'group-hover:opacity-50'}
                  ${feature.color === 'blue' ? 'from-blue-500/5 to-transparent' : ''}
                  ${feature.color === 'purple' ? 'from-purple-500/5 to-transparent' : ''}
                  ${feature.color === 'indigo' ? 'from-indigo-500/5 to-transparent' : ''}
                  ${feature.color === 'cyan' ? 'from-cyan-500/5 to-transparent' : ''}
                  ${feature.color === 'teal' ? 'from-teal-500/5 to-transparent' : ''}
                  ${feature.color === 'orange' ? 'from-orange-500/5 to-transparent' : ''}
                `}></div>
                
                <div className="flex items-center space-x-2 z-10 relative">
                  <div className={`
                    w-6 h-6 rounded-lg flex items-center justify-center transition-all duration-300
                    bg-white/5 group-hover:bg-white/10
                    ${activeFeature === feature.id ? 'scale-110 bg-white/10' : ''}
                    ${feature.color === 'blue' && activeFeature === feature.id ? 'text-blue-300' : ''}
                    ${feature.color === 'purple' && activeFeature === feature.id ? 'text-purple-300' : ''}
                    ${feature.color === 'indigo' && activeFeature === feature.id ? 'text-indigo-300' : ''}
                    ${feature.color === 'cyan' && activeFeature === feature.id ? 'text-cyan-300' : ''}
                    ${feature.color === 'teal' && activeFeature === feature.id ? 'text-teal-300' : ''}
                    ${feature.color === 'orange' && activeFeature === feature.id ? 'text-orange-300' : ''}
                    ${!activeFeature ? 'text-white/60' : ''}
                  `}>
                    <span className="text-sm">{feature.icon}</span>
                  </div>
                  <div>
                    <p className={`
                      text-xs font-medium transition-all duration-300
                      ${activeFeature === feature.id ? 'text-white' : 'text-white/70 group-hover:text-white/90'}
                    `}>
                      {feature.name}
                    </p>
                    
                    {/* 底部装饰线 */}
                    <div className={`
                      h-0.5 transition-all duration-300 mt-0.5
                      ${activeFeature === feature.id ? 'w-full' : 'w-0 group-hover:w-4/5'}
                      ${feature.color === 'blue' ? 'bg-blue-400/70' : ''}
                      ${feature.color === 'purple' ? 'bg-purple-400/70' : ''}
                      ${feature.color === 'indigo' ? 'bg-indigo-400/70' : ''}
                      ${feature.color === 'cyan' ? 'bg-cyan-400/70' : ''}
                      ${feature.color === 'teal' ? 'bg-teal-400/70' : ''}
                      ${feature.color === 'orange' ? 'bg-orange-400/70' : ''}
                    `}></div>
                  </div>
                </div>
              </div>
            ))}
          </div>
        </div>
      </div>

      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 text-center">
        <div className="text-white/60 text-sm mb-2">
          <p>北京中兵数字科技集团有限公司 版权所有</p>
          <p className="mt-1">Copyright © {new Date().getFullYear()} Datasophon</p>
        </div>
        <div className="w-20 h-6 relative mx-auto">
          <Image 
            src="/images/login/company.png" 
            alt="中兵数科" 
            fill
            className="object-contain opacity-60"
            quality={100}
          />
        </div>
      </div>
    </div>
  );
} 