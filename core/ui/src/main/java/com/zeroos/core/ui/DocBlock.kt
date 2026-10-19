package com.zeroos.core.ui

/** One block of a document as the reader shows it (wireframe K3). */
sealed interface DocBlock {
    val text: String

    data class Heading(override val text: String) : DocBlock

    data class Paragraph(override val text: String) : DocBlock

    data class Bullet(override val text: String) : DocBlock

    /** A numbered step; [text] keeps its number ("1. Use a phone…"). */
    data class Numbered(override val text: String) : DocBlock
}

private val HEADING = Regex("^#{1,6}\\s+")
private val BULLET = Regex("^\\s*[-*]\\s+")
private val NUMBERED = Regex("^\\s*\\d+\\.\\s+")
private val NUMBERED_TITLE = Regex("^\\d+\\.\\s+[A-Z][A-Z ,—'-]+$")
private val LINK = Regex("\\[([^]]+)]\\([^)]*\\)")
private val COMMENT = Regex("<!--.*?-->", RegexOption.DOT_MATCHES_ALL)
private val TABLE_RULE = Regex("^\\|?\\s*:?-{3,}")

private enum class LineKind { CODE_FENCE, BREAK, HEADING, TABLE_ROW, BULLET, NUMBERED, TEXT }

/**
 * Turns the README (Markdown) or LICENSE (plain text) into blocks: headings, paragraphs and list items. Hard-wrapped
 * lines are joined; links keep their text; tables become "a — b" lines; code blocks and comments are left out.
 */
fun parseDocument(text: String): List<DocBlock> {
    val document = DocumentBuilder()
    var inCode = false
    for (raw in COMMENT.replace(text, "").lines()) {
        val line = clean(raw)
        val kind = kindOf(raw, line)
        if (kind == LineKind.CODE_FENCE) {
            document.flush()
            inCode = !inCode
        } else if (!inCode) {
            document.take(kind, line)
        }
    }
    document.flush()
    return document.blocks
}

private fun kindOf(raw: String, line: String): LineKind = when {
    raw.trimStart().startsWith("```") -> LineKind.CODE_FENCE
    line.isBlank() || TABLE_RULE.containsMatchIn(raw.trim()) -> LineKind.BREAK
    HEADING.containsMatchIn(raw) || NUMBERED_TITLE.matches(line) || isAllCapsTitle(line) -> LineKind.HEADING
    raw.trimStart().startsWith("|") -> LineKind.TABLE_ROW
    BULLET.containsMatchIn(raw) -> LineKind.BULLET
    NUMBERED.containsMatchIn(raw) -> LineKind.NUMBERED
    else -> LineKind.TEXT
}

/** Collects lines into blocks; a block stays open (wrapped lines join it) until a break or a new block. */
private class DocumentBuilder {
    val blocks = mutableListOf<DocBlock>()
    private val open = StringBuilder()
    private var make: (String) -> DocBlock = DocBlock::Paragraph

    fun take(kind: LineKind, line: String) = when (kind) {
        LineKind.BREAK, LineKind.CODE_FENCE -> flush()

        LineKind.HEADING -> add(DocBlock.Heading(HEADING.replace(line, "")))

        LineKind.TABLE_ROW -> add(
            DocBlock.Paragraph(
                line.trim('|').split("|").map {
                    it.trim()
                }.filter { it.isNotEmpty() }.joinToString(" — ")
            )
        )

        LineKind.BULLET -> start(DocBlock::Bullet, BULLET.replace(line, ""))

        LineKind.NUMBERED -> start(DocBlock::Numbered, line.trim())

        LineKind.TEXT -> join(line.trim())
    }

    fun flush() {
        if (open.isNotEmpty()) blocks += make(open.toString())
        open.clear()
        make = DocBlock::Paragraph
    }

    private fun add(block: DocBlock) {
        flush()
        blocks += block
    }

    private fun start(kind: (String) -> DocBlock, first: String) {
        flush()
        make = kind
        open.append(first)
    }

    private fun join(line: String) {
        if (open.isNotEmpty()) open.append(' ')
        open.append(line)
    }
}

private fun clean(line: String): String = LINK.replace(line, "$1").replace("**", "").replace("`", "").trimEnd()

private fun isAllCapsTitle(line: String): Boolean {
    val letters = line.filter { it.isLetter() }
    return letters.length >= MIN_TITLE_LETTERS && letters.all { it.isUpperCase() } && line.length <= MAX_TITLE_LENGTH
}

private const val MIN_TITLE_LETTERS = 4
private const val MAX_TITLE_LENGTH = 60
