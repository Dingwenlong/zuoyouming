import { io, Socket } from 'socket.io-client'

class WebSocketService {
  private socket: Socket | null = null
  private listeners: Map<string, Function[]> = new Map()

  connect(url: string = 'http://localhost:3000') {
    if (this.socket) return

    this.socket = io(url, {
      transports: ['websocket'],
      autoConnect: true,
      reconnection: true
    })

    this.socket.on('connect', () => {
      console.log('WebSocket connected')
    })

    this.socket.on('disconnect', () => {
      console.log('WebSocket disconnected')
    })

    this.socket.on('seat_update', (data: any) => {
      this.notify('seat_update', data)
    })
  }

  disconnect() {
    if (this.socket) {
      this.socket.disconnect()
      this.socket = null
    }
  }

  on(event: string, callback: Function) {
    if (!this.listeners.has(event)) {
      this.listeners.set(event, [])
    }
    this.listeners.get(event)?.push(callback)
  }

  off(event: string, callback: Function) {
    if (!this.listeners.has(event)) return
    const callbacks = this.listeners.get(event)
    const index = callbacks?.indexOf(callback)
    if (index !== undefined && index > -1) {
      callbacks?.splice(index, 1)
    }
  }

  private notify(event: string, data: any) {
    const callbacks = this.listeners.get(event)
    callbacks?.forEach(cb => cb(data))
  }
}

export const wsService = new WebSocketService()
