# Voice Command Mode Orb Fix & Visual Upgrade

## Summary
Diagnose and fix the Voice Command Mode Orb animation in AU Notes so it renders reliably with vibrant particle motion, audio reactivity, and proper layout spacing without being obscured by overlapping UI elements.

---

## User Review & Critical Decisions

> [!IMPORTANT]
> The primary reason the orb was not rendering is that Android WebView restricts file URL access by default (`allowFileAccessFromFileURLs = false`), which prevents `file:///android_asset/orb.html` from loading `three.min.js`. Furthermore, WebGL context creation can silently fail on emulator environments. 
> 
> We are implementing a two-pronged solution:
> 1. **Fix WebView WebGL Orb Settings:** Configure WebView with explicit asset loading permissions, WebGL hardware acceleration, and error handling.
> 2. **Native Compose Dynamic 3D Particle Orb Fallback:** Implement a 100% reliable, zero-lag, native Jetpack Compose 3D Particle & Glow Orb that animates smoothly at 60 FPS across all devices and emulators, reacting to microphone input and TTS speech in real time.
> 3. **Layout Polish:** Elevate the Orb as the central visual hero in `CommandModeScreen` while repositioning the microphone controls cleanly below the orb.

---

## 1. Overview & Core Concept
* **What It Does**: Provides a fluid, glowing, futuristic JARVIS-style AI Voice Orb in Command Mode that visually reacts to:
  * `idle`: Subtle breathing glow and gentle orbital ring rotation.
  * `listening`: Expanded reactive particle aura that pulses directly with live microphone amplitude (`micLevel`).
  * `thinking`: Rapid orbital ring acceleration and swirling particle galaxy.
  * `speaking`: Rhythmic audio-wave contractions matching Text-to-Speech output.
* **Key Value**: Restores the signature aesthetic of the voice assistant and makes voice interactions intuitive, satisfying, and modern.

---

## 2. User Experience & Visual Design

### Key User Flows
1. **Entry**: User taps "Command Mode" in the sidebar. The screen opens with a glowing, floating 3D particle sphere in the center.
2. **Listening**: Tapping the Mic button or saying a command expands the orb with vibrant crimson/cyan ripples scaling to voice loudness.
3. **Processing**: While executing a command or verifying a PIN, the orb shifts to an energetic thinking spin.
4. **Voice Response**: While AU Voice speaks back, the orb pulses with speech cadence.

### Visual Architecture
* **Color Palette**: Cyberpunk Neon / Crimson glassmorphism matching the AU Notes theme (`#FF2D55`, `#7C3AED`, `#00F0FF`, deep midnight backdrop).
* **Depth & Particle Density**: Concentric rings, glowing radial gradient backdrop, and multi-layered particle shells.

---

## 3. Key Product Decisions & Trade-Offs

* **Dual Engine (Native Compose + WebView WebGL)**:
  * *Chosen Approach*: Fix WebView asset permissions in `OrbView.kt` (`allowFileAccessFromFileURLs`, `allowUniversalAccessFromFileURLs`) while adding a native Compose Canvas Particle Orb engine that is active and guaranteed to work even if WebGL is disabled or unavailable.
  * *Why*: Guarantees that the orb *never* shows a blank void or crashes on any device/emulator, while maintaining 60 FPS performance and zero battery drain.
* **Unobstructed Layout**:
  * *Chosen Approach*: Move the mic button below the Orb container rather than layering an opaque 110dp circle directly over the center of the orb.
  * *Why*: Allows the user to see the beautiful particle core and internal dynamics clearly.

---

## 4. Technical Architecture & System Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    CommandModeScreen                        │
├──────────────────────────────┬──────────────────────────────┤
│  Top Bar (Back + Title + FX) │  Live Audio State & Logs     │
├──────────────────────────────┴──────────────────────────────┤
│                                                             │
│                Hero Voice Orb (240dp Height)                │
│   ┌─────────────────────────────────────────────────────┐   │
│   │             Native Compose 3D Particle Orb          │   │
│   │   • Outer Halo Radial Glow (Shader/Canvas)          │   │
│   │   • Multi-Layered Rotating Orbital Rings            │   │
│   │   • Floating 3D Particle Cloud with Sine-waves      │   │
│   │   • Live Mic Amplitude Modulation (0.0 to 1.0)      │   │
│   └─────────────────────────────────────────────────────┘   │
│                                                             │
│            Floating Mic Action & State Pill                 │
│         [ 🎙️ Tap to Speak / Listening Pulse ]               │
│                                                             │
│              Recent Voice Command Logs (LazyColumn)         │
│                                                             │
└─────────────────────────────────────────────────────────────┘
```

### Steps to Execute
1. **Update `OrbView.kt`**:
   - Add WebView settings: `allowFileAccessFromFileURLs = true`, `allowUniversalAccessFromFileURLs = true`, WebGL flags, and console message debugging.
   - Introduce `NativeParticleOrb` Composable utilizing Compose `Canvas`, `rememberInfiniteTransition`, and trigonometry-based 3D particle projection reacting to `level()` and `state`.
   - Seamlessly display the native orb to guarantee 100% visual uptime.
2. **Refactor `CommandModeScreen.kt` Layout**:
   - Separate the Orb container from the Mic button so the orb is unobstructed.
   - Integrate speech level and mic level into the state transition for realistic breathing and audio reactivity.
3. **Verify Build**:
   - Run `compile_applet` to confirm clean compilation.
