<template>
  <div class="dashboard">
    <div class="card-row">
      <el-card class="data-card" shadow="hover">
        <template #header>👥 今日日活 (DAU)</template>
        <div class="num">{{ stats.dau }}</div>
        <div class="trend up">实时 Redis 数据 🟢</div>
      </el-card>

      <el-card class="data-card" shadow="hover">
        <template #header>💰 平台总销售额</template>
        <div class="num" style="color:#ff9d00">¥{{ stats.totalSales }}</div>
        <div class="trend">门票支付总计 📈</div>
      </el-card>

      <el-card class="data-card" shadow="hover">
        <template #header>📝 累计攻略数</template>
        <div class="num" style="color:#67c23a">{{ stats.postCount }}</div>
        <div class="trend">社区活跃内容</div>
      </el-card>

      <el-card class="data-card" shadow="hover">
        <template #header>👤 注册总用户</template>
        <div class="num" style="color:#909399">{{ stats.userCount }}</div>
        <div class="trend">平台用户底座</div>
      </el-card>
    </div>

    <div class="chart-row">
      <el-card class="chart-card" shadow="never">
        <div ref="lineChart" style="height: 380px;"></div>
      </el-card>
      <el-card class="chart-card" shadow="never">
        <div ref="pieChart" style="height: 380px;"></div>
      </el-card>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted, reactive } from 'vue'
import * as echarts from 'echarts'
import axios from 'axios'

const stats = reactive({
  dau: 0,
  totalSales: 0,
  postCount: 0,
  spotCount: 0,
  userCount: 0
})

const lineChart = ref(null)
const pieChart = ref(null)

const loadData = async () => {
  try {
    const res = await axios.get('http://localhost:8080/admin/stats')
    if (res.data.code === '200') {
      const data = res.data.data
      Object.assign(stats, data)
      renderCharts(data.trendDates, data.trendData, data.pieData)
    }
  } catch (e) {
    console.error('获取看板数据失败', e)
  }
}

const renderCharts = (trendDates, trendData, pieData) => {
  const myLine = echarts.init(lineChart.value)
  myLine.setOption({
    title: { text: '近七日用户活跃度趋势', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'axis' },
    grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
    xAxis: { type: 'category', boundaryGap: false, data: trendDates },
    yAxis: { type: 'value' },
    series: [{
      data: trendData,
      type: 'line',
      smooth: true,
      areaStyle: { color: 'rgba(64,158,255,0.2)' },
      itemStyle: { color: '#409EFF' }
    }]
  })

  const myPie = echarts.init(pieChart.value)
  myPie.setOption({
    title: { text: '热门景点搜索占比 (Top 5)', left: 'center', textStyle: { fontSize: 16 } },
    tooltip: { trigger: 'item', formatter: '{b} : {c} 热度 ({d}%)' },
    legend: { bottom: '0', left: 'center' },
    series: [{
      type: 'pie',
      // 🔥 修复点：缩小饼图半径，为外部文字留出充足空间
      radius: ['35%', '55%'],
      center: ['50%', '45%'],
      itemStyle: { borderRadius: 8, borderColor: '#fff', borderWidth: 2 },
      label: {
        show: true,
        // 🔥 修复点：使用换行符分隔名称与百分比，彻底解决横向挤压重叠问题
        formatter: '{b}\n{d}%',
        lineHeight: 18
      },
      labelLine: {
        length: 15,
        length2: 20 // 加长第二段引导线
      },
      data: pieData.length > 0 ? pieData : [{ name: '暂无数据', value: 0 }]
    }]
  })

  window.addEventListener('resize', () => {
    myLine.resize()
    myPie.resize()
  })
}

onMounted(() => {
  loadData()
})
</script>

<style scoped>
.dashboard { padding: 20px; background: #f0f2f5; min-height: calc(100vh - 60px); }
.card-row { display: flex; gap: 20px; margin-bottom: 25px; }
.data-card { flex: 1; text-align: center; border-radius: 10px; border: none; }
.num { font-size: 34px; font-weight: bold; color: #409EFF; margin: 15px 0; font-family: 'Helvetica Neue', Arial, sans-serif; }
.trend { font-size: 13px; color: #999; }
.trend.up { color: #67c23a; }
.chart-row { display: flex; gap: 20px; }
.chart-card { flex: 1; border-radius: 10px; border: none; }
</style>