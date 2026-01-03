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
              <span class="label">门票价格</span>
              <div class="price-val">
                <span v-if="spot.ticketPrice == 0" class="free">免费开放</span>
                <span v-else class="money">
                  <span class="symbol">¥</span>{{ spot.ticketPrice }}
                </span>
              </div>
            </div>

            <div class="desc-box">{{ spot.description }}</div>

            <div class="action-row">
              <el-button type="primary" size="large" icon="EditPen" @click="scrollToComment">写点评</el-button>
              <el-button size="large" icon="Share">分享</el-button>
            </div>
          </div>
        </div>

        <div class="ai-summary-card" v-if="aiSummary">
          <div class="ai-header">
            <span class="ai-icon">✨</span>
            <span class="ai-title">AI 智能口碑总结</span>
            <el-tag size="small" type="warning" effect="plain" style="margin-left: 10px">基于真实评论分析</el-tag>
          </div>
          <div class="ai-content">
            {{ aiSummary }}
          </div>
        </div>

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
import { useRoute } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { Star, StarFilled, EditPen, Share } from '@element-plus/icons-vue'

const route = useRoute()
const spotId = route.params.id
const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const loading = ref(false)
const spot = ref(null)
const comments = ref([])
const aiSummary = ref('') // AI 摘要
const isFav = ref(false)  // 收藏状态

const newComment = reactive({ score: 5, content: '' })

// 初始化加载
const init = async () => {
  loading.value = true
  try {
    // 1. 获取详情
    const res1 = await axios.get(`http://localhost:8080/attraction/detail/${spotId}`)
    spot.value = res1.data.data

    // 2. 获取评论
    loadComments()

    // 3. 检查是否收藏 (需登录)
    if (currentUser.userId) {
      const res3 = await axios.get(`http://localhost:8080/favorite/check`, {
        params: { userId: currentUser.userId, spotId: spotId }
      })
      isFav.value = res3.data.data
    }

    // 4. 获取 AI 摘要
    loadAiSummary()

  } catch (e) {
    console.error(e)
    ElMessage.error('加载详情失败')
  } finally {
    loading.value = false
  }
}

const loadComments = async () => {
  const res = await axios.get(`http://localhost:8080/comment/list?spotId=${spotId}`)
  comments.value = res.data.data
}

const loadAiSummary = async () => {
  // 异步加载摘要，不阻塞主界面
  try {
    const res = await axios.get(`http://localhost:8080/ai/summary?spotId=${spotId}`)
    if (res.data.code === '200') {
      aiSummary.value = res.data.data
    }
  } catch (e) {
    console.log('AI 摘要加载跳过')
  }
}

// 切换收藏
const toggleFav = async () => {
  if (!currentUser.userId) return ElMessage.warning('请先登录后收藏')

  try {
    const res = await axios.post(`http://localhost:8080/favorite/toggle`, {
      userId: currentUser.userId,
      spotId: spotId
    })

    if (res.data.code === '200') {
      isFav.value = !isFav.value
      ElMessage.success(isFav.value ? '已添加到收藏夹' : '已取消收藏')
    }
  } catch(e) {
    ElMessage.error('操作失败')
  }
}

// 提交评论 (修复版)
const submitComment = async () => {
  if (!newComment.content) return ElMessage.warning('评论内容不能为空')

  try {
    const res = await axios.post('http://localhost:8080/comment/add', {
      spotId,
      content: newComment.content,
      score: newComment.score,
      userId: currentUser.userId || 1
    })

    // 🔥 核心修复：必须判断 code 是否为 200
    if (res.data.code === '200') {
      ElMessage.success('评论发布成功！')
      newComment.content = ''
      loadComments() // 刷新列表
    } else {
      // 如果不是 200 (比如 403)，显示后端返回的具体错误信息 (如：包含违规内容)
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('评论失败，请检查网络')
  }
}

const scrollToComment = () => {
  document.getElementById('comment-area').scrollIntoView({ behavior: 'smooth' })
}
const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

onMounted(init)
</script>

<style scoped>
.detail-page { background: #f5f7fa; min-height: 100vh; padding: 20px 0; }
.detail-container { max-width: 1100px; margin: 0 auto; padding: 0 20px; }
.breadcrumb-bar { margin-bottom: 20px; font-size: 14px; }

.content-wrapper { background: white; border-radius: 16px; padding: 40px; box-shadow: 0 8px 30px rgba(0,0,0,0.04); }

/* 顶部区域 */
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

.price-card { background: #f8fbfd; border: 1px solid #eef6fc; padding: 15px 20px; border-radius: 10px; margin-bottom: 25px; display: flex; align-items: center; }
.price-card .label { color: #666; margin-right: 15px; }
.price-val .money { color: #ff5e00; font-size: 36px; font-weight: bold; }
.price-val .symbol { font-size: 20px; margin-right: 2px; }
.price-val .free { color: #67c23a; font-size: 24px; font-weight: bold; }

.desc-box { color: #555; line-height: 1.6; margin-bottom: 30px; flex: 1; font-size: 15px; }
.action-row { display: flex; gap: 15px; }

/* AI 摘要卡片 */
.ai-summary-card {
  background: linear-gradient(135deg, #e3f2fd 0%, #bbdefb 100%);
  border-radius: 12px; padding: 20px 25px; margin-bottom: 40px;
  position: relative; overflow: hidden;
  box-shadow: 0 4px 15px rgba(33, 150, 243, 0.15);
}
.ai-summary-card::before {
  content: '"'; position: absolute; right: 20px; top: 0px; font-size: 100px; color: rgba(255,255,255,0.4); font-family: serif; pointer-events: none;
}
.ai-header { display: flex; align-items: center; margin-bottom: 12px; }
.ai-icon { font-size: 22px; margin-right: 8px; animation: pulse 2s infinite; }
.ai-title { font-weight: 800; color: #1565c0; font-size: 16px; letter-spacing: 0.5px; }
.ai-content { color: #0d47a1; font-size: 15px; line-height: 1.7; text-align: justify; font-weight: 500; }

@keyframes pulse { 0% {transform: scale(1);} 50% {transform: scale(1.1);} 100% {transform: scale(1);} }

/* 介绍与评论 */
.section-block { margin-top: 40px; padding-top: 30px; border-top: 1px dashed #eee; }
.section-block h3 { border-left: 5px solid #409EFF; padding-left: 12px; font-size: 20px; margin-bottom: 20px; }
/* 修改部分：让文本自适应高度 */
.long-text {
  line-height: 1.8;
  color: #444;
  font-size: 16px;
  /* 允许换行，不设固定高度 */
  white-space: pre-wrap;
  min-height: 100px;
  height: auto;
}

/* 评论区样式 */
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