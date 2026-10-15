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
 * NFR-L4: user-facing text must come from string resources. Flags string literals containing letters
 * passed to text-showing composables (positionally) or to text-like named parameters.
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
                null -> index == 0 && callee in TEXT_COMPOSABLES
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
        val TEXT_COMPOSABLES = setOf("Text", "BasicText", "ClickableText")
        val TEXT_PARAMETERS = setOf("text", "contentDescription", "label", "placeholder", "title", "message", "stateDescription")
    }
}
