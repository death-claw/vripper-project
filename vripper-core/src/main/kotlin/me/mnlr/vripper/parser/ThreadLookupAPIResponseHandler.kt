package me.mnlr.vripper.parser

import org.xml.sax.Attributes
import org.xml.sax.helpers.DefaultHandler
import me.mnlr.vripper.SpringContext
import me.mnlr.vripper.delegate.LoggerDelegate
import me.mnlr.vripper.host.Host
import me.mnlr.vripper.model.ImageItem
import me.mnlr.vripper.model.PostItem
import me.mnlr.vripper.model.ThreadItem
import me.mnlr.vripper.services.SettingsService

class ThreadLookupAPIResponseHandler : DefaultHandler() {
    private val log by LoggerDelegate()
    private val supportedHosts: Collection<Host> =
        SpringContext.getBeansOfType(Host::class.java).values
    private val settingsService: SettingsService =
        SpringContext.getBean(SettingsService::class.java)
    private var error: String = ""
    private val hostMap: MutableMap<Host, Int> = mutableMapOf()
    private val postItemList: MutableList<PostItem> = mutableListOf()
    private val imageItemList: MutableList<ImageItem> = mutableListOf()
    private lateinit var threadItem: ThreadItem
    private lateinit var threadId: String
    private lateinit var threadTitle: String
    private lateinit var forum: String
    private lateinit var securityToken: String
    private lateinit var postId: String
    private lateinit var postTitle: String
    private var postCounter: Int = 0

    val result: ThreadItem
        get() = threadItem

    override fun startDocument() {}
    override fun startElement(
        uri: String, localName: String, qName: String, attributes: Attributes
    ) {
        when (qName.lowercase()) {
            "error" -> error = attributes.getValue("details")
            "thread" -> {
                threadId = attributes.getValue("id")?.trim() ?: ""
                threadTitle = attributes.getValue("title")?.trim() ?: ""
            }

            "forum" -> forum = attributes.getValue("title")?.trim() ?: ""
            "user" -> securityToken = attributes.getValue("hash")?.trim() ?: ""
            "post" -> {
                postId = attributes.getValue("id")?.trim() ?: ""
                postCounter = attributes.getValue("number")?.trim()?.toInt() ?: 0
                val title = attributes.getValue("title")?.trim() ?: ""
                postTitle = title.ifBlank { threadTitle }
            }

            "image" -> {
                val mainLink = attributes.getValue("main_url")?.trim() ?: ""
                val thumbLink = attributes.getValue("thumb_url")?.trim() ?: ""
                val type = attributes.getValue("type")?.trim() ?: ""
                if(type == "linked") {
                    try {
                        val host = supportedHosts.first {
                            it.isSupported(mainLink)
                        }.let { host ->
                            hostMap.computeIfAbsent(host) { 0 }
                            hostMap[host] = hostMap[host]!! + 1
                            host
                        }
                        imageItemList.add(ImageItem(mainLink, thumbLink, host))
                    } catch (e: Exception) {
                        log.warn("Unsupported link: $mainLink")
                    }
                }
            }
        }
    }

    override fun endElement(uri: String, localName: String, qName: String) {
        if ("post".equals(qName, true)) {
            if (imageItemList.isNotEmpty()) {
                postItemList.add(
                    PostItem(
                        threadId,
                        threadTitle,
                        postId,
                        postCounter,
                        postTitle,
                        imageItemList.size,
                        "${settingsService.settings.viperSettings.host}/threads/?p=$postId&viewfull=1#post$postId",
                        hostMap.toMap().map { Pair(it.key.host, it.value) },
                        securityToken,
                        forum,
                        imageItemList.toList()
                    )
                )
            }
            imageItemList.clear()
            hostMap.clear()
        }
    }

    override fun endDocument() {
        threadItem = ThreadItem(threadId, threadTitle, securityToken, forum, postItemList.toList())
    }
}