package com.bitmovin.analytics.utils

import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

internal interface ScopeProvider {
    fun createMainScope(name: String? = null): CoroutineScope
    fun createIoScope(name: String? = null): CoroutineScope
    fun createDefaultScope(name: String? = null): CoroutineScope

    companion object {
        @JvmStatic
        fun create(): ScopeProvider = DefaultScopeProvider()
    }
}

internal class DefaultScopeProvider : ScopeProvider {
    override fun createMainScope(name: String?) = createScope(Dispatchers.Main, name)
    override fun createIoScope(name: String?) = createScope(Dispatchers.IO, name)
    override fun createDefaultScope(name: String?) = createScope(Dispatchers.Default, name)
}

private fun createScope(dispatcher: CoroutineDispatcher, name: String?): CoroutineScope {
    var context = dispatcher + SupervisorJob()
    name?.let { context += CoroutineName(it) }
    return CoroutineScope(context)
}
