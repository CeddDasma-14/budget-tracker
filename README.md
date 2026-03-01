# Budget Tracker 💰

A native Android budget tracking app built with **Kotlin + Jetpack Compose**, featuring a clean Material 3 design and real-time balance calculations in Philippine Peso (₱).

## Features

### Core
- **Real-time balance** — remaining balance updates instantly as you type
- **Lock/unlock expense rows** — freeze entries when done, tap edit to modify
- **Paid tracking** — mark individual expenses as paid with visual strikethrough
- **Receipt photos** — attach gallery images to any expense row; per-row or global flow

### Tier 1 — Productivity
- **Expense Categories** — 10 categories (Housing, Food, Transport, etc.) with emoji badges and color coding
- **Budget Templates** — save your current budget as a reusable template, load it with one tap
- **Budget Progress Bar** — animated color-coded bar (green → amber → red) showing spending %
- **Swipe to Delete** — swipe expense rows left to dismiss instantly

### Tier 2 — Analytics & Automation
- **CSV Export** — export any budget session to a `.csv` file and share via any app
- **Monthly Spending Chart** — animated bar chart of the last 6 sessions in History
- **Recurring Expenses** — mark expenses as recurring; they carry over from templates
- **Receipt OCR** — scan a receipt photo to auto-detect and fill the amount (ML Kit)

### Tier 3 — Convenience
- **Home Screen Widget** — Glance-based widget showing the app at a glance
- **Spending Insights** — top-5 categories ranked by total spend across all sessions
- **Dark Mode** — toggle in Settings; persists across launches
- **Daily Reminder** — configurable notification reminder via WorkManager

## Tech Stack

| Layer | Technology |
|-------|-----------|
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt (Dagger) |
| Database | Room v2 with migration |
| Async | Kotlin Coroutines + StateFlow |
| Preferences | DataStore |
| Image Loading | Coil |
| OCR | ML Kit Text Recognition |
| Background Work | WorkManager + HiltWorker |
| Widget | Glance AppWidget |
| Navigation | Navigation Compose |

## Screenshots

> Coming soon

## Getting Started

### Prerequisites
- Android Studio Hedgehog or later
- Android SDK 26+
- JDK 17

### Build

```bash
git clone https://github.com/CeddDasma-14/budget-tracker.git
cd budget-tracker
./gradlew assembleDebug
```

Open in Android Studio and run on a device or emulator (API 26+).

## Project Structure

```
app/src/main/java/com/cedd/budgettracker/
├── data/
│   ├── local/          # Room entities, DAOs, relations, database
│   ├── preferences/    # DataStore repository
│   └── repository/     # BudgetRepository (single source of truth)
├── di/                 # Hilt modules
├── domain/model/       # UI state & domain models
├── navigation/         # NavHost + Screen sealed classes
├── notification/       # WorkManager reminder worker
├── presentation/
│   ├── budget/         # Main budget editor screen + ViewModel
│   ├── history/        # Session history screen + ViewModel
│   ├── settings/       # Settings screen + ViewModel
│   └── components/     # Reusable Compose components
└── widget/             # Glance home screen widget
```

## License

MIT License — see [LICENSE](LICENSE) for details.
