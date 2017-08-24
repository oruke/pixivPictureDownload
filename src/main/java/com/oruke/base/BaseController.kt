package com.oruke.base

import org.springframework.web.context.request.RequestContextHolder
import org.springframework.web.context.request.ServletRequestAttributes

import javax.servlet.http.HttpServletRequest
import javax.servlet.http.HttpSession

open class BaseController {

    /**
     * 取得request对象.

     * @return HttpServletRequest
     */
    val request: HttpServletRequest
        get() = (RequestContextHolder.getRequestAttributes() as ServletRequestAttributes).request

    /**
     * 取得session对象.

     * @return HttpSession
     */
    val session: HttpSession
        get() = request.session
}
