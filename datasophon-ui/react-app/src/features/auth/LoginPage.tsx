import { useState } from 'react';
import { useNavigate } from '@tanstack/react-router';
import { z } from 'zod';
import { useForm } from 'react-hook-form';
import { post } from '@/api/http';
import { setToken, setUserInfo } from '@/utils/auth';

// 登录表单验证模式
const loginSchema = z.object({
  username: z.string().min(1, '用户名不能为空'),
  password: z.string().min(1, '密码不能为空'),
  remember: z.boolean().optional(),
});

type LoginForm = z.infer<typeof loginSchema>;

const LoginPage = () => {
  const navigate = useNavigate();
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);
  
  const { register, handleSubmit, formState: { errors } } = useForm<LoginForm>({
    defaultValues: {
      username: '',
      password: '',
      remember: true,
    },
  });
  
  const onSubmit = async (data: LoginForm) => {
    try {
      setLoading(true);
      setError(null);
      
      // 调用登录API
      const response = await post('/login', {
        username: data.username,
        password: data.password,
      });
      
      // 处理登录成功
      setToken(response.token);
      setUserInfo({
        userId: response.userId,
        username: response.username,
        realName: response.realName,
        avatar: response.avatar,
        roles: response.roles || [],
        permissions: response.permissions || [],
      });
      
      // 跳转到首页
      navigate({ to: '/' });
    } catch (err: any) {
      setError(err.message || '登录失败，请稍后再试');
    } finally {
      setLoading(false);
    }
  };
  
  return (
    <div className="min-h-screen flex items-center justify-center bg-gray-100">
      <div className="max-w-md w-full bg-white rounded-lg shadow-lg p-8">
        <div className="text-center mb-8">
          <h1 className="text-3xl font-bold text-gray-800">DataSophon</h1>
          <p className="text-gray-600 mt-2">大数据平台管理系统</p>
        </div>
        
        {error && (
          <div className="mb-4 p-3 bg-red-100 border border-red-200 text-red-700 rounded">
            {error}
          </div>
        )}
        
        <form onSubmit={handleSubmit(onSubmit)}>
          <div className="mb-4">
            <label className="block text-gray-700 text-sm font-medium mb-2">
              用户名
            </label>
            <input
              {...register('username')}
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 ${
                errors.username ? 'border-red-500 focus:ring-red-200' : 'border-gray-300 focus:ring-blue-200'
              }`}
              placeholder="请输入用户名"
            />
            {errors.username && (
              <p className="mt-1 text-sm text-red-600">{errors.username.message}</p>
            )}
          </div>
          
          <div className="mb-6">
            <label className="block text-gray-700 text-sm font-medium mb-2">
              密码
            </label>
            <input
              {...register('password')}
              type="password"
              className={`w-full px-3 py-2 border rounded-md focus:outline-none focus:ring-2 ${
                errors.password ? 'border-red-500 focus:ring-red-200' : 'border-gray-300 focus:ring-blue-200'
              }`}
              placeholder="请输入密码"
            />
            {errors.password && (
              <p className="mt-1 text-sm text-red-600">{errors.password.message}</p>
            )}
          </div>
          
          <div className="flex items-center justify-between mb-6">
            <div className="flex items-center">
              <input
                {...register('remember')}
                id="remember"
                type="checkbox"
                className="h-4 w-4 text-blue-600 focus:ring-blue-500 border-gray-300 rounded"
              />
              <label htmlFor="remember" className="ml-2 block text-sm text-gray-700">
                记住我
              </label>
            </div>
            
            <div className="text-sm">
              <a href="#" className="text-blue-600 hover:text-blue-800">
                忘记密码?
              </a>
            </div>
          </div>
          
          <button
            type="submit"
            disabled={loading}
            className={`w-full py-2 px-4 rounded-md text-white font-medium ${
              loading 
                ? 'bg-blue-400 cursor-not-allowed' 
                : 'bg-blue-600 hover:bg-blue-700 focus:outline-none focus:ring-2 focus:ring-blue-200'
            }`}
          >
            {loading ? '登录中...' : '登录'}
          </button>
        </form>
      </div>
    </div>
  );
};

export default LoginPage; 