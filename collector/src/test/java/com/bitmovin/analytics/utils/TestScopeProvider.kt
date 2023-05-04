package com.bitmovin.analytics.utils

import io.mockk.spyk
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineName
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.isActive
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher

@OptIn(ExperimentalCoroutinesApi::class)
class TestScopeProvider(
    private val testDispatcher: TestDispatcher = UnconfinedTestDispatcher(),
) : ScopeProvider {
    val scopes = mutableListOf<CoroutineScope>()

    override fun createMainScope(name: String?) = createScope(testDispatcher, name).also { scopes.add(it) }
    override fun createIoScope(name: String?) = createScope(testDispatcher, name).also { scopes.add(it) }
    override fun createDefaultScope(name: String?) = createScope(testDispatcher, name).also { scopes.add(it) }
}

private fun createScope(dispatcher: CoroutineDispatcher, name: String?): CoroutineScope {
    var context = dispatcher + SupervisorJob()
    name?.let { context += CoroutineName(it) }
    return spyk(CoroutineScope(context))
}

val TestScopeProvider.areScopesCancelled: Boolean get() = scopes.none { it.isActive }
