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
        boolean printHelp = false;

        for (int i = 0; i < args.length; i++) {
            if ((args[i].charAt(0) == '-') && (args[i].length() == 2)) {
                switch (args[i].charAt(1)) {
                case 's':
                    try {
                        if (this.processSingleFile(args[++i]))
                            retval = ConfigReturnType.FILE;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        System.err.println("Error (option '-s'): Incorrect format");
                        printHelp = false;
                    }
                    break;
                case 'd':
                    try {
                        if (this.processDirectory(args[++i]))
                            retval = ConfigReturnType.DIRECTORY;
                    } catch (ArrayIndexOutOfBoundsException ex) {
                        System.err.println("Error (option '-d'): Incorect format");
                        printHelp = false;
                    }
                    break;
                case 'h':
                    printHelp = true;
                    break;
                default:
                    System.err.printf("Error: Unkown option: %s\n", args[i]);
                    break;
                }
            } else {
                printHelp = true;
            }
        }

        if (printHelp)
            this.help();

        return retval;
	}

	/**
	 * Prints help menu to stdout.
	 */
	private void help() {
        System.out.println("USAGE: [-s <filename> | -d <directory name> | -h]\n");
        System.out.println("Welcome to the C Dependency Graph Tool.\n");
        System.out.println("FLAG\tDESCRIPTION");
		System.out.println("-h\tPrints the help menu");
		System.out.println("-s\tProcess a single file");
		System.out.println("-d\tProcessing a directory");
        // System.out.println("You may not run more than a single command at a time.");
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
			System.err.println("\nError: File name provided cannot resolve to a file");
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
			System.err.println("\nError: Directory name provided cannot resolve to a directory");
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
