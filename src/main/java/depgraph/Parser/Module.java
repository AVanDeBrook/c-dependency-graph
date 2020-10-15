package depgraph.Parser;

import java.util.ArrayList;
import java.util.List;

public class Module {

	List<Node> nodes = new ArrayList<Node>();

	public Module() {
		nodes = new ArrayList<Node>();
	}

	public void addNode(Node node) {
		System.out.println("Adding node to module...");
		nodes.add(node);
	}

	public List<Node> getNodes() {
		return nodes;
	}
}
