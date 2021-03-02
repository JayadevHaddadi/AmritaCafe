package edu.amrita.amritacafe.printer

import com.epson.epos2.Epos2Exception
import java.lang.IllegalArgumentException

enum class ErrorStatus(val code: Int, val message: String? = null) {
    ParameterError(Epos2Exception.ERR_PARAM),
    ConnectionError(Epos2Exception.ERR_CONNECT,
        "Failed to communicate/connect with printer.  Run disconnect/connect."),
    TimeoutError(Epos2Exception.ERR_TIMEOUT, "Timed out trying to communicate with printer."),
    MemoryError(Epos2Exception.ERR_MEMORY),
    IllegalUseError(Epos2Exception.ERR_ILLEGAL, "Illegal operation: tried to connect while already connected, etc."),
    ProcessingError(Epos2Exception.ERR_PROCESSING, "Process could not be run bc a similar process being run by another thread."),
    UnsupporedError(Epos2Exception.ERR_UNSUPPORTED),
    NotFoundError(Epos2Exception.ERR_NOT_FOUND, "Printer not found. Check connection type / address."),
    InUseError(Epos2Exception.ERR_IN_USE, "In use.  Stop using device from another application."),
    InvalidTypeError(Epos2Exception.ERR_TYPE_INVALID),
    DisconnectError(Epos2Exception.ERR_DISCONNECT, "Failed to disconnect."),
    AlreadyOpenedError(Epos2Exception.ERR_ALREADY_OPENED),
    AlreadyUsedError(Epos2Exception.ERR_ALREADY_USED),
    BoxOverError(Epos2Exception.ERR_BOX_COUNT_OVER),
    CountOverError(Epos2Exception.ERR_BOX_CLIENT_OVER),
    FailureError(Epos2Exception.ERR_FAILURE, "An unknown error occurred.");

    companion object {
        fun fromCode(statusCode: Int)
                = values().find { it.code == statusCode }
            ?:
                throw IllegalArgumentException()
    }
}