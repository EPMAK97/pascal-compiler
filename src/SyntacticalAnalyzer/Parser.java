package SyntacticalAnalyzer;

import Tokens.*;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;

import static Tokens.TokenValue.*;

public class Parser {
    private Tokenizer tokenizer;
    private static HashMap<TokenValue, String> hashTokens;

    private static void initHashTokens() {
        HashMap<String, Pair> operators = Tokenizer.getOperators();
        HashMap<String, Pair> separators = Tokenizer.getSeparators();
        HashMap<String, Pair> words = Tokenizer.getWords();

        for (String key : operators.keySet())
            hashTokens.put(operators.get(key).getTokenValue(), key);
        for (String key : separators.keySet())
            hashTokens.put(separators.get(key).getTokenValue(), key);
        for (String key : words.keySet())
            hashTokens.put(words.get(key).getTokenValue(), key);
    }

    public Parser(String filePath) throws SyntaxException {
        tokenizer = new Tokenizer(filePath);
        initHashTokens();
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
        if (currentValue() != tokenValue)
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
        tokenizer.Next();
        nodes.addAll(parseDeclarationPart());
        require(KEYWORD_BEGIN, "begin");
        ArrayList<Node> finalBlock = parseBlock(true);
        tokenizer.Next();
        require(SEP_DOT, ".");
        if (finalBlock != null)
            nodes.addAll(finalBlock);
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
                    list.add(new VarNode(parseVariables(false), tokenVar));
                    break;
                case KEYWORD_TYPE:
                    Token tokenType = tokenizer.getCurrentToken();
                    Node typeNode = new ConstantNode(parseTypes(), tokenType);
                    list.add(typeNode);
                    break;
                case KEYWORD_PROCEDURE:
                    Token tokenProcedure = tokenizer.getCurrentToken();
                    ArrayList<Node> procedureNode = parseProcedure();
                    ProcedureNode node;
                    if (procedureNode != null)
                        node = new ProcedureNode(procedureNode, tokenProcedure);
                    else {
                        node = new ProcedureNode(null, tokenProcedure);
                        tokenizer.Next();
                    }
                    list.add(node);
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
            if (currentValue() != OP_COLON) {
                tokenizer.Next();
                require(VARIABLE, "identifier");
            }
        }
        return newVariables;
    }

    private ArrayList<Node> parseVariables(boolean isFormalParamets) throws SyntaxException {
        ArrayList<Node> nodes = new ArrayList<>();
        tokenizer.Next();
        require(VARIABLE, "variable");
        while (currentValue() == VARIABLE) {

            ArrayList<Node> newVariables = getIdentifierList();

            tokenizer.Next();
            if (currentValue() == KEYWORD_ARRAY) {
                Token arrayToken = tokenizer.getCurrentToken();

                Node typeNode = parseArray();

                for (Node variable : newVariables) {
                    variable.setChildren(new ArrayList<>(Arrays.asList(typeNode)));
                }

                Node arrayNode = new ArrayNode(newVariables, arrayToken);
                findSemicolon();
                nodes.add(arrayNode);
                continue;
            }

            require(KEYWORD_INTEGER, KEYWORD_DOUBLE, "integer", "double");

            Node varNode = (currentValue() == KEYWORD_INTEGER) ?
                    new VarIntegerNode(newVariables, tokenizer.getCurrentToken()) :
                    new VarDoubleNode(newVariables, tokenizer.getCurrentToken());
            nodes.add(varNode);

            if (!isFormalParamets)
                findSemicolon();
            else
                tokenizer.Next();
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
        return currentValue() == KEYWORD_RECORD;
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

                for (Node variable : newVariables) {
                    variable.setChildren(new ArrayList<>(Arrays.asList(typeNode)));
                }

                Node arrayNode = new ArrayNode(newVariables, arrayToken);
                findSemicolon();
                nodes.add(arrayNode);
            }

            else if (isRecord()) {
                nodes.add(parseRecord(newVariables));
            }
        }
        return nodes;
    }

    private Node parseRecord(ArrayList<Node> newVariables) throws SyntaxException {
        Token recordToken = tokenizer.getCurrentToken();
        ArrayList<Node> children = new ArrayList<>();

        tokenizer.Next();
        while (currentValue() != KEYWORD_END) {
            ArrayList<Node> identifiers = getIdentifierList();
            tokenizer.Next();

            // TODO may be we need to do foreach loop for all identifiers and then parse ......
            if (isArray()) {
                Token arrayToken = tokenizer.getCurrentToken();
                //Node typeNode = new ArrayNode(new ArrayList<Node>(Arrays.asList(parseArray())), arrayToken);
                Node typeNode = parseArray();
                for (Node identifier : identifiers) {
                    identifier.setChildren(new ArrayList<>(Arrays.asList(typeNode)));
                }

                Node arrayNode = new ArrayNode(identifiers, arrayToken);
                //findSemicolon();

                children.add(arrayNode);
                //Node arrayNode = new ArrayNode(newVariables, arrayToken);

                findSemicolon();
            }

            if (isSimpleType() || currentValue() == VARIABLE) {
                Node simpleTypeNode = (currentValue() == KEYWORD_INTEGER) ?
                        new VarIntegerNode(identifiers, tokenizer.getCurrentToken()) :
                        new VarDoubleNode(identifiers, tokenizer.getCurrentToken());

//                for (Node identifier : identifiers) {
//                    identifier.setChildren(new ArrayList<>(Arrays.asList(simpleTypeNode)));
//                }

                children.add(simpleTypeNode);

                //Node newRecordField = new RecordNode(identifiers, recordToken);
                //children.add(newRecordField);
                //children.add(new VarNode(new ArrayList<Node>(Arrays.asList(newRecordField))));
                findSemicolon();
            }

            if (isRecord()) {
                children.add(parseRecord(identifiers));
            }
        }

        tokenizer.Next();
        tokenizer.Next();

        for (Node variable : newVariables) {
            variable.setChildren(children);
        }

        Node recordsNode = new ArrayNode(newVariables, recordToken);
        return recordsNode;
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

    private ArrayList<Node> parseProcedure() throws SyntaxException {
        tokenizer.Next();
        require(VARIABLE, "identifier");
        tokenizer.Next();

        ArrayList<Node> parameters = null;
        ArrayList<Node> localParameters = null;
        ArrayList<Node> block = null;
        if (currentValue() == SEP_BRACKETS_LEFT) {
            parameters = parseFormalParameters();
        }
        if (currentValue() != KEYWORD_BEGIN) {
            require(SEP_SEMICOLON, ";");
            tokenizer.Next();
            localParameters = parseDeclarationPart();
        }
        require(KEYWORD_BEGIN, "begin");
        block = parseBlock(false);
        findSemicolon();
        ArrayList<Node> procedureNodes = new ArrayList<>();
        if (parameters != null)
            procedureNodes.addAll(parameters);
        if (localParameters != null)
            procedureNodes.addAll(localParameters);
        procedureNodes.addAll(block);
        return procedureNodes;
    }

    private ArrayList<Node> parseFormalParameters() throws SyntaxException {
        //tokenizer.Next();
        ArrayList<Node> parameters = new ArrayList<>();

        tokenizer.Next();
        while (currentValue() != SEP_BRACKETS_RIGHT) {

            switch (currentValue()) {
                case KEYWORD_VAR:
                    //parameters.addAll(parseVariables(true));
                    //System.out.println(currentValue());
                    tokenizer.Next();
                    parameters.addAll(getFormalParameters());
                    tokenizer.Next();
                    break;
                case VARIABLE:
                    // TODO make type checking
                    parameters.addAll(getFormalParameters());
                    tokenizer.Next();
                    break;
                default:
                    require(SEP_SEMICOLON, SEP_BRACKETS_RIGHT, ";", ")");
                    tokenizer.Next();
                    //System.out.println(currentValue());
                    break;
            }
        }
        tokenizer.Next();
        require(SEP_SEMICOLON, ";");
        return parameters;
    }

    private ArrayList<Node> getFormalParameters() throws SyntaxException {
        ArrayList<Node> parameters = new ArrayList<>();
        ArrayList<Node> newVarVariables = getIdentifierList();
        tokenizer.Next();
        require(KEYWORD_INTEGER, KEYWORD_DOUBLE, KEYWORD_ARRAY ,"integer", "double", "array");
        if (currentValue() == KEYWORD_ARRAY) {
            Token arrayToken = tokenizer.getCurrentToken();
            tokenizer.Next();
            tokenizer.Next();
            VarNode varNode = new VarNode(newVarVariables, tokenizer.getCurrentToken());
            parameters.add(new ArrayNode(new ArrayList<>(Arrays.asList(varNode)), arrayToken));
        }
        else
            parameters.add(currentValue() == KEYWORD_INTEGER ?
                    new VarIntegerNode(newVarVariables, tokenizer.getCurrentToken()) :
                    new VarDoubleNode(newVarVariables, tokenizer.getCurrentToken()));
        return parameters;
    }

    private ArrayList<Node> parseBlock(boolean isFinalBlock) throws SyntaxException {
        ArrayList<Node> children = new ArrayList<>();
        tokenizer.getNextToken();
        while (currentValue() != KEYWORD_END) {
            switch (currentValue()) {
                case VARIABLE:
                    Token variableToken = tokenizer.getCurrentToken();
                    tokenizer.Next();
                    require(KEYWORD_ASSIGN, ":=");
                    Node expression = parseLogicalExpression();
                    require(SEP_SEMICOLON, ";");
                    children.add(new VarNode(new ArrayList<Node>(Arrays.asList(expression)), variableToken));
                    //System.out.println(currentValue());
                    //System.out.println("OK");
                    break;
                case KEYWORD_FOR:
                    ArrayList<Node> loopChildren = new ArrayList<>();
                    Token loopToken = tokenizer.getCurrentToken();
                    tokenizer.Next();
                    require(VARIABLE, "identifier");
                    Token loopCounterVariable = tokenizer.getCurrentToken();
                    //Node loopCounterVariableNode = new VarNode(tokenizer.getCurrentToken());
                    tokenizer.Next();
                    require(KEYWORD_ASSIGN, ":=");
                    Node from = parseExpr();
                    require(KEYWORD_TO, "to");
                    Node to = parseExpr();
                    loopChildren.add(new VarNode(new ArrayList<>(Arrays.asList(from, to)), loopCounterVariable));
                    require(KEYWORD_DO, "do");
                    tokenizer.Next();
                    if (currentValue() == KEYWORD_BEGIN)
                        loopChildren.addAll(parseBlock(false));
                    else {
                        require(VARIABLE, "identifier");
                        Token insideLoopVariableToken = tokenizer.getCurrentToken();
                        tokenizer.Next();
                        require(KEYWORD_ASSIGN, ":=");
                        Node insideLoopExpression = parseLogicalExpression();
                        loopChildren.add(new VarNode(new ArrayList<>(Arrays.asList(insideLoopExpression)), insideLoopVariableToken));
                        require(SEP_SEMICOLON, ";");
                        //tokenizer.Next();
                        //System.out.println(tokenizer.getCurrentToken());
                    }
                    children.add(new LoopForNode(loopChildren, loopToken));
                    if (currentValue() == KEYWORD_END)
                        tokenizer.Next();
                    break;
                case KEYWORD_WHILE:
                    ArrayList<Node> loopChildren1 = new ArrayList<>();
                    Token whileToken = tokenizer.getCurrentToken();
                    tokenizer.Next();
                    require(VARIABLE, "identifier");
                    Node leftCondition = new VarNode(tokenizer.getCurrentToken());
                    tokenizer.Next();
                    if (!isLogical(currentValue()))
                        throw new SyntaxException(String.format("Error in pos %s:%s required %s but found %s",
                                tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY(),
                                "logical", tokenizer.getCurrentToken().getText()));
                    Token logicToken = tokenizer.getCurrentToken();
                    tokenizer.Next();
                    require(VARIABLE, CONST_INTEGER, CONST_DOUBLE, "identifier", "integer", "double");
                    Node rightCondition = new VarNode(tokenizer.getCurrentToken());
                    loopChildren1.add(new VarNode(new ArrayList<>(Arrays.asList(leftCondition, rightCondition)), logicToken));
                    tokenizer.Next();
                    require(KEYWORD_DO, "do");
                    tokenizer.Next();
                    if (currentValue() == KEYWORD_BEGIN)
                        loopChildren1.addAll(parseBlock(false));
                    else {
                        require(VARIABLE, " identifier");
                        Token insideLoopVariableToken = tokenizer.getCurrentToken();
                        tokenizer.Next();
                        require(KEYWORD_ASSIGN, ":=");
                        Node insideLoopExpression = parseLogicalExpression();
                        loopChildren1.add(new VarNode(new ArrayList<>(Arrays.asList(insideLoopExpression)), insideLoopVariableToken));
                        require(SEP_SEMICOLON, ";");
                    }
                    children.add(new LoopForNode(loopChildren1, whileToken));
                    if (currentValue() == KEYWORD_END)
                        tokenizer.Next();
                    //Node node1 = new LogicOperation(new ArrayList<>(Arrays.asList()), tokenizer.);
                    break;
                case KEYWORD_PROCEDURE:
                    if (isFinalBlock)
                        throw new SyntaxException(String.format("Error in pos %s:%s illegal expression",
                                tokenizer.getCurrentToken().getPosX(), tokenizer.getCurrentToken().getPosY()));
                    Token tokenProcedure = tokenizer.getCurrentToken();
                    ArrayList<Node> procedureNode = parseProcedure();
                    ProcedureNode node;
                    if (procedureNode != null)
                        node = new ProcedureNode(procedureNode, tokenProcedure);
                    else {
                        node = new ProcedureNode(null, tokenProcedure);
                        tokenizer.Next();
                    }
                    children.add(node);
                    continue;
                default:
                    require(VARIABLE, "identifier");
                    break;
            }
            tokenizer.Next();
        }
        return children;
    }

    public class SymTable {
        public HashMap<String, TokenValue> table;
    }
}
