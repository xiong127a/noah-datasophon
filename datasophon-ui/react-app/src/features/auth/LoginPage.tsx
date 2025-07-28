import { useState, useEffect, useRef } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { post } from '@/api/http';
import { setToken, setUserInfo } from '@/utils/auth';

// 登录表单验证模式
const loginSchema = z.object({
  username: z.string().min(1, '用户名不能为空'),
  password: z.string().min(1, '密码不能为空'),
  remember: z.boolean().optional(),
});

type LoginForm = z.infer<typeof loginSchema>;

const LoginPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  // 添加交互状态
  const [activeField, setActiveField] = useState<string | null>(null);
  const canvasRef = useRef<HTMLCanvasElement | null>(null);
  
  // 创建动态特效
  useEffect(() => {
    if (!canvasRef.current) return;
    
    const canvas = canvasRef.current;
    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // 设置canvas尺寸
    const setCanvasSize = () => {
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    setCanvasSize();
    window.addEventListener('resize', setCanvasSize);
    
    // 粒子系统
    const particles: Particle[] = [];
    const particleCount = 400; // 增加粒子数量，提高可玩性
    const connectionDistance = 180; // 增加连接距离
    const mousePosition = { x: 0, y: 0 };
    let animationFrame: number;
    
    class Particle {
      x: number;
      y: number;
      size: number;
      speedX: number;
      speedY: number;
      color: string;
      
      constructor() {
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        this.size = Math.random() * 2 + 0.5;
        this.speedX = (Math.random() - 0.5) * 0.8; // 加快粒子移动速度
        this.speedY = (Math.random() - 0.5) * 0.8;
        this.color = `rgba(255, 255, 255, ${Math.random() * 0.5 + 0.3})`;
      }
      
      update() {
        this.x += this.speedX;
        this.y += this.speedY;
        
        if (this.x > canvas.width) this.x = 0;
        else if (this.x < 0) this.x = canvas.width;
        
        if (this.y > canvas.height) this.y = 0;
        else if (this.y < 0) this.y = canvas.height;
      }
      
      draw() {
        if (!ctx) return;
        ctx.fillStyle = this.color;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
      }
    }
    
    // 创建初始粒子
    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle());
    }
    
    // 绘制连线
    const connect = (p1: Particle, p2: Particle, distance: number) => {
      if (!ctx) return;
      const opacity = 1 - distance / connectionDistance;
      ctx.strokeStyle = `rgba(255, 255, 255, ${opacity * 0.5})`;
      ctx.lineWidth = 0.5;
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
    };
    
    // 动画函数
    const animate = () => {
      if (!ctx) return;
      
      // 清空画布并创建背景渐变
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // 更新和绘制粒子
      particles.forEach(particle => {
        particle.update();
        particle.draw();
        
        // 绘制粒子之间的连接线
        particles.forEach(otherParticle => {
          if (particle === otherParticle) return;
          
          const dx = particle.x - otherParticle.x;
          const dy = particle.y - otherParticle.y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          
          if (distance < connectionDistance) {
            connect(particle, otherParticle, distance);
          }
        });
        
        // 与鼠标位置的连接线
        const dx = particle.x - mousePosition.x;
        const dy = particle.y - mousePosition.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < connectionDistance * 2) { // 增加与鼠标交互的范围
          // 绘制更亮的连接线到鼠标
          if (ctx) {
            const opacity = 1 - distance / (connectionDistance * 2);
            ctx.strokeStyle = `rgba(100, 180, 255, ${opacity * 0.8})`;
            ctx.lineWidth = 1;
            ctx.beginPath();
            ctx.moveTo(particle.x, particle.y);
            ctx.lineTo(mousePosition.x, mousePosition.y);
            ctx.stroke();
          }
          
          // 粒子朝鼠标方向移动
          particle.x += (mousePosition.x - particle.x) * 0.03; // 增加吸引力
          particle.y += (mousePosition.y - particle.y) * 0.03;
        }
      });
      
      animationFrame = requestAnimationFrame(animate);
    };
    
    // 鼠标移动事件
    const handleMouseMove = (e: MouseEvent) => {
      mousePosition.x = e.clientX;
      mousePosition.y = e.clientY;
    };
    
    window.addEventListener('mousemove', handleMouseMove);
    
    // 开始动画
    animate();
    
    // 清理函数
    return () => {
      cancelAnimationFrame(animationFrame);
      window.removeEventListener('mousemove', handleMouseMove);
      window.removeEventListener('resize', setCanvasSize);
    };
  }, []);
  
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    defaultValues: {
      username: '',
      password: '',
      remember: true,
    },
  });
  
  const onSubmit = async (data: LoginForm) => {
    try {
      setLoading(true);
      setError(null);
      
      // 调用登录API
      const response = await post('/login', {
        name: data.username,
        password: data.password,
      });
      
      // 处理登录成功
      setToken(response.token);
      setUserInfo({
        userId: response.userId,
        username: response.username,
        realName: response.realName,
        avatar: response.avatar,
        roles: response.roles || [],
        permissions: response.permissions || [],
      });
      
      // 跳转到首页
      navigate({ to: '/' });
    } catch (err: any) {
      setError(err.message || '登录失败，请稍后再试');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="min-h-screen w-full relative overflow-hidden">
      {/* 高科技动态背景 */}
      <canvas 
        ref={canvasRef} 
        className="absolute top-0 left-0 w-full h-full bg-gradient-to-bl from-blue-950 via-blue-900 to-indigo-900 z-0"
      />
      
      {/* 静态背景层 - 提供深度感 */}
      <div className="absolute inset-0 z-0 opacity-30">
        <div className="absolute w-full h-full bg-grid-white/[0.03] bg-[length:50px_50px]"></div>
        
        {/* 高科技圆环 */}
        <div className="absolute top-1/4 left-1/4 w-96 h-96 border border-blue-500/20 rounded-full animate-spin-slow"></div>
        <div className="absolute bottom-1/3 right-1/4 w-72 h-72 border-2 border-indigo-500/10 rounded-full animate-reverse-spin-slow"></div>
        
        {/* 高科技装饰线 */}
        <div className="absolute top-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-blue-500/50 to-transparent"></div>
        <div className="absolute bottom-0 left-0 w-full h-1 bg-gradient-to-r from-transparent via-blue-400/50 to-transparent"></div>
      </div>

      {/* 左上角公司logo */}
      <div className="absolute left-10 top-10 z-10">
        <div className="relative">
          <div className="absolute inset-0 bg-blue-500/20 rounded-full animate-pulse"></div>
          <img src="/company.png" alt="中兵数科" className="h-12 relative z-10 drop-shadow-blue-lg" />
        </div>
      </div>
      
      {/* 上方产品logo */}
      <div className="absolute left-1/2 -translate-x-1/2 top-24 z-10 flex flex-col items-center">
        <div className="relative">
          <div className="absolute -inset-4 bg-blue-500/10 rounded-full blur-md animate-pulse"></div>
          <img src="/product.png" alt="DataSophon" className="h-12 relative z-10 drop-shadow-blue-lg" />
        </div>
        <h1 className="mt-6 text-3xl font-bold bg-clip-text text-transparent bg-gradient-to-r from-blue-200 to-indigo-200">
          Noah大数据基础平台
        </h1>
        <div className="mt-2 flex items-center">
          <div className="h-0.5 w-10 bg-gradient-to-r from-transparent to-blue-400/50"></div>
          <div className="mx-2 w-2 h-2 bg-blue-400/70 rounded-full"></div>
          <div className="h-0.5 w-10 bg-gradient-to-l from-transparent to-blue-400/50"></div>
        </div>
      </div>

      {/* 登录卡片 */}
      <div className="absolute top-1/2 left-1/2 -translate-x-1/2 -translate-y-1/2 z-10 w-full max-w-md px-6">
        {/* 高科技登录框 */}
        <div className="relative">
          {/* 背景发光效果 */}
          <div className="absolute -inset-1 bg-gradient-to-r from-blue-600/20 to-indigo-600/20 rounded-3xl blur-lg"></div>
          
          {/* 登录卡片主体 */}
          <div className="relative bg-gradient-to-br from-slate-800/70 to-slate-900/70 backdrop-filter backdrop-blur-xl rounded-3xl overflow-hidden shadow-2xl border border-white/10">
            {/* 装饰线 */}
            <div className="absolute top-0 left-0 w-full h-0.5 bg-gradient-to-r from-transparent via-blue-500/50 to-transparent"></div>
            <div className="absolute bottom-0 left-0 w-full h-0.5 bg-gradient-to-r from-transparent via-blue-400/50 to-transparent"></div>
            
            {/* 角落装饰 */}
            <div className="absolute top-0 left-0 w-16 h-px bg-blue-500/50"></div>
            <div className="absolute top-0 left-0 h-16 w-px bg-blue-500/50"></div>
            <div className="absolute top-0 right-0 w-16 h-px bg-blue-500/50"></div>
            <div className="absolute top-0 right-0 h-16 w-px bg-blue-500/50"></div>
            <div className="absolute bottom-0 left-0 w-16 h-px bg-blue-500/50"></div>
            <div className="absolute bottom-0 left-0 h-16 w-px bg-blue-500/50"></div>
            <div className="absolute bottom-0 right-0 w-16 h-px bg-blue-500/50"></div>
            <div className="absolute bottom-0 right-0 h-16 w-px bg-blue-500/50"></div>
            
            {/* 内容区域 */}
            <div className="relative px-8 pt-10 pb-10">
              {/* 标题 */}
              <h2 className="text-center text-2xl font-medium text-white mb-8">
                欢迎<span className="text-blue-400">登录</span>
              </h2>
              
              {error && (
                <div className="mb-8 p-4 bg-red-900/40 backdrop-blur-md border border-red-500/30 text-red-200 rounded-2xl text-sm animate-pulse-slow">
                  <div className="flex items-center">
                    <div className="i-carbon-warning-filled w-5 h-5 mr-2 text-red-400"></div>
                    {error}
                  </div>
                </div>
              )}
              
              <form onSubmit={handleSubmit(onSubmit)} className="space-y-8">
                {/* 用户名输入框 */}
                <div className="space-y-2">
                  <div
                    className={`relative transition-all duration-300 ${
                      activeField === 'username' ? 'scale-[1.02]' : ''
                    }`}
                  >
                    {/* 背景发光效果 */}
                    <div className={`absolute -inset-0.5 bg-gradient-to-r from-blue-500/30 to-indigo-500/30 rounded-2xl blur-sm transition-opacity duration-300 ${
                      activeField === 'username' || errors.username ? 'opacity-100' : 'opacity-0'
                    }`}></div>
                    
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 flex items-center pl-5">
                        <div className={`i-carbon-user h-5 w-5 ${
                          activeField === 'username' ? 'text-blue-400' : 'text-blue-300/70'
                        }`}></div>
                      </div>
                      
                      <div className="absolute top-0 left-12 h-full w-px bg-white/10"></div>
                      
                      <input
                        {...register('username')}
                        className="w-full pl-16 pr-5 py-4 bg-slate-800/50 backdrop-filter backdrop-blur-md border border-slate-700/50 text-white rounded-2xl focus:outline-none"
                        placeholder="请输入用户名"
                        autoComplete="off"
                        onFocus={() => setActiveField('username')}
                        onBlur={() => setActiveField(null)}
                      />
                      
                      {/* 输入框内的高亮线效果 */}
                      <div className={`absolute bottom-0 left-0 w-full h-0.5 bg-gradient-to-r from-blue-500 to-indigo-500 transform transition-transform duration-300 ${
                        activeField === 'username' ? 'scale-x-100' : 'scale-x-0'
                      }`}></div>
                    </div>
                  </div>
                  {errors.username && (
                    <p className="pl-4 text-sm text-red-400">
                      <span className="i-carbon-warning inline-block mr-1 align-text-bottom"></span>
                      {errors.username.message}
                    </p>
                  )}
                </div>
                
                {/* 密码输入框 */}
                <div className="space-y-2">
                  <div
                    className={`relative transition-all duration-300 ${
                      activeField === 'password' ? 'scale-[1.02]' : ''
                    }`}
                  >
                    {/* 背景发光效果 */}
                    <div className={`absolute -inset-0.5 bg-gradient-to-r from-indigo-500/30 to-blue-500/30 rounded-2xl blur-sm transition-opacity duration-300 ${
                      activeField === 'password' || errors.password ? 'opacity-100' : 'opacity-0'
                    }`}></div>
                    
                    <div className="relative">
                      <div className="absolute inset-y-0 left-0 flex items-center pl-5">
                        <div className={`i-carbon-password h-5 w-5 ${
                          activeField === 'password' ? 'text-indigo-400' : 'text-indigo-300/70'
                        }`}></div>
                      </div>
                      
                      <div className="absolute top-0 left-12 h-full w-px bg-white/10"></div>
                      
                      <input
                        {...register('password')}
                        type="password"
                        className="w-full pl-16 pr-5 py-4 bg-slate-800/50 backdrop-filter backdrop-blur-md border border-slate-700/50 text-white rounded-2xl focus:outline-none"
                        placeholder="请输入密码"
                        autoComplete="current-password"
                        onFocus={() => setActiveField('password')}
                        onBlur={() => setActiveField(null)}
                      />
                      
                      {/* 输入框内的高亮线效果 */}
                      <div className={`absolute bottom-0 left-0 w-full h-0.5 bg-gradient-to-r from-indigo-500 to-blue-500 transform transition-transform duration-300 ${
                        activeField === 'password' ? 'scale-x-100' : 'scale-x-0'
                      }`}></div>
                    </div>
                  </div>
                  {errors.password && (
                    <p className="pl-4 text-sm text-red-400">
                      <span className="i-carbon-warning inline-block mr-1 align-text-bottom"></span>
                      {errors.password.message}
                    </p>
                  )}
                </div>
                
                {/* 记住密码和忘记密码 */}
                <div className="flex-between">
                  <div className="flex items-center">
                    <div className="relative">
                      <input
                        {...register('remember')}
                        id="remember"
                        type="checkbox"
                        className="peer sr-only"
                      />
                      <div className="w-5 h-5 border border-slate-600 rounded-md bg-slate-800/70 peer-checked:bg-blue-500/30 peer-checked:border-blue-400 transition-all duration-300"></div>
                      <div className="absolute top-0.5 left-0.5 text-white opacity-0 peer-checked:opacity-100 transition-all duration-300">
                        <div className="i-carbon-checkmark w-4 h-4"></div>
                      </div>
                    </div>
                    <label htmlFor="remember" className="ml-3 block text-sm text-slate-300 hover:text-white transition-colors">
                      记住我
                    </label>
                  </div>
                  
                  <div className="text-sm">
                    <a href="#" className="text-blue-400 hover:text-blue-300 transition duration-200">
                      忘记密码?
                    </a>
                  </div>
                </div>
                
                {/* 登录按钮 */}
                <div className="pt-2">
                  <button
                    type="submit"
                    disabled={loading}
                    className="tech-btn w-full relative overflow-hidden flex justify-center py-4 px-4 border border-blue-500/30 rounded-2xl shadow-lg text-sm font-medium text-white bg-gradient-to-r from-blue-600/40 to-indigo-600/40 hover:from-blue-500/40 hover:to-indigo-500/40 backdrop-filter backdrop-blur-md transition-all duration-300"
                  >
                    {/* 霓虹灯边框效果 */}
                    <div className="absolute inset-0 border border-blue-400/30 rounded-2xl glow-blue-sm"></div>
                    
                    {/* 按钮内部光效 */}
                    <div className="absolute inset-0 w-full h-full bg-gradient-to-r from-transparent via-white/10 to-transparent -translate-x-full hover:translate-x-full transition-all duration-1000 ease-in-out"></div>
                    
                    {loading ? (
                      <span className="flex items-center relative z-10">
                        <svg className="animate-spin -ml-1 mr-3 h-5 w-5 text-white" xmlns="http://www.w3.org/2000/svg" fill="none" viewBox="0 0 24 24">
                          <circle className="opacity-25" cx="12" cy="12" r="10" stroke="currentColor" strokeWidth="4"></circle>
                          <path className="opacity-75" fill="currentColor" d="M4 12a8 8 0 018-8V0C5.373 0 0 5.373 0 12h4zm2 5.291A7.962 7.962 0 014 12H0c0 3.042 1.135 5.824 3 7.938l3-2.647z"></path>
                        </svg>
                        登录中...
                      </span>
                    ) : (
                      <span className="relative z-10 flex items-center">
                        <span>登录系统</span>
                        <span className="ml-2 i-carbon-arrow-right"></span>
                      </span>
                    )}
                    
                    {/* 霓虹灯发光效果 */}
                    <div className="absolute -inset-0.5 bg-gradient-to-r from-blue-500/50 to-indigo-500/50 rounded-2xl blur opacity-0 group-hover:opacity-100 transition duration-300 -z-10"></div>
                  </button>
                </div>
              </form>
            </div>
          </div>
        </div>
      </div>
      
      {/* 底部版权信息 */}
      <div className="absolute bottom-16 left-1/2 -translate-x-1/2 text-center text-sm text-blue-200/70">
        <p>北京中兵数字科技集团有限公司 版权所有</p>
        <p className="mt-1">Copyright © {new Date().getFullYear()} DataSophon</p>
      </div>
      
      {/* 底部logo */}
      <div className="absolute bottom-5 left-1/2 -translate-x-1/2 opacity-70 hover:opacity-100 transition-opacity duration-300">
        <img src="/company.png" alt="中兵数科" className="h-6 drop-shadow-blue-sm" />
      </div>
    </div>
  );
};

export default LoginPage; 