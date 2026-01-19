package cn.ratnoumi.bcardtools

import android.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Test
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        assertEquals(4, 2 + 2)
    }

    @Test
    fun dome01() {
        val s =
            byteArrayOf(0x50, 0x45, 0x54, 0x47, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00)
        print(bytesToStr(s))

        println(bytesToFloat(byteArrayOf(0x00, 0x00, 0xE0.toByte(), 0x3F)))
        println(bytesToInt(byteArrayOf(0x41, 0x00, 0x00, 0x00)))
        println(bytesToInt(byteArrayOf(0x45, 0x01)))

    }

    @Test
    fun dome02() {
        val hex = "504c4100000000000000000000000000"
        val bytes = ByteArray(hex.length / 2)
        for (i in 0..<bytes.size) {
            bytes[i] = hex.substring(i * 2, i * 2 + 2).toInt(16).toByte()
        }
        println(bytes.toHexString())
        println(bytesToStr(bytes))
    }

    @Test
    fun dome03() {

    }


    fun bytesToFloat(arr: ByteArray): Float {
        return ByteBuffer.wrap(arr).order(ByteOrder.LITTLE_ENDIAN).float
    }

    fun bytesToInt(arr: ByteArray, start: Int = 0): Int {
        return (arr[start + 1].toInt() shl 8) + arr[start]
    }


    fun bytesToStr(arr: ByteArray): String {
        val bytes = if (arr.indexOf(0) != -1) {
            arr.copyOf(arr.indexOf(0))
        } else {
            arr
        }
        return String(bytes, Charsets.US_ASCII)
    }

}