package depgraph.Parser;

/**
 * Essentially a data structure to represent the connections between two nodes.
 * Based on an edge_stmt from the formal grammar definition for DOT.
 *
 * From the official DOT language grammar: edge_stmt: (node_id | subgraph)
 * edgeRHS [attr_list] edgeRHS: edgeop (node_id | subgraph) [edgeRHS] node_id:
 * ID [port]
 */
public class Edge {

	/**
	 * node_id of the source node in the edge_stmt that the Edge class
	 * represents (essentially the left-hand side of the edge_stmt).
	 */
	private String sourceNodeId;

	/**
	 * Reference to an object with a nodeId that matches the sourceNode.
	 */
	private Node sourceNodeObject;

	/**
	 * node_id of the destination node in the edge_stmt that the Edge class
	 * represents (essentially the right-hand side of the edge_stmt).
	 */
	private String destinationNodeId;

	/**
	 * Reference to an object with a nodeId that matches the destinationNode.
	 */
	private Node destinationNodeObject;

	/**
	 * No-arg constructor
	 */
	public Edge() {
		this.sourceNodeId = "";
		this.destinationNodeId = "";
		this.sourceNodeObject = null;
		this.destinationNodeObject = null;
	}

	/* Setters and Getters */

	public String getSourceNodeId() {
		return this.sourceNodeId;
	}

	public void setSourceNodeId(String sourceNodeId) {
		this.sourceNodeId = sourceNodeId;
	}

	public Node getSourceNodeObject() {
		return this.sourceNodeObject;
	}

	public void setSourceNodeObject(Node sourceObject) {
		this.sourceNodeObject = sourceObject;
	}

	public String getDestinationNodeId() {
		return this.destinationNodeId;
	}

	public void setDestinationNodeId(String destinationNodeId) {
		this.destinationNodeId = destinationNodeId;
	}

	public Node getDestinationNodeObject() {
		return this.destinationNodeObject;
	}

	public void setDestinationNodeObject(Node destinationNodeObject) {
		this.destinationNodeObject = destinationNodeObject;
	}

	@Override
	public String toString() {
		return String.format("SOURCE: %s\nDESTINATION: %s\n", this.sourceNodeId, this.destinationNodeId);
	}
}
