<template>
  <div class="page-container">
    <a-card :bordered="false" class="profile-card">
      <template #title>
        <span class="card-title">{{ isNewUser ? '完善个人信息' : '个人信息' }}</span>
      </template>
      
      <a-alert
        v-if="isNewUser"
        message="新用户提醒"
        description="欢迎加入！请先完善您的个人信息，绑定手机号并设置密码后方可使用系统功能。"
        type="warning"
        show-icon
        class="mb-6"
      />

      <div class="profile-content">
        <!-- 左侧头像区域 -->
        <div class="avatar-section">
          <a-avatar :size="100" :src="userInfo?.avatar" class="profile-avatar">
            <template #icon><user-outlined /></template>
          </a-avatar>
          <a-button type="link" class="mt-4">更换头像</a-button>
        </div>

        <!-- 右侧表单区域 -->
        <div class="form-section">
          <a-form
            ref="formRef"
            :model="formState"
            :rules="rules"
            layout="vertical"
            @finish="handleFinish"
            class="profile-form"
          >
            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="学号/工号" name="studentId">
                  <a-input v-model:value="formState.studentId" placeholder="请输入学号" :disabled="!isNewUser" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="真实姓名" name="realName">
                  <a-input v-model:value="formState.realName" placeholder="请输入真实姓名" :disabled="!isNewUser" />
                </a-form-item>
              </a-col>
            </a-row>

            <a-row :gutter="24">
              <a-col :span="12">
                <a-form-item label="昵称" name="alias">
                  <a-input v-model:value="formState.alias" placeholder="请输入昵称" />
                </a-form-item>
              </a-col>
              <a-col :span="12">
                <a-form-item label="手机号" name="phone">
                  <a-input v-model:value="formState.phone" placeholder="请输入手机号" :disabled="!isNewUser && !!userInfo?.phone" />
                </a-form-item>
              </a-col>
            </a-row>

            <!-- 仅新用户需要验证手机和设置密码 -->
            <template v-if="isNewUser">
              <a-form-item label="手机验证码" name="verifyCode">
                <div class="flex gap-2">
                  <a-input v-model:value="formState.verifyCode" placeholder="请输入验证码" />
                  <a-button 
                    :disabled="countdown > 0" 
                    @click="sendCode"
                    :loading="sending"
                  >
                    {{ countdown > 0 ? `${countdown}s 后重发` : '获取验证码' }}
                  </a-button>
                </div>
              </a-form-item>

              <a-row :gutter="24">
                <a-col :span="12">
                  <a-form-item label="设置登录密码" name="password">
                    <a-input-password v-model:value="formState.password" placeholder="6-20位字符" />
                  </a-form-item>
                </a-col>
                <a-col :span="12">
                  <a-form-item label="确认密码" name="confirmPassword">
                    <a-input-password v-model:value="formState.confirmPassword" placeholder="再次输入密码" />
                  </a-form-item>
                </a-col>
              </a-row>
            </template>

            <a-form-item class="mt-4">
              <a-button type="primary" html-type="submit" :loading="loading" size="large">
                {{ isNewUser ? '提交绑定' : '保存修改' }}
              </a-button>
            </a-form-item>
          </a-form>
        </div>
      </div>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { reactive, ref, computed, onUnmounted, onMounted } from 'vue'
import { UserOutlined } from '@ant-design/icons-vue'
import { useUserStore } from '../../stores/user'
import { message } from 'ant-design-vue'
import type { Rule } from 'ant-design-vue/es/form'
import { useRouter } from 'vue-router'

const userStore = useUserStore()
const router = useRouter()
const isNewUser = computed(() => userStore.isNewUser)
const userInfo = computed(() => userStore.userInfo)

const formRef = ref()
const loading = ref(false)
const sending = ref(false)
const countdown = ref(0)
let timer: number | null = null

// 模拟验证码
let validCode = ''
let codeExpireTime = 0

const formState = reactive({
  studentId: '',
  realName: '',
  alias: '',
  phone: '',
  verifyCode: '',
  password: '',
  confirmPassword: ''
})

const validateConfirmPassword = async (_rule: Rule, value: string) => {
  if (value === '') {
    return Promise.reject('请再次输入密码')
  } else if (value !== formState.password) {
    return Promise.reject('两次输入密码不一致')
  } else {
    return Promise.resolve()
  }
}

const rules = computed(() => {
  const baseRules: Record<string, Rule[]> = {
    alias: [{ required: true, message: '请输入昵称' }],
    phone: [
      { required: true, message: '请输入手机号' },
      { pattern: /^1[3-9]\d{9}$/, message: '请输入有效的手机号' }
    ]
  }

  if (isNewUser.value) {
    return {
      ...baseRules,
      studentId: [{ required: true, message: '请输入学号' }],
      realName: [{ required: true, message: '请输入真实姓名' }],
      verifyCode: [{ required: true, message: '请输入验证码' }],
      password: [
        { required: true, message: '请输入密码' },
        { min: 6, max: 20, message: '密码长度需在6-20位之间' }
      ],
      confirmPassword: [
        { required: true, validator: validateConfirmPassword, trigger: 'change' }
      ]
    }
  }

  return baseRules
})

const sendCode = async () => {
  try {
    await formRef.value.validateFields(['phone'])
    sending.value = true
    
    // 模拟发送
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    validCode = Math.floor(100000 + Math.random() * 900000).toString()
    codeExpireTime = Date.now() + 2 * 60 * 1000
    
    message.success(`验证码已发送: ${validCode} (有效期2分钟)`)
    
    countdown.value = 120
    timer = window.setInterval(() => {
      countdown.value--
      if (countdown.value <= 0) {
        clearInterval(timer!)
        timer = null
      }
    }, 1000)
    
  } catch (error) {
    // 校验失败
  } finally {
    sending.value = false
  }
}

const handleFinish = async (values: any) => {
  if (isNewUser.value) {
    // 校验验证码
    if (Date.now() > codeExpireTime) {
      message.error('验证码已过期，请重新获取')
      return
    }
    if (values.verifyCode !== validCode) {
      message.error('验证码错误')
      return
    }
  }

  loading.value = true
  try {
    // 模拟API请求
    await new Promise(resolve => setTimeout(resolve, 1000))
    
    if (isNewUser.value) {
      userStore.completeBinding()
      message.success('绑定成功，欢迎使用！')
      // 绑定完成后跳转首页
      router.push('/')
    } else {
      message.success('保存成功')
    }
  } catch (error) {
    message.error('操作失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  if (userInfo.value) {
    // 回填信息 (如果是已有用户)
    formState.alias = userInfo.value.username || ''
    // 其他字段...
  }
})

onUnmounted(() => {
  if (timer) clearInterval(timer)
})
</script>

<style scoped>
.page-container {
  padding: 24px;
}

.profile-card {
  border-radius: 8px;
  box-shadow: 0 1px 2px 0 rgba(0, 0, 0, 0.03), 0 1px 6px -1px rgba(0, 0, 0, 0.02), 0 2px 4px 0 rgba(0, 0, 0, 0.02);
}

.card-title {
  font-size: 18px;
  font-weight: 600;
  color: #1e293b;
}

.profile-content {
  display: flex;
  gap: 48px;
  padding: 24px 0;
}

.avatar-section {
  flex: 0 0 200px;
  display: flex;
  flex-direction: column;
  align-items: center;
  padding-top: 24px;
  border-right: 1px solid #f1f5f9;
}

.profile-avatar {
  background-color: #f1f5f9;
  border: 4px solid #fff;
  box-shadow: 0 4px 6px -1px rgba(0, 0, 0, 0.1), 0 2px 4px -1px rgba(0, 0, 0, 0.06);
}

.form-section {
  flex: 1;
  max-width: 800px;
}

.profile-form {
  padding-right: 24px;
}

.mb-6 {
  margin-bottom: 24px;
}

.mt-4 {
  margin-top: 16px;
}

.flex {
  display: flex;
}

.gap-2 {
  gap: 8px;
}

@media screen and (max-width: 768px) {
  .profile-content {
    flex-direction: column;
    gap: 24px;
  }
  
  .avatar-section {
    border-right: none;
    border-bottom: 1px solid #f1f5f9;
    padding-bottom: 24px;
    flex: auto;
  }
  
  .profile-form {
    padding-right: 0;
  }
}
</style>
