package tn.esprit.utils;

import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.SQLException;
import java.sql.Statement;

public class MyDatabase {
    private static MyDatabase instance;
    private Connection connection;
    private boolean ensuredStreamTables;

    private static final String URL = "jdbc:mysql://localhost:4307/esport-db";
    private static final String USER = "root";
    private static final String PASSWORD = "";

    private MyDatabase() {
        ensureOpenConnection();
        if (connection != null) {
            ensureStreamFeatureTables();
        }
    }

    public static MyDatabase getInstance() {
        if (instance == null) {
            instance = new MyDatabase();
        }
        return instance;
    }

    private void ensureOpenConnection() {
        try {
            if (connection == null || connection.isClosed()) {
                connection = DriverManager.getConnection(URL, USER, PASSWORD);
                ensuredStreamTables = false;
                System.out.println("Connexion JDBC établie (esport-db)");
            }
        } catch (SQLException e) {
            System.out.println("Erreur connexion : " + e.getMessage());
            connection = null;
        }
    }

    /**
     * Crée les tables manquantes (commentaires / réactions vidéo) si la base a été créée avant ces scripts.
     * Pas de FOREIGN KEY ici : l'errno 150 est fréquent si {@code stream}/{@code video} est MyISAM, si {@code id}
     * n'est pas exactement le même type, ou sans InnoDB — l'app reste cohérente côté Java.
     */
    private void ensureStreamFeatureTables() {
        if (connection == null || ensuredStreamTables) {
            return;
        }
        try (Statement st = connection.createStatement()) {
            try {
                st.executeUpdate("ALTER TABLE stream ENGINE=InnoDB");
            } catch (SQLException ignored) {
            }
            try {
                st.executeUpdate("ALTER TABLE video ENGINE=InnoDB");
            } catch (SQLException ignored) {
            }
            try {
                st.executeUpdate("ALTER TABLE stream MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT");
            } catch (SQLException ignored) {
            }
            try {
                st.executeUpdate("ALTER TABLE video MODIFY COLUMN id INT NOT NULL AUTO_INCREMENT");
            } catch (SQLException ignored) {
            }

            // Une requête par table : si l’une échoue, les autres sont quand même tentées.
            execDdl(st,
                    "CREATE TABLE IF NOT EXISTS stream_comment ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY, "
                            + "stream_id INT NOT NULL, "
                            + "username VARCHAR(255) NOT NULL, "
                            + "body TEXT NOT NULL, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + "KEY idx_stream_comment_stream (stream_id)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                    "stream_comment");

            execDdl(st,
                    "CREATE TABLE IF NOT EXISTS video_comment ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY, "
                            + "video_id INT NOT NULL, "
                            + "username VARCHAR(255) NOT NULL, "
                            + "body TEXT NOT NULL, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + "KEY idx_video_comment_video (video_id)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                    "video_comment");

            execDdl(st,
                    "CREATE TABLE IF NOT EXISTS video_reaction ("
                            + "id INT AUTO_INCREMENT PRIMARY KEY, "
                            + "video_id INT NOT NULL, "
                            + "username VARCHAR(255) NOT NULL, "
                            + "type VARCHAR(50) NOT NULL, "
                            + "created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP, "
                            + "KEY idx_video_reaction_video (video_id)"
                            + ") ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci",
                    "video_reaction");

            ensuredStreamTables = true;
        } catch (SQLException e) {
            System.err.println("ensureStreamFeatureTables: " + e.getMessage());
        }
    }

    public Connection getConnection() {
        ensureOpenConnection();
        if (connection != null && !ensuredStreamTables) {
            synchronized (this) {
                if (connection != null && !ensuredStreamTables) {
                    ensureStreamFeatureTables();
                }
            }
        }
        return connection;
    }

    private static void execDdl(Statement st, String ddl, String label) {
        try {
            st.executeUpdate(ddl);
        } catch (SQLException e) {
            System.err.println("ensureStreamFeatureTables [" + label + "]: " + e.getMessage());
        }
    }
}
