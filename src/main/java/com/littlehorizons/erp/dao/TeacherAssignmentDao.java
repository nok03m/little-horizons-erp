package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.TeacherAssignment;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.sql.Time;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class TeacherAssignmentDao {

    public List<TeacherAssignment> findAll() {
        String sql = """
                SELECT a.id_asignacion, a.id_profesor, a.id_materia, a.anio_academico, a.seccion,
                       a.dia_semana, a.hora_inicio, a.hora_fin,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS teacher_name,
                       m.nombre_materia AS subject_name
                FROM asignaciones_profesor a
                JOIN profesores p ON p.id_profesor = a.id_profesor
                JOIN materias m ON m.id_materia = a.id_materia
                ORDER BY a.anio_academico DESC, teacher_name
                """;
        return query(sql, null);
    }

    public List<TeacherAssignment> findByTeacherId(int teacherId) {
        String sql = """
                SELECT a.id_asignacion, a.id_profesor, a.id_materia, a.anio_academico, a.seccion,
                       a.dia_semana, a.hora_inicio, a.hora_fin,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS teacher_name,
                       m.nombre_materia AS subject_name
                FROM asignaciones_profesor a
                JOIN profesores p ON p.id_profesor = a.id_profesor
                JOIN materias m ON m.id_materia = a.id_materia
                WHERE a.id_profesor = ?
                ORDER BY a.anio_academico DESC
                """;
        return query(sql, teacherId);
    }

    public Optional<TeacherAssignment> findById(int id) {
        String sql = """
                SELECT a.id_asignacion, a.id_profesor, a.id_materia, a.anio_academico, a.seccion,
                       a.dia_semana, a.hora_inicio, a.hora_fin,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS teacher_name,
                       m.nombre_materia AS subject_name
                FROM asignaciones_profesor a
                JOIN profesores p ON p.id_profesor = a.id_profesor
                JOIN materias m ON m.id_materia = a.id_materia
                WHERE a.id_asignacion = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapAssignment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find assignment id=" + id, exception);
            throw new IllegalStateException("Unable to find assignment.", exception);
        }
        return Optional.empty();
    }

    public int insert(TeacherAssignment assignment) {
        validateSchedule(assignment);
        String sql = """
                INSERT INTO asignaciones_profesor
                    (id_profesor, id_materia, anio_academico, seccion, dia_semana, hora_inicio, hora_fin)
                VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection()) {
            connection.setAutoCommit(false);
            try {
            try (PreparedStatement lock = connection.prepareStatement(
                    "SELECT id_profesor FROM profesores WHERE id_profesor = ? FOR UPDATE")) {
                lock.setInt(1, assignment.getTeacherId());
                try (ResultSet result = lock.executeQuery()) {
                    if (!result.next()) throw new IllegalArgumentException("Teacher not found.");
                }
            }
            String overlapSql = """
                    SELECT 1 FROM asignaciones_profesor
                    WHERE id_profesor = ? AND anio_academico = ? AND dia_semana = ?
                      AND hora_inicio < ? AND hora_fin > ? LIMIT 1
                    """;
            try (PreparedStatement overlap = connection.prepareStatement(overlapSql)) {
                overlap.setInt(1, assignment.getTeacherId());
                overlap.setInt(2, assignment.getAcademicYear());
                overlap.setString(3, assignment.getDayOfWeek());
                overlap.setTime(4, Time.valueOf(assignment.getEndTime()));
                overlap.setTime(5, Time.valueOf(assignment.getStartTime()));
                try (ResultSet result = overlap.executeQuery()) {
                    if (result.next()) throw new IllegalArgumentException("Teacher already has an overlapping assignment in this academic year.");
                }
            }
            try (PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setInt(1, assignment.getTeacherId());
            statement.setInt(2, assignment.getSubjectId());
            statement.setInt(3, assignment.getAcademicYear());
            if (assignment.getSection() == null || assignment.getSection().isBlank()) {
                statement.setNull(4, Types.VARCHAR);
            } else {
                statement.setString(4, assignment.getSection());
            }
            statement.setString(5, assignment.getDayOfWeek());
            statement.setTime(6, Time.valueOf(assignment.getStartTime()));
            statement.setTime(7, Time.valueOf(assignment.getEndTime()));
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    assignment.setId(id);
                    connection.commit();
                    return id;
                }
            }
            }
            throw new IllegalStateException("Assignment insert did not return an id.");
            } catch (SQLException | RuntimeException exception) {
                try {
                    connection.rollback();
                } catch (SQLException rollbackException) {
                    exception.addSuppressed(rollbackException);
                }
                throw exception;
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert assignment", exception);
            throw new IllegalStateException("Unable to create assignment. It may already exist.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM asignaciones_profesor WHERE id_asignacion = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete assignment id=" + id, exception);
            throw new IllegalStateException("Unable to delete assignment.", exception);
        }
    }

    private List<TeacherAssignment> query(String sql, Integer teacherId) {
        List<TeacherAssignment> assignments = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (teacherId != null) {
                statement.setInt(1, teacherId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    assignments.add(mapAssignment(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list assignments", exception);
            throw new IllegalStateException("Unable to list assignments.", exception);
        }
        return assignments;
    }

    private TeacherAssignment mapAssignment(ResultSet resultSet) throws SQLException {
        TeacherAssignment assignment = new TeacherAssignment();
        assignment.setId(resultSet.getInt("id_asignacion"));
        assignment.setTeacherId(resultSet.getInt("id_profesor"));
        assignment.setSubjectId(resultSet.getInt("id_materia"));
        assignment.setAcademicYear(resultSet.getInt("anio_academico"));
        assignment.setSection(resultSet.getString("seccion"));
        assignment.setDayOfWeek(resultSet.getString("dia_semana"));
        Time start = resultSet.getTime("hora_inicio");
        Time end = resultSet.getTime("hora_fin");
        assignment.setStartTime(start == null ? null : start.toLocalTime());
        assignment.setEndTime(end == null ? null : end.toLocalTime());
        assignment.setTeacherName(resultSet.getString("teacher_name"));
        assignment.setSubjectName(resultSet.getString("subject_name"));
        return assignment;
    }

    private void validateSchedule(TeacherAssignment assignment) {
        if (assignment.getDayOfWeek() == null || assignment.getStartTime() == null || assignment.getEndTime() == null
                || !List.of("Lunes", "Martes", "Miercoles", "Jueves", "Viernes", "Sabado").contains(assignment.getDayOfWeek())
                || !assignment.getStartTime().isBefore(assignment.getEndTime())) {
            throw new IllegalArgumentException("A valid day and a start time before the end time are required.");
        }
    }
}
