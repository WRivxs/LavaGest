-- ============================================================
-- LavaGest PRO - Migración v2: Roles, Catálogos y Tipos de Lavado
-- Ejecutar en Supabase SQL Editor
-- ============================================================

-- 1. Tabla de usuarios del sistema (login)
CREATE TABLE IF NOT EXISTS app_users (
    id              BIGSERIAL PRIMARY KEY,
    username        VARCHAR(50) NOT NULL UNIQUE,
    password_hash   VARCHAR(64) NOT NULL,  -- SHA-256 hex
    role            VARCHAR(20) NOT NULL DEFAULT 'encargado' CHECK (role IN ('admin', 'encargado')),
    worker_id       BIGINT REFERENCES workers(id) ON DELETE SET NULL,
    active          BOOLEAN NOT NULL DEFAULT true,
    created_at      TIMESTAMP DEFAULT NOW()
);

-- 2. Catálogo de tipos de vehículo (dinámico, creado por admin)
CREATE TABLE IF NOT EXISTS vehicle_types_catalog (
    id      BIGSERIAL PRIMARY KEY,
    name    VARCHAR(50) NOT NULL UNIQUE,
    active  BOOLEAN NOT NULL DEFAULT true
);

-- 3. Tipos de lavado (dinámico, cada uno asignado a un tipo de vehículo)
CREATE TABLE IF NOT EXISTS wash_types (
    id                      BIGSERIAL PRIMARY KEY,
    name                    VARCHAR(100) NOT NULL,
    price                   NUMERIC(12, 2) NOT NULL CHECK (price >= 0),
    vehicle_type_catalog_id BIGINT NOT NULL REFERENCES vehicle_types_catalog(id) ON DELETE CASCADE,
    active                  BOOLEAN NOT NULL DEFAULT true,
    UNIQUE(name, vehicle_type_catalog_id)
);

-- 4. Agregar columnas nuevas a la tabla washes existente
ALTER TABLE washes
    ADD COLUMN IF NOT EXISTS wash_type_id      BIGINT REFERENCES wash_types(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS client_name       VARCHAR(100),
    ADD COLUMN IF NOT EXISTS created_by_user_id BIGINT REFERENCES app_users(id) ON DELETE SET NULL;

-- ============================================================
-- DATOS INICIALES
-- ============================================================

-- Tipos de vehículo base
INSERT INTO vehicle_types_catalog (name) VALUES ('Moto') ON CONFLICT (name) DO NOTHING;
INSERT INTO vehicle_types_catalog (name) VALUES ('Carro') ON CONFLICT (name) DO NOTHING;

-- Tipos de lavado para Moto
INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Lavado normal', 7000, id FROM vehicle_types_catalog WHERE name = 'Moto'
ON CONFLICT DO NOTHING;

INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Brillado', 10000, id FROM vehicle_types_catalog WHERE name = 'Moto'
ON CONFLICT DO NOTHING;

-- Tipos de lavado para Carro
INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Lavado general', 30000, id FROM vehicle_types_catalog WHERE name = 'Carro'
ON CONFLICT DO NOTHING;

INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Lavado con motor', 45000, id FROM vehicle_types_catalog WHERE name = 'Carro'
ON CONFLICT DO NOTHING;

INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Lavado + brillada', 45000, id FROM vehicle_types_catalog WHERE name = 'Carro'
ON CONFLICT DO NOTHING;

INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Lavado + polichada', 50000, id FROM vehicle_types_catalog WHERE name = 'Carro'
ON CONFLICT DO NOTHING;

INSERT INTO wash_types (name, price, vehicle_type_catalog_id)
SELECT 'Completo', 70000, id FROM vehicle_types_catalog WHERE name = 'Carro'
ON CONFLICT DO NOTHING;

-- Usuario ADMIN inicial (password: admin123)
-- SHA-256 de "admin123" = 240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a
INSERT INTO app_users (username, password_hash, role, active)
VALUES ('admin', '240be518fabd2724ddb6f04eeb1da5967448d7e831c08c8fa822809f74c720a', 'admin', true)
ON CONFLICT (username) DO NOTHING;

-- ============================================================
-- VERIFICAR
-- ============================================================
SELECT 'vehicle_types_catalog' as tabla, count(*) as registros FROM vehicle_types_catalog
UNION ALL
SELECT 'wash_types', count(*) FROM wash_types
UNION ALL
SELECT 'app_users', count(*) FROM app_users;
