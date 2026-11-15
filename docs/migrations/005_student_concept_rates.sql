-- Persist student-specific rates and discount/benefit reasons by validity period.
-- Select erp_little_horizons with USE before running this migration.

CREATE TABLE tarifas_estudiante_concepto (
    id_tarifa INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_concepto INT NOT NULL,
    monto DECIMAL(10,2) NOT NULL,
    vigente_desde DATE NOT NULL,
    vigente_hasta DATE NULL,
    motivo VARCHAR(255) NOT NULL,
    autorizado_por VARCHAR(50) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_tarifa_monto CHECK (monto > 0),
    CONSTRAINT chk_tarifa_vigencia CHECK (vigente_hasta IS NULL OR vigente_hasta >= vigente_desde),
    CONSTRAINT fk_tarifa_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    CONSTRAINT fk_tarifa_concepto FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto),
    INDEX idx_tarifa_alumno_concepto_fecha (id_estudiante, id_concepto, vigente_desde, vigente_hasta)
) ENGINE=InnoDB COMMENT='Tarifas especiales por estudiante, concepto y vigencia';
