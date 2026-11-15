package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.AttendanceRecord;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.Date;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class AttendanceDao {

    public List<AttendanceRecord> findAll() {
        String sql = baseSelect() + " ORDER BY a.fecha_asistencia DESC, e.primer_apellido";
        return query(sql);
    }

    public List<AttendanceRecord> findByStudentId(int studentId) {
        String sql = baseSelect() + " WHERE a.id_estudiante = ? ORDER BY a.fecha_asistencia DESC";
        List<AttendanceRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, studentId);
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapAttendance(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list attendance by student", exception);
            throw new IllegalStateException("Unable to list attendance.", exception);
        }
        return records;
    }

    public List<AttendanceRecord> findAbsencesBetween(LocalDate from, LocalDate to) {
        String sql = baseSelect() + """
                 WHERE a.estado_asistencia = 'Ausente'
                   AND a.fecha_asistencia BETWEEN ? AND ?
                ORDER BY a.fecha_asistencia, e.primer_apellido
                """;
        List<AttendanceRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setDate(1, Date.valueOf(from));
            statement.setDate(2, Date.valueOf(to));
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    records.add(mapAttendance(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list absences", exception);
            throw new IllegalStateException("Unable to list absences.", exception);
        }
        return records;
    }

    public Optional<AttendanceRecord> findById(int id) {
        String sql = baseSelect() + " WHERE a.id_asistencia = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAttendance(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find attendance id=" + id, exception);
            throw new IllegalStateException("Unable to find attendance.", exception);
        }
        return Optional.empty();
    }

    public int insert(AttendanceRecord record) {
        String sql = """
                INSERT INTO asistencias (
                    id_estudiante, id_materia, fecha_asistencia, estado_asistencia, observaciones, registrado_por
                ) VALUES (?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, record.getStudentId());
            statement.setInt(2, record.getSubjectId());
            statement.setDate(3, Date.valueOf(record.getAttendanceDate()));
            statement.setString(4, record.getStatus());
            if (record.getObservations() == null || record.getObservations().isBlank()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, record.getObservations());
            }
            if (record.getRecordedByTeacherId() == null) {
                statement.setNull(6, Types.INTEGER);
            } else {
                statement.setInt(6, record.getRecordedByTeacherId());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    record.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert attendance", exception);
            throw new IllegalStateException("Unable to create attendance. It may already exist for that date.", exception);
        }
        throw new IllegalStateException("Attendance insert did not return an id.");
    }

    public boolean update(AttendanceRecord record) {
        String sql = """
                UPDATE asistencias SET
                    id_estudiante = ?, id_materia = ?, fecha_asistencia = ?, estado_asistencia = ?,
                    observaciones = ?, registrado_por = ?
                WHERE id_asistencia = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, record.getStudentId());
            statement.setInt(2, record.getSubjectId());
            statement.setDate(3, Date.valueOf(record.getAttendanceDate()));
            statement.setString(4, record.getStatus());
            if (record.getObservations() == null || record.getObservations().isBlank()) {
                statement.setNull(5, Types.VARCHAR);
            } else {
                statement.setString(5, record.getObservations());
            }
            if (record.getRecordedByTeacherId() == null) {
                statement.setNull(6, Types.INTEGER);
            } else {
                statement.setInt(6, record.getRecordedByTeacherId());
            }
            statement.setInt(7, record.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update attendance id=" + record.getId(), exception);
            throw new IllegalStateException("Unable to update attendance.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM asistencias WHERE id_asistencia = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete attendance id=" + id, exception);
            throw new IllegalStateException("Unable to delete attendance.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT a.id_asistencia, a.id_estudiante, a.id_materia, a.fecha_asistencia, a.estado_asistencia,
                       a.observaciones, a.registrado_por,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name,
                       m.nombre_materia AS subject_name
                FROM asistencias a
                JOIN estudiantes e ON e.id_estudiante = a.id_estudiante
                JOIN materias m ON m.id_materia = a.id_materia
                """;
    }

    private List<AttendanceRecord> query(String sql) {
        List<AttendanceRecord> records = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                records.add(mapAttendance(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list attendance", exception);
            throw new IllegalStateException("Unable to list attendance.", exception);
        }
        return records;
    }

    private AttendanceRecord mapAttendance(ResultSet resultSet) throws SQLException {
        AttendanceRecord record = new AttendanceRecord();
        record.setId(resultSet.getInt("id_asistencia"));
        record.setStudentId(resultSet.getInt("id_estudiante"));
        record.setSubjectId(resultSet.getInt("id_materia"));
        record.setAttendanceDate(resultSet.getDate("fecha_asistencia").toLocalDate());
        record.setStatus(resultSet.getString("estado_asistencia"));
        record.setObservations(resultSet.getString("observaciones"));
        int teacherId = resultSet.getInt("registrado_por");
        record.setRecordedByTeacherId(resultSet.wasNull() ? null : teacherId);
        record.setStudentName(resultSet.getString("student_name"));
        record.setSubjectName(resultSet.getString("subject_name"));
        return record;
    }
}
