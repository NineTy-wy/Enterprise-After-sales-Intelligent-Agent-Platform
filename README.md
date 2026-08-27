# Enterprise After-Sales Agent Platform

This repository contains a production-shaped AI application platform for
after-sales support teams.

## Architecture

- `backend-spring`: Spring Boot business service for users, tenants, RBAC,
  knowledge bases, documents, tickets, tool permissions, audit logs, storage,
  and asynchronous document jobs.
- `agent-service`: FastAPI orchestration service for document parsing, chunking,
  Embedding, Hybrid Search, reranking, context compression, ReAct,
  Plan & Execute, tool calling, LLM fallback, and evaluation.
- `frontend-web`: React/Vite operations console.
- `deploy`: Docker Compose stack for PostgreSQL, Redis, RabbitMQ, MinIO,
  Qdrant, Spring Boot, FastAPI, and Nginx.

## Development Modes

The Spring Boot default profile is `local`. It uses in-memory business
repositories, local file storage, a local asynchronous executor, and a Mock
Agent client, so the business API can be developed without infrastructure.

The Docker Compose profile uses PostgreSQL, RabbitMQ, MinIO, Qdrant, and the
HTTP Agent client. The default LLM and Embedding providers remain local
fallbacks until real model credentials are configured.

## Quick Start

```powershell
cd "F:\AI Agent\agent-support-platform"
Copy-Item deploy\.env.example deploy\.env
docker compose -f deploy\docker-compose.yml up -d --build
```

Open `http://localhost` after the frontend container becomes healthy.
The local demo account is configured by `DEMO_*` values in `deploy/.env`.

## Verification

```powershell
cd backend-spring
.\mvnw.cmd test

cd ..\agent-service
python -m compileall -q app
python -m unittest discover -s tests

cd ..\frontend-web
npm install
npm run build

cd ..
docker compose -f deploy\docker-compose.yml config
```

When `EMBEDDING_PROVIDER=openai` is selected, set
`EMBEDDING_DIMENSIONS` to the actual model dimension and use a new Qdrant
collection if the dimension changes. Legacy `.doc` files should be converted
to `.docx` before upload. The Agent parser supports `.xls` through `xlrd`.
