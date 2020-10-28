package depgraph;

import java.util.List;

import depgraph.Configurator.ConfigType;
import depgraph.Configurator.Configurator;
import depgraph.Parser.Edge;
import depgraph.Parser.Node;
import depgraph.Parser.Parser;
import depgraph.Reader.Reader;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
	private static Parser parser;

	public static void main(String[] args) {
		configurator = new Configurator();
		reader = new Reader();
		parser = new Parser();

		try {
			start(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void start(String[] args) throws Exception {
		/*
		 * For development only: To run Manager using Eclipse, uncomment the
		 * following line
		 */
		String[] testargs = { "-s", "test\\dot-files\\bms_8c_a40eb276efea852638c5ba83e53569ebc_cgraph.dot" };

		List<String> files = null;
		ConfigType fileType = configurator.manageCmdLineArguments(testargs);

		if (fileType == ConfigType.DIRECTORY) {
			files = reader.readDirectory(configurator.getDirectoryName());
		} else if (fileType == ConfigType.FILE) {
			files = reader.readSingleFile(configurator.getFileName());
		}

		if (files != null) {
			parser.parse(files);
		}

		for (Node node : parser.getNodes()) {
			System.out.println(node.toString());
		}
		for (Edge edge : parser.getEdges()) {
			System.out.println(edge.toString());
		}

		// TODO Call graph writer
		// TODO Call DOT runner
	}
}
