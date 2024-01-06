package com.example.test_multithread.client


import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.Socket
import java.util.concurrent.Executors

/**
 * Socket client
 */
object SocketClient {

    private const val TAG = "SocketClient"
    private const val SOCKET_PORT = 9527

    private var socket: Socket? = null
    private var outputStream: OutputStream? = null

    private lateinit var mCallback: ClientCallback

    /**
     * Connection services
     */
    fun connectServer(ipAddress: String, callback: ClientCallback) {
        mCallback = callback
        Thread {
            try {
                socket = Socket(ipAddress, SOCKET_PORT)
                ClientThread(socket!!, mCallback).start()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }.start()
    }

    /**
     * Close the connection
     */
    fun closeConnect() {
        try {
            outputStream?.close()
            socket?.apply {
                shutdownInput()
                shutdownOutput()
                close()
            }
            Log.d(TAG, "Close connection")
        } catch (e: IOException) {
            e.printStackTrace()
            Log.e(TAG, "Failed to close connection: ${e.message}")
        }
    }

    /**
     * Send data to server
     * @param msg The string to be sent to the server
     */
    fun sendToServer(msg: String) {
        Executors.newCachedThreadPool().execute {
            if (socket?.isClosed == true) {
                Log.e(TAG, "sendToServer: Socket is closed")
                return@execute
            }

            try {
                outputStream = socket?.getOutputStream()
                outputStream?.write(msg.toByteArray())
                outputStream?.flush()
                mCallback.otherMsg("toServer: $msg")
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to send message to server: ${e.message}")
            }
        }
    }

    class ClientThread(private val socket: Socket, private val callback: ClientCallback) : Thread() {

        override fun run() {
            val inputStream: InputStream?
            try {
                inputStream = socket.getInputStream()
                val buffer = ByteArray(1024)
                var len: Int
                var receiveStr = ""

                while (inputStream.read(buffer).also { len = it } != -1) {
                    receiveStr += String(buffer, 0, len, Charsets.UTF_8)
                    if (len < 1024) {
                        callback.receiveServerMsg(receiveStr)
                        receiveStr = ""
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Error in client thread: ${e.message}")
                callback.receiveServerMsg("")
            } finally {
                closeResources()
            }
        }

        private fun closeResources() {
            try {
                socket.close()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to close socket: ${e.message}")
            }
        }
    }
}
