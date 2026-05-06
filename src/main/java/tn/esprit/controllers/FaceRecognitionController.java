package tn.esprit.controllers;

import javafx.application.Platform;
import javafx.event.ActionEvent;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Hyperlink;
import javafx.scene.control.Label;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.core.MatOfRect;
import org.opencv.core.Point;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgcodecs.Imgcodecs;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;
import org.opencv.videoio.VideoCapture;
import org.opencv.videoio.Videoio;
import tn.esprit.entities.User;
import tn.esprit.services.FaceRecognitionService;
import tn.esprit.services.ServiceUser;
import tn.esprit.services.TwoFactorService;
import tn.esprit.utils.SessionManager;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.sql.SQLException;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class FaceRecognitionController {

    @FXML private ImageView cameraView;
    @FXML private Label titleLabel;
    @FXML private Label statusLabel;
    @FXML private Button captureFaceButton;
    @FXML private Button disableFaceButton;
    @FXML private Hyperlink backLink;

    private VideoCapture capture;
    private CascadeClassifier faceDetector;
    private ScheduledExecutorService timer;
    private Mat lastDetectedFace;
    private User user;
    private Mode mode = Mode.ENROLL;

    private final ServiceUser serviceUser = new ServiceUser();
    private final FaceRecognitionService faceRecognitionService = new FaceRecognitionService();
    private final TwoFactorService twoFactorService = new TwoFactorService();

    @FXML
    public void initialize() {
        try {
            nu.pattern.OpenCV.loadLocally();

            faceDetector = new CascadeClassifier();
            boolean cascadeLoaded = loadFaceDetector();

            if (user == null) {
                user = SessionManager.getCurrentUser();
            }

            updateModeUi();
            updateDisableButtonState();
            startCamera();
            if (!faceRecognitionService.isConfigured()) {
                updateStatus(faceRecognitionService.getConfigurationError(), "#e8314a");
            }
            if (!cascadeLoaded) {
                updateStatus("Camera started. Face detection is unavailable, but capture still works.", "#f5a623");
            }
        } catch (Exception e) {
            e.printStackTrace();
            updateStatus("Unable to initialize face recognition: " + e.getMessage(), "#e8314a");
        }
    }

    public void setUser(User user) {
        this.user = user;
        updateDisableButtonState();
    }

    public void setLoginMode() {
        this.mode = Mode.LOGIN;
        if (titleLabel != null) {
            updateModeUi();
        }
    }

    private boolean loadFaceDetector() {
        try {
            URL resource = getClass().getResource("/haarcascade_frontalface_default.xml");
            if (resource == null) {
                System.out.println("Cascade resource not found.");
                return false;
            }

            try (java.io.InputStream inputStream = resource.openStream()) {
                File tempFile = File.createTempFile("cascade", ".xml");
                java.nio.file.Files.copy(
                        inputStream,
                        tempFile.toPath(),
                        java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                tempFile.deleteOnExit();

                long fileSize = tempFile.length();
                if (fileSize < 100_000) {
                    System.out.println("Cascade file is too small to be valid: " + fileSize + " bytes");
                    return false;
                }

                boolean loaded = faceDetector.load(tempFile.getAbsolutePath());
                if (!loaded || faceDetector.empty()) {
                    System.out.println("Failed to load cascade classifier from: " + tempFile.getAbsolutePath());
                    return false;
                }
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    private String getFaceDataPath() {
        if (user == null) {
            return "face_data/enrolled_face.jpg";
        }
        return "face_data/user_" + user.getId() + ".jpg";
    }

    private void updateDisableButtonState() {
        if (mode == Mode.LOGIN) {
            if (disableFaceButton != null) {
                disableFaceButton.setVisible(false);
                disableFaceButton.setManaged(false);
            }
            return;
        }

        boolean enabled = user != null
                && user.isFaceEnabled()
                && Files.exists(Paths.get(getFaceDataPath()));

        if (disableFaceButton != null) {
            disableFaceButton.setVisible(enabled);
            disableFaceButton.setManaged(enabled);
        }
    }

    private void startCamera() {
        capture = new VideoCapture(0, Videoio.CAP_DSHOW);
        if (!capture.isOpened()) {
            capture = new VideoCapture(1, Videoio.CAP_DSHOW);
        }

        if (!capture.isOpened()) {
            updateStatus("Error accessing camera: requested device not found.", "#e8314a");
            return;
        }

        updateStatus("Camera initialized. Position your face in front of it.", "#888888");

        timer = Executors.newSingleThreadScheduledExecutor();
        timer.scheduleAtFixedRate(this::grabFrame, 0, 33, TimeUnit.MILLISECONDS);
    }

    private void grabFrame() {
        Mat frame = new Mat();
        if (!capture.read(frame) || frame.empty()) {
            return;
        }

        Mat previewFrame = frame.clone();

        if (faceDetector != null && !faceDetector.empty()) {
            Mat gray = new Mat();
            MatOfRect faceDetections = new MatOfRect();

            Imgproc.cvtColor(frame, gray, Imgproc.COLOR_BGR2GRAY);
            Imgproc.equalizeHist(gray, gray);
            faceDetector.detectMultiScale(
                    gray,
                    faceDetections,
                    1.1,
                    3,
                    0,
                    new Size(80, 80),
                    new Size(400, 400)
            );

            Rect[] faces = faceDetections.toArray();
            if (faces.length > 0) {
                Rect face = faces[0];
                Imgproc.rectangle(previewFrame, face, new Scalar(255, 0, 0), 2);
                Imgproc.putText(
                        previewFrame,
                        "Face detected",
                        new Point(face.x, Math.max(20, face.y - 10)),
                        Imgproc.FONT_HERSHEY_SIMPLEX,
                        0.6,
                        new Scalar(255, 255, 255),
                        1
                );
            }
        }

        if (lastDetectedFace != null) {
            lastDetectedFace.release();
        }
        lastDetectedFace = frame.clone();

        Image imageToShow = matToImage(previewFrame);
        Platform.runLater(() -> {
            cameraView.setImage(imageToShow);
            if (mode == Mode.LOGIN) {
                updateStatus("Camera is running. Click Sign In when ready.", "#22aa44");
            } else {
                updateStatus("Camera is running. Click Capture Face when ready.", "#22aa44");
            }
        });
    }

    @FXML
    private void handleCaptureFace(ActionEvent event) {
        if (mode == Mode.LOGIN) {
            handleLoginWithFace();
            return;
        }

        User currentUser = user != null ? user : SessionManager.getCurrentUser();
        if (currentUser == null) {
            updateStatus("No user is logged in.", "#e8314a");
            return;
        }

        if (lastDetectedFace == null || lastDetectedFace.empty()) {
            updateStatus("No valid frame available. Please look at the camera.", "#e8314a");
            return;
        }

        captureFaceButton.setText("Processing...");
        captureFaceButton.setDisable(true);

        new Thread(() -> {
            try {
                faceRecognitionService.validateFaceImage(lastDetectedFace);
                Files.createDirectories(Paths.get("face_data"));

                String path = getFaceDataPath();
                Imgcodecs.imwrite(path, lastDetectedFace);

                currentUser.setFaceEncoding(path);
                currentUser.setFaceEnabled(true);
                serviceUser.modifier(currentUser);

                user = currentUser;
                SessionManager.setCurrentUser(currentUser);

                Platform.runLater(() -> {
                    captureFaceButton.setText("Capture Face");
                    captureFaceButton.setDisable(false);
                    updateDisableButtonState();
                    updateStatus("Face enrolled successfully.", "#22aa44");
                });
            } catch (SQLException e) {
                Platform.runLater(() -> {
                    captureFaceButton.setText("Capture Face");
                    captureFaceButton.setDisable(false);
                    updateStatus("Error saving to database: " + e.getMessage(), "#e8314a");
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    captureFaceButton.setText("Capture Face");
                    captureFaceButton.setDisable(false);
                    updateStatus("Error saving face: " + e.getMessage(), "#e8314a");
                });
            }
        }).start();
    }

    private void handleLoginWithFace() {
        if (lastDetectedFace == null || lastDetectedFace.empty()) {
            updateStatus("No valid frame available. Please look at the camera.", "#e8314a");
            return;
        }

        captureFaceButton.setText("Processing...");
        captureFaceButton.setDisable(true);

        new Thread(() -> {
            try {
                if (!faceRecognitionService.isConfigured()) {
                    throw new IOException(faceRecognitionService.getConfigurationError());
                }

                faceRecognitionService.validateFaceImage(lastDetectedFace);
                List<User> usersWithFace = serviceUser.getUsersWithFaceEnabled();
                if (usersWithFace.isEmpty()) {
                    throw new IOException("No users with face recognition enrolled.");
                }

                User matchedUser = null;
                double bestMatch = Double.MAX_VALUE;
                String recognitionError = null;

                for (User candidate : usersWithFace) {
                    String facePath = candidate.getFaceEncoding();
                    if (facePath == null || facePath.isBlank()) {
                        continue;
                    }

                    FaceRecognitionService.MatchResult result =
                            faceRecognitionService.recognizeFace(facePath, lastDetectedFace);

                    if (result.hasError()) {
                        recognitionError = result.errorMessage();
                        continue;
                    }

                    if (result.matched() && result.confidence() < bestMatch) {
                        bestMatch = result.confidence();
                        matchedUser = candidate;
                    }
                }

                User finalMatchedUser = matchedUser;
                String finalRecognitionError = recognitionError;

                Platform.runLater(() -> {
                    captureFaceButton.setText("Sign In");
                    captureFaceButton.setDisable(false);

                    if (finalMatchedUser != null) {
                        SessionManager.setCurrentUser(finalMatchedUser);
                        updateStatus("Face recognized. Signing in as " + finalMatchedUser.getNom() + ".", "#22aa44");
                        stopCamera();
                        openPostLoginScreen(finalMatchedUser);
                    } else if (finalRecognitionError != null) {
                        updateStatus(finalRecognitionError, "#e8314a");
                    } else {
                        updateStatus("Face not recognized. Try again or use email login.", "#e8314a");
                    }
                });
            } catch (Exception e) {
                Platform.runLater(() -> {
                    captureFaceButton.setText("Sign In");
                    captureFaceButton.setDisable(false);
                    updateStatus("Face login failed: " + e.getMessage(), "#e8314a");
                });
            }
        }).start();
    }

    @FXML
    private void handleDisableFaceRecognition(ActionEvent event) {
        User currentUser = user != null ? user : SessionManager.getCurrentUser();

        try {
            Files.deleteIfExists(Paths.get(getFaceDataPath()));

            if (currentUser != null) {
                currentUser.setFaceEnabled(false);
                currentUser.setFaceEncoding(null);
                serviceUser.modifier(currentUser);
                SessionManager.setCurrentUser(currentUser);
                user = currentUser;
            }

            updateDisableButtonState();
            updateStatus("Face recognition has been disabled.", "#888888");
        } catch (Exception e) {
            updateStatus("Error disabling: " + e.getMessage(), "#e8314a");
        }
    }

    @FXML
    private void handleBackToProfile(ActionEvent event) {
        stopCamera();
        if (mode == Mode.LOGIN) {
            goToLogin();
            return;
        }

        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/ProfileUser.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Profile User");
            stage.show();
        } catch (IOException e) {
            e.printStackTrace();
            updateStatus("Unable to return to profile.", "#e8314a");
        }
    }

    private void goToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/Login.fxml"));
            Parent root = loader.load();
            Stage stage = (Stage) cameraView.getScene().getWindow();
            stage.setScene(new Scene(root));
            stage.setTitle("Login");
            stage.show();
        } catch (IOException e) {
            updateStatus("Unable to return to login.", "#e8314a");
        }
    }

    private void openPostLoginScreen(User loggedUser) {
        try {
            Stage stage = (Stage) cameraView.getScene().getWindow();
            if (twoFactorService.requires2FA(loggedUser)) {
                Parent root = FXMLLoader.load(getClass().getResource("/TwoFactorVerify.fxml"));
                stage.setScene(new Scene(root));
                stage.setTitle("Two-Factor Authentication");
                stage.show();
                return;
            }

            String fxml = "admin@gmail.com".equalsIgnoreCase(loggedUser.getEmail()) ? "/main.fxml" : "/home.fxml";
            Parent root = FXMLLoader.load(getClass().getResource(fxml));
            stage.setScene(new Scene(root, 980, 720));
            stage.setTitle("Esports Community");
            stage.show();
        } catch (IOException e) {
            updateStatus("Unable to open the next screen.", "#e8314a");
        }
    }

    private void updateModeUi() {
        if (mode == Mode.LOGIN) {
            titleLabel.setText("Face Recognition Sign In");
            captureFaceButton.setText("Sign In");
            disableFaceButton.setVisible(false);
            disableFaceButton.setManaged(false);
            backLink.setText("Back to Login");
        } else {
            titleLabel.setText("Face Recognition Setup");
            captureFaceButton.setText("Capture Face");
            backLink.setText("Back to Profile");
            updateDisableButtonState();
        }
    }

    private void updateStatus(String message, String color) {
        Platform.runLater(() -> {
            statusLabel.setStyle("-fx-font-size: 13px; -fx-text-fill: " + color + ";");
            statusLabel.setText(message);
        });
    }

    private Image matToImage(Mat frame) {
        MatOfByte buffer = new MatOfByte();
        Imgcodecs.imencode(".png", frame, buffer);
        return new Image(new java.io.ByteArrayInputStream(buffer.toArray()));
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

    private enum Mode {
        ENROLL,
        LOGIN
    }
}
