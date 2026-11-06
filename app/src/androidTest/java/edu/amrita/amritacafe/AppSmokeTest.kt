package edu.amrita.amritacafe

import android.content.pm.ActivityInfo
import androidx.lifecycle.Lifecycle
import androidx.test.core.app.ActivityScenario
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import edu.amrita.amritacafe.activities.MainActivity
import edu.amrita.amritacafe.activities.SettingsActivity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Runs on a real Android system (emulator in CI, or a tablet via
 * ./gradlew connectedDebugAndroidTest). Opens each screen, rotates it and checks the app is
 * still alive - catches crashes that only show up on real Android (R8, ViewBinding, permissions).
 */
@RunWith(AndroidJUnit4::class)
class AppSmokeTest {

    private fun rotate(scenario: ActivityScenario<*>, orientation: Int) {
        scenario.onActivity { it.requestedOrientation = orientation }
        InstrumentationRegistry.getInstrumentation().waitForIdleSync()
        Thread.sleep(1500)
    }

    private fun <T : android.app.Activity> openAndRotate(cls: Class<T>) {
        ActivityScenario.launch(cls).use { scenario ->
            InstrumentationRegistry.getInstrumentation().waitForIdleSync()
            rotate(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            rotate(scenario, ActivityInfo.SCREEN_ORIENTATION_PORTRAIT)
            rotate(scenario, ActivityInfo.SCREEN_ORIENTATION_LANDSCAPE)
            assertTrue(scenario.state.isAtLeast(Lifecycle.State.STARTED))
        }
    }

    @Test
    fun settingsScreenOpensAndRotates() = openAndRotate(SettingsActivity::class.java)

    @Test
    fun mainScreenOpensAndRotates() = openAndRotate(MainActivity::class.java)

    @Test
    fun appPackageIsCorrect() {
        assertEquals("edu.amrita.amritacafe", InstrumentationRegistry.getInstrumentation().targetContext.packageName)
    }
}
