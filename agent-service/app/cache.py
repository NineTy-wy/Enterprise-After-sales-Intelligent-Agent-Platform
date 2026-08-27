import json
import time
from dataclasses import dataclass
from typing import Any


@dataclass
class CacheEntry:
    expires_at: float
    value: Any


class ResponseCache:
    """带 Redis 优先级的 TTL 缓存，Redis 不可用时自动回退到本地内存。"""

    def __init__(
        self,
        ttl_seconds: int = 30,
        redis_url: str | None = None,
    ) -> None:
        self.ttl_seconds = max(1, ttl_seconds)
        self._entries: dict[str, CacheEntry] = {}
        self._redis = None
        if redis_url:
            try:
                import redis

                self._redis = redis.Redis.from_url(
                    redis_url,
                    decode_responses=True,
                    socket_connect_timeout=1,
                    socket_timeout=1,
                )
            except Exception:
                self._redis = None

    def get(self, key: str) -> Any | None:
        if self._redis is not None:
            try:
                value = self._redis.get(self._redis_key(key))
                if value is not None:
                    return json.loads(value)
            except Exception:
                # Redis 短暂不可用时不影响当前问答请求。
                self._redis = None

        entry = self._entries.get(key)
        if entry is None:
            return None
        if entry.expires_at <= time.time():
            self._entries.pop(key, None)
            return None
        return entry.value

    def set(self, key: str, value: Any) -> None:
        serialized_value = value.model_dump() if hasattr(value, "model_dump") else value
        self._entries[key] = CacheEntry(
            expires_at=time.time() + self.ttl_seconds,
            value=serialized_value,
        )
        if self._redis is not None:
            try:
                self._redis.setex(
                    self._redis_key(key),
                    self.ttl_seconds,
                    json.dumps(serialized_value, ensure_ascii=False),
                )
            except Exception:
                self._redis = None

    def clear(self) -> None:
        self._entries.clear()
        if self._redis is not None:
            try:
                keys = list(self._redis.scan_iter(match="agent-platform:chat:*"))
                if keys:
                    self._redis.delete(*keys)
            except Exception:
                self._redis = None

    def _redis_key(self, key: str) -> str:
        return f"agent-platform:chat:{key}"
