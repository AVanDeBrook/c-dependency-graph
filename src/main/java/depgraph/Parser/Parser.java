package depgraph.Parser;

public class Parser {
	private Lexer lexer;

	public Parser() {
		// TODO: Constructor
		lexer = new Lexer();
	}

	// public Parser(List<String> fileContents) {
	// 	lexer = new Lexer();
	// }

	// public Parser(String fileContents) {
	// 	lexer = new Lexer();
	// }

	public ArrayList<Graph> parse(List<String> fileContents) {
		ArrayList<Graph> graphList = new ArrayList<String>();

		for (String contents : fileContents) {
			this.parse(contents);
		}
	}

	public void parse(String fileContents) {
		// TODO: Return type
	}
}
