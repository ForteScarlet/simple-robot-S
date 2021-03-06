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

package love.forte.simbot.listener


/**
 *
 * 监听函数的执行回执。
 *
 *
 *
 * @author ForteScarlet -> https://github.com/ForteScarlet
 */
public interface ListenResult<T> {

    /**
     * 是否执行成功。
     */
    fun isSuccess(): Boolean

    /**
     * 是否阻断接下来的监听函数的执行。
     */
    fun isBreak(): Boolean

    /**
     * 最终的执行结果。
     */
    val result: T?

    /**
     * 如果执行出现了异常，此处为异常。
     */
    val cause: Throwable?

    
    @Deprecated("Renamed to 'cause'", ReplaceWith("cause"))
    val throwable: Throwable? get() = cause


    /**
     * Default默认的无效实现, 会用于对一些无效化响应值进行判断。
     * @since 2.0.0
     */
    companion object Default : ListenResult<Nothing> {
        override fun isSuccess(): Boolean = false
        override fun isBreak(): Boolean = false
        override val result: Nothing? = null
        override val cause: Throwable? = null
    }

}

/**
 * 监听函数响应值工厂。
 */
interface ListenerResultFactory {
    fun getResult(result: Any?, listenerFunction: ListenerFunction, throwable: Throwable? = null): ListenResult<*>
}

