package com.zeroos.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.api.RuleSet
import io.gitlab.arturbosch.detekt.api.RuleSetProvider

class ZeroRuleSetProvider : RuleSetProvider {
    override val ruleSetId: String = "zeroos"

    override fun instance(config: Config): RuleSet = RuleSet(
        ruleSetId,
        listOf(
            HardcodedUiText(config),
            HardcodedUiValue(config),
            NoDirectLogging(config),
        ),
    )
}
