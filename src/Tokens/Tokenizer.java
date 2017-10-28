package Tokens;

import java.util.HashMap;

public class Tokenizer {
    private static Reader reader;
    private static StringBuilder builder;
    private static HashMap<String, Pair> operators, separators, words;
    private State currentState = State.FREE;
    private boolean isDouble = false;
    private boolean hasNext = true;

    private Token currentToken;

    static {
        builder     = new StringBuilder();
        operators   = new HashMap<String, Pair>() {{
            put("+", new Pair(TokenType.OPERATOR, TokenValue.OP_PLUS));
            put("-", new Pair(TokenType.OPERATOR, TokenValue.OP_MINUS));
            put("*", new Pair(TokenType.OPERATOR, TokenValue.OP_MULT));
            put("/", new Pair(TokenType.OPERATOR, TokenValue.OP_DIVISION));
            put("@", new Pair(TokenType.OPERATOR, TokenValue.OP_DOG));
            put("#", new Pair(TokenType.OPERATOR, TokenValue.OP_LATTICE));
            put("$", new Pair(TokenType.OPERATOR, TokenValue.OP_DOLLAR));
            put("^", new Pair(TokenType.OPERATOR, TokenValue.OP_CAP));
            put("=", new Pair(TokenType.OPERATOR, TokenValue.OP_EQUAL));
            put(">", new Pair(TokenType.OPERATOR, TokenValue.OP_GREATER));
            put("<", new Pair(TokenType.OPERATOR, TokenValue.OP_LESS));
            put(":", new Pair(TokenType.OPERATOR, TokenValue.OP_COLON));
        }};
        separators = new HashMap<String, Pair>() {{
            put("[", new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_SQUARE_LEFT));
            put("]", new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_SQUARE_RIGHT));
            put("(", new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_LEFT));
            put(")", new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_RIGHT));
            put("{", new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_FIGURE_LEFT));
            put("}", new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_FIGURE_RIGHT));
            put(",", new Pair(TokenType.SEPARATOR, TokenValue.SEP_COMMA));
            put(".", new Pair(TokenType.SEPARATOR, TokenValue.SEP_DOT));
            put(";", new Pair(TokenType.SEPARATOR, TokenValue.SEP_SEMICOLON));
        }};
        words = new HashMap<String, Pair>(){{
            put("integer",  new Pair(TokenType.INTEGER, TokenValue.KEYWORD_INTEGER));
            put("double",   new Pair(TokenType.DOUBLE, TokenValue.KEYWORD_DOUBLE));
            put("char",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_CHARACTER));
            put("var",      new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_VAR));
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
            put("write",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_WRITE));
            put("writeln",  new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_WRITELN));
            put("read",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_READ));
            put("readln",   new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_READLN));
        }};
    }

    private enum State {
        FREE,
        NOT_NUMBER,
        WAITING_DOT,
        WAITING_EXP,
        WAITING_PLUS_MINUS,
        WAITING_NUMBERS,
    }

    public Tokenizer(String filePath) {
        reader = new Reader(filePath);
//        File directory = new File("./src/Test/output.txt");
//        try {
//            bufferedWriter = new BufferedWriter(new FileWriter(directory));
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
    }

    private void setCurrentToken(Token currentToken) {
        this.currentToken = currentToken;
        hasNext = true;
    }

    private boolean isNum(char c) { return ('0' <= c) && ('9' >= c); }

    public Token getCurrentToken() { return currentToken; }

    public Token getNextToken() { Next(); return currentToken; }

    private void identifyType(char c) throws LexicalException {
        if (String.valueOf(c).matches("[a-zA-Z]|_|\'"))
            parseWord(c);
        else if (separators.containsKey(Character.toString(c)))
            parseSeparator(c);
        else if (operators.containsKey(Character.toString(c)))
            parseOperator(c);
        else if (isNum(c))
            parseNum(c);
        else if (c == ' ')
            identifyType(reader.getChar());
    }

    public boolean Next() {
        currentToken = null;
        //tokenArrayList.clear();
        char c = reader.getChar();
        if (c == '\0') {
            currentToken = new Token(new Pair(TokenType.END_OF_FILE, TokenValue.KEYWORD_EOF),
                    reader.xPos, reader.yPos, "\0");
            return false;
        }
        try {
            identifyType(c);
        } catch (LexicalException e) {
            System.out.println(e.getMessage());
            return false;
        } catch (NumberFormatException e) {
            System.out.println("Exception: overflow");
            return false;
        }
        return hasNext;
    }

    public void print() {
        System.out.println(currentToken);
    }

    private void passToken(Pair pair, int x, int y, String text,String value) {
        setCurrentToken(new Token(pair, x, y, text, value));
    }

    private void passToken(Pair pair, int x, int y, String text) {
        setCurrentToken(new Token(pair, x, y, text));
    }

    private void prepareNumberToken(int x, int y) {
        if (isDouble) {
            //System.out.println(builder.toString());
            passToken(new Pair(TokenType.DOUBLE, TokenValue.CONST_DOUBLE), x, y,
                    builder.toString(), String.valueOf(Double.parseDouble(builder.toString())));
        }
        else
            passToken(new Pair(TokenType.INTEGER, TokenValue.CONST_INTEGER), x, y,
                    builder.toString(), String.valueOf(Integer.parseInt(builder.toString())));
    }

    private void prepareWordToken(int x, int y) {
        if (words.containsKey(builder.toString().toLowerCase()))
            passToken(words.get(builder.toString().toLowerCase()), x, y, builder.toString());
        else
            passToken(new Pair(TokenType.IDENTIFIER, TokenValue.VARIABLE), x, y, builder.toString());
    }

    private boolean isValidState() { return currentState != State.NOT_NUMBER; }

    private void parseNum(char c) throws LexicalException {

        // begin of number
        int x = reader.xPos;
        int y = reader.yPos;

        builder.setLength(0);
        currentState = State.FREE;
        isDouble = false;

        while (isValidState()) {

            switch (currentState) {
                case FREE:
                    currentState = State.WAITING_DOT;
                    builder.append(c);
                    break;
                case WAITING_DOT:
                    if (c == '.') {
                        currentState = State.WAITING_EXP;
                        builder.append(c);
                        isDouble = true;
                    }
                    else if (c == 'e' || c == 'E') {
                        currentState = State.WAITING_PLUS_MINUS;
                        builder.append(c);
                        isDouble = true;
                    }
                    else if (isNum(c))
                        builder.append(c);
                    else
                        currentState = State.NOT_NUMBER;
                    break;
                case WAITING_EXP:
                    char lastChar = builder.charAt(builder.length() - 1);
                    if (c == 'e' || c == 'E') {
                        builder.append(c);
                        currentState = State.WAITING_PLUS_MINUS;
                    }
                    else if (isNum(c))
                        builder.append(c);
                    else if (c == '.') {
                        builder.deleteCharAt(builder.length() - 1);
                        reader.singleCharacterRollback();
                        isDouble = false;
                        currentState = State.NOT_NUMBER;
                    }
                    else if (lastChar == '.')
                        throw new LexicalException("Exception: the number can't end at the dot");
                    else
                        currentState = State.NOT_NUMBER;
                    break;
                case WAITING_PLUS_MINUS:
                    lastChar = builder.charAt(builder.length() - 1);
                    if (c == '+' || c == '-') {
                        if (isNum(builder.charAt(builder.length() - 1))) {
                            currentState = State.NOT_NUMBER;
                            break;
                        }
                        builder.append(c);
                        currentState = State.WAITING_NUMBERS;
                    }
                    else if (isNum(c))
                        builder.append(c);
                    else if (lastChar == 'e' || lastChar == 'E')
                        throw new LexicalException("Exception: the number can't end at the exp");
                    else
                        currentState = State.NOT_NUMBER;
                    break;
                case WAITING_NUMBERS:
                    lastChar = builder.charAt(builder.length() - 1);
                    if (isNum(c))
                        builder.append(c);
                    else if (lastChar == '+' || lastChar == '-')
                        throw new LexicalException("Exception: the number can't end at the symbol of plus or minus");
                    else
                        currentState = State.NOT_NUMBER;
                    break;
            }

            if (currentState == State.NOT_NUMBER) {
                prepareNumberToken(x, y);
            }
            else {
                if (!reader.endOfLine())
                    c = reader.getChar();
                else {
                    prepareNumberToken(x, y);
                    //currentState = State.NOT_NUMBER;
                    return;
                }
            }
        }
        if (currentState == State.NOT_NUMBER || reader.endOfLine())
            reader.singleCharacterRollback();
    }

    private void parseSeparator(char c) throws LexicalException {
        if (c == '.') {
            // special symbol
            char nextChar = '\0';
            if (reader.lookAhead())
                nextChar = reader.getChar();
            if (nextChar != '\0') {
                if (nextChar == '.')
                    passToken(new Pair(TokenType.SEPARATOR, TokenValue.SEP_DOUBLE_DOT),
                            reader.xPos, reader.yPos - 1, "..");
                else {
                    passToken(separators.get(Character.toString(c)),
                            reader.xPos, reader.yPos, Character.toString(c));
                    reader.singleCharacterRollback();
                }
            }
            else
                passToken(separators.get(Character.toString(c)),
                        reader.xPos, reader.yPos, Character.toString(c));
        }
        else if (c == '{') {
            char nextChar = '\0';
            while(nextChar != '}') {
                nextChar = reader.getChar();
                if (nextChar == '\0')
                    throw new LexicalException("Exception: unclosed comment");
            }
        }
        else if (c == '(') {
            char nextChar = reader.getChar();
            if (nextChar != '*') {
                reader.singleCharacterRollback();
                passToken(new Pair(TokenType.SEPARATOR, TokenValue.SEP_BRACKETS_LEFT),
                        reader.xPos, reader.yPos, Character.toString(c));
            }
            else {
                char firstChar;
                char secondChar = '\0';
                while(secondChar != ')') {
                    firstChar = reader.getChar();
                    if (firstChar == '\0')
                        throw new LexicalException("Exception: unclosed comment");
                    if (firstChar == '*')
                        secondChar = reader.getChar();
                }
            }
        }
        else
            passToken(separators.get(Character.toString(c)), reader.xPos, reader.yPos, Character.toString(c));
    }

    private void parseOperator(char c) {
        // special symbol
        char nextChar = '\0';
        // begin of word
        int x = reader.xPos;
        int y = reader.yPos;
        builder.setLength(0);
        if (reader.lookAhead())
            nextChar = reader.getChar();

        switch (c) {
            case ':' :
                if (nextChar == '=')
                    passToken(new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_ASSIGN),
                            reader.xPos, reader.yPos - 1, ":=");
                else {
                    passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                    if (nextChar != '\0')
                        reader.singleCharacterRollback();
                }
                break;
            case '<' :
                if (nextChar == '>')
                    passToken(new Pair(TokenType.OPERATOR, TokenValue.OP_NOT_EQUAL),
                            reader.xPos, reader.yPos - 1, "<>");
                else if (nextChar == '=')
                    passToken(new Pair(TokenType.OPERATOR, TokenValue.OP_LESS_OR_EQUAL),
                            reader.xPos, reader.yPos - 1, "<=");
                else {
                    passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                    if (reader.lookAhead())
                        reader.singleCharacterRollback();
                }
                break;
            case '>' :
                if (nextChar == '=')
                    passToken(new Pair(TokenType.OPERATOR, TokenValue.OP_GREATER_OR_EQUAL),
                            reader.xPos, reader.yPos - 1, ">=");
                else {
                    passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                    reader.singleCharacterRollback();
                }
                break;
            case '/' :
                if (nextChar == '/')
                    reader.markCommentLineDoubleSlash();
                else {
                    passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                    reader.singleCharacterRollback();
                }
                break;
            case '$' :
                reader.singleCharacterRollback();
                nextChar = reader.getChar();
                while (String.valueOf(nextChar).matches("[0-9A-F]")) {
                    builder.append(nextChar);
                    nextChar = reader.getChar();
                }
                Integer valueOfHex = Integer.parseInt(builder.toString(), 16);
                passToken(new Pair(TokenType.HEX, TokenValue.CONST_HEX),
                        x, y, "$" + builder.toString(), valueOfHex.toString());
                    reader.singleCharacterRollback();
                break;
            default:
                if (!operators.containsKey(nextChar + "")) {
                    passToken(operators.get(c + ""), reader.xPos,
                            nextChar == '\0' ? reader.yPos : reader.yPos - 1, c + "");
                    if (nextChar != '\0')
                        reader.singleCharacterRollback();
                    break;
                }
                passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                reader.singleCharacterRollback();
                break;
        }
    }

    private void parseWord(char c) throws LexicalException {
        // begin of word
        int x = reader.xPos;
        int y = reader.yPos;
        builder.setLength(0);

        if (c == '\'') {
            char firstChar;
            char secondChar;
            while(true) {
                firstChar = reader.getChar();
                if (firstChar == '\0')
                    throw new LexicalException("Exception: unclosed apostrophe");
                if (firstChar == '\'') {
                    secondChar = reader.getChar();
                    if (secondChar == '\'') {
                        builder.append(firstChar);
                        continue;
                    }
                    else
                        break;
                }
                builder.append(firstChar);
            }
            passToken(new Pair(TokenType.STRING, TokenValue.CONST_STRING), x, y, builder.toString());
            return;
        }

        while (String.valueOf(c).matches("[a-zA-Z]|_|\'")) {
            builder.append(c);
            c = reader.getChar();
            // we moved to a new line
            if (x < reader.xPos) {
                prepareWordToken(x, y);
                reader.singleCharacterRollback();
                break;
            }
            if (!String.valueOf(c).matches("[a-zA-Z]|_|\'")) {
                prepareWordToken(x, y);
                reader.singleCharacterRollback();
            }
        }
    }
}




