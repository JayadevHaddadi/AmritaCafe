package edu.amrita.amritacafe.printer

import android.content.SharedPreferences
import androidx.core.content.edit
import edu.amrita.amritacafe.settings.Configuration
import edu.amrita.amritacafe.settings.Configuration.Companion.RANGE_FROM_DEFAULT
import edu.amrita.amritacafe.settings.Configuration.Companion.RANGE_TO_DEFAULT
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OrderNumberService(private val preferences: SharedPreferences) {
    var currentOrderNumber
        get() = preferences.getInt(LAST_ORDER_NUMBER, RANGE_FROM_DEFAULT)
        set(value) {
            preferences.edit {
                putInt(LAST_ORDER_NUMBER, value)
                apply()
            }
        }

    suspend fun next() = mutex.withLock {
        val rangeFrom = preferences.getInt(Configuration.RANGE_FROM, RANGE_FROM_DEFAULT)
        val rangeTo = preferences.getInt(Configuration.RANGE_TO, RANGE_TO_DEFAULT)

        currentOrderNumber = if ((currentOrderNumber + 1) > rangeTo)
            rangeFrom
        else
            currentOrderNumber + 1
        currentOrderNumber
    }

    fun updateRange() {
        val rangeFrom = preferences.getInt(Configuration.RANGE_FROM, RANGE_FROM_DEFAULT)
        val rangeTo = preferences.getInt(Configuration.RANGE_TO, RANGE_TO_DEFAULT)
        if (currentOrderNumber < rangeFrom || currentOrderNumber > rangeTo)
            currentOrderNumber = rangeFrom
    }

    companion object {
        const val LAST_ORDER_NUMBER = "last_order_number"
        private val mutex = Mutex()
    }
}