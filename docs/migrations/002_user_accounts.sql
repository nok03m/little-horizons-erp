-- Add persistent application identities for console authentication.
-- Select erp_little_horizons with USE before running this migration.
-- No default account/password is created. Bootstrap the administrator from
-- a secure terminal after this table exists.

CREATE TABLE IF NOT EXISTS usuarios_app (
    id_usuario INT AUTO_INCREMENT PRIMARY KEY,
    nombre_usuario VARCHAR(50) NOT NULL UNIQUE,
    hash_contrasena VARCHAR(255) NOT NULL,
    rol ENUM('ADMIN', 'ACADEMIC', 'FINANCE', 'LIBRARIAN') NOT NULL,
    activo BOOLEAN NOT NULL DEFAULT TRUE,
    fecha_creacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    fecha_actualizacion TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    INDEX idx_usuario_activo (nombre_usuario, activo)
) ENGINE=InnoDB COMMENT='Cuentas autenticadas y roles de acceso del ERP';
