<template>
  <div class="plan-page">
    <div class="sidebar">
      <div class="sidebar-header">
        <h2>🗺️ 智能行程规划</h2>
        <p class="subtitle">选择城市与景点，AI 定制路线</p>
      </div>

      <div class="filter-box">
        <span class="label">当前城市：</span>
        <el-select
            v-model="selectedCity"
            placeholder="请选择"
            @change="loadSpots"
            style="width: 200px"
        >
          <el-option v-for="c in cityList" :key="c" :label="c" :value="c" />
        </el-select>
      </div>

      <div class="select-list" v-loading="loading">
        <el-checkbox-group v-model="selectedSpots">
          <div v-for="spot in spotList" :key="spot.spotId" class="check-item">
            <el-checkbox :value="spot" size="large" border>
              <div class="chk-content">
                <span class="chk-name">{{ spot.name }}</span>
                <span class="chk-tag">{{ spot.ticketPrice > 0 ? '¥'+spot.ticketPrice : '免费' }}</span>
              </div>
            </el-checkbox>
          </div>
        </el-checkbox-group>
        <el-empty v-if="spotList.length === 0" description="该城市暂无录入景点" />
      </div>

      <div class="action-bar">
        <div class="count">已选: {{ selectedSpots.length }} 个</div>
        <el-button type="primary" round :loading="planning" @click="generateRoute" :disabled="selectedSpots.length < 2">
          ✨ 生成路线
        </el-button>
      </div>

      <div class="result-box" v-if="routeResult.length > 0">
        <div class="result-header">
          <h3>🚀 推荐顺序：</h3>
          <el-button type="success" size="small" @click="savePlan" :loading="saving" round icon="Check">
            保存方案
          </el-button>
        </div>
        <el-steps direction="vertical" :active="routeResult.length">
          <el-step v-for="(name, index) in routeResult" :key="index" :title="name" />
        </el-steps>
      </div>
    </div>

    <div id="map" class="map-container"></div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import 'leaflet/dist/leaflet.css'
import L from 'leaflet'

const cityList = ref([])
const selectedCity = ref('')
const spotList = ref([])
const selectedSpots = ref([])
const loading = ref(false)
const planning = ref(false)
const saving = ref(false)
const routeResult = ref([])
let map = null
let markers = []
let routeLine = null

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')

const cityCoords = {
  '上海': [31.2304, 121.4737],
  '北京': [39.9042, 116.4074],
  '成都': [30.5728, 104.0668],
  '西安': [34.3416, 108.9398],
  '广州': [23.1291, 113.2644],
  '杭州': [30.2741, 120.1551]
}

const loadCities = async () => {
  try {
    const res = await axios.get('http://localhost:8080/attraction/cities')
    cityList.value = res.data.data
    if (cityList.value.length > 0) {
      selectedCity.value = cityList.value[0]
      loadSpots()
    }
  } catch(e) {}
}

const loadSpots = async () => {
  if(!selectedCity.value) return
  loading.value = true
  selectedSpots.value = []
  routeResult.value = []

  try {
    const res = await axios.get('http://localhost:8080/attraction/list', {
      // 在规划页面通常不分页，请求足够大的 pageSize 获取同城所有景点供勾选
      params: { city: selectedCity.value, pageNum: 1, pageSize: 500 }
    })
    if (res.data.code === '200') {
      // 修复点 2：适配最新的后端物理分页返回结构，解析 res.data.data.list
      spotList.value = res.data.data.list || []
    }
    const center = cityCoords[selectedCity.value] || [39.9042, 116.4074]
    if (map) map.setView(center, 11)
    else initMap(center)

  } finally {
    loading.value = false
  }
}

const initMap = (center) => {
  if (map) return
  map = L.map('map').setView(center, 11)
  L.tileLayer('https://{s}.tile.openstreetmap.org/{z}/{x}/{y}.png', {
    attribution: '&copy; OpenStreetMap'
  }).addTo(map)
}

const generateRoute = async () => {
  if (selectedSpots.value.length < 2) return
  planning.value = true
  try {
    const names = selectedSpots.value.map(s => s.name)
    const res = await axios.post('http://localhost:8080/ai/plan', { spots: names })
    if (res.data.code === '200') {
      const aiText = res.data.data
      const sortedNames = []
      const rawList = aiText.split(/[,，、\n]/)
      rawList.forEach(token => {
        const cleanName = token.trim()
        const spot = selectedSpots.value.find(s => cleanName.includes(s.name) || s.name.includes(cleanName))
        if (spot && !sortedNames.includes(spot)) sortedNames.push(spot)
      })
      const finalRoute = sortedNames.length > 1 ? sortedNames : selectedSpots.value

      routeResult.value = finalRoute.map(s => s.name)
      drawRouteOnMap(finalRoute)
      ElMessage.success('智能规划完成！')
    }
  } catch (e) {
    ElMessage.error('规划失败，请稍后重试')
  } finally {
    planning.value = false
  }
}

const savePlan = async () => {
  if (!currentUser.userId) {
    ElMessage.warning('请先登录后再保存行程')
    return
  }
  saving.value = true
  try {
    const contentText = routeResult.value.map((name, index) => `第${index + 1}站: ${name}`).join('\n')
    const res = await axios.post('http://localhost:8080/ai/saveItinerary', {
      userId: currentUser.userId,
      content: contentText
    })
    if (res.data.code === '200') {
      ElMessage.success('行程已成功保存至个人中心！')
    } else {
      ElMessage.error(res.data.msg || '保存失败')
    }
  } catch (e) {
    ElMessage.error('网络异常，保存失败')
  } finally {
    saving.value = false
  }
}

const drawRouteOnMap = (routeSpots) => {
  markers.forEach(m => map.removeLayer(m))
  if (routeLine) map.removeLayer(routeLine)
  markers = []

  const latlngs = []
  routeSpots.forEach((spot, index) => {
    if (spot.latitude && spot.longitude) {
      const marker = L.marker([spot.latitude, spot.longitude])
          .addTo(map)
          .bindPopup(`<b>${index + 1}. ${spot.name}</b>`)
          .openPopup()
      markers.push(marker)
      latlngs.push([spot.latitude, spot.longitude])
    }
  })

  if (latlngs.length > 0) {
    routeLine = L.polyline(latlngs, { color: 'red', weight: 4, dashArray: '10, 10' }).addTo(map)
    map.fitBounds(L.latLngBounds(latlngs))
  }
}

onMounted(() => {
  loadCities()
})
</script>

<style scoped>
.plan-page { display: flex; height: calc(100vh - 64px); }
.sidebar { width: 350px; background: white; border-right: 1px solid #ddd; display: flex; flex-direction: column; z-index: 2; box-shadow: 2px 0 10px rgba(0,0,0,0.1); }
.sidebar-header { padding: 20px; border-bottom: 1px solid #eee; background: #f9f9f9; }
.filter-box { padding: 15px 20px; border-bottom: 1px solid #eee; display: flex; align-items: center; gap: 10px; }
.select-list { flex: 1; overflow-y: auto; padding: 10px; }
.check-item { margin-bottom: 10px; }
.chk-content { display: flex; justify-content: space-between; width: 100%; }
.chk-tag { color: #ff9d00; font-weight: bold; }
.action-bar { padding: 15px; border-top: 1px solid #eee; background: white; display: flex; justify-content: space-between; align-items: center; }
.result-box { padding: 20px; background: #f0f9eb; border-top: 1px solid #e1f3d8; max-height: 350px; overflow-y: auto; }
.result-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 15px; }
.map-container { flex: 1; background: #eee; z-index: 1; }
</style>