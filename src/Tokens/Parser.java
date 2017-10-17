package Tokens;

import Tokens.Types.Pair;

public abstract class Parser implements Parsable {
    protected Reader reader;
    protected Tokenizer tokenizer;

    protected Parser(Reader reader, Tokenizer tokenizer) {
        this.reader = reader;
        this.tokenizer = tokenizer;
    }

    @Override
    public void passToken(Pair pair, int x, int y, String value) {
        tokenizer.setCurrentToken(new Token(pair, x, y, value));
    }

    @Override
    public abstract void parse(char c);

}
