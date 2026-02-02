<template>
  <div class="config-container">
    <a-card title="系统配置" :bordered="false">
      <a-table 
        :columns="columns" 
        :data-source="configs" 
        :loading="loading"
        row-key="id"
        :pagination="false"
      >
        <template #bodyCell="{ column, record }">
          <template v-if="column.key === 'configValue'">
            <a-input-number 
              v-if="record.configKey === 'violation_time' || record.configKey === 'min_credit_score'"
              v-model:value="record.configValue" 
              style="width: 100%"
              :min="0"
            />
            <a-switch 
              v-else-if="record.configKey === 'message_square_enabled'"
              :checked="record.configValue === 'true'"
              @update:checked="(val: boolean) => record.configValue = String(val)"
            />
            <a-input v-else v-model:value="record.configValue" />
          </template>
          <template v-else-if="column.key === 'action'">
            <a-button type="link" @click="handleUpdate(record)">保存</a-button>
          </template>
        </template>
      </a-table>
    </a-card>
  </div>
</template>

<script setup lang="ts">
import { ref, onMounted } from 'vue'
import { message } from 'ant-design-vue'
import { getConfigs, updateConfig, type SysConfig } from '../../api/config'

const loading = ref(false)
const configs = ref<SysConfig[]>([])

const columns = [
  { title: '配置名称', dataIndex: 'configName', key: 'configName' },
  { title: '配置项', dataIndex: 'configKey', key: 'configKey' },
  { title: '配置值', dataIndex: 'configValue', key: 'configValue' },
  { title: '操作', key: 'action' }
]

const fetchConfigs = async () => {
  loading.value = true
  try {
    const res = await getConfigs()
    configs.value = (res as any).data || res
  } catch (error) {
    message.error('获取配置失败')
  } finally {
    loading.value = false
  }
}

const handleUpdate = async (record: SysConfig) => {
  try {
    // Ensure value is string for backend
    const data = { ...record, configValue: String(record.configValue) }
    await updateConfig(data)
    message.success('配置更新成功')
    fetchConfigs()
  } catch (error) {
    message.error('更新失败')
  }
}

onMounted(() => {
  fetchConfigs()
})
</script>

<style scoped>
.config-container {
  padding: 24px;
}
</style>
