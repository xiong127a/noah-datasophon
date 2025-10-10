# 表格表头样式指南

## 目录

1. 全局表格表头样式
2. 表头旋转组件
3. 苹果风格表格组件
4. 使用示例
5. 最佳实践

## 1. 全局表格表头样式

### 使用方法

只需要在表格容器上添加 `global-table-header` 类名即可应用全局表头样式，解决表头文字垂直显示的问题。

```html
<div class="global-table-header">
  <a-table :columns="columns" :data-source="data" />
</div>
```

### 主要特性

- 确保所有表头正常水平显示，不会竖直旋转
- 修复排序列的表头样式
- 解决表格内容溢出问题

## 2. 表头旋转组件

### 基本用法

`RotatedHeader` 组件可以实现表头文字的水平或垂直显示。

```html
<template>
  <rotated-header direction="horizontal">水平表头文字</rotated-header>
  <!-- 或 -->
  <rotated-header direction="vertical">垂直表头文字</rotated-header>
</template>

<script>
import RotatedHeader from '@/components/table/RotatedHeader.vue'

export default {
  components: {
    RotatedHeader
  }
}
</script>
```

### 属性

| 属性名 | 描述 | 类型 | 默认值 |
| --- | --- | --- | --- |
| direction | 方向：vertical(垂直) 或 horizontal(水平) | String | horizontal |
| angle | 旋转角度（仅在vertical模式下有效） | Number | 90 |
| width | 表头宽度 | Number/String | auto |
| height | 表头高度 | Number/String | auto |

### 在表格中使用

可以在表格的自定义表头单元格中使用 `RotatedHeader` 组件：

```html
<a-table :columns="columns">
  <template #headerCell="{ column }">
    <rotated-header :direction="headerDirection">
      {{ column.title }}
    </rotated-header>
  </template>
</a-table>
```

## 3. 苹果风格表格组件

### 基本用法

`AppleStyleTable` 是一个高级表格组件，集成了苹果设计风格和表头旋转等特性。

```html
<apple-style-table
  title="表格标题"
  :columns="columns"
  :data-source="data"
  showHeaderOptions
>
  <template #tableOperations>
    <a-button type="primary">新增</a-button>
  </template>
</apple-style-table>
```

### 主要特性

- 苹果设计风格
- 表头旋转切换
- 列显示/隐藏设置
- 主题切换（亮色/暗色）
- 完全可定制的插槽

### 属性

| 属性名 | 描述 | 类型 | 默认值 |
| --- | --- | --- | --- |
| title | 表格标题 | String | '' |
| dataSource | 数据源 | Array | [] |
| columns | 列配置 | Array | [] |
| loading | 是否加载中 | Boolean | false |
| showPagination | 是否显示分页 | Boolean | true |
| pagination | 分页配置 | Object/Boolean | { ... } |
| scroll | 表格滚动配置 | Object | { x: 'max-content' } |
| rowSelection | 行选择配置 | Object/null | null |
| size | 表格尺寸 | String | 'default' |
| tableBordered | 是否有表格边框 | Boolean | false |
| bordered | 是否有卡片边框 | Boolean | false |
| showHeaderOptions | 是否显示表头选项 | Boolean | true |
| headStyle | 卡片头部样式 | Object | {} |
| bodyStyle | 卡片内容样式 | Object | {} |

### 插槽

| 插槽名 | 描述 |
| --- | --- |
| tableOperations | 表格操作区域 |
| [column.dataIndex] | 自定义列内容 |
| [column.slots.customHeaderCell] | 自定义表头单元格 |

### 事件

| 事件名 | 描述 | 参数 |
| --- | --- | --- |
| direction-change | 表头方向变更 | direction |
| theme-change | 主题变更 | darkMode |
| column-change | 列变更 | checkedValues |
| change | 表格变更 | pagination, filters, sorter |

## 4. 使用示例

查看 `TableHeaderDemo.vue` 文件获取完整的使用示例。

### 基础表头样式示例

```html
<div class="global-table-header">
  <a-table :columns="columns" :data-source="data" />
</div>
```

### 表头旋转示例

```html
<template>
  <div>
    <a-button @click="toggleHeaderDirection">
      切换表头方向：{{ headerDirection }}
    </a-button>
    
    <a-table :columns="columns" :data-source="data">
      <template #headerCell="{ column }">
        <rotated-header :direction="headerDirection">
          {{ column.title }}
        </rotated-header>
      </template>
    </a-table>
  </div>
</template>

<script>
export default {
  data() {
    return {
      headerDirection: 'horizontal'
    }
  },
  methods: {
    toggleHeaderDirection() {
      this.headerDirection = this.headerDirection === 'horizontal' ? 'vertical' : 'horizontal'
    }
  }
}
</script>
```

### 高级表格组件示例

```html
<apple-style-table
  title="苹果风格高级表格"
  :columns="columns"
  :data-source="data"
  showHeaderOptions
>
  <template #tableOperations>
    <a-button type="primary">新增</a-button>
    <a-button class="mgl8">导出</a-button>
  </template>
</apple-style-table>
```

## 5. 最佳实践

- 对于简单场景，使用 `global-table-header` 类名即可解决表头垂直显示问题
- 需要切换表头方向的场景，使用 `RotatedHeader` 组件
- 完整的高级表格场景，推荐使用 `AppleStyleTable` 组件
- 表头文字尽量简洁，避免过长内容
- 设置合理的列宽，避免内容挤压
- 对于复杂表格，建议使用水平表头，垂直表头适合列较多但每列内容较简单的场景 