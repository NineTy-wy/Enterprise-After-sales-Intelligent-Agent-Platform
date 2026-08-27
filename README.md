# Enterprise After-sales Intelligent Agent Platform

企业售后智能 Agent 平台，一个面向售后支持团队的 AI 应用工程项目。系统围绕“企业知识库 + 售后工单 + Agent 问答”构建完整业务闭环，支持文档解析、Chunk 切分、Embedding、向量检索、Hybrid Search、Rerank、上下文压缩、ReAct / Plan & Execute、Tool Calling、敏感信息过滤、异步索引和 Docker 全栈部署。

> 当前版本已完成本地 Docker 全栈部署与核心业务链路验证。默认使用 `LLM_PROVIDER=mock` 和 `EMBEDDING_PROVIDER=hash`，可在不配置真实大模型 Key 的情况下跑通完整流程；上线展示时可切换为 OpenAI 兼容模型服务。

## 项目定位

传统售后场景中，故障码手册、维修 SOP、产品说明书、售后政策等资料通常分散在 PDF、Word、Excel 或内部文档中。售后人员处理客户问题时，经常需要人工检索大量资料，响应慢、经验依赖强，并且普通关键词搜索难以理解自然语言问题。

本项目提供一个企业内部售后智能支持平台：

- 售后主管上传和管理企业知识资料。
- 系统异步完成文档解析、切分、向量化和索引。
- 售后人员录入客户问题并创建工单。
- Agent 基于知识库检索相关内容，生成带引用来源和执行轨迹的处理建议。
- 平台通过租户隔离、权限控制和敏感信息过滤降低数据泄露风险。

## 核心功能

### 知识库管理

- 创建、查询、归档知识库。
- 支持租户级数据隔离。
- 支持知识库状态控制，归档后禁止继续上传文档。
- 支持文档列表查询和索引状态展示。

### 文档处理流水线

- 支持 PDF、Word、Excel、CSV、TXT、Markdown 等文件类型。
- 上传文件存储到 MinIO 或本地文件系统。
- 通过 RabbitMQ 发布异步文档处理任务。
- FastAPI Agent 服务完成文档解析、Chunk 切分、Embedding 和向量入库。
- 文档状态流转：`UPLOADED -> PROCESSING -> INDEXED / FAILED`。

### RAG 检索增强问答

- 查询改写。
- 多路召回。
- Hybrid Search。
- Rerank 重排序。
- 上下文压缩。
- 知识库引用来源返回。
- Token 使用量统计。
- 响应缓存。
- 模型不可用时本地降级。

### Agent 编排

- 支持 ReAct 模式。
- 支持 Plan & Execute 模式。
- 支持多 Agent 协作式执行轨迹。
- 支持 Tool Calling，通过 Spring Boot 网关控制工具权限。
- 支持流式输出接口，前端可实时展示生成过程。

### 售后工单

- 创建售后工单。
- 查询工单列表和详情。
- 更新工单状态。
- 支持客户名称、产品型号、问题描述、优先级和状态等业务字段。
- 刷新后数据可持久化到 PostgreSQL。

### 安全与工程化

- JWT 登录认证。
- 多租户数据隔离。
- 角色模型：`ADMIN`、`AGENT`、`OPERATOR`、`VIEWER`。
- 敏感词过滤。
- Tool 权限控制。
- 审计日志。
- Docker Compose 一键启动全栈基础设施。
- Flyway 数据库迁移。
- Actuator 健康检查。

## 技术栈

### 后端业务服务

- Java 21
- Spring Boot 3.3
- Spring MVC
- Spring Validation
- Spring Security
- Spring Data JPA
- Spring AMQP
- Spring Boot Actuator
- PostgreSQL
- Flyway
- MinIO SDK
- JWT
- Maven

### Agent 服务

- Python
- FastAPI
- Uvicorn
- Pydantic Settings
- LangChain / OpenAI 兼容调用
- Qdrant
- Redis
- PDF / Word / Excel / CSV / TXT 解析
- Hash Embedding 本地降级
- JSONL 评估数据存储

### 前端

- React 18
- TypeScript
- Vite
- Lucide React
- Nginx

### 部署与基础设施

- Docker
- Docker Compose
- PostgreSQL 16
- Redis 7
- RabbitMQ Management
- MinIO
- Qdrant
- Nginx

## 系统架构

```text
Browser
  |
  | HTTP
  v
Nginx / Frontend Web
  |
  | /api
  v
Spring Boot Backend
  |        |        |          |
  |        |        |          +--> PostgreSQL: 用户、知识库、文档、工单、审计
  |        |        +-------------> MinIO: 原始上传文件
  |        +----------------------> RabbitMQ: 文档异步处理任务
  +-------------------------------> FastAPI Agent Service
                                      |
                                      +--> Qdrant: 向量检索
                                      +--> Redis: 缓存
                                      +--> LLM Provider: OpenAI 兼容模型服务
```

## 目录结构

```text
agent-support-platform
├── backend-spring       # Spring Boot 业务后端
├── agent-service        # FastAPI Agent / RAG 服务
├── frontend-web         # React 前端控制台
├── deploy               # Docker Compose 与部署配置
├── docs                 # 设计说明书
├── README.md
└── .gitignore
```

## 快速启动

### 1. 准备环境

需要提前安装：

- JDK 21
- Docker Desktop
- Git

如果要本地单独运行前端或 Agent 服务，还需要：

- Node.js
- Python

### 2. 配置环境变量

复制环境变量模板：

```powershell
cd "F:\AI Agent\agent-support-platform"
Copy-Item deploy\.env.example deploy\.env
```

修改 `deploy/.env` 中的密码、Token 和模型配置。

本地首次验证可以保持：

```env
LLM_PROVIDER=mock
EMBEDDING_PROVIDER=hash
```

这种配置不调用真实大模型，适合本地联调和演示基础链路。

### 3. 启动全栈服务

```powershell
cd "F:\AI Agent\agent-support-platform"
docker compose --env-file deploy\.env -f deploy\docker-compose.yml up -d --build
```

查看服务状态：

```powershell
docker compose --env-file deploy\.env -f deploy\docker-compose.yml ps
```

正常情况下应看到以下服务运行：

```text
postgres
redis
rabbitmq
minio
qdrant
agent-service
backend-spring
frontend-web
```

### 4. 访问系统

前端入口：

```text
http://localhost
```

默认演示账号：

```text
租户 ID：tenant_demo
用户名：demo
密码：demo123456
```

健康检查：

```text
http://localhost:8000/health
http://localhost:8080/actuator/health
```

## 业务验收流程

本地启动成功后，可以按以下流程验证核心业务闭环：

1. 登录系统。
2. 创建知识库，例如“售后设备故障知识库”。
3. 上传测试文档，例如 TXT、PDF 或 DOCX。
4. 等待文档状态变为 `INDEXED`。
5. 进入“智能问答”，选择 ReAct 模式提问。
6. 检查回答是否包含引用来源和执行轨迹。
7. 切换 Plan & Execute 模式再次提问。
8. 创建售后工单并刷新页面，验证数据持久化。

测试文档示例：

```text
A100 设备售后故障处理手册

E03 报警处理：
1. 关闭设备电源，等待 30 秒后重新启动。
2. 检查设备进风口和散热风扇是否堵塞。
3. 检查电源输入是否稳定。
4. 如果故障仍然存在，记录设备序列号并联系售后工程师。
```

示例问题：

```text
A100 设备出现 E03 报警应该怎么处理？
```

## 接入真实大模型

默认配置使用 Mock LLM，因此回答中可能出现本地降级说明。若要用于公开演示或部署上线，建议配置真实 OpenAI 兼容模型：

```env
LLM_PROVIDER=openai
LLM_BASE_URL=https://api.openai.com/v1
LLM_API_KEY=your-api-key
LLM_MODEL=gpt-4o-mini
```

如果使用其他 OpenAI 兼容服务，例如 DeepSeek、通义千问兼容接口、硅基流动或火山方舟，需要将：

```env
LLM_BASE_URL
LLM_API_KEY
LLM_MODEL
```

替换为对应平台提供的配置。

修改后重启服务：

```powershell
docker compose --env-file deploy\.env -f deploy\docker-compose.yml up -d agent-service backend-spring
```

如果使用真实 Embedding 模型，需要同步调整：

```env
EMBEDDING_PROVIDER=openai
EMBEDDING_MODEL=text-embedding-3-small
EMBEDDING_DIMENSIONS=1536
```

注意：Embedding 维度变化后，应使用新的 Qdrant collection 或重建原有向量集合。

## 本地开发

### Spring Boot

```powershell
cd backend-spring
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run
```

默认 `local` profile 使用内存仓储、本地文件存储和 Mock Agent Client，方便在不启动 Docker 基础设施时开发业务接口。

### FastAPI Agent 服务

```powershell
cd agent-service
python -m venv .venv
.\.venv\Scripts\activate
pip install -r requirements.txt
uvicorn app.main:app --reload --port 8000
```

### React 前端

```powershell
cd frontend-web
npm install
npm run dev
```

## 测试与验证

后端测试：

```powershell
cd backend-spring
.\mvnw.cmd test
```

Agent 服务测试：

```powershell
cd agent-service
python -m compileall -q app
python -m unittest discover -s tests
```

前端构建：

```powershell
cd frontend-web
npm install
npm run build
```

Docker Compose 配置校验：

```powershell
docker compose --env-file deploy\.env -f deploy\docker-compose.yml config --quiet
```

## 已覆盖的 AI 应用开发能力

- PDF / Word / Excel / CSV / TXT / Markdown 解析
- Chunk 切分与 overlap
- Embedding 向量化
- Qdrant 向量检索
- Hybrid Search
- 多路召回
- Rerank 重排序
- 查询改写
- 上下文压缩
- LLM 生成
- ReAct
- Plan & Execute
- 多 Agent 协作轨迹
- Tool Calling
- AI 评估闭环
- 知识库权限隔离
- 敏感信息过滤
- Tool 权限控制
- Token 控制
- 缓存优化
- 流式输出
- 异步任务处理
- 模型降级策略

## 上线前建议

当前项目已具备本地全栈运行能力。若用于真实公网展示，建议补充：

- 配置真实 LLM 和 Embedding Provider。
- 将 `APP_SECURITY_ENABLED` 设置为 `true`。
- 替换所有默认密码、JWT Secret 和服务 Token。
- 配置正式域名和 HTTPS。
- 收紧 CORS 域名。
- 接入对象存储、托管 PostgreSQL 或云服务器数据盘备份。
- 增加 CI/CD 流水线。
- 增加前端管理页，例如用户管理、角色管理、知识库权限配置。
- 增加客户门户，例如客户提交问题、查看工单进度、满意度评价。

## 项目状态

- 本地 Docker 全栈部署：已完成
- 前端登录：已验证
- 知识库创建与持久化：已验证
- 文档上传与异步索引：已验证
- ReAct 问答：已验证
- Plan & Execute 问答：已验证
- 工单创建与持久化：已验证
- 真实大模型接入：预留配置，需提供实际模型 Key
