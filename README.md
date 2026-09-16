# CRM & eCommerce B2B Multi-Tenant SaaS

Plataforma empresarial de comercio unificado y gestión de relaciones con clientes, diseñada bajo una arquitectura multitenancy con aislamiento lógico de datos por inquilino (`tenant_id`), motor centralizado de suscripciones SaaS y consola administrativa para operaciones comerciales.

---

## Stack Tecnológico

* **Backend Core:** Java 21 LTS, Spring Boot 3.3.3, Spring Security 6 (Autenticación JWT Stateless), Spring Data JPA / Hibernate 6, Flyway Migrations.
* **Frontend Web:** Angular 18 (Arquitectura Standalone, Signals para gestión reactiva de estado, Reactive Forms, Guards e Interceptores HTTP funcionales), Tailwind CSS.
* **Base de Datos:** PostgreSQL 16 con soporte para aislamiento multitenant y claves primarias UUID.
* **Microservicios y Móvil (En integración):** FastAPI (Python 3.12 para reportes analíticos y procesamiento asíncrono) y Flutter (Cliente móvil multiplataforma para clientes finales).
* **Arquitectura:** Monorepo modular desacoplado.

---

## Requisitos del Entorno

Asegúrate de tener instaladas las siguientes herramientas antes de iniciar el proyecto localmente:

* **Java Development Kit (JDK):** Versión 21 LTS[cite: 1, 2].
* **Apache Maven:** 3.9+[cite: 2].
* **Node.js:** Versión 20.x o superior[cite: 2].
* **npm:** Versión 10.x o superior.
* **PostgreSQL:** Versión 16 instalada y corriendo en el puerto estándar `5432`[cite: 1, 2].

---

## Puesta en Marcha Local

### 1. Base de Datos
Accede a PostgreSQL y crea la base de datos central antes de inicializar cualquier servicio[cite: 2]:

```sql
CREATE DATABASE crm_ecommerce_saas;

```

> **Configuración de credenciales:** Los parámetros de conexión locales se encuentran en `backend-core/src/main/resources/application.properties` (`user: postgres` / `pass: admin123`). El esquema de base de datos, relaciones foráneas e índices iniciales se crean automáticamente al arrancar el backend mediante Flyway (`V1__init_saas_core_security.sql`).
> 
> 

### 2. Backend Core (Spring Boot)

Abre una terminal en el directorio del backend:

```bash
cd backend-core
mvn clean compile
mvn spring-boot:run

```

El servidor arrancará y quedará a la escucha en `http://localhost:8080`.

### 3. Frontend Web (Angular 18)

Abre una segunda terminal en el directorio del cliente web:

```bash
cd frontend-web
npm install
npm start

```

La aplicación web compilará en modo observador (*hot reload*) en `http://localhost:4200`.

---

## Credenciales Iniciales de Acceso

Para acceder al Backoffice Administrativo Global con permisos de plataforma (`ROLE_SUPER_ADMIN`):

* **URL de Acceso:** `http://localhost:4200/login`
* **Correo Electrónico:** `superadmin@saas.com`

* **Contraseña:** `admin123`


---

## Estructura del Monorepo

```text
crm_ecommerce_saas/
├── backend-core/                 # Núcleo transaccional y reglas de negocio en Spring Boot 3
│   ├── src/main/java/com/saas/crm/
│   │   ├── controller/           # Endpoints REST (Auth, Tenants, Planes)
│   │   ├── domain/entity/        # Entidades JPA (Usuario, Rol, Tenant, PlanSaaS)
│   │   ├── dto/                  # Records inmutables de transferencia de datos
│   │   ├── repository/           # Repositorios JPA de Spring Data
│   │   ├── security/             # Filtros JWT, interceptores y SecurityConfig
│   │   └── service/              # Lógica de negocio y transacciones de base de datos
│   └── src/main/resources/
│       ├── db/migration/         # Scripts SQL versionados de Flyway
│       └── application.properties# Configuración de entorno y base de datos
├── frontend-web/                 # Panel SPA corporativo desarrollado en Angular 18
│   ├── src/app/
│   │   ├── core/                 # Servicios globales (Auth, Tenants), Guards e Interceptores
│   │   ├── features/
│   │   │   ├── auth/             # Módulo y formulario de autenticación
│   │   │   ├── dashboard/        # Layout base administrativo y KPIs
│   │   │   └── tenants/          # Gestión de empresas, subdominios y planes
│   │   └── shared/               # Componentes transversales y directivas
│   └── tailwind.config.js        # Tokens y paleta corporativa
└── README.md                     # Manual técnico y guía de despliegue

