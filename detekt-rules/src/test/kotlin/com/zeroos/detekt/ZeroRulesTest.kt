package com.zeroos.detekt

import io.gitlab.arturbosch.detekt.api.Config
import io.gitlab.arturbosch.detekt.test.lint
import org.junit.Assert.assertEquals
import org.junit.Test

class ZeroRulesTest {

    @Test
    fun nfrL4_flagsLiteralTextInComposables() {
        val code = """
            fun Screen() {
                Text("Hello")
                Text(text = "Hi")
                Icon(contentDescription = "Back")
                Text(stringResource(R.string.ok))
                Text("")
                Text("${'$'}count")
                Text("42")
            }
        """.trimIndent()
        assertEquals(3, HardcodedUiText(Config.empty).lint(code).size)
    }

    @Test
    fun nfrL8_flagsLiteralColorsAndSizes() {
        val code = """
            fun Screen() {
                val a = Color(0xFF000000)
                val b = Color(1f, 0f, 0f)
                val c = 16.dp
                val d = 12.sp
                val e = 0.dp
                val f = Color.Black
                val g = ZeroTheme.spacing.small
            }
        """.trimIndent()
        assertEquals(4, HardcodedUiValue(Config.empty).lint(code).size)
    }

    @Test
    fun f17_1_flagsDirectLogging() {
        val code = """
            import android.util.Log
            fun f() {
                Log.d("tag", "msg")
                println("x")
                print("y")
                logger.info("home", "ok")
                list.print()
            }
        """.trimIndent()
        assertEquals(4, NoDirectLogging(Config.empty).lint(code).size)
    }
}
