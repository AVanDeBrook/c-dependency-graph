package depgraph.Parser;

import java.util.ArrayList;
import java.util.List;

public class Parser {

	private Lexer lexer;

	/**
	 * No-arg constructor.
	 */
	public Parser() {
		lexer = new Lexer();
	}

	/**
	 * Kick off point for parsing, used by Manager.
	 */
	public ArrayList<Graph> parse(List<String> fileContents) {
		ArrayList<Graph> graphList = new ArrayList<Graph>();

		for (String singleFileContents : fileContents) {
			graphList.add(this.parse(singleFileContents));
		}

		return graphList;
	}

	/**
	 * Parses file contents and creates a graph object.
	 *
	 * @param fileContents - A string containing the contents of a single DOT
	 * file
	 * @return A graph object representing the relevant values found in the DOT
	 * file
	 */
	private Graph parse(String fileContents) {
		String[] lines = fileContents.split("\n");
		// Token token = null;

		for (String line : lines) {
			System.out.println(lexer.tokenize(line));
			String eol = lexer.getEndOfLine();
			if (eol != null) {
				System.out.printf("EOL: %s\n\n", eol);
			}
		}

		// temp return to satisfy errors/warnings
		return new Graph();
	}
}
