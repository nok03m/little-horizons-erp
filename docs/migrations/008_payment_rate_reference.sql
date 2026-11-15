-- Preserve which student-specific rate was applied to each payment.
-- Run after migrations 001-007, with the target schema selected.

ALTER TABLE pagos
    ADD COLUMN id_tarifa INT NULL COMMENT 'Tarifa estudiantil aplicada, si corresponde',
    ADD INDEX idx_pago_tarifa (id_tarifa),
    ADD CONSTRAINT fk_pago_tarifa FOREIGN KEY (id_tarifa)
        REFERENCES tarifas_estudiante_concepto(id_tarifa);
