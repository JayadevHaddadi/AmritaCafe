package edu.amrita.amritacafe.model

import android.content.Context
import android.widget.FrameLayout
import androidx.appcompat.view.ContextThemeWrapper
import androidx.test.core.app.ApplicationProvider
import edu.amrita.amritacafe.R
import edu.amrita.amritacafe.menu.MenuItem
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/** Regression test for the 80113 crash: IndexOutOfBoundsException in OrderAdapter click handlers. */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class OrderAdapterTest {

    private val context: Context = ContextThemeWrapper(
        ApplicationProvider.getApplicationContext<Context>(), R.style.AppTheme
    )

    private fun item(code: String) = MenuItem(code, code, 10f, "TOAST")

    @Test
    fun tappingCancelOnAStaleRowAfterClearDoesNotCrash() {
        val adapter = OrderAdapter(context)
        adapter.add(item("A"))
        val row = adapter.getView(0, null, FrameLayout(context))

        adapter.clear()
        row.findViewById<android.view.View>(R.id.cancel_button).performClick()
        row.findViewById<android.view.View>(R.id.amount_TV).performClick()

        assertEquals(0, adapter.count)
    }

    @Test
    fun cancelRemovesTheTappedItem() {
        val adapter = OrderAdapter(context)
        adapter.add(item("A"))
        adapter.add(item("B"))
        val rowB = adapter.getView(1, null, FrameLayout(context))
        rowB.findViewById<android.view.View>(R.id.cancel_button).performClick()
        assertEquals(listOf("A"), adapter.orderItems.map { it.menuItem.code })
    }
}
