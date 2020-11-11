package depgraph.Configurator;

import java.io.File;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

// @formatter:off
/**
 * Responsible for parsing and handling command-line arguments passed to the
 * program.
 *
 * List of possible flags:
 * - s DOT file to process
 * - d directory of DOT files to process.
 * - h print help menu
 *
 * Run in gradle using (replace ... with desired arguments): gradle run --args="..."
 *
 * Otherwise pass arguments normally.
 */
// @formatter:on
public class Configurator {

	private static Logger logger;

	private static Handler[] handlers;

	/**
	 * Name of file, if the passed argument is a single file.
	 */
	private String nameOfFile;

	/**
	 * Name of a directory, if the passed argument is a directory.
	 */
	private String nameOfDirectory;

	/**
	 * No-arg constructor. Initializes class attributes to null-strings.
	 */
	public Configurator() {
		nameOfDirectory = "";
		nameOfFile = "";
		logger = Logger.getLogger("depgraph");
		handlers = logger.getHandlers();
	}

	/**
	 * Parses command-line arguments and processes/responds to them.
	 *
	 * @param args Command-line arguments passed from main.
	 * @return Type of handle for the reader to process (FILE, DIRECTORY, NONE)
	 */
	public ConfigType manageCmdLineArguments(String[] args) {
		logger.fine("Processing command line arguments...");
		ConfigType typeToReturn = ConfigType.NONE;

		for (int i = 0; i < args.length; i++) {
			if ((args[i].charAt(0) == '-') && (args[i].length() == 2)) {
				switch (args[i].charAt(1)) {
				case 's':
					try {
						logger.fine("Single file selected...");
						if (this.processSingleFile(args[++i])) {
							typeToReturn = ConfigType.FILE;
						}
					} catch (ArrayIndexOutOfBoundsException ex) {
						System.out.println("Incorrect format for option -s");
					}
					break;
				case 'd':
					try {
						logger.fine("Directory selected...");
						if (this.processDirectory(args[++i])) {
							typeToReturn = ConfigType.DIRECTORY;
						}
					} catch (ArrayIndexOutOfBoundsException ex) {
						System.out.println("Incorect format for option -d");
					}
					break;
				case 'v':
					processVerbosity(Integer.parseInt(args[++i]));
					break;
				case 'h':
					printHelp();
					break;
				default:
					System.out.println(String.format("Unkown option: %s", args[i]));
					break;
				}
			} else {
				printHelp();
			}
		}

		return typeToReturn;
	}

	private void processVerbosity(int levelofVerbosity) {
		switch (levelofVerbosity) {
		case 0:
			handlers[0].setLevel(Level.SEVERE);
			System.out.println("Verbosity set to SEVERE");
			break;
		case 1:
			handlers[0].setLevel(Level.WARNING);
			System.out.println("Verbosity set to WARNING");
			break;
		case 2:
			handlers[0].setLevel(Level.INFO);
			System.out.println("Verbosity set to INFO");
			break;
		case 3:
			handlers[0].setLevel(Level.FINE);
			System.out.println("Verbosity set to FINE");
			break;
		default:
			System.out.println("Verbosity set to default INFO");
			break;
		}

	}

	/**
	 * Prints help menu to stdout.
	 */
	private void printHelp() {
		System.out.println("\nWelcome to the C Dependency Graph Tool!\n");
		System.out.println("FLAG\tDESCRIPTION\t\tUSAGE");
		System.out.println("-h\tPrint the help menu\t-h");
		System.out.println("-s\tProcess a single file\t-s <file path with extension .dot>");
		System.out.println("-d\tProcess a directory\t-d <directory path>");
		System.out.println("-v\tSet logging verbosity\t-v <0-3>");
		System.out.println("-L\tSet logger output file\t-L <file path>");
		System.out.println("-o\tName program output\t-v <name>");
		System.out.println();
	}

	/**
	 * Checks if the passed file exists and sets the class attribute if it does.
	 *
	 * @param fileName name of the file.
	 * @return True if param was an existing file and class attribute was set,
	 * false otherwise.
	 */
	private boolean processSingleFile(String fileName) {
		File singleFile = new File(fileName);
		boolean success = false;
		logger.fine("Validating file input...");
		if (singleFile.isFile()) {
			logger.fine("File is valid");
			this.nameOfFile = fileName;
			success = true;
		} else {
			System.out.println("File name provided cannot resolve to a file");
		}

		return success;
	}

	/**
	 * Checks if the passed directory exists and sets the class attribute if it
	 * does.
	 *
	 * @param directoryName name of the directory.
	 * @return True if param was an existing directory and class attribute was
	 * set, false otherwise.
	 */
	private boolean processDirectory(String directoryName) {
		File directory = new File(directoryName);
		boolean success = false;
		logger.fine("Validating directory input...");
		if (directory.isDirectory()) {
			logger.fine("Directory is valid");
			this.nameOfDirectory = directoryName;
			success = true;
		} else {
			System.out.println("Directory name provided cannot resolve to a directory");
		}

		return success;
	}

	/* Setters and Getters */

	public String getFileName() {
		return nameOfFile;
	}

	public String getDirectoryName() {
		return nameOfDirectory;
	}
}
