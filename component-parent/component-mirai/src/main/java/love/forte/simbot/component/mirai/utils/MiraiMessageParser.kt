/*
 *
 *  * Copyright (c) 2020. ForteScarlet All rights reserved.
 *  * Project  simple-robot
 *  * File     MiraiAvatar.kt
 *  *
 *  * You can contact the author through the following channels:
 *  * github https://github.com/ForteScarlet
 *  * gitee  https://gitee.com/ForteScarlet
 *  * email  ForteScarlet@163.com
 *  * QQ     1149159218
 *
 */

@file:JvmName("MiraiMessageParsers")

package love.forte.simbot.component.mirai.utils

import cn.hutool.core.io.FileUtil
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import love.forte.catcode.*
import love.forte.catcode.codes.Nyanko
import love.forte.simbot.api.message.MessageContent
import love.forte.simbot.component.mirai.message.*
import net.mamoe.mirai.contact.Group
import net.mamoe.mirai.message.data.*
import net.mamoe.mirai.message.data.Image.Key.queryUrl
import net.mamoe.mirai.utils.ExternalResource.Companion.toExternalResource
import net.mamoe.mirai.utils.ExternalResource.Companion.uploadAsImage
import net.mamoe.mirai.utils.MiraiExperimentalApi
import java.io.File
import java.io.InputStream

// /**
//  * message chain 为 [EmptyMessageChain] 的 [MiraiMessageContent]。
//  */
// @get:JvmName("getEmptyMiraiMessageContent")
// public val EmptyMiraiMessageContent: MiraiMessageContent = MiraiMessageChainContent(EmptyMessageChain)




/**
 * 将一个 [MessageContent] 转化为一个 [MiraiMessageContent]。
 */
public fun MessageContent.toMiraiMessageContent(message: MessageChain?): MiraiMessageContent {
    return if(this is MiraiMessageContent) {
        this
    } else {
        msg.toMiraiMessageContent(message)
    }
}


/**
 * 将可能存在catcode的字符串文本转化为 [MiraiMessageContent]。
 */
public fun String.toMiraiMessageContent(message: MessageChain?): MiraiMessageContent {
    return CatCodeUtil.split(this) {
        // cat code.
        if (startsWith(CAT_HEAD)) Nyanko.byCode(this).toMiraiMessageContent(message)
        // normal text.
        else MiraiSingleMessageContent(PlainText(this.deCatText()))
    }.let { MiraiListMessageContent(it) }
}

/**
 * [Neko] 转化为 [MiraiMessageContent]。
 */
@OptIn(MiraiExperimentalApi::class)
public fun Neko.toMiraiMessageContent(message: MessageChain?): MiraiMessageContent {
    return when (this.type) {
        "at" -> {
            val all = this["all"] == "true"
            val code = this["code"]?.toLong()
            if (all) {
                MiraiSingleMessageContent(AtAll)
            } else {
                // codes not empty.
                code?.let { MiraiSingleAtMessageContent(it) } ?: throw IllegalArgumentException("no at 'code' in $this.")
            }
        }

        "atAll", "atall" -> MiraiSingleMessageContent(AtAll)

        // face
        "face" -> {
            val id: Int = this["id"]?.toInt() ?: throw IllegalArgumentException("no face 'id' in $this.")
            MiraiSingleMessageContent(Face(id))
        }

        // 戳一戳，窗口抖动
        "poke", "shake" -> {
            val type: Int = this["type"]?.toInt() ?: return MiraiSingleMessageContent(PokeMessage.ChuoYiChuo)
            val id: Int = this["id"]?.toInt() ?: -1
            val code: Long = this["code"]?.toLong() ?: -1L
            val cat = this
            MiraiSingleMessageContent({
                if (it is Group) {
                    // nudge, need code
                    if (code == -1L) {
                        throw IllegalStateException("Unable to locate the target for nudge: no 'code' parameter in cat ${this@toMiraiMessageContent}.")
                    }

                    val nudge = it[code]?.nudge()
                        ?: throw IllegalArgumentException("cannot found nudge target: no such member($code) in group($id).")
                    it.launch { nudge.sendTo(it) }
                    EmptySingleMessage
                } else {
                    // poke.
                    PokeMessage.values.find { p -> p.pokeType == type && p.id == id } ?: PokeMessage.ChuoYiChuo
                }
            }, cat)
        }

        // 头像抖动
        "nudge" -> {
            val target = this["target"]
            MiraiNudgedMessageContent(target?.toLong())
        }


        // image
        "image" -> {
            val id = this["id"]
            if (id != null) {
                // id, if contains
                if (message != null) {
                    val foundImg = message.find {
                        (it is Image && it.imageId == id) ||
                                (it is FlashImage && it.image.imageId == id)
                    }
                    if (foundImg != null) {
                        return MiraiSingleMessageContent(foundImg)
                    }
                }

                return MiraiSingleMessageContent(Image(id))
            }





            // file, or url
            val filePath = this["file"]
            val file: File? = filePath?.let { FileUtil.file(it) }?.takeIf { it.exists() }
            val flash: Boolean = this["flash"] == "true"
            if (file != null) {
                // 存在文件
                val imageNeko = CatCodeUtil.nekoTemplate.image(filePath)
                MiraiImageMessageContent(flash, imageNeko) { c -> file.uploadAsImage(c) }
            } else {
                // 没有文件，看看有没有url。
                val url = filePath?.takeIf { it.startsWith("http") }?.let { Url(it) }
                    ?: this["url"]?.let { Url(it) }
                    ?: throw IllegalArgumentException("The img has no source in $this")

                // val urlId = url.encodedPath
                val imageNeko = CatCodeUtil.nekoTemplate.image(url.toString())
                MiraiImageMessageContent(flash, imageNeko) { c ->
                    url.toStream().uploadAsImage(c)
                }
            }
        }

        // voice
        "voice" -> {
            if (message != null) {
                val id = this["id"]
                if (id != null) {
                    val findVoice = message.find { it is Voice && it.id == id }
                    if (findVoice != null) {
                        return MiraiSingleMessageContent(findVoice)
                    }
                }
            }


            // file, or url
            val filePath = this["file"]
            val file: File? = filePath?.let { FileUtil.file(it) }?.takeIf { it.exists() }
            if (file != null) {
                // 存在文件
                val recordNeko = CatCodeUtil.nekoTemplate.record(filePath)
                MiraiVoiceMessageContent(recordNeko) { c ->
                    if (c is Group) {
                        file.toExternalResource().use { c.uploadVoice(it) }
                    } else throw IllegalStateException("Mirai does not support sending private voice.")
                }
            } else {
                // 没有文件，看看有没有url。
                val url = filePath?.takeIf { it.startsWith("http") }?.let { Url(it) }
                    ?: this["url"]?.let { Url(it) }
                    ?: throw IllegalArgumentException("The voice has no source in $this")

                val urlId = url.encodedPath
                val recordNeko = CatCodeUtil.nekoTemplate.record(urlId)
                MiraiVoiceMessageContent(recordNeko) { c ->
                    if (c is Group) {
                        url.toStream().toExternalResource().use { c.uploadVoice(it) }
                    } else throw IllegalStateException("Mirai does not support sending private voice.")
                }
            }
        }

        // 分享
        "share" -> {
            // 至少需要一个url
            val url: String = this["url"] ?: throw IllegalArgumentException("The 'url' could not be found in $this.")
            val title: String? =this["title"]
            val content: String? = this["content"]
            val coverUrl: String? = this["coverUrl"]

            MiraiSingleMessageContent(RichMessage.share(url, title, content, coverUrl))
        }

        "rich" -> {
            val content: String = this["content"] ?: "{}"
            // 如果没有serviceId，认为其为lightApp
            val serviceId: Int = this["serviceId"]?.toInt() ?: return MiraiSingleMessageContent(LightApp(content))
            MiraiSingleMessageContent(SimpleServiceMessage(serviceId, content))
        }


        "app", "json" -> {
            val content: String = this["content"] ?: "{}"
            MiraiSingleMessageContent(LightApp(content))
        }


        "xml" -> {
            val xmlCode = this
            // 解析的参数
            val serviceId = this["serviceId"]?.toInt() ?: 60
            // 构建xml
            val xml = buildXmlMessage(serviceId) {
                // action
                xmlCode["action"]?.also { this.action = it }
                // 一般为点击这条消息后跳转的链接
                xmlCode["actionData"]?.also { this.actionData = it }
                /*
                   摘要, 在官方客户端内消息列表中显示
                 */
                xmlCode["brief"]?.also { this.brief = it }
                xmlCode["flag"]?.also { this.flag = it.toInt() }
                xmlCode["url"]?.also { this.url = it }
                // sourceName 好像是名称
                xmlCode["sourceName"]?.also { this.sourceName = it }
                // sourceIconURL 好像是图标
                xmlCode["sourceIconURL"]?.also { this.sourceIconURL = it }

                // builder
//                val keys = xmlCode.params.keys

                this.item {
                    xmlCode["bg"]?.also { this.bg = it.toInt() }
                    xmlCode["layout"]?.also { this.layout = it.toInt() }
                    // picture(coverUrl: String)
                    xmlCode["picture_coverUrl"]?.also { this.picture(it) }
                    // summary(text: String, color: String = "#000000")
                    xmlCode["summary_text"]?.also {
                        val color: String = xmlCode["summary_color"] ?: "#000000"
                        this.summary(it, color)
                    }
                    // title(text: String, size: Int = 25, color: String = "#000000")
                    xmlCode["title_text"]?.also {
                        val size: Int = xmlCode["title_size"]?.toInt() ?: 25
                        val color: String = xmlCode["title_color"] ?: "#000000"
                        this.title(it, size, color)
                    }

                }
            }
            MiraiSingleMessageContent(xml)
        }


        else -> {
            val kvs = this.entries.joinToString(",") { it.key + "=" + it.value }
            MiraiSingleMessageContent(PlainText("$type($kvs)"))

        }

    }


}


/**
 * 将一个 [MessageChain] 转化为携带catcode的字符串。
 */
public fun MessageChain.toCatCode(): String {
    return this.asSequence().map { it.toNeko() }.joinToString("") { it.toString() }
}

/**
 * 将一个 [MessageChain] 转化为携带 [Neko] 的列表。
 */
public fun MessageChain.toNeko(): List<Neko> {
    return this.mapNotNull {
        if (it is MessageSource) {
            null
        } else {
            it.toNeko()
        }
    }
}


/**
 * 将一个 [SingleMessage] 转化为携带cat字符串。
 * 普通文本会被转化为 [CAT:text,text=xxx]
 */
public fun SingleMessage.toNeko(): Neko {
    return when (this) {
        // at all
        AtAll -> CatCodeUtil.nekoTemplate.atAll()
        // at
        is At -> CatCodeUtil.nekoTemplate.at(target)
        // 普通文本, 转义
        is PlainText -> CatCodeUtil.toNeko("text", false, "text=${CatEncoder.encodeParams(content)}")
        // face
        is Face -> CatCodeUtil.nekoTemplate.face(id.toString())

        // market face
        is MarketFace -> CatCodeUtil.getNekoBuilder("marketFace", true)
            .key("id").value(id)
            .key("name").value(name)
            .build()

        // vip face
        is VipFace -> CatCodeUtil.getNekoBuilder("vipFace", true)
            .key("kindId").value(this.kind.id)
            .key("kindName").value(this.kind.name)
            .key("count").value(this.count)
            .build()

        is PokeMessage -> {
            // poke, 戳一戳
            CatCodeUtil.getNekoBuilder("poke", false)
                .key("type").value(pokeType)
                .key("id").value(id)
                .build()
        }
        is Image -> {
            CatCodeUtil.getLazyNekoBuilder("image", true)
                .key("id").value(imageId)
                .key("url").value { runBlocking { this@toNeko.queryUrl() } }
                .build()
        }
        is FlashImage -> {
            val img = this.image
            // cat code中不再携带url参数
            // CatCodeUtil.stringTemplate.image()
            CatCodeUtil.getLazyNekoBuilder("image", true)
                .key("id").value(img.imageId)
                .key("url").value { runBlocking { img.queryUrl() } }
                .key("flash").value(true)
                .build()
        }
        is Voice -> {
            CatCodeUtil.getLazyNekoBuilder("voice", true)
                .key("id").value { id }
                .key("name").value(fileName)
                .key("size").value(fileSize).apply {
                    url?.let { key("url").value(it) }
                }
                .build()

        }
        // 引用回复
        is QuoteReply -> {
            CatCodeUtil.getLazyNekoBuilder("quote", true)
                .key("id").value(with(this.source){ "$fromId.${this.ids.joinToString(",", "[", "]")}" })
                .key("quote").value { this.source.originalMessage.toCatCode() }
                .build()
        }


        // 富文本，xml或json
        is RichMessage -> CatCodeUtil.getNekoBuilder("rich", true)
            .key("content").value(content)
            .build()

        // else.
        else -> {
            CatCodeUtil.getNekoBuilder("mirai", true)
                .key("code").value(this.toString()).build()
        }
    }
}


/**
 * ktor http client
 */
private val httpClient: HttpClient = HttpClient()


/**
 * 通过http网络链接得到一个输入流。
 * 通常认为是一个http-get请求
 */
public suspend fun Url.toStream(): InputStream {
    val urlString = this.toString()
    // bot?.logger?.debug("mirai.http.connection.try", urlString)
    val response = httpClient.get<HttpResponse>(this)
    val status = response.status
    if (status.value < 300) {
        // debug("mirai.http.connection.success", urlString)
        // success
        return response.content.toInputStream()
    } else {
        throw IllegalStateException("connection to '$urlString' failed ${status.value}: ${status.description}")
    }
}



private val Voice.id: String get() = md5.decodeToString()