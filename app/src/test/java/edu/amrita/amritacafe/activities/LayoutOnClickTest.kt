package edu.amrita.amritacafe.activities

import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

/**
 * Every android:onClick="name" in a layout must have a matching `fun name(view: View)` in the
 * activity, or tapping it crashes ("Could not find method cancelOrder(View)", 80113).
 */
class LayoutOnClickTest {

    private val activities = listOf(MainActivity::class.java, SettingsActivity::class.java)

    @Test
    fun everyLayoutOnClickHasAHandler() {
        val resDir = File("src/main/res")
        assertTrue("res dir not found from ${File(".").absolutePath}", resDir.isDirectory)
        val regex = Regex("""android:onClick="([^"]+)"""")
        val missing = mutableListOf<String>()
        resDir.listFiles { f -> f.isDirectory && f.name.startsWith("layout") }!!.forEach { dir ->
            dir.listFiles { f -> f.extension == "xml" }!!.forEach { file ->
                regex.findAll(file.readText()).map { it.groupValues[1] }.forEach { name ->
                    val found = activities.any { cls ->
                        cls.methods.any { it.name == name && it.parameterTypes.contentEquals(arrayOf(android.view.View::class.java)) }
                    }
                    if (!found) missing += "${dir.name}/${file.name}: $name"
                }
            }
        }
        assertTrue("onClick handlers with no matching method: $missing", missing.isEmpty())
    }
}
