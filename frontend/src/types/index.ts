export interface User {
  id: number
  username: string
  nickname: string
  email: string
  avatar?: string
  role: string
  tenantId: number
}

export interface LoginRequest {
  username: string
  password: string
  tenantId?: number
}

export interface LoginResponse {
  accessToken: string
  refreshToken: string
  tokenType: string
  expiresIn: number
  userInfo: User
}

export interface Document {
  id: number
  name: string
  fileType: string
  fileSize: number
  status: 'pending' | 'processing' | 'completed' | 'failed'
  vectorized: number
  uploadUserId: number
  createTime: string
  updateTime: string
}

export interface QaRequest {
  question: string
  sessionId?: string
  maxDocs?: number
}

export interface QaResponse {
  question: string
  answer: string
  relatedDocuments: RelatedDocument[]
  responseTime: number
  sessionId: string
}

export interface RelatedDocument {
  documentId: number
  documentName: string
  score: number
  snippet: string
}

export interface ApiResponse<T = any> {
  code: number
  message: string
  data: T
  timestamp: string
}
