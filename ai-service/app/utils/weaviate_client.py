import weaviate
from weaviate.classes.init import Auth
from app.config import get_settings

settings = get_settings()


def get_weaviate_client():
    """获取Weaviate客户端"""
    if settings.weaviate_api_key:
        client = weaviate.connect_to_wcs(
            cluster_url=settings.weaviate_url,
            auth_credentials=Auth.api_key(settings.weaviate_api_key),
        )
    else:
        client = weaviate.connect_to_local(
            host=settings.weaviate_url.replace("http://", "").replace("https://", ""),
        )
    return client


def init_weaviate_schema():
    """初始化Weaviate Schema"""
    client = get_weaviate_client()

    try:
        # 检查集合是否存在
        collections = client.collections.list_all()
        if "Document" not in [c.name for c in collections]:
            # 创建Document集合
            client.collections.create(
                name="Document",
                properties=[
                    {
                        "name": "documentId",
                        "dataType": ["int"],
                        "description": "文档ID",
                    },
                    {
                        "name": "tenantId",
                        "dataType": ["int"],
                        "description": "租户ID",
                    },
                    {
                        "name": "content",
                        "dataType": ["text"],
                        "description": "文档内容",
                    },
                    {
                        "name": "fileName",
                        "dataType": ["string"],
                        "description": "文件名",
                    },
                ],
                vectorizer_config=weaviate.classes.config.Configure.Vectorizer.text2vec_openai(
                    model=settings.embedding_model,
                ),
            )
            print("Weaviate schema initialized successfully")
    finally:
        client.close()
