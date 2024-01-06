package com.example.test_multithread

import android.net.wifi.WifiManager
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Toast
import androidx.recyclerview.widget.LinearLayoutManager
import com.example.test_multithread.client.ClientCallback
import com.example.test_multithread.client.SocketClient
import com.example.test_multithread.databinding.ActivityMainBinding
import com.example.test_multithread.server.ServerCallback
import com.example.test_multithread.server.SocketServer

class MainActivity : AppCompatActivity(), ServerCallback, ClientCallback {

    private val TAG = MainActivity::class.java.simpleName

    private lateinit var binding: ActivityMainBinding

    //Whether it is currently the server
    private var isServer = true

    //Whether the Socket service is open
    private var openSocket = false

    //Is the Socket service connected?
    private var connectSocket = false

    //Message list
    private val messages = ArrayList<Message>()
    //Message adapter
    private lateinit var msgAdapter: MsgAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initView()
    }

    private fun initView() {
        binding.tvIpAddress.text = "Ip address: ${getIp()}"
        //Switch between server and client
        binding.rg.setOnCheckedChangeListener { _, checkedId ->
            isServer = when (checkedId) {
                R.id.rb_server -> true
                R.id.rb_client -> false
                else -> true
            }
            binding.layServer.visibility = if (isServer) View.VISIBLE else View.GONE
            binding.layClient.visibility = if (isServer) View.GONE else View.VISIBLE
            binding.etMsg.hint = if (isServer) "Send to client" else "Send to server"
        }
        //Open service/Close service Server-side processing
        binding.btnStartService.setOnClickListener {
            // Disable the button temporarily to prevent multiple clicks
            binding.btnStartService.isEnabled = false

            // Toggle the server state and perform the corresponding action
            openSocket = if (openSocket) {
                // If stopServer is synchronous, perform UI updates immediately
                SocketServer.stopServer()
                updateUIAfterServerStopped()
                false
            } else {
                // If startServer is synchronous, perform UI updates immediately
                // Note: You might want to handle errors or exceptions if any
                // during the startServer operation
                SocketServer.startServer(this)
                updateUIAfterServerStarted()
                true
            }

            Log.d(TAG, "openSocket: $openSocket")
        }
        //Connect service/disconnect client processing
        binding.btnConnectService.setOnClickListener {
            // Disable the button temporarily to prevent multiple clicks
            binding.btnConnectService.isEnabled = false

            // Check if the server is open before attempting to connect
            if (!openSocket) {
                showMsg("The service is not currently open")
                binding.btnConnectService.isEnabled = true
                return@setOnClickListener
            }

            connectSocket = if (connectSocket) {
                SocketClient.closeConnect()
                updateUIAfterConnectionClosed()
                false
            } else {
                val ip = binding.etIpAddress.text.toString()
                if (ip.isEmpty()) {
                    showMsg("Please enter the IP address")
                    binding.btnConnectService.isEnabled = true
                    return@setOnClickListener
                }

                SocketClient.connectServer(ip, this)
                updateUIAfterConnectionOpened()
                true
            }

            binding.btnConnectService.text = if (connectSocket) "Close the connection" else "Connect the service"
        }

        //Send message to server/client
        binding.btnSendMsg.setOnClickListener {
            val msg = binding.etMsg.text.toString().trim()
            if (msg.isEmpty()) {
                showMsg("Please enter the information to be sent")
                return@setOnClickListener
            }

            // Check whether the server is open
            if (!openSocket) {
                showMsg("The service is not currently open")
                return@setOnClickListener
            }

            // At this point, it's guaranteed that the server is open
            if (isServer) {
                SocketServer.sendToAllClients(msg)
            } else {
                Log.d(TAG, "the value of isServer: " + isServer)
                // Optionally handle client-side sending
                SocketClient.sendToServer(msg)
            }

            binding.etMsg.setText("")
            updateList(if (isServer) 1 else 2, msg)
        }

        //Initialization list
        msgAdapter = MsgAdapter(messages)
        binding.rvMsg.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = msgAdapter
        }
    }

    // test
    private fun updateUIAfterServerStopped() {
        runOnUiThread {
            showMsg("Service closed")
            binding.btnStartService.text = "Open the service"
            binding.btnStartService.isEnabled = true

            // Close the client connection when the server is stopped
            if (connectSocket) {
                SocketClient.closeConnect()
                updateUIAfterConnectionClosed()
            }
        }
    }


    private fun updateUIAfterServerStarted() {
        runOnUiThread {
            showMsg("Service opened")
            binding.btnStartService.text = "Close the service"
            binding.btnStartService.isEnabled = true
        }
    }

    private fun updateUIAfterConnectionClosed() {
        runOnUiThread {
            showMsg("Connection unsuccessfully")
            binding.btnConnectService.text = "Connect the service"
            binding.btnConnectService.isEnabled = true
        }
    }

    private fun updateUIAfterConnectionOpened() {
        runOnUiThread {
            showMsg("Connection successfully")
            binding.btnConnectService.text = "Close the connection"
            binding.btnConnectService.isEnabled = true
        }
    }

    private fun getIp() =
        intToIp((applicationContext.getSystemService(WIFI_SERVICE) as WifiManager).connectionInfo.ipAddress)

    private fun intToIp(ip: Int) =
        "${(ip and 0xFF)}.${(ip shr 8 and 0xFF)}.${(ip shr 16 and 0xFF)}.${(ip shr 24 and 0xFF)}"

    /**
     * Receive messages from the client
     */
    override fun receiveClientMsg(success: Boolean, msg: String) = updateList(2, msg)

    /**
     * Receive messages from the server
     */
    override fun receiveServerMsg(msg: String) = updateList(1, msg)


    override fun otherMsg(msg: String) {
        Log.d(TAG, msg)
    }

    /**
     * update list
     */
    private fun updateList(type: Int, msg: String) {
        messages.add(Message(type, msg))
        runOnUiThread {
            (if (messages.size == 0) 0 else messages.size - 1).apply {
                msgAdapter.notifyItemChanged(this)
                binding.rvMsg.smoothScrollToPosition(this)
            }
        }
    }

    private fun showMsg(msg: String) = Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
}