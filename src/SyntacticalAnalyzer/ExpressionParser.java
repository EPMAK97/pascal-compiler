package SyntacticalAnalyzer;

import Tokens.LexicalException;
import Tokens.Token;
import Tokens.Tokenizer;
import Tokens.Types.TokenValue;

import java.util.ArrayList;
import java.util.StringJoiner;

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
        //System.out.println(t.getValue().equals("+") ? "AA" : "BB");
        while (t != null && (t.getText().equals("+") || t.getText().equals("-"))) {
            //tokenizer.Next();
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
        //System.out.println(e.token);
        //System.out.println(tokenizer.getCurrentToken());
        //tokenizer.Next();
        //System.out.println(tokenizer.getCurrentToken());
        //Token t = tokenizer.getCurrentToken();
        Token t = currentToken();

        while (t != null && (t.getText().equals("*") || t.getText().equals("/"))) {
            //System.out.println("AAAAA");
            //tokenizer.Next();
            ArrayList<Node> arrayList = new ArrayList<>();
            arrayList.add(e);
            arrayList.add(parseFactor());
            //System.out.println(arrayList.get(1));
            e = new BinOpNode(arrayList, t);
            t = currentToken();
        }
        return e;
    }

    private Node parseFactor()
    {
        Token t = currentToken();
        //tokenizer.Next();
        //System.out.println(t);
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

    public static class TreePrinter {

        static boolean first = true;
        static boolean last = false;
        static int cnt = 0;

        public static void print(Node node, String indent) {
//            System.out.print("--");
//            if (cnt == 1) {
//                System.out.println(node.childs.get(0).token.getText().equals("+") ||
//                        node.childs.get(1).token.getText().equals("-") ? node.childs.get(1) : node.childs.get(0));
//                System.out.print(indent + "├");
//                System.out.println(first && !last ? node.token.getText() : "");
//                System.out.print(indent + "└─");
//                System.out.print(node.childs.get(0).token.getText().equals("+") ||
//                        node.childs.get(1).token.getText().equals("-") ? node.childs.get(0) : node.childs.get(1));
//            }
//            else {
//                System.out.println(last ? node.childs.get(1) : node.childs.get(0));
//                System.out.print(indent + "├");
//                System.out.println(first && !last ? node.token.getText() : "");
//                System.out.print(indent + "└─");
//                System.out.print((!first  && !last && cnt == 1) || last ? node.childs.get(0) : node.childs.get(1));
//            }
//            if (first) {
//                System.out.print(node.token.getText());
//                System.out.println("--");
//            }
//            } else
//                System.out.println(node.childs.get(0) + "--");

            if (node.childs.get(0) != null && !first)
                System.out.println(node.token.getText() + "--" + node.childs.get(1));
            else
                System.out.println(node.token.getText() + "--" + node.childs.get(0));
            System.out.println(indent + "|");
            System.out.print(indent + "└──");
            if (node.childs.get(0).childs == null && node.childs.get(1).childs == null) {
                //System.out.println("ADSFs");
                System.out.println(node.childs.get(0));
            }
            indent += "   ";
            first = false;

            for (int i = 0; i < node.childs.size(); i++) {
                if (node.childs.get(i).childs == null)
                    continue;
                print(node.childs.get(i), indent);
            }
        }
    }

}
