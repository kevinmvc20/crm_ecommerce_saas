-- ============================================================================
-- V2: Tabla de auditoria de pagos de suscripcion SaaS
-- ============================================================================
CREATE TABLE pago_suscripcion_saas (
    id             UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id      UUID          NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    plan_saas_id   INT           NOT NULL REFERENCES plan_saas(id) ON DELETE RESTRICT,
    monto          DECIMAL(12,2) NOT NULL,
    metodo_pago    VARCHAR(30)   NOT NULL,
    transaccion_id VARCHAR(100)  NOT NULL,
    estado         VARCHAR(30)   NOT NULL DEFAULT 'PENDIENTE',
    created_at     TIMESTAMP     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_pago_saas_tenant ON pago_suscripcion_saas(tenant_id);
CREATE INDEX idx_pago_saas_plan   ON pago_suscripcion_saas(plan_saas_id);
