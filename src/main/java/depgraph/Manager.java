package depgraph;

import java.util.List;

import depgraph.Reader.Reader;

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

		try {
			start();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public Manager() {

	}

	/**
	 * Method used by Configurator to provide the file location and kick off the
	 * process
	 */
	public static void start() throws Exception {

		List<String> files = null;
		if (configurator.isDirectory()) {
			files = reader.readDirectory(configurator.getDirectory());
		} else {
			files = reader.readSingleFile(configurator.getFilePath());
		}
		if (files == null) {
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