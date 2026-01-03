<template>
  <div class="user-manage">
    <el-card header="👤 用户与权限管理">
      <el-table :data="tableData" stripe style="width: 100%" v-loading="loading">
        <el-table-column prop="userId" label="ID" width="80" />
        <el-table-column prop="username" label="用户名" />
        <el-table-column prop="role" label="角色">
          <template #default="scope">
            <el-tag :type="scope.row.role === 'admin' ? 'danger' : 'info'">{{ scope.row.role }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="权限操作" width="200">
          <template #default="scope">
            <div v-if="currentUser.userId === 1">
              <el-button
                  v-if="scope.row.role === 'user'"
                  type="warning" size="small"
                  @click="changeRole(scope.row, 'admin')"
              >提拔</el-button>
              <el-button
                  v-else-if="scope.row.role === 'admin' && scope.row.userId !== 1"
                  type="info" size="small"
                  @click="changeRole(scope.row, 'user')"
              >降级</el-button>
            </div>
          </template>
        </el-table-column>
        <el-table-column label="状态操作" width="120">
          <template #default="scope">
            <el-button
                size="small"
                :type="scope.row.password === 'BANNED' ? 'success' : 'danger'"
                @click="toggleBan(scope.row)"
                :disabled="scope.row.role === 'admin'"
            >
              {{ scope.row.password === 'BANNED' ? '解封' : '封号' }}
            </el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const tableData = ref([])
const loading = ref(false)
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const loadData = async () => {
  loading.value = true
  const res = await axios.get('http://localhost:8080/user/list')
  tableData.value = res.data.data
  loading.value = false
}

const changeRole = async (row, newRole) => {
  try {
    const res = await axios.post('http://localhost:8080/user/changeRole', {
      operatorId: currentUser.userId,
      targetUserId: row.userId,
      newRole: newRole
    })
    if (res.data.code === '200') {
      ElMessage.success('权限已修改')
      row.role = newRole
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch(e) { ElMessage.error('操作失败') }
}

const toggleBan = async (user) => {
  try {
    const res = await axios.post('http://localhost:8080/user/toggleStatus', user)
    ElMessage.success(res.data.msg)
    loadData()
  } catch(e) { ElMessage.error('操作失败') }
}

onMounted(loadData)
</script>