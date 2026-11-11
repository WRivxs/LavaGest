-- ============================================================
-- MIGRACIÓN V3: TELÉFONO DE CLIENTE Y SOPORTE DE LIQUIDACIÓN
-- ============================================================

-- 1. Agregar columna client_phone a la tabla washes
ALTER TABLE washes
    ADD COLUMN IF NOT EXISTS client_phone VARCHAR(30);

-- 2. Asegurar índices para búsquedas rápidas por placa y teléfono
CREATE INDEX IF NOT EXISTS idx_washes_plate ON washes(plate);
CREATE INDEX IF NOT EXISTS idx_washes_client_phone ON washes(client_phone);
