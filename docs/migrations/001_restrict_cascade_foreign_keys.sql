-- Preserve dependent academic, financial, extracurricular, and loan history.
-- Select the target database with USE before running this migration.
-- Take a backup first. The migration changes only foreign-key delete rules;
-- it does not delete or rewrite application rows.

DROP PROCEDURE IF EXISTS migrate_cascade_foreign_keys_to_restrict;

DELIMITER $$

CREATE PROCEDURE migrate_cascade_foreign_keys_to_restrict()
BEGIN
    DECLARE finished BOOLEAN DEFAULT FALSE;
    DECLARE child_table VARCHAR(64);
    DECLARE constraint_name VARCHAR(64);
    DECLARE child_columns TEXT;
    DECLARE parent_table VARCHAR(64);
    DECLARE parent_columns TEXT;
    DECLARE update_rule VARCHAR(16);
    DECLARE replacement_constraint VARCHAR(64);

    DECLARE cascade_constraints CURSOR FOR
        SELECT k.TABLE_NAME,
               k.CONSTRAINT_NAME,
               GROUP_CONCAT(CONCAT('`', REPLACE(k.COLUMN_NAME, '`', '``'), '`')
                            ORDER BY k.ORDINAL_POSITION SEPARATOR ', '),
               k.REFERENCED_TABLE_NAME,
               GROUP_CONCAT(CONCAT('`', REPLACE(k.REFERENCED_COLUMN_NAME, '`', '``'), '`')
                            ORDER BY k.ORDINAL_POSITION SEPARATOR ', '),
               r.UPDATE_RULE
        FROM information_schema.KEY_COLUMN_USAGE k
        JOIN information_schema.REFERENTIAL_CONSTRAINTS r
          ON r.CONSTRAINT_SCHEMA = k.CONSTRAINT_SCHEMA
         AND r.TABLE_NAME = k.TABLE_NAME
         AND r.CONSTRAINT_NAME = k.CONSTRAINT_NAME
        WHERE k.CONSTRAINT_SCHEMA = DATABASE()
          AND k.REFERENCED_TABLE_NAME IS NOT NULL
          AND r.DELETE_RULE = 'CASCADE'
        GROUP BY k.TABLE_NAME, k.CONSTRAINT_NAME, k.REFERENCED_TABLE_NAME, r.UPDATE_RULE;

    DECLARE CONTINUE HANDLER FOR NOT FOUND SET finished = TRUE;

    IF DATABASE() IS NULL THEN
        SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Select the target database with USE before migration.';
    END IF;

    OPEN cascade_constraints;
    migration_loop: LOOP
        FETCH cascade_constraints
         INTO child_table, constraint_name, child_columns, parent_table, parent_columns, update_rule;

        IF finished THEN
            LEAVE migration_loop;
        END IF;

        SET replacement_constraint = CONCAT(
            'restrict_', LEFT(MD5(CONCAT(child_table, '.', constraint_name)), 32)
        );

        SET @migration_sql = CONCAT(
            'ALTER TABLE `', REPLACE(child_table, '`', '``'), '` ',
            'DROP FOREIGN KEY `', REPLACE(constraint_name, '`', '``'), '`, ',
            'ADD CONSTRAINT `', replacement_constraint, '` ',
            'FOREIGN KEY (', child_columns, ') ',
            'REFERENCES `', REPLACE(parent_table, '`', '``'), '` (', parent_columns, ') ',
            'ON DELETE RESTRICT ON UPDATE ', update_rule
        );

        PREPARE migration_statement FROM @migration_sql;
        EXECUTE migration_statement;
        DEALLOCATE PREPARE migration_statement;
    END LOOP;
    CLOSE cascade_constraints;
END$$

CALL migrate_cascade_foreign_keys_to_restrict()$$
DROP PROCEDURE migrate_cascade_foreign_keys_to_restrict$$

DELIMITER ;
