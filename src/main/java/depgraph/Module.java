package depgraph;

import java.util.List;

public class Module {
    
	List<Node> nodes;
	
	public Module() {
		
	}
	
	public void addNode(Node node) {
		nodes.add(node);
	}
	
	public List<Node> getNodes() {
		return nodes;
	}
}
