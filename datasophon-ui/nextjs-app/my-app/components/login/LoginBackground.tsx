"use client";

export default function LoginBackground() {
  return (
    <div className="absolute inset-0 overflow-hidden">
      {/* 渐变背景 */}
      <div className="absolute inset-0 bg-gradient-to-br from-blue-950 via-indigo-900 to-blue-900" />
      
      {/* 模糊圆圈 */}
      <div className="absolute w-[500px] h-[500px] bg-blue-500/15 rounded-full -top-[100px] -right-[100px] blur-[5rem] animate-float" />
      <div className="absolute w-[600px] h-[600px] bg-indigo-500/10 rounded-full -bottom-[200px] -left-[200px] blur-[5rem] animate-pulse-slow" />
      <div className="absolute w-[300px] h-[300px] bg-purple-500/12 rounded-full top-[40%] left-[30%] blur-[5rem] animate-float" />
      
      {/* 网格背景 */}
      <div className="absolute inset-0 bg-[linear-gradient(rgba(99,102,241,0.03)_1px,transparent_1px),linear-gradient(90deg,rgba(99,102,241,0.03)_1px,transparent_1px)] bg-[size:40px_40px] opacity-50 [perspective:500px] [transform-style:preserve-3d] [transform:rotateX(60deg)_translateZ(-100px)]" />
      
      {/* 光效 */}
      <div className="absolute top-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-blue-500/50 to-transparent animate-shimmer" />
      <div className="absolute bottom-0 left-0 right-0 h-px bg-gradient-to-r from-transparent via-purple-500/50 to-transparent animate-shimmer" />
    </div>
  );
} 