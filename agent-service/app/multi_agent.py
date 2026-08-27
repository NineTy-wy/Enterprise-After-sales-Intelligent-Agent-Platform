from dataclasses import dataclass


@dataclass
class PlannerAgent:
    """负责编排任务拆解和策略选择。"""

    def plan(self, mode: str) -> list[str]:
        if mode == "plan_execute":
            return [
                "PlannerAgent: 识别售后问题、产品和客户诉求",
                "RetrievalAgent: 执行查询改写与 Hybrid Search",
                "RetrievalAgent: Rerank 并压缩上下文",
                "AnswerAgent: 生成答案并检查引用",
            ]
        return [
            "PlannerAgent: 观察并解析用户问题",
            "RetrievalAgent: 查询改写、混合检索和重排序",
            "AnswerAgent: 调用 LLM 生成可追溯回答",
        ]
