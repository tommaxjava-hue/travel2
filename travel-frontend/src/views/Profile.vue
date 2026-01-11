<template>
  <div class="profile-page">
    <div class="profile-container">

      <div class="left-card">
        <el-card shadow="hover" class="user-card">
          <div class="user-header">
            <div class="avatar-wrapper">
              <el-avatar :size="100" :src="userInfo.avatar || defaultAvatar" class="user-avatar" />
              <el-upload
                  class="avatar-uploader"
                  action="http://localhost:8080/upload"
                  :show-file-list="false"
                  :on-success="handleAvatarSuccess"
                  :before-upload="beforeAvatarUpload"
              >
                <div class="upload-mask">
                  <el-icon><Camera /></el-icon>
                  <span>换头像</span>
                </div>
              </el-upload>
            </div>
            <h2 class="nickname">{{ userInfo.name || userInfo.nickname || userInfo.username }}</h2>
            <div class="tags-row" v-if="userInfo.gender || userInfo.age">
              <el-tag v-if="userInfo.gender" size="small" :type="userInfo.gender === '女' ? 'danger' : ''">{{ userInfo.gender }}</el-tag>
              <el-tag v-if="userInfo.age" size="small" type="info" style="margin-left:5px">{{ userInfo.age }}岁</el-tag>
            </div>

            <p class="role-tag">
              <el-tag
                  :type="userInfo.role === 'admin' ? 'danger' : 'success'"
                  size="small"
                  effect="dark"
              >
                {{ userInfo.role === 'admin' ? '管理员' : '普通用户' }}
              </el-tag>
            </p>

            <p class="bio">{{ userInfo.email || '暂未绑定邮箱' }}</p>
          </div>

          <div class="stats-row">
            <div class="stat-item" @click="$router.push('/itinerary')">
              <div class="num">📅</div>
              <div class="label">我的行程</div>
            </div>
            <div class="stat-item" @click="$router.push('/itinerary')">
              <div class="num">❤️</div>
              <div class="label">我的收藏</div>
            </div>
          </div>
        </el-card>
      </div>

      <div class="right-card">
        <el-card shadow="hover">
          <el-tabs v-model="activeTab" class="profile-tabs">

            <el-tab-pane label="📝 编辑资料" name="info">
              <el-form :model="form" label-position="top" class="edit-form">
                <el-row :gutter="20">
                  <el-col :span="12">
                    <el-form-item label="账号 (不可修改)">
                      <el-input v-model="form.username" disabled prefix-icon="User" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="12">
                    <el-form-item label="姓名 / 昵称">
                      <el-input v-model="form.name" placeholder="怎么称呼您？" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-row :gutter="20">
                  <el-col :span="8">
                    <el-form-item label="性别">
                      <el-select v-model="form.gender" placeholder="选择性别" style="width:100%">
                        <el-option label="男" value="男" />
                        <el-option label="女" value="女" />
                        <el-option label="保密" value="保密" />
                      </el-select>
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="年龄">
                      <el-input-number v-model="form.age" :min="1" :max="120" style="width:100%" />
                    </el-form-item>
                  </el-col>
                  <el-col :span="8">
                    <el-form-item label="所在城市">
                      <el-input v-model="form.city" placeholder="如：北京" prefix-icon="Location" />
                    </el-form-item>
                  </el-col>
                </el-row>

                <el-form-item label="联系电话">
                  <el-input v-model="form.phone" placeholder="请输入手机号" prefix-icon="Iphone" />
                </el-form-item>

                <el-form-item label="电子邮箱">
                  <el-input v-model="form.email" placeholder="用于接收通知" prefix-icon="Message" />
                </el-form-item>

                <el-form-item label="兴趣标签 (用于AI推荐)">
                  <el-checkbox-group v-model="selectedTags">
                    <el-checkbox label="古迹" border />
                    <el-checkbox label="美食" border />
                    <el-checkbox label="亲子" border />
                    <el-checkbox label="爬山" border />
                    <el-checkbox label="海边" border />
                    <el-checkbox label="打卡" border />
                    <el-checkbox label="自驾" border />
                  </el-checkbox-group>
                </el-form-item>

                <el-form-item>
                  <el-button type="primary" @click="saveProfile" :loading="loading" size="large" style="width: 150px;">保存修改</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

            <el-tab-pane label="🔒 安全中心" name="security">
              <el-form :model="pwdForm" label-width="100px" style="max-width: 500px; margin-top: 20px">
                <el-form-item label="新密码">
                  <el-input v-model="pwdForm.password" type="password" show-password />
                </el-form-item>
                <el-form-item label="确认密码">
                  <el-input v-model="pwdForm.confirm" type="password" show-password />
                </el-form-item>
                <el-form-item>
                  <el-button type="danger" @click="updatePassword">修改密码</el-button>
                </el-form-item>
              </el-form>
            </el-tab-pane>

          </el-tabs>
        </el-card>
      </div>

    </div>
  </div>
</template>

<script setup>
import { ref, reactive, onMounted } from 'vue'
import axios from 'axios'
import { ElMessage } from 'element-plus'
import { User, Iphone, Message, Camera, Location } from '@element-plus/icons-vue'

const activeTab = ref('info')
const loading = ref(false)
const defaultAvatar = 'https://cube.elemecdn.com/3/7c/3ea6beec64369c2642b92c6726f1epng.png'

const userInfo = ref({})
const form = reactive({
  userId: null,
  username: '',
  name: '',
  nickname: '', // 兼容旧字段
  gender: '',
  age: null,
  city: '',
  phone: '',
  email: '',
  avatar: '',
  tags: '',
  role: '' // 确保这里有 role 字段占位
})
const selectedTags = ref([])

const pwdForm = reactive({ password: '', confirm: '' })

// 初始化加载
const loadUser = async () => {
  const localUser = JSON.parse(localStorage.getItem('user') || '{}')
  if (!localUser.userId) return

  try {
    const res = await axios.get(`http://localhost:8080/user/info?userId=${localUser.userId}`)
    if (res.data.code === '200') {
      userInfo.value = res.data.data
      Object.assign(form, res.data.data)
      if (form.tags) {
        selectedTags.value = form.tags.split(',')
      } else {
        selectedTags.value = []
      }
    }
  } catch (e) {
    ElMessage.error('获取用户信息失败')
  }
}

// 保存资料
const saveProfile = async () => {
  loading.value = true
  try {
    form.tags = selectedTags.value.join(',')

    const res = await axios.post('http://localhost:8080/user/update', form)
    if (res.data.code === '200') {
      ElMessage.success('资料更新成功！')
      userInfo.value = res.data.data
      localStorage.setItem('user', JSON.stringify(res.data.data))
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch (e) {
    ElMessage.error('网络错误')
  } finally {
    loading.value = false
  }
}

// 修改密码
const updatePassword = async () => {
  if (!pwdForm.password || pwdForm.password.length < 6) return ElMessage.warning('密码长度至少6位')
  if (pwdForm.password !== pwdForm.confirm) return ElMessage.warning('两次密码不一致')

  try {
    const res = await axios.post('http://localhost:8080/user/update', {
      userId: form.userId,
      password: pwdForm.password
    })
    if (res.data.code === '200') {
      ElMessage.success('密码修改成功')
      pwdForm.password = ''
      pwdForm.confirm = ''
    } else {
      ElMessage.error(res.data.msg)
    }
  } catch(e) { ElMessage.error('请求失败') }
}

// 头像上传
const handleAvatarSuccess = (res) => {
  if (res.code === '200') {
    form.avatar = res.data
    userInfo.value.avatar = res.data
    saveProfile()
  } else {
    ElMessage.error('上传失败')
  }
}

const beforeAvatarUpload = (rawFile) => {
  if (rawFile.size / 1024 / 1024 > 5) {
    ElMessage.error('头像大小不能超过 5MB!')
    return false
  }
  return true
}

onMounted(loadUser)
</script>

<style scoped>
.profile-page { min-height: 100vh; background-color: #f5f7fa; padding: 40px 0; }
.profile-container { max-width: 1100px; margin: 0 auto; display: flex; gap: 20px; padding: 0 20px; }

/* 左侧样式 */
.left-card { width: 320px; flex-shrink: 0; }
.user-header { text-align: center; padding: 20px 0; }

.avatar-wrapper { position: relative; width: 100px; height: 100px; margin: 0 auto 15px; border-radius: 50%; overflow: hidden; border: 4px solid #fff; box-shadow: 0 4px 12px rgba(0,0,0,0.1); }
.avatar-uploader { height: 100%; width: 100%; cursor: pointer; }
.upload-mask { position: absolute; top: 0; left: 0; width: 100%; height: 100%; background: rgba(0,0,0,0.5); color: white; display: flex; flex-direction: column; justify-content: center; align-items: center; opacity: 0; transition: 0.3s; font-size: 12px; }
.avatar-wrapper:hover .upload-mask { opacity: 1; }

.nickname { margin: 10px 0 5px; font-size: 22px; color: #333; }
.role-tag { margin-bottom: 10px; }
.tags-row { margin-bottom: 15px; }
.bio { color: #999; font-size: 13px; padding: 0 20px; line-height: 1.5; }

.stats-row { display: flex; border-top: 1px solid #f0f0f0; margin-top: 20px; padding-top: 20px; }
.stat-item { flex: 1; text-align: center; cursor: pointer; transition: 0.2s; }
.stat-item:hover { background-color: #fafafa; }
.stat-item .num { font-size: 20px; margin-bottom: 5px; }
.stat-item .label { font-size: 12px; color: #666; }

/* 右侧样式 */
.right-card { flex: 1; }
.profile-tabs :deep(.el-tabs__item) { font-size: 16px; }
.edit-form { padding: 10px 20px; }
</style>