"""
Kafka消费者 - 处理文档解析和向量化任务
"""
import json
import logging
from kafka import KafkaConsumer
from sqlalchemy import create_engine, text
from app.config import get_settings
from app.services.document_parser import DocumentParser
from app.services.vector_service import VectorService
from app.utils.minio_client import download_file

logging.basicConfig(level=logging.INFO)
logger = logging.getLogger(__name__)

settings = get_settings()


class DocumentConsumer:
    """文档处理消费者"""

    def __init__(self):
        self.consumer = KafkaConsumer(
            settings.kafka_topic_document_parse,
            bootstrap_servers=settings.kafka_bootstrap_servers.split(","),
            value_deserializer=lambda m: json.loads(m.decode("utf-8")),
            group_id="gzdoc-ai-document-parser",
            auto_offset_reset="earliest",
        )
        self.parser = DocumentParser()
        self.vector_service = VectorService()
        self.db_engine = create_engine(settings.database_url)

    def start(self):
        """启动消费者"""
        logger.info("Document consumer started")

        for message in self.consumer:
            try:
                data = message.value
                logger.info(f"Processing document: {data}")

                document_id = data["documentId"]
                storage_path = data["storagePath"]
                file_type = data["fileType"]

                # 1. 下载文件
                file_data = download_file(storage_path)

                # 2. 解析文档
                content = self.parser.parse(file_data, file_type)

                # 3. 更新数据库 - 保存解析内容
                self._update_document_content(document_id, content, "completed")

                # 4. 向量化
                tenant_id = self._get_tenant_id(document_id)
                vector_id = self.vector_service.store_document_vector(
                    document_id=document_id,
                    tenant_id=tenant_id,
                    content=content,
                    file_name=storage_path.split("/")[-1],
                )

                # 5. 更新向量化状态
                self._update_vectorize_status(document_id, vector_id)

                logger.info(f"Document {document_id} processed successfully")

            except Exception as e:
                logger.error(f"Error processing document: {str(e)}")
                if "document_id" in locals():
                    self._update_document_content(document_id, "", "failed")

    def _update_document_content(self, document_id: int, content: str, status: str):
        """更新文档内容和状态"""
        with self.db_engine.connect() as conn:
            conn.execute(
                text(
                    "UPDATE t_document SET content = :content, status = :status, update_time = NOW() "
                    "WHERE id = :id"
                ),
                {"content": content, "status": status, "id": document_id},
            )
            conn.commit()

    def _update_vectorize_status(self, document_id: int, vector_id: str):
        """更新向量化状态"""
        with self.db_engine.connect() as conn:
            conn.execute(
                text(
                    "UPDATE t_document SET vectorized = 1, vector_id = :vector_id, update_time = NOW() "
                    "WHERE id = :id"
                ),
                {"vector_id": vector_id, "id": document_id},
            )
            conn.commit()

    def _get_tenant_id(self, document_id: int) -> int:
        """获取租户ID"""
        with self.db_engine.connect() as conn:
            result = conn.execute(
                text("SELECT tenant_id FROM t_document WHERE id = :id"),
                {"id": document_id},
            )
            row = result.fetchone()
            return row[0] if row else 0


if __name__ == "__main__":
    consumer = DocumentConsumer()
    consumer.start()
