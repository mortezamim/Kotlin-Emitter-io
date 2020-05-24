package ir.mjmim.emitter.core.listeners

interface IConnectListener {
    fun onSuccess(url:String)
    fun onFailure(url:String,exception: Throwable)
}