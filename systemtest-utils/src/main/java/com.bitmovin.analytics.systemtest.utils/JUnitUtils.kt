package com.bitmovin.analytics.systemtest.utils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.runBlocking
import org.junit.rules.TestRule
import org.junit.runner.Description
import org.junit.runners.model.Statement

/**
 * Repeat annotation to repeat a test multiple times
 * ```kotlin
 *
 * @Rule @JvmField
 * val repeatRule = RepeatRule()
 *
 * ...
 *
 * @Test
 * @Repeat(5)
 * fun test() {
 *     // test code
 * }
 * ```
 */
@Retention(AnnotationRetention.RUNTIME)
@Target(AnnotationTarget.FUNCTION, AnnotationTarget.ANNOTATION_CLASS)
annotation class Repeat(val value: Int = 1)

/**
 * Has to be used as a rule in the test class for the repeat annotation to work
 * ```kotlin
 * @Rule @JvmField
 * val repeatRule = RepeatRule()
 * ```
 */
class RepeatRule : TestRule {
    private class RepeatStatement(private val statement: Statement, private val repeat: Int) : Statement() {
        @Throws(Throwable::class)
        override fun evaluate() {
            for (i in 0 until repeat) {
                statement.evaluate()
            }
        }
    }

    override fun apply(
        statement: Statement,
        description: Description,
    ): Statement {
        var result = statement
        val repeat = description.getAnnotation(Repeat::class.java)
        if (repeat != null) {
            val times = repeat.value
            result = RepeatStatement(statement, times)
        }
        return result
    }
}

/**
 * Should be used to run async operations in a test.
 *
 * Why context : JUnit does not allow tests to be suspendable function yet.
 * At the mean time, a lot of tests needs to work on the main Thread and we want to be 100% on that the action have resulted.
 * The easiest way to wait for another Coroutine to be done to continue is withContext, which is a blocking function.
 * Wrapped into `runBlockedTest`, content will be in a coroutine scope without adding a suspend to the function signature.
 * ```kotlin
 * @Test
 * fun test() = runBlockingTest {
 *    // test code
 *    ...
 *    withContext(...) {...}
 *    ...
 * }
 * ```
 * @since [AN-4182](https://bitmovin.atlassian.net/browse/AN-4182)
 */
fun <T> runBlockingTest(block: suspend CoroutineScope.() -> T) {
    runBlocking {
        block()
    }
    return
}
