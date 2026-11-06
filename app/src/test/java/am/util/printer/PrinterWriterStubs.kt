package am.util.printer

// Test-only stand-ins. The Hoin printer AAR references these classes (only from its
// printImage(), which the app never calls) but doesn't ship them. Android resolves them lazily
// so the app is fine; the JVM test runtime verifies eagerly and needs them to exist.
abstract class PrinterWriter
class PrinterWriter58mm : PrinterWriter()
class PrinterWriter80mm : PrinterWriter()
