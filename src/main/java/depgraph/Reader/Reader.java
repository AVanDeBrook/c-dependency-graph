package depgraph.Reader;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

/**
 *
 */
public class Reader {

	private static Logger logger;

	public Reader() {
		logger = Logger.getLogger("depgraph");
	}

	/**
	 * Method used to get the contents of a single DOT file at a given location.
	 *
	 * @param filePath - The path of a single DOT file to read.
	 * @return A list containing one string, the contents of the file. Null if
	 * the file path given did not exist.
	 * @throws Exception if passed file is not a dot file.
	 */
	public List<String> readSingleFile(String filePath) throws Exception {
		logger.fine("Reading single file...");
		List<String> filesList = new ArrayList<String>();

		if (isDotFile(filePath)) {
			logger.info("Reading file: " + filePath);
			filesList.add(read(filePath));
		} else {
			System.out.println("Invalid File Extension: Must be '.dot'");
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
	 * @throws Exception if directory does not contain DOT files.
	 */
	public List<String> readDirectory(String directory) throws Exception {
		logger.fine("Reading directory...");
		File folder = new File(directory);
		File[] filesInDir = folder.listFiles();
		List<String> filesList = new ArrayList<String>();

		for (File file : filesInDir) {
			if (file.isFile() && isDotFile(file.toString())) {
				logger.info("Reading file: " + file.toString());
				filesList.add(read(file.toString()));
			}
		}

		if (filesList.isEmpty()) {
			System.out.println("Directory did not contain any DOT files");
			throw new Exception("Directory did not contain any DOT files");
		}

		return filesList;
	}

	/**
	 * Helper function to read contents of file and convert it to a string.
	 *
	 * @param filePath - The path of a file to read.
	 * @return String containing contents of the file.
	 */
	private String read(String filePath) {
		StringBuilder stringBuild = new StringBuilder();
		File file = new File(filePath);
		String line;

		try {
			BufferedReader reader = new BufferedReader(new FileReader(file));
			while ((line = reader.readLine()) != null)
				stringBuild.append(line + "\n");
			reader.close();
		} catch (IOException e) {
			e.printStackTrace();
		}

		return stringBuild.toString();
	}

	/**
	 * Helper function to check if given file is a DOT file.
	 *
	 * @param filePath - The path of a single file.
	 * @return True if DOT file, false otherwise.
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
