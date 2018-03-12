package SyntacticalAnalyzer;

import Generator.CodeAsm;
import Generator.CommandAsm;
import Generator.DataType;
import Generator.RegisterType;
import Tokens.*;
import Tokens.Pair;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.*;

import static Generator.CommandAsm.CommandType;
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
        // put all tokens from Tokenizer in a hash
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


    private Node parseExpression() throws SyntaxException {
        Node e = parseExpr();
        Token t = currentToken();
        while (t.getTokenValue() != KEYWORD_EOF && isLogical(t.getTokenValue())) {
            goToNextToken();
            e = calculateConstants(e, parseExpr(), t);
            t = currentToken();
        }
        return e;
    }

    private Node parseExpr() throws SyntaxException {
        Node e = parseTerm();
        Token t = currentToken(); // +
        while (t.getTokenValue() != KEYWORD_EOF && isExpr(t.getTokenValue())) {
            goToNextToken();
            e = calculateConstants(e, parseTerm(), t);
            t = currentToken();
        }
        return e;
    }

    private Node parseTerm() throws SyntaxException {
        Node e = parseFactor();
        Token t = currentToken();
        while (t.getTokenValue() != KEYWORD_EOF && (isTerm(t.getTokenValue()))) {
            goToNextToken();
            e = calculateConstants(e, parseFactor(), t);
            t = currentToken();
        }
        return e;
    }

    private Node parseFactor() throws SyntaxException {
        Token currentToken = currentToken();
        goToNextToken();
        switch (currentToken.getTokenValue()) {
            case OP_MINUS:
                Node factor = parseFactor();
                if (factor instanceof ConstNode) {
                    Double result = getValueForEval(factor);
                    if (factor.type.category == Category.INT || factor.type.category == Category.CHAR)
                        return new ConstNode(factor.type, result.intValue() * -1);
                    return new ConstNode(factor.type, getValueForEval(factor) * -1.0);
                }
                return new UnaryMinusNode(getList(factor), currentToken);
            case KEYWORD_NOT:
                return new NotNode(getList(parseFactor()), currentToken);
            case VARIABLE: {
                switch (getTypeFromTable(currentToken).category) {
                    case ARRAY:
                        return indexedVariable(new VarNode(currentToken, getTypeFromTable(currentToken)));
                    case RECORD:
                        return fieldAccess(new VarNode(currentToken, getTypeFromTable(currentToken)));
                    case FUNCTION:
                        return parseFunctionCall(new VarNode(currentToken, getTypeFromTable(currentToken)));
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
                Node e = parseExpression();
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return e;
            default:
                throwSyntaxException("Error in pos %s:%s expected identifier, constant or expression", currentToken());
        }
        return null;
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

    private Type getTypeForOperation(Type left, Type right, Token operation) throws SyntaxException {
        if (operation == null)
            throw new SyntaxException(String.format("Error in pos %s:%s unknown operation",
                    currentToken().getPosX(), currentToken().getPosY()));
        if (!left.isScalar() || !right.isScalar())
            throwSyntaxException("Error in pos %s:%s unsupported operands types \"%s\", \"%s\" for \"%s\"",
                currentToken(), left.category.toString(), right.category.toString(), operation.getText());
        if (isLogical(operation.getTokenValue()))
            return IntType();
        if (left == CharType() || right == CharType())
            throwSyntaxException("Error in pos %s:%s unsupported operands types \"%s\", \"%s\" for \"%s\"",
                currentToken(), left.category.toString(), right.category.toString(), operation.getText());
        if (isIntOnly(operation.getTokenValue()))
            if (left != IntType() || right != IntType())
                throwSyntaxException("Error in pos %s:%s unsupported operands types \"%s\", \"%s\" for \"%s\"",
                        currentToken(), left.category.toString(), right.category.toString(), operation.getText());
        Type resultType = left;
        if (left == IntType() && right == DoubleType() || left == DoubleType() && right == IntType())
            resultType = DoubleType();
        else if (left != right)
            throwSyntaxException("Error in pos %s:%s incompatible types", currentToken());
        if (operation.getTokenValue() == OP_DIVISION)
            return DoubleType();
        return resultType;    }

    private Double getValueForEval(Node left) {
        return left.type  == char_ ? (int)left.token.getText().charAt(0) : left.type == DoubleType() ?
                Double.parseDouble(left.token.getText()) : Integer.parseInt(left.token.getText());
    }

    private Object evalOperation(Double leftValue, Double rightValue, TokenValue operation) {
        switch (operation) {
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
            case KEYWORD_SHL:
                return leftValue.intValue() << rightValue.intValue();
            case KEYWORD_SHR:
                return leftValue.intValue() >> rightValue.intValue();
            case KEYWORD_XOR:
                return leftValue.intValue() ^ rightValue.intValue();
            case KEYWORD_DIV:
                return Math.round(leftValue / rightValue);
            case KEYWORD_MOD:
                return leftValue.intValue() % rightValue.intValue();
            case KEYWORD_AND:
                return leftValue.intValue() & rightValue.intValue();
            case KEYWORD_OR:
                return leftValue.intValue() | rightValue.intValue();
            case OP_MULT:
                return leftValue * rightValue;
            case OP_DIVISION:
                return leftValue / rightValue;
            case OP_PLUS:
                return leftValue + rightValue;
            case OP_MINUS:
                return leftValue - rightValue;
            default:
                return null; //:-)
        }
    }


    private Object getResultForOperation(Node left, Node right, Token operation, Type operationType) {
        Double leftValue = getValueForEval(left);
        Double rightValue = getValueForEval(right);
        Object result = evalOperation(leftValue, rightValue, operation.getTokenValue());
        if (operationType.category == Category.INT || operationType.category == Category.CHAR) //{
            return (int)((Double.parseDouble(result.toString())));
        return result;
    }

    private Node cast(Node left, Type rightType, Type operationType, Token operation) {
        Type leftType = left.type;
        if (!isLogical(operation.getTokenValue()) && leftType.category != operationType.category)
                return new CastNode(left, leftType, operationType, left.token);
        if (rightType.category == Category.DOUBLE && leftType.category != Category.DOUBLE)
                return new CastNode(left, leftType, DoubleType(), left.token);
        if (rightType.category == Category.CHAR && leftType.category != Category.CHAR)
                return new CastNode(left, leftType, DoubleType(), left.token);
        return left;
    }

    private Node calculateConstants(Node left, Node right, Token operation) throws SyntaxException {
        Type operationType = getTypeForOperation(left.type, right.type, operation);
        left  = cast(left, right.type, operationType, operation);
        right = cast(right, left.type, operationType, operation);
        ConstNode l = null;
        ConstNode r = null;
        if (left instanceof ConstNode)
            l = (ConstNode)left;
        if (right instanceof ConstNode)
            r = (ConstNode)right;
        if (l == null || r == null)
            return new BinOpNode(getList(left, right), operation, operationType);
        return new ConstNode(operationType, getResultForOperation(left, right, operation, operationType));
    }

    private void throwSyntaxException(String textException, Token current, String ... substitutions) throws SyntaxException {
        ArrayList<String> list = new ArrayList<String>(Arrays.asList(substitutions));
        list.add(0, current.getPosX()); list.add(1, current.getPosY());
        throw new SyntaxException(String.format(textException, list.toArray()));
    }

    private ArrayList<Node> getList(Node ... nodes) { return new ArrayList<>(Arrays.asList(nodes)); }

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

    private Node parseFunctionCall(Node var) throws SyntaxException {
        FunctionType type = (FunctionType)getTypeFromTable(var.token);
        Node result = new FunctionCallNode(var);
        result.children.add(parseParameterList(type));
        switch (result.type.category) {
            case ARRAY:
                return indexedVariable(result);
            case RECORD:
                return fieldAccess(result);
            default:
                return result;
        }
    }

    private Node parseParameterList(FunctionType type) throws SyntaxException {
        Node result = new ParamListNode(type.returnType);
        ArrayList<Node> arguments = new ArrayList<>();
        if (type.params.symTable.size() != 0 || currentValue() == SEP_BRACKETS_LEFT)
            parseExpressionList(arguments, false);
        if (arguments.size() != type.params.symTable.size())
            throwSyntaxException("Error in pos %s:%s illegal parameters count", currentToken());
        int index = 0;
        for (Map.Entry<String, SymTable.Symbol> entry : type.params.symTable.entrySet()) {
            requireTypesCompatibility(entry.getValue().type, arguments.get(index).type, false);
            if (entry.getValue().type.category != arguments.get(index).type.category)
                result.children.add(new CastNode(arguments.get(index), arguments.get(index).type, entry.getValue().type, arguments.get(index).token));
            else
                result.children.add(arguments.get(index));
            index++;
        }

        return result;
    }

    private void parseExpressionList(ArrayList<Node> nodes, boolean isRead) throws SyntaxException {
        requireFollowingToken(SEP_BRACKETS_LEFT);
        while (currentToken().getTokenValue() != SEP_BRACKETS_RIGHT) {
            nodes.add(parseExpression());
            if (isRead && getSymbolFromTable(nodes.get(nodes.size() - 1).token).isConst)
                throwSyntaxException("Error in pos %s:%s can't read to const variable", nodes.get(nodes.size() - 1).token);
            if (currentToken().getTokenValue() == SEP_BRACKETS_RIGHT)
                break;
            requireFollowingToken(SEP_COMMA);
        }
        goToNextToken();
    }

    private Node castVariables(Token token) throws SyntaxException {
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
                throwSyntaxException("Error in pos %s:%s can't convert from %s to %s", token,
                        expr.type.category.toString(), token.getText());
        }
        requireFollowingToken(SEP_BRACKETS_RIGHT);
        return new CastNode(expr, expr.type, castType, token);
    }

    public Type parse() throws SyntaxException {
        goToNextToken();
        String name = "@MAIN";
        if (currentValue() == KEYWORD_PROGRAM)
            name = parseProgram();
        tables = new Stack<>();
        tables.push(new SymTable());
        declarationPart();
        FunctionType main = new FunctionType(new SymTable(), tables.peek(), NIL(), compoundStatement(), name);
        ArrayList<Node> children = main.compound_statement.children;
        if (!(children.get(children.size() - 1) instanceof ExitNode))
            main.compound_statement.children.add(new ExitNode(currentToken()));
        Token result = new Token("result", new Pair(TokenType.IDENTIFIER, VARIABLE)); // Magic identifier result
        main.vars.addVARSymbol(getList(new VarNode(result)), NIL(), null, false);
        main.vars.calculateOffsets();
//        for (SymTable table : tables) {
//            table.printOffsets();
//            System.out.println(">>>>>>>>>>>>>>>>>>>>>>>>>>>>>");
//        }
        return main;
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


    private void declarationPart() throws SyntaxException { // declaration_part
        while (true) {
            switch (currentValue()) {
                case KEYWORD_TYPE:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    while (currentValue() == VARIABLE) {
                        typeDeclaration(); // type_declaration
                        requireFollowingToken(SEP_SEMICOLON);
                    }
                    break;
                case KEYWORD_VAR:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    while (currentValue() == VARIABLE) {
                        variableDeclaration(); // variable_declaration
                        requireFollowingToken(SEP_SEMICOLON);
                    }
                    break;
                case KEYWORD_CONST:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    while (currentValue() == VARIABLE) {
                        constDeclaration(); // const_declaration
                        requireFollowingToken(SEP_SEMICOLON);
                    }
                    break;
                case KEYWORD_FUNCTION:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    functionDeclaration(false); // function_declaration
                    requireFollowingToken(SEP_SEMICOLON);
                    break;
                case KEYWORD_PROCEDURE:
                    goToNextToken();
                    requireCurrentToken(VARIABLE);
                    functionDeclaration(true); // procedure_declaration
                    requireFollowingToken(SEP_SEMICOLON);
                    break;
                default:
                    return;
            }
        }
    }


    private ArrayList<Node> identifierList() throws SyntaxException { // identifier_list
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

    private Type parseType() throws SyntaxException { // type
        Token currentToken = currentToken();
        goToNextToken();
        Type type = null;
        switch (currentToken.getTokenValue()) {
            case VARIABLE:
                type = typeAlias(currentToken);
                break;
            case KEYWORD_ARRAY:
                return arrayType();
            case KEYWORD_RECORD:
                return recordType();
            case KEYWORD_INTEGER:
            case KEYWORD_DOUBLE:
            case KEYWORD_CHARACTER:
                type = simpleType(currentToken);
         //       goToNextToken();
                break;
            default:
                throwSyntaxException("Error in pos %s:%s expected type but got %s", currentToken, currentToken.getText());
                //throw new SyntaxException(String.format("Error in pos %s:%s expected type but got %s",
                  //      currentToken.getPosX(), currentToken.getPosY(), currentToken.getText()));
        }
//        requireFollowingToken(SEP_SEMICOLON, OP_EQUAL);
        return type;
    }

    private Type typeAlias(Token token) throws SyntaxException {
        //goToNextToken();
        return getTypeFromTable(token);
    }

    private Type simpleType(Token token) throws SyntaxException { // simple_type
        //goToNextToken();
        if (token.getTokenValue() == KEYWORD_INTEGER)
            return IntType();
        if (token.getTokenValue() == KEYWORD_DOUBLE)
            return DoubleType();
        return CharType();
    }

    private Type arrayType() throws SyntaxException { // array_type
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

    private Type recordType() throws SyntaxException {
        SymTable fields = new SymTable();
        while (currentValue() == VARIABLE) { // { identifier_list, ":", type, ";" };
            fields.addVARSymbol(identifierList(), parseType(), null, false); // May be not....
            requireFollowingToken(SEP_SEMICOLON);
        }
        fields.calculateOffsets();
        //fields.printOffsets();
        //System.out.println(".........");
        requireFollowingToken(KEYWORD_END);
        return new RecordType(fields);
    }

    private Node typedConstant(Type type) throws SyntaxException { // typed_constant
        Node typedConstant;
        switch (type.category) {
            case RECORD:
                requireFollowingToken(SEP_BRACKETS_LEFT);
                RecordType recordType = (RecordType)type;
                typedConstant = new TypedConstant(recordType);
                for (Map.Entry<String, SymTable.Symbol> pair : recordType.fields.symTable.entrySet()) {
                    Token variable = currentToken();
                    requireFollowingToken(VARIABLE);
                    if (!pair.getKey().equals(variable.getText())) {
                        if (recordType.fields.symTable.containsKey(variable.getText()))
                            throwSyntaxException("Error in pos %s:%s illegal initialization order", variable);
                        throwSyntaxException("Error in pos %s:%s unknown record field identifier %s", variable, variable.getText());
                    }
                    requireFollowingToken(OP_COLON);
                    typedConstant.children.add(typedConstant(pair.getValue().type));
                    requireFollowingToken(SEP_SEMICOLON);
                }
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return typedConstant;
            case ARRAY:
                ArrayType arrayType = (ArrayType)type;
                typedConstant = new TypedConstant(arrayType);
                int min = Integer.parseInt(arrayType.min.result.toString());
                int max = Integer.parseInt(arrayType.max.result.toString());
                Type element = arrayType.elementType;
                requireFollowingToken(SEP_BRACKETS_LEFT);
                for (int i = min; i <= max; i++) {
                    typedConstant.children.add(typedConstant(element));
                    if (i != max) {
                        requireFollowingToken(SEP_COMMA);
                    }
                }
                requireFollowingToken(SEP_BRACKETS_RIGHT);
                return typedConstant;
            case INT:
            case DOUBLE:
            case CHAR:
                typedConstant = parseExpression();
                requireTypesCompatibility(type, typedConstant.type, false);
                if (type.category != typedConstant.type.category)
                    return new CastNode(typedConstant, typedConstant.type, type, typedConstant.token);
                else
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
        throwSyntaxException("Error in pos %s:%s incompatible types: got %s expected %s", currentToken(),
            right.category.toString(), left.category.toString());
    }

    private Node parseConstValue(ArrayList<Node> identifiers, Type type) throws SyntaxException {
        Node value = null;
        if (currentValue() == OP_EQUAL) {
            goToNextToken();
            if (identifiers.size() > 1)
                throwSyntaxException("Error in pos %s:%s only one variable can be initialized", currentToken());
            value = typedConstant(type);
        }
        return value;
    }

    private void variableDeclaration() throws SyntaxException { // type_declaration_part,  variable_declaration_part
        ArrayList<Node> identifiers = identifierList();
        Type type = parseType();
        Node value = parseConstValue(identifiers, type);
        tables.peek().addVARSymbol(identifiers, type, value, false);
    }

    private void typeDeclaration() throws SyntaxException { // type_declaration = identifier, "=", type, ";";
        Token identifier = currentToken();
        goToNextToken();
        requireFollowingToken(OP_EQUAL);
        Type type = parseType();
        tables.peek().addTYPESymbol(identifier, type);
    }

    private void constDeclaration() throws SyntaxException { // typed_constant_declaration | constant_declaration
        Token identifier = currentToken();
        goToNextToken();
        switch (currentValue()) {
            case OP_EQUAL: {
                goToNextToken();
                Node expression = parseExpression();
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
                Node typedConstant = typedConstant(type);
                tables.peek().addCONSTSymbol(identifier, type, typedConstant);
                return;
                }
            default:
                throwSyntaxException("Error in pos %s:%s expected colon or equal", currentToken());
        }
    }

    private void functionDeclaration(boolean isProcedure) throws SyntaxException { // function_header, ";", block, ";";
        exitFound = false;
        Token functionIdentifier = currentToken();
        goToNextToken();
        SymTable params = new SymTable();
        if (currentValue() == SEP_BRACKETS_LEFT) {
            goToNextToken();
            do {
                requireCurrentToken(VARIABLE, KEYWORD_VAR, KEYWORD_CONST, SEP_BRACKETS_RIGHT);
                switch (currentValue()) {
                    case VARIABLE: {
                        ArrayList<Node> identifiers = identifierList();
                        Type type = parseType();
                        Node value = parseConstValue(identifiers, type);
                        params.addVARSymbol(identifiers, type, value, false);
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON);
                        break;
                    }
                    case KEYWORD_VAR: {
                        goToNextToken();
                        requireCurrentToken(VARIABLE);
                        ArrayList<Node> identifiers = identifierList();
                        Type type = parseType();
                        params.addVARSymbol(identifiers, type, null, true);
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON, OP_EQUAL);
                        if (currentValue() == OP_EQUAL)
                            throwSyntaxException("Error in pos %s:%s can't initialize value passed by reference", currentToken());
                        break;
                    }
                    case KEYWORD_CONST: {
                        goToNextToken();
                        requireCurrentToken(VARIABLE);
                        ArrayList<Node> identifiers = identifierList();
                        Type type = parseType();
                        requireCurrentToken(SEP_BRACKETS_RIGHT, SEP_SEMICOLON, OP_EQUAL);
                        Node typedConstant = null;
                        if (currentValue() == OP_EQUAL) {
                            goToNextToken();
                            typedConstant = typedConstant(type);
                        }
                        params.addCONSTSymbol(identifiers, type, typedConstant);
                        break;
                    }
                }
                if (currentValue() != SEP_BRACKETS_RIGHT) goToNextToken();
            } while (currentValue() != SEP_BRACKETS_RIGHT);
            goToNextToken();
        }
        Type returnType = NIL();
        if (!isProcedure) {
            requireFollowingToken(OP_COLON);
            returnType = parseType(); // type_identifier
        }
        FunctionType functionType = new FunctionType(null, null, returnType,
                null, functionIdentifier.getText());
        tables.peek().addVARSymbol(functionIdentifier, functionType);
        tables.push(params);
        requireFollowingToken(SEP_SEMICOLON);
        tables.peek().calculateOffsets();

        functionType.params = tables.peek();
        // Declaration
        tables.push(new SymTable());
        declarationPart();
        //if (!isProcedure && !tables.peek().symTable.containsKey("result")) {
            Token result = new Token("result", new Pair(TokenType.IDENTIFIER, VARIABLE)); // Magic identifier result
            tables.peek().addVARSymbol(getList(new VarNode(result)), returnType, null, false);
        //}
        // Compound_statement
        functionType.compound_statement = compoundStatement();
        ArrayList<Node> children = functionType.compound_statement.children;
        if (children.size() == 0 || !(children.get(children.size() - 1) instanceof ExitNode)) {
            functionType.compound_statement.children.add(new ExitNode(currentToken()));
            exitFound = true;
        }
        tables.peek().calculateOffsets();
        functionType.vars = tables.peek();
        tables.pop();
        tables.pop();
        //tables.peek().addVARSymbol(functionIdentifier, functionType);
        if (resultCount == 0 && functionType.returnType != NIL() && !exitFound)
            throwSyntaxException("Error in pos %s:%s RESULT identifier in function not found", currentToken());
        if (resultCount > 0) resultCount--;
    }

    private Node parseStatements() throws SyntaxException {
        Node statement = new BodyFunction();
        do {
            goToNextToken();
            Node node = statementPart();
            if (node != null)
                statement.children.add(node);
        } while (currentValue() == SEP_SEMICOLON);
        return statement;
    }

    private Node compoundStatement() throws SyntaxException {
        requireCurrentToken(KEYWORD_BEGIN);
        Node statement = parseStatements();
        requireFollowingToken(KEYWORD_END);
        return statement;
    }

    private Node statementPart() throws SyntaxException { // compound_statement
        switch (currentValue()) { // statement_list
            case VARIABLE:
                if (currentToken().getText().toLowerCase().equals("result")) // Magic identifier
                    resultCount++;
                Token currentToken = currentToken();
                if (getSymbolFromTable(currentToken).isConst)
                    throwSyntaxException("Error in pos %s:%s can't modify const variable", currentToken);
                Type type = getTypeFromTable(currentToken);
                if (type.category == Category.FUNCTION) {
                    goToNextToken();
                    return parseFunctionCall(new VarNode(currentToken, getTypeFromTable(currentToken)));
                }
                return assignStatement();
            case KEYWORD_IF:
                goToNextToken();
                return ifStatement();
            case KEYWORD_BEGIN:
                return compoundStatement();
            case KEYWORD_WHILE:
                return whileStatement();
            case KEYWORD_FOR:
                return forStatement();
            case KEYWORD_WRITE:
                return write();
            case KEYWORD_READ:
                return write();
            case KEYWORD_CONTINUE:
                return parseContinue(currentToken());
            case KEYWORD_BREAK:
                return parseBreak(currentToken());
            case KEYWORD_EXIT:
                return parseExit(currentToken());
            case SEP_SEMICOLON:
            case KEYWORD_END:
                return null;
            default:
                throwSyntaxException("Error in pos %s:%s unexpected token %s", currentToken(), currentToken().getText());
                //throw new SyntaxException(String.format("Error in pos %s:%s unexpected token %s",
                  //      currentToken().getPosX(), currentToken().getPosY(), currentToken().getText()));
        }
        return null;
    }

    private Node parseExit(Token token) throws SyntaxException {
        exitFound = true;
        Node exitNode = new ExitNode(token);
        goToNextToken();
        if (currentValue() == SEP_BRACKETS_LEFT) {
            Node expr = parseExpression();
            Type newType = tables.peek().symTable.get("result").type;
            requireTypesCompatibility(newType, expr.type, false);
            if (expr.type.category != newType.category)
                exitNode.children.add(new CastNode(expr, expr.type, newType, expr.token));
            else
                exitNode.children.add(expr);
        }
        return exitNode;
    }

    private Node parseContinue(Token token) throws SyntaxException {
        if (loopCount == 0)
            throwSyntaxException("Error in pos %s:%s continue not allowed", currentToken());
        goToNextToken();
        return new ContinueNode(token);
    }

    private Node parseBreak(Token token) throws SyntaxException {
        if (loopCount == 0)
            throwSyntaxException("Error in pos %s:%s break not allowed", currentToken());
        goToNextToken();
        return new BreakNode(token);
    }

    private Node write() throws SyntaxException { // read
        Token current = currentToken();
        Node node = currentToken().getTokenValue() == KEYWORD_READ ?
                new ReadNode(currentToken()) : new WriteNode(currentToken());
        goToNextToken();
        parseExpressionList(node.children, current.getTokenValue() == KEYWORD_READ);
        return node;
    }

    private Node whileStatement() throws SyntaxException {
        loopCount++;
        WhileNode whileNode = new WhileNode();
        goToNextToken();
        whileNode.setCondition(parseExpression());
        requireFollowingToken(KEYWORD_DO);
        whileNode.setBody(statementPart());
        loopCount--;
        return whileNode;
    }

    private Node forStatement() throws SyntaxException {
        loopCount++;
        Token forToken = currentToken();
        goToNextToken();
        requireCurrentToken(VARIABLE);
        Node start = new VarNode(currentToken(), getTypeFromTable(currentToken()));
        requireTypesCompatibility(IntType(), start.type, false);
        ForNode forNode = new ForNode(tables.peek().getSymbol(currentToken()), forToken);
        goToNextToken();
        requireFollowingToken(KEYWORD_ASSIGN);
        Node from = parseExpression();
        requireTypesCompatibility(IntType(), from.type, false);
        forNode.setFrom(from);
        if (currentToken().getTokenValue() == KEYWORD_DOWNTO)
            forNode.isDownTo = true;
        Token toName = currentToken();
        requireFollowingToken(KEYWORD_TO, KEYWORD_DOWNTO);
        Node to = parseExpression();
        requireTypesCompatibility(IntType(), to.type, false);
        forNode.setTo(to);
        requireFollowingToken(KEYWORD_DO);
        forNode.setBody(statementPart());
        forNode.setFromName(start);
        forNode.setToName(toName);
        loopCount--;
        return forNode;
    }

    private Node ifStatement() throws SyntaxException {
        IfNode ifNode = new IfNode();
        ifNode.setIf_(parseExpression());
        requireFollowingToken(KEYWORD_THEN);
        ifNode.setThen_(statementPart());
        if (currentValue() == KEYWORD_ELSE) {
            goToNextToken();
            ifNode.setElse_(statementPart());
        }
        return ifNode;
    }

    private Node assignStatement() throws SyntaxException {
        Node identifier = null;
        Token currenToken = currentToken();
        Type type = getTypeFromTable(currentToken());
        switch (type.category) {
            case ARRAY:
                goToNextToken();
                identifier = indexedVariable(new VarNode(currenToken, type));
                break;
            case RECORD:
                goToNextToken();
                identifier = fieldAccess(new VarNode(currenToken, type));
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
        Node expression = parseExpression();
        //SymTable.Symbol expressionSymbol = getSymbolFromTable(expression.token);
//        if (tables.peek().isType(currenToken) && !expressionSymbol.isType ||
//                !tables.peek().isType(currenToken) && expressionSymbol.isType)
//            throwSyntaxException("Error in pos %s %s can't assign value to a type", currenToken);
        //isTypes(currenToken, expression);
        //System.out.println(currentToken());
        requireTypesCompatibility(identifierType, expression.type, false);
        Type rightType = expression.type.category == Category.FUNCTION ?
                ((FunctionType)expression.type).returnType : expression.type;
        //expression.type = rightType;
        if (rightType.category != identifier.type.category)
            expression = new CastNode(expression, expression.type, identifier.type, expression.token);
        //return new CastNode(left, leftType, operationType, left.token);
        return new AssignStatement(identifier, expression);
    }

    private void isTypes(Token token, Node node) throws SyntaxException {
        if (getSymbolFromTable(token).isType) {
            if (node.token.getTokenValue() == VARIABLE)
                if (getSymbolFromTable(node.token).isType)
                    return;
            throwSyntaxException("Error in pos %s:%s incompatible types: expected type %s", currentToken(),
                getSymbolFromTable(token).type.category.toString());
        }
    }

    private Node indexedVariable(Node index) throws SyntaxException {
        Type currType = index.type;
        while (currentValue() == SEP_BRACKETS_SQUARE_LEFT) {
            if (currType.category != Category.ARRAY)
                throwSyntaxException("Error in pos %s:%s wrong size of array", currentToken());
            goToNextToken();
            ArrayType a = (ArrayType)currType;
            Node expr = parseExpression();
            if (expr.type.category != Category.INT)
                throwSyntaxException("Error in pos %s:%s require INT type in index", currentToken());
            index = new IndexNode(getList(index, expr), a.elementType);
            requireFollowingToken(SEP_BRACKETS_SQUARE_RIGHT);
            currType = a.elementType;
        }
        if (currType.category == Category.RECORD)
            return fieldAccess(index);
        return index;
    }

    private Node fieldAccess(Node node) throws SyntaxException {
        Type type = node.type;
        while (currentValue() == SEP_DOT) {
            if (type.category != Category.RECORD)
                throwSyntaxException("Error in pos %s:%s invalid record access", currentToken());
            goToNextToken();
            requireCurrentToken(VARIABLE);
            RecordType recordType = (RecordType)type;
            Iterator<Map.Entry<String, SymTable.Symbol>> it = recordType.fields.symTable.entrySet().iterator();
            SymTable.Symbol symbol = recordType.fields.symTable.get(currentToken().getText().toLowerCase());
            if (symbol == null)
                throwSyntaxException("Error in pos %s:%s field not found %s", currentToken(), currentToken().getText());
            Node varNode = new VarNode(currentToken(), symbol.type);
            node = new FieldAccessNode(getList(node, varNode), symbol.type);
            type = symbol.type;
            goToNextToken();
        }
        if (type.category == Category.ARRAY)
            return indexedVariable(node);
        return node;
    }

    // TODO сделать одну функцию на эти двк операции ниже
    private Type getTypeFromTable(Token identifier) throws SyntaxException {
        for (int i = tables.size() - 1; i >= 0; i--)
            if (tables.get(i).getAliasTypeFunction(identifier))
                return tables.get(i).getAliasType(identifier);
        throwSyntaxException("Error in pos %s:%s identifier not found %s", identifier, identifier.getText());
        return null;
    }

    private SymTable.Symbol getSymbolFromTable(Token identifier) throws SyntaxException {
        for (int i = tables.size() - 1; i >= 0; i--)
            if (tables.get(i).getAliasTypeFunction(identifier))
                return tables.get(i).symTable.get(identifier.getText().toLowerCase());
        throwSyntaxException("Error in pos %s:%s identifier not found %s", identifier, identifier.getText());
        return null;
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

        public int getSize() {
            switch (category) {
                case INT:
                    return 4;
                case DOUBLE:
                    return 8;
                case CHAR:
                    return 1;
                case RECORD:
                    return ((RecordType)this).getSize();
                case ARRAY:
                    return ((ArrayType)this).getSize();
                default:
                    return 0;
            }
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

        @Override
        public int getSize() {
            int size = (Integer.parseInt(max.result.toString()) -
                    Integer.parseInt(min.result.toString()) + 1) * elementType.getSize();
            return size + (4 - size % 4) % 4;
        }

        public int getArrayOffset(Type type) {
            switch (type.category) {
                case INT:
                    return 4;
                case DOUBLE:
                    return 8;
                case CHAR:
                    return 1;
                case ARRAY:
                case RECORD:
                    return type.getSize();
                default: // :-)
            }
            return 0;
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

        public int getRecordOffset(Token token, Type type) {
            int offset = fields.getOffset(token.getText());
            switch (type.category) {
                case INT:
                    return offset - 4;
                case DOUBLE:
                    return offset - 8;
                case CHAR:
                    return offset - 1;
                case ARRAY:
                    return offset - ((ArrayType)type).getSize();
                case RECORD:
                    return offset - ((RecordType)type).getSize();
                default: // :-)
            }
            return 0;
        }

        @Override
        public int getSize() {
            return fields.getSize() + (4 - fields.getSize() % 4) % 4;
        }

        public String toString() {
            String result = category + "\n";
            for (Map.Entry<String, SymTable.Symbol> entry: fields.symTable.entrySet())
                result += spaces.toString() + spaces + entry.getKey() + " : " + entry.getValue().type.toString()+ "\n";
            result += spaces + "END";
            return result;
        }

    }

    public class FunctionType extends Type {
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

    public class SymTable {
        public LinkedHashMap<String, Symbol> symTable;
        private int size = 0;

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
            size += isPointerParam ? symbols.size() * 4 : symbols.size() * type.getSize();
            for (Node symbol : symbols) {
                //size += isPointerParam ? 4 : type.getSize();
                checkDuplicated(symbol.getToken());
                symTable.put(symbol.token.getText().toLowerCase(), new Symbol(type, value, false, isPointerParam));
            }
        }

        private void addVARSymbol(Token symbol, Type type) throws SyntaxException {
            size += type.getSize();
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

        private LinkedHashMap<String, Integer> offsets = new LinkedHashMap<>();

        public void calculateOffsets() {
            int offset = 0;
            for (Map.Entry<String, Symbol> entry : symTable.entrySet()) {
                if (entry.getValue().isType) {
                    offsets.put(entry.getKey(), 0);
                    continue;
                }
                offset += entry.getValue().isPointerParam ? 4 : entry.getValue().type.getSize();
                offsets.put(entry.getKey(), offset);
            }
        }

        public void printOffsets() {
            for (Map.Entry<String, Integer> entry : offsets.entrySet()) {
                System.out.println(entry.getKey() + " " + entry.getValue());
            }
        }


        public int getSize() {
            return size;
        }

        public int getOffset(String name) {
            return offsets.get(name);
        }

        public class Symbol {
            public Type type;
            public Node value;
            public boolean isType = false;
            public boolean isConst = false;
            public boolean isPointerParam = false;

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

        public void genCode(CodeAsm asm) {
            for (Map.Entry<String, Symbol> entry : symTable.entrySet()) {
                if (entry.getValue().type.category == Category.FUNCTION) {
                    FunctionType type = (FunctionType)entry.getValue().type;
                    asm.startFunction(type.params, type.vars, entry.getKey());
                    type.vars.genCode(asm);
                    type.compound_statement.genAsmCode(asm, false);
                    asm.endFunction();
                }
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

        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            for (Node node : children)
                node.genAsmCode(asm, isLeft);
        }

    }

    public void putValueOnStack(CodeAsm asm, Type type) {
        asm.add(CommandAsm.CommandType.POP, RegisterType.EAX);
        switch (type.category) {
            case CHAR:
                asm.add(CommandAsm.CommandType.MOV, RegisterType.AL, DataType.BYTE, RegisterType.EAX, 0);
                asm.add(CommandAsm.CommandType.SUB, RegisterType.ESP, 1);
                asm.add(CommandAsm.CommandType.MOV, DataType.BYTE, RegisterType.ESP, 0, RegisterType.AL);
                return;
            case INT:
                asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, RegisterType.EAX, 0);
                return;
            case DOUBLE:
                asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, RegisterType.EAX, 4);
                asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, RegisterType.EAX, 0);
                return;
            case RECORD:
            case ARRAY:
                asm.add(CommandAsm.CommandType.ADD, RegisterType.EAX, type.getSize() - 4);
                asm.add(CommandAsm.CommandType.MOV, RegisterType.ECX, type.getSize() / 4);
                String label = asm.getLabelName("COPYSTRUCT");
                asm.add(CommandAsm.CommandType.LABEL, label);
                asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, RegisterType.EAX, 0);
                asm.add(CommandAsm.CommandType.SUB, RegisterType.EAX, 4);
                asm.add(CommandAsm.CommandType.LOOP, label);
        } // TODO throw
    }

    public class VarNode extends Node {
        public VarNode(Token token) { super(null, token); }
        public VarNode(Token token, Type type) { super(new ArrayList<>(), token); this.type = type; }
        public VarNode(ArrayList<Node> children, Token token) { super(children, token); }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            String name = token.getText().toLowerCase();
            javafx.util.Pair<Integer, Integer> offset = asm.getOffset(name);
            asm.add(CommandAsm.CommandType.MOV, RegisterType.EAX, DataType.DWORD, RegisterType.EBP, offset.getKey());
            asm.add(CommandAsm.CommandType.SUB, RegisterType.EAX, offset.getValue());
            if (asm.isVar(name))
                asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, RegisterType.EAX, 0);
            else
                asm.add(CommandAsm.CommandType.PUSH, RegisterType.EAX);
            if (!isLeft)
                putValueOnStack(asm, asm.getType(name));
        }
    }


    public class ConstNode extends Node {
        Object result;

        public ConstNode(Type type, Object result) { // change plz
            super(null, new Token(result, new Pair(TokenType.IDENTIFIER, type.category == Category.INT
                    ? TokenValue.CONST_INTEGER : TokenValue.CONST_DOUBLE)));
            this.type = type;
            this.result = result;
        }
        public ConstNode(Token token, Type type, Object result) {
            super(null, token);
            this.result = result;
            this.type = type;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            switch (this.type.category) {
                case INT:
                    asm.add(CommandAsm.CommandType.PUSH, Integer.parseInt(result.toString()));
                    break;
                case CHAR:
                    if (result.toString().length() > 1) {
                        String constant = asm.addStringConstant(result.toString());
                        asm.add(CommandAsm.CommandType.PUSH, constant);
                    }
                    else {
                        asm.add(CommandAsm.CommandType.SUB, RegisterType.ESP, 1);
                        asm.add(CommandAsm.CommandType.MOV, DataType.BYTE, RegisterType.ESP, 0,
                                (result.toString().codePointAt(0)));
                    }
                    break;
                case DOUBLE:
                    String s = asm.addDoubleConstant(Double.parseDouble(result.toString()));
                    asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, s, 4);
                    asm.add(CommandAsm.CommandType.PUSH, DataType.DWORD, s, 0);
                    break;
            }
        }
    }

    // Operations ..............

    HashMap<TokenValue, CommandType> operationsInt = new HashMap<TokenValue, CommandType>() {{
        put(OP_PLUS, CommandType.ADD);
        put(OP_MINUS, CommandType.SUB);
        put(OP_MULT, CommandType.IMUL);
        put(KEYWORD_AND, CommandType.AND);
        put(KEYWORD_OR, CommandType.OR);
        put(KEYWORD_XOR, CommandType.XOR);
        put(KEYWORD_DIV, CommandType.IDIV);
        put(KEYWORD_MOD, CommandType.IDIV);
        put(KEYWORD_ASSIGN, CommandType.MOV);
        put(KEYWORD_SHL, CommandType.SHL);
        put(KEYWORD_SHR, CommandType.SHR);
        put(OP_GREATER_OR_EQUAL, CommandType.SETL);
        put(OP_GREATER, CommandType.SETLE);
        put(OP_LESS_OR_EQUAL, CommandType.SETG);
        put(OP_LESS, CommandType.SETGE);
        put(OP_EQUAL, CommandType.SETNE);
        put(OP_NOT_EQUAL, CommandType.SETE);
    }};

    HashMap<TokenValue, CommandType> operationsDouble = new HashMap<TokenValue, CommandType>() {{
        put(OP_PLUS, CommandType.ADDSD);
        put(OP_MINUS, CommandType.SUBSD);
        put(OP_MULT, CommandType.MULSD);
        put(OP_DIVISION, CommandType.DIVSD);
        put(KEYWORD_ASSIGN, CommandType.MOVSD);
        put(OP_GREATER_OR_EQUAL, CommandType.SETB);
        put(OP_GREATER, CommandType.SETBE);
        put(OP_LESS_OR_EQUAL, CommandType.SETA);
        put(OP_LESS, CommandType.SETAE);
        put(OP_EQUAL, CommandType.JP);
        put(OP_NOT_EQUAL, CommandType.JNP);
    }};

    public void compareToAsm(CodeAsm asm, Node node) {
        node.children.get(0).genAsmCode(asm, false);
        node.children.get(1).genAsmCode(asm, false);
        Type leftType = node.children.get(0).type;
        CommandType commandType;
        switch (leftType.category) {
            case CHAR:
                asm.add(CommandType.MOVSX, RegisterType.EBX, DataType.BYTE, RegisterType.ESP, 0);
                asm.add(CommandType.MOVSX, RegisterType.EAX, DataType.BYTE, RegisterType.ESP, 1);
                asm.add(CommandType.SUB, RegisterType.ESP, 2);
                asm.add(CommandType.CMP, RegisterType.EAX, RegisterType.EBX);
                commandType = operationsInt.get(node.token.getTokenValue());
                break;
            case INT:
                asm.add(CommandType.POP, RegisterType.EBX);
                asm.add(CommandType.CMP, DataType.DWORD, RegisterType.ESP, 0, RegisterType.EBX);
                commandType = operationsInt.get(node.token.getTokenValue());
                break;
            case DOUBLE:
                boolean isEqual = node.token.getTokenValue() == OP_EQUAL || node.token.getTokenValue() == OP_NOT_EQUAL;
                CommandType compareType = isEqual ? CommandType.UCOMISD : CommandType.COMISD;
                asm.add(CommandType.MOVSD, RegisterType.XMM0, DataType.QWORD, RegisterType.ESP, 8);
                asm.add(CommandType.MOVSD, RegisterType.XMM1, DataType.QWORD, RegisterType.ESP, 0);
                asm.add(CommandType.ADD, RegisterType.ESP, 12);
                asm.add(compareType, RegisterType.XMM0, RegisterType.XMM1);
                commandType = operationsDouble.get(node.token.getTokenValue());
                if (!isEqual)
                    break;
                String label = asm.getLabelName("CONDFAIL");
                String endLabel = asm.getLabelName("ENDCOND");
                asm.add(CommandType.LAHF);
                asm.add(CommandType.TEST, RegisterType.AH, 68);
                asm.add(commandType, label);
                asm.add(CommandType.MOV, DataType.DWORD, RegisterType.ESP, 0, -1);
                asm.add(CommandType.JMP, endLabel);
                asm.add(CommandType.LABEL, label);
                asm.add(CommandType.MOV, DataType.DWORD, RegisterType.ESP, 0, 0);
                asm.add(CommandType.LABEL, endLabel);
                return;
            default:
                return; // :-)
        }
        asm.add(commandType, RegisterType.AL);
        asm.add(CommandType.SUB, RegisterType.AL, 1);
        asm.add(CommandType.MOVSX, RegisterType.EAX, RegisterType.AL);
        asm.add(CommandType.MOV, DataType.DWORD, RegisterType.ESP, 0, RegisterType.EAX);
    }

    public class BinOpNode extends Node {
        public BinOpNode(ArrayList<Node> children, Token token, Type type) {
            super(children, token);
            this.type = type;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            if (isLogical(token.getTokenValue())) {
                compareToAsm(asm, this);
                return;
            }
            children.get(0).genAsmCode(asm, false);
            if (children.size() == 1) {
                if (token.getTokenValue() == OP_MINUS)
                    if (type.category == Category.DOUBLE)
                        asm.add(CommandType.XOR, DataType.BYTE, RegisterType.ESP, 7, 128);
                    else
                        asm.add(CommandType.NEG, DataType.DWORD, RegisterType.ESP, 0);
                else if (token.getTokenValue() == KEYWORD_NOT)
                    asm.add(CommandType.NOT, DataType.DWORD, RegisterType.ESP, 0);
                return;
            }
            children.get(1).genAsmCode(asm, false);
            CommandType commandType;
            RegisterType reg1, reg2;
            if (type.category == Category.DOUBLE) {
                reg1 = RegisterType.XMM0;
                reg2 = RegisterType.XMM1;
                asm.add(CommandType.MOVSD, reg2, DataType.QWORD, RegisterType.ESP, 0);
                asm.add(CommandType.ADD, RegisterType.ESP, 8);
                asm.add(CommandType.MOVSD, reg1, DataType.QWORD, RegisterType.ESP, 0);
                commandType = operationsDouble.get(token.getTokenValue());
            } else {
                reg1 = RegisterType.EAX;
                reg2 = RegisterType.EBX;
                commandType = operationsInt.get(token.getTokenValue());
                if (commandType == CommandType.SHL || commandType == CommandType.SHR) {
                    reg2 = RegisterType.CL;
                    asm.add(CommandType.MOV, RegisterType.EBX, RegisterType.ECX);
                    asm.add(CommandType.POP, RegisterType.ECX);
                }
                else
                    asm.add(CommandType.POP, reg2);
                asm.add(CommandType.POP, reg1);
            }
            if (commandType == CommandType.IDIV)
                asm.add(CommandType.CDQ);
            if (commandType == CommandType.IMUL || commandType == CommandType.IDIV)
                asm.add(commandType, reg2);
            else
                asm.add(commandType, reg1, reg2);
            if (type.category == Category.DOUBLE)
                asm.add(CommandType.MOVSD, DataType.QWORD, RegisterType.ESP, 0, reg1);
            else
                asm.add(CommandType.PUSH, token.getTokenValue() == KEYWORD_MOD ? RegisterType.EDX : reg1);
            if (commandType == CommandType.SHL || commandType == CommandType.SHR)
                asm.add(CommandType.MOV, RegisterType.ECX, RegisterType.EBX);
        }

        //public void add(CommandAsm.CommandType type, DataType dataType, RegisterType registerType1, Integer offset, RegisterType registerType) {

    }

    public class UnaryMinusNode extends Node {
        public UnaryMinusNode(ArrayList<Node> children, Token token) {
            super(children, token);
            this.type = children.get(0).type;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            children.get(0).genAsmCode(asm, false);
            if (type.category == Category.DOUBLE)
                asm.add(CommandType.XOR, DataType.BYTE, RegisterType.ESP, 7, 128);
            else
                asm.add(CommandType.NEG, DataType.DWORD, RegisterType.ESP, 0);
        }
    }

    public class NotNode extends Node {
        public NotNode(ArrayList<Node> children, Token token) {
            super(children, token);
            this.type = children.get(0).type;
        }
    }

    public class TypedConstant extends Node {
        public TypedConstant(Type type) {
            super(new ArrayList<>(), new Token("typed_constant", new Pair(TokenType.IDENTIFIER, VARIABLE)));
            this.type = type;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            super.genAsmCode(asm, isLeft);
        }
    }

    public class BodyFunction extends Node {
        public BodyFunction() { super(new ArrayList<>(), new Token("statements", new Pair(TokenType.IDENTIFIER, VARIABLE))); }
    }

    public class AssignStatement extends Node {
        public AssignStatement(Node identifier, Node expression) {
            super(getList(identifier, expression), new Token(":=", new Pair(TokenType.KEYWORD, KEYWORD_ASSIGN))); }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            children.get(0).genAsmCode(asm, true);
            children.get(1).genAsmCode(asm, !children.get(1).type.isScalar());
            Type type = children.get(1).type;
            type = type.category == Category.FUNCTION ? ((FunctionType)type).returnType : type;
            switch (type.category) {
                case INT:
                    asm.add(CommandAsm.CommandType.POP, RegisterType.EAX);
                    asm.add(CommandAsm.CommandType.POP, RegisterType.EBX);
                    asm.add(CommandAsm.CommandType.MOV, DataType.DWORD, RegisterType.EBX, 0, RegisterType.EAX);
                    return;
                case CHAR:
                    asm.add(CommandType.MOV, RegisterType.AL, DataType.BYTE, RegisterType.ESP, 0);
                    asm.add(CommandAsm.CommandType.ADD, RegisterType.ESP, 1);
                    asm.add(CommandAsm.CommandType.POP, RegisterType.EBX);
                    asm.add(CommandAsm.CommandType.MOV, DataType.BYTE, RegisterType.EBX, 0, RegisterType.AL);
                    return;
                case DOUBLE:
                    asm.add(CommandAsm.CommandType.MOVSD, RegisterType.XMM0, DataType.QWORD, RegisterType.ESP, 0);
                    asm.add(CommandAsm.CommandType.ADD, RegisterType.ESP, 8);
                    asm.add(CommandAsm.CommandType.POP, RegisterType.EAX);
                    asm.add(CommandAsm.CommandType.MOVSD, DataType.QWORD, RegisterType.EAX, 0, RegisterType.XMM0);
                    return;
                case ARRAY:
                case RECORD:
                    asm.add(CommandAsm.CommandType.POP, RegisterType.EAX);
                    asm.add(CommandAsm.CommandType.POP, RegisterType.EBX);
                    asm.add(CommandAsm.CommandType.MOV, RegisterType.ECX, children.get(1).type.getSize() / 4);
                    String label = asm.getLabelName("COPYSTRUCT");
                    asm.add(CommandAsm.CommandType.LABEL, label);
                    asm.add(CommandAsm.CommandType.MOV, RegisterType.EDX, DataType.DWORD, RegisterType.EAX, 0);
                    asm.add(CommandAsm.CommandType.MOV, DataType.DWORD, RegisterType.EBX, 0, RegisterType.EDX);
                    asm.add(CommandAsm.CommandType.ADD, RegisterType.EAX, 4);
                    asm.add(CommandAsm.CommandType.ADD, RegisterType.EBX, 4);
                    asm.add(CommandAsm.CommandType.LOOP, label);
                    return;
            }
        }
    }

    public class IfNode extends Node {
        public IfNode() { super(getTmpLists(2), new Token("if", new Pair(TokenType.KEYWORD, KEYWORD_IF))); }

        public Node getIf_() { return this.children.get(0); }

        public Node getThen_() { return this.children.get(1); }

        public Node getElse_() { return children.size() > 2 ? this.children.get(2) : null; }

        public void setIf_(Node if_) { this.children.set(0, if_); }

        public void setThen_(Node then_) { this.children.set(1, then_); }

        public void setElse_(Node else_) {
            this.children.add(null);
            //else_.token = new Token("else", new Pair(TokenType.IDENTIFIER, VARIABLE));
            this.children.set(2, else_);
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            String elseLabel = asm.getLabelName("IFFAIL");
            String endLabel = asm.getLabelName("IFEND");
            getIf_().genAsmCode(asm, false);
            asm.add(CommandType.POP, RegisterType.EAX);
            asm.add(CommandType.TEST, RegisterType.EAX, RegisterType.EAX);
            asm.add(CommandType.JZ, elseLabel);
            if (getThen_() != null)
                getThen_().genAsmCode(asm, false);
            asm.add(CommandType.JMP, endLabel);
            asm.add(CommandType.LABEL, elseLabel);
            if (getElse_() != null)
                getElse_().genAsmCode(asm, false);
            asm.add(CommandType.LABEL, endLabel);
        }
    }

    public class WhileNode extends Node {
        public WhileNode() { super(getTmpLists(2), new Token("while", new Pair(TokenType.IDENTIFIER, VARIABLE))); }

        public Node getCondition() { return this.children.get(0); }

        public Node getBody() { return this.children.get(1); }

        public void setCondition(Node condition) { this.children.set(0, condition); }

        public void setBody(Node body) { this.children.set(1, body); }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            String bodyLabel = asm.getLabelName("WHILEBODY");
            String condLabel = asm.getLabelName("WHILECOND");
            String endLabel  = asm.getLabelName("WHILEEND");
            asm.pushLoopStart(condLabel);
            asm.pushLoopEnd(endLabel);
            asm.add(CommandType.JMP, condLabel);
            asm.add(CommandType.LABEL, bodyLabel);
            getBody().genAsmCode(asm, false);
            asm.add(CommandType.LABEL, condLabel);
            getCondition().genAsmCode(asm, false);
            asm.add(CommandType.POP, RegisterType.EAX);
            asm.add(CommandType.TEST, RegisterType.EAX, RegisterType.EAX);
            asm.add(CommandType.JNZ, bodyLabel);
            asm.add(CommandType.LABEL, endLabel);
            asm.popLoopStart();
            asm.popLoopEnd();
        }
    }

    public class ForNode extends Node {
        public SymTable.Symbol counter;
        public boolean isDownTo = false;

        public ForNode(SymTable.Symbol counter, Token token) {
            super(getTmpLists(5), token);
            this.counter = counter;
        }

        public Node getFrom() { return this.children.get(1); }

        public Node getTo() { return this.children.get(3); }

        public Node getBody() { return this.children.get(4); }

        public void setFromName(Node fromName) { this.children.set(0, fromName); }

        public void setFrom(Node from) { this.children.set(1, from); }

        private void setToName(Token to) {this.children.set(2, new VarNode(to, NIL())); }

        public void setTo(Node to) { this.children.set(3, to); }

        public void setBody(Node body) { this.children.set(4, body); }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            //asm.add(CommandType.SUB, RegisterType.ESP, 12);
            String bodyLabel = asm.getLabelName("LOOPBODY");
            String condLabel = asm.getLabelName("LOOPCOND");
            String endLabel  = asm.getLabelName("LOOPEND");
            asm.incForCount();
            asm.pushLoopStart(condLabel);
            asm.pushLoopEnd(endLabel);
            getTo().genAsmCode(asm, false);
            getFrom().genAsmCode(asm, false);
            javafx.util.Pair<Integer, Integer> offset = asm.getOffset(children.get(0).token.getText().toLowerCase());
            asm.add(CommandType.POP, RegisterType.EAX);
            asm.add(CommandType.MOV, RegisterType.EBX, DataType.DWORD, RegisterType.EBP, offset.getKey());
            asm.add(CommandType.MOV, DataType.DWORD, RegisterType.EBX, -offset.getValue(), RegisterType.EAX);
            asm.add(isDownTo ? CommandType.INC : CommandType.DEC, DataType.DWORD, RegisterType.EBX, -offset.getValue());
            asm.add(CommandType.JMP, condLabel);
            asm.add(CommandType.LABEL, bodyLabel);
            if (children.size() == 5 && getBody() != null)
                getBody().genAsmCode(asm, false);
            asm.add(CommandType.LABEL, condLabel);
            asm.add(CommandType.MOV, RegisterType.EBX, DataType.DWORD, RegisterType.EBP, offset.getKey());
            asm.add(isDownTo ? CommandType.DEC : CommandType.INC, DataType.DWORD, RegisterType.EBX, -offset.getValue());
            asm.add(CommandType.MOV, RegisterType.EAX, DataType.DWORD, RegisterType.ESP, 0);
            asm.add(CommandType.CMP, DataType.DWORD, RegisterType.EBX, -offset.getValue(), RegisterType.EAX);
            asm.add(isDownTo ? CommandType.JGE : CommandType.JLE, bodyLabel);
            asm.add(CommandType.LABEL, endLabel);
            asm.add(CommandType.ADD, RegisterType.ESP, 4);
            //asm.add(CommandType.ADD, RegisterType.ESP, 16);
            asm.popLoopStart();
            asm.popLoopEnd();
            asm.decForCount();
        }
    }

    private class IndexNode extends Node {
        public IndexNode(ArrayList<Node> children, Type type) {
            super(children, new Token("[]", new Pair(TokenType.SEPARATOR, SEP_BRACKETS_SQUARE_LEFT)));
            this.type = type;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            children.get(1).genAsmCode(asm, false);
            children.get(0).genAsmCode(asm, true);
            asm.add(CommandType.POP, RegisterType.ECX);
            asm.add(CommandType.POP, RegisterType.EAX);
            Type variableType = children.get(0).type;
            ConstNode min = ((ArrayType)variableType).min;
            int minValue = Integer.parseInt(min.result.toString());
            //int index = Integer.parseInt(((ConstNode)children.get(1)).result.toString());
            if (minValue != 0)
                asm.add(CommandType.SUB, RegisterType.EAX, minValue);
            ArrayType arrayType = (ArrayType)children.get(0).type;
            asm.add(CommandType.MOV, RegisterType.EBX, arrayType.elementType.getSize());
            asm.add(CommandType.IMUL, RegisterType.EBX);
            asm.add(CommandType.ADD, RegisterType.ECX, RegisterType.EAX);
            asm.add(CommandType.PUSH, RegisterType.ECX);
            if (isLeft)
                return;
            putValueOnStack(asm, type);
        }
    }

    private class FieldAccessNode extends Node {
        public FieldAccessNode(ArrayList<Node> children, Type type) {
            super(children, new Token(".", new Pair(TokenType.SEPARATOR, SEP_DOT)));
            this.type = type;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            children.get(0).genAsmCode(asm, true);
            Type variableType = children.get(0).type;
            int offset = ((RecordType)variableType).getRecordOffset(children.get(1).token, children.get(1).type);
            if (offset != 0)
                asm.add(CommandType.ADD, DataType.DWORD, RegisterType.ESP, 0, offset);
            if (isLeft)
                return;
            putValueOnStack(asm, type);
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

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            for (Node node : children)
                node.genAsmCode(asm, isLeft);
        }
    }

    private class CastNode extends Node {
        Type oldType = null;

        public CastNode(Node to, Type oldType, Type newType, Token token) {
            super(getList(to), token);
            //to.type = newType;
            this.oldType = oldType;
            this.type = newType;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            children.get(0).genAsmCode(asm, false);
            Type baseType = children.get(0).type;
            Type resultType = type;
            switch (resultType.category) {
                case CHAR:
                    switch (baseType.category) {
                        case CHAR:
                            return;
                        case INT:
                            asm.add(CommandType.POP, RegisterType.EAX);
                            asm.add(CommandType.SUB, RegisterType.ESP, 1);
                            asm.add(CommandType.MOV, DataType.BYTE, RegisterType.ESP, 0, RegisterType.AL);
                            return;
                        case DOUBLE:
                            asm.add(CommandType.CVTTSD2SI, RegisterType.EAX, DataType.QWORD, RegisterType.ESP, 0);
                            asm.add(CommandType.ADD, RegisterType.ESP, 7);
                            asm.add(CommandType.MOV, RegisterType.ESP,  DataType.BYTE, RegisterType.AL, 0);
                            return;
                        default:
                            return; // :-)
                    }
                case INT:
                    switch (baseType.category) {
                        case CHAR:
                            asm.add(CommandType.MOVSX, RegisterType.EAX, DataType.BYTE, RegisterType.ESP, 0);
                            asm.add(CommandType.SUB, RegisterType.ESP, 3);
                            asm.add(CommandType.MOV,  DataType.DWORD, RegisterType.ESP, 0, RegisterType.EAX);
                            return;
                        case INT:
                            return;
                        case DOUBLE:
                            asm.add(CommandType.CVTTSD2SI, RegisterType.EAX, DataType.QWORD, RegisterType.ESP, 0);
                            asm.add(CommandType.ADD, RegisterType.ESP, 4);
                            asm.add(CommandType.MOV, DataType.DWORD, RegisterType.ESP, 0, RegisterType.EAX);
                            break;
                        default:
                            return; // :-)
                    }
                case DOUBLE:
                    switch (baseType.category) {
                        case CHAR:
                            asm.add(CommandType.MOVSX, RegisterType.EAX, DataType.BYTE, RegisterType.ESP, 0);
                            asm.add(CommandType.SUB, RegisterType.ESP, 7);
                            break;
                        case INT:
                            asm.add(CommandType.MOV, RegisterType.EAX, DataType.DWORD, RegisterType.ESP, 0);
                            asm.add(CommandType.SUB, RegisterType.ESP, 4);
                            break;
                        case DOUBLE:
                            return;
                        default:
                            return; // :-)
                    }
                    asm.add(CommandType.cvtsi2sd, RegisterType.XMM0, RegisterType.EAX);
                    asm.add(CommandType.MOVSD, DataType.QWORD, RegisterType.ESP, 0, RegisterType.XMM0);
                    return;
                default:
                    return; // :-)
            }
        }
    }

    private class WriteNode extends Node {

        public WriteNode(Token token) { super(new ArrayList<>(), token); }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            int size = 4;
            StringBuilder builder = new StringBuilder();
            for (Node node : children) {
                Type type = node.type;
                if (type.category == Category.FUNCTION)
                    type = ((FunctionType)type).returnType;
                switch (type.category) {
                    case INT:
                        size += 4;
                        builder.append("%d");
                        break;
                    case DOUBLE:
                        size += 8;
                        builder.append("%f");
                        break;
                    case CHAR:
//                        if (node.token.getText().length() > 1)
//                            builder.append("%s");
//                        else
                            builder.append("%c");
                        size += 4;
                        break;
                }
            }
            int al_size = (16 - (size + 4 * asm.getLoopForCount()) % 16) % 16;
            if (al_size > 0)
                asm.add(CommandAsm.CommandType.SUB, RegisterType.ESP, al_size);
            for (int i = children.size() - 1; i >= 0; i--) {
                Node child = children.get(i);
                Type type = child.type;
                if (type.category == Category.FUNCTION)
                    type = ((FunctionType)type).returnType;
                child.genAsmCode(asm, false);
                if (type.category == Category.CHAR) {
                    asm.add(CommandAsm.CommandType.MOVSX, RegisterType.EAX, DataType.BYTE, RegisterType.ESP, 0);
                    asm.add(CommandAsm.CommandType.ADD, RegisterType.ESP, 1);
                    asm.add(CommandAsm.CommandType.PUSH, RegisterType.EAX);
                }
            }
            builder.append(Character.toString((char)10)); // for line break
            String format = asm.addStringConstant(builder.toString());
            asm.add(CommandAsm.CommandType.PUSH, format);
            asm.add(CommandAsm.CommandType.CALL, "_printf");
            asm.add(CommandAsm.CommandType.ADD, RegisterType.ESP, size + al_size);
        }
    }

    private class FunctionCallNode extends Node {
        public FunctionCallNode(Node varNode) {
            super(getList(varNode), new Token("()", new Pair(TokenType.UNDEFINED, VARIABLE)));
            this.type = ((FunctionType)varNode.type).returnType; // returned type
//            if (this.type.category == Category.ARRAY)
//                this.type = ((ArrayType)type).elementType;
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            FunctionType functionType = (FunctionType)children.get(0).type;
            int i = 0;
            Iterator it = functionType.params.symTable.entrySet().iterator();
            for (; i < children.get(1).children.size(); i++) { // function parameters
                Map.Entry<String, SymTable.Symbol> pair = (Map.Entry)it.next();
                if (pair.getValue().isConst || pair.getValue().isPointerParam)
                    children.get(1).children.get(i).genAsmCode(asm, pair.getValue().isPointerParam);
                else
                    children.get(1).children.get(i).genAsmCode(asm, false);
            }
            while (it.hasNext()) {
                Map.Entry<String, SymTable.Symbol> pair = (Map.Entry)it.next();
                pair.getValue().value.genAsmCode(asm, false);
            }
            asm.add(CommandType.CALL, asm.getFunctionName(children.get(0).token.getText().toLowerCase()));
            switch (functionType.returnType.category) {
                case CHAR:
                    asm.add(CommandType.SUB, RegisterType.ESP, 1);
                    asm.add(CommandAsm.CommandType.MOV, DataType.BYTE, RegisterType.ESP, 0, RegisterType.AL);
                    return;
                case INT:
                    asm.add(CommandType.PUSH, RegisterType.EAX);
                    return;
                case DOUBLE:
                    asm.add(CommandType.SUB, RegisterType.ESP, 8);
                    asm.add(CommandType.MOVSD, DataType.QWORD, RegisterType.ESP, 0, RegisterType.XMM0);
                    return;
                case ARRAY:
                case RECORD:
                    asm.add(CommandType.PUSH, "__temp@var");
                    return;
                case NIL:
                    return;
                default:
                    return;
            }
        }
    }

    private class ContinueNode extends Node {
        public ContinueNode(Token token) {
            super(null, token);
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            asm.add(CommandType.JMP, asm.getLoopStarts().peek());
        }
    }

    private class BreakNode extends Node {
        public BreakNode(Token token) {
            super(null, token);
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            asm.add(CommandType.JMP, asm.getLoopEnds().peek());
        }
    }

    private class ExitNode extends Node {
        public ExitNode(Token token) {
            super(new ArrayList<>(), token);
        }

        @Override
        public void genAsmCode(CodeAsm asm, boolean isLeft) {
            Type resultFunctionType = asm.getCurrFunctionResultType();
            if (children.size() > 0) {
                children.get(0).genAsmCode(asm, !resultFunctionType.isScalar());
                switch (resultFunctionType.category) {
                    case CHAR:
                        asm.add(CommandType.MOV, RegisterType.AL, DataType.BYTE, RegisterType.ESP, 0);
                        asm.add(CommandType.ADD, RegisterType.ESP, 1);
                        break;
                    case INT:
                        asm.add(CommandType.POP, RegisterType.EAX);
                        break;
                    case DOUBLE:
                        asm.add(CommandType.MOVSD, RegisterType.XMM0, DataType.QWORD, RegisterType.ESP, 0);
                        asm.add(CommandType.ADD, RegisterType.ESP, 8);
                        break;
                    case NIL:
                        break;
                    case ARRAY:
                    case RECORD:
                        asm.add(CommandType.MOV, RegisterType.EBX, "__temp@var");
                        asm.add(CommandType.POP, RegisterType.EAX);
                        asm.add(CommandType.MOV, RegisterType.ECX, resultFunctionType.getSize() / 4);
                        String label = asm.getLabelName("COPYSTRUCT");
                        asm.add(CommandType.LABEL, label);
                        asm.add(CommandType.MOV, RegisterType.EDX, DataType.DWORD, RegisterType.EAX, 0);
                        asm.add(CommandType.MOV, DataType.DWORD, RegisterType.EBX, 0, RegisterType.EDX);
                        asm.add(CommandType.ADD, RegisterType.EAX, 4);
                        asm.add(CommandType.ADD, RegisterType.EBX, 4);
                        asm.add(CommandType.LOOP, label);
                        break;
                }
            } else {
                javafx.util.Pair<Integer, Integer> offset = asm.getOffset("result");
                switch (resultFunctionType.category) {
                    case CHAR:
                        asm.add(CommandType.MOV, RegisterType.AL, DataType.BYTE, RegisterType.EBP, -offset.getValue());
                        break;
                    case INT:
                        asm.add(CommandType.MOV, RegisterType.EAX, DataType.DWORD, RegisterType.EBP, -offset.getValue());
                        break;
                    case DOUBLE:
                        asm.add(CommandType.MOVSD, RegisterType.XMM0, DataType.QWORD, RegisterType.EBP, -offset.getValue());
                        break;
                    case NIL:
                        break;
                    case ARRAY:
                    case RECORD:
                        asm.add(CommandType.MOV, RegisterType.EBX, "__temp@var");
                        asm.add(CommandType.LEA, RegisterType.EAX, RegisterType.EBP, -offset.getValue());
                        asm.add(CommandType.MOV, RegisterType.ECX, resultFunctionType.getSize() / 4);
                        String label = asm.getLabelName("COPYSTRUCT");
                        asm.add(CommandType.LABEL, label);
                        asm.add(CommandType.MOV, RegisterType.EDX, DataType.DWORD, RegisterType.EAX, 0);
                        asm.add(CommandType.MOV, DataType.DWORD, RegisterType.EBX, 0, RegisterType.EDX);
                        asm.add(CommandType.ADD, RegisterType.EAX, 4);
                        asm.add(CommandType.ADD, RegisterType.EBX, 4);
                        asm.add(CommandType.LOOP, label);
                        break;
                }
            }
            asm.add(CommandType.LEAVE);
            asm.add(CommandType.RET, asm.getCurrentFunctionParamSize());
        }
    }

    private class ReadNode extends Node {
        public ReadNode(Token token) { super(new ArrayList<>(), token); }
    }
}
