"use client"

import React, { useState, useEffect } from "react"
import { zodResolver } from "@hookform/resolvers/zod"
import { useForm } from "react-hook-form"
import * as z from "zod"
import { User, Mail, Phone, Lock, Eye, EyeOff } from "lucide-react"
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
    .min(6, "密码至少6个字符")
    .max(20, "密码最多20个字符")
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
        password: z.string().min(6, "密码至少6个字符").max(20, "密码最多20个字符"),
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
      password: isEditMode ? true : !!watchPassword && watchPassword.length >= 6,
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
      <DialogContent className="sm:max-w-[500px]">
        <DialogHeader>
          <DialogTitle className="text-xl font-semibold">{title}</DialogTitle>
          <DialogDescription>{description}</DialogDescription>
        </DialogHeader>

        <Form {...form}>
          <form onSubmit={form.handleSubmit(onSubmit)} className="space-y-6">
            <div className="space-y-4">
              {/* 用户名字段 */}
              <FormField
                control={form.control}
                name="username"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <div
                        className={`w-2 h-2 rounded-full transition-colors ${
                          validationStatus.username
                            ? "bg-green-500 shadow-green-500/50 shadow-sm"
                            : "bg-red-500 shadow-red-500/50 shadow-sm animate-pulse"
                        }`}
                      />
                      <User className="h-4 w-4" />
                      用户名称
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="请输入用户名称"
                        {...field}
                        disabled={loading}
                        maxLength={20}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 密码字段 */}
              <FormField
                control={form.control}
                name="password"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <div
                        className={`w-2 h-2 rounded-full transition-colors ${
                          validationStatus.password
                            ? "bg-green-500 shadow-green-500/50 shadow-sm"
                            : "bg-red-500 shadow-red-500/50 shadow-sm animate-pulse"
                        }`}
                      />
                      <Lock className="h-4 w-4" />
                      {isEditMode ? "新密码（留空不修改）" : "用户密码"}
                    </FormLabel>
                    <FormControl>
                      <div className="relative">
                        <Input
                          type={showPassword ? "text" : "password"}
                          placeholder={isEditMode ? "留空则不修改密码" : "请输入用户密码"}
                          {...field}
                          disabled={loading}
                          maxLength={20}
                        />
                        <Button
                          type="button"
                          variant="ghost"
                          size="sm"
                          className="absolute right-0 top-0 h-full px-3 py-2 hover:bg-transparent"
                          onClick={() => setShowPassword(!showPassword)}
                        >
                          {showPassword ? (
                            <EyeOff className="h-4 w-4 text-gray-400" />
                          ) : (
                            <Eye className="h-4 w-4 text-gray-400" />
                          )}
                        </Button>
                      </div>
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 邮箱字段 */}
              <FormField
                control={form.control}
                name="email"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <div
                        className={`w-2 h-2 rounded-full transition-colors ${
                          validationStatus.email
                            ? "bg-green-500 shadow-green-500/50 shadow-sm"
                            : "bg-red-500 shadow-red-500/50 shadow-sm animate-pulse"
                        }`}
                      />
                      <Mail className="h-4 w-4" />
                      邮箱地址
                    </FormLabel>
                    <FormControl>
                      <Input
                        type="email"
                        placeholder="请输入邮箱地址"
                        {...field}
                        disabled={loading}
                        maxLength={50}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />

              {/* 手机字段 */}
              <FormField
                control={form.control}
                name="phone"
                render={({ field }) => (
                  <FormItem>
                    <FormLabel className="flex items-center gap-2">
                      <div
                        className={`w-2 h-2 rounded-full transition-colors ${
                          validationStatus.phone
                            ? "bg-green-500 shadow-green-500/50 shadow-sm"
                            : "bg-red-500 shadow-red-500/50 shadow-sm animate-pulse"
                        }`}
                      />
                      <Phone className="h-4 w-4" />
                      手机号码
                    </FormLabel>
                    <FormControl>
                      <Input
                        placeholder="请输入手机号码"
                        {...field}
                        disabled={loading}
                        maxLength={11}
                      />
                    </FormControl>
                    <FormMessage />
                  </FormItem>
                )}
              />
            </div>

            <DialogFooter>
              <Button
                type="button"
                variant="outline"
                onClick={handleCancel}
                disabled={loading}
              >
                取消
              </Button>
              <Button type="submit" disabled={loading}>
                {loading ? "处理中..." : isEditMode ? "保存" : "创建"}
              </Button>
            </DialogFooter>
          </form>
        </Form>
      </DialogContent>
    </Dialog>
  )
}

export default AddUserDialog 