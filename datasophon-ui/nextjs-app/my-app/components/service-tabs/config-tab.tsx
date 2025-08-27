"use client"

import React, { useState, useEffect, useCallback, useMemo } from 'react'
import { 
  Settings, Save, Download, Eye, FileText, Database, History, Users,
  Search, List, Grid3x3, Copy, Package
} from 'lucide-react'

import { Button } from '@/components/ui/button'
import { Input } from '@/components/ui/input'
import { Badge } from '@/components/ui/badge'
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from '@/components/ui/select'
import { Label } from '@/components/ui/label'
import { Dialog, DialogContent, DialogHeader, DialogTitle, DialogTrigger } from '@/components/ui/dialog'
import { ScrollArea } from '@/components/ui/scroll-area'
import { toast } from 'sonner'

import ConfigParameterForm from '@/components/config/ConfigParameterForm'
import { createClusterHeaders } from '@/lib/cluster-id-header'
import { apiV1, API_PATHS_V1, API_BASE_URL } from '@/lib/api-config-v1'

interface ConfigTabProps {
  serviceId: string
  serviceName: string
}

// 接口定义
interface ConfigVersion {
  version: number
  createTime: string
  isCurrent: boolean
}

interface RoleGroup {
  id: number
  roleGroupName: string
}

interface ConfigFile {
  fileName: string
  filePath: string
  fileSize: number
  lastModified: string
}

// 压缩格式接口定义
interface CompressFormat {
  format: string
  description: string
  supportPassword: boolean // 后端现在返回布尔值
}

export default function ConfigTab({ serviceId, serviceName }: ConfigTabProps) {
  // 主要状态
  const [hasLoadedConfigFiles, setHasLoadedConfigFiles] = useState(false)
  const [loading, setLoading] = useState(false)
  
  // 配置参数相关状态  
  const [configVersions, setConfigVersions] = useState<ConfigVersion[]>([])
  const [currentVersion, setCurrentVersion] = useState<number>()
  const [roleGroups, setRoleGroups] = useState<RoleGroup[]>([])
  const [currentRoleGroup, setCurrentRoleGroup] = useState<number>()
  const [compareMode, setCompareMode] = useState(false)
  const [compareVersion] = useState<number>()
  
  // 配置导出相关状态
  const [configFiles, setConfigFiles] = useState<ConfigFile[]>([])
  const [downloadModalVisible, setDownloadModalVisible] = useState(false)
  const [downloadFormats, setDownloadFormats] = useState<CompressFormat[]>([])
  const [selectedFormat, setSelectedFormat] = useState('zip')
  const [usePassword, setUsePassword] = useState(false)
  const [password, setPassword] = useState('')
  const [confirmPassword, setConfirmPassword] = useState('')
  const [downloadLoading, setDownloadLoading] = useState(false)
  const [downloadProgress, setDownloadProgress] = useState(0)
  
  // 配置文件预览相关状态
  const [previewVisible, setPreviewVisible] = useState(false)
  const [previewContent, setPreviewContent] = useState('')
  const [currentPreviewFile, setCurrentPreviewFile] = useState('')
  const [searchText, setSearchText] = useState('')
  const [viewMode, setViewMode] = useState<'list' | 'grid'>('list')

  // 获取集群ID
  const clusterId = new URLSearchParams(window.location.search).get('clusterId') || 
                   localStorage.getItem('clusterId') || 
                   '1'

  // 计算总文件大小
  const totalSize = useMemo(() => {
    const total = configFiles.reduce((sum, file) => {
      const size = parseFloat(file.fileSize?.toString() || '0')
      return sum + size
    }, 0)
    return total > 1024 ? `${(total / 1024).toFixed(2)} MB` : `${total.toFixed(2)} KB`
  }, [configFiles])

  // 获取配置版本
  const fetchConfigVersions = useCallback(async () => {
    if (!serviceId || !clusterId || !currentRoleGroup) return
    
    try {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.get(`${API_PATHS_V1.GET_CONFIG_VERSION}?serviceInstanceId=${serviceId}&roleGroupId=${currentRoleGroup}`, { headers })
      
      if (response.data.code === 200) {
        setConfigVersions(response.data.data || [])
        // 设置当前版本
        const current = response.data.data?.find((v: ConfigVersion) => v.isCurrent)
        if (current) {
          setCurrentVersion(current.version)
        }
      } else {
        toast.error(response.data.msg || '获取配置版本失败')
      }
    } catch (error) {
      console.error('获取配置版本失败:', error)
      toast.error('获取配置版本失败')
    }
  }, [serviceId, clusterId, currentRoleGroup])

  // 获取角色组列表
  const fetchRoleGroups = useCallback(async () => {
    if (!serviceId || !clusterId) return
    
    try {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.get(`${API_PATHS_V1.GET_ROLE_GROUP_LIST}?serviceInstanceId=${serviceId}`, { headers })
      
      if (response.data.code === 200) {
        setRoleGroups(response.data.data || [])
        if (response.data.data?.length > 0) {
          setCurrentRoleGroup(response.data.data[0].id)
        }
      } else {
        toast.error(response.data.msg || '获取角色组列表失败')
      }
    } catch (error) {
      console.error('获取角色组列表失败:', error)
      toast.error('获取角色组列表失败')
    }
  }, [serviceId, clusterId])

  // 获取支持的压缩格式
  const fetchSupportedFormats = useCallback(async () => {
    try {
      const response = await apiV1.get(API_PATHS_V1.GET_SUPPORTED_COMPRESS_FORMATS)
      
      if (response.data.code === 200) {
        setDownloadFormats(response.data.data || [])
      } else {
        // 使用默认格式作为后备
        setDownloadFormats([
          { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: true },
          { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: false },
          { format: '7z', description: '高压缩率，标准7z格式', supportPassword: false }
        ])
      }
    } catch (error) {
      console.error('获取压缩格式失败:', error)
      // 使用默认格式
      setDownloadFormats([
        { format: 'zip', description: '兼容性最佳，几乎所有系统都支持', supportPassword: true },
        { format: 'tar.gz', description: 'Linux/Unix系统常用格式，压缩率高', supportPassword: false },
        { format: '7z', description: '高压缩率，标准7z格式', supportPassword: false }
      ])
    }
  }, [])

  // 获取配置文件列表
  const fetchConfigFiles = useCallback(async () => {
    if (!serviceId || !clusterId) return
    
    setLoading(true)
    try {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.post(API_PATHS_V1.GET_SERVICE_CONFIG_FILES, {
        serviceInstanceId: serviceId
      }, { headers })
      
      if (response.data.code === 200) {
        setConfigFiles(response.data.data || [])
        // 如果还没有获取压缩格式，现在获取
        if (downloadFormats.length === 0) {
          fetchSupportedFormats()
        }
      } else {
        toast.error(response.data.msg || '获取配置文件列表失败')
      }
    } catch (error) {
      console.error('获取配置文件列表失败:', error)
      toast.error('获取配置文件列表失败')
    } finally {
      setLoading(false)
    }
  }, [serviceId, clusterId, downloadFormats.length, fetchSupportedFormats])

  // 预览配置文件
  const previewConfigFile = useCallback(async (file: ConfigFile) => {
    if (!serviceId || !clusterId) return
    
    setPreviewVisible(true)
    setCurrentPreviewFile(file.fileName)
    setPreviewContent('正在加载...')
    
    try {
      const headers = createClusterHeaders(clusterId)
      const response = await apiV1.post(API_PATHS_V1.PREVIEW_SERVICE_CONFIG_FILE, {
        serviceInstanceId: serviceId,
        fileName: file.fileName
      }, { headers })
      
      if (response.data.code === 200) {
        setPreviewContent(response.data.data || '')
      } else {
        setPreviewContent('预览失败: ' + (response.data.msg || '未知错误'))
      }
    } catch (error) {
      console.error('预览配置文件失败:', error)
      setPreviewContent('预览失败: 网络错误')
    }
  }, [serviceId, clusterId])

  // 下载单个配置文件
  const downloadSingleConfig = useCallback(async (file: ConfigFile) => {
    if (!serviceId || !clusterId) return
    
    try {
      // 使用fetch API并添加认证头
      const token = localStorage.getItem('jwt_token')
      const headers = createClusterHeaders(clusterId, {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      })
      const response = await fetch(`${API_BASE_URL}${API_PATHS_V1.DOWNLOAD_SINGLE_CONFIG_FILE}?serviceInstanceId=${serviceId}&fileName=${encodeURIComponent(file.fileName)}`, {
        method: 'GET',
        headers: headers,
        credentials: 'include'
      })

      if (!response.ok) {
        if (response.status === 401) {
          toast.error('认证失败，请重新登录')
          return
        }
        throw new Error('下载失败')
      }

      // 获取文件内容并创建下载链接
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = file.fileName
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
      toast.success('配置文件下载成功')
    } catch (error) {
      console.error('下载配置文件失败:', error)
      toast.error('下载配置文件失败')
    }
  }, [serviceId, clusterId])



  // 打包下载所有配置文件
  const downloadAllConfigs = useCallback(async () => {
    if (!serviceId || !clusterId) return
    
    // 验证密码
    if (usePassword) {
      if (!password || password !== confirmPassword) {
        toast.error('密码不匹配或为空')
        return
      }
    }
    
    setDownloadLoading(true)
    setDownloadProgress(0)
    
    try {
      let downloadUrl = `${API_BASE_URL}${API_PATHS_V1.DOWNLOAD_ALL_SERVICE_CONFIG_FILES}?serviceInstanceId=${serviceId}&format=${selectedFormat}`
      
      if (usePassword && password) {
        downloadUrl += `&password=${encodeURIComponent(password)}`
      }
      
      // 使用fetch API并添加认证头
      const token = localStorage.getItem('jwt_token')
      const headers = createClusterHeaders(clusterId, {
        'Authorization': token ? `Bearer ${token}` : '',
        'Content-Type': 'application/json'
      })
      
      // 模拟下载进度
      const progressInterval = setInterval(() => {
        setDownloadProgress(prev => {
          if (prev >= 90) {
            clearInterval(progressInterval)
            return prev
          }
          return prev + 10
        })
      }, 200)
      
      const response = await fetch(downloadUrl, {
        method: 'GET',
        headers: headers,
        credentials: 'include'
      })

      if (!response.ok) {
        clearInterval(progressInterval)
        if (response.status === 401) {
          toast.error('认证失败，请重新登录')
          return
        }
        throw new Error('下载失败')
      }

      // 获取文件内容并创建下载链接
      const blob = await response.blob()
      const url = window.URL.createObjectURL(blob)
      const link = document.createElement('a')
      link.href = url
      link.download = `${serviceName}_configs.${selectedFormat}`
      document.body.appendChild(link)
      link.click()
      document.body.removeChild(link)
      window.URL.revokeObjectURL(url)
      
      // 完成下载
      setDownloadProgress(100)
      setTimeout(() => {
        setDownloadModalVisible(false)
        setPassword('')
        setConfirmPassword('')
        toast.success(`${selectedFormat.toUpperCase()} 格式配置文件下载成功`)
        setDownloadProgress(0)
      }, 500)
      
    } catch (error) {
      console.error('下载配置文件失败:', error)
      toast.error('下载配置文件失败')
    } finally {
      setDownloadLoading(false)
    }
  }, [serviceId, clusterId, selectedFormat, usePassword, password, confirmPassword, serviceName])

  // 复制配置内容
  const copyContent = useCallback(async () => {
    try {
      await navigator.clipboard.writeText(previewContent)
      toast.success('内容已复制到剪贴板')
    } catch {
      toast.error('复制失败')
    }
  }, [previewContent])

  // 处理配置保存
  const handleSaveConfig = useCallback(async (configData: Record<string, unknown>) => {
    if (!serviceId || !clusterId || !currentRoleGroup) {
      toast.error('缺少必要参数')
      return
    }

    try {
      // 将配置数据转换为后端需要的格式
      const configItems = Object.entries(configData as Record<string, unknown>).map(([name, value]) => ({
        name: name.replaceAll('!', '.'), // 转换回原始格式
        value: value
      }))

      // 过滤掉空值和隐藏项
      const filteredItems = configItems.filter(item => 
        item.value !== null && item.value !== undefined && item.value !== ''
      )

      const headers = createClusterHeaders(clusterId)
      const userStr = typeof window !== 'undefined' ? localStorage.getItem('jwt_token') : null
      const currentUser = userStr ? JSON.parse(atob(userStr.split('.')[1])) : {}

      const saveParams = {
        clusterId: parseInt(clusterId),
        serviceName: serviceName,
        serviceConfig: JSON.stringify(filteredItems),
        roleGroupId: currentRoleGroup,
        description: `保存 ${serviceName} 配置 - ${new Date().toLocaleString()}`,
        userId: currentUser.id || 1,
        username: currentUser.username || 'admin'
      }

      const response = await apiV1.post(API_PATHS_V1.SAVE_SERVICE_CONFIG, saveParams, { headers })

      if (response.data.code === 200) {
        const versionCreated = response.data.versionCreated
        if (versionCreated === false) {
          toast.info('配置未发生变更，未生成新版本')
        } else {
          toast.success('配置保存成功，已生成新版本')
          // 重新获取版本列表
          fetchConfigVersions()
        }
      } else {
        toast.error(response.data.msg || '保存配置失败')
      }
    } catch (error) {
      console.error('保存配置失败:', error)
      toast.error('保存配置失败')
    }
  }, [serviceId, clusterId, currentRoleGroup, serviceName, fetchConfigVersions])

  // 获取文件图标类型和颜色
  const getFileIconClass = useCallback((fileName: string) => {
    if (!fileName) return 'text-gray-400'
    
    const extension = fileName.split('.').pop()?.toLowerCase() || ''
    
    // 配置文件类型
    if (extension === 'xml') return 'text-orange-500'
    if (extension === 'json') return 'text-purple-500' 
    if (['yaml', 'yml'].includes(extension)) return 'text-green-500'
    if (['properties', 'prop'].includes(extension)) return 'text-blue-500'
    if (['conf', 'cfg', 'ini', 'toml'].includes(extension)) return 'text-gray-600'
    
    // 脚本文件类型
    if (['sh', 'bash', 'zsh'].includes(extension)) return 'text-green-700'
    if (['bat', 'cmd', 'ps1'].includes(extension)) return 'text-blue-700'
    
    // 日志文件类型
    if (['log', 'out', 'err', 'trace'].includes(extension)) return 'text-yellow-600'
    
    // 数据文件类型
    if (['csv', 'tsv', 'xlsx', 'xls'].includes(extension)) return 'text-green-600'
    
    // 文本文件类型
    if (['txt', 'md', 'markdown', 'rst'].includes(extension)) return 'text-gray-700'
    
    // 压缩文件类型
    if (['zip', 'tar', 'gz', 'bz2', 'rar', '7z'].includes(extension)) return 'text-red-500'
    
    // 处理没有扩展名的特殊文件
    if (!extension || extension === fileName) {
      const lowerFileName = fileName.toLowerCase()
      if (['options', 'config', 'dockerfile', 'makefile', 'readme'].includes(lowerFileName)) {
        return 'text-blue-600'
      }
    }
    
    return 'text-gray-400'
  }, [])

  // 获取文件类型描述
  const getFileTypeDescription = useCallback((fileName: string) => {
    if (!fileName) return '文本文件'
    
    const extension = fileName.split('.').pop()?.toLowerCase() || ''
    
    const typeMap: Record<string, string> = {
      // 配置文件
      'xml': 'XML 配置文件',
      'json': 'JSON 配置文件', 
      'properties': 'Properties 配置文件',
      'prop': 'Properties 配置文件',
      'yaml': 'YAML 配置文件',
      'yml': 'YAML 配置文件',
      'conf': '配置文件',
      'cfg': '配置文件',
      'ini': 'INI 配置文件',
      'toml': 'TOML 配置文件',
      
      // 脚本文件
      'sh': 'Shell 脚本',
      'bash': 'Bash 脚本',
      'zsh': 'Zsh 脚本',
      'bat': 'Windows 批处理文件',
      'cmd': 'Windows 命令文件',
      'ps1': 'PowerShell 脚本',
      
      // 日志文件
      'log': '日志文件',
      'out': '输出日志',
      'err': '错误日志',
      'trace': '跟踪日志',
      
      // 数据文件
      'csv': 'CSV 数据文件',
      'tsv': 'TSV 数据文件',
      'xlsx': 'Excel 文件',
      'xls': 'Excel 文件',
      
      // 文本文件
      'txt': '文本文件',
      'md': 'Markdown 文件',
      'markdown': 'Markdown 文件',
      'rst': 'reStructuredText 文件',
      
      // 压缩文件
      'zip': 'ZIP 压缩文件',
      'tar': 'TAR 归档文件',
      'gz': 'GZIP 压缩文件',
      'bz2': 'BZIP2 压缩文件',
      'rar': 'RAR 压缩文件',
      '7z': '7-Zip 压缩文件'
    }
    
    return typeMap[extension] || '配置文件'
  }, [])

  // 检查是否支持密码保护
  const isPasswordSupported = useMemo(() => {
    const currentFormat = downloadFormats.find(f => f.format === selectedFormat)
    if (!currentFormat) return false
    
    return currentFormat.supportPassword === true
  }, [downloadFormats, selectedFormat])

  // 过滤配置文件
  const filteredConfigFiles = useMemo(() => {
    return configFiles.filter(file => 
      file.fileName.toLowerCase().includes(searchText.toLowerCase())
    )
  }, [configFiles, searchText])

      // 渲染导出对话框内容
  const renderExportDialog = () => (
    <div className="h-full flex flex-col">
      {/* 顶部统计信息 */}
      <div className="grid grid-cols-2 lg:grid-cols-4 gap-4 lg:gap-6 mb-8">
        <div className="bg-gradient-to-br from-blue-50 via-blue-100 to-blue-200 rounded-3xl p-6 border border-blue-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center space-x-4">
            <div className="p-4 bg-gradient-to-br from-blue-500 to-blue-600 rounded-2xl shadow-lg">
              <FileText className="w-6 h-6 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-blue-700 font-semibold text-sm mb-1">配置文件总数</p>
              <p className="text-3xl font-bold text-blue-900 leading-tight">{configFiles.length}</p>
              <p className="text-blue-600 text-xs mt-1">个文件</p>
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-emerald-50 via-emerald-100 to-emerald-200 rounded-3xl p-6 border border-emerald-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center space-x-4">
            <div className="p-4 bg-gradient-to-br from-emerald-500 to-emerald-600 rounded-2xl shadow-lg">
              <Database className="w-6 h-6 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-emerald-700 font-semibold text-sm mb-1">文件总大小</p>
              <p className="text-3xl font-bold text-emerald-900 leading-tight">{totalSize}</p>
              <p className="text-emerald-600 text-xs mt-1">压缩后约 {(parseFloat(totalSize) * 0.3).toFixed(1)} {totalSize.includes('MB') ? 'MB' : 'KB'}</p>
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-purple-50 via-purple-100 to-purple-200 rounded-3xl p-6 border border-purple-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center space-x-4">
            <div className="p-4 bg-gradient-to-br from-purple-500 to-purple-600 rounded-2xl shadow-lg">
              <Package className="w-6 h-6 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-purple-700 font-semibold text-sm mb-1">压缩格式</p>
              <p className="text-3xl font-bold text-purple-900 leading-tight">{selectedFormat.toUpperCase()}</p>
              <p className="text-purple-600 text-xs mt-1">
                {downloadFormats.find(f => f.format === selectedFormat)?.description?.split('，')[0] || '高效压缩'}
              </p>
            </div>
          </div>
        </div>

        <div className="bg-gradient-to-br from-amber-50 via-amber-100 to-amber-200 rounded-3xl p-6 border border-amber-200 shadow-lg hover:shadow-xl transition-all duration-300 transform hover:-translate-y-1">
          <div className="flex items-center space-x-4">
            <div className={`p-4 rounded-2xl shadow-lg ${
              usePassword 
                ? 'bg-gradient-to-br from-green-500 to-green-600' 
                : 'bg-gradient-to-br from-gray-400 to-gray-500'
            }`}>
              <Download className="w-6 h-6 text-white" />
            </div>
            <div className="flex-1">
              <p className="text-amber-700 font-semibold text-sm mb-1">安全保护</p>
              <p className={`text-3xl font-bold leading-tight ${
                usePassword ? 'text-green-900' : 'text-gray-700'
              }`}>
                {usePassword ? '已启用' : '未启用'}
              </p>
              <p className="text-amber-600 text-xs mt-1">
                {usePassword ? '密码保护已设置' : isPasswordSupported ? '可设置密码保护' : '当前格式不支持'}
              </p>
            </div>
          </div>
        </div>
      </div>

      {/* 主要内容区域 */}
      <div className="flex-1 flex gap-6 min-h-0">
        {/* 左侧文件列表 */}
        <div className="flex-1 flex flex-col min-w-0">
          {/* 搜索和控制栏 */}
          <div className="flex flex-col sm:flex-row items-stretch sm:items-center justify-between bg-gradient-to-br from-slate-50 via-gray-50 to-blue-50 rounded-3xl p-6 mb-6 border border-gray-200 shadow-lg">
            <div className="relative flex-1 max-w-lg mb-4 sm:mb-0">
              <Search className="w-5 h-5 absolute left-5 top-1/2 transform -translate-y-1/2 text-gray-400" />
              <Input
                placeholder="搜索配置文件名称、类型或路径..."
                value={searchText}
                onChange={(e) => setSearchText(e.target.value)}
                className="pl-14 pr-6 py-4 rounded-2xl border-2 border-gray-200 bg-white shadow-sm text-base focus:border-blue-400 focus:ring-blue-400 transition-all duration-200"
              />
              {searchText && (
                <div className="absolute right-4 top-1/2 transform -translate-y-1/2">
                  <Badge variant="secondary" className="text-xs">
                    {filteredConfigFiles.length} 个结果
                  </Badge>
                </div>
              )}
            </div>
            <div className="flex items-center gap-4 sm:ml-6">
              <div className="flex items-center bg-white rounded-2xl p-1.5 shadow-lg border border-gray-200">
                <Button
                  size="sm"
                  variant={viewMode === 'list' ? "default" : "ghost"}
                  onClick={() => setViewMode('list')}
                  className={`rounded-xl px-5 py-2.5 transition-all duration-200 ${
                    viewMode === 'list' 
                      ? 'bg-gradient-to-r from-blue-500 to-blue-600 text-white shadow-md' 
                      : 'hover:bg-gray-100'
                  }`}
                >
                  <List className="w-4 h-4 mr-2" />
                  列表视图
                </Button>
                <Button
                  size="sm"
                  variant={viewMode === 'grid' ? "default" : "ghost"}
                  onClick={() => setViewMode('grid')}
                  className={`rounded-xl px-5 py-2.5 transition-all duration-200 ${
                    viewMode === 'grid' 
                      ? 'bg-gradient-to-r from-blue-500 to-blue-600 text-white shadow-md' 
                      : 'hover:bg-gray-100'
                  }`}
                >
                  <Grid3x3 className="w-4 h-4 mr-2" />
                  网格视图
                </Button>
              </div>
            </div>
          </div>

          {/* 文件列表区域 */}
          <div className="flex-1 bg-white rounded-3xl border-2 border-gray-100 shadow-lg overflow-hidden">
            {loading ? (
              <div className="flex items-center justify-center h-full">
                <div className="text-center">
                  <div className="animate-spin w-12 h-12 border-4 border-blue-500 border-t-transparent rounded-full mx-auto mb-4"></div>
                  <p className="text-lg text-gray-600">加载配置文件中...</p>
                </div>
              </div>
            ) : filteredConfigFiles.length === 0 ? (
              <div className="text-center py-20">
                <FileText className="w-20 h-20 text-gray-300 mx-auto mb-6" />
                <h4 className="text-xl font-semibold text-gray-700 mb-3">暂无配置文件</h4>
                <p className="text-gray-500 text-lg">
                  {searchText ? '没有找到匹配的配置文件' : '当前服务没有配置文件'}
                </p>
              </div>
            ) : (
              <ScrollArea className="h-full">
                {viewMode === 'list' ? (
                  <div className="divide-y divide-gray-50">
                    {filteredConfigFiles.map((file, index) => (
                      <div key={index} className="p-6 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 transition-all duration-300 group">
                        <div className="flex items-center justify-between">
                          <div className="flex items-center space-x-5 flex-1">
                            <div className={`p-4 rounded-2xl shadow-sm group-hover:shadow-md transition-all duration-200 ${
                              getFileIconClass(file.fileName).includes('orange') ? 'bg-gradient-to-br from-orange-50 to-orange-100' :
                              getFileIconClass(file.fileName).includes('purple') ? 'bg-gradient-to-br from-purple-50 to-purple-100' :
                              getFileIconClass(file.fileName).includes('green') ? 'bg-gradient-to-br from-green-50 to-green-100' :
                              getFileIconClass(file.fileName).includes('blue') ? 'bg-gradient-to-br from-blue-50 to-blue-100' :
                              'bg-gradient-to-br from-gray-50 to-gray-100'
                            }`}>
                              <FileText className={`w-7 h-7 ${getFileIconClass(file.fileName)}`} />
                            </div>
                            <div className="flex-1 min-w-0">
                              <h4 className="text-xl font-bold text-gray-900 truncate mb-2 group-hover:text-blue-700 transition-colors duration-200">
                                {file.fileName}
                              </h4>
                              <div className="flex flex-wrap items-center gap-3 text-sm">
                                <Badge variant="outline" className="bg-white">
                                  {getFileTypeDescription(file.fileName)}
                                </Badge>
                                <span className="text-gray-600 flex items-center">
                                  <Database className="w-4 h-4 mr-1" />
                                  {typeof file.fileSize === 'number' ? `${file.fileSize} KB` : file.fileSize}
                                </span>
                                <span className="text-gray-600 flex items-center">
                                  <History className="w-4 h-4 mr-1" />
                                  {file.lastModified}
                                </span>
                              </div>
                            </div>
                          </div>
                          <div className="flex items-center space-x-3">
                            <Button
                              size="default"
                              variant="outline"
                              onClick={() => previewConfigFile(file)}
                              className="text-blue-600 hover:text-white hover:bg-gradient-to-r hover:from-blue-500 hover:to-blue-600 border-blue-200 hover:border-blue-500 px-6 py-3 rounded-xl transition-all duration-200 shadow-sm hover:shadow-md"
                            >
                              <Eye className="w-4 h-4 mr-2" />
                              预览文件
                            </Button>
                            <Button
                              size="default"
                              variant="outline"
                              onClick={() => downloadSingleConfig(file)}
                              className="text-emerald-600 hover:text-white hover:bg-gradient-to-r hover:from-emerald-500 hover:to-emerald-600 border-emerald-200 hover:border-emerald-500 px-6 py-3 rounded-xl transition-all duration-200 shadow-sm hover:shadow-md"
                            >
                              <Download className="w-4 h-4 mr-2" />
                              立即下载
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                ) : (
                  <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 xl:grid-cols-4 gap-6 p-6">
                    {filteredConfigFiles.map((file, index) => (
                      <div key={index} className="bg-white border-2 border-gray-100 rounded-2xl p-6 hover:border-blue-200 hover:shadow-lg transition-all duration-200">
                        <div className="text-center">
                          <div className="p-4 bg-gray-50 rounded-2xl w-16 h-16 mx-auto mb-4 flex items-center justify-center">
                            <FileText className={`w-8 h-8 ${getFileIconClass(file.fileName)}`} />
                          </div>
                          <h4 className="text-sm font-semibold text-gray-900 truncate mb-2" title={file.fileName}>
                            {file.fileName}
                          </h4>
                          <p className="text-xs text-gray-500 mb-4">
                            {typeof file.fileSize === 'number' ? `${file.fileSize} KB` : file.fileSize}
                          </p>
                          <div className="flex justify-center space-x-2">
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => previewConfigFile(file)}
                              className="text-blue-600 border-blue-200 px-3"
                            >
                              <Eye className="w-3 h-3" />
                            </Button>
                            <Button
                              size="sm"
                              variant="outline"
                              onClick={() => downloadSingleConfig(file)}
                              className="text-green-600 border-green-200 px-3"
                            >
                              <Download className="w-3 h-3" />
                            </Button>
                          </div>
                        </div>
                      </div>
                    ))}
                  </div>
                )}
              </ScrollArea>
            )}
          </div>
        </div>

        {/* 右侧批量下载设置 */}
        <div className="w-80 flex flex-col">
          <div className="bg-white rounded-2xl border border-gray-200 shadow-lg p-6 flex-1">
            <h3 className="text-lg font-bold text-gray-800 mb-6 flex items-center">
              <Package className="w-5 h-5 mr-2 text-blue-600" />
              批量下载设置
            </h3>
            
            <div className="space-y-6">
              <div>
                <Label className="text-base font-semibold text-gray-700 mb-3 block">压缩格式</Label>
                <Select value={selectedFormat} onValueChange={setSelectedFormat}>
                  <SelectTrigger className="h-12 rounded-xl border-2 border-gray-200 hover:border-blue-400 transition-all duration-200 bg-white text-lg font-medium">
                    <SelectValue>
                      {selectedFormat.toUpperCase()}
                    </SelectValue>
                  </SelectTrigger>
                  <SelectContent className="w-80 rounded-2xl border border-gray-200 shadow-2xl bg-white/95 backdrop-blur-sm p-2">
                    {downloadFormats.map((format) => (
                      <SelectItem 
                        key={format.format} 
                        value={format.format} 
                        className="p-4 rounded-xl mb-2 hover:bg-gradient-to-r hover:from-blue-50 hover:to-indigo-50 border border-transparent hover:border-blue-200 transition-all duration-200 cursor-pointer"
                      >
                        <div className="flex flex-col space-y-2 w-full">
                          <div className="flex items-center justify-between">
                            <div className="flex items-center space-x-3">
                              <div className={`p-2 rounded-lg ${
                                format.supportPassword ? 'bg-green-100' : 'bg-gray-100'
                              }`}>
                                <Package className={`w-4 h-4 ${
                                  format.supportPassword ? 'text-green-600' : 'text-gray-500'
                                }`} />
                              </div>
                              <span className="font-bold text-lg text-gray-900">{format.format.toUpperCase()}</span>
                            </div>
                            {format.supportPassword === true && (
                              <Badge className="bg-gradient-to-r from-green-500 to-green-600 text-white text-xs px-2 py-1">
                                🔒 支持密码
                              </Badge>
                            )}
                          </div>
                          <p className="text-sm text-gray-600 leading-relaxed pl-10">{format.description}</p>
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 密码保护设置区域 - 固定占位避免移动 */}
              <div className="space-y-4">
                <div className="flex items-center justify-between">
                  <Label className="text-base font-semibold text-gray-700">密码保护</Label>
                  {isPasswordSupported ? (
                    <button
                      type="button"
                      onClick={() => setUsePassword(!usePassword)}
                      className={`relative inline-flex h-6 w-11 items-center rounded-full transition-colors duration-200 ease-in-out focus:outline-none focus:ring-2 focus:ring-blue-500 focus:ring-offset-2 ${
                        usePassword ? 'bg-blue-600' : 'bg-gray-200'
                      }`}
                    >
                      <span
                        className={`inline-block h-4 w-4 transform rounded-full bg-white transition-transform duration-200 ease-in-out ${
                          usePassword ? 'translate-x-6' : 'translate-x-1'
                        }`}
                      />
                    </button>
                  ) : (
                    <span className="text-sm text-gray-500">不支持</span>
                  )}
                </div>
                
                {/* 固定高度的密码输入区域 - 防止布局移动 */}
                <div className="min-h-[140px]">
                  {isPasswordSupported && (
                    <div className={`space-y-4 pt-2 transition-opacity duration-200 ${
                      usePassword ? 'opacity-100' : 'opacity-30 pointer-events-none'
                    }`}>
                      <div>
                        <Label className="text-sm font-medium text-gray-600 mb-2 block">密码</Label>
                        <Input 
                          type="password" 
                          value={password} 
                          onChange={(e) => setPassword(e.target.value)}
                          placeholder="请输入密码"
                          className="h-10 rounded-lg"
                          disabled={!usePassword}
                        />
                      </div>
                      <div>
                        <Label className="text-sm font-medium text-gray-600 mb-2 block">确认密码</Label>
                        <Input 
                          type="password" 
                          value={confirmPassword} 
                          onChange={(e) => setConfirmPassword(e.target.value)}
                          placeholder="请再次输入密码"
                          className={`h-10 rounded-lg ${
                            confirmPassword && password !== confirmPassword 
                              ? 'border-red-300 focus:border-red-400' 
                              : ''
                          }`}
                          disabled={!usePassword}
                        />
                        <div className="h-5 mt-1">
                          {confirmPassword && password !== confirmPassword && usePassword && (
                            <p className="text-xs text-red-500">密码不匹配</p>
                          )}
                        </div>
                      </div>
                    </div>
                  )}
                </div>
              </div>

              {downloadProgress > 0 && (
                <div className="border border-blue-200 rounded-lg p-3 bg-blue-50">
                  <div className="flex justify-between text-sm text-blue-700 mb-2">
                    <span>下载进度</span>
                    <span className="font-medium">{downloadProgress}%</span>
                  </div>
                  <div className="w-full bg-blue-100 rounded-full h-2 overflow-hidden">
                    <div 
                      className="bg-blue-500 h-2 rounded-full transition-all duration-300" 
                      style={{ width: `${downloadProgress}%` }}
                    />
                  </div>
                </div>
              )}
            </div>
          </div>

          {/* 安全提示 */}
          <div className="mt-4 p-3 bg-amber-50 border border-amber-200 rounded-lg text-xs text-amber-700">
            <div className="flex items-center">
              <Settings className="w-3 h-3 mr-2 flex-shrink-0" />
              <span>配置文件包含敏感信息，请妥善保管</span>
            </div>
          </div>
          
          {/* 底部操作按钮 */}
          <div className="flex gap-3 mt-6">
            <Button 
              variant="outline" 
              onClick={() => setDownloadModalVisible(false)}
              className="flex-1 h-11 rounded-lg"
            >
              取消
            </Button>
            <Button 
              onClick={downloadAllConfigs} 
              disabled={downloadLoading || (usePassword && (!password || password !== confirmPassword))}
              className="flex-1 h-11 rounded-lg"
            >
              {downloadLoading ? (
                <span className="flex items-center">
                  <div className="animate-spin rounded-full h-4 w-4 border-b-2 border-white mr-2" />
                  下载中...
                </span>
              ) : (
                <span className="flex items-center">
                  <Download className="w-4 h-4 mr-2" />
                  下载
                </span>
              )}
            </Button>
          </div>
        </div>
      </div>
    </div>
  )

  // 初始化数据
  useEffect(() => {
    if (serviceId && clusterId) {
      fetchRoleGroups()
    }
  }, [serviceId, clusterId, fetchRoleGroups])

  // 当角色组选择变化时，获取配置版本
  useEffect(() => {
    if (currentRoleGroup) {
      fetchConfigVersions()
    }
  }, [currentRoleGroup, fetchConfigVersions])

  return (
    <div className="h-full flex flex-col bg-gray-50">
      {/* 页面头部 - 苹果风格 */}
      <div className="bg-white p-8 border-b border-gray-100 shadow-sm">
        <div className="flex items-center justify-between">
          <div>
            <h1 className="text-3xl font-bold text-gray-900 mb-2">服务配置</h1>
            <p className="text-lg text-gray-600">管理 {serviceName} 服务的配置参数</p>
          </div>
          
          {/* 导出配置按钮 - 苹果风格 */}
          <Dialog open={downloadModalVisible} onOpenChange={setDownloadModalVisible}>
            <DialogTrigger asChild>
              <Button 
                size="lg"
                className="bg-gradient-to-r from-blue-500 to-blue-600 hover:from-blue-600 hover:to-blue-700 text-white px-8 py-4 rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300 transform hover:scale-105"
                onClick={() => {
                  if (!hasLoadedConfigFiles) {
                    fetchConfigFiles()
                    setHasLoadedConfigFiles(true)
                  }
                }}
              >
                <Download className="w-5 h-5 mr-3" />
                导出配置
          </Button>
            </DialogTrigger>
            {hasLoadedConfigFiles && (
              <DialogContent className="sm:max-w-[98vw] sm:w-[98vw] h-[90vh] overflow-hidden rounded-3xl">
                <DialogHeader className="pb-4">
                  <DialogTitle className="text-2xl font-semibold">导出配置文件</DialogTitle>
                </DialogHeader>
                <div className="flex-1 overflow-hidden">
                  {renderExportDialog()}
                </div>
              </DialogContent>
            )}
          </Dialog>
        </div>
      </div>

      {/* 主要内容区域 - 配置参数 */}
      <div className="flex-1 p-8">
        <div className="space-y-8">
          {/* 版本和角色组选择 - 苹果风格卡片 */}
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-8">
            <div className="flex flex-wrap items-center gap-6">
              <div className="flex items-center gap-3">
                <div className="p-3 bg-blue-100 rounded-2xl">
                  <History className="w-5 h-5 text-blue-600" />
                </div>
                <div>
                  <Label className="text-sm font-medium text-gray-700">配置版本</Label>
                  <Select value={currentVersion?.toString()} onValueChange={(value) => setCurrentVersion(parseInt(value))}>
                    <SelectTrigger className="w-40 mt-1 rounded-xl">
                      <SelectValue placeholder="选择版本" />
                    </SelectTrigger>
                    <SelectContent>
                      {configVersions.map((version) => (
                        <SelectItem key={version.version} value={version.version.toString()}>
                          版本 {version.version}
                          {version.isCurrent && <Badge variant="secondary" className="ml-2">当前</Badge>}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>
              
              <div className="flex items-center gap-3">
                <div className="p-3 bg-green-100 rounded-2xl">
                  <Users className="w-5 h-5 text-green-600" />
                </div>
                <div>
                  <Label className="text-sm font-medium text-gray-700">角色组</Label>
                  <Select value={currentRoleGroup?.toString()} onValueChange={(value) => setCurrentRoleGroup(parseInt(value))}>
                    <SelectTrigger className="w-48 mt-1 rounded-xl">
                      <SelectValue placeholder="选择角色组" />
                    </SelectTrigger>
                    <SelectContent>
                      {roleGroups.map((group) => (
                        <SelectItem key={group.id} value={group.id.toString()}>
                          {group.roleGroupName}
                        </SelectItem>
                      ))}
                    </SelectContent>
                  </Select>
                </div>
              </div>

              <div className="flex items-center gap-3">
                <div className="p-3 bg-purple-100 rounded-2xl">
                  <Eye className="w-5 h-5 text-purple-600" />
                </div>
                <div>
                  <Label className="text-sm font-medium text-gray-700">版本对比</Label>
                  <Button 
                    size="sm"
                    variant={compareMode ? "default" : "outline"}
                    onClick={() => setCompareMode(!compareMode)}
                    className="mt-1 rounded-xl"
                  >
                    {compareMode ? "关闭" : "开启"}
                  </Button>
                </div>
              </div>

              <div className="ml-auto">
                <Button className="bg-gradient-to-r from-green-500 to-green-600 hover:from-green-600 hover:to-green-700 text-white px-6 py-3 rounded-2xl shadow-lg hover:shadow-xl transition-all duration-300">
                  <Save className="w-5 h-5 mr-2" />
                  保存配置
                </Button>
              </div>
            </div>
          </div>

          {/* 配置表单区域 - 苹果风格 */}
          <div className="bg-white rounded-3xl shadow-lg border border-gray-100 p-8">
            <ConfigParameterForm
              serviceId={serviceId}
              currentVersion={currentVersion}
              currentRoleGroup={currentRoleGroup}
              className="min-h-[400px]"
            />
          </div>
        </div>
      </div>

      {/* 配置文件预览对话框 */}
      <Dialog open={previewVisible} onOpenChange={setPreviewVisible}>
        <DialogContent className="sm:max-w-[85vw] sm:w-[85vw] max-w-[85vw] w-[85vw] h-[88vh] max-h-[88vh] overflow-hidden bg-gradient-to-br from-white via-gray-50 to-gray-100 border-2 border-gray-300 shadow-2xl rounded-2xl p-0" showCloseButton={true}>
          <div style={{ height: 'calc(88vh - 2rem)', display: 'flex', flexDirection: 'column', padding: '1.5rem' }}>
            <DialogHeader className="border-b border-gray-200 pb-4 mb-4" style={{ flexShrink: 0 }}>
              <DialogTitle className="text-xl font-bold flex items-center gap-3">
                <div className={`p-3 rounded-xl shadow-md border border-gray-200 ${
                  getFileIconClass(currentPreviewFile).includes('orange') ? 'bg-orange-100' :
                  getFileIconClass(currentPreviewFile).includes('purple') ? 'bg-purple-100' :
                  getFileIconClass(currentPreviewFile).includes('green') ? 'bg-green-100' :
                  getFileIconClass(currentPreviewFile).includes('blue') ? 'bg-blue-100' :
                  'bg-gray-100'
                }`}>
                  <FileText className={`w-6 h-6 ${getFileIconClass(currentPreviewFile)}`} />
                </div>
                <div className="flex-1">
                  <div className="text-xl font-bold text-gray-900">{currentPreviewFile}</div>
                  <div className="text-sm text-gray-600 font-normal">
                    {getFileTypeDescription(currentPreviewFile)} • 配置文件预览
                  </div>
                </div>
              </DialogTitle>
            </DialogHeader>
            
            <div className="flex items-center justify-between gap-4 mb-4 p-4 bg-blue-50 rounded-xl border border-blue-200" style={{ flexShrink: 0 }}>
              <div className="flex items-center gap-6 text-sm">
                <span className="text-gray-700">行数: <strong className="text-blue-700">{previewContent.split('\n').length}</strong></span>
                <span className="text-gray-700">大小: <strong className="text-blue-700">{new Blob([previewContent]).size} 字节</strong></span>
                <span className="text-gray-700">编码: <strong className="text-blue-700">UTF-8</strong></span>
              </div>
              <Button
                size="sm"
                variant="outline"
                onClick={copyContent}
                className="flex items-center gap-2"
              >
                <Copy className="w-4 h-4" />
                复制内容
              </Button>
            </div>
            
            {/* 代码查看区域 - 使用原生滚动 */}
            <div 
              className="border-2 border-gray-300 rounded-xl bg-white shadow-inner" 
              style={{ 
                flex: '1 1 0',
                minHeight: 0,
                overflow: 'auto',
                position: 'relative'
              }}
            >
              <table style={{ width: '100%', borderCollapse: 'collapse' }}>
                <tbody>
                  {previewContent.split('\n').map((line, index) => (
                    <tr key={index}>
                      <td 
                        style={{ 
                          width: `${Math.max(50, String(previewContent.split('\n').length).length * 10 + 20)}px`,
                          padding: '0 10px',
                          textAlign: 'right',
                          color: '#6b7280',
                          fontSize: '13px',
                          fontFamily: 'monospace',
                          backgroundColor: '#f3f4f6',
                          borderRight: '1px solid #e5e7eb',
                          userSelect: 'none',
                          lineHeight: '1.75rem'
                        }}
                      >
                        {index + 1}
                      </td>
                      <td 
                        style={{ 
                          padding: '0 20px',
                          fontSize: '13px',
                          fontFamily: 'monospace',
                          lineHeight: '1.75rem',
                          whiteSpace: 'pre',
                          color: '#1f2937'
                        }}
                      >
                        {line || ' '}
                      </td>
                    </tr>
                  ))}
                </tbody>
              </table>
            </div>
          </div>
        </DialogContent>
      </Dialog>
    </div>
  )
}