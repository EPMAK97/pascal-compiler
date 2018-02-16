package SyntacticalAnalyzer;

import Generator.CodeAsm;
import Generator.CommandAsm;
import Generator.DataType;
import Generator.RegisterType;
import Tokens.*;

import javax.script.ScriptEngine;
import javax.script.ScriptEngineManager;
import javax.script.ScriptException;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static Tokens.TokenValue.*;

public class Parser {
    private Tokenizer tokenizer;
    private static HashMap<TokenValue, String> hashTokens;
    private Stack<SymTable> tables;
    private static int loopCount = 0;
    private static int resultCount = 0; // Result variable in function
    private static StringBuilder spaces; // For beautiful table output
    private static boolean exitFound = false; // Exit in function

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
        // there's not in the hash
        hashTokens.put(VARIABLE, "identifier");
        hashTokens.put(CONST_STRING, "const string");
        hashTokens.put(SEP_DOUBLE_DOT, "..");
        hashTokens.put(KEYWORD_ASSIGN, ":=");
        spaces = new StringBuilder("");
    }

    public Parser(String filePath) throws SyntaxException {
        tokenizer = new Tokenizer(filePath);
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

    private Node parseExpr() throws SyntaxException, ScriptException {
        Node e = parseTerm();
        Token t = currentToken(); // +
        while (t.getTokenValue() != KEYWORD_EOF && isExpr(t.getTokenValue())) {
            goToNextToken();
            Node parseTermNode = parseTerm();
            e = calculateConstants(e, parseTermNode, t);
            t = currentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException, ScriptException {
        Node e = parseFactor();
        Token t = currentToken();
        while (t.getTokenValue() != KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
            goToNextToken();
            Node parseFactorNode = parseFactor();
            e = calculateConstants(e, parseFactorNode, t);
            t = currentToken();
        }
        return e;
    }
    // a-;
    private Node parseFactor() throws SyntaxException, ScriptException {
        Token currentToken = currentToken();
        goToNextToken();
        switch (currentToken.getTokenValue()) {
            case OP_MINUS:
                return new UnaryMinusNode(getList(parseFactor()), currentToken);
            case KEYWORD_NOT:
                return new NotNode(getList(parseFactor()), currentToken);
            case VARIABLE: {
                switch (getTypeFromTable(currentToken).category) {
                    case ARRAY:
                        return indexed_variable(new VarNode(currentToken, getTypeFromTable(currentToken)));
                    case RECORD:
                        return field_access(new VarNode(currentToken, getTypeFromTable(currentToken)));
                    case FUNCTION:
                        return parseFunctionCall(new VarNode(currentToken, ((FunctionType)getTypeFromTable(currentToken)).returnType));
                    default:
                        return new VarNode(currentToken, getTypeFromTable(currentToken));
                }
            }
            case KEYWORD_INTEGER:
            case KEYWORD_DOUBLE:
            case KEYWORD_CHARACTER:
                return castVariables(currentToken);
            case CONST_INTEGER:
                return new ConstNode(currentToken, IntType(), currentToken.getValue());
            case CONST_DOUBLE:
                return new ConstNode(currentToken, DoubleType(), currentToken.getValue());
            case CONST_STRING:
                return new ConstNode(currentToken, CharType(), currentToken.getText());
            case SEP_BRACKETS_LEFT:
                Node e = parseLogicalExpression();
                requireFollowingToken(SEP_BRACKETS_RIGHT);
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
            throw new SyntaxException(String.format("Error in pos %s:%s unsupported operands types \"%s\", \"%s\" for %s",
                    currentToken().getPosX(), currentToken().getPosY(), left.category, right.category, operation.getText()));
        if (isLogical(operation.getTokenValue()))
            return IntType();
        if (left == CharType() || right == CharType())
            throw new SyntaxException(String.format("Error in pos %s:%s unsupported operands types \"%s\", \"%s\" for %s",
                    currentToken().getPosX(), currentToken().getPosY(), left.category, right.category, operation.getText()));
        if (isIntOnly(operation.getTokenValue()))
            if (left != IntType() || right != IntType())
                throw new SyntaxException(String.format("Error in pos %s:%s unsupported operands types \"%s\", \"%s\" for %s",
                        currentToken().getPosX(), currentToken().getPosY(), left.category, right.category, operation.getText()));
        Type resultType = left;
        if (left == IntType() && right == DoubleType() || left == DoubleType() && right == IntType())
            resultType = DoubleType();
        else if (left != right)
            throw new SyntaxException(String.format("Error in pos %s:%s incompatible types",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (operation.getTokenValue() == OP_DIVISION)
            return DoubleType();
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
            default: return null; // never
        }
    }

    private Node calculateConstants(Node left, Node right, Token operation) throws ScriptException {
        Type leftType = left.type;
        Type rightType = right.type;
        Type operationType = getTypeForOperation(leftType, rightType, operation);
        ConstNode l = null;
        ConstNode r = null;
        if (left instanceof ConstNode)
            l = (ConstNode)left;
        if (right instanceof ConstNode)
            r = (ConstNode)right;
        if (l == null || r == null)
            return new BinOpNode(getList(left, right), operation, operationType);
        String leftOperand = l.token == null ? l.result.toString() : left.token.getText();
        String expressionToString = leftOperand + operation.getText() + right.token.getText();
        if (isCastOnly(operation))
            if (isLogical(operation.getTokenValue()))
                return new ConstNode(operationType, getResultLogicOperation(left, right, operation));
            else
                expressionToString = leftOperand + getJavaStyleOperation(operation) + right.token.getText();
        if (operation.getTokenValue() == KEYWORD_DIV) expressionToString += "|0"; // :)
        ScriptEngineManager manager = new ScriptEngineManager();
        ScriptEngine engine = manager.getEngineByName("js");
        Object result = engine.eval(expressionToString);
        return new ConstNode(operationType, result);
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

    private Node parseFunctionCall(Node var) throws ScriptException {
        FunctionType type = (FunctionType)getTypeFromTable(var.token);
        if (type.params.symTable.size() == 0)
            return var;
        //goToNextToken();
        var.children.add(parseParameterList(type));
        switch (type.returnType.category) {
            case ARRAY: {
                var.type = type.returnType;
                Node indexedVariable = indexed_variable(var);
                var.type = type; // TODO может это не надо делать? этот SWAP
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return indexedVariable;
            }
            case RECORD:
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                var.type = type.returnType;
                if (currentValue() != SEP_DOT)
                    return var;
                Node fieldAccess = field_access(var);
                var.type = type; // TODO может это не надо делать? этот SWAP
                goToNextToken();
                return fieldAccess;
            default:
                //goToNextToken();
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return var;
        }
    }

    private Node parseParameterList(FunctionType type) throws ScriptException {
        requireFollowingToken(SEP_BRACKETS_LEFT);
        Node result = new ParamListNode(type.returnType);
        ArrayList<Node> arguments = new ArrayList<>();
        parseExpressionList(arguments);
        int i = 0;
        for (Map.Entry<String, SymTable.Symbol> entry : type.params.symTable.entrySet()) {
            if (i == type.params.symTable.size() - 1 && i == arguments.size())
                break;
            if (i == type.params.symTable.size() || i == arguments.size())
                throw new SyntaxException(String.format("Error in pos %s:%s illegal parameters count",
                        currentToken().getPosX(), currentToken().getPosY()));
            requireTypesCompatibility(entry.getValue().type, arguments.get(i).type, false);
            result.children.add(arguments.get(i));
            i++;
        }
        return result;
    }

    private void parseExpressionList(ArrayList<Node> nodes) throws ScriptException {
        while (currentToken().getTokenValue() != SEP_BRACKETS_RIGHT) {
            nodes.add(parseLogicalExpression());
            if (currentToken().getTokenValue() == SEP_BRACKETS_RIGHT || currentValue() == SEP_SEMICOLON)
                break;
            requireFollowingToken(SEP_COMMA);
        }
    }

    private Node castVariables(Token token) throws ScriptException {
        requireFollowingToken(SEP_BRACKETS_LEFT);
        Node expr = parseExpr();
        Type castType = null;
        switch (token.getTokenValue()) {
            case KEYWORD_INTEGER:
                castType = IntType();
                break;
            case KEYWORD_DOUBLE:
                castType = DoubleType();
                break;
            case KEYWORD_CHARACTER:
                castType = CharType();
                break;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s can't convert from %s to %s",
                        token.getPosX(), token.getPosY(), expr.type.category, token.getText()));
        }
        if (castType.category == expr.type.category)
            throw new SyntaxException(String.format("Error in pos %s:%s can't convert from %s to %s",
                    token.getPosX(), token.getPosY(), expr.type.category, castType.category));
        requireFollowingToken(SEP_BRACKETS_RIGHT);
        return new CastNode(expr, expr.type, castType, token);
    }

    public Type parse() throws ScriptException {
        goToNextToken();
        String name = "MAIN";
        if (currentValue() == KEYWORD_PROGRAM)
            name = parseProgram();
        tables = new Stack<>();
        tables.push(new SymTable());
        declaration_part();
        return new FunctionType(null, tables.peek(), NIL(), compound_statement(), name);
    }

    private String parseProgram() throws SyntaxException {
        goToNextToken();
        require(VARIABLE);
        String result = currentToken().getText();
        goToNextToken();
        require(SEP_SEMICOLON);
        goToNextToken();
        return result;
    }


    private void declaration_part() throws ScriptException { // declaration_part
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
                    function_declaration(false); // function_declaration
                    requireFollowingToken(SEP_SEMICOLON);
                    break;
                case KEYWORD_PROCEDURE:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    function_declaration(true); // procedure_declaration
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
        return getTypeFromTable(token);
    }

    private Type simple_type(Token token) throws SyntaxException { // simple_type
        //goToNextToken();
        if (token.getTokenValue() == KEYWORD_INTEGER)
            return IntType();
        if (token.getTokenValue() == KEYWORD_DOUBLE)
            return DoubleType();
        return CharType();
    }

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
                        throw new SyntaxException(
                            recordType.fields.symTable.containsKey(variable.getText()) ?
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
        if (right.category == Category.FUNCTION) {
            right = ((FunctionType) right).returnType;
        }
        if (left == right
                || left == DoubleType() && right == IntType()
                || allowedLeftInt && left == IntType() && right == DoubleType())
            return;
        throw new SyntaxException(String.format("Error in pos %s:%s incompatible types: got %s expected %s",
                currentToken().getPosX(), currentToken().getPosY(), right.category, left.category));
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

    private void function_declaration(boolean isProcedure) throws ScriptException { // function_header, ";", block, ";";
        exitFound = false;
        Token functionIdentifier = currentToken();
        goToNextToken();
        //SymTable params = new SymTable();
        tables.push(new SymTable()); // params
        if (currentValue() == SEP_BRACKETS_LEFT) {
            goToNextToken();
            while (currentValue() != SEP_BRACKETS_RIGHT) {
                requireCurrentToken(VARIABLE, KEYWORD_VAR, KEYWORD_CONST);
                switch (currentValue()) {
                    case VARIABLE: {
                        ArrayList<Node> identifiers = identifier_list();
                        Type type = parseType();
                        Node value = parseConstValue(identifiers, type);
                        tables.peek().addVARSymbol(identifiers, type, value, false);
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON);
                        if (currentValue() == SEP_BRACKETS_RIGHT) break;
                        goToNextToken();
                        break;
                    }
                    case KEYWORD_VAR: {
                        goToNextToken();
                        requireCurrentToken(VARIABLE);
                        ArrayList<Node> identifiers = identifier_list();
                        Type type = parseType();
                        tables.peek().addVARSymbol(identifiers, type, null, true);
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON, OP_EQUAL);
                        if (currentValue() == OP_EQUAL)
                            throw new SyntaxException(String.format(
                                "Error in pos %s:%s can't initialize value passed by reference",
                                    currentToken().getPosX(), currentToken().getPosY()));
                        if (currentValue() == SEP_BRACKETS_RIGHT) break;
                        goToNextToken();
                        break;
                    }
                    case KEYWORD_CONST: {
                        goToNextToken();
                        requireCurrentToken(VARIABLE);
                        ArrayList<Node> identifiers = identifier_list();
                        Type type = parseType();
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON, OP_EQUAL);
                        Node typed_constant = null;
                        if (currentValue() == OP_EQUAL) {
                            goToNextToken();
                            typed_constant = typed_constant(type);
                        }
                        tables.peek().addCONSTSymbol(identifiers, type, typed_constant);
                        if (currentValue() == SEP_BRACKETS_RIGHT) break;
                        goToNextToken();
                        break;
                    }
                }
            }
            goToNextToken();
        }
        Type returnType = NIL();
        if (!isProcedure) {
            requireFollowingToken(OP_COLON);
            returnType = parseType(); // type_identifier
        }
        FunctionType functionType = new FunctionType(tables.peek(), null, returnType,
                null, functionIdentifier.getText());
        requireFollowingToken(SEP_SEMICOLON);
        //tables.peek().addVARSymbol(functionIdentifier, functionType);
        //tables.peek().addVARSymbol(getList(new VarNode(functionIdentifier)), functionType, null, false);
        functionType.params = tables.peek();

        // Declaration
        tables.push(new SymTable());
        Token result = new Token("result", new Pair(TokenType.IDENTIFIER, VARIABLE)); // Magic identifier result
        tables.peek().addVARSymbol(getList(new VarNode(result)), returnType, null, false);
        declaration_part();

        // Compound_statement
        functionType.compound_statement = compound_statement();
        functionType.vars = tables.peek();
        tables.pop();
        tables.pop();
        tables.peek().addVARSymbol(functionIdentifier, functionType);
        if (resultCount == 0 && functionType.returnType != NIL() && !exitFound)
            throw new SyntaxException(String.format("Error in pos %s:%s RESULT identifier in function not found",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (resultCount > 0) resultCount--;
        //tables.peek().addVARSymbol(functionIdentifier, functionType);
    }

    private Node parseStatements() throws ScriptException {
        Node statement = new BodyFunction();
        do {
            goToNextToken();
            Node node = statement_part();
            if (node != null)
                statement.children.add(node);
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
                if (currentToken().getText().toLowerCase().equals("result"))
                    resultCount++;
                Token currentToken = currentToken();
                if (getSymbolFromTable(currentToken).isConst)
                    throw new SyntaxException(String.format("Error in pos %s:%s can't modify const variable",
                            currentToken.getPosX(), currentToken.getPosY()));
                Type type = getTypeFromTable(currentToken);
                if (type.category == Category.FUNCTION) {
                    goToNextToken();
                    return parseFunctionCall(new VarNode(currentToken, getTypeFromTable(currentToken)));
                }
                return assignment_statement();
            case KEYWORD_IF:
                goToNextToken();
                return if_statement();
            case KEYWORD_BEGIN:
                return compound_statement();
            case KEYWORD_WHILE:
                return while_statement();
            case KEYWORD_FOR:
                return for_statement();
            case KEYWORD_WRITE:
                return write(false);
            case KEYWORD_READ:
                return write(true);
            case KEYWORD_CONTINUE:
                return parse_continue(currentToken());
            case KEYWORD_BREAK:
                return parse_break(currentToken());
            case KEYWORD_EXIT:
                return parse_exit(currentToken());
            case SEP_SEMICOLON:
            case KEYWORD_END:
                return null;
            default:
                throw new SyntaxException(String.format("Error in pos %s:%s unexpected token %s",
                        currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
        }
    }

    private Node parse_exit(Token token) throws ScriptException {
        exitFound = true;
        Node exitNode = new ExitNode(token);
        goToNextToken();
        if (currentValue() == SEP_BRACKETS_LEFT) {
            Node expr = parseLogicalExpression();
            requireTypesCompatibility(tables.peek().symTable.get("result").type, expr.type, false);
            exitNode.children.add(expr);
        }
        return exitNode;
    }

    private Node parse_continue(Token token) throws SyntaxException {
        if (loopCount == 0)
            throw new SyntaxException(String.format("Error in pos %s:%s continue not allowed",
                    currentToken().getPosX(), currentToken().getPosY()));
        goToNextToken();
        return new ContinueNode(token);
    }

    private Node parse_break(Token token) throws SyntaxException {
        if (loopCount == 0)
            throw new SyntaxException(String.format("Error in pos %s:%s break not allowed",
                    currentToken().getPosX(), currentToken().getPosY()));
        goToNextToken();
        return new BreakNode(token);
    }

    private Node write(boolean isRead) throws ScriptException { // read
        Token writeToken = currentToken();
        goToNextToken();
        requireCurrentToken(SEP_BRACKETS_LEFT);
        ArrayList<Node> expressionList = new ArrayList<>();
        do {
            goToNextToken();
            Token currentVariable = currentToken();
            expressionList.add(parseLogicalExpression());
            if (isRead && getSymbolFromTable(currentVariable).isConst)
                throw new SyntaxException(String.format("Error in pos %s:%s can't read to const variable",
                        currentVariable.getPosX(), currentVariable.getPosY()));
        } while ((currentValue() == SEP_COMMA));
        requireFollowingToken(SEP_BRACKETS_RIGHT);
        return new WriteNode(expressionList, writeToken);
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
        loopCount++;
        Token forToken = currentToken();
        goToNextToken();
        requireCurrentToken(VARIABLE);
        Node start = new VarNode(currentToken(), getTypeFromTable(currentToken()));
        requireTypesCompatibility(IntType(), start.type, false);
        ForNode forNode = new ForNode(tables.peek().getSymbol(currentToken()), forToken);
        goToNextToken();
        requireFollowingToken(KEYWORD_ASSIGN);
        Node from = parseLogicalExpression();
        requireTypesCompatibility(IntType(), from.type, false);
        forNode.setFrom(from);
        if (currentToken().getTokenValue() == KEYWORD_DOWNTO)
            forNode.isDownTo = true;
        Token toName = currentToken();
        requireFollowingToken(KEYWORD_TO, KEYWORD_DOWNTO);
        Node to = parseLogicalExpression();
        requireTypesCompatibility(IntType(), to.type, false);
        forNode.setTo(to);
        requireFollowingToken(KEYWORD_DO);
        forNode.setBody(statement_part());
        forNode.setFromName(start);
        forNode.setToName(toName);
        loopCount--;
        return forNode;
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

    private Node assignment_statement() throws ScriptException {
        Node identifier = null;
        Token currenToken = currentToken();
        Type type = getTypeFromTable(currentToken());
        switch (type.category) {
            case ARRAY:
                goToNextToken();
                identifier = indexed_variable(new VarNode(currenToken, type));
                break;
            case RECORD:
                goToNextToken();
                identifier = field_access(new VarNode(currenToken, type));
                break;
            default:
                identifier = new VarNode(currenToken, type);
                goToNextToken();
                break;
        }
        if (currentValue() != KEYWORD_ASSIGN)
            return identifier;
        Type identifierType = identifier.type;
        goToNextToken();
        Node expression = parseLogicalExpression();
        isTypes(currenToken, expression);
        requireTypesCompatibility(identifierType, expression.type, false);
        return new AssignmentStatement(identifier, expression);
    }

    private void isTypes(Token token, Node node) throws SyntaxException {
        if (getSymbolFromTable(token).isType) {
            if (node.token.getTokenValue() == VARIABLE)
                if (getSymbolFromTable(node.token).isType)
                    return;
            throw new SyntaxException(String.format("Error in pos %s:%s incompatible types: expected type %s",
                currentToken().getPosX(), currentToken().getPosY(), getSymbolFromTable(token).type.category));
        }
    }

    private Node indexed_variable(Node index) throws ScriptException {
        Type currType = index.type;
        while (currentValue() == SEP_BRACKETS_SQUARE_LEFT) {
            if (currType.category != Category.ARRAY)
                throw new SyntaxException(String.format("Error in pos %s:%s wrong size of array",
                        currentToken().getPosX(), currentToken().getPosY()));
            goToNextToken();
            ArrayType a = (ArrayType)currType;
            Node expr = parseLogicalExpression();
            if (expr.type.category != Category.INT)
                throw new SyntaxException(String.format("Error in pos %s:%s require INT type in index",
                        currentToken().getPosX(), currentToken().getPosY()));
            index = new IndexNode(getList(index, expr), a.elementType);
            requireFollowingToken(SEP_BRACKETS_SQUARE_RIGHT);
            currType = a.elementType;
        }
        //if (currType.category == Category.ARRAY)
          //  throw new SyntaxException(String.format("Error in pos %s:%s wrong size of array",
            //        currentToken().getPosX(), currentToken().getPosY()));
        if (currType.category == Category.RECORD)
            return field_access(index);
        return index;
    }

    private Node field_access(Node node) throws ScriptException {
        Type type = node.type;
        while (currentValue() == SEP_DOT) {
            if (type.category != Category.RECORD)
                throw new SyntaxException(String.format("Error in pos %s:%s invalid record access",
                        currentToken().getPosX(), currentToken().getPosY()));
            goToNextToken();
            requireCurrentToken(VARIABLE);
            RecordType recordType = (RecordType)type;
            Iterator<Map.Entry<String, SymTable.Symbol>> it = recordType.fields.symTable.entrySet().iterator();
            SymTable.Symbol symbol = recordType.fields.symTable.get(currentToken().getText().toLowerCase());
            if (symbol == null)
                throw new SyntaxException(String.format("Error in pos %s:%s field not found %s",
                        currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
            Node varNode = new VarNode(currentToken(), symbol.type);
            node = new FieldAccessNode(getList(node, varNode), symbol.type);
            type = symbol.type;
            goToNextToken();
        }
        if (type.category == Category.ARRAY)
            return indexed_variable(node);
        return node;
    }

    // TODO сделать одну функцию на эти двк операции ниже
    private Type getTypeFromTable(Token identifier) throws SyntaxException {
        for (int i = tables.size() - 1; i >= 0; i--)
            if (tables.get(i).getAliasTypeFunction(identifier))
                return tables.get(i).getAliasType(identifier);
        throw new SyntaxException(String.format("Error in pos %s:%s identifier not found %s ",
                identifier.getPosX(), identifier.getPosY(), identifier.getText()));
    }

    private SymTable.Symbol getSymbolFromTable(Token identifier) throws SyntaxException {
        for (int i = tables.size() - 1; i >= 0; i--)
            if (tables.get(i).getAliasTypeFunction(identifier))
                return tables.get(i).symTable.get(identifier.getText().toLowerCase());
            throw new SyntaxException(String.format("Error in pos %s:%s identifier not found %s ",
                    identifier.getPosX(), identifier.getPosY(), identifier.getText()));
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

        public String toString() {
            return category.toString();
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

        public String toString() {
            String result = "";
            Type currType = this;
            while (currType.category == Category.ARRAY) {
                ArrayType tmp = (ArrayType) currType;
                result += "array [" + tmp.min + ", " + tmp.max + "]" + " of ";
                currType = tmp.elementType;
                if (currType.category != Category.ARRAY)
                    result += currType.toString();
            }
            return result;
        }
    }

    private class RecordType extends Type {
        public SymTable fields;

        public RecordType(SymTable fields) {
            super(Category.RECORD);
            this.fields = fields;
        }

        public String toString() {
            String result = category + "\n";
            for (Map.Entry<String, SymTable.Symbol> entry: fields.symTable.entrySet())
                result += spaces.toString() + spaces + entry.getKey() + " : " + entry.getValue().type.toString()+ "\n";
            result += spaces + "END";
            return result;
        }

    }

    private class FunctionType extends Type {
        public Type returnType;
        public SymTable params;
        public SymTable vars;
        public Node compound_statement;
        public String name;

        public FunctionType(SymTable params, SymTable vars, Type returnType, Node compound_statement, String name) {
            super(Category.FUNCTION);
            this.params = params;
            this.vars = vars;
            this.returnType = returnType;
            this.compound_statement = compound_statement;
            this.name = name;
        }

        public String toString() {
            String result = "";
            result += spaces.toString() + category + " : " + name + "\n";
            result += spaces.append("    ") + "result : " + returnType.category + "\n";
            result += getParamsVars(true);
            result += getParamsVars(false);
            result += getStringFromSOUT(compound_statement);
            spaces.delete(spaces.lastIndexOf("    "), spaces.lastIndexOf("    ") + 4);
            return result;
        }

        private String getParamsVars(boolean isParams) {
            String result = spaces + "function " + (isParams ? "params {\n" : "vars {\n");
            SymTable currTable = isParams ? params : vars;
            if (currTable == null) return "";
            for (Map.Entry<String, SymTable.Symbol> entry : currTable.symTable.entrySet()) {
                if (entry.getKey().equals(name.toLowerCase()) || entry.getKey().equals("result"))
                    continue; // for magic tokens
                if (entry.getValue().type.category != Category.FUNCTION) { // look at function above
                    result += spaces.toString() + entry.getKey() + " : ";
                    result += entry.getValue().isType ? "type " : "";
                    result += entry.getValue().isConst ? "const " : "";
                }
                result += entry.getValue().type.toString() + "\n";
                result += getStringFromSOUT(entry.getValue().value);
            }
            result += spaces + "}";
            if (isParams) result += "\n";
            return result;
        }
    }

    private String getStringFromSOUT(Node node) {
        StringBuilder builder = new StringBuilder();
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintStream ps = new PrintStream(baos);
        PrintStream old = System.out;
        System.setOut(ps);
        if (node != null) {
            System.out.println();
            node.print(0);
        }
        if (baos.size() > 0)
            builder.append(baos.toString());
        System.out.flush();
        System.setOut(old);
        return builder.toString();
    }

    private String genSpace(int size) {
        return new String(new char[Math.abs(5 - size)]).replace('\0', ' ');
    }

    // Sym Table......

    private class SymTable {
        private LinkedHashMap<String, Symbol> symTable;

        private SymTable() {
            symTable = new LinkedHashMap<>();
        }

        private void checkDuplicated(Token token) throws SyntaxException {
            if (symTable.containsKey(token.getText().toLowerCase()))
                throw new SyntaxException(String.format("Error in pos %s:%s duplicate identifier %s ",
                        token.getPosX(), token.getPosY(), token.getText()));
        }

        // Var
        private void addVARSymbol(ArrayList<Node> symbols, Type type, Node value, boolean isPointerParam) throws SyntaxException {
            for (Node symbol : symbols) {
                checkDuplicated(symbol.getToken());
                symTable.put(symbol.token.getText().toLowerCase(), new Symbol(type, value, false, isPointerParam));
            }
        }

        private void addVARSymbol(Token symbol, Type type) throws SyntaxException {
            checkDuplicated(symbol);
            symTable.put(symbol.getText().toLowerCase(), new Symbol(type, null, false, false));
        }
        // Const
        private void addCONSTSymbol(Token symbol, Type type, Node value) throws SyntaxException {
            checkDuplicated(symbol);
            symTable.put(symbol.getText().toLowerCase(), new Symbol(type, value, true, false));
        }
        private void addCONSTSymbol(ArrayList<Node> symbols, Type type, Node value) throws SyntaxException {
            for (Node symbol : symbols) {
                checkDuplicated(symbol.getToken());
                symTable.put(symbol.token.getText().toLowerCase(), new Symbol(type, value, true, false));
            }
        }
        // Type
        private void addTYPESymbol(Token symbol, Type type) throws SyntaxException {
            checkDuplicated(symbol);
            symTable.put(symbol.getText().toLowerCase(), new Symbol(type));
        }
        // Alias
        private Type getAliasType(Token token) throws SyntaxException {
            if (symTable.containsKey(token.getText().toLowerCase()))
                return symTable.get(token.getText().toLowerCase()).type;
            throw new SyntaxException(String.format("Error in pos %s:%s identifier not found %s ",
                    token.getPosX(), token.getPosY(), token.getText()));
        }
        // Alias
        private boolean getAliasTypeFunction(Token token) {
            return symTable.containsKey(token.getText().toLowerCase());
        }
        // isConst
        private boolean isConst(Token token) {
            return symTable.get(token.getText().toLowerCase()).isConst;
        }
        // isType
        private boolean isType(Token token) {
            return symTable.get(token.getText().toLowerCase()).isType;
        }

        private Symbol getSymbol(Token token) throws SyntaxException {
            for (SymTable table : tables)
                if (table.symTable.containsKey(token.getText().toLowerCase()))
                    return table.symTable.get(token.getText().toLowerCase());
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

    public class Node {
        ArrayList<Node> children;
        Token token;
        Type type;

        private Node(ArrayList<Node> children, Token token) { this.children = children; this.token = token; }

        @Override
        public String toString() {
            return token.getText();
        }

        public void print(int lengthPrefix) { print(spaces.toString(), true); }

        private void print(String prefix, boolean isTail) {
            System.out.println(prefix + (isTail ? "└── " : "├── ") + token.getText());
            if (children != null) {
                String spaces = new String(new char[token.getText().length() - 1]).replace('\0', ' ');
                String newPrefix = prefix + (isTail ? "    " : "|   ") + spaces;
                if (children.size() == 0) return;
                for (int i = 0; i < children.size() - 1; i++) {
                    children.get(i).print(newPrefix, false);
                }
                children.get(children.size() - 1).print(newPrefix, true);
            }
        }

        public void setChildren(ArrayList<Node> children) { this.children = children; }

        public Token getToken() { return token; }

        public void genAsmCode(CodeAsm asm) {
            for (Node node : children)
                node.genAsmCode(asm);
        }

    }

    public class VarNode extends Node {
        public VarNode(Token token) { super(null, token); }
        public VarNode(Token token, Type type) { super(new ArrayList<>(), token); this.type = type; }
        public VarNode(ArrayList<Node> children, Token token) { super(children, token); }

        @Override
        public void genAsmCode(CodeAsm asm) {
            int offset = 10;
            // TODO взять из таблицы
            asm.add(CommandAsm.CommandType.MOV, RegisterType.EAX, RegisterType.EBP);
            asm.add(CommandAsm.CommandType.SUB, RegisterType.EAX, offset);
            asm.add(CommandAsm.CommandType.PUSH, RegisterType.EAX);
        }
    }

    public class ConstNode extends Node {
        Object result;

        public ConstNode(Type type, Object result) { // change plz
            super(null, new Token(result, new Pair(TokenType.IDENTIFIER, type == integer_ ? TokenValue.CONST_INTEGER : TokenValue.CONST_DOUBLE)));
            this.type = type;
            this.result = result;
        }
        public ConstNode(Token token, Type type, Object result) {
            super(null, token);
            this.result = result;
            this.type = type;
        }

        @Override
        public void genAsmCode(CodeAsm asm) {
            switch (this.type.category) {
                case INT:
                    asm.add(CommandAsm.CommandType.PUSH, (Integer)result);
                    break;
                case CHAR:
                    asm.add(CommandAsm.CommandType.SUB, RegisterType.ESP, 1);
                    asm.add(CommandAsm.CommandType.MOV, DataType.BYTE, RegisterType.ESP, 0, (Integer)result);
                    break;
                case DOUBLE:
                    String s = asm.addDoubleConstant((Double)result);
                    asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, s, 4);
                    asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, s, 0);
                    break;
            }
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

    public class ArrayNode extends Node {
        public ArrayNode() {
            super(new ArrayList<>(),  new Token("typed_constant", new Pair(TokenType.IDENTIFIER, VARIABLE)));
        }
    }

    public class RecordNode extends Node {
        public RecordNode() {
            super(new ArrayList<>(), new Token("typed_constant", new Pair(TokenType.IDENTIFIER, VARIABLE)));
        }
    }

    public class BodyFunction extends Node {
        public BodyFunction() { super(new ArrayList<>(), new Token("statements", new Pair(TokenType.IDENTIFIER, VARIABLE))); }
    }

    public class AssignmentStatement extends Node {
        public AssignmentStatement(Node identifier, Node expression) {
            super(getList(identifier, expression), new Token(":=", new Pair(TokenType.KEYWORD, KEYWORD_ASSIGN)));}
    }

    public class IfNode extends Node {
        public IfNode() { super(getTmpLists(2), new Token("if", new Pair(TokenType.KEYWORD, KEYWORD_IF))); }

        public Node getIf_() { return this.children.get(0); }

        public Node getThen_() { return this.children.get(1); }

        public Node getElse_() { return this.children.get(2); }

        public void setIf_(Node if_) { this.children.set(0, if_); }

        public void setThen_(Node then_) { this.children.set(1, then_); }

        public void setElse_(Node else_) {
            this.children.add(null);
            //else_.token = new Token("else", new Pair(TokenType.IDENTIFIER, VARIABLE));
            this.children.set(2, else_); }
    }

    public class WhileNode extends Node {
        public WhileNode() { super(getTmpLists(2), new Token("while", new Pair(TokenType.IDENTIFIER, VARIABLE))); }

        public Node getCondition() { return this.children.get(0); }

        public Node getBody() { return this.children.get(1); }

        public void setCondition(Node condition) { this.children.set(0, condition); }

        public void setBody(Node body) { this.children.set(1, body); }
    }

    public class ForNode extends Node {
        public SymTable.Symbol counter;
        public boolean isDownTo = false;

        public ForNode(SymTable.Symbol counter, Token token) {

            super(getTmpLists(5), token);
            this.counter = counter; }

        public Node getFrom() { return this.children.get(1); }

        public Node getTo() { return this.children.get(3); }

        public Node getBody() { return this.children.get(4); }

        public void setFromName(Node fromName) { this.children.set(0, fromName); }

        public void setFrom(Node from) { this.children.set(1, from); }

        private void setToName(Token to) {this.children.set(2, new VarNode(to, NIL())); }

        public void setTo(Node to) { this.children.set(3, to); }

        public void setBody(Node body) { this.children.set(4, body); }
    }

    private class IndexNode extends Node {
        public IndexNode(ArrayList<Node> children, Type type) {
            super(children, new Token("[]", new Pair(TokenType.SEPARATOR, SEP_BRACKETS_SQUARE_LEFT)));
            this.type = type;
        }
    }

    private class FieldAccessNode extends Node {
        public FieldAccessNode(ArrayList<Node> children, Type type) {
            super(children, new Token(".", new Pair(TokenType.SEPARATOR, SEP_DOT)));
            this.type = type;
        }
    }

    private ArrayList<Node> getTmpLists(int size) {
        ArrayList<Node> tmp = new ArrayList<>();
        for (int i = 0; i < size; i++)
            tmp.add(null);
        return tmp;
    }

    private class ParamListNode extends Node {
        public ParamListNode(Type type) {
            super(new ArrayList<>(), new Token("params", new Pair(TokenType.IDENTIFIER, VARIABLE)));
            this.type = type;
        }
    }

    private class CastNode extends Node {
        Type oldType = null;

        public CastNode(Node to, Type oldType, Type newType, Token token) {
            super(getList(to), token);
            this.oldType = oldType;
            this.type = newType;
        }
    }

    private class WriteNode extends Node {

        public WriteNode(ArrayList<Node> children, Token token) {
            super(children, token);
        }
    }

    private class ContinueNode extends Node {
        public ContinueNode(Token token) {
            super(null, token);
        }
    }

    private class BreakNode extends Node {
        public BreakNode(Token token) {
            super(null, token);
        }
    }

    private class ExitNode extends Node {
        public ExitNode(Token token) {
            super(new ArrayList<>(), token);
        }
    }
}
