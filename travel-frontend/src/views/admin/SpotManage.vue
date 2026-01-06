<template>
  <div class="spot-manage">
    <el-card header="🏔️ 景点数据管理">
      <div class="filter-bar">
        <el-input
            v-model="keyword"
            placeholder="搜索景点名称 / 城市..."
            style="width: 300px"
            clearable
            @clear="loadData"
            @keyup.enter="loadData"
        >
          <template #append><el-button @click="loadData">搜索</el-button></template>
        </el-input>
        <el-button type="primary" style="margin-left:10px" @click="loadData">刷新列表</el-button>
      </div>

      <el-table :data="tableData" stripe style="width: 100%; margin-top: 20px" v-loading="loading">
        <el-table-column prop="spotId" label="ID" width="80" />

        <el-table-column label="封面图" width="120">
          <template #default="{row}">
            <el-image
                :src="row.imageUrl"
                style="width: 80px; height: 50px; border-radius: 4px"
                fit="cover"
                :preview-src-list="[row.imageUrl]"
                preview-teleported
            >
              <template #error>
                <div class="image-slot">无图</div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="景点名称" width="180" show-overflow-tooltip />
        <el-table-column prop="city" label="城市" width="100" />
        <el-table-column prop="address" label="详细地址" show-overflow-tooltip />
        <el-table-column prop="ticketPrice" label="票价" width="100">
          <template #default="{row}">¥{{ row.ticketPrice }}</template>
        </el-table-column>

        <el-table-column label="热门置顶" width="100">
          <template #default="{row}">
            <el-switch
                v-model="row.isHot"
                :active-value="1"
                :inactive-value="0"
                @change="handleToggleHot(row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right">
          <template #default="{row}">
            <el-button type="primary" size="small" @click="openEdit(row)">编辑</el-button>
            <el-button type="danger" size="small" @click="handleDelete(row)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog v-model="showEdit" title="修改景点详情" width="650px" destroy-on-close>
      <el-form :model="editForm" label-width="90px">
        <el-form-item label="景点名称">
          <el-input v-model="editForm.name" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="所属城市">
              <el-input v-model="editForm.city" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="门票价格">
              <el-input-number v-model="editForm.ticketPrice" :min="0" style="width: 100%" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="详细地址">
          <el-input v-model="editForm.address" />
        </el-form-item>

        <el-row :gutter="20" style="background: #fdf6ec; padding: 10px 0; border-radius: 4px; margin-bottom: 18px;">
          <el-col :span="12">
            <el-form-item label="经度 (Lng)">
              <el-input v-model="editForm.longitude" placeholder="如 116.4074" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度 (Lat)">
              <el-input v-model="editForm.latitude" placeholder="如 39.9042" />
            </el-form-item>
          </el-col>
          <div style="width:100%; text-align:center; font-size:12px; color:#e6a23c;">
            * 经纬度为空会导致行程规划地图无法显示轨迹
          </div>
        </el-row>

        <el-form-item label="景点介绍">
          <el-input v-model="editForm.description" type="textarea" :rows="4" />
        </el-form-item>

        <el-form-item label="封面图片">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/upload"
              :show-file-list="false"
              :on-success="handleEditUploadSuccess"
          >
            <img v-if="editForm.imageUrl" :src="editForm.imageUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showEdit = false">取消</el-button>
        <el-button type="primary" @click="submitEdit">保存修改</el-button>
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
  try {
    const res = await axios.get('http://localhost:8080/attraction/list', {
      params: { keyword: keyword.value }
    })
    if (res.data.code === '200') {
      tableData.value = res.data.data
    }
  } catch(e) {
    ElMessage.error('加载失败')
  } finally {
    loading.value = false
  }
}

const handleToggleHot = async (row) => {
  try {
    await axios.post('http://localhost:8080/attraction/toggleHot', row)
    ElMessage.success('热门状态已更新')
  } catch(e) {
    row.isHot = row.isHot === 1 ? 0 : 1
    ElMessage.error('操作失败')
  }
}

const openEdit = (row) => {
  Object.assign(editForm, JSON.parse(JSON.stringify(row)))
  showEdit.value = true
}

const submitEdit = async () => {
  if (!editForm.latitude || !editForm.longitude) {
    ElMessage.warning('为了地图功能正常，请务必填写经纬度！')
  }

  try {
    const res = await axios.post('http://localhost:8080/attraction/update', editForm)
    if (res.data.code === '200') {
      ElMessage.success('修改成功')
      showEdit.value = false
      loadData()
    } else {
      ElMessage.error(res.data.msg || '修改失败')
    }
  } catch(e) {
    ElMessage.error('网络错误')
  }
}

const handleDelete = (row) => {
  ElMessageBox.confirm(`确定要删除【${row.name}】吗？此操作不可恢复。`, '警告', {
    type: 'warning',
    confirmButtonText: '确定删除',
    cancelButtonText: '取消'
  }).then(async () => {
    try {
      const res = await axios.delete(`http://localhost:8080/attraction/delete/${row.spotId}`)
      if (res.data.code === '200') {
        ElMessage.success('已删除')
        loadData()
      } else {
        ElMessage.error(res.data.msg)
      }
    } catch(e) {
      ElMessage.error('删除失败')
    }
  })
}

const handleEditUploadSuccess = (res) => {
  if (res.code === '200') {
    editForm.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('图片上传失败')
  }
}

onMounted(loadData)
</script>

<style scoped>
.spot-manage { padding: 0; }
.filter-bar { display: flex; align-items: center; margin-bottom: 20px; }

.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: .3s;
}
.avatar-uploader .el-upload:hover { border-color: #409EFF; }
.avatar-uploader-icon { font-size: 28px; color: #8c939d; width: 120px; height: 120px; text-align: center; line-height: 120px; }
.avatar { width: 120px; height: 120px; display: block; object-fit: cover; }

.image-slot { display: flex; justify-content: center; align-items: center; width: 100%; height: 100%; background: #f5f7fa; color: #909399; font-size: 12px; }
</style>