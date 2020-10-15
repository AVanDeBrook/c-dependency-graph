package depgraph.Parser;

import java.util.ArrayList;
import java.util.List;

//TODO class javadoc
/**  */
public class Parser {

	private Lexer lexer;
	ArrayList<Node> nodes = new ArrayList<Node>();
	ArrayList<Edge> edges = new ArrayList<Edge>();

	/**
	 * No-arg constructor.
	 */
	public Parser() {
		lexer = new Lexer();
	}

	/**
	 * Kick off point for parsing, used by Manager.
	 * 
	 * @param TODO
	 * @return TODO
	 */
	public void parse(List<String> fileContents) {
		for (String singleFileContents : fileContents) {
			this.parse(singleFileContents);
		}
	}

	public ArrayList<Node> getNodes() {
		return nodes;
	}

	public ArrayList<Edge> getEdges() {
		return edges;
	}

	/**
	 * Parses file contents and adds the graph objects to the class lists. FIXME
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
				/*
				 * Other token types L_BRACE, R_BRACE, NODE_ATTR_STMT,
				 * EDGE_ATTR_STMT, IGNORED, and NONE are ignored because they do
				 * not provide information currently useful to us
				 */
				break;
			}
		}

		return nodes;
	}

	private String getNodeIdFromString(String valueString) {
		return valueString.substring(0, valueString.indexOf('['));
	}

	private String getNodeLabelFromString(String valueString) {
		String assigment = valueString.substring(valueString.indexOf('['), valueString.indexOf(','));
		String nodeLabel = assigment.split("=")[1];
		return nodeLabel;
	}

	private String getModulePrefixFromNodeLabel(String nodeLabel) {
		return nodeLabel.substring(0, nodeLabel.indexOf('_'));
	}

	private String getSourceNodeFromString(String valueString) {
		return valueString.substring(0, valueString.indexOf('-'));
	}

	private String getDestinationNodeFromString(String valueString) {
		return valueString.substring(valueString.indexOf('>') + 1, valueString.indexOf('['));
	}
}
