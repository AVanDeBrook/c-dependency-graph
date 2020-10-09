package depgraph.Parser;

import java.util.*;

public class Parser {
	private Lexer lexer;

	public Parser() {
		// TODO: Constructor
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
        Token token = null;

        for (String line : lines) {
            try {
                token = lexer.tokenize(line);
            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (token == null) {
                    System.out.println("IGNORED\n");
                } else {
                    System.out.println(token);
                }
            }
        }

        // temp return to satisfy errors/warnings
        return new Graph();
	}
}
