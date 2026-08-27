from typing import Any

import httpx

from .config import Settings


class LlmProvider:
    def __init__(self, settings: Settings) -> None:
        self.settings = settings
        self._langchain_chain = self._build_langchain_chain()

    async def generate(self, query: str, context: str, plan: list[str]) -> tuple[str, bool]:
        if self.settings.llm_provider == "openai" and self.settings.llm_api_key:
            try:
                if self._langchain_chain is not None:
                    return await self._langchain_generate(
                        query, context, plan
                    ), False
                return await self._openai_generate(query, context, plan), False
            except Exception:
                # 真实模型异常时回落到可解释回答，避免一次模型超时拖垮业务接口。
                return self._fallback_answer(query, context), True
        return self._fallback_answer(query, context), True

    def _build_langchain_chain(self):
        """构造 LangChain 链；依赖不可用时保留原生 HTTP 兼容路径。"""
        if self.settings.llm_provider != "openai" or not self.settings.llm_api_key:
            return None
        try:
            from langchain_core.prompts import ChatPromptTemplate
            from langchain_openai import ChatOpenAI

            prompt = ChatPromptTemplate.from_messages([
                (
                    "system",
                    "你是企业售后支持 Agent。只基于参考资料回答；"
                    "资料不足时明确说明，不要编造维修结论。",
                ),
                (
                    "human",
                    "处理计划：{plan}\n参考资料：{context}\n问题：{query}",
                ),
            ])
            model = ChatOpenAI(
                model=self.settings.llm_model,
                api_key=self.settings.llm_api_key,
                base_url=self.settings.llm_base_url,
                temperature=0.1,
                timeout=60,
            )
            return prompt | model
        except Exception:
            return None

    async def _langchain_generate(
            self,
            query: str,
            context: str,
            plan: list[str],
    ) -> str:
        result = await self._langchain_chain.ainvoke({
            "plan": plan,
            "context": context,
            "query": query,
        })
        content = getattr(result, "content", result)
        if isinstance(content, list):
            content = "\n".join(
                str(item.get("text", item)) if isinstance(item, dict)
                else str(item)
                for item in content
            )
        if not str(content).strip():
            raise ValueError("LLM 返回空内容")
        return str(content)

    def _fallback_answer(
        self,
        query: str,
        context: str,
    ) -> str:
        return (
            "这是本地降级回答（模型暂不可用或未配置）。当前已完成查询改写、"
            "混合检索、重排序和上下文压缩；配置真实 LLM_PROVIDER 与 "
            "LLM_API_KEY 后将生成正式售后答复。\n\n"
            f"问题：{query}\n\n参考上下文：{context[:1200]}"
        )

    async def _openai_generate(self, query: str, context: str, plan: list[str]) -> str:
        payload: dict[str, Any] = {
            "model": self.settings.llm_model,
            "temperature": 0.1,
            "messages": [
                {
                    "role": "system",
                    "content": (
                        "你是企业售后支持 Agent。只基于参考资料回答；"
                        "资料不足时明确说明，不要编造维修结论。"
                    ),
                },
                {
                    "role": "user",
                    "content": f"处理计划：{plan}\n参考资料：{context}\n问题：{query}",
                },
            ],
        }
        async with httpx.AsyncClient(timeout=60) as client:
            response = await client.post(
                f"{self.settings.llm_base_url.rstrip('/')}/chat/completions",
                headers={"Authorization": f"Bearer {self.settings.llm_api_key}"},
                json=payload,
            )
            response.raise_for_status()
            return response.json()["choices"][0]["message"]["content"]
