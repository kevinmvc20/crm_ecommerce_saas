-- ============================================================================
-- 1. EXTENSIONES POSTGRESQL
-- ============================================================================
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- ============================================================================
-- 2. NÚCLEO SAAS & SEGURIDAD (UML: Paquete 1)
-- ============================================================================
CREATE TABLE plan_saas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio_mensual NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    limite_usuarios INT NOT NULL DEFAULT 5,
    limite_productos INT NOT NULL DEFAULT 100,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE tenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_saas_id INT NOT NULL REFERENCES plan_saas(id) ON DELETE RESTRICT,
    nombre_comercial VARCHAR(150) NOT NULL,
    subdominio VARCHAR(80) NOT NULL UNIQUE,
    estado_suscripcion VARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
    fecha_vencimiento_plan TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE rol (
    id SERIAL PRIMARY KEY,
    tenant_id UUID NULL REFERENCES tenant(id) ON DELETE CASCADE,
    nombre VARCHAR(50) NOT NULL
);

CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    rol_id INT NOT NULL REFERENCES rol(id) ON DELETE RESTRICT,
    nombre_completo VARCHAR(200) NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_tenant ON usuario(tenant_id);
CREATE INDEX idx_tenant_subdominio ON tenant(subdominio);

-- ============================================================================
-- 3. CRM & GESTIÓN COMERCIAL (UML: Paquete 2)
-- ============================================================================
CREATE TABLE lead (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    vendedor_id UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    nombre VARCHAR(150) NOT NULL,
    email VARCHAR(150),
    telefono VARCHAR(50),
    estado VARCHAR(50) NOT NULL DEFAULT 'NUEVO',
    score INTEGER DEFAULT 0,
    notas_calificacion TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP
);

CREATE TABLE cliente (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    usuario_id_auth UUID NULL REFERENCES usuario(id) ON DELETE SET NULL,
    lead_id_origen UUID NULL REFERENCES lead(id) ON DELETE SET NULL,
    razon_social_o_nombre VARCHAR(200) NOT NULL,
    ci_nit VARCHAR(50) NOT NULL,
    email VARCHAR(150),
    telefono VARCHAR(50),
    clv_acumulado NUMERIC(12,2) DEFAULT 0.00,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE oportunidad (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    cliente_id UUID NOT NULL REFERENCES cliente(id) ON DELETE RESTRICT,
    vendedor_id UUID NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    titulo VARCHAR(200) NOT NULL,
    monto_estimado NUMERIC(12,2) NOT NULL DEFAULT 0.00,
    etapa VARCHAR(50) NOT NULL,
    probabilidad INTEGER NOT NULL DEFAULT 10,
    motivo_cierre TEXT,
    fecha_cierre_estimada DATE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE interaccion_crm (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    cliente_id UUID NULL REFERENCES cliente(id) ON DELETE CASCADE,
    lead_id UUID NULL REFERENCES lead(id) ON DELETE CASCADE,
    ejecutivo_id UUID NOT NULL REFERENCES usuario(id) ON DELETE RESTRICT,
    tipo VARCHAR(50) NOT NULL,
    descripcion TEXT NOT NULL,
    fecha_hora TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_lead_tenant ON lead(tenant_id);
CREATE INDEX idx_cliente_tenant ON cliente(tenant_id);
CREATE INDEX idx_oportunidad_tenant ON oportunidad(tenant_id);
CREATE INDEX idx_oportunidad_tenant_etapa ON oportunidad(tenant_id, etapa);
CREATE INDEX idx_interaccion_tenant_lead ON interaccion_crm(tenant_id, lead_id);
CREATE INDEX idx_interaccion_tenant_cliente ON interaccion_crm(tenant_id, cliente_id);

-- ============================================================================
-- 4. CATÁLOGO E INVENTARIO MULTI-SUCURSAL (UML: Paquete 4)
-- ============================================================================
CREATE TABLE sucursal (
    id SERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    nombre VARCHAR(120) NOT NULL,
    direccion VARCHAR(250) NULL,
    telefono VARCHAR(30) NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE categoria (
    id SERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    categoria_padre_id INT NULL REFERENCES categoria(id) ON DELETE RESTRICT,
    nombre VARCHAR(100) NOT NULL,
    descripcion TEXT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    categoria_id INT NULL REFERENCES categoria(id) ON DELETE SET NULL,
    nombre VARCHAR(150) NOT NULL,
    descripcion TEXT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE variante_producto (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    producto_id UUID NOT NULL REFERENCES producto(id) ON DELETE CASCADE,
    sku VARCHAR(60) NOT NULL,
    nombre_variante VARCHAR(100) NOT NULL,
    precio NUMERIC(12,2) NOT NULL DEFAULT 0.00 CHECK (precio >= 0),
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_variante_tenant_sku UNIQUE (tenant_id, sku)
);

CREATE TABLE stock_sucursal (
    id BIGSERIAL PRIMARY KEY,
    tenant_id UUID NOT NULL REFERENCES tenant(id) ON DELETE RESTRICT,
    sucursal_id INT NOT NULL REFERENCES sucursal(id) ON DELETE RESTRICT,
    variante_producto_id UUID NOT NULL REFERENCES variante_producto(id) ON DELETE CASCADE,
    stock_fisico INT NOT NULL DEFAULT 0 CHECK (stock_fisico >= 0),
    stock_reservado INT NOT NULL DEFAULT 0 CHECK (stock_reservado >= 0),
    stock_minimo INT NOT NULL DEFAULT 5 CHECK (stock_minimo >= 0),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uk_stock_sucursal_variante UNIQUE (sucursal_id, variante_producto_id),
    CONSTRAINT chk_stock_coherencia CHECK (stock_fisico >= stock_reservado)
);

CREATE INDEX idx_sucursal_tenant ON sucursal(tenant_id);
CREATE INDEX idx_categoria_tenant ON categoria(tenant_id);
CREATE INDEX idx_producto_tenant ON producto(tenant_id);
CREATE INDEX idx_variante_tenant ON variante_producto(tenant_id);
CREATE INDEX idx_variante_producto ON variante_producto(producto_id);
CREATE INDEX idx_stock_tenant ON stock_sucursal(tenant_id);
CREATE INDEX idx_stock_sucursal ON stock_sucursal(sucursal_id);

-- ============================================================================
-- 5. SEMILLA INICIAL (SEED DATA)
-- ============================================================================
INSERT INTO plan_saas (id, nombre, precio_mensual, limite_usuarios, limite_productos, activo) VALUES
(1, 'Básico', 15.00, 5, 500, TRUE),
(2, 'Pro', 45.00, 12, 2500, TRUE),
(3, 'Business', 89.00, 25, -1, TRUE);

INSERT INTO rol (id, tenant_id, nombre) VALUES
(1, NULL, 'ROLE_SUPER_ADMIN'),
(2, NULL, 'ROLE_ADMIN_EMPRESA'),
(3, NULL, 'ROLE_VENDEDOR'),
(4, NULL, 'ROLE_CLIENTE');

INSERT INTO usuario (id, tenant_id, rol_id, nombre_completo, email, password_hash, tipo_usuario, activo)
VALUES (
    gen_random_uuid(),
    NULL,
    1,
    'Super Administrador Global',
    'superadmin@saas.com',
    crypt('admin123', gen_salt('bf', 10)),
    'SUPER_ADMIN',
    TRUE
);