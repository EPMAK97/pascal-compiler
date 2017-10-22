package Tokens;

import Tokens.Types.Pair;
import Tokens.Types.TokenValue;

public class Token {
    private Pair pair;
    private int posX;
    private int posY;
    private String text;
    private String value;

    public Token(Pair type, int posX, int posY, String value) {
        this.pair = type;
        this.posX = posX;
        this.posY = posY;
        this.value = value;
    }

    public Token(Pair type, int posX, int posY, String text, String value) {
        this.pair = type;
        this.posX = posX;
        this.posY = posY;
        this.value = value;
        this.text = text;
    }

    public String getValue() {
        return value;
    }

    public TokenValue getTokenValue() {
        return pair.getTokenValue();
    }


    public String genSpace(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        return posX + genSpace(3 - String.valueOf(posX).length()) + "|"
                + genSpace(3 - String.valueOf(posY).length()) + posY + " | "
                + pair.getTokenType() + genSpace(11 - pair.getTokenType().toString().length()) + " | "
                + pair.getTokenValue() + genSpace(30 - pair.getTokenValue().toString().length()) + "| "
                + value;
    }

}
