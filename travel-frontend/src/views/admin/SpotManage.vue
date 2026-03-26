<template>
  <div class="spot-manage">
    <el-card class="box-card" shadow="hover">
      <template #header>
        <div class="card-header">
          <span>🗺️ 景点资源库</span>
          <div class="header-actions">
            <el-input
                v-model="searchQuery.keyword"
                placeholder="搜索景点/标签..."
                style="width: 200px; margin-right: 12px;"
                clearable
                @keyup.enter="handleSearch"
            />
            <el-input
                v-model="searchQuery.city"
                placeholder="城市筛选..."
                style="width: 150px; margin-right: 12px;"
                clearable
                @keyup.enter="handleSearch"
            />
            <el-button type="primary" @click="handleSearch" :loading="loading">
              <el-icon><Search /></el-icon> 检索
            </el-button>
            <el-button type="success" @click="openAddDialog" :disabled="!isAdmin">
              <el-icon><Plus /></el-icon> 新增景点
            </el-button>
          </div>
        </div>
      </template>

      <el-table :data="tableData" v-loading="loading" border stripe highlight-current-row style="width: 100%">
        <el-table-column prop="spotId" label="ID" width="70" align="center" />

        <el-table-column label="主图" width="100" align="center">
          <template #default="scope">
            <el-image
                :src="scope.row.imageUrl || FALLBACK_IMG"
                :preview-src-list="[scope.row.imageUrl || FALLBACK_IMG]"
                fit="cover"
                class="table-img"
                preview-teleported
            >
              <template #error>
                <div class="image-slot">
                  <el-icon><icon-picture /></el-icon>
                </div>
              </template>
            </el-image>
          </template>
        </el-table-column>

        <el-table-column prop="name" label="景点名称" min-width="250" show-overflow-tooltip>
          <template #default="scope">
            <strong>{{ scope.row.name }}</strong>
          </template>
        </el-table-column>

        <el-table-column prop="city" label="城市" width="100" align="center" />

        <el-table-column prop="ticketPrice" label="门票(元)" width="100" align="center">
          <template #default="scope">
            <span class="price-text">¥{{ scope.row.ticketPrice }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="rating" label="星级" width="80" align="center">
          <template #default="scope">
            <span style="color: #e6a23c; font-weight: bold;">{{ scope.row.rating }}</span>
          </template>
        </el-table-column>

        <el-table-column prop="isHot" label="热门推荐" width="100" align="center">
          <template #default="scope">
            <el-switch
                v-model="scope.row.isHot"
                :active-value="1"
                :inactive-value="0"
                :disabled="!isAdmin"
                @change="handleToggleHot(scope.row)"
            />
          </template>
        </el-table-column>

        <el-table-column label="高危操作" width="180" fixed="right" align="center">
          <template #default="scope">
            <el-button size="small" type="primary" plain @click="openEditDialog(scope.row)" :disabled="!isAdmin">
              编辑
            </el-button>
            <el-button size="small" type="danger" plain @click="handleDelete(scope.row)" :disabled="!isAdmin">
              删除
            </el-button>
          </template>
        </el-table-column>
      </el-table>

      <div class="pagination-container">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :page-sizes="[10, 20, 50]"
            layout="total, sizes, prev, pager, next, jumper"
            :total="total"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
        />
      </div>
    </el-card>

    <el-dialog :title="dialogTitle" v-model="dialogVisible" width="750px" destroy-on-close :close-on-click-modal="false" top="5vh">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px" class="spot-form">
        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="景点名称" prop="name">
              <el-input v-model="form.name" placeholder="请输入核心景观名称" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所在城市" prop="city">
              <el-input v-model="form.city" placeholder="如: 北京" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="景点主图" prop="imageUrl" class="upload-form-item">
          <div class="upload-container">
            <el-radio-group v-model="uploadMode" size="small" style="margin-bottom: 12px;">
              <el-radio-button label="url">🔗 外部链接</el-radio-button>
              <el-radio-button label="file">📁 本地上传</el-radio-button>
            </el-radio-group>

            <div class="upload-content">
              <el-input
                  v-if="uploadMode === 'url'"
                  v-model="form.imageUrl"
                  placeholder="请输入可访问的公网图片 URL (如 https://...)"
                  clearable
              >
                <template #prepend>HTTPS://</template>
              </el-input>

              <el-upload
                  v-if="uploadMode === 'file'"
                  class="image-uploader"
                  action="http://localhost:8080/file/upload"
                  :show-file-list="false"
                  :headers="uploadHeaders"
                  :on-success="handleUploadSuccess"
                  :before-upload="beforeUpload"
                  accept=".jpg,.jpeg,.png,.gif,.webp"
              >
                <img v-if="form.imageUrl && uploadMode === 'file'" :src="form.imageUrl" class="avatar" />
                <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
                <template #tip>
                  <div class="el-upload__tip">只能上传 jpg/png 文件，且不超过 5MB</div>
                </template>
              </el-upload>

              <transition name="el-fade-in">
                <div v-if="form.imageUrl && uploadMode === 'url'" class="url-preview">
                  <span class="preview-label">预览：</span>
                  <el-image
                      :src="form.imageUrl"
                      fit="cover"
                      class="preview-img"
                      :preview-src-list="[form.imageUrl]"
                  >
                    <template #error>
                      <div class="image-slot">
                        <el-icon><icon-picture /></el-icon> 加载失败
                      </div>
                    </template>
                  </el-image>
                </div>
              </transition>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="详细地址" prop="address">
          <el-input v-model="form.address" placeholder="请输入精确的导航地址" />
        </el-form-item>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="门票价格" prop="ticketPrice">
              <el-input-number v-model="form.ticketPrice" :min="0" :precision="2" :step="10" class="w-100" :controls="false">
                <template #prefix>￥</template>
              </el-input-number>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="推荐星级" prop="rating">
              <el-rate v-model="form.rating" allow-half show-score text-color="#ff9900" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="20">
          <el-col :span="12">
            <el-form-item label="经度(Lng)" prop="longitude">
              <el-input v-model="form.longitude" placeholder="建议使用高德地图坐标拾取器" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="纬度(Lat)" prop="latitude">
              <el-input v-model="form.latitude" placeholder="建议使用高德地图坐标拾取器" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="特色标签" prop="tags">
          <el-input v-model="form.tags" placeholder="使用英文逗号分隔，如: 5A景区,亲子,避暑" />
        </el-form-item>

        <el-form-item label="图文介绍" prop="description">
          <el-input
              v-model="form.description"
              type="textarea"
              :rows="5"
              placeholder="请输入吸引游客的详细文案，支持简单的 HTML 标签..."
              maxlength="2000"
              show-word-limit
          />
        </el-form-item>
      </el-form>

      <template #footer>
        <span class="dialog-footer">
          <el-button @click="dialogVisible = false">取消</el-button>
          <el-button type="primary" @click="submitForm" :loading="submitLoading">确认提交</el-button>
        </span>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, computed, onMounted } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { Search, Plus, Picture as IconPicture } from '@element-plus/icons-vue'
import request from '../../utils/request'

const FALLBACK_IMG = 'https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?ixlib=rb-1.2.1&auto=format&fit=crop&w=150&q=80'
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const token = localStorage.getItem('token')

const isAdmin = computed(() => currentUser.userId === 88 || currentUser.role === 'admin')

const uploadMode = ref('url')
const uploadHeaders = computed(() => ({
  Authorization: token ? `Bearer ${token}` : ''
}))

const tableData = ref([])
const loading = ref(false)
const searchQuery = reactive({ keyword: '', city: '' })

const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)

const dialogVisible = ref(false)
const submitLoading = ref(false)
const dialogTitle = ref('新增景点')
const formRef = ref(null)

const form = reactive({
  spotId: null,
  name: '',
  city: '',
  address: '',
  ticketPrice: 0,
  rating: 4.5,
  longitude: '',
  latitude: '',
  imageUrl: '',
  tags: '',
  description: ''
})

const rules = {
  name: [{ required: true, message: '景观名称绝不可为空', trigger: 'blur' }],
  imageUrl: [{ required: true, message: '请上传图片或输入链接', trigger: 'change' }],
  ticketPrice: [{ required: true, message: '必须设定票价', trigger: 'blur' }],
}

const fetchData = async () => {
  loading.value = true
  try {
    const res = await request.get('/attraction/list', {
      params: {
        ...searchQuery,
        pageNum: currentPage.value,
        pageSize: pageSize.value
      }
    })
    if (res.code === '200') {
      tableData.value = res.data.list || []
      total.value = res.data.total || 0
    }
  } finally {
    loading.value = false
  }
}

const handleSearch = () => {
  currentPage.value = 1
  fetchData()
}

const handlePageChange = (val) => {
  currentPage.value = val
  fetchData()
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
  fetchData()
}

const handleToggleHot = async (row) => {
  if (!isAdmin.value) return
  try {
    const res = await request.post('/attraction/toggleHot', { spotId: row.spotId })
    if (res.code !== '200') row.isHot = row.isHot === 1 ? 0 : 1
    else ElMessage.success('热门状态已更新')
  } catch (error) {
    row.isHot = row.isHot === 1 ? 0 : 1
  }
}

const openAddDialog = () => {
  dialogTitle.value = '➕ 录入新景点'
  resetForm()
  uploadMode.value = 'file'
  dialogVisible.value = true
}

const openEditDialog = (row) => {
  dialogTitle.value = '✏️ 编辑景点档案'
  Object.assign(form, JSON.parse(JSON.stringify(row)))
  uploadMode.value = form.imageUrl && form.imageUrl.includes('/files/') ? 'file' : 'url'
  dialogVisible.value = true
}

const resetForm = () => {
  if (formRef.value) formRef.value.resetFields()
  Object.keys(form).forEach(key => form[key] = '')
  form.spotId = null
  form.ticketPrice = 0
  form.rating = 4.5
}

const handleUploadSuccess = (res) => {
  if (res.code === '200') {
    form.imageUrl = res.data
    ElMessage.success('图片上传成功')
    formRef.value.clearValidate('imageUrl')
  } else {
    ElMessage.error(res.msg || '上传失败')
  }
}

const beforeUpload = (file) => {
  const isImg = ['image/jpeg', 'image/png', 'image/gif', 'image/webp'].includes(file.type)
  const isLt5M = file.size / 1024 / 1024 < 5

  if (!isImg) ElMessage.error('上传图片只能是 JPG/PNG/GIF/WEBP 格式!')
  if (!isLt5M) ElMessage.error('上传图片大小不能超过 5MB!')
  return isImg && isLt5M
}

const submitForm = () => {
  formRef.value.validate(async (valid) => {
    if (valid) {
      submitLoading.value = true
      try {
        const url = form.spotId ? '/attraction/update' : '/attraction/add'
        const res = await request.post(url, form)
        if (res.code === '200') {
          ElMessage.success(form.spotId ? '档案更新完毕' : '新景点录入成功')
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
    await ElMessageBox.confirm(
        `确定要销毁 <strong>${row.name}</strong> 吗？`, '⚠️ 高危警报',
        { confirmButtonText: '销毁', cancelButtonText: '取消', type: 'error', dangerouslyUseHTMLString: true }
    )
    const res = await request.delete(`/attraction/delete/${row.spotId}`)
    if (res.code === '200') {
      ElMessage.success('数据已销毁')
      fetchData()
    }
  } catch (e) { /* cancel */ }
}

onMounted(fetchData)
</script>

<style scoped>
.spot-manage {
  padding: 24px;
  background-color: #f0f2f5;
  min-height: calc(100vh - 60px);
}
.box-card { border-radius: 8px; }
.card-header { display: flex; justify-content: space-between; align-items: center; font-weight: 600; font-size: 16px; color: #303133; }
.header-actions { display: flex; align-items: center; }
.table-img { width: 60px; height: 60px; border-radius: 8px; border: 1px solid #ebeef5; box-shadow: 0 2px 4px rgba(0,0,0,0.1); }
.price-text { color: #f56c6c; font-weight: 700; font-size: 15px; }
.pagination-container { margin-top: 20px; display: flex; justify-content: flex-end; }
.w-100 { width: 100%; }

/* 上传组件样式优化 */
.upload-container {
  background-color: #f8f9fa;
  padding: 15px;
  border-radius: 8px;
  border: 1px dashed #dcdfe6;
  transition: all 0.3s;
}
.upload-container:hover { border-color: #409eff; }
.image-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.image-uploader .el-upload:hover { border-color: var(--el-color-primary); }
.avatar-uploader-icon {
  font-size: 28px;
  color: #8c939d;
  width: 120px;
  height: 120px;
  text-align: center;
  line-height: 120px;
}
.avatar { width: 120px; height: 120px; display: block; object-fit: cover; }
.url-preview {
  margin-top: 10px;
  display: flex;
  align-items: center;
  background: #fff;
  padding: 8px;
  border-radius: 4px;
}
.preview-label { font-size: 12px; color: #909399; margin-right: 8px; }
.preview-img { width: 60px; height: 60px; border-radius: 4px; border: 1px solid #eee; }
.image-slot { display: flex; justify-content: center; align-items: center; width: 100%; height: 100%; background: #f5f7fa; color: #909399; font-size: 14px; }
</style>