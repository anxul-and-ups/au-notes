package com.example.ui.util

import androidx.compose.ui.graphics.Color

/** Small, fast syntax colouring for the code boxes (editor + read mode). Colours only; text never changes. */
object SyntaxHighlighter {
    data class Token(val start: Int, val end: Int, val color: Color)

    private val cLike = setOf(
        "if", "else", "for", "while", "do", "return", "break", "continue", "switch", "case", "default", "when", "in", "is", "as",
        "fun", "val", "var", "class", "object", "interface", "enum", "data", "sealed", "abstract", "open", "override", "private", "public",
        "protected", "internal", "static", "final", "const", "void", "int", "long", "float", "double", "boolean", "bool", "char", "string",
        "new", "this", "super", "null", "nil", "true", "false", "try", "catch", "finally", "throw", "throws", "import", "package", "from",
        "function", "let", "async", "await", "export", "extends", "implements", "typeof", "instanceof", "struct", "impl", "fn", "mut",
        "use", "mod", "pub", "trait", "where", "suspend", "companion", "lateinit", "by", "init", "namespace", "using", "echo", "func", "defer", "go", "chan"
    )
    private val python = setOf(
        "def", "class", "if", "elif", "else", "for", "while", "return", "import", "from", "as", "with", "try", "except", "finally", "raise",
        "pass", "break", "continue", "lambda", "yield", "None", "True", "False", "and", "or", "not", "in", "is", "global", "nonlocal", "async", "await", "self", "print"
    )
    private val sql = setOf(
        "select", "from", "where", "insert", "into", "values", "update", "set", "delete", "create", "table", "drop", "alter", "join", "left",
        "right", "inner", "outer", "on", "group", "by", "order", "having", "limit", "and", "or", "not", "null", "as", "distinct", "primary", "key"
    )

    private fun palette(dark: Boolean) = object {
        val keyword = if (dark) Color(0xFFC678DD) else Color(0xFF9C27B0)
        val string = if (dark) Color(0xFF98C379) else Color(0xFF2E7D32)
        val comment = if (dark) Color(0xFF7F848E) else Color(0xFF8A8F98)
        val number = if (dark) Color(0xFFD19A66) else Color(0xFFB45F06)
        val func = if (dark) Color(0xFF61AFEF) else Color(0xFF1565C0)
        val tag = if (dark) Color(0xFFE06C75) else Color(0xFFD32F2F)
        val attr = if (dark) Color(0xFFD19A66) else Color(0xFFB45F06)
        val prop = if (dark) Color(0xFF56B6C2) else Color(0xFF00838F)
    }

    private val generic = Regex(
        "(?<comment>//[^\\n]*|/\\*[\\s\\S]*?\\*/|#[^\\n]*)" +
            "|(?<string>\"(?:\\\\.|[^\"\\\\\\n])*\"|'(?:\\\\.|[^'\\\\\\n])*'|`(?:\\\\.|[^`\\\\])*`)" +
            "|(?<number>\\b\\d+(?:\\.\\d+)?\\b)" +
            "|(?<word>[A-Za-z_][A-Za-z_0-9]*)"
    )
    private val markup = Regex(
        "(?<comment><!--[\\s\\S]*?-->)" +
            "|(?<tag></?[A-Za-z][\\w:-]*|/?>)" +
            "|(?<string>\"[^\"\\n]*\"|'[^'\\n]*')" +
            "|(?<attr>\\b[A-Za-z_:][\\w:.-]*(?==))"
    )
    private val css = Regex(
        "(?<comment>/\\*[\\s\\S]*?\\*/)" +
            "|(?<string>\"[^\"\\n]*\"|'[^'\\n]*')" +
            "|(?<number>#[0-9a-fA-F]{3,8}\\b|\\b\\d+(?:\\.\\d+)?(?:px|em|rem|%|vh|vw|s|ms)?\\b)" +
            "|(?<prop>[a-zA-Z-]+(?=\\s*:))"
    )

    fun tokens(code: String, lang: String, dark: Boolean): List<Token> {
        if (code.isBlank() || code.length > 60_000) return emptyList() // huge blocks stay plain (keeps typing smooth)
        val l = lang.lowercase()
        val c = palette(dark)
        val out = ArrayList<Token>()
        when {
            l in setOf("html", "xml", "svg", "xhtml") -> markup.findAll(code).forEach { m ->
                val g = m.groups
                val col = when {
                    g["comment"] != null -> c.comment
                    g["tag"] != null -> c.tag
                    g["string"] != null -> c.string
                    else -> c.attr
                }
                out.add(Token(m.range.first, m.range.last + 1, col))
            }
            l == "css" || l == "scss" -> css.findAll(code).forEach { m ->
                val g = m.groups
                val col = when {
                    g["comment"] != null -> c.comment
                    g["string"] != null -> c.string
                    g["number"] != null -> c.number
                    else -> c.prop
                }
                out.add(Token(m.range.first, m.range.last + 1, col))
            }
            else -> {
                val keywords = when (l) {
                    "python", "py" -> python
                    "sql" -> sql
                    else -> cLike
                }
                val hashComment = l in setOf("python", "py", "sh", "bash", "shell", "yaml", "yml", "ruby", "rb", "toml")
                generic.findAll(code).forEach { m ->
                    val g = m.groups
                    when {
                        g["comment"] != null -> {
                            val text = m.value
                            // '#' only starts a comment in python/shell-style languages
                            if (text.startsWith("#") && !hashComment) return@forEach
                            out.add(Token(m.range.first, m.range.last + 1, c.comment))
                        }
                        g["string"] != null -> out.add(Token(m.range.first, m.range.last + 1, c.string))
                        g["number"] != null -> out.add(Token(m.range.first, m.range.last + 1, c.number))
                        g["word"] != null -> {
                            val w = m.value
                            val key = if (l == "sql") w.lowercase() else w
                            if (key in keywords) out.add(Token(m.range.first, m.range.last + 1, c.keyword))
                            else {
                                val next = m.range.last + 1
                                if (next < code.length && code[next] == '(') out.add(Token(m.range.first, next, c.func))
                            }
                        }
                    }
                }
            }
        }
        return out
    }
}
