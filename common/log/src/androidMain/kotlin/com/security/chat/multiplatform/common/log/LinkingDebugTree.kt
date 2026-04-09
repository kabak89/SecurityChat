package com.security.chat.multiplatform.common.log

import timber.log.Timber
import java.util.regex.Pattern

/**
 * Timber tree for debug logging. Adds code line to logs
 *
 * inspired by https://medium.com/@nesterov.vas/kotlin-adaptation-timber-version-e735116d4715
 */
internal class LinkingDebugTree : Timber.DebugTree() {

    override fun log(priority: Int, tag: String?, message: String, t: Throwable?) {
        @Suppress("ThrowingExceptionsWithoutMessageOrCause")
        val stackTrace = Throwable().stackTrace
        val traceElement = getStackTraceElement(stackTrace)
        val className = extractClassName(traceElement)
        val lineNumber = traceElement.lineNumber
        val adjustedMessage = ".($className:$lineNumber) - $message"
        val customTag = createStackElementTag(traceElement) ?: tag

        super.log(
            // priority
            priority,
            // tag
            customTag,
            // message
            adjustedMessage,
            // t
            t,
        )
    }

    /**
     * Extract the class name without any anonymous class suffixes (e.g., `Foo$1`
     * becomes `Foo`).
     */
    private fun extractClassName(element: StackTraceElement): String {
        var tag = element.fileName ?: element.className ?: ""
        val matcher = ANONYMOUS_CLASS.matcher(tag)
        if (matcher.find()) {
            tag = matcher.replaceAll("")
        }
        return tag
    }

    private fun getStackTraceElement(stackTrace: Array<StackTraceElement>): StackTraceElement {
        val traceElement = stackTrace
            .firstOrNull { element ->
                val className = element.className
                !className.contains("timber.log.Timber") &&
                        !className.contains("com.security.chat.multiplatform.common.log")
            } ?: stackTrace.first()
        return traceElement
    }
}

private val ANONYMOUS_CLASS = Pattern.compile("(\\$\\d+)+$")
