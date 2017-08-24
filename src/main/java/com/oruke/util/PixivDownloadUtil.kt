package com.oruke.util

import com.google.common.io.ByteStreams
import com.oruke.handler.PixivWebSocketHandler
import com.xiaoleilu.hutool.StrUtil
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.jsoup.Jsoup
import org.jsoup.nodes.Document
import org.jsoup.nodes.Element
import org.jsoup.select.Elements
import java.io.File
import java.io.FileOutputStream

import java.util.*
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.ConcurrentMap
import java.util.concurrent.TimeUnit
import java.util.stream.Collectors

/**
 * date:2017.08.01
 * time:19:42
 * author:oruke
 */
class PixivDownloadUtil {
    private val regexMedium = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?mode=medium&illust_id=\\d+$"
    private val regexManga = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?mode=manga&illust_id=\\d+$"
    private val regexAuthorIllust1 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?type=illust&id=\\d+$"
    private val regexAuthorIllust2 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?id=\\d+&type=illust&p=\\d+$"
    private val regexAuthorManga1 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?type=manga&id=\\d+$"
    private val regexAuthorManga2 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?id=\\d+&type=manga&p=d+$"
    private val regexAuthorUgoira1 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?type=manga&id=\\d+$"
    private val regexAuthorUgoira2 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?id=\\d+&type=manga&p=\\d+$"
    private val regexAuthor1 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?id=\\d+$"
    private val regexAuthor2 = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?id=\\d+&type=all&p=\\d+$"
    private val host = "https://www.pixiv.net"
    private var cookie = ""
    private var downloadMeans = DownloadMeans.LOCAL
    private var pixivWebSocketHandler: PixivWebSocketHandler? = null
    private var sessionId: String? = null
    private val client = OkHttpClient()
    private val builder = client.newBuilder().connectTimeout(5000L, TimeUnit.MINUTES)
    private var deque = ConcurrentHashMap<String, ConcurrentHashMap<String, Any>>()

    fun headers(): MutableMap<String, String> {
        val headers = HashMap<String, String>()
        headers.put("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/59.0.3071.115 Safari/537.36")
        headers.put("Host", "www.pixiv.net")
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,image/webp,image/apng,*/*;q=0.8")
        headers.put("Referer", "https://www.pixiv.net/")
        headers.put("Cookie", cookie)
        return headers
    }

    @Throws(Exception::class)
    fun download(url: String) {
        if (url.matches(regexMedium.toRegex())) {
            medium(url)
        } else if (url.matches(regexManga.toRegex())) {
            manga(url)
        } else if (url.matches(regexAuthorIllust1.toRegex()) || url.matches(regexAuthorIllust2.toRegex())) {
            author(url, Type.MEDIUM)
        } else if (url.matches(regexAuthorManga1.toRegex()) || url.matches(regexAuthorManga2.toRegex())) {
            author(url, Type.MANGA)
        } else if (url.matches(regexAuthorUgoira1.toRegex()) || url.matches(regexAuthorUgoira2.toRegex())) {
            author(url, Type.UGOIRA)
        } else if (url.matches(regexAuthor1.toRegex()) || url.matches(regexAuthor2.toRegex())) {
            author(url, Type.ALL)
        } else {
            throw Exception("URL不匹配")
        }
    }

    @Throws(Exception::class)
    fun author(url: String, type: Type) {
        val linkUrls = LinkedList<String>()
        val calledUrls = HashSet<String>()
        val regex1: String
        val regex2: String
        when (type) {
            PixivDownloadUtil.Type.ALL -> {
                regex1 = regexAuthor1
                regex2 = regexAuthor2
            }
            PixivDownloadUtil.Type.MEDIUM -> {
                regex1 = regexAuthorIllust1
                regex2 = regexAuthorIllust2
            }
            PixivDownloadUtil.Type.MANGA -> {
                regex1 = regexAuthorManga1
                regex2 = regexAuthorManga2
            }
            PixivDownloadUtil.Type.UGOIRA -> {
                regex1 = regexAuthorUgoira1
                regex2 = regexAuthorUgoira2
            }
        }
        linkUrls.add(url)
        while (!linkUrls.isEmpty()) {
            val link = linkUrls.poll()
            calledUrls.add(link)
            val request = Request.Builder()
                    .url(link)
                    .headers(Headers.of(headers()))
                    .build()
            val response = client.newCall(request).execute()
            val html = response.body()!!.string()
            val document = Jsoup.parse(html)
            if (link.matches(regexMedium.toRegex())) {
                medium(link)
                continue
            }
            val aList = document.getElementsByTag("a")
            val links = aList.stream().map { element -> element.attr("href") }
            val intactLinks = links.filter(StrUtil::isNotBlank).map { link1 -> urlUtil(link, link1) }.filter { link1 ->
                (link1.matches(regex1.toRegex()) || link1.matches(regex2.toRegex()) || link1.matches(regexMedium.toRegex())) &&
                        !linkUrls.contains(link1) && !calledUrls.contains(link1)
            }
            for (link1 in intactLinks) {
                linkUrls.offer(link1)
            }
        }
    }

    @Throws(Exception::class)
    fun medium(url: String) {
        val request = Request.Builder()
                .url(url)
                .headers(Headers.of(headers()))
                .build()
        val response = client.newCall(request).execute()
        val html = response.body()!!.string()
        val document = Jsoup.parse(html)

        val imgUrl = document.select("img.original-image").attr("data-src")
        val aclass = document.select("div.works_display a").attr("class")
        if (StrUtil.isBlank(aclass) || !aclass.contains("multiple")) {
            img(url, imgUrl)
            return
        }

        val mangaUrl = document.select("div.works_display a.read-more").attr("href")
        manga(urlUtil(url, mangaUrl))
    }

    @Throws(Exception::class)
    fun manga(url: String) {
        val request = Request.Builder()
                .url(url)
                .headers(Headers.of(headers()))
                .build()
        val response = client.newCall(request).execute()
        val html = response.body()!!.string()
        val document = Jsoup.parse(html)

        val originalElements = document.select("div.item-container a.full-size-container")
        originalElements
                .map { it.attr("href") }
                .map {
                    Request.Builder()
                            .url(urlUtil(url, it))
                            .headers(Headers.of(headers()))
                            .build()
                }
                .map { client.newCall(it).execute() }
                .map { it.body()!!.string() }
                .map { Jsoup.parse(it) }
                .map { it.select("img").attr("src") }
                .forEach { img(url, it) }
    }

    @Throws(Exception::class)
    fun img(referer: String, imgUrl: String) {
        val headers = headers()
        headers.put("Referer", referer)
        val imgRequest = Request.Builder()
                .url(imgUrl)
                .headers(Headers.of(headers))
                .build()
        val imgResponse = client.newCall(imgRequest).execute()
        val urlSplit = imgUrl.split("/".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()
        val img = urlSplit[urlSplit.size - 1]
        val map = HashMap<String, Any>()
        when (downloadMeans) {
            DownloadMeans.LOCAL -> {
                map.put("type", "info")
                map.put("message", "正在下载：$img")
                map.put("class", "alert-success")
                pixivWebSocketHandler!!.sendMessageToSessoinId(sessionId!!, map)
                val file = File("/pixivDownload")
                if (!file.exists()) file.mkdir()
                val outStream = FileOutputStream("/pixivDownload/$img")
                ByteStreams.copy(imgResponse.body()!!.byteStream(), outStream)
                map.put("message", "下载完成：$img")
                map.put("class", "alert-info")
                pixivWebSocketHandler!!.sendMessageToSessoinId(sessionId!!, map)
            }
            DownloadMeans.BROWSER -> {
                val bytes = imgResponse.body()!!.bytes()
                val list = bytes.map { it.toString() }

                map.put("type", "file")
                map.put(img, list)
                pixivWebSocketHandler!!.sendMessageToSessoinId(sessionId!!, map)
            }
        }
    }

    fun urlUtil(url: String, link: String): String {
        if (StrUtil.isBlank(url) || StrUtil.isBlank(link)) {
            throw RuntimeException("URL转换错误")
        }
        if (link.contains("://")) {
            return link
        }
        if (link.substring(0, 1) == "/") {
            return host + link
        }
        return url.split("\\?".toRegex()).dropLastWhile { it.isEmpty() }.toTypedArray()[0] + link
    }

    enum class Type {
        MANGA,
        ALL,
        MEDIUM,
        UGOIRA
    }

    enum class DownloadMeans {
        BROWSER,
        LOCAL
    }

    companion object {

        fun getInstance(pixivWebSocketHandler: PixivWebSocketHandler, sessionId: String, cookie: String): PixivDownloadUtil {
            val pixivDownloadUtil = PixivDownloadUtil()
            pixivDownloadUtil.pixivWebSocketHandler = pixivWebSocketHandler
            pixivDownloadUtil.sessionId = sessionId
            pixivDownloadUtil.cookie = cookie
            return pixivDownloadUtil
        }

        fun getInstance(pixivWebSocketHandler: PixivWebSocketHandler, sessionId: String, downloadMeans: DownloadMeans, cookie: String): PixivDownloadUtil {
            val pixivDownloadUtil = PixivDownloadUtil()
            pixivDownloadUtil.pixivWebSocketHandler = pixivWebSocketHandler
            pixivDownloadUtil.downloadMeans = downloadMeans
            pixivDownloadUtil.sessionId = sessionId
            pixivDownloadUtil.cookie = cookie
            return pixivDownloadUtil
        }
    }

}
