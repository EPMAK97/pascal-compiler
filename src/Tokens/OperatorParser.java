package Tokens;

import Tokens.Types.Pair;
import Tokens.Types.TokenType;
import Tokens.Types.TokenValue;

import java.util.HashMap;

public class OperatorParser extends Parser {

    private static HashMap<String, Pair> operators;

    static {
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

    protected OperatorParser(Reader reader, Tokenizer tokenizer) {
        super(reader, tokenizer);
    }

    @Override
    public void parse(char c) {
        // special symbol
        char nextChar = '\0';

        if (reader.lookAhead())
            nextChar = reader.getChar();

        if (!operators.containsKey(nextChar + "")) {

            passToken(operators.get(c + ""), reader.xPos,
                    nextChar == '\0' ? reader.yPos : reader.yPos - 1, c + "");

            if (nextChar != '\0')
                reader.singleCharacterRollback();

        } else {
            switch (c) {
                case ':' :
                    if (nextChar == '=') {

                        passToken(new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_ASSIGN),
                                reader.xPos, reader.yPos - 1, ":=");
                    } else {

                        passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");

                        reader.singleCharacterRollback();

                    } break;
                case '<' :
                    if (nextChar == '>') {

                        passToken(new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_GREATER_OR_EQUAL),
                                reader.xPos, reader.yPos - 1, "<>");

                    } else if (nextChar == '=') {

                        passToken(new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_LESS_OR_EQUAL),
                                reader.xPos, reader.yPos - 1, "<=");

                    } else {

                        passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                        reader.singleCharacterRollback();

                    } break;
                case '>' :
                    if (nextChar == '=') {

                        passToken(new Pair(TokenType.OPERATOR, TokenValue.KEYWORD_GREATER_OR_EQUAL),
                                reader.xPos, reader.yPos - 1, ">=");

                    } else {

                        passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                        reader.singleCharacterRollback();

                    } break;
                case '/' :
                    if (nextChar == '/') {

                        reader.markCommentLineDoubleSlash();

                    } else {

                        passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                        reader.singleCharacterRollback();

                    } break;
                default:
                    passToken(operators.get(c + ""), reader.xPos, reader.yPos - 1, c + "");
                    reader.singleCharacterRollback();
                    break;
            }
        }
    }

}
