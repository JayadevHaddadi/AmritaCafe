
package edu.amrita.jayadev.amritacafe.settings


import com.github.kittinunf.fuel.Fuel
import com.github.kittinunf.fuel.coroutines.awaitObjectResponseResult
import com.github.kittinunf.fuel.serialization.kotlinxDeserializerOf
import edu.amrita.jayadev.amritacafe.menu.MenuItem
import kotlinx.serialization.Serializable
import android.content.Context
import edu.amrita.jayadev.amritacafe.menu.Availability
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonConfiguration
import java.io.File

val List<MenuItem>.breakfastMenu get() = this.filter { it.availability != Availability.LunchDinner }
val List<MenuItem>.lunchDinnerMenu get() = this.filter { it.availability != Availability.Breakfast }
/**
 * Maintains configuration for the application.
 *
 * Grabs JSON from remote.
 * Deserializes into a configuration object.
 * Stores a copy locally.
 * On HTTP Error, returns the locally cached copy.
 * Defaults to the defaultConfiguration for testing.
 *
 */
@Serializable
data class Configuration (
    val receiptPrinterIP: String,
    val kitchenPrinterIP: String,
    val menu: List<MenuItem>
) {


    companion object {
        private var _current = defaultConfiguration
        val current get() = _current
        private const val FILENAME = "config.json"
        private const val URL = "https://gist.github.com/tylergannon/a927c213cd97ce656016bf9aaa326231"
        private val mutex = Mutex()


        fun loadLocal(context: Context) =
            File(context.filesDir, FILENAME).run {
                _current = if (exists()) {
                    Json(JsonConfiguration.Stable).parse(serializer(), readText())
                } else {
                    defaultConfiguration
                }
            }

        suspend fun refresh(context: Context) {
            context.filesDir

            val (_, _, result) = Fuel.get(URL)
                .awaitObjectResponseResult(kotlinxDeserializerOf(loader = serializer()))
            result.fold(
                success = {
                    mutex.withLock {
                        _current = it
                        File(context.filesDir, FILENAME).writeText(
                            Json(JsonConfiguration.Stable).stringify(serializer(), it)
                        )
                    }
                },
                failure = {
                    loadLocal(context)
                }
            )
        }
    }
}

