import { useState } from 'react';
import { Outlet, useNavigate } from '@tanstack/react-router';
import { clearAuthInfo, getUserInfo } from '@/utils/auth';

/**
 * TabsLayout - 应用主框架布局，仅包含顶部导航和内容区域
 * 基于苹果设计风格的现代化界面
 */
const TabsLayout = () => {
  const navigate = useNavigate();
  const userInfo = getUserInfo();
  const [activeMenu, setActiveMenu] = useState('/overview');
  const [hoveredMenu, setHoveredMenu] = useState<string | null>(null);

  // 导航菜单数据
  const navItems = [
    {
      key: '/overview',
      label: '集群总览',
      icon: 'i-carbon-dashboard',
    },
    {
      key: '/',
      label: '主页',
      icon: 'i-carbon-home',
    }
  ];

  // 管理菜单数据
  const adminItems = [
    {
      key: '/cluster-manage',
      label: '集群管理',
      icon: 'i-carbon-cloud-services',
    },
    {
      key: '/user-manage',
      label: '用户管理',
      icon: 'i-carbon-user-admin',
    }
  ];

  // 处理菜单项点击
  const handleMenuClick = (key: string) => {
    setActiveMenu(key);
    navigate({ to: key });
  };

  // 处理菜单项hover
  const handleMenuHover = (key: string | null) => {
    setHoveredMenu(key);
  };

  // 处理退出登录
  const handleLogout = () => {
    clearAuthInfo();
    navigate({ to: '/login' });
  };

  return (
    <div className="flex flex-col h-screen w-screen overflow-hidden">
      {/* 顶部导航栏 - 苹果风格重构 */}
      <header className="sticky top-0 z-50 w-full glass-morphism shadow-subtle">
        <div className="h-16 px-6 mx-auto flex items-center justify-between">
          {/* 左侧：Logo + 导航菜单 */}
          <div className="flex items-center gap-8">
            {/* Logo区域 */}
            <div 
              className="flex items-center cursor-pointer group"
              onClick={() => handleMenuClick('/')}
            >
              <div className="w-8 h-8 mr-2 flex-center transition group-hover:scale-105">
                <img src="/company.png" alt="Logo" className="h-full" />
              </div>
              <h1 className="text-base font-semibold text-gray-900 dark:text-white tracking-tight group-hover:text-primary transition">
                DataSophon大数据平台
              </h1>
            </div>
            
            {/* 主导航菜单 */}
            <nav className="hidden md:block">
              <ul className="flex items-center gap-1">
                {navItems.map((item) => (
                  <li
                    key={item.key}
                    className="relative"
                    onMouseEnter={() => handleMenuHover(item.key)}
                    onMouseLeave={() => handleMenuHover(null)}
                    onClick={() => handleMenuClick(item.key)}
                  >
                    <div
                      className={`
                        flex items-center px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200
                        ${activeMenu === item.key 
                          ? 'bg-primary-50 text-primary-600 shadow-sm'
                          : 'text-gray-700 hover:text-primary-600 hover:bg-primary-50/50'}
                      `}
                    >
                      <div className={`
                        w-5 h-5 mr-2 flex-center transition
                        ${activeMenu === item.key ? 'text-primary-600' : 'text-gray-500'}
                      `}>
                        <div className={item.icon}></div>
                      </div>
                      <span>{item.label}</span>
                    </div>
                  </li>
                ))}
              </ul>
            </nav>
          </div>

          {/* 中间区域：可以放置状态指示器等 */}
          <div className="hidden lg:flex items-center justify-center absolute left-1/2 transform -translate-x-1/2">
            {/* 服务状态指示器等组件 */}
          </div>

          {/* 右侧：管理菜单 + 用户信息 + 退出按钮 */}
          <div className="flex items-center gap-4">
            {/* 管理菜单 */}
            <nav className="hidden md:block">
              <ul className="flex items-center gap-1">
                {adminItems.map((item) => (
                  <li
                    key={item.key}
                    className="relative"
                    onMouseEnter={() => handleMenuHover(item.key)}
                    onMouseLeave={() => handleMenuHover(null)}
                    onClick={() => handleMenuClick(item.key)}
                  >
                    <div
                      className={`
                        flex items-center px-4 py-2.5 rounded-xl text-sm font-medium transition-all duration-200
                        ${activeMenu === item.key 
                          ? 'bg-primary-50 text-primary-600 shadow-sm'
                          : 'text-gray-700 hover:text-primary-600 hover:bg-primary-50/50'}
                      `}
                    >
                      <div className={`
                        w-5 h-5 mr-2 flex-center transition
                        ${activeMenu === item.key ? 'text-primary-600' : 'text-gray-500'}
                      `}>
                        <div className={item.icon}></div>
                      </div>
                      <span>{item.label}</span>
                    </div>
                  </li>
                ))}
              </ul>
            </nav>

            {/* 用户信息 */}
            <div className="user-avatar ml-4">
              <div className="flex items-center px-4 py-2 rounded-xl bg-gradient-to-br from-white to-gray-50 border border-gray-100 shadow-sm transition hover:shadow-md hover:border-primary-100 hover:-translate-y-0.5">
                <div className="i-carbon-user w-5 h-5 mr-2 text-primary-500"></div>
                <span className="text-sm font-medium text-gray-800">{userInfo?.username || '未登录'}</span>
              </div>
            </div>

            {/* 退出登录按钮 */}
            <button 
              onClick={handleLogout}
              className="w-10 h-10 flex-center rounded-xl bg-white border border-gray-100 shadow-sm text-gray-600 hover:text-primary-600 hover:-translate-y-0.5 hover:shadow-md hover:border-primary-100 transition-all"
              aria-label="退出登录"
            >
              <div className="i-carbon-logout w-5 h-5"></div>
            </button>
          </div>
        </div>
      </header>

      {/* 主内容区域 - 全屏显示 */}
      <main className="flex-1 w-full bg-gray-50 overflow-auto">
        <div className="container mx-auto py-6 px-6">
          <Outlet /> {/* 子路由内容将在此处渲染 */}
        </div>
      </main>
    </div>
  );
};

export default TabsLayout; 