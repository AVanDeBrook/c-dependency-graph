package depgraph;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.LogManager;
import java.util.logging.Logger;
import java.util.logging.ConsoleHandler;
import java.util.logging.Handler;

import depgraph.Configurator.ConfigType;
import depgraph.Configurator.Configurator;
import depgraph.Parser.Edge;
import depgraph.Parser.Node;
import depgraph.Parser.Parser;
import depgraph.Reader.Reader;

public class Manager {

	private static Configurator configurator;
	private static Reader reader;
    private static Parser parser;
    private static Logger logger;
    private static ConsoleHandler consoleHandler;

	public static void main(String[] args) {

        init_logger();
        logger.finest("Program initializing ...");
		configurator = new Configurator();
		reader = new Reader();
        parser = new Parser();

        //levels of logging include
        /* Severe / warning / info / config / fine / finer / finest */
        /*    0   /    1    /  2   /   3    /  4   /   5   /    6   */

		try {
            logger.info("Program starting ...");
            start(args);
		} catch (Exception e) {
            //e.printStackTrace();
            logger.log(Level.WARNING, "Start() did not run correctly", e);
		}
	}

    private static void init_logger(){
        //removing the root logger to be able to access loggers more definitively
        Logger rootLogger = Logger.getLogger("");
        Handler [] rootHandlers = rootLogger.getHandlers();
        for (Handler h : rootHandlers){
            h.close();
            rootLogger.removeHandler(h);
        }

        //make a generic logger and handler
        logger = Logger.getLogger("depgraph"); //logger for the overall project
        consoleHandler = new ConsoleHandler(); //prints to std err

        logger.addHandler(consoleHandler); //add an output vector
        logger.setLevel(Level.ALL); //log all but dont print all yet
        consoleHandler.setLevel(Level.ALL); //print all for now
    }

	private static void start(String[] args) throws Exception {

//		String[] testArgs = { "-s", "test\\dot-files\\bms_8c_a40eb276efea852638c5ba83e53569ebc_cgraph.dot" };
//		String[] testArgs = { "-h" };

        List<String> files = null;
        logger.finest("Processing arguments ...");
        ConfigType fileType = configurator.manageCmdLineArguments(args);
		if (fileType == ConfigType.DIRECTORY) {
            logger.finest("Reading directory ...");
			files = reader.readDirectory(configurator.getDirectoryName());
		} else if (fileType == ConfigType.FILE) {
            logger.finest("Reading file ...");
			files = reader.readSingleFile(configurator.getFileName());
		}

		if (files != null) {
			parser.parse(files);
		}

		// for (Node node : parser.getNodes()) {
		// 	System.out.println(node);
		// }

		// for (Edge edge : parser.getEdges()) {
		// 	System.out.println(edge);
		// }

		// for (depgraph.Parser.Module mod : parser.getModules()) {
		// 	System.out.println(mod);
		// }

		// TODO Call graph writer
		// TODO Call DOT runner
	}
}
