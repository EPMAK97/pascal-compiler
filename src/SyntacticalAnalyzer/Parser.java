package SyntacticalAnalyzer;

import Tokens.Token;
import Tokens.TokenType;
import Tokens.TokenValue;
import Tokens.Tokenizer;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.util.*;

import static Tokens.TokenValue.*;

public class Parser {
    private Tokenizer tokenizer;
    private static HashMap<TokenValue, String> hashTokens;
    private Stack<SymTable> tables;

    static {
        hashTokens = new HashMap<>();
        HashMap<String, Tokens.Pair> operators  = Tokenizer.getOperators();
        HashMap<String, Tokens.Pair> separators = Tokenizer.getSeparators();
        HashMap<String, Tokens.Pair> words      = Tokenizer.getWords();
        for (String key : operators.keySet())
            hashTokens.put(operators.get(key).getTokenValue(), key);
        for (String key : separators.keySet())
            hashTokens.put(separators.get(key).getTokenValue(), key);
        for (String key : words.keySet())
            hashTokens.put(words.get(key).getTokenValue(), key);
        // this is not in the hash
        hashTokens.put(VARIABLE, "identifier");
        hashTokens.put(CONST_STRING, "const string");
        hashTokens.put(SEP_DOUBLE_DOT, "..");
        hashTokens.put(KEYWORD_ASSIGN, ":=");
    }

    public Parser(String filePath) throws SyntaxException {
        tokenizer = new Tokenizer(filePath);
        //Node node = parse();
    }


    private Node parseLogicalExpression() throws SyntaxException, ScriptException  {
        Node e = parseExpr();
        Token t = currentToken();
        while (t.getTokenValue() != KEYWORD_EOF && isLogical(t.getTokenValue())) {
            goToNextToken();
            Node parseExprNode = parseExpr();
            e = calculateConstants(e, parseExprNode, t);
            t = currentToken();
        }
        return e;
    }

    // a + (b * 2)

    private Node parseExpr() throws SyntaxException, ScriptException {
        Node e = parseTerm();
        //Token t = tokenizer.getNextToken();
        Token t = currentToken(); // +
        while (t.getTokenValue() != KEYWORD_EOF && isExpr(t.getTokenValue())) {
            goToNextToken();
            Node parseTermNode = parseTerm();
            //TokenValue value = compareRelevanceOfTypes(e, parseTermNode.getToken(), t.getTokenValue());
            e = calculateConstants(e, parseTermNode, t);
            //e.setResultType(value);
            t = currentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException, ScriptException {
        Node e = parseFactor();
        //e = calculateConstants();
        //ConstNode node = (ConstNode)e;
        //System.out.println(node.result);
        Token t = tokenizer.getNextToken();
        while (t.getTokenValue() != KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
            goToNextToken();
            Node parseFactorNode = parseFactor();
            //TokenValue value = compareRelevanceOfTypes(e, parseFactorNode.getToken(), t.getTokenValue());
            //System.out.println("ParseTermValue " + value);
            e = calculateConstants(e, parseFactorNode, t);
            //e.setResultType(value);
            t = tokenizer.getNextToken();
        }
        return e;
    }
    // a-;
    private Node parseFactor() throws SyntaxException, ScriptException {
        Token t = currentToken();
        switch (t.getTokenValue()) {
            case OP_MINUS:
                ArrayList<Node> list = getList(parseFactor());
                return new UnaryMinusNode(list, t);
            case KEYWORD_NOT:
                ArrayList<Node> list1 = getList(parseFactor());
                return new NotNode(list1, t);
            case VARIABLE:
                //TokenValue value = checkFactorSymbol(currentToken().getText()); // TODO что-то сделать для отображения
                Node tmp = new VarNode(t);
                //if (value == KEYWORD_ARRAY)
                  //  tmp.setResultType(getArrayTypeFromTable(t.getText()));
                return tmp;
//            case CONST_INTEGER:
//                //Node node = new ConstNode(t, NodeType.CONST_INT);
//                //node.setResultType(CONST_INTEGER); // TODO убрать нахрен тип результата, можно смотреть просто по категории
//                //return new ConstNode(t, NodeType.CONST_INT, t.getText());
//            case CONST_DOUBLE:
//                //Node node1 = new ConstNode(t, NodeType.CONST_DOUBLE);
//                //node1.setResultType(CONST_DOUBLE);
//                return new ConstNode(t, NodeType.CONST_DOUBLE, t.getText());
            case CONST_INTEGER:
                return new ConstNode(t, IntType(), t.getValue());
            case CONST_DOUBLE:
                return new ConstNode(t, DoubleType(), t.getValue());
            case CONST_STRING:
                return new ConstNode(t, CharType(), t.getText());
            case SEP_BRACKETS_LEFT:
                goToNextToken();
                Node e = parseExpr();
                //requireNew(SEP_BRACKETS_RIGHT);
                requireCurrentToken(SEP_BRACKETS_RIGHT);
                //requireFollowingToken(SEP_BRACKETS_RIGHT);
//                throw new SyntaxException(String.format("Error in pos %s:%s required %s but found %s",
//                        currentToken().getPosX(), currentToken().getPosY(),
//                        SEP_BRACKETS_RIGHT, currentToken().getText()));
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

    private boolean isIntOnly(TokenValue tv) {
        return tv == KEYWORD_SHL     ||
                tv == KEYWORD_SHR    ||
                tv == KEYWORD_DIV    ||
                tv == KEYWORD_MOD    ||
                tv == KEYWORD_OR     ||
                tv == KEYWORD_AND    ||
                tv == KEYWORD_XOR;
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

    private Type getTypeForOperation(Type left, Type right, Token operation) throws SyntaxException {
        if (operation == null)
            throw new SyntaxException(String.format("Error in pos %s:%s unknown operation",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (!left.isScalar() || !right.isScalar())
            throw new SyntaxException(String.format("Error in pos %s:%s incompatible types",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (isLogical(operation.getTokenValue()))
            return integer_;
        if (left == char_ || right == char_)
            throw new SyntaxException(String.format("Error in pos %s:%s incompatible types",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (isIntOnly(operation.getTokenValue())) {
            if (left != integer_ || right != integer_)
                throw new SyntaxException(String.format("Error in pos %s:%s incompatible types",
                        currentToken().getPosX(), currentToken().getPosY()));
        }
        Type resultType = left;
        if (left == integer_ && right == double_ || left == double_ && right == integer_)
            resultType = double_;
        else if (left != right)
            throw new SyntaxException(String.format("Error in pos %s:%s incompatible types",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (operation.getTokenValue() == OP_DIVISION)
            return double_;
        return resultType;
    }

    private boolean isCastOnly(Token operation) {
        return  operation.getTokenValue() != OP_PLUS  &&
                operation.getTokenValue() != OP_MINUS &&
                operation.getTokenValue() != OP_MULT  &&
                operation.getTokenValue() != OP_DIVISION;
    }

    private Object getResultLogicOperation(Node left, Node right, Token operation) {
        Double leftValue;
        Double rightValue;
        leftValue = left.type == char_ ? (int)left.token.getText().charAt(0) :
                Double.parseDouble(left.token.getText()); // TODO написать ноду каста если один из операндов дабл
        rightValue = right.type == char_ ? (int)right.token.getText().charAt(0) :
                Double.parseDouble(right.token.getText()); // TODO написать ноду каста
        switch (operation.getTokenValue()) {
            case OP_GREATER:
                return leftValue > rightValue ? 1 : 0;
            case OP_LESS:
                return leftValue < rightValue ? 1 : 0;
            case OP_GREATER_OR_EQUAL:
                return leftValue >= rightValue ? 1 : 0;
            case OP_LESS_OR_EQUAL:
                return leftValue <= rightValue ? 1 : 0;
            case OP_EQUAL:
                return leftValue.equals(rightValue) ? 1 : 0;
            case OP_NOT_EQUAL:
                return !leftValue.equals(rightValue) ? 1 : 0;
            default: return null;
        }
    }

    private String getJavaStyleOperation(Token operation) {
        switch (operation.getTokenValue()) {
            case KEYWORD_AND:
                return "&";
            case KEYWORD_OR:
                return "|";
            case KEYWORD_SHL:
                return "<<";
            case KEYWORD_SHR:
                return ">>";
            case KEYWORD_DIV:
                return "/";
            case KEYWORD_MOD:
                return "%";
            default: return null;
        }
    }

    private Node calculateConstants(Node left, Node right, Token operation) throws ScriptException {
        if (left.getToken().getTokenValue() == VARIABLE)
            left.type = tables.peek().getAliasType(left.getToken());
        if (right.getToken().getTokenValue() == VARIABLE)
            right.type = tables.peek().getAliasType(right.getToken());
        Type type = getTypeForOperation(left.type, right.type, operation);
        if (left.getToken().getTokenValue() == VARIABLE || right.getToken().getTokenValue() == VARIABLE)
        //if (!table.isConst(left.getToken()) || !table.isConst(right.getToken()))
            return new BinOpNode(getList(left, right), operation, type);
        ConstNode leftNode = (ConstNode)left;
        String leftOperand = leftNode.token == null ? leftNode.result.toString() : left.token.getText();
        String expressionToString = leftOperand + operation.getText() + right.token.getText();
        if (isCastOnly(operation))
            if (isLogical(operation.getTokenValue()))
                return new ConstNode(type, getResultLogicOperation(left, right, operation));
            else
                expressionToString = leftOperand + getJavaStyleOperation(operation) + right.token.getText();
        if (operation.getTokenValue() == KEYWORD_DIV) expressionToString += "|0"; // :)
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        Object result = engine.eval(expressionToString);
        return new ConstNode(type, result);
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
                joiner.add("const integer");
            else if (value == CONST_DOUBLE)
                joiner.add("const double");
            joiner.add(hashTokens.get(value));
        }

        throw new SyntaxException(String.format("Error in pos %s:%s required %s but found %s",
                currentToken().getPosX(), currentToken().getPosY(),
                joiner.toString(), currentToken().getText()));
    }

    private void requireCurrentToken(TokenValue ... tokenValues) throws SyntaxException {
        require(tokenValues);
    }

    private void requireFollowingToken(TokenValue ... tokenValues) throws SyntaxException {
        require(tokenValues);
        goToNextToken();
    }

    public Type parse() throws ScriptException {
        ArrayList<Node> nodes = new ArrayList<>();
        goToNextToken();
        if (currentValue() == KEYWORD_PROGRAM)
            parseProgram();
        tables = new Stack<>();
        tables.push(new SymTable());
        type_declaration_part();
        return new FunctionType(null, tables.pop(), NIL(), compound_statement());
        //return new MainNode(nodes, new Token(new Tokens.Pair(TokenType.IDENTIFIER, VARIABLE), 1, 2, "Main Program"));
    }

    private void parseProgram() throws SyntaxException {
//        goToNextToken();
//        require(VARIABLE);
//        goToNextToken();
//        require(SEP_SEMICOLON);
//        goToNextToken();
    }


    //change name plz
    private void type_declaration_part() throws ScriptException { // variable_declaration_part
        //Token currentToken = currentToken();
        Runnable declarationPartFunction;
        while (true) {
            switch (currentValue()) {
                case KEYWORD_TYPE:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    while (currentValue() == VARIABLE) {
                        type_declaration(); // type_declaration
                        requireFollowingToken(SEP_SEMICOLON);
                    }
                    break;
                case KEYWORD_VAR:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    while (currentValue() == VARIABLE) {
                        variable_declaration(); // variable_declaration
                        requireFollowingToken(SEP_SEMICOLON);
                    }
                    break;
                case KEYWORD_CONST:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    while (currentValue() == VARIABLE) {
                        const_declaration(); // const_declaration
                        requireFollowingToken(SEP_SEMICOLON);
                    }
                    break;
                case KEYWORD_FUNCTION:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    function_declaration(false);
                    requireFollowingToken(SEP_SEMICOLON);
                    break;
                case KEYWORD_PROCEDURE:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    function_declaration(true);
                    requireFollowingToken(SEP_SEMICOLON);
                    break;
                default:
                    return;
            }
        }
    }


    private ArrayList<Node> identifier_list() throws SyntaxException { // identifier_list
        ArrayList<Node> newVariables = new ArrayList<>();
        while (true) {
            newVariables.add(new VarNode(currentToken()));
            goToNextToken();
            requireCurrentToken(SEP_COMMA, OP_COLON);
            if (currentValue() == OP_COLON)
                break;
            goToNextToken();
        }
        goToNextToken();
        return newVariables;
    }

    private Type parseType() throws ScriptException { // type
        /*simple_type
                | structured_type
                | type_alias;*/
        Token currentToken = currentToken();
        goToNextToken();
        Type type;
        switch (currentToken.getTokenValue()) {
            case VARIABLE:
                type = type_alias(currentToken);
                break;
            case KEYWORD_ARRAY:
                return array_type();
            case KEYWORD_RECORD:
                return record_type();
            case KEYWORD_INTEGER:
            case KEYWORD_DOUBLE:
            case KEYWORD_CHARACTER:
                type = simple_type(currentToken);
         //       goToNextToken();
                break;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s expected type but got %s",
                        currentToken.getPosX(), currentToken.getPosY(), currentToken.getText()));
        }
//        requireFollowingToken(SEP_SEMICOLON, OP_EQUAL);
        return type;
    }

    private Type type_alias(Token token) throws ScriptException {
        //goToNextToken();
        return tables.peek().getAliasType(token);
    }

    private Type simple_type(Token token) throws SyntaxException { // simple_type
        //goToNextToken();
        if (token.getTokenValue() == KEYWORD_INTEGER)
            return IntType();
        if (token.getTokenValue() == KEYWORD_DOUBLE)
            return DoubleType();
        return CharType();
    }
    // a: array [1..10] of integer = (1, 2, .. );
    private Type array_type() throws ScriptException { // array_type
        requireFollowingToken(SEP_BRACKETS_SQUARE_LEFT);
        Node min = parseExpr();
        if (min.type != integer_) // TODO make function check node Type
            throw new SyntaxException(String.format("Error in pos %s:%s expected const integer in array definition but found %s ",
                    currentToken().getPosX(), currentToken().getPosY(), min.type));
        requireFollowingToken(SEP_DOUBLE_DOT);
        Node max = parseExpr();
        requireFollowingToken(SEP_BRACKETS_SQUARE_RIGHT);
        if (max.type != integer_)
            throw new SyntaxException(String.format("Error in pos %s:%s expected const integer in array definition but found %s ",
                    currentToken().getPosX(), currentToken().getPosY(), max.type));
        requireFollowingToken(KEYWORD_OF);
        return new ArrayType(parseType(), (ConstNode)min, (ConstNode)max);
    }
    // a : record a : integer;  b : double; end;
    private Type record_type() throws ScriptException {
        SymTable fields = new SymTable();
        while (currentValue() == VARIABLE) { // { identifier_list, ":", type, ";" };
            fields.addVARSymbol(identifier_list(), parseType(), null, false); // May be not....
            requireFollowingToken(SEP_SEMICOLON);
        }
        requireFollowingToken(KEYWORD_END);
        return new RecordType(fields);
    }

    private Node typed_constant(Type type) throws ScriptException { // typed_constant
        Node typedConstant;
        switch (type.category) {
            case RECORD:
                typedConstant = new RecordNode();
                requireFollowingToken(SEP_BRACKETS_LEFT);
                RecordType recordType = (RecordType)type;
                for (Map.Entry<String, SymTable.Symbol> pair : recordType.fields.symTable.entrySet()) {
                    Token variable = currentToken();
                    requireFollowingToken(VARIABLE);
                    if (!pair.getKey().equals(variable.getText()))
                        throw new SyntaxException(recordType.fields.symTable.containsKey(variable.getText()) ?
                                String.format("Error in pos %s:%s illegal initialization order ",
                                        variable.getPosX(), variable.getPosY()) :
                                String.format("Error in pos %s:%s unknown record field identifier %s ",
                                        variable.getPosX(), variable.getPosY(), variable.getText()));
                    requireFollowingToken(OP_COLON);
                    typedConstant.children.add(typed_constant(pair.getValue().type));
                    requireFollowingToken(SEP_SEMICOLON);
                }
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return typedConstant;
            case ARRAY:
                ArrayType arrayType = (ArrayType)type;
                int min = Integer.parseInt(arrayType.min.result.toString());
                int max = Integer.parseInt(arrayType.max.result.toString());
                Type element = arrayType.elementType;
                requireFollowingToken(SEP_BRACKETS_LEFT);
                typedConstant = new ArrayNode();
                for (int i = min; i <= max; i++) {
                    typedConstant.children.add(typed_constant(element));
                    if (i != max) {
                        requireFollowingToken(SEP_COMMA);
                    }
                }
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return typedConstant;
            case INT:
            case DOUBLE:
            case CHAR:
                typedConstant = parseLogicalExpression();
                requireTypesCompatibility(type, typedConstant.type, false);
                return typedConstant;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s expected array, record, integer, char or double " +
                                "in typed constant but found %s ",
                        currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
        }
    }

    private void requireTypesCompatibility(Type left, Type right, boolean allowedLeftInt) throws SyntaxException {
        if (left == right
                || left == DoubleType() && right == IntType()
                || allowedLeftInt && left == IntType() && right == DoubleType())
            return;
        throw new SyntaxException(String.format("Error in pos %s:%s incompatible types: got %s expected integer",
                currentToken().getPosX(), currentToken().getPosY(), right.category));
    }

    private Node parseConstValue(ArrayList<Node> identifiers, Type type) throws ScriptException {
        Node value = null;
        if (currentValue() == OP_EQUAL) {
            goToNextToken();
            if (identifiers.size() > 1)
                throw new SyntaxException(String.format("Error in pos %s:%s only one variable can be initialized",
                        currentToken().getPosX(), currentToken().getPosY()));
            value = typed_constant(type);
        }
        return value;
    }

    private void variable_declaration() throws ScriptException { // type_declaration_part,  variable_declaration_part
        ArrayList<Node> identifiers = identifier_list();
        Type type = parseType();
        Node value = parseConstValue(identifiers, type);
        tables.peek().addVARSymbol(identifiers, type, value, false);
    }

    private void type_declaration() throws ScriptException { // type_declaration = identifier, "=", type, ";";
        Token identifier = currentToken();
        goToNextToken();
        requireFollowingToken(OP_EQUAL);
        Type type = parseType();
        tables.peek().addTYPESymbol(identifier, type);
    }

    private void const_declaration() throws ScriptException { // typed_constant_declaration | constant_declaration
        Token identifier = currentToken();
        goToNextToken();
        switch (currentValue()) {
            case OP_EQUAL: {
                goToNextToken();
                Node expression = parseLogicalExpression();
                if (!(expression instanceof ConstNode))
                    throw new SyntaxException(String.format("Error in pos %s:%s illegal expression ",
                            currentToken().getPosX(), currentToken().getPosY()));
                tables.peek().addCONSTSymbol(identifier, expression.type, expression);
                return;
                }
            case OP_COLON: {
                goToNextToken();
                Type type = parseType();
                requireFollowingToken(OP_EQUAL);
                Node typed_constant = typed_constant(type);
                tables.peek().addCONSTSymbol(identifier, type, typed_constant);
                return;
                }
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s expected colon or equal",
                        currentToken().getPosX(), currentToken().getPosY()));
        }
    }

    /*
    function f : Integer;
    begin
    end;
     */

    private void function_declaration(boolean isProcedure) throws ScriptException { // function_header, ";", block, ";";
        Token functionIdentifier = currentToken();
        goToNextToken();
        SymTable params = new SymTable();
        if (currentValue() == SEP_BRACKETS_LEFT) {
            goToNextToken();
            while (currentValue() != SEP_BRACKETS_RIGHT) {
                requireCurrentToken(VARIABLE, KEYWORD_VAR);
                switch (currentValue()) {
                    case VARIABLE: {
                        ArrayList<Node> identifiers = identifier_list();
                        Type type = parseType();
                        Node value = parseConstValue(identifiers, type);
                        params.addVARSymbol(identifiers, type, value, false);
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON);
                        if (currentValue() == SEP_BRACKETS_RIGHT) break;
                        goToNextToken();
                        break;
                    }
                    case KEYWORD_VAR:
                        goToNextToken();
                        requireCurrentToken(VARIABLE);
                        ArrayList<Node> identifiers = identifier_list();
                        Type type = parseType();
                        params.addVARSymbol(identifiers, type, null, true);
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON);
                        if (currentValue() == SEP_BRACKETS_RIGHT) break;
                        goToNextToken();
                        break;
                }
            }
            goToNextToken();
        }
        Type returnType = NIL();
        if (!isProcedure) {
            requireFollowingToken(OP_COLON);
            returnType = parseType(); // type_identifier
        }
        FunctionType functionType = new FunctionType(params, null, returnType, null);
        tables.peek().addVARSymbol(getList(new VarNode(functionIdentifier)), functionType, null, false);
        requireFollowingToken(SEP_SEMICOLON);
        tables.push(new SymTable());
        type_declaration_part();
        functionType.compound_statement = compound_statement();
        functionType.vars = tables.pop();
    }

    private Node parseStatements() throws ScriptException {
        Node statement = new BodyFunction();
        do {
            goToNextToken();
            statement.children.add(statement_part());
        } while (currentValue() == SEP_SEMICOLON);
        return statement;
    }

    private Node compound_statement() throws ScriptException {
        requireCurrentToken(KEYWORD_BEGIN);
        Node statement = parseStatements();
        requireFollowingToken(KEYWORD_END);
        return statement;
    }

    private Node statement_part() throws ScriptException { // compound_statement
        switch (currentValue()) { // statement_list
            case VARIABLE:
                Token currentToken = currentToken();
                goToNextToken();
                return assignment_statement(currentToken);
            case KEYWORD_IF:
                goToNextToken();
                return if_statement();
            case KEYWORD_BEGIN:
                return compound_statement();
            case KEYWORD_WHILE:
                return while_statement();
            case KEYWORD_FOR:
                return for_statement();
            case SEP_SEMICOLON:
            case KEYWORD_END:
                return null;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s unexpected token %s",
                        currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
        }
    }

    private Node while_statement() throws ScriptException {
        WhileNode whileNode = new WhileNode();
        goToNextToken();
        whileNode.setCondition(parseLogicalExpression());
        requireFollowingToken(KEYWORD_DO);
        whileNode.setBody(statement_part());
        return whileNode;
    }

    private Node for_statement() throws ScriptException {
        goToNextToken();
        requireCurrentToken(VARIABLE);
        ForNode forNode = new ForNode(tables.peek().getSymbol(currentToken()));
        goToNextToken();
        requireFollowingToken(KEYWORD_ASSIGN);
        forNode.setFrom(parseLogicalExpression());
        if (currentToken().getTokenValue() == KEYWORD_DOWNTO)
            forNode.isDownTo = true;
        requireFollowingToken(KEYWORD_TO, KEYWORD_DOWNTO);
        forNode.setTo(parseLogicalExpression());
        requireFollowingToken(KEYWORD_DO);
        forNode.setBody(statement_part());
        return null;
    }

    private Node if_statement() throws ScriptException {
        IfNode ifNode = new IfNode();
        ifNode.setIf_(parseLogicalExpression());
        requireFollowingToken(KEYWORD_THEN);
        ifNode.setThen_(statement_part());
        if (currentValue() == KEYWORD_ELSE) {
            goToNextToken();
            ifNode.setElse_(statement_part());
        }
        return ifNode;
    }

    private Node assignment_statement(Token identifier) throws ScriptException {
        Type identifierType = tables.peek().getAliasType(identifier);
        requireFollowingToken(KEYWORD_ASSIGN);
        Node typedConstant = parseLogicalExpression();
        requireTypesCompatibility(identifierType, typedConstant.type, false);
        return new AssignmentStatement(typedConstant, identifier);
    }

    private void simple_statement() {
        /*assignment_statement
                | procedure_statement
                | read
                | write
                | "continue"
                | "break"
                | exit;*/
        switch (currentValue()) {
            case VARIABLE:

        }
    }

    private void goToNextToken() { tokenizer.Next(); }

    private Token currentToken() { return tokenizer.getCurrentToken(); }

    private TokenValue currentValue() { return tokenizer.getCurrentToken().getTokenValue(); }

    // Type......

    private enum Category {
        INT, DOUBLE, CHAR, ARRAY, RECORD, NIL, FUNCTION
    }

    public class Type {
        public Category category;

        public Type(Category category) {
            this.category = category;
        }

        public boolean isScalar() {
            return category == Category.INT || category == Category.DOUBLE || category == Category.CHAR;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    private Type double_;

    private Type DoubleType() {
        if (double_ == null)
            double_ = new Type(Category.DOUBLE);
        return double_;
    }

    private Type integer_;

    private Type IntType() {
        if (integer_ == null)
            integer_ = new Type(Category.INT);
        return integer_;
    }

    private Type char_;

    private Type CharType() {
        if (char_ == null)
            char_ = new Type(Category.CHAR);
        return char_;
    }

    private Type nil_;

    private Type NIL() {
        if (nil_ == null)
            nil_ = new Type(Category.NIL);
        return nil_;
    }


    private class ArrayType extends Type {
        public Type elementType;
        public ConstNode min;
        public ConstNode max;

        public ArrayType(Type elementType, ConstNode min, ConstNode max) {
            super(Category.ARRAY);
            this.elementType = elementType;
            this.min = min;
            this.max = max;
        }
    }

    private class RecordType extends Type {
        public SymTable fields;

        public RecordType(SymTable fields) {
            super(Category.RECORD);
            this.fields = fields;
        }
    }

    private class FunctionType extends Type {
        public Type returnType;
        public SymTable params;
        public SymTable vars;
        public Node compound_statement;

        public FunctionType(SymTable params, SymTable vars, Type returnType, Node compound_statement) {
            super(Category.FUNCTION);
            this.params = params;
            this.vars = vars;
            this.returnType = returnType;
            this.compound_statement = compound_statement;
        }

        @Override
        public String toString() {
            return super.toString();
        }
    }

    // Sym Table......

    private class SymTable {
        private LinkedHashMap<String, Symbol> symTable;

        private SymTable() {
            symTable = new LinkedHashMap<>();
        }

        private void checkDuplicated(Token token) throws SyntaxException {
            if (symTable.containsKey(token.getText()))
                throw new SyntaxException(String.format("Error in pos %s:%s duplicate identifier %s ",
                        token.getPosX(), token.getPosY(), token.getText()));
        }

        // Var
        private void addVARSymbol(ArrayList<Node> symbols, Type type, Node value, boolean isPointerParam) throws SyntaxException {
            for (Node symbol : symbols) {
                checkDuplicated(symbol.getToken());
                symTable.put(symbol.token.getText(), new Symbol(type, value, false, isPointerParam));
            }
        }
        // Const
        private void addCONSTSymbol(Token symbol, Type type, Node value) throws SyntaxException {
            checkDuplicated(symbol);
            symTable.put(symbol.getText(), new Symbol(type, value, true, false));
        }
        // Type
        private void addTYPESymbol(Token symbol, Type type) throws SyntaxException {
            checkDuplicated(symbol);
            symTable.put(symbol.getText(), new Symbol(type));
        }
        // Alias
        private Type getAliasType(Token token) throws SyntaxException {
            if (symTable.containsKey(token.getText()))
                return symTable.get(token.getText()).type;
            throw new SyntaxException(String.format("Error in pos %s:%s identifier not found %s ",
                    token.getPosX(), token.getPosY(), token.getText()));
        }
        // isConst
        private boolean isConst(Token token) {
            return symTable.get(token.getText()).isConst;
        }
        // isType
        private boolean isType(Token token) {
            return symTable.get(token.getText()).isType;
        }

        private Symbol getSymbol(Token token) throws SyntaxException {
            for (SymTable table : tables)
                if (table.symTable.containsKey(token.getText()))
                    return table.symTable.get(token.getText());
            throw new SyntaxException(String.format("Error in pos %s:%s identifier not found %s ",
                    token.getPosX(), token.getPosY(), token.getText()));
        }

        private class Symbol {
            Type type;
            Node value;
            boolean isType = false;
            boolean isConst = false;
            boolean isPointerParam = false;

            private Symbol(Type type, Node value, boolean isConst, boolean isPointerParam) {
                this.type = type;
                this.value = value;
                this.isConst = isConst;
                this.isPointerParam = isPointerParam;
            }

            private Symbol(Type type) {
                this.type = type;
                this.isType = true;
            }

        }

    }

    // Node......

    private enum NodeType {
        CONST_INT, CONST_DOUBLE; // .... later

        @Override
        public String toString() {
            return this == CONST_DOUBLE ? "const double" : "const integer";
        }
    }

    private enum ResultType {
        CONST; // .... .. .. .. later
    }

    public class Node {
        ArrayList<Node> children;
        Token token;
//        ResultType resultType;
//        NodeType type;
        Type type;


        private Node(ArrayList<Node> children, Token token) { this.children = children; this.token = token; }

        private Node() {}

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

        //public TokenValue getResultType() { return resultType; }

        //public void setResultType(TokenValue resultType) { this.resultType = resultType; }
    }

    public class VarNode extends Node {
        public VarNode(Token token) { super(null, token); }
        public VarNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ConstNode extends Node {
        private Object result;
        public ConstNode(Type type, Object result) {
            super(null, null);
            this.type = type;
            this.result = result;
            //this.resultType = ResultType.CONST;
        }
        public ConstNode(Token token, Type type, Object result) {
            super(null, token);
            this.result = result;
            this.type = type;
        }
    }

    public class BinOpNode extends Node {
        public BinOpNode(ArrayList<Node> children, Token token, Type type) {
            super(children, token);
            this.type = type;
        }
    }

    public class UnaryMinusNode extends Node {
        public UnaryMinusNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class NotNode extends Node {
        public NotNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class MainNode extends Node {
        public MainNode(ArrayList<Node> children, Token token) { super(children, token); }
    }

    public class ArrayNode extends Node {
        public ArrayNode() {
            super(new ArrayList<>(), null);
        }
    }

    public class RecordNode extends Node {
        public RecordNode() {
            super(new ArrayList<>(), null);
        }
    }

    public class BodyFunction extends Node {
        public BodyFunction() { super(new ArrayList<>(), null); }
    }

    public class AssignmentStatement extends Node {
        public AssignmentStatement(Node expression, Token token) { super(getList(expression), token);}
    }

    public class IfNode extends Node {
        public IfNode() { super(getTmpLists(3), null); }

        public Node getIf_() { return this.children.get(0); }

        public Node getThen_() { return this.children.get(1); }

        public Node getElse_() { return this.children.get(2); }

        public void setIf_(Node if_) { this.children.set(0, if_); }

        public void setThen_(Node then_) { this.children.set(1, then_); }

        public void setElse_(Node else_) { this.children.set(2, else_); }
    }

    public class WhileNode extends Node {
        public WhileNode() { super(getTmpLists(2), null); }

        public Node getCondition() { return this.children.get(0); }

        public Node getBody() { return this.children.get(1); }

        public void setCondition(Node condition) { this.children.set(0, condition); }

        public void setBody(Node body) { this.children.set(1, body); }
    }

    public class ForNode extends Node {
        public SymTable.Symbol counter;
        public boolean isDownTo = false;

        public ForNode(SymTable.Symbol counter) { super(getTmpLists(3), null); this.counter = counter; }

        public Node getFrom() { return this.children.get(0); }

        public Node getTo() { return this.children.get(1); }

        public Node getBody() { return this.children.get(2); }

        public void setFrom(Node from) { this.children.set(0, from); }

        public void setTo(Node to) { this.children.set(1, to); }

        public void setBody(Node body) { this.children.set(2, body); }
    }

    private ArrayList<Node> getTmpLists(int size) {
        ArrayList<Node> tmp = new ArrayList<>();
        for (int i = 0; i < size; i++)
            tmp.add(null);
        return tmp;
    }
}
