# 消费记录 ExpenseTrack

一个只在本机运行的 Android 记账 App：

1. **自动捕获建设银行消费通知**。来源包括建行 App 推送（`com.chinamworld.main`），以及任何含“建设银行 / 建行 / 龙卡 / 95533”的通知（95533 短信、建行微信公众号）。支出（消费、支付、支出、扣款、取款、转出、缴费）和收入（收入、存入、转入、入账、汇入）都会记录；退款、退货、撤销、冲正算作收入。信用卡还款只是自己账户之间转钱，会被忽略。收入显示为绿色的 +¥，单独统计「收入」和「结余」；日历、柱状图、AI 分析和每日支出只看支出。
2. **每天傍晚发通知**，告诉你今天一共花了多少、花了几笔。默认 19:00，可在 App 里改。

3. **点一笔记录可以编辑**：写备注记下买了什么；金额或收支类型识别错了也可以直接改。手动记账同样可以选支出或收入。
4. **统计页**：按日 / 周 / 月 / 年查看。月视图是日历，颜色越深花得越多；周、年视图是柱状图，点日期或柱子可以下钻。
5. **AI 消费分析（可选）**：在「设置」里填入 DeepSeek API Key（加密存在本机），统计页点「开始分析」，DeepSeek 会按备注给每笔消费分类，App 在本地算出消费分布，并显示总结、观察和建议。每个时间段的结果保存在本机，重开 App 仍在；之后如果这段时间的记录有增删或改了备注，会提示结果可能已过时。

同一笔消费如果同时收到 App 推送和短信，只记一次。另外支持手动记账和删除记录。数据存在本地 Room 数据库里。只有点「开始分析」或「测试连接」时才联网，而且只发送金额、时间、备注和商户名，不发送银行通知原文。

## 运行

1. 用 **Android Studio**（Ladybug 或更新版本，自带 JDK 17）打开本目录，等 Gradle 同步完成。
2. 手机打开 USB 调试并连上电脑，点 Run。
3. 打开 App 后按页面顶部的提示完成授权：
   - **读取通知**：必须开，否则无法捕获。
   - **发送通知**：Android 13 及以上需要。
   - **准时提醒（精确闹钟）**：可选。不开的话，系统可能会晚几分钟才提醒。

命令行跑单元测试：`./gradlew test`（Gradle 8.9 需要 JDK 17–21；Android Studio 自带的 JDK 25 跑不了命令行构建，可用 `JAVA_HOME=$(/usr/libexec/java_home -v 21)`）。项目里没有提交 wrapper 的 jar，先在 Android Studio 里同步一次，或者执行 `gradle wrapper`。

## 国产 ROM 必看（小米 / 华为 / OPPO / vivo）

这些系统会主动杀后台，导致通知捕获和定时提醒失效。App 里「设置 › 后台运行 · 防止漏记」按当前机型列出了步骤，每步都有按钮直达对应的系统页面：

1. **读取通知**：必须开。
2. **自启动**：例如 vivo 在 i管家 › 应用管理 › 权限管理 › 自启动。
3. **后台耗电**：改成允许后台高耗电 / 无限制。
4. **建行 App 通知**：建行 App 自己的消息推送和系统通知都要开着，否则没有通知可读。
5. **锁定在后台**：在最近任务里锁定「消费记录」，一键清理时会跳过它。
6. **常驻通知**（默认开）：通知栏显示「自动记账中 · 今日支出」，让系统不易清理，被划掉后会自动重新出现，可在同一处关掉。

被清理后，重新打开一次 App 就会重新连上通知监听，并补记通知栏里还留着的建行消费。已经从通知栏划掉的通知读不回来。

授权完成后，「记录」页会提示一次这一步，点「已设置好」即可收起。

## 发给朋友试用

1. 构建正式版：`gradle assembleRelease`，安装包在 `app/build/outputs/apk/release/app-release.apk`（约 1.3 MB）。
2. 把 APK 发给对方，对方安装时允许「安装未知来源应用」。
3. 打开后按「记录」页顶部的提示授权，再到「设置」走一遍后台运行的步骤。
4. AI 分析需要对方用自己的 DeepSeek API Key。

正式版目前用调试证书签名，适合私下分发；上架应用商店前要换成自己的发布证书。

## 如果某条通知没被识别

识别规则都在 `app/src/main/java/com/expensetrack/parser/CcbNotificationParser.kt`。把那条通知的原文（金额、卡号可以打码）加到 `CcbNotificationParserTest.kt` 里作为新用例，再调整正则即可。

## 结构

```
parser/   CcbNotificationParser  通知文本 → 金额/商户（纯函数，有单测）
capture/  BankNotificationListener  NotificationListenerService
data/     Room 实体、DAO、仓库、去重规则
summary/  AlarmManager 定时、开机重排、每日小结通知
stats/    StatsPeriod / StatsCalculator  日周月年区间与汇总（纯函数，有单测）
ai/       DeepSeekClient、SpendingAnalysisPrompt  提示词构造与结果解析（有单测）
settings/ SettingsStore、SecretCipher（Android Keystore 加密 API Key）
ui/       Compose 界面：记录 / 统计 / 设置 三个标签页
```
