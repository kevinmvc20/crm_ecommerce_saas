-- 1. Extensiones requeridas para generación de identificadores UUID
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "pgcrypto";

-- 2. Tabla: PlanSaaS
CREATE TABLE plan_saas (
    id SERIAL PRIMARY KEY,
    nombre VARCHAR(100) NOT NULL,
    precio_mensual NUMERIC(12, 2) NOT NULL DEFAULT 0.00,
    limite_usuarios INT NOT NULL DEFAULT 5,
    limite_productos INT NOT NULL DEFAULT 100,
    activo BOOLEAN NOT NULL DEFAULT TRUE
);

-- 3. Tabla: Tenant (Empresa inquilina)
CREATE TABLE tenant (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    plan_saas_id INT NOT NULL,
    nombre_comercial VARCHAR(150) NOT NULL,
    subdominio VARCHAR(80) NOT NULL UNIQUE,
    estado_suscripcion VARCHAR(50) NOT NULL DEFAULT 'ACTIVO',
    fecha_vencimiento_plan TIMESTAMP,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_tenant_plan_saas FOREIGN KEY (plan_saas_id) REFERENCES plan_saas(id) ON DELETE RESTRICT
);

-- 4. Tabla: Rol
CREATE TABLE rol (
    id SERIAL PRIMARY KEY,
    tenant_id UUID NULL,
    nombre VARCHAR(50) NOT NULL,
    CONSTRAINT fk_rol_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE CASCADE
);

-- 5. Tabla: Usuario
CREATE TABLE usuario (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    tenant_id UUID NULL,
    rol_id INT NOT NULL,
    email VARCHAR(150) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    tipo_usuario VARCHAR(50) NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_usuario_tenant FOREIGN KEY (tenant_id) REFERENCES tenant(id) ON DELETE RESTRICT,
    CONSTRAINT fk_usuario_rol FOREIGN KEY (rol_id) REFERENCES rol(id) ON DELETE RESTRICT
);

-- 6. Índices estratégicos para autenticación y aislamiento multitenant
CREATE INDEX idx_usuario_email ON usuario(email);
CREATE INDEX idx_usuario_tenant ON usuario(tenant_id);
CREATE INDEX idx_tenant_subdominio ON tenant(subdominio);

-- ============================================================================
-- SEED DATA INICIAL PARA SPRINT 1 (Login SuperAdmin)
-- ============================================================================

-- Plan SaaS inicial por defecto
INSERT INTO plan_saas (id, nombre, precio_mensual, limite_usuarios, limite_productos, activo)
VALUES (1, 'Plan Empresarial Base', 49.99, 10, 500, TRUE);

-- Roles predeterminados de la plataforma
INSERT INTO rol (id, tenant_id, nombre) VALUES
(1, NULL, 'ROLE_SUPER_ADMIN'),
(2, NULL, 'ROLE_ADMIN_EMPRESA'),
(3, NULL, 'ROLE_VENDEDOR'),
(4, NULL, 'ROLE_CLIENTE');

-- Usuario SUPER_ADMIN inicial
-- Credenciales de acceso:
-- Email: superadmin@saas.com
-- Contraseña en texto plano: admin123 (hash encriptado con BCrypt costo 10)
INSERT INTO usuario (id, tenant_id, rol_id, email, password_hash, tipo_usuario, activo)
VALUES (
    gen_random_uuid(),
    NULL,
    1,
    'superadmin@saas.com',
    '$2a$10$N.zmdr9k7uOCQb376NoUnuTJ8iAt6Z5EHsM8lE9lBOsl7iKTVKIUi',
    'SUPER_ADMIN',
    TRUE
);