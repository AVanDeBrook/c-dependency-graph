
package depgraph.Parser;

import java.util.HashMap;

public enum NodeAttributeType {

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

	private NodeAttributeType(Integer id, String keyword) {
		this.id = id;
		this.keyword = keyword;
	}

	private Integer id;
	private String keyword;

	private static final HashMap<Integer, NodeAttributeType> idToTypeLookup = new HashMap<Integer, NodeAttributeType>();
	private static final HashMap<NodeAttributeType, Integer> typeToIdLookup = new HashMap<NodeAttributeType, Integer>();
	private static final HashMap<String, NodeAttributeType> keywordToTypeLookup = new HashMap<String, NodeAttributeType>();
	static {
		for (NodeAttributeType type : NodeAttributeType.values()) {
			idToTypeLookup.put(type.getId(), type);
			typeToIdLookup.put(type, type.getId());
			keywordToTypeLookup.put(type.getKeyword(), type);
		}
	}

	public static NodeAttributeType getTypeIfKeyword(String word) {
		return keywordToTypeLookup.get(word);
	}

	public static NodeAttributeType getTypeFromId(Integer id) {
		return idToTypeLookup.get(id);
	}

	public static Integer getIdFromType(NodeAttributeType type) {
		return typeToIdLookup.get(type);
	}

	public Integer getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}
}