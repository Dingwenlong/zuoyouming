<template>
  <div class="dashboard-container">
    <a-row :gutter="[16, 16]">
      <a-col :xs="24" :sm="24" :md="8">
        <a-card>
          <a-statistic title="总座位数" :value="120" style="margin-right: 50px">
            <template #suffix>
              <user-outlined style="color: #1890ff" />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="24" :md="8">
        <a-card>
          <a-statistic title="空闲" :value="45" :value-style="{ color: '#3f8600' }">
            <template #suffix>
              <check-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
      <a-col :xs="24" :sm="24" :md="8">
        <a-card>
          <a-statistic title="使用中" :value="75" :value-style="{ color: '#cf1322' }">
            <template #suffix>
              <close-circle-outlined />
            </template>
          </a-statistic>
        </a-card>
      </a-col>
    </a-row>

    <a-row :gutter="[16, 16]" style="margin-top: 24px">
      <a-col :xs="24" :md="12">
        <a-card title="实时座位利用率" :bordered="false">
          <base-chart :options="usageChartOption" height="300px" />
        </a-card>
      </a-col>
      <a-col :xs="24" :md="12">
        <a-card title="近7日预约趋势" :bordered="false">
          <base-chart :options="trendChartOption" height="300px" />
        </a-card>
      </a-col>
    </a-row>

    <a-card title="快捷操作" style="margin-top: 24px" :bordered="false">
      <a-space size="large" wrap>
        <router-link to="/seat">
          <a-button type="primary" size="large">
            <template #icon><video-camera-outlined /></template>
            预约座位
          </a-button>
        </router-link>
        <a-button size="large" v-if="userInfo?.role !== 'guest'">
          <template #icon><history-outlined /></template>
          查看记录
        </a-button>
      </a-space>
    </a-card>

    <a-card title="近期动态" style="margin-top: 24px" :bordered="false">
      <a-list item-layout="horizontal" :data-source="activities">
        <template #renderItem="{ item }">
          <a-list-item>
            <a-list-item-meta
              :description="item.time"
            >
              <template #title>
                <a href="javascript:;">{{ item.title }}</a>
              </template>
              <template #avatar>
                <a-avatar :style="{ backgroundColor: item.color }">
                  <template #icon>
                    <component :is="item.icon" />
                  </template>
                </a-avatar>
              </template>
            </a-list-item-meta>
          </a-list-item>
        </template>
      </a-list>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, computed, onMounted } from 'vue'
import {
  UserOutlined,
  CheckCircleOutlined,
  CloseCircleOutlined,
  VideoCameraOutlined,
  HistoryOutlined,
  ClockCircleOutlined
} from '@ant-design/icons-vue'
import BaseChart from '../components/Chart/BaseChart.vue'
import { getDashboardStats } from '../api/stats'
import { useUserStore } from '../stores/user'

const userStore = useUserStore()
const userInfo = computed(() => userStore.userInfo)

const stats = ref({
  totalSeats: 0,
  available: 0,
  occupied: 0,
  maintenance: 0,
  trend: [] as number[]
})

onMounted(async () => {
  try {
    const data = await getDashboardStats()
    stats.value = (data as any).data || data
  } catch (error) {
    console.error(error)
  }
})

const usageChartOption = computed(() => ({
  tooltip: {
    trigger: 'item'
  },
  legend: {
    top: '5%',
    left: 'center'
  },
  series: [
    {
      name: '座位状态',
      type: 'pie',
      radius: ['40%', '70%'],
      avoidLabelOverlap: false,
      itemStyle: {
        borderRadius: 10,
        borderColor: '#fff',
        borderWidth: 2
      },
      label: {
        show: false,
        position: 'center'
      },
      emphasis: {
        label: {
          show: true,
          fontSize: 40,
          fontWeight: 'bold'
        }
      },
      labelLine: {
        show: false
      },
      data: [
        { value: stats.value.available, name: '空闲', itemStyle: { color: '#3f8600' } },
        { value: stats.value.occupied, name: '使用中', itemStyle: { color: '#cf1322' } },
        { value: stats.value.maintenance, name: '维修中', itemStyle: { color: '#faad14' } }
      ]
    }
  ]
}))

const trendChartOption = computed(() => ({
  tooltip: {
    trigger: 'axis'
  },
  grid: {
    left: '3%',
    right: '4%',
    bottom: '3%',
    containLabel: true
  },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: {
    type: 'value'
  },
  series: [
    {
      name: '预约人数',
      type: 'line',
      stack: 'Total',
      smooth: true,
      lineStyle: { width: 3, color: '#1890ff' },
      areaStyle: {
        color: {
          type: 'linear',
          x: 0, y: 0, x2: 0, y2: 1,
          colorStops: [
            { offset: 0, color: 'rgba(24,144,255,0.3)' },
            { offset: 1, color: 'rgba(24,144,255,0.01)' }
          ]
        }
      },
      data: stats.value.trend
    }
  ]
}))

const activities = ref([
  {
    title: '用户 "admin" 预约了座位 A-01',
    time: '10分钟前',
    icon: CheckCircleOutlined,
    color: '#87d068'
  },
  {
    title: '用户 "student1" 释放了座位 B-05',
    time: '30分钟前',
    icon: ClockCircleOutlined,
    color: '#108ee9'
  },
  {
    title: '系统维护计划',
    time: '2小时前',
    icon: HistoryOutlined,
    color: '#faad14'
  }
])
</script>

<style scoped>
.dashboard-container {
  padding: 0;
}
</style>
