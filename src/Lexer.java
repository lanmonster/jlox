import java.util.*;

public class Lexer {
    private String source;
    private int start = 0;
    private int current = 0;
    private int line = 1;

    private static final Map<String, TokenType> keywords;

    static {
        keywords = new HashMap<>();
        keywords.put("and",    TokenType.AND);
        keywords.put("class",  TokenType.CLASS);
        keywords.put("else",   TokenType.ELSE);
        keywords.put("false",  TokenType.FALSE);
        keywords.put("for",    TokenType.FOR);
        keywords.put("fun",    TokenType.FUN);
        keywords.put("if",     TokenType.IF);
        keywords.put("nil",    TokenType.NIL);
        keywords.put("or",     TokenType.OR);
        keywords.put("print",  TokenType.PRINT);
        keywords.put("return", TokenType.RETURN);
        keywords.put("super",  TokenType.SUPER);
        keywords.put("this",   TokenType.THIS);
        keywords.put("true",   TokenType.TRUE);
        keywords.put("var",    TokenType.VAR);
        keywords.put("while",  TokenType.WHILE);
    }
    Lexer(String source) {
        this.source = source;
    }
    public List<Token> lex() {
        List<Token> tokens = new ArrayList<>();

        while (!eof()) {
            start = current;
            tokens.add(lexOne());
        }

        tokens.add(makeToken(TokenType.EOF));

        return tokens.stream().filter(Objects::nonNull).toList();
    }

    private boolean eof() {
        return current >= source.length();
    }

    private char consume() {
        return source.charAt(current++);
    }
    private char peek() {
        if (eof()) return '\0';
        return peek(0);
    }
    private char peek(int n) {
        if (current + n >= source.length()) return '\0';
        return source.charAt(current + n);
    }

    private boolean isDigit(char c) {
        return c >= '0' && c <= '9';
    }

    private boolean isAlpha(char c) {
        return (c >= 'a' && c <= 'z') || (c >= 'A' && c <= 'Z') || c == '_';
    }

    private boolean isAlphanumeric(char c) {
        return isAlpha(c) || isDigit(c);
    }

    private Token makeToken(TokenType type) {
        return makeToken(type, null);
    }

    private Token makeToken(TokenType type, Object literal) {
        String text = source.substring(start, current);
        return new Token(type, text, literal, line);
    }

    private Token lexOne() {
        char c = consume();
        switch (c) {
            case '(' -> {
                return makeToken(TokenType.LEFT_PAREN);
            }
            case ')' -> {
                return makeToken(TokenType.RIGHT_PAREN);
            }
            case '{' -> {
                return makeToken(TokenType.LEFT_BRACE);
            }
            case '}' -> {
                return makeToken(TokenType.RIGHT_BRACE);
            }
            case ',' -> {
                return makeToken(TokenType.COMMA);
            }
            case '.' -> {
                return makeToken(TokenType.DOT);
            }
            case '-' -> {
                return makeToken(TokenType.MINUS);
            }
            case '+' -> {
                return makeToken(TokenType.PLUS);
            }
            case ';' -> {
                return makeToken(TokenType.SEMICOLON);
            }
            case '*' -> {
                return makeToken(TokenType.STAR);
            }
            case '!' -> {
                if (peek() == '=') {
                    current++;
                    return makeToken(TokenType.BANG_EQUAL);
                }
                return makeToken(TokenType.BANG);
            }
            case '=' -> {
                if (peek() == '=') {
                    current++;
                    return makeToken(TokenType.EQUAL_EQUAL);
                }
                return makeToken(TokenType.EQUAL);
            }
            case '<' -> {
                if (peek() == '=') {
                    current++;
                    return makeToken(TokenType.LESS_EQUAL);
                }
                return makeToken(TokenType.LESS);
            }
            case '>'-> {
                if (peek() == '=') {
                    current++;
                    return makeToken(TokenType.GREATER_EQUAL);
                }
                return makeToken(TokenType.GREATER);
            }
            case '/' -> {
                if (peek() == '/') {
                    while (peek() != '\n') consume();
                    line++;
                    return null;
                }
                return makeToken(TokenType.SLASH);
            }
            // Ignore whitespace.
            case ' ', '\r', '\t', '\n' -> {
                if (c == '\n') line++;
                return null;
            }
            case '"' -> {
                return lexString();
            }
            default -> {
                if (isDigit(c)) return lexNumber();
                if (isAlpha(c)) return lexIdentifier();
                Main.error(line, "Unexpected character.");
                return null;
            }
        }
    }

    private Token lexString() {
        while (peek() != '"' && !eof()) {
            if (peek() == '\n') line++;
            consume();
        }
        if (eof()) {
            Main.error(line, "Unterminated string.");
            return null;
        }
        consume();
        return makeToken(TokenType.STRING, source.substring(start+1, current-1));
    }

    private Token lexNumber() {
        while (isDigit(peek())) consume();
        if (peek() == '.' && isDigit(peek(1))) {
            consume();
            while (isDigit(peek())) consume();
        }
        return makeToken(TokenType.NUMBER, Double.parseDouble(source.substring(start, current)));
    }

    private Token lexIdentifier() {
        while (isAlphanumeric(peek())) consume();
        return makeToken(keywords.getOrDefault(source.substring(start, current), TokenType.IDENTIFIER));
    }

}
