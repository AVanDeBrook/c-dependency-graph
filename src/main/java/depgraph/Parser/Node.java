package depgraph.Parser;

import java.util.StringTokenizer;

public class Node {

	private String name;
	private String functionName;
	private String prefix;
	private int[] connection;
	private String[][] attributes;

	public Node() {
	}

	public void setAttributes(String[][] attributes) {
		this.attributes = attributes;
		this.functionName = getFunctionNameFromAttributes();
		StringTokenizer st = new StringTokenizer(this.functionName, "_");
		this.prefix = st.nextToken();
	}

	private String getFunctionNameFromAttributes() {
		int key = 0;
		int value = 1;
		String foundFunctionName = "";
		for (int counter = 0; counter < attributes.length; counter++) {
			if (attributes[counter][key].matches(NodeAttributeType.LABEL.getKeyword())) {
				return attributes[counter][value];
			}
		}
		return foundFunctionName;
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}
}
