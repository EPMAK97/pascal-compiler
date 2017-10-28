package SyntacticalAnalyzer;

import Tokens.Token;
import Tokens.Tokenizer;

import java.util.ArrayList;

public class Parser {
    private Tokenizer tokenizer;

    public Parser(String filePath) {
        tokenizer = new Tokenizer(filePath);
    }

    private Token currentToken() {
        if (tokenizer.Next())
            return tokenizer.getCurrentToken();
        return null;
    }

    public Node parse() {
        Node node = parseDeclaration();
        return node;
    }

    private Node parseDeclaration() {
        ArrayList<Node> arrayList = new ArrayList<>();
        while (true) {
            Token t = currentToken();
            switch (t.getTokenValue()) {
                case KEYWORD_VAR:
                    ArrayList<Node> children = new ArrayList<>();
                    //children.add(parseVariableDeclaration());
                    //children.add(parseVariableDeclaration());
                    return new VarNode(parseVariableDeclaration(), t);
                case KEYWORD_IF:
                    parseIfStatement();
                    //return new IfNode(,t);
                default: return null;
            }
        }
    }

    private void parseIfStatement() {

    }

    private ArrayList<Node> parseVariableDeclaration() {
        boolean loop = true;
        ArrayList<Node> children = new ArrayList<>();
        //ArrayList<Token> tokens = new ArrayList<>();
        ArrayList<Node> nodes = new ArrayList<>();
        Node node = null;
        while (loop) {
            Token t = currentToken();
            switch (t.getTokenValue()) {
                case VARIABLE:
                    children.add(new VarNode(null, t));
                    //tokens.add(t);
                    //parseVariableIdentifiers();
                    break;
                case OP_COLON:
                    t = currentToken();
                    switch (t.getTokenValue()) {
                        case KEYWORD_DOUBLE:
                            node = new VarIntegerNode(children, t);
                            nodes.add(node);
                            break;
                        case KEYWORD_INTEGER:
                            node = new VarIntegerNode(children, t);
                            nodes.add(node);
                            break;
                        case KEYWORD_CHARACTER:
                            node = new VarIntegerNode(children, t);
                            nodes.add(node);
                            break;
                        default:
                            break; // TODO throw Exception

                    }
                    break;
                case SEP_COMMA:
                    break;
                case SEP_SEMICOLON:
                    children = new ArrayList<>();
                    break;
                case KEYWORD_BEGIN:
                    loop = false;
                    break;
                default: loop = false;
            }
            //t = currentToken();
        }
        return nodes;
    }

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
            System.out.print(prefix + (isTail ? "└── " : "├── "));
            System.out.println(token.getText() == null ? token.getValue() : token.getText());
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

    public class IfNode extends Node {
        public IfNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class VarNode extends Node {
        public VarNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ConstNode extends Node {
        public ConstNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class BinOpNode extends Node {
        public BinOpNode(ArrayList<Node> children, Token token) { super(children, token);}
    }

    public class VarIntegerNode extends Node {
        public VarIntegerNode(ArrayList<Node> children, Token token) { super(children, token);}
    }



//    private void parseVariableIdentifiers() {
//        boolean loop;
//        while(loop) {
//            Token t = currentToken();
//            switch (t.getTokenValue()) {
//                case
//
//                default: loop = false;
//            }
//        }
//    }
}
