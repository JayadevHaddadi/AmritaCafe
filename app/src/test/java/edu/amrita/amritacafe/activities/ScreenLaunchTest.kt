package edu.amrita.amritacafe.activities

import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Opens each screen in portrait and landscape. Catches layout/ViewBinding mismatches between
 * res/layout and res/layout-land (missing IDs, bad attributes) and crashes in onCreate.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ScreenLaunchTest {

    /**
     * The Hoin printer SDK keeps a static singleton bound to the Application it was created with.
     * On a device there's one Application per process, but Robolectric creates a fresh one per test,
     * so clear the singleton to mimic a clean process start.
     */
    @org.junit.Before
    fun resetPrinterSingleton() {
        com.example.hoinprinterlib.HoinPrinter::class.java.declaredFields
            .filter { java.lang.reflect.Modifier.isStatic(it.modifiers) && it.type == com.example.hoinprinterlib.HoinPrinter::class.java }
            .forEach { it.isAccessible = true; it.set(null, null) }
    }

    private fun launchAndRotate(cls: Class<out android.app.Activity>, startQualifier: String, nextQualifier: String) {
        org.robolectric.RuntimeEnvironment.setQualifiers(startQualifier)
        ActivityScenario.launch(cls).use { scenario ->
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
            org.robolectric.RuntimeEnvironment.setQualifiers(nextQualifier)
            scenario.recreate()
            assertEquals(Lifecycle.State.RESUMED, scenario.state)
        }
    }

    @Test
    fun settingsOpensInPortraitThenLandscape() =
        launchAndRotate(SettingsActivity::class.java, "w800dp-h1280dp-port", "w1280dp-h800dp-land")

    @Test
    fun settingsOpensInLandscapeThenPortrait() =
        launchAndRotate(SettingsActivity::class.java, "w1280dp-h800dp-land", "w800dp-h1280dp-port")

    @Test
    fun mainScreenOpensInLandscapeThenPortrait() =
        launchAndRotate(MainActivity::class.java, "w1280dp-h800dp-land", "w800dp-h1280dp-port")

    @Test
    fun mainScreenOpensOnAPhone() =
        launchAndRotate(MainActivity::class.java, "w400dp-h800dp-port", "w800dp-h400dp-land")
}
