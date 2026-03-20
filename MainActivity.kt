package com.example.kasatoggle

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import kotlin.concurrent.thread

class MainActivity : AppCompatActivity() {

    private val kasaIp = "192.168.1.50"
    private val kasaPort = 9999

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        togglePlug()
        finish() // closes immediately after running
    }

    private fun togglePlug() {
        thread {
            try {
                val currentState = getCurrentState()
                val newState = if (currentState == 1) 0 else 1
                sendCommand("""{"system":{"set_relay_state":{"state":$newState}}}""")
            } catch (_: Exception) {}
        }
    }

    private fun getCurrentState(): Int {
        val response = sendCommand("""{"system":{"get_sysinfo":{}}}""")
        return if (response.contains("\"relay_state\":1")) 1 else 0
    }

    private fun sendCommand(command: String): String {
        val socket = Socket(kasaIp, kasaPort)
        val output: OutputStream = socket.getOutputStream()
        val input: InputStream = socket.getInputStream()

        val encrypted = encrypt(command)
        output.write(encrypted)

        val buffer = ByteArray(2048)
        val len = input.read(buffer)

        socket.close()

        return decrypt(buffer.copyOf(len))
    }

    private fun encrypt(input: String): ByteArray {
        var key = 171
        val result = ByteArray(input.length + 4)

        val len = input.length
        result[0] = (len shr 24).toByte()
        result[1] = (len shr 16).toByte()
        result[2] = (len shr 8).toByte()
        result[3] = len.toByte()

        for (i in input.indices) {
            val enc = input[i].code xor key
            key = enc
            result[i + 4] = enc.toByte()
        }
        return result
    }

    private fun decrypt(input: ByteArray): String {
        var key = 171
        val result = StringBuilder()

        for (i in 4 until input.size) {
            val dec = input[i].toInt() xor key
            key = input[i].toInt()
            result.append(dec.toChar())
        }
        return result.toString()
    }
}
