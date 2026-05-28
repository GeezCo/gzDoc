from minio import Minio
from app.config import get_settings

settings = get_settings()


def get_minio_client() -> Minio:
    """获取MinIO客户端"""
    return Minio(
        settings.minio_endpoint,
        access_key=settings.minio_access_key,
        secret_key=settings.minio_secret_key,
        secure=settings.minio_secure,
    )


def download_file(storage_path: str) -> bytes:
    """从MinIO下载文件"""
    client = get_minio_client()
    response = client.get_object(settings.minio_bucket, storage_path)
    data = response.read()
    response.close()
    response.release_conn()
    return data
