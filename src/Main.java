import Tokens.Tokenizer;

import java.io.BufferedReader;

public class Main {
    private static BufferedReader reader;

    public static void main(String[] args) {
        if (args.length == 0) {
            System.out.println("Pascal fresh Compiler version 0.1 [2017/10/08] " +
                    "Copyright (c) 2017 by Donskoy Ilya");
            System.out.println("pascal_compiler.jar [options] <inputfile> " +
                    "use -l option to obtain a table of tokens");
        }
        else if (args.length == 1)
            System.out.println("pascal_compiler.jar [options] <inputfile>"+
                    "use -l option to obtain a table of tokens");
        else switch (args[0]) {
                case "-l": {
                    Tokenizer tokenizer = new Tokenizer(args[1]);
                    while (tokenizer.Next()) {
                        tokenizer.print();
                    }
                }
            }

        // some others args ....
    }
}
