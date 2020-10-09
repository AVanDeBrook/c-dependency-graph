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
        TokenTypeEnum tokenType = TokenTypeEnum.NONE;
        Token token = null;

        for (int i = 0; i < lineArray.length; i++) {
            if (lineArray[i] != ' ' && lineArray[i] != '\n') {
                buffer += lineArray[i];
            }

            if (this.checkKeyword(buffer)) {
                String tempValue = this.lookAhead(Arrays.copyOfRange(lineArray, i+1, lineArray.length));

                if (buffer.equals("digraph")) {
                    tokenType = TokenTypeEnum.DIGRAPH_DEF;
                } else if (buffer.equals("node")) {
                    tokenType = TokenTypeEnum.NODE_ATTR_STMT;
                } else if (buffer.equals("edge")) {
                    tokenType = TokenTypeEnum.EDGE_ATTR_STMT;
                }

                token = new Token(tokenType, tempValue);
                return token;
            } else if (nodeIdPattern.matcher(buffer).find()) {
                String rhs = this.lookAhead(Arrays.copyOfRange(lineArray, i+1, lineArray.length));
                String lookAheadString = this.lookAhead(Arrays.copyOfRange(lineArray, i+1, i+4));

                if (lookAheadString.charAt(0) == '[') {
                    tokenType = TokenTypeEnum.NODE_STMT;
                } else if (lookAheadString.equals("->")) {
                    tokenType = TokenTypeEnum.EDGE_STMT;
                    rhs = rhs.substring(0, rhs.indexOf("["));
                }

                token = new Token(tokenType, buffer + rhs);
                return token;
            }

            if (lineArray[i] == '{') {
                token = new Token(TokenTypeEnum.L_BRACE, String.format("%s", lineArray[i]));
            } else if (lineArray[i] == '}') {
                token = new Token(TokenTypeEnum.R_BRACE, String.format("%s", lineArray[i]));
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
                continue;
            } else {
                buffer += c;
            }
        }

        return buffer;
    }
}
