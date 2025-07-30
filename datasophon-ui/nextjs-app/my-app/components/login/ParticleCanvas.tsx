"use client";

import { useRef, useEffect } from "react";

interface ParticleCanvasProps {
  className?: string;
}

class Particle {
  x: number = 0;
  y: number = 0;
  speedX: number = 0;
  speedY: number = 0;
  size: number = 1;
  color: string = "rgba(255, 255, 255, 0.5)";
  
  constructor(canvas: HTMLCanvasElement) {
    this.x = Math.random() * canvas.width;
    this.y = Math.random() * canvas.height;
    this.speedX = (Math.random() - 0.5) * 0.5; // 降低速度
    this.speedY = (Math.random() - 0.5) * 0.5; // 降低速度
    this.size = Math.random() * 1.2 + 0.5; // 较小的粒子
    this.color = `rgba(255, 255, 255, ${Math.random() * 0.5 + 0.2})`;
  }
  
  update(canvas: HTMLCanvasElement) {
    // 基本移动
    this.x += this.speedX;
    this.y += this.speedY;
    
    // 边界检查 - 循环移动
    if (this.x < 0) this.x = canvas.width;
    else if (this.x > canvas.width) this.x = 0;
    if (this.y < 0) this.y = canvas.height;
    else if (this.y > canvas.height) this.y = 0;
  }
  
  draw(ctx: CanvasRenderingContext2D) {
    ctx.fillStyle = this.color;
    ctx.beginPath();
    ctx.arc(this.x, this.y, this.size, 0, Math.PI * 2);
    ctx.fill();
  }
}

export default function ParticleCanvas({ className = "" }: ParticleCanvasProps) {
  const canvasRef = useRef<HTMLCanvasElement>(null);

  useEffect(() => {
    const canvas = canvasRef.current;
    if (!canvas) return;

    const ctx = canvas.getContext("2d");
    if (!ctx) return;
    
    // 设置Canvas尺寸
    const handleResize = () => {
      if (!canvas) return;
      canvas.width = window.innerWidth;
      canvas.height = window.innerHeight;
    };
    
    handleResize();
    window.addEventListener("resize", handleResize);
    
    // 创建粒子
    const particles: Particle[] = [];
    const particleCount = 200; // 调整粒子数量
    const connectionDistance = 160; // 连接距离
    const mousePosition = { x: window.innerWidth / 2, y: window.innerHeight / 2 };
    let animationFrame: number;
    
    // 创建粒子实例
    for (let i = 0; i < particleCount; i++) {
      particles.push(new Particle(canvas));
    }
    
    // 绘制连线
    const drawConnection = (p1: Particle, p2: Particle, distance: number) => {
      const opacity = 1 - distance / connectionDistance;
      ctx.strokeStyle = `rgba(255, 255, 255, ${opacity * 0.45})`;
      ctx.lineWidth = 0.35;
      ctx.beginPath();
      ctx.moveTo(p1.x, p1.y);
      ctx.lineTo(p2.x, p2.y);
      ctx.stroke();
    };
    
    // 动画主函数
    const animate = () => {
      ctx.clearRect(0, 0, canvas.width, canvas.height);
      
      // 更新粒子
      particles.forEach((particle, index) => {
        particle.update(canvas);
        particle.draw(ctx);
        
        // 只检查后续粒子，避免重复计算
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
      const rect = canvas.getBoundingClientRect();
      mousePosition.x = e.clientX - rect.left;
      mousePosition.y = e.clientY - rect.top;
    };
    
    window.addEventListener("mousemove", handleMouseMove);
    
    animate();
    
    // 清理函数
    return () => {
      cancelAnimationFrame(animationFrame);
      window.removeEventListener("resize", handleResize);
      window.removeEventListener("mousemove", handleMouseMove);
    };
  }, []);

  return (
    <canvas 
      ref={canvasRef} 
      className={`absolute inset-0 pointer-events-none z-0 ${className}`}
    />
  );
} 