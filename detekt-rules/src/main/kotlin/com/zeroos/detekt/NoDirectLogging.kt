package com.zeroos.detekt

import io.gitlab.arturbosch.detekt.api.CodeSmell
import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.Debt
import io.gitlab.arturbosch.detekt.api.Entity
import io.gitlab.arturbosch.detekt.api.Issue
import io.gitlab.arturbosch.detekt.api.Rule
import io.gitlab.arturbosch.detekt.api.Severity
import org.jetbrains.kotlin.psi.KtCallExpression
import org.jetbrains.kotlin.psi.KtDotQualifiedExpression
import org.jetbrains.kotlin.psi.KtImportDirective

/**
 * F17.1: all logging goes through the ZeroOS Logger. Flags `android.util.Log` imports and calls, and
 * `print`/`println`. Configure `excludes` for core/logging, which wraps Logcat.
 */
class NoDirectLogging(config: Config = Config.empty) : Rule(config) {
    override val issue = Issue(
        javaClass.simpleName,
        Severity.Defect,
        "Use the Logger from core/logging instead of android.util.Log or println (F17.1).",
        Debt.FIVE_MINS,
    )

    override fun visitImportDirective(importDirective: KtImportDirective) {
        super.visitImportDirective(importDirective)
        if (importDirective.importedFqName?.asString() == "android.util.Log") {
            report(CodeSmell(issue, Entity.from(importDirective), "android.util.Log is not allowed; use Logger."))
        }
    }

    override fun visitCallExpression(expression: KtCallExpression) {
        super.visitCallExpression(expression)
        val callee = expression.calleeExpression?.text ?: return
        val receiver = (expression.parent as? KtDotQualifiedExpression)
            ?.takeIf { it.selectorExpression == expression }
            ?.receiverExpression?.text
        val isPrint = receiver == null && callee in PRINT_FUNCTIONS
        val isLogcat = (receiver == "Log" || receiver == "android.util.Log") && callee in LOG_FUNCTIONS
        if (isPrint || isLogcat) {
            report(CodeSmell(issue, Entity.from(expression), "Direct logging via ${expression.text.substringBefore('(')}; use Logger."))
        }
    }

    private companion object {
        val PRINT_FUNCTIONS = setOf("print", "println")
        val LOG_FUNCTIONS = setOf("v", "d", "i", "w", "e", "wtf", "println")
    }
}
