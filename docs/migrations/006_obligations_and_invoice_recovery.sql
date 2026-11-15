-- Track invoice state for retries and model charges with due dates/partial payments.
-- Run after migrations 001-005 with the target database selected and a tested backup.

ALTER TABLE pagos
    ADD COLUMN estado_factura ENUM('PENDIENTE', 'EMITIDA') NOT NULL DEFAULT 'PENDIENTE';

UPDATE pagos p
LEFT JOIN facturas f ON f.id_pago = p.id_pago
SET p.estado_factura = IF(f.id_pago IS NULL, 'PENDIENTE', 'EMITIDA');

ALTER TABLE facturas
    ADD CONSTRAINT uq_factura_pago UNIQUE (id_pago);

CREATE TABLE obligaciones_pago (
    id_obligacion INT AUTO_INCREMENT PRIMARY KEY,
    id_estudiante INT NOT NULL,
    id_concepto INT NOT NULL,
    periodo_clave VARCHAR(20) NOT NULL,
    fecha_vencimiento DATE NOT NULL,
    monto_debido DECIMAL(10,2) NOT NULL,
    descripcion VARCHAR(255) NOT NULL,
    creado_por VARCHAR(50) NOT NULL,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_obligacion_monto CHECK (monto_debido > 0),
    CONSTRAINT uq_obligacion_periodo UNIQUE (id_estudiante, id_concepto, periodo_clave),
    CONSTRAINT fk_obligacion_estudiante FOREIGN KEY (id_estudiante) REFERENCES estudiantes(id_estudiante),
    CONSTRAINT fk_obligacion_concepto FOREIGN KEY (id_concepto) REFERENCES conceptos_pago(id_concepto),
    INDEX idx_obligacion_vencimiento (fecha_vencimiento, id_estudiante, id_concepto)
) ENGINE=InnoDB COMMENT='Cargos con vencimiento y saldo calculado por aplicaciones de pago';

CREATE TABLE aplicaciones_pago (
    id_aplicacion INT AUTO_INCREMENT PRIMARY KEY,
    id_pago INT NOT NULL,
    id_obligacion INT NOT NULL,
    monto_aplicado DECIMAL(10,2) NOT NULL,
    fecha_aplicacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_aplicacion_monto CHECK (monto_aplicado > 0),
    CONSTRAINT uq_pago_obligacion UNIQUE (id_pago, id_obligacion),
    CONSTRAINT fk_aplicacion_pago FOREIGN KEY (id_pago) REFERENCES pagos(id_pago),
    CONSTRAINT fk_aplicacion_obligacion FOREIGN KEY (id_obligacion) REFERENCES obligaciones_pago(id_obligacion),
    INDEX idx_aplicacion_obligacion (id_obligacion)
) ENGINE=InnoDB COMMENT='Aplicaciones parciales o totales de pagos a obligaciones';
