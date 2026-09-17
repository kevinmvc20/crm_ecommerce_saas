-- Agrega la columna nombre_completo a la tabla usuario
-- Requerida para el aprovisionamiento de usuarios operacionales (ADMIN_EMPRESA, VENDEDOR)
ALTER TABLE usuario
    ADD COLUMN IF NOT EXISTS nombre_completo VARCHAR(200) NOT NULL DEFAULT '';

-- Elimina el DEFAULT temporal una vez añadida la columna
-- (los registros existentes del seed quedan con cadena vacía; en producción se migrarían manualmente)
ALTER TABLE usuario
    ALTER COLUMN nombre_completo DROP DEFAULT;
