<template>
  <div class="seat-map-container" ref="containerRef">
    <div class="map-controls">
      <a-space>
        <div class="legend-item">
          <span class="dot available"></span> 空闲
        </div>
        <div class="legend-item">
          <span class="dot occupied"></span> 占用
        </div>
        <div class="legend-item">
          <span class="dot selected"></span> 已选
        </div>
      </a-space>
    </div>

    <svg
      ref="svgRef"
      :width="width"
      :height="height"
      class="seat-svg"
      @mousedown="handleMouseDown"
      @mousemove="handleMouseMove"
      @mouseup="handleMouseUp"
      @wheel="handleWheel"
    >
      <g :transform="`translate(${transform.x}, ${transform.y}) scale(${transform.scale})`">
        <!-- 背景层：教室轮廓 -->
        <rect
          x="0"
          y="0"
          :width="mapWidth"
          :height="mapHeight"
          fill="#f8fafc"
          stroke="#e2e8f0"
          stroke-width="2"
        />

        <!-- 装饰物：讲台 -->
        <rect
          :x="mapWidth / 2 - 60"
          y="20"
          width="120"
          height="40"
          fill="#cbd5e1"
          rx="4"
        />
        <text
          :x="mapWidth / 2"
          y="45"
          text-anchor="middle"
          fill="#64748b"
          font-size="14"
        >讲台</text>

        <!-- 座位层 -->
        <g v-for="seat in seats" :key="seat.id">
          <!-- 座位图标 -->
          <rect
            :x="seat.x"
            :y="seat.y"
            :width="seatSize"
            :height="seatSize"
            :rx="8"
            :fill="getSeatColor(seat)"
            :stroke="selectedSeatId === seat.id ? '#3b82f6' : 'none'"
            :stroke-width="selectedSeatId === seat.id ? 3 : 0"
            class="seat-rect"
            @click="handleSeatClick(seat)"
          />
          <!-- 座位号 -->
          <text
            :x="seat.x + seatSize / 2"
            :y="seat.y + seatSize / 2 + 5"
            text-anchor="middle"
            fill="#fff"
            font-size="12"
            pointer-events="none"
          >{{ seat.label }}</text>
        </g>
      </g>
    </svg>
    
    <div class="zoom-controls">
      <a-button-group>
        <a-button @click="zoomIn">
          <template #icon><plus-outlined /></template>
        </a-button>
        <a-button @click="resetZoom">
          <template #icon><reload-outlined /></template>
        </a-button>
        <a-button @click="zoomOut">
          <template #icon><minus-outlined /></template>
        </a-button>
      </a-button-group>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted, reactive } from 'vue'
import { PlusOutlined, MinusOutlined, ReloadOutlined } from '@ant-design/icons-vue'

export interface Seat {
  id: number
  label: string
  x: number
  y: number
  status: 'available' | 'occupied' | 'maintenance'
  type?: 'window' | 'power' | 'normal'
}

const props = defineProps<{
  seats: Seat[]
}>()

const emit = defineEmits(['select'])

const width = ref(800)
const height = ref(600)
const mapWidth = 800
const mapHeight = 600
const seatSize = 40

const selectedSeatId = ref<number | null>(null)

const transform = reactive({
  x: 0,
  y: 0,
  scale: 1
})

// 拖拽状态
let isDragging = false
let startX = 0
let startY = 0

const getSeatColor = (seat: Seat) => {
  if (seat.id === selectedSeatId.value) return '#3b82f6' // 选中蓝
  switch (seat.status) {
    case 'available': return '#10b981' // 绿色
    case 'occupied': return '#ef4444' // 红色
    case 'maintenance': return '#94a3b8' // 灰色
    default: return '#cbd5e1'
  }
}

const handleSeatClick = (seat: Seat) => {
  if (seat.status !== 'available') return
  selectedSeatId.value = seat.id
  emit('select', seat)
}

// 缩放/平移逻辑
const handleWheel = (e: WheelEvent) => {
  e.preventDefault()
  const scaleBy = 1.1
  const oldScale = transform.scale
  let newScale = e.deltaY > 0 ? oldScale / scaleBy : oldScale * scaleBy
  
  // 限制缩放范围
  newScale = Math.min(Math.max(0.5, newScale), 3)
  
  transform.scale = newScale
}

const handleMouseDown = (e: MouseEvent) => {
  isDragging = true
  startX = e.clientX - transform.x
  startY = e.clientY - transform.y
}

const handleMouseMove = (e: MouseEvent) => {
  if (!isDragging) return
  transform.x = e.clientX - startX
  transform.y = e.clientY - startY
}

const handleMouseUp = () => {
  isDragging = false
}

const zoomIn = () => {
  transform.scale = Math.min(transform.scale * 1.2, 3)
}

const zoomOut = () => {
  transform.scale = Math.max(transform.scale / 1.2, 0.5)
}

const resetZoom = () => {
  transform.scale = 1
  transform.x = 0
  transform.y = 0
}

onMounted(() => {
  // 自适应容器宽度（可选）
  const container = document.querySelector('.seat-map-container')
  if (container) {
    width.value = container.clientWidth
    height.value = 500
  }
})
</script>

<style scoped>
.seat-map-container {
  width: 100%;
  height: 500px;
  background: #fff;
  border-radius: 8px;
  overflow: hidden;
  position: relative;
  border: 1px solid #e2e8f0;
}

.map-controls {
  position: absolute;
  top: 16px;
  left: 16px;
  z-index: 10;
  background: rgba(255, 255, 255, 0.9);
  padding: 8px 12px;
  border-radius: 6px;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.zoom-controls {
  position: absolute;
  bottom: 16px;
  right: 16px;
  z-index: 10;
  box-shadow: 0 2px 4px rgba(0,0,0,0.1);
}

.legend-item {
  display: flex;
  align-items: center;
  font-size: 12px;
  color: #64748b;
}

.dot {
  width: 10px;
  height: 10px;
  border-radius: 50%;
  margin-right: 6px;
}

.dot.available { background: #10b981; }
.dot.occupied { background: #ef4444; }
.dot.selected { background: #3b82f6; }

.seat-rect {
  cursor: pointer;
  transition: fill 0.2s;
}

.seat-rect:hover {
  filter: brightness(0.9);
}
</style>
