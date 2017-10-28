package SyntacticalAnalyzer;

import Tokens.Token;
import Tokens.TokenValue;
import Tokens.Tokenizer;

import java.util.ArrayList;
import java.util.Arrays;

public class ExpressionParser {

    private Tokenizer tokenizer;

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
    }


    public class VarNode extends Node {
        public VarNode(Token token) {
            super(null, token);
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
        public LogicOperation(ArrayList<Node> children, Token token) {
            super(children, token);
        }
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


    public ExpressionParser(String filePath) {
        tokenizer = new Tokenizer(filePath);
    }

    public Node parse() throws SyntaxException {
        return parseLogicalExpression();
    }

    private Node parseLogicalExpression() throws SyntaxException {
        Node e = parseExpr();
        Token t = tokenizer.getCurrentToken();
        while (t.getTokenValue() != TokenValue.KEYWORD_EOF && isLogical(t.getTokenValue())) {
            ArrayList<Node> arrayList = new ArrayList<>(Arrays.asList(e, parseExpr()));
            e = new LogicOperation(arrayList, t);
            t = tokenizer.getCurrentToken();
        }
        return e;
    }

    private Node parseExpr() throws SyntaxException {
        Node e = parseTerm();
        Token t = tokenizer.getCurrentToken();
        while (t.getTokenValue() != TokenValue.KEYWORD_EOF && isExpr(t.getTokenValue())) {
            ArrayList<Node> arrayList = new ArrayList<>(Arrays.asList(e, parseTerm()));
            e = new BinOpNode(arrayList, t);
            t = tokenizer.getCurrentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException {
        Node e = parseFactor();
        Token t = tokenizer.getNextToken();
        while (t.getTokenValue() != TokenValue.KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
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
                if (tokenizer.getCurrentToken().getTokenValue() != TokenValue.SEP_BRACKETS_RIGHT) {
                    throw new SyntaxException(String.format("Error in pos %s:%s non-closed bracket",
                            tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY()));
                }
                return e;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s expected identifier, constant or expression ",
                        tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY()));
        }
    }

    private boolean isLogical(TokenValue tv) {
        return  tv == TokenValue.OP_GREATER ||
                tv == TokenValue.OP_LESS    ||
                tv == TokenValue.OP_GREATER_OR_EQUAL ||
                tv == TokenValue.OP_LESS_OR_EQUAL    ||
                tv == TokenValue.OP_EQUAL   ||
                tv == TokenValue.OP_NOT_EQUAL;
    }

    private boolean isExpr(TokenValue tv) {
        return  tv == TokenValue.OP_PLUS    ||
                tv == TokenValue.OP_MINUS   ||
                tv == TokenValue.KEYWORD_OR ||
                tv == TokenValue.KEYWORD_XOR;
    }

    private boolean isTerm(TokenValue tv) {
        return  tv == TokenValue.OP_MULT        ||
                tv == TokenValue.OP_DIVISION    ||
                tv == TokenValue.KEYWORD_DIV    ||
                tv == TokenValue.KEYWORD_MOD    ||
                tv == TokenValue.KEYWORD_AND    ||
                tv == TokenValue.KEYWORD_SHL    ||
                tv == TokenValue.KEYWORD_SHR;
    }
}
