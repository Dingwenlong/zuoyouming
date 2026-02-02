import request from '../utils/request'

export interface SysConfig {
  id: number
  configKey: string
  configValue: string
  configName: string
  updateTime?: string
}

export function getConfigs() {
  return request<SysConfig[]>({
    url: '/configs/list',
    method: 'get'
  })
}

export function updateConfig(data: SysConfig) {
  return request<boolean>({
    url: '/configs',
    method: 'put',
    data
  })
}
