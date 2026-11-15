package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.SchoolResource;
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

public class SchoolResourceDao {

    public List<SchoolResource> findAll() {
        String sql = baseSelect() + " ORDER BY r.nombre_recurso";
        return query(sql, null);
    }

    public List<SchoolResource> findByStatus(String status) {
        String sql = baseSelect() + " WHERE r.estado_recurso = ? ORDER BY r.nombre_recurso";
        return query(sql, status);
    }

    public Optional<SchoolResource> findById(int id) {
        String sql = baseSelect() + " WHERE r.id_recurso = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapResource(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find resource id=" + id, exception);
            throw new IllegalStateException("Unable to find resource.", exception);
        }
        return Optional.empty();
    }

    public int insert(SchoolResource resource) {
        String sql = """
                INSERT INTO recursos (
                    codigo_recurso, nombre_recurso, id_categoria, descripcion,
                    fecha_adquisicion, ubicacion_fisica, estado_recurso
                ) VALUES (?, ?, ?, ?, ?, ?, ?)
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            bindResource(statement, resource);
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    resource.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert resource", exception);
            throw new IllegalStateException("Unable to create resource. Code may already exist.", exception);
        }
        throw new IllegalStateException("Resource insert did not return an id.");
    }

    public boolean update(SchoolResource resource) {
        String sql = """
                UPDATE recursos SET
                    codigo_recurso = ?, nombre_recurso = ?, id_categoria = ?, descripcion = ?,
                    fecha_adquisicion = ?, ubicacion_fisica = ?, estado_recurso = ?
                WHERE id_recurso = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            bindResource(statement, resource);
            statement.setInt(8, resource.getId());
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update resource id=" + resource.getId(), exception);
            throw new IllegalStateException("Unable to update resource.", exception);
        }
    }

    public boolean updateStatus(int resourceId, String status) {
        String sql = "UPDATE recursos SET estado_recurso = ? WHERE id_recurso = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setString(1, status);
            statement.setInt(2, resourceId);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to update resource status id=" + resourceId, exception);
            throw new IllegalStateException("Unable to update resource status.", exception);
        }
    }

    public boolean deleteById(int id) {
        String sql = "DELETE FROM recursos WHERE id_recurso = ?";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            return statement.executeUpdate() > 0;
        } catch (SQLException exception) {
            AppLogger.error("Failed to delete resource id=" + id, exception);
            throw new IllegalStateException("Unable to delete resource.", exception);
        }
    }

    private String baseSelect() {
        return """
                SELECT r.id_recurso, r.codigo_recurso, r.nombre_recurso, r.id_categoria, r.descripcion,
                       r.fecha_adquisicion, r.ubicacion_fisica, r.estado_recurso,
                       c.nombre_categoria AS category_name
                FROM recursos r
                JOIN categorias_recurso c ON c.id_categoria = r.id_categoria
                """;
    }

    private List<SchoolResource> query(String sql, String status) {
        List<SchoolResource> resources = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            if (status != null) {
                statement.setString(1, status);
            }
            try (ResultSet resultSet = statement.executeQuery()) {
                while (resultSet.next()) {
                    resources.add(mapResource(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list resources", exception);
            throw new IllegalStateException("Unable to list resources.", exception);
        }
        return resources;
    }

    private void bindResource(PreparedStatement statement, SchoolResource resource) throws SQLException {
        statement.setString(1, resource.getCode());
        statement.setString(2, resource.getName());
        statement.setInt(3, resource.getCategoryId());
        if (resource.getDescription() == null || resource.getDescription().isBlank()) {
            statement.setNull(4, Types.VARCHAR);
        } else {
            statement.setString(4, resource.getDescription());
        }
        if (resource.getAcquisitionDate() == null) {
            statement.setNull(5, Types.DATE);
        } else {
            statement.setDate(5, Date.valueOf(resource.getAcquisitionDate()));
        }
        if (resource.getPhysicalLocation() == null || resource.getPhysicalLocation().isBlank()) {
            statement.setNull(6, Types.VARCHAR);
        } else {
            statement.setString(6, resource.getPhysicalLocation());
        }
        statement.setString(7, resource.getStatus());
    }

    private SchoolResource mapResource(ResultSet resultSet) throws SQLException {
        SchoolResource resource = new SchoolResource();
        resource.setId(resultSet.getInt("id_recurso"));
        resource.setCode(resultSet.getString("codigo_recurso"));
        resource.setName(resultSet.getString("nombre_recurso"));
        resource.setCategoryId(resultSet.getInt("id_categoria"));
        resource.setDescription(resultSet.getString("descripcion"));
        Date acquisition = resultSet.getDate("fecha_adquisicion");
        resource.setAcquisitionDate(acquisition == null ? null : acquisition.toLocalDate());
        resource.setPhysicalLocation(resultSet.getString("ubicacion_fisica"));
        resource.setStatus(resultSet.getString("estado_recurso"));
        resource.setCategoryName(resultSet.getString("category_name"));
        return resource;
    }
}
