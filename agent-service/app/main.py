from fastapi import Depends, FastAPI, Header, HTTPException

from .config import get_settings
from .evaluation import EvaluationStore, evaluate
from .parser import parse_document, parse_document_bytes
from .rag import (
    Chunk,
    HashEmbeddingProvider,
    InMemoryHybridStore,
    OpenAIEmbeddingProvider,
    QdrantHybridStore,
    split_chunks,
)
from .schemas import (
    ChatRequest,
    ChatResponse,
    EvaluationRequest,
    EvaluationResponse,
    HealthResponse,
    IngestRequest,
    IngestResponse,
)
from .workflow import AgentWorkflow
from .security import verify_service_token
from .storage import read_object

settings = get_settings()
if settings.embedding_provider == "openai" and settings.llm_api_key:
    embedding = OpenAIEmbeddingProvider(
        settings.llm_base_url,
        settings.llm_api_key,
        settings.embedding_model,
        settings.embedding_dimensions,
    )
else:
    embedding = HashEmbeddingProvider(settings.embedding_dimensions)

memory_store = InMemoryHybridStore(embedding)
vector_store_status = "UP"
if settings.vector_store == "qdrant":
    try:
        store = QdrantHybridStore(
            settings.qdrant_url,
            settings.qdrant_collection,
            fallback=memory_store,
            embedding=embedding,
        )
    except Exception:
        # 向量库不可用时仍允许服务启动，检索暂时落到本地内存。
        store = memory_store
        vector_store_status = "DEGRADED"
else:
    store = memory_store
workflow = AgentWorkflow(settings, store)
evaluation_store = EvaluationStore(settings)
app = FastAPI(title=settings.app_name, version="1.0.0")


@app.get("/health", response_model=HealthResponse)
async def health() -> HealthResponse:
    return HealthResponse(
        status="UP",
        components={
            "parser": "UP",
            "embedding": "UP",
            "vectorStore": vector_store_status,
            "llm": "UP" if settings.llm_provider == "mock" or settings.llm_api_key else "DEGRADED",
        },
    )


@app.post("/v1/ingest", response_model=IngestResponse)
async def ingest(
    request: IngestRequest,
    _: None = Depends(verify_service_token),
) -> IngestResponse:
    try:
        if request.content:
            text = request.content
        else:
            try:
                text = parse_document(request.storagePath, request.fileType)
            except (FileNotFoundError, ValueError):
                text = parse_document_bytes(
                    read_object(settings, request.storagePath),
                    request.fileType,
                )
        pieces = split_chunks(text)
        chunks = [
            Chunk(
                chunk_id=f"{request.documentId}-{index}",
                document_id=request.documentId,
                tenant_id=request.tenantId,
                knowledge_base_id=request.knowledgeBaseId,
                file_name=request.fileName,
                content=piece,
            )
            for index, piece in enumerate(pieces)
        ]
        if not chunks:
            raise ValueError("文档没有可提取的文本内容，暂不能建立索引")
        store.replace_document(chunks)
        workflow.clear_cache()
        return IngestResponse(
            status="INDEXED",
            chunkCount=len(chunks),
            message="解析、Chunk 切分、Embedding 和向量入库完成",
        )
    except FileNotFoundError as exc:
        raise HTTPException(status_code=404, detail=str(exc)) from exc
    except ValueError as exc:
        raise HTTPException(status_code=422, detail=str(exc)) from exc


@app.post("/v1/chat", response_model=ChatResponse)
async def chat(
    request: ChatRequest,
    _: None = Depends(verify_service_token),
    end_user_authorization: str | None = Header(
        default=None,
        alias="X-End-User-Authorization",
    ),
) -> ChatResponse:
    return await workflow.chat(request, end_user_authorization)


@app.post("/v1/evaluate", response_model=EvaluationResponse)
async def run_evaluation(
    request: EvaluationRequest,
    _: None = Depends(verify_service_token),
) -> EvaluationResponse:
    result = evaluate(request)
    evaluation_store.append(request, result)
    return result
