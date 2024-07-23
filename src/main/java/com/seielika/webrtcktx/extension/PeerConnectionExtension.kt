package com.seielika.webrtcktx.extension

import com.seielika.webrtcktx.exception.ConnectionCreateException
import org.webrtc.CandidatePairChangeEvent
import org.webrtc.DataChannel
import org.webrtc.IceCandidate
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.IceConnectionState
import org.webrtc.PeerConnection.IceServer
import org.webrtc.PeerConnection.SignalingState
import org.webrtc.PeerConnectionFactory
import org.webrtc.RtpReceiver
import org.webrtc.RtpTransceiver
import org.webrtc.SdpObserver
import org.webrtc.SessionDescription

inline fun PeerConnection?.doCreateAnswer(
    mediaConstraints: MediaConstraints,
    crossinline createSuccess: (sdp: SessionDescription?) -> Unit = {},
    crossinline setSuccess: () -> Unit = {},
) {
    val observer = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            createSuccess.invoke(sdp)
        }

        override fun onSetSuccess() {
            setSuccess.invoke()
        }

        override fun onCreateFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.CREATE_FAILURE
            )
        }

        override fun onSetFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.SET_FAILURE
            )
        }
    }

    this?.createAnswer(observer, mediaConstraints);
}

inline fun PeerConnection?.doCreateOffer(
    mediaConstraints: MediaConstraints,
    crossinline createSuccess: (sdp: SessionDescription?) -> Unit = {},
    crossinline setSuccess: () -> Unit = {}
) {
    val observer = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            createSuccess.invoke(sdp)
        }

        override fun onSetSuccess() {
            setSuccess.invoke()
        }

        override fun onCreateFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.CREATE_FAILURE
            )
        }

        override fun onSetFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.SET_FAILURE
            )
        }
    }

    this?.createOffer(observer, mediaConstraints);
}

inline fun PeerConnection?.doSetLocalDescription(
    sdp: SessionDescription?,
    crossinline createSuccess: (sdp: SessionDescription?) -> Unit = {},
    crossinline setSuccess: () -> Unit = {}
) {
    val observer = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            createSuccess.invoke(sdp)
        }

        override fun onSetSuccess() {
            setSuccess.invoke()
        }

        override fun onCreateFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.CREATE_FAILURE
            )
        }

        override fun onSetFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.SET_FAILURE
            )
        }
    }

    this?.setLocalDescription(observer, sdp);
}

inline fun PeerConnection?.doSetRemoteDescription(
    sdp: SessionDescription?,
    crossinline createSuccess: (sdp: SessionDescription?) -> Unit = {},
    crossinline setSuccess: () -> Unit = {}
) {
    val observer = object : SdpObserver {
        override fun onCreateSuccess(sdp: SessionDescription?) {
            createSuccess.invoke(sdp)
        }

        override fun onSetSuccess() {
            setSuccess.invoke()
        }

        override fun onCreateFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.CREATE_FAILURE
            )
        }

        override fun onSetFailure(msg: String?) {
            throw ConnectionCreateException(
                msg,
                ConnectionCreateException.CreateTypeEnum.SET_FAILURE
            )
        }
    }

    this?.setRemoteDescription(observer, sdp);
}

inline fun PeerConnectionFactory.doCreatePeerConnection(
    configuration: PeerConnection.RTCConfiguration,
    crossinline doOnSignalingChange: (signalingState: SignalingState) -> Unit = {},
    crossinline doOnIceConnectionChange: (iceConnectionState: IceConnectionState) -> Unit = {},
    crossinline doOnStandardizedIceConnectionChange: (newState: IceConnectionState) -> Unit = {},
    crossinline doOnConnectionChange: (newState: PeerConnection.PeerConnectionState) -> Unit = {},
    crossinline doOnIceConnectionReceivingChange: (b: Boolean) -> Unit = {},
    crossinline doOnIceGatheringChange: (iceGatheringState: PeerConnection.IceGatheringState) -> Unit = {},
    crossinline doOnIceCandidate: (iceCandidate: IceCandidate) -> Unit = {},
    crossinline doOnIceCandidatesRemoved: (iceCandidates: Array<IceCandidate>) -> Unit = {},
    crossinline doOnSelectedCandidatePairChanged: (event: CandidatePairChangeEvent) -> Unit = {},
    crossinline doOnAddStream: (mediaStream: MediaStream) -> Unit = {},
    crossinline doOnRemoveStream: (mediaStream: MediaStream) -> Unit = {},
    crossinline doOnDataChannel: (dataChannel: DataChannel) -> Unit = {},
    crossinline doOnRenegotiationNeeded: () -> Unit = {},
    crossinline doOnAddTrack: (rtpReceiver: RtpReceiver, mediaStreams: Array<out MediaStream>) -> Unit = {_,_ ->},
    crossinline doOnRemoveTrack: (rtpReceiver: RtpReceiver?) -> Unit = {_ ->},
    crossinline doOnTrack: (transceiver: RtpTransceiver) -> Unit = {},
): PeerConnection? {
    val observer = object: PeerConnection.Observer {
        override fun onSignalingChange(p0: SignalingState) {
            doOnSignalingChange.invoke(p0)
        }

        override fun onIceConnectionChange(p0: IceConnectionState) {
            doOnIceConnectionChange.invoke(p0)
        }

        override fun onIceConnectionReceivingChange(p0: Boolean) {
            doOnIceConnectionReceivingChange.invoke(p0)
        }

        override fun onIceGatheringChange(p0: PeerConnection.IceGatheringState) {
            doOnIceGatheringChange.invoke(p0)
        }

        override fun onIceCandidate(p0: IceCandidate) {
            doOnIceCandidate.invoke(p0);
        }

        override fun onIceCandidatesRemoved(p0: Array<IceCandidate>) {
            doOnIceCandidatesRemoved.invoke(p0)
        }

        override fun onAddStream(p0: MediaStream) {
            doOnAddStream.invoke(p0)
        }

        override fun onRemoveStream(p0: MediaStream) {
            doOnRemoveStream.invoke(p0)
        }

        override fun onDataChannel(p0: DataChannel) {
            doOnDataChannel.invoke(p0)
        }

        override fun onRenegotiationNeeded() {
            doOnRenegotiationNeeded.invoke();
        }

        override fun onAddTrack(receiver: RtpReceiver, mediaStreams: Array<out MediaStream>) {
            doOnAddTrack.invoke(receiver, mediaStreams)
        }

        override fun onTrack(transceiver: RtpTransceiver) {
            super.onTrack(transceiver)
            doOnTrack(transceiver)
        }
    }

    return this.createPeerConnection(configuration, observer)
}

fun List<String>.createIceServer(): List<IceServer> {
    val resList = mutableListOf<IceServer>()
    for (i in this) {
        resList.add(IceServer.builder(i).createIceServer())
    }
    return resList
}