package SyntacticalAnalyzer;

import Tokens.*;
import Tokens.Pair;
import javafx.util.*;

import java.util.*;

import static Tokens.TokenValue.*;

public class Parser {
    private Tokenizer tokenizer;
    private static HashMap<TokenValue, String> hashTokens;
    private static ArrayList<Map<String, javafx.util.Pair<TokenValue, ArrayList<Node>>>> symTables;
    private static int currentSymTableIndex = 0;

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

        symTables = new ArrayList<>();
        currentSymTableIndex = 0;
    }

    public Parser(String filePath) throws SyntaxException {
        tokenizer = new Tokenizer(filePath);
        //Node node = parse();
    }

    public class Node {

        ArrayList<Node> children;
        Token token;
        private TokenValue resultType;

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

        public Token getToken() { return token; }

        public TokenValue getResultType() { return resultType; }

        public void setResultType(TokenValue resultType) { this.resultType = resultType; }
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
            Node parseTermNode = parseTerm();
            //System.out.println(parseTermNode.getResultType() + " ASDFASDFASDF LOL LOL LOL");
            TokenValue value = compareRelevanceOfTypes(e, parseTermNode.getToken(), t.getTokenValue());
            //System.out.println("ParseExprValue " + value);
            ArrayList<Node> arrayList = getList(e, parseTermNode);
            e = new BinOpNode(arrayList, t);
            //System.out.println(e.getToken() + " LOLL ");
            e.setResultType(value);
            t = currentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException {
        Node e = parseFactor();
        Token t = tokenizer.getNextToken();
        while (t.getTokenValue() != KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
            Node parseFactorNode = parseFactor();
            TokenValue value = compareRelevanceOfTypes(e, parseFactorNode.getToken(), t.getTokenValue());
            //System.out.println("ParseTermValue " + value);
            ArrayList<Node> arrayList = getList(e, parseFactorNode);
            e = new BinOpNode(arrayList, t);
            e.setResultType(value);
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
                TokenValue value = checkFactorSymbol(currentToken().getText()); // TODO что-то сделать для отображения
                Node tmp = new VarNode(t);
                if (value == KEYWORD_ARRAY)
                    tmp.setResultType(getArrayTypeFromTable(t.getText()));
                return tmp;
            case CONST_INTEGER:
                Node node = new ConstNode(t);
                node.setResultType(CONST_INTEGER);
                return node;
            case CONST_DOUBLE:
                Node node1 = new ConstNode(t);
                node1.setResultType(CONST_DOUBLE);
                return node1;
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

    private String castValueToOutput(TokenValue value) {
        switch (value) {
            case CONST_DOUBLE:      return "double";
            case CONST_INTEGER:     return "integer";
            case KEYWORD_INTEGER:   return "integer";
            case KEYWORD_DOUBLE:    return "double";
            case KEYWORD_TYPE:      return "type";
            case KEYWORD_DIV:       return "div";
            case KEYWORD_MOD:       return "mod";
            case KEYWORD_SHL:       return "shl";
            case KEYWORD_SHR:       return "shr";
            case KEYWORD_XOR:       return "xor";
            default:                return "";
        }
    }

    private TokenValue checkIntegerOperation(TokenValue left, TokenValue right, TokenValue op) throws SyntaxException {
        //System.out.println(right);
        if (left != KEYWORD_INTEGER && left != CONST_INTEGER ||
                right != KEYWORD_INTEGER && right != CONST_INTEGER)
            throw new SyntaxException(String.format("Error in pos %s:%s operator is not overloaded: \"%s\" %s \"%s\"",
                    currentToken().getPosX(), currentToken().getPosY(),
                    castValueToOutput(left), castValueToOutput(op), castValueToOutput(right)));
        return CONST_INTEGER;
    }

    private boolean checkTypes(TokenValue currenValue, TokenValue ... values) {
        for (TokenValue value : values)
            if (currenValue == value)
                return true;
        return false;
    }

    private TokenValue checkTypeOperation(TokenValue left, TokenValue right) {
        if (checkTypes(left, KEYWORD_DOUBLE, CONST_DOUBLE) ||
                checkTypes(right, KEYWORD_DOUBLE, CONST_DOUBLE))
            return CONST_DOUBLE;
        return CONST_INTEGER;
    }


    private TokenValue compareRelevanceOfTypes(Node right, Token left, TokenValue operation) throws SyntaxException {
        // TODO мне надо проверять, есть ли деление или вещ число в операциях чтобы определить, подходит ли то что слева для того, что справа!!1!!1

        TokenValue leftOperandType = left.getTokenValue() == VARIABLE ?
                getTypeFromTable(left.getText()) : left.getTokenValue();
        TokenValue rightOperandType = right.token.getTokenValue() == VARIABLE ?
                getTypeFromTable(right.token.getText()) : right.token.getTokenValue();

        if (right.getResultType() != null)
            rightOperandType = right.getResultType();

//        System.out.println("right = " + rightOperandType + " his result type = " + right.getResultType());
//        System.out.println();

        switch (operation) {
            case KEYWORD_MOD:
                return checkIntegerOperation(leftOperandType, rightOperandType, operation);
            case KEYWORD_DIV:
                return checkIntegerOperation(leftOperandType, rightOperandType, operation);
            case KEYWORD_SHL:
                return checkIntegerOperation(leftOperandType, rightOperandType, operation);
            case KEYWORD_SHR:
                return checkIntegerOperation(leftOperandType, rightOperandType, operation);
            case KEYWORD_XOR:
                return checkIntegerOperation(leftOperandType, rightOperandType, operation);
            case OP_PLUS:
                return checkTypeOperation(leftOperandType, rightOperandType);
            case OP_MINUS:
                return checkTypeOperation(leftOperandType, rightOperandType);
            case OP_MULT:
                return checkTypeOperation(leftOperandType, rightOperandType);
            case OP_DIVISION:
                return CONST_DOUBLE;
        }
        if (operation == OP_GREATER          ||
            operation == OP_LESS             ||
            operation == OP_GREATER_OR_EQUAL ||
            operation == OP_LESS_OR_EQUAL    ||
            operation == OP_EQUAL            ||
            operation == OP_NOT_EQUAL        ||
            operation == KEYWORD_AND         ||
            operation == KEYWORD_OR)
            return CONST_INTEGER;
        else
            return null;
    }

    private ArrayList<Node> getList(Node ... nodes) {
        return new ArrayList<>(Arrays.asList(nodes));
    }

    private void require(TokenValue ... tokenValues) throws SyntaxException {
        StringJoiner joiner = new StringJoiner(" or ");
        for (TokenValue value : tokenValues) {
            if (currentValue() == value)
                return;
            if (value == CONST_INTEGER)
                joiner.add("integer");
            else if (value == CONST_DOUBLE)
                joiner.add("double");
            else
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
        symTables.add(new LinkedHashMap<>());
        nodes.addAll(parseDeclarationPart());
        require(KEYWORD_BEGIN);
        ArrayList<Node> finalBlock = parseBlock();
        goToNextToken();
        require(SEP_DOT);
        if (finalBlock != null)
            nodes.addAll(finalBlock);
        printSymTable();
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
        ArrayList<Node> list = new ArrayList<>(); // TODO здесь идет обращение к последней таблице символов в листе
        while (currentValue() != KEYWORD_BEGIN) // end of declaration part
            list.add(getNextDeclarationNode());
        return list;
    }

    private void printSymTable() {
        Map<String, javafx.util.Pair<TokenValue, ArrayList<Node>>> table = symTables.get(currentSymTableIndex);
        //java.util.Iterator iter = table.keySet().iterator();
        for (Map.Entry<String, javafx.util.Pair<TokenValue, ArrayList<Node>>> entry : table.entrySet()) {
            String key = entry.getKey();
            javafx.util.Pair<TokenValue, ArrayList<Node>> value = entry.getValue();
            System.out.print(String.format("%s" +
                            "%" + (10 - key.length()) + "s"
                            + "%" + 10 + "s"
                            + "%" + (10 - value.getKey().toString().length()) + "s",
                    key, "", value.getKey().toString(), ""));
            System.out.println();
            if (value.getValue() != null)
                value.getValue().forEach(item -> item.print(String.format("%" + 30 + "s", ""), false));
            System.out.println();

        }
        //table.entrySet().forEach(item ->
                //System.out.println(item.getKey() + ": " + item.getValue().getKey() + " " + item.getValue().getValue())
        //);
    }

    private void declareRecord(ArrayList<Node> newVariables, Node record, TokenValue value) throws SyntaxException {
        for (Node node : newVariables) {
            if (!symTables.get(currentSymTableIndex).containsKey(node.token.getText())) {
                //System.out.println(node.token.getText() + " " + value);
                symTables.get(currentSymTableIndex).put(node.token.getText(), new javafx.util.Pair<>(value, record.children.get(0).children));
                //System.out.println("its value = " + symTable.get(node.token.getText()).getValue());
            } else
                throw new SyntaxException(String.format("Error in pos %s:%s duplicate identifier \"%s\"",
                        node.getToken().getPosX(), node.getToken().getPosY(), node.children == null ? node.getToken().getText() :
                                node.children.get(0).getToken().getText()));
        }
    }

    private void declareVariables(ArrayList<Node> newVariables, TokenValue value) throws SyntaxException {
        for (Node node : newVariables) {
            if (!symTables.get(currentSymTableIndex).containsKey(node.token.getText())) {
                //System.out.println(node.token.getText() + " " + value);
                symTables.get(currentSymTableIndex).put(node.token.getText(), new javafx.util.Pair<>(value, node.children));
                //System.out.println("its value = " + symTable.get(node.token.getText()).getValue());
            } else
                throw new SyntaxException(String.format("Error in pos %s:%s duplicate identifier \"%s\"",
                        node.getToken().getPosX(), node.getToken().getPosY(), node.getToken().getText()));
        }
    }

    private ArrayList<Node> getIdentifierList(TokenValue mark) throws SyntaxException {
        ArrayList<Node> newVariables = new ArrayList<>();
        while (currentValue() != mark) {
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
            Token typeToken = null;
            ArrayList<Node> newVariables = getIdentifierList(OP_COLON);
            goToNextToken();
            if (isArray()) {
                declareVariables(newVariables, currentValue());
                Node arrayNode = parseArray(newVariables);
                goToNextToken();
                if (currentValue() == OP_EQUAL) {
                    if (newVariables.size() > 1)
                        throw new SyntaxException(String.format("Error in pos %s:%s only one variable can be initialized",
                                currentToken().getPosX(), currentToken().getPosY()));
                    goToNextToken();
                    require(SEP_BRACKETS_LEFT);
                    ArrayList<Integer> tmp = new ArrayList<>();
                    int count = newVariables.get(0).children.get(0).children.size() / 2 - 1;
                    for (int i = 0; i < count; i++) tmp.add(0);
                    initializeArray1(newVariables, tmp, 0); // after...
                    // setValueToSymTable(...) ... after
                }
                setValuesToSymTable(arrayNode);
                nodes.add(arrayNode);
                require(SEP_SEMICOLON);
                goToNextToken();
                continue;
            }
            else if (isRecord()) {
                typeToken = currentToken();
                Node record = parseRecord(newVariables);
                declareVariables(newVariables, typeToken.getTokenValue());
                require(SEP_SEMICOLON, OP_EQUAL);
                nodes.add(record);
                if (currentValue() == OP_EQUAL)
                    initializeRecord(record);
                goToNextToken();
                continue;
            }
            if (currentValue() == VARIABLE) {
                int symTableIndex = checkAlias(currentToken().getText());
                declareVariables(newVariables, currentValue());
                typeToken = symTables.get(symTableIndex).get(currentToken().getText()).getValue().get(0).getToken();
                //System.out.println(symTable.get(currentToken().getText()).getKey());
                Node varNode = new VarNode(newVariables, typeToken);
                nodes.add(varNode);
                initializeVariable(typeToken.getTokenValue(), newVariables);
            } else {
                require(KEYWORD_INTEGER, KEYWORD_DOUBLE);
                //type = currentValue();
                typeToken = currentToken();
                declareVariables(newVariables, currentValue());
                Node varNode = (currentValue() == KEYWORD_INTEGER) ?
                        new VarIntegerNode(newVariables, currentToken()) :
                        new VarDoubleNode(newVariables, currentToken());
                nodes.add(varNode);
                initializeVariable(typeToken.getTokenValue(), newVariables);
            }
            //findSemicolon();
        }
        return nodes;
    }

    private void parseInsideArrayDefinition(ArrayList<Node> list, int left, int right) throws SyntaxException {
        TokenValue requiredType = list.get(0).children.get(0).getToken().getTokenValue();
        for (int i = left; i <= right; i++) {
            if (requiredType == KEYWORD_INTEGER)
                if (currentValue() == CONST_DOUBLE)
                    throw new SyntaxException(String.format("Error in pos %s:%s got \"double\" expected \"integer\"",
                            currentToken().getPosX(), currentToken().getPosY()));
            goToNextToken();
            require(i < right ? SEP_COMMA : SEP_BRACKETS_RIGHT);
            if (currentValue() != SEP_BRACKETS_RIGHT)
                goToNextToken();
        }
    }

    private int getDimensionByIndex(ArrayList<Node> list, int index) {
        return Integer.parseInt(list.get(0).children.get(0).children.get(index).getToken().getText());
    }

    private void initializeRecord(Node record) throws SyntaxException {
        goToNextToken();
        require(SEP_BRACKETS_LEFT);
        for (Node valueOfRecord : record.children.get(0).children) {
            goToNextToken();
            TokenValue currentValue = valueOfRecord.children.get(0).getToken().getTokenValue();
            require(currentValue);
            Token recordToken = valueOfRecord.children.get(0).getToken();
            if (!currentToken().getText().equals(recordToken.getText()))
                throw new SyntaxException(String.format("Error in pos %s:%s unknown record field identifier \"%s\"",
                        currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
            goToNextToken();
            require(OP_COLON);
            if (valueOfRecord.getToken().getTokenValue() == KEYWORD_RECORD)
                initializeRecord(valueOfRecord);
            else if (valueOfRecord.getToken().getTokenValue() == KEYWORD_ARRAY) {
                goToNextToken();
                require(SEP_BRACKETS_LEFT);
                ArrayList<Integer> tmp = new ArrayList<>();
                int count = valueOfRecord.children.get(0).children.get(0).children.size() / 2 - 1;
                for (int i = 0; i < count; i++) tmp.add(0);
                ArrayList<Node> nodes =  valueOfRecord.children;
                initializeArray1(nodes, tmp, 0);
                require(SEP_SEMICOLON);
            }
            else{
                Node expression = parseExpr();
                checkOperationCompatibility(valueOfRecord.children.get(0).getToken().getTokenValue(), expression.getResultType());
                require(SEP_SEMICOLON);
                System.out.println("checked");
            }
        }
        require(SEP_SEMICOLON);
        goToNextToken();
        require(SEP_BRACKETS_RIGHT);
        goToNextToken();
        require(SEP_SEMICOLON);
    }

    private void initializeArray1(ArrayList<Node> list, ArrayList<Integer> marks, int position) throws SyntaxException {
        int dimensionsCount = list.get(0).children.get(0).children.size() / 2;
        goToNextToken();
        int index1 = position == 0 ? 0 : position * 2;
        int index2 = position == 0 ? 1 : position * 2 + 1;
        int rightSize = getDimensionByIndex(list, index2) - getDimensionByIndex(list, index1) + 1; // т.к.[]
        if (position < dimensionsCount - 1) {  // попадаем в промежуток до - 1 элемента
            require(SEP_BRACKETS_LEFT, SEP_COMMA, SEP_BRACKETS_RIGHT, SEP_SEMICOLON, CONST_INTEGER, CONST_DOUBLE);
            switch (currentValue()) {
                case SEP_COMMA:
                    marks.set(position, marks.get(position) + 1); // Сохранять будем только когда мы вышли из скобки или на запятой стоим
                    // будем чистить здесь)//noooo
                    if (marks.get(position) == rightSize)
                        require(SEP_BRACKETS_RIGHT);
                        //System.out.println("Необходимый размер превышен 523, " + currentToken().getPosX() + " " + currentToken().getPosY());
                    initializeArray1(list, marks, position + 1); // пробросим в ELSE
                    break;
                case SEP_BRACKETS_RIGHT:
                    marks.set(position, marks.get(position) + 1); // Сохранять будем только когда мы вышли из скобки(поднялись из ELSE)
                    // чистим
                    // проверяем размерность
                    if (marks.get(position) != rightSize)
                        require(SEP_COMMA);
                        //System.out.println("Необходимый размер не набран 531, " + currentToken().getPosX() + " " + currentToken().getPosY());
                    if (position != 0)
                        for (int i = position; i < marks.size(); i++)
                            marks.set(i, 0);
                    initializeArray1(list, marks, position == 0 ? 0 : position - 1); // будем ждать запятую или
                    break;
                case SEP_SEMICOLON:
                    return;
                case SEP_BRACKETS_LEFT:
                    initializeArray1(list, marks, position + 1);
                    break;
            }
        } else {
            require(SEP_BRACKETS_LEFT, CONST_INTEGER, CONST_DOUBLE, SEP_SEMICOLON);
            if (currentValue() == SEP_BRACKETS_LEFT)
                goToNextToken();
            if (currentValue() == SEP_SEMICOLON) {
                if (position == 0)
                    return;
                if (marks.get(position) != rightSize)
                    throw new SyntaxException(String.format("Error in pos %s:%s not dialed the required size",
                            currentToken().getPosX(), currentToken().getPosY()));
                  //  System.out.println("Необходимый размер не набран 538, " + currentToken().getPosX() + " " + currentToken().getPosY());
                else
                    return;
            }
            parseInsideArrayDefinition(list, getDimensionByIndex(list, index1),  getDimensionByIndex(list, index2));
            require(SEP_BRACKETS_RIGHT);
            initializeArray1(list, marks, position == 0 ? 0 : position - 1);
        }
    }

    private void initializeVariable(TokenValue type, ArrayList<Node> list) throws SyntaxException {
        goToNextToken();
        if (currentValue() == SEP_SEMICOLON)
            goToNextToken();
        else {
            require(OP_EQUAL, SEP_SEMICOLON);
            //goToNextToken();
            if (list.size() > 1)
                throw new SyntaxException(String.format("Error in pos %s:%s only one variable can be initialized",
                        currentToken().getPosX(), currentToken().getPosY()));
            Node expression = parseExpr();
            if (type == KEYWORD_INTEGER &&  expression.getResultType() == CONST_DOUBLE)
                throw new SyntaxException(String.format("Error in pos %s:%s incompatible types: got \"double\" expected \"integer\"",
                        currentToken().getPosX(), currentToken().getPosY()));
            setValueToSymTable(list.get(0).getToken().getText(), expression);
            //System.out.println(symTables.get(0).get(list.get(0).getToken().getText()).getValue().get(0));
            require(SEP_SEMICOLON);
            goToNextToken();
        }
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
        declareVariables(nodes, KEYWORD_CONST);
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
            //ArrayList<Node> newVariables = getIdentifierList(OP_EQUAL);
            //ArrayList<Node> newVariables = getIdentifierList(null);
            Token currentVariable = currentToken();
            goToNextToken();
            require(OP_EQUAL);
            goToNextToken();
            if (isSimpleType()) {
                Node varNode = currentValue() == KEYWORD_INTEGER ?
                        new VarIntegerNode(getList(new VarNode(currentToken())), currentVariable) :
                        new VarDoubleNode(getList(new VarNode(currentToken())), currentVariable);
                nodes.add(varNode);
//                for (Node variable : newVariables)
//                    variable.setChildren();
                findSemicolon();
            }
            else if (isType()) {
                require(VARIABLE); // TODO make types checking
                nodes.add(new VarNode(currentVariable));
                findSemicolon();
            }
            else if (isArray()) {
                nodes.add(parseArray(getList(new VarNode(currentVariable))));
                findSemicolon();
            }
            else if (isRecord()) {
                //Token token =
                //Node record = parseRecord(getList(new VarNode(currentVariable)));
                //declareVariables(newVariables, typeToken.getTokenValue());
                nodes.add(parseRecord(getList(new VarNode(currentVariable))));
                goToNextToken();
            }
        }
        declareVariables(nodes, KEYWORD_TYPE);
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
                continue;
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
        require(KEYWORD_END);
        goToNextToken();
        //require(SEP_SEMICOLON);
        //goToNextToken();
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
        int i;
        Token typeToken = null;
        if (currentValue() == VARIABLE) {
            i = checkAlias(currentToken().getText());
            typeToken = symTables.get(i).get(currentToken().getText()).getValue().get(0).getToken();
        }
        if (typeToken == null)
            require(KEYWORD_INTEGER, KEYWORD_DOUBLE);
        Node typeNode = new VarNode(arrayDimensional, typeToken == null ? currentToken() : typeToken); // TODO это тип, надо проверить alias и потом на базовый тип
        for (Node variable : newVariables)
            variable.setChildren(getList(typeNode));
        return new ArrayNode(newVariables, arrayToken);
    }

    private ArrayList<Node> parseSubroutine(boolean isFunction) throws SyntaxException {
        symTables.add(new LinkedHashMap<>()); // TODO
        currentSymTableIndex++;
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
        currentSymTableIndex--;
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
        checkIfConditionVariable(currentToken().getText());
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

    private TokenValue getTypeFromTable(String key) {
        for (int i = 0; i < symTables.size(); i++) {
            if (symTables.get(i).containsKey(key))
                return symTables.get(i).get(key).getKey();
        }
        return null; // Never! But it's necessary
    }

    private TokenValue convertToType(Node expression) {
        return null;
    }

    private void checkOperationCompatibility(TokenValue left, TokenValue right) throws SyntaxException {
        if (left == CONST_INTEGER || left == KEYWORD_INTEGER)
            if (right == CONST_DOUBLE)
                throw new SyntaxException(String.format("Error in pos %s:%s got \"double\" expected \"integer\"",
                        currentToken().getPosX(), currentToken().getPosY()));
    }

    private void checkOperationCompatibility(String value, TokenValue right, boolean isArray) throws SyntaxException {
        int i = checkPresenceSymbol(value);
        TokenValue left;
        javafx.util.Pair<TokenValue, ArrayList<Node>> tokenValuePair = symTables.get(i).get(value);
        left = isArray ? tokenValuePair.getValue().get(0).children.get(0).getToken().getTokenValue()
                : tokenValuePair.getKey();
        if (left == CONST_INTEGER || left == KEYWORD_INTEGER)
            if (right == CONST_DOUBLE)
                throw new SyntaxException(String.format("Error in pos %s:%s got \"double\" expected \"integer\"",
                        currentToken().getPosX(), currentToken().getPosY()));
    }

    private ArrayList<Integer> getDimensionFromSymTable(String value) throws SyntaxException {
        int index = checkPresenceSymbol(value);
        ArrayList<Node> arrayNode = symTables.get(index).get(value).getValue().get(0).children.get(0).children;
        ArrayList<Integer> dimension = new ArrayList<>();
        for (Node node : arrayNode)
            dimension.add(Integer.parseInt(node.getToken().getText()));
        return dimension;
    }

    private void checkLengthBrackets(String value) throws SyntaxException {
        ArrayList<Integer> dimension = getDimensionFromSymTable(value);
        int countBrackets = 0;
        while (countBrackets < dimension.size() / 2) {
            require(SEP_BRACKETS_SQUARE_LEFT);
            Node index = parseExpr(); // TODO потом здесь будет какая-то работа с памятью, пока только проверки :-)
            if (index.getResultType() != CONST_INTEGER)
                throw new SyntaxException(String.format("Error in pos %s:%s got \"double\" expected \"integer\"",
                        currentToken().getPosX(), currentToken().getPosY()));
            require(SEP_BRACKETS_SQUARE_RIGHT);
            goToNextToken();
            countBrackets++;
        }
        if (countBrackets + 1 != dimension.size() / 2 && currentValue() == SEP_BRACKETS_SQUARE_LEFT)
            throw new SyntaxException(String.format("Error in pos %s:%s expected identifier, constant or expression ",
                    currentToken().getPosX(), currentToken().getPosY()));
    }

    private Node parseStatement() throws SyntaxException {
        boolean isArray = false;
        require(VARIABLE);
        Token variableToken = currentToken();
        goToNextToken();
        require(KEYWORD_ASSIGN, SEP_BRACKETS_SQUARE_LEFT);
        isArray = currentValue() == SEP_BRACKETS_SQUARE_LEFT;
        if (isArray)
           checkLengthBrackets(variableToken.getText());
        require(KEYWORD_ASSIGN);
        Node expression = parseLogicalExpression();
        // достаём из таблицы символов
        checkOperationCompatibility(variableToken.getText(), expression.getResultType(), isArray);
        //compareRelevanceOfTypes()
        //System.out.println(expression.getResultType() + " TYPE ANS");
        //compareRelevanceOfTypes(variableToken.getTokenValue(), convertToType(expression), KEYWORD_ASSIGN);
        //setValueToSymTable(variableToken.getText(), expression);
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
        checkLoopCounterVariable(loopCounterVariable.getText());
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

    private int checkPresenceSymbol(String value) throws SyntaxException {
        for (int i = 0; i < symTables.size(); i++) {
            if (symTables.get(i).containsKey(value))
                return i;
        }
        throw new SyntaxException(String.format("Error in pos %s:%s identifier not found \"%s\"",
                currentToken().getPosX(), currentToken().getPosY(), value));
    }

    private void checkLoopCounterVariable(String value) throws SyntaxException {
//        checkPresenceSymbol(value);
//        if (!checkTypeMatch(value, VARIABLE))
//            throw new SyntaxException(String.format("Error in pos %s:%s illegal counter variable",
//                    currentToken().getPosX(), currentToken().getPosY()));
    }

    private void checkIfConditionVariable(String value) throws SyntaxException {
//        checkPresenceSymbol(value);
//        if (!checkTypeMatch(value, KEYWORD_INTEGER, KEYWORD_DOUBLE, KEYWORD_CONST))
//            throw new SyntaxException(String.format("Error in pos %s:%s current identifier not allowed here",
//                    currentToken().getPosX(), currentToken().getPosY()));
    }

    private int checkAlias(String value) throws SyntaxException {
        int presenceSymbol = checkPresenceSymbol(value);
        TokenValue tokenValue = symTables.get(presenceSymbol).get(value).getValue().get(0).getToken().getTokenValue();
        if (tokenValue != KEYWORD_INTEGER && tokenValue != KEYWORD_DOUBLE && tokenValue != KEYWORD_CONST) {
            throw new SyntaxException(String.format("Error in pos %s:%s current type not allowed here",
                    currentToken().getPosX(), currentToken().getPosY()));
        }
        return presenceSymbol;
    }

    private boolean checkTypeMatch(int i, String key, TokenValue ... values) throws SyntaxException {
        for (TokenValue value : values)
            if (symTables.get(i).get(key).getKey() == value)
                return true;
        return false;
    }

    private void checkStatementSymbol(String value) throws SyntaxException {
        int i = checkPresenceSymbol(value);
        if (!checkTypeMatch(i, value, KEYWORD_DOUBLE, KEYWORD_INTEGER, KEYWORD_ARRAY))
            throw new SyntaxException(String.format("Error in pos %s:%s current identifier \"%s\" not allowed here",
                    currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
    }

    private TokenValue checkFactorSymbol(String value) throws SyntaxException {
        int i = checkPresenceSymbol(value);
        if (!checkTypeMatch(i, value, KEYWORD_DOUBLE, KEYWORD_INTEGER, KEYWORD_CONST, KEYWORD_ARRAY)) {
            //System.out.println(currentToken().getTokenType());
            throw new SyntaxException(String.format("Error in pos %s:%s current type not allowed here",
                    currentToken().getPosX(), currentToken().getPosY()));
        }
        // Здесь будем обрабатывать массивы и функции
        if (checkTypeMatch(i, value, KEYWORD_ARRAY)) {
            goToNextToken();
            checkLengthBrackets(value);
            goToNextToken();
        }
        return symTables.get(i).get(value).getKey();
    }

    private void setValuesToSymTable(Node value) {
        String[] keys = new String[value.children.size()];
        //System.out.println(value.children.get(0).getToken());
        for (int i = 0; i < keys.length; i++) {
            keys[i] = value.children.get(i).getToken().getText();
            //System.out.println(keys[i]);
        }
        //System.out.println(value.children.get(0).children);
        Node kek = new ArrayNode(value.children.get(0).children, value.token);
        //System.out.println(kek.children.get(0));
        for (String key : keys) {
            symTables.get(currentSymTableIndex).computeIfPresent(key, (k, v) -> v = new javafx.util.Pair<>(v.getKey(), getList(kek)));
        }
            //symTables.get(currentSymTableIndex).put(key, new javafx.util.Pair<>(value.getToken().getTokenValue(), getList(value)));
    }

    private void setValueToSymTable(String key, Node statementNode) {
        symTables.get(currentSymTableIndex).computeIfPresent(key, (k, v) -> v = new javafx.util.Pair<>(v.getKey(), getList(statementNode)));
    }

    private ArrayList<Node> parseBlock() throws SyntaxException {
        ArrayList<Node> children = new ArrayList<>();
        goToNextToken();
        while (currentValue() != KEYWORD_END) {
            switch (currentValue()) {
                case VARIABLE:
                    checkStatementSymbol(currentToken().getText());
                    Node node = parseStatement();
                    //setValueToSymTable();
                    children.add(node);
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

    private TokenValue getArrayTypeFromTable(String value) throws SyntaxException {
        int i = checkPresenceSymbol(value);
        return symTables.get(i).get(value).getValue().get(0).children.get(0).getToken().getTokenValue();
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
}
