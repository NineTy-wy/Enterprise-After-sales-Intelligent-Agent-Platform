from functools import lru_cache

from pydantic_settings import BaseSettings, SettingsConfigDict


class Settings(BaseSettings):
    app_name: str = "after-sales-agent-service"
    host: str = "0.0.0.0"
    port: int = 8000
    llm_provider: str = "mock"
    llm_base_url: str = "https://api.openai.com/v1"
    llm_api_key: str = ""
    llm_model: str = "gpt-4o-mini"
    embedding_provider: str = "hash"
    embedding_model: str = "text-embedding-3-small"
    embedding_dimensions: int = 128
    vector_store: str = "memory"
    qdrant_url: str = "http://localhost:6333"
    qdrant_collection: str = "after_sales_chunks"
    storage_root: str = "./data/files"
    storage_mode: str = "local"
    minio_endpoint: str = "http://localhost:9000"
    minio_access_key: str = "minioadmin"
    minio_secret_key: str = "minioadmin"
    minio_bucket: str = "agent-platform"
    redis_url: str = "redis://localhost:6379/0"
    backend_service_url: str = "http://localhost:8080"
    agent_service_token: str = ""
    top_k: int = 8
    rerank_top_k: int = 4
    max_context_tokens: int = 3500
    cache_ttl_seconds: int = 30
    sensitive_terms: str = ""
    evaluation_file: str = "./data/evaluations.jsonl"

    model_config = SettingsConfigDict(
        env_file=".env",
        env_file_encoding="utf-8",
        extra="ignore",
    )

    def sensitive_term_list(self) -> list[str]:
        return [item.strip().lower() for item in self.sensitive_terms.split(",")
                if item.strip()]


@lru_cache
def get_settings() -> Settings:
    return Settings()
