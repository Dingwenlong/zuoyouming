<template>
  <div class="check-in-page">
    <a-card title="座位签到" :bordered="false">
      <div class="unified-checkin-flow">
        <!-- 步骤 1: 定位状态 -->
        <div class="status-section mb-6">
          <div class="flex items-center justify-between p-4 bg-gray-50 rounded-lg">
            <div class="flex items-center">
              <environment-two-tone :two-tone-color="gpsStatus === 'success' ? '#52c41a' : (gpsStatus === 'error' ? '#ff4d4f' : '#1890ff')" style="font-size: 24px; margin-right: 12px" />
              <div>
                <div class="font-bold">{{ gpsMessage }}</div>
                <div v-if="coords" class="text-xs text-gray-500">
                  经度: {{ coords.longitude.toFixed(6) }}, 纬度: {{ coords.latitude.toFixed(6) }}
                </div>
              </div>
            </div>
            <a-button size="small" @click="getLocation" :loading="locating">重新定位</a-button>
          </div>
        </div>

        <!-- 步骤 2: 扫码区域 -->
        <div class="scan-section" :class="{ 'opacity-50 pointer-events-none': gpsStatus !== 'success' }">
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
                <p class="qr-hint">可拖拽此二维码到下方扫码区</p>
              </div>
              <a-button type="dashed" @click="simulateScan" class="mt-2" :disabled="gpsStatus !== 'success'">
                <template #icon><select-outlined /></template>
                模拟扫码并签到
              </a-button>
            </div>
          </div>

          <a-alert
            v-if="gpsStatus !== 'success'"
            message="等待定位"
            description="请先完成定位，确保您已在图书馆范围内，随后将自动开启扫码。"
            type="warning"
            show-icon
            class="mb-4"
          />
          <a-alert
            v-else
            message="请扫描座位二维码"
            description="定位已成功，请扫描您预约座位上的二维码完成签到。"
            type="success"
            show-icon
            class="mb-4"
          />

          <div class="scanner-container" @dragover.prevent @drop.prevent="onDrop">
            <qr-scanner :allow-file="isAdmin" @scan="handleQrScan" />
          </div>
        </div>
      </div>
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
    // 匹配后端 ReservationService.java L349 的格式: "Area:A区,SeatNo:A-01"
    return `Area:${selectedArea.value},SeatNo:${selectedSeatNo.value}`
  }
  return ''
})

onMounted(async () => {
  // 1. 获取座位列表
  try {
    const data = await getSeats()
    allSeats.value = data as any
  } catch (e) {
    console.error('Failed to fetch seats', e)
  }

  // 2. 自动开启定位
  getLocation()
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

  if (gpsStatus.value !== 'success' || !coords.value) {
    message.error('签到失败：请先完成定位')
    return
  }
  
  try {
    await checkIn(reservation.value.id, { 
      qrCode: text,
      lat: coords.value.latitude,
      lng: coords.value.longitude
    })
    message.success('扫码并定位签到成功')
    userStore.checkIn()
    router.push('/dashboard')
  } catch (e: any) {
    message.error(e.response?.data?.msg || '签到失败')
  }
}

// GPS Logic
const locating = ref(false)
const gpsStatus = ref<'idle' | 'success' | 'error'>('idle')
const gpsMessage = ref('正在检测您的位置...')
const coords = ref<GeolocationCoordinates | null>(null)

const getLocation = () => {
  if (!navigator.geolocation) {
    message.error('您的浏览器不支持地理定位')
    gpsStatus.value = 'error'
    gpsMessage.value = '浏览器不支持定位'
    return
  }

  locating.value = true
  gpsStatus.value = 'idle'
  gpsMessage.value = '正在获取位置...'

  navigator.geolocation.getCurrentPosition(
    (position) => {
      coords.value = position.coords
      locating.value = false
      gpsStatus.value = 'success'
      gpsMessage.value = '定位成功，已在范围内'
    },
    (error) => {
      locating.value = false
      gpsStatus.value = 'error'
      gpsMessage.value = `定位失败: ${error.message}`
      message.error('定位失败，请检查定位权限')
    },
    { enableHighAccuracy: true, timeout: 10000 }
  )
}
</script>

<style scoped>
.check-in-page {
  padding: 24px;
}

.unified-checkin-flow {
  max-width: 600px;
  margin: 0 auto;
}

.flex {
  display: flex;
}

.items-center {
  align-items: center;
}

.justify-between {
  justify-content: space-between;
}

.p-4 {
  padding: 1rem;
}

.bg-gray-50 {
  background-color: #f9fafb;
}

.rounded-lg {
  border-radius: 0.5rem;
}

.font-bold {
  font-weight: 700;
}

.text-xs {
  font-size: 0.75rem;
}

.text-gray-500 {
  color: #6b7280;
}

.mb-4 {
  margin-bottom: 1rem;
}

.mb-6 {
  margin-bottom: 1.5rem;
}

.mt-2 {
  margin-top: 0.5rem;
}

.admin-qr-generator {
  width: 100%;
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
  border: 2px solid transparent;
  transition: all 0.3s;
}

.opacity-50 {
  opacity: 0.5;
}

.pointer-events-none {
  pointer-events: none;
}
</style>
