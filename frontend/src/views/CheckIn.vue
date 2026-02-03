<template>
  <div class="check-in-page">
    <a-card title="座位签到" :bordered="false">
      <a-tabs v-model:activeKey="activeTab">
        <a-tab-pane key="qr" tab="扫码签到">
          <div class="scan-wrapper">
            <div v-if="isAdmin" class="admin-qr-generator">
              <a-divider>管理员：生成测试二维码</a-divider>
              <div class="generator-controls">
                <a-select v-model:value="selectedArea" placeholder="选择区域" style="width: 150px">
                  <a-select-option v-for="area in areas" :key="area" :value="area">{{ area }}</a-select-option>
                </a-select>
                <a-select v-model:value="selectedSeatNo" placeholder="选择座位" style="width: 150px" :disabled="!selectedArea">
                  <a-select-option v-for="no in availableSeatsInArea" :key="no" :value="no">{{ no }}</a-select-option>
                </a-select>
              </div>
              <div v-if="generatedQrText" class="qr-display-area">
                <div class="qr-code-box" draggable="true" @dragstart="(e) => e.dataTransfer?.setData('text', generatedQrText)">
                  <qrcode-vue :value="generatedQrText" :size="160" level="H" />
                  <p class="qr-hint">可拖拽此二维码或点击下方验证</p>
                </div>
                <a-button type="dashed" @click="simulateScan" class="mt-2">
                  <template #icon><select-outlined /></template>
                  验证该座位签到
                </a-button>
              </div>
            </div>

            <a-alert
              :message="isAdmin ? '扫码验证' : '请允许使用摄像头权限'"
              :description="isAdmin ? '扫描二维码或拖入上方生成的二维码' : '请扫描座位上的二维码进行签到'"
              type="info"
              show-icon
              class="mb-4"
            />
            <div class="scanner-container" @dragover.prevent @drop.prevent="onDrop">
              <qr-scanner :allow-file="isAdmin" @scan="handleQrScan" />
            </div>
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
import { ref, computed, onMounted, watch } from 'vue'
import { message } from 'ant-design-vue'
import { EnvironmentTwoTone, SelectOutlined } from '@ant-design/icons-vue'
import QrcodeVue from 'qrcode.vue'
import QrScanner from '../components/CheckIn/QrScanner.vue'
import { checkIn } from '../api/reservation'
import { getSeats, type Seat } from '../api/seat'
import { useUserStore } from '../stores/user'
import { useRouter } from 'vue-router'

const activeTab = ref('qr')
const userStore = useUserStore()
const router = useRouter()
const userInfo = computed(() => userStore.userInfo)
const isAdmin = computed(() => userInfo.value?.role === 'admin')
const reservation = computed(() => userStore.reservation)

// Admin QR Generation Logic
const allSeats = ref<Seat[]>([])
const areas = computed(() => [...new Set(allSeats.value.map(s => s.area))])
const selectedArea = ref<string>('')
const selectedSeatNo = ref<string>('')
const availableSeatsInArea = computed(() => 
  allSeats.value.filter(s => s.area === selectedArea.value).map(s => s.seatNo)
)

const generatedQrText = computed(() => {
  if (selectedArea.value && selectedSeatNo.value) {
    return `seat:${selectedArea.value}:${selectedSeatNo.value}`
  }
  return ''
})

onMounted(async () => {
  try {
    const data = await getSeats()
    allSeats.value = data as any
  } catch (e) {
    console.error('Failed to fetch seats', e)
  }
})

watch(selectedArea, () => {
  selectedSeatNo.value = ''
})

const simulateScan = () => {
  if (generatedQrText.value) {
    handleQrScan(generatedQrText.value)
  }
}

const onDrop = (e: DragEvent) => {
  const text = e.dataTransfer?.getData('text')
  if (text) {
    handleQrScan(text)
  }
}

// QR Code Logic
const handleQrScan = async (text: string) => {
  if (!reservation.value) {
    message.warning('您当前没有活跃的预约记录')
    return
  }
  
  try {
    await checkIn(reservation.value.id, { qrCode: text })
    message.success('扫码签到成功')
    userStore.checkIn()
    router.push('/dashboard')
  } catch (e: any) {
    message.error(e.response?.data?.msg || '签到失败')
  }
}

// GPS Logic
const locating = ref(false)
const gpsStatus = ref<'idle' | 'success' | 'error'>('idle')
const gpsMessage = ref('请确保您在图书馆范围内 (200米)')
const coords = ref<GeolocationCoordinates | null>(null)

const getLocation = () => {
  if (!navigator.geolocation) {
    message.error('您的浏览器不支持地理定位')
    return
  }

  if (!reservation.value) {
    message.warning('您当前没有活跃的预约记录')
    return
  }

  locating.value = true
  gpsMessage.value = '正在获取位置...'

  navigator.geolocation.getCurrentPosition(
    async (position) => {
      coords.value = position.coords
      try {
        await checkIn(reservation.value!.id, { 
          lat: position.coords.latitude, 
          lng: position.coords.longitude 
        })
        locating.value = false
        gpsStatus.value = 'success'
        gpsMessage.value = '定位成功！您已在图书馆范围内并完成签到。'
        message.success('签到成功')
        userStore.checkIn()
        router.push('/dashboard')
      } catch (e: any) {
        locating.value = false
        gpsStatus.value = 'error'
        gpsMessage.value = e.response?.data?.msg || '签到失败'
        message.error(gpsMessage.value)
      }
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

.admin-qr-generator {
  width: 100%;
  max-width: 500px;
  margin-bottom: 24px;
  padding: 16px;
  background: #f8f9fa;
  border-radius: 8px;
  text-align: center;
}

.generator-controls {
  display: flex;
  gap: 12px;
  justify-content: center;
  margin-bottom: 16px;
}

.qr-display-area {
  display: flex;
  flex-direction: column;
  align-items: center;
}

.qr-code-box {
  padding: 16px;
  background: white;
  border: 1px dashed #d9d9d9;
  border-radius: 4px;
  cursor: grab;
  transition: all 0.3s;
}

.qr-code-box:hover {
  border-color: #1890ff;
  background: #f0f7ff;
}

.qr-hint {
  margin-top: 8px;
  font-size: 12px;
  color: #8c8c8c;
}

.scanner-container {
  width: 100%;
  max-width: 500px;
  border: 2px solid transparent;
  transition: all 0.3s;
}

.scanner-container:hover {
  border-color: rgba(24, 144, 255, 0.2);
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
