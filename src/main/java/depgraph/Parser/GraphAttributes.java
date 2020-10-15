
package depgraph.Parser;

import java.util.HashMap;

public enum GraphAttributes {

	// @formatter:off
	FONTNAME(0, "fontname"),
	FONTSIZE(1, "fontsize"),
	LABELFONTNAME(2, "labelfontname"),
	LABELFONTSIZE(3, "labelfontsize"),
	SHAPE(4, "shape");
	// @formatter:on

	private GraphAttributes(Integer id, String keyword) {
		this.id = id;
		this.keyword = keyword;
	}

	private Integer id;
	private String keyword;

	private static final HashMap<Integer, GraphAttributes> idToTokenTypeLookup = new HashMap<Integer, GraphAttributes>();
	private static final HashMap<GraphAttributes, Integer> tokenTypeToIdLookup = new HashMap<GraphAttributes, Integer>();
	private static final HashMap<String, GraphAttributes> keywordToTokenTypeLookup = new HashMap<String, GraphAttributes>();
	static {
		for (GraphAttributes type : GraphAttributes.values()) {
			idToTokenTypeLookup.put(type.getId(), type);
			tokenTypeToIdLookup.put(type, type.getId());
			keywordToTokenTypeLookup.put(type.getKeyword(), type);
		}
	}


	public static GraphAttributes getTokenTypeIfKeyword(String word) {
		return keywordToTokenTypeLookup.get(word);
	}

	public static GraphAttributes getTokenTypeFromId(Integer id) {
		return idToTokenTypeLookup.get(id);
	}

	public static Integer getIdFromTokenType(GraphAttributes type) {
		return tokenTypeToIdLookup.get(type);
	}

	public Integer getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}
}