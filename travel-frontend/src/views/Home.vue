<template>
  <div class="home-page">
    <div class="hero-wrapper">
      <div class="hero-bg" style="background-image: url('[https://images.unsplash.com/photo-1506744038136-46273834b3fb?ixlib=rb-1.2.1&auto=format&fit=crop&w=2000&q=80](https://images.unsplash.com/photo-1506744038136-46273834b3fb?ixlib=rb-1.2.1&auto=format&fit=crop&w=2000&q=80)');"></div>
      <div class="hero-overlay">
        <h1 class="slogan">2026，探索未知的世界</h1>
        <div class="big-search-box">
          <input v-model="keyword" placeholder="搜目的地 / 攻略 / 酒店..." @keyup.enter="loadSpots">
          <button @click="loadSpots">搜索</button>
        </div>
        <div class="hot-tags">
          <span v-for="tag in ['古迹', '海边', '亲子', '美食', '自驾']" :key="tag" @click="clickTag(tag)">{{ tag }}</span>
        </div>
      </div>
    </div>

    <div class="content-wrapper">
      <div class="panel-header">
        <div class="ph-left">
          <h2>🔥 热门推荐</h2>
          <span class="ph-sub">根据您的兴趣标签推荐：{{ currentUser.tags || '暂无' }}</span>
        </div>
        <el-button link>查看更多 >></el-button>
      </div>

      <div class="spot-grid-layout" v-loading="loading">
        <div v-for="spot in spotList" :key="spot.spotId" class="spot-card" @click="$router.push(`/detail/${spot.spotId}`)">
          <div class="card-img">
            <img :src="spot.imageUrl || '[https://via.placeholder.com/300x200](https://via.placeholder.com/300x200)'" />
            <div class="price-badge">¥{{ spot.ticketPrice }}</div>
          </div>
          <div class="card-info">
            <div class="c-title">{{ spot.name }}</div>
            <div class="c-meta">
              <span class="c-city">📍 {{ spot.city }}</span>
              <span class="c-rating">★ {{ spot.rating }}</span>
            </div>
          </div>
        </div>
      </div>
    </div>

    <div class="ai-float" @click="aiVisible = !aiVisible">🤖</div>

    <div v-show="aiVisible" class="ai-window">
      <div class="chat-header">
        <span>🤖 AI 随身导游</span>
        <span class="close-btn" @click="aiVisible = false">×</span>
      </div>

      <div class="chat-body" ref="chatBox">
        <div v-for="(msg, index) in chatHistory" :key="index" :class="['msg-row', msg.role]">
          <div class="bubble-wrapper">
            <div class="bubble" v-html="msg.content.replace(/\n/g, '<br>')"></div>
            <div v-if="msg.role === 'ai'" class="actions">
              <el-icon class="save-icon" title="收藏回答" @click="saveAiNote(msg.content)"><Star /></el-icon>
            </div>
          </div>
        </div>
        <div v-if="aiLoading" class="msg-row ai"><div class="bubble">思考中...</div></div>
      </div>

      <div class="chat-footer">
        <el-input v-model="aiQuestion" placeholder="问我..." @keyup.enter="sendAi">
          <template #append><el-button @click="sendAi" :loading="aiLoading">发送</el-button></template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const spotList = ref([])
const loading = ref(false)
const keyword = ref('')

const aiVisible = ref(false)
const aiQuestion = ref('')
const aiLoading = ref(false)
const chatHistory = ref([{ role: 'ai', content: '你好！我是你的智能导游，想去哪里玩？' }])
const chatBox = ref(null)

const loadSpots = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/attraction/list', {
      params: { userId: currentUser.userId, keyword: keyword.value }
    })
    if(res.data.code === '200') spotList.value = res.data.data
  } finally { loading.value = false }
}
const clickTag = (tag) => { keyword.value = tag; loadSpots() }

const sendAi = async () => {
  if(!aiQuestion.value) return
  chatHistory.value.push({role:'user', content: aiQuestion.value})
  const q = aiQuestion.value; aiQuestion.value = ''
  aiLoading.value = true
  nextTick(() => { if(chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight })

  try {
    const res = await axios.post('http://localhost:8080/ai/ask', {question: q})
    chatHistory.value.push({role:'ai', content: res.data.data})
  } catch(e) { chatHistory.value.push({role:'ai', content: '网络开小差了...'}) }

  aiLoading.value = false
  nextTick(() => { if(chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight })
}

const saveAiNote = async (content) => {
  if (!currentUser.userId) return ElMessage.warning('请先登录')
  try {
    const res = await axios.post('http://localhost:8080/ai/saveNote', {
      userId: currentUser.userId,
      content: content
    })
    if (res.data.code === '200') ElMessage.success('已收藏到笔记')
    else ElMessage.error(res.data.msg)
  } catch(e) { ElMessage.error('收藏失败') }
}

onMounted(() => loadSpots())
</script>

<style scoped>
.hero-wrapper { position: relative; width: 100%; height: 450px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.hero-bg { position: absolute; width: 100%; height: 100%; background-size: cover; background-position: center; filter: brightness(0.7); transition: transform 3s; }
.hero-wrapper:hover .hero-bg { transform: scale(1.05); }
.hero-overlay { position: relative; z-index: 2; text-align: center; color: white; width: 100%; max-width: 800px; }
.slogan { font-size: 36px; margin-bottom: 30px; letter-spacing: 2px; text-shadow: 0 2px 10px rgba(0,0,0,0.5); }
.big-search-box { display: flex; background: rgba(255,255,255,0.95); border-radius: 8px; padding: 5px; box-shadow: 0 8px 20px rgba(0,0,0,0.2); }
.big-search-box input { flex: 1; border: none; font-size: 16px; padding: 12px 20px; outline: none; background: transparent; }
.big-search-box button { background: #ff9d00; color: white; border: none; border-radius: 6px; width: 100px; font-size: 18px; cursor: pointer; transition: 0.3s; }
.hot-tags { margin-top: 20px; display: flex; gap: 15px; justify-content: center; }
.hot-tags span { cursor: pointer; padding: 4px 12px; background: rgba(0,0,0,0.3); border-radius: 20px; font-size: 14px; transition: 0.3s; border: 1px solid rgba(255,255,255,0.2); }

.content-wrapper { max-width: 1200px; margin: 40px auto; padding: 0 20px; }
.panel-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 25px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.spot-grid-layout { display: grid; grid-template-columns: repeat(4, 1fr); gap: 24px; }
.spot-card { background: white; border-radius: 8px; overflow: hidden; box-shadow: 0 2px 10px rgba(0,0,0,0.05); cursor: pointer; transition: all 0.3s; }
.spot-card:hover { transform: translateY(-5px); box-shadow: 0 15px 30px rgba(0,0,0,0.1); }
.card-img { height: 160px; position: relative; }
.card-img img { width: 100%; height: 100%; object-fit: cover; }
.price-badge { position: absolute; bottom: 8px; left: 8px; background: rgba(0,0,0,0.7); color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.card-info { padding: 12px; }
.c-title { font-weight: bold; font-size: 16px; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.c-meta { display: flex; justify-content: space-between; color: #999; font-size: 13px; }

.ai-float { position: fixed; bottom: 30px; right: 30px; width: 60px; height: 60px; background: #409EFF; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 30px; box-shadow: 0 5px 20px rgba(64,158,255,0.4); cursor: pointer; z-index: 100; transition: transform 0.2s; }
.ai-float:hover { transform: scale(1.1); }
.ai-window { position: fixed; bottom: 100px; right: 30px; width: 350px; height: 500px; background: white; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); z-index: 101; border: 1px solid #eee; display: flex; flex-direction: column; }
.chat-header { padding: 15px; background: #409EFF; color: white; border-radius: 12px 12px 0 0; display: flex; justify-content: space-between; font-weight: bold; }
.close-btn { cursor: pointer; font-size: 20px; }
.chat-body { flex: 1; padding: 15px; overflow-y: auto; background: #f9f9f9; }
.msg-row { margin-bottom: 15px; display: flex; }
.msg-row.ai { justify-content: flex-start; }
.msg-row.user { justify-content: flex-end; }
.bubble-wrapper { display: flex; flex-direction: column; max-width: 80%; }
.bubble { padding: 10px 14px; border-radius: 10px; font-size: 14px; line-height: 1.5; word-wrap: break-word; }
.ai .bubble { background: white; border: 1px solid #eee; color: #333; }
.user .bubble { background: #409EFF; color: white; margin-left: auto; }
.actions { margin-top: 5px; text-align: right; }
.save-icon { cursor: pointer; color: #999; font-size: 16px; }
.save-icon:hover { color: #E6A23C; }
.chat-footer { padding: 10px; border-top: 1px solid #eee; background: white; border-radius: 0 0 12px 12px; }
</style>