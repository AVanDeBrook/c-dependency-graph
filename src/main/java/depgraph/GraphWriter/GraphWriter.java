package depgraph.GraphWriter;

import java.io.*;
import java.util.*;
import java.util.stream.Collectors;

import depgraph.Parser.Edge;
import depgraph.Parser.Module;
import depgraph.Parser.Node;

/**
 * Class to read templates and create the module based dependency graph in the
 * DOT language. This can be renered by dot (program) afterwards to create the
 * desired image.
 *
 * There are three templates that this class uses called graph.temp,
 * subgraph-inner.temp, and subgraph-outter.temp. These templates specifiy the
 * syntax and styles that are wanted in the resulting image with some special
 * tokens that this class replaces with the proper definitions while creating
 * the final graph.
 *
 * List of all special tokens handled by this class:
 * <ul>
 * <li>%graph.subgraph_cluster% - clusters of subgraphs to render in that
 * specific spot in the graph (note that this is the combination of the inner
 * and outter most subgraphs)</li>
 * <li>%graph.edge_defs% - series of edge definitions (see DOT grammar)</li>
 * <li>%subgraph.visibility% - visibility of the function nodes in the subgraph
 * (e.g. public or private)</li>
 * <li>%subgraph.modulePrefix% - shorthand version of the module name (e.g. ADC,
 * BAL, BMS, etc.)</li>
 * <li>%subgraph.node_defs% - definition of every node in the subgraph</li>
 * <li>%subgraph.node_clusters% - the public/private graphs that contain the
 * actual function nodes (assembled from subgraph-inner.temp)</li>
 * </ul>
 *
 * TODO the writeGraph functions are supposed to throw Exceptions when there is
 * an error creating the graph(s). This is currently not implemented.
 */
public class GraphWriter {

	/**
	 * Configurable path to the graph template. Called "graph.temp" by default.
	 */
	private String graphTemplatePath;

	/**
	 * Configurable path to the outter-most subgraph template. Called
	 * "subgraph-outter.temp" by default.
	 */
	private String outterSubgraphTemplatePath;

	/**
	 * Configurable path to inner-most subgraph template. Called
	 * "subgraph-inner.temp" by default.
	 */
	private String innerSubgraphTemplatePath;

	/**
	 * Contents of the graph template file for reference throughout the class.
	 */
	private String graphTemplate;

	/**
	 * Contents of the outter-most subgraph template file for reference throughout
	 * the class. Refer to test/example/bms-bal-cnt-example.dot for the rationale on
	 * why there is an inner and outter subgraph template.
	 */
	private String outterSubgraphTemplate;

	/**
	 * Contents of teh inner-most subgraph template file for reference throughout
	 * the class. Refer the test/example/bms-bal-cnt-example.dot for the retaionale
	 * on why there is an inner and outter subgraph template.
	 */
	private String innerSubgraphTemplate;

	/**
	 * List of edges created by the Parser class. Needed when creating the
	 * connections between nodes and modules in the dependency graph.
	 */
	private List<Edge> edges;

	/**
	 * List of modules created by the Parser class. Needed when creating the
	 * representation of modules in the dependency graph.
	 */
	private List<Module> modules;

	/**
	 * No-arg constructor
	 */
	public GraphWriter() {
		graphTemplatePath = "templates/graph.temp";
		innerSubgraphTemplatePath = "templates/subgraph-inner.temp";
		outterSubgraphTemplatePath = "templates/subgraph-outter.temp";
		graphTemplate = "";
		innerSubgraphTemplate = "";
		outterSubgraphTemplate = "";

		this.edges = null;
		this.modules = null;
	}

	/**
	 * Construct a GraphWriter with predefined modules and edges.
	 *
	 * @param modules List of modules (created and passed from the Parser class)
	 * @param edges   List of edges (created and passed from the Parser class)
	 */
	public GraphWriter(List<Module> modules, List<Edge> edges) {
		graphTemplatePath = "templates/graph.temp";
		outterSubgraphTemplatePath = "templates/subgraph-outter.temp";
		innerSubgraphTemplatePath = "templates/subgraph-inner.temp";
		graphTemplate = "";
		innerSubgraphTemplate = "";
		outterSubgraphTemplate = "";

		this.edges = edges;
		this.modules = modules;
	}

	/**
	 * Reads graph and subgraph templates from the graphTemplatePath and
	 * subgraphTemplatePath. By default, these are set to "templates/graph.temp" and
	 * "templates/subgraph.temp"
	 */
	public void readTemplates() {
		graphTemplate = readTemplate(graphTemplatePath);
		outterSubgraphTemplate = readTemplate(outterSubgraphTemplatePath);
		innerSubgraphTemplate = readTemplate(innerSubgraphTemplatePath);
	}

	/**
	 * Draws graph with a default name.
	 *
	 * @throws Exception when there is an error creating the output file.
	 */
	public void writeGraph() throws Exception {
		writeGraph("out");
	}

	/**
	 * Core function to build and write the rearranged graph. This function
	 * gets/creates all relevant definitions in the graph such as the node, edge,
	 * graph, and subgraph definitions, then arranges them in the proper order and
	 * writes the graph to a file with the specified name.
	 *
	 * @param fileName Name of the file to write to.
	 * @throws Exception If there is an error creating the graph or writing it to a
	 *                   file.
	 */
	public void writeGraph(String fileName) throws Exception {
		ArrayList<String> moduleCluster = new ArrayList<String>();
		ArrayList<String> nodeDefs = new ArrayList<String>();
		String graph = graphTemplate;

		for (Module module : modules) {
			String nodeClusters = getInnerSubgraph(module);
			String subgraphCluster = outterSubgraphTemplate;
			subgraphCluster = subgraphCluster.replaceAll("%subgraph.modulePrefix%", module.getModulePrefix());
			subgraphCluster = subgraphCluster.replaceAll("%subgraph.node_clusters%", nodeClusters);
			moduleCluster.add(subgraphCluster);
		}

		for (Edge edge : edges)
			nodeDefs.add(createEdgeDefString(edge));

		graph = graph.replaceAll("%graph.subgraph_cluster%", moduleCluster.stream().collect(Collectors.joining()));
		graph = graph.replaceAll("%graph.edge_defs%", nodeDefs.stream().collect(Collectors.joining()));

		writeToFile(fileName + ".dot", graph);
	}

	/**
	 * Helper function to consistently create a node defintion with a guarantee of
	 * correct syntax. <br>
	 * <br>
	 * <strong>Note:</strong> There may be bugs with nodes being rendered outside of
	 * modules, because the DOT grammar says to use node_ids in edge definitions,
	 * they are used here as well. When a node is rendered in the wrong spot it
	 * tends to show up with no label and so, instead, will have a default label of
	 * the node_id (usually something along the lines of "Node#"). <br>
	 * <br>
	 * To help with debugging change edge.getSourceNodeId() and
	 * edge.getDestinationNodeId() to edge.getSourceNodeObject().getNodeLabel() and
	 * edge.getDestinationNodeObject().getNodeLabel(), respectively.
	 *
	 * @param edge Edge object to create a node definition from.
	 * @return String representation of an edge definition according to the DOT
	 *         grammar.
	 */
	private String createEdgeDefString(Edge edge) {
		return String.format("%s -> %s;", edge.getSourceNodeId(), edge.getDestinationNodeId(),
				edge.getSourceNodeObject().getNodeLabel());
	}

	/**
	 * Assembles the inner subgraph into its public and private function boxes.
	 *
	 * @param module Module object to separate into public/private function nodes
	 *               and node_defs.
	 * @return String representation of the public/private functions in a module.
	 *         Note that these are concatenated on return.
	 */
	private String getInnerSubgraph(Module module) {
		String[] subgraphs = { "", "" };
		ArrayList<String> publicNodes = new ArrayList<String>();
		ArrayList<String> privateNodes = new ArrayList<String>();

		for (Node node : module.getNodes()) {
			String nodeDef = createNodeString(node);
			if (node.isPublic())
				publicNodes.add(nodeDef);
			else
				privateNodes.add(nodeDef);
		}

		subgraphs[0] = createInnerSubgraphString(module, publicNodes, true);
		subgraphs[1] = createInnerSubgraphString(module, privateNodes, false);

		return (subgraphs[0] + subgraphs[1]);
	}

	/**
	 * Helper function to consistently create a node definition from a Node object.
	 *
	 * @param node Node object to create a node definition based on.
	 * @return String representation of the node according to the DOT grammar.
	 */
	private String createNodeString(Node node) {
		return String.format("%s [label=\"%s\"];", node.getNodeId(), node.getNodeLabel());
	}

	/**
	 * Helper function to generalize the process of assembling the inner-most part
	 * of the subgraph.
	 *
	 * @param module   Module to assemble into subgraphs.
	 * @param nodeDefs Node definitions (created before with help from the
	 *                 createNodeString function) to assemble into the subgraph.
	 * @param isPublic Boolean representing whether the passed functions are public
	 *                 or private.
	 * @return String representation of the inner-most subgraph according to the DOT
	 *         grammar.
	 */
	private String createInnerSubgraphString(Module module, ArrayList<String> nodeDefs, boolean isPublic) {
		String visibility = (isPublic) ? "Public" : "Private";
		String subgraph = "";

		subgraph = innerSubgraphTemplate.replaceAll("%subgraph.visibility%", visibility);
		subgraph = subgraph.replaceAll("%subgraph.modulePrefix%", module.getModulePrefix());
		subgraph = subgraph.replaceAll("%subgraph.node_defs%", nodeDefs.stream().collect(Collectors.joining()));

		return subgraph;
	}

	/**
	 * Helper function to write the resulting graph to a file.
	 *
	 * @param fileName Name of the file to write.
	 * @param graph    Graph to write to the file.
	 */
	private void writeToFile(String fileName, String graph) {
		try {
			BufferedWriter writer = new BufferedWriter(new FileWriter(new File(fileName)));
			writer.write(graph);
			writer.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}
	}

	/**
	 * Reads template from templatePath and returns the resulting string. File is
	 * read character-by-character, so the string will be an exact copy (including
	 * whitespace).
	 *
	 * @param templatePath Path to the specific template to read.
	 * @return Contents of the template file.
	 */
	private String readTemplate(String templatePath) {
		BufferedReader reader;
		String template = "";
		try {
			reader = new BufferedReader(new FileReader(new File(templatePath)));

			while (reader.ready())
				template += (char) reader.read();

			reader.close();
		} catch (Exception ex) {
			ex.printStackTrace();
		}

		return template;
	}

	/* Setters and Getters */

	public String getGraphTemplatePath() {
		return this.graphTemplatePath;
	}

	public void setGraphTemplatePath(String graphTemplatePath) {
		this.graphTemplatePath = graphTemplatePath;
	}

	public String getInnerSubgraphTemplatePath() {
		return this.innerSubgraphTemplatePath;
	}

	public void setInnerSubgraphTemplatePath(String innerSubgraphTemplatePath) {
		this.innerSubgraphTemplatePath = innerSubgraphTemplatePath;
	}

	public String getOutterSubgraphTemplatePath() {
		return this.outterSubgraphTemplatePath;
	}

	public void setOutterSubgraphTemplatePath(String outterSubgraphTemplatePath) {
		this.outterSubgraphTemplatePath = outterSubgraphTemplatePath;
	}

	public String getGraphTemplate() {
		return this.graphTemplate;
	}

	public String getInnerSubgraphTemplate() {
		return this.innerSubgraphTemplate;
	}

	public String getOutterSubgraphTemplate() {
		return this.outterSubgraphTemplate;
	}

	public List<Module> getModules() {
		return this.modules;
	}

	public void setModules(List<Module> modules) {
		this.modules = modules;
	}

	public List<Edge> getEdges() {
		return this.edges;
	}

	public void setEdges(List<Edge> edges) {
		this.edges = edges;
	}
}
