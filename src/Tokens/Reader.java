package Tokens;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;

public class Reader {

    public int xPos = 0;
    public int yPos = 0;

    private BufferedReader reader;
    private String currentString = "";


    public Reader(String fileName) {
        try {
            reader = new BufferedReader(new FileReader(fileName));
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }

    private boolean nextLine() {
        try {
            if (reader.ready()) {
                currentString = reader.readLine();
                xPos++;
                return true;
            } else {
                // ....
                currentString = "";
                return false;
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }

    public char getChar() {
        if (currentString.length() - Math.abs(yPos) <= 0) {
            if (!nextLine())
                return '\0';
            yPos = 0;
        }
        //char c = !currentString.equals("") ? currentString.charAt(yPos++) : '\n';
        //if (c == ' ')
        return !currentString.equals("") ? currentString.charAt(yPos++) : '\n';
    }

    public boolean lookAhead() {
        return currentString.length() - yPos != 0;
    }

    public void singleCharacterRollback() {
        yPos--;
    }

    public boolean endOfLine() { return currentString.length() - Math.abs(yPos) <= 0; }

    public char previousCharacter() {
        return  currentString.charAt(yPos - 3);
    }

    public void markCommentLineDoubleSlash() {
        yPos = currentString.length();
    }

    @Override
    protected void finalize() throws Throwable {
        reader.close();
    }
}
