package Tokens.Types;

public class Pair {
    private TokenType tokenType;
    private TokenValue tokenValue;

    public Pair(TokenType tokenType, TokenValue tokenValue) {
        this.tokenType = tokenType;
        this.tokenValue = tokenValue;
    }

    public TokenType getTokenType() {
        return tokenType;
    }

    public void setTokenType(TokenType tokenType) {
        this.tokenType = tokenType;
    }

    public TokenValue getTokenValue() {
        return tokenValue;
    }

    public void setTokenValue(TokenValue tokenValue) {
        this.tokenValue = tokenValue;
    }
}