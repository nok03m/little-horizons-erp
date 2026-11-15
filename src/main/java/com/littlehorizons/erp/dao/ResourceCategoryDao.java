package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.ResourceCategory;
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

public class ResourceCategoryDao {

    public List<ResourceCategory> findAll() {
        String sql = """
                SELECT id_categoria, nombre_categoria, descripcion
                FROM categorias_recurso
                ORDER BY nombre_categoria
                """;
        List<ResourceCategory> categories = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                categories.add(mapCategory(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list resource categories", exception);
            throw new IllegalStateException("Unable to list resource categories.", exception);
        }
        return categories;
    }

    public Optional<ResourceCategory> findById(int id) {
        String sql = """
                SELECT id_categoria, nombre_categoria, descripcion
                FROM categorias_recurso
                WHERE id_categoria = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapCategory(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find resource category id=" + id, exception);
            throw new IllegalStateException("Unable to find resource category.", exception);
        }
        return Optional.empty();
    }

    public int insert(ResourceCategory category) {
        String sql = "INSERT INTO categorias_recurso (nombre_categoria, descripcion) VALUES (?, ?)";
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            statement.setString(1, category.getName());
            if (category.getDescription() == null || category.getDescription().isBlank()) {
                statement.setNull(2, Types.VARCHAR);
            } else {
                statement.setString(2, category.getDescription());
            }
            statement.executeUpdate();
            try (ResultSet keys = statement.getGeneratedKeys()) {
                if (keys.next()) {
                    int id = keys.getInt(1);
                    category.setId(id);
                    return id;
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to insert resource category", exception);
            throw new IllegalStateException("Unable to create resource category.", exception);
        }
        throw new IllegalStateException("Resource category insert did not return an id.");
    }

    private ResourceCategory mapCategory(ResultSet resultSet) throws SQLException {
        ResourceCategory category = new ResourceCategory();
        category.setId(resultSet.getInt("id_categoria"));
        category.setName(resultSet.getString("nombre_categoria"));
        category.setDescription(resultSet.getString("descripcion"));
        return category;
    }
}
