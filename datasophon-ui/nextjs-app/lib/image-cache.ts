/**
 * 图片缓存管理 - 防止重复请求
 */

interface ImageCacheItem {
  url: string
  loading: boolean
  error: boolean
  timestamp: number
}

class ImageCacheManager {
  private cache = new Map<string, ImageCacheItem>()
  private loadingPromises = new Map<string, Promise<string>>()

  /**
   * 获取缓存的图片URL，如果不存在则异步加载
   */
  async getImageUrl(imagePath: string, loadFunction: (path: string) => Promise<Blob>): Promise<string> {
    // 如果是网络图片或data URL，直接返回
    if (imagePath.startsWith('http') || imagePath.startsWith('data:')) {
      return imagePath
    }

    // 检查缓存
    const cached = this.cache.get(imagePath)
    if (cached && !cached.error && cached.url) {
      return cached.url
    }

    // 检查是否正在加载
    const loadingPromise = this.loadingPromises.get(imagePath)
    if (loadingPromise) {
      return loadingPromise
    }

    // 开始加载
    const promise = this.loadImage(imagePath, loadFunction)
    this.loadingPromises.set(imagePath, promise)

    try {
      const url = await promise
      this.loadingPromises.delete(imagePath)
      return url
    } catch (error) {
      this.loadingPromises.delete(imagePath)
      throw error
    }
  }

  private async loadImage(imagePath: string, loadFunction: (path: string) => Promise<Blob>): Promise<string> {
    try {
      // 标记正在加载
      this.cache.set(imagePath, {
        url: '',
        loading: true,
        error: false,
        timestamp: Date.now()
      })

      const blob = await loadFunction(imagePath)
      const url = URL.createObjectURL(blob)

      // 更新缓存
      this.cache.set(imagePath, {
        url,
        loading: false,
        error: false,
        timestamp: Date.now()
      })

      return url

    } catch (error) {
      // 标记错误
      this.cache.set(imagePath, {
        url: '',
        loading: false,
        error: true,
        timestamp: Date.now()
      })

      throw error
    }
  }

  /**
   * 获取图片状态
   */
  getImageStatus(imagePath: string): { loading: boolean; error: boolean; url?: string } {
    const cached = this.cache.get(imagePath)
    if (!cached) {
      return { loading: false, error: false }
    }

    return {
      loading: cached.loading,
      error: cached.error,
      url: cached.url || undefined
    }
  }

  /**
   * 清理缓存（释放blob URL）
   */
  clearCache() {
    this.cache.forEach((item) => {
      if (item.url && item.url.startsWith('blob:')) {
        URL.revokeObjectURL(item.url)
      }
    })
    this.cache.clear()
    this.loadingPromises.clear()
  }

  /**
   * 清理过期缓存（默认1小时）
   */
  clearExpiredCache(maxAge = 60 * 60 * 1000) {
    const now = Date.now()
    this.cache.forEach((item, key) => {
      if (now - item.timestamp > maxAge) {
        if (item.url && item.url.startsWith('blob:')) {
          URL.revokeObjectURL(item.url)
        }
        this.cache.delete(key)
      }
    })
  }
}

// 全局单例
export const imageCache = new ImageCacheManager()

// 在页面卸载时清理缓存
if (typeof window !== 'undefined') {
  window.addEventListener('beforeunload', () => {
    imageCache.clearCache()
  })
}
