package edu.amrita.amritacafe.printer

import java.lang.IllegalArgumentException

enum class CompletedJobStatus(val code: Int, val message: String) {
    Success(0, "Success"),
    Printing(1, "Printing"),
    AutorecoverError(2, "Auto-recover error. Let printer cool down or open+close cover."),
    CoverOpenError(3, "Cover open."),
    CutterError(4, "Auto-cutter Error."),
    MechanicalError(5, "Mechanical Error."),
    EmptyError(6, "Paper is empty."),
    UnrecoverableError(7, "Unrecoverable Error. Power off and then on the printer."),
    FailureError(8, "Failure..."),
    NotFoundError(9, "The connection type and/or IP address are not correct, or device offline."),
    SystemError(10, "System Error. Reboot tablet and printer."),
    PortError(11, "Connection Error"),
    TimeoutError(12, "Timeout error.");

    companion object {
        fun fromCode(callbackCode: Int) = values()
            .find { it.code == callbackCode }
            ?: throw IllegalArgumentException("Tried to translate callback code $callbackCode")
    }
}