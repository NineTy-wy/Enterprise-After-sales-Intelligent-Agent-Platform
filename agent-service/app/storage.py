from io import BytesIO

from .config import Settings


def read_object(settings: Settings, object_key: str) -> bytes:
    """从共享目录或 MinIO 读取已上传文件。"""
    from pathlib import Path

    storage_root = Path(settings.storage_root).resolve()
    local_path = Path(object_key)
    if local_path.is_absolute():
        local_path = local_path.resolve()
    else:
        local_path = (storage_root / object_key).resolve()
    if not local_path.is_relative_to(storage_root):
        raise ValueError("非法存储路径")
    if local_path.exists():
        return local_path.read_bytes()

    if settings.storage_mode == "minio":
        from minio import Minio

        endpoint = settings.minio_endpoint.replace("http://", "").replace("https://", "")
        client = Minio(
            endpoint,
            access_key=settings.minio_access_key,
            secret_key=settings.minio_secret_key,
            secure=settings.minio_endpoint.startswith("https://"),
        )
        response = client.get_object(settings.minio_bucket, object_key)
        try:
            return response.read()
        finally:
            response.close()
            response.release_conn()

    raise FileNotFoundError(f"storage object not found: {object_key}")


def as_stream(data: bytes) -> BytesIO:
    return BytesIO(data)
