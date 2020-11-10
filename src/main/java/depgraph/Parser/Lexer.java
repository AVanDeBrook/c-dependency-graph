package depgraph.Parser;

import java.util.Arrays;
import java.util.regex.Pattern;

/**
 * The Lexer class, specifically the tokenize function, reduces the passed line
 * down to one Token with the necessary information for the Parser to do its
 * job. It does so by determining the type of declaration from the DOT language
 * grammar.
 *
 * The tokenize function goes through each line character-by-character and
 * builds up a buffer of what has been read until it matches a known pattern
 * from the DOT language grammar.
 */
public class Lexer {
	/**
	 * Regex pattern to check for node_stmts with attribute lists.
	 */
	private Pattern nodePattern;

	/**
	 * Regex pattern to check for node_stmts without attribute lists.
	 */
	private Pattern nodePattern2;

	/**
	 * Regex pattern to check for edge_stmts with attribute lists.
	 */
	private Pattern edgePattern;

	/**
	 * Regex pattern to check for edge_stmts without attribute lists.
	 */
	private Pattern edgePattern2;

	/**
	 * Used for some statements where the attribute string may have been omitted
	 * in the Token value.
	 *
	 * May change this in the future to grab the attribute string for every
	 * value, so that the calling context can grab it if needed.
	 */
	private String endOfLine;

	/**
	 * Constructor that initializes identifier regex patterns and EOL attribute
	 * to null.
	 */
	public Lexer() {
		nodePattern = Pattern.compile("^Node\\d+\\[");
		nodePattern2 = Pattern.compile("^Node\\d+;");
		edgePattern = Pattern.compile("^Node\\d+->Node\\d+\\[");
		edgePattern2 = Pattern.compile("^Node\\d+->Node\\d+;");
		endOfLine = null;
	}

	/**
	 * Core of the Lexer class. Scans and tokenizes a line and returns a Token
	 * object with the relevant info from that line.
	 *
	 * @param line A single line from the file to tokenize.
	 * @return Token with relevant data from the tokenized line.
	 */
	public Token tokenize(String line) {
		char[] charsInLine = line.toCharArray();
		String buffer = "";
		TokenType tokenType = TokenType.NONE;
		String tokenValue = null;
		Token token = null;
		if (endOfLine != null)
			endOfLine = null;

		for (int i = 0; i < charsInLine.length; i++) {

			if (charsInLine[i] != ' ' && charsInLine[i] != '\n' && charsInLine[i] != '\"') {
				buffer += charsInLine[i];
			}

			if (charsInLine[i] == '{') {

				tokenType = TokenType.L_BRACE;
				tokenValue = String.format("%s", charsInLine[i]);
				token = new Token(tokenType, tokenValue);
				return token;

			} else if (charsInLine[i] == '}') {

				tokenType = TokenType.R_BRACE;
				tokenValue = String.format("%s", charsInLine[i]);
				token = new Token(tokenType, tokenValue);
				return token;

			} else if (TokenType.getTypeIfKeyword(buffer) != null) {

				tokenType = TokenType.getTypeIfKeyword(buffer);
				tokenValue = this.lookAhead(Arrays.copyOfRange(charsInLine, i + 1, charsInLine.length));
				token = new Token(tokenType, tokenValue);
				return token;

			} else if (nodePattern.matcher(buffer).find() || nodePattern2.matcher(buffer).find()) {

				tokenType = TokenType.NODE_STMT;
				String rhs = this.lookAhead(Arrays.copyOfRange(charsInLine, i + 1, charsInLine.length));
				tokenValue = buffer + rhs;
				token = new Token(tokenType, tokenValue);
				return token;

			} else if (edgePattern.matcher(buffer).find() || edgePattern2.matcher(buffer).find()) {

				tokenType = TokenType.EDGE_STMT;
				String rhs = this.lookAhead(Arrays.copyOfRange(charsInLine, i + 1, charsInLine.length));
				tokenValue = buffer + rhs;
				token = new Token(tokenType, tokenValue);
				return token;
			}
		}

		if (token == null)
			token = new Token(TokenType.IGNORED, buffer);

		return token;
	}

	/**
	 * Scans forward by one "word" and returns the result. Skips white space and
	 * semi-colons (end of line char).
	 *
	 * @param arr Subset of the char array from the tokenize function.
	 * @return String representing the "word" found.
     * @deprecated
	 */
    @Deprecated
	private String lookAhead(char[] arr) {
		String buffer = "";

		for (char c : arr) {
			if (c != ' ' && c != '\n' && c != ';' && c != 0 && c != '\"') {
				buffer += c;
			}
		}

		return buffer;
	}

	// TODO Not in use, Aaron has ideas to reevaluate this
	/**
	 * If part of the string in the buffer was not returned, it will be saved
	 * here.
	 *
	 * @return rest of the string in buffer until the EOL aka ';' (e.g.
	 * EDGE_STMTs usually have an attribute string in brackets afterwards. That
	 * can be retrieved with this function).
	 */
	public String getEndOfLine() {
		return this.endOfLine;
	}
}
