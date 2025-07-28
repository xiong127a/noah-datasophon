import { Outlet } from '@tanstack/react-router';

/**
 * 页面布局 - 用于带有子页面的布局
 */
const PageLayout = () => {
  return (
    <div className="min-h-full bg-gray-50">
      <div className="container py-4">
        <Outlet /> {/* 子路由内容将在此处渲染 */}
      </div>
    </div>
  );
};

export default PageLayout; 