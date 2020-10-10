package depgraph;

import java.util.List;

import depgraph.Configurator.ConfigType;
import depgraph.Configurator.Configurator;
import depgraph.Parser.Graph;
import depgraph.Parser.Parser;
import depgraph.Reader.Reader;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
	private static Parser parser;
	private static Manipulator manipulator;
	private static List<Graph> graphList;

	public static void main(String[] args) {
		configurator = new Configurator();
		reader = new Reader();
		parser = new Parser();
		manipulator = new Manipulator();

		try {
			start(args);
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	private static void start(String[] args) throws Exception {
		List<String> files = null;

//		String[] testArgs = { "-s", "test\\dot-files\\appltask_8c_acbf30997012258f31a0d0b52062dc35b_cgraph.dot" };
//		ConfigType fileType = configurator.manageCmdLineArguments(testArgs);
		ConfigType fileType = configurator.manageCmdLineArguments(args);

		if (fileType == ConfigType.DIRECTORY) {
			files = reader.readDirectory(configurator.getDirectoryName());
		} else if (fileType == ConfigType.FILE) {
			files = reader.readSingleFile(configurator.getFileName());
		}

		if (files == null) {
			return;
		}

		graphList = parser.parse(files);

		// TODO continue flow
	}
}