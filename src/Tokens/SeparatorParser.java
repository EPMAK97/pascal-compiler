package Tokens;

import Tokens.Types.Pair;
import Tokens.Types.TokenType;
import Tokens.Types.TokenValue;

import java.util.HashMap;

public class SeparatorParser implements Parsable {

    private Reader reader;
    private Tokenizer tokenizer;

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

    @Override
    public void setReaderAndTokinizer(Reader reader, Tokenizer tokenizer) {
        this.reader = reader;
        this.tokenizer = tokenizer;
    }

    @Override
    public void passToken(Pair pair, int x, int y, String value) {
        tokenizer.setCurrentToken(new Token(pair, x, y, value));
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
