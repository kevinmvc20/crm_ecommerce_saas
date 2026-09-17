-- ============================================================================
-- V4 – Módulo CRM: lead, cliente, oportunidad, interaccion_crm
-- Compatible con PostgreSQL 16
-- ============================================================================

-- ─────────────────────────────────────────────────────────────────────────────
-- 1. Tabla: lead (prospecto comercial)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE lead (
    id                  UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id           UUID            NOT NULL,
    vendedor_id         UUID            NULL,
    nombre              VARCHAR(150)    NOT NULL,
    email               VARCHAR(150)    NULL,
    telefono            VARCHAR(50)     NULL,
    estado              VARCHAR(50)     NOT NULL DEFAULT 'NUEVO',
    score               INTEGER         NOT NULL DEFAULT 0,
    notas_calificacion  TEXT            NULL,
    created_at          TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at          TIMESTAMP       NULL,

    CONSTRAINT fk_lead_tenant
        FOREIGN KEY (tenant_id)    REFERENCES tenant(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_lead_vendedor
        FOREIGN KEY (vendedor_id)  REFERENCES usuario(id)  ON DELETE SET NULL,
    CONSTRAINT chk_lead_estado
        CHECK (estado IN ('NUEVO', 'CONTACTADO', 'CALIFICADO', 'DESCALIFICADO', 'CONVERTIDO'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 2. Tabla: cliente (cliente convertido o directo)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE cliente (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID            NOT NULL,
    usuario_id_auth         UUID            NULL,
    lead_id_origen          UUID            NULL,
    razon_social_o_nombre   VARCHAR(200)    NOT NULL,
    ci_nit                  VARCHAR(50)     NOT NULL,
    email                   VARCHAR(150)    NULL,
    telefono                VARCHAR(50)     NULL,
    clv_acumulado           NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    activo                  BOOLEAN         NOT NULL DEFAULT TRUE,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_cliente_tenant
        FOREIGN KEY (tenant_id)       REFERENCES tenant(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_cliente_usuario_auth
        FOREIGN KEY (usuario_id_auth) REFERENCES usuario(id)  ON DELETE SET NULL,
    CONSTRAINT fk_cliente_lead_origen
        FOREIGN KEY (lead_id_origen)  REFERENCES lead(id)     ON DELETE SET NULL
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 3. Tabla: oportunidad (pipeline de ventas)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE oportunidad (
    id                      UUID            PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id               UUID            NOT NULL,
    cliente_id              UUID            NOT NULL,
    vendedor_id             UUID            NOT NULL,
    titulo                  VARCHAR(200)    NOT NULL,
    monto_estimado          NUMERIC(12,2)   NOT NULL DEFAULT 0.00,
    etapa                   VARCHAR(50)     NOT NULL DEFAULT 'CALIFICACION',
    probabilidad            INTEGER         NOT NULL DEFAULT 10,
    motivo_cierre           TEXT            NULL,
    fecha_cierre_estimada   DATE            NULL,
    created_at              TIMESTAMP       NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_oportunidad_tenant
        FOREIGN KEY (tenant_id)   REFERENCES tenant(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_oportunidad_cliente
        FOREIGN KEY (cliente_id)  REFERENCES cliente(id)  ON DELETE RESTRICT,
    CONSTRAINT fk_oportunidad_vendedor
        FOREIGN KEY (vendedor_id) REFERENCES usuario(id)  ON DELETE RESTRICT,
    CONSTRAINT chk_oportunidad_etapa
        CHECK (etapa IN ('CALIFICACION', 'PROPUESTA', 'NEGOCIACION', 'GANADA', 'PERDIDA')),
    CONSTRAINT chk_oportunidad_probabilidad
        CHECK (probabilidad BETWEEN 0 AND 100)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 4. Tabla: interaccion_crm (registro de contactos / actividades)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE TABLE interaccion_crm (
    id           UUID        PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID        NOT NULL,
    cliente_id   UUID        NULL,
    lead_id      UUID        NULL,
    ejecutivo_id UUID        NOT NULL,
    tipo         VARCHAR(50) NOT NULL,
    descripcion  TEXT        NOT NULL,
    fecha_hora   TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_interaccion_tenant
        FOREIGN KEY (tenant_id)    REFERENCES tenant(id)   ON DELETE RESTRICT,
    CONSTRAINT fk_interaccion_cliente
        FOREIGN KEY (cliente_id)   REFERENCES cliente(id)  ON DELETE SET NULL,
    CONSTRAINT fk_interaccion_lead
        FOREIGN KEY (lead_id)      REFERENCES lead(id)     ON DELETE SET NULL,
    CONSTRAINT fk_interaccion_ejecutivo
        FOREIGN KEY (ejecutivo_id) REFERENCES usuario(id)  ON DELETE RESTRICT,
    CONSTRAINT chk_interaccion_tipo
        CHECK (tipo IN ('LLAMADA', 'REUNION', 'CORREO', 'NOTA'))
);

-- ─────────────────────────────────────────────────────────────────────────────
-- 5. Índices por tenant_id (aislamiento multi-tenant)
-- ─────────────────────────────────────────────────────────────────────────────
CREATE INDEX idx_lead_tenant_id           ON lead(tenant_id);
CREATE INDEX idx_lead_vendedor_id         ON lead(vendedor_id);
CREATE INDEX idx_lead_estado              ON lead(tenant_id, estado);

CREATE INDEX idx_cliente_tenant_id        ON cliente(tenant_id);
CREATE INDEX idx_cliente_ci_nit           ON cliente(tenant_id, ci_nit);

CREATE INDEX idx_oportunidad_tenant_id    ON oportunidad(tenant_id);
CREATE INDEX idx_oportunidad_cliente_id   ON oportunidad(tenant_id, cliente_id);

CREATE INDEX idx_interaccion_tenant_id    ON interaccion_crm(tenant_id);
CREATE INDEX idx_interaccion_lead_id      ON interaccion_crm(lead_id);
CREATE INDEX idx_interaccion_cliente_id   ON interaccion_crm(cliente_id);
