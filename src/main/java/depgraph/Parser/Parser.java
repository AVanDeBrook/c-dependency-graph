package depgraph.Parser;

import java.util.*;

public class Parser {
	private Lexer lexer;

	public Parser() {
		lexer = new Lexer();
	}

	public ArrayList<Graph> parse(List<String> fileContents) {
		ArrayList<Graph> graphList = new ArrayList<Graph>();

		for (String contents : fileContents) {
			graphList.add(this.parse(contents));
        }

        return graphList;
	}

	public Graph parse(String fileContents) {
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
