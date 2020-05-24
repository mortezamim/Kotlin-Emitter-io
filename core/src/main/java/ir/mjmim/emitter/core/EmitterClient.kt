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
import java.io.UnsupportedEncodingException


class EmitterClient(ctx: Context) {

    private var ctx: Context? = ctx

    var instance: EmitterClient = this

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

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable?) {
                    log("Connect failed: $url", true)
                    listener.onFailure(url, exception)
                    exception?.printStackTrace()
                    client?.unregisterResources()
                    client = null
                }
            }

        } catch (ex: MqttException) {
            ex.printStackTrace()
        } catch (e: Exception) {
            log("Connect failed: $url", true)
            listener.onFailure(url, e)
        }
        return instance
    }

    fun addGlobalCallback(listener: IGlobalListener): EmitterClient {
        if (client == null) throw NullPointerException("Client can't be null...")
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
                log("MessageArrived : $msg")
                listener.messageArrived(topic, msg)
            }

            override fun deliveryComplete(token: IMqttDeliveryToken?) {
                var msg = ""
                token?.let { msg = String(it.message.payload) }
                log("DeliveryComplete : $msg")
                listener.deliveryComplete(msg)
            }
        })
        return instance
    }

    fun subscribeTopic(secretKey: String, topicName: String, listener: IConnectListener? = null): EmitterClient {
        if (client == null) throw NullPointerException("Client can't be null...")
        val qos = 1
        val topicPath = topicPathFormatter(secretKey, topicName)
        try {
            val subToken = client!!.subscribe(topicPath, qos)

            subToken.actionCallback = object : IMqttActionListener {
                override fun onSuccess(asyncActionToken: IMqttToken) {
                    log("Subscribed to : $topicName")
                    listener?.onSuccess(topicPath)
                }

                override fun onFailure(asyncActionToken: IMqttToken, exception: Throwable?) {
                    log("Subscribe failed : $topicName", true)
                    exception?.printStackTrace()
                    listener?.onFailure(topicPath, exception)
                }
            }
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        return instance
    }

    fun unsubscribeTopic(secretKey: String, topicName: String, listener: IConnectListener? = null){
        if (client == null) throw NullPointerException("Client can't be null...")
        val topicPath = topicPathFormatter(secretKey, topicName)
        client?.unsubscribe(topicPath,ctx,object :IMqttActionListener{
            override fun onSuccess(asyncActionToken: IMqttToken?) {
                log("unsubscribed from : $topicName")
                listener?.onSuccess(topicPath)
            }

            override fun onFailure(asyncActionToken: IMqttToken?, exception: Throwable?) {
                log("unsubscribe failed : $topicName", true)
                exception?.printStackTrace()
                listener?.onFailure(topicPath, exception)
            }

        })
    }

    fun publishMessage(secretKey: String, topicName: String, payload: String): EmitterClient {
        if (client == null) throw NullPointerException("Client can't be null...")
        val encodedPayload: ByteArray
        val topicPath = topicPathFormatter(secretKey, topicName)
        try {
            encodedPayload = payload.toByteArray()
            val message = MqttMessage(encodedPayload)
            client!!.publish(topicPath, message)
        } catch (e: UnsupportedEncodingException) {
            e.printStackTrace()
        } catch (e: MqttException) {
            e.printStackTrace()
        }
        return instance
    }

    fun dispose() {
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
        return instance
    }

    private fun log(s: String, e: Boolean = false) {
        if (!enableLogging) return
        if (e) Logger.e(s) else Logger.d(s)
    }

}
