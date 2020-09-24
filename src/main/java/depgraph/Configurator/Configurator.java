package depgraph.Configurator;

import java.io.File;

public class Configurator {
	private String nameOfFile;
	private String nameOfDirectory;

	/**
	 * Initializes class attributes to null-strings.
	 */
	public Configurator() {
		nameOfDirectory = "";
		nameOfFile = "";
	}

	/**
	 * Parses command-line arguments and processes/responds to them.
	 *
	 * @param args - command-line arguments passed from main.
	 * @return - type of handle for the reader to process (FILE, DIRECTORY, NONE)
	 */
	public ConfigReturnType manageCmdLineArguments(String[] args) {
		ConfigReturnType retval = ConfigReturnType.NONE;

		if (args.length == 0) {
			System.out.println("No arguments passed.");
			return retval;
		}

		for (int i = 0; i < args.length; i++) {
			if ((args[i].charAt(0) == '-') && (args[i].length() == 2)) {
				switch (args[i].charAt(1)) {
				case 's':
					// Logging purposes: System.out.println("Name of file passed to the program: " + args[i]);
					if (this.processSingleFile(args[++i]))
						retval = ConfigReturnType.FILE;
					break;
				case 'd':
					// Logging purposes: System.out.println("Path of directory passed to the program: " + args[i]);
					if (this.processDirectory(args[++i]))
						retval = ConfigReturnType.DIRECTORY;
                    break;
                case 'h':
				default:
					this.help();
					break;
				}
			}
		}
		return retval;
	}

	/**
	 * Prints help menu to stdout.
	 */
	private void help() {
		System.out.println("Welcome to the C Dependency Graph Tool.\n");
		System.out.println("Command line argument syntax is as follows:");
		System.out.println("gradle run --args=\" [-s <filename> | -d <directory name> | -h] \"\n");
		System.out.println("Possible arguments include -h for help,");
		System.out.println("-s for processing a single file,");
		System.out.println("-d for processing a directory.\n");
		System.out.println("If having problems providing a path to a file or directory, remember that the root directroy is c-dependecy-graph");
        System.out.println("You may not run more than a single command at a time.");
    }

	/**
	 * Checks if the passed file exists and sets the class attribute if it does.
	 *
	 * @param fileName - name of the file.
	 */
	private boolean processSingleFile(String fileName) {
		// source directory is SeniorDesign\c-dependency-graph
		File singleFile = new File(fileName);
		boolean retval = false;

		if (singleFile.isFile()) {
			this.nameOfFile = fileName;
			retval = true;
		} else {
			System.out.println("File name provided cannot resolve to a file");
		}

		return retval;
	}

	/**
	 * Checks if the passed directory exists and sets the class attribute if it does.
	 *
	 * @param directorName - name of the directory.
	 */
	private boolean processDirectory(String directoryName) {
		File singleDirectory = new File(directoryName);
		boolean retval = false;

		if (singleDirectory.isDirectory()) {
			this.nameOfDirectory = directoryName;
			retval = true;
		} else {
			System.out.println("Directory name provided cannot resolve to a directory");
		}

		return retval;
	}

	/**** Accessor Functions  ****/

	/**
	 * Gets file name, if it exists.
	 *
	 * @return - file name.
	 */
	public String getFileName() {
		return nameOfFile;
	}

	/**
	 * Gets directory name, if it exists.
	 *
	 * @return - directory name.
	 */
	public String getDirectoryName() {
		return nameOfDirectory;
	}
}
