"use client"

import { useState, useEffect } from "react"
import { HardDrive, Plus, Search, Trash2, Edit, CheckCircle, AlertCircle, Globe, FolderOpen, Settings } from "lucide-react"

import { Button } from "@/components/ui/button"
import { Input } from "@/components/ui/input"
import { Card, CardContent, CardHeader } from "@/components/ui/card"
import { Badge } from "@/components/ui/badge"
import { Dialog, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from "@/components/ui/dialog"
import { Label } from "@/components/ui/label"
import { Textarea } from "@/components/ui/textarea"
import { Select, SelectContent, SelectItem, SelectTrigger, SelectValue } from "@/components/ui/select"
import FinalNavbar from "../layout/navbar-final"
import { apiV1, API_PATHS_V1 } from "@/lib/api-config-v1"
import { appleToast } from "@/lib/apple-toast"

// 存储库类型枚举
const REPO_TYPES = [
  { value: 'local', label: '本地存储库', supported: true, icon: FolderOpen },
  { value: 'http', label: 'HTTP', supported: true, icon: Globe },
  { value: 'https', label: 'HTTPS', supported: false, icon: Globe },
  { value: 's3', label: 'Amazon S3', supported: false, icon: Globe },
  { value: 'hdfs', label: 'HDFS', supported: false, icon: Globe },
  { value: 'oss', label: '阿里云OSS', supported: false, icon: Globe },
]

// 定义存储库类型
interface Repository {
  id: string  // 使用string避免雪花算法ID丢失精度
  repoName: string
  repoType: string
  repoUrl: string
  frameCode?: string
  description?: string
  isDefault: number
  status: number
  createdAt: string
  updatedAt: string
}

export default function ClusterStorage() {
  const [loading, setLoading] = useState(false)
  const [repositories, setRepositories] = useState<Repository[]>([])
  const [searchTerm, setSearchTerm] = useState("")
  
  // 对话框状态
  const [addDialogOpen, setAddDialogOpen] = useState(false)
  const [editDialogOpen, setEditDialogOpen] = useState(false)
  const [deleteDialogOpen, setDeleteDialogOpen] = useState(false)
  
  // 表单状态
  const [formData, setFormData] = useState({
    id: "0",  // 使用string类型
    repoName: "",
    repoType: "http",
    repoUrl: "",
    description: "",
  })
  
  const [selectedRepo, setSelectedRepo] = useState<Repository | null>(null)
  const [testingConnection, setTestingConnection] = useState(false)
  const [testResult, setTestResult] = useState("")

  // 获取存储库列表
  const fetchRepositories = async () => {
    setLoading(true)
    try {
      const response = await apiV1.get(API_PATHS_V1.PARCEL_REPOSITORY_LIST)
      if (response.data.code === 200) {
        setRepositories(response.data.data || [])
      }
    } catch (error) {
      console.error('获取存储库列表失败:', error)
      appleToast.error("获取存储库列表失败")
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchRepositories()
  }, [])

  // 过滤存储库
  const filteredRepositories = repositories.filter(repo =>
    repo.repoName.toLowerCase().includes(searchTerm.toLowerCase()) ||
    repo.repoUrl.toLowerCase().includes(searchTerm.toLowerCase())
  )

  // 打开添加对话框
  const handleAddRepository = () => {
    setFormData({
      id: "0",  // 使用string类型
      repoName: "",
      repoType: "http",
      repoUrl: "",
      description: "",
    })
    setTestResult("")
    setAddDialogOpen(true)
  }

  // 打开编辑对话框
  const handleEditRepository = (repo: Repository) => {
    setFormData({
      id: repo.id,
      repoName: repo.repoName,
      repoType: repo.repoType,
      repoUrl: repo.repoUrl,
      description: repo.description || "",
    })
    setSelectedRepo(repo)
    setTestResult("")
    setEditDialogOpen(true)
  }

  // 打开删除对话框
  const handleDeleteRepository = (repo: Repository) => {
    setSelectedRepo(repo)
    setDeleteDialogOpen(true)
  }

  // 测试连接
  const handleTestConnection = async () => {
    if (!formData.repoUrl.trim()) {
      appleToast.error("请输入存储库URL")
      return
    }

    setTestingConnection(true)
    setTestResult("")
    
    try {
      const response = await apiV1.post(API_PATHS_V1.PARCEL_REPOSITORY_TEST, {
        url: formData.repoUrl
      })
      
      if (response.data.code === 200) {
        setTestResult("✓ " + response.data.data)
        appleToast.success("连接测试成功")
      } else {
        setTestResult("✗ " + response.data.msg)
        appleToast.error("连接测试失败: " + response.data.msg)
      }
    } catch (error: any) {
      const errorMsg = error.response?.data?.msg || "网络错误"
      setTestResult("✗ " + errorMsg)
      appleToast.error("连接测试失败: " + errorMsg)
    } finally {
      setTestingConnection(false)
    }
  }

  // 创建存储库
  const handleCreateRepository = async () => {
    if (!formData.repoName.trim() || !formData.repoUrl.trim()) {
      appleToast.error("请填写存储库名称和URL")
      return
    }

    try {
      const response = await apiV1.post(API_PATHS_V1.PARCEL_REPOSITORY_CREATE, {
        repoName: formData.repoName,
        repoType: formData.repoType,
        repoUrl: formData.repoUrl,
        description: formData.description,
        isDefault: 0,
        status: 1,
      })

      if (response.data.code === 200) {
        appleToast.success("存储库创建成功")
        setAddDialogOpen(false)
        fetchRepositories()
      } else {
        appleToast.error("创建失败: " + response.data.msg)
      }
    } catch (error: any) {
      appleToast.error("创建失败: " + (error.response?.data?.msg || "网络错误"))
    }
  }

  // 更新存储库
  const handleUpdateRepository = async () => {
    if (!formData.repoName.trim() || !formData.repoUrl.trim()) {
      appleToast.error("请填写存储库名称和URL")
      return
    }

    try {
      const response = await apiV1.put(API_PATHS_V1.PARCEL_REPOSITORY_UPDATE, {
        id: formData.id,
        repoName: formData.repoName,
        repoUrl: formData.repoUrl,
        description: formData.description,
      })

      if (response.data.code === 200) {
        appleToast.success("存储库更新成功")
        setEditDialogOpen(false)
        fetchRepositories()
      } else {
        appleToast.error("更新失败: " + response.data.msg)
      }
    } catch (error: any) {
      appleToast.error("更新失败: " + (error.response?.data?.msg || "网络错误"))
    }
  }

  // 删除存储库
  const handleConfirmDelete = async () => {
    if (!selectedRepo) return

    try {
      const response = await apiV1.delete(`${API_PATHS_V1.PARCEL_REPOSITORY_DELETE}/${selectedRepo.id}`)

      if (response.data.code === 200) {
        appleToast.success("存储库删除成功")
        setDeleteDialogOpen(false)
        setSelectedRepo(null)
        fetchRepositories()
      } else {
        appleToast.error("删除失败: " + response.data.msg)
      }
    } catch (error: any) {
      appleToast.error("删除失败: " + (error.response?.data?.msg || "网络错误"))
    }
  }

  // 设置默认存储库
  const handleSetDefault = async (repo: Repository) => {
    try {
      const response = await apiV1.put(`${API_PATHS_V1.PARCEL_REPOSITORY_SET_DEFAULT}/${repo.id}`)

      if (response.data.code === 200) {
        appleToast.success("已设置为默认存储库")
        fetchRepositories()
      } else {
        appleToast.error("设置失败: " + response.data.msg)
      }
    } catch (error: any) {
      appleToast.error("设置失败: " + (error.response?.data?.msg || "网络错误"))
    }
  }

  return (
    <div className="min-h-screen bg-gradient-to-br from-slate-50 via-blue-50 to-indigo-50">
      <FinalNavbar />
      
      <div className="container mx-auto px-4 py-8 max-w-7xl">
        {/* 页面标题 */}
        <div className="mb-8">
          <div className="flex items-center gap-3 mb-2">
            <div className="p-3 bg-gradient-to-br from-blue-500 to-indigo-600 rounded-2xl shadow-lg">
              <HardDrive className="w-6 h-6 text-white" />
            </div>
            <div>
              <h1 className="text-3xl font-bold bg-gradient-to-r from-slate-900 to-slate-700 bg-clip-text text-transparent">
                集群存储库管理
              </h1>
              <p className="text-sm text-slate-600 mt-1">
                管理本地和远程HTTP存储库，配置组件包下载源
              </p>
            </div>
          </div>
        </div>

        {/* 搜索和添加栏 */}
        <div className="flex gap-4 mb-6">
          <div className="flex-1 relative">
            <Search className="absolute left-3 top-1/2 transform -translate-y-1/2 w-5 h-5 text-slate-400" />
            <Input
              type="text"
              placeholder="搜索存储库名称或URL..."
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
              className="pl-10 h-12 rounded-xl border-slate-200 focus:border-blue-500 transition-colors"
            />
          </div>
          <Button
            onClick={handleAddRepository}
            className="h-12 px-6 bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700 text-white rounded-xl shadow-lg hover:shadow-xl transition-all duration-200"
          >
            <Plus className="w-5 h-5 mr-2" />
            添加存储库
          </Button>
        </div>

        {/* 存储库列表 */}
        {loading ? (
          <div className="text-center py-12">
            <div className="inline-block animate-spin rounded-full h-12 w-12 border-b-2 border-blue-600"></div>
            <p className="mt-4 text-slate-600">加载中...</p>
          </div>
        ) : filteredRepositories.length === 0 ? (
          <Card className="rounded-2xl border-slate-200 shadow-sm">
            <CardContent className="py-16 text-center">
              <div className="inline-flex items-center justify-center w-16 h-16 rounded-full bg-slate-100 mb-4">
                <HardDrive className="w-8 h-8 text-slate-400" />
              </div>
              <h3 className="text-lg font-semibold text-slate-900 mb-2">
                {searchTerm ? "未找到匹配的存储库" : "还没有存储库"}
              </h3>
              <p className="text-slate-600 mb-6">
                {searchTerm ? "尝试使用其他关键词搜索" : "点击上方按钮添加第一个存储库"}
              </p>
              {!searchTerm && (
                <Button
                  onClick={handleAddRepository}
                  className="bg-gradient-to-r from-blue-500 to-indigo-600 hover:from-blue-600 hover:to-indigo-700"
                >
                  <Plus className="mr-2 h-4 w-4" />
                  添加存储库
                </Button>
              )}
            </CardContent>
          </Card>
        ) : (
          <div className="space-y-4">
            {filteredRepositories.map((repo) => (
              <Card key={repo.id} className="rounded-2xl border-slate-200 shadow-sm hover:shadow-md transition-shadow">
                <CardHeader className="pb-4">
                  <div className="flex items-start justify-between">
                    <div className="flex items-start space-x-4">
                      <div className={`p-3 rounded-xl ${
                        repo.repoType === 'local' 
                          ? 'bg-gradient-to-br from-amber-400 to-orange-500' 
                          : 'bg-gradient-to-br from-blue-400 to-indigo-500'
                      }`}>
                        {repo.repoType === 'local' ? (
                          <FolderOpen className="w-6 h-6 text-white" />
                        ) : (
                          <Globe className="w-6 h-6 text-white" />
                        )}
                      </div>
                      <div className="flex-1">
                        <div className="flex items-center gap-2 mb-1">
                          <h3 className="text-lg font-semibold text-slate-900">{repo.repoName}</h3>
                          {repo.isDefault === 1 && (
                            <Badge className="bg-gradient-to-r from-green-500 to-emerald-600 text-white">
                              默认
                            </Badge>
                          )}
                          <Badge variant="outline" className={
                            repo.repoType === 'local' ? 'border-orange-300 text-orange-700' : 'border-blue-300 text-blue-700'
                          }>
                            {repo.repoType === 'local' ? '本地' : 'HTTP'}
                          </Badge>
                          {repo.status === 1 ? (
                            <Badge variant="outline" className="border-green-300 text-green-700">
                              <CheckCircle className="w-3 h-3 mr-1" />
                              启用
                            </Badge>
                          ) : (
                            <Badge variant="outline" className="border-slate-300 text-slate-600">
                              <AlertCircle className="w-3 h-3 mr-1" />
                              禁用
                            </Badge>
                          )}
                        </div>
                        <p className="text-sm text-slate-600 mb-2 font-mono">{repo.repoUrl}</p>
                        {repo.description && (
                          <p className="text-sm text-slate-500">{repo.description}</p>
                        )}
                        {repo.frameCode && (
                          <Badge variant="secondary" className="mt-2">
                            框架: {repo.frameCode}
                          </Badge>
                        )}
                      </div>
                    </div>
                    <div className="flex gap-2">
                      {repo.isDefault !== 1 && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleSetDefault(repo)}
                          className="rounded-lg"
                        >
                          <Settings className="w-4 h-4 mr-1" />
                          设为默认
                        </Button>
                      )}
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleEditRepository(repo)}
                        className="rounded-lg"
                      >
                        <Edit className="w-4 h-4 mr-1" />
                        编辑
                      </Button>
                      {!(repo.repoType === 'local' && repo.isDefault === 1) && (
                        <Button
                          size="sm"
                          variant="outline"
                          onClick={() => handleDeleteRepository(repo)}
                          className="rounded-lg border-red-200 text-red-600 hover:bg-red-50"
                        >
                          <Trash2 className="w-4 h-4 mr-1" />
                          删除
                        </Button>
                      )}
                    </div>
                  </div>
                </CardHeader>
              </Card>
            ))}
          </div>
        )}

        {/* 添加存储库对话框 */}
        <Dialog open={addDialogOpen} onOpenChange={setAddDialogOpen}>
          <DialogContent className="sm:max-w-[600px]">
            <DialogHeader>
              <DialogTitle>添加存储库</DialogTitle>
              <DialogDescription>
                添加新的HTTP远程存储库，用于下载组件包
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="add-name">存储库名称 *</Label>
                <Input
                  id="add-name"
                  value={formData.repoName}
                  onChange={(e) => setFormData({...formData, repoName: e.target.value})}
                  placeholder="例如：公司内部存储库"
                  className="rounded-lg"
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="add-type">存储库类型 *</Label>
                <Select 
                  value={formData.repoType} 
                  onValueChange={(value) => setFormData({...formData, repoType: value})}
                >
                  <SelectTrigger className="rounded-lg">
                    <SelectValue placeholder="选择存储库类型" />
                  </SelectTrigger>
                  <SelectContent>
                    {REPO_TYPES.map((type) => (
                      <SelectItem 
                        key={type.value} 
                        value={type.value}
                        disabled={!type.supported}
                        className={!type.supported ? "opacity-50" : ""}
                      >
                        <div className="flex items-center justify-between w-full">
                          <span>{type.label}</span>
                          {!type.supported && (
                            <Badge variant="secondary" className="ml-2 text-xs">
                              暂不支持
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {formData.repoType && !REPO_TYPES.find(t => t.value === formData.repoType)?.supported && (
                  <p className="text-sm text-amber-600">
                    此类型暂不支持，请选择其他类型
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="add-url">存储库URL *</Label>
                <div className="flex gap-2">
                  <Input
                    id="add-url"
                    value={formData.repoUrl}
                    onChange={(e) => setFormData({...formData, repoUrl: e.target.value})}
                    placeholder="http://192.168.1.30/BDP/packages/"
                    className="rounded-lg flex-1"
                  />
                  <Button
                    type="button"
                    variant="outline"
                    onClick={handleTestConnection}
                    disabled={testingConnection}
                    className="rounded-lg"
                  >
                    {testingConnection ? "测试中..." : "测试连接"}
                  </Button>
                </div>
                {testResult && (
                  <p className={`text-sm ${testResult.startsWith('✓') ? 'text-green-600' : 'text-red-600'}`}>
                    {testResult}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="add-description">描述</Label>
                <Textarea
                  id="add-description"
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  placeholder="存储库的用途和说明"
                  className="rounded-lg"
                  rows={3}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setAddDialogOpen(false)} className="rounded-lg">
                取消
              </Button>
              <Button onClick={handleCreateRepository} className="rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600">
                创建
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 编辑存储库对话框 */}
        <Dialog open={editDialogOpen} onOpenChange={setEditDialogOpen}>
          <DialogContent className="sm:max-w-[600px]">
            <DialogHeader>
              <DialogTitle>编辑存储库</DialogTitle>
              <DialogDescription>
                修改存储库的配置信息
              </DialogDescription>
            </DialogHeader>
            <div className="space-y-4 py-4">
              <div className="space-y-2">
                <Label htmlFor="edit-name">存储库名称 *</Label>
                <Input
                  id="edit-name"
                  value={formData.repoName}
                  onChange={(e) => setFormData({...formData, repoName: e.target.value})}
                  className="rounded-lg"
                  disabled={selectedRepo?.repoType === 'local' && selectedRepo?.isDefault === 1}
                />
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-type">存储库类型 *</Label>
                <Select 
                  value={formData.repoType} 
                  onValueChange={(value) => setFormData({...formData, repoType: value})}
                  disabled={selectedRepo?.repoType === 'local'}
                >
                  <SelectTrigger className="rounded-lg">
                    <SelectValue placeholder="选择存储库类型" />
                  </SelectTrigger>
                  <SelectContent>
                    {REPO_TYPES.map((type) => (
                      <SelectItem 
                        key={type.value} 
                        value={type.value}
                        disabled={!type.supported}
                        className={!type.supported ? "opacity-50" : ""}
                      >
                        <div className="flex items-center justify-between w-full">
                          <span>{type.label}</span>
                          {!type.supported && (
                            <Badge variant="secondary" className="ml-2 text-xs">
                              暂不支持
                            </Badge>
                          )}
                        </div>
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
                {selectedRepo?.repoType === 'local' && (
                  <p className="text-sm text-slate-500">
                    本地存储库类型不可修改
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-url">存储库URL *</Label>
                <div className="flex gap-2">
                  <Input
                    id="edit-url"
                    value={formData.repoUrl}
                    onChange={(e) => setFormData({...formData, repoUrl: e.target.value})}
                    className="rounded-lg flex-1"
                    disabled={selectedRepo?.repoType === 'local'}
                  />
                  {selectedRepo?.repoType !== 'local' && (
                    <Button
                      type="button"
                      variant="outline"
                      onClick={handleTestConnection}
                      disabled={testingConnection}
                      className="rounded-lg"
                    >
                      {testingConnection ? "测试中..." : "测试连接"}
                    </Button>
                  )}
                </div>
                {testResult && (
                  <p className={`text-sm ${testResult.startsWith('✓') ? 'text-green-600' : 'text-red-600'}`}>
                    {testResult}
                  </p>
                )}
              </div>
              <div className="space-y-2">
                <Label htmlFor="edit-description">描述</Label>
                <Textarea
                  id="edit-description"
                  value={formData.description}
                  onChange={(e) => setFormData({...formData, description: e.target.value})}
                  className="rounded-lg"
                  rows={3}
                />
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setEditDialogOpen(false)} className="rounded-lg">
                取消
              </Button>
              <Button onClick={handleUpdateRepository} className="rounded-lg bg-gradient-to-r from-blue-500 to-indigo-600">
                保存
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>

        {/* 删除确认对话框 */}
        <Dialog open={deleteDialogOpen} onOpenChange={setDeleteDialogOpen}>
          <DialogContent className="sm:max-w-[500px]">
            <DialogHeader>
              <DialogTitle>确认删除</DialogTitle>
              <DialogDescription>
                确定要删除存储库 "{selectedRepo?.repoName}" 吗？
              </DialogDescription>
            </DialogHeader>
            <div className="py-4">
              <div className="flex items-start gap-3 p-4 bg-amber-50 border border-amber-200 rounded-lg">
                <AlertCircle className="w-5 h-5 text-amber-600 flex-shrink-0 mt-0.5" />
                <div className="text-sm text-amber-800">
                  <p className="font-medium mb-1">此操作无法撤销</p>
                  <p>删除后，使用此存储库的集群将无法下载组件包。建议先检查是否有集群正在使用。</p>
                </div>
              </div>
            </div>
            <DialogFooter>
              <Button variant="outline" onClick={() => setDeleteDialogOpen(false)} className="rounded-lg">
                取消
              </Button>
              <Button 
                onClick={handleConfirmDelete} 
                className="rounded-lg bg-gradient-to-r from-red-500 to-red-600 hover:from-red-600 hover:to-red-700"
              >
                确认删除
              </Button>
            </DialogFooter>
          </DialogContent>
        </Dialog>
      </div>
    </div>
  )
}
