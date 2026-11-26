"""Prepare clearly named Xpersiva assets for the Expo app."""

from pathlib import Path
from shutil import copyfile

from PIL import Image


ROOT = Path(__file__).resolve().parent
SOURCE = ROOT / "v2-portal"
DESTINATION = ROOT / "app-ready"

FILES = [
    ("xpersiva-portal-app-icon.png", "icon.png"),
    ("xpersiva-portal-android-background.png", "android-icon-background.png"),
    ("xpersiva-portal-android-foreground.png", "android-icon-foreground.png"),
    ("xpersiva-portal-android-monochrome.png", "android-icon-monochrome.png"),
    ("xpersiva-portal-wordmark-light.png", "logo-header.png"),
    ("xpersiva-portal-startup-symbol.png", "startup-symbol.png"),
    ("xpersiva-portal-startup-symbol.png", "splash-icon.png"),
    ("xpersiva-intro-para-app-suave.mp4", "startup-intro.mp4"),
]


def main() -> None:
    DESTINATION.mkdir(exist_ok=True)
    for original, final in FILES:
        copyfile(SOURCE / original, DESTINATION / final)

    # Match the existing 48 x 48 favicon asset in the Expo project.
    with Image.open(DESTINATION / "icon.png") as icon:
        icon.resize((48, 48), Image.Resampling.LANCZOS).save(DESTINATION / "favicon.png")

    print(f"Prepared {len(FILES) + 1} files in {DESTINATION}")


if __name__ == "__main__":
    main()
