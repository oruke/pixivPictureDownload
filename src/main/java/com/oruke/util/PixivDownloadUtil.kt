package com.oruke.util

import com.google.common.io.ByteStreams
import com.oruke.handler.PixivWebSocketHandler
import com.xiaoleilu.hutool.StrUtil
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.Request
import org.jsoup.Jsoup
import java.io.File
import java.io.FileOutputStream
import java.util.*
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicLong

/**
 * date:2017.08.01
 * time:19:42
 * author:oruke
 */
class PixivDownloadUtil internal constructor(private val pixivWebSocketHandler: PixivWebSocketHandler, private val sessionId: String, private val cookie: String,
                                             private val url: String) : Runnable {
    private val regexMedium = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?mode=medium&illust_id=\\d+$".toRegex()
    private val regexManga = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?mode=manga&illust_id=\\d+$".toRegex()
    private val regexAuthorIllust = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?(type=illust&id=\\d+|id=\\d+&type=illust&p=\\d+)$".toRegex()
    private val regexAuthorManga = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?(type=manga&id=\\d+|id=\\d+&type=manga&p=d+)$".toRegex()
    private val regexAuthorUgoira = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?(type=manga&id=\\d+|id=\\d+&type=manga&p=\\d+)$".toRegex()
    private val regexAuthor = "^http(s)?://www\\.pixiv\\.net/member_illust\\.php\\?(id=\\d+|id=\\d+&type=all&p=\\d+)$".toRegex()
    private val regexBookmark = "^http(s)?://www\\.pixiv\\.net/bookmark\\.php\\w+$".toRegex()
    private val host = "https://www.pixiv.net"
    var downloadMeans = DownloadMeans.LOCAL
    private val client = OkHttpClient()
    private val builder = client.newBuilder().connectTimeout(5000L, TimeUnit.MINUTES)
    private val urlQueue = SpiderQueue(url)
    private var count: AtomicLong = AtomicLong(0)
    var thread = 100

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
    fun download() {
        if (url.matches(regexMedium)) {
            medium(url)
        } else if (url.matches(regexManga)) {
            manga(url)
        } else if (url.matches(regexAuthorIllust)) {
            author(Type.MEDIUM)
        } else if (url.matches(regexAuthorManga)) {
            author(Type.MANGA)
        } else if (url.matches(regexAuthorUgoira)) {
            author(Type.UGOIRA)
        } else if (url.matches(regexAuthor)) {
            author(Type.ALL)
        } else if (url.matches(regexBookmark)) {
            author(Type.BOOKMARK)
        } else {
            throw Exception("URL不匹配")
        }
        println(count)
    }

    @Throws(Exception::class)
    fun author(type: Type) {
        val regex: Regex
        when (type) {
            PixivDownloadUtil.Type.ALL -> {
                regex = regexAuthor
            }
            PixivDownloadUtil.Type.MEDIUM -> {
                regex = regexAuthorIllust
            }
            PixivDownloadUtil.Type.MANGA -> {
                regex = regexAuthorManga
            }
            PixivDownloadUtil.Type.UGOIRA -> {
                regex = regexAuthorUgoira
            }
            PixivDownloadUtil.Type.BOOKMARK -> {
                regex = regexBookmark
            }
        }
        val i = AtomicLong(0)
        while (true) {
            i.getAndAdd(1)
            println(Thread.currentThread().name + "=>" + urlQueue.size())
            val link = urlQueue.poll() ?: break
            val request = Request.Builder()
                    .url(link)
                    .headers(Headers.of(headers()))
                    .build()
            val response = client.newCall(request).execute()
            val html = response.body()!!.string()
            val document = Jsoup.parse(html)
            if (link.matches(regexMedium)) {
                medium(link)
                continue
            }
            val aList = document.getElementsByTag(".layout-column-2 a")
            val links = aList.stream().map { element -> element.attr("href") }
            val intactLinks = links.filter(StrUtil::isNotBlank).map { link1 -> urlUtil(link, link1) }.filter { link1 ->
                (link1.matches(regex) || link1.matches(regexMedium))
            }
            for (link1 in intactLinks) {
                urlQueue.add(link1)
            }
            if (i.get() == 1L) {
                (1..thread).forEach { Thread(this).start() }
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
                pixivWebSocketHandler.sendMessageToSessionId(sessionId, map)
                val file = File("/pixivDownload")
                if (!file.exists()) file.mkdir()
                val outStream = FileOutputStream("/pixivDownload/$img")
                ByteStreams.copy(imgResponse.body()!!.byteStream(), outStream)
                outStream.flush()
                outStream.close()
                map.put("message", "下载完成：$img")
                map.put("class", "alert-info")
                pixivWebSocketHandler.sendMessageToSessionId(sessionId, map)
            }
            DownloadMeans.BROWSER -> {
                val bytes = imgResponse.body()!!.bytes()
                val list = bytes.map { it.toString() }

                map.put("type", "file")
                map.put(img, list)
                pixivWebSocketHandler.sendMessageToSessionId(sessionId, map)
            }
        }
        count.getAndAdd(1)
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

    override fun run() {
        download()
    }

    fun start() {
        Thread(this).start()
    }

    enum class Type {
        MANGA,
        ALL,
        MEDIUM,
        UGOIRA,
        BOOKMARK
    }

    enum class DownloadMeans {
        BROWSER,
        LOCAL
    }

}
