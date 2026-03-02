# CeddFlow — Budget Tracker

> **Own Your Budget. Stay on Track.**

A personal budget tracking Android app built with Kotlin + Jetpack Compose. Designed to help you manage spending, track expenses by category, and stay on top of your financial goals — all offline, no account required.

**[⬇️ Download Latest APK](https://github.com/CeddDasma-14/budget-tracker/releases/latest)**

---

## Features

### Core
- **Budget Sessions** — Create named sessions with a starting balance; all expenses tracked within a session
- **Live Balance** — Remaining budget recalculates in real time as you type
- **Lock / Unlock Rows** — Freeze entries when done; tap edit icon to modify
- **Paid Tracking** — Mark individual expenses as paid with visual strikethrough
- **Swipe to Delete + Undo** — Swipe any expense row to delete; an undo snackbar lets you reverse it
- **Confirm Before Clear** — "Start New Budget" shows a confirmation dialog before wiping the session

### Categories & Organization
- **Expense Categories** — Assign a category to every expense (Food, Transport, Bills, Shopping, Health, Entertainment, Education, Other) via a dropdown picker
- **Category Summary Card** — Automatically groups and totals expenses by category at the bottom of the budget screen
- **Sort Expenses** — Sort the expense list by amount (high/low), category, or paid-first
- **Expense Notes** — Add a free-text note to any expense row for extra context

### Budget Goals
- **Savings Goal** — Set a savings target for any session; a goal-met badge appears once remaining balance reaches the goal
- **Over-Budget Warning** — A warning banner appears if total expenses exceed the initial budget

### Templates
- **Budget Templates** — Save any session's expenses as a reusable template; load it when starting a new budget to pre-fill rows instantly
- **Copy Session as Template** — In History, copy any past session's expenses as a new template with one tap

### History & Insights
- **Full Session History** — All past sessions stored and browsable in the History screen
- **Month Filter** — Filter history by year/month using a horizontal chip row
- **Search** — Search past sessions by name in real time
- **Monthly Summary Card** — When a month is selected, shows total budget, total spent, total remaining, and session count
- **Session Expansion** — Tap any history entry to expand it and see all individual expenses
- **Category Breakdown** — Expanded sessions include a category-by-category spending breakdown
- **Spending Insights** — Card showing top-5 spending categories across all (or filtered) sessions

### Charts & Visuals
- **Monthly Spending Chart** — Bar chart in History visualizing spending across the last 6 months
- **Budget Progress Bar** — Color-coded bar in the session header shows how much of your budget you've used (green → amber → red)
- **Animated Splash Screen** — On launch, the logo and slogan animate in with a staggered fade + slide effect

### Receipts & OCR
- **Receipt Photos** — Attach a photo receipt to any expense using the system Photo Picker (no legacy permissions)
- **Receipt OCR** — "Scan Amount" automatically reads the total from a receipt photo using ML Kit

### Recurring Expenses
- **Recurring Expenses** — Mark expenses as recurring; they show a recurring badge icon and carry over from templates

### Data & Export
- **CSV Export** — Export any session's expenses as a CSV file and share via any installed app
- **Room Database (v4)** — All data persisted locally; no internet required

### Notifications & Widget
- **Daily Reminder Notification** — Set a daily reminder time in Settings; WorkManager fires a notification to prompt you to log expenses
- **Home Screen Widget** — A Glance-based widget shows your current session's remaining balance and budget name on the home screen

### Customization
- **Dark Mode** — Toggle dark/light theme in Settings; preference saved and applied immediately

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| Architecture | MVVM + Repository |
| DI | Hilt 2.52 |
| Database | Room 2.6.1 (v4) |
| Preferences | DataStore 1.1.1 |
| Navigation | Navigation Compose 2.8.5 |
| Images | Coil 2.7.0 |
| OCR | ML Kit Text Recognition 16.0.1 |
| Widget | Glance 1.1.0 |
| Background | WorkManager 2.9.1 |
| Min SDK | 26 (Android 8.0) |
| Target SDK | 35 (Android 15) |

---

## Setup

1. Clone the repo:
   ```bash
   git clone https://github.com/CeddDasma-14/budget-tracker.git
   cd budget-tracker
   ```

2. Open in **Android Studio Hedgehog** or later.

3. Sync Gradle and run on a device or emulator (API 26+).

> No API keys or accounts required — everything runs fully offline.

---

## Project Structure

```
app/src/main/java/com/cedd/budgettracker/
├── data/
│   ├── local/          # Room entities, DAOs, relations, database (v4)
│   ├── preferences/    # DataStore repository
│   └── repository/     # BudgetRepository (single source of truth)
├── di/                 # Hilt modules
├── domain/model/       # UI state & domain models
├── navigation/         # NavHost + splash overlay
├── notification/       # WorkManager daily reminder
├── presentation/
│   ├── budget/         # Main budget editor screen + ViewModel
│   ├── history/        # Session history screen + ViewModel
│   ├── settings/       # Settings screen + ViewModel
│   └── components/     # Reusable Compose components
└── widget/             # Glance home screen widget
```

---

## License

Personal project — feel free to use as a reference.
