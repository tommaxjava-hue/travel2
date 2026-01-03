<template>
  <div class="post-detail-page" v-loading="loading">
    <el-button @click="$router.back()" style="margin-bottom: 20px">返回</el-button>

    <div v-if="post" class="post-container">
      <div class="post-header">
        <h1>{{ post.title }}</h1>
        <div class="meta">
          <span>👤 {{ post.authorName || '匿名用户' }}</span>
          <span>🕒 {{ formatTime(post.createTime) }}</span>
          <span>👁 {{ post.viewCount }} 浏览</span>
        </div>
      </div>

      <div v-if="post.coverImg" class="cover-img">
        <img :src="post.coverImg" alt="封面" />
      </div>

      <div class="post-body">
        <div class="content-text">{{ post.content }}</div>
      </div>
    </div>
    <el-empty v-else description="攻略不存在或已删除" />
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import { useRoute } from 'vue-router'
import axios from 'axios'

const route = useRoute()
const post = ref(null)
const loading = ref(false)

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
.post-detail-page { max-width: 800px; margin: 20px auto; padding: 20px; }
.post-container { background: white; padding: 40px; border-radius: 12px; box-shadow: 0 4px 20px rgba(0,0,0,0.05); }
.post-header h1 { font-size: 28px; margin-bottom: 15px; color: #333; }
.meta { color: #999; font-size: 14px; display: flex; gap: 20px; border-bottom: 1px solid #eee; padding-bottom: 20px; margin-bottom: 30px; }
.cover-img img { width: 100%; border-radius: 8px; margin-bottom: 30px; max-height: 500px; object-fit: cover; }
.content-text { font-size: 18px; line-height: 1.8; color: #444; white-space: pre-wrap; }
</style>