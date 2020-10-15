package depgraph.Parser;

import java.util.LinkedList;

public class Graph {

	private String name = "";
	private String prefix = "";
	public String[][] edgeAttributes;
	public String[][] nodeAttributes;
	public LinkedList<Node> nodes = new LinkedList<Node>();

	public Graph() {
	}

	public void addNode(Node nodeToAdd) {
		boolean isAlreadyAdded = false;
		for (int i = 0; i < nodes.size(); i++) {
			if (nodes.get(i).getName() == nodeToAdd.getName())
				isAlreadyAdded = true;
		}

		if (!isAlreadyAdded) {
			nodes.add(nodeToAdd);
		}
	}

	public String getName() {
		return this.name;
	}

	public void setName(String name) {
		this.name = name;
	}

	public void setNodeAttributes(String[][] nodeAttributes) {
		this.nodeAttributes = nodeAttributes;
	}

	public void setEdgeAttributes(String[][] edgeAttributes) {
		this.edgeAttributes = edgeAttributes;
	}

	public String getPrefix() {
		return this.prefix;
	}

	public void setPrefix(String prefix) {
		this.prefix = prefix;
	}
}
