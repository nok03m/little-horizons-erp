package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AcademicRecord;
import com.littlehorizons.erp.model.AcademicPerformanceRecord;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AcademicRecordDao {

    public List<AcademicPerformanceRecord> findRecordsForPerformanceReport() {
        String sql = """
                SELECT r.id_registro, r.valor_nota, r.periodo_academico,
                       m.id_materia, m.codigo_materia, m.nombre_materia
                FROM registros_academicos r
                JOIN materias m ON m.id_materia = r.id_materia
                """;
        List<AcademicPerformanceRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                records.add(new AcademicPerformanceRecord(
                        resultSet.getInt("id_registro"),
                        resultSet.getBigDecimal("valor_nota"),
                        resultSet.getString("periodo_academico"),
                        resultSet.getInt("id_materia"),
                        resultSet.getString("codigo_materia"),
                        resultSet.getString("nombre_materia")));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to load academic performance records", exception);
            throw new IllegalStateException("Unable to load academic performance records.", exception);
        }
        return records;
    }

    public List<AcademicRecord> findAll() {
        return query(baseSelect() + " ORDER BY r.anio_academico DESC, e.primer_apellido", null, null);
    }

    public List<AcademicRecord> findByStudentId(int studentId) {
        return query(baseSelect() + " WHERE r.id_estudiante = ? ORDER BY r.anio_academico DESC, r.periodo_academico",
                studentId, null);
    }

    public List<AcademicRecord> findBySubjectAndYear(int subjectId, int academicYear) {
        String sql = baseSelect() + " WHERE r.id_materia = ? AND r.anio_academico = ? ORDER BY e.primer_apellido";
        List<AcademicRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, subjectId);
            statement.setInt(2, academicYear);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapRecord(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list academic records by subject/year", exception);
            throw new IllegalStateException("Unable to list academic records.", exception);
        }
        return records;
    }

    public Optional<AcademicRecord> findById(int id) {
        String sql = baseSelect() + " WHERE r.id_registro = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapRecord(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find academic record id=" + id, exception);
            throw new IllegalStateException("Unable to find academic record.", exception);
        }
        return Optional.empty();
    }

    public int insert(AcademicRecord record) {
        String sql = """
                INSERT INTO registros_academicos (
                    id_estudiante, id_materia, id_profesor, anio_academico, periodo_academico,
                    valor_nota, observaciones, fecha_registro
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindRecord(statement, record);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    record.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert academic record", exception);
            throw new IllegalStateException("Unable to create academic record. It may already exist for that period.", exception);
        }
        throw new IllegalStateException("Academic record insert did not return an id.");
    }

    public boolean update(AcademicRecord record) {
        String sql = """
                UPDATE registros_academicos SET
                    id_estudiante = ?, id_materia = ?, id_profesor = ?, anio_academico = ?,
                    periodo_academico = ?, valor_nota = ?, observaciones = ?, fecha_registro = ?
                WHERE id_registro = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindRecord(statement, record);
            statement.setInt(9, record.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update academic record id=" + record.getId(), exception);
            throw new IllegalStateException("Unable to update academic record.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM registros_academicos WHERE id_registro = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete academic record id=" + id, exception);
            throw new IllegalStateException("Unable to delete academic record.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT r.id_registro, r.id_estudiante, r.id_materia, r.id_profesor, r.anio_academico,
                       r.periodo_academico, r.valor_nota, r.observaciones, r.fecha_registro,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       m.nombre_materia AS subject_name,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS teacher_name
                FROM registros_academicos r
                JOIN estudiantes e ON e.id_estudiante = r.id_estudiante
                JOIN materias m ON m.id_materia = r.id_materia
                JOIN profesores p ON p.id_profesor = r.id_profesor
                """;
    }

    private List<AcademicRecord> query(String sql, Integer studentId, Integer unused) {
        List<AcademicRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapRecord(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list academic records", exception);
            throw new IllegalStateException("Unable to list academic records.", exception);
        }
        return records;
    }

    private void bindRecord(PreparedStatement statement, AcademicRecord record) throws SQLException {
        statement.setInt(1, record.getStudentId());
        statement.setInt(2, record.getSubjectId());
        statement.setInt(3, record.getTeacherId());
        statement.setInt(4, record.getAcademicYear());
        statement.setString(5, record.getAcademicPeriod());
        statement.setBigDecimal(6, record.getGradeValue());
        if (record.getObservations() == null || record.getObservations().isBlank()) {
            statement.setNull(7, Types.VARCHAR);
        } else {
            statement.setString(7, record.getObservations());
        }
        statement.setDate(8, Date.valueOf(record.getRecordDate()));
    }

    private AcademicRecord mapRecord(ResultSet resultSet) throws SQLException {
        AcademicRecord record = new AcademicRecord();
        record.setId(resultSet.getInt("id_registro"));
        record.setStudentId(resultSet.getInt("id_estudiante"));
        record.setSubjectId(resultSet.getInt("id_materia"));
        record.setTeacherId(resultSet.getInt("id_profesor"));
        record.setAcademicYear(resultSet.getInt("anio_academico"));
        record.setAcademicPeriod(resultSet.getString("periodo_academico"));
        record.setGradeValue(resultSet.getBigDecimal("valor_nota"));
        record.setObservations(resultSet.getString("observaciones"));
        record.setRecordDate(resultSet.getDate("fecha_registro").toLocalDate());
        record.setStudentName(resultSet.getString("student_name"));
        record.setSubjectName(resultSet.getString("subject_name"));
        record.setTeacherName(resultSet.getString("teacher_name"));
        return record;
    }
}
