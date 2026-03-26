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

      <el-button type="success" size="large" class="pay-btn" @click="finishPay" :loading="paying">
        我已完成支付
      </el-button>
    </el-card>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
// 🔥 修复点 1：替换原生 axios，使用封装好自动带 Token 的 request
import request from '../utils/request'
import { ElMessage } from 'element-plus'

const route = useRoute()
const router = useRouter()
const orderId = route.query.orderId
const price = route.query.price
const paying = ref(false)

const finishPay = async () => {
  paying.value = true
  try {
    // 🔥 修复点 2：将 orderId 强制转为 Number 以迎合后端要求
    const res = await request.post('/order/pay', {
      orderId: Number(orderId)
    })

    // 🔥 修复点 3：根据后端真实的返回结果来决定走向
    if (res.code === '200') {
      ElMessage.success('支付成功！祝您旅途愉快')
      // 支付成功后，跳转回个人中心让用户直观地看到订单状态已变
      router.push('/profile')
    } else {
      ElMessage.error(res.msg || '支付失败，请重试')
    }
  } catch (error) {
    ElMessage.error('支付请求异常，请检查网络或登录状态')
  } finally {
    paying.value = false
  }
}
</script>

<style scoped>
.pay-container { height: 80vh; display: flex; align-items: center; justify-content: center; background: #f5f7fa; }
.pay-card { width: 400px; text-align: center; padding: 20px; box-shadow: 0 10px 30px rgba(0,0,0,0.1); border-radius: 12px; }
.amount { font-size: 40px; font-weight: bold; color: #ff9d00; margin: 20px 0; }
.qr-box { margin: 30px 0; padding: 20px; background: #f9f9f9; border-radius: 8px; border: 1px dashed #ddd; }
.pay-btn { width: 100%; font-size: 16px; font-weight: bold; }
</style>