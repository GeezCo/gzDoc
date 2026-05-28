-- 创建数据库
CREATE DATABASE IF NOT EXISTS gzdoc;

-- 切换到gzdoc数据库
\c gzdoc;

-- 创建租户表
CREATE TABLE IF NOT EXISTS t_tenant (
    id BIGSERIAL PRIMARY KEY,
    name VARCHAR(100) NOT NULL,
    code VARCHAR(50) UNIQUE NOT NULL,
    plan VARCHAR(20) NOT NULL DEFAULT 'basic',
    status SMALLINT DEFAULT 1,
    expired_at TIMESTAMP,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

-- 创建用户表
CREATE TABLE IF NOT EXISTS t_user (
    id BIGSERIAL PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(100),
    phone VARCHAR(20),
    tenant_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'user',
    status SMALLINT DEFAULT 1,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES t_tenant(id)
);

-- 创建文档表
CREATE TABLE IF NOT EXISTS t_document (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_size BIGINT,
    file_type VARCHAR(50),
    page_count INT,
    status SMALLINT DEFAULT 0,
    process_progress INT DEFAULT 0,
    error_msg TEXT,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES t_tenant(id),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);

-- 创建问答记录表
CREATE TABLE IF NOT EXISTS t_qa_record (
    id BIGSERIAL PRIMARY KEY,
    tenant_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    question TEXT NOT NULL,
    answer TEXT,
    sources JSONB,
    feedback SMALLINT,
    token_count INT,
    cost DECIMAL(10, 4),
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (tenant_id) REFERENCES t_tenant(id),
    FOREIGN KEY (user_id) REFERENCES t_user(id)
);

-- 创建索引
CREATE INDEX idx_user_tenant ON t_user(tenant_id);
CREATE INDEX idx_user_username ON t_user(username);
CREATE INDEX idx_document_tenant ON t_document(tenant_id);
CREATE INDEX idx_document_user ON t_document(user_id);
CREATE INDEX idx_document_status ON t_document(status);
CREATE INDEX idx_qa_tenant ON t_qa_record(tenant_id);
CREATE INDEX idx_qa_user ON t_qa_record(user_id);
CREATE INDEX idx_qa_created ON t_qa_record(created_at);

-- 插入测试数据
INSERT INTO t_tenant (name, code, plan, status) VALUES
('测试租户', 'test_tenant', 'basic', 1);

INSERT INTO t_user (username, password, email, tenant_id, role, status) VALUES
('admin', '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iAt6Z5EH', 'admin@gzdoc.com', 1, 'admin', 1);

-- 注释
COMMENT ON TABLE t_tenant IS '租户表';
COMMENT ON TABLE t_user IS '用户表';
COMMENT ON TABLE t_document IS '文档表';
COMMENT ON TABLE t_qa_record IS '问答记录表';
