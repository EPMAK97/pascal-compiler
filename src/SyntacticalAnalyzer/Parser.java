package SyntacticalAnalyzer;

import Tokens.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.StringJoiner;

import static Tokens.TokenValue.*;

public class Parser {
    private Tokenizer tokenizer;
    private static HashMap<TokenValue, String> hashTokens;

    static {
        hashTokens = new HashMap<>();
        HashMap<String, Pair> operators  = Tokenizer.getOperators();
        HashMap<String, Pair> separators = Tokenizer.getSeparators();
        HashMap<String, Pair> words      = Tokenizer.getWords();
        for (String key : operators.keySet())
            hashTokens.put(operators.get(key).getTokenValue(), key);
        for (String key : separators.keySet())
            hashTokens.put(separators.get(key).getTokenValue(), key);
        for (String key : words.keySet())
            hashTokens.put(words.get(key).getTokenValue(), key);
        // this is not in the hash
        hashTokens.put(VARIABLE, "identifier");
        hashTokens.put(SEP_DOUBLE_DOT, "..");
        hashTokens.put(KEYWORD_ASSIGN, ":=");
    }

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
        public VarNode(Token token) { super(null, token); }
        public VarNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ConstNode extends Node {
        public ConstNode(Token token) { super(null, token); }
    }

    public class BinOpNode extends Node {
        public BinOpNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class LogicOperation extends Node {
        public LogicOperation(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class UnaryMinusNode extends Node {
        public UnaryMinusNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class NotNode extends Node {
        public NotNode(ArrayList<Node> children, Token token) { super(children, token); }
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

    public class ProcedureNode extends Node {
        public ProcedureNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class LoopForNode extends Node {
        public LoopForNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class IfNode extends Node {
        public IfNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ElseNode extends Node {
        public ElseNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ElseIfNode extends Node {
        public ElseIfNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class FunctionNode extends Node {
        public FunctionNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    private Node parseLogicalExpression() throws SyntaxException {
        Node e = parseExpr();
        Token t = currentToken();
        while (t.getTokenValue() != KEYWORD_EOF && isLogical(t.getTokenValue())) {
            ArrayList<Node> arrayList = getList(e, parseExpr());
            e = new LogicOperation(arrayList, t);
            t = currentToken();
        }
        return e;
    }

    private Node parseExpr() throws SyntaxException {
        Node e = parseTerm();
        Token t = currentToken();
        while (t.getTokenValue() != KEYWORD_EOF && isExpr(t.getTokenValue())) {
            ArrayList<Node> arrayList = getList(e, parseTerm());
            e = new BinOpNode(arrayList, t);
            t = currentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException {
        Node e = parseFactor();
        Token t = tokenizer.getNextToken();
        while (t.getTokenValue() != KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
            ArrayList<Node> arrayList = getList(e, parseFactor());
            e = new BinOpNode(arrayList, t);
            t = tokenizer.getNextToken();
        }
        return e;
    }

    private Node parseFactor() throws SyntaxException {
        Token t = tokenizer.getNextToken();
        switch (t.getTokenValue()) {
            case OP_MINUS:
                ArrayList<Node> list = getList(parseFactor());
                return new UnaryMinusNode(list, t);
            case KEYWORD_NOT:
                ArrayList<Node> list1 = getList(parseFactor());
                return new NotNode(list1, t);
            case VARIABLE:
                return new VarNode(t);
            case CONST_INTEGER:
                return new ConstNode(t);
            case CONST_DOUBLE:
                return new ConstNode(t);
            case SEP_BRACKETS_LEFT:
                Node e = parseExpr();
                require(SEP_BRACKETS_RIGHT);
                return e;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s expected identifier, constant or expression ",
                        currentToken().getPosX(), currentToken().getPosY()));
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

    private ArrayList<Node> getList(Node ... nodes) {
        return new ArrayList<>(Arrays.asList(nodes));
    }

    private void require(TokenValue ... tokenValues) throws SyntaxException {
        StringJoiner joiner = new StringJoiner(" or ");
        for (TokenValue value : tokenValues) {
            if (currentValue() == value)
                return;
            joiner.add(hashTokens.get(value));
        }
        throw new SyntaxException(String.format("Error in pos %s:%s required %s but found %s",
                currentToken().getPosX(), currentToken().getPosY(),
                joiner.toString(), currentToken().getText()));
    }

    public Node parse() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        goToNextToken();
        if (currentValue() == KEYWORD_PROGRAM)
            parseProgram();
        nodes.addAll(parseDeclarationPart());
        require(KEYWORD_BEGIN);
        ArrayList<Node> finalBlock = parseBlock();
        goToNextToken();
        require(SEP_DOT);
        if (finalBlock != null)
            nodes.addAll(finalBlock);
        return new MainNode(nodes, new Token(new Pair(TokenType.IDENTIFIER, VARIABLE), 1, 2, "Main Program"));
    }

    private void parseProgram() throws SyntaxException {
        goToNextToken();
        require(VARIABLE);
        goToNextToken();
        require(SEP_SEMICOLON);
        goToNextToken();
    }

    private Node getNextDeclarationNode() throws SyntaxException {
        Token currentToken = currentToken();
        switch (currentValue()) {
            case KEYWORD_CONST:
                return new ConstantNode(parseConst(), currentToken);
            case KEYWORD_VAR:
                return new VarNode(parseVariables(), currentToken);
            case KEYWORD_TYPE:
                return new ConstantNode(parseTypes(), currentToken);
            case KEYWORD_PROCEDURE:
                return new ProcedureNode(parseSubroutine(false), currentToken);
            case KEYWORD_FUNCTION:
                return new FunctionNode(parseSubroutine(true), currentToken);
            default:
                require(KEYWORD_CONST, KEYWORD_VAR, KEYWORD_TYPE);
                break;
        }
        return null; // Never))) But it's necessary
    }

    private ArrayList<Node> parseDeclarationPart() throws SyntaxException {
        ArrayList<Node> list = new ArrayList<>();
        while (currentValue() != KEYWORD_BEGIN) // end of declaration part
            list.add(getNextDeclarationNode());
        return list;
    }

    private ArrayList<Node> getIdentifierList(TokenValue mark) throws SyntaxException {
        ArrayList<Node> newVariables = new ArrayList<>();
        while (currentValue() != mark) {
            //Token newConstToken = tokenizer.getCurrentToken(); // current variable
            newVariables.add(new VarNode(currentToken()));
            goToNextToken();
            require(SEP_COMMA, mark);
            if (currentValue() != mark) {
                goToNextToken();
                require(VARIABLE);
            }
        }
        return newVariables;
    }

    private ArrayList<Node> parseVariables() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        goToNextToken();
        require(VARIABLE);
        while (currentValue() == VARIABLE) {
            ArrayList<Node> newVariables = getIdentifierList(OP_COLON);
            goToNextToken();
            if (isArray()) {
                nodes.add(parseArray(newVariables));
                findSemicolon();
                continue;
            }
            require(KEYWORD_INTEGER, KEYWORD_DOUBLE);
            Node varNode = (currentValue() == KEYWORD_INTEGER) ?
                    new VarIntegerNode(newVariables, currentToken()) :
                    new VarDoubleNode(newVariables, currentToken());
            nodes.add(varNode);
            findSemicolon();
        }
        return nodes;
    }

    private ArrayList<Node> parseConst() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        goToNextToken();
        require(VARIABLE);
        while (currentValue() == VARIABLE) {
            Token newConstToken = currentToken();
            goToNextToken();
            require(OP_EQUAL);
            ArrayList<Node> list = getList(parseLogicalExpression());
            nodes.add(new ConstantNode(list, newConstToken));
            require(SEP_SEMICOLON);
            goToNextToken();
        }
        return nodes;
    }

    private ArrayList<Node> parseDimensional() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        while (currentValue() != SEP_BRACKETS_SQUARE_RIGHT) {
            nodes.add(parseExpr());
            require(SEP_DOUBLE_DOT);
            nodes.add(parseExpr());
            require(SEP_BRACKETS_SQUARE_RIGHT, SEP_COMMA);
        }
        return nodes;
    }

    private ArrayList<Node> parseTypes() throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        goToNextToken();
        require(VARIABLE);
        while (currentValue() == VARIABLE) {
            ArrayList<Node> newVariables = getIdentifierList(OP_EQUAL);
            goToNextToken();
            if (isSimpleType()) {
                Node varNode = currentValue() == KEYWORD_INTEGER ?
                        new VarIntegerNode(newVariables, currentToken()) :
                        new VarDoubleNode(newVariables, currentToken());
                nodes.add(varNode);
                findSemicolon();
            }
            else if (isType()) {
                require(VARIABLE); // TODO make types checking
                nodes.add(new VarNode(newVariables, currentToken()));
                findSemicolon();
            }
            else if (isArray()) {

                nodes.add(parseArray(newVariables));
                findSemicolon();
            }
            else if (isRecord())
                nodes.add(parseRecord(newVariables));
        }
        return nodes;
    }

    private Node parseRecord(ArrayList<Node> newVariables) throws SyntaxException {
        Token recordToken = currentToken();
        ArrayList<Node> children = new ArrayList<>();

        goToNextToken();
        while (currentValue() != KEYWORD_END) {
            ArrayList<Node> identifiers = getIdentifierList(OP_COLON);
            goToNextToken();
            if (isArray()) {
                children.add(parseArray(identifiers));
                findSemicolon();
            }
            if (isSimpleType() || currentValue() == VARIABLE) {
                Node simpleTypeNode = (currentValue() == KEYWORD_INTEGER) ?
                        new VarIntegerNode(identifiers, currentToken()) :
                        new VarDoubleNode(identifiers, currentToken());
                children.add(simpleTypeNode);
                findSemicolon();
            }
            if (isRecord())
                children.add(parseRecord(identifiers));
        }
        goToNextToken();
        goToNextToken();
        //System.out.println(newVariables);
        for (Node variable : newVariables)
            variable.setChildren(children);
        return new VarNode(newVariables, recordToken);
    }

    private TokenValue currentValue() {
        return tokenizer.getCurrentToken().getTokenValue();
    }

    private void requireBasicTypes() throws SyntaxException {
        require(KEYWORD_INTEGER, KEYWORD_DOUBLE, KEYWORD_ARRAY);
    }

    private Node parseArray(ArrayList<Node> newVariables) throws SyntaxException {
        Token arrayToken = currentToken();
        ArrayList<Node> arrayDimensional = new ArrayList<>();
        requireBasicTypes();
        while (currentValue() == KEYWORD_ARRAY) {
            goToNextToken();
            require(SEP_BRACKETS_SQUARE_LEFT);
            arrayDimensional.addAll(parseDimensional());
            goToNextToken();
            require(KEYWORD_OF);
            goToNextToken();
        }
        Node typeNode = new VarNode(arrayDimensional, currentToken());
        for (Node variable : newVariables)
            variable.setChildren(getList(typeNode));
        return new ArrayNode(newVariables, arrayToken);
    }

    private ArrayList<Node> parseSubroutine(boolean isFunction) throws SyntaxException {
        goToNextToken();
        require(VARIABLE);
        goToNextToken();
        ArrayList<Node> parameters = null;
        ArrayList<Node> localParameters = null;
        ArrayList<Node> block = null;
        if (currentValue() == SEP_BRACKETS_LEFT || isFunction)
            parameters = parseFormalParameters(isFunction);
        if (currentValue() != KEYWORD_BEGIN) {
            require(SEP_SEMICOLON);
            goToNextToken();
            localParameters = parseDeclarationPart();
        }
        require(KEYWORD_BEGIN);
        block = parseBlock();
        findSemicolon();
        ArrayList<Node> procedureNodes = new ArrayList<>();
        if (parameters != null)
            procedureNodes.addAll(parameters);
        if (localParameters != null)
            procedureNodes.addAll(localParameters);
        procedureNodes.addAll(block);
        return procedureNodes;
    }

    private ArrayList<Node> parseFormalParameters(boolean isFunction) throws SyntaxException {
        ArrayList<Node> parameters = new ArrayList<>();
        if (currentValue() == SEP_BRACKETS_LEFT) {
            goToNextToken();
            while (currentValue() != SEP_BRACKETS_RIGHT) {
                switch (currentValue()) {
                    case KEYWORD_VAR:
                        goToNextToken();
                        parameters.addAll(getFormalParameters());
                        goToNextToken();
                        break;
                    case VARIABLE:
                        parameters.addAll(getFormalParameters());
                        goToNextToken();
                        break;
                    default:
                        require(SEP_SEMICOLON, SEP_BRACKETS_RIGHT);
                        goToNextToken();
                        break;
                }
            }
        }
        if (isFunction) {
            if (currentValue() == SEP_BRACKETS_RIGHT)
                goToNextToken();
            require(OP_COLON);
            goToNextToken();
            require(KEYWORD_INTEGER, KEYWORD_DOUBLE); // TODO change this when checking types
            parameters.add(new VarNode(currentToken())); // Change...
        }
        goToNextToken();
        require(SEP_SEMICOLON);
        return parameters;
    }

    private ArrayList<Node> getFormalParameters() throws SyntaxException {
        ArrayList<Node> parameters = new ArrayList<>();
        ArrayList<Node> newVarVariables = getIdentifierList(OP_COLON);
        goToNextToken();
        require(KEYWORD_INTEGER, KEYWORD_DOUBLE, KEYWORD_ARRAY);
        if (currentValue() == KEYWORD_ARRAY) {
            Token arrayToken = currentToken();
            goToNextToken();
            goToNextToken();
            VarNode varNode = new VarNode(newVarVariables, currentToken());
            parameters.add(new ArrayNode(getList(varNode), arrayToken));
        }
        else
            parameters.add(currentValue() == KEYWORD_INTEGER ?
                    new VarIntegerNode(newVarVariables, currentToken()) :
                    new VarDoubleNode(newVarVariables, currentToken()));
        return parameters;
    }

    private Node getLogicalCondition() throws SyntaxException {
        goToNextToken();
        require(VARIABLE);
        Node leftCondition = new VarNode(currentToken());
        goToNextToken();
        if (!isLogical(currentValue()))
            throw new SyntaxException(String.format("Error in pos %s:%s required %s but found %s",
                    tokenizer.getCurrentToken().getPosX(), currentToken().getPosY(),
                    "logical", currentToken().getText()));
        Token logicToken = currentToken();
        goToNextToken();
        require(VARIABLE, CONST_INTEGER, CONST_DOUBLE);
        Node rightCondition = new VarNode(currentToken());
        return new LogicOperation(getList(leftCondition, rightCondition), logicToken);
    }

    private Node parseStatement() throws SyntaxException {
        require(VARIABLE);
        Token variableToken = currentToken();
        goToNextToken();
        require(KEYWORD_ASSIGN);
        Node expression = parseLogicalExpression();
        require(SEP_SEMICOLON);
        return new VarNode(getList(expression), variableToken);
    }

    private Node parseIf() throws SyntaxException {
        Token ifToken = currentToken();
        ArrayList<Node> ifChildren = new ArrayList<>();
        ifChildren.add(getLogicalCondition());
        goToNextToken();
        require(KEYWORD_THEN);
        goToNextToken();
        if (currentValue() == KEYWORD_BEGIN)
            ifChildren.addAll(parseBlock());
        else
            ifChildren.add(parseStatement());
        if (currentValue() == KEYWORD_END) {
            goToNextToken();
            if (currentValue() != KEYWORD_ELSE) {
                require(SEP_SEMICOLON);
                goToNextToken();
            }
        }
        else
            goToNextToken();
        if (currentValue() == KEYWORD_ELSE) {
            while (currentValue() == KEYWORD_ELSE) {
                Token elseToken = currentToken();
                ArrayList<Node> elseChildren = new ArrayList<>();
                goToNextToken();
                if (currentValue() == KEYWORD_BEGIN || currentValue() == KEYWORD_IF) {
                    if (currentValue() == KEYWORD_IF) {
                        Token elseIfToken = currentToken();
                        ArrayList<Node> elseIfChildren = new ArrayList<>();
                        elseIfChildren.add(getLogicalCondition());
                        goToNextToken();
                        require(KEYWORD_THEN);
                        goToNextToken();
                        if (currentValue() == KEYWORD_BEGIN)
                            elseIfChildren.addAll(parseBlock());
                        else
                            elseIfChildren.add(parseStatement());
                        ifChildren.add(new ElseIfNode(elseIfChildren, elseIfToken));
                        goToNextToken();
                        if (currentValue() != KEYWORD_ELSE)
                            require(SEP_SEMICOLON);
                        continue;
                    } else
                        elseChildren.addAll(parseBlock());
                } else {
                    elseChildren.add(parseStatement());
                    ifChildren.add(new ElseNode(elseChildren, elseToken));
                    break;
                }
                goToNextToken();
            }
            require(SEP_SEMICOLON);
            goToNextToken();
        }
        return new IfNode(ifChildren, ifToken);
    }

    private void isLoopBlock(ArrayList<Node> loopChildren) throws SyntaxException {
        if (currentValue() == KEYWORD_BEGIN) {
            loopChildren.addAll(parseBlock());
            goToNextToken();
            require(SEP_SEMICOLON);
        }
        else
            loopChildren.add(parseStatement());
    }

    private Node parseFor() throws SyntaxException {
        ArrayList<Node> loopChildren = new ArrayList<>();
        Token loopToken = currentToken();
        goToNextToken();
        require(VARIABLE);
        Token loopCounterVariable = currentToken();
        goToNextToken();
        require(KEYWORD_ASSIGN);
        Node from = parseExpr();
        require(KEYWORD_TO);
        Node to = parseExpr();
        loopChildren.add(new VarNode(getList(from, to), loopCounterVariable));
        require(KEYWORD_DO);
        goToNextToken();
        isLoopBlock(loopChildren);
        return new LoopForNode(loopChildren, loopToken);
    }

    private Node parseWhile() throws SyntaxException {
        ArrayList<Node> loopChildren = new ArrayList<>();
        Token whileToken = currentToken();
        loopChildren.add(getLogicalCondition());
        goToNextToken();
        require(KEYWORD_DO);
        goToNextToken();
        isLoopBlock(loopChildren);
        return new LoopForNode(loopChildren, whileToken);
    }

    private ArrayList<Node> parseBlock() throws SyntaxException {
        ArrayList<Node> children = new ArrayList<>();
        goToNextToken();
        while (currentValue() != KEYWORD_END) {
            switch (currentValue()) {
                case VARIABLE:
                    children.add(parseStatement());
                    break;
                case KEYWORD_FOR:
                    children.add(parseFor());
                    break;
                case KEYWORD_WHILE:
                    children.add(parseWhile());
                    break;
                case KEYWORD_IF:
                    children.add(parseIf());
                    continue;
                default:
                    require(VARIABLE);
                    break;
            }
            goToNextToken();
        }
        return children;
    }

    private void goToNextToken() { tokenizer.Next(); }

    private Token currentToken() { return tokenizer.getCurrentToken(); }

    private boolean isSimpleType() { return currentValue() == KEYWORD_INTEGER || currentValue() == KEYWORD_DOUBLE; }

    private boolean isType() { return currentValue() == VARIABLE; }

    private boolean isArray() { return currentValue() == KEYWORD_ARRAY; }

    private boolean isRecord() { return currentValue() == KEYWORD_RECORD; }

    private void findSemicolon() throws SyntaxException {
        goToNextToken();
        require(SEP_SEMICOLON);
        goToNextToken();
    }

    public class SymTable {
        public HashMap<String, TokenValue> table;
    }
}
