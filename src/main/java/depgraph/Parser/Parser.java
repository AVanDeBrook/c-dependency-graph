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
     * When adding edges the getNodeObjectFromId function is called, which
     * searches a given array list for a specific object with a matching nodeId.
     * If there is no object with a matching nodeId, then it will throw a
     * NullPointerException, which is caught and effectively ignored.
	 *
	 * @param fileContents - A string containing the contents of a single DOT
	 * file
	 * @return A node object representing the relevant values found in the DOT
	 * file
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
                System.out.println("Running for: " + graphName);
				break;
			case NODE_STMT:
                Node newNode = new Node();
				newNode.setNodeId(getNodeIdFromString(tokenizedLine.getValue()));
				newNode.setNodeLabel(getNodeLabelFromString(tokenizedLine.getValue()));
				newNode.setModulePrefix(getModulePrefixFromNodeLabel(newNode.getNodeLabel()));
                newNode.setIsRoot(newNode.getNodeLabel().equals(graphName));

                nodeCollection.add(newNode);
				break;
			case EDGE_STMT:
                Edge newEdge = new Edge();
                String sourceNode = getSourceNodeFromString(tokenizedLine.getValue());
                String destinationNode = getDestinationNodeFromString(tokenizedLine.getValue());

                try {
                    newEdge.setSourceNode(sourceNode);
                    newEdge.setSourceObject(getNodeObjectFromId(nodeCollection, sourceNode));
                } catch (NullPointerException ex) {
                } catch (Exception ex) {
                    ex.printStackTrace();
                }

                try {
                    newEdge.setDestinationNode(destinationNode);
                    newEdge.setDestinationObject(getNodeObjectFromId(nodeCollection, destinationNode));
                } catch (NullPointerException ex) {
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
            if (e.getSourceObject() == null)
                e.setSourceObject(getNodeObjectFromId(nodeCollection, e.getSourceNode()));

            if (e.getDestinationObject() == null)
                e.setDestinationObject(getNodeObjectFromId(nodeCollection, e.getDestinationNode()));
        }

        nodeCollection = cleanUpNodeCollection(nodeCollection);
        edgeCollection = cleanUpEdgeCollection(edgeCollection);

        nodes.addAll(nodeCollection);
        edges.addAll(edgeCollection);
    }

    private ArrayList <Edge> cleanUpEdgeCollection(ArrayList<Edge> oldCollection){
        ArrayList<Edge> newCollection = new ArrayList<Edge>();

        for(Edge tempEdge : oldCollection){
            //rewrite duplicates according to the global node
            if(isDuplicate(tempEdge.getSourceObject().getNodeLabel())){
                Node tempSrcNode = getGlobalNodeFromNodeLabel(tempEdge.getSourceObject().getNodeLabel());
                tempEdge.setSourceNode(tempSrcNode.getNodeId());
                tempEdge.setSourceObject(tempSrcNode);

            }
            if(isDuplicate(tempEdge.getDestinationObject().getNodeLabel())){
                Node tempDstNode = getGlobalNodeFromNodeLabel(tempEdge.getDestinationObject().getNodeLabel());
                tempEdge.setDestinationNode(tempDstNode.getNodeId());
                tempEdge.setDestinationObject(tempDstNode);
            }
            newCollection.add(tempEdge);
        }

        return newCollection;
    }
    /**
     * Returns the global node object that corresponds to a given nodeLabel
     * @param nodeLabel
     * @return Node
     */
    private Node getGlobalNodeFromNodeLabel(String nodeLabel){
       Node returnNode = null;

        for(Node tempNode : this.nodes){
            if(tempNode.getNodeLabel().equals(nodeLabel))
                returnNode = tempNode;
        }

        return returnNode;
    }

    /**
     * Removes nodes in a collection that are already accounted for in the global context
     * @param oldCollection
     * @return newCollection
     */
    private ArrayList <Node> cleanUpNodeCollection(ArrayList<Node> oldCollection){
        ArrayList<Node> newCollection = new ArrayList<Node>();

        for(Node tempNode: oldCollection){
            //only adds new nodes to the global array
            if(!isDuplicate(tempNode.getNodeLabel())){
                newCollection.add(tempNode);
            }
        }

        return newCollection;
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
        String modulePrefix = "";
        if (nodeLabel.indexOf('_') == -1)
            modulePrefix = "RTOS";
        else
            modulePrefix = nodeLabel.substring(0, nodeLabel.indexOf('_'));
        return modulePrefix;
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

    /**
     * Simple search function to find a node that matches a specific ID
     * (nodeId) in a list. Both the list and ID need to passed to this
     * function, however, it works for both source and destination nodes.
     *
     * @param nodeCollection        ArrayList of nodes to search through.
     * @param nodeId                Specific ID to match.
     * @return                      The node with a matching ID.
     * @throws NullPointerException If a node with a matching ID cannot be found,
     *                              then a NullPointerException is thrown.
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

    private boolean isDuplicate(String nodeLabel) {
        boolean duplicate = false;
        for (Node node : nodes)
            if (node.getNodeLabel().equals(nodeLabel))
                duplicate = true;
        return duplicate;
    }

    private String findNodeIdFromLabel(String nodeLabel) {
        String nodeId = "";
        for (Node node : nodes)
            if (node.getNodeLabel().equals(nodeLabel))
                nodeId = node.getNodeId();
        return nodeId;
    }

    /* Setters and Getters  */

    public ArrayList<Node> getNodes() {
		return nodes;
	}

	public ArrayList<Edge> getEdges() {
		return edges;
	}
}
