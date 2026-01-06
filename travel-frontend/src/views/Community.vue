<template>
  <div class="community-page">
    <div class="page-header">
      <div class="title-group">
        <h2>📒 游记攻略</h2>
        <span class="subtitle">发现更广阔的世界，分享你的旅途故事</span>
      </div>
      <el-button type="warning" size="large" icon="Edit" round @click="showPublish = true">发布游记</el-button>
    </div>

    <div class="post-list" v-loading="loading">
      <div v-for="post in postList" :key="post.postId" class="post-item" @click="$router.push('/post/'+post.postId)">
        <div class="post-cover">
          <img :src="post.coverImg || 'https://via.placeholder.com/240x160?text=No+Image'" @error="e => e.target.src='https://via.placeholder.com/240x160?text=No+Image'" />
        </div>
        <div class="post-content">
          <h3 class="post-title">{{ post.title }}</h3>
          <p class="post-excerpt">{{ stripHtml(post.content) }}</p>

          <div class="post-meta">
            <div class="author">
              <el-avatar :size="24" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <span class="name">{{ post.authorName || '匿名旅行家' }}</span>
            </div>
            <div class="stats">
              <span>👁 {{ post.viewCount || 0 }} 浏览</span>
              <span>🕒 {{ formatTime(post.createTime) }}</span>
            </div>
          </div>
        </div>
      </div>
      <el-empty v-if="postList.length === 0 && !loading" description="暂无攻略，快来抢占沙发！" />
    </div>

    <el-dialog v-model="showPublish" title="发布精彩游记" width="700px" destroy-on-close>
      <el-form :model="form" label-position="top">
        <el-form-item label="标题 *">
          <el-input v-model="form.title" placeholder="起个好标题..." />
        </el-form-item>

        <el-form-item label="封面美图">
          <el-upload
              class="avatar-uploader"
              action="http://localhost:8080/upload"
              :show-file-list="false"
              :on-success="handlePostUploadSuccess"
              :before-upload="beforeUpload"
          >
            <img v-if="form.coverImg" :src="form.coverImg" class="avatar" />
            <el-icon v-else class="avatar-uploader-icon"><Plus /></el-icon>
          </el-upload>
        </el-form-item>

        <el-form-item label="正文内容 (支持图文混排)">
          <el-upload
              action="http://localhost:8080/upload"
              :show-file-list="false"
              :on-success="handleInsertImage"
              style="display: inline-block; margin-bottom: 10px;"
          >
            <el-button size="small" type="primary" icon="Picture">插入图片到正文</el-button>
          </el-upload>

          <el-input
              v-model="form.content"
              type="textarea"
              :rows="12"
              placeholder="写下你的旅途故事... (点击上方按钮可插入图片)"
          />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="showPublish = false">取消</el-button>
        <el-button @click="handlePublish" type="primary" :loading="publishing">发布</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Edit, Plus, Picture } from '@element-plus/icons-vue'

const postList = ref([])
const loading = ref(false)
const showPublish = ref(false)
const publishing = ref(false)
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const form = reactive({
  title: '',
  coverImg: '',
  content: ''
})

const loadPosts = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/post/list')
    if (res.data.code === '200') postList.value = res.data.data
  } catch(e) { ElMessage.error('获取列表失败') }
  finally { loading.value = false }
}

const handlePublish = async () => {
  if (!form.title || !form.content) {
    ElMessage.warning('请填写标题和正文')
    return
  }

  publishing.value = true
  try {
    const payload = {
      title: form.title,
      content: form.content,
      coverImg: form.coverImg,
      userId: currentUser.userId || 1
    }
    const res = await axios.post('http://localhost:8080/post/add', payload)
    if (res.data.code === '200') {
      ElMessage.success('发布成功！')
      showPublish.value = false
      form.title = ''; form.content = ''; form.coverImg = ''
      loadPosts()
    } else {
      ElMessage.error(res.data.msg || '发布失败')
    }
  } catch(e) { ElMessage.error('发布请求出错') }
  finally { publishing.value = false }
}

const handlePostUploadSuccess = (res) => {
  if (res.code === '200') form.coverImg = res.data
}

const beforeUpload = (rawFile) => {
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('图片不能超过5MB!')
    return false
  }
  return true
}

// 🔥 核心修复：插入 HTML 图片标签，而非 Markdown
const handleInsertImage = (res) => {
  if (res.code === '200') {
    // 使用 img 标签，并加上样式限制宽度，防止图片过大撑破页面
    const imgHtml = `\n<img src="${res.data}" style="max-width:100%; border-radius:8px; margin: 10px 0; display:block;" />\n`
    form.content += imgHtml
    ElMessage.success('图片已插入')
  }
}

// 🔥 辅助函数：去除 HTML 标签，仅保留纯文本用于列表展示
const stripHtml = (html) => {
  if (!html) return ''
  // 将 <br> 换成空格，然后去除所有标签
  let text = html.replace(/<br\s*\/?>/gi, ' ')
  return text.replace(/<[^>]+>/g, '').substring(0, 100) + '...'
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

onMounted(loadPosts)
</script>

<style scoped>
.community-page { max-width: 1000px; margin: 30px auto; padding: 0 20px; }
.page-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 40px; }
.title-group h2 { margin: 0; font-size: 28px; color: #333; }
.subtitle { color: #999; margin-top: 5px; display: block; }

.post-list { display: flex; flex-direction: column; gap: 20px; }
.post-item { display: flex; background: white; padding: 20px; border-radius: 12px; cursor: pointer; transition: all 0.3s; border: 1px solid #f0f0f0; }
.post-item:hover { box-shadow: 0 8px 25px rgba(0,0,0,0.08); transform: translateY(-2px); border-color: #ff9d00; }

.post-cover { width: 240px; height: 160px; flex-shrink: 0; border-radius: 8px; overflow: hidden; margin-right: 20px; }
.post-cover img { width: 100%; height: 100%; object-fit: cover; transition: 0.5s; }
.post-item:hover .post-cover img { transform: scale(1.05); }

.post-content { flex: 1; display: flex; flex-direction: column; justify-content: space-between; }
.post-title { margin: 0 0 10px 0; font-size: 20px; color: #333; line-height: 1.4; }
.post-item:hover .post-title { color: #ff9d00; }
.post-excerpt { color: #666; font-size: 14px; line-height: 1.6; margin: 0; display: -webkit-box; -webkit-line-clamp: 3; -webkit-box-orient: vertical; overflow: hidden; flex: 1; }

.post-meta { display: flex; justify-content: space-between; align-items: center; margin-top: 15px; color: #999; font-size: 13px; }
.author { display: flex; align-items: center; gap: 8px; }
.stats { display: flex; gap: 15px; }

/* 上传框样式 */
.avatar-uploader-icon { border: 1px dashed #d9d9d9; padding: 30px; font-size: 28px; color: #8c939d; width: 100px; height: 100px; text-align: center; border-radius: 6px; }
.avatar { width: 100px; height: 100px; object-fit: cover; border-radius: 6px; }
</style>