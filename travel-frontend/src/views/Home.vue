<template>
  <div class="home-page">
    <div class="hero-wrapper">
      <div class="hero-bg" style="background-image: url('https://images.unsplash.com/photo-1469854523086-cc02fe5d8800?ixlib=rb-1.2.1&auto=format&fit=crop&w=1920&q=80');"></div>

      <div class="hero-overlay">
        <h1 class="slogan">AI 智启旅程，发现世界之美</h1>

        <div class="big-search-box">
          <input v-model="keyword" placeholder="搜目的地 / 攻略..." @keyup.enter="loadSpots">
          <button @click="loadSpots">搜索</button>
        </div>
      </div>
    </div>

    <div class="content-wrapper" id="spot-list-anchor">
      <div class="panel-header">
        <h2>🔥 热门推荐 <span class="ph-sub" v-if="currentUser.tags"> (专属推荐: {{currentUser.tags}})</span></h2>
      </div>

      <div class="spot-grid-layout" v-loading="loading">
        <div v-for="spot in paginatedSpots" :key="spot.spotId" class="spot-card" @click="$router.push(`/detail/${spot.spotId}`)">
          <div class="card-img">
            <img
                :src="spot.imageUrl"
                loading="lazy"
                @error="handleImgError"
            />
            <div class="rating-badge">★ {{ spot.rating }}</div>
          </div>
          <div class="card-info">
            <div class="c-title">{{ spot.name }}</div>
            <div class="c-meta">
              <span>📍 {{ spot.city }}</span>
              <span class="price">¥{{ spot.ticketPrice }}</span>
            </div>
          </div>
        </div>
      </div>

      <el-empty v-if="spotList.length === 0 && !loading" description="暂无相关景点，试试搜索其他城市" />

      <div class="pagination-box" v-if="spotList.length > 0">
        <el-pagination
            v-model:current-page="currentPage"
            v-model:page-size="pageSize"
            :total="spotList.length"
            :page-sizes="[12, 24, 36]"
            layout="total, sizes, prev, pager, next, jumper"
            background
            @current-change="handlePageChange"
            @size-change="handleSizeChange"
        />
      </div>
    </div>

    <div class="ai-float" @click="aiVisible = !aiVisible">🤖</div>

    <div v-show="aiVisible" class="ai-window">
      <div class="chat-header">
        <span>🤖 AI 导游 (流式版)</span>
        <span class="close-btn" @click="aiVisible = false">×</span>
      </div>

      <div class="chat-body" ref="chatBox">
        <div v-for="(msg, index) in chatHistory" :key="index" :class="['msg-row', msg.role]">
          <div class="bubble-wrapper">
            <div class="bubble" v-html="msg.content.replace(/\n/g, '<br>')"></div>
            <div v-if="msg.role === 'ai'" class="actions">
              <el-icon class="save-icon" title="保存到我的行程" @click="saveAiNote(msg.content)"><Star /></el-icon>
            </div>
          </div>
        </div>
        <div v-if="aiLoading" class="msg-row ai"><div class="bubble">思考中...</div></div>
      </div>

      <div class="chat-footer">
        <el-input v-model="aiQuestion" placeholder="问我..." @keyup.enter="sendAiStream">
          <template #append><el-button @click="sendAiStream" :loading="aiLoading">发送</el-button></template>
        </el-input>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, nextTick, watch, computed } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { Star } from '@element-plus/icons-vue'

const router = useRouter()
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const spotList = ref([])
const loading = ref(false)
const keyword = ref('')

// 分页变量
const currentPage = ref(1)
const pageSize = ref(12)

const aiVisible = ref(false)
const aiQuestion = ref('')
const aiLoading = ref(false)
const chatHistory = ref([{ role: 'ai', content: '你好！我是你的智能导游，想去哪里玩？' }])
const chatBox = ref(null)

const FALLBACK_IMG = 'https://images.unsplash.com/photo-1596394516093-501ba68a0ba6?ixlib=rb-1.2.1&auto=format&fit=crop&w=800&q=80'

const paginatedSpots = computed(() => {
  const start = (currentPage.value - 1) * pageSize.value
  const end = start + pageSize.value
  return spotList.value.slice(start, end)
})

const handleImgError = (e) => {
  e.target.src = FALLBACK_IMG
}

watch(keyword, (newVal) => {
  if (!newVal) loadSpots()
})

const loadSpots = async () => {
  loading.value = true
  try {
    const res = await axios.get('http://localhost:8080/attraction/list', {
      params: { userId: currentUser.userId, keyword: keyword.value }
    })
    if(res.data.code === '200') {
      spotList.value = res.data.data
      currentPage.value = 1
    }
  } finally { loading.value = false }
}

const handlePageChange = (val) => {
  currentPage.value = val
  document.getElementById('spot-list-anchor').scrollIntoView({ behavior: 'smooth' })
}

const handleSizeChange = (val) => {
  pageSize.value = val
  currentPage.value = 1
}

const sendAiStream = () => {
  if (!aiQuestion.value) return
  const q = aiQuestion.value

  chatHistory.value.push({ role: 'user', content: q })
  aiQuestion.value = ''
  aiLoading.value = true

  nextTick(() => { if(chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight })

  const eventSource = new EventSource(`http://localhost:8080/ai/stream/ask?question=${encodeURIComponent(q)}&userId=${currentUser.userId || ''}`)

  let currentAiMsg = null

  eventSource.onopen = () => {
    aiLoading.value = false
    chatHistory.value.push({ role: 'ai', content: '' })
    currentAiMsg = chatHistory.value[chatHistory.value.length - 1]
  }

  eventSource.onmessage = (e) => {
    if (currentAiMsg) {
      currentAiMsg.content += e.data
      nextTick(() => { if(chatBox.value) chatBox.value.scrollTop = chatBox.value.scrollHeight })
    }
  }

  eventSource.onerror = () => {
    eventSource.close()
    aiLoading.value = false
    if (currentAiMsg && !currentAiMsg.content) {
      currentAiMsg.content = "连接中断，请检查后端服务是否启动。"
    }
  }
}

const saveAiNote = async (content) => {
  if (!currentUser.userId) return ElMessage.warning('请先登录')
  try {
    const res = await axios.post('http://localhost:8080/ai/saveItinerary', {
      userId: currentUser.userId,
      content: content
    })
    if (res.data.code === '200') ElMessage.success('已保存到【我的行程计划】')
    else ElMessage.error(res.data.msg)
  } catch(e) { ElMessage.error('保存失败') }
}

onMounted(() => loadSpots())
</script>

<style scoped>
.home-page { min-height: 100vh; background-color: #f5f7fa; }
.hero-wrapper { position: relative; height: 450px; display: flex; align-items: center; justify-content: center; overflow: hidden; }
.hero-bg { position: absolute; width: 100%; height: 100%; background-size: cover; background-position: center; filter: brightness(0.7); transition: transform 3s; }
.hero-wrapper:hover .hero-bg { transform: scale(1.05); }
.hero-overlay { position: relative; z-index: 2; text-align: center; color: white; width: 100%; max-width: 800px; }
.slogan { font-size: 42px; margin-bottom: 30px; letter-spacing: 2px; text-shadow: 0 4px 10px rgba(0,0,0,0.5); font-weight: 800; }

/* nav-bar 样式已删除，现在由 App.vue 接管 */

.big-search-box { display: flex; background: rgba(255,255,255,0.25); backdrop-filter: blur(10px); padding: 5px; border-radius: 50px; border: 1px solid rgba(255,255,255,0.3); box-shadow: 0 8px 32px rgba(0,0,0,0.1); }
.big-search-box input { flex: 1; border: none; font-size: 16px; padding: 12px 25px; outline: none; background: transparent; color: white; }
.big-search-box input::placeholder { color: rgba(255,255,255,0.9); }
.big-search-box button { background: #ff9d00; color: white; border: none; border-radius: 30px; padding: 10px 35px; font-size: 16px; cursor: pointer; transition: 0.3s; font-weight: bold; }
.big-search-box button:hover { background: #ffaa00; transform: scale(1.05); }

.content-wrapper { max-width: 1200px; margin: 40px auto; padding: 0 20px; }
.panel-header { display: flex; justify-content: space-between; align-items: flex-end; margin-bottom: 25px; border-bottom: 1px solid #eee; padding-bottom: 10px; }
.panel-header h2 { font-size: 24px; color: #333; margin: 0; }
.ph-sub { font-size: 14px; color: #666; font-weight: normal; margin-left: 10px; }

.spot-grid-layout { display: grid; grid-template-columns: repeat(4, 1fr); gap: 25px; min-height: 400px; }
.spot-card { background: white; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 15px rgba(0,0,0,0.05); transition: 0.3s; cursor: pointer; border: 1px solid #f0f0f0; }
.spot-card:hover { transform: translateY(-8px); box-shadow: 0 12px 30px rgba(0,0,0,0.1); border-color: #ff9d00; }
.card-img { height: 180px; position: relative; }
.card-img img { width: 100%; height: 100%; object-fit: cover; }
.price-badge { position: absolute; bottom: 8px; left: 8px; background: rgba(0,0,0,0.7); color: #fff; padding: 2px 8px; border-radius: 4px; font-size: 12px; }
.rating-badge { position: absolute; top: 10px; right: 10px; background: rgba(255,157,0,0.9); color: white; padding: 4px 8px; border-radius: 6px; font-size: 12px; font-weight: bold; }

.card-info { padding: 15px; }
.c-title { font-weight: bold; font-size: 17px; margin-bottom: 8px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #333; }
.c-meta { display: flex; justify-content: space-between; align-items: center; color: #666; font-size: 13px; }
.price { color: #ff9d00; font-weight: bold; font-size: 16px; }

.pagination-box { margin-top: 40px; display: flex; justify-content: center; }

.ai-float { position: fixed; bottom: 30px; right: 30px; width: 60px; height: 60px; background: #409EFF; border-radius: 50%; display: flex; align-items: center; justify-content: center; color: white; font-size: 30px; box-shadow: 0 5px 20px rgba(64,158,255,0.4); cursor: pointer; z-index: 100; transition: transform 0.2s; }
.ai-float:hover { transform: scale(1.1); }
.ai-window { position: fixed; bottom: 100px; right: 30px; width: 350px; height: 500px; background: white; border-radius: 12px; box-shadow: 0 10px 40px rgba(0,0,0,0.15); z-index: 101; border: 1px solid #eee; display: flex; flex-direction: column; overflow: hidden; }
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
.save-icon { cursor: pointer; color: #999; font-size: 16px; transition: 0.3s; }
.save-icon:hover { color: #E6A23C; transform: scale(1.2); }
.chat-footer { padding: 10px; border-top: 1px solid #eee; background: white; border-radius: 0 0 12px 12px; }
</style>