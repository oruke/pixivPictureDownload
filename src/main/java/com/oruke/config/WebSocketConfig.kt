package com.oruke.config

import com.oruke.handler.DownLoadHandler
import com.oruke.handler.PixivWebSocketHandler
import com.oruke.handler.SessionHandshakeInterceptor
import org.springframework.context.annotation.Configuration
import org.springframework.web.socket.config.annotation.EnableWebSocket
import org.springframework.web.socket.config.annotation.WebSocketConfigurer
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry

@Configuration
@EnableWebSocket
open class WebSocketConfig : WebSocketConfigurer {

    override fun registerWebSocketHandlers(webSocketHandlerRegistry: WebSocketHandlerRegistry) {
        webSocketHandlerRegistry.addHandler(PixivWebSocketHandler(), "/websocket")
                .addHandler(DownLoadHandler(), "/websocket/download")
                .addInterceptors(SessionHandshakeInterceptor())
                .setAllowedOrigins("*")
    }
}
