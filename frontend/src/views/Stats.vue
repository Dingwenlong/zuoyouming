<template>
  <div class="stats-container">
    <a-page-header
      title="数据统计分析"
      sub-title="查看座位使用热力图、违规趋势及区域拥堵情况"
    />
    
    <a-row :gutter="[24, 24]">
      <!-- 区域拥堵度 (柱状图) -->
      <a-col :xs="24" :lg="12">
        <a-card title="区域拥堵度分析" :bordered="false">
          <base-chart :options="congestionOption" height="350px" />
        </a-card>
      </a-col>

      <!-- 违规趋势 (折线图) -->
      <a-col :xs="24" :lg="12">
        <a-card title="违规次数统计 (近7天)" :bordered="false">
          <base-chart :options="violationOption" height="350px" />
        </a-card>
      </a-col>

      <!-- 座位热力图 -->
      <a-col :span="24">
        <a-card title="图书馆座位使用热力图" :bordered="false">
          <base-chart :options="heatmapOption" height="500px" />
        </a-card>
      </a-col>
    </a-row>
  </div>
</template>

<script setup lang="ts">
import { computed, ref, onMounted } from 'vue'
import BaseChart from '../components/Chart/BaseChart.vue'
import { getHeatmapData } from '../api/stats'

const hours = ['8:00', '10:00', '12:00', '14:00', '16:00', '18:00', '20:00', '22:00']
const days = ['A区', 'B区', 'C区', 'D区', 'E区']

const heatmapData = ref([])

onMounted(async () => {
  try {
    const data = await getHeatmapData()
    heatmapData.value = (data as any).data || data
  } catch (error) {
    console.error(error)
  }
})

// 1. 区域拥堵度配置
const congestionOption = computed(() => ({
  tooltip: { trigger: 'axis', axisPointer: { type: 'shadow' } },
  grid: { left: '3%', right: '4%', bottom: '3%', containLabel: true },
  xAxis: {
    type: 'category',
    data: ['A区(自习)', 'B区(阅览)', 'C区(电子)', 'D区(研讨)', 'E区(休闲)'],
    axisTick: { alignWithLabel: true }
  },
  yAxis: { type: 'value', name: '占用率(%)', max: 100 },
  series: [
    {
      name: '占用率',
      type: 'bar',
      barWidth: '60%',
      data: [85, 92, 45, 70, 30],
      itemStyle: {
        color: (params: any) => {
          const val = params.value
          if (val > 90) return '#cf1322' // 极度拥堵
          if (val > 70) return '#faad14' // 拥堵
          return '#3f8600' // 舒适
        }
      }
    }
  ]
}))

// 2. 违规趋势配置
const violationOption = computed(() => ({
  tooltip: { trigger: 'axis' },
  legend: { data: ['违约未签到', '超时未离座'] },
  xAxis: {
    type: 'category',
    boundaryGap: false,
    data: ['周一', '周二', '周三', '周四', '周五', '周六', '周日']
  },
  yAxis: { type: 'value' },
  series: [
    {
      name: '违约未签到',
      type: 'line',
      smooth: true,
      data: [12, 5, 8, 15, 6, 4, 10],
      itemStyle: { color: '#ff4d4f' }
    },
    {
      name: '超时未离座',
      type: 'line',
      smooth: true,
      data: [3, 2, 5, 1, 4, 8, 2],
      itemStyle: { color: '#fa8c16' }
    }
  ]
}))

const heatmapOption = computed(() => ({
  tooltip: { position: 'top' },
  grid: { height: '50%', top: '10%' },
  xAxis: {
    type: 'category',
    data: hours,
    splitArea: { show: true }
  },
  yAxis: {
    type: 'category',
    data: days,
    splitArea: { show: true }
  },
  visualMap: {
    min: 0,
    max: 100,
    calculable: true,
    orient: 'horizontal',
    left: 'center',
    bottom: '15%',
    inRange: {
      color: ['#f0f9ff', '#bae6fd', '#0ea5e9', '#0369a1']
    }
  },
  series: [
    {
      name: '拥挤度',
      type: 'heatmap',
      data: heatmapData.value,
      label: { show: true },
      emphasis: {
        itemStyle: {
          shadowBlur: 10,
          shadowColor: 'rgba(0, 0, 0, 0.5)'
        }
      }
    }
  ]
}))
</script>

<style scoped>
.stats-container {
  padding: 24px;
}
</style>
