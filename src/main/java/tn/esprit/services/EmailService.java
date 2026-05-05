package tn.esprit.services;

import jakarta.mail.Authenticator;
import jakarta.mail.Message;
import jakarta.mail.MessagingException;
import jakarta.mail.PasswordAuthentication;
import jakarta.mail.Session;
import jakarta.mail.Transport;
import jakarta.mail.internet.InternetAddress;
import jakarta.mail.internet.MimeMessage;

import java.util.Properties;

public class EmailService {

    private static final String FROM_EMAIL = "rajhiaziz2@gmail.com"; // ← ton email
    private static final String PASSWORD = "utgi jjoa ohux mymp";     // ← mot de passe app Gmail

    public void sendLowStockAlert(String productName, int stock, String toEmail) {
        try {
            String body = """
                    <html>
                    <body style="font-family: Arial; background-color: #0f172a; color: white; padding: 20px;">
                        <div style="background-color: #1e293b; padding: 20px; border-radius: 10px;">
                            <h2 style="color: #f87171;">Alerte Stock Faible</h2>
                            <p>Le produit <strong style="color: #7c3aed;">%s</strong> a un stock critique.</p>
                            <div style="background-color: #0f172a; padding: 15px; border-radius: 8px;">
                                <p>Produit : <strong>%s</strong></p>
                                <p>Stock restant : <strong style="color: #f87171;">%d</strong></p>
                                <p>Action requise : Reapprovisionner immediatement.</p>
                            </div>
                            <p style="color: #94a3b8; font-size: 12px;">
                                Gaming Gear Management System
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(productName, productName, stock);

            sendHtmlEmail(toEmail, "Alerte Stock Faible - " + productName, body);
            System.out.println("Email envoye pour : " + productName);
        } catch (MessagingException e) {
            System.out.println("Erreur email : " + e.getMessage());
        }
    }

    public void sendTeamInvitationEmail(String toEmail, String equipeName, String ownerName) {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        Session session = Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });

        try {
            Message message = new MimeMessage(session);
            message.setFrom(new InternetAddress(FROM_EMAIL));
            message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
            message.setSubject("🎮 Invitation à rejoindre l'équipe " + equipeName);

            String body = """
                    <html>
                    <body style="font-family: Arial; background-color: #0f172a; color: white; padding: 20px;">
                        <div style="background-color: #1e293b; padding: 24px; border-radius: 14px; border: 1px solid rgba(124,58,237,0.3);">
                            <h2 style="color: #a78bfa;">🎮 Invitation d'équipe</h2>
                            <p>Bonjour,</p>
                            <p>Vous avez été invité(e) par <strong style="color: #7c3aed;">%s</strong> à rejoindre l'équipe <strong style="color: #7c3aed;">%s</strong>.</p>
                            <div style="background-color: #0f172a; padding: 16px; border-radius: 10px; margin: 16px 0;">
                                <p>👑 Invité par : <strong>%s</strong></p>
                                <p>🏆 Équipe : <strong>%s</strong></p>
                            </div>
                            <p>Connectez-vous à la plateforme pour <strong>accepter</strong> ou <strong>refuser</strong> cette invitation.</p>
                            <p style="color: #64748b; font-size: 12px; margin-top: 20px;">
                                ESPORTS COMMUNITY — Gaming Platform
                            </p>
                        </div>
                    </body>
                    </html>
                    """.formatted(ownerName, equipeName, ownerName, equipeName);

            message.setContent(body, "text/html; charset=utf-8");
            Transport.send(message);

            System.out.println("✅ Email invitation envoyé à : " + toEmail);

        } catch (MessagingException e) {
            System.out.println("❌ Erreur email invitation : " + e.getMessage());
        }
    }
}