package edu.amrita.amritacafe.printer

import java.lang.IllegalArgumentException

enum class ErrorStatus(val code: Int, val message: String? = null) {
    ParameterError(1),
    ConnectionError(2, "Failed to communicate/connect with printer. Run disconnect/connect."),
    TimeoutError(3, "Timed out trying to communicate with printer."),
    MemoryError(4),
    IllegalUseError(5, "Illegal operation: tried to connect while already connected, etc."),
    ProcessingError(6, "Process could not be run bc a similar process being run by another thread."),
    UnsupporedError(7),
    NotFoundError(8, "Printer not found. Check connection type / address."),
    InUseError(9, "In use. Stop using device from another application."),
    InvalidTypeError(10),
    DisconnectError(11, "Failed to disconnect."),
    AlreadyOpenedError(12),
    AlreadyUsedError(13),
    BoxOverError(14),
    CountOverError(15),
    FailureError(16, "An unknown error occurred.");

    companion object {
        fun fromCode(statusCode: Int) =
            values().find { it.code == statusCode }
                ?: throw IllegalArgumentException("Unknown status code $statusCode")
    }
}