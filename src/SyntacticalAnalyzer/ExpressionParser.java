package SyntacticalAnalyzer;

import Tokens.LexicalException;
import Tokens.Token;
import Tokens.Tokenizer;
import Tokens.TokenValue;

import java.util.ArrayList;

public class ExpressionParser {

    private Tokenizer tokenizer;

    public abstract class Node {

        ArrayList<Node> children;
        Token token;

        public Node(ArrayList<Node> children, Token token) {
            this.children = children;
            this.token = token;
        }

        @Override
        public String toString() {
            return token.getText();
        }

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

        public VarNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }

    }

    public class ConstNode extends Node {

        public ConstNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }

    }

    public class BinOpNode extends Node {

        public BinOpNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }

    }

    public ExpressionParser(String filePath) {
        tokenizer = new Tokenizer(filePath);
    }

    public Node parse() {
        Node e = null;
        try {
            e = parseExpr();
        } catch (SyntaxException e1) {
            System.out.println(e1.getMessage());
            return null;
        }
        return e;
    }

    private Node parseExpr() throws SyntaxException {
        Node e = parseTerm();
        Token t = tokenizer.getCurrentToken();
        while (t != null && (t.getText().equals("+") || t.getText().equals("-"))) {
            ArrayList<Node> arrayList = new ArrayList<>();
            arrayList.add(e);
            arrayList.add(parseTerm());
            e = new BinOpNode(arrayList, t);
            t = tokenizer.getCurrentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException {
        Node e = parseFactor();
        Token t = currentToken();

        while (t != null && (t.getText().equals("*") || t.getText().equals("/"))) {
            ArrayList<Node> arrayList = new ArrayList<>();
            arrayList.add(e);
            arrayList.add(parseFactor());
            e = new BinOpNode(arrayList, t);
            t = currentToken();
        }
        return e;
    }

    private Node parseFactor() throws SyntaxException {
        Token t = currentToken();
        switch (t.getTokenValue()) {
            case VARIABLE:
                return new VarNode(null, t);
            case CONST_INTEGER:
                return new ConstNode(null, t);
            case CONST_DOUBLE:
                return new ConstNode(null, t);
            case SEP_BRACKETS_LEFT:
                Node e = parseExpr();
                if (tokenizer.getCurrentToken() == null
                        || tokenizer.getCurrentToken().getTokenValue() != TokenValue.SEP_BRACKETS_RIGHT) {
                    throw new SyntaxException("Error: non-closed bracket");
                }
                return e;
            default:
                throw new SyntaxException("Error: expected identifier, constant or expression");
        }
    }

    private Token currentToken() {
        if (tokenizer.Next())
            return tokenizer.getCurrentToken();
        return null;
    }

}
