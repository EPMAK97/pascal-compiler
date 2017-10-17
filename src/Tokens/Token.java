package Tokens;


import Tokens.Types.Pair;

public class Token {
    private Pair pair;
    private int posX;
    private int posY;
    private String value;

    public Token(Pair type, int posX, int posY, String value) {
        this.pair = type;
        this.posX = posX;
        this.posY = posY;
        this.value = value;
    }

    @Override
    public String toString() {
        String sp = "\t";
        return posX + sp + posY + sp + pair.getTokenType() + sp + pair.getTokenValue() + sp + value + sp + '\n';
    }
}
