-- Add an atomic invoice counter for concurrent application instances.
-- Select erp_little_horizons with USE before running this migration.

CREATE TABLE IF NOT EXISTS invoice_sequence (
    sequence_id TINYINT PRIMARY KEY,
    next_value INT UNSIGNED NOT NULL,
    CHECK (sequence_id = 1)
) ENGINE=InnoDB COMMENT='Secuencia transaccional y concurrente de facturas';

INSERT INTO invoice_sequence (sequence_id, next_value)
SELECT 1, COALESCE(MAX(id_factura), 0)
FROM facturas
WHERE TRUE
ON DUPLICATE KEY UPDATE next_value = GREATEST(next_value, VALUES(next_value));
