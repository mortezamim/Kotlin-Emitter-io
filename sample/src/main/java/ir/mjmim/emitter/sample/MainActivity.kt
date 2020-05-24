package ir.mjmim.emitter.sample

import android.annotation.SuppressLint
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import ir.mjmim.emitter.core.EmitterClient
import ir.mjmim.emitter.core.listeners.IConnectListener
import ir.mjmim.emitter.core.listeners.IGlobalListener
import kotlinx.android.synthetic.main.activity_main.*
import java.text.SimpleDateFormat
import java.util.*

class MainActivity : AppCompatActivity() {

    enum class ActionButtons {
        DISCONNECTED,
        CONNECTED,
        SUBSCRIBED
    }

    private val serverPath = "tcp://192.168.1.101:8080"
    private val topicName = "demo/"
    private val secretKey = "TWsZT5U-6CDZX5wxHuU26C-bT2GVPjyJ"

    private var client: EmitterClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    override fun onStart() {
        super.onStart()
        initListeners()
        updateActionButtons(ActionButtons.DISCONNECTED)

        initialClient()
    }

    private fun initListeners() {
        btnConnect.setOnClickListener {
            startConnect(serverPath)
        }
        btnSubscribe.setOnClickListener {
            subscribeTopic(secretKey, topicName)
        }
        clearHistory.setOnClickListener {
            MessageHistory.text = ""
        }
    }

    private fun initialClient() {
        if (client != null) return
        client = EmitterClient(this.applicationContext).enableLogging()
    }

    private fun startConnect(path: String) {
        client?.connect(path, object : IConnectListener {
            override fun onSuccess(url: String) {
                updateActionButtons(ActionButtons.CONNECTED)
            }

            override fun onFailure(url: String, exception: Throwable) {
                updateActionButtons(ActionButtons.DISCONNECTED)
            }
        })?.addGlobalCallback(object : IGlobalListener {
            override fun connectionLost(cause: Throwable?) {
                updateActionButtons(ActionButtons.DISCONNECTED)
            }

            @SuppressLint("SetTextI18n")
            override fun messageArrived(topic: String?, message: String?) {
                message?.let {
                    val sdf = SimpleDateFormat("hh:mm:ss", Locale.getDefault())
                    MessageHistory.text = "${MessageHistory.text}\n${sdf.format(Date())}: $it"
                }
            }

            override fun deliveryComplete(token: String?) {
            }
        })
    }

//    fun publishToTopic(client: EmitterClient, secretKey: String, topicName: String, s: String) {
//        client.publishMessage(secretKey, topicName, s)
//    }

    private fun subscribeTopic(secretKey: String, topicName: String) {
        client?.subscribeTopic(secretKey, topicName, object : IConnectListener {
            override fun onSuccess(url: String) {
                updateActionButtons(ActionButtons.SUBSCRIBED)
            }

            override fun onFailure(url: String, exception: Throwable) {
            }

        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateActionButtons(state: ActionButtons) {
        when (state) {
            ActionButtons.DISCONNECTED -> {
                btnConnect.isEnabled = true
                btnSubscribe.isEnabled = false
                btnConnect.text = "Connect to broker"
                btnSubscribe.text = "Subscribe to topic"
            }
            ActionButtons.CONNECTED -> {
                btnConnect.isEnabled = false
                btnSubscribe.isEnabled = true
                btnConnect.text = "Connected to $serverPath"
            }
            ActionButtons.SUBSCRIBED -> {
                btnConnect.isEnabled = false
                btnSubscribe.isEnabled = false
                btnSubscribe.text = "Subscribe to $topicName"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.dispose()
    }
}
