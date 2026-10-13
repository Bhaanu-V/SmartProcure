-- ==============================================================================
-- SmartProcure Schema Migration V3
-- Purchase Requests, PR Line Items, Audit Logs, and Notifications
-- ==============================================================================

-- 1. Purchase Requests Table
CREATE TABLE IF NOT EXISTS purchase_requests (
    id BIGSERIAL PRIMARY KEY,
    request_number VARCHAR(64) NOT NULL UNIQUE,
    title VARCHAR(255) NOT NULL,
    description TEXT,
    department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL,
    requester_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    total_amount NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    currency VARCHAR(10) NOT NULL DEFAULT 'USD',
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    priority VARCHAR(20) NOT NULL DEFAULT 'MEDIUM',
    rejection_reason TEXT,
    ai_risk_score NUMERIC(5, 2),
    ai_recommendation VARCHAR(50),
    ai_analysis_summary TEXT,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pr_total_non_negative CHECK (total_amount >= 0)
);

-- 2. Purchase Request Line Items Table
CREATE TABLE IF NOT EXISTS pr_line_items (
    id BIGSERIAL PRIMARY KEY,
    request_id BIGINT NOT NULL REFERENCES purchase_requests(id) ON DELETE CASCADE,
    catalog_item_id BIGINT REFERENCES catalog_items(id) ON DELETE SET NULL,
    item_description VARCHAR(255) NOT NULL,
    category VARCHAR(100),
    quantity NUMERIC(10, 2) NOT NULL DEFAULT 1.00,
    unit_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    total_price NUMERIC(15, 2) NOT NULL DEFAULT 0.00,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_pr_qty_positive CHECK (quantity > 0),
    CONSTRAINT chk_pr_price_non_negative CHECK (unit_price >= 0),
    CONSTRAINT chk_pr_line_total_non_negative CHECK (total_price >= 0)
);

-- 3. Audit Logs Table (Immutable audit trail for compliance)
CREATE TABLE IF NOT EXISTS audit_logs (
    id BIGSERIAL PRIMARY KEY,
    entity_name VARCHAR(100) NOT NULL,
    entity_id BIGINT NOT NULL,
    action VARCHAR(100) NOT NULL,
    actor_id BIGINT REFERENCES users(id) ON DELETE SET NULL,
    actor_username VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(45),
    timestamp TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 4. In-App Notifications Table
CREATE TABLE IF NOT EXISTS notifications (
    id BIGSERIAL PRIMARY KEY,
    recipient_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,
    title VARCHAR(255) NOT NULL,
    message TEXT NOT NULL,
    type VARCHAR(50) NOT NULL DEFAULT 'INFO',
    reference_type VARCHAR(50),
    reference_id BIGINT,
    is_read BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP WITH TIME ZONE NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 5. Extend Purchase Orders Table with Request and Supplier linkages
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'purchase_orders' AND column_name = 'request_id') THEN
        ALTER TABLE purchase_orders ADD COLUMN request_id BIGINT REFERENCES purchase_requests(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'purchase_orders' AND column_name = 'supplier_id') THEN
        ALTER TABLE purchase_orders ADD COLUMN supplier_id BIGINT REFERENCES suppliers(id) ON DELETE SET NULL;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'purchase_orders' AND column_name = 'department_id') THEN
        ALTER TABLE purchase_orders ADD COLUMN department_id BIGINT REFERENCES departments(id) ON DELETE SET NULL;
    END IF;
END $$;

-- Indices for performance
CREATE INDEX IF NOT EXISTS idx_pr_status ON purchase_requests(status);
CREATE INDEX IF NOT EXISTS idx_pr_requester ON purchase_requests(requester_id);
CREATE INDEX IF NOT EXISTS idx_pr_dept ON purchase_requests(department_id);
CREATE INDEX IF NOT EXISTS idx_pr_items_request ON pr_line_items(request_id);
CREATE INDEX IF NOT EXISTS idx_audit_entity ON audit_logs(entity_name, entity_id);
CREATE INDEX IF NOT EXISTS idx_audit_timestamp ON audit_logs(timestamp);
CREATE INDEX IF NOT EXISTS idx_notif_recipient ON notifications(recipient_id, is_read);
CREATE INDEX IF NOT EXISTS idx_po_request_id ON purchase_orders(request_id);
CREATE INDEX IF NOT EXISTS idx_po_supplier_id ON purchase_orders(supplier_id);
