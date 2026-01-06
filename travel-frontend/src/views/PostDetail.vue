<template>
  <div class="post-detail-page" v-loading="loading">
    <el-button @click="$router.back()" style="margin-bottom: 20px" icon="Back">返回列表</el-button>

    <div v-if="post" class="post-container">
      <div class="post-header">
        <h1>{{ post.title }}</h1>
        <div class="meta">
          <div class="author">
            <el-avatar :size="30" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
            <span class="name">{{ post.authorName || '匿名用户' }}</span>
          </div>
          <span>🕒 {{ formatTime(post.createTime) }}</span>
          <span>👁 {{ post.viewCount }} 浏览</span>
        </div>
      </div>

      <div v-if="post.coverImg" class="cover-img">
        <img :src="post.coverImg" alt="封面" />
      </div>

      <div class="post-body">
        <div class="content-text" v-html="post.content"></div>
      </div>
    </div>

    <el-empty v-else description="攻略不存在或已删除" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'
import { Back } from '@element-plus/icons-vue'

const route = useRoute()
const post = ref(null)
const loading = ref(false)

// 时间格式化
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

onMounted(async () => {
  loading.value = true
  try {
    const res = await axios.get(`http://localhost:8080/post/detail/${route.params.id}`)
    if (res.data.code === '200') {
      post.value = res.data.data
    }
  } finally { loading.value = false }
})
</script>

<style scoped>
.post-detail-page { max-width: 900px; margin: 30px auto; padding: 0 20px; }
.post-container { background: white; padding: 50px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }

.post-header h1 { font-size: 32px; margin-bottom: 20px; color: #333; line-height: 1.3; }
.meta { color: #999; font-size: 14px; display: flex; align-items: center; gap: 20px; border-bottom: 1px solid #eee; padding-bottom: 20px; margin-bottom: 30px; }
.author { display: flex; align-items: center; gap: 8px; color: #555; font-weight: bold; }

.cover-img img { width: 100%; border-radius: 8px; margin-bottom: 40px; max-height: 500px; object-fit: cover; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }

/* 正文样式 */
.content-text {
  font-size: 18px;
  line-height: 1.8;
  color: #333;
  /* 关键：保留换行符，同时允许 HTML 渲染 */
  white-space: pre-wrap;
  word-wrap: break-word;
}

/* 针对 v-html 内部图片的样式 */
:deep(.content-text img) {
  max-width: 100%;
  border-radius: 8px;
  margin: 20px 0;
  box-shadow: 0 4px 12px rgba(0,0,0,0.05);
}
</style>