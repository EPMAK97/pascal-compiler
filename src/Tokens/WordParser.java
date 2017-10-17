package Tokens;

import Tokens.Types.Pair;
import Tokens.Types.TokenType;
import Tokens.Types.TokenValue;

import java.util.HashMap;

public class WordParser extends Parser {

    private static String alphabet;
    private static StringBuilder builder;

    private static HashMap<String, Pair> words;

    static {

        builder = new StringBuilder();
        alphabet = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_";

        words = new HashMap<String, Pair>(){{
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
            put("write",    new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_WRITE));
            put("writeln",  new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_WRITELN));
            put("read",     new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_READ));
            put("readln",   new Pair(TokenType.KEYWORD, TokenValue.KEYWORD_READLN));
        }};
    }

    protected WordParser(Reader reader, Tokenizer tokenizer) {
        super(reader, tokenizer);
    }

    private void prepareToken(int x, int y) {
        if (words.containsKey(builder.toString().toLowerCase()))
            passToken(words.get(builder.toString().toLowerCase()), x, y, builder.toString());
        else
            passToken(new Pair(TokenType.IDENTIFIER, TokenValue.VARIABLE), x, y, builder.toString());
    }

    @Override
    public void parse(char c) {

        builder.setLength(0);

        // begin of word
        int x = reader.xPos;
        int y = reader.yPos;

        if (c == '\'') {
            char firstChar;
            char secondChar;
            while(true) {
                firstChar = reader.getChar();
                if (firstChar == '\0') {
                    System.out.println("unclosed apostrophe");
                    break;
                }

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

            if (firstChar != '\0')
                passToken(new Pair(TokenType.STRING, TokenValue.KEYWORD_UNRESERVED), x, y, builder.toString());

            return;
        }

        while (alphabet.contains(Character.toString(c))) {
            builder.append(c);

            c = reader.getChar();
            // we moved to a new line
            if (x < reader.xPos) {
                prepareToken(x, y);
                reader.singleCharacterRollback();
                break;
            }

            if (!alphabet.contains(Character.toString(c))) {
                prepareToken(x, y);
                reader.singleCharacterRollback();
            }

        }

    }
}