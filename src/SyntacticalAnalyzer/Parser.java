package SyntacticalAnalyzer;

import Tokens.*;

import java.util.ArrayList;
import java.util.Arrays;

import static Tokens.TokenValue.*;

public class Parser {
    private Tokenizer tokenizer;

    public Parser(String filePath) throws SyntaxException {
        tokenizer = new Tokenizer(filePath);
        //Node node = parse();
    }

    public class Node {

        ArrayList<Node> children;
        Token token;

        private Node(ArrayList<Node> children, Token token) {
            this.children = children;
            this.token = token;
        }

        @Override
        public String toString() {
            return token.getText();
        }

        public void print() { print("", true); }

        private void print(String prefix, boolean isTail) {
            System.out.println(prefix + (isTail ? "└── " : "├── ") + token.getText());
            if (children != null) {
                for (int i = 0; i < children.size() - 1; i++) {
                    children.get(i).print(prefix + (isTail ? "    " : "│   "), false);
                }
                if (children.size() > 0) {
                    children.get(children.size() - 1)
                            .print(prefix + (isTail ? "    " : "│   "), true);
                }
            }
        }

        public void setChildren(ArrayList<Node> children) { this.children = children; }
    }

    public class VarNode extends Node {
        public VarNode(Token token) {
            super(null, token);
        }
        public VarNode(ArrayList<Node> children, Token token) {
            super(children, token);
        }
    }

    public class ConstNode extends Node {
        public ConstNode(Token token) {
            super(null, token);
        }
    }

    public class BinOpNode extends Node {
        public BinOpNode(ArrayList<Node> children, Token token) {
            super(children, token);
        }
    }

    public class LogicOperation extends Node {
        public LogicOperation(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class UnaryMinusNode extends Node {
        public UnaryMinusNode(ArrayList<Node> children, Token token) {
            super(children, token);
        }
    }

    public class NotNode extends Node {
        public NotNode(ArrayList<Node> children, Token token) {
            super(children, token);
        }
    }

    public class ifNode extends Node {
        public ifNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class thenNode extends Node {
        public thenNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class elseNode extends Node {
        public elseNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class IdentifierNode extends Node {
        public IdentifierNode(Token token) { super(null, token); }
    }

    public class AssignNode extends Node {
        public AssignNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ConstantNode extends Node {
        public ConstantNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class MainNode extends Node {
        public MainNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class VarIntegerNode extends Node {
        public VarIntegerNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class VarDoubleNode extends Node {
        public VarDoubleNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ArrayNode extends Node {
        public ArrayNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    private Node parseLogicalExpression() throws SyntaxException {
        Node e = parseExpr();
        Token t = tokenizer.getCurrentToken();
        while (t.getTokenValue() != KEYWORD_EOF && isLogical(t.getTokenValue())) {
            ArrayList<Node> arrayList = new ArrayList<>(Arrays.asList(e, parseExpr()));
            e = new LogicOperation(arrayList, t);
            t = tokenizer.getCurrentToken();
        }
        return e;
    }

    private Node parseExpr() throws SyntaxException {
        Node e = parseTerm();
        Token t = tokenizer.getCurrentToken();
        while (t.getTokenValue() != KEYWORD_EOF && isExpr(t.getTokenValue())) {
            ArrayList<Node> arrayList = new ArrayList<>(Arrays.asList(e, parseTerm()));
            e = new BinOpNode(arrayList, t);
            t = tokenizer.getCurrentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException {
        Node e = parseFactor();
        Token t = tokenizer.getNextToken();
        while (t.getTokenValue() != KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
            ArrayList<Node> arrayList = new ArrayList<>(Arrays.asList(e, parseFactor()));
            e = new BinOpNode(arrayList, t);
            t = tokenizer.getNextToken();
        }
        return e;
    }

    private Node parseFactor() throws SyntaxException {
        Token t = tokenizer.getNextToken();
        switch (t.getTokenValue()) {
            case OP_MINUS:
                ArrayList<Node> list = new ArrayList<>(Arrays.asList(parseFactor()));
                return new UnaryMinusNode(list, t);
            case KEYWORD_NOT:
                ArrayList<Node> list1 = new ArrayList<>(Arrays.asList(parseFactor()));
                return new NotNode(list1, t);
            case VARIABLE:
                return new VarNode(t);
            case CONST_INTEGER:
                return new ConstNode(t);
            case CONST_DOUBLE:
                return new ConstNode(t);
            case SEP_BRACKETS_LEFT:
                Node e = parseExpr();
                if (tokenizer.getCurrentToken().getTokenValue() != SEP_BRACKETS_RIGHT) {
                    throw new SyntaxException(String.format("Error in pos %s:%s non-closed bracket",
                            tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY()));
                }
                return e;
            default:
                //return null;
                throw new SyntaxException(String.format("Error in pos %s:%s expected identifier, constant or expression ",
                        tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY()));
        }
    }

    private boolean isLogical(TokenValue tv) {
        return  tv == OP_GREATER ||
                tv == OP_LESS    ||
                tv == OP_GREATER_OR_EQUAL ||
                tv == OP_LESS_OR_EQUAL    ||
                tv == OP_EQUAL   ||
                tv == OP_NOT_EQUAL;
    }

    private boolean isExpr(TokenValue tv) {
        return  tv == OP_PLUS    ||
                tv == OP_MINUS   ||
                tv == KEYWORD_OR ||
                tv == KEYWORD_XOR;
    }

    private boolean isTerm(TokenValue tv) {
        return  tv == OP_MULT        ||
                tv == OP_DIVISION    ||
                tv == KEYWORD_DIV    ||
                tv == KEYWORD_MOD    ||
                tv == KEYWORD_AND    ||
                tv == KEYWORD_SHL    ||
                tv == KEYWORD_SHR;
    }

    private void require(TokenValue tokenValue, String string) throws SyntaxException {
        if (tokenizer.getCurrentToken().getTokenValue() != tokenValue)
            throw new SyntaxException(String.format("Error in pos %s:%s required %s but found %s",
                    tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY(),
                    string, tokenizer.getCurrentToken().getText()));
    }

    private void require(TokenValue tokenValue1, TokenValue tokenValue2, String string1, String string2) throws SyntaxException {
        TokenValue currentTokenValue    = tokenizer.getCurrentToken().getTokenValue();
        String currentTokenValueString  = tokenizer.getCurrentToken().getText();
        String currentTokenPosY         = tokenizer.getCurrentToken().getPosY();
        String currentTokenPosX         = tokenizer.getCurrentToken().getPosX();
        if (currentTokenValue != tokenValue1 && currentTokenValue != tokenValue2)
            throw new SyntaxException(String.format("Error in pos %s:%s required %s or %s but found %s",
                    currentTokenPosX, currentTokenPosY, string1, string2 ,currentTokenValueString));
    }

    private void require(TokenValue tokenValue1, TokenValue tokenValue2, TokenValue tokenValue3,
                         String string1, String string2, String string3) throws SyntaxException {
        TokenValue currentTokenValue    = tokenizer.getCurrentToken().getTokenValue();
        String currentTokenValueString  = tokenizer.getCurrentToken().getText();
        String currentTokenPosY         = tokenizer.getCurrentToken().getPosY();
        String currentTokenPosX         = tokenizer.getCurrentToken().getPosX();
        if (currentTokenValue != tokenValue1 && currentTokenValue != tokenValue2 && currentTokenValue != tokenValue3)
            throw new SyntaxException(String.format("Error in pos %s:%s required %s or %s or %s but found %s",
                    currentTokenPosX, currentTokenPosY, string1, string2, string3, currentTokenValueString));
    }

    public Node parse() throws SyntaxException {

        Node node = null;
        ArrayList<Node> nodes = new ArrayList<>();

        //parseProgram();
        nodes.addAll(parseBlock());
        node = new MainNode(nodes, new Token(new Pair(TokenType.IDENTIFIER, VARIABLE), 1, 2, "Main Program"));
        return node;
    }

    private void parseProgram() throws SyntaxException {
        //tokenizer.getNextToken();
        require(KEYWORD_PROGRAM, "program");

        //tokenizer.getNextToken();
        require(VARIABLE, "identifier");

        //parseIdentifier();
        //tokenizer.getNextToken();
        require(SEP_SEMICOLON, ";");
    }

    private ArrayList<Node> parseBlock() throws SyntaxException {
        tokenizer.getNextToken();
        return parseDeclarationPart();
    }

    private ArrayList<Node> parseDeclarationPart() throws SyntaxException {
        ArrayList<Node> list = new ArrayList<>();
        while (currentValue() != KEYWORD_BEGIN) {
            switch (currentValue()) {
                case KEYWORD_CONST:
                    Token tokenConst = tokenizer.getCurrentToken();
                    Node constantNode = new ConstantNode(parseConst(), tokenConst);
                    list.add(constantNode);
                    break;
                case KEYWORD_VAR:
                    Token tokenVar = tokenizer.getCurrentToken();
                    list.add(new VarNode(parseVariables(), tokenVar));
                    break;
                case KEYWORD_TYPE:
                    Token tokenType = tokenizer.getCurrentToken();
                    Node typeNode = new ConstantNode(parseTypes(), tokenType);
                    list.add(typeNode);
                    break;
                default:
                    require(KEYWORD_CONST, KEYWORD_VAR, KEYWORD_TYPE, "const", "var", "type");
                    break;
            }
        }
        return list;
    }

    private ArrayList<Node> getIdentifierList() throws SyntaxException {
        ArrayList<Node> newVariables = new ArrayList<>();
        while (currentValue() != OP_COLON) {
            //Token newConstToken = tokenizer.getCurrentToken(); // current variable
            newVariables.add(new VarNode(tokenizer.getCurrentToken()));
            tokenizer.Next();
            require(SEP_COMMA, OP_COLON, ",", ":");
            if (currentValue() != OP_COLON)
                tokenizer.Next();
        }
        return newVariables;
    }

    private ArrayList<Node> parseVariables() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        tokenizer.Next();
        require(VARIABLE, "variable");
        while (currentValue() == VARIABLE) {

            ArrayList<Node> newVariables = getIdentifierList();

            tokenizer.Next();
            require(KEYWORD_INTEGER, KEYWORD_DOUBLE, "integer", "double");
            Node varNode = (currentValue() == KEYWORD_INTEGER) ?
                    new VarIntegerNode(newVariables, tokenizer.getCurrentToken()) :
                    new VarDoubleNode(newVariables, tokenizer.getCurrentToken());
            nodes.add(varNode);

            findSemicolon();
        }
        return nodes;
    }

    private ArrayList<Node> parseConst() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        tokenizer.Next();
        require(VARIABLE, "variable");
        while (currentValue() == VARIABLE) {
            Token newConstToken = tokenizer.getCurrentToken();
            tokenizer.Next();
            require(OP_EQUAL, "=");
            Node exprNode = parseLogicalExpression();
            ArrayList<Node> list = new ArrayList<>(Arrays.asList(exprNode));
            nodes.add(new ConstantNode(list, newConstToken));
            require(SEP_SEMICOLON, ";");
            tokenizer.Next();
        }
        return nodes;
    }

    private boolean isSimpleType() {
        return currentValue() == KEYWORD_INTEGER || currentValue() == KEYWORD_DOUBLE;
    }

    private boolean isType() {
        return currentValue() == VARIABLE;
    }

    private boolean isArray() {
        return currentValue() == KEYWORD_ARRAY;
    }

    private boolean isRecord() {
        TokenValue currentTokenValue = currentValue();
        return currentTokenValue == KEYWORD_RECORD;
    }

    private void findSemicolon() throws SyntaxException {
        tokenizer.Next();
        require(SEP_SEMICOLON, ";");
        tokenizer.Next();
    }

    private ArrayList<Node> parseDimensional() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();

        while (currentValue() != SEP_BRACKETS_SQUARE_RIGHT) {
            nodes.add(parseExpr());
            require(SEP_DOUBLE_DOT, "..");
            nodes.add(parseExpr());
            require(SEP_BRACKETS_SQUARE_RIGHT, SEP_COMMA, "]", ",");
        }
        return nodes;
    }

    private ArrayList<Node> parseTypes() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        tokenizer.Next();
        require(VARIABLE, "variable");
        while (currentValue() == VARIABLE) {

            ArrayList<Node> newVariables = new ArrayList<>();
            while (currentValue() != OP_EQUAL) {
                //Token newConstToken = tokenizer.getCurrentToken(); // current variable
                newVariables.add(new VarNode(tokenizer.getCurrentToken()));
                tokenizer.Next();
                require(SEP_COMMA, OP_EQUAL, ",", "=");
                if (currentValue() != OP_EQUAL)
                    tokenizer.Next();
            }

            tokenizer.Next();
            if (isSimpleType()) {
                //ArrayList<Node> newTypes = new ArrayList<>(Arrays.asList(tokenizer.getCurrentToken()));
                Node varNode = currentValue() == KEYWORD_INTEGER ?
                        new VarIntegerNode(newVariables, tokenizer.getCurrentToken()) :
                        new VarDoubleNode(newVariables, tokenizer.getCurrentToken());
                nodes.add(varNode);
                findSemicolon();
            }

            else if (isType()) {
                require(VARIABLE, "identifier"); // TODO make types checking
                nodes.add(new VarNode(newVariables, tokenizer.getCurrentToken()));
                findSemicolon();
            }

            else if (isArray()) {
                Token arrayToken = tokenizer.getCurrentToken();

                Node typeNode = parseArray();

                for (Node identifier : newVariables) {
                    identifier.setChildren(new ArrayList<>(Arrays.asList(typeNode)));
                }

                Node arrayNode = new ArrayNode(newVariables, arrayToken);
                findSemicolon();
                nodes.add(arrayNode);
            }

            else if (isRecord()) {
                Node recordNode = parseRecord(newVariables);
            }
        }
        return nodes;
    }

    private Node parseRecord(ArrayList<Node> newVariables) throws SyntaxException {
        Token recordToken = tokenizer.getCurrentToken();
        ArrayList<Node> children = new ArrayList<>();

        while (currentValue() != KEYWORD_END) {
            tokenizer.Next();
            ArrayList<Node> identifiers = new ArrayList<>();
            while (currentValue() == VARIABLE) {
                identifiers.add(new VarNode(tokenizer.getCurrentToken()));
                tokenizer.Next();
                if (currentValue() == SEP_COMMA)
                    tokenizer.Next();
            }
            require(OP_COLON, ":");

            tokenizer.Next();

            if (isArray()) {
                Token arrayToken = tokenizer.getCurrentToken();
                Node typeNode = parseArray();

                for (Node identifier : identifiers) {
                    identifier.setChildren(new ArrayList<>(Arrays.asList(typeNode)));
                }
                Node arrayNode = new ArrayNode(newVariables, arrayToken);

                findSemicolon();
            }

            tokenizer.Next();

        }

        return null;
    }

    private TokenValue currentValue() {
        return tokenizer.getCurrentToken().getTokenValue();
    }

    private void requireBasicTypes() throws SyntaxException {
        require(KEYWORD_INTEGER, KEYWORD_DOUBLE, KEYWORD_ARRAY,
                "integer", "double", "array");
    }

    private Node parseArray() throws SyntaxException {
        ArrayList<Node> arrayDimensional = new ArrayList<>();
        requireBasicTypes();
        while (currentValue() == KEYWORD_ARRAY) {
            tokenizer.Next();
            require(SEP_BRACKETS_SQUARE_LEFT, "[");
            arrayDimensional.addAll(parseDimensional());
            tokenizer.Next();
            require(KEYWORD_OF, "of");
            tokenizer.Next();
        }
        return new VarNode(arrayDimensional, tokenizer.getCurrentToken());
    }

}
