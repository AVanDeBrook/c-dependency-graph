package depgraph.Parser;

public class Token {
    private TokenTypeEnum type;
    private String value;

    /**
     * No-arg constructor. Initializes the object to null values.
     */
    public Token() {
        this.type = TokenTypeEnum.NONE;
        this.value = "";
    }

    /**
     * Simple constructor for Token object creation.
     * Initializes the type and value attribute fields.
     *
     * @param type  (enum) type of token.
     * @param value (String) actual value of the token. Usually an ID or attribute.
     */
    public Token(TokenTypeEnum type, String value) {
        this.type = type;
        this.value = value;
    }

    public TokenTypeEnum getToken() {
        return type;
    }

    public String getValue() {
        return value;
    }

    public void setToken(TokenTypeEnum type) {
        this.type = type;
    }

    public void setValue(String value) {
        this.value = value;
    }

    @Override
    public String toString() {
        return String.format("Type: %s\nValue: %s\n", this.type, this.value);
    }
}
