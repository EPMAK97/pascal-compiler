package Tokens;


public class Token {
    private Tokenizer.Pair type;
    private int posX;
    private int posY;
    private String value;

    public Token(Tokenizer.Pair type, int posX, int posY, String value) {
        this.type = type;
        this.posX = posX;
        this.posY = posY;
        this.value = value;
    }

    public void print() {
        String sp = "   ";
        System.out.println(posX + ":" + posY + sp + type.getTokenType() + sp + type.getTokenValue() + sp + value);
    }

 }
