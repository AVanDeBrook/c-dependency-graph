package depgraph;

import java.util.List;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
	private static Parser parser;
	private static Manipulator manipulator;
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
	 * Method used by Configurator to provide the file location
	 * 
	 * @param boolean isSingleFile - true if location is a single file, false if
	 *                location is a directory
	 * @param String  location - the location of a single DOT file or a directory
	 *                containing DOT file(s)
	 */
	public void start(boolean isSingleFile, String location) {
		List<String> fileContents = null;
		if (isSingleFile) {
			fileContents = reader.readSingleFile(location);
		} else {
			fileContents = reader.readDirectory(location);
		}
		if (fileContents == null || fileContents.isEmpty()) {
			return;
		}
		graph = new Graph();
		for (String singleFile : fileContents) {
			graph.addModule(parser.parse(singleFile));
		}
		graph = manipulator.manipulate(graph);
	}
}