import re
import hashlib

from .cache import ResponseCache
from .config import Settings
from .llm import LlmProvider
from .multi_agent import PlannerAgent
from .rag import HybridStore, compress_context, rerank
from .schemas import ChatRequest, ChatResponse, Citation
from .tools import ToolRegistry, should_create_ticket


class AgentWorkflow:
    """ReAct 与 Plan & Execute 的统一编排入口。"""

    def __init__(self, settings: Settings, store: HybridStore) -> None:
        self.settings = settings
        self.store = store
        self.llm = LlmProvider(settings)
        self.planner = PlannerAgent()
        self.tools = ToolRegistry(settings)
        self.cache = ResponseCache(
            settings.cache_ttl_seconds,
            settings.redis_url,
        )

    def rewrite_query(self, query: str) -> str:
        normalized = re.sub(r"\s+", " ", query.strip())
        if not normalized:
            return normalized

        # 保留原问题，同时补充售后检索常用同义词，提升关键词召回覆盖率。
        expansions: list[str] = []
        if any(marker in normalized for marker in ("报错", "报警", "错误码")):
            expansions.append("故障码 排查 处理")
        if any(marker in normalized for marker in ("怎么办", "如何处理", "怎么解决")):
            expansions.append("维修步骤 注意事项")
        return f"{normalized} {' '.join(expansions)}".strip()

    def build_plan(self, request: ChatRequest, rewritten_query: str) -> list[str]:
        return self.planner.plan(request.mode)

    async def chat(
        self,
        request: ChatRequest,
        end_user_authorization: str | None = None,
    ) -> ChatResponse:
        rewritten_query = self.rewrite_query(request.query)
        cache_key = hashlib.sha256((
            request.tenantId
            + "|"
            + request.userId
            + "|"
            + rewritten_query
            + "|"
            + ",".join(request.knowledgeBaseIds)
            + "|"
            + request.mode
            + "|"
            + (request.sessionId or "")
        ).encode("utf-8")).hexdigest()
        cacheable = not should_create_ticket(rewritten_query)
        if cacheable:
            cached = self.cache.get(cache_key)
            if cached is not None:
                return (
                    cached
                    if isinstance(cached, ChatResponse)
                    else ChatResponse.model_validate(cached)
                )
        plan = self.build_plan(request, rewritten_query)
        candidates = self.store.search(
            rewritten_query,
            request.tenantId,
            request.knowledgeBaseIds,
            self.settings.top_k,
        )
        ranked = rerank(rewritten_query, candidates, self.settings.rerank_top_k)
        context = compress_context(ranked, self.settings.max_context_tokens * 4)
        answer, fallback_used = await self.llm.generate(
            rewritten_query, context, plan
        )
        if should_create_ticket(rewritten_query):
            try:
                await self.tools.invoke(
                    "create_ticket",
                    {
                        "customerName": "待确认客户",
                        "issueDescription": rewritten_query,
                        "priority": "HIGH",
                    },
                    authorization=end_user_authorization,
                )
                plan.append("ToolCallingAgent: 已创建售后工单")
            except Exception:
                plan.append("ToolCallingAgent: 工单工具不可用，等待人工创建")
        citations = [
            Citation(
                documentId=item.chunk.document_id,
                fileName=item.chunk.file_name,
                chunkId=item.chunk.chunk_id,
                score=round(item.score, 4),
                content=item.chunk.content,
            )
            for item in ranked
        ]
        prompt_tokens = len((rewritten_query + context).split())
        completion_tokens = len(answer.split())
        response = ChatResponse(
            answer=answer,
            citations=citations,
            trace=plan,
            tokenUsage={
                "promptTokens": prompt_tokens,
                "completionTokens": completion_tokens,
                "totalTokens": prompt_tokens + completion_tokens,
            },
            fallbackUsed=fallback_used,
        )
        if cacheable:
            self.cache.set(cache_key, response)
        return response

    def clear_cache(self) -> None:
        self.cache.clear()
