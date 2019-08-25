package edu.amrita.jayadev.amritacafe.receiptprinter

import android.content.Context
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class OrderNumberService(context: Context) {

    private val preferences = PreferenceManager.getDefaultSharedPreferences(context)

    suspend fun next() = mutex.withLock {
        val seed = preferences.getInt("order_number_seed", 0)
        val interval = preferences.getInt("order_number_interval", 3)
        val offset = preferences.getInt("order_number_offset", 0)

        val nextNumber = if (seed % interval != offset) {
            seed + interval - (seed % interval)
        } else {
            seed + interval
        }
        preferences.edit {
            putInt("order_number_seed", nextNumber)
            commit()
        }
        nextNumber
    }

    companion object {
        private val mutex = Mutex()
    }
}