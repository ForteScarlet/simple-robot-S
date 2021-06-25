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

@file:JvmName("SerializerModuleRegistrars")

package love.forte.simbot.component.kaiheila

import kotlinx.serialization.KSerializer
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import kotlinx.serialization.json.Json
import kotlinx.serialization.modules.SerializersModule
import kotlinx.serialization.modules.SerializersModuleBuilder
import love.forte.simbot.component.kaiheila.`object`.Channel
import love.forte.simbot.component.kaiheila.`object`.Guild
import love.forte.simbot.component.kaiheila.`object`.Role
import love.forte.simbot.component.kaiheila.`object`.User
import org.jetbrains.annotations.TestOnly
import java.util.concurrent.CopyOnWriteArraySet


public interface SerializerModuleRegistrar {
    fun SerializersModuleBuilder.serializerModule()
}


private val serializerModuleRegistrars = CopyOnWriteArraySet<SerializerModuleRegistrar>()


public fun init() {
    serializerModuleRegistrars.add(Role)
    serializerModuleRegistrars.add(Channel)
    serializerModuleRegistrars.add(User)
    serializerModuleRegistrars.add(Guild)
}


public data class KaiheilaJson(val json: Json)


@get:TestOnly
public val khlJson: Json by lazy {
    Json {
        init()
        serializersModule = SerializersModule {
            serializerModuleRegistrars.forEach {
                it.apply {
                    serializerModule()
                }
            }
        }
        isLenient = true
        ignoreUnknownKeys = true
        classDiscriminator = "#KHLT"

    }
}

public object BooleanAsIntSerializer : KSerializer<Boolean> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("BooleanAsInt", PrimitiveKind.INT)
    override fun deserialize(decoder: Decoder): Boolean = decoder.decodeInt() == 0
    override fun serialize(encoder: Encoder, value: Boolean) {
        encoder.encodeInt(if (value) 0 else 1)
    }
}