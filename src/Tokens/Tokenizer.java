package Tokens;

import Tokens.Enums.TokenType;
import Tokens.Enums.TokenValue;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.HashMap;
import java.util.List;

public class Tokenizer {
    private static List<String> lines;
    private static HashMap<String, Pair> keyWords, separators, operators;
    private static String alphabet;
    private static State currentState = State.unreserved;
    private static GlobalState globalState = GlobalState.unreserved;

    static {
        alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    }

    private static int posX = 1;
    private static int posY = 1;

    {
        keyWords = new HashMap<String, Pair>(){{
            put("integer",  new Pair(TokenType.INTEGER, TokenValue.KEYWORD_INT));
            put("double",   new Pair(TokenType.DOUBLE, TokenValue.KEYWORD_DOUBLE));
            put("char",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_CHARACTER));
            put("var",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_VAR));
            put("absolute", new Pair(TokenType.DIRECTIVE, TokenValue.DIRECTIVE_ABSOLUTE));
            put("and",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_AND));
            put("array",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_ARRAY));
            put("begin",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_BEGIN));
            put("break",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_BREAK));
            put("case",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_CASE));
            put("continue", new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_CONTINUE));
            put("const",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_CONST));

            put("div",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_DIV));
            put("do",       new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_DO));
            put("downto",   new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_DOWNTO));
            put("else",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_ELSE));
            put("end",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_END));
            put("exit",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_EXIT));

            put("file",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_FILE));
            put("for",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_FOR));
            put("forward",  new Pair(TokenType.DIRECTIVE, TokenValue.DIRECTIVE_FORWARD));
            put("function", new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_FUNCTION));
            put("end",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_END));
            put("exit",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_EXIT));
            put("if",       new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_IF));
            put("in",       new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_IF));
            put("mod",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_MOD));
            put("nil",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_NIL));
            put("not",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_NOT));
            put("of",       new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_OF));
            put("or",       new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_OR));
            put("procedure",new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_PROCEDURE));
            put("record",   new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_RECORD));
            put("repeat",   new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_REPEAT));
            put("set",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_SET));
            put("shl",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_SHL));
            put("shr",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_SHR));
            put("string",   new Pair(TokenType.STRING, TokenValue.KEYWORD_STRING));
            put("then",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_THEN));
            put("to",       new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_TO));
            put("type",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_TYPE));
            put("while",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_WHILE));
            put("until",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_UNTIL));
            put("with",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_WITH));
            put("xor",      new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_XOR));
            put("goto",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_XOR));
            put("label",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_LABEL));
            put("program",  new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_PROGRAM));
        }};

        separators = new HashMap<String, Pair>() {{

            put("[", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_SQUARE_LEFT));
            put("]", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_SQUARE_RIGHT));
            put("(", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_LEFT));
            put(")", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_RIGHT));
            put("{", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_FIGURE_LEFT));
            put("}", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_FIGURE_RIGHT));
            put(",", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_COMMA));
            put(".", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_DOT));
            put(";", new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_SEMICOLON));

        }};

        operators = new HashMap<String, Pair>() {{
            put("+", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_PLUS));
            put("-", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_MINUS));
            put("*", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_MULT));
            put("/", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_DIVISION));
            put("@", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_DOG));
            put("#", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_LATTICE));
            put("$", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_DOLLAR));
            put("^", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_CAP));
            put("=", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_EQUAL));
            put(">", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_GREATER));
            put("<", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_LESS));
            put(":", new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_COLON));

        }};

    }

    private enum State {
        unreserved,
        beginningOfLine,
        insideTheLine,
        endOfLine,
    }

    private enum GlobalState {
        unreserved,
        string,
    }

    public Tokenizer(String file) {
        try {
            lines = Files.readAllLines(Paths.get(file), StandardCharsets.UTF_8);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private static boolean itIsLetter(char c) { return alphabet.contains(Character.toString(c)); }

    private static boolean isNum(char c) {
        return ('0' <= c) && ('9' >= c);
    }

    private static boolean isApostrophe(char c) { return c == '\''; }

    public void getNextToken() {
        for (String s : lines) {

            boolean endOfLine = false;

            StringBuilder builder = new StringBuilder();

            for (int i = 0; i < s.length(); i++) {


                if (s.length() - i == 1)
                    endOfLine = true;

                if (s.charAt(i) == '\n'  && currentState == State.beginningOfLine) {
                    System.out.println("ERROR UNCLOSED LINE in pos " + posX + " " + i);
                    System.exit(0);
                }

                if (isApostrophe(s.charAt(i))) {

                    switch (currentState) {
                        case unreserved :
                            //if ()
                            globalState = GlobalState.string;
                            currentState = State.beginningOfLine;
                            continue;
                        case beginningOfLine:
                            currentState = State.endOfLine;
                            continue;
                        case endOfLine:
                            currentState = State.insideTheLine;
                            builder.append(s.charAt(i));
                            continue;
                        case insideTheLine:
                            currentState = State.endOfLine;
                            continue;
                    }

                }

                if (currentState == State.endOfLine) {
                    globalState = GlobalState.unreserved;
                    Token token = new Token(new Pair(TokenType.STRING, TokenValue.KEYWORD_UNRESERVED),
                            posX, posY, builder.toString());
                    token.print();
                    continue;
                }

                if (globalState == GlobalState.string) {
                    builder.append(s.charAt(i));
                    continue;
                }

                // Parse words

                if (itIsLetter(s.charAt(i))) {

                    builder.append(s.charAt(i));

                    if (endOfLine || !itIsLetter(s.charAt(i + 1)) || isApostrophe(s.charAt(i))) {

                        String key = builder.toString().toLowerCase();
                        if (keyWords.containsKey(key)) {
                            Token token = new Token(keyWords.get(key), posX, posY, builder.toString());
                            //System.out.println("я поймал ключевое слово");
                            token.print();
                        }
                        else {
                            Token token = new Token(new Pair(TokenType.IDENTIFIER, TokenValue.VARIABLE), posX, posY, key);
                            System.out.println("я поймал переменную");
                            token.print();
                        }
                        //System.out.println(builder.toString());
                        builder = new StringBuilder();
                    }
                }

                // Parse separators

                else if (separators.containsKey(s.charAt(i) + "")) {
                    if (!endOfLine && s.charAt(i) == '.' && s.charAt(i + 1) == '.') {
                        Token token = new Token(new Pair(TokenType.SEPARATOR,
                                TokenValue.KEYWORD_DOUBLE_DOT), posX, i + 1, "..");
                        token.print();
                        i++;
                        continue;
                    }
                    Token token = new Token(separators.get(s.charAt(i) + ""), posX, i + 1, s.charAt(i) + "");
                    token.print();
                }

                // Parse operation

                else if (operators.containsKey(s.charAt(i) + "")) {

                    if (s.charAt(i) == '/') {
                        if (!endOfLine && s.charAt(i + 1) == '/') {
                            break;
                        }
                    }

                    if (!endOfLine && s.charAt(i) == ':' && s.charAt(i + 1) == '=') {
                        Token token = new Token(new Pair(TokenType.OPERATOR,
                                TokenValue.KEYWORD_ASSIGN), posX, i + 1, ":=");
                        token.print();
                        i++;
                        continue;
                    }

                    if (!endOfLine && s.charAt(i) == '<' && s.charAt(i + 1) == '>') {
                        Token token = new Token(new Pair(TokenType.OPERATOR,
                                TokenValue.KEYWORD_NOT_EQUAL), posX, i + 1, "<>");
                        token.print();
                        i++;
                        continue;
                    }


                    if (!endOfLine && s.charAt(i) == '<' && s.charAt(i + 1) == '=') {
                        Token token = new Token(new Pair(TokenType.OPERATOR,
                                TokenValue.KEYWORD_LESS_OR_EQUAL), posX, i + 1, "<=");
                        token.print();
                        i++;
                        continue;
                    }


                    if (!endOfLine && s.charAt(i) == '>' && s.charAt(i + 1) == '=') {
                        Token token = new Token(new Pair(TokenType.OPERATOR,
                                TokenValue.KEYWORD_GREATER_OR_EQUAL), posX, i + 1, ">=");
                        token.print();
                        i++;
                        continue;
                    }

                    Token token = new Token(operators.get(s.charAt(i) + ""),
                            posX, i + 1, s.charAt(i) + "");
                    token.print();
                }

                // Parse numbers

                else if (isNum(s.charAt(i))) {
                    StringBuilder stringBuilder = new StringBuilder();
                    int k = i;
                    while (s.length() - k >= 1 && isNum(s.charAt(k))) {
                        stringBuilder.append(s.charAt(k++) + "");
                    }
                    Token token = new Token(new Pair(TokenType.INTEGER,
                            TokenValue.KEYWORD_UNRESERVED), posX, i + 1, stringBuilder.toString());
                    token.print();
                    if (k > i)
                        i = k - 1;
//                    token.print();
                }
            }

            if (currentState == State.endOfLine) {
                globalState = GlobalState.unreserved;
                Token token = new Token(new Pair(TokenType.STRING, TokenValue.KEYWORD_UNRESERVED),
                        posX, posY, builder.toString());
                token.print();
            }
            //System.out.println(s.length());
            posY = s.length() + 1;
            posX++;
        }
        Token token = new Token(new Pair(TokenType.END_OF_FILE, TokenValue.KEYWORD_UNRESERVED), posX - 1, posY, "");
        token.print();
    }

    public void launch() {
        getNextToken();
    }

    public class Pair {
        private TokenType tokenType;
        private TokenValue tokenValue;

        public Pair(TokenType tokenType, TokenValue tokenValue) {
            this.tokenType = tokenType;
            this.tokenValue = tokenValue;
        }

        public TokenType getTokenType() {
            return tokenType;
        }

        public void setTokenType(TokenType tokenType) {
            this.tokenType = tokenType;
        }

        public TokenValue getTokenValue() {
            return tokenValue;
        }

        public void setTokenValue(TokenValue tokenValue) {
            this.tokenValue = tokenValue;
        }
    }
}