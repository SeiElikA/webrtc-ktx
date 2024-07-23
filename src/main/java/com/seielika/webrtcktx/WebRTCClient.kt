package com.seielika.webrtcktx

import android.content.Context
import android.content.res.Resources
import android.util.Log
import com.seielika.webrtcktx.entity.IceCandidateMessage
import com.seielika.webrtcktx.entity.SdpMessage
import com.seielika.webrtcktx.exception.ConnectionCreateException
import com.seielika.webrtcktx.extension.createIceServer
import com.seielika.webrtcktx.extension.doCreateAnswer
import com.seielika.webrtcktx.extension.doCreateOffer
import com.seielika.webrtcktx.extension.doCreatePeerConnection
import com.seielika.webrtcktx.extension.doSetLocalDescription
import com.seielika.webrtcktx.extension.doSetRemoteDescription
import org.webrtc.AudioSource
import org.webrtc.AudioTrack
import org.webrtc.Camera1Enumerator
import org.webrtc.Camera2Enumerator
import org.webrtc.CameraEnumerator
import org.webrtc.DefaultVideoDecoderFactory
import org.webrtc.DefaultVideoEncoderFactory
import org.webrtc.EglBase
import org.webrtc.HardwareVideoDecoderFactory
import org.webrtc.HardwareVideoEncoderFactory
import org.webrtc.MediaConstraints
import org.webrtc.MediaStream
import org.webrtc.PeerConnection
import org.webrtc.PeerConnection.RTCConfiguration
import org.webrtc.PeerConnectionFactory
import org.webrtc.SessionDescription
import org.webrtc.SoftwareVideoEncoderFactory
import org.webrtc.SurfaceTextureHelper
import org.webrtc.VideoCapturer
import org.webrtc.VideoSource
import org.webrtc.VideoTrack


class WebRTCClient(
    private var context: Context,
    private var eglBaseContext: EglBase.Context,
    private var isSender: Boolean = true
) {
    var videoTrack: VideoTrack? = null
    private var addStreamCallback: ((MediaStream) -> Unit)? = null;
    private var audioTrack: AudioTrack? = null
    private var websocketClient: SignalServerChannel? = null;
    private var peerConnectionFactory: PeerConnectionFactory;
    private var videoCapturer: VideoCapturer? = null;
    private var peer: Peer? = null;
    private val videoDecoderFactory by lazy {
        DefaultVideoDecoderFactory(
            eglBaseContext
        )
    }
    private val videoEncoderFactory by lazy {
        DefaultVideoEncoderFactory(eglBaseContext, true, true)
    }

    companion object {
        val ICE_SERVERS = listOf("stun:stun.l.google.com:19302", "stun:stun.stunprotocol.org")
        const val AUDIO_TRACK_ID = "webrtc_audio"
        const val VIDEO_TRACK_ID = "webrtc_video"
        const val SURFACE_THREAD_NAME = "CaptureThread"
        const val SENDER = "Sender"
        const val RECEIVER = "Receiver"
    }

    init {
        // create PeerConnectionFactory
        PeerConnectionFactory.initialize(
            PeerConnectionFactory.InitializationOptions
                .builder(context)
                .createInitializationOptions()
        )

        peerConnectionFactory = PeerConnectionFactory
            .builder()
            .setVideoEncoderFactory(videoEncoderFactory)
            .setVideoDecoderFactory(videoDecoderFactory)
            .createPeerConnectionFactory()

        // create Websocket channel
        initWebsocket()
    }

    fun connectRemote() {
        this.peer = Peer()
        val connection = peer?.pc;
        connection?.addTrack(this.videoTrack)
        connection?.addTrack(this.audioTrack)
    }

    fun startCall() {
        peer?.createOffer()
    }

    fun stopCamera() {
        videoCapturer?.stopCapture();
    }

    fun setOnAddStream(addStreamCallback: ((MediaStream) -> Unit)) {
        this.addStreamCallback = addStreamCallback
    }

    fun initLocalCamera() {
        videoCapturer = createCameraCapture()

        // create AudioSource/Track
        val audioSource: AudioSource = peerConnectionFactory.createAudioSource(MediaConstraints())
        this.audioTrack = peerConnectionFactory.createAudioTrack(AUDIO_TRACK_ID, audioSource)

        // create VideoSource/Track
        val videoSource: VideoSource =
            peerConnectionFactory.createVideoSource(videoCapturer?.isScreencast ?: false)
        this.videoTrack = peerConnectionFactory.createVideoTrack(VIDEO_TRACK_ID, videoSource)

        // Surface Method
        val surfaceTextureHelper = SurfaceTextureHelper.create(SURFACE_THREAD_NAME, eglBaseContext)

        // create VideoCapture
        videoCapturer?.initialize(
            surfaceTextureHelper,
            context,
            videoSource.capturerObserver
        )

        videoTrack?.setEnabled(true)

        // get screen height width
        val width = Resources.getSystem().displayMetrics.widthPixels;
        val height = Resources.getSystem().displayMetrics.heightPixels;
        videoCapturer?.startCapture(width, height, 60)
    }

    fun dispose() {
        websocketClient?.close()
        audioTrack?.dispose()
        videoTrack?.dispose()
        videoCapturer?.dispose()
    }

    /**
     * private function
     */

    private fun initWebsocket() {
        websocketClient = SignalServerChannel(
            onReceiveOffer = { sdpDescription ->
                if (this@WebRTCClient.peer == null) {
                    peer = Peer()
                }
                val offerSdp = SessionDescription(SessionDescription.Type.OFFER, sdpDescription)
                try {
                    peer?.pc?.doSetRemoteDescription(offerSdp, setSuccess = {
                        peer?.createAnswer()
                    })
                } catch(e: ConnectionCreateException) {
                    Log.e("TAG", "receive offer error", e)
                }
            },
            onReceiveAnswer = { sdpDescription ->
                val answerSdp = SessionDescription(SessionDescription.Type.ANSWER, sdpDescription)
                peer?.pc?.doSetRemoteDescription(answerSdp)
            },
            onReceiveTrickle = { iceCandidate ->
                peer?.pc?.addIceCandidate(iceCandidate)
            }
        )
        websocketClient?.connect(if(isSender) SENDER else RECEIVER, "ws://172.16.191.37:5520/websocket")
    }

    private fun createCameraCapture(): VideoCapturer? {
        val enumerator: CameraEnumerator = if (Camera2Enumerator.isSupported(context)) {
            Camera2Enumerator(context)
        } else {
            Camera1Enumerator(true);
        }
        val deviceNames = enumerator.deviceNames

        // First, try to find front facing camera
        for (deviceName in deviceNames) {
            if (enumerator.isBackFacing(deviceName)) {
                val videoCapture: VideoCapturer? = enumerator.createCapturer(deviceName, null)

                if (videoCapture != null) {
                    return videoCapture
                }
            }
        }

        // Front facing camera not found, try something else
        for (deviceName in deviceNames) {
            if (enumerator.isFrontFacing(deviceName)) {
                val videoCapture: VideoCapturer? = enumerator.createCapturer(deviceName, null)

                if (videoCapture != null) {
                    return videoCapture
                }
            }
        }

        return null
    }

    inner class Peer {
        var pc: PeerConnection?

        init {
            pc = createPeerConnection()
        }

        private fun createPeerConnection(): PeerConnection? {
            val rtcConfiguration = RTCConfiguration(ICE_SERVERS.createIceServer())

            return this@WebRTCClient.peerConnectionFactory.doCreatePeerConnection(
                rtcConfiguration,
                doOnIceCandidate = { iceCandidate ->
                    val iceCandidateMsg = IceCandidateMessage(
                        iceCandidate = iceCandidate,
                        sender = if(isSender) SENDER else RECEIVER,
                        receiver = if(!isSender) SENDER else RECEIVER
                    )
                    websocketClient?.sendMessage(iceCandidateMsg.toString())
                },
                doOnIceCandidatesRemoved = { iceCandidates ->
                    pc?.removeIceCandidates(iceCandidates);
                },
                doOnAddStream = { stream ->
                    Log.e("TAG", "onAddStream");
                    this@WebRTCClient.addStreamCallback?.invoke(stream);
                },
                doOnAddTrack = { _, _ ->
                    Log.d("TAG", "onAddTrack");
                }
            )
        }

        fun createOffer() {
            pc?.doCreateOffer(MediaConstraints(), createSuccess = { sdp ->
                pc?.doSetLocalDescription(sdp) {
                    Log.e("TAG", "createOffer setLocalDescription onSetSuccess")
                    val sdpMsg = SdpMessage(
                        sdp = sdp,
                        sender = if(isSender) SENDER else RECEIVER,
                        receiver = if(!isSender) SENDER else RECEIVER
                    )
                    websocketClient?.sendMessage(sdpMsg.toString())
                }
            })
        }

        fun createAnswer() {
            pc?.doCreateAnswer(MediaConstraints(), createSuccess = { sdp ->
                pc?.doSetLocalDescription(sdp) {
                    Log.e("TAG", "createAnswer setLocalDescription onSetSuccess")
                    val sdpMsg = SdpMessage(
                        sdp = sdp,
                        sender = if(isSender) SENDER else RECEIVER,
                        receiver = if(!isSender) SENDER else RECEIVER
                    )
                    websocketClient?.sendMessage(sdpMsg.toString())
                }
            })
        }
    }
}