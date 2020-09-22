package depgraph;

import java.util.List;

import depgraph.Configurator.*;

public class Manager {

	public static void main(String[] args) {
		List<String> fileContents = null;

		Configurator configurator = new Configurator();
		Reader reader = new Reader();
		// Parser parser = new Parser();
		// Manipulator manipulator = new Manipulator();

		ConfigReturnType retval = configurator.manageCmdLineArguments(args);

		if (retval == ConfigReturnType.DIRECTORY)
			fileContents = reader.readDirectory(configurator.getDirectoryName());
		else if (retval == ConfigReturnType.FILE)
			fileContents = reader.readSingleFile(configurator.getFileName());
		else
			return;

		// for (String s : fileContents)
		// 	parser.parse(s);
	}

	public Manager() { }

	/**
	 * Method used by Configurator to provide the file location and kick off the
	 * process
	 *
	 * @param boolean isSingleFile - true if location is a single file, false if
	 *                location is a directory
	 * @param String  location - the location of a single DOT file or a directory
	 *                containing DOT file(s)
	 */
	// public void start(boolean isSingleFile, String location) {
	// 	if (location == null) {
	// 		return;
	// 	}

	// 	List<String> files = null;
	// 	if (isSingleFile) {
	// 		files = reader.readSingleFile(location);
	// 	} else {
	// 		files = reader.readDirectory(location);
	// 	}
	// 	if (files == null || files.isEmpty()) {
	// 		return;
	// 	}

	// 	graph = new Graph();
	// 	for (String singleFile : files) {
	// 		Module module = parser.parse(singleFile);
	// 		graph.addModule(module);
	// 	}

	// 	graph = manipulator.manipulate(graph);

	// 	// TODO continue flow
	// }
}