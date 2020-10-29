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

//		String[] testArgs = { "-s", "test\\dot-files\\bms_8c_a40eb276efea852638c5ba83e53569ebc_cgraph.dot" };
//		String[] testArgs = { "-h" };

		List<String> files = null;
		ConfigType fileType = configurator.manageCmdLineArguments(args);

		if (fileType == ConfigType.DIRECTORY) {
			files = reader.readDirectory(configurator.getDirectoryName());
		} else if (fileType == ConfigType.FILE) {
			files = reader.readSingleFile(configurator.getFileName());
		}

		if (files != null) {
			parser.parse(files);
		}

		for (Node node : parser.getNodes()) {
			System.out.println(node);
		}

		for (Edge edge : parser.getEdges()) {
			System.out.println(edge);
		}

		for (depgraph.Parser.Module mod : parser.getModules()) {
			System.out.println(mod);
		}

		// TODO Call graph writer
		// TODO Call DOT runner
	}
}
