import { useState } from 'react';
import { Outlet, useNavigate } from '@tanstack/react-router';
import { clearAuthInfo, getUserInfo } from '@/utils/auth';

/**
 * TabsLayout - 应用主框架，包含顶部导航、侧边栏和内容区域
 */
const TabsLayout = () => {
  const navigate = useNavigate();
  const [collapsed, setCollapsed] = useState(false);
  const userInfo = getUserInfo();

  // 导航菜单数据
  const navItems = [
    {
      key: '/overview',
      label: '集群总览',
      icon: '📊',
    },
    {
      key: '/',
      label: '主页',
      icon: '🏠',
    }
  ];

  // 处理退出登录
  const handleLogout = () => {
    clearAuthInfo();
    navigate({ to: '/login' });
  };

  return (
    <div className="flex h-screen flex-col">
      {/* 顶部导航栏 */}
      <header className="bg-blue-600 text-white h-16 shadow flex items-center px-4">
        <div className="flex-1 flex items-center">
          <h1 className="text-xl font-bold">DataSophon大数据平台</h1>
        </div>
        <div className="flex items-center space-x-4">
          <span>{userInfo?.username || '未登录'}</span>
          <button 
            onClick={handleLogout}
            className="px-3 py-1 bg-blue-700 rounded hover:bg-blue-800"
          >
            退出登录
          </button>
        </div>
      </header>

      <div className="flex flex-1 overflow-hidden">
        {/* 侧边导航 */}
        <nav className={`bg-gray-800 text-white ${collapsed ? 'w-16' : 'w-56'} transition-all duration-300 flex flex-col`}>
          <div className="p-4 flex justify-between items-center">
            {!collapsed && <span className="text-lg font-medium">导航菜单</span>}
            <button 
              onClick={() => setCollapsed(!collapsed)}
              className="p-1 rounded hover:bg-gray-700"
            >
              {collapsed ? '»' : '«'}
            </button>
          </div>
          <ul className="flex-1 overflow-y-auto">
            {navItems.map((item) => (
              <li key={item.key} className="mb-1">
                <a 
                  className="flex items-center px-4 py-2 hover:bg-gray-700 cursor-pointer"
                  onClick={() => navigate({ to: item.key })}
                >
                  <span className="mr-3">{item.icon}</span>
                  {!collapsed && <span>{item.label}</span>}
                </a>
              </li>
            ))}
          </ul>
        </nav>

        {/* 主内容区域 */}
        <main className="flex-1 overflow-auto bg-gray-50 p-4">
          <Outlet /> {/* 子路由内容将在此处渲染 */}
        </main>
      </div>
    </div>
  );
};

export default TabsLayout; 