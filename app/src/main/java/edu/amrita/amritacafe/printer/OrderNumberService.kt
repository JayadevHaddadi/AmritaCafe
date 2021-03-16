package edu.amrita.amritacafe.printer

import android.content.SharedPreferences
import androidx.core.content.edit
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OrderNumberService(private val preferences: SharedPreferences) {
    var currentOrderNumber
        get() = preferences.getInt(LAST_ORDER_NUMBER, 0)
        set(value) {
            preferences.edit {
                putInt(LAST_ORDER_NUMBER, value)
                apply()
            }
        }

    suspend fun next() = mutex.withLock {
        currentOrderNumber = currentOrderNumber + 1
        currentOrderNumber
    }

    companion object {
        const val LAST_ORDER_NUMBER = "last_order_number"
        private val mutex = Mutex()
    }
}