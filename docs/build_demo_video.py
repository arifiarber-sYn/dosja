#!/usr/bin/env python3
"""Build the Dosja demo video from Paparazzi renders + neural TTS narration (edge-tts)."""
import json
import pathlib
import subprocess
import sys

SNAP = pathlib.Path("/home/kllosha/dosja/app/src/test/snapshots/images")
OUT = pathlib.Path("/home/kllosha/dosja/docs/demo")
OUT.mkdir(parents=True, exist_ok=True)
WORK = pathlib.Path("/tmp/dosja-video")
WORK.mkdir(exist_ok=True)

FONT_B = "/usr/share/fonts/google-noto/NotoSans-Bold.ttf"
FONT_R = "/usr/share/fonts/google-noto/NotoSans-Regular.ttf"
BG = "0x0E1116"
W, H = 1920, 1080

SCENES = [
    dict(
        key="title",
        headline="Dosja",
        bullets=[
            "Private legal document work on your phone.",
            "On-device Gemini Nano first, cloud only when you allow it.",
            "",
            "Google #BuildHybridAI 2026  ·  Android",
        ],
        phone=None,
        narration=(
            "A client's case file is the most confidential document a lawyer holds. "
            "Dosja is an Android assistant that summarises it on the phone itself, with Gemini Nano, "
            "and only uses the cloud when a human says so."
        ),
    ),
    dict(
        key="list",
        headline="On-device first",
        bullets=[
            "Gemini Nano through ML Kit GenAI runs on the phone.",
            "Short documents never touch the network.",
            "Nothing leaves the device by default.",
        ],
        phone="al.dosja.ui_ScreensTest_document_list.png",
        narration=(
            "Every document starts on the device. Summaries and clause extraction run in Gemini Nano "
            "through ML Kit GenAI, with no network call at all."
        ),
    ),
    dict(
        key="ondevice",
        headline="Every result is labelled",
        bullets=[
            "ON-DEVICE, CLOUD or DEMO on each card.",
            "The reader never has to guess where the text went.",
        ],
        phone="al.dosja.ui_ScreensTest_on_device_result.png",
        narration=(
            "Each result is labelled by where it came from. A lawyer never has to guess "
            "whether a summary left the phone."
        ),
    ),
    dict(
        key="consent",
        headline="Long documents stop and ask",
        bullets=[
            "The on-device model declines: nothing is escalated silently.",
            "The dialog states the size and the destination.",
            "\u201cKeep on device\u201d is always available.",
        ],
        phone="al.dosja.ui_ScreensTest_consent_dialog.png",
        narration=(
            "When the on-device model declines, or the file is longer than the threshold, the app does not "
            "quietly escalate. It stops, shows the size and the destination, and asks."
        ),
    ),
    dict(
        key="cloud",
        headline="Cloud only with consent",
        bullets=[
            "Firebase AI Logic  ·  Gemini",
            "App Check gates every cloud call.",
            "Remote Config sets the threshold once per office.",
        ],
        phone="al.dosja.ui_ScreensTest_cloud_result.png",
        narration=(
            "Only after consent does Firebase AI Logic answer, gated by App Check, with the routing "
            "threshold supplied by Remote Config so an office configures twenty phones once."
        ),
    ),
    dict(
        key="closing",
        headline="github.com/arifiarber-sYn/dosja",
        bullets=[
            "MIT licensed  ·  6 unit tests on the routing policy",
            "Kotlin  ·  Jetpack Compose  ·  ML Kit GenAI  ·  Firebase AI Logic",
            "",
            "Screens rendered from the app's Compose layout (Paparazzi, JVM).",
        ],
        phone=None,
        narration=(
            "The routing policy is unit tested, the code is MIT licensed, and the repository is public. "
            "Dosja: the file does not leave the phone unless you say so."
        ),
    ),
]


VOICE = "en-US-BrianNeural"


def synth(text, out_path):
    """Neural TTS through edge-tts (Microsoft Read Aloud voices, free endpoints)."""
    import asyncio
    import edge_tts

    async def _go():
        comm = edge_tts.Communicate(text, VOICE, rate="+4%")
        await comm.save(str(out_path))

    asyncio.run(_go())


def run(cmd):
    p = subprocess.run(cmd, capture_output=True, text=True)
    if p.returncode != 0:
        print("FAILED:", " ".join(cmd)[:300])
        print(p.stderr[-2500:])
        sys.exit(1)
    return p.stdout


def dur_of(path):
    out = run(["ffprobe", "-v", "error", "-show_entries", "format=duration", "-of", "json", str(path)])
    return float(json.loads(out)["format"]["duration"])


def esc(text):
    return text.replace("'", "\u2019")


scene_files = []
for i, sc in enumerate(SCENES):
    wav = WORK / f"{i:02d}.mp3"
    synth(sc["narration"], wav)
    dur = dur_of(wav) + 0.7

    # text files for drawtext (avoids shell escaping)
    for name, body in (("head", sc["headline"]), ("body", "\n".join(sc["bullets"]))):
        (WORK / f"{i:02d}-{name}.txt").write_text(esc(body), encoding="utf-8")

    inputs = ["-f", "lavfi", "-i", f"color=c={BG}:s={W}x{H}:r=30:d={dur:.2f}", "-i", str(wav)]
    fc = []
    if sc["phone"]:
        inputs += ["-i", str(SNAP / sc["phone"])]
        fc.append("[2:v]scale=-2:940[p]")
        fc.append("[0:v][p]overlay=1395:70[v0]")
        base = "[v0]"
    else:
        base = "[0:v]"

    head_size = 78 if sc["key"] == "title" else 56
    fc.append(
        f"{base}drawtext=fontfile={FONT_B}:textfile={WORK}/{i:02d}-head.txt:"
        f"fontcolor=white:fontsize={head_size}:x=100:y=250:line_spacing=14[v1]"
    )
    fc.append(
        f"[v1]drawtext=fontfile={FONT_R}:textfile={WORK}/{i:02d}-body.txt:"
        f"fontcolor=0xD5DAE3:fontsize=32:x=104:y={250 + head_size + 60}:line_spacing=18[v]"
    )

    out = WORK / f"{i:02d}.mp4"
    run([
        "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
        *inputs,
        "-filter_complex", ";".join(fc),
        "-map", "[v]", "-map", "1:a",
        "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-pix_fmt", "yuv420p",
        "-c:a", "aac", "-b:a", "128k", "-ar", "44100", "-ac", "2",
        "-t", f"{dur:.2f}", str(out),
    ])
    scene_files.append(out)
    print(f"scene {i} ok ({dur:.1f}s)")

lst = WORK / "list.txt"
lst.write_text("".join(f"file '{p}'\n" for p in scene_files), encoding="utf-8")
final = OUT / "dosja-demo.mp4"
run([
    "ffmpeg", "-y", "-hide_banner", "-loglevel", "error",
    "-f", "concat", "-safe", "0", "-i", str(lst),
    "-c:v", "libx264", "-preset", "medium", "-crf", "20", "-pix_fmt", "yuv420p",
    "-c:a", "aac", "-b:a", "128k",
    "-movflags", "+faststart",
    str(final),
])
print("done:", final, f"{dur_of(final):.1f}s", f"{final.stat().st_size/1e6:.1f} MB")
