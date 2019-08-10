package edu.amrita.jayadev.amritacafe.printer

import android.content.Context
import com.epson.epos2.Epos2CallbackCode
import com.epson.epos2.Epos2Exception
import edu.amrita.jayadev.amritacafe.R

object ShowMsg {
    fun showException(e: Exception, method: String, context: Context) {
        var msg = ""
        if (e is Epos2Exception) {
            msg = String.format(
                "%s\n\t%s\n%s\n\t%s",
                context.getString(R.string.title_err_code),
                getEposExceptionText(e.errorStatus),
                context.getString(R.string.title_err_method),
                method
            )
        } else {
            msg = e.toString()
        }
        show(msg, context)
    }

    fun showResult(code: Int, errMsg: String, context: Context) {
        var msg = ""
        if (errMsg.isEmpty()) {
            msg = String.format(
                "\t%s\n\t%s\n",
                context.getString(R.string.title_msg_result),
                getCodeText(code)
            )
        } else {
            msg = String.format(
                "\t%s\n\t%s\n\n\t%s\n\t%s\n",
                context.getString(R.string.title_msg_result),
                getCodeText(code),
                context.getString(R.string.title_msg_description),
                errMsg
            )
        }
        show(msg, context)
    }

    fun showMsg(msg: String, context: Context) {
        show(msg, context)
    }

    private fun show(msg: String, context: Context) {
//        val alertDialog = AlertDialog.Builder(context)
//        alertDialog.setMessage(msg)
//        alertDialog.setPositiveButton(
//            "OK",
//            DialogInterface.OnClickListener { dialog, whichButton -> return@OnClickListener })
//        alertDialog.create()
//        alertDialog.show()
        println("THIS IS THE MESSAGE: $msg" )
    }

    private fun getEposExceptionText(state: Int): String {
        var return_text = ""
        when (state) {
            Epos2Exception.ERR_PARAM -> return_text = "ERR_PARAM"
            Epos2Exception.ERR_CONNECT -> return_text = "ERR_CONNECT"
            Epos2Exception.ERR_TIMEOUT -> return_text = "ERR_TIMEOUT"
            Epos2Exception.ERR_MEMORY -> return_text = "ERR_MEMORY"
            Epos2Exception.ERR_ILLEGAL -> return_text = "ERR_ILLEGAL"
            Epos2Exception.ERR_PROCESSING -> return_text = "ERR_PROCESSING"
            Epos2Exception.ERR_NOT_FOUND -> return_text = "ERR_NOT_FOUND"
            Epos2Exception.ERR_IN_USE -> return_text = "ERR_IN_USE"
            Epos2Exception.ERR_TYPE_INVALID -> return_text = "ERR_TYPE_INVALID"
            Epos2Exception.ERR_DISCONNECT -> return_text = "ERR_DISCONNECT"
            Epos2Exception.ERR_ALREADY_OPENED -> return_text = "ERR_ALREADY_OPENED"
            Epos2Exception.ERR_ALREADY_USED -> return_text = "ERR_ALREADY_USED"
            Epos2Exception.ERR_BOX_COUNT_OVER -> return_text = "ERR_BOX_COUNT_OVER"
            Epos2Exception.ERR_BOX_CLIENT_OVER -> return_text = "ERR_BOX_CLIENT_OVER"
            Epos2Exception.ERR_UNSUPPORTED -> return_text = "ERR_UNSUPPORTED"
            Epos2Exception.ERR_FAILURE -> return_text = "ERR_FAILURE"
            else -> return_text = String.format("%d", state)
        }
        return return_text
    }

    private fun getCodeText(state: Int): String {
        var return_text = ""
        when (state) {
            Epos2CallbackCode.CODE_SUCCESS -> return_text = "PRINT_SUCCESS"
            Epos2CallbackCode.CODE_PRINTING -> return_text = "PRINTING"
            Epos2CallbackCode.CODE_ERR_AUTORECOVER -> return_text = "ERR_AUTORECOVER"
            Epos2CallbackCode.CODE_ERR_COVER_OPEN -> return_text = "ERR_COVER_OPEN"
            Epos2CallbackCode.CODE_ERR_CUTTER -> return_text = "ERR_CUTTER"
            Epos2CallbackCode.CODE_ERR_MECHANICAL -> return_text = "ERR_MECHANICAL"
            Epos2CallbackCode.CODE_ERR_EMPTY -> return_text = "ERR_EMPTY"
            Epos2CallbackCode.CODE_ERR_UNRECOVERABLE -> return_text = "ERR_UNRECOVERABLE"
            Epos2CallbackCode.CODE_ERR_FAILURE -> return_text = "ERR_FAILURE"
            Epos2CallbackCode.CODE_ERR_NOT_FOUND -> return_text = "ERR_NOT_FOUND"
            Epos2CallbackCode.CODE_ERR_SYSTEM -> return_text = "ERR_SYSTEM"
            Epos2CallbackCode.CODE_ERR_PORT -> return_text = "ERR_PORT"
            Epos2CallbackCode.CODE_ERR_TIMEOUT -> return_text = "ERR_TIMEOUT"
            Epos2CallbackCode.CODE_ERR_JOB_NOT_FOUND -> return_text = "ERR_JOB_NOT_FOUND"
            Epos2CallbackCode.CODE_ERR_SPOOLER -> return_text = "ERR_SPOOLER"
            Epos2CallbackCode.CODE_ERR_BATTERY_LOW -> return_text = "ERR_BATTERY_LOW"
            Epos2CallbackCode.CODE_ERR_TOO_MANY_REQUESTS -> return_text = "ERR_TOO_MANY_REQUESTS"
            Epos2CallbackCode.CODE_ERR_REQUEST_ENTITY_TOO_LARGE -> return_text = "ERR_REQUEST_ENTITY_TOO_LARGE"
            Epos2CallbackCode.CODE_CANCELED -> return_text = "CODE_CANCELED"
            Epos2CallbackCode.CODE_ERR_NO_MICR_DATA -> return_text = "ERR_NO_MICR_DATA"
            Epos2CallbackCode.CODE_ERR_ILLEGAL_LENGTH -> return_text = "ERR_ILLEGAL_LENGTH"
            Epos2CallbackCode.CODE_ERR_NO_MAGNETIC_DATA -> return_text = "ERR_NO_MAGNETIC_DATA"
            Epos2CallbackCode.CODE_ERR_RECOGNITION -> return_text = "ERR_RECOGNITION"
            Epos2CallbackCode.CODE_ERR_READ -> return_text = "ERR_READ"
            Epos2CallbackCode.CODE_ERR_NOISE_DETECTED -> return_text = "ERR_NOISE_DETECTED"
            Epos2CallbackCode.CODE_ERR_PAPER_JAM -> return_text = "ERR_PAPER_JAM"
            Epos2CallbackCode.CODE_ERR_PAPER_PULLED_OUT -> return_text = "ERR_PAPER_PULLED_OUT"
            Epos2CallbackCode.CODE_ERR_CANCEL_FAILED -> return_text = "ERR_CANCEL_FAILED"
            Epos2CallbackCode.CODE_ERR_PAPER_TYPE -> return_text = "ERR_PAPER_TYPE"
            Epos2CallbackCode.CODE_ERR_WAIT_INSERTION -> return_text = "ERR_WAIT_INSERTION"
            Epos2CallbackCode.CODE_ERR_ILLEGAL -> return_text = "ERR_ILLEGAL"
            Epos2CallbackCode.CODE_ERR_INSERTED -> return_text = "ERR_INSERTED"
            Epos2CallbackCode.CODE_ERR_WAIT_REMOVAL -> return_text = "ERR_WAIT_REMOVAL"
            Epos2CallbackCode.CODE_ERR_DEVICE_BUSY -> return_text = "ERR_DEVICE_BUSY"
            Epos2CallbackCode.CODE_ERR_IN_USE -> return_text = "ERR_IN_USE"
            Epos2CallbackCode.CODE_ERR_CONNECT -> return_text = "ERR_CONNECT"
            Epos2CallbackCode.CODE_ERR_DISCONNECT -> return_text = "ERR_DISCONNECT"
            Epos2CallbackCode.CODE_ERR_MEMORY -> return_text = "ERR_MEMORY"
            Epos2CallbackCode.CODE_ERR_PROCESSING -> return_text = "ERR_PROCESSING"
            Epos2CallbackCode.CODE_ERR_PARAM -> return_text = "ERR_PARAM"
            else -> return_text = String.format("%d", state)
        }
        return return_text
    }
}
