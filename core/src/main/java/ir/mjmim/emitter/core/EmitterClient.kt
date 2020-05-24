package ir.mjmim.emitter.core

import android.content.Context
import com.orhanobut.logger.AndroidLogAdapter
import com.orhanobut.logger.FormatStrategy
import com.orhanobut.logger.Logger
import com.orhanobut.logger.PrettyFormatStrategy
import ir.mjmim.emitter.core.listeners.IConnectListener
import ir.mjmim.emitter.core.listeners.IGlobalListener
import org.eclipse.paho.android.service.MqttAndroidClient
import org.eclipse.paho.client.mqttv3.*


class EmitterClient(private var ctx: Context) {
    private var enableLogging: Boolean = false
    private var clientId: String? = null
    private var client: MqttAndroidClient? = null


    fun connect(url: String, listener: IConnectListener): EmitterClient {
        log("Start Connect to: $url")
        try {
            clientId = MqttClient.generateClientId()
            client = MqttAndroidClient(ctx, url, clientId)
            val options = MqttConnectOptions()
            options.mqttVersion = MqttConnectOptions.MQTT_VERSION_DEFAULT
            options.isCleanSession = true
            options.isAutomaticReconnect = true
            client?.connect(options)?.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    log("Connected to: $url")
                    listener.onSuccess(url)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    log("Connect failed: $url", true)
                    listener.onFailure(url, exception)
                    exception.printStackTrace()
                    client?.unregisterResources()
                }
            }

        } catch (ex: MqttException) {
            ex.printStackTrace()
        } catch (e: Exception) {
            log("Connect failed: $url", true)
            listener.onFailure(url, e)
        }
        return this
    }

    fun addGlobalCallback(listener: IGlobalListener): EmitterClient {
        if (client == null) throw NullPointerException("connection initial failed...")
        client?.setCallback(object : MqttCallback {
            override fun connectionLost(cause: Throwable?) {
                log("Connection lost", true)
                listener.connectionLost(cause)
                cause?.printStackTrace()
            }

            @Throws(java.lang.Exception::class)
            override fun messageArrived(topic: String?, message: MqttMessage?) {
                var msg = ""
                message?.let { msg = String(it.payload) }
                log(msg)
                listener.messageArrived(topic, msg)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                var msg = ""
                token?.let { msg = String(it.message.payload) }
                log(msg)
                listener.deliveryComplete(msg)
            }
        })
        return this
    }

    fun subscribeTopic(secretKey: String, topicName: String, listener: IConnectListener? = null): EmitterClient {
        val qos = 1
        val topicPath = topicPathFormatter(secretKey, topicName)
        try {
            val subToken = client!!.subscribe(topicPath, qos)

            subToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    log("Subscribed to : $topicName")
                    listener?.onSuccess(topicPath)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable) {
                    // The subscription could not be performed, maybe the user was not
                    // authorized to subscribe on the specified topic e.g. using wildcards
                    log("Subscribe failed : $topicName", true)
                    listener?.onFailure(topicPath, exception)
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        return this
    }

//    fun publishMessage(secretKey: String, topicName: String, message: String) {
//        val path = messageFormatter(secretKey, topicName)
//    }

    fun dispose(){
        client?.let {
            if (it.isConnected) {
                client?.disconnect()
                client = null
            }
        }
    }

    private fun topicPathFormatter(secretKey: String, topicName: String): String {
        return "$secretKey/$topicName"
    }

    fun enableLogging(customTag: String = "Kotlin-Emitter-IO"): EmitterClient {
        if (this.enableLogging) return this
        Logger.clearLogAdapters()
        this.enableLogging = true
        val formatStrategy: FormatStrategy = PrettyFormatStrategy.newBuilder()
            .showThreadInfo(false)
            .tag(customTag)
            .methodCount(0)
            .build()
        Logger.addLogAdapter(AndroidLogAdapter(formatStrategy))
        return this
    }

    private fun log(s: String, e: Boolean = false) {
        if (!enableLogging) return
        if (e) Logger.e(s) else Logger.d(s)
    }

}
