<template>
  <div class="dashboard">
    <div class="card-row">
      <el-card class="data-card" shadow="hover">
        <template #header>👥 今日日活 (DAU)</template>
        <div class="num">1,208</div>
        <div class="trend up">较昨日 +15% 📈</div>
      </el-card>
      <el-card class="data-card" shadow="hover">
        <template #header>📝 累计攻略数</template>
        <div class="num">8,542</div>
        <div class="trend">本周新增 120 篇</div>
      </el-card>
      <el-card class="data-card" shadow="hover">
        <template #header>🤖 AI 调用次数</template>
        <div class="num">45,291</div>
        <div class="trend up">高频使用 🔥</div>
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
import { ref, onMounted } from 'vue'
import * as echarts from 'echarts'

const lineChart = ref(null)
const pieChart = ref(null)

onMounted(() => {
  // 1. 折线图
  const myLine = echarts.init(lineChart.value)
  myLine.setOption({
    title: { text: '近七日用户访问量趋势' },
    tooltip: { trigger: 'axis' },
    xAxis: { type: 'category', data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日'] },
    yAxis: { type: 'value' },
    series: [{ data: [820, 932, 901, 934, 1290, 1330, 1320], type: 'line', smooth: true, areaStyle: {} }]
  })

  // 2. 饼图
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
.card-row { display: flex; gap: 20px; margin-bottom: 20px; }
.data-card { flex: 1; text-align: center; }
.num { font-size: 32px; font-weight: bold; color: #409EFF; margin: 10px 0; }
.trend { font-size: 13px; color: #999; }
.trend.up { color: #67c23a; }
.chart-row { display: flex; gap: 20px; }
.chart-card { flex: 1; }
</style>