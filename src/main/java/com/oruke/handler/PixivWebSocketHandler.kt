package com.oruke.handler

import com.alibaba.fastjson.JSON
import org.springframework.stereotype.Component
import org.springframework.web.socket.*

import java.io.IOException
import java.util.concurrent.ConcurrentHashMap

@Component
open class PixivWebSocketHandler : WebSocketHandler {
    private val sessionMap = ConcurrentHashMap<String, WebSocketSession>()

    @Throws(Exception::class)
    override fun afterConnectionEstablished(webSocketSession: WebSocketSession) {
        println("成功建立socket连接")
        val sessionId = webSocketSession.attributes["sessionId"]
        if (sessionId != null) {
            sessionMap.put(sessionId.toString(), webSocketSession)
        }
    }

    @Throws(Exception::class)
    override fun handleMessage(webSocketSession: WebSocketSession, webSocketMessage: WebSocketMessage<*>) {

    }

    @Throws(Exception::class)
    override fun handleTransportError(webSocketSession: WebSocketSession, throwable: Throwable) {

    }

    @Throws(Exception::class)
    override fun afterConnectionClosed(webSocketSession: WebSocketSession, closeStatus: CloseStatus) {

    }

    override fun supportsPartialMessages(): Boolean {
        return false
    }

    /**
     * 给所有在线用户发送消息

     * @param data
     */
    @Synchronized
    fun <T> sendMessageToAll(data: T) {
        sessionMap.forEach { sessionId, webSocketSession ->
            try {
                if (webSocketSession.isOpen) {
                    webSocketSession.sendMessage(TextMessage(JSON.toJSONString(data)))
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    }

    /**
     * 给某个用户发送消息

     * @param sessionId
     * *
     * @param data
     */
    @Synchronized
    fun <T> sendMessageToSessionId(sessionId: String, data: T) {
        val webSocketSession = sessionMap[sessionId]
        try {
            if (webSocketSession != null && webSocketSession.isOpen) {
                webSocketSession.sendMessage(TextMessage(JSON.toJSONString(data)))
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }

    }
}
