import tempfile
import unittest
from pathlib import Path

from app.config import Settings
from app.evaluation import evaluate
from app.rag import Chunk, HashEmbeddingProvider, InMemoryHybridStore, split_chunks
from app.schemas import ChatRequest, Citation, EvaluationRequest
from app.workflow import AgentWorkflow


class AgentWorkflowTest(unittest.IsolatedAsyncioTestCase):
    async def test_chat_should_retrieve_rerank_compress_and_return_citation(self):
        settings = Settings(
            llm_provider="mock",
            vector_store="memory",
            redis_url="",
            embedding_dimensions=32,
            top_k=5,
            rerank_top_k=2,
            max_context_tokens=80,
        )
        store = InMemoryHybridStore(HashEmbeddingProvider(32))
        store.add([
            Chunk(
                chunk_id="doc-1-0",
                document_id="doc-1",
                tenant_id="tenant-a",
                knowledge_base_id="kb-a",
                file_name="A100维修手册.txt",
                content="A100设备出现E03报警时，先断电，再检查温度传感器连接。",
            ),
            Chunk(
                chunk_id="doc-2-0",
                document_id="doc-2",
                tenant_id="tenant-b",
                knowledge_base_id="kb-b",
                file_name="其他租户.txt",
                content="其他租户资料不能被 tenant-a 检索到。",
            ),
        ])
        workflow = AgentWorkflow(settings, store)

        response = await workflow.chat(ChatRequest(
            tenantId="tenant-a",
            userId="user-a",
            query="A100 出现 E03 报警怎么办？",
            knowledgeBaseIds=["kb-a"],
            mode="plan_execute",
        ))

        self.assertTrue(response.fallbackUsed)
        self.assertEqual(1, len(response.citations))
        self.assertEqual("doc-1", response.citations[0].documentId)
        self.assertIn("PlannerAgent", response.trace[0])
        self.assertGreater(response.tokenUsage["totalTokens"], 0)

    def test_split_chunks_should_overlap_and_evaluation_should_score_citations(self):
        chunks = split_chunks("abcdefg", chunk_size=4, overlap=2)
        self.assertEqual(["abcd", "cdef", "efg"], chunks)

        response = evaluate(EvaluationRequest(
            query="E03报警怎么处理",
            answer="E03报警先检查传感器连接",
            citations=[
                Citation(
                    documentId="doc-1",
                    fileName="manual.txt",
                    chunkId="doc-1-0",
                    score=0.9,
                    content="E03报警处理步骤：检查传感器连接。",
                )
            ],
        ))

        self.assertGreater(response.overall, 0)
        self.assertLessEqual(response.overall, 1)

    def test_evaluation_store_path_is_configurable(self):
        with tempfile.TemporaryDirectory() as directory:
            settings = Settings(evaluation_file=str(Path(directory) / "eval.jsonl"))
            self.assertTrue(settings.evaluation_file.endswith("eval.jsonl"))


if __name__ == "__main__":
    unittest.main()
