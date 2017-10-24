package SyntacticalAnalyzer;

import Tokens.LexicalException;
import Tokens.Token;
import Tokens.Tokenizer;
import Tokens.Types.TokenValue;

import java.util.ArrayList;

public class ExpressionParser {

    private Tokenizer tokenizer;

    public abstract class Node {

        ArrayList<Node> childs;
        Token token;

        public Node(ArrayList<Node> childs, Token token) {
            this.childs = childs;
            this.token = token;
        }

        @Override
        public String toString() {
            return token.getText();
        }

        public abstract void print(String indent);

        public void print() {
            print("", true);
        }

        private void print(String prefix, boolean isTail) {
            System.out.println(prefix + (isTail ? "└── " : "├── ") + token.getText());
            if (childs != null) {
                for (int i = 0; i < childs.size() - 1; i++) {
                    childs.get(i).print(prefix + (isTail ? "    " : "│   "), false);
                }
                if (childs.size() > 0) {
                    childs.get(childs.size() - 1)
                            .print(prefix + (isTail ? "    " : "│   "), true);
                }
            }
        }
    }


    public class VarNode extends Node {

        public VarNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }

        @Override
        public void print(String indent) {
            System.out.println(indent + token.getText());
        }
    }

    public class ConstNode extends Node {

        public ConstNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }

        @Override
        public void print(String indent) {
            System.out.println(indent + token.getText());
        }

    }

    public class BinOpNode extends Node {

        public BinOpNode(ArrayList<Node> childs, Token token) {
            super(childs, token);
        }

        @Override
        public void print(String indent) {
            System.out.println(indent + token.getText());
            String indent1 = indent + "     ";
            if (childs.get(1).childs == null)
                indent1 += "|";
            for (Node n : childs) {
                n.print(indent1);
            }
        }

    }

    public ExpressionParser(String filePath) {
        tokenizer = new Tokenizer(filePath);
    }

    public Node parse() {
        Node e = parseExpr();
        return e;
    }

    private Node parseExpr() {
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

    private Node parseTerm() {
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

    private Node parseFactor()
    {
        Token t = currentToken();
        switch (t.getTokenValue())
        {
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
                    try {
                        throw new LexicalException();
                    } catch (LexicalException e1) {
                        System.out.println("Error: non-closed bracket");
                    }
                }
                return e;
            default:
                try {
                    throw new LexicalException();
                } catch (LexicalException e1) {
                    System.out.println("Error: expected identifier, constant or expression");
                }
                break;
        }
        return null;
    }
    private Token currentToken() {
        if (tokenizer.Next())
            return tokenizer.getCurrentToken();
        return null;
    }

}
