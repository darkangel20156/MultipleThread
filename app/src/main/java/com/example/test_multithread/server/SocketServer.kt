package com.example.test_multithread.server


import android.util.Log
import android.widget.Toast
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.net.ServerSocket
import java.net.Socket
import java.util.concurrent.Executors
/**
 * Socket server
 */
object SocketServer {

    private val TAG = SocketServer::class.java.simpleName

    private const val SOCKET_PORT = 9527

    private var socket: Socket? = null
    private var serverSocket: ServerSocket? = null

    private lateinit var mCallback: ServerCallback

    private lateinit var outputStream: OutputStream

    private var result = false

    private val clientThreads = mutableListOf<ServerThread>()

    // test
    private val executorService = Executors.newFixedThreadPool(10)
    private val serverExecutor = Executors.newSingleThreadExecutor()
    /**
     * Start service
     */

//        fun startServer(callback: ServerCallback): Boolean {
//        mCallback = callback
//        Thread {
//            try {
//                serverSocket = ServerSocket(SOCKET_PORT)
//                while (result) {
//                    socket = serverSocket?.accept()
//                    mCallback.otherMsg("${socket?.inetAddress} to connected")
//                    val clientThread = ServerThread(socket!!, mCallback)
//                    clientThreads.add(clientThread)
//                    Log.d(TAG, "the value of clientThreads: $clientThreads")
//
////                    Log.d(TAG, "the value of clientThreads:" + clientThreads )
//                    println("the value of clientThreads: $clientThreads")
//                    clientThread.start()
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                result = false
//            }
//        }.start()
//        return result
//    }

    fun startServer(callback: ServerCallback): Boolean {
        mCallback = callback
        executorService.execute {
            try {
                serverSocket = ServerSocket(SOCKET_PORT)
                while (result) {
                    socket = serverSocket?.accept()
                    mCallback.otherMsg("${socket?.inetAddress} to connected")
                    val clientThread = ServerThread(socket!!, mCallback)
                    clientThreads.add(clientThread)
                    Log.d(TAG, "The value of clientThreads: " + clientThreads);
                    print("value: $clientThreads")
                    clientThread.start()
                }
            } catch (e: IOException) {
                e.printStackTrace()
                result = false
            }
        }
        return result
    }

//    fun startServer(callback: ServerCallback): Boolean {
//        mCallback = callback
//
//        try {
//            serverSocket = ServerSocket(SOCKET_PORT)
//            isRunning = true
//
//            while (isRunning) {
//                val socket = serverSocket?.accept()
//                socket?.let {
//                    mCallback.otherMsg("${socket.inetAddress} connected")
//                    val clientThread = ServerThread(socket, mCallback)
//                    clientThreads.add(clientThread)
//                    clientThread.start()
//                }
//            }
//
//        } catch (e: IOException) {
//            e.printStackTrace()
//            isRunning = false
//        } finally {
//            stopServer()
//        }
//
//        return isRunning
//    }

    /**
     * Close service
     */
//    fun stopServer() {
//        result = false
//
//        for (clientThread in clientThreads) {
//            clientThread.stopThread()
//        }
//
//        serverSocket?.close()
//        clientThreads.clear()
//    }

    fun stopServer() {
        serverExecutor.shutdownNow()
        executorService.shutdownNow()
        // Rest of your cleanup code...
    }

    /**
     * Send to all clients
     */
    fun sendToAllClients(msg: String) {
        for (clientThread in clientThreads) {
            clientThread.sendToClient(msg)
        }
    }

//    class ServerThread(private val socket: Socket, private val callback: ServerCallback) :
//        Thread() {
//
//        private lateinit var outputStream: OutputStream
//        private lateinit var inputStream: InputStream
//        private var running = true
//
//        override fun run() {
//            try {
//                outputStream = socket.getOutputStream()
//                inputStream = socket.getInputStream()
//
//                val buffer = ByteArray(1024)
//                var len = 0
//                var receiveStr = ""
//
//                while (running && inputStream.read(buffer).also { len = it } != -1) {
//                    receiveStr += String(buffer, 0, len, Charsets.UTF_8)
//                    if (len < 1024) {
//                        callback.receiveClientMsg(true, receiveStr)
//                        receiveStr = ""
//                    }
//                }
//            } catch (e: IOException) {
//                e.printStackTrace()
//                callback.receiveClientMsg(false, "")
//            } finally {
//                stopThread()
//            }
//        }
//
////        fun sendToClient(msg: String) {
////            Thread {
////                if (socket.isClosed) {
////                    Log.e(TAG, "sendToClient: Socket is closed")
////                    return@Thread
////                }
////
////                try {
////                    outputStream.write(msg.toByteArray())
////                    outputStream.flush()
////                    mCallback.otherMsg("toClient: $msg")
////                    Log.d(TAG, "Sent to client successfully")
////                } catch (e: IOException) {
////                    e.printStackTrace()
////                    Log.e(TAG, "Failed to send message to client")
////                }
////            }.start()
////        }
//
//        fun sendToClient(msg: String) {
//            Thread {
//                if (socket!!.isClosed) {
//                    Log.e(TAG, "sendToClient: Socket is closed")
//                    return@Thread
//                }
//                outputStream = socket!!.getOutputStream()
//                try {
//                    outputStream.write(msg.toByteArray())
//                    outputStream.flush()
//                    mCallback.otherMsg("toClient: $msg")
//                    Log.d(TAG, "Sent to client successfully")
//                } catch (e: IOException) {
//                    e.printStackTrace()
//                    Log.e(TAG, "Failed to send message to client")
//                }
//            }.start()
//        }
//
//        fun stopThread() {
//            running = false
//            socket.close()
//        }
//    }

    class ServerThread(private val socket: Socket, private val callback: ServerCallback) : Thread() {

        private lateinit var outputStream: OutputStream
        private lateinit var inputStream: InputStream
        private var running = true

        override fun run() {
            try {
                outputStream = socket.getOutputStream()
                inputStream = socket.getInputStream()

                val buffer = ByteArray(1024)
                var len = 0
                var receiveStr = ""

                while (running && inputStream.read(buffer).also { len = it } != -1) {
                    receiveStr += String(buffer, 0, len, Charsets.UTF_8)
                    if (len < 1024) {
                        callback.receiveClientMsg(true, receiveStr)
                        receiveStr = ""
                    }
                }
            } catch (e: IOException) {
                e.printStackTrace()
                callback.receiveClientMsg(false, "")
            } finally {
                closeResources()
            }
        }

        fun sendToClient(msg: String) {
            Thread {
                if (socket.isClosed) {
                    Log.e(TAG, "sendToClient: Socket is closed")
                    return@Thread
                }
                try {
                    outputStream.write(msg.toByteArray())
                    outputStream.flush()
                    callback.otherMsg("toClient: $msg")
                    Log.d(TAG, "Sent to client successfully")
                } catch (e: IOException) {
                    e.printStackTrace()
                    Log.e(TAG, "Failed to send message to client")
                }
            }.start()
        }

        private fun closeResources() {
            try {
                socket.close()
                outputStream.close()
                inputStream.close()
            } catch (e: IOException) {
                e.printStackTrace()
                Log.e(TAG, "Failed to close socket: ${e.message}")
            }
        }

        fun stopThread() {
            running = false
        }
    }

}