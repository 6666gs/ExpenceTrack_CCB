package com.expensetrack.setup

/**
 * Where each phone brand hides its "keep this app alive" switches. Chinese ROMs kill background
 * apps aggressively, which silently stops notification capture and the evening summary.
 * Paths differ slightly between system versions, so the UI words them as hints, not promises.
 */
data class RomGuide(
    val brand: String,
    val autoStartPath: String,
    val batteryPath: String,
    /** How to pin the app in the recents screen so one-tap clearing skips it. */
    val lockPath: String,
    /** Vendor screens tried in order before falling back to the app's system info page. */
    val autoStartScreens: List<VendorScreen>,
) {
    companion object {
        fun forManufacturer(manufacturer: String): RomGuide = when (manufacturer.trim().lowercase()) {
            "vivo", "iqoo" -> VIVO
            "xiaomi", "redmi", "poco" -> XIAOMI
            "oppo", "oneplus", "realme" -> OPPO
            "huawei", "honor" -> HUAWEI
            else -> GENERIC
        }

        private val VIVO = RomGuide(
            brand = "vivo / iQOO",
            autoStartPath = "i管家 › 应用管理 › 权限管理 › 自启动，打开「消费记录」",
            batteryPath = "设置 › 电池 › 后台耗电管理 › 消费记录 › 允许后台高耗电",
            lockPath = "最近任务里按住「消费记录」卡片下拉，出现锁的图标即可",
            autoStartScreens = listOf(
                VendorScreen("com.vivo.permissionmanager", "com.vivo.permissionmanager.activity.BgStartUpManagerActivity"),
                VendorScreen("com.iqoo.secure", "com.iqoo.secure.ui.phoneoptimize.BgStartUpManager"),
            ),
        )
        private val XIAOMI = RomGuide(
            brand = "小米 / Redmi",
            autoStartPath = "设置 › 应用设置 › 应用管理 › 消费记录 › 打开「自启动」",
            batteryPath = "同一页面 › 省电策略 › 无限制",
            lockPath = "最近任务里长按「消费记录」卡片，点锁形图标",
            autoStartScreens = listOf(
                VendorScreen("com.miui.securitycenter", "com.miui.permcenter.autostart.AutoStartManagementActivity"),
            ),
        )
        private val OPPO = RomGuide(
            brand = "OPPO / 一加 / realme",
            autoStartPath = "设置 › 应用 › 应用管理 › 消费记录 › 耗电管理 › 允许自动启动",
            batteryPath = "同一页面 › 允许后台行为",
            lockPath = "最近任务里点「消费记录」卡片右上角的菜单，选「锁定」",
            autoStartScreens = listOf(
                VendorScreen("com.coloros.safecenter", "com.coloros.safecenter.permission.startup.StartupAppListActivity"),
                VendorScreen("com.coloros.safecenter", "com.coloros.safecenter.startupapp.StartupAppListActivity"),
            ),
        )
        private val HUAWEI = RomGuide(
            brand = "华为 / 荣耀",
            autoStartPath = "设置 › 电池 › 应用启动管理 › 消费记录 › 关闭「自动管理」，三个开关全开",
            batteryPath = "同上，开启「允许后台活动」",
            lockPath = "最近任务里按住「消费记录」卡片下拉，出现锁的图标即可",
            autoStartScreens = listOf(
                VendorScreen("com.huawei.systemmanager", "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
                VendorScreen("com.hihonor.systemmanager", "com.hihonor.systemmanager.startupmgr.ui.StartupNormalAppListActivity"),
            ),
        )
        private val GENERIC = RomGuide(
            brand = "其他手机",
            autoStartPath = "在系统设置里找到「消费记录」，允许自启动 / 后台运行",
            batteryPath = "电池优化选「不优化」或「无限制」",
            lockPath = "在最近任务里锁定「消费记录」（如系统支持）",
            autoStartScreens = emptyList(),
        )
    }
}

/** A vendor settings activity, kept as plain strings so this file stays testable on the JVM. */
data class VendorScreen(val packageName: String, val className: String)
