-- ============================================================
-- MIGRACIÓN V4: PERFIL DE USUARIO (NOMBRE Y TELÉFONO)
-- ============================================================

ALTER TABLE app_users
    ADD COLUMN IF NOT EXISTS full_name VARCHAR(100),
    ADD COLUMN IF NOT EXISTS phone VARCHAR(30);
