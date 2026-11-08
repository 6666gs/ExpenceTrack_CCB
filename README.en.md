# ExpenseTrack (消费记录)

[中文](README.md) | **English**

An on-device Android expense tracker. It reads **China Construction Bank (CCB, 建设银行)** transaction notifications, records every payment and deposit, shows daily / weekly / monthly / yearly statistics, and can optionally ask DeepSeek to summarise where your money went.

- No account and no server: all records stay on your phone.
- The APK is about 1.3 MB and runs on Android 8.0 and later.

> The app's interface is in Chinese, because it targets CCB's Chinese-language notifications.

## Features

1. **Automatic capture** from the CCB app's push notifications, 95533 SMS and the CCB WeChat account.
   - Spending: 消费, 支付, 支出, 扣款, 取款, 转出, 缴费.
   - Income: 收入, 存入, 转入, 入账, 汇入. Refunds and reversals (退款, 退货, 撤销, 冲正) count as income too.
   - Credit-card repayments (还款) only move money between your own accounts, so they are ignored.
   - A transaction that arrives as both an app push and an SMS is recorded once.
2. **Editing**: tap a record to add a note (what you bought) or to fix a misread amount or the income / spending type. Manual entry and deletion are supported too.
3. **Statistics** by day, week, month or year. The month view is a calendar heat map (darker means more spending). Week and year views are bar charts that drill down when tapped. When there is income, 收入 (income) and 结余 (balance) are shown alongside. The calendar, the charts and the AI analysis count spending only.
4. **AI analysis (optional)**: with your own DeepSeek API key, each expense is categorised by its note. The result is shown as a donut chart, plus a one-line summary, two observations and one suggestion. Results are saved per period and flagged as outdated when the records change.
5. **Daily summary**: a notification at a time you choose (19:00 by default) with today's spending and income.
6. **No missed records**: an ongoing "自动记账中 · 今日支出" notification makes the system less likely to kill the app, and it comes back if swiped away. If the app is killed anyway, opening it once reconnects the listener and backfills any CCB notifications still in the notification shade.

## Install

1. Download `app-release.apk`, open it on your phone and allow installing from unknown sources when asked.
   - **WeChat** renames shared APKs to `.apk.1`, which stops them installing. Save the file, rename it back to `.apk` in a file manager, then install. Alternatively, send it zipped or via another channel.
2. Open the app and grant notification access when the 记录 (Records) tab prompts you.
3. Go to 设置 › 后台运行 · 防止漏记 (Settings › Background) and follow the steps for your phone brand. Each step has a button that opens the matching system screen.

## Permissions

The app does **not** request call, contacts, SMS, location, camera, microphone or storage permissions.

**Granted by you:**

| Permission | Required? | Why |
|---|---|---|
| Notification access | **Yes** | Read CCB transaction notifications; nothing is recorded without it |
| Post notifications | Recommended | Daily summary and the ongoing "自动记账中" notification |
| Alarms & reminders (exact alarms) | Optional | Delivers the daily summary on time; it may otherwise be a few minutes late |

**Granted automatically at install:**

| Permission | Why |
|---|---|
| Internet | Only used when you tap 开始分析 (Analyse) or 测试连接 (Test connection) |
| Run at startup | Re-schedules the daily summary after a reboot |
| Foreground service | Keeps the "自动记账中" notification running so the app isn't killed |

**About the notification-access warning**: Android warns that the app "can read all notifications, including contact names and messages". That is simply what this permission means; Android has no way to grant access to one app's notifications only, so every expense tracker or fitness band app shows the same warning. This app checks each notification on the device and immediately drops anything that isn't from CCB. Nothing else is stored or sent anywhere.

## Privacy

- Records live in a local database. Cloud backup and device-to-device transfer are disabled.
- The network is used only for DeepSeek, and only when you ask. Analysis sends the amount, time, your note and the merchant of each expense in the chosen period. The raw bank notification text, which may contain your card's last digits, is never sent.
- The DeepSeek API key is encrypted with the Android Keystore.
- HTTPS only; release builds are not debuggable.

## Chinese Android phones (vivo / Xiaomi / OPPO / Huawei)

These systems aggressively kill background apps, which stops capture and reminders. The Settings tab lists these steps for your brand:

1. **Notification access**: required.
2. **Auto-start**: e.g. on vivo, i管家 › 应用管理 › 权限管理 › 自启动.
3. **Battery**: allow high background power use / no restrictions.
4. **CCB app notifications**: the CCB app's own push setting and its system notifications must be on.
5. **Lock in recents**: lock the app in the recent-apps screen so one-tap cleaning skips it.
6. **Ongoing notification**: on by default; can be switched off on the same screen.

A notification you swipe away from the shade cannot be read later, so it is worth completing all of these steps.

## Build from source

- Open the project in **Android Studio** (Ladybug or newer), sync, and press Run.
- Command-line builds need JDK 17–21 (Gradle 8.9 does not run on newer JDKs). The wrapper JAR is not committed: sync once in Android Studio or run `gradle wrapper` first, then:

```bash
./gradlew testDebugUnitTest assembleRelease
```

- The APK is written to `app/build/outputs/apk/release/app-release.apk`.
- Release builds are currently signed with the debug key, which is fine for sharing privately. Use your own release key before publishing to an app store.

## A notification isn't recognised?

Parsing rules live in [`CcbNotificationParser.kt`](app/src/main/java/com/expensetrack/parser/CcbNotificationParser.kt). Add the notification text (with card digits and balances masked) as a test case in [`CcbNotificationParserTest.kt`](app/src/test/java/com/expensetrack/parser/CcbNotificationParserTest.kt) and adjust the rules. Issues with masked samples are welcome too.

## Project structure

```
parser/    CcbNotificationParser      notification text → amount / merchant / income flag (pure, unit-tested)
capture/   BankNotificationListener   notification listener; ongoing notification, rebind, backfill
data/      Room entities, DAO, repository, de-duplication, migrations
summary/   AlarmManager scheduling, reboot handling, daily summary notification
stats/     StatsPeriod / StatsCalculator  period ranges and totals (pure, unit-tested)
ai/        DeepSeekClient, SpendingAnalysisPrompt, AnalysisCodec  prompt, parsing, persistence (unit-tested)
setup/     RomGuide, SetupIntents     per-brand background settings and shortcuts
settings/  SettingsStore, SecretCipher (Keystore-encrypted API key)
ui/        Compose screens: Records / Statistics / Settings
```
