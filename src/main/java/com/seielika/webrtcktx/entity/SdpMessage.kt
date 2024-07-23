package com.seielika.webrtcktx.entity

import org.json.JSONObject
import org.webrtc.SessionDescription

data class SdpMessage(
    val event: String = "sdp",
    val sdp: SessionDescription?,
    val sender: String,
    val receiver: String
    ) {

    override fun toString(): String {
        val msgObj = JSONObject()
        msgObj.put("event", "sdp")
        msgObj.put("type", sdp?.type.toString())
        msgObj.put("description", sdp?.description)
        msgObj.put("sender", sender )
        msgObj.put("receiver", receiver)
        return msgObj.toString()
    }
}