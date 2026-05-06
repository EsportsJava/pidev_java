package tn.esprit.services;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import dev.samstevens.totp.code.*;
import dev.samstevens.totp.secret.DefaultSecretGenerator;
import dev.samstevens.totp.time.SystemTimeProvider;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import tn.esprit.entities.User;

import java.awt.image.BufferedImage;

public class TwoFactorService {

    private static final String ISSUER = "EsportsCommunity";

    // ── Secret Generation ─────────────────────────────────────────────────────
    public String generateSecret() {
        return new DefaultSecretGenerator().generate();
    }

    // ── QR Code Generation ────────────────────────────────────────────────────
    public Image generateQRCodeImage(String email, String secret) {
        try {
            String otpAuthUrl = buildOtpAuthUrl(email, secret);
            QRCodeWriter writer = new QRCodeWriter();
            BitMatrix bitMatrix = writer.encode(otpAuthUrl, BarcodeFormat.QR_CODE, 200, 200);
            BufferedImage bufferedImage = MatrixToImageWriter.toBufferedImage(bitMatrix);
            return SwingFXUtils.toFXImage(bufferedImage, null);
        } catch (Exception e) {
            System.out.println("QR code generation error: " + e.getMessage());
            return null;
        }
    }

    // ── OTP URL Builder ───────────────────────────────────────────────────────
    private String buildOtpAuthUrl(String email, String secret) {
        return "otpauth://totp/" + ISSUER + ":" + email
                + "?secret=" + secret
                + "&issuer=" + ISSUER
                + "&algorithm=SHA1"
                + "&digits=6"
                + "&period=30";
    }

    // ── Code Verification ─────────────────────────────────────────────────────
    public boolean verifyCode(String secret, String code) {
        try {
            if (secret == null || code == null || code.trim().length() != 6) {
                return false;
            }
            CodeVerifier verifier = new DefaultCodeVerifier(
                    new DefaultCodeGenerator(),
                    new SystemTimeProvider()
            );
            return verifier.isValidCode(secret, code.trim());
        } catch (Exception e) {
            System.out.println("2FA verification error: " + e.getMessage());
            return false;
        }
    }

    // ── Enable 2FA for User ───────────────────────────────────────────────────
    public boolean enable2FA(User user, String secret, String code) {
        if (!verifyCode(secret, code)) return false;
        user.setGoogle2faSecret(secret);
        user.setIs2faEnabled(true);
        return true;
    }

    // ── Disable 2FA for User ──────────────────────────────────────────────────
    public void disable2FA(User user) {
        user.setGoogle2faSecret(null);
        user.setIs2faEnabled(false);
    }

    // ── Check if 2FA needed on login ──────────────────────────────────────────
    public boolean requires2FA(User user) {
        return user != null
                && user.isIs2faEnabled()
                && user.getGoogle2faSecret() != null
                && !user.getGoogle2faSecret().isEmpty();
    }
}