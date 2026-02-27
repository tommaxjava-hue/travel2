<template>
  <div class="detail-page">
    <div class="detail-container" v-loading="loading">

      <div class="breadcrumb-bar">
        <el-breadcrumb separator="/">
          <el-breadcrumb-item :to="{ path: '/' }">首页</el-breadcrumb-item>
          <el-breadcrumb-item>景点详情</el-breadcrumb-item>
          <el-breadcrumb-item>{{ spot?.name }}</el-breadcrumb-item>
        </el-breadcrumb>
      </div>

      <div v-if="spot" class="content-wrapper">
        <div class="top-section">
          <div class="img-box">
            <img :src="spot.imageUrl || 'https://images.unsplash.com/photo-1507525428034-b723cf961d3e?ixlib=rb-1.2.1&auto=format&fit=crop&w=1000&q=80'" />
          </div>

          <div class="info-box">
            <div class="title-row">
              <h1 class="title">{{ spot.name }}</h1>
              <el-button
                  class="fav-btn"
                  :type="isFav ? 'danger' : 'default'"
                  :icon="isFav ? 'StarFilled' : 'Star'"
                  circle
                  size="large"
                  @click="toggleFav"
                  :title="isFav ? '取消收藏' : '收藏景点'"
              />
            </div>

            <div class="meta-row">
              <el-tag type="success" effect="dark">{{ spot.city }}</el-tag>
              <div class="rate-box">
                <el-rate v-model="spot.rating" disabled text-color="#ff9900" show-score />
              </div>
              <span class="views">🔥 热度 {{ spot.viewCount || 8848 }}</span>
            </div>

            <div class="price-card">
              <div class="price-left">
                <span class="label">门票价格</span>
                <div class="price-val">
                  <span v-if="spot.ticketPrice == 0" class="free">免费开放</span>
                  <span v-else class="money">
                    <span class="symbol">¥</span>{{ spot.ticketPrice }}
                  </span>
                </div>
              </div>
              <el-button
                  v-if="spot.ticketPrice > 0"
                  type="warning"
                  size="large"
                  class="book-btn"
                  @click="bookTicket"
              >
                立即预订
              </el-button>
            </div>

            <div class="desc-box">{{ spot.description }}</div>

            <div class="action-row">
              <el-button type="primary" size="large" icon="EditPen" @click="scrollToComment">写点评</el-button>
              <el-button size="large" icon="Share">分享</el-button>
            </div>
          </div>
        </div>

        <el-card class="ai-summary-card" shadow="hover" v-loading="aiLoading" element-loading-text="AI大模型正在深度分析全网评价...">
          <template #header>
            <div class="ai-header">
              <div class="title-with-icon">
                <el-icon class="ai-icon"><MagicStick /></el-icon>
                <span class="gradient-text">AI 智能口碑总结</span>
              </div>
              <el-tag type="primary" effect="light" round size="small">大模型驱动</el-tag>
            </div>
          </template>
          <div class="ai-content">
            <div v-if="aiSummary" class="ai-text" v-html="aiSummary.replace(/\n/g, '<br>')"></div>
            <el-empty v-else-if="!aiLoading" description="暂无足够数据供AI分析" :image-size="60" />
          </div>
        </el-card>
        <div class="section-block">
          <h3>📖 景点介绍</h3>
          <p class="long-text">{{ spot.contentText || spot.description }}</p>
        </div>

        <div class="section-block" id="comment-area">
          <div class="section-header">
            <h3>💬 游客点评 ({{ comments.length }})</h3>
          </div>

          <div class="post-comment">
            <el-input
                v-model="newComment.content"
                type="textarea"
                :rows="3"
                placeholder="这个景点怎么样？写下你的真实体验，帮助更多人..."
            />
            <div class="post-actions">
              <span class="rate-label">打个分：</span>
              <el-rate v-model="newComment.score" />
              <el-button type="primary" @click="submitComment" style="margin-left: auto;">发布评论</el-button>
            </div>
          </div>

          <div class="comment-list">
            <div v-for="item in comments" :key="item.commentId" class="comment-item">
              <el-avatar :size="48" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
              <div class="c-content">
                <div class="c-user">
                  <span class="name">{{ item.username || '匿名游客' }}</span>
                  <span class="time">{{ formatTime(item.createTime) }}</span>
                </div>
                <div class="c-rate">
                  <el-rate v-model="item.score" disabled size="small" />
                </div>
                <p class="c-text">{{ item.content }}</p>
              </div>
            </div>
            <el-empty v-if="comments.length === 0" description="还没有人评论，快来抢沙发！" />
          </div>
        </div>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 企业级改造：替换原生 axios，使用全局 request 进行 Token 自动拦截
import request from '../utils/request'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, EditPen, Share, MagicStick } from '@element-plus/icons-vue'

const route = useRoute()
const router = useRouter()
const spotId = route.params.id || route.query.id
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const spot = ref(null)
const comments = ref([])
const aiSummary = ref('') // AI 摘要
const aiLoading = ref(false) // 专门控制 AI 卡片的加载动画
const isFav = ref(false)  // 收藏状态

const newComment = reactive({ score: 5, content: '' })

// 初始化加载
const init = async () => {
  loading.value = true
  try {
    // 1. 获取详情
    const res1 = await request.get(`/attraction/detail/${spotId}`)
    if (res1.code === '200') {
      spot.value = res1.data
    }

    // 2. 获取评论
    loadComments()

    // 3. 检查是否收藏 (需登录)
    if (currentUser.userId) {
      const res3 = await request.get(`/favorite/check`, {
        params: { userId: currentUser.userId, spotId: spotId }
      })
      if (res3.code === '200') {
        isFav.value = res3.data
      }
    }

    // 4. 获取 AI 摘要
    loadAiSummary()

  } catch (e) {
    console.error('初始化失败:', e)
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  try {
    const res = await request.get(`/comment/list?spotId=${spotId}`)
    if (res.code === '200') {
      comments.value = res.data || []
    }
  } catch (e) {
    console.error('评论加载失败:', e)
  }
}

const loadAiSummary = async () => {
  aiLoading.value = true
  try {
    // 适配后端最新的 RESTful 接口路径
    const res = await request.get(`/ai/summary/${spotId}`)
    if (res.code === '200') {
      aiSummary.value = res.data
    }
  } catch (e) {
    console.log('AI 摘要拉取异常，跳过渲染')
  } finally {
    aiLoading.value = false
  }
}

// 切换收藏
const toggleFav = async () => {
  if (!currentUser.userId) return ElMessage.warning('请先登录后收藏')

  try {
    const res = await request.post(`/favorite/toggle`, {
      userId: currentUser.userId,
      spotId: spotId
    })

    if (res.code === '200') {
      isFav.value = !isFav.value
      ElMessage.success(isFav.value ? '已添加到收藏夹' : '已取消收藏')
    }
  } catch(e) {
    console.error('收藏失败:', e)
  }
}

// 预订功能
const bookTicket = async () => {
  if (!currentUser.userId) {
    ElMessage.warning('请先登录')
    return router.push('/login')
  }

  try {
    const res = await request.post('/order/create', {
      spotId: spot.value.spotId,
      spotName: spot.value.name,
      price: spot.value.ticketPrice
    })

    if (res.code === '200') {
      // 跳转支付页
      router.push(`/payment?orderId=${res.data}&price=${spot.value.ticketPrice}`)
    }
  } catch(e) {
    console.error('预订失败:', e)
  }
}

// 提交评论
const submitComment = async () => {
  if (!newComment.content) return ElMessage.warning('评论内容不能为空')

  try {
    const res = await request.post('/comment/add', {
      spotId,
      content: newComment.content,
      score: newComment.score,
      userId: currentUser.userId || 1
    })

    if (res.code === '200') {
      ElMessage.success('评论发布成功！')
      newComment.content = ''
      loadComments() // 发布后立刻刷新评论区
    }
  } catch (e) {
    console.error('发布评论失败:', e)
  }
}

const scrollToComment = () => {
  document.getElementById('comment-area').scrollIntoView({ behavior: 'smooth' })
}
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

onMounted(() => {
  init()
})
</script>

<style scoped>
.detail-page { background: #f5f7fa; min-height: 100vh; padding: 20px 0; }
.detail-container { max-width: 1100px; margin: 0 auto; padding: 0 20px; }
.breadcrumb-bar { margin-bottom: 20px; font-size: 14px; }

.content-wrapper { background: white; border-radius: 16px; padding: 40px; box-shadow: 0 8px 30px rgba(0,0,0,0.04); }

/* 顶部区域 (保留原样) */
.top-section { display: flex; gap: 40px; margin-bottom: 40px; }
.img-box { flex: 1; height: 360px; border-radius: 12px; overflow: hidden; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.img-box img { width: 100%; height: 100%; object-fit: cover; transition: transform 0.5s; }
.img-box:hover img { transform: scale(1.05); }

.info-box { flex: 1; display: flex; flex-direction: column; }
.title-row { display: flex; justify-content: space-between; align-items: flex-start; margin-bottom: 15px; }
.title { font-size: 32px; margin: 0; color: #333; line-height: 1.2; }
.fav-btn { font-size: 22px; margin-left: 20px; flex-shrink: 0; }

.meta-row { display: flex; align-items: center; gap: 15px; margin-bottom: 25px; }
.views { color: #999; font-size: 13px; }

/* 价格卡片样式优化 (保留原样) */
.price-card { background: #f8fbfd; border: 1px solid #eef6fc; padding: 15px 20px; border-radius: 10px; margin-bottom: 25px; display: flex; align-items: center; justify-content: space-between; }
.price-left { display: flex; align-items: center; }
.price-card .label { color: #666; margin-right: 15px; }
.price-val .money { color: #ff5e00; font-size: 36px; font-weight: bold; }
.price-val .symbol { font-size: 20px; margin-right: 2px; }
.price-val .free { color: #67c23a; font-size: 24px; font-weight: bold; }
.book-btn { font-weight: bold; padding: 0 30px; font-size: 16px; box-shadow: 0 4px 10px rgba(255,153,0,0.3); }

.desc-box { color: #555; line-height: 1.6; margin-bottom: 30px; flex: 1; font-size: 15px; }
.action-row { display: flex; gap: 15px; }

/* ================= AI 卡片专属炫酷样式 ================= */
.ai-summary-card {
  margin-bottom: 25px;
  border-radius: 12px;
  border: 1px solid transparent;
  background-image: linear-gradient(#fff, #fff), linear-gradient(135deg, #a8edea 0%, #fed6e3 100%);
  background-origin: border-box;
  background-clip: padding-box, border-box;
  box-shadow: 0 10px 30px rgba(0,0,0,0.05);
}
.ai-header {
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.title-with-icon {
  display: flex;
  align-items: center;
  font-size: 18px;
  font-weight: bold;
}
.ai-icon {
  font-size: 22px;
  color: #e83e8c;
  margin-right: 8px;
}
.gradient-text {
  background: linear-gradient(90deg, #4facfe 0%, #00f2fe 100%);
  -webkit-background-clip: text;
  -webkit-text-fill-color: transparent;
}
.ai-content {
  padding: 10px 5px;
}
.ai-text {
  font-size: 15px;
  line-height: 1.8;
  color: #333;
  letter-spacing: 0.5px;
}
/* ====================================================== */

/* 介绍与评论 (保留原样) */
.section-block { margin-top: 40px; padding-top: 30px; border-top: 1px dashed #eee; }
.section-block h3 { border-left: 5px solid #409EFF; padding-left: 12px; font-size: 20px; margin-bottom: 20px; }
.long-text {
  line-height: 1.8;
  color: #444;
  font-size: 16px;
  white-space: pre-wrap;
  min-height: 100px;
  height: auto;
}

/* 评论区样式 (保留原样) */
.post-comment { background: #f9fafc; padding: 20px; border-radius: 10px; margin-bottom: 30px; border: 1px solid #f0f0f0; }
.post-actions { display: flex; align-items: center; margin-top: 15px; }
.rate-label { margin-right: 10px; color: #666; }

.comment-item { display: flex; gap: 20px; border-bottom: 1px solid #f0f0f0; padding: 25px 0; }
.c-content { flex: 1; }
.c-user { display: flex; justify-content: space-between; margin-bottom: 8px; }
.c-user .name { font-weight: bold; font-size: 15px; color: #333; }
.c-user .time { color: #bbb; font-size: 12px; }
.c-rate { margin-bottom: 10px; }
.c-text { color: #555; line-height: 1.6; font-size: 15px; margin: 0; }
</style>