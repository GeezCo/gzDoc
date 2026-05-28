from typing import List, Dict, Any
from openai import OpenAI
from app.config import get_settings
from app.utils.weaviate_client import get_weaviate_client

settings = get_settings()


class VectorService:
    """向量化服务"""

    def __init__(self):
        self.openai_client = OpenAI(api_key=settings.openai_api_key)

    def create_embedding(self, text: str) -> List[float]:
        """创建文本向量"""
        response = self.openai_client.embeddings.create(
            model=settings.embedding_model,
            input=text,
        )
        return response.data[0].embedding

    def store_document_vector(
        self,
        document_id: int,
        tenant_id: int,
        content: str,
        file_name: str,
    ) -> str:
        """存储文档向量到Weaviate"""
        client = get_weaviate_client()

        try:
            collection = client.collections.get("Document")

            # 插入文档
            result = collection.data.insert(
                properties={
                    "documentId": document_id,
                    "tenantId": tenant_id,
                    "content": content,
                    "fileName": file_name,
                }
            )

            return str(result)
        finally:
            client.close()

    def search_similar_documents(
        self,
        query: str,
        tenant_id: int,
        limit: int = 5,
    ) -> List[Dict[str, Any]]:
        """搜索相似文档"""
        client = get_weaviate_client()

        try:
            collection = client.collections.get("Document")

            # 向量搜索
            response = collection.query.near_text(
                query=query,
                limit=limit,
                filters={"path": ["tenantId"], "operator": "Equal", "valueInt": tenant_id},
                return_properties=["documentId", "content", "fileName"],
                return_metadata=["distance"],
            )

            results = []
            for obj in response.objects:
                results.append({
                    "document_id": obj.properties["documentId"],
                    "document_name": obj.properties["fileName"],
                    "content": obj.properties["content"],
                    "score": 1 - obj.metadata.distance,  # 转换为相似度分数
                })

            return results
        finally:
            client.close()
