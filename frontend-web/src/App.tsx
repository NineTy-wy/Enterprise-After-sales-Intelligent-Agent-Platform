import { useEffect, useMemo, useState } from "react";
import {
  Activity,
  Archive,
  Bot,
  BookOpen,
  ChevronRight,
  FileText,
  LayoutDashboard,
  LogIn,
  MessageSquare,
  Plus,
  Send,
  ShieldCheck,
  Ticket as TicketIcon,
  Upload,
} from "lucide-react";
import { api, ChatResponse, DocumentItem, KnowledgeBase, Ticket } from "./api";

type View = "overview" | "knowledge" | "tickets" | "assistant";

function App() {
  const [authenticated, setAuthenticated] = useState(
    Boolean(localStorage.getItem("agent_access_token")),
  );
  const [view, setView] = useState<View>("overview");
  const [knowledgeBases, setKnowledgeBases] = useState<KnowledgeBase[]>([]);
  const [tickets, setTickets] = useState<Ticket[]>([]);
  const [indexedDocumentCount, setIndexedDocumentCount] = useState(0);
  const [selectedKnowledgeBase, setSelectedKnowledgeBase] = useState<string>();
  const [documents, setDocuments] = useState<DocumentItem[]>([]);
  const [notice, setNotice] = useState("");

  const refresh = async () => {
    try {
      const [bases, currentTickets] = await Promise.all([
        api.knowledgeBases(),
        api.tickets(),
      ]);
      const documentsByKnowledgeBase = await Promise.all(
        bases.map((base) => api.documents(base.id)),
      );
      setKnowledgeBases(bases);
      setTickets(currentTickets);
      setIndexedDocumentCount(
        documentsByKnowledgeBase
          .flat()
          .filter((item) => item.status === "INDEXED").length,
      );
      if (!selectedKnowledgeBase && bases[0]) {
        setSelectedKnowledgeBase(bases[0].id);
      }
    } catch (error) {
      setNotice(error instanceof Error ? error.message : "请求失败");
    }
  };

  useEffect(() => {
    if (!authenticated) return;
    void refresh();
  }, [authenticated]);

  useEffect(() => {
    if (!authenticated) return;
    if (!selectedKnowledgeBase) return;
    let disposed = false;
    const loadDocuments = async () => {
      try {
        const result = await api.documents(selectedKnowledgeBase);
        if (!disposed) setDocuments(result);
      } catch (error) {
        if (!disposed) {
          setNotice(error instanceof Error ? error.message : "文档加载失败");
        }
      }
    };
    void loadDocuments();
    const timer = window.setInterval(() => void loadDocuments(), 3000);
    return () => {
      disposed = true;
      window.clearInterval(timer);
    };
  }, [authenticated, selectedKnowledgeBase]);

  const stats = useMemo(() => ({
    bases: knowledgeBases.length,
    indexed: indexedDocumentCount,
    openTickets: tickets.filter((item) => item.status !== "CLOSED").length,
  }), [indexedDocumentCount, knowledgeBases.length, tickets]);

  if (!authenticated) {
    return <LoginScreen onSuccess={() => setAuthenticated(true)} onError={setNotice} notice={notice} />;
  }

  return (
    <div className="app-shell">
      <aside className="sidebar">
        <div className="brand">
          <div className="brand-mark"><Bot size={20} /></div>
          <div><strong>ServiceMind</strong><span>售后智能支持平台</span></div>
        </div>
        <nav>
          <NavItem icon={<LayoutDashboard />} label="运营总览" active={view === "overview"} onClick={() => setView("overview")} />
          <NavItem icon={<BookOpen />} label="知识库中心" active={view === "knowledge"} onClick={() => setView("knowledge")} />
          <NavItem icon={<TicketIcon />} label="售后工单" active={view === "tickets"} onClick={() => setView("tickets")} />
          <NavItem icon={<MessageSquare />} label="智能问答" active={view === "assistant"} onClick={() => setView("assistant")} />
        </nav>
        <div className="sidebar-footer"><ShieldCheck size={16} />租户隔离已启用</div>
      </aside>
      <main className="main-content">
        <header className="topbar">
          <div><span className="eyebrow">AFTER-SALES OPERATIONS</span><h1>{viewTitle(view)}</h1></div>
          <button className="user-chip" onClick={() => { localStorage.removeItem("agent_access_token"); setAuthenticated(false); }}><span className="status-dot" />演示用户 <small>退出</small></button>
        </header>
        {notice && <div className="notice">{notice}<button onClick={() => setNotice("")}>关闭</button></div>}
        {view === "overview" && <Overview stats={stats} setView={setView} />}
        {view === "knowledge" && (
          <KnowledgeView
            knowledgeBases={knowledgeBases}
            selectedId={selectedKnowledgeBase}
            documents={documents}
            onSelect={setSelectedKnowledgeBase}
            onRefresh={refresh}
            onDocumentsRefresh={async () => {
              if (!selectedKnowledgeBase) return;
              try {
                setDocuments(await api.documents(selectedKnowledgeBase));
              } catch (error) {
                setNotice(error instanceof Error ? error.message : "文档加载失败");
              }
            }}
            onArchive={async (knowledgeBaseId) => {
              try {
                await api.archiveKnowledgeBase(knowledgeBaseId);
                await refresh();
              } catch (error) {
                setNotice(error instanceof Error ? error.message : "归档失败");
              }
            }}
            onError={setNotice}
          />
        )}
        {view === "tickets" && <TicketView tickets={tickets} onCreated={refresh} onUpdated={refresh} onError={setNotice} />}
        {view === "assistant" && <AssistantView knowledgeBases={knowledgeBases} onError={setNotice} />}
      </main>
    </div>
  );
}

function LoginScreen({ onSuccess, onError, notice }: { onSuccess: () => void; onError: (message: string) => void; notice: string }) {
  const [tenantId, setTenantId] = useState("tenant_demo");
  const [username, setUsername] = useState("demo");
  const [password, setPassword] = useState("demo123456");
  const [loading, setLoading] = useState(false);
  const submit = async () => {
    setLoading(true);
    try {
      const result = await api.login(tenantId, username, password);
      localStorage.setItem("agent_access_token", result.accessToken);
      onSuccess();
    } catch (error) {
      onError(error instanceof Error ? error.message : "登录失败");
    } finally {
      setLoading(false);
    }
  };
  return <div className="login-shell"><div className="login-panel"><div className="brand login-brand"><div className="brand-mark"><Bot size={20} /></div><div><strong>ServiceMind</strong><span>售后智能支持平台</span></div></div><span className="eyebrow">SECURE WORKSPACE</span><h1>登录工作台</h1><p>进入知识库、工单和 Agent 协作空间。</p>{notice && <div className="notice">{notice}</div>}<label>租户 ID<input value={tenantId} onChange={(event) => setTenantId(event.target.value)} /></label><label>用户名<input value={username} onChange={(event) => setUsername(event.target.value)} /></label><label>密码<input type="password" value={password} onChange={(event) => setPassword(event.target.value)} onKeyDown={(event) => event.key === "Enter" && void submit()} /></label><button className="primary-button login-button" onClick={() => void submit()} disabled={loading}><LogIn size={17} />{loading ? "登录中..." : "登录"}</button><small className="login-hint">本地演示账号：tenant_demo / demo / demo123456</small></div></div>;
}

function NavItem({ icon, label, active, onClick }: { icon: JSX.Element; label: string; active: boolean; onClick: () => void }) {
  return <button className={`nav-item ${active ? "active" : ""}`} onClick={onClick}>{icon}<span>{label}</span><ChevronRight size={15} /></button>;
}

function Overview({ stats, setView }: { stats: { bases: number; indexed: number; openTickets: number }; setView: (view: View) => void }) {
  return <section className="page">
    <div className="hero-strip"><div><span className="eyebrow light">TODAY'S WORKSPACE</span><h2>让每一次售后响应，都有可靠依据。</h2><p>统一管理知识资产、工单流程和 Agent 推理结果。</p></div><button className="primary-button" onClick={() => setView("assistant")}><MessageSquare size={17} />开始问答</button></div>
    <div className="stats-grid">
      <Stat icon={<BookOpen />} label="知识库" value={stats.bases} detail="当前租户" />
      <Stat icon={<FileText />} label="已索引文档" value={stats.indexed} detail="可用于检索" />
      <Stat icon={<TicketIcon />} label="进行中工单" value={stats.openTickets} detail="需要跟进" />
      <Stat icon={<Activity />} label="Agent 状态" value="UP" detail="Mock / HTTP 可切换" />
    </div>
    <div className="section-heading"><div><span className="eyebrow">CORE CAPABILITIES</span><h3>业务闭环</h3></div></div>
    <div className="capability-grid">
      <Capability title="知识运营" text="上传 PDF、Word、Excel，异步完成解析、切分、向量化和索引。" icon={<BookOpen />} onClick={() => setView("knowledge")} />
      <Capability title="智能问答" text="查询改写、多路召回、Hybrid Search、Rerank 和可追溯引用。" icon={<Bot />} onClick={() => setView("assistant")} />
      <Capability title="工单协同" text="从客户问题到处理状态，沉淀 Agent 建议与人工执行结果。" icon={<TicketIcon />} onClick={() => setView("tickets")} />
    </div>
  </section>;
}

function Stat({ icon, label, value, detail }: { icon: JSX.Element; label: string; value: string | number; detail: string }) {
  return <div className="stat"><div className="stat-icon">{icon}</div><div><span>{label}</span><strong>{value}</strong><small>{detail}</small></div></div>;
}

function Capability({ title, text, icon, onClick }: { title: string; text: string; icon: JSX.Element; onClick: () => void }) {
  return <button className="capability" onClick={onClick}><div className="capability-icon">{icon}</div><div><h4>{title}</h4><p>{text}</p></div><ChevronRight /></button>;
}

function KnowledgeView({ knowledgeBases, selectedId, documents, onSelect, onRefresh, onDocumentsRefresh, onArchive, onError }: { knowledgeBases: KnowledgeBase[]; selectedId?: string; documents: DocumentItem[]; onSelect: (id: string) => void; onRefresh: () => void; onDocumentsRefresh: () => Promise<void>; onArchive: (knowledgeBaseId: string) => Promise<void>; onError: (message: string) => void }) {
  const [name, setName] = useState("");
  const [description, setDescription] = useState("");
  const selected = knowledgeBases.find((item) => item.id === selectedId);
  const create = async () => {
    if (!name.trim()) return;
    try { await api.createKnowledgeBase(name, description); setName(""); setDescription(""); onRefresh(); } catch (error) { onError(error instanceof Error ? error.message : "创建失败"); }
  };
  const upload = async (file: File) => {
    if (!selectedId) return;
    try { await api.uploadDocument(selectedId, file); onRefresh(); await onDocumentsRefresh(); } catch (error) { onError(error instanceof Error ? error.message : "上传失败"); }
  };
  const archive = async () => {
    if (!selectedId || selected?.status === "ARCHIVED") return;
    if (!window.confirm("归档后将不能继续上传文档，确认归档吗？")) return;
    await onArchive(selectedId);
  };
  return <section className="page">
    <div className="split-layout">
      <div className="panel list-panel"><div className="panel-header"><div><span className="eyebrow">KNOWLEDGE ASSETS</span><h3>知识库</h3></div><span className="count">{knowledgeBases.length}</span></div>
        <div className="create-row"><input value={name} onChange={(event) => setName(event.target.value)} placeholder="新知识库名称" /><button className="icon-button" title="创建知识库" onClick={create}><Plus size={18} /></button></div>
        <input className="description-input" value={description} onChange={(event) => setDescription(event.target.value)} placeholder="描述（可选）" />
        <div className="base-list">{knowledgeBases.map((item) => <button key={item.id} className={`base-item ${selectedId === item.id ? "selected" : ""}`} onClick={() => onSelect(item.id)}><div><strong>{item.name}</strong><small>{item.description || "暂无描述"}</small></div><span className={`tag ${item.status.toLowerCase()}`}>{item.status}</span></button>)}</div>
      </div>
      <div className="panel detail-panel"><div className="panel-header"><div><span className="eyebrow">DOCUMENT PIPELINE</span><h3>{selected?.name || "选择知识库"}</h3></div>{selected && <div className="panel-actions">{selected.status === "ACTIVE" && <label className="upload-button"><Upload size={16} />上传文档<input type="file" accept=".pdf,.doc,.docx,.xls,.xlsx,.csv,.txt,.md" onChange={(event) => event.target.files?.[0] && void upload(event.target.files[0])} /></label>}<button className="icon-button" title="归档知识库" onClick={() => void archive()} disabled={selected.status === "ARCHIVED"}><Archive size={17} /></button></div>}</div>
        {selected ? <><div className="detail-meta"><span>状态 <b className={`tag ${selected.status.toLowerCase()}`}>{selected.status}</b></span><span>文档 {documents.length} 份</span><span>支持异步索引</span></div><div className="document-list">{documents.map((document) => <div className="document-row" key={document.id}><FileText size={18} /><div><strong>{document.fileName}</strong><small>{formatBytes(document.fileSize)} · {new Date(document.uploadedAt).toLocaleString()}</small></div><span className={`tag ${document.status.toLowerCase()}`}>{document.status}</span></div>)}{documents.length === 0 && <Empty text="还没有文档，上传第一份售后资料" />}</div></> : <Empty text="选择一个知识库查看文档" />}
      </div>
    </div>
  </section>;
}

function TicketView({ tickets, onCreated, onUpdated, onError }: { tickets: Ticket[]; onCreated: () => void; onUpdated: () => void; onError: (message: string) => void }) {
  const [customerName, setCustomerName] = useState("");
  const [issueDescription, setIssueDescription] = useState("");
  const create = async () => {
    try { await api.createTicket({ customerName, issueDescription, priority: "MEDIUM" }); setCustomerName(""); setIssueDescription(""); onCreated(); } catch (error) { onError(error instanceof Error ? error.message : "创建失败"); }
  };
  const updateStatus = async (ticketId: string, status: Ticket["status"]) => {
    try { await api.updateTicket(ticketId, { status }); onUpdated(); } catch (error) { onError(error instanceof Error ? error.message : "状态更新失败"); }
  };
  return <section className="page"><div className="panel"><div className="panel-header"><div><span className="eyebrow">CASE MANAGEMENT</span><h3>售后工单</h3></div><button className="primary-button compact" onClick={create}><Plus size={16} />新建工单</button></div><div className="ticket-create"><input value={customerName} onChange={(event) => setCustomerName(event.target.value)} placeholder="客户姓名" /><input value={issueDescription} onChange={(event) => setIssueDescription(event.target.value)} placeholder="问题描述，填写后点击新建工单" /></div><div className="table">{tickets.map((ticket) => <div className="table-row" key={ticket.id}><strong>{ticket.ticketNo}</strong><span>{ticket.customerName}</span><span>{ticket.issueDescription}</span><select className="status-select" value={ticket.status} onChange={(event) => void updateStatus(ticket.id, event.target.value as Ticket["status"])}><option value="OPEN">OPEN</option><option value="IN_PROGRESS">IN_PROGRESS</option><option value="RESOLVED">RESOLVED</option><option value="CLOSED">CLOSED</option></select><span className={`priority ${ticket.priority.toLowerCase()}`}>{ticket.priority}</span></div>)}{tickets.length === 0 && <Empty text="还没有工单" />}</div></div></section>;
}

function AssistantView({ knowledgeBases, onError }: { knowledgeBases: KnowledgeBase[]; onError: (message: string) => void }) {
  const [query, setQuery] = useState("");
  const [mode, setMode] = useState("react");
  const [response, setResponse] = useState<ChatResponse>();
  const [streamingAnswer, setStreamingAnswer] = useState("");
  const [loading, setLoading] = useState(false);
  const submit = async () => {
    if (!query.trim()) return;
    const activeKnowledgeBaseIds = knowledgeBases
      .filter((item) => item.status === "ACTIVE")
      .map((item) => item.id);
    setLoading(true);
    setStreamingAnswer("");
    setResponse(undefined);
    try {
      await api.chatStream(query, activeKnowledgeBaseIds, mode, {
        onMessage: (content) => setStreamingAnswer((current) => current + content),
        onDone: (result) => {
          setResponse(result);
          setStreamingAnswer("");
        },
      });
    } catch (error) {
      onError(error instanceof Error ? error.message : "Agent 请求失败");
    } finally {
      setLoading(false);
    }
  };
  return <section className="page assistant-page"><div className="assistant-layout"><div className="assistant-main"><div className="assistant-intro"><div className="capability-icon large"><Bot /></div><div><span className="eyebrow">AGENT WORKSPACE</span><h2>售后智能问答</h2><p>答案将附带知识库引用和执行轨迹，支持 ReAct 与 Plan & Execute。</p></div></div>{response ? <div className="answer"><div className="answer-label"><Bot size={16} />Agent 回答 {response.fallbackUsed && <span className="tag">LOCAL FALLBACK</span>}</div><p>{response.answer}</p>{response.citations.length > 0 && <div className="citations"><strong>引用来源</strong>{response.citations.map((citation) => <div key={citation.chunkId}><FileText size={14} /><span>{citation.fileName}</span><small>{citation.content.slice(0, 120)}...</small></div>)}</div>}</div> : streamingAnswer ? <div className="answer"><div className="answer-label"><Bot size={16} />Agent 正在生成</div><p>{streamingAnswer}<span className="stream-cursor" /></p></div> : <Empty text="输入客户问题，开始一次可追溯的 Agent 推理" />}{<div className="chat-composer"><textarea value={query} disabled={loading} onChange={(event) => setQuery(event.target.value)} onKeyDown={(event) => { if (event.key === "Enter" && !event.shiftKey) { event.preventDefault(); void submit(); } }} placeholder="例如：A100设备出现E03报警，应该如何排查？" /><div><select value={mode} disabled={loading} onChange={(event) => setMode(event.target.value)}><option value="react">ReAct</option><option value="plan_execute">Plan & Execute</option></select><button className="send-button" title="发送问题" onClick={() => void submit()} disabled={loading}><Send size={18} /></button></div></div>}</div><div className="trace-panel"><span className="eyebrow">EXECUTION TRACE</span><h4>本次调用</h4>{(response?.trace || [loading ? "Agent 正在执行..." : "等待 Agent 调用"]).map((step, index) => <div className="trace-step" key={`${step}-${index}`}><span>{String(index + 1).padStart(2, "0")}</span>{step}</div>)}{response && <div className="token-box"><span>Token usage</span><strong>{response.tokenUsage.totalTokens}</strong><small>prompt {response.tokenUsage.promptTokens} · completion {response.tokenUsage.completionTokens}</small></div>}</div></div></section>;
}

function Empty({ text }: { text: string }) { return <div className="empty"><FileText size={22} /><span>{text}</span></div>; }
function viewTitle(view: View) { return ({ overview: "运营总览", knowledge: "知识库中心", tickets: "售后工单", assistant: "智能问答" })[view]; }
function formatBytes(bytes: number) { return bytes < 1024 * 1024 ? `${Math.ceil(bytes / 1024)} KB` : `${(bytes / 1024 / 1024).toFixed(1)} MB`; }

export default App;
