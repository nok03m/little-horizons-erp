-- Add persistent failed-login lockout state to application accounts.
-- Select the target database with USE before running this migration.

ALTER TABLE usuarios_app
    ADD COLUMN failed_attempts SMALLINT UNSIGNED NOT NULL DEFAULT 0,
    ADD COLUMN locked_until TIMESTAMP NULL;
