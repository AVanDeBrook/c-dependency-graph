package depgraph.Parser;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.StringTokenizer;

public class Parser {

	private Lexer lexer;

	/**
	 * No-arg constructor.
	 */
	public Parser() {
		lexer = new Lexer();
	}

	/**
	 * Kick off point for parsing, used by Manager.
	 */
	public ArrayList<Graph> parse(List<String> fileContents) {
		ArrayList<Graph> graphList = new ArrayList<Graph>();

		for (String singleFileContents : fileContents) {
			graphList.add(this.parse(singleFileContents));
		}

		return graphList;
	}

	/**
	 * Parses file contents and creates a graph object.
	 *
	 * @param fileContents - A string containing the contents of a single DOT
	 * file
	 * @return A graph object representing the relevant values found in the DOT
	 * file
	 */
	private Graph parse(String fileContents) {
		String[] lines = fileContents.split("\n");
		// Token token = null;
		Graph functionGraph = new Graph();

		for (String line : lines) {
			Token tokenizedLine = lexer.tokenize(line);
			// System.out.println(tokenizedLine);

			switch (tokenizedLine.getToken()) {
			case DIGRAPH_DEF:
				StringTokenizer tkp = new StringTokenizer(tokenizedLine.getValue(), "_ \"");
				StringTokenizer tkn = new StringTokenizer(tokenizedLine.getValue(), "\"");

				functionGraph.setName(tkn.nextToken());
				functionGraph.setPrefix(tkp.nextToken());

				System.out.println("\nFunction evaluated: " + functionGraph.getName());
				System.out.println("\nPrefix: " + functionGraph.getPrefix());
				break;
			case L_BRACE:
				System.out.println("\nFunction entered...");
				break;
			case R_BRACE:
				System.out.println("\nFunction exited...");
				break;
			case NODE_ATTR_STMT:
				functionGraph.setNodeAttributes(this.splitIntoKeyValuePairs(tokenizedLine.getValue()));
				break;
			case EDGE_ATTR_STMT:
				functionGraph.setEdgeAttributes(this.splitIntoKeyValuePairs(tokenizedLine.getValue()));
				break;
			case NODE_STMT:
				/*
				 * separate label from attributes save node in the graph array
				 */
				String[] name = new String[2];
				HashMap<String, String> attribtues = new HashMap<String, String>();

				HashMap<String, String> attributes;
				Node newNode = new Node();
				name = this.separateNodeNameFromAttr(tokenizedLine.getValue());
				attributes = this.splitIntoKeyValuePairs(name[1]);
				newNode.setName(name[0]);
				newNode.setAttributes(attributes);
				functionGraph.addNode(newNode);

				break;
			case EDGE_STMT:
				/*
				 * separate label from attributes save connection in the node
				 * array
				 */
				// TODO
				break;
			case IGNORED:
				break;
			case NONE:
				break;
			default:
				break;

			}
		}

		// temp return to satisfy errors/warnings
		return new Graph();
	}

	private HashMap<String, String> splitIntoKeyValuePairs(String unfilteredString) {

		StringTokenizer multiTokenizer = new StringTokenizer(unfilteredString, "[]=,\"");
		int numAttributes = multiTokenizer.countTokens() / 2;
		HashMap<String, String> keyValuePairs = new HashMap<String, String>();

		while (multiTokenizer.hasMoreElements()) {

			String key = multiTokenizer.nextToken();
			String value = multiTokenizer.nextToken();

			keyValuePairs.put(key, value);

			System.out.println("Key: " + key + "\tValue: " + value);

		}

		return keyValuePairs;
	}

	private String[] separateNodeNameFromAttr(String unfilteredNode) {
		StringTokenizer st = new StringTokenizer(unfilteredNode, "[]");
		String[] nameAttributeArray = new String[2];
		nameAttributeArray[0] = st.nextToken();
		if (st.hasMoreElements())
			nameAttributeArray[1] = st.nextToken();

		return nameAttributeArray;
	}

}
