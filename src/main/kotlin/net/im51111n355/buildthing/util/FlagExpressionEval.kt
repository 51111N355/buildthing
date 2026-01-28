package net.im51111n355.buildthing.util

// ИИшка 🤪 🤪 🤪

object FlagExpressionEval {
    fun eval(
        expression: String,
        flagGetter: (String) -> Boolean,
    ): Boolean {
        val lexer = Lexer(expression)
        val cache = HashMap<String, Boolean>()

        val parser =
            Parser(
                lexer = lexer,
                resolveFlag = { name ->
                    when (name) {
                        "true" -> true
                        "false" -> false
                        else -> cache.getOrPut(name) { flagGetter(name) }
                    }
                },
            )

        val result = parser.parseExpr()
        parser.expect(TokenType.EOF)
        return result
    }

    class FlagExpressionParseException(
        message: String,
        val index: Int,
    ) : IllegalArgumentException("$message (at index $index)")

    private enum class TokenType {
        IDENT,
        OR,
        AND,
        NOT,
        LPAREN,
        RPAREN,
        EOF,
    }

    private data class Token(
        val type: TokenType,
        val text: String,
        val index: Int,
    )

    private class Lexer(
        private val input: String,
    ) {
        private var i: Int = 0
        private val n: Int = input.length

        fun nextToken(): Token {
            skipWhitespace()
            if (i >= n) return Token(TokenType.EOF, "", i)

            val start = i
            return when (val c = input[i]) {
                '(' -> {
                    i++
                    Token(TokenType.LPAREN, "(", start)
                }
                ')' -> {
                    i++
                    Token(TokenType.RPAREN, ")", start)
                }
                '!' -> {
                    i++
                    Token(TokenType.NOT, "!", start)
                }
                '|' -> {
                    if (i + 1 < n && input[i + 1] == '|') {
                        i += 2
                        Token(TokenType.OR, "||", start)
                    } else {
                        throw FlagExpressionParseException(
                            "Expected '||'",
                            start,
                        )
                    }
                }
                '&' -> {
                    if (i + 1 < n && input[i + 1] == '&') {
                        i += 2
                        Token(TokenType.AND, "&&", start)
                    } else {
                        throw FlagExpressionParseException(
                            "Expected '&&'",
                            start,
                        )
                    }
                }
                else -> {
                    if (!isIdentStart(c)) {
                        throw FlagExpressionParseException(
                            "Unexpected character '$c'",
                            start,
                        )
                    }
                    i++
                    while (i < n && isIdentPart(input[i])) i++
                    val text = input.substring(start, i)
                    Token(TokenType.IDENT, text, start)
                }
            }
        }

        private fun skipWhitespace() {
            while (i < n && input[i].isWhitespace()) i++
        }

        private fun isIdentStart(c: Char): Boolean {
            return c.isLetterOrDigit() || c == '_' || c == '.' || c == '-' || c == ':'
        }

        private fun isIdentPart(c: Char): Boolean = isIdentStart(c)
    }

    private class Parser(
        private val lexer: Lexer,
        private val resolveFlag: (String) -> Boolean,
    ) {
        private var lookahead: Token = lexer.nextToken()

        fun parseExpr(): Boolean = parseOr()

        private fun parseOr(): Boolean {
            var left = parseAnd()
            while (lookahead.type == TokenType.OR) {
                consume(TokenType.OR)
                val right = parseAnd()
                left = left || right
            }
            return left
        }

        private fun parseAnd(): Boolean {
            var left = parseUnary()
            while (lookahead.type == TokenType.AND) {
                consume(TokenType.AND)
                val right = parseUnary()
                left = left && right
            }
            return left
        }

        private fun parseUnary(): Boolean {
            return if (lookahead.type == TokenType.NOT) {
                consume(TokenType.NOT)
                !parseUnary()
            } else {
                parsePrimary()
            }
        }

        private fun parsePrimary(): Boolean {
            return when (lookahead.type) {
                TokenType.IDENT -> {
                    val name = lookahead.text
                    consume(TokenType.IDENT)
                    resolveFlag(name)
                }
                TokenType.LPAREN -> {
                    consume(TokenType.LPAREN)
                    val v = parseExpr()
                    consume(TokenType.RPAREN)
                    v
                }
                else -> {
                    throw FlagExpressionParseException(
                        "Expected flag name or '('",
                        lookahead.index,
                    )
                }
            }
        }

        fun expect(type: TokenType) {
            if (lookahead.type != type) {
                throw FlagExpressionParseException(
                    "Expected $type but found ${lookahead.type}",
                    lookahead.index,
                )
            }
        }

        private fun consume(type: TokenType) {
            if (lookahead.type != type) {
                throw FlagExpressionParseException(
                    "Expected $type but found ${lookahead.type}",
                    lookahead.index,
                )
            }
            lookahead = lexer.nextToken()
        }
    }
}