package depgraph.Parser;

import java.util.Arrays;
import java.util.regex.Pattern;

public class Lexer {
	private Pattern nodePattern;
	private Pattern nodePattern2;
	private Pattern edgePattern;
	private Pattern edgePattern2;
	private String endOfLine;

	/**
	 * Initializes identifier regex patterns and EOL attribute to null.
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
	 * @param line - A single line from the file to tokenize.
	 * @return Token with relevant data from the tokenized line.
	 * @see Token, Parser
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

			if (charsInLine[i] != ' ' && charsInLine[i] != '\n') {
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

			} else if (TokenType.getTokenTypeIfKeyword(buffer) != null) {

				tokenType = TokenType.getTokenTypeIfKeyword(buffer);
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
	 * @param arr - Subset of the char array from the tokenize function.
	 * @return String representing the "word" found.
	 */
	private String lookAhead(char[] arr) {
		String buffer = "";

		for (char c : arr) {
			if (c != ' ' && c != '\n' && c != ';' && c != 0) {
				buffer += c;
			}
		}

		return buffer;
	}

	/**
	 * If part of the string in the buffer was not returned, it will be saved
	 * here.
	 *
	 * @return rest of the string in buffer until the EOL (e.g. EDGE_STMTs
	 * usually have an attribute string in brackets afterwards. That can be
	 * retrieved with this function).
	 */
	public String getEndOfLine() {
		return this.endOfLine;
	}
}
