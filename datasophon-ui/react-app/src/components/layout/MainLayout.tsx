import { Outlet } from '@tanstack/react-router';

/**
 * 主布局组件 - 应用的根布局
 */
const MainLayout = () => {
  return (
    <div className="h-screen">
      <Outlet /> {/* 子路由内容将在此处渲染 */}
    </div>
  );
};

export default MainLayout; 