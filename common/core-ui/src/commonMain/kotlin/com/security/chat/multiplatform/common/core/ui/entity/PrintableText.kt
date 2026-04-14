package com.security.chat.multiplatform.common.core.ui.entity

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Immutable
import org.jetbrains.compose.resources.PluralStringResource
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.pluralStringResource
import org.jetbrains.compose.resources.stringResource

@Immutable
public sealed interface PrintableText {

    @Immutable
    public data class Raw(val s: String) : PrintableText

    @Immutable
    public class StringRes(
        public val resId: StringResource,
        vararg formatArgs: Any,
    ) : PrintableText {

        internal val params: Array<out Any> = formatArgs

    }

    @Immutable
    public class PluralRes(
        public val resId: PluralStringResource,
        public val quantity: Int,
        vararg formatArgs: Any,
    ) : PrintableText {

        internal val params: Array<out Any> = formatArgs

    }

    public companion object {
        public val EMPTY: PrintableText = Raw("")
    }

}

@Composable
public fun PrintableText.resolve(): String {
    return when (this) {
        is PrintableText.Raw -> this.s

        is PrintableText.PluralRes -> {
            pluralStringResource(
                this.resId,
                this.quantity,
                *this.params.resolveFormatArgs(),
            )
        }

        is PrintableText.StringRes -> {
            stringResource(
                this.resId,
                *this.params.resolveFormatArgs(),
            )
        }
    }
}

@Composable
internal fun Array<out Any>.resolveFormatArgs(): Array<Any> {
    return this
        .map {
            if (it is PrintableText) {
                it.resolve()
            } else {
                it
            }
        }
        .toTypedArray()
}

public fun rawPrintableText(string: String): PrintableText = PrintableText.Raw(string)

public fun resPrintableText(
    resId: StringResource,
    vararg formatArgs: Any,
): PrintableText = PrintableText.StringRes(resId, *formatArgs)