import { Link } from '@tanstack/react-router';

const ForbiddenPage = () => {
  return (
    <div className="min-h-screen flex flex-col items-center justify-center bg-gray-100 px-4">
      <div className="text-center">
        <h1 className="text-9xl font-extrabold text-red-600">403</h1>
        <h2 className="text-3xl font-bold text-gray-800 mt-4">访问被禁止</h2>
        <p className="text-lg text-gray-600 mt-4 max-w-md mx-auto">
          很抱歉，您没有权限访问此页面。
        </p>
        <div className="mt-8">
          <Link
            to="/"
            className="bg-blue-600 text-white px-6 py-3 rounded-md font-medium hover:bg-blue-700 transition-colors"
          >
            返回首页
          </Link>
        </div>
      </div>
    </div>
  );
};

export default ForbiddenPage; 