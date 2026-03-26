<template>
  <div class="banner-manage">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🖼️ 首页轮播图管理</span>
          <el-button type="success" @click="openAddDialog" :disabled="!isAdmin">
            <el-icon><Plus /></el-icon> 新增轮播图
          </el-button>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe style="width: 100%">
        <el-table-column prop="bannerId" label="ID" width="70" align="center" />

        <el-table-column label="轮播图片" min-width="250" align="center">
          <template #default="scope">
            <el-image
                :src="scope.row.imageUrl"
                :preview-src-list="[scope.row.imageUrl]"
                fit="cover"
                class="banner-img"
                preview-teleported
            />
          </template>
        </el-table-column>

        <el-table-column prop="sortOrder" label="排序(越小越靠前)" width="150" align="center">
          <template #default="scope">
            <el-tag type="info">{{ scope.row.sortOrder }}</el-tag>
          </template>
        </el-table-column>

        <el-table-column prop="isShow" label="前台展示状态" width="150" align="center">
          <template #default="scope">
            <el-switch
                v-model="scope.row.isShow"
                :active-value="1"
                :inactive-value="0"
                :disabled="!isAdmin"
                @change="handleStatusChange(scope.row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openEditDialog(scope.row)" :disabled="!isAdmin">编辑</el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(scope.row)" :disabled="!isAdmin">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="600px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
        <el-form-item label="轮播图片" prop="imageUrl">
          <el-radio-group v-model="uploadMode" size="small" style="margin-bottom: 10px;">
            <el-radio-button label="url">外部链接</el-radio-button>
            <el-radio-button label="file">本地上传</el-radio-button>
          </el-radio-group>

          <el-input v-if="uploadMode === 'url'" v-model="form.imageUrl" placeholder="请输入图片外链" clearable />

          <el-upload
              v-if="uploadMode === 'file'"
              class="image-uploader"
              action="http://localhost:8080/file/upload"
              :show-file-list="false"
              :headers="uploadHeaders"
              :on-success="handleUploadSuccess"
              accept=".jpg,.jpeg,.png,.gif,.webp"
          >
            <img v-if="form.imageUrl && uploadMode === 'file'" :src="form.imageUrl" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="跳转链接" prop="linkUrl">
          <el-input v-model="form.linkUrl" placeholder="点击图片后跳转的网址（可选）" />
        </el-form-item>

        <el-row>
          <el-col :span="12">
            <el-form-item label="显示排序" prop="sortOrder">
              <el-input-number v-model="form.sortOrder" :min="0" :max="999" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="是否展示" prop="isShow">
              <el-switch v-model="form.isShow" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确认保存</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Plus } from '@element-plus/icons-vue'
import request from '../../utils/request'

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const token = localStorage.getItem('token')
const isAdmin = computed(() => currentUser.userId === 88 || currentUser.role === 'admin')

const uploadMode = ref('url')
const uploadHeaders = computed(() => ({ Authorization: token ? `Bearer ${token}` : '' }))

const tableData = ref([])
const loading = ref(false)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const dialogTitle = ref('新增轮播图')
const formRef = ref(null)

const form = reactive({
  bannerId: null,
  imageUrl: '',
  linkUrl: '',
  sortOrder: 0,
  isShow: 1
})

const rules = {
  imageUrl: [{ required: true, message: '图片地址不能为空', trigger: 'change' }]
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/banner/list')
    if (res.code === '200') tableData.value = res.data
  } finally {
    loading.value = false
  }
}

const handleStatusChange = async (row) => {
  if (!isAdmin.value) return
  try {
    await request.post('/banner/update', row)
    ElMessage.success('状态更新成功')
  } catch (e) {
    row.isShow = row.isShow === 1 ? 0 : 1
  }
}

const openAddDialog = () => {
  dialogTitle.value = '新增轮播图'
  if (formRef.value) formRef.value.resetFields()
  Object.keys(form).forEach(key => form[key] = '')
  form.bannerId = null; form.sortOrder = 0; form.isShow = 1
  uploadMode.value = 'file'
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogTitle.value = '编辑轮播图'
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  uploadMode.value = form.imageUrl && form.imageUrl.includes('/files/') ? 'file' : 'url'
  dialogVisible.value = true
}

const handleUploadSuccess = (res) => {
  if (res.code === '200') {
    form.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const url = form.bannerId ? '/banner/update' : '/banner/add'
        const res = await request.post(url, form)
        if (res.code === '200') {
          ElMessage.success('保存成功')
          dialogVisible.value = false
          fetchData()
        }
      } finally {
        submitLoading.value = false
      }
    }
  })
}

const handleDelete = async (row) => {
  try {
    await ElMessageBox.confirm('确定要删除这张轮播图吗？', '警告', { type: 'warning' })
    const res = await request.delete(`/banner/delete/${row.bannerId}`)
    if (res.code === '200') {
      ElMessage.success('删除成功')
      fetchData()
    }
  } catch (e) {}
}

onMounted(fetchData)
</script>

<style scoped>
.banner-manage { padding: 24px; background-color: #f0f2f5; min-height: calc(100vh - 60px); }
.box-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: bold; }
.banner-img { width: 160px; height: 90px; border-radius: 6px; border: 1px solid #eee; }

.image-uploader .el-upload { border: 1px dashed #d9d9d9; border-radius: 6px; cursor: pointer; overflow: hidden; }
.avatar-uploader-icon { font-size: 28px; color: #8c939d; width: 160px; height: 90px; line-height: 90px; text-align: center; border: 1px dashed #ddd; border-radius: 6px;}
.avatar { width: 160px; height: 90px; display: block; object-fit: cover; }
</style>