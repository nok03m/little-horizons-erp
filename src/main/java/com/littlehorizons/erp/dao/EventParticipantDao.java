package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.EventParticipant;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Types;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class EventParticipantDao {

    public List<EventParticipant> findAll() {
        return query(baseSelect() + " ORDER BY ev.fecha_evento DESC, e.primer_apellido", null, null);
    }

    public List<EventParticipant> findByEventId(int eventId) {
        return query(baseSelect() + " WHERE p.id_evento = ? ORDER BY e.primer_apellido", eventId, null);
    }

    public List<EventParticipant> findByStudentId(int studentId) {
        return query(baseSelect() + " WHERE p.id_estudiante = ? ORDER BY ev.fecha_evento DESC", null, studentId);
    }

    public Optional<EventParticipant> findById(int id) {
        String sql = baseSelect() + " WHERE p.id_participacion = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapParticipant(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find participant id=" + id, exception);
            throw new IllegalStateException("Unable to find participant.", exception);
        }
        return Optional.empty();
    }

    public int insert(EventParticipant participant) {
        String sql = """
                INSERT INTO participantes_evento (
                    id_evento, id_estudiante, rol_participacion, posicion_obtenida, observaciones
                ) VALUES (?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindParticipant(statement, participant);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    participant.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert participant", exception);
            throw new IllegalStateException("Unable to register participant. Student may already be registered.", exception);
        }
        throw new IllegalStateException("Participant insert did not return an id.");
    }

    public boolean update(EventParticipant participant) {
        String sql = """
                UPDATE participantes_evento SET
                    id_evento = ?, id_estudiante = ?, rol_participacion = ?, posicion_obtenida = ?, observaciones = ?
                WHERE id_participacion = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindParticipant(statement, participant);
            statement.setInt(6, participant.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update participant id=" + participant.getId(), exception);
            throw new IllegalStateException("Unable to update participant.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM participantes_evento WHERE id_participacion = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete participant id=" + id, exception);
            throw new IllegalStateException("Unable to delete participant.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT p.id_participacion, p.id_evento, p.id_estudiante, p.rol_participacion,
                       p.posicion_obtenida, p.observaciones,
                       ev.nombre_evento AS event_name,
                       CONCAT(e.primer_nombre, ' ', e.primer_apellido) AS student_name
                FROM participantes_evento p
                JOIN eventos ev ON ev.id_evento = p.id_evento
                JOIN estudiantes e ON e.id_estudiante = p.id_estudiante
                """;
    }

    private List<EventParticipant> query(String sql, Integer eventId, Integer studentId) {
        List<EventParticipant> participants = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (eventId != null) {
                statement.setInt(1, eventId);
            } else if (studentId != null) {
                statement.setInt(1, studentId);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    participants.add(mapParticipant(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list participants", exception);
            throw new IllegalStateException("Unable to list participants.", exception);
        }
        return participants;
    }

    private void bindParticipant(PreparedStatement statement, EventParticipant participant) throws SQLException {
        statement.setInt(1, participant.getEventId());
        statement.setInt(2, participant.getStudentId());
        statement.setString(3, participant.getParticipationRole());
        if (participant.getPositionObtained() == null || participant.getPositionObtained().isBlank()) {
            statement.setNull(4, Types.VARCHAR);
        } else {
            statement.setString(4, participant.getPositionObtained());
        }
        if (participant.getObservations() == null || participant.getObservations().isBlank()) {
            statement.setNull(5, Types.VARCHAR);
        } else {
            statement.setString(5, participant.getObservations());
        }
    }

    private EventParticipant mapParticipant(ResultSet resultSet) throws SQLException {
        EventParticipant participant = new EventParticipant();
        participant.setId(resultSet.getInt("id_participacion"));
        participant.setEventId(resultSet.getInt("id_evento"));
        participant.setStudentId(resultSet.getInt("id_estudiante"));
        participant.setParticipationRole(resultSet.getString("rol_participacion"));
        participant.setPositionObtained(resultSet.getString("posicion_obtenida"));
        participant.setObservations(resultSet.getString("observaciones"));
        participant.setEventName(resultSet.getString("event_name"));
        participant.setStudentName(resultSet.getString("student_name"));
        return participant;
    }
}
