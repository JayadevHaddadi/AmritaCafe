package edu.amrita.amritacafe.CloudStorage

import android.content.Context
import com.android.volley.RequestQueue
import com.android.volley.toolbox.Volley

/**
 * One app-wide Volley RequestQueue.
 *
 * Every Volley.newRequestQueue() call starts 5 new threads (4 network + 1 cache dispatcher)
 * that are never stopped. Creating a queue per request leaks threads until the OS refuses to
 * create more ("OutOfMemoryError: pthread_create failed"), which crashed the app after many orders.
 */
object SharedRequestQueue {
    @Volatile
    private var instance: RequestQueue? = null

    fun get(context: Context): RequestQueue {
        return instance ?: synchronized(this) {
            instance ?: Volley.newRequestQueue(context.applicationContext).also { instance = it }
        }
    }
}
