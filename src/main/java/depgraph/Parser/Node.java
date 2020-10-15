package depgraph.Parser;

/**
 * Represents node_stmt from the formal grammar definition for DOT
 */
public class Node {

	/** node_id in the formal grammar definition for DOT */
	private String nodeId;

	/**
	 * the first attribute in the list, appears as label="XXX_XxxXxx" and
	 * matches the graph ID
	 */
	private String nodeLabel;

	/**
	 * The first three letters of nodeLabel, XXX
	 */
	private String modulePrefix;

	/**
	 * This node is a root if the nodeLabel matches the graph name (what appears
	 * next to diagraph at the top of the file)
	 */
	private boolean isRoot;

	/**
	 * No-arg constructor.
	 */
	public Node() {
	}

	/* Setters and Getters */

	public String getNodeId() {
		return this.nodeId;
	}

	public void setNodeId(String nodeId) {
		this.nodeId = nodeId;
	}

	public String getNodeLabel() {
		return this.nodeLabel;
	}

	public void setNodeLabel(String nodeLabel) {
		this.nodeLabel = nodeLabel;
	}

	public String getModulePrefix() {
		return this.modulePrefix;
	}

	public void setModulePrefix(String modulePrefix) {
		this.modulePrefix = modulePrefix;
	}

	public boolean isRoot() {
		return this.isRoot;
	}

	public void setIsRoot(boolean isRoot) {
		this.isRoot = isRoot;
	}

	@Override
	public String toString() {
		// TODO
//		return String.format("NodeId: %s\nNodeLabel: %s\n", this.nodeId, this.nodeLabel);
		return String.format("%s %s %s %s", this.nodeId, this.nodeLabel, this.modulePrefix, this.isRoot);
	}
}
