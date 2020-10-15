
package depgraph.Parser;

import java.util.HashMap;

public enum GraphAttributeType {

	// @formatter:off
	FONTNAME(0, "fontname"),
	FONTSIZE(1, "fontsize"),
	LABELFONTNAME(2, "labelfontname"),
	LABELFONTSIZE(3, "labelfontsize"),
	SHAPE(4, "shape");
	// @formatter:on

	private GraphAttributeType(Integer id, String keyword) {
		this.id = id;
		this.keyword = keyword;
	}

	private Integer id;
	private String keyword;

	private static final HashMap<Integer, GraphAttributeType> idToTypeLookup = new HashMap<Integer, GraphAttributeType>();
	private static final HashMap<GraphAttributeType, Integer> typeToIdLookup = new HashMap<GraphAttributeType, Integer>();
	private static final HashMap<String, GraphAttributeType> keywordToTypeLookup = new HashMap<String, GraphAttributeType>();
	static {
		for (GraphAttributeType type : GraphAttributeType.values()) {
			idToTypeLookup.put(type.getId(), type);
			typeToIdLookup.put(type, type.getId());
			keywordToTypeLookup.put(type.getKeyword(), type);
		}
	}

	public static GraphAttributeType getTypeIfKeyword(String word) {
		return keywordToTypeLookup.get(word);
	}

	public static GraphAttributeType getTypeFromId(Integer id) {
		return idToTypeLookup.get(id);
	}

	public static Integer getIdFromType(GraphAttributeType type) {
		return typeToIdLookup.get(type);
	}

	public Integer getId() {
		return id;
	}

	public String getKeyword() {
		return keyword;
	}
}