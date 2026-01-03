<template>
  <div class="profile-page">
    <div class="profile-header">
      <div class="user-info">
        <el-avatar :size="80" src="https://cube.elemecdn.com/0/88/03b0d39583f48206768a7534e55bcpng.png" />
        <div class="texts">
          <h2>{{ currentUser.username }}</h2>
          <p>
            <el-tag size="small" v-if="currentUser.role==='admin'" type="danger">管理员</el-tag>
            <el-tag size="small" v-else>普通用户</el-tag>
            <span style="margin-left: 10px; color: #999;">兴趣标签：{{ currentUser.tags || '无' }}</span>
          </p>
        </div>
      </div>
    </div>

    <div class="profile-body">
      <el-tabs type="border-card">
        <el-tab-pane label="📅 我的行程计划">
          <div v-if="itineraries.length > 0">
            <el-timeline>
              <el-timeline-item
                  v-for="item in itineraries"
                  :key="item.id"
                  :timestamp="formatTime(item.createTime)"
                  placement="top"
                  color="#409EFF"
              >
                <el-card class="itinerary-card">
                  <div class="i-header">
                    <h4>{{ item.title }}</h4>
                    <span class="i-date" v-if="item.startDate">{{ item.startDate }} 至 {{ item.endDate }}</span>
                  </div>
                  <div class="i-content">{{ item.note }}</div>
                </el-card>
              </el-timeline-item>
            </el-timeline>
          </div>
          <el-empty v-else description="还没有生成过行程，快去问问 AI 吧！" />
        </el-tab-pane>

        <el-tab-pane label="❤️ 我的收藏">
          <div class="fav-grid" v-if="favorites.length > 0">
            <div v-for="spot in favorites" :key="spot.spotId" class="fav-item" @click="$router.push(`/detail/${spot.spotId}`)">
              <img :src="spot.imageUrl || 'https://via.placeholder.com/200'" class="fav-img" />
              <div class="fav-info">
                <div class="fav-name">{{ spot.name }}</div>
                <div class="fav-city">{{ spot.city }}</div>
              </div>
            </div>
          </div>
          <el-empty v-else description="还没有收藏任何景点哦" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const itineraries = ref([])
const favorites = ref([])

const loadData = async () => {
  if (!currentUser.userId) return

  // 1. 加载行程
  const res1 = await axios.get(`http://localhost:8080/itinerary/list?userId=${currentUser.userId}`)
  itineraries.value = res1.data.data

  // 2. 加载收藏
  const res2 = await axios.get(`http://localhost:8080/favorite/list?userId=${currentUser.userId}`)
  favorites.value = res2.data.data
}

const formatTime = (t) => t ? t.replace('T', ' ').substring(0, 16) : ''

onMounted(loadData)
</script>

<style scoped>
.profile-page { max-width: 1000px; margin: 30px auto; padding: 0 20px; }

.profile-header {
  background: white; padding: 30px; border-radius: 8px; margin-bottom: 20px;
  box-shadow: 0 2px 10px rgba(0,0,0,0.05); display: flex; align-items: center;
}
.user-info { display: flex; gap: 20px; align-items: center; }
.texts h2 { margin: 0 0 10px 0; }

.itinerary-card { background: #fbfbfb; }
.i-header { display: flex; justify-content: space-between; margin-bottom: 10px; font-weight: bold; }
.i-content { white-space: pre-wrap; color: #555; font-size: 14px; max-height: 200px; overflow-y: auto; }

/* 收藏列表 Grid */
.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }
.fav-item { cursor: pointer; border-radius: 8px; overflow: hidden; border: 1px solid #eee; transition: 0.3s; }
.fav-item:hover { transform: translateY(-3px); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
.fav-img { width: 100%; height: 120px; object-fit: cover; }
.fav-info { padding: 10px; background: white; }
.fav-name { font-weight: bold; font-size: 14px; margin-bottom: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; }
.fav-city { color: #999; font-size: 12px; }
</style>