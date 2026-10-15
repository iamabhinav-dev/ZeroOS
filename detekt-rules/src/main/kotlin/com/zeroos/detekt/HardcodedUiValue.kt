package com.zeroos.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtConstantExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtNameReferenceExpression

/**
 * NFR-L8: colors and sizes come from core/ui theme tokens. Flags `Color(<literal>)` and `<number>.dp` / `.sp`
 * (except zero). Configure `excludes` for core/ui, where the tokens are defined.
 */
class HardcodedUiValue(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Style,
        "Use theme tokens from core/ui instead of hardcoded colors and sizes (NFR-L8).",
        Debt.FIVE_MINS,
    )

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val isColor = expression.calleeExpression?.text == "Color"
        val literalArgs = expression.valueArguments.isNotEmpty() &&
            expression.valueArguments.all { it.getArgumentExpression() is KtConstantExpression }
        if (isColor && literalArgs) {
            report(CodeSmell(issue, Entity.from(expression), "Hardcoded color: use a core/ui color token."))
        }
    }

    override fun visitDotQualifiedExpression(expression: KtDotQualifiedExpression) {
        super.visitDotQualifiedExpression(expression)
        val receiver = expression.receiverExpression as? KtConstantExpression ?: return
        val unit = (expression.selectorExpression as? KtNameReferenceExpression)?.getReferencedName() ?: return
        if (unit in UNITS && !receiver.isZero()) {
            report(CodeSmell(issue, Entity.from(expression), "Hardcoded size ${expression.text}: use a core/ui dimension token."))
        }
    }

    private fun KtConstantExpression.isZero() = text.trimEnd('f', 'F', 'L').toDoubleOrNull() == 0.0

    private companion object {
        val UNITS = setOf("dp", "sp")
    }
}
