package depgraph;

import java.util.List;

public class Reader {

	Manager manager;

	/**
	 * Constructor
	 */
	public Reader() {
		manager = new Manager();
	}

	/**
	 * Method used by Manager to store the contents of a single file at a given
	 * location in a String
	 * 
	 * @param the location of a single DOT file to read
	 */
	public void readSingleFile(String location) {
		List<String> fileContents = null;
		String singleFileContents = null;
		// TODO read contents of single DOT file
		fileContents.add(singleFileContents);
		manager.provideFileContents(fileContents);
	}

	public void readDirectory(String location) {
		List<String> fileContents = null;
		// TODO read contents of all DOT files in directory
		manager.provideFileContents(fileContents);
	}
}
