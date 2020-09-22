package depgraph;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class Reader {

	public Reader() {

	}

	/**
	 * Method used to get the contents of a single DOT file at a given location
	 *
	 * @param the path of a single DOT file to read
	 * @return a list containing one string, the contents of the file. Null if
	 * the file path given did not exist.
	 */
	public List<String> readSingleFile(String filePath) {
		System.out.println("Reader reading single file...");

		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("File given does not exist.");
			return null;
		}

		int index = filePath.lastIndexOf('.');
		if (index > 0) {
			String extension = filePath.substring(index + 1);
			if (!extension.equals("dot")) {
				System.out.println("File given was not a DOT file.");
				return null;
			}
		}

		List<String> filesList = new ArrayList<String>();
		filesList.add(read(filePath));
		return filesList;
	}

	/**
	 * Method used to get the contents of all DOT files in a given directory as
	 * a list of Strings (one string = one file)
	 *
	 * @param a directory containing DOT files
	 * @return a list of strings, each string representing the contents of one
	 * file. Null if the directory did not exist or did not contain DOT files.
	 */
	public List<String> readDirectory(String directory) {
		System.out.println("Reader reading files from directory...");

		File folder = new File(directory);
		if (!folder.isDirectory()) {
			System.out.println("Directory given does not exist.");
			return null;
		}

		List<String> filesList = new ArrayList<String>();
		File[] filesInDir = folder.listFiles();
		for (File file : filesInDir) {
			String filePath = file.toString();
			int index = filePath.lastIndexOf('.');
			if (index > 0) {
				String extension = filePath.substring(index + 1);
				if (file.isFile() && extension.equals("dot")) {
					filesList.add(read(filePath));
				}
			}
		}

		if (filesList.isEmpty()) {
			System.out.println("Directory did not contain any DOT files.");
			return null;
		}

		return filesList;
	}

	public String read(String filePath) {
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

}