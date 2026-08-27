import json
import re
from datetime import datetime, timezone
from pathlib import Path

from .config import Settings
from .schemas import EvaluationRequest, EvaluationResponse


def _tokens(text: str) -> set[str]:
    """同时提取英文/数字词和中文单字，避免中文问题按空格切词失真。"""
    return {
        token.lower()
        for token in re.findall(r"[A-Za-z0-9_]+|[\u4e00-\u9fff]", text)
        if token.strip()
    }


def evaluate(request: EvaluationRequest) -> EvaluationResponse:
    answer = request.answer.strip()
    citations = request.citations
    answer_tokens = _tokens(answer)
    groundedness = 0.0
    if citations:
        citation_tokens = set().union(*(_tokens(citation.content)
                                        for citation in citations))
        overlap = len(answer_tokens & citation_tokens)
        groundedness = min(1.0, overlap / max(len(answer_tokens), 1) * 1.5)
    query_tokens = _tokens(request.query)
    relevance = len(query_tokens & answer_tokens) / max(len(query_tokens), 1)
    citation_coverage = min(1.0, len(citations) / 2) if citations else 0.0
    if request.expectedAnswer:
        expected_terms = _tokens(request.expectedAnswer)
        relevance = len(expected_terms & answer_tokens) / max(len(expected_terms), 1)
    overall = round((groundedness + relevance + citation_coverage) / 3, 4)
    return EvaluationResponse(
        groundedness=round(groundedness, 4),
        relevance=round(relevance, 4),
        citationCoverage=round(citation_coverage, 4),
        overall=overall,
        notes=[
            "评估结果用于离线回归和模型版本对比，不替代人工抽检。",
            "生产环境可接入 LLM-as-a-Judge 和标注数据集。",
        ],
    )


class EvaluationStore:
    """将每次评估结果追加到 JSONL，便于离线回归和版本对比。"""

    def __init__(self, settings: Settings) -> None:
        self.path = Path(settings.evaluation_file)

    def append(
        self,
        request: EvaluationRequest,
        response: EvaluationResponse,
    ) -> None:
        self.path.parent.mkdir(parents=True, exist_ok=True)
        with self.path.open("a", encoding="utf-8") as file:
            file.write(json.dumps({
                "createdAt": datetime.now(timezone.utc).isoformat(),
                "request": request.model_dump(),
                "response": response.model_dump(),
            }, ensure_ascii=False) + "\n")
