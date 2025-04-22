<template>
  <div class="table-header-demo">
    <h1 class="demo-title">表格表头样式演示</h1>
    
    <div class="demo-section">
      <h2 class="section-title">1. 基础表头样式</h2>
      <a-card class="demo-card global-table-header">
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="{ pageSize: 5 }"
        />
      </a-card>
    </div>
    
    <div class="demo-section">
      <h2 class="section-title">2. 苹果风格表格样式</h2>
      <div class="apple-style-table">
        <a-table
          :columns="columns"
          :data-source="data"
          :pagination="{ pageSize: 5 }"
        />
      </div>
    </div>
    
    <div class="demo-section">
      <h2 class="section-title">3. 表头旋转样式</h2>
      <rotated-header-table-demo />
    </div>
    
    <div class="demo-section">
      <h2 class="section-title">4. 高级表格组件</h2>
      <apple-style-table
        title="苹果风格高级表格"
        :columns="columns"
        :data-source="data"
        :pagination="{ pageSize: 5 }"
        showHeaderOptions
      >
        <template #tableOperations>
          <a-button type="primary">
            新增
          </a-button>
          <a-button class="mgl8">
            导出
          </a-button>
        </template>
      </apple-style-table>
    </div>
  </div>
</template>

<script>
import RotatedHeaderTableDemo from '@/components/table/RotatedHeaderTable.vue'
import AppleStyleTable from '@/components/table/AppleStyleTable.vue'

/**
 * 表格表头样式演示页面
 * 展示各种表头样式和配置选项
 */
export default {
  name: 'TableHeaderDemo',
  components: {
    RotatedHeaderTableDemo,
    AppleStyleTable
  },
  data() {
    return {
      columns: [
        {
          title: '序号',
          dataIndex: 'id',
          key: 'id',
          width: 80
        },
        {
          title: '项目名称',
          dataIndex: 'name',
          key: 'name',
          width: 150
        },
        {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 150
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 120
        },
        {
          title: '负责人',
          dataIndex: 'owner',
          key: 'owner',
          width: 120
        },
        {
          title: '所属部门',
          dataIndex: 'department',
          key: 'department',
          width: 150
        },
        {
          title: '描述信息',
          dataIndex: 'description',
          key: 'description',
          width: 200
        },
        {
          title: '操作',
          key: 'action',
          width: 120,
          slots: { customRender: 'action' }
        }
      ],
      data: Array.from({ length: 10 }).map((_, index) => ({
        key: index,
        id: index + 1,
        name: `项目 ${index + 1}`,
        createTime: '2023-05-01 12:00:00',
        status: index % 3 === 0 ? '完成' : (index % 3 === 1 ? '进行中' : '未开始'),
        owner: `用户${index + 1}`,
        department: `部门${Math.floor(index / 3) + 1}`,
        description: `这是项目${index + 1}的描述信息，用于测试表格显示效果。`
      }))
    }
  },
  methods: {
    // 格式化时间
    formatTime(time) {
      if (!time) return '-';
      return new Date(time).toLocaleString('zh-CN', {
        year: 'numeric',
        month: '2-digit',
        day: '2-digit',
        hour: '2-digit',
        minute: '2-digit',
        second: '2-digit',
        hour12: false
      });
    }
  }
}
</script>

<style lang="less" scoped>
.table-header-demo {
  padding: 24px;
  background: #f5f7fa;
  min-height: 100vh;
  
  .demo-title {
    font-size: 28px;
    font-weight: 500;
    color: #333;
    margin-bottom: 24px;
    text-align: center;
  }
  
  .demo-section {
    margin-bottom: 32px;
    
    .section-title {
      font-size: 20px;
      font-weight: 500;
      color: #333;
      margin-bottom: 16px;
      position: relative;
      padding-left: 12px;
      
      &::before {
        content: '';
        position: absolute;
        left: 0;
        top: 50%;
        transform: translateY(-50%);
        width: 4px;
        height: 16px;
        background: #1890ff;
        border-radius: 2px;
      }
    }
    
    .demo-card {
      background: #fff;
      border-radius: 8px;
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      overflow: hidden;
    }
  }
  
  // 苹果风格表格样式覆盖
  :deep(.ant-pagination-item-active) {
    border-color: #1890ff;
    background: #1890ff;
    
    a {
      color: #fff !important;
    }
  }
  
  :deep(.ant-btn-primary) {
    background: #1890ff;
    border-color: #1890ff;
    
    &:hover, &:focus {
      background: #40a9ff;
      border-color: #40a9ff;
    }
  }
}
</style> 