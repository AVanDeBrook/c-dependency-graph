package depgraph.Parser;

import java.util.*;

/**
 * This is the core of the C Dependency Graph project. This class is what
 * creates an object-oriented representation of the graphs and the nodes and
 * edges within so that they can be changed and reformatted into a module based
 * dependency view by the manipulator.
 *
 * Each input graph is handled one at a time and parsed line by line until the
 * end of the file is reached.
 *
 * The Parser uses a Lexer class contained within the same package to determine
 * what kind of declaration (e.g. node_stmt, edge_stmt, attr_list, etc.) is on
 * the current line in the graph being processed. Based on that, the parser
 * will know how to interpret the information returned from the Lexer's tokenizer function.
 */
public class Parser {

    /**
     * Lexer class that the Parser uses to determine what type of statement is
     * incoming and how to handle it.
     */
    private Lexer lexer;

    /**
     * Collection of Node objects created based on the contents of the dot
     * files passed to the program.
     */
    private ArrayList<Node> nodes;

    /**
     * Collection of Edge objects created based on the connections between
     * nodes defined the dot file(s) passed to the program.
     */
	private ArrayList<Edge> edges;

	/**
	 * No-arg constructor.
	 */
	public Parser() {
        lexer = new Lexer();
        nodes = new ArrayList<Node>();
        edges = new ArrayList<Edge>();
	}

	/**
	 * Kick off point for parsing, used by Manager.
	 *
	 * @param fileContents File contents read by the Reader class in List form
	 */
	public void parse(List<String> fileContents) {
		for (String singleFileContents : fileContents) {
			this.parse(singleFileContents);
		}
	}

	/**
	 * Handles individual files. Passes each line in a file to the Lexer so it
     * can be tokenized and handled more easily. At the moment, this function
     * ignores L_BRACE, R_BRACE, NODE_ATTR_STMT, EDGE_ATTR_STMT, IGNORED, and
     * NONE because they have no real use in the information we are storing.
	 *
	 * @param fileContents - A string containing the contents of a single DOT
	 * file
	 * @return A node object representing the relevant values found in the DOT
	 * file
	 */
	private ArrayList<Node> parse(String fileContents) {
		String[] lines = fileContents.split("\n");
		String graphName = null;

		for (String line : lines) {
			Token tokenizedLine = lexer.tokenize(line);

			switch (tokenizedLine.getToken()) {
			case DIGRAPH_DEF:
				graphName = tokenizedLine.getValue();
				break;
			case NODE_STMT:
				Node newNode = new Node();
				newNode.setNodeId(getNodeIdFromString(tokenizedLine.getValue()));
				newNode.setNodeLabel(getNodeLabelFromString(tokenizedLine.getValue()));
				newNode.setModulePrefix(getModulePrefixFromNodeLabel(newNode.getNodeLabel()));
				newNode.setIsRoot(newNode.getNodeLabel().equals(graphName));
				System.out.println(newNode);
				nodes.add(newNode);
				break;
			case EDGE_STMT:
				Edge newEdge = new Edge();
				newEdge.setSourceNode(getSourceNodeFromString(tokenizedLine.getValue()));
				newEdge.setDestinationNode(getDestinationNodeFromString(tokenizedLine.getValue()));
				System.out.println(newEdge);
				edges.add(newEdge);
				break;
			default:
				break;
			}
		}

		return nodes;
	}

    /**
     * Determines the node_id from a node_stmt.
     *
     * @param valueString node_stmt passed from the Lexer in a Token object
     * @return node_id as defined in the official dot language grammar
     */
	private String getNodeIdFromString(String valueString) {
		return valueString.substring(0, valueString.indexOf('['));
	}

    /**
     * Determines the label of a node from the attribute list (will always be
     * the first attribute in a node_stmt).
     *
     * @param valueString either and attribute list, or the entire node_stmt
     *                    line
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
     *                  getNodeLabelFromString function first and pass the
     *                  result here).
     * @return Module prefix of a function (e.g. BAL, BMS, CONT, etc.)
     */
	private String getModulePrefixFromNodeLabel(String nodeLabel) {
		return nodeLabel.substring(0, nodeLabel.indexOf('_'));
	}

    /**
     * Determines source node from an edge_stmt. edge_stmts always take the
     * form src -> dest (whitespace is omitted by the Lexer).
     *
     * @param valueString edge_stmt line from the Token returned by the Lexer
     * @return node_id of the src node in the edge_stmt
     */
	private String getSourceNodeFromString(String valueString) {
		return valueString.substring(0, valueString.indexOf('-'));
	}

    /**
     * Determines the destination node from an edge_stmt. edge_stmts always
     * take the form src -> dest (whitespace omitted by the Lexer).
     *
     * @param valueString edge_stmt line from the Token returned by the Lexer
     * @return node_id of the dest node in the edge_stmt
     */
	private String getDestinationNodeFromString(String valueString) {
		return valueString.substring(valueString.indexOf('>') + 1, valueString.indexOf('['));
    }

    /* Setters and Getters  */

    public ArrayList<Node> getNodes() {
		return nodes;
	}

	public ArrayList<Edge> getEdges() {
		return edges;
	}
}
