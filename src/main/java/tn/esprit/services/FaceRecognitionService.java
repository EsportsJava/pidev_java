package tn.esprit.services;

import org.json.JSONObject;
import org.opencv.core.Mat;
import org.opencv.core.MatOfByte;
import org.opencv.imgcodecs.Imgcodecs;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

public class FaceRecognitionService {

    private static final String PYTHON_COMMAND = "python";
    private static final String SCRIPT_PATH = "scripts/deepface_verify.py";
    private static final long PROCESS_TIMEOUT_SECONDS = 120;

    public boolean isConfigured() {
        return runHealthCheck().ok();
    }

    public String getConfigurationError() {
        ProcessResult result = runHealthCheck();
        if (result.ok()) {
            return "";
        }
        return result.message();
    }

    public MatchResult recognizeFace(String enrolledFacePath, Mat currentFace) {
        try {
            if (enrolledFacePath == null || enrolledFacePath.isBlank()) {
                return MatchResult.error("No enrolled face image found for this user.");
            }

            Path enrolledPath = Path.of(enrolledFacePath);
            if (!Files.exists(enrolledPath)) {
                return MatchResult.error("Enrolled face image is missing: " + enrolledFacePath);
            }

            Path capturedImage = writeTempImage(currentFace, "deepface-captured-");
            try {
                ProcessResult result = runPythonCommand("verify", enrolledPath.toString(), capturedImage.toString());
                if (!result.ok()) {
                    return MatchResult.error(result.message());
                }

                JSONObject payload = new JSONObject(extractJsonPayload(result.message()));
                return MatchResult.success(
                        payload.optBoolean("verified", false),
                        payload.optDouble("distance", 0.0)
                );
            } finally {
                Files.deleteIfExists(capturedImage);
            }
        } catch (Exception e) {
            return MatchResult.error("DeepFace error: " + e.getMessage());
        }
    }

    public void validateFaceImage(Mat faceImage) throws IOException {
        Path capturedImage = writeTempImage(faceImage, "deepface-validate-");
        try {
            ProcessResult result = runPythonCommand("validate", capturedImage.toString());
            if (!result.ok()) {
                throw new IOException(result.message());
            }
        } finally {
            Files.deleteIfExists(capturedImage);
        }
    }

    private ProcessResult runHealthCheck() {
        return runPythonCommand("health");
    }

    private ProcessResult runPythonCommand(String... args) {
        try {
            List<String> command = new ArrayList<>();
            command.add(PYTHON_COMMAND);
            command.add(SCRIPT_PATH);
            for (String arg : args) {
                command.add(arg);
            }

            ProcessBuilder processBuilder = new ProcessBuilder(command);
            processBuilder.redirectErrorStream(true);
            processBuilder.environment().put("PYTHONUTF8", "1");
            processBuilder.environment().put("PYTHONIOENCODING", "utf-8");

            Process process = processBuilder.start();
            boolean finished = process.waitFor(PROCESS_TIMEOUT_SECONDS, TimeUnit.SECONDS);
            if (!finished) {
                process.destroyForcibly();
                return new ProcessResult(false, "DeepFace process timed out.");
            }

            String output = readAll(process);
            if (process.exitValue() != 0) {
                return new ProcessResult(false, normalizeMessage(output));
            }

            return new ProcessResult(true, output.trim());
        } catch (IOException e) {
            return new ProcessResult(false, "Unable to start Python DeepFace script: " + e.getMessage());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return new ProcessResult(false, "DeepFace process was interrupted.");
        }
    }

    private String readAll(Process process) throws IOException {
        StringBuilder builder = new StringBuilder();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(process.getInputStream(), StandardCharsets.UTF_8))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (!builder.isEmpty()) {
                    builder.append(System.lineSeparator());
                }
                builder.append(line);
            }
        }
        return builder.toString();
    }

    private String normalizeMessage(String output) {
        String trimmed = output == null ? "" : output.trim();
        if (trimmed.isEmpty()) {
            return "DeepFace command failed with no output.";
        }
        return trimmed;
    }

    private String extractJsonPayload(String output) throws IOException {
        String trimmed = normalizeMessage(output);
        int start = trimmed.lastIndexOf('{');
        int end = trimmed.lastIndexOf('}');
        if (start >= 0 && end > start) {
            return trimmed.substring(start, end + 1);
        }
        throw new IOException("DeepFace verify did not return JSON: " + trimmed);
    }

    private Path writeTempImage(Mat image, String prefix) throws IOException {
        if (image == null || image.empty()) {
            throw new IOException("No valid face image available.");
        }

        Path tempFile = Files.createTempFile(prefix, ".jpg");
        MatOfByte buffer = new MatOfByte();
        boolean encoded = Imgcodecs.imencode(".jpg", image, buffer);
        if (!encoded) {
            Files.deleteIfExists(tempFile);
            throw new IOException("Unable to encode captured image.");
        }

        Files.write(tempFile, buffer.toArray());
        return tempFile;
    }

    private record ProcessResult(boolean ok, String message) {}

    public record MatchResult(boolean matched, double confidence, String errorMessage) {
        public static MatchResult success(boolean matched, double confidence) {
            return new MatchResult(matched, confidence, null);
        }

        public static MatchResult error(String errorMessage) {
            return new MatchResult(false, 0.0, errorMessage);
        }

        public boolean hasError() {
            return errorMessage != null && !errorMessage.isBlank();
        }
    }
}
