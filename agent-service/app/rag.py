from dataclasses import dataclass
import hashlib
import math
import re
import uuid
from typing import Protocol

import httpx


class EmbeddingProvider(Protocol):
    dimensions: int

    def embed(self, text: str) -> list[float]:
        ...

    def similarity(self, left: list[float], right: list[float]) -> float:
        ...

@dataclass
class Chunk:
    chunk_id: str
    document_id: str
    tenant_id: str
    knowledge_base_id: str
    file_name: str
    content: str


@dataclass
class ScoredChunk:
    chunk: Chunk
    score: float


class HybridStore(Protocol):
    def add(self, chunks: list[Chunk]) -> None:
        ...

    def replace_document(self, chunks: list[Chunk]) -> None:
        ...

    def search(
        self,
        query: str,
        tenant_id: str,
        knowledge_base_ids: list[str],
        top_k: int,
    ) -> list[ScoredChunk]:
        ...


def split_chunks(text: str, chunk_size: int = 800, overlap: int = 120) -> list[str]:
    if chunk_size <= 0:
        raise ValueError("chunk_size must be greater than zero")
    overlap = max(0, min(overlap, chunk_size - 1))
    normalized = re.sub(r"\s+", " ", text or "").strip()
    if not normalized:
        return []
    chunks: list[str] = []
    start = 0
    while start < len(normalized):
        end = min(len(normalized), start + chunk_size)
        chunks.append(normalized[start:end])
        if end == len(normalized):
            break
        start = max(start + 1, end - overlap)
    return chunks


class HashEmbeddingProvider:
    """无外部模型时的确定性向量，保证本地开发和测试可运行。"""

    def __init__(self, dimensions: int = 128) -> None:
        self.dimensions = max(8, dimensions)

    def embed(self, text: str) -> list[float]:
        dimensions = self.dimensions
        vector = [0.0] * dimensions
        for token in re.findall(r"\w+", text.lower()):
            digest = hashlib.sha256(token.encode("utf-8")).digest()
            index = int.from_bytes(digest[:4], "big") % dimensions
            vector[index] += 1.0
        norm = math.sqrt(sum(value * value for value in vector)) or 1.0
        return [value / norm for value in vector]

    def similarity(self, left: list[float], right: list[float]) -> float:
        return sum(a * b for a, b in zip(left, right))


class OpenAIEmbeddingProvider:
    """OpenAI 兼容 Embedding API；网络或模型失败时回退到 Hash 向量。"""

    def __init__(
        self,
        base_url: str,
        api_key: str,
        model: str,
        dimensions: int,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.api_key = api_key
        self.model = model
        self.dimensions = max(8, dimensions)
        self.fallback = HashEmbeddingProvider(self.dimensions)

    def embed(self, text: str) -> list[float]:
        try:
            response = httpx.post(
                f"{self.base_url}/embeddings",
                headers={"Authorization": f"Bearer {self.api_key}"},
                json={"model": self.model, "input": text},
                timeout=30,
            )
            response.raise_for_status()
            vector = response.json()["data"][0]["embedding"]
            if len(vector) != self.dimensions:
                # 防止模型切换后向量维度和 Qdrant collection 不一致。
                return self.fallback.embed(text)
            return [float(value) for value in vector]
        except (httpx.HTTPError, KeyError, IndexError, TypeError, ValueError):
            return self.fallback.embed(text)

    def similarity(self, left: list[float], right: list[float]) -> float:
        return sum(a * b for a, b in zip(left, right))


class InMemoryHybridStore:
    """同时保存文本倒排所需数据和向量，模拟生产中的 Hybrid Search。"""

    def __init__(self, embedding: EmbeddingProvider | None = None) -> None:
        self._chunks: dict[str, Chunk] = {}
        self._vectors: dict[str, list[float]] = {}
        self._embedding = embedding or HashEmbeddingProvider()

    def add(self, chunks: list[Chunk]) -> None:
        for chunk in chunks:
            self._chunks[chunk.chunk_id] = chunk
            self._vectors[chunk.chunk_id] = self._embedding.embed(chunk.content)

    def replace_document(self, chunks: list[Chunk]) -> None:
        if chunks:
            document_id = chunks[0].document_id
            tenant_id = chunks[0].tenant_id
            old_ids = [
                chunk_id
                for chunk_id, chunk in self._chunks.items()
                if chunk.document_id == document_id
                and chunk.tenant_id == tenant_id
            ]
            for chunk_id in old_ids:
                self._chunks.pop(chunk_id, None)
                self._vectors.pop(chunk_id, None)
        self.add(chunks)

    def search(
        self,
        query: str,
        tenant_id: str,
        knowledge_base_ids: list[str],
        top_k: int,
    ) -> list[ScoredChunk]:
        query_tokens = set(re.findall(r"\w+", query.lower()))
        query_vector = self._embedding.embed(query)
        candidates: list[ScoredChunk] = []
        for chunk_id, chunk in self._chunks.items():
            if chunk.tenant_id != tenant_id:
                continue
            if knowledge_base_ids and chunk.knowledge_base_id not in knowledge_base_ids:
                continue
            lexical = sum(token in chunk.content.lower() for token in query_tokens)
            lexical_score = lexical / max(len(query_tokens), 1)
            vector_score = self._embedding.similarity(
                query_vector, self._vectors[chunk_id]
            )
            candidates.append(ScoredChunk(
                chunk=chunk,
                score=0.55 * vector_score + 0.45 * lexical_score,
            ))
        return sorted(candidates, key=lambda item: item.score, reverse=True)[:top_k]


class QdrantHybridStore:
    """Qdrant 向量检索适配器，文本词法分数仍在服务侧融合。"""

    def __init__(
        self,
        base_url: str,
        collection: str,
        fallback: InMemoryHybridStore | None = None,
        embedding: EmbeddingProvider | None = None,
    ) -> None:
        self.base_url = base_url.rstrip("/")
        self.collection = collection
        self.fallback = fallback
        self.embedding = embedding or HashEmbeddingProvider()
        self._ensure_collection()

    def _ensure_collection(self) -> None:
        response = httpx.get(
            f"{self.base_url}/collections/{self.collection}",
            timeout=5,
        )
        if response.status_code == 200:
            return
        response = httpx.put(
            f"{self.base_url}/collections/{self.collection}",
            json={
                "vectors": {
                    "size": self.embedding.dimensions,
                    "distance": "Cosine",
                }
            },
            timeout=5,
        )
        response.raise_for_status()

    def add(self, chunks: list[Chunk]) -> None:
        if self.fallback is not None:
            # 持续维护内存镜像，便于 Qdrant 短暂不可用时无缝降级检索。
            self.fallback.add(chunks)
        try:
            self._add_to_qdrant(chunks)
        except Exception:
            if self.fallback is None:
                raise

    def _add_to_qdrant(self, chunks: list[Chunk]) -> None:
        points = [
            {
                # Qdrant 点位 ID 只能使用无符号整数或 UUID，
                # 业务 Chunk ID 放到 payload 中保留原始可追溯标识。
                "id": str(uuid.uuid5(uuid.NAMESPACE_URL, chunk.chunk_id)),
                "vector": self.embedding.embed(chunk.content),
                "payload": {
                    "chunkId": chunk.chunk_id,
                    "documentId": chunk.document_id,
                    "tenantId": chunk.tenant_id,
                    "knowledgeBaseId": chunk.knowledge_base_id,
                    "fileName": chunk.file_name,
                    "content": chunk.content,
                },
            }
            for chunk in chunks
        ]
        response = httpx.put(
            f"{self.base_url}/collections/{self.collection}/points",
            params={"wait": "true"},
            json={"points": points},
            timeout=20,
        )
        response.raise_for_status()

    def replace_document(self, chunks: list[Chunk]) -> None:
        if not chunks:
            return
        document_id = chunks[0].document_id
        tenant_id = chunks[0].tenant_id
        if self.fallback is not None:
            # 先更新回退库，即使后续 Qdrant 写入失败也不会丢失本次索引。
            self.fallback.replace_document(chunks)
        try:
            response = httpx.post(
                f"{self.base_url}/collections/{self.collection}/points/delete",
                params={"wait": "true"},
                json={
                    "filter": {
                        "must": [
                            {"key": "tenantId", "match": {"value": tenant_id}},
                            {"key": "documentId", "match": {"value": document_id}},
                        ]
                    }
                },
                timeout=20,
            )
            response.raise_for_status()
            self._add_to_qdrant(chunks)
        except Exception:
            if self.fallback is None:
                raise

    def search(
        self,
        query: str,
        tenant_id: str,
        knowledge_base_ids: list[str],
        top_k: int,
    ) -> list[ScoredChunk]:
        must = [{"key": "tenantId", "match": {"value": tenant_id}}]
        if knowledge_base_ids:
            must.append({
                "key": "knowledgeBaseId",
                "match": {"any": knowledge_base_ids},
            })
        try:
            response = httpx.post(
                f"{self.base_url}/collections/{self.collection}/points/search",
                json={
                    "vector": self.embedding.embed(query),
                    "limit": top_k,
                    "with_payload": True,
                    "filter": {"must": must},
                },
                timeout=20,
            )
            response.raise_for_status()
            results = response.json().get("result", [])
            qdrant_results = [
                ScoredChunk(
                    chunk=Chunk(
                        # 新数据使用 payload 中的业务 ID；兼容旧点位时回退到 Qdrant ID。
                        chunk_id=item["payload"].get("chunkId", str(item["id"])),
                        document_id=item["payload"]["documentId"],
                        tenant_id=item["payload"]["tenantId"],
                        knowledge_base_id=item["payload"]["knowledgeBaseId"],
                        file_name=item["payload"]["fileName"],
                        content=item["payload"]["content"],
                    ),
                    score=float(item.get("score", 0)),
                )
                for item in results
            ]
            if qdrant_results or self.fallback is None:
                return qdrant_results
            return self.fallback.search(query, tenant_id, knowledge_base_ids, top_k)
        except Exception:
            if self.fallback is None:
                raise
            return self.fallback.search(query, tenant_id, knowledge_base_ids, top_k)


def rerank(query: str, candidates: list[ScoredChunk], top_k: int) -> list[ScoredChunk]:
    query_tokens = set(re.findall(r"\w+", query.lower()))
    rescored = []
    for candidate in candidates:
        overlap = sum(token in candidate.chunk.content.lower()
                      for token in query_tokens)
        score = candidate.score + overlap / max(len(query_tokens), 1) * 0.2
        rescored.append(ScoredChunk(candidate.chunk, score))
    return sorted(rescored, key=lambda item: item.score, reverse=True)[:top_k]


def compress_context(candidates: list[ScoredChunk], max_chars: int) -> str:
    if max_chars <= 0:
        return ""
    result: list[str] = []
    used = 0
    for candidate in candidates:
        text = f"[{candidate.chunk.file_name}] {candidate.chunk.content}"
        remaining = max_chars - used
        if remaining <= 0:
            break
        if len(text) > remaining:
            result.append(text[:remaining])
            break
        result.append(text)
        used += len(text)
    return "\n".join(result)
