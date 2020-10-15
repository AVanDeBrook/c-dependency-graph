
package depgraph.Parser;

import java.util.HashMap;

public enum NodeAttributes {

	// @formatter:off
	LABEL(0, "label"),
	HEIGHT(1, "height"),
	WIDTH(2, "width"),
	COLOR(3, "color"),
    FILLCOLOR(4, "fillcolor"),
    STYLE(5, "style"),
    FONTCOLOR(6, "fontcolor"),
    FONTSIZE(7,"fontsize"),
    FONTNAME(8, "fontname"),
    URL(9, "URL");
	// @formatter:on

	private NodeAttributes(Integer id, String keyword) {
		this.id = id;
		this.keyword = keyword;
	}

	private Integer id;
	private String keyword;

	private static final HashMap<Integer, NodeAttributes> idToTokenTypeLookup = new HashMap<Integer, NodeAttributes>();
	private static final HashMap<NodeAttributes, Integer> tokenTypeToIdLookup = new HashMap<NodeAttributes, Integer>();
	private static final HashMap<String, NodeAttributes> keywordToTokenTypeLookup = new HashMap<String, NodeAttributes>();
	static {
		for (NodeAttributes type : NodeAttributes.values()) {
			idToTokenTypeLookup.put(type.getId(), type);
			tokenTypeToIdLookup.put(type, type.getId());
			keywordToTokenTypeLookup.put(type.getKeyword(), type);
		}
	}


	public static NodeAttributes getTokenTypeIfKeyword(String word) {
		return keywordToTokenTypeLookup.get(word);
	}

	public static NodeAttributes getTokenTypeFromId(Integer id) {
		return idToTokenTypeLookup.get(id);
	}

	public static Integer getIdFromTokenType(NodeAttributes type) {
		return tokenTypeToIdLookup.get(type);
	}

	public Integer getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}
}