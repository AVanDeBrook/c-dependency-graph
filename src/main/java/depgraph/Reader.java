package depgraph;

import java.io.BufferedReader;
import java.io.*;
import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

public class Reader {

	public Reader() {

	}


	/**
	 * Method used to get the contents of a single DOT file at a given location
	 *If file is given that does not work, it will print that it does not exist
	 *
	 * @param the path of a single DOT file to read
	 * @return a list containing one string, the contents of the file
	 */
	public List<String> readSingleFile(String filePath) {
		System.out.println("Reader reading single file...");
		List<String> filesList = new ArrayList<String>();
		
		File file = new File(filePath);
		if (!file.exists()) {
			System.out.println("File given does not exist.");
			return null;
		}
		
		filesList.add(read(filePath));
		return filesList;
	}

	/**
	 * Method used to get the contents of all DOT files in a given directory as a
	 * list of Strings (one string = one file)
	 * If directory is given that doesn't exist, it will print that it doesn't
	 *
	 * @param a directory containing DOT files
	 * @return a list of strings, each string representing the contents of one file
	 */
	public List<String> readDirectory(String directory) {
		System.out.println("Reader reading files from directory...");
		List<String> filesList = new ArrayList<String>();
		File folder = new File(directory);

		if (!folder.isDirectory()) {
			System.out.println("Directory given does not exist.");
			return null;
		}
		
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