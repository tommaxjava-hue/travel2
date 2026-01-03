<template>
  <div class="ai-add-page">
    <div class="page-header">
      <h2>✨ AI 智能景点录入</h2>
      <p class="tip">从马蜂窝/携程复制一段介绍，AI 自动帮你提取字段入库。</p>
    </div>

    <div class="workspace">
      <el-card class="input-panel" header="1. 粘贴原始文本">
        <el-input
            v-model="rawText"
            type="textarea"
            :rows="15"
            placeholder="请粘贴一段乱七八糟的景点介绍文本，例如：
外滩位于上海市黄浦区的黄浦江畔，全长1.5公里...门票是免费的，全天开放..."
        />
        <div class="btn-area">
          <el-button type="primary" size="large" @click="analyzeText" :loading="analyzing" icon="MagicStick">
            AI 一键提取
          </el-button>
          <el-button @click="rawText = ''">清空</el-button>
        </div>
      </el-card>

      <el-card class="form-panel" header="2. 确认并入库">
        <el-form :model="form" label-width="80px">
          <el-form-item label="景点名称">
            <el-input v-model="form.name" />
          </el-form-item>
          <el-form-item label="所属城市">
            <el-select v-model="form.city" style="width: 100%">
              <el-option value="上海" label="上海" />
              <el-option value="北京" label="北京" />
              <el-option value="其他" label="其他" />
            </el-select>
          </el-form-item>
          <el-form-item label="门票价格">
            <el-input-number v-model="form.ticketPrice" :min="0" />
          </el-form-item>
          <el-form-item label="开放时间">
            <el-input v-model="form.openTime" />
          </el-form-item>
          <el-form-item label="景点介绍">
            <el-input v-model="form.description" type="textarea" :rows="4" />
          </el-form-item>

          <el-form-item label="景点封面">
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
import { MagicStick, Plus } from '@element-plus/icons-vue' // 引入图标

const rawText = ref('')
const analyzing = ref(false)
const submitting = ref(false)

const form = reactive({
  name: '',
  city: '上海',
  ticketPrice: 0,
  openTime: '',
  description: '',
  imageUrl: '' // 默认为空，等待上传或AI填入
})

// 1. 调用 AI 解析文本
const analyzeText = async () => {
  if (!rawText.value || rawText.value.length < 10) return ElMessage.warning('请先粘贴一段足够长的文本')

  analyzing.value = true
  try {
    const res = await axios.post('http://localhost:8080/ai/parse', { text: rawText.value })
    if (res.data.code === '200') {
      const data = JSON.parse(res.data.data) // 解析 AI 返回的 JSON 字符串

      // 自动填表
      form.name = data.name || ''
      form.city = data.city || '上海'
      form.ticketPrice = data.ticketPrice || 0
      form.openTime = data.openTime || '全天'
      form.description = data.description || ''
      // 注意：AI 解析不出图片URL，图片通常需要人工上传

      ElMessage.success('AI 解析成功！请手动上传图片后保存')
    } else {
      ElMessage.error('AI 解析失败: ' + res.data.msg)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('解析出错，请检查后端 AI 接口')
  } finally {
    analyzing.value = false
  }
}

// 2. 保存到数据库
const submitToDb = async () => {
  if (!form.name) return ElMessage.warning('名称不能为空')

  submitting.value = true
  try {
    const payload = {
      ...form,
      contentText: form.description, // 确保 AI RAG 可用
      rating: 5.0
    }

    const res = await axios.post('http://localhost:8080/attraction/add', payload)

    if (res.data.code === '200') {
      ElMessage.success(`🎉 成功！景点【${form.name}】已正式上线！`)
      // 清空表单
      form.name = ''
      form.description = ''
      form.ticketPrice = 0
      form.openTime = ''
      form.imageUrl = ''
      rawText.value = ''
    } else {
      ElMessage.error('入库失败: ' + res.data.msg)
    }
  } catch (e) {
    console.error(e)
    ElMessage.error('网络错误')
  } finally {
    submitting.value = false
  }
}

// --- 图片上传相关逻辑 ---
const handleUploadSuccess = (res) => {
  if (res.code === '200') {
    form.imageUrl = res.data // 后端返回的是完整URL
    ElMessage.success('图片上传成功')
  } else {
    ElMessage.error('上传失败')
  }
}

const beforeUpload = (rawFile) => {
  if (rawFile.type !== 'image/jpeg' && rawFile.type !== 'image/png') {
    ElMessage.error('图片必须是 JPG 或 PNG 格式!')
    return false
  } else if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片大小不能超过 5MB!')
    return false
  }
  return true
}
</script>

<style scoped>
.ai-add-page { max-width: 1100px; margin: 0 auto; }
.page-header { margin-bottom: 20px; }
.tip { color: #666; font-size: 14px; }
.workspace { display: flex; gap: 20px; }
.input-panel, .form-panel { flex: 1; }
.btn-area { margin-top: 15px; display: flex; gap: 10px; }
.w-100 { width: 100%; margin-top: 20px; }

/* 上传组件样式 */
.avatar-uploader .el-upload {
  border: 1px dashed #d9d9d9;
  border-radius: 6px;
  cursor: pointer;
  position: relative;
  overflow: hidden;
  transition: var(--el-transition-duration-fast);
}
.avatar-uploader .el-upload:hover { border-color: #409EFF; }
.avatar-uploader-icon {
  font-size: 28px; color: #8c939d;
  width: 100px; height: 100px;
  text-align: center; line-height: 100px;
}
.avatar { width: 100px; height: 100px; display: block; object-fit: cover; }
</style>