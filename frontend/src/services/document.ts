import request from '@/utils/request'
import type { Document } from '@/types'

export const documentApi = {
  // 上传文档
  upload: (file: File) => {
    const formData = new FormData()
    formData.append('file', file)
    return request.post<any, Document>('/document/upload', formData, {
      headers: {
        'Content-Type': 'multipart/form-data',
      },
    })
  },

  // 分页查询文档
  page: (pageNum: number, pageSize: number) => {
    return request.get<any, { records: Document[]; total: number; current: number; size: number }>(
      '/document/page',
      {
        params: { pageNum, pageSize },
      }
    )
  },

  // 获取文档详情
  getById: (id: number) => {
    return request.get<any, Document>(`/document/${id}`)
  },

  // 删除文档
  delete: (id: number) => {
    return request.delete(`/document/${id}`)
  },
}
