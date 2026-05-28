import { useState, useEffect } from 'react'
import { Table, Button, Upload, message, Popconfirm, Tag, Space } from 'antd'
import { UploadOutlined, DeleteOutlined } from '@ant-design/icons'
import type { UploadProps, TableColumnsType } from 'antd'
import { documentApi } from '@/services/document'
import type { Document } from '@/types'
import dayjs from 'dayjs'

const DocumentList = () => {
  const [documents, setDocuments] = useState<Document[]>([])
  const [loading, setLoading] = useState(false)
  const [pagination, setPagination] = useState({
    current: 1,
    pageSize: 10,
    total: 0,
  })

  const fetchDocuments = async (page: number = 1, pageSize: number = 10) => {
    setLoading(true)
    try {
      const response = await documentApi.page(page, pageSize)
      setDocuments(response.records)
      setPagination({
        current: response.current,
        pageSize: response.size,
        total: response.total,
      })
    } catch (error) {
      console.error('获取文档列表失败:', error)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    fetchDocuments()
  }, [])

  const handleDelete = async (id: number) => {
    try {
      await documentApi.delete(id)
      message.success('删除成功')
      fetchDocuments(pagination.current, pagination.pageSize)
    } catch (error) {
      console.error('删除失败:', error)
    }
  }

  const uploadProps: UploadProps = {
    name: 'file',
    showUploadList: false,
    customRequest: async ({ file, onSuccess, onError }) => {
      try {
        await documentApi.upload(file as File)
        message.success('上传成功')
        onSuccess?.(null)
        fetchDocuments(pagination.current, pagination.pageSize)
      } catch (error) {
        console.error('上传失败:', error)
        onError?.(error as Error)
      }
    },
  }

  const getStatusTag = (status: string) => {
    const statusMap: Record<string, { color: string; text: string }> = {
      pending: { color: 'default', text: '待处理' },
      processing: { color: 'processing', text: '处理中' },
      completed: { color: 'success', text: '已完成' },
      failed: { color: 'error', text: '失败' },
    }
    const config = statusMap[status] || statusMap.pending
    return <Tag color={config.color}>{config.text}</Tag>
  }

  const columns: TableColumnsType<Document> = [
    {
      title: '文档名称',
      dataIndex: 'name',
      key: 'name',
    },
    {
      title: '文件类型',
      dataIndex: 'fileType',
      key: 'fileType',
    },
    {
      title: '文件大小',
      dataIndex: 'fileSize',
      key: 'fileSize',
      render: (size: number) => `${(size / 1024).toFixed(2)} KB`,
    },
    {
      title: '状态',
      dataIndex: 'status',
      key: 'status',
      render: (status: string) => getStatusTag(status),
    },
    {
      title: '向量化',
      dataIndex: 'vectorized',
      key: 'vectorized',
      render: (vectorized: number) => (
        <Tag color={vectorized === 1 ? 'success' : 'default'}>
          {vectorized === 1 ? '已向量化' : '未向量化'}
        </Tag>
      ),
    },
    {
      title: '上传时间',
      dataIndex: 'createTime',
      key: 'createTime',
      render: (time: string) => dayjs(time).format('YYYY-MM-DD HH:mm:ss'),
    },
    {
      title: '操作',
      key: 'action',
      render: (_, record) => (
        <Space>
          <Popconfirm
            title="确定要删除这个文档吗？"
            onConfirm={() => handleDelete(record.id)}
            okText="确定"
            cancelText="取消"
          >
            <Button type="link" danger icon={<DeleteOutlined />}>
              删除
            </Button>
          </Popconfirm>
        </Space>
      ),
    },
  ]

  return (
    <div>
      <div style={{ marginBottom: 16, display: 'flex', justifyContent: 'space-between' }}>
        <h1>文档管理</h1>
        <Upload {...uploadProps}>
          <Button type="primary" icon={<UploadOutlined />}>
            上传文档
          </Button>
        </Upload>
      </div>
      <Table
        columns={columns}
        dataSource={documents}
        rowKey="id"
        loading={loading}
        pagination={{
          ...pagination,
          onChange: (page, pageSize) => fetchDocuments(page, pageSize),
        }}
      />
    </div>
  )
}

export default DocumentList
