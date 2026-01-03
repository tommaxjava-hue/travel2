<template>
  <div class="spot-manage">
    <el-card header="🏔️ 景点数据管理">
      <div class="filter-bar">
        <el-input v-model="keyword" placeholder="搜索..." style="width: 300px" @clear="loadData" clearable>
          <template #append><el-button @click="loadData">搜索</el-button></template>
        </el-input>
        <el-button type="primary" style="margin-left:10px" @click="loadData">刷新</el-button>
      </div>

      <el-table :data="tableData" stripe style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="spotId" label="ID" width="60" />
        <el-table-column label="图片" width="100">
          <template #default="scope">
            <img :src="scope.row.imageUrl" style="width: 80px; height: 50px; object-fit: cover; border-radius: 4px;" />
          </template>
        </el-table-column>
        <el-table-column prop="name" label="名称" width="150" show-overflow-tooltip />
        <el-table-column prop="city" label="城市" width="80" />
        <el-table-column prop="ticketPrice" label="票价" width="80" />
        <el-table-column label="热门" width="80">
          <template #default="scope">
            <el-switch v-model="scope.row.isHot" :active-value="1" :inactive-value="0" @change="handleToggleHot(scope.row)" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="180">
          <template #default="scope">
            <el-button type="primary" size="small" @click="openEdit(scope.row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(scope.row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showEdit" title="修改景点详情" width="600px">
      <el-form :model="editForm" label-width="80px">
        <el-form-item label="名称"><el-input v-model="editForm.name" /></el-form-item>
        <el-form-item label="城市"><el-input v-model="editForm.city" /></el-form-item>
        <el-form-item label="票价"><el-input-number v-model="editForm.ticketPrice" :min="0" /></el-form-item>
        <el-form-item label="介绍"><el-input v-model="editForm.description" type="textarea" :rows="4" /></el-form-item>
        <el-form-item label="封面">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/upload"
              :show-file-list="false"
              :on-success="handleEditUploadSuccess"
          >
            <img v-if="editForm.imageUrl" :src="editForm.imageUrl" class="avatar" style="width:100px;height:100px;object-fit:cover"/>
            <el-icon v-else class="avatar-uploader-icon" style="border:1px dashed #ddd;padding:30px;font-size:28px;color:#8c939d"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'

const tableData = ref([])
const loading = ref(false)
const keyword = ref('')
const showEdit = ref(false)
const editForm = reactive({})

const loadData = async () => {
  loading.value = true
  const res = await axios.get('http://localhost:8080/attraction/list', { params: { keyword: keyword.value } })
  tableData.value = res.data.data
  loading.value = false
}

const handleToggleHot = async (row) => {
  await axios.post('http://localhost:8080/attraction/toggleHot', row)
  ElMessage.success('状态更新')
}

const openEdit = (row) => {
  Object.assign(editForm, row)
  showEdit.value = true
}

const submitEdit = async () => {
  try {
    const res = await axios.post('http://localhost:8080/attraction/update', editForm)
    if (res.data.code === '200') {
      ElMessage.success('修改成功')
      showEdit.value = false
      loadData()
    } else {
      ElMessage.error('失败')
    }
  } catch(e) { ElMessage.error('网络错误') }
}

const handleDelete = (row) => {
  ElMessageBox.confirm('确定删除吗？').then(async () => {
    await axios.delete(`http://localhost:8080/attraction/delete/${row.spotId}`)
    loadData()
  })
}

const handleEditUploadSuccess = (res) => {
  if (res.code === '200') editForm.imageUrl = res.data
}

onMounted(loadData)
</script>

<style scoped>
.filter-bar { display: flex; align-items: center; }
</style>