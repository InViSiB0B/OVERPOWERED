# OVERPOWERED

[![Android](https://img.shields.io/badge/Platform-Android%2010%2B-green.svg)](https://developer.android.com)
[![Kotlin](https://img.shields.io/badge/Language-Kotlin-blue.svg)](https://kotlinlang.org)
[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Status](https://img.shields.io/badge/Status-MVP%20Development-orange.svg)](#project-status)

## Introduction

**OVERPOWERED** is a revolutionary gamified daily task management app built specifically for users with ADHD and executive dysfunction. Unlike traditional productivity tools that can feel overwhelming and punitive, OVERPOWERED flips the script by rewarding tasks completed just in time when the brain finally gets the urge to act—and it does this with minimal input required.

## Why OVERPOWERED?

People with ADHD often struggle with traditional productivity tools because they're not designed for neurodivergent brains. To-do lists feel overwhelming, rigid scheduling doesn't work, calendars are tedious to maintain, and the lack of short-term fulfillment kills motivation. 

**OVERPOWERED solves these problems** by creating an engaging, sustainable system that works with ADHD brains rather than against them.

## Features

### MVP

- **Task Creation**: The User can create and complete tasks either short term or long term
- **Clean Daily Interface**: Daily check on in progress and track ongoing tasks
- **Instant Gratification**: The User is awarded for completing tasks with profile customization options and prestige

### ADHD-Specific Features

- **Just-in-Time Motivation**: Rewards the "now or never" motivation style that drives ADHD action
- **Pressure-Based System**: Creates genuine sense of lost opportunity rather than shame-based punishment
- **Focus Mode**: Pause notifications during task work with follow-up completion reminders

### Progression System

- **Experience Points**: Earn XP for every completed task
- **Level Progression**: Unlock new rewards, themes, and customizations as you advance
- **Achievement Gallery**: Collect badges and milestones for various accomplishments
- **Daily Streaks**: Bonus rewards for maintaining consistent completion habits
- **Visual Progress**: Satisfying progress bars and completion animations

## Technologies

### Development Stack

- **Primary Language**: Kotlin
- **Platform**: Android 10+ (API level 30)
- **IDE**: Android Studio
- **Architecture**: MVVM with Android Architecture Components

### Backend & APIs

- **Database**: Firebase Firestore
- **Authentication**: Firebase Auth
- **Push Notifications**: Firebase Cloud Messaging (FCM)
- **Cloud Storage**: Firebase Storage for user data and progress

### Additional Tools

- **Design**: Adobe Photoshop/Illustrator for assets
- **Version Control**: Git with feature-branch workflow
- **Testing**: Android testing frameworks

## Installation

### For End Users

#### System Requirements
- Android device running Android 10 or higher
- Minimum 80MB storage space
- Internet connection for initial setup and sync

#### Installation Steps
1. Download latest `.apk` from the [Releases](../../releases) section
2. Open the app and complete the quick onboarding process
3. Start completing tasks and earning rewards!

### Development Setup

#### Prerequisites
- **Android Studio**: Latest stable version (2023.1.1 or higher)
- **JDK**: OpenJDK 17 or higher
- **Android SDK**: API level 30 (Android 10)
- **Git**: For version control

#### Getting Started

1. **Clone the Repository**
   ```bash
   git clone https://github.com/InViSiB0B/OVERPOWERED.git
   cd overpowered
   ```

2. **Open in Android Studio**
   - Launch Android Studio
   - Select "Open an existing project"
   - Navigate to the cloned repository folder

3. **Build the Project**
   ```bash
   ./gradlew assembleDebug
   ```

4. **Run on Device/Emulator**
   - Connect Android device with USB debugging enabled
   - OR create Android Virtual Device (AVD) with API 30+
   - Click "Run" in Android Studio or use:
   ```bash
   ./gradlew installDebug
   ```

## License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## Contributors

### Development Team
- **Charles Martell**
- **Nicholas Duong** 
- **Colin Eaton**

### Maintainers
This project is currently maintained by the Full Sail University Capstone team listed above.

## Project Status

**Current Status**: MVP Development (Alpha Phase)

---
