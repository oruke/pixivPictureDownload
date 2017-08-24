package com.oruke.handler

import com.alibaba.fastjson.JSON
import com.oruke.util.PixivDownloadUtil
import org.springframework.web.socket.WebSocketMessage
import org.springframework.web.socket.WebSocketSession
import java.util.HashMap
import javax.servlet.http.HttpSession

class DownLoadHandler : PixivWebSocketHandler() {
    override fun handleMessage(webSocketSession: WebSocketSession, webSocketMessage: WebSocketMessage<*>) {
        val map = JSON.parse(webSocketMessage.payload as String) as Map<*, *>
        val url = (map["url"] as String).trim()
        val cookie = (map["cookie"] as String).trim()
        val downloadMeans = PixivDownloadUtil.DownloadMeans.valueOf(map["downloadMeans"] as String)
        val sessionId = webSocketSession.attributes["sessionId"] as String
        val session = webSocketSession.attributes["session"] as HttpSession
        session.setAttribute("url", url)
        val sendMap = HashMap<String, Any>()
        sendMap["type"] = "info"
        try {
            val pixivDownloadUtil = PixivDownloadUtil.getInstance(this, sessionId, downloadMeans, cookie)
            pixivDownloadUtil.download(url)
            sendMap.put("message", "【$url】 下载完成")
            sendMap.put("class", "alert-success")
            this.sendMessageToSessoinId(sessionId, sendMap)
        } catch (e: Exception) {
            e.printStackTrace()
            sendMap["message"] = "下载【$url】异常，已退出"
            sendMap["class"] = "alert-error"
            this.sendMessageToSessoinId(sessionId, sendMap)
        }

    }
}