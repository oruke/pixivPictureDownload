package com.oruke.conttoller.rest

import com.oruke.base.BaseController
import com.oruke.handler.PixivWebSocketHandler
import okhttp3.OkHttpClient
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController

@RestController
@RequestMapping("/pixiv")
class PixivResource : BaseController() {
    @Autowired
    private val pixivWebSocketHandler: PixivWebSocketHandler? = null
    private val client = OkHttpClient()
}
