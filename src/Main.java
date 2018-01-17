import SyntacticalAnalyzer.ExpressionParser;
import SyntacticalAnalyzer.Parser;
import SyntacticalAnalyzer.SyntaxException;
import Tokens.Tokenizer;

import javax.script.ScriptException;
import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.util.ArrayList;

public class Main {

    public static void main(String[] args) {
//        try {
//            System.setOut(new PrintStream(
//                    new BufferedOutputStream(
//                            new FileOutputStream(
//                                    "/Users/ilyadonskoj/IdeaProjects/pascal_compiler/src/Test/Syntax/Parser/output.txt"))));
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        }
        if (args.length == 0) {
            System.out.println("Pascal fresh Compiler version 0.2 [2017/03/11] " +
                    "Copyright (c) 2017 by Donskoy Ilya");
            System.out.println("pascal_compiler.jar [options] <inputfile> " +
                    "use -l option to obtain a table of tokens" +
                    "use -s option to obtain a parse tree");
        }
        else if (args.length == 1)
            System.out.println("pascal_compiler.jar [options] <inputfile>" +
                    "use -l option to obtain a table of tokens" +
                    "use -s option to obtain a parse tree");
        else switch (args[0]) {
                case "-l":
                    try {
                        Tokenizer tokenizer = new Tokenizer(args[1]);
                        while (tokenizer.Next())
                            tokenizer.print();
                    } catch (Exception e) {
                        System.out.println(e.getMessage());
                    } finally {
                        System.out.close();
                    }
                    break;
                case "-s":
//                    try {
//                        ExpressionParser parser = new ExpressionParser(args[1]);
//                        ExpressionParser.Node node = parser.parse();
//                        node.print();
//                    } catch (SyntaxException e) {
//                        //e.printStackTrace();
//                        System.out.println(e.getMessage());
//                    } finally {
//                        System.out.close();
//                    }
//                    break;
                case "-t":
                    try {
                        Parser parser1 = new Parser(args[1]);
                        Parser.Type type = parser1.parse();
                        System.out.println(type);
                        //node.print();
                    } catch (SyntaxException e) {
                        //e.printStackTrace();
                        System.out.println(e.getMessage());
                    } catch (ScriptException e) {
                            //e.printStackTrace();
                            System.out.println(e.getMessage());
                    } finally {
                        System.out.close();
                    }
                    break;
                // some others args ....
            }
    }
}
