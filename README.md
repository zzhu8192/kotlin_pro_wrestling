# Pro Wrestling (Kotlin / libGDX)

[![Kotlin 2.4.21](https://img.shields.io/badge/Kotlin-2.4.21-7F52FF.svg?logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![libGDX 1.13.5](https://img.shields.io/badge/libGDX-1.13.5-E10098.svg)](https://libgdx.com/)
[![Java 21 LTS](https://img.shields.io/badge/Java-21%20LTS-ED8B00.svg?logo=openjdk&logoColor=white)](https://openjdk.org/)
[![Maven 3.9+](https://img.shields.io/badge/Maven-3.9+-C71A36.svg?logo=apachemaven&logoColor=white)](https://maven.apache.org/)
[![License: Apache 2.0](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](LICENSE)

An open-source 2D pro wrestling game engine and homage inspired by the timeless mechanics of the classic 1987 8-bit NES title *Pro Wrestling*, built from the ground up in **Kotlin 2.4** and **libGDX 1.13.5** running on **Java 21 LTS**.

Vibe-coded over several iterations as a creative sandbox exploring retro game architecture, finite state machines, animation frame interpolation, collision physics, and responsive fighting game grapple mechanics.

---

> [!NOTE] Legal Notice & Disclaimer
> This project is an independent, non-commercial open-source educational game and fan homage.
> - **Zero Copyrighted Assets**: This repository contains **no** proprietary ROM images, raw NES tile bank dumps, or audio assets from Nintendo Co., Ltd. Visuals utilize independently drawn high-resolution sprite sheets and procedural vector geometry, while music consists of public-domain classical arrangements and custom synthesized sound effects.
> - **Trademark & Copyright**: *Pro Wrestling* is a registered trademark of Nintendo Co., Ltd. This project is not affiliated with, endorsed by, sponsored by, or connected to Nintendo Co., Ltd. All game code, state machines, and physics in this repository are original implementations.

---

## Highlights

- **Accurate 8-Bit Grapple Mechanics & Rules**:
  - **Collar-and-Elbow Lockup**: Walking into an opponent triggers a lockup. Timing your input to the canvas foot-stomp cue awards grapple initiative!
  - **Dynamic Damage & Fatigue**: Attacks deal damage and stun based on a comprehensive damage matrix ([`damage_table.json`](file:///home/zzhu/homerepos/kotlin_pro_wrestling/src/main/resources/assets/data/damage_table.json)). High-impact moves (Brainbusters, Piledrivers, Backbreakers) succeed once the opponent is sufficiently weakened.
  - **Rope Elasticity & Irish Whips**: Double-tap Left/Right to run into the ring ropes and rebound with momentum for flying attacks. Whip opponents across the ring to intercept them on the rebound.
  - **Referee AI & 3-Count Pinfalls**: Active referee moves into position, drops to the mat, and slaps the canvas for counts 1, 2, and 3. Button mashing allows weakened wrestlers a fighting chance to kick out.
  - **20-Count Ring-Out**: Brawl outside the ring on the arena floor, but re-enter before the referee's audible 20-count or forfeit the match.
  - **Turnbuckle Climbing & Diving**: Scale top corner posts and launch high-flying body splashes onto standing or downed opponents.
- **Engine Architecture & Visuals**:
  - **Multi-layer Rendering**: Procedural vector ring geometry, depth-sorted ropes, and dynamic crowd flashbulbs.
  - **Sub-frame Animation Interpolation**: Smooth animation transitions ([`FrameInterpolator.kt`](file:///home/zzhu/homerepos/kotlin_pro_wrestling/src/main/kotlin/prowrestling/animation/FrameInterpolator.kt)) with procedural fallback if textures are unavailable.
  - **Resolution Independence**: 4:3 presentation using libGDX `FitViewport` (480×360 virtual resolution) scaling cleanly to 1080p, 1440p, or 4K.
- **Audio Engine**:
  - Public-domain classical MIDI arrangements (Bach, Vivaldi) and retro impact sound effects.

---

## Character Roster

| Wrestler | Country | Archetype | Signature Move & Input |
|---|---|---|---|
| **Fighter Hayabusa** | Japan | Balanced Striker | Back Brain Kick (*Enzuigiri*) — Standing A behind or Grapple Down + A |
| **Star Man** | Mexico | High-Flying Cruiserweight | Somersault Kick / Flying Cross Chop — Grapple Down + A / Running + B |
| **Kin Corn Karn** | Korea | Martial Arts Master | Mongolian Chop / Reverse Karate Kick — Grapple Down + A |
| **Giant Panther** | USA | Powerhouse Brawler | Iron Claw / Headbutt — Grapple Down + A |
| **The Amazon** | Unknown | Hardcore Heel | Piranha Bite (Biting attack) / Illegal Choke — Grapple Down + A |
| **King Slender** | USA | Technical Heavyweight | Backbreaker (*Slender Bender*) — Grapple Down + A |
| **The Great Puma** | Unknown | Final Boss / Champion | Master Execution — Capable of performing all signature techniques |

---

## Controls

### Keyboard Layout

| NES Button | Default Key | In-Ring Function |
|---|---|---|
| **D-Pad Up / Down / Left / Right** | **Arrow Keys** | Walk / Position / Climb Turnbuckles |
| **Double Tap Left / Right** | **Arrow Keys (Quick 2x)** | Dash & Run into Ropes |
| **B Button** | **Z** | Punch / Body Slam (in grapple) / Irish Whip / Ground Stomp |
| **A Button** | **X** | Kick / Vertical Suplex (in grapple) / Piledriver / Cover & Pin |
| **Select** | **A** | Toggle Menu Options |
| **Start** | **S** or **Enter** | Confirm / Start Match / Pause |

### Move Execution List

- **Striking:**
  - `Z`: Quick jab punch (light stun).
  - `X`: Mid-range thrust kick (pushes opponent back).
  - `Running + Z`: Flying body press / clothesline.
  - `Running + X`: Running dropkick.
- **Grapple / Lockup (Walk directly into opponent):**
  - `Z` (at stomp cue): Body Slam.
  - `Left/Right + Z`: Irish Whip (propels opponent into opposite ropes).
  - `X` / `Up + X`: Brainbuster Vertical Suplex.
  - `Left/Right + X`: Belly-to-Back Suplex.
  - `Down + X`: Piledriver / Character Signature Special.
- **Downed Opponent:**
  - Stand over body + `X`: Pin attempt (Cover for 3-count).
  - Stand over body + `Z`: Ground stomp / knee drop.
- **Turnbuckles:**
  - Walk into top-left or top-right post + `Up`: Climb turnbuckle.
  - On turnbuckle + `Z` or `X`: Flying body splash dive.

---

## Project Structure

```text
kotlin_pro_wrestling/
├── src/
│   ├── main/
│   │   ├── kotlin/prowrestling/
│   │   │   ├── DesktopLauncher.kt      # Desktop entry point (libGDX Lwjgl3)
│   │   │   ├── ProWrestlingGame.kt     # Main game lifecycle manager
│   │   │   ├── ai/                     # CPU controllers & difficulty tiers
│   │   │   ├── animation/              # Animation player & sub-frame interpolator
│   │   │   ├── audio/                  # MIDI music & sound effect players
│   │   │   ├── core/                   # Game state, wrestler entities, physics & rules
│   │   │   ├── input/                  # Input action mappers & double-tap dash detector
│   │   │   ├── render/                 # Wrestler, referee, ring, crowd & HUD renderers
│   │   │   └── screen/                 # Title, character select, match, tournament screens
│   │   └── resources/assets/
│   │       ├── audio/                  # MIDI soundtracks and 16-bit WAV SFX
│   │       ├── data/                   # Wrestlers, moves, damage tables, animations JSON
│   │       └── sprites/atlas/          # High-resolution wrestler and referee sprite sheets
│   └── test/kotlin/prowrestling/
│       └── CoreLogicTest.kt            # Unit test suite for match rules, damage, and grapples
├── pom.xml                             # Maven build configuration (Kotlin 2.4, Java 21, libGDX)
└── README.md
```

---

## Building and Running

### Prerequisites
- **JDK 21+** (LTS recommended)
- **Maven 3.9+**

### Run directly with Maven
```bash
mvn compile exec:java
```

### Run unit tests
```bash
mvn test
```

### Build self-contained executable JAR
```bash
mvn package -DskipTests
java -jar target/pro-wrestling.jar
```

---

## License

This project is licensed under the [Apache License, Version 2.0](LICENSE).
