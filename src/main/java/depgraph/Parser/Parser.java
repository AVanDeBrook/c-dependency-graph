package depgraph.Parser;

import java.util.ArrayList;
import java.util.List;

/**
 * The Parser is the core of the C Dependency Graph project. This class creates
 * an object-oriented representation of the graphs and the nodes and edges
 * within so that they can be changed and reformatted into a module based
 * dependency view.
 *
 * Each input graph is handled one at a time and parsed line by line until the
 * end of the file is reached.
 *
 * The Parser uses the Lexer to determine what kind of declaration (e.g.
 * node_stmt, edge_stmt, attr_list, etc.) is on the current line in the graph
 * being processed. Based on that, the Parser will know how to interpret the
 * information returned from the Lexer's tokenizer function.
 */
public class Parser {

	/**
	 * Lexer class that the Parser uses to determine what type of statement is
	 * incoming and how to handle it.
	 */
	private Lexer lexer;

	/**
	 * Collection of Node objects created based on the contents of the DOT
	 * file(s) passed to the program.
	 */
	private ArrayList<Node> nodes;

	/**
	 * Collection of Edge objects created based on the connections between nodes
	 * defined the DOT file(s) passed to the program.
	 */
	private ArrayList<Edge> edges;

	/**
	 * Collection of Module objects created based on the module prefixes parsed
	 * when the nodes are created.
	 */
	private ArrayList<Module> modules;

	/**
	 * No-arg constructor.
	 */
	private int lastNodeId;

	public Parser() {
		lexer = new Lexer();
		nodes = new ArrayList<Node>();
		edges = new ArrayList<Edge>();
		modules = new ArrayList<Module>();
		lastNodeId = 0;
	}

	/**
	 * Kick off point for parsing, used by Manager.
	 *
	 * Parses every individual file handed to the program into nodes and edges,
	 * then separates those nodes into modules based on the parsed module
	 * prefix.
	 *
	 * @param fileContents File contents created by the Reader. In List form,
	 * each String contains the contents of one file.
	 */
	public void parse(List<String> fileContents) {
		for (String singleFileContents : fileContents) {
			this.parse(singleFileContents);
		}

		for (Node node : nodes) {
			Module module = getModuleFromModulePrefix(node.getModulePrefix().toUpperCase());

			if (node.getModulePrefix().equals(""))
				continue;

			if (module == null) {
				module = new Module(node.getModulePrefix().toUpperCase());
				modules.add(module);
			}

			module.add(node);
		}
	}

	/**
	 * Handles a single file's contents. Passes each line of the file to the
	 * Lexer so it can be tokenized to ease handling. At the moment, this
	 * function ignores L_BRACE, R_BRACE, NODE_ATTR_STMT, EDGE_ATTR_STMT,
	 * IGNORED, and NONE because they have no real use in the information we are
	 * storing.
	 *
	 * When an edge is found, setting the Edge object's Node attributes may not
	 * be possible if a node hasn't been parsed yet. We attempt by calling the
	 * getNodeObjectFromId method, which searches for a Node object with a
	 * matching nodeId. If there is not yet an object with a matching nodeId,
	 * then it will throw a NPE, which is caught and effectively ignored. We try
	 * again after we've finished parsing all of the lines in the file.
	 *
	 * @param fileContents A string containing the contents of a single DOT file
	 */
	private void parse(String fileContents) {
		String[] lines = fileContents.split("\n");
		String graphName = null;
		ArrayList<Node> nodeCollection = new ArrayList<Node>();
		ArrayList<Edge> edgeCollection = new ArrayList<Edge>();

		for (String line : lines) {
			Token tokenizedLine = lexer.tokenize(line);

			switch (tokenizedLine.getToken()) {
			case DIGRAPH_DEF:
				graphName = tokenizedLine.getValue();
				//System.out.println("Parsing graph: " + graphName);
				break;
			case NODE_STMT:
				Node newNode = new Node();
				newNode.setNodeId(getNodeIdFromString(tokenizedLine.getValue()));
				newNode.setNodeLabel(getNodeLabelFromString(tokenizedLine.getValue()));
				newNode.setModulePrefix(getModulePrefixFromNodeLabel(newNode.getNodeLabel()));
				newNode.setIsRoot(newNode.getNodeLabel().equals(graphName));
				newNode.setIsPublic(getIsPublicFromNodeLabel(newNode.getNodeLabel()));
				nodeCollection.add(newNode);
				break;
			case EDGE_STMT:
				Edge newEdge = new Edge();
				String sourceNodeId = getSourceNodeIdFromString(tokenizedLine.getValue());
				String destinationNodeId = getDestinationNodeIdFromString(tokenizedLine.getValue());
				newEdge.setSourceNodeId(sourceNodeId);
				newEdge.setDestinationNodeId(destinationNodeId);

				try {
					newEdge.setSourceNodeObject(getNodeObjectFromId(nodeCollection, sourceNodeId));
				} catch (NullPointerException ex) {
					/* Ignored */
				} catch (Exception ex) {
					ex.printStackTrace();
				}
				try {
					newEdge.setDestinationNodeObject(getNodeObjectFromId(nodeCollection, destinationNodeId));
				} catch (NullPointerException ex) {
					/* Ignored */
				} catch (Exception ex) {
					ex.printStackTrace();
				}

				edgeCollection.add(newEdge);
				break;
			default:
				break;
			}
		}

		for (Edge e : edgeCollection) {
			if (e.getSourceNodeObject() == null)
				e.setSourceNodeObject(getNodeObjectFromId(nodeCollection, e.getSourceNodeId()));
			if (e.getDestinationNodeObject() == null)
				e.setDestinationNodeObject(getNodeObjectFromId(nodeCollection, e.getDestinationNodeId()));
		}

		nodeCollection = cleanUpNodeCollection(nodeCollection);
		nodes.addAll(nodeCollection);
		edgeCollection = cleanUpEdgeCollection(edgeCollection);
		edges.addAll(edgeCollection);
	}

	/**
	 * Takes a collection of Edges. If the source or destination Node in an Edge
	 * exists in the global context, it substitutes that for what was previously
	 * stored in the Edge. This aids in keeping nodes unique in the global
	 * context.
	 *
	 * @param oldCollection The list of Edges before updating their Nodes
	 * @return newCollection The list of Edges after updating their Nodes
	 */
	private ArrayList<Edge> cleanUpEdgeCollection(ArrayList<Edge> oldCollection) {
		ArrayList<Edge> newCollection = new ArrayList<Edge>();

		for (Edge edge : oldCollection) {

			if (existsInNodeList(edge.getSourceNodeObject().getNodeLabel())) {
				Node srcNode = getGlobalNodeFromNodeLabel(edge.getSourceNodeObject().getNodeLabel());
				edge.setSourceNodeId(srcNode.getNodeId());
				edge.setSourceNodeObject(srcNode);
			}

			if (existsInNodeList(edge.getDestinationNodeObject().getNodeLabel())) {
				Node dstNode = getGlobalNodeFromNodeLabel(edge.getDestinationNodeObject().getNodeLabel());
				edge.setDestinationNodeId(dstNode.getNodeId());
				edge.setDestinationNodeObject(dstNode);
			}

			if (!edge.getSourceNodeObject().getModulePrefix()
					.equals(edge.getDestinationNodeObject().getModulePrefix())) {
				newCollection.add(edge);
			}
		}

		return newCollection;
	}

	/**
	 * Returns the global Node object that corresponds to a given node label
	 *
	 * @param nodeLabel The label of the Node to search for
	 * @return a Node that exists globally with the given label
	 */
	private Node getGlobalNodeFromNodeLabel(String nodeLabel) {
		for (Node node : this.nodes)
			if (node.getNodeLabel().equals(nodeLabel))
				return node;
		return null;
	}

	/**
	 * Removes nodes in a collection that are already accounted for in the
	 * global context
	 *
	 * @param oldCollection The list of Nodes before updating
	 * @return newCollection The list of Nodes after updating
	 */
	private ArrayList<Node> cleanUpNodeCollection(ArrayList<Node> oldCollection) {
		ArrayList<Node> newCollection = new ArrayList<Node>();
		for (Node node : oldCollection)
			if (!existsInNodeList(node.getNodeLabel())) {
				node.setNodeId("Node" + this.lastNodeId);
				lastNodeId++;
				newCollection.add(node);
			}
		return newCollection;
	}

	/**
	 * Determines the node_id from a node_stmt.
	 *
	 * @param valueString The value of a Lexer created Token which has a
	 * TokenType of node_stmt
	 * @return node_id as defined in the official DOT language grammar
	 */
	private String getNodeIdFromString(String valueString) {
		return valueString.substring(0, valueString.indexOf('['));
	}

	/**
	 * Determines the label of a node from the attribute list (will always be
	 * the first attribute in a node_stmt).
	 *
	 * @param valueString Either an attribute list, or the entire node_stmt line
	 * @return Label of the function
	 */
	private String getNodeLabelFromString(String valueString) {
		String assigment = valueString.substring(valueString.indexOf('['), valueString.indexOf(','));
		String nodeLabel = assigment.split("=")[1];
		return nodeLabel;
	}

	/**
	 * Determines the module prefix of a function from the node label.
	 *
	 * @param nodeLabel Label of the node (easiest to use the
	 * getNodeLabelFromString function first and pass the result here).
	 * @return Module prefix of a function (e.g. BAL, BMS, CONT, etc.)
	 */
	private String getModulePrefixFromNodeLabel(String nodeLabel) {
		String modulePrefix = "";
		if (nodeLabel.indexOf('_') == -1)
			modulePrefix = "RTOS";
		else
			modulePrefix = nodeLabel.substring(0, nodeLabel.indexOf('_'));
		return modulePrefix;
	}

	/**
	 * Determines if a function is public or private based on the function
	 * name's capitalization.
	 *
	 * If there is no module prefix (no '_' char) then it is an RTOS function
	 * and therefore public.
	 *
	 * @param nodeLabel the label of the node, in the form MOD_FunctionName
	 * @return True if the function is public, False if private, based on
	 * capitalization of the function name in nodeLabel
	 */
	private Boolean getIsPublicFromNodeLabel(String nodeLabel) {
		String functionName = "";

		if (nodeLabel.indexOf('_') != -1 && nodeLabel.charAt(0) != '_') {
			functionName = nodeLabel.substring(1, nodeLabel.indexOf('_'));
		} else {
			return true;
		}

		char[] charArray = functionName.toCharArray();

		if (Character.isLowerCase(charArray[0])) {
			return false;
		} else {
			return true;
		}
	}

	/**
	 * Determines source node from an edge_stmt. edge_stmts always take the form
	 * src -> dest (whitespace is omitted by the Lexer).
	 *
	 * @param valueString The value of a Lexer created Token which has a
	 * TokenType of edge_stmt
	 * @return node_id of the source node in the edge_stmt
	 */
	private String getSourceNodeIdFromString(String valueString) {
		return valueString.substring(0, valueString.indexOf('-'));
	}

	/**
	 * Determines the destination node from an edge_stmt. edge_stmts always take
	 * the form src -> dest (whitespace omitted by the Lexer).
	 *
	 * @param valueString The value of a Lexer created Token which has a
	 * TokenType of edge_stmt
	 * @return node_id of the destination node in the edge_stmt
	 */
	private String getDestinationNodeIdFromString(String valueString) {
		return valueString.substring(valueString.indexOf('>') + 1, valueString.indexOf('['));
	}

	/**
	 * Simple search function to find a node that matches a specific ID (nodeId)
	 * in a list. Both the list and ID need to passed to this function, however,
	 * it works for both source and destination nodes.
	 *
	 * @param nodeCollection ArrayList of nodes to search through.
	 * @param nodeId Specific ID to match.
	 * @return The node with a matching ID.
	 * @throws NullPointerException If a node with a matching ID cannot be
	 * found, then a NullPointerException is thrown.
	 */
	private Node getNodeObjectFromId(ArrayList<Node> nodeCollection, String nodeId) throws NullPointerException {
		Node output = null;
		for (Node node : nodeCollection)
			if (node.getNodeId().equals(nodeId))
				output = node;

		if (output == null)
			throw new NullPointerException("Could not find node: " + nodeId + "\n");

		return output;
	}

	/**
	 * Helper function to scan the "global" list of nodes and check if there is
	 * a duplicate of the given node label.
	 *
	 * @param nodeLabel The node label (aka function name) to check against.
	 * @return true if there is a match, false otherwise.
	 */
	private boolean existsInNodeList(String nodeLabel) {
		for (Node node : nodes)
			if (node.getNodeLabel().equals(nodeLabel))
				return true;
		return false;
	}

	/**
	 * Scans the "global" list of modules to find a Module object matching with
	 * a matching module prefix.
	 *
	 * @param modulePrefix Specific module prefix to search for (e.g. ADC, BAL,
	 * etc.).
	 * @return Module object with the matching module prefix, null if one
	 * doesn't exist.
	 */
	private Module getModuleFromModulePrefix(String modulePrefix) {
		Module module = null;

		for (Module m : modules)
			if (m.getModulePrefix().equals(modulePrefix))
				module = m;

		return module;
	}

	/* Setters and Getters */

	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public ArrayList<Edge> getEdges() {
		return edges;
	}

	public ArrayList<Module> getModules() {
		return modules;
	}
}
