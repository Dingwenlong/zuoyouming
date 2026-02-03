<template>
  <div class="qr-scanner-container">
    <div id="reader" width="100%"></div>
    <div class="scan-status" v-if="error">{{ error }}</div>
  </div>
</template>

<script setup lang="ts">
import { onMounted, onUnmounted, ref } from 'vue'
import { Html5QrcodeScanner } from 'html5-qrcode'

const props = defineProps<{
  allowFile?: boolean
}>()

const emit = defineEmits(['scan', 'error'])
const error = ref('')
let scanner: Html5QrcodeScanner | null = null

onMounted(() => {
  scanner = new Html5QrcodeScanner(
    "reader",
    { fps: 10, qrbox: { width: 250, height: 250 } },
    /* verbose= */ false
  )
  
  scanner.render(onScanSuccess, onScanFailure)
})

onUnmounted(() => {
  if (scanner) {
    scanner.clear().catch(error => {
      console.error("Failed to clear html5-qrcode scanner. ", error)
    })
  }
})

const onScanSuccess = (decodedText: string, _decodedResult: any) => {
  emit('scan', decodedText)
  // Optional: Stop scanning after success
  // if (scanner) scanner.clear()
}

const onScanFailure = (_errorMessage: string) => {
  // handle scan failure, usually better to ignore and keep scanning.
  // for example:
  // console.warn(`Code scan error = ${error}`)
}
</script>

<style scoped>
.qr-scanner-container {
  width: 100%;
  max-width: 500px;
  margin: 0 auto;
}

/* 隐藏文件选择按钮（如果 props.allowFile 为 false） */
:deep(#reader__dashboard_section_swaplink) {
  display: v-bind(allowFile ? 'inline-block' : 'none') !important;
}

:deep(input[type="file"]) {
  display: v-bind(allowFile ? 'block' : 'none') !important;
}
.scan-status {
  margin-top: 10px;
  color: red;
  text-align: center;
}
</style>
