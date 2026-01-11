<template>
  <div class="dashboard">
    <div class="card-row">
      <el-card class="data-card" shadow="hover">
        <template #header>👥 今日日活 (DAU)</template>
        <div class="num">{{ stats.dau }}</div>
        <div class="trend up">实时数据 🟢</div>
      </el-card>

      <el-card class="data-card" shadow="hover">
        <template #header>📝 累计攻略数</template>
        <div class="num">{{ stats.postCount }}</div>
        <div class="trend">社区活跃内容</div>
      </el-card>

      <el-card class="data-card" shadow="hover">
        <template #header>🏔️ 收录景点</template>
        <div class="num">{{ stats.spotCount }}</div>
        <div class="trend">平台核心资源</div>
      </el-card>

      <el-card class="data-card" shadow="hover">
        <template #header>👤 注册用户</template>
        <div class="num">{{ stats.userCount }}</div>
        <div class="trend">持续增长中 📈</div>
      </el-card>
    </div>

    <div class="chart-row">
      <el-card class="chart-card">
        <div ref="lineChart" style="height: 350px;"></div>
      </el-card>
      <el-card class="chart-card">
        <div ref="pieChart" style="height: 350px;"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios' // 🔥 引入 axios

// 🔥 定义响应式数据对象，初始值为 0
const stats = reactive({
  dau: 0,
  postCount: 0,
  spotCount: 0,
  userCount: 0
})

const lineChart = ref(null)
const pieChart = ref(null)

// 🔥 加载后台数据的方法
const loadData = async () => {
  try {
    const res = await axios.get('http://localhost:8080/admin/stats')
    if (res.data.code === '200') {
      // 将接口返回的数据覆盖到 stats 对象
      Object.assign(stats, res.data.data)
    }
  } catch (e) {
    console.error('获取看板数据失败', e)
  }
}

onMounted(async () => {
  // 1. 页面加载时先去后台查数据
  await loadData()

  // 2. 初始化图表
  const myLine = echarts.init(lineChart.value)
  myLine.setOption({
    title: { text: '近七日用户访问量趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [{ data: [820, 932, 901, 934, 1290, 1330, 1320], type: 'line', smooth: true, areaStyle: {} }]
  })

  const myPie = echarts.init(pieChart.value)
  myPie.setOption({
    title: { text: '热门搜索景点占比' },
    tooltip: { trigger: 'item' },
    series: [{
      type: 'pie', radius: ['40%', '70%'],
      data: [
        { value: 1048, name: '上海迪士尼' },
        { value: 735, name: '北京故宫' },
        { value: 580, name: '外滩夜景' },
        { value: 484, name: '长城' },
        { value: 300, name: '环球影城' }
      ]
    }]
  })

  window.onresize = () => { myLine.resize(); myPie.resize(); }
})
</script>

<style scoped>
.dashboard { padding: 20px; }
.card-row { display: flex; gap: 20px; margin-bottom: 20px; }
.data-card { flex: 1; text-align: center; }
.num { font-size: 32px; font-weight: bold; color: #409EFF; margin: 10px 0; }
.trend { font-size: 13px; color: #999; }
.trend.up { color: #67c23a; }
.chart-row { display: flex; gap: 20px; }
.chart-card { flex: 1; }
</style>