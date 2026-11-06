package com.azikar24.wormaceptor.feature.preferences.data

import android.content.Context
import android.content.SharedPreferences
import android.content.pm.ApplicationInfo
import com.azikar24.wormaceptor.domain.entities.PreferenceValue
import io.kotest.matchers.collections.shouldBeEmpty
import io.kotest.matchers.collections.shouldContainExactly
import io.kotest.matchers.collections.shouldContainExactlyInAnyOrder
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.nulls.shouldBeNull
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import io.mockk.Runs
import io.mockk.every
import io.mockk.just
import io.mockk.mockk
import io.mockk.verify
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Nested
import org.junit.jupiter.api.Test
import java.io.File

class PreferencesDataSourceTest {

    private val prefsDir = File(System.getProperty("java.io.tmpdir"), "prefs_test_${System.nanoTime()}")
    private val appInfo = ApplicationInfo().apply {
        dataDir = prefsDir.parent
    }
    private val context = mockk<Context>(relaxed = true) {
        every { applicationInfo } returns appInfo
    }
    private lateinit var dataSource: PreferencesDataSource

    @BeforeEach
    fun setUp() {
        // Ensure a clean prefs directory named "shared_prefs" under the parent
        val sharedPrefsDir = File(appInfo.dataDir, "shared_prefs")
        sharedPrefsDir.deleteRecursively()
        sharedPrefsDir.mkdirs()

        dataSource = PreferencesDataSource(context)
    }

    @Nested
    inner class `getPreferenceFileNames` {

        @Test
        fun `returns empty list when shared_prefs directory does not exist`() {
            val missingDirInfo = ApplicationInfo().apply {
                dataDir = "/non/existent/path/${System.nanoTime()}"
            }
            val ctx = mockk<Context>(relaxed = true) {
                every { applicationInfo } returns missingDirInfo
            }
            val ds = PreferencesDataSource(ctx)

            val result = ds.getPreferenceFileNames()

            result.shouldBeEmpty()
        }

        @Test
        fun `returns only xml file names without extension`() {
            val sharedPrefsDir = File(appInfo.dataDir, "shared_prefs")
            File(sharedPrefsDir, "app_settings.xml").createNewFile()
            File(sharedPrefsDir, "user_prefs.xml").createNewFile()
            File(sharedPrefsDir, "backup.bak").createNewFile()

            val result = dataSource.getPreferenceFileNames()

            result shouldHaveSize 2
            result shouldContainExactlyInAnyOrder listOf("app_settings", "user_prefs")
        }

        @Test
        fun `ignores subdirectories inside shared_prefs`() {
            val sharedPrefsDir = File(appInfo.dataDir, "shared_prefs")
            File(sharedPrefsDir, "app_settings.xml").createNewFile()
            File(sharedPrefsDir, "subdir").mkdir()

            val result = dataSource.getPreferenceFileNames()

            result shouldHaveSize 1
            result.first() shouldBe "app_settings"
        }

        @Test
        fun `returns empty list when directory is empty`() {
            val result = dataSource.getPreferenceFileNames()

            result.shouldBeEmpty()
        }
    }

    @Nested
    inner class `getPreferenceFiles` {

        @Test
        fun `returns preference files with item counts`() {
            val sharedPrefsDir = File(appInfo.dataDir, "shared_prefs")
            File(sharedPrefsDir, "app_settings.xml").createNewFile()
            File(sharedPrefsDir, "user_prefs.xml").createNewFile()

            val prefs1 = mockk<SharedPreferences> {
                every { all } returns mapOf("key1" to "val1", "key2" to 42)
            }
            val prefs2 = mockk<SharedPreferences> {
                every { all } returns mapOf("theme" to "dark")
            }

            every { context.getSharedPreferences("app_settings", Context.MODE_PRIVATE) } returns prefs1
            every { context.getSharedPreferences("user_prefs", Context.MODE_PRIVATE) } returns prefs2

            val result = dataSource.getPreferenceFiles()

            result shouldHaveSize 2
            result.first { it.name == "app_settings" }.itemCount shouldBe 2
            result.first { it.name == "user_prefs" }.itemCount shouldBe 1
        }

        @Test
        fun `returns empty list when no xml files exist`() {
            val result = dataSource.getPreferenceFiles()

            result.shouldBeEmpty()
        }
    }

    @Nested
    inner class `getPreferenceItems` {

        @Test
        fun `extracts typed items sorted by key`() {
            val prefs = mockk<SharedPreferences> {
                every { all } returns mapOf(
                    "zeta" to "hello",
                    "alpha" to 42,
                    "beta" to true,
                )
            }
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.getPreferenceItems("file")

            result shouldHaveSize 3
            result[0].key shouldBe "alpha"
            result[0].value.shouldBeInstanceOf<PreferenceValue.IntValue>()
            (result[0].value as PreferenceValue.IntValue).value shouldBe 42
            result[1].key shouldBe "beta"
            result[1].value.shouldBeInstanceOf<PreferenceValue.BooleanValue>()
            result[2].key shouldBe "zeta"
            result[2].value.shouldBeInstanceOf<PreferenceValue.StringValue>()
        }

        @Test
        fun `handles all supported types`() {
            val prefs = mockk<SharedPreferences> {
                every { all } returns mapOf(
                    "s" to "text",
                    "i" to 1,
                    "l" to 2L,
                    "f" to 3.14f,
                    "b" to false,
                    "ss" to setOf("a", "b"),
                )
            }
            every { context.getSharedPreferences("typed", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.getPreferenceItems("typed")

            result shouldHaveSize 6
            val typeNames = result.map { it.value.typeName }.toSet()
            typeNames shouldBe setOf("String", "Int", "Long", "Float", "Boolean", "StringSet")
        }

        @Test
        fun `skips entries with unsupported value types`() {
            val prefs = mockk<SharedPreferences> {
                every { all } returns mapOf(
                    "good" to "value",
                    "bad" to byteArrayOf(1, 2, 3),
                )
            }
            every { context.getSharedPreferences("mixed", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.getPreferenceItems("mixed")

            result shouldHaveSize 1
            result.first().key shouldBe "good"
        }

        @Test
        fun `sorts case-insensitively`() {
            val prefs = mockk<SharedPreferences> {
                every { all } returns mapOf(
                    "Banana" to "b",
                    "apple" to "a",
                    "Cherry" to "c",
                )
            }
            every { context.getSharedPreferences("sort", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.getPreferenceItems("sort")

            result.map { it.key } shouldContainExactly listOf("apple", "Banana", "Cherry")
        }
    }

    @Nested
    inner class `getPreference` {

        @Test
        fun `returns typed value for existing key`() {
            val prefs = mockk<SharedPreferences> {
                every { all } returns mapOf("key" to 99L)
            }
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.getPreference("file", "key")

            result.shouldBeInstanceOf<PreferenceValue.LongValue>()
            (result as PreferenceValue.LongValue).value shouldBe 99L
        }

        @Test
        fun `returns null for missing key`() {
            val prefs = mockk<SharedPreferences> {
                every { all } returns emptyMap()
            }
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.getPreference("file", "missing")

            result.shouldBeNull()
        }
    }

    @Nested
    inner class `setPreference` {

        private val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
            every { apply() } just Runs
        }
        private val prefs = mockk<SharedPreferences> {
            every { edit() } returns editor
        }

        @BeforeEach
        fun setUpPrefs() {
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs
        }

        @Test
        fun `puts String value`() {
            dataSource.setPreference("file", "key", PreferenceValue.StringValue("hello"))

            verify { editor.putString("key", "hello") }
            verify { editor.apply() }
        }

        @Test
        fun `puts Int value`() {
            dataSource.setPreference("file", "key", PreferenceValue.IntValue(42))

            verify { editor.putInt("key", 42) }
            verify { editor.apply() }
        }

        @Test
        fun `puts Long value`() {
            dataSource.setPreference("file", "key", PreferenceValue.LongValue(100L))

            verify { editor.putLong("key", 100L) }
            verify { editor.apply() }
        }

        @Test
        fun `puts Float value`() {
            dataSource.setPreference("file", "key", PreferenceValue.FloatValue(1.5f))

            verify { editor.putFloat("key", 1.5f) }
            verify { editor.apply() }
        }

        @Test
        fun `puts Boolean value`() {
            dataSource.setPreference("file", "key", PreferenceValue.BooleanValue(true))

            verify { editor.putBoolean("key", true) }
            verify { editor.apply() }
        }

        @Test
        fun `puts StringSet value`() {
            val stringSet = setOf("a", "b", "c")
            dataSource.setPreference("file", "key", PreferenceValue.StringSetValue(stringSet))

            verify { editor.putStringSet("key", stringSet) }
            verify { editor.apply() }
        }
    }

    @Nested
    inner class `deletePreference` {

        @Test
        fun `removes key from editor`() {
            val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
                every { remove("key") } returns this
                every { apply() } just Runs
            }
            val prefs = mockk<SharedPreferences> {
                every { edit() } returns editor
            }
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs

            dataSource.deletePreference("file", "key")

            verify { editor.remove("key") }
            verify { editor.apply() }
        }
    }

    @Nested
    inner class `clearFile` {

        @Test
        fun `clears all preferences`() {
            val editor = mockk<SharedPreferences.Editor>(relaxed = true) {
                every { clear() } returns this
                every { apply() } just Runs
            }
            val prefs = mockk<SharedPreferences> {
                every { edit() } returns editor
            }
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs

            dataSource.clearFile("file")

            verify { editor.clear() }
            verify { editor.apply() }
        }
    }

    @Nested
    inner class `registerChangeListener` {

        @Test
        fun `registers listener and returns SharedPreferences`() {
            val listener = mockk<SharedPreferences.OnSharedPreferenceChangeListener>()
            val prefs = mockk<SharedPreferences>(relaxed = true)
            every { context.getSharedPreferences("file", Context.MODE_PRIVATE) } returns prefs

            val result = dataSource.registerChangeListener("file", listener)

            result shouldBe prefs
            verify { prefs.registerOnSharedPreferenceChangeListener(listener) }
        }
    }

    @Nested
    inner class `unregisterChangeListener` {

        @Test
        fun `unregisters listener from prefs`() {
            val listener = mockk<SharedPreferences.OnSharedPreferenceChangeListener>()
            val prefs = mockk<SharedPreferences>(relaxed = true)

            dataSource.unregisterChangeListener(prefs, listener)

            verify { prefs.unregisterOnSharedPreferenceChangeListener(listener) }
        }
    }
}
