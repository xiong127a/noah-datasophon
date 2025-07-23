<template>
  <div class="user-manage-container">
    <h1 class="page-title">用户管理</h1>
    <div class="user-manage-content">
      <div class="action-bar">
        <button class="action-button add-user">
          <i class="icon add-icon"></i>
          <span>添加用户</span>
        </button>
        <div class="search-box">
          <input type="text" placeholder="搜索用户名" class="search-input" />
          <button class="search-button">搜索</button>
        </div>
      </div>
      
      <div class="user-table">
        <table>
          <thead>
            <tr>
              <th>用户名</th>
              <th>邮箱</th>
              <th>状态</th>
              <th>角色</th>
              <th>创建时间</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr>
              <td>admin</td>
              <td>admin@example.com</td>
              <td><span class="status-tag active">启用</span></td>
              <td>管理员</td>
              <td>2023-01-01 12:00:00</td>
              <td class="action-column">
                <button class="action-icon edit-icon" title="编辑"></button>
                <button class="action-icon delete-icon" title="删除"></button>
              </td>
            </tr>
            <tr>
              <td>user1</td>
              <td>user1@example.com</td>
              <td><span class="status-tag active">启用</span></td>
              <td>普通用户</td>
              <td>2023-02-15 10:30:00</td>
              <td class="action-column">
                <button class="action-icon edit-icon" title="编辑"></button>
                <button class="action-icon delete-icon" title="删除"></button>
              </td>
            </tr>
            <tr>
              <td>user2</td>
              <td>user2@example.com</td>
              <td><span class="status-tag inactive">禁用</span></td>
              <td>普通用户</td>
              <td>2023-03-20 15:45:00</td>
              <td class="action-column">
                <button class="action-icon edit-icon" title="编辑"></button>
                <button class="action-icon delete-icon" title="删除"></button>
              </td>
            </tr>
          </tbody>
        </table>
      </div>
      
      <div class="pagination">
        <button class="pagination-button prev">上一页</button>
        <div class="pagination-numbers">
          <button class="pagination-number active">1</button>
          <button class="pagination-number">2</button>
          <button class="pagination-number">3</button>
        </div>
        <button class="pagination-button next">下一页</button>
      </div>
    </div>
  </div>
</template>

<script>
export default {
  name: 'UserManage',
  data() {
    return {
      users: [
        {
          id: 1,
          username: 'admin',
          email: 'admin@example.com',
          status: 'active',
          role: '管理员',
          createTime: '2023-01-01 12:00:00'
        },
        {
          id: 2,
          username: 'user1',
          email: 'user1@example.com',
          status: 'active',
          role: '普通用户',
          createTime: '2023-02-15 10:30:00'
        },
        {
          id: 3,
          username: 'user2',
          email: 'user2@example.com',
          status: 'inactive',
          role: '普通用户',
          createTime: '2023-03-20 15:45:00'
        }
      ],
      currentPage: 1,
      pageSize: 10,
      totalUsers: 3,
      searchKeyword: ''
    }
  },
  methods: {
    addUser() {
      // 实现添加用户的逻辑
    },
    editUser(userId) {
      // 实现编辑用户的逻辑
    },
    deleteUser(userId) {
      // 实现删除用户的逻辑
    },
    search() {
      // 实现搜索功能
    },
    changePage(page) {
      // 实现分页功能
    }
  }
}
</script>

<style scoped>
.user-manage-container {
  padding: 20px;
  height: 100%;
}

.page-title {
  font-size: 24px;
  font-weight: 500;
  margin-bottom: 20px;
  color: #333;
}

.user-manage-content {
  background: #fff;
  padding: 24px;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
}

.action-bar {
  display: flex;
  justify-content: space-between;
  margin-bottom: 20px;
}

.action-button {
  display: flex;
  align-items: center;
  padding: 8px 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 4px;
  cursor: pointer;
  font-size: 14px;
}

.action-button:hover {
  background-color: #40a9ff;
}

.add-icon::before {
  content: "+";
  margin-right: 8px;
  font-weight: bold;
}

.search-box {
  display: flex;
}

.search-input {
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-right: none;
  border-radius: 4px 0 0 4px;
  outline: none;
  width: 200px;
}

.search-input:focus {
  border-color: #40a9ff;
}

.search-button {
  padding: 8px 16px;
  background-color: #1890ff;
  color: white;
  border: none;
  border-radius: 0 4px 4px 0;
  cursor: pointer;
}

.search-button:hover {
  background-color: #40a9ff;
}

.user-table {
  margin-bottom: 20px;
  overflow-x: auto;
}

table {
  width: 100%;
  border-collapse: collapse;
}

th {
  background-color: #fafafa;
  padding: 12px 16px;
  text-align: left;
  font-weight: 500;
  color: #333;
  border-bottom: 1px solid #e8e8e8;
}

td {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
}

tr:hover td {
  background-color: #fafafa;
}

.status-tag {
  display: inline-block;
  padding: 4px 8px;
  border-radius: 2px;
  font-size: 12px;
}

.status-tag.active {
  background-color: #e6f7ff;
  color: #1890ff;
  border: 1px solid #91d5ff;
}

.status-tag.inactive {
  background-color: #fff1f0;
  color: #ff4d4f;
  border: 1px solid #ffa39e;
}

.action-column {
  display: flex;
  gap: 8px;
}

.action-icon {
  width: 24px;
  height: 24px;
  border: none;
  background-color: transparent;
  cursor: pointer;
  position: relative;
}

.edit-icon::before {
  content: "✎";
  color: #1890ff;
}

.delete-icon::before {
  content: "✕";
  color: #ff4d4f;
}

.pagination {
  display: flex;
  justify-content: center;
  align-items: center;
  margin-top: 20px;
}

.pagination-button {
  padding: 6px 12px;
  background-color: white;
  border: 1px solid #d9d9d9;
  cursor: pointer;
}

.pagination-button.prev {
  border-radius: 4px 0 0 4px;
}

.pagination-button.next {
  border-radius: 0 4px 4px 0;
}

.pagination-button:hover {
  color: #40a9ff;
  border-color: #40a9ff;
}

.pagination-numbers {
  display: flex;
}

.pagination-number {
  width: 32px;
  height: 32px;
  display: flex;
  justify-content: center;
  align-items: center;
  margin: 0 4px;
  border: 1px solid #d9d9d9;
  background-color: white;
  cursor: pointer;
  border-radius: 4px;
}

.pagination-number:hover {
  color: #40a9ff;
  border-color: #40a9ff;
}

.pagination-number.active {
  color: white;
  background-color: #1890ff;
  border-color: #1890ff;
}
</style> 