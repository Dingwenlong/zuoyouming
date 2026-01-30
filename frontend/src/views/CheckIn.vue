<template>
  <div class="check-in-page">
    <a-card title="座位签到" :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="qr" tab="扫码签到">
          <div class="scan-wrapper">
            <a-alert
              message="请允许使用摄像头权限"
              description="请扫描座位上的二维码进行签到"
              type="info"
              show-icon
              class="mb-4"
            />
            <qr-scanner @scan="handleQrScan" />
          </div>
        </a-tab-pane>
        
        <a-tab-pane key="gps" tab="定位签到">
          <div class="gps-wrapper">
            <div class="gps-status">
              <environment-two-tone :two-tone-color="gpsStatus === 'success' ? '#52c41a' : '#1890ff'" style="font-size: 48px" />
              <p class="mt-4">{{ gpsMessage }}</p>
              <p v-if="coords" class="coords-text">
                经度: {{ coords.longitude.toFixed(6) }} <br/>
                纬度: {{ coords.latitude.toFixed(6) }}
              </p>
            </div>
            
            <a-button 
              type="primary" 
              size="large" 
              @click="getLocation" 
              :loading="locating"
              class="mt-6"
            >
              获取定位并签到
            </a-button>
          </div>
        </a-tab-pane>
      </a-tabs>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref } from 'vue'
import { message } from 'ant-design-vue'
import { EnvironmentTwoTone } from '@ant-design/icons-vue'
import QrScanner from '../components/CheckIn/QrScanner.vue'

const activeTab = ref('qr')

// QR Code Logic
const handleQrScan = (text: string) => {
  console.log('Scanned:', text)
  message.success(`扫码成功: ${text}`)
  // TODO: Call API to check in
}

// GPS Logic
const locating = ref(false)
const gpsStatus = ref<'idle' | 'success' | 'error'>('idle')
const gpsMessage = ref('请确保您在图书馆范围内 (50米)')
const coords = ref<GeolocationCoordinates | null>(null)

const getLocation = () => {
  if (!navigator.geolocation) {
    message.error('您的浏览器不支持地理定位')
    return
  }

  locating.value = true
  gpsMessage.value = '正在获取位置...'

  navigator.geolocation.getCurrentPosition(
    (position) => {
      locating.value = false
      gpsStatus.value = 'success'
      coords.value = position.coords
      
      // Calculate distance (Simple Haversine formula approximation or mock check)
      // For demo, we assume success
      gpsMessage.value = '定位成功！您已在图书馆范围内。'
      message.success('签到成功')
    },
    (error) => {
      locating.value = false
      gpsStatus.value = 'error'
      gpsMessage.value = `定位失败: ${error.message}`
      message.error('定位失败，请检查权限')
    }
  )
}
</script>

<style scoped>
.check-in-page {
  padding: 24px;
}

.scan-wrapper, .gps-wrapper {
  display: flex;
  flex-direction: column;
  align-items: center;
  padding: 20px 0;
}

.gps-status {
  text-align: center;
}

.coords-text {
  font-size: 12px;
  color: #999;
  margin-top: 8px;
}

.mb-4 {
  margin-bottom: 16px;
  width: 100%;
  max-width: 500px;
}

.mt-4 {
  margin-top: 16px;
}

.mt-6 {
  margin-top: 24px;
}
</style>
