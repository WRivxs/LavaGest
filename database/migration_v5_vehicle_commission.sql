-- Migration v5: Comisión dinámica por tipo de vehículo
-- Agrega columna commission_pct a vehicle_types_catalog
-- Valor por defecto: 40.00%

ALTER TABLE vehicle_types_catalog
    ADD COLUMN IF NOT EXISTS commission_pct NUMERIC(5,2) NOT NULL DEFAULT 40.00;

-- Verificación
SELECT id, name, active, commission_pct FROM vehicle_types_catalog ORDER BY name;
