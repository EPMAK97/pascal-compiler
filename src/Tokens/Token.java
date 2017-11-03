package Tokens;

public class Token {
    private Pair pair = null;
    private String posX = null;
    private String posY = null;
    private String text = null;
    private String value = null;

    public Token(Pair type, int posX, int posY, String text) {
        this.pair = type;
        this.posX = String.valueOf(posX);
        this.posY = String.valueOf(posY);
        this.text = text;
    }

    public Token(Pair type, int posX, int posY, String text, String value) {
        this.pair = type;
        this.posX = String.valueOf(posX);
        this.posY = String.valueOf(posY);
        this.text = text;
        this.value = value;
    }

    public String getValue() {
        return value == null ? "" : value;
    }

    public String getText() {return text == null ? "" : text; }

    public TokenValue getTokenValue() { return pair.getTokenValue(); }

    public TokenType getTokenType() { return pair.getTokenType(); }

    public String getType() {return pair.getTokenType().toString(); }

    public String getPosX() { return posX; }

    public String getPosY() { return posY; }

    public String genSpace(int count) {
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < count; i++) {
            builder.append(" ");
        }
        return builder.toString();
    }

    @Override
    public String toString() {
        String tokenType = pair.getTokenType().toString();
        String tokenValue = pair.getTokenValue().toString();
        String lexem = posX + genSpace(3 - posX.length()) + "|"
                + genSpace(3 - posY.length()) + posY + " | "
                + tokenType + genSpace(11 - tokenType.length()) + " | "
                + tokenValue + genSpace(28 - tokenValue.length()) + "| "
                + text + genSpace(10 - text.length()) + "| ";
        lexem += value == null ? "" : value;
        return lexem;
    }

}
