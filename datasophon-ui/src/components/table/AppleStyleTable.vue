<template>
  <div class="apple-style-table-container" :class="{ 'dark-mode': darkMode }">
    <a-card 
      class="table-card" 
      :bordered="bordered" 
      :title="title"
      :headStyle="headStyle"
      :bodyStyle="bodyStyle"
    >
      <!-- 表格操作区 -->
      <div class="table-operations" v-if="$slots.tableOperations || showHeaderOptions">
        <slot name="tableOperations"></slot>
        
        <!-- 表头设置按钮 -->
        <div class="header-options" v-if="showHeaderOptions">
          <a-tooltip title="显示/隐藏列">
            <a-button type="link" @click="toggleColumnSettingVisible">
              <setting-outlined />
            </a-button>
          </a-tooltip>
          
          <a-tooltip title="切换表头方向">
            <a-button type="link" @click="toggleHeaderDirection">
              <swap-outlined :rotate="headerDirection === 'vertical' ? 90 : 0" />
            </a-button>
          </a-tooltip>
          
          <a-tooltip title="切换表格主题">
            <a-button type="link" @click="toggleTheme">
              <theme-outlined />
            </a-button>
          </a-tooltip>
        </div>
      </div>
      
      <!-- 表格主体 -->
      <a-table
        class="apple-style-table"
        :class="{ 'global-table-header': headerDirection === 'horizontal' }"
        :columns="visibleColumns"
        :data-source="dataSource"
        :loading="loading"
        :pagination="showPagination ? paginationConfig : false"
        :scroll="scroll"
        :rowSelection="rowSelection"
        :size="size"
        :bordered="tableBordered"
        @change="handleTableChange"
      >
        <!-- 表头单元格自定义 -->
        <template #headerCell="{ column }">
          <template v-if="column.customRender && column.slots && column.slots.customHeaderCell">
            <slot :name="column.slots.customHeaderCell" :column="column"></slot>
          </template>
          <template v-else-if="headerDirection === 'vertical' && column.key !== 'action'">
            <rotated-header direction="vertical">
              {{ column.title }}
            </rotated-header>
          </template>
        </template>
        
        <!-- 数据单元格自定义渲染 -->
        <template #bodyCell="{ column, text, record, index }">
          <!-- 处理创建时间列的显示 -->
          <template v-if="column.dataIndex === 'createTime' || column.key === 'createTime'">
            <span class="format-time-cell">{{ formatTime(text) }}</span>
          </template>
          <!-- 其他列使用默认插槽渲染 -->
          <template v-else-if="$slots[column.dataIndex || column.key]">
            <slot :name="column.dataIndex || column.key" :text="text" :record="record" :index="index"></slot>
          </template>
        </template>
        
        <!-- 传递所有插槽内容 -->
        <template v-for="(_, name) in $slots" #[name]="slotData">
          <slot :name="name" v-bind="slotData"></slot>
        </template>
      </a-table>
      
      <!-- 列设置抽屉 -->
      <a-drawer
        title="列设置"
        placement="right"
        :visible="columnSettingVisible"
        :width="280"
        @close="columnSettingVisible = false"
      >
        <a-checkbox-group 
          :value="selectedColumnKeys"
          :options="columnOptions"
          @change="handleColumnChange"
        />
      </a-drawer>
    </a-card>
  </div>
</template>

<script>
import { SettingOutlined, SwapOutlined, ThemeOutlined } from '@ant-design/icons-vue'
import RotatedHeader from './RotatedHeader.vue'

/**
 * 苹果风格表格组件
 * 结合了苹果设计风格和表头旋转等高级特性
 */
export default {
  name: 'AppleStyleTable',
  components: {
    RotatedHeader,
    SettingOutlined,
    SwapOutlined,
    ThemeOutlined
  },
  props: {
    // 表格标题
    title: {
      type: String,
      default: ''
    },
    // 数据源
    dataSource: {
      type: Array,
      default: () => []
    },
    // 列配置
    columns: {
      type: Array,
      default: () => []
    },
    // 是否加载中
    loading: {
      type: Boolean,
      default: false
    },
    // 是否显示分页
    showPagination: {
      type: Boolean,
      default: true
    },
    // 分页配置
    pagination: {
      type: [Object, Boolean],
      default: () => ({
        current: 1,
        pageSize: 10,
        total: 0,
        showSizeChanger: true,
        showQuickJumper: true,
        showTotal: (total) => `共 ${total} 条记录`
      })
    },
    // 表格滚动配置
    scroll: {
      type: Object,
      default: () => ({ x: 'max-content' })
    },
    // 行选择配置
    rowSelection: {
      type: [Object, null],
      default: null
    },
    // 表格尺寸
    size: {
      type: String,
      default: 'default'
    },
    // 是否有边框
    tableBordered: {
      type: Boolean,
      default: false
    },
    // 卡片是否有边框
    bordered: {
      type: Boolean,
      default: false
    },
    // 是否显示表头选项
    showHeaderOptions: {
      type: Boolean,
      default: true
    },
    // 卡片头部样式
    headStyle: {
      type: Object,
      default: () => ({})
    },
    // 卡片内容样式
    bodyStyle: {
      type: Object,
      default: () => ({})
    }
  },
  data() {
    return {
      // 表头方向：horizontal 或 vertical
      headerDirection: 'horizontal',
      // 列设置抽屉是否可见
      columnSettingVisible: false,
      // 所选列的键
      selectedColumnKeys: [],
      // 是否暗黑模式
      darkMode: false
    }
  },
  computed: {
    // 分页配置
    paginationConfig() {
      return typeof this.pagination === 'object' ? {
        ...this.pagination,
        showTotal: (total) => `共 ${total} 条记录`
      } : false
    },
    // 可见列
    visibleColumns() {
      if (this.selectedColumnKeys.length === 0) {
        return this.columns
      }
      return this.columns.filter(col => 
        this.selectedColumnKeys.includes(col.key || col.dataIndex)
      )
    },
    // 列选项
    columnOptions() {
      return this.columns.map(col => ({
        label: col.title,
        value: col.key || col.dataIndex
      }))
    }
  },
  watch: {
    columns: {
      immediate: true,
      handler(newCols) {
        if (newCols && newCols.length > 0) {
          this.selectedColumnKeys = newCols
            .filter(col => !col.hidden)
            .map(col => col.key || col.dataIndex)
        }
      }
    }
  },
  methods: {
    // 切换表头方向
    toggleHeaderDirection() {
      this.headerDirection = this.headerDirection === 'horizontal' ? 'vertical' : 'horizontal'
      this.$emit('direction-change', this.headerDirection)
    },
    // 切换主题
    toggleTheme() {
      this.darkMode = !this.darkMode
      this.$emit('theme-change', this.darkMode)
    },
    // 切换列设置抽屉可见性
    toggleColumnSettingVisible() {
      this.columnSettingVisible = !this.columnSettingVisible
    },
    // 处理列变更
    handleColumnChange(checkedValues) {
      this.selectedColumnKeys = checkedValues
      this.$emit('column-change', checkedValues)
    },
    // 处理表格变更
    handleTableChange(pagination, filters, sorter) {
      this.$emit('change', pagination, filters, sorter)
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
.apple-style-table-container {
  .table-card {
    border-radius: 8px;
    transition: all 0.3s ease;
    
    &.ant-card {
      box-shadow: 0 1px 3px rgba(0, 0, 0, 0.1);
      
      :deep(.ant-card-head) {
        min-height: 56px;
        padding: 0 16px;
        border-bottom: 1px solid #f0f0f0;
        
        .ant-card-head-title {
          padding: 16px 0;
          font-size: 16px;
          font-weight: 500;
          color: #333;
        }
      }
      
      :deep(.ant-card-body) {
        padding: 16px;
      }
    }
  }
  
  .table-operations {
    display: flex;
    justify-content: space-between;
    align-items: center;
    margin-bottom: 16px;
    
    .header-options {
      display: flex;
      align-items: center;
      
      .ant-btn {
        color: #1890ff;
        padding: 4px 8px;
        
        &:hover {
          color: #40a9ff;
          background: rgba(24, 144, 255, 0.1);
        }
      }
    }
  }
  
  :deep(.apple-style-table) {
    .ant-table {
      background: #fff;
      border-radius: 8px;
      overflow: hidden;
      
      .ant-table-thead > tr > th {
        background: #f5f7fa;
        color: #333;
        font-weight: 500;
        border-bottom: 1px solid #e6e6e6;
        transition: background 0.3s ease;
        
        &::before {
          display: none; // 移除默认的竖线
        }
      }
      
      .ant-table-tbody > tr > td {
        border-bottom: 1px solid #f0f0f0;
        transition: all 0.3s ease;
      }
      
      .ant-table-tbody > tr:hover > td {
        background-color: #f9f9fb;
      }
      
      .ant-table-tbody > tr:last-child > td {
        border-bottom: none;
      }
    }
    
    .ant-pagination {
      margin-top: 16px;
      
      .ant-pagination-item-active {
        border-color: #1890ff;
        background: #1890ff;
        
        a {
          color: #fff;
        }
      }
    }
  }
  
  // 暗黑模式
  &.dark-mode {
    .table-card {
      background: #1f1f1f;
      
      :deep(.ant-card-head) {
        background: #1f1f1f;
        border-bottom: 1px solid #303030;
        
        .ant-card-head-title {
          color: #e6e6e6;
        }
      }
    }
    
    :deep(.apple-style-table) {
      .ant-table {
        background: #1f1f1f;
        
        .ant-table-thead > tr > th {
          background: #2d2d2d;
          color: #e6e6e6;
          border-bottom: 1px solid #303030;
        }
        
        .ant-table-tbody > tr > td {
          background: #1f1f1f;
          color: #e6e6e6;
          border-bottom: 1px solid #303030;
        }
        
        .ant-table-tbody > tr:hover > td {
          background: #2a2a2a;
        }
        
        .ant-empty {
          color: #e6e6e6;
          
          .ant-empty-img-simple-path {
            fill: #2a2a2a;
          }
          
          .ant-empty-img-simple-ellipse {
            fill: #2d2d2d;
          }
        }
      }
      
      .ant-pagination {
        .ant-pagination-item a {
          color: #e6e6e6;
        }
      }
    }
  }
}
</style> 