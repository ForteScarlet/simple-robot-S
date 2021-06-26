/*
 *
 *  * Copyright (c) 2021. ForteScarlet All rights reserved.
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

package love.forte.simbot.component.kaiheila.api.v3.guild

import io.ktor.http.*
import kotlinx.serialization.DeserializationStrategy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import love.forte.simbot.component.kaiheila.`object`.Channel
import love.forte.simbot.component.kaiheila.`object`.Role
import love.forte.simbot.component.kaiheila.api.*


/**
 * [获取服务器详情](https://developer.kaiheila.cn/doc/http/guild#%E8%8E%B7%E5%8F%96%E6%9C%8D%E5%8A%A1%E5%99%A8%E8%AF%A6%E6%83%85)
 *
 * api: /guild/view
 *
 * request method: GET
 *
 */
public class GuildViewReq(private val guildId: String) : GuildApiReq<ObjectResp<GuildView>> {
    companion object Key : ApiData.Req.Key by key("/guild/view") {
        private val ROUTE = listOf("guild", "view")
    }
    override val method: HttpMethod
        get() = HttpMethod.Get

    override val key: ApiData.Req.Key
        get() = Key

    override val body: Any?
        get() = null

    override val dataSerializer: DeserializationStrategy<ObjectResp<GuildView>>
        get() = objectResp(GuildView.serializer())

    override fun route(builder: RouteInfoBuilder) {
        builder.apiPath = ROUTE
        builder.parameters {
            append("guild_id", guildId)
        }
    }
}


/*
{
    "code": 0,
    "message": "操作成功",
    "data": {
        "id": "91686000000",
        "name": "Hello",
        "topic": "",
        "master_id": "17000000",
        "is_master": false,
        "icon": "",
        "invite_enabled": true,
        "notify_type": 2,
        "region": "beijing",
        "enable_open": true,
        "open_id": "1600000",
        "default_channel_id": "2710000000",
        "welcome_channel_id": "0",
        "features": [],
        "roles": [
            {
                "role_id": 0,
                "name": "@全体成员",
                "color": 0,
                "position": 999,
                "hoist": 0,
                "mentionable": 0,
                "permissions": 148691464
            }
        ],
        "channels": [
            {
                "id": "37090000000",
                "user_id": "1780000000",
                "parent_id": "0",
                "name": "Hello World",
                "type": 1,
                "level": 100,
                "limit_amount": 0,
                "is_category": false,
                "is_readonly": false,
                "is_private": false
            }
        ],
        "emojis": [
            {
                "name": "ceeb65XXXXXXX0j60jpwfu",
                "id": "9168XXXXX53/4c43fcb7XXXXX0c80ck"
            }
        ],
        "user_config": {
            "notify_type": null,
            "nickname": "XX",
            "role_ids": [
                702
            ],
            "chat_setting": "1"
        }
    }
}

 */

@Serializable
public data class GuildView(
    val id: String,
    val name: String,
    val topic: String,
    @SerialName("master_id")
    val masterId: String,
    val icon: String,
    @SerialName("notify_type")
    val notifyType: Int,
    val region: String,
    @SerialName("enable_open")
    val enableOpen: Boolean,
    @SerialName("open_id")
    val openId: String,
    @SerialName("default_channel_id")
    val defaultChannelId: String,
    @SerialName("welcome_channel_id")
    val welcomeChannelId: String,
    val roles: List<Role>,
    val channels: List<Channel>
) : GuildApiRespData





