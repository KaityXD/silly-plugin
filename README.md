# KatsSillyPlugin

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Kotlin](https://img.shields.io/badge/kotlin-1.9.24-blue.svg)](https://kotlinlang.org/)
[![Platform](https://img.shields.io/badge/platform-Folia%20%7C%20Paper-green.svg)](https://papermc.io/)

A high-performance, feature-rich server core designed specifically for modern Minecraft environments (Folia/Paper). Built from the ground up with Kotlin, **KatsSillyPlugin** focuses on modularity, ease of use, and providing a seamless experience for both players and administrators.

---

## 🎯 Project Goals & Progress

Our goal is to create an all-in-one server solution that doesn't feel bloated, but provides all the "missing" features every server needs.

### ✅ What's Done
- **Modern TPA System:** Robust request handling with timeout and safety checks.
- **Unified Group & Permission Management:** A fully integrated, SQLite-backed permission system with support for group inheritance, weights, and live updates.
- **AnvilInput API:** A clean, reusable utility for gathering text input from players via the Anvil GUI—no more messy chat-based inputs.
- **Advanced Moderation Suite:** Kicks, bans, mutes, and freezes, all manageable via an intuitive GUI.
- **Home & Kit Systems:** Multi-home support and dynamic kit creation with cooldowns.
- **Performance-First Architecture:** Designed with Folia's regional threading in mind to ensure zero-lag operations on large servers.

### 🚀 What's Coming (Roadmap)
- **Display Entity Manipulation Library:** We are currently working on a built-in library to make spawning and animating Display Entities (Text, Item, Block) dead simple for other developers and internal modules.
- **Custom Event System:** More hooks for third-party integration.
- **Web-Hook Support:** Optional Discord logging for administrative actions.

---

## ✨ Features at a Glance

- **Rich GUI Menus:** Every system (Home, Kits, Settings, Moderation) is built using a custom, responsive menu framework.
- **Dynamic Chat Formatting:** Full support for MiniMessage (gradients, hex colors) that scales with your permission groups.
- **Persistent Stats Tracking:** Automatic logging of kills, deaths, and playtime into local SQLite.
- **Automatic Module Discovery:** Adding new commands or features is as simple as creating a new class; the plugin handles the rest.

---

## 🛠 Technical Details

- **Language:** [Kotlin](https://kotlinlang.org/)
- **Database:** [SQLite](https://www.sqlite.org/) (Local file-based, no setup required)
- **Minimum Java Version:** 21
- **API:** Folia-API (Compatible with Paper)

---

## 📦 Building and Installation

### Prerequisites
- Java 21 JDK
- Internet connection (to fetch dependencies)

### Build Instructions
```bash
# Clone the repository
git clone https://github.com/KaityXD/KatsSillyPlugin.git
cd KatsSillyPlugin

# Build the shadowed JAR
./gradlew shadowJar
```

### Installation
1. Locate the JAR in `build/libs/KatsSillyPlugin-1.0.2.jar`.
2. Drop it into your server's `plugins` folder.
3. Restart your server.

---

## 🤝 Contributing

Contributions are what make the open-source community such an amazing place to learn, inspire, and create. Any contributions you make are **greatly appreciated**.

1. Fork the Project
2. Create your Feature Branch (`git checkout -b feature/AmazingFeature`)
3. Commit your Changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the Branch (`git origin push feature/AmazingFeature`)
5. Open a Pull Request

---

## 🤖 AI Disclosure

This project utilizes AI-assisted tools for generating boilerplate code, documentation, and specific data structures. While AI was used to accelerate the development workflow, the core architecture, logic flow, and system integration are manually designed and reviewed to ensure high quality and project integrity.

---

## 📄 License

Distributed under the MIT License. See `LICENSE` for more information.

---
*Created by KaityXD — Making server management silly simple.*
