package Tokens;

import Tokens.Types.Pair;

public interface Parsable {
    public void parse(char c);

    public void passToken(Pair pair, int x, int y, String value);
}