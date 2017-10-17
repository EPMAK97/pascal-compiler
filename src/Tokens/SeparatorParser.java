package Tokens;

import Tokens.Types.Pair;
import Tokens.Types.TokenType;
import Tokens.Types.TokenValue;

import java.util.HashMap;

public class SeparatorParser extends Parser {

    private static HashMap<String, Pair> separators;

    static {
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
    }

    protected SeparatorParser(Reader reader, Tokenizer tokenizer) {
        super(reader, tokenizer);
    }

    @Override
    public void parse(char c) {
        if (c == '.') {
            // special symbol
            char nextChar = '\0';
            if (reader.lookAhead())
                nextChar = reader.getChar();

            if (nextChar != '\0') {
                if (nextChar == '.')
                    passToken(new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_DOUBLE_DOT),
                            reader.xPos, reader.yPos - 1, "..");
                else
                    reader.singleCharacterRollback();
            }
            else
                passToken(separators.get(Character.toString(c)),
                        reader.xPos, reader.yPos, Character.toString(c));
        }
        else if (c == '{') {

            char nextChar;

            while(true) {
                nextChar = reader.getChar();
                if (nextChar == '\0') {
                    System.out.println("UNCLOSED COMMENT");
                    break;
                }
                if (nextChar == '}')
                    break;
            }
        }
        else if (c == '(') {
            char nextChar = reader.getChar();
            if (nextChar != '*') {
                reader.singleCharacterRollback();
                passToken(new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_BRACKETS_LEFT),
                        reader.xPos, reader.yPos, Character.toString(c));
            }
            else {
                char firstChar;
                char secondChar;
                while(true) {
                    firstChar = reader.getChar();

                    if (firstChar == '\0') {
                        System.out.println("UNCLOSED COMMENT");
                        break;
                    }

                    if (firstChar == '*') {
                        secondChar = reader.getChar();
                        if (secondChar == ')')
                            break;
                    }
                }
            }
        }

        else {
            passToken(separators.get(Character.toString(c)), reader.xPos, reader.yPos, Character.toString(c));
        }
    }
}
