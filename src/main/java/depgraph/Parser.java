package depgraph;

public class Parser {
	
	Manager manager;

	public Parser() {
		manager = new Manager();
	}
	
	public Module parse(String singleFileContents) {
		// TODO parse
		Module module = new Module();
		Node node = new Node();
		module.addNode(node);
		return module;
	}
}
