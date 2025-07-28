import { useNavigate } from '@tanstack/react-router';

const NotFoundPage = () => {
  const navigate = useNavigate();

  return (
    <div className="flex flex-col items-center justify-center min-h-screen bg-gray-50 px-4">
      <div className="text-center">
        <h1 className="text-9xl font-bold text-primary-500">404</h1>
        <div className="mt-4 text-xl font-medium text-gray-600">页面未找到</div>
        <p className="mt-2 text-gray-500">您访问的页面不存在或已被移除</p>
        <button
          onClick={() => navigate({ to: '/' })}
          className="mt-8 px-6 py-3 bg-primary-500 text-white rounded-lg shadow-md hover:bg-primary-600 transition-all"
        >
          返回首页
        </button>
      </div>
    </div>
  );
};

export default NotFoundPage; 