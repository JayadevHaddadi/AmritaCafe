package edu.amrita.amritacafe.printer

import android.content.SharedPreferences
import androidx.core.content.edit
import edu.amrita.amritacafe.settings.Configuration
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OrderNumberService(private val preferences: SharedPreferences) {

    private var lastOrderNumber
        get() = preferences.getInt(LAST_ORDER_NUMBER, 0)
        set(value) {
            preferences.edit {
                putInt(LAST_ORDER_NUMBER, value)
                apply()
            }
        }

    suspend fun next() = mutex.withLock {
        val range = preferences.getString(Configuration.ORDER_NUMBER_RANGE, "100")!!.toInt()

        lastOrderNumber = range + (lastOrderNumber + 1) % 100
        lastOrderNumber
    }

    companion object {
        const val LAST_ORDER_NUMBER = "last_order_number"
        private val mutex = Mutex()
    }
}