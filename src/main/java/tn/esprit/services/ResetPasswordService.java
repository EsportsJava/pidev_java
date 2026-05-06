package tn.esprit.services;

import tn.esprit.entities.User;
import tn.esprit.utils.MyDatabase;

import java.security.SecureRandom;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.time.LocalDateTime;

public class ResetPasswordService {

    private static final int TOKEN_EXPIRATION_MINUTES = 15;
    private static final SecureRandom RANDOM = new SecureRandom();

    private final Connection connection;
    private final ServiceUser serviceUser;
    private final EmailService emailService;

    public ResetPasswordService() {
        this(MyDatabase.getInstance().getConnection(), new ServiceUser(), new EmailService());
    }

    public ResetPasswordService(Connection connection, ServiceUser serviceUser, EmailService emailService) {
        this.connection = connection;
        this.serviceUser = serviceUser;
        this.emailService = emailService;
    }

    public void requestPasswordReset(String email) throws SQLException {
        ensureTokenTableExists();
        User user = serviceUser.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Aucun compte n'est associe a cet email.");
        }

        String code = generateResetCode();
        LocalDateTime expiresAt = LocalDateTime.now().plusMinutes(TOKEN_EXPIRATION_MINUTES);

        invalidateActiveTokens(user.getId());
        saveResetToken(user.getId(), code, expiresAt);

        try {
            emailService.sendPasswordResetCode(user.getEmail(), user.getNom(), code);
        } catch (Exception e) {
            invalidateActiveTokens(user.getId());
            throw new IllegalStateException(buildResetEmailErrorMessage(e), e);
        }
    }

    public boolean isResetCodeValid(String email, String code) throws SQLException {
        ensureTokenTableExists();
        return findActiveToken(email, code) != null;
    }

    public void resetPassword(String email, String code, String newPassword) throws SQLException {
        ensureTokenTableExists();
        if (newPassword == null || newPassword.length() < 6) {
            throw new IllegalArgumentException("Le mot de passe doit contenir au moins 6 caracteres.");
        }

        ResetTokenRecord tokenRecord = findActiveToken(email, code);
        if (tokenRecord == null) {
            throw new IllegalArgumentException("Le code est invalide ou expire.");
        }

        serviceUser.updatePassword(tokenRecord.userId(), newPassword);
        markTokenAsUsed(tokenRecord.id());
    }

    private void saveResetToken(int userId, String code, LocalDateTime expiresAt) throws SQLException {
        String sql = "INSERT INTO password_reset_token(user_id, token, expires_at, used) VALUES (?, ?, ?, false)";
        try (PreparedStatement ps = getConnectionOrThrow().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.setString(2, code);
            ps.setTimestamp(3, Timestamp.valueOf(expiresAt));
            ps.executeUpdate();
        }
    }

    private void invalidateActiveTokens(int userId) throws SQLException {
        String sql = "UPDATE password_reset_token SET used = true WHERE user_id = ? AND used = false";
        try (PreparedStatement ps = getConnectionOrThrow().prepareStatement(sql)) {
            ps.setInt(1, userId);
            ps.executeUpdate();
        }
    }

    private void markTokenAsUsed(int tokenId) throws SQLException {
        String sql = "UPDATE password_reset_token SET used = true WHERE id = ?";
        try (PreparedStatement ps = getConnectionOrThrow().prepareStatement(sql)) {
            ps.setInt(1, tokenId);
            ps.executeUpdate();
        }
    }

    private ResetTokenRecord findActiveToken(String email, String code) throws SQLException {
        String sql = """
                SELECT prt.id, prt.user_id
                FROM password_reset_token prt
                INNER JOIN user u ON u.id = prt.user_id
                WHERE u.email = ?
                  AND prt.token = ?
                  AND prt.used = false
                  AND prt.expires_at >= NOW()
                ORDER BY prt.id DESC
                LIMIT 1
                """;
        try (PreparedStatement ps = getConnectionOrThrow().prepareStatement(sql)) {
            ps.setString(1, email);
            ps.setString(2, code);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return new ResetTokenRecord(rs.getInt("id"), rs.getInt("user_id"));
                }
            }
        }
        return null;
    }

    private Connection getConnectionOrThrow() throws SQLException {
        if (connection == null) {
            throw new SQLException("Database connection is not available.");
        }
        return connection;
    }

    private void ensureTokenTableExists() throws SQLException {
        String sql = """
                CREATE TABLE IF NOT EXISTS password_reset_token (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT NOT NULL,
                    token VARCHAR(20) NOT NULL,
                    expires_at DATETIME NOT NULL,
                    used BOOLEAN NOT NULL DEFAULT FALSE,
                    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    CONSTRAINT fk_password_reset_user FOREIGN KEY (user_id) REFERENCES `user`(id) ON DELETE CASCADE
                )
                """;
        try (Statement stmt = getConnectionOrThrow().createStatement()) {
            stmt.execute(sql);
        }
    }

    private String generateResetCode() {
        int value = 100000 + RANDOM.nextInt(900000);
        return String.valueOf(value);
    }

    private String buildResetEmailErrorMessage(Exception e) {
        String message = e.getMessage();
        if (message != null && !message.isBlank()) {
            return "Impossible d'envoyer l'email de reinitialisation. " + message;
        }
        return "Impossible d'envoyer l'email de reinitialisation.";
    }

    private record ResetTokenRecord(int id, int userId) {
    }
}