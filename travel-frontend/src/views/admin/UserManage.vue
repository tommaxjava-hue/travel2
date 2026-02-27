<template>
  <div class="user-manage">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>👥 用户管理 (最高统帅专属)</span>
          <el-button type="primary" plain size="small" @click="fetchUsers" :loading="loading">
            <el-icon><Refresh /></el-icon> 刷新数据
          </el-button>
        </div>
      </template>

      <el-table
          :data="tableData"
          v-loading="loading"
          stripe
          border
          highlight-current-row
          style="width: 100%"
      >
        <el-table-column prop="userId" label="用户ID" width="100" align="center">
          <template #default="scope">
            <el-tag v-if="scope.row.userId === 88" type="danger" effect="dark">88 (统帅)</el-tag>
            <span v-else>{{ scope.row.userId }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="username" label="登录账号" min-width="120" />

        <el-table-column prop="role" label="系统角色" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'admin' ? 'danger' : 'success'" effect="light">
              {{ scope.row.role === 'admin' ? '管理员' : '普通用户' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="tags" label="偏好标签" min-width="150" show-overflow-tooltip />

        <el-table-column prop="password" label="账号状态" width="120" align="center">
          <template #default="scope">
            <el-tag :type="scope.row.password === 'BANNED' ? 'info' : 'success'" effect="dark">
              {{ scope.row.password === 'BANNED' ? '🚫 已封禁' : '✅ 正常' }}
            </el-tag>
          </template>
        </el-table-column>

        <el-table-column label="高危操作" width="220" fixed="right" align="center">
          <template #default="scope">
            <el-button
                size="small"
                :type="scope.row.password === 'BANNED' ? 'success' : 'warning'"
                @click="toggleStatus(scope.row)"
                :disabled="scope.row.userId === 88"
            >
              {{ scope.row.password === 'BANNED' ? '解封账号' : '封禁账号' }}
            </el-button>

            <el-button
                size="small"
                :type="scope.row.role === 'admin' ? 'info' : 'primary'"
                @click="openRoleDialog(scope.row)"
                :disabled="scope.row.userId === 88"
            >
              {{ scope.row.role === 'admin' ? '降级为用户' : '提拔为管理' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog
        v-model="dialogVisible"
        :title="targetRole === 'admin' ? '⚠️ 权限提升确认' : '🔻 权限降级确认'"
        width="400px"
        destroy-on-close
    >
      <div style="line-height: 1.6;">
        确定要将用户 <strong><span :style="{ color: targetRole === 'admin' ? '#f56c6c' : '#e6a23c' }">{{ currentRow?.username }}</span></strong>
        {{ targetRole === 'admin' ? '提拔为系统管理员' : '降级为普通用户' }}吗？<br>
        <span style="font-size: 12px; color: #909399;">
          {{ targetRole === 'admin' ? '此操作将赋予该用户进入后台的权限（但无权操作核心数据）。' : '此操作将立即收回该用户的后台访问权限。' }}
        </span>
      </div>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">手滑了</el-button>
          <el-button :type="targetRole === 'admin' ? 'danger' : 'warning'" @click="changeRole">
            确认{{ targetRole === 'admin' ? '提拔' : '降级' }}
          </el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Refresh } from '@element-plus/icons-vue'
import request from '../../utils/request'

const tableData = ref([])
const loading = ref(false)
const dialogVisible = ref(false)
const currentRow = ref(null)
const targetRole = ref('admin') // 记录即将变更的目标角色

const fetchUsers = async () => {
  loading.value = true
  try {
    const res = await request.get('/user/list')
    if (res.code === '200') {
      tableData.value = res.data
    }
  } catch (error) {
    console.error('获取用户列表失败:', error)
  } finally {
    loading.value = false
  }
}

const toggleStatus = async (row) => {
  try {
    await ElMessageBox.confirm(
        `您正在执行高危操作：确定要 <strong style="color:red">${row.password === 'BANNED' ? '解封' : '封禁'}</strong> 用户 ${row.username} 吗？`,
        '系统安全提示',
        {
          type: 'warning',
          dangerouslyUseHTMLString: true,
          confirmButtonText: '执行',
          cancelButtonText: '取消'
        }
    )
    const res = await request.post('/user/toggleStatus', { userId: row.userId })
    if (res.code === '200') {
      ElMessage.success(res.data || '操作成功')
      fetchUsers()
    }
  } catch (e) {
    if (e !== 'cancel') console.error(e)
  }
}

const openRoleDialog = (row) => {
  currentRow.value = row
  // 如果当前是管理员，目标角色就是普通用户 (user)；反之则提拔为管理员 (admin)
  targetRole.value = row.role === 'admin' ? 'user' : 'admin'
  dialogVisible.value = true
}

const changeRole = async () => {
  try {
    const res = await request.post('/user/changeRole', {
      targetUserId: currentRow.value.userId,
      newRole: targetRole.value
    })
    if (res.code === '200') {
      ElMessage.success('系统权限变更成功')
      dialogVisible.value = false
      fetchUsers()
    }
  } catch (error) {
    console.error('权限变更失败:', error)
  }
}

onMounted(() => {
  fetchUsers()
})
</script>

<style scoped>
.user-manage {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.box-card {
  border-radius: 8px;
}
.card-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  font-weight: 600;
  font-size: 16px;
  color: #303133;
}
</style>