import json
import sys

if hasattr(sys.stdout, "reconfigure"):
    sys.stdout.reconfigure(encoding="utf-8", errors="replace")
if hasattr(sys.stderr, "reconfigure"):
    sys.stderr.reconfigure(encoding="utf-8", errors="replace")


def fail(message: str, code: int = 1) -> None:
    print(message)
    sys.exit(code)


try:
    from deepface import DeepFace
except Exception as exc:
    fail(
        "DeepFace is not installed. Run: python -m pip install deepface opencv-python. "
        f"Details: {exc}"
    )


def health() -> None:
    print("ok")


def validate(image_path: str) -> None:
    faces = DeepFace.extract_faces(
        img_path=image_path,
        detector_backend="opencv",
        enforce_detection=True,
    )
    if len(faces) == 0:
        fail("No face detected in captured image.")
    if len(faces) > 1:
        fail("Multiple faces detected in captured image. Use an image with exactly one face.")
    print("ok")


def verify(enrolled_path: str, captured_path: str) -> None:
    result = DeepFace.verify(
        img1_path=enrolled_path,
        img2_path=captured_path,
        model_name="VGG-Face",
        detector_backend="opencv",
        enforce_detection=True,
    )
    print(
        json.dumps(
            {
                "verified": bool(result.get("verified", False)),
                "distance": float(result.get("distance", 0.0)),
                "threshold": float(result.get("threshold", 0.0)),
                "model": result.get("model", "VGG-Face"),
            }
        )
    )


def main() -> None:
    if len(sys.argv) < 2:
        fail("Usage: python scripts/deepface_verify.py <health|validate|verify> [args]")

    command = sys.argv[1]

    try:
        if command == "health":
            health()
            return
        if command == "validate":
            if len(sys.argv) != 3:
                fail("Usage: python scripts/deepface_verify.py validate <image_path>")
            validate(sys.argv[2])
            return
        if command == "verify":
            if len(sys.argv) != 4:
                fail("Usage: python scripts/deepface_verify.py verify <enrolled_path> <captured_path>")
            verify(sys.argv[2], sys.argv[3])
            return
        fail(f"Unknown command: {command}")
    except Exception as exc:
        fail(f"DeepFace processing failed: {exc}")


if __name__ == "__main__":
    main()
