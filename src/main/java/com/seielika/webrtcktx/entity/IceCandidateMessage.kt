package com.seielika.webrtcktx.entity

import org.json.JSONObject
import org.webrtc.IceCandidate
import org.webrtc.SessionDescription

data class IceCandidateMessage(
    val event: String = "trickle",
    val iceCandidate: IceCandidate,
    val sender: String,
    val receiver: String
    ) {

    override fun toString(): String {
        val msgObj = JSONObject()
        msgObj.put("event", event)
        msgObj.put("sender", sender)
        msgObj.put("receiver", receiver)

        val candidateObj = JSONObject()
        candidateObj.put("sdpMid", iceCandidate.sdpMid)
        candidateObj.put("sdpMLineIndex", iceCandidate.sdpMLineIndex)
        candidateObj.put("sdp", iceCandidate.sdp)

        msgObj.put("candidate", candidateObj)
        return msgObj.toString()
    }
}