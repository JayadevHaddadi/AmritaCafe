package edu.amrita.amritacafe.printer

import com.epson.epos2.Epos2CallbackCode
import java.lang.IllegalArgumentException

enum class CompletedJobStatus(val code: Int, val message: String) {
    Success(Epos2CallbackCode.CODE_SUCCESS, "Success"),
    Printing(Epos2CallbackCode.CODE_PRINTING, "Printing"),
    AutorecoverError(Epos2CallbackCode.CODE_ERR_AUTORECOVER,
        "Auto-recover error. Let printer cool down or open+close cover."),
    CoverOpenError(Epos2CallbackCode.CODE_ERR_COVER_OPEN, "Cover open."),
    CutterError(Epos2CallbackCode.CODE_ERR_CUTTER, "Auto-cutter Error."),
    MechanicalError(Epos2CallbackCode.CODE_ERR_MECHANICAL, "Mechanical Error."),
    EmptyError(Epos2CallbackCode.CODE_ERR_EMPTY, "Paper is empty."),
    UnrecoverableError(Epos2CallbackCode.CODE_ERR_UNRECOVERABLE, "Unrecoverable Error. " +
            "Power off and then on the printer."),
    FailureError(Epos2CallbackCode.CODE_ERR_FAILURE, "Failure..."),
    NotFoundError(Epos2CallbackCode.CODE_ERR_NOT_FOUND, "The connection type and/or IP " +
            "address are not correct, or device offline."),
    SystemError(Epos2CallbackCode.CODE_ERR_SYSTEM, "System Error. Reboot tablet and printer."),
    PortError(Epos2CallbackCode.CODE_ERR_PORT, "Connection Error"),
    TimeoutError(Epos2CallbackCode.CODE_ERR_TIMEOUT, "Timeout error.");

    companion object {
        fun fromCode(callbackCode: Int) = values()
            .find { it.code == callbackCode }
                ?:
                throw IllegalArgumentException("Tried to translate callback code $callbackCode")

    }
}