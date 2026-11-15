package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolEvent;
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

public class SchoolEventDao {

    public List<SchoolEvent> findAll() {
        String sql = baseSelect() + " ORDER BY e.fecha_evento DESC, e.nombre_evento";
        List<SchoolEvent> events = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                events.add(mapEvent(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list events", exception);
            throw new IllegalStateException("Unable to list events.", exception);
        }
        return events;
    }

    public Optional<SchoolEvent> findById(int id) {
        String sql = baseSelect() + " WHERE e.id_evento = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapEvent(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find event id=" + id, exception);
            throw new IllegalStateException("Unable to find event.", exception);
        }
        return Optional.empty();
    }

    public int insert(SchoolEvent event) {
        String sql = """
                INSERT INTO eventos (
                    nombre_evento, descripcion, tipo_evento, fecha_evento, lugar,
                    tiene_competencia, id_organizador, esta_activo
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindEvent(statement, event);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    event.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert event", exception);
            throw new IllegalStateException("Unable to create event.", exception);
        }
        throw new IllegalStateException("Event insert did not return an id.");
    }

    public boolean update(SchoolEvent event) {
        String sql = """
                UPDATE eventos SET
                    nombre_evento = ?, descripcion = ?, tipo_evento = ?, fecha_evento = ?, lugar = ?,
                    tiene_competencia = ?, id_organizador = ?, esta_activo = ?
                WHERE id_evento = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindEvent(statement, event);
            statement.setInt(9, event.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update event id=" + event.getId(), exception);
            throw new IllegalStateException("Unable to update event.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM eventos WHERE id_evento = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete event id=" + id, exception);
            throw new IllegalStateException("Unable to delete event.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT e.id_evento, e.nombre_evento, e.descripcion, e.tipo_evento, e.fecha_evento, e.lugar,
                       e.tiene_competencia, e.id_organizador, e.esta_activo,
                       CONCAT(p.primer_nombre, ' ', p.primer_apellido) AS organizer_name
                FROM eventos e
                LEFT JOIN profesores p ON p.id_profesor = e.id_organizador
                """;
    }

    private void bindEvent(PreparedStatement statement, SchoolEvent event) throws SQLException {
        statement.setString(1, event.getName());
        if (event.getDescription() == null || event.getDescription().isBlank()) {
            statement.setNull(2, Types.VARCHAR);
        } else {
            statement.setString(2, event.getDescription());
        }
        statement.setString(3, event.getEventType());
        statement.setDate(4, Date.valueOf(event.getEventDate()));
        if (event.getLocation() == null || event.getLocation().isBlank()) {
            statement.setNull(5, Types.VARCHAR);
        } else {
            statement.setString(5, event.getLocation());
        }
        statement.setBoolean(6, event.isHasCompetition());
        if (event.getOrganizerTeacherId() == null) {
            statement.setNull(7, Types.INTEGER);
        } else {
            statement.setInt(7, event.getOrganizerTeacherId());
        }
        statement.setBoolean(8, event.isActive());
    }

    private SchoolEvent mapEvent(ResultSet resultSet) throws SQLException {
        SchoolEvent event = new SchoolEvent();
        event.setId(resultSet.getInt("id_evento"));
        event.setName(resultSet.getString("nombre_evento"));
        event.setDescription(resultSet.getString("descripcion"));
        event.setEventType(resultSet.getString("tipo_evento"));
        event.setEventDate(resultSet.getDate("fecha_evento").toLocalDate());
        event.setLocation(resultSet.getString("lugar"));
        event.setHasCompetition(resultSet.getBoolean("tiene_competencia"));
        int organizerId = resultSet.getInt("id_organizador");
        event.setOrganizerTeacherId(resultSet.wasNull() ? null : organizerId);
        event.setActive(resultSet.getBoolean("esta_activo"));
        event.setOrganizerTeacherName(resultSet.getString("organizer_name"));
        return event;
    }
}
