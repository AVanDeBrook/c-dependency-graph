package depgraph.Parser;

/**
 * Essentially a data structure to represent the connections between two nodes.
 * Based on an edge_stmt from the formal grammar definition for DOT.
 *
 * From the official dot language grammar:
 *     edge_stmt: (node_id | subgraph) edgeRHS [attr_list]
 *     edgeRHS:   edgeop (node_id | subgraph) [edgeRHS]
 *     node_id:   ID [port]
 */
public class Edge {

	/**
     * node_id of the source node in the edge_stmt that the Edge class
     * represents (essentially the left-hand side of the edge_stmt).
     */
	private String sourceNode;

	/**
     * node_id of the destination node in the edge_stmt that the Edge class
     * represents (essentially the right-hand side of the edge_stmt).
     */
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
		return String.format("SOURCE: %s\nDESTINATION: %s\n", this.sourceNode, this.destinationNode);
	}
}
