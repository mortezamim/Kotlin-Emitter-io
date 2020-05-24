package ir.mjmim.emitter.sample

import android.annotation.SuppressLint
import android.graphics.Color
import android.os.Bundle
import android.view.KeyEvent
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
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
    var currentState = ActionButtons.DISCONNECTED

    private val serverPath = "tcp://192.168.1.101:8080"
    private val topicName = "demo/"
    private val secretKey = "TWsZT5U-6CDZX5wxHuU26C-bT2GVPjyJ"

    private var client: EmitterClient? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
    }

    @SuppressLint("SetTextI18n")
    override fun onStart() {
        super.onStart()
        initListeners()
        updateActionButtons(ActionButtons.DISCONNECTED)

        tvMessageHistory.text = "" +
                "Step 1: set 'serverPath' , 'topicName' , 'secretKey' in MainActivity\n" +
                "Note: If you have problem with these values check out 'https://emitter.io/develop/what-is-emitter/'\n\n" +
                "Step 2 - Connect to broker then send message \n\n" +
                "Step 3 - (optional) If you want to show messages in list subscribe to topic"
        tvMessageHistory.setTextColor(Color.parseColor("#EA8235"))
        initialClient()
    }

    private fun initListeners() {
        btnConnect.setOnClickListener {
            startConnect(serverPath)
            tvMessageHistory.text = ""
            tvMessageHistory.setTextColor(ContextCompat.getColor(this, R.color.colorAccent))
        }
        btnSubscribe.setOnClickListener {
            if (currentState == ActionButtons.SUBSCRIBED)
                unsubscribeTopic(secretKey, topicName)
                else
            subscribeTopic(secretKey, topicName)
        }
        clearHistory.setOnClickListener {
            tvMessageHistory.text = ""
        }
        btnSend.setOnClickListener {
            publishToTopic(secretKey, topicName, etMessage.text.toString())
        }

        etMessage.setOnKeyListener(object : View.OnKeyListener {
            override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
                if ((event?.action == KeyEvent.ACTION_DOWN) &&
                    (keyCode == KeyEvent.KEYCODE_ENTER)
                ) {
                    publishToTopic(secretKey, topicName, etMessage.text.toString())
                    return true
                }
                return false
            }

        })
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

            override fun onFailure(url: String, exception: Throwable?) {
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
                    tvMessageHistory.text = "${tvMessageHistory.text}\n${sdf.format(Date())}: $it"
                }
            }

            override fun deliveryComplete(token: String?) {
            }
        })
    }

    private fun publishToTopic(secretKey: String, topicName: String, s: String) {
        if (s.trim().isEmpty()) {
            etMessage.requestFocus()
            return
        }
        etMessage.setText("")

        client?.publishMessage(secretKey, topicName, s)
    }

    private fun subscribeTopic(secretKey: String, topicName: String) {
        client?.subscribeTopic(secretKey, topicName, object : IConnectListener {
            override fun onSuccess(url: String) {
                updateActionButtons(ActionButtons.SUBSCRIBED)
            }

            override fun onFailure(url: String, exception: Throwable?) {
            }

        })
    }

    private fun unsubscribeTopic(secretKey: String, topicName: String) {
        client?.unsubscribeTopic(secretKey, topicName, object : IConnectListener {
            override fun onSuccess(url: String) {
                updateActionButtons(ActionButtons.CONNECTED)
            }

            override fun onFailure(url: String, exception: Throwable?) {
            }

        })
    }

    @SuppressLint("SetTextI18n")
    private fun updateActionButtons(state: ActionButtons) {
        currentState = state
        when (state) {
            ActionButtons.DISCONNECTED -> {
                btnConnect.isEnabled = true
                btnSubscribe.isEnabled = false
                btnSend.isEnabled = false
                etMessage.isEnabled = false

                btnConnect.text = "Connect to broker"
                btnSubscribe.text = "Subscribe to topic"
            }
            ActionButtons.CONNECTED -> {
                btnConnect.isEnabled = false
                btnSubscribe.isEnabled = true
                btnSend.isEnabled = true
                etMessage.isEnabled = true
                btnConnect.text = "Connected to $serverPath"
                btnSubscribe.text = "Subscribe to topic"
            }
            ActionButtons.SUBSCRIBED -> {
                btnConnect.isEnabled = false
//                btnSubscribe.isEnabled = false
                btnSubscribe.text = "unsubscribe from $topicName"
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        client?.dispose()
    }
}
