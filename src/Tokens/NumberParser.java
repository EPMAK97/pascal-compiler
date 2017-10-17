package Tokens;

import Tokens.Types.Pair;
import Tokens.Types.TokenType;
import Tokens.Types.TokenValue;

public class NumberParser implements Parsable {

    private Reader reader;
    private Tokenizer tokenizer;

    private static String numericСharacters;
    private static StringBuilder builder;

    private State currentState = State.INTEGER;

    static {
        builder = new StringBuilder();
        numericСharacters = "0123456789.+-eE";
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

    private static boolean isNum(char c) { return ('0' <= c) && ('9' >= c); }

    private enum State {
        DOUBLE,
        INTEGER,
    }

    private void prepareToken(int x, int y) {
        if (currentState == State.INTEGER) {
            passToken(new Pair(TokenType.INTEGER, TokenValue.KEYWORD_INTEGER), x, y, builder.toString());
        }
        // ____________________________
        else {
            passToken(new Pair(TokenType.DOUBLE, TokenValue.KEYWORD_DOUBLE), x, y, builder.toString());
        }
    }

    @Override
    public void parse(char c) {

        // begin of number
        int x = reader.xPos;
        int y = reader.yPos;

        builder.setLength(0);
        currentState = State.INTEGER;

        while (numericСharacters.contains(Character.toString(c))) {
            // special symbol
            char nextChar = '\0';

            if (reader.lookAhead())
                nextChar = reader.getChar();

            if (!numericСharacters.contains(Character.toString(nextChar))) {

                if (isNum(c))
                    builder.append(c);

                prepareToken(x, y);

                if (!isNum(c))
                    reader.singleCharacterRollback();
                if (nextChar != '\0')
                    reader.singleCharacterRollback();

                break;

            } else {

                if (c == '.') {

                    if (nextChar == '.') {

                        // we need to do anything with StringBuilder
                        prepareToken(x, y);
                        passToken(new Pair(TokenType.SEPARATOR, TokenValue.KEYWORD_DOUBLE_DOT),
                                reader.xPos, reader.yPos - 1, "..");
                        break;

                    } else if (isNum(nextChar) || nextChar == 'e' || nextChar == 'E') {

                        currentState = State.DOUBLE;
                        builder.append(c);
                        builder.append(nextChar);

                    } // else not a number exception

                } else if (c == 'e' || c == 'E') {

                    currentState = State.DOUBLE;

                    if (isNum(nextChar)) {

                        builder.append(c);
                        builder.append(nextChar);

                    } else {

                        builder.append(c);
                        reader.singleCharacterRollback();

                    }

                } else if (c == '+' || c == '-') {

                    if (reader.previousCharacter() == 'e' || reader.previousCharacter() == 'E') {

                        builder.append(c);
                        reader.singleCharacterRollback();

                    } else {

                        prepareToken(x, y);
                        //System.out.println("AA");
                        reader.singleCharacterRollback();
                        //reader.singleCharacterRollback();
                        reader.singleCharacterRollback();
                        break;
                    }

                } else {

                    // else it's number
                    builder.append(c);
                    reader.singleCharacterRollback();

                }
            }

            c = reader.getChar();
            if (!numericСharacters.contains(Character.toString(c))) {

                prepareToken(x, y);
                reader.singleCharacterRollback();
                break;

            }

        }

    }

}
