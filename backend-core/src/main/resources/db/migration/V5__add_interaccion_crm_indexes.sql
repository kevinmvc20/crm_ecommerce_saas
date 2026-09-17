-- ============================================================================
-- V5 – Índices compuestos adicionales para interaccion_crm (multi-tenant)
-- Compatible con PostgreSQL 16
-- ============================================================================
-- Los índices simples ya existen en V4. Aquí agregamos los compuestos
-- que aceleran las consultas filtradas por (tenant_id, lead_id) y
-- (tenant_id, cliente_id) simultáneamente.
-- ============================================================================

CREATE INDEX idx_interaccion_tenant_lead
    ON interaccion_crm (tenant_id, lead_id);

CREATE INDEX idx_interaccion_tenant_cliente
    ON interaccion_crm (tenant_id, cliente_id);
