package edu.amrita.jayadev.amritacafe.printer

import android.app.Activity
import android.graphics.Color
import android.view.View
import android.widget.ProgressBar
import android.widget.TextView
import com.epson.epos2.Epos2Exception
import com.epson.epos2.printer.Printer
import com.epson.epos2.printer.PrinterStatusInfo
import com.epson.epos2.printer.ReceiveListener
import edu.amrita.jayadev.amritacafe.R
import edu.amrita.jayadev.amritacafe.model.OrderAdapter

class Printer(
    val mContext: Activity,
    val printerIP: String,
    val feedBackTV: TextView,
    val progressBar: ProgressBar
) : ReceiveListener {

    init {
        mContext.runOnUiThread {
            feedBackTV.visibility = View.INVISIBLE
            progressBar.visibility = View.VISIBLE
        }
    }

    var mPrinter: Printer? = null

    override fun onPtrReceive(p0: Printer?, code: Int, status: PrinterStatusInfo, p3: String?) {
        mContext.runOnUiThread(Runnable {
            ShowMsg.showResult(code, makeErrorMessage(status), mContext, this::setMessageInDialog)

            dispPrinterWarnings(status)

//            setMessageInDialog(showResult)

//            updateButtonState(true)

            Thread(Runnable { disconnectPrinter() }).start()
        })
    }

    fun setMessageInDialog(message: String) {
        mContext.runOnUiThread {
            feedBackTV.text = message
            feedBackTV.visibility = View.VISIBLE
            progressBar.visibility = View.INVISIBLE
            if (message == "PRINT_SUCCESS")
                feedBackTV.setTextColor(Color.GREEN)
            else
                feedBackTV.setTextColor(Color.RED)
        }
    }

    fun runPrintReceiptSequence(
        orderList: MutableList<OrderAdapter.OrderItem>,
        orderNumber: Int,
        totalToPay: Int
    ): Boolean {
        if (!initializeObject()) {
            return false
        }

        if (!createReceiptData(orderList, orderNumber, totalToPay)) {
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
            mPrinter = Printer(Printer.TM_M30, Printer.MODEL_ANK, mContext)
        } catch (e: Exception) {
            ShowMsg.showException(e, "Printer", mContext, this::setMessageInDialog)
            return false
        }

        mPrinter!!.setReceiveEventListener(this)

        return true
    }

    private fun createReceiptData(
        orderList: MutableList<OrderAdapter.OrderItem>,
        orderNumber: Int,
        totalToPay: Int
    ): Boolean {
        var method = ""

        if (mPrinter == null) {
            return false
        }

        try {
            mPrinter!!.addTextSize(3, 3)
            mPrinter!!.addText("ORDER: $orderNumber\n")
            mPrinter!!.addTextSize(2, 2)
            mPrinter!!.addFeedLine(1)

            val textData: StringBuilder = StringBuilder()
            orderList.forEach {
                val countAndName = "${it.amount} ${it.name}"
                val priceString = "${it.totPrice}"
                textData.append(getLineWithSpaces(countAndName, priceString))
                if (it.comment != "")
                    textData.append("   ${it.comment}\n")
                textData.append("\n")
            }
            mPrinter!!.addText(textData.toString())

            mPrinter!!.addText(getLineWithSpaces("TOTAL", "$totalToPay"))
            mPrinter!!.addFeedLine(1)

            method = "addCut"
            mPrinter!!.addCut(Printer.CUT_FEED)
        } catch (e: Exception) {
            ShowMsg.showException(e, method, mContext, this::setMessageInDialog)
            return false
        }

        return true
    }

    private fun getLineWithSpaces(countAndName: String, priceString: String): String {
        val textData: StringBuilder = StringBuilder()

        val size = countAndName.length + priceString.length
        val missingSpace = 20 - size
        textData.append(countAndName)
        for (i in 1..missingSpace) {
            textData.append(" ")
        }
        textData.append(priceString)
        textData.append("\n")

        return textData.toString()
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

        val status = mPrinter!!.status

        dispPrinterWarnings(status)

        if (!isPrintable(status)) {
            ShowMsg.showMsg(makeErrorMessage(status), mContext, this::setMessageInDialog)
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
            ShowMsg.showException(e, "sendData", mContext, this::setMessageInDialog)
            try {
                mPrinter!!.disconnect()
            } catch (ex: Exception) {
                ex.printStackTrace()
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
        var warningsMsg = "???????????"

        if (status == null) {
            return
        }

        if (status.paper == Printer.PAPER_NEAR_END) {
            warningsMsg += mContext.getString(R.string.handlingmsg_warn_receipt_near_end)
        }

        if (status.batteryLevel == Printer.BATTERY_LEVEL_1) {
            warningsMsg += mContext.getString(R.string.handlingmsg_warn_battery_near_end)
        }


        println("THIS IS THE TOAST: $warningsMsg" )
//        edtWarnings.setText(warningsMsg)
//        Toast.makeText(mContext, warningsMsg, Toast.LENGTH_LONG).show()
    }

    private fun connectPrinter(): Boolean {
        var isBeginTransaction = false

        if (mPrinter == null) {
            return false
        }

        try {
            mPrinter!!.connect("TCP:" + printerIP, Printer.PARAM_DEFAULT)
        } catch (e: Exception) {
            ShowMsg.showException(e, "connect", mContext, this::setMessageInDialog)
            return false
        }

        try {
            mPrinter!!.beginTransaction()
            isBeginTransaction = true
        } catch (e: Exception) {
            ShowMsg.showException(e, "beginTransaction", mContext, this::setMessageInDialog)
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
            mContext.runOnUiThread(Runnable {
                ShowMsg.showException(
                    e,
                    "endTransaction",
                    mContext,
                    this::setMessageInDialog
                )
            })
        }

        try {
            mPrinter!!.disconnect()
        } catch (e: Exception) {
            mContext.runOnUiThread(Runnable {
                ShowMsg.showException(
                    e,
                    "disconnect",
                    mContext,
                    this::setMessageInDialog
                )
            })
        }

        finalizeObject()
    }
}