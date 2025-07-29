import React from 'react';
import NoahNavbar from '../components/NoahNavbar';

export default function Home() {
  return (
    <div>
      <NoahNavbar />
      <main className="container mx-auto px-4 py-8">
        <h1 className="text-3xl font-bold text-center mb-8">Noah大数据基础平台</h1>
        <div className="grid grid-cols-1 md:grid-cols-3 gap-6">
          <div className="col-span-3 bg-gradient-to-r from-blue-50 to-indigo-50 rounded-2xl p-6 shadow-sm">
            <h2 className="text-xl font-semibold text-slate-800 mb-4">欢迎使用Noah大数据基础平台</h2>
            <p className="text-slate-600">
              这是一个基于Next.js和Tailwind CSS构建的大数据管理平台界面。
            </p>
          </div>
        </div>
      </main>
    </div>
  );
} 