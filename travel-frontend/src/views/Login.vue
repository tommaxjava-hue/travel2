<template>
  <div class="login-container">
    <div class="login-box">
      <h2>✈️ 智能旅游平台</h2>

      <el-tabs v-model="activeTab" class="custom-tabs">
        <el-tab-pane label="登录" name="login">
          <el-form :model="loginForm">
            <el-form-item>
              <el-input v-model="loginForm.username" placeholder="用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password />
            </el-form-item>
            <el-form-item class="captcha-row">
              <el-input v-model="regForm.verifyCode" placeholder="验证码" style="width: 140px;" />
              <img :src="captchaImg" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
            </el-form-item>
            <el-button type="primary" class="w-100" @click="handleLogin" :loading="loading">立即登录</el-button>
          </el-form>
        </el-tab-pane>

        <el-tab-pane label="注册" name="register">
          <el-form :model="regForm">
            <el-form-item>
              <el-input v-model="regForm.username" placeholder="设置用户名" prefix-icon="User" />
            </el-form-item>
            <el-form-item>
              <el-input v-model="regForm.password" type="password" placeholder="设置密码" prefix-icon="Lock" show-password />
            </el-form-item>

            <el-form-item class="captcha-row">
              <el-input v-model="regForm.verifyCode" placeholder="验证码" style="width: 140px;" />
              <img :src="captchaImg" class="captcha-img" @click="refreshCaptcha" title="点击刷新" />
            </el-form-item>

            <el-form-item label="兴趣标签">
              <el-checkbox-group v-model="selectedTags">
                <el-checkbox label="古迹" />
                <el-checkbox label="美食" />
                <el-checkbox label="亲子" />
                <el-checkbox label="打卡" />
                <el-checkbox label="爬山" />
              </el-checkbox-group>
            </el-form-item>

            <el-button type="success" class="w-100" @click="handleRegister" :loading="loading">注册账号</el-button>
          </el-form>
        </el-tab-pane>
      </el-tabs>
    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)

const loginForm = reactive({ username: '', password: '' })
// 整合 regForm：既有验证码字段，又有后续处理需要的标签逻辑
const regForm = reactive({ username: '', password: '', verifyCode: '', verifyKey: '' })
const selectedTags = ref([]) // 选中的标签
const captchaImg = ref('')

// 获取验证码 (保留旧功能)
const refreshCaptcha = async () => {
  try {
    const res = await axios.get('http://localhost:8080/captcha/image')
    if (res.data.code === '200') {
      regForm.verifyKey = res.data.data.key
      captchaImg.value = res.data.data.image // Base64直接展示
    }
  } catch(e) { console.error(e) }
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password) return ElMessage.warning('请输入完整')
  loading.value = true
  try {
    const res = await axios.post('http://localhost:8080/user/login', loginForm)
    if (res.data.code === '200') {
      localStorage.setItem('user', JSON.stringify(res.data.data))
      ElMessage.success('登录成功')
      router.push('/')
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch(e) { ElMessage.error('网络错误') }
  finally { loading.value = false }
}

const handleRegister = async () => {
  // 校验整合：既要校验账号密码，也要校验验证码
  if (!regForm.username || !regForm.password || !regForm.verifyCode) return ElMessage.warning('请填写完整')

  loading.value = true
  try {
    // 拼接标签
    const tagsStr = selectedTags.value.join(',')

    // 发送请求：包含验证码参数 + 标签参数
    const res = await axios.post('http://localhost:8080/user/register', {
      ...regForm,
      tags: tagsStr
    })

    if (res.data.code === '200') {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.username = regForm.username
    } else {
      ElMessage.error(res.data.msg)
      refreshCaptcha() // 失败刷新验证码
    }
  } catch(e) { ElMessage.error('注册失败') }
  finally { loading.value = false }
}

onMounted(() => {
  refreshCaptcha()
})
</script>

<style scoped>
.login-container { height: 100vh; display: flex; justify-content: center; align-items: center; background: linear-gradient(135deg, #8BC6EC 0%, #9599E2 100%); }
.login-box { width: 380px; padding: 40px; background: white; border-radius: 12px; box-shadow: 0 10px 25px rgba(0,0,0,0.1); text-align: center; }
.w-100 { width: 100%; margin-top: 10px; }
.captcha-row { display: flex; justify-content: space-between; align-items: center; }
.captcha-img { height: 38px; cursor: pointer; border: 1px solid #ddd; border-radius: 4px; margin-left: 10px; }
</style>