# SPARC-Notify

**SPARC-Notify** is a Kotlin-based Android application that helps caretakers receive emergency and other important notifications from patients. It is designed for use in care environments—hospitals, assisted living, or home care—where quick, reliable communication is critical.

Caretakers can subscribe to specific patient codes and receive notifications for different types of needs (food, doctor call, restroom, emergency). The app delivers instant, high-priority notifications using Firebase Cloud Messaging (FCM).

---

## Features

- **Patient Code Subscription**: Add, remove, and manage patient codes (e.g., a five-character alphanumeric ID per patient).
- **Notification Types**: Select which notification types you want to receive for each patient code:
  - **FOOD**: Patient requests food.
  - **DOCTOR_CALL**: Patient needs medical attention.
  - **RESTROOM**: Patient requests restroom assistance.
  - **EMERGENCY**: Emergency/urgent notification.
- **Custom Notification Filtering**: Receive only the notification types you select for each code.
- **High-Priority Emergency Alerts**: Emergency notifications use a unique sound and channel for immediate attention.
- **Local Persistence**: Subscriptions and preferences are saved locally on your device.
- **Modern Android Permissions**: Requests notification permissions automatically on Android 13+.
- **Secure and Private**: No patient data beyond non-identifying codes is stored or transmitted.

---

## Getting Started

### Prerequisites

- Android device (API 21+ recommended; notification permissions are handled for Android 13+).
- Firebase Cloud Messaging (FCM) enabled on the backend.
- [Kotlin](https://kotlinlang.org/) (the app is written entirely in Kotlin).

### Installation

1. **Clone the repository**:
   ```bash
   git clone https://github.com/FrankTheSssnake/SPARC-Notify.git
   ```

2. **Open in Android Studio**:
   - Select `Open an existing Android Studio project`, and choose the `SPARC-Notify` directory.

3. **Build and Run**:
   - Connect your Android device or use the emulator.
   - Click "Run" in Android Studio.

---

## Usage

### Managing Patient Codes

- Tap the "+" Floating Action Button to add a new patient code.
- Patient codes must be exactly 5 letters or digits (A-Z, a-z, 0-9).
- You can remove a code at any time using the "Remove" button.
- Tap the type selector to choose which notification types (FOOD, DOCTOR CALL, RESTROOM, EMERGENCY) you wish to receive for each code.

### Receiving Notifications

- When a subscribed patient triggers a notification, you receive an alert via FCM.
- Emergency notifications appear with high priority, vibration, special sound, and visibility on the lock screen.
- Tapping a notification opens the app.

### Notification Filtering

- You only get notifications for the types you have enabled per code.
- All preferences are stored locally, so notifications are filtered before being shown.

---

## Core Architecture

- **MainActivity**: Handles patient code management, subscription, permissions, and UI.
- **MyFirebaseMessagingService**: Listens for FCM messages, filters them, and displays notifications.
- **Data Storage**: Uses `SharedPreferences` to persist patient code subscriptions and selected types as a JSON structure.

---

## Notification Channels

- Two channels: `emergency_channel` and `default_channel`.
- Emergency notifications use a unique sound (`R.raw.emergency`).
- All notifications use high priority and are visible on the lock screen.

---

## License

This project is licensed under the GNU General Public License v3.0. See the [LICENSE](LICENSE) file for details.

---

## Author

FrankTheSssnake
