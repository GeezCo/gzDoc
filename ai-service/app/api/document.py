from fastapi import APIRouter, HTTPException
from app.models.schemas import (
    DocumentParseRequest,
    DocumentParseResponse,
    VectorizeRequest,
    VectorizeResponse,
)
from app.services.document_parser import DocumentParser
from app.services.vector_service import VectorService
from app.utils.minio_client import download_file

router = APIRouter()
document_parser = DocumentParser()
vector_service = VectorService()


@router.post("/parse", response_model=DocumentParseResponse)
async def parse_document(request: DocumentParseRequest):
    """解析文档"""
    try:
        # 从MinIO下载文件
        file_data = download_file(request.storage_path)

        # 解析文档
        content = document_parser.parse(file_data, request.file_type)

        return DocumentParseResponse(
            document_id=request.document_id,
            content=content,
            status="completed",
        )

    except Exception:
        return DocumentParseResponse(
            document_id=request.document_id,
            content="",
            status="failed",
        )


@router.post("/vectorize", response_model=VectorizeResponse)
async def vectorize_document(request: VectorizeRequest):
    """向量化文档"""
    try:
        # 存储文档向量
        vector_id = vector_service.store_document_vector(
            document_id=request.document_id,
            tenant_id=request.tenant_id,
            content=request.content,
            file_name=f"document_{request.document_id}",
        )

        return VectorizeResponse(
            document_id=request.document_id,
            vector_id=vector_id,
            status="completed",
        )

    except Exception as e:
        raise HTTPException(status_code=500, detail=f"向量化失败: {str(e)}")
