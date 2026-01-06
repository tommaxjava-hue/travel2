<template>
  <div class="pay-container">
    <el-card class="pay-card">
      <h2>收银台</h2>
      <div class="amount">¥{{ price }}</div>
      <p>订单号：{{ orderId }}</p>

      <div class="qr-box">
        <img src="https://api.qrserver.com/v1/create-qr-code/?size=150x150&data=TravelPay" />
        <p>请使用 微信/支付宝 扫码</p>
      </div>

      <el-button type="success" size="large" class="pay-btn" @click="finishPay">我已完成支付</el-button>
    </el-card>
  </div>
</template>

<script setup>
import { useRoute, useRouter } from 'vue-router'
import axios from 'axios'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderId = route.query.orderId
const price = route.query.price

const finishPay = async () => {
  await axios.post('http://localhost:8080/order/pay', { orderId: orderId })
  ElMessage.success('支付成功！祝您旅途愉快')
  router.push('/')
}
</script>

<style scoped>
.pay-container { height: 80vh; display: flex; align-items: center; justify-content: center; background: #f5f7fa; }
.pay-card { width: 400px; text-align: center; padding: 20px; }
.amount { font-size: 40px; font-weight: bold; color: #ff9d00; margin: 20px 0; }
.qr-box { margin: 30px 0; padding: 20px; background: #f9f9f9; border-radius: 8px; }
.pay-btn { width: 100%; }
</style>