package depgraph.GraphWriter;

import java.io.*;
import java.util.*;

import depgraph.Parser.Edge;
import depgraph.Parser.Module;
import depgraph.Parser.Node;

/**
 * Class to read graph templates, fill them out and write the resulting graphviz
 * DOT graph to a ".dot" file.
 */
public class GraphWriter {

	/**
	 * General form of an edge definition in DOT based on the official grammar.
	 */
	public final String EDGE_DEFINITION = "%edge.src.id% -> %edge.dest.id%;";

	/**
	 * Configurable path to the graph template. Called "graph.temp" by default.
	 */
	private String graphTemplatePath;

	/**
	 * TODO
	 */
	private String outterSubgraphTemplatePath;

	/**
	 * Configurable path to subgraph template. Called "subgraph.temp" by default.
	 */
	private String innerSubgraphTemplatePath;

	/**
	 * Contents of the graph template file for reference throughout the class.
	 */
	private String graphTemplate;

	/**
	 * TODO
	 */
	private String outterSubgraphTemplate;

	/**
	 * Contents of the subgraph template file for reference throughout the class.
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
		writeGraph("out.dot");
	}

	/**
	 * Core function to build and write the rearranged graph.
	 *
	 * TODO details on drawGraph function
	 *
	 * @param fileName Name of the file to write to.
	 * @throws Exception If there is an error creating the graph or writing it to a
	 *                   file.
	 */
	public void writeGraph(String fileName) throws Exception {
		ArrayList<String> moduleCluster = new ArrayList<String>();
		String graph = graphTemplate;
		String subgraphClusters = "";

		for (Module module : modules) {
			String nodeClusters = getInnerSubgraph(module);
			String subgraphCluster = outterSubgraphTemplate;
			subgraphCluster = subgraphCluster.replaceAll("%subgraph.modulePrefix%", module.getModulePrefix());
			subgraphCluster = subgraphCluster.replaceAll("%subgraph.node_clusters%", nodeClusters);
			moduleCluster.add(subgraphCluster);
		}

		for (String cluster : moduleCluster)
			subgraphClusters += cluster;

		graph = graph.replaceAll("%graph.subgraph_cluster%", subgraphClusters);

		writeToFile(fileName, graph);
	}

	/**
	 * TODO
	 *
	 * @param module
	 * @return
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
	 * TODO
	 *
	 * @param node
	 * @return
	 */
	private String createNodeString(Node node) {
		return String.format("%s [label=\"%s\"];", node.getNodeId(), node.getNodeLabel());
	}

	/**
	 * TODO
	 *
	 * @param module
	 * @param nodesDefs
	 * @param isPublic
	 * @return
	 */
	private String createInnerSubgraphString(Module module, ArrayList<String> nodesDefs, boolean isPublic) {
		String visibility = (isPublic) ? "Public" : "Private";
		String nodes = "";
		String subgraph = "";

		for (String def : nodesDefs)
			nodes += def;

		subgraph = innerSubgraphTemplate.replaceAll("%subgraph.visibility%", visibility);
		subgraph = subgraph.replaceAll("%subgraph.modulePrefix%", module.getModulePrefix());
		subgraph = subgraph.replaceAll("%subgraph.node_defs%", nodes);

		return subgraph;
	}

	/**
	 * TODO
	 *
	 * @param fileName
	 * @param graph
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
