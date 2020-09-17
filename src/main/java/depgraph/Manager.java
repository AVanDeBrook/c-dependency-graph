package depgraph;

import java.util.List;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
	private static Parser parser;
	private static Manipulator manipulator;
	private static Graph graph;

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

		start();
	}

	public Manager() {

	}

	/**
	 * Method used by Configurator to provide the file location and kick off the
	 * process
	 */
	public static void start() {
		List<String> files = null;
		if (configurator.isDirectory()) {
			files = reader.readDirectory(configurator.getDirectory());
		} else {
			files = reader.readSingleFile(configurator.getFilePath());
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