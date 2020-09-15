package depgraph;

import java.util.List;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
	private static Parser parser;
	private static Manipulator manipulator;

	private String singleFile = null;
	private String directory = null;
	private List<String> fileContents = null;
	private Graph graph;

	public static void main(String[] args) {
		if (args.length != 0) {
			for (String s : args) {
				System.out.println(s);
			}
			return;
		}

		configurator = new Configurator();
		reader = new Reader();
		parser = new Parser();
		manipulator = new Manipulator();
	}

	/**
	 * Constructor
	 */
	public Manager() {

	}

	/**
	 * Method used by Configurator to provide the file location to Reader
	 * 
	 * @param the location of a single DOT file to read
	 */
	public void provideSingleFile(String location) {
		singleFile = location;
		reader.readSingleFile(location);
	}

	/**
	 * Method used by Configurator to provide a directory to Reader
	 * 
	 * @param the directory containing DOT files to read
	 */
	public void provideDirectory(String location) {
		directory = location;
		reader.readDirectory(location);
	}

	/**
	 * Method used by Reader to provide file contents to Parser
	 * 
	 * @param a list of strings, each of which represent the contents of one file to
	 *          be parsed
	 */
	public void provideFileContents(List<String> contents) {
		fileContents = contents;
		graph = new Graph();
		for (String singleFile : fileContents) {
			parser.parse(singleFile);
		}
		manipulator.manipulate(graph);
	}

	/**
	 * Method used by Parser to provide Module to Graph
	 * 
	 * @param a module containing nodes
	 */
	public void collectModule(Module module) {
		graph.addModule(module);
	}

	/**
	 * Method used by Configurator to clear stored values to begin process again
	 * with a new set of files
	 */
	public void clearForNewProcess() {
		singleFile = null;
		directory = null;
		fileContents = null;
	}
}