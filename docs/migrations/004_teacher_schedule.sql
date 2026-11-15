-- Add optional schedule fields; existing assignments remain valid and unset.
-- Select erp_little_horizons with USE before running this migration.

ALTER TABLE asignaciones_profesor
    ADD COLUMN dia_semana ENUM('Lunes', 'Martes', 'Miercoles', 'Jueves', 'Viernes', 'Sabado') NULL,
    ADD COLUMN hora_inicio TIME NULL,
    ADD COLUMN hora_fin TIME NULL,
    ADD CONSTRAINT chk_asignacion_horario
        CHECK ((dia_semana IS NULL AND hora_inicio IS NULL AND hora_fin IS NULL) OR
               (dia_semana IS NOT NULL AND hora_inicio IS NOT NULL AND hora_fin IS NOT NULL AND hora_inicio < hora_fin)),
    ADD INDEX idx_docente_horario (id_profesor, anio_academico, dia_semana, hora_inicio, hora_fin);
