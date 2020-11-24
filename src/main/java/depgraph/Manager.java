package depgraph;

import java.util.List;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.Logger;

import depgraph.Configurator.ConfigType;
import depgraph.Configurator.Configurator;
import depgraph.Parser.Edge;
import depgraph.Parser.Node;
import depgraph.Parser.Parser;
import depgraph.Reader.Reader;
import depgraph.GraphWriter.GraphWriter;
import depgraph.ImageRenderer.ImageRenderer;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
	private static Parser parser;
	private static GraphWriter writer;
	private static Logger logger;
	private static ConsoleHandler consoleHandler;
	private static ImageRenderer renderer;

	public static void main(String[] args) {

		initLogger();
		logger.info("Program start, logger initialized");
		configurator = new Configurator();
		reader = new Reader();
		parser = new Parser();
		writer = new GraphWriter();
		renderer = new ImageRenderer();

		try {
			start(args);
		} catch (Exception e) {
			logger.severe("Exception occurred at program start: " + e);
		}
	}

	/**
	 * Initializes our project logger named depgraph. Removes root logger to
	 * suppress duplicate logging/handling. Prints to std err. Sets initial logging
	 * level to INFO until the configurator changes it.
	 *
	 * Syntax for use: Logger logger = Logger.getLogger("depgraph");
	 * logger.log(Level.XXX, "message"); logger.xxx("message");
	 *
	 * Log levels currently in use: SEVERE, WARNING, INFO, FINE
	 *
	 * NOTE: Output to the user that should run no matter what (expected
	 * functionality) should use System.out.println("message") rather than logging
	 */
	private static void initLogger() {
		Logger rootLogger = Logger.getLogger("");
		Handler[] rootHandlers = rootLogger.getHandlers();
		for (Handler h : rootHandlers) {
			h.close();
			rootLogger.removeHandler(h);
		}

		logger = Logger.getLogger("depgraph");
		consoleHandler = new ConsoleHandler();
		logger.addHandler(consoleHandler);
		logger.setLevel(Level.ALL);
		consoleHandler.setLevel(Level.INFO);
	}

	private static void start(String[] args) throws Exception {
		// String[] testArgs = { "-s",
		// "test\\dot-files\\bms_8c_a40eb276efea852638c5ba83e53569ebc_cgraph.dot" };
		// String[] testArgs = { "-d", "test\\dot-files" };
		// String[] testArgs = { "-h" };
		// String[] testArgs = { "-v", "3" };

		List<String> files = null;
		ConfigType fileType = configurator.manageCmdLineArguments(args);
		if (fileType == ConfigType.DIRECTORY) {
			files = reader.readDirectory(configurator.getDirectoryName());
		} else if (fileType == ConfigType.FILE) {
			files = reader.readSingleFile(configurator.getFileName());
		}

		if (files != null) {
			parser.parse(files);
		}

		for (Node node : parser.getNodes()) {
			logger.fine(node.toString());
		}
		for (Edge edge : parser.getEdges()) {
			logger.fine(edge.toString());
		}
		for (depgraph.Parser.Module mod : parser.getModules()) {
			logger.fine(mod.toString());
		}

		writer.setModules(parser.getModules());
		writer.setEdges(parser.getEdges());
		writer.readTemplates();

		System.out.println("Output path: " + configurator.getOutputPath());

		if (!configurator.getOutputPath().equals("")) {
			String outFile = configurator.getOutputPath();
			if (outFile.contains(".")) {
				writer.writeGraph(outFile.split("\\.")[0]);
				renderer.renderImage(outFile);
			} else {
				System.out.println("Error: Output file must have a file extension.");
			}
		} else {
			writer.writeGraph();
			renderer.renderImage();
		}

		logger.info("Program end");
	}
}
