package edu.amrita.amritacafe.crash

import android.os.Looper
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class CrashHandlerTest {

    private fun npeAt(className: String) = NullPointerException("socket is null").apply {
        stackTrace = arrayOf(StackTraceElement(className, "run", "BluetoothService.java", 310))
    }

    private val worker = Thread({}, "AcceptThread")

    @Test
    fun hoinSdkWorkerThreadCrashIsContained() {
        assertTrue(CrashHandler.isSurvivablePrinterSdkCrash(worker, npeAt("com.hoin.btsdk.BluetoothService\$AcceptThread")))
        assertTrue(CrashHandler.isSurvivablePrinterSdkCrash(worker, npeAt("com.example.hoinprinterlib.module.BleUnit.BleUnit")))
    }

    @Test
    fun appCrashesAreNeverSwallowed() {
        assertFalse(CrashHandler.isSurvivablePrinterSdkCrash(worker, npeAt("edu.amrita.amritacafe.model.OrderAdapter")))
        assertFalse(CrashHandler.isSurvivablePrinterSdkCrash(worker, IndexOutOfBoundsException().apply {
            stackTrace = arrayOf(StackTraceElement("com.hoin.btsdk.BluetoothService", "x", "f", 1))
        }))
    }

    @Test
    fun mainThreadCrashesAreNeverSwallowed() {
        assertFalse(CrashHandler.isSurvivablePrinterSdkCrash(Looper.getMainLooper().thread, npeAt("com.hoin.btsdk.BluetoothService")))
    }
}
