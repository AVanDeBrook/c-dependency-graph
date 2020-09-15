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
	 * Method used to get the contents of a single DOT file at a given location as a
	 * String
	 * 
	 * @param the location of a single DOT file to read
	 * @return a list containing one string, the contents of the file
	 */
	public List<String> readSingleFile(String location) {
		List<String> fileContents = null;
		String singleFileContents = null;
		// TODO read contents of single DOT file
		fileContents.add(singleFileContents);
		return fileContents;
	}

	/**
	 * Method used to get the contents of all DOT files in a given directory as a
	 * list of Strings (one string = one file)
	 * 
	 * @param the location of a single DOT file to read
	 * @return a list of strings, each string representing the contents of one file
	 */
	public List<String> readDirectory(String location) {
		List<String> fileContents = null;
		// TODO read contents of all DOT files in directory
		return fileContents;
	}
}
