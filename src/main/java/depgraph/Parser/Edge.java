package depgraph.Parser;

/**
 * Represents edge_stmt from the formal grammar definition for DOT
 */
public class Edge {

	/** TODO */
	private String sourceNode;

	/** TODO */
	private String destinationNode;

	/* Setters and Getters */

	public String getSourceNode() {
		return this.sourceNode;
	}

	public void setSourceNode(String sourceNode) {
		this.sourceNode = sourceNode;
	}

	public String getDestinationNode() {
		return this.destinationNode;
	}

	public void setDestinationNode(String destinationNode) {
		this.destinationNode = destinationNode;
	}

	@Override
	public String toString() {
		// TODO
//		return String.format("NodeId: %s\nNodeLabel: %s\n", this.nodeId, this.nodeLabel);
		return String.format("%s %s", this.sourceNode, this.destinationNode);
	}
}
