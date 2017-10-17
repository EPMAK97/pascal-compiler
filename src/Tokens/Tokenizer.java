package Tokens;

public class Tokenizer {

    private static NumberParser numberParser;
    private static OperatorParser operatorParser;
    private static SeparatorParser separatorParser;
    private static WordParser wordParser;
    private static Reader reader;

    private static String alphabet, separators, operators, digits;

    private boolean hasNext = true;

    private Token currentToken;
    //private static ArrayList<Token> tokenArrayList;

    static {
        alphabet    = "abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ_\'";
        separators  = "[](){},.;";
        operators   = "+-*/@#$^=><:";
        digits      = "0123456789";
        //tokenArrayList = new ArrayList<>();
    }

    public Tokenizer(String filePath) {
        reader = new Reader(filePath);
        numberParser    = new NumberParser(reader, this);
        operatorParser  = new OperatorParser(reader, this);
        separatorParser = new SeparatorParser(reader, this);
        wordParser      = new WordParser(reader, this);
    }

    public void setCurrentToken(Token currentToken) {
        //tokenArrayList.add(currentToken);
        this.currentToken = currentToken;
        hasNext = true;
    }

    public Token getCurrentToken() { return currentToken; }

    public void identifyType(char c) {
        if (alphabet.contains(c + ""))
            wordParser.parse(c);
        if (separators.contains(c + ""))
            separatorParser.parse(c);
        if (operators.contains(c + ""))
            operatorParser.parse(c);
        if (digits.contains(c + ""))
            numberParser.parse(c);
    }

    public boolean Next() {

        currentToken = null;
        //tokenArrayList.clear();

        char c = reader.getChar();

        // End Of File, need added special token
        if (c == '\0') {
            System.out.println("END OF FILE");
            return false;
        }

        identifyType(c);

        return hasNext;
    }


    public void print() {

        if (currentToken != null)
            System.out.print(currentToken);
//        for (Token aTokenArrayList : tokenArrayList) {
//            System.out.print(aTokenArrayList);
//        }
    }

}
