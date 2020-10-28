package depgraph.Parser;

/**
 *
 */
public class Token {
	/**
	 * Type of declaration that was found on the tokenized line.
	 * 
	 * @see TokenType
	 */
	private TokenType type;

	/**
	 * Value of the line that was tokenized.
	 */
	private String value;

	/**
	 * No-arg constructor. Initializes the object to null/zero state values.
	 */
	public Token() {
		this.type = TokenType.NONE;
		this.value = "";
	}

	/**
	 * Simple constructor for Token object creation. Initializes the type and
	 * value attribute fields.
	 *
	 * @param type Type of token, of TokenType enumeration
	 * @param value Actual value of the token. Usually an ID or attribute.
	 */
	public Token(TokenType type, String value) {
		this.type = type;
		this.value = value;
	}

	/* Setters and Getters */

	public TokenType getToken() {
		return type;
	}

	public String getValue() {
		return value;
	}

	public void setToken(TokenType type) {
		this.type = type;
	}

	public void setValue(String value) {
		this.value = value;
	}

	@Override
	public String toString() {
		return String.format("TYPE: %s\nVALUE: %s\n", this.type, this.value);
	}
}
