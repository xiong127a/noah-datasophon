<template>
  <div class="rotated-header-table-demo global-table-header apple-style-table">
    <a-card :bordered="false" title="表头样式示例">
      <div class="table-operations">
        <a-button type="primary" @click="toggleHeaderDirection">
          切换表头方向：{{ headerDirection === 'horizontal' ? '水平' : '垂直' }}
        </a-button>
      </div>
      
      <a-table
        :columns="columns"
        :data-source="data"
        :pagination="{ pageSize: 5 }"
        :scroll="{ x: 1000 }"
      >
        <template #headerCell="{ column }">
          <template v-if="column.customHeaderCell">
            <rotated-header :direction="headerDirection">
              {{ column.title }}
            </rotated-header>
          </template>
        </template>
        
        <!-- 处理表格数据单元格的显示 -->
        <template #bodyCell="{ column, text }">
          <!-- 处理创建时间列的显示 -->
          <template v-if="column.dataIndex === 'createTime' || column.key === 'createTime'">
            <span class="format-time-cell">{{ formatTime(text) }}</span>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script>
import RotatedHeader from './RotatedHeader.vue'

/**
 * 旋转表头表格示例
 * 演示如何在表格中使用垂直或水平方向的表头
 */
export default {
  name: 'RotatedHeaderTableDemo',
  components: {
    RotatedHeader
  },
  data() {
    return {
      // 表头方向：horizontal 或 vertical
      headerDirection: 'horizontal',
      columns: [
        {
          title: '序号',
          dataIndex: 'id',
          key: 'id',
          width: 80,
          fixed: 'left',
          customHeaderCell: true
        },
        {
          title: '项目名称',
          dataIndex: 'name',
          key: 'name',
          width: 150,
          customHeaderCell: true
        },
        {
          title: '创建时间',
          dataIndex: 'createTime',
          key: 'createTime',
          width: 150,
          customHeaderCell: true
        },
        {
          title: '状态',
          dataIndex: 'status',
          key: 'status',
          width: 120,
          customHeaderCell: true
        },
        {
          title: '负责人',
          dataIndex: 'owner',
          key: 'owner',
          width: 120,
          customHeaderCell: true
        },
        {
          title: '所属部门',
          dataIndex: 'department',
          key: 'department',
          width: 150,
          customHeaderCell: true
        },
        {
          title: '描述信息',
          dataIndex: 'description',
          key: 'description',
          width: 200,
          customHeaderCell: true
        },
        {
          title: '操作',
          key: 'action',
          fixed: 'right',
          width: 120,
          customHeaderCell: true,
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
    // 切换表头方向
    toggleHeaderDirection() {
      this.headerDirection = this.headerDirection === 'horizontal' ? 'vertical' : 'horizontal'
    },
    
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
.rotated-header-table-demo {
  padding: 20px;
  
  .table-operations {
    margin-bottom: 16px;
    
    .ant-btn {
      margin-right: 8px;
    }
  }
  
  // 垂直表头时的样式调整
  :deep(.ant-table-thead > tr > th) {
    &.vertical-header {
      padding: 0 !important;
      height: 120px; // 为垂直表头预留足够的高度
      
      // 当使用垂直表头时，列宽可以适当缩小
      &.ant-table-cell {
        min-width: 50px !important;
      }
    }
  }
}
</style> 