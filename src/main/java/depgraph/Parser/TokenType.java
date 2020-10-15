
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

	private static final HashMap<Integer, TokenType> idToTypeLookup = new HashMap<Integer, TokenType>();
	private static final HashMap<TokenType, Integer> typeToIdLookup = new HashMap<TokenType, Integer>();
	private static final HashMap<String, TokenType> keywordToTypeLookup = new HashMap<String, TokenType>();
	static {
		for (TokenType type : TokenType.values()) {
			idToTypeLookup.put(type.getId(), type);
			typeToIdLookup.put(type, type.getId());
			keywordToTypeLookup.put(type.getKeyword(), type);
		}
	}

	/**
	 * Gets a TokenType if the provided string is a keyword.
	 * 
	 * @param word - The word to search for
	 * @return A TokenType if the provided string was a keyword, null otherwise
	 */
	public static TokenType getTypeIfKeyword(String word) {
		return keywordToTypeLookup.get(word);
	}

	public static TokenType getTypeFromId(Integer id) {
		return idToTypeLookup.get(id);
	}

	public static Integer getIdFromType(TokenType type) {
		return typeToIdLookup.get(type);
	}

	public Integer getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}
}