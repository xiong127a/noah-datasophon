"use client"

import React, { useState, useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import * as z from "zod"
import { User, Mail, Phone, Lock, Eye, EyeOff, UserPlus, Sparkles, ShieldCheck } from "lucide-react"
import { Button } from "@/components/ui/button"
import {
  Dialog,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
} from "@/components/ui/dialog"
import {
  Form,
  FormControl,
  FormField,
  FormItem,
  FormLabel,
  FormMessage,
} from "@/components/ui/form"
import { Input } from "@/components/ui/input"
import { toast } from "sonner"
import { API_PATHS, api } from "@/lib/api-config"
import type { User as UserType, CreateUserRequest, UpdateUserRequest } from "@/types/user"

// 表单验证模式
const userFormSchema = z.object({
  username: z
    .string()
    .min(3, "用户名至少3个字符")
    .max(20, "用户名最多20个字符")
    .regex(/^[a-zA-Z0-9_]+$/, "用户名只能包含字母、数字和下划线"),
  password: z
    .string()
    .min(1, "请输入密码")
    .optional(),
  email: z
    .string()
    .email("请输入有效的邮箱地址")
    .max(50, "邮箱地址最多50个字符"),
  phone: z
    .string()
    .regex(/^1[3-9]\d{9}$/, "请输入有效的手机号码"),
})

type UserFormData = z.infer<typeof userFormSchema>

interface AddUserDialogProps {
  open: boolean
  onClose: () => void
  onSuccess: () => void
  mode: "add" | "edit"
  user?: UserType | null
}

function AddUserDialog({
  open,
  onClose,
  onSuccess,
  mode,
  user,
}: AddUserDialogProps) {
  const [loading, setLoading] = useState(false)
  const [showPassword, setShowPassword] = useState(false)
  const [validationStatus, setValidationStatus] = useState({
    username: false,
    password: false,
    email: false,
    phone: false,
  })

  const isEditMode = mode === "edit"
  const title = isEditMode ? "编辑用户" : "添加用户"
  const description = isEditMode ? "修改用户信息" : "创建新的用户账户"

  // 动态更新表单验证schema
  const currentSchema = isEditMode
    ? userFormSchema.omit({ password: true }).extend({
        password: z.string().optional(),
      })
    : userFormSchema.extend({
        password: z.string().min(1, "请输入密码"),
      })

  const form = useForm<UserFormData>({
    resolver: zodResolver(currentSchema),
    defaultValues: {
      username: "",
      password: "",
      email: "",
      phone: "",
    },
  })

  // 监听特定字段变化更新指示器状态
  const watchUsername = form.watch("username")
  const watchPassword = form.watch("password")
  const watchEmail = form.watch("email")
  const watchPhone = form.watch("phone")

  useEffect(() => {
    setValidationStatus({
      username: !!watchUsername && watchUsername.length >= 3,
      password: isEditMode ? true : !!watchPassword && watchPassword.length >= 1,
      email: !!watchEmail && /^\S+@\S+\.\S+$/.test(watchEmail),
      phone: !!watchPhone && /^1[3-9]\d{9}$/.test(watchPhone),
    })
  }, [watchUsername, watchPassword, watchEmail, watchPhone, isEditMode])

  // 初始化表单数据（编辑模式）
  useEffect(() => {
    if (open && isEditMode && user) {
      form.reset({
        username: user.username,
        password: "",
        email: user.email,
        phone: user.phone,
      })
    } else if (open && !isEditMode) {
      form.reset({
        username: "",
        password: "",
        email: "",
        phone: "",
      })
    }
  }, [open, isEditMode, user, form])

  // 检查用户名是否存在
  const checkUsername = async (username: string): Promise<boolean> => {
    if (!username || (isEditMode && username === user?.username)) {
      return true
    }

    try {
      const response = await api.post(API_PATHS.USER_CHECK_NAME, {
        username,
        excludeId: isEditMode ? user?.id : undefined,
      })
      return !response.data.data // 如果用户名不存在，返回true
    } catch (error) {
      console.error("检查用户名失败:", error)
      return true // 网络错误时允许提交，后端会处理
    }
  }

  // 提交表单
  const onSubmit = async (data: UserFormData) => {
    setLoading(true)

    try {
      // 检查用户名
      const isUsernameValid = await checkUsername(data.username)
      if (!isUsernameValid) {
        form.setError("username", { message: "用户名已存在" })
        setLoading(false)
        return
      }

      let response
      if (isEditMode && user) {
        // 编辑用户
        const updateData: UpdateUserRequest = {
          id: user.id,
          username: data.username,
          email: data.email,
          phone: data.phone,
        }
        // 只有密码不为空时才包含密码字段
        if (data.password && data.password.trim()) {
          updateData.password = data.password
        }
        response = await api.post(API_PATHS.USER_UPDATE, updateData)
      } else {
        // 添加用户
        const createData: CreateUserRequest = {
          username: data.username,
          password: data.password!,
          email: data.email,
          phone: data.phone,
        }
        response = await api.post(API_PATHS.USER_SAVE, createData)
      }

      if (response.data.code === 200) {
        toast.success(isEditMode ? "用户更新成功" : "用户创建成功")
        onSuccess()
      } else {
        toast.error(response.data.message || `${isEditMode ? "更新" : "创建"}用户失败`)
      }
    } catch (error) {
      console.error(`${isEditMode ? "更新" : "创建"}用户失败:`, error)
      toast.error(`${isEditMode ? "更新" : "创建"}用户失败，请检查网络连接`)
    } finally {
      setLoading(false)
    }
  }

  // 取消操作
  const handleCancel = () => {
    form.reset()
    onClose()
  }

  return (
    <Dialog open={open} onOpenChange={onClose}>
      <DialogContent className="sm:max-w-[600px] border-0 shadow-2xl bg-white/95 backdrop-blur-xl rounded-3xl overflow-hidden">
        {/* 装饰性背景 */}
        <div className="absolute inset-0 bg-gradient-to-br from-blue-50/80 via-white/90 to-purple-50/80 pointer-events-none" />
        <div className="absolute top-0 right-0 w-32 h-32 bg-gradient-to-br from-indigo-400/20 to-purple-400/20 rounded-full blur-2xl transform translate-x-16 -translate-y-16" />
        <div className="absolute bottom-0 left-0 w-24 h-24 bg-gradient-to-br from-blue-400/20 to-cyan-400/20 rounded-full blur-2xl transform -translate-x-12 translate-y-12" />
        
        <div className="relative z-10">
          <DialogHeader className="pb-6">
            <div className="flex items-center space-x-3 mb-2">
              <div className="w-10 h-10 rounded-2xl bg-gradient-to-br from-indigo-500 via-purple-600 to-pink-500 flex items-center justify-center shadow-lg">
                {isEditMode ? (
                  <User className="h-5 w-5 text-white" />
                ) : (
                  <UserPlus className="h-5 w-5 text-white" />
                )}
              </div>
              <div>
                <DialogTitle className="text-2xl font-bold bg-gradient-to-r from-slate-800 via-slate-700 to-slate-600 bg-clip-text text-transparent">
                  {title}
                </DialogTitle>
              </div>
            </div>
            <DialogDescription className="text-base text-slate-600 flex items-center space-x-2">
              <Sparkles className="h-4 w-4 text-blue-500" />
              <span>{description}</span>
            </DialogDescription>
            
            {/* 表单完成度进度条 */}
            <div className="mt-4 space-y-2">
              <div className="flex items-center justify-between">
                <span className="text-xs font-medium text-slate-500">表单完成度</span>
                <span className="text-xs font-medium text-slate-500">
                  {Math.round((Object.values(validationStatus).filter(Boolean).length / Object.values(validationStatus).length) * 100)}%
                </span>
              </div>
              <div className="w-full bg-slate-200/50 rounded-full h-2 overflow-hidden">
                <div 
                  className="h-full bg-gradient-to-r from-blue-500 via-purple-500 to-pink-500 rounded-full transition-all duration-500 ease-out"
                  style={{ 
                    width: `${(Object.values(validationStatus).filter(Boolean).length / Object.values(validationStatus).length) * 100}%` 
                  }}
                />
              </div>
            </div>
          </DialogHeader>

          <Form {...form}>
            <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
              <div className="space-y-5">{/* 增加字段间距 */}
                {/* 用户名字段 */}
                <FormField
                  control={form.control}
                  name="username"
                  render={({ field }) => (
                    <FormItem className="space-y-3">
                      <FormLabel className="flex items-center gap-3 text-sm font-semibold text-slate-700">
                        <div className="flex items-center gap-2">
                          <div
                            className={`w-3 h-3 rounded-full transition-all duration-300 ${
                              validationStatus.username
                                ? "bg-gradient-to-r from-green-400 to-emerald-500 shadow-lg shadow-green-500/30"
                                : "bg-gradient-to-r from-red-400 to-rose-500 shadow-lg shadow-red-500/30 animate-pulse"
                            }`}
                          />
                          <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-blue-100 to-indigo-200 flex items-center justify-center">
                            <User className="h-4 w-4 text-blue-600" />
                          </div>
                        </div>
                        用户名称
                      </FormLabel>
                      <FormControl>
                        <div className="relative group">
                          <Input
                            placeholder="请输入用户名称"
                            {...field}
                            disabled={loading}
                            maxLength={20}
                            className="h-12 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm focus:border-blue-400 focus:ring-2 focus:ring-blue-100 transition-all duration-300 group-hover:shadow-lg"
                          />
                          <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-blue-500/5 to-indigo-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none" />
                        </div>
                      </FormControl>
                      <FormMessage className="text-xs" />
                    </FormItem>
                  )}
                />

                {/* 密码字段 */}
                <FormField
                  control={form.control}
                  name="password"
                  render={({ field }) => (
                    <FormItem className="space-y-3">
                      <FormLabel className="flex items-center gap-3 text-sm font-semibold text-slate-700">
                        <div className="flex items-center gap-2">
                          <div
                            className={`w-3 h-3 rounded-full transition-all duration-300 ${
                              validationStatus.password
                                ? "bg-gradient-to-r from-green-400 to-emerald-500 shadow-lg shadow-green-500/30"
                                : "bg-gradient-to-r from-red-400 to-rose-500 shadow-lg shadow-red-500/30 animate-pulse"
                            }`}
                          />
                          <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-purple-100 to-pink-200 flex items-center justify-center">
                            <Lock className="h-4 w-4 text-purple-600" />
                          </div>
                        </div>
                        {isEditMode ? "新密码（留空不修改）" : "用户密码"}
                      </FormLabel>
                      <FormControl>
                        <div className="relative group">
                          <Input
                            type={showPassword ? "text" : "password"}
                            placeholder={isEditMode ? "留空则不修改密码" : "请输入密码"}
                            {...field}
                            disabled={loading}
                            className="h-12 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm focus:border-purple-400 focus:ring-2 focus:ring-purple-100 transition-all duration-300 group-hover:shadow-lg pr-12"
                          />
                          <Button
                            type="button"
                            variant="ghost"
                            size="sm"
                            className="absolute right-2 top-1/2 -translate-y-1/2 h-8 w-8 p-0 rounded-xl hover:bg-purple-50 transition-colors duration-200"
                            onClick={() => setShowPassword(!showPassword)}
                          >
                            {showPassword ? (
                              <EyeOff className="h-4 w-4 text-slate-500 hover:text-purple-600 transition-colors" />
                            ) : (
                              <Eye className="h-4 w-4 text-slate-500 hover:text-purple-600 transition-colors" />
                            )}
                          </Button>
                          <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-purple-500/5 to-pink-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none" />
                        </div>
                      </FormControl>
                      <FormMessage className="text-xs" />
                    </FormItem>
                  )}
                />

                {/* 邮箱字段 */}
                <FormField
                  control={form.control}
                  name="email"
                  render={({ field }) => (
                    <FormItem className="space-y-3">
                      <FormLabel className="flex items-center gap-3 text-sm font-semibold text-slate-700">
                        <div className="flex items-center gap-2">
                          <div
                            className={`w-3 h-3 rounded-full transition-all duration-300 ${
                              validationStatus.email
                                ? "bg-gradient-to-r from-green-400 to-emerald-500 shadow-lg shadow-green-500/30"
                                : "bg-gradient-to-r from-red-400 to-rose-500 shadow-lg shadow-red-500/30 animate-pulse"
                            }`}
                          />
                          <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-emerald-100 to-green-200 flex items-center justify-center">
                            <Mail className="h-4 w-4 text-emerald-600" />
                          </div>
                        </div>
                        邮箱地址
                      </FormLabel>
                      <FormControl>
                        <div className="relative group">
                          <Input
                            type="email"
                            placeholder="请输入邮箱地址"
                            {...field}
                            disabled={loading}
                            maxLength={50}
                            className="h-12 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm focus:border-emerald-400 focus:ring-2 focus:ring-emerald-100 transition-all duration-300 group-hover:shadow-lg"
                          />
                          <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-emerald-500/5 to-green-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none" />
                        </div>
                      </FormControl>
                      <FormMessage className="text-xs" />
                    </FormItem>
                  )}
                />

                {/* 手机字段 */}
                <FormField
                  control={form.control}
                  name="phone"
                  render={({ field }) => (
                    <FormItem className="space-y-3">
                      <FormLabel className="flex items-center gap-3 text-sm font-semibold text-slate-700">
                        <div className="flex items-center gap-2">
                          <div
                            className={`w-3 h-3 rounded-full transition-all duration-300 ${
                              validationStatus.phone
                                ? "bg-gradient-to-r from-green-400 to-emerald-500 shadow-lg shadow-green-500/30"
                                : "bg-gradient-to-r from-red-400 to-rose-500 shadow-lg shadow-red-500/30 animate-pulse"
                            }`}
                          />
                          <div className="w-8 h-8 rounded-xl bg-gradient-to-br from-orange-100 to-amber-200 flex items-center justify-center">
                            <Phone className="h-4 w-4 text-orange-600" />
                          </div>
                        </div>
                        手机号码
                      </FormLabel>
                      <FormControl>
                        <div className="relative group">
                          <Input
                            placeholder="请输入手机号码"
                            {...field}
                            disabled={loading}
                            maxLength={11}
                            className="h-12 rounded-2xl border-slate-200/50 bg-white/80 backdrop-blur-sm focus:border-orange-400 focus:ring-2 focus:ring-orange-100 transition-all duration-300 group-hover:shadow-lg"
                          />
                          <div className="absolute inset-0 rounded-2xl bg-gradient-to-r from-orange-500/5 to-amber-500/5 opacity-0 group-hover:opacity-100 transition-opacity duration-300 pointer-events-none" />
                        </div>
                      </FormControl>
                      <FormMessage className="text-xs" />
                    </FormItem>
                  )}
                />
            </div>

              <DialogFooter className="pt-6 border-t border-slate-200/50 mt-8">
                <div className="flex items-center justify-end space-x-4 w-full">
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleCancel}
                    disabled={loading}
                    className="h-12 px-8 rounded-2xl border-slate-200/50 bg-white/80 hover:bg-slate-50 transition-all duration-300 font-medium"
                  >
                    取消
                  </Button>
                  <Button 
                    type="submit" 
                    disabled={loading || !Object.values(validationStatus).every(Boolean)}
                    className={`h-12 px-8 rounded-2xl text-white border-0 shadow-lg transition-all duration-300 font-medium flex items-center space-x-2 ${
                      Object.values(validationStatus).every(Boolean)
                        ? "bg-gradient-to-r from-indigo-500 via-purple-600 to-pink-500 hover:from-indigo-600 hover:via-purple-700 hover:to-pink-600 hover:shadow-xl hover:scale-105"
                        : "bg-gradient-to-r from-slate-400 to-slate-500 cursor-not-allowed opacity-60"
                    } disabled:opacity-50 disabled:cursor-not-allowed`}
                  >
                    {loading && (
                      <div className="w-4 h-4 border-2 border-white/30 border-t-white rounded-full animate-spin" />
                    )}
                    <span>{loading ? "处理中..." : isEditMode ? "保存修改" : "创建用户"}</span>
                    {!loading && (
                      <ShieldCheck className="h-4 w-4 ml-1" />
                    )}
                  </Button>
                </div>
              </DialogFooter>
            </form>
          </Form>
        </div>
      </DialogContent>
    </Dialog>
  )
}

export default AddUserDialog 