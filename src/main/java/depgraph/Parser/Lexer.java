package depgraph.Parser;

import java.util.Arrays;
import java.util.regex.*;

public class Lexer {
    public final String[] keywords = {
        "digraph", "node", "edge"
    };

    private Pattern nodeIdPattern = Pattern.compile("Node[0-9]+");

    public Lexer() { }

    /**
     * Core of the Lexer class. Scans and tokenizes a string and returns
     * a Token with the relevant info from that line.
     *
     * @param line Line from the file to tokenize.
     * @return Token with relevant data from the tokenized line.
     * @see Token
     */
    public Token tokenize(String line) {
        char[] lineArray = line.toCharArray();
        String buffer = "";
        Token token = null;

        for (int i = 0; i < lineArray.length; i++) {
            if (lineArray[i] != ' ' && lineArray[i] != '\n') {
                buffer += lineArray[i];
            }

            if (this.checkKeyword(buffer)) {
                TokenTypeEnum tokenType = TokenTypeEnum.NONE;
                // decide what to do for each keyword
                if (buffer.equals("digraph")) {
                    tokenType = TokenTypeEnum.DIGRAPH_DEF;
                } else if (buffer.equals("node")) {
                    tokenType = TokenTypeEnum.NODE_ATTR_STMT;
                } else if (buffer.equals("edge")) {
                    tokenType = TokenTypeEnum.EDGE_ATTR_STMT;
                }
                // look ahead 1
                String tempValue = this.lookAhead(Arrays.copyOfRange(lineArray, i+1, lineArray.length));
                token = new Token(tokenType, tempValue);
            } else if (nodeIdPattern.matcher(buffer).find()) {
                // check Node statement
                // look ahead 1 to see if it's an edge stmt
                token = new Token(TokenTypeEnum.NODE_STMT, "NOT IMPLEMENTED");
            }

            if (lineArray[i] == '{') {
                token = new Token(TokenTypeEnum.L_BRACKET, String.format("%c", lineArray[i]));
            } else if (lineArray[i] == '}') {
                token = new Token(TokenTypeEnum.R_BRACKET, String.format("%c", lineArray[i]));
            }
        }

        return token;
    }

    /**
     * Simple linear search for a keyword match.
     *
     * @param word keyword to search for.
     * @return True if word is a keyword, false otherwise.
     */
    private boolean checkKeyword(String word) {
        boolean match = false;

        for (String keyword : keywords) {
            if (word.equals(keyword)) {
                match = true;
            }
        }

        return match;
    }

    /**
     * Scans forward by one "word" and returns the result.
     *
     * @param arr Subset of the char array from the tokenize function.
     * @return String representing the "word" found.
     */
    private String lookAhead(char[] arr) {
        String buffer = "";

        for (char c : arr) {
            if (c == ' ' || c == '\n' || c == ';') {
                break;
            } else {
                buffer += c;
            }
        }

        return buffer;
    }
}
