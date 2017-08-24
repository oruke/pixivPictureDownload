package com.oruke.conttoller

import com.oruke.base.BaseController
import org.springframework.stereotype.Controller
import org.springframework.ui.Model
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.RequestMapping

@Controller
class PixivController : BaseController() {

    @GetMapping("")
    fun download(model: Model): String {
        return "download"
    }
}
