-- ============================================================================
-- V6 – Oportunidad: columna updated_at + índice compuesto (tenant_id, etapa)
-- Compatible con PostgreSQL 16
-- ============================================================================

-- ─── 1. Agregar columna updated_at ────────────────────────────────────────────
ALTER TABLE oportunidad
    ADD COLUMN IF NOT EXISTS updated_at TIMESTAMP WITHOUT TIME ZONE
        NOT NULL DEFAULT NOW();

-- ─── 2. Índice compuesto para filtrar por tenant + etapa (Kanban por columna) ──
CREATE INDEX IF NOT EXISTS idx_oportunidad_tenant_etapa
    ON oportunidad(tenant_id, etapa);
