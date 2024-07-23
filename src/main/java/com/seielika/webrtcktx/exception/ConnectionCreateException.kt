package com.seielika.webrtcktx.exception

class ConnectionCreateException(msg: String?, var type: CreateTypeEnum): Exception(msg) {
    enum class CreateTypeEnum {
        CREATE_FAILURE,
        SET_FAILURE
    }
}