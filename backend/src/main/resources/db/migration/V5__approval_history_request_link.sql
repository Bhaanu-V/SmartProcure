-- ==============================================================================
-- SmartProcure Schema Migration V5
-- Link Approval History to Purchase Requests and Support Multi-Tier Stages
-- ==============================================================================

ALTER TABLE approval_history ALTER COLUMN po_id DROP NOT NULL;

DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'approval_history' AND column_name = 'request_id') THEN
        ALTER TABLE approval_history ADD COLUMN request_id BIGINT REFERENCES purchase_requests(id) ON DELETE CASCADE;
    END IF;

    IF NOT EXISTS (SELECT 1 FROM information_schema.columns WHERE table_schema = 'smartprocure' AND table_name = 'approval_history' AND column_name = 'stage') THEN
        ALTER TABLE approval_history ADD COLUMN stage VARCHAR(50) DEFAULT 'MANAGER';
    END IF;
END $$;

CREATE INDEX IF NOT EXISTS idx_approval_request_id ON approval_history(request_id);
CREATE INDEX IF NOT EXISTS idx_approval_stage ON approval_history(stage);
