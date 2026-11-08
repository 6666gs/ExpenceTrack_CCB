package com.expensetrack.setup

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RomGuideTest {
    @Test
    fun `matches brands case-insensitively including sub-brands`() {
        assertEquals("vivo / iQOO", RomGuide.forManufacturer("vivo").brand)
        assertEquals("vivo / iQOO", RomGuide.forManufacturer(" IQOO ").brand)
        assertEquals("小米 / Redmi", RomGuide.forManufacturer("Xiaomi").brand)
        assertEquals("OPPO / 一加 / realme", RomGuide.forManufacturer("OnePlus").brand)
        assertEquals("华为 / 荣耀", RomGuide.forManufacturer("HONOR").brand)
    }

    @Test
    fun `unknown brand falls back to generic advice without vendor screens`() {
        val guide = RomGuide.forManufacturer("Google")
        assertEquals("其他手机", guide.brand)
        assertTrue(guide.autoStartScreens.isEmpty())
    }
}
