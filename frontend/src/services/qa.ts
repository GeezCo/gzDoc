import request from '@/utils/request'
import type { QaRequest, QaResponse } from '@/types'

export const qaApi = {
  // 提问
  ask: (data: QaRequest) => {
    return request.post<any, QaResponse>('/qa/ask', data)
  },

  // 获取问答历史
  getHistory: (limit: number = 10) => {
    return request.get<any, any[]>('/qa/history', {
      params: { limit },
    })
  },
}
