package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import tn.esprit.entities.User;
import tn.esprit.services.CloudinaryService;
import tn.esprit.services.ServiceUser;
import tn.esprit.services.TwoFactorService;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.opencv.imgcodecs.Imgcodecs;

public class ProfileUserController {

    @FXML private Label nameLabel;
    @FXML private Label emailLabel;
    @FXML private Label roleBadgeLabel;
    @FXML private Label activeStatusLabel;
    @FXML private Label userIdLabel;
    @FXML private Label twoFAStatusLabel;
    @FXML private Label avatarLabel;
    @FXML private ImageView avatarImageView;
    @FXML private Button editProfileButton;
    @FXML private Button uploadPhotoButton;
    @FXML private Label profileImageStatusLabel;
    @FXML private Button enable2FAButton;
    @FXML private Button disable2FAButton;
    @FXML private Label faceRecognitionStatusLabel;
    @FXML private Button enableFaceRecognitionButton;
    @FXML private Button profileDisableFaceButton;
    @FXML private javafx.scene.layout.VBox cameraSection;
    @FXML private ImageView cameraView;
    @FXML private Label cameraStatusLabel;
    @FXML private Button captureFaceButton;
    @FXML private Button disableFaceButton;

    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private ScheduledExecutorService timer;
    private Mat lastDetectedFace;
    private final ServiceUser serviceUser = new ServiceUser();
    private final TwoFactorService twoFactorService = new TwoFactorService();
    private final CloudinaryService cloudinaryService = new CloudinaryService();

    @FXML
    public void initialize() {
        try {
            nu.pattern.OpenCV.loadLocally();
            System.out.println("OpenCV loaded successfully");
            System.out.println("Face detector initialization skipped (cascade issues)");
            refreshProfile();
        } catch (Exception e) {
            System.out.println("Error initializing profile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String loadCascadePath() {
        try {
            URL resource = getClass().getResource("/haarcascade_frontalface_default.xml");
            if (resource == null) {
                System.out.println("Cascade file not found in resources!");
                return null;
            }

            if (resource.getProtocol().equals("jar")) {
                try (java.io.InputStream is = resource.openStream()) {
                    File temp = File.createTempFile("cascade", ".xml");
                    java.nio.file.Files.copy(is, temp.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    temp.deleteOnExit();
                    System.out.println("Cascade extracted to temp: " + temp.getAbsolutePath());
                    return temp.getAbsolutePath();
                }
            }

            String path = resource.getPath();
            path = java.net.URLDecoder.decode(path, "UTF-8");
            if (path.startsWith("/") && path.length() > 2 && path.charAt(2) == ':') {
                path = path.substring(1);
            }
            System.out.println("Cascade path: " + path);
            if (!new File(path).exists()) {
                System.out.println("Cascade file does not exist at: " + path);
                return null;
            }
            return path;
        } catch (Exception e) {
            System.out.println("Error loading cascade: " + e.getMessage());
            e.printStackTrace();
            return null;
        }
    }

    @FXML
    private void handleEditProfile() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ajouterUser.fxml"));
            Parent root = loader.load();
            AjouterUserController controller = loader.getController();
            controller.setUserToEdit(currentUser);

            Stage stage = new Stage();
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setTitle("Modifier Profil");
            stage.setScene(new Scene(root));
            stage.showAndWait();

            refreshProfile();
        } catch (IOException e) {
            nameLabel.setText("Unable to open edit form");
        }
    }

    @FXML
    private void handleLogout() {
        SessionManager.clear();

        try {
            Parent root = FXMLLoader.load(getClass().getResource("/Login.fxml"));
            Stage stage = (Stage) editProfileButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            nameLabel.setText("Unable to open login page");
        }
    }

    private void refreshProfile() {
        try {
            User user = SessionManager.getCurrentUser();

            if (user == null) {
                nameLabel.setText("Guest User");
                emailLabel.setText("No authenticated user");
                roleBadgeLabel.setText("VISITOR");
                activeStatusLabel.setText("Offline");
                userIdLabel.setText("--");
                if (twoFAStatusLabel != null) {
                    twoFAStatusLabel.setText("2FA not configured");
                }
                updateTwoFactorButtons(false);
                updateFaceRecognitionButtons(false);
                if (cameraSection != null) {
                    cameraSection.setVisible(false);
                    cameraSection.setManaged(false);
                }
                avatarLabel.setText("GU");
                loadProfileImage(null);
                return;
            }

            nameLabel.setText(safeValue(user.getNom(), "Unknown User"));
            emailLabel.setText(safeValue(user.getEmail(), "No email"));
            roleBadgeLabel.setText(formatRole(user.getRoles()));
            avatarLabel.setText(buildInitials(user.getNom(), user.getEmail()));
            loadProfileImage(user);
            updateTwoFactorStatus(user);
            updateFaceRecognitionStatus(user);

            if (user.isFaceEnabled() && user.getFaceEncoding() != null) {
                System.out.println("User has face recognition enabled");
            }
        } catch (NullPointerException e) {
            System.out.println("NullPointerException in refreshProfile: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private String safeValue(String value, String fallback) {
        return value == null || value.isBlank() ? fallback : value;
    }

    private String formatRole(String roles) {
        if (roles == null || roles.isBlank()) {
            return "USER";
        }

        return roles
                .replace("[", "")
                .replace("]", "")
                .replace("\"", "")
                .replace("ROLE_", "")
                .replace("_", " ")
                .trim()
                .toUpperCase();
    }

    private String buildInitials(String name, String email) {
        String source = (name != null && !name.isBlank()) ? name.trim() : email;
        if (source == null || source.isBlank()) {
            return "UP";
        }

        String[] parts = source.split("\\s+");
        if (parts.length >= 2) {
            return (parts[0].substring(0, 1) + parts[1].substring(0, 1)).toUpperCase();
        }

        String compact = source.replaceAll("[^A-Za-z0-9]", "");
        if (compact.length() >= 2) {
            return compact.substring(0, 2).toUpperCase();
        }

        return compact.isEmpty() ? "UP" : compact.toUpperCase();
    }

    @FXML
    private void handleUploadPhoto() {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            updateProfileImageStatus("No user is logged in.", "#e8314a");
            return;
        }

        if (!cloudinaryService.isConfigured()) {
            updateProfileImageStatus(cloudinaryService.getConfigurationError(), "#e8314a");
            return;
        }

        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Select Profile Picture");
        fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Image Files", "*.png", "*.jpg", "*.jpeg", "*.webp")
        );

        Stage stage = (Stage) editProfileButton.getScene().getWindow();
        File selectedFile = fileChooser.showOpenDialog(stage);
        if (selectedFile == null) {
            return;
        }

        uploadPhotoButton.setDisable(true);
        updateProfileImageStatus("Uploading image...", "#f5a623");

        new Thread(() -> {
            try {
                String imageUrl = cloudinaryService.uploadProfileImage(selectedFile, currentUser.getId());
                currentUser.setProfileImageUrl(imageUrl);
                serviceUser.modifier(currentUser);
                SessionManager.setCurrentUser(currentUser);

                Platform.runLater(() -> {
                    loadProfileImage(currentUser);
                    updateProfileImageStatus("Profile picture updated.", "#22aa44");
                    uploadPhotoButton.setDisable(false);
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    updateProfileImageStatus("Database update failed: " + e.getMessage(), "#e8314a");
                    uploadPhotoButton.setDisable(false);
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    updateProfileImageStatus("Upload failed: " + e.getMessage(), "#e8314a");
                    uploadPhotoButton.setDisable(false);
                });
            }
        }).start();
    }

    private void loadProfileImage(User user) {
        if (avatarImageView == null || avatarLabel == null) {
            return;
        }

        String imageUrl = user == null ? null : user.getProfileImageUrl();
        if (imageUrl == null || imageUrl.isBlank()) {
            avatarImageView.setImage(null);
            avatarImageView.setVisible(false);
            avatarImageView.setManaged(false);
            avatarLabel.setVisible(true);
            avatarLabel.setManaged(true);
            return;
        }

        Image image = new Image(imageUrl, true);
        avatarImageView.setImage(image);
        avatarImageView.setVisible(true);
        avatarImageView.setManaged(true);
        avatarLabel.setVisible(false);
        avatarLabel.setManaged(false);
    }

    private void updateProfileImageStatus(String message, String color) {
        if (profileImageStatusLabel == null) {
            return;
        }
        profileImageStatusLabel.setText(message);
        profileImageStatusLabel.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 13px;");
    }

    private void updateTwoFactorStatus(User user) {
        if (twoFAStatusLabel == null) {
            return;
        }

        boolean enabled = user.isIs2faEnabled()
                && user.getGoogle2faSecret() != null
                && !user.getGoogle2faSecret().isBlank();

        if (enabled) {
            twoFAStatusLabel.setText("2FA is enabled");
            twoFAStatusLabel.setStyle("-fx-text-fill: #22aa44; -fx-font-size: 14px;");
        } else {
            twoFAStatusLabel.setText("2FA is not enabled");
            twoFAStatusLabel.setStyle("-fx-text-fill: #f5a623; -fx-font-size: 14px;");
        }

        updateTwoFactorButtons(enabled);
    }

    private void updateTwoFactorButtons(boolean enabled) {
        if (enable2FAButton != null) {
            enable2FAButton.setVisible(!enabled);
            enable2FAButton.setManaged(!enabled);
        }
        if (disable2FAButton != null) {
            disable2FAButton.setVisible(enabled);
            disable2FAButton.setManaged(enabled);
        }
    }

    private void updateFaceRecognitionStatus(User user) {
        boolean enabled = user.isFaceEnabled();

        if (faceRecognitionStatusLabel != null) {
            if (enabled) {
                faceRecognitionStatusLabel.setText("Face recognition is enabled");
                faceRecognitionStatusLabel.setStyle("-fx-text-fill: #22aa44; -fx-font-size: 14px;");
            } else {
                faceRecognitionStatusLabel.setText("Face recognition is not enabled");
                faceRecognitionStatusLabel.setStyle("-fx-text-fill: #f5a623; -fx-font-size: 14px;");
            }
        }

        updateFaceRecognitionButtons(enabled);

        if (cameraSection != null) {
            cameraSection.setVisible(false);
            cameraSection.setManaged(false);
        }
    }

    private void updateFaceRecognitionButtons(boolean enabled) {
        if (enableFaceRecognitionButton != null) {
            enableFaceRecognitionButton.setVisible(!enabled);
            enableFaceRecognitionButton.setManaged(!enabled);
        }
        if (profileDisableFaceButton != null) {
            profileDisableFaceButton.setVisible(enabled);
            profileDisableFaceButton.setManaged(enabled);
        }
    }

    public void handleEnableFaceRecognition(ActionEvent actionEvent) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            System.out.println("No user logged in");
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/FaceRecognition.fxml"));
            Parent root = loader.load();

            FaceRecognitionController controller = loader.getController();
            controller.setUser(currentUser);

            Stage stage = (Stage) enableFaceRecognitionButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Face Recognition");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateCameraStatus("Unable to open face recognition screen.", "#e8314a");
        }
    }

    private void startCamera() {
        System.out.println("Starting camera...");

        if (faceDetector == null || faceDetector.empty()) {
            System.out.println("Warning: Face detector not loaded, but continuing with camera...");
        }

        capture = new VideoCapture(0, org.opencv.videoio.Videoio.CAP_DSHOW);
        System.out.println("Camera isOpened: " + capture.isOpened());

        if (!capture.isOpened()) {
            updateCameraStatus("Error: Camera not found. Try camera index 1.", "#e8314a");
            System.out.println("Trying camera index 1...");
            capture = new VideoCapture(1, org.opencv.videoio.Videoio.CAP_DSHOW);
            if (!capture.isOpened()) {
                updateCameraStatus("Error: No camera device found!", "#e8314a");
                return;
            }
        }

        System.out.println("Camera opened successfully, starting frame capture...");
        updateCameraStatus("Camera initialized, waiting for face...", "#888888");

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(this::grabFrame, 0, 33, TimeUnit.MILLISECONDS);
    }

    private void grabFrame() {
        try {
            Mat frame = new Mat();
            if (!capture.isOpened() || !capture.read(frame) || frame.empty()) {
                return;
            }

            Image imageToShow = matToImage(frame);
            if (imageToShow != null) {
                Platform.runLater(() -> cameraView.setImage(imageToShow));
            }

            lastDetectedFace = frame.clone();

            Platform.runLater(() -> {
                updateCameraStatus("Ready to capture. Click Capture Face to save.", "#22aa44");
                captureFaceButton.setStyle("-fx-background-color: #22aa44; -fx-text-fill: white; -fx-padding: 10; -fx-font-weight: bold;");
            });
        } catch (Exception e) {
            System.out.println("Error in grabFrame: " + e.getMessage());
            e.printStackTrace();
        }
    }

    @FXML
    private void handleCaptureFace(ActionEvent event) {
        User currentUser = SessionManager.getCurrentUser();
        if (lastDetectedFace == null || lastDetectedFace.empty() || currentUser == null) {
            updateCameraStatus("No valid face detected. Please look at the camera.", "#e8314a");
            return;
        }

        captureFaceButton.setText("Processing...");
        captureFaceButton.setDisable(true);

        new Thread(() -> {
            try {
                Files.createDirectories(Paths.get("face_data"));

                String path = "face_data/user_" + currentUser.getId() + ".jpg";
                Imgcodecs.imwrite(path, lastDetectedFace);

                System.out.println("Face saved to: " + path);

                currentUser.setFaceEncoding(path);
                currentUser.setFaceEnabled(true);
                serviceUser.modifier(currentUser);
                SessionManager.setCurrentUser(currentUser);

                Platform.runLater(() -> {
                    captureFaceButton.setText("Capture Face");
                    captureFaceButton.setDisable(false);
                    updateCameraStatus("Face enrolled successfully.", "#22aa44");
                    stopCamera();
                    refreshProfile();
                });
            } catch (Exception e) {
                System.out.println("Error saving face: " + e.getMessage());
                e.printStackTrace();
                Platform.runLater(() -> {
                    captureFaceButton.setText("Capture Face");
                    captureFaceButton.setDisable(false);
                    updateCameraStatus("Error saving face: " + e.getMessage(), "#e8314a");
                });
            }
        }).start();
    }

    @FXML
    private void handleDisableFace(ActionEvent event) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            Files.deleteIfExists(Paths.get("face_data/user_" + currentUser.getId() + ".jpg"));
            currentUser.setFaceEnabled(false);
            currentUser.setFaceEncoding(null);
            serviceUser.modifier(currentUser);
            SessionManager.setCurrentUser(currentUser);
            updateCameraStatus("Face recognition has been disabled.", "#888888");
            stopCamera();
            refreshProfile();
        } catch (Exception e) {
            updateCameraStatus("Error disabling: " + e.getMessage(), "#e8314a");
        }
    }

    private void updateCameraStatus(String message, String color) {
        try {
            Platform.runLater(() -> {
                if (cameraStatusLabel != null) {
                    cameraStatusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
                    cameraStatusLabel.setText(message);
                }
            });
        } catch (Exception e) {
            System.out.println("Error updating status: " + e.getMessage());
        }
    }

    private Image matToImage(Mat frame) {
        try {
            MatOfByte buffer = new MatOfByte();
            Imgcodecs.imencode(".png", frame, buffer);
            byte[] imageData = buffer.toArray();
            if (imageData != null && imageData.length > 0) {
                return new Image(new java.io.ByteArrayInputStream(imageData));
            }
        } catch (Exception e) {
            System.out.println("Error converting Mat to Image: " + e.getMessage());
        }
        return null;
    }

    private void stopCamera() {
        if (timer != null && !timer.isShutdown()) {
            timer.shutdown();
            try {
                timer.awaitTermination(100, TimeUnit.MILLISECONDS);
            } catch (InterruptedException ignored) {
            }
        }
        if (capture != null && capture.isOpened()) {
            capture.release();
        }
        if (lastDetectedFace != null) {
            lastDetectedFace.release();
            lastDetectedFace = null;
        }
    }

    private void loadUserFaceImage(int userId) {
        System.out.println("Face image stored for user: " + userId);
    }

    @FXML
    private void handleEnable2FA(ActionEvent event) {
        try {
            URL fxmlUrl = getClass().getResource("/TwoFactorSetup.fxml");
            if (fxmlUrl == null) {
                fxmlUrl = getClass().getResource("/tn/esprit/views/TwoFactorSetup.fxml");
            }
            if (fxmlUrl == null) {
                System.out.println("TwoFactorSetup.fxml not found!");
                return;
            }

            Parent root = FXMLLoader.load(fxmlUrl);
            Stage stage = (Stage) editProfileButton.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Setup 2FA");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleDisable2FA(ActionEvent event) {
        User currentUser = SessionManager.getCurrentUser();
        if (currentUser == null) {
            return;
        }

        try {
            twoFactorService.disable2FA(currentUser);
            serviceUser.modifier(currentUser);
            SessionManager.setCurrentUser(currentUser);
            refreshProfile();
        } catch (Exception e) {
            if (twoFAStatusLabel != null) {
                twoFAStatusLabel.setText("Unable to disable 2FA");
                twoFAStatusLabel.setStyle("-fx-text-fill: #e8314a; -fx-font-size: 14px;");
            }
        }
    }
}
