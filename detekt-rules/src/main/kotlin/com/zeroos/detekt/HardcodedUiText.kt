package com.zeroos.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtStringTemplateExpression
import org.jetbrains.kotlin.psi.KtValueArgument

/**
 * NFR-L4: user-facing text must come from string resources. Flags string literals containing letters passed
 * to text-showing composables, ZeroOS's own components and toasts (at their text positions), or to text-like
 * named parameters.
 */
class HardcodedUiText(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "User-facing text must use stringResource(R.string.…) from core/resources (NFR-L4).",
        Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text ?: return
        expression.valueArguments.forEachIndexed { index, argument ->
            val name = argument.getArgumentName()?.asName?.asString()
            val isTextArgument = when (name) {
                null -> index in TEXT_POSITIONS[callee].orEmpty()
                else -> name in TEXT_PARAMETERS
            }
            if (isTextArgument && argument.isLiteralWithLetters()) {
                report(CodeSmell(issue, Entity.from(argument), "Hardcoded UI text in $callee(): use a string resource."))
            }
        }
    }

    private fun KtValueArgument.isLiteralWithLetters(): Boolean {
        val literal = getArgumentExpression() as? KtStringTemplateExpression ?: return false
        return literal.entries.any { entry -> entry.expression == null && entry.text.any(Char::isLetter) }
    }

    private companion object {
        /** Which positional arguments are shown as text, per function. */
        val TEXT_POSITIONS: Map<String, Set<Int>> = mapOf(
            // Compose
            "Text" to setOf(0),
            "BasicText" to setOf(0),
            "ClickableText" to setOf(0),
            // ZeroOS components in core/ui and feature modules
            "ScreenTitle" to setOf(0),
            "BodyText" to setOf(0),
            "AppLabel" to setOf(0),
            "MinimalListItem" to setOf(0),
            "ToggleRow" to setOf(0),
            "TextAction" to setOf(0),
            "HoldToConfirm" to setOf(0),
            "NavRow" to setOf(0),
            "Line" to setOf(0),
            "SettingRow" to setOf(0, 1),
            // Toast.makeText(context, text, duration)
            "makeText" to setOf(1),
        )
        val TEXT_PARAMETERS = setOf(
            "text", "contentDescription", "label", "placeholder", "title", "message", "stateDescription", "trailing", "value", "hint"
        )
    }
}
