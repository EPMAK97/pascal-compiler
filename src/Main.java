import SyntacticalAnalyzer.ExpressionParser;
import Tokens.Tokenizer;
import apple.laf.JRSUIUtils;

import java.io.BufferedOutputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.PrintStream;

public class Main {

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Pascal fresh Compiler version 0.1 [2017/10/08] " +
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
                case "-l": {
//                    try {
//                        System.setOut(new PrintStream(
//                                new BufferedOutputStream(
//                                        new FileOutputStream(
//                                                "/Users/ilyadonskoj/IdeaProjects/pascal_compiler/src/Test/output.txt"))));
//                    } catch (FileNotFoundException e) {
//                        e.printStackTrace();
//                    }
//                    Tokenizer tokenizer = null;
//                    tokenizer = new Tokenizer(args[1]);
//                    while (tokenizer.Next()) {
//                        tokenizer.print();
//                    }
//                    System.out.close();
                }
                case "-s": {
                    ExpressionParser parser = new ExpressionParser(args[1]);
                    ExpressionParser.Node node = parser.parse();
                    node.print("");
                    //ExpressionParser.TreePrinter.print(node, "");

//                    Tokenizer tokenizer = new Tokenizer(args[1]);
//                    while (tokenizer.Next()) {
//                        tokenizer.print();
//                    }
                }
            }

        // some others args ....
    }
}
