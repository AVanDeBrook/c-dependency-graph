
package depgraph.Parser;

import java.util.HashMap;

public enum TokenType {

	// @formatter:off
	NONE(0, null),
	IGNORED(1, null),
	DIGRAPH_DEF(2, "digraph"),
	NODE_ATTR_STMT(3, "node"),
	EDGE_ATTR_STMT(4, "edge"),
	NODE_STMT(5, null),
	EDGE_STMT(6, null),
	L_BRACE(7, null),
	R_BRACE(8, null);
	// @formatter:on

	private TokenType(Integer id, String keyword) {
		this.id = id;
		this.keyword = keyword;
	}

	private Integer id;
	private String keyword;

	private static final HashMap<Integer, TokenType> idToTokenTypeLookup = new HashMap<Integer, TokenType>();
	private static final HashMap<TokenType, Integer> tokenTypeToIdLookup = new HashMap<TokenType, Integer>();
	private static final HashMap<String, TokenType> keywordToTokenTypeLookup = new HashMap<String, TokenType>();
	static {
		for (TokenType type : TokenType.values()) {
			idToTokenTypeLookup.put(type.getId(), type);
			tokenTypeToIdLookup.put(type, type.getId());
			keywordToTokenTypeLookup.put(type.getKeyword(), type);
		}
	}

	/**
	 * Gets a TokenType if the provided string is a keyword.
	 * 
	 * @param word - The word to search for
	 * @return A TokenType if the provided string was a keyword, null otherwise
	 */
	public static TokenType getTokenTypeIfKeyword(String word) {
		return keywordToTokenTypeLookup.get(word);
	}

	public static TokenType getTokenTypeFromId(Integer id) {
		return idToTokenTypeLookup.get(id);
	}

	public static Integer getIdFromTokenType(TokenType type) {
		return tokenTypeToIdLookup.get(type);
	}

	public Integer getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}
}