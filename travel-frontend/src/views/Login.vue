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
              <el-input v-model="loginForm.password" type="password" placeholder="密码" prefix-icon="Lock" show-password @keyup.enter="handleLogin" />
            </el-form-item>
            <el-form-item class="captcha-row">
              <el-input v-model="loginForm.verifyCode" placeholder="验证码" style="width: 140px;" @keyup.enter="handleLogin" />
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
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
// 企业级改造：使用封装好的 request 对象
import request from '../utils/request'

const router = useRouter()
const activeTab = ref('login')
const loading = ref(false)

// 修复：为登录表单增加验证码字段
const loginForm = reactive({ username: '', password: '', verifyCode: '', verifyKey: '' })
const regForm = reactive({ username: '', password: '', verifyCode: '', verifyKey: '' })
const selectedTags = ref([])
const captchaImg = ref('')

// 获取验证码 (改用 request)
const refreshCaptcha = async () => {
  try {
    const res = await request.get('/captcha/image')
    if (res.code === '200') {
      // 同时更新登录和注册表单的 verifyKey
      regForm.verifyKey = res.data.key
      loginForm.verifyKey = res.data.key
      captchaImg.value = res.data.image
    }
  } catch(e) {
    console.error(e)
  }
}

const handleLogin = async () => {
  if (!loginForm.username || !loginForm.password || !loginForm.verifyCode) {
    return ElMessage.warning('请输入完整信息(含验证码)')
  }
  loading.value = true
  try {
    const res = await request.post('/user/login', loginForm)
    if (res.code === '200') {
      // 企业级修复：正确存储 Token 和 擦除敏感信息后的 User 对象
      localStorage.setItem('token', res.data.token)
      localStorage.setItem('user', JSON.stringify(res.data.userInfo))
      ElMessage.success('登录成功')

      if (res.data.userInfo.role === 'admin') {
        router.push('/admin/dashboard')
      } else {
        router.push('/')
      }
    } else {
      // 失败后自动刷新验证码
      refreshCaptcha()
      loginForm.verifyCode = ''
    }
  } catch(e) {
    console.error(e)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
}

const handleRegister = async () => {
  if (!regForm.username || !regForm.password || !regForm.verifyCode) {
    return ElMessage.warning('请填写完整')
  }

  loading.value = true
  try {
    const tagsStr = selectedTags.value.join(',')

    const res = await request.post('/user/register', {
      ...regForm,
      tags: tagsStr
    })

    if (res.code === '200') {
      ElMessage.success('注册成功，请登录')
      activeTab.value = 'login'
      loginForm.username = regForm.username
      refreshCaptcha() // 注册成功切回登录时，顺便刷新一下验证码
    } else {
      refreshCaptcha()
      regForm.verifyCode = ''
    }
  } catch(e) {
    console.error(e)
    refreshCaptcha()
  } finally {
    loading.value = false
  }
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