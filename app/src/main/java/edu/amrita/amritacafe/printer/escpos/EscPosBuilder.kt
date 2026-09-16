package edu.amrita.amritacafe.printer.escpos

import java.io.ByteArrayOutputStream
import java.nio.charset.Charset

class EscPosBuilder(private val charset: Charset = Charsets.UTF_8) {

    private val stream = ByteArrayOutputStream()

    init {
        initPrinter()
    }

    fun initPrinter(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x40)) // ESC @
        return this
    }

    fun alignLeft(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x61, 0x00)) // ESC a 0
        return this
    }

    fun alignCenter(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x61, 0x01)) // ESC a 1
        return this
    }

    fun alignRight(): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x61, 0x02)) // ESC a 2
        return this
    }

    fun textSize(widthMultiplier: Int = 1, heightMultiplier: Int = 1): EscPosBuilder {
        val w = (widthMultiplier.coerceIn(1, 8) - 1) and 0x07
        val h = (heightMultiplier.coerceIn(1, 8) - 1) and 0x07
        val n = ((w shl 4) or h).toByte()
        stream.write(byteArrayOf(0x1D, 0x21, n)) // GS ! n
        return this
    }

    fun bold(enable: Boolean): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x45, if (enable) 0x01 else 0x00)) // ESC E n
        return this
    }

    fun underline(enable: Boolean): EscPosBuilder {
        stream.write(byteArrayOf(0x1B, 0x2D, if (enable) 0x01 else 0x00)) // ESC - n
        return this
    }


    fun feedLines(count: Int = 1): EscPosBuilder {
        if (count > 0) {
            val n = count.coerceIn(1, 255).toByte()
            stream.write(byteArrayOf(0x1B, 0x64, n)) // ESC d n
        }
        return this
    }

    fun text(content: String): EscPosBuilder {
        stream.write(content.toByteArray(charset))
        return this
    }

    fun line(content: String = ""): EscPosBuilder {
        if (content.isNotEmpty()) {
            stream.write(content.toByteArray(charset))
        }
        stream.write(0x0A) // LF (\n)
        return this
    }

    fun horizontalLine(char: Char = '-', length: Int = 32): EscPosBuilder {
        val lineStr = char.toString().repeat(length)
        return line(lineStr)
    }

    fun cut(feedLines: Int = 1): EscPosBuilder {
        if (feedLines > 0) {
            feedLines(feedLines)
        }
        stream.write(byteArrayOf(0x1D, 0x56, 0x42, 0x00)) // GS V 66 0 (feed paper and partial cut)
        return this
    }

    fun build(): ByteArray = stream.toByteArray()
}
