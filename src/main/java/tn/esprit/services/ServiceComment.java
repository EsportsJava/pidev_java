package tn.esprit.services;

import tn.esprit.entities.Comment;
import tn.esprit.utils.MyDatabase;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

public class ServiceComment {

    private Connection cnx;

    public ServiceComment() {
        cnx = MyDatabase.getInstance().getConnection();
    }

    // ========== AJOUTER UN COMMENTAIRE ==========
    public void ajouter(Comment comment) {
        String sql = "INSERT INTO comment (blog_id, user_id, content, created_at, user_country, user_country_code, user_flag) VALUES (?, ?, ?, NOW(), ?, ?, ?)";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, comment.getBlogId());
            ps.setInt(2, comment.getUserId());
            ps.setString(3, comment.getContent());
            ps.setString(4, comment.getUserCountry());
            ps.setString(5, comment.getUserCountryCode());
            ps.setString(6, comment.getUserFlag());

            int result = ps.executeUpdate();
            System.out.println("✅ Commentaire ajouté - Résultat: " + result);
            System.out.println("   Pays: " + comment.getUserCountry());
            System.out.println("   Drapeau: " + comment.getUserFlag());

            // Mettre à jour le compteur
            String updateCount = "UPDATE blog SET comment_count = (SELECT COUNT(*) FROM comment WHERE blog_id = ?) WHERE id = ?";
            try (PreparedStatement ps2 = cnx.prepareStatement(updateCount)) {
                ps2.setInt(1, comment.getBlogId());
                ps2.setInt(2, comment.getBlogId());
                ps2.executeUpdate();
            }

        } catch (SQLException e) {
            System.err.println("❌ Erreur insertion commentaire: " + e.getMessage());
            e.printStackTrace();
        }
    }
    // ========== MODIFIER UN COMMENTAIRE ==========
    public void modifier(Comment comment) {
        String sql = "UPDATE comment SET content = ? WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setString(1, comment.getContent());
            ps.setInt(2, comment.getId());
            ps.executeUpdate();
            System.out.println("✅ Commentaire modifié");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========== SUPPRIMER UN COMMENTAIRE ==========
    public void supprimer(int commentId, int blogId) {
        String sql = "DELETE FROM comment WHERE id = ?";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, commentId);
            ps.executeUpdate();

            // Mettre à jour le compteur de commentaires du blog
            String updateCount = "UPDATE blog SET comment_count = (SELECT COUNT(*) FROM comment WHERE blog_id = ?) WHERE id = ?";
            try (PreparedStatement ps2 = cnx.prepareStatement(updateCount)) {
                ps2.setInt(1, blogId);
                ps2.setInt(2, blogId);
                ps2.executeUpdate();
            }

            System.out.println("✅ Commentaire supprimé");
        } catch (SQLException e) {
            e.printStackTrace();
        }
    }

    // ========== RÉCUPÉRER LES COMMENTAIRES D'UN BLOG ==========
    public List<Comment> getByBlogId(int blogId) {
        List<Comment> list = new ArrayList<>();

        String sql = "SELECT c.*, u.nom as user_name FROM comment c " +
                "JOIN user u ON c.user_id = u.id " +
                "WHERE c.blog_id = ? ORDER BY c.created_at DESC";

        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ResultSet rs = ps.executeQuery();
            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getInt("id"));
                c.setBlogId(rs.getInt("blog_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setContent(rs.getString("content"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUserName(rs.getString("user_name"));

                // ⚠️ Récupérer les colonnes (même si null)
                c.setUserCountry(rs.getString("user_country"));
                c.setUserCountryCode(rs.getString("user_country_code"));
                c.setUserFlag(rs.getString("user_flag"));

                System.out.println("📖 COMMENTAIRE CHARGÉ:");
                System.out.println("   User: " + c.getUserName());
                System.out.println("   Pays DB: " + c.getUserCountry());
                System.out.println("   Drapeau DB: " + c.getUserFlag());

                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========== RÉCUPÉRER TOUS LES COMMENTAIRES (ADMIN) ==========
    public List<Comment> getAllWithDetails() {
        List<Comment> list = new ArrayList<>();

        String sql = "SELECT c.*, u.nom as user_name, b.title as blog_title " +
                "FROM comment c " +
                "JOIN user u ON c.user_id = u.id " +
                "JOIN blog b ON c.blog_id = b.id " +
                "ORDER BY c.created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getInt("id"));
                c.setBlogId(rs.getInt("blog_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setContent(rs.getString("content"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUserName(rs.getString("user_name"));
                c.setBlogTitle(rs.getString("blog_title"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========== COMMENTAIRES D'AUJOURD'HUI ==========
    public List<Comment> getCommentsOfToday() {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.*, u.nom as user_name, b.title as blog_title " +
                "FROM comment c " +
                "JOIN user u ON c.user_id = u.id " +
                "JOIN blog b ON c.blog_id = b.id " +
                "WHERE DATE(c.created_at) = CURDATE() " +
                "ORDER BY c.created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getInt("id"));
                c.setBlogId(rs.getInt("blog_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setContent(rs.getString("content"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUserName(rs.getString("user_name"));
                c.setBlogTitle(rs.getString("blog_title"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========== COMMENTAIRES DE CETTE SEMAINE ==========
    public List<Comment> getCommentsOfThisWeek() {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.*, u.nom as user_name, b.title as blog_title " +
                "FROM comment c " +
                "JOIN user u ON c.user_id = u.id " +
                "JOIN blog b ON c.blog_id = b.id " +
                "WHERE YEARWEEK(c.created_at) = YEARWEEK(CURDATE()) " +
                "ORDER BY c.created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getInt("id"));
                c.setBlogId(rs.getInt("blog_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setContent(rs.getString("content"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUserName(rs.getString("user_name"));
                c.setBlogTitle(rs.getString("blog_title"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========== COMMENTAIRES DE CE MOIS ==========
    public List<Comment> getCommentsOfThisMonth() {
        List<Comment> list = new ArrayList<>();
        String sql = "SELECT c.*, u.nom as user_name, b.title as blog_title " +
                "FROM comment c " +
                "JOIN user u ON c.user_id = u.id " +
                "JOIN blog b ON c.blog_id = b.id " +
                "WHERE MONTH(c.created_at) = MONTH(CURDATE()) " +
                "AND YEAR(c.created_at) = YEAR(CURDATE()) " +
                "ORDER BY c.created_at DESC";

        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {

            while (rs.next()) {
                Comment c = new Comment();
                c.setId(rs.getInt("id"));
                c.setBlogId(rs.getInt("blog_id"));
                c.setUserId(rs.getInt("user_id"));
                c.setContent(rs.getString("content"));
                c.setCreatedAt(rs.getTimestamp("created_at"));
                c.setUserName(rs.getString("user_name"));
                c.setBlogTitle(rs.getString("blog_title"));
                list.add(c);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return list;
    }

    // ========== COMPTER LES COMMENTAIRES D'UN BLOG ==========
    public int getCountByBlogId(int blogId) {
        String sql = "SELECT COUNT(*) FROM comment WHERE blog_id = ?";
        try (PreparedStatement ps = cnx.prepareStatement(sql)) {
            ps.setInt(1, blogId);
            ResultSet rs = ps.executeQuery();
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }

    // ========== COMPTER LE NOMBRE TOTAL DE COMMENTAIRES (ADMIN) ==========
    public int getTotalCommentCount() {
        String sql = "SELECT COUNT(*) FROM comment";
        try (Statement st = cnx.createStatement();
             ResultSet rs = st.executeQuery(sql)) {
            if (rs.next()) {
                return rs.getInt(1);
            }
        } catch (SQLException e) {
            e.printStackTrace();
        }
        return 0;
    }
}