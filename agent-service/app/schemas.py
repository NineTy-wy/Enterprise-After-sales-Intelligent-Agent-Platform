from typing import Any

from pydantic import BaseModel, Field


class IngestRequest(BaseModel):
    documentId: str
    tenantId: str
    knowledgeBaseId: str
    fileName: str
    fileType: str
    storagePath: str
    content: str | None = None


class IngestResponse(BaseModel):
    status: str
    chunkCount: int
    message: str


class Citation(BaseModel):
    documentId: str
    fileName: str
    chunkId: str
    score: float
    content: str


class ChatRequest(BaseModel):
    tenantId: str
    userId: str
    sessionId: str | None = None
    query: str = Field(min_length=1, max_length=4000)
    knowledgeBaseIds: list[str] = Field(default_factory=list)
    mode: str = "react"


class ChatResponse(BaseModel):
    answer: str
    citations: list[Citation]
    trace: list[str]
    tokenUsage: dict[str, int]
    fallbackUsed: bool


class EvaluationRequest(BaseModel):
    query: str = Field(min_length=1, max_length=4000)
    answer: str
    expectedAnswer: str | None = None
    citations: list[Citation] = Field(default_factory=list)


class EvaluationResponse(BaseModel):
    groundedness: float
    relevance: float
    citationCoverage: float
    overall: float
    notes: list[str]


class HealthResponse(BaseModel):
    status: str
    components: dict[str, str]
