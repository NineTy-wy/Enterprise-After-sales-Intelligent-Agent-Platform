export type ApiResponse<T> = {
  code: number;
  message: string;
  data: T;
};

export type KnowledgeBase = {
  id: string;
  name: string;
  description?: string;
  status: "ACTIVE" | "ARCHIVED";
  createdAt: string;
  updatedAt: string;
};

export type DocumentItem = {
  id: string;
  knowledgeBaseId: string;
  fileName: string;
  fileType: string;
  fileSize: number;
  status: "UPLOADED" | "PROCESSING" | "INDEXED" | "FAILED";
  failureReason?: string;
  uploadedAt: string;
  updatedAt: string;
};

export type Ticket = {
  id: string;
  ticketNo: string;
  customerName: string;
  productModel?: string;
  issueDescription: string;
  priority: "LOW" | "MEDIUM" | "HIGH" | "URGENT";
  status: "OPEN" | "IN_PROGRESS" | "RESOLVED" | "CLOSED";
  assignedTo?: string;
  createdAt: string;
  updatedAt: string;
};

export type ChatResponse = {
  answer: string;
  citations: Array<{
    documentId: string;
    fileName: string;
    chunkId: string;
    score: number;
    content: string;
  }>;
  trace: string[];
  tokenUsage: Record<string, number>;
  fallbackUsed: boolean;
};

export type ChatStreamHandlers = {
  onMessage: (content: string) => void;
  onDone: (response: ChatResponse) => void;
};

export type AuthResponse = {
  tokenType: string;
  accessToken: string;
  expiresInSeconds: number;
  user: { id: string; tenantId: string; username: string; displayName: string; roles: string[] };
};

const json = async <T>(url: string, init?: RequestInit): Promise<T> => {
  const token = localStorage.getItem("agent_access_token");
  const response = await fetch(url, {
    ...init,
    headers: {
      "Content-Type": "application/json",
      ...(token ? { Authorization: `Bearer ${token}` } : {}),
      ...init?.headers,
    },
  });
  const body = (await response.json()) as ApiResponse<T>;
  if (!response.ok || body.code !== 0) throw new Error(body.message);
  return body.data;
};

export const api = {
  login: (tenantId: string, username: string, password: string) =>
    json<AuthResponse>("/api/auth/login", {
      method: "POST",
      body: JSON.stringify({ tenantId, username, password }),
    }),
  knowledgeBases: () => json<KnowledgeBase[]>("/api/knowledge-bases"),
  createKnowledgeBase: (name: string, description: string) =>
    json<KnowledgeBase>("/api/knowledge-bases", {
      method: "POST",
      body: JSON.stringify({ name, description }),
    }),
  archiveKnowledgeBase: (knowledgeBaseId: string) =>
    json<void>(`/api/knowledge-bases/${knowledgeBaseId}/archive`, {
      method: "POST",
    }),
  documents: (knowledgeBaseId: string) =>
    json<DocumentItem[]>(`/api/knowledge-bases/${knowledgeBaseId}/documents`),
  uploadDocument: (knowledgeBaseId: string, file: File) => {
    const form = new FormData();
    form.append("file", file);
    const token = localStorage.getItem("agent_access_token");
    return fetch(`/api/knowledge-bases/${knowledgeBaseId}/documents/upload`, {
      method: "POST",
      body: form,
      headers: token ? { Authorization: `Bearer ${token}` } : undefined,
    }).then(async (response) => {
      const body = (await response.json()) as ApiResponse<DocumentItem>;
      if (!response.ok || body.code !== 0) throw new Error(body.message);
      return body.data;
    });
  },
  tickets: () => json<Ticket[]>("/api/tickets"),
  createTicket: (payload: object) =>
    json<Ticket>("/api/tickets", { method: "POST", body: JSON.stringify(payload) }),
  updateTicket: (ticketId: string, payload: object) =>
    json<Ticket>(`/api/tickets/${ticketId}`, {
      method: "PATCH",
      body: JSON.stringify(payload),
    }),
  chat: (query: string, knowledgeBaseIds: string[], mode: string) =>
    json<ChatResponse>("/api/agent/chat", {
      method: "POST",
      body: JSON.stringify({ query, knowledgeBaseIds, mode }),
    }),
  chatStream: async (
    query: string,
    knowledgeBaseIds: string[],
    mode: string,
    handlers: ChatStreamHandlers,
    signal?: AbortSignal,
  ) => {
    const token = localStorage.getItem("agent_access_token");
    const response = await fetch("/api/agent/chat/stream", {
      method: "POST",
      body: JSON.stringify({ query, knowledgeBaseIds, mode }),
      signal,
      headers: {
        "Content-Type": "application/json",
        Accept: "text/event-stream",
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
      },
    });
    if (!response.ok || !response.body) {
      let message = "Agent 流式请求失败";
      try {
        const body = (await response.json()) as ApiResponse<unknown>;
        message = body.message || message;
      } catch {
        // 非 JSON 错误响应保持默认提示。
      }
      throw new Error(message);
    }

    const reader = response.body.getReader();
    const decoder = new TextDecoder();
    let buffer = "";
    let eventName = "message";
    let dataLines: string[] = [];

    const dispatch = () => {
      if (dataLines.length === 0) return;
      const data = dataLines.join("\n");
      if (eventName === "done") {
        handlers.onDone(JSON.parse(data) as ChatResponse);
      } else {
        handlers.onMessage(data);
      }
      eventName = "message";
      dataLines = [];
    };

    while (true) {
      const { done, value } = await reader.read();
      buffer += decoder.decode(value, { stream: !done });
      const lines = buffer.split(/\r?\n/);
      buffer = lines.pop() || "";
      for (const line of lines) {
        if (line === "") {
          dispatch();
        } else if (line.startsWith("event:")) {
          eventName = line.slice(6).trim();
        } else if (line.startsWith("data:")) {
          dataLines.push(line.slice(5).trimStart());
        }
      }
      if (done) {
        if (buffer.startsWith("data:")) {
          dataLines.push(buffer.slice(5).trimStart());
        } else if (buffer.startsWith("event:")) {
          eventName = buffer.slice(6).trim();
        }
        dispatch();
        break;
      }
    }
  },
};
