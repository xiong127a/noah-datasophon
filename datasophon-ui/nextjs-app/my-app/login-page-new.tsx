"use client";

import { useState, useRef, useEffect } from "react";
import { useRouter } from "next/navigation";
import { User, Lock, Eye, EyeOff } from "lucide-react";
import Image from "next/image";

import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import ParticleCanvas from "@/components/login/ParticleCanvas";
import LoginBackground from "@/components/login/LoginBackground";

export default function LoginPageNew() {
  const [username, setUsername] = useState("");
  const [password, setPassword] = useState("");
  const [showPassword, setShowPassword] = useState(false);
  const [isLoading, setIsLoading] = useState(false);
  const [error, setError] = useState("");
  const [activeField, setActiveField] = useState<string | null>(null);
  const [btnHover, setBtnHover] = useState(false);
  const [btnActive, setBtnActive] = useState(false);
  const [logoHover, setLogoHover] = useState(false);
  const [hoverTag, setHoverTag] = useState<string | null>(null);
  const [activeFeature, setActiveFeature] = useState<string | null>(null);
  const loginCardRef = useRef<HTMLDivElement>(null);
  const titleRef = useRef<HTMLDivElement>(null);
  const featuresContainerRef = useRef<HTMLDivElement>(null);
  const router = useRouter();

  // 3D标题自动动画效果
  useEffect(() => {
    if (!titleRef.current) return;
    
    // 设置自动动画
    const animateTitle = () => {
      if (!titleRef.current) return;
      
      const time = Date.now() / 2000;
      const moveX = Math.sin(time) * 8; // 增大旋转幅度
      const moveY = Math.cos(time * 0.8) * 5; // 增大旋转幅度
      const translateZ = Math.sin(time * 0.5) * 5 + 20; // 添加Z轴移动
      
      titleRef.current.style.transform = `perspective(800px) rotateX(${moveY}deg) rotateY(${moveX}deg) translateZ(${translateZ}px)`;
    };
    
    const interval = setInterval(animateTitle, 30); // 提高刷新率
    return () => clearInterval(interval);
  }, []);
  
  // 功能点滚动效果
  useEffect(() => {
    if (!featuresContainerRef.current) return;
    
    const features = featuresContainerRef.current;
    let animationFrameId: number;
    let scrollPosition = 0;
    let isPaused = false;
    
    const scrollFeatures = () => {
      if (!isPaused && features) {
        scrollPosition += 0.5;
        if (scrollPosition >= features.scrollWidth / 2) {
          scrollPosition = 0;
        }
        features.scrollLeft = scrollPosition;
      }
      animationFrameId = requestAnimationFrame(scrollFeatures);
    };
    
    // 鼠标进入暂停滚动
    const handleMouseEnter = () => {
      isPaused = true;
    };
    
    // 鼠标离开继续滚动
    const handleMouseLeave = () => {
      isPaused = false;
    };
    
    features.addEventListener('mouseenter', handleMouseEnter);
    features.addEventListener('mouseleave', handleMouseLeave);
    
    animationFrameId = requestAnimationFrame(scrollFeatures);
    
    return () => {
      cancelAnimationFrame(animationFrameId);
      features.removeEventListener('mouseenter', handleMouseEnter);
      features.removeEventListener('mouseleave', handleMouseLeave);
    };
  }, []);

  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault();
    setIsLoading(true);
    setError("");

    try {
      // 模拟登录延迟
      await new Promise((resolve) => setTimeout(resolve, 1000));

      // 开发阶段：任何用户名密码都可以登录
      if (username && password) {
        router.push("/");
      } else {
        throw new Error("请输入用户名和密码");
      }
    } catch (err: any) {
      setError(err.message || "登录失败，请稍后再试");
      
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

  // 扩展功能列表
  const features = [
    { id: "manage", name: "智能管理", icon: "🧠", color: "blue" },
    { id: "ha", name: "高可用性", icon: "⚡", color: "purple" },
    { id: "tenant", name: "多租户支持", icon: "👥", color: "indigo" },
    { id: "cluster", name: "多集群支持", icon: "🌐", color: "cyan" },
    { id: "visual", name: "可视化配置", icon: "📊", color: "teal" },
    { id: "deploy", name: "一键部署", icon: "🚀", color: "orange" },
    { id: "control", name: "一键启停", icon: "⏯️", color: "rose" },
  ];

  return (
    <div className="fixed inset-0 flex items-center justify-center overflow-hidden">
      <LoginBackground />
      <ParticleCanvas />
      
      <div className="absolute left-8 top-8 z-10">
        <Image 
          src="/login-img/company.png" 
          alt="中兵数科" 
          width={120} 
          height={40} 
          className="h-10 w-auto opacity-80" 
        />
      </div>
      
      {/* 增强3D标题效果 */}
      <div 
        ref={titleRef}
        className="absolute top-14 left-1/2 transform -translate-x-1/2 z-10 transition-transform duration-200"
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
        </div>
      </div>

      <div
        ref={loginCardRef}
        className="w-full max-w-md z-50 transition-all duration-300 animate-scale-in"
      >
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl border border-white/20 shadow-2xl p-10 transition-all duration-300 hover:shadow-blue-500/10 relative overflow-hidden">
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
                  src="/login-img/logo.svg" 
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
                    ${username || activeField === "username" ? "pt-7 pb-3" : "py-5"}
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
                    ${password || activeField === "password" ? "pt-7 pb-3" : "py-5"}
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
                  relative w-full py-6 bg-gradient-to-r from-blue-500 to-purple-600 rounded-xl shadow-lg border-0
                  transition-all duration-300 overflow-hidden
                  ${btnHover ? "shadow-blue-500/25" : ""}
                  ${btnActive ? "scale-98" : btnHover ? "scale-102" : ""}
                `}
              >
                <div className={`
                  absolute inset-0 bg-gradient-to-r from-transparent via-white/20 to-transparent transform translate-x-[-100%] transition-transform duration-1000
                  ${btnHover ? "translate-x-[100%]" : ""}
                `} />
                
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

          {/* 全新优化的功能点展示 */}
          <div className="mt-6 relative overflow-hidden">
            {/* 渐变阴影效果 */}
            <div className="absolute left-0 top-0 bottom-0 w-8 z-10 bg-gradient-to-r from-[rgba(15,23,42,0.8)] to-transparent pointer-events-none"></div>
            <div className="absolute right-0 top-0 bottom-0 w-8 z-10 bg-gradient-to-l from-[rgba(15,23,42,0.8)] to-transparent pointer-events-none"></div>
            
            {/* 滚动容器 */}
            <div 
              ref={featuresContainerRef}
              className="flex overflow-x-auto pb-2 pt-1 scrollbar-hide snap-x"
              style={{ scrollbarWidth: 'none', msOverflowStyle: 'none' }}
            >
              {/* 添加重复功能点实现无缝滚动 */}
              {[...features, ...features].map((feature, index) => (
                <div
                  key={`${feature.id}-${index}`}
                  className="snap-center shrink-0 first:pl-2 last:pr-2"
                >
                  <div
                    className={`
                      px-3 py-1.5 mx-1 rounded-lg cursor-pointer transition-all duration-300 relative overflow-hidden
                      ${activeFeature === feature.id ? 'shadow-sm' : ''}
                      ${activeFeature === feature.id ? `shadow-${feature.color}-500/30` : ''}
                      group
                    `}
                    onMouseEnter={() => setActiveFeature(feature.id)}
                    onMouseLeave={() => setActiveFeature(null)}
                  >
                    <div className={`
                      absolute inset-0 opacity-0 transition-all duration-300 
                      ${activeFeature === feature.id ? 'opacity-100' : ''}
                      ${feature.color === 'blue' ? 'bg-gradient-to-r from-blue-900/30 to-blue-700/10' : ''}
                      ${feature.color === 'purple' ? 'bg-gradient-to-r from-purple-900/30 to-purple-700/10' : ''}
                      ${feature.color === 'indigo' ? 'bg-gradient-to-r from-indigo-900/30 to-indigo-700/10' : ''}
                      ${feature.color === 'cyan' ? 'bg-gradient-to-r from-cyan-900/30 to-cyan-700/10' : ''}
                      ${feature.color === 'teal' ? 'bg-gradient-to-r from-teal-900/30 to-teal-700/10' : ''}
                      ${feature.color === 'orange' ? 'bg-gradient-to-r from-orange-900/30 to-orange-700/10' : ''}
                      ${feature.color === 'rose' ? 'bg-gradient-to-r from-rose-900/30 to-rose-700/10' : ''}
                    `}></div>
                    
                    <div className="flex items-center relative z-10">
                      <span className={`
                        flex items-center justify-center w-4 h-4 rounded transition-all duration-300
                        ${activeFeature === feature.id ? 'scale-110 mr-2' : 'mr-1.5 opacity-80'}
                        ${feature.color === 'blue' && activeFeature === feature.id ? 'text-blue-300' : ''}
                        ${feature.color === 'purple' && activeFeature === feature.id ? 'text-purple-300' : ''}
                        ${feature.color === 'indigo' && activeFeature === feature.id ? 'text-indigo-300' : ''}
                        ${feature.color === 'cyan' && activeFeature === feature.id ? 'text-cyan-300' : ''}
                        ${feature.color === 'teal' && activeFeature === feature.id ? 'text-teal-300' : ''}
                        ${feature.color === 'orange' && activeFeature === feature.id ? 'text-orange-300' : ''}
                        ${feature.color === 'rose' && activeFeature === feature.id ? 'text-rose-300' : ''}
                      `}
                      style={{ fontSize: '10px' }}
                      >
                        {feature.icon}
                      </span>
                      <span className={`
                        text-xs font-medium whitespace-nowrap transition-colors duration-300
                        ${activeFeature === feature.id ? 'text-white' : 'text-white/70'}
                      `}>
                        {feature.name}
                      </span>
                    </div>
                    
                    {/* 底部动画线条 */}
                    <div className={`
                      absolute bottom-0 left-0 h-[2px] transition-all duration-300
                      ${activeFeature === feature.id ? 'w-full' : 'w-0'}
                      ${feature.color === 'blue' ? 'bg-gradient-to-r from-blue-400 to-blue-500/50' : ''}
                      ${feature.color === 'purple' ? 'bg-gradient-to-r from-purple-400 to-purple-500/50' : ''}
                      ${feature.color === 'indigo' ? 'bg-gradient-to-r from-indigo-400 to-indigo-500/50' : ''}
                      ${feature.color === 'cyan' ? 'bg-gradient-to-r from-cyan-400 to-cyan-500/50' : ''}
                      ${feature.color === 'teal' ? 'bg-gradient-to-r from-teal-400 to-teal-500/50' : ''}
                      ${feature.color === 'orange' ? 'bg-gradient-to-r from-orange-400 to-orange-500/50' : ''}
                      ${feature.color === 'rose' ? 'bg-gradient-to-r from-rose-400 to-rose-500/50' : ''}
                    `}></div>
                  </div>
                </div>
              ))}
            </div>
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
            src="/login-img/company.png" 
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