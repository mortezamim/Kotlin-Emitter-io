package ir.mjmim.emitter.core.listeners

import org.eclipse.paho.client.mqttv3.IMqttDeliveryToken
import org.eclipse.paho.client.mqttv3.MqttMessage

interface IGlobalListener {
    fun connectionLost(cause: Throwable?)
    fun messageArrived(topic: String?, message: String?)
    @Throws(Exception::class)
    fun deliveryComplete(token: String?)
}