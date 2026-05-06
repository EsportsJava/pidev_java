package tn.esprit.services;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

import tn.esprit.entities.Category;
import tn.esprit.utils.MyDatabase;

public class CategoryService {
    private Connection connection;

    public CategoryService() {
        connection = MyDatabase.getInstance().getConnection();
    }


    public void addCategory(Category category) {
        String sql = "INSERT INTO category (name) VALUES (?)";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.executeUpdate();
            System.out.println("Catégorie ajoutée : " + category.getName());
        } catch (SQLException e) {
            System.out.println("Erreur ajout : " + e.getMessage());
        }
    }


    public List<Category> getAllCategories() {
        List<Category> list = new ArrayList<>();
        String sql = "SELECT * FROM category";
        try (Statement st = connection.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            while (rs.next()) {
                list.add(new Category(rs.getInt("id"), rs.getString("name")));
            }
        } catch (SQLException e) {
            System.out.println("Erreur lecture : " + e.getMessage());
        }
        return list;
    }


    public Category getCategoryById(int id) {
        String sql = "SELECT * FROM category WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return new Category(rs.getInt("id"), rs.getString("name"));
            }
        } catch (SQLException e) {
            System.out.println(" Erreur : " + e.getMessage());
        }
        return null;
    }


    public void updateCategory(Category category) {
        String sql = "UPDATE category SET name = ? WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setString(1, category.getName());
            ps.setInt(2, category.getId());
            ps.executeUpdate();
            System.out.println("Catégorie mise à jour : " + category.getId());
        } catch (SQLException e) {
            System.out.println(" Erreur mise à jour : " + e.getMessage());
        }
    }


    public void deleteCategory(int id) {
        String sql = "DELETE FROM category WHERE id = ?";
        try (PreparedStatement ps = connection.prepareStatement(sql)) {
            ps.setInt(1, id);
            ps.executeUpdate();
            System.out.println(" Catégorie supprimée : " + id);
        } catch (SQLException e) {
            System.out.println("Erreur suppression : " + e.getMessage());
        }
    }
}