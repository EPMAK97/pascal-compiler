package SyntacticalAnalyzer;

import Tokens.Token;
import Tokens.TokenValue;
import Tokens.Tokenizer;

import java.util.ArrayList;

public class ExpressionParser {

    private Tokenizer tokenizer;

    public class Node {

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

    public class LogicOperation extends Node {
        public LogicOperation(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }
    }

    public class UnaryMinusNode extends Node {
        public UnaryMinusNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }
    }

    public class NotNode extends Node {
        public NotNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }
    }


    public ExpressionParser(String filePath) {
        tokenizer = new Tokenizer(filePath);
    }

    public Node parse() {
        Node e = null;
        try {
            e = parseLogicalExpression();
        } catch (SyntaxException e1) {
            System.out.println(e1.getMessage());
            return null;
        }
        return e;
    }

    private Node parseLogicalExpression() throws SyntaxException {
        Node e = parseExpr();
        Token t = tokenizer.getCurrentToken();
        while (t != null && (isLogical(t.getText()))) {
            ArrayList<Node> arrayList = new ArrayList<>();
            arrayList.add(e);
            arrayList.add(parseExpr());
            e = new LogicOperation(arrayList, t);
            t = tokenizer.getCurrentToken();
        }
        return e;
    }

    private Node parseExpr() throws SyntaxException {
        Node e = parseTerm();
        Token t = tokenizer.getCurrentToken();
        while (t != null && (isExpr(t.getText()))) {
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
        while (t != null && (isTerm(t.getText()))) {
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
            case OP_MINUS:
                ArrayList<Node> list = new ArrayList<>();
                list.add(parseFactor());
                return new UnaryMinusNode(list, t);
            case KEYWORD_NOT:
                ArrayList<Node> list1 = new ArrayList<>();
                list1.add(parseFactor());
                return new NotNode(list1, t);
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

    private boolean isLogical(String s) {
        return s.equals(">") || s.equals("<") || s.equals(">=")
                || s.equals("<=") || s.equals("=")  || s.equals("<>");
    }

    private boolean isExpr(String s) {
        return s.equals("+") || s.equals("-") || s.equals("or") || s.equals("xor");
    }

    private boolean isTerm(String s) {
        return s.equals("*") || s.equals("/") || s.equals("div") || s.equals("mod")
                || s.equals("and") || s.equals("shl") || s.equals("shr");
    }
}
