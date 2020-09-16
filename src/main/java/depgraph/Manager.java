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

		configurator.provideLocation();
	}

	public Manager() {

	}

	/**
	 * Method used by Configurator to provide the file location and kick off the
	 * process
	 * 
	 * @param boolean isSingleFile - true if location is a single file, false if
	 *                location is a directory
	 * @param String  location - the location of a single DOT file or a directory
	 *                containing DOT file(s)
	 */
	public void start(boolean isSingleFile, String location) {
		if (location == null) {
			return;
		}

		List<String> files = null;
		if (isSingleFile) {
			files = reader.readSingleFile(location);
		} else {
			files = reader.readDirectory(location);
		}
		if (files == null || files.isEmpty()) {
			return;
		}

		graph = new Graph();
		for (String singleFile : files) {
			Module module = parser.parse(singleFile);
			graph.addModule(module);
		}

		graph = manipulator.manipulate(graph);

		// TODO continue flow
	}
}