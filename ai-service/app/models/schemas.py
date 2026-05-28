from pydantic import BaseModel, Field
from typing import List, Optional


class QARequest(BaseModel):
    question: str = Field(..., description="用户问题")
    tenant_id: int = Field(..., description="租户ID")
    session_id: Optional[str] = Field(None, description="会话ID")
    max_docs: int = Field(5, description="最大返回文档数", ge=1, le=10)


class RelatedDocument(BaseModel):
    document_id: int = Field(..., description="文档ID")
    document_name: str = Field(..., description="文档名称")
    score: float = Field(..., description="相关度分数")
    snippet: str = Field(..., description="相关片段")


class QAResponse(BaseModel):
    question: str = Field(..., description="问题")
    answer: str = Field(..., description="答案")
    related_documents: List[RelatedDocument] = Field(default_factory=list, description="相关文档")
    session_id: str = Field(..., description="会话ID")


class DocumentParseRequest(BaseModel):
    document_id: int = Field(..., description="文档ID")
    storage_path: str = Field(..., description="存储路径")
    file_type: str = Field(..., description="文件类型")


class DocumentParseResponse(BaseModel):
    document_id: int = Field(..., description="文档ID")
    content: str = Field(..., description="解析内容")
    status: str = Field(..., description="状态")


class VectorizeRequest(BaseModel):
    document_id: int = Field(..., description="文档ID")
    content: str = Field(..., description="文档内容")
    tenant_id: int = Field(..., description="租户ID")


class VectorizeResponse(BaseModel):
    document_id: int = Field(..., description="文档ID")
    vector_id: str = Field(..., description="向量ID")
    status: str = Field(..., description="状态")
