import { useState, useRef, useEffect } from 'react';
import { useForm } from 'react-hook-form';
import { z } from 'zod';
import { useNavigate } from '@tanstack/react-router';
import { post } from '@/api/http';
import { setToken, setUserInfo } from '@/utils/auth';

// 登录表单验证模式
const loginSchema = z.object({
  username: z.string().min(1, '请输入用户名'),
  password: z.string().min(1, '请输入密码'),
});

type LoginFormData = z.infer<typeof loginSchema>;

const LoginPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState('');
  const [activeField, setActiveField] = useState<string | null>(null);
  const [btnHover, setBtnHover] = useState(false);
  const [btnActive, setBtnActive] = useState(false);
  const [showPassword, setShowPassword] = useState(false);
  const [logoHover, setLogoHover] = useState(false);
  const [hoverTag, setHoverTag] = useState<string | null>(null);
  const canvasRef = useRef<HTMLCanvasElement>(null);
  const containerRef = useRef<HTMLDivElement>(null);
  const loginCardRef = useRef<HTMLDivElement>(null);

  const {
    register,
    handleSubmit,
    watch,
    formState: { errors },
  } = useForm<LoginFormData>();

  const usernameValue = watch('username');
  const passwordValue = watch('password');

  // 线束效果
  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext('2d');
    if (!ctx) return;
    
    // 设置Canvas尺寸
    const handleResize = () => {
      if (!canvas) return;
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    
    handleResize();
    window.addEventListener('resize', handleResize);
    
    // 创建粒子
    const particles: Particle[] = [];
    const particleCount = 200; // 调整粒子数量
    const connectionDistance = 160; // 增大连接距离
    const mousePosition = { x: window.innerWidth / 2, y: window.innerHeight / 2 };
    let animationFrame: number;
    
    class Particle {
      x: number = 0;
      y: number = 0;
      speedX: number = 0;
      speedY: number = 0;
      size: number = 1;
      color: string = 'rgba(255, 255, 255, 0.5)';
      
      constructor() {
        if (!canvas) return; // 安全检查
        this.x = Math.random() * canvas.width;
        this.y = Math.random() * canvas.height;
        this.speedX = (Math.random() - 0.5) * 0.5; // 降低速度
        this.speedY = (Math.random() - 0.5) * 0.5; // 降低速度
        this.size = Math.random() * 1.2 + 0.5; // 较小的粒子
        this.color = `rgba(255, 255, 255, ${Math.random() * 0.5 + 0.2})`;
      }
      
      update() {
        if (!canvas) return; // 安全检查
        // 基本移动
        this.x += this.speedX;
        this.y += this.speedY;
        
        // 边界检查 - 循环移动
        if (this.x < 0) this.x = canvas.width;
        else if (this.x > canvas.width) this.x = 0;
        if (this.y < 0) this.y = canvas.height;
        else if (this.y > canvas.height) this.y = 0;
      }
      
      draw() {
        if (!ctx) return;
        ctx.fillStyle = this.color;
        ctx.beginPath();
        ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
        ctx.fill();
      }
    }
    
    // 创建粒子
    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle());
    }
    
    // 绘制连线 - 优化版本
    const drawConnection = (p1: Particle, p2: Particle, distance: number) => {
      if (!ctx) return;
      const opacity = 1 - distance / connectionDistance;
      ctx.strokeStyle = `rgba(255, 255, 255, ${opacity * 0.45})`; // 微调透明度
      ctx.lineWidth = 0.35; // 调整线宽
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
    };
    
    // 动画主函数
    const animate = () => {
      if (!ctx || !canvas) return;
      
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // 更新粒子
      particles.forEach((particle, index) => {
        particle.update();
        particle.draw();
        
        // 只检查后续粒子，避免重复计算
        // 增加检查数量，确保更多连线
        const checkLimit = Math.min(particles.length, index + 60);
        for (let j = index + 1; j < checkLimit; j++) {
          const otherParticle = particles[j];
          const dx = particle.x - otherParticle.x;
          const dy = particle.y - otherParticle.y;
          const distance = Math.sqrt(dx * dx + dy * dy);
          
          if (distance < connectionDistance) {
            drawConnection(particle, otherParticle, distance);
          }
        }
        
        // 与鼠标的交互
        const dx = particle.x - mousePosition.x;
        const dy = particle.y - mousePosition.y;
        const distance = Math.sqrt(dx * dx + dy * dy);
        
        if (distance < connectionDistance * 1.5) {
          const opacity = 1 - distance / (connectionDistance * 1.5);
          ctx.strokeStyle = `rgba(100, 180, 255, ${opacity * 0.6})`;
          ctx.lineWidth = 0.6;
          ctx.beginPath();
          ctx.moveTo(particle.x, particle.y);
          ctx.lineTo(mousePosition.x, mousePosition.y);
          ctx.stroke();
          
          // 粒子朝鼠标方向轻微移动
          particle.x += (mousePosition.x - particle.x) * 0.01;
          particle.y += (mousePosition.y - particle.y) * 0.01;
        }
      });
      
      animationFrame = requestAnimationFrame(animate);
    };
    
    // 鼠标移动事件
    const handleMouseMove = (e: MouseEvent) => {
      if (!canvas) return;
      const rect = canvas.getBoundingClientRect();
      mousePosition.x = e.clientX - rect.left;
      mousePosition.y = e.clientY - rect.top;
    };
    
    window.addEventListener('mousemove', handleMouseMove);
    
    animate();
    
    // 清理函数
    return () => {
      cancelAnimationFrame(animationFrame);
      window.removeEventListener('resize', handleResize);
      window.removeEventListener('mousemove', handleMouseMove);
    };
  }, []);

  const onSubmit = async (data: LoginFormData) => {
    setLoading(true);
    setError('');

    try {
      const response = await post('/login', {
        username: data.username,
        password: data.password,
      });
      
      setToken(response.token);
      setUserInfo({
        userId: response.userId,
        username: response.username,
        realName: response.realName,
        avatar: response.avatar,
        roles: response.roles || [],
        permissions: response.permissions || [],
      });
      
      navigate({ to: '/' });
    } catch (err: any) {
      setError(err.message || '登录失败，请稍后再试');
      
      // 添加卡片震动效果
      if (loginCardRef.current) {
        loginCardRef.current.classList.add('animate-error-shake');
        setTimeout(() => {
          if (loginCardRef.current) {
            loginCardRef.current.classList.remove('animate-error-shake');
          }
        }, 820);
      }
    } finally {
      setLoading(false);
    }
  };

  const togglePasswordVisibility = () => {
    setShowPassword(!showPassword);
  };

  return (
    <div 
      ref={containerRef} 
      className="fixed inset-0 flex items-center justify-center bg-gradient-to-br from-blue-950 via-indigo-900 to-blue-900 p-4 overflow-hidden"
    >
      {/* 动态背景效果 */}
      <div className="absolute inset-0 overflow-hidden">
        {/* 模糊圆圈 */}
        <div className="absolute w-500px h-500px bg-blue-500/15 rounded-full -top-100px -right-100px blur-5xl animate-float-apple"></div>
        <div className="absolute w-600px h-600px bg-indigo-500/10 rounded-full -bottom-200px -left-200px blur-5xl animate-pulse-slow"></div>
        <div className="absolute w-300px h-300px bg-purple-500/12 rounded-full top-40% left-30% blur-5xl animate-float-apple"></div>
        
        {/* 网格背景 */}
        <div className="absolute inset-0 bg-[linear-gradient(rgba(99,102,241,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(99,102,241,0.03)_1px,transparent_1px)] bg-[size:40px_40px] opacity-50 perspective-500px preserve-3d rotate-x-60deg -translate-z-100px"></div>
        
        {/* 光效 */}
        <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-blue-500/50 to-transparent animate-shimmer"></div>
        <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-purple-500/50 to-transparent animate-shimmer"></div>
      </div>

      {/* Canvas背景 - 3D线束效果 */}
      <canvas 
        ref={canvasRef} 
        className="absolute inset-0 pointer-events-none z-0"
      />
      
      {/* Logo区域 */}
      <div className="absolute left-8 top-8 z-10">
        <img src="/company.png" alt="中兵数科" className="h-10 opacity-80" />
      </div>
      
      <div className="absolute top-20 left-1/2 transform -translate-x-1/2 z-10">
        <img src="/product.png" alt="DataSophon" className="h-10 opacity-80" />
      </div>

      {/* 登录卡片 */}
      <div
        ref={loginCardRef}
        className="w-full max-w-md z-50 transition-all duration-300 animate-scale-in"
      >
        <div className="bg-white/10 backdrop-blur-xl rounded-3xl border border-white/20 shadow-2xl p-10 transition-all duration-300 hover:shadow-blue-500/10 relative overflow-hidden">
          {/* 卡片内部光晕 */}
          <div className="absolute inset-0 bg-gradient-to-r from-transparent via-white/5 to-transparent opacity-0 group-hover:opacity-100 transition-opacity"></div>
          
          {/* 品牌区域 */}
          <div className="text-center mb-8">
            <div 
              className="relative mx-auto mb-4 w-18 h-18 transition-transform duration-300 hover:scale-110"
              onMouseEnter={() => setLogoHover(true)}
              onMouseLeave={() => setLogoHover(false)}
            >
              {/* 环绕轨道 */}
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
              
              {/* Logo */}
              <img 
                src="/logo.png" 
                alt="Datasophon Logo" 
                className={`w-18 h-18 object-contain filter drop-shadow-lg transition-transform duration-300 ${logoHover ? 'scale-110' : ''}`} 
              />
            </div>
            
            <h2 className="text-2xl font-semibold text-transparent bg-clip-text bg-gradient-to-r from-white to-blue-200 mb-1">
              Datasophon
            </h2>
            <p className="text-white/70 text-sm">
              一站式大数据平台部署与管理系统
            </p>
            <div className="mt-2 h-0.5 bg-gradient-to-r from-transparent via-blue-400/40 to-transparent"></div>
          </div>
          
          {/* 错误提示 */}
          {error && (
            <div className="mb-6 rounded-xl overflow-hidden relative">
              <div className="p-4 border border-red-500/30 bg-gradient-to-r from-red-500/10 to-red-900/10 backdrop-blur-lg relative z-10 flex items-center">
                <div className="i-carbon-warning-filled text-red-400 w-5 h-5 mr-3"></div>
                <span className="text-red-200 text-sm">{error}</span>
              </div>
              <div className="absolute inset-0 bg-red-500/5 blur-xl"></div>
            </div>
          )}
          
          <form onSubmit={handleSubmit(onSubmit)} className="space-y-5">
            {/* 用户名输入框 */}
            <div className="relative">
              <div className={`
                relative rounded-xl transition-all duration-300 overflow-hidden
                ${activeField === 'username' ? 'bg-blue-900/30 shadow-md shadow-blue-900/20 scale-105' : 'bg-white/5 hover:bg-white/10 hover:scale-102'}
                ${errors.username ? 'border border-red-400' : ''}
              `}>
                {/* 图标 */}
                <div className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none">
                  <div className={`
                    i-carbon-user w-5 h-5 transition-all duration-300
                    ${activeField === 'username' ? 'text-blue-400 scale-125' : 'text-white/60'}
                  `}></div>
                </div>
                
                {/* 标签 */}
                <div className={`
                  absolute pointer-events-none left-12 transition-all duration-300
                  ${usernameValue || activeField === 'username' 
                    ? 'top-2 text-xs text-blue-300' 
                    : 'top-1/2 -translate-y-1/2 text-sm text-white/60'}
                `}>
                  用户名
                </div>
                
                {/* 输入框 */}
                <input
                  {...register("username")}
                  type="text"
                  autoComplete="username"
                  className={`
                    w-full bg-transparent border-0 outline-none rounded-xl
                    ${usernameValue || activeField === 'username' ? 'pt-7 pb-3' : 'py-5'}
                    px-12 text-white placeholder-white/30
                  `}
                  placeholder={activeField === 'username' ? '请输入用户名' : ''}
                  onFocus={() => setActiveField('username')}
                  onBlur={() => setActiveField(null)}
                />
                
                {/* 底部高亮线 - 简化版 */}
                <div className={`
                  absolute bottom-0 left-3 right-3 h-px transition-transform duration-300 origin-left
                  ${activeField === 'username' ? 'scale-x-100 bg-blue-400' : 'scale-x-0 bg-white/20'}
                `}></div>
              </div>
              
              {/* 错误信息 */}
              {errors.username && (
                <div className="mt-1.5 pl-4 flex items-center">
                  <div className="i-carbon-warning-alt mr-1 text-red-400 w-3.5 h-3.5"></div>
                  <span className="text-red-400 text-xs">{errors.username.message}</span>
                </div>
              )}
            </div>
            
            {/* 密码输入框 */}
            <div className="relative">
              <div className={`
                relative rounded-xl transition-all duration-300 overflow-hidden
                ${activeField === 'password' ? 'bg-blue-900/30 shadow-md shadow-blue-900/20 scale-105' : 'bg-white/5 hover:bg-white/10 hover:scale-102'}
                ${errors.password ? 'border border-red-400' : ''}
              `}>
                {/* 图标 */}
                <div className="absolute left-4 top-1/2 -translate-y-1/2 pointer-events-none">
                  <div className={`
                    i-carbon-password w-5 h-5 transition-all duration-300
                    ${activeField === 'password' ? 'text-blue-400 scale-125' : 'text-white/60'}
                  `}></div>
                </div>
                
                {/* 标签 */}
                <div className={`
                  absolute pointer-events-none left-12 transition-all duration-300
                  ${passwordValue || activeField === 'password' 
                    ? 'top-2 text-xs text-blue-300' 
                    : 'top-1/2 -translate-y-1/2 text-sm text-white/60'}
                `}>
                  密码
                </div>
                
                {/* 输入框 */}
                <input
                  {...register("password")}
                  type={showPassword ? "text" : "password"}
                  autoComplete="current-password"
                  className={`
                    w-full bg-transparent border-0 outline-none rounded-xl
                    ${passwordValue || activeField === 'password' ? 'pt-7 pb-3' : 'py-5'}
                    px-12 pr-12 text-white placeholder-white/30
                  `}
                  placeholder={activeField === 'password' ? '请输入密码' : ''}
                  onFocus={() => setActiveField('password')}
                  onBlur={() => setActiveField(null)}
                />
                
                {/* 显示/隐藏密码按钮 */}
                <div 
                  className="absolute right-4 top-1/2 -translate-y-1/2 cursor-pointer hover:scale-110 transition-transform"
                  onClick={togglePasswordVisibility}
                >
                  <div className={`
                    ${showPassword ? 'i-carbon-view' : 'i-carbon-view-off'} 
                    w-5 h-5 text-white/60 hover:text-blue-400 transition-colors duration-300
                  `}></div>
                </div>
                
                {/* 底部高亮线 - 简化版 */}
                <div className={`
                  absolute bottom-0 left-3 right-3 h-px transition-transform duration-300 origin-left
                  ${activeField === 'password' ? 'scale-x-100 bg-blue-400' : 'scale-x-0 bg-white/20'}
                `}></div>
              </div>
              
              {/* 错误信息 */}
              {errors.password && (
                <div className="mt-1.5 pl-4 flex items-center">
                  <div className="i-carbon-warning-alt mr-1 text-red-400 w-3.5 h-3.5"></div>
                  <span className="text-red-400 text-xs">{errors.password.message}</span>
                </div>
              )}
            </div>
            
            {/* 登录按钮 */}
            <div className="pt-6 relative">
              {/* 按钮阴影 */}
              <div className="absolute inset-x-0 bottom-0 h-10 bg-blue-500/30 blur-xl opacity-30 rounded-full"></div>
              
              <button
                type="submit"
                disabled={loading}
                className={`
                  relative w-full h-14 rounded-xl overflow-hidden
                  bg-gradient-to-r from-blue-600 to-indigo-600 
                  transition-all duration-300 border border-white/10
                  ${loading ? 'cursor-not-allowed opacity-80' : 'hover:brightness-110 hover:scale-105 hover:shadow-lg hover:shadow-blue-500/25'}
                `}
                onMouseEnter={() => setBtnHover(true)}
                onMouseLeave={() => {
                  setBtnHover(false);
                  setBtnActive(false);
                }}
                onMouseDown={() => setBtnActive(true)}
                onMouseUp={() => setBtnActive(false)}
              >
                {/* 顶部高光线 */}
                <div className="absolute top-0 inset-x-0 h-px bg-white/40"></div>
                
                {/* 内部光效 */}
                <div className={`absolute inset-0 bg-gradient-to-r from-transparent via-white/10 to-transparent opacity-0 transition-opacity duration-300 ${btnHover ? 'opacity-100' : ''}`}></div>
                
                {/* 内容层 */}
                <div className={`
                  flex items-center justify-center w-full h-full
                  ${btnActive ? 'translate-y-px scale-98' : ''}
                  transition-transform duration-200
                `}>
                  {loading ? (
                    <div className="flex items-center justify-center">
                      <div className="flex space-x-2">
                        <div className="w-2 h-2 bg-white rounded-full animate-pulse"></div>
                        <div className="w-2 h-2 bg-white rounded-full animate-pulse" style={{ animationDelay: '0.2s' }}></div>
                        <div className="w-2 h-2 bg-white rounded-full animate-pulse" style={{ animationDelay: '0.4s' }}></div>
                      </div>
                    </div>
                  ) : (
                    <div className="flex items-center justify-center">
                      <span className="text-white font-medium mr-2 text-lg">登录</span>
                      <div className={`
                        i-carbon-arrow-right w-5 h-5 text-white
                        transition-all duration-300
                        ${btnHover ? 'translate-x-1.5 scale-110' : ''}
                      `}></div>
                    </div>
                  )}
                </div>
              </button>
            </div>
          </form>
          
          {/* 特性标签 */}
          <div className="flex justify-center gap-5 mt-8 mb-4">
            {['智能管理', '高可用性', '多用户支持'].map((tag) => (
              <div 
                key={tag} 
                className={`
                  flex items-center gap-1.5 py-1.5 px-3 
                  transition-all duration-300
                  ${hoverTag === tag 
                    ? 'bg-blue-500/30 scale-110 shadow-lg shadow-blue-500/20' 
                    : 'bg-white/5 hover:bg-white/10'}
                  rounded-full cursor-pointer
                `}
                onMouseEnter={() => setHoverTag(tag)}
                onMouseLeave={() => setHoverTag(null)}
              >
                <span className={`
                  w-1.5 h-1.5 rounded-full 
                  transition-all duration-300
                  ${hoverTag === tag ? 'bg-blue-300 scale-150' : 'bg-blue-400'}
                `}></span>
                <span className={`
                  text-xs transition-all duration-300
                  ${hoverTag === tag ? 'text-white font-medium' : 'text-white/70'}
                `}>{tag}</span>
              </div>
            ))}
          </div>
        </div>
      </div>
      
      {/* 底部信息 */}
      <div className="absolute bottom-8 left-1/2 transform -translate-x-1/2 text-center">
        <div className="text-white/60 text-sm mb-2">
          <p>北京中兵数字科技集团有限公司 版权所有</p>
          <p className="mt-1">Copyright © {new Date().getFullYear()} Datasophon</p>
        </div>
        <img src="/company.png" alt="中兵数科" className="h-6 opacity-50 mx-auto" />
      </div>
    </div>
  );
};

export default LoginPage;