package com.oruke.handler

import org.springframework.http.server.ServerHttpRequest
import org.springframework.http.server.ServerHttpResponse
import org.springframework.http.server.ServletServerHttpRequest
import org.springframework.web.socket.WebSocketHandler
import org.springframework.web.socket.server.HandshakeInterceptor

class SessionHandshakeInterceptor : HandshakeInterceptor {
    @Throws(Exception::class)
    override fun beforeHandshake(serverHttpRequest: ServerHttpRequest, serverHttpResponse: ServerHttpResponse,
                                 webSocketHandler: WebSocketHandler, map: MutableMap<String, Any>): Boolean {
        val servletRequest = serverHttpRequest as ServletServerHttpRequest
        val session = servletRequest.servletRequest.session
        map.put("sessionId", session.id)
        return true
    }

    override fun afterHandshake(serverHttpRequest: ServerHttpRequest, serverHttpResponse: ServerHttpResponse,
                                webSocketHandler: WebSocketHandler, e: Exception?) {

    }
}
