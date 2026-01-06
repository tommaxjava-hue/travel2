<template>
  <div class="ai-add-page">
    <div class="page-header">
      <h2>✨ AI 智能景点录入</h2>
      <p class="tip">粘贴一段文本，AI 自动提取名称、地址、票价、经纬度等核心信息。</p>
    </div>

    <div class="workspace">
      <el-card class="input-panel" header="1. 粘贴原始文本">
        <el-input
            v-model="rawText"
            type="textarea"
            :rows="18"
            placeholder="请粘贴景点介绍，例如：
北京环球度假区位于北京市通州区...门票418元...
（提示：文本越详细，AI 提取越准确）"
        />
        <div class="btn-area">
          <el-button type="primary" size="large" @click="analyzeText" :loading="analyzing" icon="MagicStick">
            AI 一键提取
          </el-button>
          <el-button @click="rawText = ''">清空</el-button>
        </div>
      </el-card>

      <el-card class="form-panel" header="2. 确认并入库">
        <el-form :model="form" label-width="90px">
          <el-form-item label="景点名称 *">
            <el-input v-model="form.name" placeholder="必填" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="城市">
                <el-input v-model="form.city" placeholder="如：北京" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="门票">
                <el-input-number v-model="form.ticketPrice" :min="0" style="width: 100%" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="详细地址 *">
            <el-input v-model="form.address" type="textarea" :rows="2" placeholder="必填，用于地图定位" />
          </el-form-item>

          <el-row :gutter="20">
            <el-col :span="12">
              <el-form-item label="纬度 (Lat)">
                <el-input v-model="form.latitude" placeholder="39.90" />
              </el-form-item>
            </el-col>
            <el-col :span="12">
              <el-form-item label="经度 (Lng)">
                <el-input v-model="form.longitude" placeholder="116.40" />
              </el-form-item>
            </el-col>
          </el-row>

          <el-form-item label="开放时间">
            <el-input v-model="form.openTime" />
          </el-form-item>

          <el-form-item label="景点介绍">
            <el-input v-model="form.description" type="textarea" :rows="4" />
          </el-form-item>

          <el-form-item label="封面图片">
            <el-upload
                class="avatar-uploader"
                action="http://localhost:8080/upload"
                :show-file-list="false"
                :on-success="handleUploadSuccess"
                :before-upload="beforeUpload"
            >
              <img v-if="form.imageUrl" :src="form.imageUrl" class="avatar" />
              <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
            </el-upload>
          </el-form-item>

          <el-button type="success" class="w-100" @click="submitToDb" :loading="submitting">
            确认无误，保存入库
          </el-button>
        </el-form>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { MagicStick, Plus } from '@element-plus/icons-vue'

const rawText = ref('')
const analyzing = ref(false)
const submitting = ref(false)

const form = reactive({
  name: '',
  city: '',
  address: '', // 🔥 必须有这个字段
  latitude: '',
  longitude: '',
  ticketPrice: 0,
  openTime: '',
  description: '',
  imageUrl: ''
})

// 1. 调用 AI 解析
const analyzeText = async () => {
  if (!rawText.value || rawText.value.length < 5) return ElMessage.warning('请先粘贴文本')

  analyzing.value = true
  try {
    const res = await axios.post('http://localhost:8080/ai/parse', { text: rawText.value })
    if (res.data.code === '200') {
      const data = JSON.parse(res.data.data)

      // 🔥 修复：将 AI 返回的所有字段都映射到 form
      form.name = data.name || ''
      form.city = data.city || ''
      form.address = data.address || '' // 关键修复
      form.ticketPrice = data.ticketPrice || 0
      form.openTime = data.openTime || '全天'
      form.description = data.description || ''

      // 如果 AI 返回了坐标，也填进去
      if(data.latitude) form.latitude = data.latitude
      if(data.longitude) form.longitude = data.longitude

      ElMessage.success('AI 解析成功，请核对信息')
    } else {
      ElMessage.error('解析失败: ' + res.data.msg)
    }
  } catch (e) {
    ElMessage.error('AI 接口调用出错')
  } finally {
    analyzing.value = false
  }
}

// 2. 保存入库
const submitToDb = async () => {
  // 手动校验
  if (!form.name) return ElMessage.warning('名称必填')
  if (!form.address) return ElMessage.warning('地址必填 (AI没提取到请手动填写)')

  // 经纬度检查 (为了地图功能)
  if(!form.latitude || !form.longitude) {
    ElMessage.warning('提示：未填写经纬度，地图上将无法显示此景点')
  }

  submitting.value = true
  try {
    const payload = {
      ...form,
      contentText: form.description,
      rating: 4.8, // 默认评分
      isHot: 0
    }
    const res = await axios.post('http://localhost:8080/attraction/add', payload)

    if (res.data.code === '200') {
      ElMessage.success('入库成功！')
      // 重置表单
      Object.keys(form).forEach(key => form[key] = '')
      form.ticketPrice = 0
      rawText.value = ''
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('提交失败')
  } finally {
    submitting.value = false
  }
}

// 图片上传
const handleUploadSuccess = (res) => {
  if (res.code === '200') {
    form.imageUrl = res.data
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const beforeUpload = (rawFile) => {
  // 🔥 放宽限制到 10MB
  if (rawFile.size / 1024 / 1024 > 10) {
    ElMessage.error('图片不能超过 10MB!')
    return false
  }
  return true
}
</script>

<style scoped>
.ai-add-page { max-width: 1200px; margin: 20px auto; padding: 0 20px; }
.workspace { display: flex; gap: 20px; margin-top: 20px; }
.input-panel { flex: 1; }
.form-panel { flex: 1.2; }
.btn-area { margin-top: 15px; display: flex; gap: 10px; }
.w-100 { width: 100%; margin-top: 20px; }

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
</style>