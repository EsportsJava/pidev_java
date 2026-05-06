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

    private static final String FROM_EMAIL = "saadamaryem776@gmail.com";
    private static final String PASSWORD = "lowt tvkf sjwb enwr";

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

    public void sendPasswordResetCode(String toEmail, String userName, String code) throws MessagingException {
        String safeName = (userName == null || userName.isBlank()) ? "utilisateur" : userName;
        String body = """
                <html>
                <body style="font-family: Arial; background-color: #0f172a; color: white; padding: 20px;">
                    <div style="background-color: #1e293b; padding: 24px; border-radius: 10px;">
                        <h2 style="color: #c4b5fd;">Reinitialisation du mot de passe</h2>
                        <p>Bonjour <strong>%s</strong>,</p>
                        <p>Utilisez ce code pour reinitialiser votre mot de passe :</p>
                        <div style="background-color: #0f172a; padding: 18px; border-radius: 8px; text-align: center;">
                            <span style="font-size: 28px; font-weight: bold; letter-spacing: 4px; color: #facc15;">%s</span>
                        </div>
                        <p style="margin-top: 16px;">Ce code expire dans 15 minutes et ne peut etre utilise qu'une seule fois.</p>
                    </div>
                </body>
                </html>
                """.formatted(safeName, code);

        sendHtmlEmail(toEmail, "Code de reinitialisation du mot de passe", body);
    }

    private void sendHtmlEmail(String toEmail, String subject, String body) throws MessagingException {
        Message message = new MimeMessage(createSession());
        message.setFrom(new InternetAddress(FROM_EMAIL));
        message.setRecipients(Message.RecipientType.TO, InternetAddress.parse(toEmail));
        message.setSubject(subject);
        message.setContent(body, "text/html; charset=utf-8");
        Transport.send(message);
    }

    private Session createSession() {
        Properties props = new Properties();
        props.put("mail.smtp.host", "smtp.gmail.com");
        props.put("mail.smtp.port", "587");
        props.put("mail.smtp.auth", "true");
        props.put("mail.smtp.starttls.enable", "true");

        return Session.getInstance(props, new Authenticator() {
            @Override
            protected PasswordAuthentication getPasswordAuthentication() {
                return new PasswordAuthentication(FROM_EMAIL, PASSWORD);
            }
        });
    }
}