<template>
  <div class="user-guide">
    <a-card class="card-shadow">
      <MarkdownDocViewer
        :service-id="serviceId"
        doc-type="guide"
        empty-text="暂无用户指南信息"
      />
    </a-card>
  </div>
</template>

<script>
import MarkdownDocViewer from './MarkdownDocViewer.vue';

export default {
  name: 'UserGuide',
  components: {
    MarkdownDocViewer
  },
  props: {
    serviceId: {
      type: String,
      required: true
    }
  }
}
</script>

<style lang="less">
.user-guide {
  width: 100%;
  
  // 覆盖目录侧边栏样式
  :deep(.content-wrapper .sidebar) {
    // 确保侧边栏固定
    position: sticky !important;
    top: 84px !important;
    align-self: flex-start !important;
    z-index: 5 !important;
    background-color: #fff;
    max-height: calc(100vh - 120px) !important;
  }
  
  :deep(.sidebar .custom-nav) {
    // 添加滚动条样式
    overflow-y: auto;
    max-height: calc(100vh - 200px);
    padding-right: 10px;
    
    // 隐藏默认滚动条
    &::-webkit-scrollbar {
      width: 8px;
    }
    
    // 滚动条轨道
    &::-webkit-scrollbar-track {
      background: transparent;
    }
    
    // 滚动条滑块
    &::-webkit-scrollbar-thumb {
      background-color: rgba(0, 0, 0, 0.2);
      border-radius: 4px;
      
      // 悬停时的样式
      &:hover {
        background-color: rgba(0, 0, 0, 0.4);
      }
    }
    
    // 仅在滚动时显示滚动条
    &:not(:hover)::-webkit-scrollbar-thumb {
      background-color: transparent;
    }
    
    // Firefox滚动条支持
    scrollbar-width: thin;
    scrollbar-color: rgba(0, 0, 0, 0.2) transparent;
    
    // 目录项样式优化
    ul.table-of-contents {
      padding-left: 15px;
      
      li {
        margin-bottom: 6px;
        
        a {
          display: block;
          padding: 4px 0;
          color: rgba(0, 0, 0, 0.65);
          text-decoration: none;
          transition: all 0.3s;
          
          &:hover, &.active {
            color: #1890ff;
          }
        }
      }
    }
  }
}
</style> 