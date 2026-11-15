package com.littlehorizons.erp.dao;

import com.littlehorizons.erp.model.Grade;
import com.littlehorizons.erp.util.AppLogger;
import com.littlehorizons.erp.util.DatabaseConnection;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public class GradeDao {

    public List<Grade> findAll() {
        String sql = """
                SELECT id_grado, nombre_grado, nivel_numerico
                FROM grados
                ORDER BY nivel_numerico
                """;
        List<Grade> grades = new ArrayList<>();
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql);
             ResultSet resultSet = statement.executeQuery()) {
            while (resultSet.next()) {
                grades.add(mapGrade(resultSet));
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to list grades", exception);
            throw new IllegalStateException("Unable to list grades.", exception);
        }
        return grades;
    }

    public Optional<Grade> findById(int id) {
        String sql = """
                SELECT id_grado, nombre_grado, nivel_numerico
                FROM grados
                WHERE id_grado = ?
                """;
        try (Connection connection = DatabaseConnection.getInstance().getConnection();
             PreparedStatement statement = connection.prepareStatement(sql)) {
            statement.setInt(1, id);
            try (ResultSet resultSet = statement.executeQuery()) {
                if (resultSet.next()) {
                    return Optional.of(mapGrade(resultSet));
                }
            }
        } catch (SQLException exception) {
            AppLogger.error("Failed to find grade id=" + id, exception);
            throw new IllegalStateException("Unable to find grade.", exception);
        }
        return Optional.empty();
    }

    private Grade mapGrade(ResultSet resultSet) throws SQLException {
        Grade grade = new Grade();
        grade.setId(resultSet.getInt("id_grado"));
        grade.setName(resultSet.getString("nombre_grado"));
        grade.setNumericLevel(resultSet.getInt("nivel_numerico"));
        return grade;
    }
}
