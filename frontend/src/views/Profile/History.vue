<template>
  <div class="page-container">
    <a-card :bordered="false" title="我的预约/签到记录">
      <a-timeline mode="left" class="mt-4">
        <a-timeline-item
          v-for="record in history"
          :key="record.id"
          :color="getStatusColor(record.status)"
        >
          <template #dot>
            <component :is="getStatusIcon(record.status)" style="font-size: 16px" />
          </template>
          
          <div class="record-item">
            <div class="record-header">
              <span class="seat-no">{{ record.seatNumber }} 座位</span>
              <a-tag :color="getStatusColor(record.status)">
                {{ getStatusText(record.status) }}
              </a-tag>
              <a-button 
                type="link" 
                size="small" 
                danger 
                v-if="record.status === 'violation'"
                @click="handleAppeal(record)"
              >
                申诉
              </a-button>
            </div>
            <div class="record-time">
              {{ record.startTime }} ~ {{ record.endTime }}
            </div>
            <div class="record-type">
              类型：{{ record.type === 'appointment' ? '预约' : '现场签到' }}
            </div>
          </div>
        </a-timeline-item>
      </a-timeline>

      <a-empty v-if="history.length === 0" description="暂无记录" />
    </a-card>

    <!-- 申诉弹窗 -->
    <a-modal
      v-model:open="appealVisible"
      title="违规申诉"
      @ok="submitAppeal"
      :confirmLoading="submitting"
    >
      <a-form layout="vertical">
        <a-form-item label="违规记录">
          <p>{{ currentRecord?.startTime }} - {{ currentRecord?.seatNumber }}</p>
        </a-form-item>
        <a-form-item label="申诉理由" required>
          <a-textarea v-model:value="appealReason" :rows="4" placeholder="请详细描述申诉理由..." />
        </a-form-item>
        <a-form-item label="上传凭证">
          <a-upload
            v-model:file-list="fileList"
            list-type="picture-card"
            :before-upload="() => false"
          >
            <div v-if="fileList.length < 3">
              <plus-outlined />
              <div style="margin-top: 8px">上传</div>
            </div>
          </a-upload>
        </a-form-item>
      </a-form>
    </a-modal>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { 
  ClockCircleOutlined, 
  CheckCircleOutlined, 
  CloseCircleOutlined, 
  ExclamationCircleOutlined,
  PlusOutlined
} from '@ant-design/icons-vue'
import { message } from 'ant-design-vue'
import { getMyHistory, submitAppeal as submitAppealApi, type ReservationRecord } from '../../api/reservation'

const history = ref<ReservationRecord[]>([])
const appealVisible = ref(false)
const submitting = ref(false)
const currentRecord = ref<ReservationRecord | null>(null)
const appealReason = ref('')
const fileList = ref([])

onMounted(async () => {
  try {
    const res = await getMyHistory()
    history.value = Array.isArray(res) ? res : (res as any).data || []
  } catch (error) {
    console.error(error)
  }
})

const handleAppeal = (record: ReservationRecord) => {
  currentRecord.value = record
  appealReason.value = ''
  fileList.value = []
  appealVisible.value = true
}

const submitAppeal = async () => {
  if (!appealReason.value) {
    message.warning('请填写申诉理由')
    return
  }
  
  if (!currentRecord.value) return

  submitting.value = true
  try {
    await submitAppealApi(currentRecord.value.id, {
      reason: appealReason.value
    })
    message.success('申诉已提交，请等待管理员审核')
    appealVisible.value = false
  } catch (error) {
    message.error('提交失败')
  } finally {
    submitting.value = false
  }
}

const getStatusColor = (status: string) => {
  switch (status) {
    case 'active': return 'blue'
    case 'completed': return 'green'
    case 'violation': return 'red'
    case 'cancelled': return 'gray'
    default: return 'blue'
  }
}

const getStatusText = (status: string) => {
  switch (status) {
    case 'active': return '进行中'
    case 'completed': return '已完成'
    case 'violation': return '违规'
    case 'cancelled': return '已取消'
    default: return status
  }
}

const getStatusIcon = (status: string) => {
  switch (status) {
    case 'active': return ClockCircleOutlined
    case 'completed': return CheckCircleOutlined
    case 'violation': return ExclamationCircleOutlined
    case 'cancelled': return CloseCircleOutlined
    default: return ClockCircleOutlined
  }
}
</script>

<style scoped>
.page-container {
  padding: 24px;
}
.mt-4 {
  margin-top: 24px;
}
.record-item {
  margin-bottom: 20px;
}
.record-header {
  font-weight: bold;
  font-size: 16px;
  margin-bottom: 4px;
}
.seat-no {
  margin-right: 8px;
}
.record-time {
  color: #666;
  font-size: 13px;
}
.record-type {
  color: #999;
  font-size: 12px;
  margin-top: 4px;
}
</style>
