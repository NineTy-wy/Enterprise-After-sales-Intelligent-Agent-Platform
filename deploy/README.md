# Deployment

`docker-compose.yml` contains the production-shaped local deployment:

- PostgreSQL: business data and Flyway schema
- Redis: cache/session extension point
- RabbitMQ: asynchronous document processing
- MinIO: uploaded document object storage
- Qdrant: vector retrieval
- Spring Boot: business API and security gateway
- FastAPI: RAG and Agent orchestration
- Nginx: frontend static assets and `/api` reverse proxy

Before starting the full stack, copy `deploy/.env.example` to `deploy/.env`
and replace all passwords and tokens. The Compose stack reads service
configuration from `deploy/.env`; `agent-service/.env.example` is only for
running FastAPI directly outside Docker.

The default compose stack uses `APP_SECURITY_ENABLED=false` so the first
environment can be smoke-tested without login. Set it to `true` after the
frontend login flow and production JWT secret are configured.

The PostgreSQL profile seeds the configured demo account once when it does not
already exist. The default local credentials are `tenant_demo`, `demo`, and
`demo123456`; set `DEMO_*` values in `deploy/.env` before a shared deployment.

The default Agent configuration is intentionally zero-dependency:
`LLM_PROVIDER=mock`, `EMBEDDING_PROVIDER=hash`, and `VECTOR_STORE=qdrant`.
After a real Embedding model is selected, keep `EMBEDDING_DIMENSIONS` aligned
with the selected model and rebuild the Qdrant collection if its dimension
changes. Legacy `.doc` files should be converted to `.docx` before upload.
