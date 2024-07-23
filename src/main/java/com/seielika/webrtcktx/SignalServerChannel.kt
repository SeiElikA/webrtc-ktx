package com.seielika.webrtcktx

import android.util.Log
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject
import org.webrtc.IceCandidate


class SignalServerChannel(
    private var onRegister: (userInfo: JSONObject) -> Unit = {},
    private var onReceiveOffer: (sdpDescription: String) -> Unit = {},
    private var onReceiveAnswer: (sdpDescription: String) -> Unit = {},
    private var onReceiveTrickle: (iceCandidate: IceCandidate) -> Unit = {},
    private var onClose: (code: Int, reason: String) -> Unit = {_, _ ->},
    private var onFailure: (t: Throwable, response: Response?) -> Unit = {_, _ ->}
) {
    private var webSocket: WebSocket? = null
    private var isConnected: Boolean = false

    fun connect(userId: String, url: String) {
        val client = OkHttpClient()
        val request: Request = Request.Builder()
            .url(url)
            .build()

        webSocket = client.newWebSocket(request, object: WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                super.onMessage(webSocket, text)
                this@SignalServerChannel.isConnected = true;

                val msgObj = JSONObject(text)
                val event = msgObj.getString("event")
                if ("sdp" == event) {
                    val sdpType = msgObj.getString("type")
                    val sdpDescription = msgObj.getString("description")
                    if (sdpType.lowercase() == "offer") {
                        Log.e("TAG", "On offer")
                        onReceiveOffer.invoke(sdpDescription);
                    } else if (sdpType.lowercase() == "answer") {
                        Log.e("TAG", "On answer")
                        onReceiveAnswer.invoke(sdpDescription);
                    }
                } else if ("trickle" == event) {
                    Log.e("TAG", "On trickle")
                    val candidateObj = msgObj.getJSONObject("candidate")
                    val sdpMid = candidateObj.getString("sdpMid")
                    val sdpMLineIndex = candidateObj.getInt("sdpMLineIndex")
                    val sdp = candidateObj.getString("sdp")
                    val iceCandidate = IceCandidate(sdpMid, sdpMLineIndex, sdp)
                    onReceiveTrickle.invoke(iceCandidate)
                }
            }

            override fun onOpen(webSocket: WebSocket, response: Response) {
                super.onOpen(webSocket, response)
                Log.e("TAG", "Connecting websocket server")
                this@SignalServerChannel.isConnected = true;
                val registerMsg = JSONObject()
                registerMsg.put("event", "register")
                registerMsg.put("userId", userId)
                webSocket.send(registerMsg.toString())
                onRegister.invoke(registerMsg)
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                super.onClosed(webSocket, code, reason)
                this@SignalServerChannel.isConnected = false;
                onClose.invoke(code, reason)
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                super.onFailure(webSocket, t, response)
                this@SignalServerChannel.isConnected = false;
                Log.e("TAG", response?.message ?: "Websocket error");
                onFailure.invoke(t, response)
            }
        })
    }

    fun sendMessage(message: String) {
        if (webSocket != null && isConnected) {
            Log.d("TAG", "send==>>$message")
            webSocket!!.send(message)
        } else {
            Log.e("TAG", "send failed socket not connected")
        }
    }

    fun close() {
        if (webSocket != null) {
            webSocket!!.close(1000, "manual close")
            webSocket = null
        }
    }
}