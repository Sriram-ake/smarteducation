# SnapGrade — On-Device AI Grading for Android (iQOO 15)

SnapGrade is an on-device, phone-first Android application designed for the **iQOO Hackathon (Smart Education Track)**. It enables teachers and students to instantly grade handwritten assignments and short-answer sheets by combining **CameraX capture**, **Google ML Kit on-device handwriting OCR**, and an **on-device grading engine** running locally on the Snapdragon platform.

---

## ⚡ Performance Budget & Demo Benchmarks

| Pipeline Stage | Technology | Typical Latency | Live Demo Budget |
| :--- | :--- | :--- | :--- |
| **Image Capture & Contrast** | CameraX + Bitmap Normalization | ~100 ms | < 250 ms |
| **Handwriting OCR** | Google ML Kit Text Recognition v2 | ~150 – 250 ms | < 500 ms |
| **Rubric Inference & Scoring** | On-Device Snapdragon Engine (LLM / Edge Evaluator) | ~1.5 – 2.2 s (LLM) / ~60 ms (Edge) | < 3,500 ms |
| **Compose UI Rendering** | Jetpack Compose Material 3 | ~40 ms | < 100 ms |
| **Total End-to-End** | Complete Pipeline | **~1.9 – 2.8 seconds** | **< 5.0 seconds** |

---

## 🧠 Architectural Highlights

1. **100% On-Device & Zero Cloud Dependencies:**
   - Inference runs entirely on the device (Snapdragon NPU / Hexagon DSP / Adreno GPU).
   - No external API keys or internet connection required.

2. **Handwriting OCR Engine:**
   - Employs **Google ML Kit Text Recognition v2 (bundled Latin model)**.
   - Includes contrast normalization to enhance faint pencil and pen strokes.
   - Preserves logical text blocks and line grouping.

3. **Dual-Engine On-Device Grading:**
   - **MediaPipe GenAI / LiteRT Hook:** Compatible with quantized small models (such as *Qwen2.5-0.5B-Instruct INT4*, *SmolLM-360M/1.7B INT4*, or *Gemma-2-2B INT4*).
   - **Snapdragon Edge Semantic Evaluator:** Built-in zero-dependency concept and keyword evaluator that guarantees immediate (<60ms) fail-proof scoring during stage pitches.

4. **Review & Finalize UI:**
   - Overall Score gauge with letter grade badge (A+, A, B, etc.).
   - Expandable OCR text view with an **"Edit Raw Text"** button so teachers can fix handwriting misreads and re-grade.
   - Interactive per-criterion scoring steppers (`-` and `+`) and sliders.
   - 1-2 lines of editable feedback per criterion.
   - One-tap "Share / Copy Report" and "Finalize Grade" dialog.

5. **Pre-Loaded Demo Samples:**
   - Includes built-in sample submissions for all 3 rubrics (Photosynthesis, Newton's Third Law, Industrial Revolution) for instantaneous presentation without a printed physical sheet.

---

## 🚀 Building & Running

### Prerequisites
- Android Studio Koala / Ladybug or newer
- JDK 17 (Java 17 LTS)
- Android SDK Platform 34 / 35
- Physical Android device (e.g., iQOO 15 / Snapdragon device) or Android Emulator with Camera support

### Build Commands
```powershell
# Compile and run unit tests
.\gradlew.bat test

# Build debug APK
.\gradlew.bat assembleDebug
```
The resulting APK will be located at:
`app/build/outputs/apk/debug/app-debug.apk`

---

## 📋 Pre-Configured Demo Rubrics

1. **Biology: Photosynthesis Mechanism (10 pts)**
   - *Light-Dependent Reactions* (4 pts): Chlorophyll, water photolysis, ATP/NADPH, O2.
   - *Calvin Cycle / Dark Reaction* (4 pts): Carbon fixation, stroma, glucose synthesis.
   - *Scientific Clarity & Terminology* (2 pts): Coherence and accurate vocabulary.

2. **Physics: Newton's Third Law of Motion (10 pts)**
   - *Law Statement & Symmetry* (4 pts): Action and reaction forces are equal and opposite.
   - *Distinct Bodies* (3 pts): Forces act on two separate objects, not cancelling out.
   - *Real-World Application* (3 pts): Rocket propulsion, swimming, or walking.

3. **History: The Industrial Revolution (10 pts)**
   - *Primary Causes & Innovations* (4 pts): Steam engine, coal reserves, mechanization.
   - *Socio-Economic Impacts* (4 pts): Urbanization, factory labor, class dynamics.
   - *Coherence & Argumentation* (2 pts): Logical prose and causal links.
