import { useState } from 'react'
import { Card, Input, Button, List, Tag, Space, Spin } from 'antd'
import { SendOutlined } from '@ant-design/icons'
import { qaApi } from '@/services/qa'
import type { QaResponse } from '@/types'
import './QA.css'

const { TextArea } = Input

interface Message {
  type: 'question' | 'answer'
  content: string
  relatedDocuments?: QaResponse['relatedDocuments']
  responseTime?: number
}

const QA = () => {
  const [question, setQuestion] = useState('')
  const [messages, setMessages] = useState<Message[]>([])
  const [loading, setLoading] = useState(false)
  const [sessionId, setSessionId] = useState<string>()

  const handleAsk = async () => {
    if (!question.trim()) {
      return
    }

    const userMessage: Message = {
      type: 'question',
      content: question,
    }

    setMessages((prev) => [...prev, userMessage])
    setQuestion('')
    setLoading(true)

    try {
      const response = await qaApi.ask({
        question: question.trim(),
        sessionId,
        maxDocs: 5,
      })

      setSessionId(response.sessionId)

      const aiMessage: Message = {
        type: 'answer',
        content: response.answer,
        relatedDocuments: response.relatedDocuments,
        responseTime: response.responseTime,
      }

      setMessages((prev) => [...prev, aiMessage])
    } catch (error) {
      console.error('提问失败:', error)
    } finally {
      setLoading(false)
    }
  }

  return (
    <div className="qa-container">
      <h1 style={{ marginBottom: 24 }}>智能问答</h1>
      <Card className="qa-card">
        <div className="messages-container">
          {messages.length === 0 ? (
            <div className="empty-state">
              <p>请输入您的问题，我会基于已上传的文档为您解答</p>
            </div>
          ) : (
            <List
              dataSource={messages}
              renderItem={(item, index) => (
                <div
                  key={index}
                  className={`message ${item.type === 'question' ? 'question' : 'answer'}`}
                >
                  <div className="message-content">
                    <div className="message-text">{item.content}</div>
                    {item.type === 'answer' && item.relatedDocuments && item.relatedDocuments.length > 0 && (
                      <div className="related-docs">
                        <div className="related-docs-title">相关文档：</div>
                        <Space wrap>
                          {item.relatedDocuments.map((doc) => (
                            <Tag key={doc.documentId} color="blue">
                              {doc.documentName} (相关度: {(doc.score * 100).toFixed(1)}%)
                            </Tag>
                          ))}
                        </Space>
                      </div>
                    )}
                    {item.type === 'answer' && item.responseTime && (
                      <div className="response-time">响应时间: {item.responseTime}ms</div>
                    )}
                  </div>
                </div>
              )}
            />
          )}
          {loading && (
            <div className="loading-container">
              <Spin tip="正在思考..." />
            </div>
          )}
        </div>
        <div className="input-container">
          <TextArea
            value={question}
            onChange={(e) => setQuestion(e.target.value)}
            placeholder="请输入您的问题..."
            autoSize={{ minRows: 2, maxRows: 6 }}
            onPressEnter={(e) => {
              if (!e.shiftKey) {
                e.preventDefault()
                handleAsk()
              }
            }}
          />
          <Button
            type="primary"
            icon={<SendOutlined />}
            onClick={handleAsk}
            loading={loading}
            disabled={!question.trim()}
          >
            发送
          </Button>
        </div>
      </Card>
    </div>
  )
}

export default QA
