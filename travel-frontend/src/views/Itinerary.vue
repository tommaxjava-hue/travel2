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
                <el-card class="itinerary-card" shadow="hover">
                  <div class="i-header">
                    <div class="i-title-box">
                      <h4>{{ item.title }}</h4>
                      <span class="i-date" v-if="item.startDate">{{ item.startDate }} 至 {{ item.endDate }}</span>
                    </div>
                    <el-popconfirm
                        title="确定要删除这个行程计划吗？"
                        confirm-button-text="删除"
                        cancel-button-text="取消"
                        confirm-button-type="danger"
                        @confirm="handleDeleteItinerary(item.id)"
                    >
                      <template #reference>
                        <el-button type="danger" size="small" icon="Delete" circle plain title="删除行程"></el-button>
                      </template>
                    </el-popconfirm>
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

        <el-tab-pane label="🎫 我的订单">
          <div v-if="orders.length > 0">
            <el-table :data="orders" style="width: 100%" stripe border>
              <el-table-column prop="orderId" label="订单号" width="100" align="center" />
              <el-table-column prop="spotName" label="景点名称" min-width="150" />
              <el-table-column prop="price" label="实付款" width="120" align="center">
                <template #default="scope">
                  <strong style="color:#ff9d00">¥{{ scope.row.price }}</strong>
                </template>
              </el-table-column>
              <el-table-column prop="status" label="状态" width="100" align="center">
                <template #default="scope">
                  <el-tag :type="scope.row.status === 'PAID' ? 'success' : 'warning'" effect="dark">
                    {{ scope.row.status === 'PAID' ? '已支付' : '待支付' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column label="操作" width="120" align="center">
                <template #default="scope">
                  <el-button
                      v-if="scope.row.status === 'UNPAID'"
                      type="primary"
                      size="small"
                      @click="$router.push(`/payment?orderId=${scope.row.orderId}&price=${scope.row.price}`)">
                    继续支付
                  </el-button>
                </template>
              </el-table-column>
            </el-table>
          </div>
          <el-empty v-else description="您还没有任何购票订单" />
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const currentUser = JSON.parse(localStorage.getItem('user') || '{}')
const itineraries = ref([])
const favorites = ref([])
const orders = ref([]) // 新增：保存订单列表状态

const loadData = async () => {
  if (!currentUser.userId) return

  try {
    const res1 = await axios.get(`http://localhost:8080/itinerary/list?userId=${currentUser.userId}`)
    if (res1.data.code === '200') itineraries.value = res1.data.data

    const res2 = await axios.get(`http://localhost:8080/favorite/list?userId=${currentUser.userId}`)
    if (res2.data.code === '200') favorites.value = res2.data.data

    // 【核心补全】：发起接口请求获取订单数据
    const res3 = await axios.get(`http://localhost:8080/order/myList`, {
      headers: { 'Authorization': `Bearer ${localStorage.getItem('token')}` }
    })
    if (res3.data.code === '200') orders.value = res3.data.data

  } catch (e) {
    console.error("获取个人资产数据失败", e)
  }
}

const handleDeleteItinerary = async (id) => {
  try {
    const res = await axios.delete(`http://localhost:8080/itinerary/delete/${id}`)
    if (res.data.code === '200') {
      ElMessage.success('行程记录已删除')
      loadData()
    } else {
      ElMessage.error(res.data.msg || '删除失败')
    }
  } catch (e) {
    ElMessage.error('网络异常，删除操作失败')
  }
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
.texts h2 { margin: 0 0 10px 0; color: #333; }

.itinerary-card { background: #fbfbfb; transition: all 0.3s ease; border: 1px solid #eef0f4; }
.i-header { display: flex; justify-content: space-between; align-items: center; margin-bottom: 12px; font-weight: bold; border-bottom: 1px dashed #e4e7ed; padding-bottom: 10px;}
.i-title-box h4 { margin: 0 0 5px 0; color: #303133; font-size: 16px; }
.i-date { color: #909399; font-size: 13px; font-weight: normal; }
.i-content { white-space: pre-wrap; color: #606266; font-size: 14px; line-height: 1.6; max-height: 200px; overflow-y: auto; padding: 5px; }

.fav-grid { display: grid; grid-template-columns: repeat(auto-fill, minmax(180px, 1fr)); gap: 15px; }
.fav-item { cursor: pointer; border-radius: 8px; overflow: hidden; border: 1px solid #eee; transition: 0.3s; }
.fav-item:hover { transform: translateY(-3px); box-shadow: 0 5px 15px rgba(0,0,0,0.1); }
.fav-img { width: 100%; height: 120px; object-fit: cover; }
.fav-info { padding: 10px; background: white; }
.fav-name { font-weight: bold; font-size: 14px; margin-bottom: 5px; white-space: nowrap; overflow: hidden; text-overflow: ellipsis; color: #303133; }
.fav-city { color: #909399; font-size: 12px; }
</style>