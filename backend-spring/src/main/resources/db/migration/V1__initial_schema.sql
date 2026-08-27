-- 基础业务表。所有业务表显式保存 tenant_id，避免把租户隔离只寄托在应用层。
create table if not exists user_accounts (
    id varchar(36) primary key,
    tenant_id varchar(64) not null,
    username varchar(100) not null,
    display_name varchar(100) not null,
    password_hash varchar(255) not null,
    roles varchar(500) not null,
    status varchar(20) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_user_tenant_username unique (tenant_id, username)
);

create table if not exists knowledge_bases (
    id varchar(36) primary key,
    tenant_id varchar(64) not null,
    name varchar(100) not null,
    description varchar(500),
    status varchar(20) not null,
    created_by varchar(64) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_knowledge_base_tenant_name unique (tenant_id, name)
);

create table if not exists documents (
    id varchar(36) primary key,
    tenant_id varchar(64) not null,
    knowledge_base_id varchar(36) not null,
    file_name varchar(255) not null,
    file_type varchar(32) not null,
    file_size bigint not null,
    storage_path varchar(500) not null,
    status varchar(20) not null,
    failure_reason varchar(1000),
    uploaded_by varchar(64) not null,
    uploaded_at timestamp not null,
    updated_at timestamp not null,
    constraint fk_documents_knowledge_base
        foreign key (knowledge_base_id) references knowledge_bases (id)
);

create table if not exists support_tickets (
    id varchar(36) primary key,
    tenant_id varchar(64) not null,
    ticket_no varchar(40) not null,
    customer_name varchar(100) not null,
    product_model varchar(100),
    issue_description varchar(2000) not null,
    priority varchar(20) not null,
    status varchar(20) not null,
    assigned_to varchar(64),
    created_by varchar(64) not null,
    created_at timestamp not null,
    updated_at timestamp not null,
    constraint uk_ticket_tenant_no unique (tenant_id, ticket_no)
);

create table if not exists audit_logs (
    id varchar(36) primary key,
    tenant_id varchar(64) not null,
    actor_id varchar(64) not null,
    action varchar(100) not null,
    resource_type varchar(50) not null,
    resource_id varchar(100),
    detail_json text,
    created_at timestamp not null
);

create table if not exists chat_sessions (
    id varchar(36) primary key,
    tenant_id varchar(64) not null,
    user_id varchar(64) not null,
    title varchar(200),
    created_at timestamp not null,
    updated_at timestamp not null
);

create table if not exists chat_messages (
    id varchar(36) primary key,
    session_id varchar(36) not null,
    tenant_id varchar(64) not null,
    role varchar(20) not null,
    content text not null,
    citations_json text,
    token_count integer,
    created_at timestamp not null,
    constraint fk_chat_messages_session
        foreign key (session_id) references chat_sessions (id)
);

create index if not exists idx_knowledge_bases_tenant
    on knowledge_bases (tenant_id, created_at desc);

create index if not exists idx_documents_knowledge_base
    on documents (tenant_id, knowledge_base_id, uploaded_at desc);

create index if not exists idx_documents_status
    on documents (tenant_id, status);

create index if not exists idx_support_tickets_tenant_status
    on support_tickets (tenant_id, status, updated_at desc);

create index if not exists idx_audit_logs_tenant_created
    on audit_logs (tenant_id, created_at desc);

create index if not exists idx_chat_messages_session_created
    on chat_messages (session_id, created_at);
