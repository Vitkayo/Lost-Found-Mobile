# Campus Found Mobile

Campus Found Mobile is a native Android lost-and-found app for campus use. Students can browse lost and found items as guests, search and filter listings, sign in, report items, manage their posts, and keep basic profile information in sync with a demo backend.

This repository contains the Android mobile app only. It is separate from the Campus Found Laravel website and is presented as a student portfolio / demo project.

## Screenshots

| Login | Home | Report | Profile |
| --- | --- | --- | --- |
| ![Login screen](screenshots/login.png) | ![Home screen](screenshots/home.png) | ![Report screen](screenshots/report.png) | ![Profile screen](screenshots/profile.png) |

## Highlights

- Guest browsing with search, filtering, pull-to-refresh, and item detail pages.
- Email or phone login, registration, password reset, profile editing, and logout.
- Lost/found item reporting with category, status, location, contact details, and optional photos.
- Profile dashboard with user stats, owned posts, edit support, and delete support.
- Room cache fallback so previously loaded items remain available when the network fails.
- Light and dark theme support.
- Optional Firebase Storage image upload with a base64 fallback for demo use.

## Tech Stack

| Layer | Tools |
| --- | --- |
| Language | Kotlin |
| UI | Android Views, Material Components, View Binding, Navigation Component |
| Architecture | MVVM, Repository pattern, Hilt dependency injection |
| Networking | Retrofit, Gson, MockAPI |
| Local storage | Room |
| Async | Kotlin Coroutines, Flow / StateFlow |
| Images | Glide, Firebase Storage optional |
| Testing | Gradle unit test task, Espresso instrumented tests, Room instrumentation tests |

## Architecture

```text
Activities / Fragments
        ↓
ViewModels
        ↓
Repositories
        ↓
Retrofit / MockAPI  +  Room cache
```

Key implementation notes:

- Guest users can browse Home and item details.
- Report and Profile are protected by a login guard in `MainActivity`.
- `ItemRepository` syncs remote items from MockAPI and stores them in Room for offline fallback.
- Auth, registration, forgot password, and profile edits are backed by MockAPI `/user`.
- Photo uploads use Firebase Storage when `app/google-services.json` is configured; otherwise the app uses a demo-friendly base64 fallback.

## Demo Flow

1. Open the app and continue as a guest.
2. Browse Home, search, filter by status/category, and open an item detail page.
3. Tap Report or Profile to see the login-required flow.
4. Register a new account or sign in with a demo account.
5. Use Forgot Password on the login screen to reset a MockAPI demo account.
6. Report a lost or found item with a location and contact method.
7. Refresh Home, open Profile, review your posts, then logout.

## Demo Accounts

| Email | Password |
| --- | --- |
| `vit@gmail.com` | `12345678` |
| `demo@test.com` | `123456` |
| `demo@gmail.com` | `123456` |

You can also register a new account from the login screen.

## Getting Started

### Requirements

- Android Studio Ladybug or newer
- JDK 11+
- Android SDK 35
- Android device or emulator with API 28+

### Setup

```bash
git clone https://github.com/bundavit/Campus-Found-Mobile.git
cd Campus-Found-Mobile
```

Create a local SDK config:

```bash
cp local.properties.example local.properties
```

Then update `local.properties` with your Android SDK path.

Firebase is optional for the demo. If you want Firebase Storage uploads, copy `app/google-services.json.example` to `app/google-services.json` and replace the placeholder values with your Firebase Android app configuration. Do not commit the real `google-services.json` file.

### Build and Run

```bash
./gradlew assembleDebug
./gradlew installDebug
```

On Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
.\gradlew.bat installDebug
```

## Testing and Quality Checks

Run the local unit test task:

```bash
./gradlew testDebugUnitTest
```

Run Android Lint:

```bash
./gradlew lintDebug
```

Run instrumented tests with an emulator or device connected:

```bash
./gradlew connectedDebugAndroidTest
```

Latest local verification:

- `.\gradlew.bat clean testDebugUnitTest assembleDebug` passed.
- `.\gradlew.bat lintDebug` passed with warnings only.

## Backend

The demo backend uses MockAPI:

- Base URL: `https://6a1460d76c7db8aac05469d9.mockapi.io/`
- Users: `GET/POST/PUT /user`
- Items: `GET/POST/PUT/DELETE /items`
- Photos: Firebase Storage when configured, otherwise base64 fallback

## Project Info

| Property | Value |
| --- | --- |
| Application ID | `com.lostfound` |
| Namespace | `com.example.lostfound` |
| Version | `1.1` (`versionCode` 2) |
| Min SDK | 28 |
| Target SDK | 35 |
| Compile SDK | 35 |

## Repository Hygiene

The repository intentionally excludes local and generated files such as:

- `local.properties`
- `.gradle/`, `build/`, and `app/build/`
- `.idea/`
- `.env` files
- real Firebase config files
- signing keys and release artifacts
- local demo photo folders

Tracked screenshots and helper scripts are included because they support the portfolio/demo presentation.

## Known Limitations

This app is intentionally scoped as a student demo. A production release would need stronger backend and security design:

- Passwords are stored in plain text on MockAPI for demo purposes.
- Local profile sync uses SharedPreferences; production should use token-based auth and encrypted storage.
- There is no admin moderation, push notification system, or email verification.
- Khmer localization is partial, with English fallback strings.
- Release builds currently keep R8 minification disabled.

## License

No open-source license is currently specified. This repository is shared for academic and portfolio review.
