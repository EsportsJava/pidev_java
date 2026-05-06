package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;

public final class BlogSchemaInitializer {

    private static boolean initialized;

    private BlogSchemaInitializer() {
    }

    public static synchronized void ensureInitialized(Connection connection) {
        if (initialized || connection == null) {
            return;
        }

        try (Statement statement = connection.createStatement()) {
            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS blog_like (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "blog_id INT NOT NULL, " +
                            "user_id INT NOT NULL, " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "UNIQUE KEY uq_blog_like (blog_id, user_id), " +
                            "INDEX idx_blog_like_blog (blog_id), " +
                            "INDEX idx_blog_like_user (user_id), " +
                            "CONSTRAINT fk_blog_like_blog FOREIGN KEY (blog_id) REFERENCES blog(id) ON DELETE CASCADE, " +
                            "CONSTRAINT fk_blog_like_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            statement.executeUpdate(
                    "CREATE TABLE IF NOT EXISTS blog_rating (" +
                            "id INT AUTO_INCREMENT PRIMARY KEY, " +
                            "blog_id INT NOT NULL, " +
                            "user_id INT NOT NULL, " +
                            "rating INT NOT NULL, " +
                            "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                            "updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP, " +
                            "UNIQUE KEY uq_blog_rating (blog_id, user_id), " +
                            "INDEX idx_blog_rating_blog (blog_id), " +
                            "INDEX idx_blog_rating_user (user_id), " +
                            "CONSTRAINT fk_blog_rating_blog FOREIGN KEY (blog_id) REFERENCES blog(id) ON DELETE CASCADE, " +
                            "CONSTRAINT fk_blog_rating_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE" +
                            ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci"
            );

            addColumnIfMissing(connection, statement, "comment", "user_country", "VARCHAR(100) NULL");
            addColumnIfMissing(connection, statement, "comment", "user_country_code", "VARCHAR(10) NULL");
            addColumnIfMissing(connection, statement, "comment", "user_flag", "VARCHAR(16) NULL");

            initialized = true;
        } catch (SQLException e) {
            System.err.println("Erreur initialisation schema blog: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private static void addColumnIfMissing(
            Connection connection,
            Statement statement,
            String tableName,
            String columnName,
            String definition
    )
            throws SQLException {
        if (columnExists(connection, tableName, columnName)) {
            return;
        }

        statement.executeUpdate("ALTER TABLE " + tableName + " ADD COLUMN " + columnName + " " + definition);
    }

    private static boolean columnExists(Connection connection, String tableName, String columnName) throws SQLException {
        DatabaseMetaData metaData = connection.getMetaData();
        try (ResultSet columns = metaData.getColumns(connection.getCatalog(), null, tableName, columnName)) {
            return columns.next();
        }
    }
}
