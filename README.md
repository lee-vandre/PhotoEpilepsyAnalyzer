# 🎥 PhotoEpilepsyAnalyzer

A Spring Boot REST API that analyzes videos frame-by-frame for photosensitive-epilepsy flash triggers, based on WCAG 2.2 general flash thresholds — and automatically generates a corrected, safe version when violations are found.

![Java](https://img.shields.io/badge/Java-17-orange)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.x-brightgreen)
![JavaCV](https://img.shields.io/badge/JavaCV-FFmpeg%20%2F%20OpenCV-blue)
![WCAG](https://img.shields.io/badge/WCAG-2.2-005A9C)

---

## Overview

Flashing or strobing visuals above a certain intensity and frequency can trigger seizures in people with photosensitive epilepsy. WCAG 2.2 defines the safety thresholds most accessibility standards are built on. **PhotoEpilepsyAnalyzer** applies those thresholds programmatically: it decodes a video, measures perceptual luminance frame-by-frame, flags any one-second window with too many rapid brightness shifts, and — instead of just reporting the problem — rebuilds a corrected video with the offending frames frozen to a safe anchor frame.

It's a full pipeline: **decode → analyze → detect → correct → re-encode**, built on native FFmpeg/OpenCV bindings rather than a pure-Java approximation.

---

## ⚠️ Content Warning

> [!WARNING]
> The clip below is an **unmodified example input** used to demonstrate detection — it contains intense, rapid flashing and is included only to show what the tool flags. If you are photosensitive, please skip it and go straight to the [cleansed output](#after-corrected-output) below.

https://github.com/user-attachments/assets/6a22553e-76e1-4915-8645-e9fac1d0a4db

---

## Demo

### Cleansing Process

| | |
|---|---|
| <img width="1916" alt="Upload interface" src="https://github.com/user-attachments/assets/e0f10c2a-7d15-409d-87d9-f68c3eb89d65" /> | <img width="1919" alt="Scan in progress" src="https://github.com/user-attachments/assets/bd44085a-4ac4-4376-ae8b-2a98c17ab570" /> |
| **1. Upload a video** | **2. Frame-by-frame luminance scan runs** |
| <img width="1911" alt="Safety report with violation timeline" src="https://github.com/user-attachments/assets/d0d6e0b7-a494-48b2-90e5-cc741d93cdc8" /> | <img width="1899" alt="Download corrected video" src="https://github.com/user-attachments/assets/23abd332-0d11-4675-8d58-a04526394a3e" /> |
| **3. Violations reported on a timeline** | **4. Corrected file ready to download** |

### After: Corrected Output

The same clip after frame-freeze correction — flash windows are now locked to a safe anchor frame, with the rest of the video untouched.

https://github.com/user-attachments/assets/8a955ef1-3e91-4a02-b13f-916e311d8bb6

---

## How It Works

**1. Luminance extraction** — Every frame is decoded via FFmpeg and reduced to a single relative luminance value using ITU-R BT.709 perceptual weighting (`0.2126R + 0.7152G + 0.0722B`), producing a chronological brightness profile for the whole video.

**2. Violation detection** — A sliding window equal to one second of frames scans the profile. Within each window, any adjacent-frame brightness delta ≥ a threshold counts as a "shift." More than 3 shifts in a one-second window trips a violation — this mirrors the WCAG 2.2 general flash threshold (content should not flash more than three times in any one-second period).

**3. Frame-freeze correction** — For every violation window, the frame immediately before the flashing begins is captured as a safe anchor. The reassembler then re-encodes the video, substituting that anchor frame across the entire unsafe window while leaving every other frame untouched.

**4. Re-encoding** — The corrected stream is written out via FFmpeg (H.264 video / AAC audio) to a new file, ready for download.

> **Known limitation:** correction currently applies to the video track only — audio passes through unmodified, so a flash paired with a synced sound cue will have its visual fixed but not its audio.

---

## Tech Stack

- **Java 17** / **Spring Boot** — REST API and dependency wiring
- **JavaCV** (FFmpeg + OpenCV native bindings) — frame decoding, pixel-level analysis, and re-encoding
- **Maven** — build and dependency management

---

## API

### `POST /api/videos/analyze`

Multipart upload. Runs the full analyze → correct pipeline and returns a JSON report.

```
curl -F "file=@input.mp4" http://localhost:8080/api/videos/analyze
```

**Response**
```json
{
  "success": true,
  "safe": false,
  "violationTimestamps": [0.0, 1.0, 2.0],
  "message": null,
  "downloadUrl": "/api/videos/download/3f5c5335-022a-4fe5-9548-4694fbd18cae"
}
```

### `GET /api/videos/download/{jobId}`

Streams back the corrected video for a given job, when one was generated.

---

## Project Structure

```
com.example.PhotoEpilepsyAnalyzer
├── VideoController.java          # REST endpoints
├── VideoProcessingService.java   # Orchestrates analyze → correct pipeline
├── VideoAnalyzer.java            # Luminance extraction + WCAG threshold evaluation
├── VideoReassembler.java         # Frame-freeze correction + re-encoding
├── VideoCleanser.java            # Luminance-profile-level correction (non-video path)
├── NativeFrameNormalizer.java    # Low-level pixel delta clamping (OpenCV Mat access)
├── AnalysisReport.java           # safe / violationTimestamps result model
└── AnalysisResponse.java         # Unified API response envelope
```

---

## Getting Started

```bash
git clone https://github.com/<your-username>/PhotoEpilepsyAnalyzer.git
cd PhotoEpilepsyAnalyzer
mvn spring-boot:run
```

Then open `http://localhost:8080` and upload a video.

**Requirements:** Java 17+, Maven. No external FFmpeg install needed — JavaCV bundles the native binaries.

---

## Roadmap

- [ ] Correct audio alongside video for flash windows with synced sound cues
- [ ] Async job processing for large files, with a polling/status endpoint
- [ ] Configurable flash-threshold sensitivity per WCAG's alternate "red flash" criteria

