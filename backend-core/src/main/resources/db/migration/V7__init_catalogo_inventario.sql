-- ============================================================================
-- V7: Módulo 4 — Catálogo e Inventario Multi-Sucursal
-- Tablas: sucursal, categoria, producto, variante_producto, stock_sucursal
-- ============================================================================

-- ─── 1. Sucursal ─────────────────────────────────────────────────────────────
CREATE TABLE sucursal (
    id         SERIAL       PRIMARY KEY,
    tenant_id  UUID         NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    nombre     VARCHAR(120) NOT NULL,
    direccion  VARCHAR(250) NULL,
    telefono   VARCHAR(30)  NULL,
    activo     BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_sucursal_tenant ON sucursal(tenant_id);

-- ─── 2. Categoria ────────────────────────────────────────────────────────────
CREATE TABLE categoria (
    id                SERIAL       PRIMARY KEY,
    tenant_id         UUID         NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    categoria_padre_id INT         NULL     REFERENCES categoria(id) ON DELETE RESTRICT,
    nombre            VARCHAR(100) NOT NULL,
    descripcion       TEXT         NULL,
    activo            BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at        TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_categoria_tenant ON categoria(tenant_id);

-- ─── 3. Producto ─────────────────────────────────────────────────────────────
CREATE TABLE producto (
    id           UUID         PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id    UUID         NOT NULL REFERENCES tenant(id)   ON DELETE RESTRICT,
    categoria_id INT          NULL     REFERENCES categoria(id) ON DELETE SET NULL,
    nombre       VARCHAR(150) NOT NULL,
    descripcion  TEXT         NULL,
    activo       BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at   TIMESTAMP    NOT NULL DEFAULT NOW(),
    updated_at   TIMESTAMP    NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_producto_tenant ON producto(tenant_id);

-- ─── 4. Variante de Producto ─────────────────────────────────────────────────
CREATE TABLE variante_producto (
    id              UUID          PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id       UUID          NOT NULL REFERENCES tenant(id)   ON DELETE RESTRICT,
    producto_id     UUID          NOT NULL REFERENCES producto(id)  ON DELETE CASCADE,
    sku             VARCHAR(60)   NOT NULL,
    nombre_variante VARCHAR(100)  NOT NULL,
    precio          NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (precio >= 0),
    activo          BOOLEAN       NOT NULL DEFAULT TRUE,
    created_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    updated_at      TIMESTAMP     NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_variante_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE INDEX idx_variante_tenant   ON variante_producto(tenant_id);
CREATE INDEX idx_variante_producto ON variante_producto(producto_id);

-- ─── 5. Stock por Sucursal ───────────────────────────────────────────────────
CREATE TABLE stock_sucursal (
    id                  BIGSERIAL NOT NULL PRIMARY KEY,
    tenant_id           UUID      NOT NULL REFERENCES tenant(id)            ON DELETE RESTRICT,
    sucursal_id         INT       NOT NULL REFERENCES sucursal(id)          ON DELETE RESTRICT,
    variante_producto_id UUID     NOT NULL REFERENCES variante_producto(id) ON DELETE CASCADE,
    stock_fisico        INT       NOT NULL DEFAULT 0 CHECK (stock_fisico    >= 0),
    stock_reservado     INT       NOT NULL DEFAULT 0 CHECK (stock_reservado >= 0),
    stock_minimo        INT       NOT NULL DEFAULT 5 CHECK (stock_minimo    >= 0),
    created_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_at          TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT uk_stock_sucursal_variante  UNIQUE (sucursal_id, variante_producto_id),
    CONSTRAINT chk_stock_coherencia        CHECK  (stock_fisico >= stock_reservado)
);

CREATE INDEX idx_stock_tenant   ON stock_sucursal(tenant_id);
CREATE INDEX idx_stock_sucursal ON stock_sucursal(sucursal_id);
