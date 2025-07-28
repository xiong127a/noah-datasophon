import { useState, useRef, useEffect } from 'react';
import { Outlet, useNavigate } from '@tanstack/react-router';
import { clearAuthInfo, getUserInfo } from '@/utils/auth';

/**
 * TabsLayout - 基于苹果设计风格的主框架，包含顶部导航和内容区域
 */
const TabsLayout = () => {
  const navigate = useNavigate();
  const userInfo = getUserInfo();
  const [activeMenu, setActiveMenu] = useState('/overview');
  const [hoveredMenu, setHoveredMenu] = useState<string | null>(null);
  const [expandedMenu, setExpandedMenu] = useState<string | null>(null);
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const [isClusterMenuOpen, setIsClusterMenuOpen] = useState(false);
  const [alarmCount, setAlarmCount] = useState(3);
  
  // 引用DOM元素
  const userDropdownRef = useRef<HTMLDivElement>(null);
  const clusterDropdownRef = useRef<HTMLDivElement>(null);

  // 主菜单数据 (左侧)
  const mainMenuItems = [
    {
      key: '/',
      label: '主页',
      icon: 'i-carbon-home',
    },
    {
      key: '/host-manage',
      label: '主机管理',
      icon: 'i-carbon-bare-metal-server',
    },
    {
      key: '/alarm-manage',
      label: '告警管理',
      icon: 'i-carbon-notification',
      children: [
        { key: '/alarm-manage/notice', label: '通知组管理', icon: 'i-carbon-notification-new' },
        { key: '/alarm-manage/group', label: '告警组管理', icon: 'i-carbon-notification-off' },
        { key: '/alarm-manage/table', label: '告警指标管理', icon: 'i-carbon-table' },
        { key: '/alarm-manage/help', label: '使用帮助', icon: 'i-carbon-help' }
      ]
    },
    {
      key: '/system-manage',
      label: '系统管理',
      icon: 'i-carbon-settings',
      children: [
        { key: '/system-manage/tenant', label: '租户管理', icon: 'i-carbon-group' },
        { key: '/system-manage/user', label: '用户管理', icon: 'i-carbon-user-profile' },
        { key: '/system-manage/rack', label: '机架管理', icon: 'i-carbon-data-base' },
        { key: '/system-manage/tag', label: '标签管理', icon: 'i-carbon-tag' },
        { key: '/system-manage/log', label: '日志审计', icon: 'i-carbon-document' }
      ]
    }
  ];

  // 管理菜单数据 (右侧)
  const adminMenuItems = [
    {
      key: '/cluster-manage',
      label: '集群管理',
      icon: 'i-carbon-cloud-services',
      children: [
        { key: '/cluster-manage/list', label: '集群列表', icon: 'i-carbon-list' },
        { key: '/cluster-manage/storage', label: '集群存储库', icon: 'i-carbon-data-base' },
        { key: '/cluster-manage/framework', label: '集群框架', icon: 'i-carbon-network-4' }
      ]
    },
    {
      key: '/user-manage',
      label: '用户管理',
      icon: 'i-carbon-user-admin',
    }
  ];

  // 集群数据
  const clusters = [
    { id: '1', name: '测试集群1', type: 'K8S', status: 'running' },
    { id: '2', name: '生产环境', type: 'Linux', status: 'running' },
    { id: '3', name: '开发环境', type: 'Linux', status: 'warning' }
  ];
  const [currentCluster, setCurrentCluster] = useState(clusters[0]);

  // 处理菜单项点击
  const handleMenuClick = (key: string, hasChildren: boolean) => {
    if (!hasChildren) {
      setActiveMenu(key);
      navigate({ to: key });
      // 关闭所有展开的菜单
      setExpandedMenu(null);
      setHoveredMenu(null);
    } else {
      // 切换展开状态
      setExpandedMenu(expandedMenu === key ? null : key);
      if (expandedMenu !== key) {
        setHoveredMenu(key);
      }
    }
  };

  // 处理子菜单项点击
  const handleSubMenuClick = (parentKey: string, key: string) => {
    setActiveMenu(key);
    navigate({ to: key });
    // 关闭所有展开的菜单
    setExpandedMenu(null);
    setHoveredMenu(null);
  };

  // 处理菜单项hover
  const handleMenuHover = (key: string | null, hasChildren: boolean) => {
    if (hasChildren) {
      setHoveredMenu(key);
    }
  };

  // 处理菜单项离开
  const handleMenuLeave = (key: string | null) => {
    // 如果没有固定展开的菜单，则清除悬停状态
    if (expandedMenu !== key) {
      setHoveredMenu(null);
    }
  };

  // 切换用户菜单
  const toggleUserMenu = () => {
    setIsUserMenuOpen(!isUserMenuOpen);
    // 关闭其他菜单
    setIsClusterMenuOpen(false);
  };

  // 切换集群菜单
  const toggleClusterMenu = () => {
    setIsClusterMenuOpen(!isClusterMenuOpen);
    // 关闭其他菜单
    setIsUserMenuOpen(false);
  };

  // 选择集群
  const selectCluster = (cluster: typeof clusters[0]) => {
    setCurrentCluster(cluster);
    setIsClusterMenuOpen(false);
  };

  // 处理退出登录
  const handleLogout = () => {
    clearAuthInfo();
    navigate({ to: '/login' });
  };

  // 打开历史操作
  const openHistoryOperations = () => {
    console.log('打开历史操作');
  };

  // 打开告警管理
  const openAlarmManagement = () => {
    navigate({ to: '/alarm-manage/notice' });
  };

  // 点击外部关闭下拉菜单
  useEffect(() => {
    const handleClickOutside = (event: MouseEvent) => {
      if (userDropdownRef.current && !userDropdownRef.current.contains(event.target as Node)) {
        setIsUserMenuOpen(false);
      }
      if (clusterDropdownRef.current && !clusterDropdownRef.current.contains(event.target as Node)) {
        setIsClusterMenuOpen(false);
      }
    };

    document.addEventListener('mousedown', handleClickOutside);
    return () => {
      document.removeEventListener('mousedown', handleClickOutside);
    };
  }, []);

  return (
    <div className="flex flex-col h-screen w-screen overflow-hidden">
      {/* 顶部导航栏 - 苹果风格实现 */}
      <header className="sticky top-0 z-50 w-full h-15 apple-header">
        <div className="h-full mx-auto flex items-center justify-between px-6">
          {/* 左侧：Logo + 导航菜单 */}
          <div className="flex items-center h-full">
            {/* Logo区域 */}
            <div 
              className="flex items-center cursor-pointer mr-10 transition-apple-spring hover:opacity-80 hover:scale-102"
              onClick={() => handleMenuClick('/', false)}
            >
              <div className="w-8 h-8 mr-3 flex-center">
                <img src="/logo.png" alt="Logo" className="h-full" />
              </div>
              <h1 className="text-base font-medium text-apple-dark tracking-tight">
                Noah大数据基础平台
              </h1>
            </div>
            
            {/* 左侧主菜单 */}
            <nav className="h-full">
              <ul className="nav-menu flex items-center h-full list-none m-0 p-0 gap-1.5">
                {mainMenuItems.map((item) => (
                  <li
                    key={item.key}
                    className={`
                      nav-item relative h-full flex items-center cursor-pointer
                      ${activeMenu === item.key || (item.children && item.children.some(child => activeMenu === child.key)) ? 'active' : ''}
                    `}
                    onMouseEnter={() => handleMenuHover(item.key, !!item.children)}
                    onMouseLeave={() => handleMenuLeave(item.key)}
                    onClick={() => handleMenuClick(item.key, !!item.children)}
                  >
                    <div className={`
                      nav-link flex items-center gap-2 px-4 py-3 rounded-lg transition-all duration-300
                      ${activeMenu === item.key || (item.children && item.children.some(child => activeMenu === child.key)) ? 'text-primary-600' : 'text-gray-800'}
                    `}>
                      <div className={`
                        nav-icon w-5 h-5 flex-center
                        ${activeMenu === item.key || (item.children && item.children.some(child => activeMenu === child.key)) 
                          ? 'text-primary-600' 
                          : 'text-gray-500'
                        }
                      `}>
                        <div className={item.icon}></div>
                      </div>
                      <span className="nav-text text-sm font-medium whitespace-nowrap">{item.label}</span>
                      {item.children && (
                        <div className={`
                          w-4 h-4 flex-center opacity-60 transition-transform duration-150
                          ${(hoveredMenu === item.key || expandedMenu === item.key) ? 'rotate-180 opacity-100' : ''}
                        `}>
                          <div className="i-carbon-chevron-down"></div>
                        </div>
                      )}
                    </div>

                    {/* 子菜单下拉面板 */}
                    {item.children && (hoveredMenu === item.key || expandedMenu === item.key) && (
                      <div 
                        className="absolute top-full left-0 min-w-55 glass-morphism-menu rounded-xl overflow-hidden z-50 animate-scale-in origin-top-left shadow-menu"
                        onMouseEnter={() => setHoveredMenu(item.key)}
                        onMouseLeave={() => handleMenuLeave(item.key)}
                      >
                        <div className="p-1.5">
                          <div className="flex items-center justify-between px-3 py-1.5 border-b border-gray-100/80 mb-1">
                            <span className="text-sm font-medium text-apple-dark">{item.label}</span>
                            <div 
                              className="w-6 h-6 rounded-full hover:bg-black/5 flex-center cursor-pointer transition-all"
                              onClick={(e) => {
                                e.stopPropagation();
                                setExpandedMenu(null);
                                setHoveredMenu(null);
                              }}
                            >
                              <div className="i-carbon-close text-gray-500 w-3 h-3"></div>
                            </div>
                          </div>
                          <ul className="list-none m-0 p-0 py-1">
                            {item.children.map((subItem) => (
                              <li
                                key={subItem.key}
                                className={`
                                  mx-1 rounded-lg transition-colors duration-150
                                  ${activeMenu === subItem.key ? 'menu-active' : 'text-gray-700 hover:menu-hover'}
                                `}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleSubMenuClick(item.key, subItem.key);
                                }}
                              >
                                <div className="menu-item">
                                  <div className={`
                                    w-5 h-5 mr-2 flex-center
                                    ${activeMenu === subItem.key ? 'text-primary-600' : 'text-gray-500'}
                                  `}>
                                    <div className={subItem.icon}></div>
                                  </div>
                                  <span className="text-sm">{subItem.label}</span>
                                </div>
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </nav>
          </div>

          {/* 右侧：管理菜单 + 操作按钮 + 用户信息 */}
          <div className="flex items-center gap-3 mr-1">
            {/* 右侧管理菜单 - 统一样式与左侧一致 */}
            <nav className="h-full mr-4">
              <ul className="admin-menu flex items-center h-full list-none m-0 p-0 gap-1.5">
                {adminMenuItems.map((item) => (
                  <li
                    key={item.key}
                    className={`
                      admin-item relative h-full flex items-center cursor-pointer
                      ${activeMenu === item.key || (item.children && item.children.some(child => activeMenu === child.key)) ? 'active' : ''}
                    `}
                    onMouseEnter={() => handleMenuHover(item.key, !!item.children)}
                    onMouseLeave={() => handleMenuLeave(item.key)}
                    onClick={() => handleMenuClick(item.key, !!item.children)}
                  >
                    <div className={`
                      admin-link flex items-center gap-2 px-4 py-3 rounded-lg transition-all duration-300
                      ${activeMenu === item.key || (item.children && item.children.some(child => activeMenu === child.key)) ? 'text-primary-600' : 'text-gray-800'}
                    `}>
                      <div className={`
                        admin-icon w-5 h-5 flex-center
                        ${activeMenu === item.key || (item.children && item.children.some(child => activeMenu === child.key)) 
                          ? 'text-primary-600' 
                          : 'text-gray-500'
                        }
                      `}>
                        <div className={item.icon}></div>
                      </div>
                      <span className="admin-text text-sm font-medium whitespace-nowrap">{item.label}</span>
                      {item.children && (
                        <div className={`
                          w-4 h-4 flex-center opacity-60 transition-transform duration-150
                          ${(hoveredMenu === item.key || expandedMenu === item.key) ? 'rotate-180 opacity-100' : ''}
                        `}>
                          <div className="i-carbon-chevron-down"></div>
                        </div>
                      )}
                    </div>

                    {/* 子菜单下拉面板 */}
                    {item.children && (hoveredMenu === item.key || expandedMenu === item.key) && (
                      <div 
                        className="absolute top-full right-0 min-w-55 glass-morphism-menu rounded-xl overflow-hidden z-50 animate-scale-in origin-top-right shadow-menu"
                        onMouseEnter={() => setHoveredMenu(item.key)}
                        onMouseLeave={() => handleMenuLeave(item.key)}
                      >
                        <div className="p-1.5">
                          <div className="flex items-center justify-between px-3 py-1.5 border-b border-gray-100/80 mb-1">
                            <span className="text-sm font-medium text-apple-dark">{item.label}</span>
                            <div 
                              className="w-6 h-6 rounded-full hover:bg-black/5 flex-center cursor-pointer transition-all"
                              onClick={(e) => {
                                e.stopPropagation();
                                setExpandedMenu(null);
                                setHoveredMenu(null);
                              }}
                            >
                              <div className="i-carbon-close text-gray-500 w-3 h-3"></div>
                            </div>
                          </div>
                          <ul className="list-none m-0 p-0 py-1">
                            {item.children.map((subItem) => (
                              <li
                                key={subItem.key}
                                className={`
                                  mx-1 rounded-lg transition-colors duration-150
                                  ${activeMenu === subItem.key ? 'menu-active' : 'text-gray-700 hover:menu-hover'}
                                `}
                                onClick={(e) => {
                                  e.stopPropagation();
                                  handleSubMenuClick(item.key, subItem.key);
                                }}
                              >
                                <div className="menu-item">
                                  <div className={`
                                    w-5 h-5 mr-2 flex-center
                                    ${activeMenu === subItem.key ? 'text-primary-600' : 'text-gray-500'}
                                  `}>
                                    <div className={subItem.icon}></div>
                                  </div>
                                  <span className="text-sm">{subItem.label}</span>
                                </div>
                              </li>
                            ))}
                          </ul>
                        </div>
                      </div>
                    )}
                  </li>
                ))}
              </ul>
            </nav>

            {/* 集群选择器 */}
            <div 
              className="relative" 
              ref={clusterDropdownRef}
              onMouseEnter={() => setIsClusterMenuOpen(true)}
              onMouseLeave={() => {
                setTimeout(() => {
                  setIsClusterMenuOpen(false);
                }, 200);
              }}
            >
              <button 
                className="flex items-center h-9 px-3 py-1.5 rounded-lg bg-black/3 hover:bg-black/5 border border-transparent hover:border-gray-100/20 transition-apple"
                onClick={toggleClusterMenu}
              >
                <div className="w-5 h-5 mr-2 text-primary-600">
                  {currentCluster.type === 'K8S' ? (
                    <img src="/icons/kubernetes-logo.svg" alt="K8S" className="w-5 h-5" />
                  ) : (
                    <img src="/icons/linux-tux.svg" alt="Linux" className="w-5 h-5" />
                  )}
                </div>
                <div className="flex flex-col mr-2">
                  <span className="text-xs font-medium text-apple-dark">{currentCluster.name}</span>
                  <span className="text-xs text-gray-500">{currentCluster.type}</span>
                </div>
                <div className={`w-4 h-4 text-gray-500 transition-transform duration-200 ${isClusterMenuOpen ? 'rotate-180' : ''}`}>
                  <div className="i-carbon-chevron-down"></div>
                </div>
              </button>
              
              {/* 集群下拉菜单 */}
              {isClusterMenuOpen && (
                <div className="absolute top-full right-0 mt-1 w-70 glass-morphism-menu rounded-xl shadow-menu overflow-hidden z-50 animate-scale-in origin-top-right">
                  <div className="p-3 border-b border-gray-100/80">
                    <span className="text-sm font-medium text-apple-dark">选择集群</span>
                  </div>
                  <div className="max-h-80 overflow-y-auto p-2">
                    {clusters.map((cluster) => (
                      <div
                        key={cluster.id}
                        className={`
                          flex items-center p-2 rounded-lg cursor-pointer transition-apple
                          ${currentCluster.id === cluster.id ? 'bg-primary-50' : 'hover:bg-black/3'}
                        `}
                        onClick={() => selectCluster(cluster)}
                      >
                        <div className="relative">
                          <div className="w-8 h-8 flex-center bg-black/3 rounded-md">
                            {cluster.type === 'K8S' ? (
                              <img src="/icons/kubernetes-logo.svg" alt="K8S" className="w-6 h-6" />
                            ) : (
                              <img src="/icons/linux-tux.svg" alt="Linux" className="w-6 h-6" />
                            )}
                          </div>
                          <div className={`
                            absolute -bottom-1 -right-1 w-2.5 h-2.5 rounded-full border-2 border-white
                            ${cluster.status === 'running' ? 'bg-apple-green' : 'bg-apple-orange'}
                          `}></div>
                        </div>
                        <div className="ml-3">
                          <p className="text-sm font-medium text-apple-dark">{cluster.name}</p>
                          <p className="text-xs text-gray-500">{cluster.type}</p>
                        </div>
                        {currentCluster.id === cluster.id && (
                          <div className="ml-auto text-primary-600">
                            <div className="i-carbon-checkmark"></div>
                          </div>
                        )}
                      </div>
                    ))}
                  </div>
                </div>
              )}
            </div>

            {/* 历史操作按钮 */}
            <div className="flex-center px-2 cursor-pointer" onClick={openHistoryOperations} title="历史操作">
              <div className="i-carbon-time w-5 h-5 text-gray-700"></div>
            </div>

            {/* 告警按钮 */}
            <div className="relative flex-center px-2 cursor-pointer" onClick={openAlarmManagement} title="告警管理">
              <div className="i-carbon-notification w-5 h-5 text-gray-700"></div>
              {alarmCount > 0 && (
                <div className="absolute -top-1 -right-1 min-w-4 h-4 px-1 rounded-full bg-apple-pink text-white text-xs flex-center">
                  {alarmCount}
                </div>
              )}
            </div>

            {/* 用户中心 - 苹果风格简化版 */}
            <div 
              className="relative" 
              ref={userDropdownRef}
              onMouseEnter={() => setIsUserMenuOpen(true)}
              onMouseLeave={() => setIsUserMenuOpen(false)}
            >
              <button 
                className="flex items-center h-9 px-3 py-1.5 rounded-full bg-gradient-to-b from-white/90 to-gray-50/90 backdrop-blur-sm border border-gray-200/50 shadow-sm hover:shadow-md hover:border-primary-200/50 active:scale-98 transition-apple-spring"
                onClick={toggleUserMenu}
                aria-label="用户中心"
              >
                <div className="w-6 h-6 rounded-full bg-gradient-to-br from-primary-500 to-primary-600 flex-center mr-2 shadow-sm">
                  <div className="i-carbon-user text-white w-3.5 h-3.5"></div>
                </div>
                <span className="text-sm font-medium mr-2 text-gray-800">{userInfo?.username || '未登录'}</span>
                <div className={`w-3.5 h-3.5 text-gray-500 transition-transform duration-200 ${isUserMenuOpen ? 'rotate-180' : ''}`}>
                  <div className="i-carbon-chevron-down"></div>
                </div>
              </button>

              {/* 用户下拉菜单 - 简化版 */}
              {isUserMenuOpen && (
                <div className="absolute top-full right-0 mt-2 w-48 glass-morphism-menu rounded-xl shadow-menu overflow-hidden z-50 animate-scale-in origin-top-right">
                  {/* 用户信息头部 - 简化版 */}
                  <div className="p-4 border-b border-gray-100/50">
                    <div className="flex items-center gap-3">
                      <div className="w-10 h-10 rounded-full bg-gradient-to-br from-primary-500 to-primary-600 flex-center shadow-sm">
                        <div className="i-carbon-user text-white w-5 h-5"></div>
                      </div>
                      <div className="flex-1">
                        <p className="text-sm font-medium text-gray-800">{userInfo?.username || '未登录'}</p>
                        <p className="text-xs text-gray-500">系统管理员</p>
                      </div>
                    </div>
                  </div>
                  
                  {/* 菜单项 - 只保留用户中心和退出登录 */}
                  <div className="p-2">
                    <a className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-gray-50/80 active:bg-gray-100/80 transition-apple cursor-pointer">
                      <div className="w-8 h-8 rounded-lg bg-primary-50 flex-center text-primary-600">
                        <div className="i-carbon-user-profile w-4 h-4"></div>
                      </div>
                      <span className="text-sm font-medium text-gray-800">用户中心</span>
                    </a>
                    
                    <div className="h-px bg-gray-100/50 my-1"></div>
                    
                    <a 
                      className="flex items-center gap-3 p-2.5 rounded-lg hover:bg-red-50/80 active:bg-red-100/80 transition-apple cursor-pointer" 
                      onClick={handleLogout}
                    >
                      <div className="w-8 h-8 rounded-lg bg-red-50 flex-center text-red-500">
                        <div className="i-carbon-logout w-4 h-4"></div>
                      </div>
                      <span className="text-sm font-medium text-red-600">退出登录</span>
                    </a>
                  </div>
                </div>
              )}
            </div>
          </div>
        </div>
      </header>

      {/* 主内容区域 - 全屏显示 */}
      <main className="flex-1 w-full bg-gray-50 overflow-auto">
        <Outlet /> {/* 子路由内容将在此处渲染 */}
      </main>
    </div>
  );
};

export default TabsLayout;