package com.oruke.handler

import com.alibaba.fastjson.JSON
import com.oruke.util.PixivDownloadUtil
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.util.*

class DownLoadHandler : PixivWebSocketHandler() {
    override fun handleMessage(webSocketSession: WebSocketSession, webSocketMessage: WebSocketMessage<*>) {
        val map = JSON.parse(webSocketMessage.payload as String) as Map<*, *>
        val url = (map["url"] as String).trim()
        val cookie = (map["cookie"] as String).trim()
        val downloadMeans = PixivDownloadUtil.DownloadMeans.valueOf(map["downloadMeans"] as String)
        val sessionId = webSocketSession.attributes["sessionId"] as String
        val sendMap = HashMap<String, Any>()
        sendMap["type"] = "info"
        try {
            val pixivDownloadUtil = PixivDownloadUtil(this, sessionId, cookie, url)
            pixivDownloadUtil.downloadMeans = downloadMeans
            pixivDownloadUtil.start()
            sendMap.put("message", "【$url】 下载完成")
            sendMap.put("class", "alert-success")
            this.sendMessageToSessionId(sessionId, sendMap)
        } catch (e: Exception) {
            e.printStackTrace()
            sendMap["message"] = "下载【$url】异常，已退出"
            sendMap["class"] = "alert-error"
            this.sendMessageToSessionId(sessionId, sendMap)
        }

    }
}