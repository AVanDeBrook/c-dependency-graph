package depgraph;

import java.util.ArrayList;
import java.util.List;

public class Reader {

	Manager manager;

	public Reader() {
		manager = new Manager();
	}

	/**
	 * Method used to get the contents of a single DOT file at a given location
	 * 
	 * @param location - the path of a single DOT file to read
	 * @return a list containing one string, the contents of the file
	 */
	public List<String> readSingleFile(String location) {
		System.out.println("Reader reading single file...");
		List<String> files = new ArrayList<String>();
		// TODO read contents of single DOT file
		String singleFile = "dummy file contents";
		files.add(singleFile);
		return files;
	}

	/**
	 * Method used to get the contents of all DOT files in a given directory as a
	 * list of Strings (one string = one file)
	 * 
	 * @param the location of a single DOT file to read
	 * @return a list of strings, each string representing the contents of one file
	 */
	public List<String> readDirectory(String location) {
		System.out.println("Reader reading files from directory...");
		List<String> files = new ArrayList<String>();
		// TODO read contents of all DOT files in directory
		String singleFile1 = "dummy file contents 1";
		String singleFile2 = "dummy file contents 2";
		files.add(singleFile1);
		files.add(singleFile2);
		return files;
	}
}
