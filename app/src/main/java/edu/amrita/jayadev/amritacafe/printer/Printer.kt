package edu.amrita.jayadev.amritacafe.printer

import android.app.Activity
import android.graphics.BitmapFactory
import android.widget.Toast
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import edu.amrita.jayadev.amritacafe.R

public class Printer(val mContext: Activity) : ReceiveListener {

    var mPrinter: Printer? = null

    override fun onPtrReceive(p0: Printer?, code: Int, status: PrinterStatusInfo, p3: String?) {
        mContext.runOnUiThread(Runnable {
            ShowMsg.showResult(code, makeErrorMessage(status), mContext)

            dispPrinterWarnings(status)

//            updateButtonState(true)

            Thread(Runnable { disconnectPrinter() }).start()
        })
    }

    fun runPrintReceiptSequence(): Boolean {
        if (!initializeObject()) {
            return false
        }

        if (!createReceiptData()) {
            finalizeObject()
            return false
        }

        if (!printData()) {
            finalizeObject()
            return false
        }

        return true
    }

    private fun initializeObject(): Boolean {
        try {
            mPrinter = Printer(Printer.TM_M30,Printer.MODEL_ANK,mContext)
        } catch (e: Exception) {
            ShowMsg.showException(e, "Printer", mContext)
            return false
        }

        mPrinter!!.setReceiveEventListener(this)

        return true
    }

    private fun createReceiptData(): Boolean {
        var method = ""
        val logoData = BitmapFactory.decodeResource(mContext.getResources(), R.drawable.store)
        var textData: StringBuilder? = StringBuilder()
        val barcodeWidth = 2
        val barcodeHeight = 100

        if (mPrinter == null) {
            return false
        }

        try {
            method = "addTextAlign"
            mPrinter!!.addTextAlign(Printer.ALIGN_CENTER)

            method = "addImage"
            mPrinter!!.addImage(
                logoData, 0, 0,
                logoData.width,
                logoData.height,
                Printer.COLOR_1,
                Printer.MODE_MONO,
                Printer.HALFTONE_DITHER,
                Printer.PARAM_DEFAULT.toDouble(),
                Printer.COMPRESS_AUTO
            )

            method = "addFeedLine"
            mPrinter!!.addFeedLine(1)
            textData!!.append("THE STORE 123 (555) 555 – 5555\n")
            textData.append("STORE DIRECTOR – John Smith\n")
            textData.append("\n")
            textData.append("7/01/07 16:58 6153 05 0191 134\n")
            textData.append("ST# 21 OP# 001 TE# 01 TR# 747\n")
            textData.append("------------------------------\n")
            method = "addText"
            mPrinter!!.addText(textData.toString())
            textData.delete(0, textData.length)

            textData.append("400 OHEIDA 3PK SPRINGF  9.99 R\n")
            textData.append("410 3 CUP BLK TEAPOT    9.99 R\n")
            textData.append("445 EMERIL GRIDDLE/PAN 17.99 R\n")
            textData.append("438 CANDYMAKER ASSORT   4.99 R\n")
            textData.append("474 TRIPOD              8.99 R\n")
            textData.append("433 BLK LOGO PRNTED ZO  7.99 R\n")
            textData.append("458 AQUA MICROTERRY SC  6.99 R\n")
            textData.append("493 30L BLK FF DRESS   16.99 R\n")
            textData.append("407 LEVITATING DESKTOP  7.99 R\n")
            textData.append("441 **Blue Overprint P  2.99 R\n")
            textData.append("476 REPOSE 4PCPM CHOC   5.49 R\n")
            textData.append("461 WESTGATE BLACK 25  59.99 R\n")
            textData.append("------------------------------\n")
            method = "addText"
            mPrinter!!.addText(textData.toString())
            textData.delete(0, textData.length)

            textData.append("SUBTOTAL                160.38\n")
            textData.append("TAX                      14.43\n")
            method = "addText"
            mPrinter!!.addText(textData.toString())
            textData.delete(0, textData.length)

            method = "addTextSize"
            mPrinter!!.addTextSize(2, 2)
            method = "addText"
            mPrinter!!.addText("TOTAL    174.81\n")
            method = "addTextSize"
            mPrinter!!.addTextSize(1, 1)
            method = "addFeedLine"
            mPrinter!!.addFeedLine(1)

            textData.append("CASH                    200.00\n")
            textData.append("CHANGE                   25.19\n")
            textData.append("------------------------------\n")
            method = "addText"
            mPrinter!!.addText(textData.toString())
            textData.delete(0, textData.length)

            textData.append("Purchased item total number\n")
            textData.append("Sign Up and Save !\n")
            textData.append("With Preferred Saving Card\n")
            method = "addText"
            mPrinter!!.addText(textData.toString())
            textData.delete(0, textData.length)
            method = "addFeedLine"
            mPrinter!!.addFeedLine(2)

            method = "addBarcode"
            mPrinter!!.addBarcode(
                "01209457",
                Printer.BARCODE_CODE39,
                Printer.HRI_BELOW,
                Printer.FONT_A,
                barcodeWidth,
                barcodeHeight
            )

            method = "addCut"
            mPrinter!!.addCut(Printer.CUT_FEED)
        } catch (e: Exception) {
            ShowMsg.showException(e, method, mContext)
            return false
        }

        textData = null

        return true
    }

    private fun finalizeObject() {
        if (mPrinter == null) {
            return
        }

        mPrinter!!.clearCommandBuffer()

        mPrinter!!.setReceiveEventListener(null)

        mPrinter = null
    }

    private fun printData(): Boolean {
        if (mPrinter == null) {
            return false
        }

        if (!connectPrinter()) {
            return false
        }

        val status = mPrinter!!.getStatus()

        dispPrinterWarnings(status)

        if (!isPrintable(status)) {
            ShowMsg.showMsg(makeErrorMessage(status), mContext)
            try {
                mPrinter!!.disconnect()
            } catch (ex: Exception) {
                // Do nothing
            }

            return false
        }

        try {
            mPrinter!!.sendData(Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            ShowMsg.showException(e, "sendData", mContext)
            try {
                mPrinter!!.disconnect()
            } catch (ex: Exception) {
                // Do nothing
            }

            return false
        }

        return true
    }

    private fun makeErrorMessage(status: PrinterStatusInfo): String {
        var msg = ""

        if (status.online == Printer.FALSE) {
            msg += mContext.getString(R.string.handlingmsg_err_offline)
        }
        if (status.connection == Printer.FALSE) {
            msg += mContext.getString(R.string.handlingmsg_err_no_response)
        }
        if (status.coverOpen == Printer.TRUE) {
            msg += mContext.getString(R.string.handlingmsg_err_cover_open)
        }
        if (status.paper == Printer.PAPER_EMPTY) {
            msg += mContext.getString(R.string.handlingmsg_err_receipt_end)
        }
        if (status.paperFeed == Printer.TRUE || status.panelSwitch == Printer.SWITCH_ON) {
            msg += mContext.getString(R.string.handlingmsg_err_paper_feed)
        }
        if (status.errorStatus == Printer.MECHANICAL_ERR || status.errorStatus == Printer.AUTOCUTTER_ERR) {
            msg += mContext.getString(R.string.handlingmsg_err_autocutter)
            msg += mContext.getString(R.string.handlingmsg_err_need_recover)
        }
        if (status.errorStatus == Printer.UNRECOVER_ERR) {
            msg += mContext.getString(R.string.handlingmsg_err_unrecover)
        }
        if (status.errorStatus == Printer.AUTORECOVER_ERR) {
            if (status.autoRecoverError == Printer.HEAD_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat)
                msg += mContext.getString(R.string.handlingmsg_err_head)
            }
            if (status.autoRecoverError == Printer.MOTOR_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat)
                msg += mContext.getString(R.string.handlingmsg_err_motor)
            }
            if (status.autoRecoverError == Printer.BATTERY_OVERHEAT) {
                msg += mContext.getString(R.string.handlingmsg_err_overheat)
                msg += mContext.getString(R.string.handlingmsg_err_battery)
            }
            if (status.autoRecoverError == Printer.WRONG_PAPER) {
                msg += mContext.getString(R.string.handlingmsg_err_wrong_paper)
            }
        }
        if (status.batteryLevel == Printer.BATTERY_LEVEL_0) {
            msg += mContext.getString(R.string.handlingmsg_err_battery_real_end)
        }

        return msg
    }

    private fun isPrintable(status: PrinterStatusInfo?): Boolean {
        if (status == null) {
            return false
        }

        if (status.connection == Printer.FALSE) {
            return false
        } else if (status.online == Printer.FALSE) {
            return false
        } else {
        }//print available

        return true
    }

    private fun dispPrinterWarnings(status: PrinterStatusInfo?) {
//        val edtWarnings = findViewById<View>(R.id.edtWarnings) as EditText
        var warningsMsg = ""

        if (status == null) {
            return
        }

        if (status.paper == Printer.PAPER_NEAR_END) {
            warningsMsg += mContext.getString(R.string.handlingmsg_warn_receipt_near_end)
        }

        if (status.batteryLevel == Printer.BATTERY_LEVEL_1) {
            warningsMsg += mContext.getString(R.string.handlingmsg_warn_battery_near_end)
        }

//        edtWarnings.setText(warningsMsg)
        Toast.makeText(mContext,warningsMsg,Toast.LENGTH_LONG).show()
    }

    private fun connectPrinter(): Boolean {
        var isBeginTransaction = false

        if (mPrinter == null) {
            return false
        }

        try {
            mPrinter!!.connect("TCP:192.168.0.11", Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            ShowMsg.showException(e, "connect", mContext)
            return false
        }

        try {
            mPrinter!!.beginTransaction()
            isBeginTransaction = true
        } catch (e: Exception) {
            ShowMsg.showException(e, "beginTransaction", mContext)
        }

        if (isBeginTransaction == false) {
            try {
                mPrinter!!.disconnect()
            } catch (e: Epos2Exception) {
                // Do nothing
                return false
            }

        }

        return true
    }

    private fun disconnectPrinter() {
        if (mPrinter == null) {
            return
        }

        try {
            mPrinter!!.endTransaction()
        } catch (e: Exception) {
            mContext.runOnUiThread(Runnable { ShowMsg.showException(e, "endTransaction", mContext) })
        }

        try {
            mPrinter!!.disconnect()
        } catch (e: Exception) {
            mContext.runOnUiThread(Runnable { ShowMsg.showException(e, "disconnect", mContext) })
        }

        finalizeObject()
    }
}