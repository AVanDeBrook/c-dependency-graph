package depgraph.Reader;

import java.io.*;
import java.util.*;

public class Reader {
	/**
	 * Method used to get the contents of a single DOT file at a given location.
	 *
	 * @param filePath - The path of a single DOT file to read.
	 * @return A list containing one string, the contents of the file. Null if
	 * the file path given did not exist.
	 */
	public List<String> readSingleFile(String filePath) throws Exception {
		List<String> filesList = new ArrayList<String>();

		System.out.println("Reading single file...");

		if (isDotFile(filePath)) {
			filesList.add(read(filePath));
		} else {
			throw new Exception("Invalid File Extension: Must be '.dot'");
		}

		return filesList;
	}

	/**
	 * Method used to get the contents of all DOT files in a given directory as
	 * a list of Strings (one string = one file).
	 *
	 * @param directory - A directory containing DOT files.
	 * @return A list of strings, each string representing the contents of one
	 * file. Null if the directory did not exist or did not contain DOT files.
	 */
	public List<String> readDirectory(String directory) throws Exception {
		File folder = new File(directory);
		File[] filesInDir = folder.listFiles();
		List<String> filesList = new ArrayList<String>();

		System.out.println("Reading files from directory...");

		for (File file : filesInDir) {
			if (file.isFile() && isDotFile(file.toString())) {
				filesList.add(read(file.toString()));
			}
		}

		if (filesList.isEmpty()) {
			throw new Exception("Directory did not contain any DOT files.");
		}

		return filesList;
	}

	/**
	 * Helper function to read contents of file and convert it to a string.
	 *
	 * @param filePath - The path of a file to read.
	 * @return - String version of the file.
	 */
	private String read(String filePath) {
		StringBuilder stringBuild = new StringBuilder();
		File file = new File(filePath);
		String line;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while ((line = reader.readLine()) != null)
				stringBuild.append(line);
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringBuild.toString();
	}

	/**
	 * Helper function to check if given file is a 'dot' file.
	 *
	 * @param filePath - The path of a single file.
	 * @return True if dot file, false otherwise.
	 */
	private boolean isDotFile(String filePath) {
		int index = filePath.lastIndexOf('.');
		String extension = filePath.substring(index + 1);

		if (index != -1 && extension.equals("dot")) {
			return true;
		} else {
			return false;
		}
	}
}
