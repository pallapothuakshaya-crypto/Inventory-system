package com.ims.inventory.dao;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.SQLIntegrityConstraintViolationException; // <-- NEW IMPORT ADDED
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.springframework.stereotype.Repository;

import com.ims.inventory.config.DatabaseConnection;
import com.ims.inventory.model.Category;

@Repository
public class CategoryDAOImpl implements CategoryDAO {

    private Connection conn() throws SQLException {
        return DatabaseConnection.getInstance().getConnection();
    }

    @Override
    public void addCategory(Category c) {
        String sql = "INSERT INTO categories (category_name, description) VALUES (?, ?)";
        try (PreparedStatement ps = conn().prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            ps.setString(1, c.getCategoryName());
            ps.setString(2, c.getDescription());
            ps.executeUpdate();
            try (ResultSet keys = ps.getGeneratedKeys()) {
                if (keys.next()) {
                    c.setCategoryId(keys.getLong(1));
                }
            }
        } 
        // NEW CATCH BLOCK ADDED HERE
        catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEntryException("Category name already exists", e);
        } 
        catch (SQLException e) {
            throw new DaoException("Could not add category", e);
        }
    }

    @Override
    public boolean updateCategory(Category c) {
        String sql = "UPDATE categories SET category_name = ?, description = ? WHERE category_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setString(1, c.getCategoryName());
            ps.setString(2, c.getDescription());
            ps.setLong(3, c.getCategoryId());
            return ps.executeUpdate() > 0;
        } 
        // NEW CATCH BLOCK ADDED HERE
        catch (SQLIntegrityConstraintViolationException e) {
            throw new DuplicateEntryException("Category name already exists", e);
        } 
        catch (SQLException e) {
            throw new DaoException("Could not update category", e);
        }
    }

    @Override
    public boolean deleteCategory(long id) {
        String sql = "DELETE FROM categories WHERE category_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            return ps.executeUpdate() > 0;
        } catch (SQLException e) {
            throw new DaoException("Could not delete category", e);
        }
    }

    @Override
    public Optional<Category> getCategoryById(long id) {
        String sql = "SELECT category_id, category_name, description FROM categories WHERE category_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                return rs.next() ? Optional.of(mapRow(rs)) : Optional.empty();
            }
        } catch (SQLException e) {
            throw new DaoException("Could not read category", e);
        }
    }

    @Override
    public List<Category> getAllCategories() {
        String sql = "SELECT category_id, category_name, description FROM categories ORDER BY category_name";
        List<Category> list = new ArrayList<>();
        try (PreparedStatement ps = conn().prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            while (rs.next()) {
                list.add(mapRow(rs));
            }
            return list;
        } catch (SQLException e) {
            throw new DaoException("Could not list categories", e);
        }
    }

    @Override
    public boolean isCategoryInUse(long id) {
        String sql = "SELECT COUNT(*) FROM products WHERE category_id = ?";
        try (PreparedStatement ps = conn().prepareStatement(sql)) {
            ps.setLong(1, id);
            try (ResultSet rs = ps.executeQuery()) {
                rs.next();
                return rs.getInt(1) > 0;
            }
        } catch (SQLException e) {
            throw new DaoException("Could not check category usage", e);
        }
    }

    private Category mapRow(ResultSet rs) throws SQLException {
        return new Category(
            rs.getLong("category_id"),
            rs.getString("category_name"),
            rs.getString("description"));
    }
}