from pydantic_settings import BaseSettings
from functools import lru_cache


class Settings(BaseSettings):
    # Application
    app_name: str = "gzdoc-ai-service"
    app_version: str = "1.0.0"
    debug: bool = True
    host: str = "0.0.0.0"
    port: int = 8000

    # Database
    database_url: str

    # Weaviate
    weaviate_url: str
    weaviate_api_key: str = ""

    # OpenAI
    openai_api_key: str
    openai_model: str = "gpt-4"
    embedding_model: str = "text-embedding-3-small"

    # Kafka
    kafka_bootstrap_servers: str
    kafka_topic_document_parse: str = "document-parse"
    kafka_topic_document_vectorize: str = "document-vectorize"

    # Redis
    redis_host: str = "localhost"
    redis_port: int = 6379
    redis_db: int = 0
    redis_password: str = ""

    # MinIO
    minio_endpoint: str
    minio_access_key: str
    minio_secret_key: str
    minio_bucket: str = "gzdoc"
    minio_secure: bool = False

    class Config:
        env_file = ".env"
        case_sensitive = False


@lru_cache()
def get_settings() -> Settings:
    return Settings()
