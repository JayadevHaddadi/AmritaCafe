package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import androidx.preference.PreferenceManager
import androidx.test.core.app.ApplicationProvider
import edu.amrita.amritacafe.menu.MenuItem
import edu.amrita.amritacafe.menu.RegularOrderItem
import edu.amrita.amritacafe.model.Order
import edu.amrita.amritacafe.settings.Configuration
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.File

/**
 * Guards the offline order queue (google_sheets_pending_queue.json): it holds real sales data,
 * so it must persist across restarts, never be silently lost, and respect the per-tablet
 * "Push orders to Google Sheets" setting.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OfflineOrderSyncTest {

    private lateinit var context: Context
    private val queueFile get() = File(context.filesDir, "google_sheets_pending_queue.json")
    private val prefs get() = PreferenceManager.getDefaultSharedPreferences(context)

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        prefs.edit().clear().commit()
        context.filesDir.listFiles()?.forEach { it.delete() }
        OfflineOrderSync.resetForTests()
    }

    @After
    fun tearDown() {
        OfflineOrderSync.resetForTests()
    }

    private fun orderJson(n: Int) =
        JSONObject().put("tablet", "Test").put("order", n.toString()).put("items", JSONArray()).toString()

    @Test
    fun pushIsEnabledByDefault() {
        assertTrue(OfflineOrderSync.isPushEnabled(context))
        assertTrue(Configuration(prefs).pushOrdersToSheets)
    }

    @Test
    fun configurationAndSyncShareTheSamePushSetting() {
        Configuration(prefs).pushOrdersToSheets = false
        assertFalse(OfflineOrderSync.isPushEnabled(context))
        Configuration(prefs).pushOrdersToSheets = true
        assertTrue(OfflineOrderSync.isPushEnabled(context))
    }

    @Test
    fun queuedOrdersSurviveAnAppRestart() {
        OfflineOrderSync.addOrder(context, orderJson(1))
        OfflineOrderSync.addOrder(context, orderJson(2))
        assertEquals(2, OfflineOrderSync.getPendingCount(context))

        OfflineOrderSync.resetForTests() // drop in-memory cache, like a process restart
        assertEquals(2, OfflineOrderSync.getPendingCount(context))
        assertEquals(2, JSONArray(queueFile.readText()).length())
        val orders = OfflineOrderSync.getPendingQueueSnapshot(context).map { JSONObject(it.payload).getString("order") }
        assertEquals(listOf("1", "2"), orders)
    }

    @Test
    fun clearQueueRemovesEverythingAndReportsCount() {
        repeat(5) { OfflineOrderSync.addOrder(context, orderJson(it)) }
        assertEquals(5, OfflineOrderSync.clearQueue(context))
        assertEquals(0, OfflineOrderSync.getPendingCount(context))
        OfflineOrderSync.resetForTests()
        assertEquals(0, OfflineOrderSync.getPendingCount(context))
    }

    @Test
    fun corruptQueueFileIsPreservedNotOverwritten() {
        queueFile.writeText("[{\"id\":\"x\",\"payload\":") // truncated JSON
        assertEquals(0, OfflineOrderSync.getPendingCount(context))
        OfflineOrderSync.addOrder(context, orderJson(9))

        val preserved = context.filesDir.listFiles()!!.filter { it.name.contains(".corrupt-") }
        assertEquals(1, preserved.size)
        assertEquals("[{\"id\":\"x\",\"payload\":", preserved.single().readText())
        assertEquals(1, OfflineOrderSync.getPendingCount(context))
    }

    @Test
    fun legacyPreferenceQueueIsMigratedToDisk() {
        prefs.edit().putString("offline_orders_queue", JSONArray().put(JSONObject(orderJson(7))).toString()).commit()
        assertEquals(1, OfflineOrderSync.getPendingCount(context))
        OfflineOrderSync.addOrder(context, orderJson(8))
        OfflineOrderSync.resetForTests()
        assertEquals(2, OfflineOrderSync.getPendingCount(context))
    }

    @Test
    fun ordersAreNotQueuedWhenPushIsDisabled() {
        Configuration(prefs).pushOrdersToSheets = false
        val order = Order(
            orderNumber = 1,
            orderItems = listOf(RegularOrderItem(MenuItem("Toast", "T", 5f, "TOAST")))
        )
        sendToSheets(listOf(order), Configuration(prefs), context)
        OfflineOrderSync.runInBackground { } // flush the background executor
        Thread.sleep(300)
        assertEquals(0, OfflineOrderSync.getPendingCount(context))
        assertFalse(queueFile.exists())
    }
}
